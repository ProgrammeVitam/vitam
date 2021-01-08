/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.ingest.external.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
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
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.util.ExecutionOutput;
import fr.gouv.vitam.ingest.external.common.util.JavaExecuteScript;
import fr.gouv.vitam.ingest.external.core.exception.ManifestDigestValidationException;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.MessageLogbookEngineHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.common.WorkspaceFileSystem;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FilenameUtils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.i18n.VitamLogbookMessages.getOutcomeDetail;

/**
 * Implementation of IngestExtern
 */
public class IngestExternalImpl implements IngestExternal {
    private static final String WORKSPACE_ERROR_MESSAGE = "Erreur de workspace. L'ATR ne sera pas stocké.";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);

    private static final String INGEST_EXT = "STP_SANITY_CHECK_SIP";
    private static final String SANITY_CHECK_SIP = "SANITY_CHECK_SIP";
    private static final String CHECK_CONTAINER = "CHECK_CONTAINER";
    private static final String ATR_NOTIFICATION = "ATR_NOTIFICATION";
    public static final String INGEST_INT_UPLOAD = "STP_UPLOAD_SIP";
    private static final String MANIFEST_FILE_NAME_CHECK = "MANIFEST_FILE_NAME_CHECK";
    private static final String MANIFEST_DIGEST_CHECK = "MANIFEST_DIGEST_CHECK";

    private static final String STP_INGEST_FINALISATION = "STP_INGEST_FINALISATION";

    private static final String CAN_NOT_SCAN_VIRUS = "Can not scan virus";

    private static final String CAN_NOT_STORE_FILE = "Can not store file";

    private static final String IS_NOT_SUPPORTED = " is not supported";

    private static final String SIP_FORMAT = "SIP format :";

    private static final String SIP_WRONG_FORMAT = "SIP Wrong format : ";

    private static final String BEGIN_SIEG_FRIED_FORMAT_IDENTIFICATION = "Begin siegFried format identification";

    private static final String CAN_NOT_READ_FILE = "Can not read file";
    private static final int STATUS_ANTIVIRUS_NOT_PERFORMED = 3;
    private static final int STATUS_ANTIVIRUS_EXCEPTION_OCCURRED = -1;
    private static final int STATUS_ANTIVIRUS_KO = 2;
    private static final int STATUS_ANTIVIRUS_WARNING = 1;
    private static final int STATUS_ANTIVIRUS_OK = 0;
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final String PRONOM_NAMESPACE = "pronom";
    private final IngestExternalConfiguration config;

    private final FormatIdentifierFactory formatIdentifierFactory;
    private final IngestInternalClientFactory ingestInternalClientFactory;
    private final ManifestDigestValidator manifestDigestValidator;

    public IngestExternalImpl(IngestExternalConfiguration config,
        FormatIdentifierFactory formatIdentifierFactory,
        IngestInternalClientFactory ingestInternalClientFactory,
        ManifestDigestValidator manifestDigestValidator) {
        this.config = config;
        this.formatIdentifierFactory = formatIdentifierFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
        this.manifestDigestValidator = manifestDigestValidator;
    }

    @Override
    public PreUploadResume preUploadAndResume(InputStream input, String workflowIdentifier, GUID guid,
        AsyncResponse asyncResponse)
        throws IngestExternalException, VitamClientException, WorkspaceClientServerException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        WorkspaceFileSystem workspaceFileSystem;
        LogbookOperationParameters startedParameters;
        WorkFlow workflow;
        try (IngestInternalClient ingestClient = ingestInternalClientFactory.getClient()) {
            // Load workflow information from processing
            Optional<WorkFlow> optional = ingestClient.getWorkflowDetails(workflowIdentifier);
            if (!optional.isPresent()) {
                throw new WorkflowNotFoundException("Workflow " + workflowIdentifier + " not found");
            }
            workflow = optional.get();

            LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(workflow.getTypeProc());
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

            startedParameters = LogbookParameterHelper.newLogbookOperationParameters(
                guid, workflow.getIdentifier(), guid,
                logbookTypeProcess, StatusCode.STARTED,
                messageLogbookEngineHelper.getLabelOp(workflow.getIdentifier(), StatusCode.STARTED) + " : " +
                    guid.toString(),
                guid);

            startedParameters.getMapParameters().put(LogbookParameterName.objectIdentifierIncome, guid.getId());

            helper.createDelegate(startedParameters);

            String eventTypeStarted = VitamLogbookMessages.getEventTypeStarted(INGEST_EXT);
            LogbookOperationParameters sipSanityParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDFactory.newEventGUID(guid),
                    eventTypeStarted,
                    guid,
                    logbookTypeProcess,
                    StatusCode.OK,
                    messageLogbookEngineHelper.getLabelOp(eventTypeStarted, StatusCode.OK),
                    guid);
            helper.updateDelegate(sipSanityParameters);

            try {
                LOGGER.debug("Initialize Workflow operation (" + guid + ") ... ");
                ingestClient.initWorkflow(workflow);
            } catch (WorkspaceClientServerException e) {
                LOGGER.error("Workspace Server error", e);
                e.setWorkflowIdentifier(workflow.getIdentifier());
                throw e;
            } catch (VitamException e) {
                throw new IngestExternalException(e);
            }
            LOGGER.debug("Workflow initialized operation (" + guid + ") ... ");

            workspaceFileSystem = new WorkspaceFileSystem(new StorageConfiguration().setStoragePath(config.getPath()));
            workspaceFileSystem.createContainer(guid.toString());

            LOGGER.debug("Put file in local disk to be analysed operation (" + guid + ")");
            workspaceFileSystem.putObject(guid.getId(), guid.getId(), input);
            // Implementation of asynchrone
            LOGGER.debug("Responds to asyncResponse the continue in background operation (" + guid + ")");
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.ACCEPTED)
                .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.UNKNOWN)
                .build(), input);

        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException ex) {
            throw new IngestExternalException(ex);
        } catch (IOException ex) {
            LOGGER.error("Cannot load WorkspaceFileSystem ", ex);
            throw new IllegalStateException(ex);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(CAN_NOT_STORE_FILE, e);
            throw new IngestExternalException(e);
        }

        return new PreUploadResume(
            helper,
            workflow,
            startedParameters,
            workspaceFileSystem);
    }

    @Override
    public StatusCode upload(PreUploadResume preUploadResume, String xAction, GUID guid,
        String manifestDigestValue, String manifestDigestAlgo)
        throws IngestExternalException {
        final GUID ingestExtGuid = GUIDFactory.newEventGUID(guid);
        LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(preUploadResume.getWorkFlow().getTypeProc());
        WorkspaceFileSystem workspaceFileSystem = preUploadResume.getWorkspaceFileSystem();
        LogbookOperationsClientHelper helper = preUploadResume.getHelper();
        try {
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

            final String antiVirusScriptName = config.getAntiVirusScriptName();
            final long timeoutScanDelay = config.getTimeoutScanDelay();
            final String containerNamePath = guid.getId();
            final String objectNamePath = guid.getId();
            final String filePath = FilenameUtils.normalize(config.getPath() + "/" + containerNamePath + "/" + objectNamePath);
            try {
                SafeFileChecker.checkSafeFilePath(filePath);
            } catch (IOException e) {
                throw new IngestExternalException("File path " + filePath + " is invalid", e);
            }
            final File file = new File(filePath);
            if (!file.canRead()) {
                LOGGER.error(CAN_NOT_READ_FILE);
                throw new IngestExternalException(CAN_NOT_READ_FILE);
            }
            ExecutionOutput executionOutput;
            final LogbookOperationParameters antivirusParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDFactory.newEventGUID(guid),
                    SANITY_CHECK_SIP,
                    guid,
                    logbookTypeProcess,
                    StatusCode.OK,
                    messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.OK),
                    guid);
            antivirusParameters.putParameterValue(LogbookParameterName.parentEventIdentifier, ingestExtGuid.getId());
            // SANITY_CHECK_SIP.STARTED
            try {
                /*
                 * Return values of script scan-clamav.sh return 0: scan OK - no virus 1: virus found and corrected 2:
                 * virus found but not corrected 3: Fatal scan not performed
                 */
                executionOutput = JavaExecuteScript.executeCommand(antiVirusScriptName, filePath, timeoutScanDelay);
            } catch (final Exception e) {
                LOGGER.error(CAN_NOT_SCAN_VIRUS, e);
                throw new IngestExternalException(e);
            }
            InputStream inputStream = null;
            boolean isFileInfected = false;
            String mimeType = "";
            boolean isSupportedMedia = false;
            ManifestFileName manifestFileName = null;

            switch (executionOutput.getExitCode()) {
                case STATUS_ANTIVIRUS_OK:
                    LOGGER.info(IngestExternalOutcomeMessage.OK_VIRUS.toString());
                    // nothing to do, default already set to ok
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
                case STATUS_ANTIVIRUS_NOT_PERFORMED:
                case STATUS_ANTIVIRUS_EXCEPTION_OCCURRED:
                    LOGGER.error("{},{},{}", IngestExternalOutcomeMessage.FATAL_VIRUS.toString(),
                        executionOutput.getStdout(), executionOutput.getStderr());
                    antivirusParameters.setStatus(StatusCode.FATAL);
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(SANITY_CHECK_SIP, StatusCode.FATAL));
                    antivirusParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(SANITY_CHECK_SIP, StatusCode.FATAL));
                    isFileInfected = true;
                    break;
                default:
                    break;
            }

            final LogbookOperationParameters endParameters = LogbookParameterHelper.newLogbookOperationParameters(
                ingestExtGuid,
                INGEST_EXT,
                guid,
                logbookTypeProcess,
                StatusCode.UNKNOWN,
                VitamLogbookMessages.getCodeOp(INGEST_EXT, StatusCode.UNKNOWN),
                guid);
            // update end step param
            if (antivirusParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                endParameters.setStatus(antivirusParameters.getStatus());
                endParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    messageLogbookEngineHelper.getOutcomeDetail(INGEST_EXT, antivirusParameters.getStatus()));
            }

            if (!isFileInfected) {

                final LogbookOperationParameters formatParameters =
                    LogbookParameterHelper.newLogbookOperationParameters(
                        GUIDFactory.newEventGUID(guid),
                        CHECK_CONTAINER,
                        guid,
                        logbookTypeProcess,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(CHECK_CONTAINER, StatusCode.OK),
                        guid);
                formatParameters.putParameterValue(LogbookParameterName.parentEventIdentifier, ingestExtGuid.getId());
                // CHECK_CONTAINER.STARTED

                // instantiate SiegFried final
                try {
                    final FormatIdentifier formatIdentifier =
                        formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
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
                } catch (final FormatIdentifierNotFoundException | FormatIdentifierBadRequestException | FileFormatNotFoundException | FormatIdentifierTechnicalException | FormatIdentifierFactoryException e) {
                    LOGGER.error(e);
                    formatParameters.setStatus(StatusCode.FATAL);
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(CHECK_CONTAINER, StatusCode.FATAL));
                    formatParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        messageLogbookEngineHelper.getLabelOp(CHECK_CONTAINER, StatusCode.FATAL));
                }

                // update end step param if
                if (formatParameters.getStatus().compareTo(endParameters.getStatus()) > 1) {
                    endParameters.setStatus(formatParameters.getStatus());
                    endParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                        messageLogbookEngineHelper.getOutcomeDetail(INGEST_EXT, formatParameters.getStatus()));
                }

                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    messageLogbookEngineHelper.getLabelOp(INGEST_EXT, endParameters.getStatus()));

                LogbookOperationParameters manifestFileNameCheck;
                LogbookOperationParameters manifestFileDigestCheckFailure = null;

                InputStream inputStreamTmp = null;
                if (isSupportedMedia) {
                    manifestFileNameCheck = LogbookParameterHelper.newLogbookOperationParameters(
                        GUIDFactory.newEventGUID(guid),
                        MANIFEST_FILE_NAME_CHECK,
                        guid,
                        logbookTypeProcess,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(MANIFEST_FILE_NAME_CHECK, StatusCode.OK),
                        guid);
                    manifestFileNameCheck
                        .putParameterValue(LogbookParameterName.parentEventIdentifier, ingestExtGuid.getId());
                    try {
                        // check manifest file name by regex
                        inputStreamTmp = (InputStream) workspaceFileSystem
                            .getObject(guid.getId(), guid.getId(), null, null).getEntity();
                        manifestFileName = checkManifestFile(inputStreamTmp, mimeType, manifestDigestAlgo, manifestDigestValue);
                        if (manifestFileName.isManifestFile()) {
                            inputStream = (InputStream) workspaceFileSystem
                                .getObject(guid.getId(), guid.getId(), null, null).getEntity();
                        } else {
                            LOGGER.error("Nom du fichier manifest n'est pas conforme");

                            manifestFileNameCheck.setStatus(StatusCode.KO);
                            manifestFileNameCheck.putParameterValue(LogbookParameterName.outcomeDetail,
                                messageLogbookEngineHelper.getOutcomeDetail(MANIFEST_FILE_NAME_CHECK, StatusCode.KO));
                            manifestFileNameCheck.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                                messageLogbookEngineHelper.getLabelOp(MANIFEST_FILE_NAME_CHECK, StatusCode.KO));
                            ObjectNode msg = JsonHandler.createObjectNode();
                            msg.put("FileName", manifestFileName.getFileName());
                            msg.put("AllowedCharacters", VitamConstants.MANIFEST_FILE_NAME_REGEX);
                            manifestFileNameCheck.putParameterValue(LogbookParameterName.eventDetailData,
                                JsonHandler.unprettyPrint(msg));
                        }
                    } catch (final ContentAddressableStorageException e) {
                        LOGGER.error(e);
                        throw new IngestExternalException(e);
                    } catch (ArchiveException | IOException e) {
                        LOGGER.error(e);
                        manifestFileNameCheck.setStatus(StatusCode.FATAL);
                        manifestFileNameCheck.putParameterValue(LogbookParameterName.outcomeDetail,
                            messageLogbookEngineHelper.getOutcomeDetail(MANIFEST_FILE_NAME_CHECK, StatusCode.FATAL));
                        manifestFileNameCheck.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            messageLogbookEngineHelper.getLabelOp(MANIFEST_FILE_NAME_CHECK, StatusCode.FATAL));
                    } catch (ManifestDigestValidationException e) {
                        LOGGER.error(e);
                        manifestFileDigestCheckFailure = LogbookParameterHelper.newLogbookOperationParameters(
                            GUIDFactory.newEventGUID(guid),
                            MANIFEST_DIGEST_CHECK,
                            guid,
                            logbookTypeProcess,
                            StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(MANIFEST_DIGEST_CHECK, StatusCode.KO),
                            guid);
                        manifestFileDigestCheckFailure
                            .putParameterValue(LogbookParameterName.parentEventIdentifier, ingestExtGuid.getId());
                        manifestFileDigestCheckFailure.setStatus(StatusCode.KO);
                        ObjectNode msg = JsonHandler.createObjectNode();
                        msg.put("Error", e.getMessage());
                        manifestFileDigestCheckFailure.putParameterValue(LogbookParameterName.eventDetailData,
                            JsonHandler.unprettyPrint(msg));
                    } finally {
                        StreamUtils.closeSilently(inputStreamTmp);
                    }

                    // update end step param if manifest file name is failed
                    if (manifestFileNameCheck.getStatus().compareTo(endParameters.getStatus()) > 1) {
                        endParameters.setStatus(manifestFileNameCheck.getStatus());
                        endParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                            messageLogbookEngineHelper.getOutcomeDetail(INGEST_EXT, manifestFileNameCheck.getStatus()));
                        endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            messageLogbookEngineHelper.getLabelOp(INGEST_EXT, endParameters.getStatus()));
                    }

                    // write to logbook parent and child events
                    helper.updateDelegate(endParameters);
                    helper.updateDelegate(antivirusParameters);
                    helper.updateDelegate(formatParameters);
                    helper.updateDelegate(manifestFileNameCheck);
                    if (manifestFileNameCheck.getStatus().compareTo(StatusCode.OK) > 0) {
                        logbookAndGenerateATR(preUploadResume, guid, manifestFileNameCheck.getStatus(),
                            isFileInfected, helper, MANIFEST_FILE_NAME_CHECK, "");
                    }

                    if (manifestFileDigestCheckFailure != null) {
                        helper.updateDelegate(manifestFileDigestCheckFailure);
                        logbookAndGenerateATR(preUploadResume, guid, manifestFileDigestCheckFailure.getStatus(),
                            isFileInfected, helper, MANIFEST_DIGEST_CHECK, "");
                    }

                } else {
                    // write to logbook parent and child events
                    helper.updateDelegate(endParameters);
                    helper.updateDelegate(antivirusParameters);
                    helper.updateDelegate(formatParameters);

                    logbookAndGenerateATR(preUploadResume, guid, StatusCode.KO, isFileInfected,
                        helper, CHECK_CONTAINER, ". Format non supporté : " + mimeType);
                }
            } else {
                // finalize end step param
                endParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    messageLogbookEngineHelper.getLabelOp(INGEST_EXT, endParameters.getStatus()));

                // write logbook
                helper.updateDelegate(endParameters);
                helper.updateDelegate(antivirusParameters);

                logbookAndGenerateATR(preUploadResume, guid, antivirusParameters.getStatus(), isFileInfected, helper,
                    SANITY_CHECK_SIP, "");
            }

            try (IngestInternalClient ingestClient = ingestInternalClientFactory.getClient()) {
                ingestClient.uploadInitialLogbook(helper.removeCreateDelegate(guid.getId()));

                if (!isFileInfected && isSupportedMedia && manifestFileName != null &&
                    manifestFileName.isManifestFile()) {

                    ingestClient
                        .upload(inputStream, CommonMediaType.valueOf(mimeType), preUploadResume.getWorkFlow(), xAction);
                    return StatusCode.OK;
                } else {
                    cancelOperation(guid);
                    return StatusCode.KO;
                }
            } catch (ZipFilesNameNotAllowedException e) {
                helper = new LogbookOperationsClientHelper();

                logbookAndGenerateATR(preUploadResume, guid, StatusCode.KO, isFileInfected, helper,
                    INGEST_INT_UPLOAD, "");

                try (IngestInternalClient ingestClient =
                    ingestInternalClientFactory.getClient()) {
                    ingestClient.uploadFinalLogbook(helper.removeUpdateDelegate(guid.getId()));
                } catch (VitamException ex) {
                    throw new IngestExternalException(ex);
                } finally {
                    cancelOperation(guid);
                }

                return StatusCode.KO;
            } catch (WorkspaceClientServerException e) {
                logbookAndGenerateATR(preUploadResume, guid, StatusCode.FATAL, isFileInfected, helper,
                    INGEST_INT_UPLOAD, "");
                try (IngestInternalClient ingestClient =
                    ingestInternalClientFactory.getClient()) {
                    ingestClient.uploadFinalLogbook(helper.removeUpdateDelegate(guid.getId()));
                } catch (VitamException ex) {
                    throw new IngestExternalException(ex);
                }
                return StatusCode.FATAL;
            } catch (final VitamException e) {
                throw new IngestExternalException(e);
            }
        } catch (LogbookClientNotFoundException e) {
            throw new IngestExternalException(e);
        } finally {
            if (workspaceFileSystem != null) {
                try {
                    workspaceFileSystem.deleteObject(guid.getId(), guid.getId());
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
                try {
                    workspaceFileSystem.deleteContainer(guid.getId(), true);
                } catch (final ContentAddressableStorageException e) {
                    LOGGER.warn(e);
                }
            }
        }
    }

    private void cancelOperation(GUID guid) throws IngestExternalException {
        try (IngestInternalClient ingestClient = ingestInternalClientFactory.getClient()) {
            RequestResponse<ItemStatus> requestResponse =
                ingestClient.cancelOperationProcessExecution(guid.getId());

            if (!requestResponse.isOk()) {
                VitamError error = (VitamError) requestResponse;
                throw new IngestExternalException("Error occurs while cancel operation : " + error.getMessage());
            }
        } catch (IngestExternalException e) {
            throw e;
        } catch (
            final Exception e) {
            throw new IngestExternalException(e);
        }

    }

    /**
     * @param preUploadResume
     * @param operationId
     * @param statusCode
     * @param isFileInfected
     * @param helper
     * @param atrEventType
     * @param additionalMessage
     * @throws InvalidGuidOperationException
     * @throws LogbookClientNotFoundException
     */
    private void logbookAndGenerateATR(PreUploadResume preUploadResume, GUID operationId, StatusCode statusCode,
        boolean isFileInfected,
        LogbookOperationsClientHelper helper, String atrEventType, String additionalMessage)
        throws LogbookClientNotFoundException {

        LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(preUploadResume.getWorkFlow().getTypeProc());

        MessageLogbookEngineHelper messageLogbookEngineHelper =
            new MessageLogbookEngineHelper(logbookTypeProcess);

        // Finalisation STARTED event
        String eventType = VitamLogbookMessages.getEventTypeStarted(STP_INGEST_FINALISATION);
        GUID eventId = GUIDFactory.newEventGUID(operationId);
        StatusCode finalisationStatusCode = StatusCode.OK;
        //
        LogbookOperationParameters stpIngestFinalisationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                eventId,
                eventType,
                operationId,
                logbookTypeProcess,
                finalisationStatusCode,
                VitamLogbookMessages.getCodeOp(eventType, finalisationStatusCode),
                operationId);
        helper.updateDelegate(stpIngestFinalisationParameters);


        StatusCode atrStatusCode = StatusCode.OK;
        String atrKo = null;
        try {
            atrKo = AtrKoBuilder.buildAtrKo(operationId.getId(), "ArchivalAgencyToBeDefined",
                    "TransferringAgencyToBeDefined", atrEventType, additionalMessage, statusCode,
                    stpIngestFinalisationParameters.getEventDateTime());
            if (isFileInfected || !statusCode.equals(StatusCode.FATAL)) {
                storeATR(operationId, atrKo);
            }

        } catch (IngestExternalException e) {
            LOGGER.error(e);
            atrStatusCode = StatusCode.KO;
        }

        LOGGER.warn("ATR KO created : " + atrKo);


        eventType = STP_INGEST_FINALISATION;
        GUID finalisationEventId = GUIDFactory.newEventGUID(operationId);
        String outComeDetailMessage = VitamLogbookMessages.getCodeOp(eventType, finalisationStatusCode);

        if (statusCode.equals(StatusCode.FATAL) && !isFileInfected) {
            finalisationStatusCode = statusCode;
            outComeDetailMessage = WORKSPACE_ERROR_MESSAGE;
        }
        stpIngestFinalisationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                finalisationEventId,
                eventType,
                operationId,
                logbookTypeProcess,
                finalisationStatusCode,
                outComeDetailMessage,
                operationId);


        final LogbookOperationParameters transferNotificationParameters =
            getAtrNotificationEvent(operationId, logbookTypeProcess, atrStatusCode, finalisationEventId);

        if (!StatusCode.OK.equals(atrStatusCode)) {
            // Erase information of finalisation event if atrStatusCode is not OK
            // Because parent event should have the correct status if atr fail
            stpIngestFinalisationParameters.setStatus(atrStatusCode);
            stpIngestFinalisationParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(eventType, atrStatusCode));
            stpIngestFinalisationParameters
                .putParameterValue(LogbookParameterName.outcomeDetail, getOutcomeDetail(eventType, atrStatusCode));
        }

        helper.updateDelegate(stpIngestFinalisationParameters);
        helper.updateDelegate(transferNotificationParameters);

        preUploadResume.getStartedParameters().putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(operationId).getId());
        preUploadResume.getStartedParameters().setStatus(statusCode);
        preUploadResume.getStartedParameters().putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(preUploadResume.getWorkFlow().getIdentifier(), statusCode));
        preUploadResume.getStartedParameters().putParameterValue(LogbookParameterName.outcomeDetailMessage,
            messageLogbookEngineHelper.getLabelOp(preUploadResume.getWorkFlow().getIdentifier(), statusCode));
        // update PROCESS_SIP
        helper.updateDelegate(preUploadResume.getStartedParameters());
    }


    /**
     * Store ATR in the workspace
     *
     * @param ingestGuid
     * @param atrKo
     * @throws IngestExternalException
     */
    private void storeATR(GUID ingestGuid, String atrKo) throws IngestExternalException {
        try (IngestInternalClient client = ingestInternalClientFactory.getClient()) {
            client.storeATR(ingestGuid, new ByteArrayInputStream(atrKo.getBytes(CharsetUtils.UTF8)));
        } catch (VitamClientException e) {
            throw new IngestExternalException(e);
        }
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

    /**
     * This method is called when a workspace exception occurs
     *
     * @param workflowIdentifier
     * @param typeProcess
     * @param operationId
     * @param asyncResponse
     * @throws VitamException
     */
    public void createATRFatalWorkspace(String workflowIdentifier, String typeProcess, GUID operationId,
        AsyncResponse asyncResponse)
        throws VitamException {
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(typeProcess);
        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

        LogbookOperationParameters startedParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            workflowIdentifier,
            operationId,
            logbookTypeProcess,
            StatusCode.STARTED,
            messageLogbookEngineHelper.getLabelOp(workflowIdentifier, StatusCode.STARTED) + " : " +
                operationId.getId(),
            operationId);
        helper.createDelegate(startedParameters);


        // Finalisation STARTED event
        String eventType = VitamLogbookMessages.getEventTypeStarted(STP_INGEST_FINALISATION);
        GUID eventId = GUIDReader.getGUID(operationId.getId());
        //
        LogbookOperationParameters stpIngestFinalisationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                eventId,
                eventType,
                operationId,
                logbookTypeProcess,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK),
                operationId);
        helper.updateDelegate(stpIngestFinalisationParameters);


        // As problem is with workspace, then atr can't be saved
        eventType = STP_INGEST_FINALISATION;
        GUID finalisationEventId = GUIDReader.getGUID(operationId.getId());
        stpIngestFinalisationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                finalisationEventId,
                eventType,
                operationId,
                logbookTypeProcess,
                StatusCode.FATAL,
                WORKSPACE_ERROR_MESSAGE,
                operationId);

        final LogbookOperationParameters transferNotificationParameters =
            getAtrNotificationEvent(operationId, logbookTypeProcess, StatusCode.OK, finalisationEventId);


        helper.updateDelegate(stpIngestFinalisationParameters);
        helper.updateDelegate(transferNotificationParameters);


        startedParameters.setStatus(StatusCode.FATAL);
        startedParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            messageLogbookEngineHelper.getOutcomeDetail(workflowIdentifier, StatusCode.FATAL));
        startedParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            messageLogbookEngineHelper.getLabelOp(workflowIdentifier, StatusCode.FATAL));

        helper.updateDelegate(startedParameters);


        try (IngestInternalClient ingestClient =
            ingestInternalClientFactory.getClient()) {
            ingestClient.uploadInitialLogbook(helper.removeCreateDelegate(operationId.getId()));
        } finally {
            String atr = AtrKoBuilder.buildAtrKo(operationId.getId(), "ArchivalAgencyToBeDefined",
                "TransferringAgencyToBeDefined",
                INGEST_INT_UPLOAD, null, StatusCode.FATAL, stpIngestFinalisationParameters.getEventDateTime());

            handleResponseWithATR(operationId, asyncResponse, atr);
        }
    }

    public void handleResponseWithATR(GUID operationId, AsyncResponse asyncResponse, String entity) {


        AsyncInputStreamHelper responseHelper =
            new AsyncInputStreamHelper(asyncResponse, new ByteArrayInputStream(entity.getBytes(CharsetUtils.UTF8)));
        final ResponseBuilder responseBuilder =
            Response.status(Status.SERVICE_UNAVAILABLE).type(MediaType.APPLICATION_OCTET_STREAM)
                .header(GlobalDataRest.X_REQUEST_ID, operationId.getId())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL);
        responseHelper.writeResponse(responseBuilder);
    }

    private LogbookOperationParameters getAtrNotificationEvent(GUID operationId, LogbookTypeProcess logbookTypeProcess,
        StatusCode statusCode, GUID finalisationEventId) {
        GUID atrEventId = GUIDFactory.newEventGUID(operationId);
        final LogbookOperationParameters event =
            LogbookParameterHelper.newLogbookOperationParameters(
                atrEventId,
                ATR_NOTIFICATION,
                operationId,
                logbookTypeProcess,
                statusCode,
                VitamLogbookMessages.getCodeOp(ATR_NOTIFICATION, statusCode),
                operationId);
        event.putParameterValue(LogbookParameterName.parentEventIdentifier, finalisationEventId.getId());
        return event;
    }

    private ManifestFileName checkManifestFile(InputStream in, String mimeType,
        String manifestDigestAlgo, String manifestDigestValue)
        throws IOException, ArchiveException, ManifestDigestValidationException {

        ManifestFileName manifestFileName = new ManifestFileName();
        try (final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
            .createArchiveInputStream(CommonMediaType.valueOf(mimeType), in)) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                LOGGER.debug("SIP Files : " + entry.getName());
                if (archiveInputStream.canReadEntryData(entry)) {
                    if (!entry.isDirectory() && entry.getName().split("/").length == 1) {
                        manifestFileName.setFileName(entry.getName());
                        if (entry.getName().matches(VitamConstants.MANIFEST_FILE_NAME_REGEX)) {
                            manifestFileName.setManifestFile(true);


                            manifestDigestValidator.checkManifestDigest(archiveInputStream, manifestDigestAlgo, manifestDigestValue);

                            break;
                        }
                    }
                }
            }
        }
        return manifestFileName;
    }
}
