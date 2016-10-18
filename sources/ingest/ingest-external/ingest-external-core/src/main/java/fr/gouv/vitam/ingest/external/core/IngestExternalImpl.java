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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.ingest.external.api.IngestExternal;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.api.IngestExternalOutcomeMessage;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
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

    private static final int STATUS_ANTIVIRUS_KO = 2;
    private static final int STATUS_ANTIVIRUS_WARNING = 1;
    private static final int STATUS_ANTIVIRUS_OK = 0;
    private static final String INGEST_EXT = "STP_SANITY_CHECK_SIP";
    private static final String SANITY_CHECK_SIP = "SANITY_CHECK_SIP";
    private static final String CHECK_CONTAINER = "CHECK_CONTAINER";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";

    private static final String PRONOM_NAMESPACE = "pronom";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);
    private final IngestExternalConfiguration config;
    private static final int DEFAULT_TENANT = 0;
    private FormatIdentifier formatIdentifier;

    /**
     * Constructor IngestExternalImpl with parameter IngestExternalConfi guration
     *
     * @param config
     */
    public IngestExternalImpl(IngestExternalConfiguration config) {
        this.config = config;

    }

    @Override
    public Response upload(InputStream input) throws IngestExternalException, XMLStreamException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        final GUID guid = GUIDFactory.newEventGUID(DEFAULT_TENANT);
        // Store in local
        GUID containerName = guid;
        final GUID objectName = guid;
        final GUID ingestGuid = guid;

        final FileSystem workspaceFileSystem =
            new FileSystem(new StorageConfiguration().setStoragePath(config.getPath()));
        final String antiVirusScriptName = config.getAntiVirusScriptName();
        final long timeoutScanDelay = config.getTimeoutScanDelay();
        Response responseResult = null;

        try {
            try {
                workspaceFileSystem.createContainer(containerName.toString());
            } catch (final ContentAddressableStorageAlreadyExistException e) {
                LOGGER.error("Can not store file", e);
                throw new IngestExternalException(e);
            }
            try {
                workspaceFileSystem.putObject(containerName.getId(), objectName.getId(), input);
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error("Can not store file", e);
                throw new IngestExternalException(e);
            }

            final List<LogbookParameters> logbookParametersList = new ArrayList<>();
            final LogbookParameters startedParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.STARTED),
                containerName);

            final LogbookParameters endParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                LogbookTypeProcess.INGEST,
                StatusCode.UNKNOWN,
                VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.UNKNOWN),
                containerName);

            // TODO should be the file name from a header
            startedParameters.getMapParameters().put(LogbookParameterName.objectIdentifierIncome, objectName.getId());
            logbookParametersList.add(startedParameters);

            final String filePath = config.getPath() + "/" + containerName.getId() + "/" + objectName.getId();
            File file = new File(filePath);
            if (!file.canRead()) {
                LOGGER.error("Can not read file");
                throw new IngestExternalException("Can not read file");
            }
            int antiVirusResult;

            try {
                /*
                 * Return values of script scan-clamav.sh return 0: scan OK - no virus 1: virus found and corrected 2:
                 * virus found but not corrected 3: Fatal scan not performed
                 */
                antiVirusResult = JavaExecuteScript.executeCommand(antiVirusScriptName, filePath, timeoutScanDelay);
            } catch (final Exception e) {
                LOGGER.error("Can not scan virus", e);
                throw new IngestExternalException(e);
            }

            final LogbookParameters antivirusParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                SANITY_CHECK_SIP,
                containerName,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(SANITY_CHECK_SIP, StatusCode.STARTED),
                containerName);

            InputStream inputStream = null;
            boolean isFileInfected = false;
            String mimeType = CommonMediaType.ZIP;
            boolean isSupportedMedia = false;

            // TODO: add fileName to KO_VIRUS string. Cf. todo in IngestExternalResource
            switch (antiVirusResult) {
                case STATUS_ANTIVIRUS_OK:
                    LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.OK);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(SANITY_CHECK_SIP, StatusCode.OK));
                    break;
                case STATUS_ANTIVIRUS_WARNING:
                case STATUS_ANTIVIRUS_KO:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.KO);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(SANITY_CHECK_SIP, StatusCode.KO));
                    isFileInfected = true;
                    break;
                default:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.FATAL);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(SANITY_CHECK_SIP, StatusCode.KO));
                    isFileInfected = true;
            }

            logbookParametersList.add(antivirusParameters);
            // update end step param
            if (antivirusParameters.getStatus().getStatusLevel() > endParameters.getStatus().getStatusLevel()) {
                endParameters.setStatus(antivirusParameters.getStatus());
            }

            if (!isFileInfected) {

                final LogbookParameters formatParameters = LogbookParametersFactory.newLogbookOperationParameters(
                    ingestGuid,
                    CHECK_CONTAINER,
                    containerName,
                    LogbookTypeProcess.INGEST,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.OK),
                    containerName);

                try {
                    LOGGER.debug("Begin siegFried format identification");
                    // instantiate SiegFried
                    formatIdentifier =
                        FormatIdentifierFactory.getInstance().getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
                    // call siegFried
                    List<FormatIdentifierResponse> formats =
                        formatIdentifier.analysePath(Paths.get(containerName.getId() + "/" + objectName.getId()));
                    FormatIdentifierResponse format = getFirstPronomFormat(formats);

                    if (format == null) {
                        formatParameters.setStatus(StatusCode.KO);
                        formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.KO));
                    } else {
                        LOGGER.debug("SIP format :" + format.getMimetype());
                        if (CommonMediaType.isSupportedFormat(format.getMimetype())) {
                            mimeType = format.getMimetype();
                            isSupportedMedia = true;
                        } else {
                            formatParameters.setStatus(StatusCode.KO);
                            formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                                VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.KO, format.getMimetype()));
                        }
                    }


                } catch (FormatIdentifierNotFoundException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.FATAL));

                } catch (FormatIdentifierFactoryException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.FATAL));

                } catch (FormatIdentifierTechnicalException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (FileFormatNotFoundException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (FormatIdentifierBadRequestException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.FATAL));
                }
                logbookParametersList.add(formatParameters);
                // update end step param
                if (antivirusParameters.getStatus().getStatusLevel() > endParameters.getStatus().getStatusLevel()) {
                    endParameters.setStatus(antivirusParameters.getStatus());
                }

                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(INGEST_EXT, endParameters.getStatus()));
                logbookParametersList.add(endParameters);

                if (isSupportedMedia) {
                    try {
                        inputStream = workspaceFileSystem.getObject(containerName.getId(), objectName.getId());
                    } catch (final ContentAddressableStorageException e) {
                        LOGGER.error(e.getMessage());
                        throw new IngestExternalException(e.getMessage(), e);
                    }
                }
            } else {
                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(INGEST_EXT, endParameters.getStatus()));
                logbookParametersList.add(endParameters);
            }
            final IngestInternalClient ingestClient =
                IngestInternalClientFactory.getInstance().getIngestInternalClient();

            try {
                // TODO Response async
                responseResult = ingestClient.upload(ingestGuid, logbookParametersList, inputStream, mimeType);
                if (responseResult.getStatus() >= 400) {
                    throw new IngestExternalException("Ingest Internal Exception");
                }
            } catch (final VitamException e) {
                throw new IngestExternalException(e.getMessage(), e);
            }

        } finally {
            try {
                workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
            } catch (final ContentAddressableStorageNotFoundException e) {
                LOGGER.warn(e);
            }
            try {
                workspaceFileSystem.deleteContainer(containerName.getId());
            } catch (final ContentAddressableStorageNotFoundException e) {
                LOGGER.warn(e);
            }
        }

        return responseResult;
    }

    /**
     * Retrieve the first corresponding file format from pronom referential
     *
     * @param formats formats list to analyze
     * @return the first pronom file format or null if not found
     */
    private FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        for (FormatIdentifierResponse format : formats) {
            if (PRONOM_NAMESPACE.equals(format.getMatchedNamespace())) {
                return format;
            }
        }
        return null;
    }
}
