/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
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
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.common.WorkspaceFileSystem;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;

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

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SANITY_CHECK_RESULT_FILE;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_ANTIVIRUS;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_DIGEST_MANIFEST;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_FILENAME_MANIFEST;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_FORMAT;

/**
 * Implementation of IngestExtern
 */
public class IngestExternalImpl implements IngestExternal {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalImpl.class);
    public static final String INGEST_INT_UPLOAD = "STP_UPLOAD_SIP";

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

    public IngestExternalImpl(
        IngestExternalConfiguration config,
        FormatIdentifierFactory formatIdentifierFactory,
        IngestInternalClientFactory ingestInternalClientFactory,
        ManifestDigestValidator manifestDigestValidator
    ) {
        this.config = config;
        this.formatIdentifierFactory = formatIdentifierFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
        this.manifestDigestValidator = manifestDigestValidator;
    }

    @Override
    public PreUploadResume preUploadAndResume(
        InputStream input,
        String workflowIdentifier,
        GUID guid,
        String xAction,
        AsyncResponse asyncResponse
    ) throws IngestExternalException, VitamClientException {
        ParametersChecker.checkParameter("input is a mandatory parameter", input);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        WorkspaceFileSystem workspaceFileSystem;
        WorkFlow workflow;
        try (IngestInternalClient ingestClient = ingestInternalClientFactory.getClient()) {
            // Load workflow information from processing
            Optional<WorkFlow> optional = ingestClient.getWorkflowDetails(workflowIdentifier);
            if (optional.isEmpty()) {
                throw new WorkflowNotFoundException("Workflow " + workflowIdentifier + " not found");
            }
            workflow = optional.get();

            LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(workflow.getTypeProc());
            MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);

            LogbookOperationParameters startedParameters = LogbookParameterHelper.newLogbookOperationParameters(
                guid,
                workflow.getIdentifier(),
                guid,
                logbookTypeProcess,
                StatusCode.STARTED,
                messageLogbookEngineHelper.getLabelOp(workflow.getIdentifier(), StatusCode.STARTED) +
                " : " +
                guid.toString(),
                guid
            );

            startedParameters.getMapParameters().put(LogbookParameterName.objectIdentifierIncome, guid.getId());
            helper.createDelegate(startedParameters);

            try {
                LOGGER.debug("Initialize Workflow operation (" + guid + ") ... ");
                ingestClient.initWorkflow(workflow);
                ingestClient.uploadInitialLogbook(helper.removeCreateDelegate(guid.getId()));
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
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.ACCEPTED)
                    .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.PAUSE)
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.UNKNOWN)
                    .build(),
                input
            );
            // we run the workflow
            ingestClient.updateOperationActionProcess(xAction, guid.getId());

            return new PreUploadResume(workflow, workspaceFileSystem);
        } catch (LogbookClientAlreadyExistsException ex) {
            throw new IngestExternalException(ex);
        } catch (IOException ex) {
            LOGGER.error("Cannot load WorkspaceFileSystem ", ex);
            throw new IllegalStateException(ex);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(CAN_NOT_STORE_FILE, e);
            throw new IngestExternalException(e);
        }
    }

    @Override
    public StatusCode upload(
        PreUploadResume preUploadResume,
        String xAction,
        GUID guid,
        String manifestDigestValue,
        String manifestDigestAlgo
    ) throws IngestExternalException {
        WorkspaceFileSystem workspaceFileSystem = preUploadResume.getWorkspaceFileSystem();
        try {
            final String antiVirusScriptName = config.getAntiVirusScriptName();
            final long timeoutScanDelay = config.getTimeoutScanDelay();
            final String containerNamePath = guid.getId();
            final String objectNamePath = guid.getId();
            final File file;
            try {
                file = SafeFileChecker.checkSafeFilePath(config.getPath(), containerNamePath, objectNamePath);
            } catch (IllegalPathException e) {
                String filePath = config.getPath() + "/" + containerNamePath + "/" + objectNamePath;
                throw new IngestExternalException("File path " + filePath + " is invalid", e);
            }
            if (!file.canRead()) {
                LOGGER.error(CAN_NOT_READ_FILE);
                throw new IngestExternalException(CAN_NOT_READ_FILE);
            }
            ExecutionOutput executionOutput;

            // CHECK ANTIVIRUS
            ItemStatus antivirusItemStatus = new ItemStatus(CHECK_ANTIVIRUS.getItemValue());
            try {
                /*
                 * Return values of script scan-clamav.sh return 0: scan OK - no virus 1: virus found and corrected 2:
                 * virus found but not corrected 3: Fatal scan not performed
                 */
                executionOutput = JavaExecuteScript.executeCommand(
                    antiVirusScriptName,
                    file.getAbsolutePath(),
                    timeoutScanDelay
                );
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
                    antivirusItemStatus.increment(OK);
                    break;
                case STATUS_ANTIVIRUS_WARNING:
                case STATUS_ANTIVIRUS_KO:
                    LOGGER.error(IngestExternalOutcomeMessage.KO_VIRUS.toString());
                    antivirusItemStatus.increment(KO);
                    isFileInfected = true;
                    break;
                case STATUS_ANTIVIRUS_NOT_PERFORMED:
                case STATUS_ANTIVIRUS_EXCEPTION_OCCURRED:
                    LOGGER.error(
                        "{},{},{}",
                        IngestExternalOutcomeMessage.FATAL_VIRUS.toString(),
                        executionOutput.getStdout(),
                        executionOutput.getStderr()
                    );
                    antivirusItemStatus.increment(FATAL);
                    isFileInfected = true;
                    break;
                default:
                    break;
            }

            final ObjectNode itemStatusFromExternal = JsonHandler.createObjectNode();
            itemStatusFromExternal.set(CHECK_ANTIVIRUS.getItemParam(), JsonHandler.toJsonNode(antivirusItemStatus));

            if (!isFileInfected) {
                // CHECK FILE FORMAT
                ItemStatus fileFormatItemStatus = new ItemStatus(CHECK_FORMAT.getItemValue());

                // instantiate SiegFried final
                try {
                    final FormatIdentifier formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(
                        FORMAT_IDENTIFIER_ID
                    );
                    LOGGER.debug(BEGIN_SIEG_FRIED_FORMAT_IDENTIFICATION);

                    // call siegFried
                    final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(file.toPath());
                    FormatIdentifierResponse format = getFirstPronomFormat(formats);
                    if (format == null) {
                        fileFormatItemStatus.increment(KO);
                    } else {
                        LOGGER.debug(SIP_FORMAT + format.getMimetype());
                        mimeType = format.getMimetype();
                        if (CommonMediaType.isSupportedFormat(format.getMimetype())) {
                            isSupportedMedia = true;
                            fileFormatItemStatus.increment(OK);
                        } else {
                            LOGGER.error(SIP_WRONG_FORMAT + format.getMimetype() + IS_NOT_SUPPORTED);
                            fileFormatItemStatus.increment(KO);
                        }
                    }
                } catch (
                    final FormatIdentifierNotFoundException
                    | FormatIdentifierBadRequestException
                    | FileFormatNotFoundException
                    | FormatIdentifierTechnicalException
                    | FormatIdentifierFactoryException e
                ) {
                    LOGGER.error(e);
                    fileFormatItemStatus.increment(FATAL);
                }
                itemStatusFromExternal.set(CHECK_FORMAT.getItemParam(), JsonHandler.toJsonNode(fileFormatItemStatus));

                InputStream inputStreamTmp = null;
                if (isSupportedMedia) {
                    // CHECK MANIFEST
                    ItemStatus manifestFileNameItemStatus = new ItemStatus(CHECK_FILENAME_MANIFEST.getItemValue());
                    ItemStatus manifestDigestItemStatus = new ItemStatus(CHECK_DIGEST_MANIFEST.getItemValue());
                    try {
                        // check manifest file name by regex
                        inputStreamTmp = workspaceFileSystem
                            .getObject(guid.getId(), guid.getId(), null, null)
                            .readEntity(InputStream.class);
                        manifestFileName = checkManifestFile(
                            inputStreamTmp,
                            mimeType,
                            manifestDigestAlgo,
                            manifestDigestValue
                        );
                        if (manifestFileName.isManifestFile()) {
                            inputStream = (InputStream) workspaceFileSystem
                                .getObject(guid.getId(), guid.getId(), null, null)
                                .getEntity();
                            manifestFileNameItemStatus.increment(OK);
                        } else {
                            LOGGER.error("Nom du fichier manifest n'est pas conforme");
                            manifestFileNameItemStatus.increment(KO);
                            ObjectNode msg = JsonHandler.createObjectNode();
                            msg.put("FileName", manifestFileName.getFileName());
                            msg.put("AllowedCharacters", VitamConstants.MANIFEST_FILE_NAME_REGEX);
                            manifestFileNameItemStatus.setEvDetailData((JsonHandler.unprettyPrint(msg)));
                        }
                    } catch (final ContentAddressableStorageException e) {
                        LOGGER.error(e);
                        throw new IngestExternalException(e);
                    } catch (ArchiveException | IOException e) {
                        LOGGER.error(e);
                        manifestFileNameItemStatus.increment(FATAL);
                    } catch (ManifestDigestValidationException e) {
                        LOGGER.error(e);
                        manifestDigestItemStatus.increment(KO);
                        ObjectNode msg = JsonHandler.createObjectNode();
                        msg.put("Error", e.getMessage());
                        manifestDigestItemStatus.setEvDetailData(JsonHandler.unprettyPrint(msg));
                    } finally {
                        StreamUtils.closeSilently(inputStreamTmp);
                    }

                    itemStatusFromExternal.set(
                        CHECK_FILENAME_MANIFEST.getItemParam(),
                        JsonHandler.toJsonNode(manifestFileNameItemStatus)
                    );
                    itemStatusFromExternal.set(
                        CHECK_DIGEST_MANIFEST.getItemParam(),
                        JsonHandler.toJsonNode(manifestDigestItemStatus)
                    );
                }
            }

            try (IngestInternalClient ingestClient = ingestInternalClientFactory.getClient()) {
                ingestClient.saveObjectToWorkspace(
                    guid.getId(),
                    SANITY_CHECK_RESULT_FILE,
                    JsonHandler.writeToInpustream(itemStatusFromExternal)
                );
                if (
                    !isFileInfected && isSupportedMedia && manifestFileName != null && manifestFileName.isManifestFile()
                ) {
                    ingestClient.upload(
                        inputStream,
                        CommonMediaType.valueOf(mimeType),
                        preUploadResume.getWorkFlow(),
                        xAction
                    );
                    return OK;
                } else {
                    return KO;
                }
            } catch (final VitamException e) {
                throw new IngestExternalException(e);
            }
        } catch (InvalidParseOperationException e) {
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

    public void handleResponseWithATR(GUID operationId, AsyncResponse asyncResponse, String entity) {
        AsyncInputStreamHelper responseHelper = new AsyncInputStreamHelper(
            asyncResponse,
            new ByteArrayInputStream(entity.getBytes(CharsetUtils.UTF8))
        );
        final ResponseBuilder responseBuilder = Response.status(Status.SERVICE_UNAVAILABLE)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_REQUEST_ID, operationId.getId())
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL);
        responseHelper.writeResponse(responseBuilder);
    }

    private ManifestFileName checkManifestFile(
        InputStream in,
        String mimeType,
        String manifestDigestAlgo,
        String manifestDigestValue
    ) throws IOException, ArchiveException, ManifestDigestValidationException {
        ManifestFileName manifestFileName = new ManifestFileName();
        try (
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(CommonMediaType.valueOf(mimeType), in)
        ) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                LOGGER.debug("SIP Files : " + entry.getName());
                if (archiveInputStream.canReadEntryData(entry)) {
                    if (!entry.isDirectory() && entry.getName().split("/").length == 1) {
                        manifestFileName.setFileName(entry.getName());
                        if (entry.getName().matches(VitamConstants.MANIFEST_FILE_NAME_REGEX)) {
                            manifestFileName.setManifestFile(true);

                            manifestDigestValidator.checkManifestDigest(
                                archiveInputStream,
                                manifestDigestAlgo,
                                manifestDigestValue
                            );

                            break;
                        }
                    }
                }
            }
        }
        return manifestFileName;
    }
}
