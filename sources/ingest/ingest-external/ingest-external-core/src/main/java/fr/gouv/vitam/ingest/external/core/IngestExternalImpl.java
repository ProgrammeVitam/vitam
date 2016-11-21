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
import java.util.List;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.AbstractMockClient;
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
import fr.gouv.vitam.common.server2.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.api.IngestExternal;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.api.IngestExternalOutcomeMessage;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.WorkspaceConfiguration;
import fr.gouv.vitam.workspace.core.filesystem.FileSystem;

/**
 * Implementation of IngestExtern
 */
public class IngestExternalImpl implements IngestExternal {

    private static final int STATUS_ANTIVIRUS_KO = 2;
    private static final int STATUS_ANTIVIRUS_WARNING = 1;
    private static final int STATUS_ANTIVIRUS_OK = 0;
    private static final String INGEST_EXT = "STP_SANITY_CHECK_SIP";
    private static final String INGEST_WORKFLOW = "PROCESS_SIP_UNITARY";
    private static final String SANITY_CHECK_SIP = "SANITY_CHECK_SIP";
    private static final String CHECK_CONTAINER = "CHECK_CONTAINER";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";

    private static final String PRONOM_NAMESPACE = "pronom";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);
    private final IngestExternalConfiguration config;
    private static final int DEFAULT_TENANT = 0;

    /**
     * Constructor IngestExternalImpl with parameter IngestExternalConfi guration
     *
     * @param config
     */
    public IngestExternalImpl(IngestExternalConfiguration config) {
        this.config = config;

    }

    @Override
    public Response upload(InputStream input, AsyncResponse asyncResponse) throws IngestExternalException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        final GUID guid = GUIDFactory.newEventGUID(DEFAULT_TENANT);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
        // Store in local
        final GUID containerName = guid;
        final GUID objectName = guid;
        final GUID ingestGuid = guid;
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        FileSystem workspaceFileSystem = null;

        try {

            final LogbookOperationParameters startedParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid, INGEST_WORKFLOW, containerName,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                ingestGuid != null ? ingestGuid.toString() : "outcomeDetailMessage",
                ingestGuid);

            // TODO P1 should be the file name from a header
            if (objectName != null) {
                startedParameters.getMapParameters().put(LogbookParameterName.objectIdentifierIncome,
                    objectName.getId());
            }
            helper.createDelegate(startedParameters);

            final LogbookOperationParameters sipSanityParameters =
                LogbookParametersFactory.newLogbookOperationParameters(
                    ingestGuid,
                    INGEST_EXT,
                    containerName,
                    LogbookTypeProcess.INGEST,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.STARTED),
                    containerName);
            helper.updateDelegate(sipSanityParameters);

            workspaceFileSystem =
                new FileSystem(new WorkspaceConfiguration().setStoragePath(config.getPath()));
            final String antiVirusScriptName = config.getAntiVirusScriptName();
            final long timeoutScanDelay = config.getTimeoutScanDelay();

            try {
                if (containerName != null) {
                    workspaceFileSystem.createContainer(containerName.toString());
                }
            } catch (final ContentAddressableStorageAlreadyExistException e) {
                LOGGER.error("Can not store file", e);
                throw new IngestExternalException(e);
            }
            try {
                if (containerName != null) {
                    workspaceFileSystem.putObject(containerName.getId(), objectName.getId(), input);
                }
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error("Can not store file", e);
                throw new IngestExternalException(e);
            }
            String containerNamePath = containerName != null ? containerName.getId() : "containerName";
            String objectNamePath = objectName != null ? objectName.getId() : "objectName";
            final String filePath = config.getPath() + "/" + containerNamePath + "/" + objectNamePath;
            File file = new File(filePath);
            if (!file.canRead()) {
                LOGGER.error("Can not read file");
                throw new IngestExternalException("Can not read file");
            }
            int antiVirusResult;

            final LogbookOperationParameters antivirusParameters =
                LogbookParametersFactory.newLogbookOperationParameters(
                    ingestGuid,
                    SANITY_CHECK_SIP,
                    containerName,
                    LogbookTypeProcess.INGEST,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(SANITY_CHECK_SIP, StatusCode.STARTED),
                    containerName);
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

            InputStream inputStream = null;
            boolean isFileInfected = false;
            String mimeType = CommonMediaType.ZIP;
            boolean isSupportedMedia = false;

            // TODO P1 : add fileName to KO_VIRUS string. Cf. todo in IngestExternalResource
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
            helper.updateDelegate(antivirusParameters);


            final LogbookOperationParameters endParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                LogbookTypeProcess.INGEST,
                StatusCode.UNKNOWN,
                VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.UNKNOWN),
                containerName);
            // update end step param
            if (antivirusParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                endParameters.setStatus(antivirusParameters.getStatus());
            }

            if (!isFileInfected) {

                final LogbookOperationParameters formatParameters =
                    LogbookParametersFactory.newLogbookOperationParameters(
                        ingestGuid,
                        CHECK_CONTAINER,
                        containerName,
                        LogbookTypeProcess.INGEST,
                        StatusCode.STARTED,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.STARTED),
                        containerName);
                formatParameters.setStatus(StatusCode.OK)
                    .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.OK));
                try {
                    LOGGER.debug("Begin siegFried format identification");
                    // instantiate SiegFried
                    FormatIdentifier formatIdentifier =
                        FormatIdentifierFactory.getInstance().getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
                    // call siegFried
                    List<FormatIdentifierResponse> formats =
                        formatIdentifier.analysePath(file.toPath());
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
                            LOGGER.error("SIP Wrong format : " + format.getMimetype() + " is not supported");
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
                helper.updateDelegate(formatParameters);
                // update end step param
                if (formatParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                    endParameters.setStatus(formatParameters.getStatus());
                }

                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(INGEST_EXT, endParameters.getStatus()));
                helper.updateDelegate(endParameters);

                if (isSupportedMedia) {
                    try {
                        inputStream = (InputStream) workspaceFileSystem
                            .getObject(containerName.getId(), objectName.getId()).getEntity();
                    } catch (final ContentAddressableStorageException e) {
                        LOGGER.error(e.getMessage());
                        throw new IngestExternalException(e);
                    }
                }
            } else {
                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(INGEST_EXT, endParameters.getStatus()));
                helper.updateDelegate(endParameters);
            }

            try (IngestInternalClient ingestClient =
                IngestInternalClientFactory.getInstance().getClient()) {

                // FIXME P1 one should finalize the Logbook Operation with new entries like
                // before calling the ingestClient: LogbookOperationParameters as Ingest-Internal started
                // after calling the ingestClient: LogbookOperationParameters as Ingest-Internal "status"
                // and in async mode add LogbookOperationParameters as Ingest-External-ATR-Forward START
                // and LogbookOperationParameters as Ingest-External-ATR-Forward OK
                // then call back ingestClient with updateFinalLogbook
                ingestClient.uploadInitialLogbook(helper.removeCreateDelegate(containerName.getId()));
                if (!isFileInfected && isSupportedMedia) {
                    Response response = ingestClient.upload(inputStream, CommonMediaType.valueOf(mimeType));
                    AsyncInputStreamHelper asyncHelper = new AsyncInputStreamHelper(asyncResponse, response);
                    ResponseBuilder responseBuilder =
                        Response.status(response.getStatus()).type(MediaType.APPLICATION_OCTET_STREAM);
                    asyncHelper.writeResponse(responseBuilder);
                    return response;
                }
                // FIXME P1 later on real ATR KO
                Response response = new AbstractMockClient.FakeInboundResponse(Status.BAD_REQUEST,
                    AtrKoBuilder.buildAtrKo(containerName.getId(), "ToBeDefined", "ToBeDefined",
                        "File upload " + (isFileInfected ? "is infected" : "has a unsupported Format: " + mimeType)),
                    MediaType.APPLICATION_XML_TYPE, null);
                AsyncInputStreamHelper asyncHelper = new AsyncInputStreamHelper(asyncResponse, response);
                ResponseBuilder responseBuilder =
                    Response.status(response.getStatus()).type(MediaType.APPLICATION_OCTET_STREAM);
                asyncHelper.writeResponse(responseBuilder);
                return response;
            } catch (final VitamException e) {
                throw new IngestExternalException(e);
            }
        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException e) {
            throw new IngestExternalException(e);
        } finally {
            if (workspaceFileSystem != null) {
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
                    }
                } catch (final ContentAddressableStorageNotFoundException e) {
                    LOGGER.warn(e);
                }
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteContainer(containerName.getId());
                    }
                } catch (final ContentAddressableStorageNotFoundException e) {
                    LOGGER.warn(e);
                }
            }
            StreamUtils.closeSilently(input);
        }
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
