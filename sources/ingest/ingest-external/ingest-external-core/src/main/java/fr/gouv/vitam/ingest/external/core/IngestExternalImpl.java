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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.exception.VitamClientException;
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
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.api.IngestExternal;
import fr.gouv.vitam.ingest.external.api.IngestExternalOutcomeMessage;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.MessageLogbookEngineHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;
import fr.gouv.vitam.workspace.common.WorkspaceFileSystem;

/**
 * Implementation of IngestExtern
 */
public class IngestExternalImpl implements IngestExternal {
    private static final String WORKSPACE_ERROR_MESSAGE = "Erreur de workspace. L'ATR ne sera pas stocké.";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);

    private static final String UNDERSCORE = "_";
    private static final String INGEST_EXT = "STP_SANITY_CHECK_SIP";
    private static final String SANITY_CHECK_SIP = "SANITY_CHECK_SIP";
    private static final String CHECK_CONTAINER = "CHECK_CONTAINER";
    private static final String ATR_NOTIFICATION = "ATR_NOTIFICATION";
    private static final String STP_INGEST_FINALISATION = "STP_INGEST_FINALISATION";

    private static final String CAN_NOT_SCAN_VIRUS = "Can not scan virus";

    private static final String CAN_NOT_STORE_FILE = "Can not store file";

    private static final String IS_NOT_SUPPORTED = " is not supported";

    private static final String SIP_FORMAT = "SIP format :";

    private static final String SIP_WRONG_FORMAT = "SIP Wrong format : ";

    private static final String BEGIN_SIEG_FRIED_FORMAT_IDENTIFICATION = "Begin siegFried format identification";

    private static final String CAN_NOT_READ_FILE = "Can not read file";
    private static final int STATUS_ANTIVIRUS_KO = 2;
    private static final int STATUS_ANTIVIRUS_WARNING = 1;
    private static final int STATUS_ANTIVIRUS_OK = 0;
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final String PRONOM_NAMESPACE = "pronom";
    private final IngestExternalConfiguration config;


    /**
     * Constructor IngestExternalImpl with parameter IngestExternalConfi guration
     *
     * @param config
     */
    public IngestExternalImpl(IngestExternalConfiguration config) {
        this.config = config;

    }

    @Override
    public PreUploadResume preUploadAndResume(InputStream input, String contextId, String action, GUID guid,
        AsyncResponse asyncResponse)
        throws IngestExternalException, WorkspaceClientServerException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
        // Store in local
        final GUID containerName = guid;
        final GUID objectName = guid;
        final GUID ingestGuid = guid;

        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        WorkspaceFileSystem workspaceFileSystem = null;
        Response responseNoProcess = null;
        String contextWithExecutionMode = contextId + UNDERSCORE + action;
        Contexts ingestContext = Contexts.valueOf(contextId);
        // FIXME Correct logbookTypeProcess identification
        LogbookTypeProcess logbookTypeProcess = ingestContext.getLogbookTypeProcess();
        LogbookOperationParameters startedParameters = null;

        try (IngestInternalClient ingestClient =
            IngestInternalClientFactory.getInstance().getClient()) {
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

            startedParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid, ingestContext.getEventType(), containerName,
                logbookTypeProcess, StatusCode.STARTED,
                messageLogbookEngineHelper.getLabelOp(ingestContext.getEventType(), StatusCode.STARTED) + " : " +
                    ingestGuid.toString(),
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
                    ingestContext.getEventType(),
                    containerName,
                    logbookTypeProcess,
                    StatusCode.STARTED,
                    messageLogbookEngineHelper.getLabelOp(INGEST_EXT, StatusCode.STARTED),
                    containerName);
            helper.updateDelegate(sipSanityParameters);

            // call ingest internal with init action (avec contextId)
            try {
                ingestClient.initWorkFlow(contextWithExecutionMode);
            } catch (WorkspaceClientServerException e) {
                LOGGER.error("Worspace Server error", e);
                throw e;
            } catch (VitamException e) {
                throw new IngestExternalException(e);
            }


            workspaceFileSystem = new WorkspaceFileSystem(new StorageConfiguration().setStoragePath(config.getPath()));
            final String antiVirusScriptName = config.getAntiVirusScriptName();
            final long timeoutScanDelay = config.getTimeoutScanDelay();


            try {
                if (containerName != null) {
                    workspaceFileSystem.createContainer(containerName.toString());
                }
            } catch (final ContentAddressableStorageAlreadyExistException |
                ContentAddressableStorageServerException e) {
                LOGGER.error(CAN_NOT_STORE_FILE, e);
                throw new IngestExternalException(e);
            }

            try {
                if (containerName != null) {
                    workspaceFileSystem.putObject(containerName.getId(), objectName.getId(), input);
                    // Implementation of asynchrone
                    AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.ACCEPTED)
                        .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE)
                        .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.UNKNOWN)
                        .build());

                }
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error(CAN_NOT_STORE_FILE, e);
                throw new IngestExternalException(e);
            }

        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException ex) {
            if (workspaceFileSystem != null) {
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
                    }
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteContainer(containerName.getId(), true);
                    }
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
            }
            throw new IngestExternalException(ex);
        } catch (IOException ex) {
            LOGGER.error("Cannot load WorkspaceFileSystem ", ex);
            throw new IllegalStateException(ex);
        }
        return new PreUploadResume(
            helper,
            logbookTypeProcess,
            startedParameters,
            workspaceFileSystem,
            contextWithExecutionMode,
            ingestContext.getEventType());
    }

    @Override
    public Response upload(PreUploadResume preUploadResume, GUID guid)
        throws IngestExternalException {
        final GUID containerName = guid;
        final GUID objectName = guid;
        final GUID ingestGuid = guid;
        Response responseNoProcess = null;
        LogbookTypeProcess logbookTypeProcess = preUploadResume.getLogbookTypeProcess();
        LogbookOperationParameters startedParameters = preUploadResume.getStartedParameters();
        WorkspaceFileSystem workspaceFileSystem = preUploadResume.getWorkspaceFileSystem();
        LogbookOperationsClientHelper helper = preUploadResume.getHelper();
        final String contextWithExecutionMode = preUploadResume.getContextWithExecutionMode();
        final String eventType = preUploadResume.getEventType();
        try {
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

            final String antiVirusScriptName = config.getAntiVirusScriptName();
            final long timeoutScanDelay = config.getTimeoutScanDelay();
            final String containerNamePath = containerName != null ? containerName.getId() : "containerName";
            final String objectNamePath = objectName != null ? objectName.getId() : "objectName";
            final String filePath = config.getPath() + "/" + containerNamePath + "/" + objectNamePath;
            final File file = new File(filePath);
            if (!file.canRead()) {
                LOGGER.error(CAN_NOT_READ_FILE);
                throw new IngestExternalException(CAN_NOT_READ_FILE);
            }
            int antiVirusResult;
            final LogbookOperationParameters antivirusParameters =
                LogbookParametersFactory.newLogbookOperationParameters(
                    ingestGuid,
                    SANITY_CHECK_SIP,
                    containerName,
                    logbookTypeProcess,
                    StatusCode.STARTED,
                    messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.STARTED),
                    containerName);
            // SANITY_CHECK_SIP.STARTED
            helper.updateDelegate(antivirusParameters);
            try {
                /*
                 * Return values of script scan-clamav.sh return 0: scan OK - no virus 1: virus found and corrected 2:
                 * virus found but not corrected 3: Fatal scan not performed
                 */
                antiVirusResult = JavaExecuteScript.executeCommand(antiVirusScriptName, filePath, timeoutScanDelay);
            } catch (final Exception e) {
                LOGGER.error(CAN_NOT_SCAN_VIRUS, e);
                throw new IngestExternalException(e);
            }
            InputStream inputStream = null;
            boolean isFileInfected = false;
            String mimeType = "";
            boolean isSupportedMedia = false;

            // TODO P1 : add fileName to KO_VIRUS string. Cf. todo in IngestExternalResource
            switch (antiVirusResult) {
                case STATUS_ANTIVIRUS_OK:
                    LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.OK);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.OK));
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.OK));
                    break;
                case STATUS_ANTIVIRUS_WARNING:
                case STATUS_ANTIVIRUS_KO:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.KO);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.KO));
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.KO));
                    isFileInfected = true;
                    break;
                default:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    antivirusParameters.setStatus(StatusCode.FATAL);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.KO));
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.KO));
                    isFileInfected = true;
            }
            helper.updateDelegate(antivirusParameters);
            final LogbookOperationParameters endParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                INGEST_EXT,
                containerName,
                logbookTypeProcess,
                StatusCode.UNKNOWN,
                VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.UNKNOWN),
                containerName);
            // update end step param
            if (antivirusParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                endParameters.setStatus(antivirusParameters.getStatus());
                endParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    messageLogbookEngineHelper.getOutcomeDetail(INGEST_EXT, antivirusParameters.getStatus()));
            }

            if (!isFileInfected) {

                final LogbookOperationParameters formatParameters =
                    LogbookParametersFactory.newLogbookOperationParameters(
                        ingestGuid,
                        CHECK_CONTAINER,
                        containerName,
                        logbookTypeProcess,
                        StatusCode.STARTED,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.STARTED),
                        containerName);
                // CHECK_CONTAINER.STARTED
                helper.updateDelegate(formatParameters);

                formatParameters.setStatus(StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetail,
                    messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.OK))
                    .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.OK));

                // instantiate SiegFried final
                try (FormatIdentifier formatIdentifier =
                    FormatIdentifierFactory.getInstance().getFormatIdentifierFor(FORMAT_IDENTIFIER_ID)) {
                    LOGGER.debug(BEGIN_SIEG_FRIED_FORMAT_IDENTIFICATION);

                    // call siegFried
                    final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(file.toPath());
                    final FormatIdentifierResponse format = getFirstPronomFormat(formats);
                    if (format == null) {
                        formatParameters.setStatus(StatusCode.KO);
                        formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                            messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.KO));
                        formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.KO));
                    } else {
                        LOGGER.debug(SIP_FORMAT +
                            format.getMimetype());
                        mimeType = format.getMimetype();
                        if (CommonMediaType.isSupportedFormat(format.getMimetype())) {
                            isSupportedMedia = true;
                        } else {
                            LOGGER.error(SIP_WRONG_FORMAT + format.getMimetype() + IS_NOT_SUPPORTED);
                            formatParameters.setStatus(StatusCode.KO);
                            formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                                messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.KO));
                            formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                                messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.KO,
                                    format.getMimetype()));
                        }
                    }
                } catch (final FormatIdentifierNotFoundException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (final FormatIdentifierFactoryException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (final FormatIdentifierTechnicalException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (final FileFormatNotFoundException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                } catch (final FormatIdentifierBadRequestException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                }
                helper.updateDelegate(formatParameters);
                // update end step param if
                if (formatParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                    endParameters.setStatus(formatParameters.getStatus());
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(INGEST_EXT, formatParameters.getStatus()));
                }
                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    messageLogbookEngineHelper.getLabelOp(INGEST_EXT, endParameters.getStatus()));
                helper.updateDelegate(endParameters);

                if (isSupportedMedia) {
                    try {
                        inputStream = (InputStream) workspaceFileSystem
                            .getObject(containerName.getId(), objectName.getId()).getEntity();
                    } catch (final ContentAddressableStorageException e) {
                        LOGGER.error(e.getMessage());
                        throw new IngestExternalException(e);
                    }
                } else {
                    responseNoProcess = prepareEarlyAtrKo(containerName, ingestGuid, helper, messageLogbookEngineHelper,
                        startedParameters, isFileInfected, mimeType, endParameters, logbookTypeProcess, eventType,
                        StatusCode.KO);
                }
            } else {
                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    messageLogbookEngineHelper.getLabelOp(INGEST_EXT, endParameters.getStatus()));
                helper.updateDelegate(endParameters);
                responseNoProcess =
                    prepareEarlyAtrKo(containerName, ingestGuid, helper, messageLogbookEngineHelper, startedParameters,
                        isFileInfected, mimeType, endParameters, logbookTypeProcess, eventType, StatusCode.KO);
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

                    ingestClient.upload(inputStream, CommonMediaType.valueOf(mimeType), contextWithExecutionMode);
                    return Response.status(Status.OK).build();
                } else {
                    cancelOperation(guid);
                    return responseNoProcess;
                }
            } catch (WorkspaceClientServerException e) {
                return prepareEarlyAtrKo(containerName, ingestGuid, helper, messageLogbookEngineHelper,
                    startedParameters, isFileInfected, mimeType, endParameters, logbookTypeProcess, eventType,
                    StatusCode.FATAL);
            } catch (final VitamException e) {
                throw new IngestExternalException(e);
            }
        } catch (LogbookClientNotFoundException e) {
            throw new IngestExternalException(e);
        } finally {
            if (workspaceFileSystem != null) {
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteObject(containerName.getId(), objectName.getId());
                    }
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
                try {
                    if (containerName != null) {
                        workspaceFileSystem.deleteContainer(containerName.getId(), true);
                    }
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
            }
        }
    }

    private void cancelOperation(GUID guid) throws IngestExternalException {
        try (IngestInternalClient ingestClient = IngestInternalClientFactory.getInstance().getClient()) {
            ingestClient.cancelOperationProcessExecution(guid.getId());
        } catch (final Exception e) {
            throw new IngestExternalException(e);
        }
    }

    /**
     * 
     * @param containerName
     * @param ingestGuid
     * @param helper
     * @param messageLogbookEngineHelper
     * @param startedParameters
     * @param isFileInfected
     * @param mimeType
     * @param endParameters
     * @param logbookTypeProcess
     * @param logbookEventType
     * @param status
     * @return
     * @throws LogbookClientNotFoundException
     * @throws IngestExternalException
     */
    private Response prepareEarlyAtrKo(final GUID containerName, final GUID ingestGuid,
        final LogbookOperationsClientHelper helper, final MessageLogbookEngineHelper messageLogbookEngineHelper,
        final LogbookOperationParameters startedParameters,
        boolean isFileInfected, String mimeType, final LogbookOperationParameters endParameters,
        LogbookTypeProcess logbookTypeProcess, String logbookEventType, StatusCode status)
        throws LogbookClientNotFoundException {
        Response responseNoProcess;
        // Add step started in the logbook
        addStpIngestFinalisationLog(ingestGuid, containerName, helper, StatusCode.STARTED, logbookTypeProcess);
        addTransferNotificationLog(ingestGuid, containerName, helper, StatusCode.STARTED, logbookTypeProcess);
        // FIXME P1 later on real ATR KO
        StatusCode atrStatusCode = StatusCode.OK;
        String atrKo = null;
        try {
	        if (isFileInfected) {
	            atrKo = AtrKoBuilder.buildAtrKo(containerName.getId(), "ArchivalAgencyToBeDefined",
	                "TransferringAgencyToBeDefined",
	                "SANITY_CHECK_SIP", null, status);
	
	        } else if (status.equals(StatusCode.FATAL)) {
	            atrKo = AtrKoBuilder.buildAtrKo(containerName.getId(), "ArchivalAgencyToBeDefined",
	                "TransferringAgencyToBeDefined",
	                "STP_UPLOAD_SIP", null, status);
	        } else {
	            atrKo = AtrKoBuilder.buildAtrKo(containerName.getId(), "ArchivalAgencyToBeDefined",
	                "TransferringAgencyToBeDefined",
	                "CHECK_CONTAINER", ". Format non supporté : " + mimeType, status);
	        }
	
	        if (!status.equals(StatusCode.FATAL)) {
	            storeATR(ingestGuid, atrKo);
	        }
        } catch (IngestExternalException e) {
        	LOGGER.error(e);
        	atrStatusCode = StatusCode.KO;
        }

        LOGGER.warn("ATR KO created : " + atrKo);
        responseNoProcess = new AbstractMockClient.FakeInboundResponse(Status.BAD_REQUEST,
            new ByteArrayInputStream(atrKo.getBytes(CharsetUtils.UTF8)), MediaType.APPLICATION_XML_TYPE, null);
        // add the step in the logbook
        addTransferNotificationLog(ingestGuid, containerName, helper, atrStatusCode, logbookTypeProcess);
        addStpIngestFinalisationLog(ingestGuid, containerName, helper, atrStatusCode, logbookTypeProcess);
        // log final status PROCESS_SIP when sanity check KO or FATAL
        // in this case PROCESS_SIP inherits SANITY_CHECK Status
        startedParameters.setStatus(endParameters.getStatus());
        startedParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(logbookEventType, endParameters.getStatus()));
        startedParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            messageLogbookEngineHelper.getLabelOp(logbookEventType, endParameters.getStatus()));
        // update PROCESS_SIP
        helper.updateDelegate(startedParameters);
        return responseNoProcess;
    }

    private void storeATR(GUID ingestGuid, String atrKo) throws IngestExternalException {
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            client.storeATR(ingestGuid, new ByteArrayInputStream(atrKo.getBytes(CharsetUtils.UTF8)));
        } catch (VitamClientException e) {
            LOGGER.error(e.getMessage());
            throw new IngestExternalException(e);
        }
    }

    private void addStpIngestFinalisationLog(GUID ingestGuid, GUID containerName, LogbookOperationsClientHelper helper,
        StatusCode status, LogbookTypeProcess logbookTypeProcess)
        throws LogbookClientNotFoundException {
        final LogbookOperationParameters stpIngestFinalisationParameters =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                STP_INGEST_FINALISATION,
                containerName,
                logbookTypeProcess,
                status,
                VitamLogbookMessages.getCodeOp(STP_INGEST_FINALISATION, status),
                containerName);
        if (status.equals(StatusCode.FATAL)) {
            stpIngestFinalisationParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                WORKSPACE_ERROR_MESSAGE);
        }
        helper.updateDelegate(stpIngestFinalisationParameters);
    }

    private void addTransferNotificationLog(GUID ingestGuid, GUID containerName, LogbookOperationsClientHelper helper,
        StatusCode status, LogbookTypeProcess logbookTypeProcess)
        throws LogbookClientNotFoundException {
        final LogbookOperationParameters transferNotificationParameters =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                ATR_NOTIFICATION,
                containerName,
                logbookTypeProcess,
                status,
                VitamLogbookMessages.getCodeOp(ATR_NOTIFICATION, status),
                containerName);
        helper.updateDelegate(transferNotificationParameters);
    }

    /**
     * Retrieve the first corresponding file format from pronom referential
     *
     * @param formats formats list to analyze
     * @return the first pronom file format or null if not found
     */
    private FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        for (final FormatIdentifierResponse format : formats) {
            if (PRONOM_NAMESPACE.equals(format.getMatchedNamespace())) {
                return format;
            }
        }
        return null;
    }

    public void createATRFatalWorkspace(String contextId, GUID guid, AsyncResponse asyncResponse)
        throws VitamException {
        Contexts ingestContext = Contexts.valueOf(contextId);
        LogbookTypeProcess logbookTypeProcess = ingestContext.getLogbookTypeProcess();
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

        LogbookOperationParameters startedParameters = LogbookParametersFactory.newLogbookOperationParameters(
            guid, ingestContext.getEventType(), guid,
            logbookTypeProcess, StatusCode.STARTED,
            messageLogbookEngineHelper.getLabelOp(ingestContext.getEventType(), StatusCode.STARTED) + " : " +
                guid.getId(),
            guid);
        helper.createDelegate(startedParameters);
        addStpIngestFinalisationLog(guid, guid, helper, StatusCode.STARTED, logbookTypeProcess);
        addTransferNotificationLog(guid, guid, helper, StatusCode.STARTED, logbookTypeProcess);
        addTransferNotificationLog(guid, guid, helper, StatusCode.OK, logbookTypeProcess);
        addStpIngestFinalisationLog(guid, guid, helper, StatusCode.FATAL, logbookTypeProcess);
        try (IngestInternalClient ingestClient =
            IngestInternalClientFactory.getInstance().getClient()) {
            ingestClient.uploadInitialLogbook(helper.removeCreateDelegate(guid.getId()));
        }
        String atr = AtrKoBuilder.buildAtrKo(guid.getId(), "ArchivalAgencyToBeDefined",
            "TransferringAgencyToBeDefined",
            "STP_UPLOAD_SIP", null, StatusCode.FATAL);

        AsyncInputStreamHelper responseHelper =
            new AsyncInputStreamHelper(asyncResponse, new ByteArrayInputStream(atr.getBytes(CharsetUtils.UTF8)));
        final ResponseBuilder responseBuilder =
            Response.status(Status.SERVICE_UNAVAILABLE).type(MediaType.APPLICATION_OCTET_STREAM)
                .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL);
        responseHelper.writeResponse(responseBuilder);
    }
}
