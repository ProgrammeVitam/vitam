/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.ingest.external.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternal;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.api.IngestExternalOutcomeMessage;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.filesystem.FileSystem;

/**
 * Implementation of IngestExtern
 */
public class IngestExternalImpl implements IngestExternal {

    private static final String INGEST_EXT = "Contrôle sanitaire SIP";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);
    private final IngestExternalConfiguration config;
    private static final int DEFAULT_TENANT = 0;

    /**
     * Constructor IngestExternalImpl with parameter IngestExternalConfiguration
     * 
     * @param config
     */
    public IngestExternalImpl(IngestExternalConfiguration config) {
        this.config = config;
    }

    @Override
    public Response upload(InputStream input) throws IngestExternalException, XMLStreamException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        GUID guid = GUIDFactory.newOperationIdGUID(DEFAULT_TENANT);
        // Store in local
        GUID containerName = guid;
        GUID objectName = GUIDFactory.newGUID();
        GUID ingestGuid = guid;

        FileSystem workspaceFileSystem = new FileSystem(new StorageConfiguration().setStoragePath(config.getPath()));
        String antiVirusScriptName = config.getAntiVirusScriptName();
        long timeoutScanDelay = config.getTimeoutScanDelay();
        Response responseResult = null;
        while (true) {
            boolean hasError = false;
            try {
                workspaceFileSystem.createContainer(containerName.toString());
            } catch (ContentAddressableStorageAlreadyExistException e) {
                hasError = true;
                containerName = GUIDFactory.newGUID();
            }
            if (!hasError) {
                break;
            }
        }

        try {
            try {
                workspaceFileSystem.putObject(containerName.getId(), objectName.getId(), input);
            } catch (ContentAddressableStorageException e) {
                LOGGER.error("Can not store file", e);
                throw new IngestExternalException(e);
            }

            List<LogbookParameters> logbookParametersList = new ArrayList<LogbookParameters>();
            LogbookParameters startedParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                LogbookTypeProcess.INGEST,
                LogbookOutcome.STARTED,
                "Début de l'action " + INGEST_EXT,
                containerName);

            // TODO should be the file name from a header
            startedParameters.getMapParameters().put(LogbookParameterName.objectIdentifierIncome, objectName.getId());
            logbookParametersList.add(startedParameters);

            String filePath = config.getPath() + "/" + containerName.getId() + "/" + objectName.getId();
            int antiVirusResult;

            try {
                /*
                 * Return values of script scan-clamav.sh return 0: scan OK - no virus 1: virus found and corrected 2:
                 * virus found but not corrected 3: Fatal scan not performed
                 */
                antiVirusResult = JavaExecuteScript.executeCommand(antiVirusScriptName, filePath, timeoutScanDelay);
            } catch (Exception e) {
                LOGGER.error("Can not scan virus", e);
                throw new IngestExternalException(e);
            }

            LogbookParameters endParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                LogbookTypeProcess.INGEST,
                LogbookOutcome.STARTED,
                "End " + INGEST_EXT,
                containerName);

            InputStream inputStream = null;
            boolean isFileInfected = false;

            // TODO: add fileName to KO_VIRUS string. Cf. todo in IngestExternalResource
            switch (antiVirusResult) {
                case 0:
                    LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
                    endParameters.setStatus(LogbookOutcome.OK);
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        IngestExternalOutcomeMessage.OK_VIRUS.value());
                    break;
                case 1:
                    LOGGER.debug(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    endParameters.setStatus(LogbookOutcome.KO);
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        IngestExternalOutcomeMessage.KO_VIRUS.value());
                    isFileInfected = true;
                    break;
                case 2:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    endParameters.setStatus(LogbookOutcome.KO);
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        IngestExternalOutcomeMessage.KO_VIRUS.value());
                    isFileInfected = true;
                    break;
                default:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    endParameters.setStatus(LogbookOutcome.FATAL);
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        IngestExternalOutcomeMessage.KO_VIRUS.value());
                    isFileInfected = true;
            }

            if (!isFileInfected) {
                try {
                    inputStream = workspaceFileSystem.getObject(containerName.getId(), objectName.getId());
                } catch (ContentAddressableStorageException e) {
                    LOGGER.error(e.getMessage());
                    throw new IngestExternalException(e.getMessage(), e);
                }
            }

            logbookParametersList.add(endParameters);
            IngestInternalClient client = IngestInternalClientFactory.getInstance().getIngestInternalClient();
            
            try {
                //TODO Response async
                responseResult = client.upload(logbookParametersList, inputStream);
                if ( responseResult.getStatus()>= 400) {
                    throw new IngestExternalException("Ingest Internal Exception");
                }
            } catch (VitamException e) {
                throw new IngestExternalException(e.getMessage(), e);
            }
            
        } finally {
            try {
                workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
            } catch (ContentAddressableStorageNotFoundException e) {
                LOGGER.warn(e);
            }
        }
        
        return responseResult;
    }
}
