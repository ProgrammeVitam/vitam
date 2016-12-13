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
package fr.gouv.vitam.ingest.internal.upload.rest;

import java.io.InputStream;
import java.util.Queue;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnauthorizeException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * IngestInternalResource implements UploadService
 *
 */
@Path("/ingest/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestInternalResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResource.class);

    private static final String FOLDER_SIP = "SIP";
    private static final String INGEST_INT_UPLOAD = "STP_UPLOAD_SIP";
    private static final String INGEST_WORKFLOW = "PROCESS_SIP_UNITARY";
    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";
    private static final String XML = ".xml";

    private final WorkspaceClient workspaceClientMock;
    private final ProcessingManagementClient processingManagementClientMock;

    /**
     * IngestInternalResource constructor
     *
     * @param configuration ingest configuration
     *
     */
    public IngestInternalResource(IngestInternalConfiguration configuration) {
        WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getProcessingUrl());
        workspaceClientMock = null;
        processingManagementClientMock = null;
    }

    /**
     * IngestInternalResource constructor for tests
     *
     * @param workspaceClient workspace client instance
     * @param processingManagementClient processing management client instance
     *
     */
    IngestInternalResource(WorkspaceClient workspaceClient, ProcessingManagementClient processingManagementClient) {
        workspaceClientMock = workspaceClient;
        processingManagementClientMock = processingManagementClient;
    }

    /**
     * Allow to create a logbook by delegation
     *
     * @param queue list of LogbookOperationParameters, first being the created master
     * @return the status of the request (CREATED meaning OK)
     */
    @POST
    @Path("/logbooks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delegateCreateLogbookOperation(Queue<LogbookOperationParameters> queue) {
        try (LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().checkValidRequestId();
            ParametersChecker.checkParameter("list is a Mandatory parameter", queue);
            client.bulkCreate(VitamThreadUtils.getVitamSession().getRequestId(), queue);
            return Response.status(Status.CREATED).build();
        } catch (IllegalArgumentException | LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(e.getMessage()).build();
        } catch (final LogbookClientServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Allow to update a logbook by delegation
     *
     * @param queue list of LogbookOperationParameters in append mode (created already done before)
     * @return the status of the request (OK)
     */
    @PUT
    @Path("/logbooks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delegateUpdateLogbookOperation(Queue<LogbookOperationParameters> queue) {
        try (LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().checkValidRequestId();
            ParametersChecker.checkParameter("list is a Mandatory parameter", queue);
            client.bulkUpdate(VitamThreadUtils.getVitamSession().getRequestId(), queue);
            return Response.status(Status.OK).build();
        } catch (IllegalArgumentException | LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (final LogbookClientServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Upload compressed SIP as Stream, will be uncompressed in workspace.</br>
     * </br>
     * Will return {@link Response} containing an InputStream for the ArchiveTransferReply (OK or KO) except in
     * INTERNAL_ERROR (no body)
     *
     * @param contentType the header Content-Type (zip, tar, ...)
     * @param uploadedInputStream the stream to upload
     * @param asyncResponse
     *
     */
    @POST
    @Path("/ingests")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR,
        CommonMediaType.BZIP2})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void uploadSipAsStream(@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
        InputStream uploadedInputStream,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> ingestAsync(asyncResponse, contentType, uploadedInputStream));
    }

    private void ingestAsync(final AsyncResponse asyncResponse, String contentType,
        InputStream uploadedInputStream) {
        LogbookOperationParameters parameters = null;
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {

            try {
                VitamThreadUtils.getVitamSession().checkValidRequestId();
                ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);

                final GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());

                parameters = logbookInitialisation(containerGUID, containerGUID);


                if (contentType == null) {
                    throw new IngestInternalException("mimeType null");
                }

                final MediaType mediaType = CommonMediaType.valueOf(contentType);
                final String archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);

                // Save sip file
                LOGGER.debug("Starting up the save file sip");
                // workspace
                parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
                callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.STARTED));
                try {
                    // push uploaded sip as stream
                    pushSipStreamToWorkspace(containerGUID.getId(), archiveMimeType,
                        uploadedInputStream,
                        parameters);
                    final String uploadSIPMsg = VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.OK);

                    callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.OK, uploadSIPMsg);
                    // processing
                    parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
                    final ItemStatus processingOk =
                        callProcessingEngine(parameters, logbookOperationsClient, containerGUID.getId());
                    try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                        final Response response =
                            storageClient.getContainerAsync(DEFAULT_TENANT, DEFAULT_STRATEGY,
                                containerGUID.getId() + XML,
                                StorageCollectionType.REPORTS);
                        final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
                        Status finalStatus = Status.OK;
                        if (!StatusCode.OK.equals(processingOk.getGlobalStatus())) {
                            if (StatusCode.WARNING.equals(processingOk.getGlobalStatus())) {
                                finalStatus = Status.PARTIAL_CONTENT;
                            } else {
                                finalStatus = Status.BAD_REQUEST;
                            }
                        }

                        helper.writeResponse(Response.status(finalStatus));
                    }
                } finally {
                    cleanWorkspace(containerGUID.getId());
                }
            } catch (final ContentAddressableStorageCompressedFileException e) {
                if (parameters != null) {
                    try {
                        final String errorMsg = VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.KO);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO, errorMsg);
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.KO));
                    } catch (final LogbookClientException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            } catch (final ContentAddressableStorageException e) {
                if (parameters != null) {
                    try {
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO, "error workspace");
                    } catch (final LogbookClientException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
                // FIXME P1 in particular Processing Exception could it be a "normal error" ?
                // Have to determine here if it is an internal error and FATAL result or processing error, so business
                // error and KO result
            } catch (final ProcessingException |
                LogbookClientException | StorageClientException | StorageNotFoundException |
                InvalidGuidOperationException e) {
                if (parameters != null) {
                    try {
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);

                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, StatusCode.KO));

                    } catch (final LogbookClientException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            } catch (final IngestInternalException e) {
                // if an IngestInternalException is thrown, that means logbook has already been updated (with a fatal
                // State)
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            } finally {
                if (logbookOperationsClient != null) {
                    logbookOperationsClient.close();
                }
                StreamUtils.closeSilently(uploadedInputStream);
            }
        }
    }

    private LogbookOperationParameters logbookInitialisation(final GUID ingestGuid, final GUID containerGUID)
        throws LogbookClientNotFoundException,
        LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException {

        return LogbookParametersFactory.newLogbookOperationParameters(
            ingestGuid, INGEST_INT_UPLOAD, containerGUID,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestGuid != null ? ingestGuid.toString() : "outcomeDetailMessage",
            ingestGuid);
    }

    private void callLogbookUpdate(LogbookOperationsClient client, LogbookOperationParameters parameters,
        StatusCode logbookOutcome, String outcomeDetailMessage)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {

        parameters.setStatus(logbookOutcome);
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
        client.update(parameters);
    }

    /**
     *
     * @param containerName
     * @param uploadedInputStream
     * @param archiveMimeType
     * @throws ContentAddressableStorageException
     */
    private void pushSipStreamToWorkspace(final String containerName,
        final String archiveMimeType,
        final InputStream uploadedInputStream, final LogbookOperationParameters parameters)
        throws ContentAddressableStorageException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageCompressedFileException, ContentAddressableStorageServerException {


        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to push stream to workspace...");
        LOGGER.debug("Try to push stream to workspace...");

        // call workspace
        WorkspaceClient workspaceClient = workspaceClientMock;
        try {
            if (workspaceClient == null) {
                workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            }
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.uncompressObject(containerName, FOLDER_SIP, archiveMimeType, uploadedInputStream);
            } else {
                throw new ContentAddressableStorageAlreadyExistException(containerName + "already exist");
            }
        } finally {
            if (workspaceClientMock == null && workspaceClient != null) {
                workspaceClient.close();
            }
        }

        LOGGER.debug(" -> push stream to workspace finished");
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "-> push stream to workspace finished");
    }

    private void cleanWorkspace(final String containerName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        // call workspace
        WorkspaceClient workspaceClient = workspaceClientMock;
        try {
            if (workspaceClient == null) {
                workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            }
            if (workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.deleteContainer(containerName, true);
            }
        } finally {
            if (workspaceClientMock == null && workspaceClient != null) {
                workspaceClient.close();
            }
        }
    }

    private ItemStatus callProcessingEngine(final LogbookOperationParameters parameters,
        final LogbookOperationsClient client,
        final String containerName) throws IngestInternalException, ProcessingException,
        LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to call processing...");
        final String workflowId = "DefaultIngestWorkflow";
        ProcessingManagementClient processingClient = processingManagementClientMock;
        try {
            if (processingClient == null) {
                processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            }
            final ItemStatus itemStatus = processingClient.executeVitamProcess(containerName, workflowId);

            callLogbookUpdate(client, parameters, itemStatus.getGlobalStatus(),
                VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, itemStatus.getGlobalStatus()));

            return itemStatus;
        } catch (WorkflowNotFoundException | ProcessingInternalServerException | IllegalArgumentException |
            ProcessingBadRequestException | ProcessingUnauthorizeException exc) {
            LOGGER.error(exc);
            callLogbookUpdate(client, parameters, StatusCode.FATAL,
                VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, StatusCode.FATAL));
            throw new IngestInternalException(exc);
        } finally {
            if (processingManagementClientMock == null && processingClient != null) {
                processingClient.close();
            }
        }

    }

}
