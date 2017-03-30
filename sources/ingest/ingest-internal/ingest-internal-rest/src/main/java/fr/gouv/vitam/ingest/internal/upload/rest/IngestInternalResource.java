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

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.common.exception.ContextNotFoundException;
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
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
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
    private static final String DEFAULT_STRATEGY = "default";
    private static final String XML = ".xml";
    private static final String FOLDERNAME = "ATR/";
    private static final String PROCESS_CONTEXT_FILE = "processContext.json";
    private static final String EXECUTION_CONTEXT = "executionContext";
    private static final String WORKFLOW_ID = "workFlowId";
    private static final String LOGBOOK_TYPE_PROCESS = "logbookTypeProcess";

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
     * @param contextId the header X-Context-Id (steptoStep or not)
     * @param actionId the header X-ACTION (next,resume,..)
     * @param uploadedInputStream the stream to upload
     * @param asyncResponse the asynchronized response
     * @throws InternalServerException if request resources server exception occurred
     * @throws VitamClientException if the server is unreachable
     *
     */
    @POST
    @Path("/ingests")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR,
        CommonMediaType.BZIP2})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void uploadSipAsStream(@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
        @HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String actionId,
        InputStream uploadedInputStream,
        @Suspended final AsyncResponse asyncResponse) {
        ParametersChecker.checkParameter("Action Id Request must not be null",
            actionId);

        ParametersChecker.checkParameter("context Id Request must not be null",
            contextId);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(
                () -> ingestAsync(asyncResponse, contentType, uploadedInputStream, contextId, actionId));
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @param asyncResponse asyncResponse
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse) {
        Status status;

        ParametersChecker.checkParameter("Action Id Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));
        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);

        try {
            GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            VitamThreadPoolExecutor.getDefaultExecutor()
                .execute(
                    () -> executeAction(asyncResponse, xAction,
                        containerGUID));

        } catch (InvalidGuidOperationException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Execute the process of an operation related to the id.
     *
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @param uploadedInputStream input stream to upload
     * @return http response
     * @throws InternalServerException if request resources server exception
     * @throws VitamClientException if the server is unreachable 
     * @throws IngestInternalException if error when request to ingest internal server
     * @throws InvalidGuidOperationException if error when create guid
     * @throws ProcessingException if error in workflow execution  
     */
    @Path("/operations/{id}")
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR,
        CommonMediaType.BZIP2})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
        InputStream uploadedInputStream) {
        Status status;
        ItemStatus resp = null;
        MediaType mediaType;
        String archiveMimeType = null;
        LogbookOperationParameters parameters = null;

        ParametersChecker.checkParameter("Action Id Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));
        ParametersChecker.checkParameter("content Type Request must not be null",
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE));
        ParametersChecker.checkParameter("context Id Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID));


        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().checkValidRequestId();
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
            final String contentType = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).get(0);
            final String contextId = headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID).get(0);
            ProcessContext process = createProcessContextObject(PROCESS_CONTEXT_FILE, contextId);

            final GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            boolean isInitMode = ProcessAction.INIT.getValue().equalsIgnoreCase(xAction);

            boolean isStartMode =
                ProcessAction.START.getValue().equalsIgnoreCase(xAction);

            if (isInitMode) {
                try (ProcessingManagementClient processManagementClient =
                    ProcessingManagementClientFactory.getInstance().getClient()) {
                    processManagementClient.initVitamProcess(contextId, containerGUID.getId(), process.getWorkFlowId());
                } catch (VitamClientException e) {
                    LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                    status = Status.INTERNAL_SERVER_ERROR;
                    return Response.status(status)
                        .entity(getErrorEntity(status))
                        .build();
                } catch (InternalServerException e) {
                    LOGGER.error(e);
                    status = Status.INTERNAL_SERVER_ERROR;
                    return Response.status(status)
                        .entity(getErrorEntity(status))
                        .build();
                } catch (BadRequestException e) {
                    LOGGER.error(e);
                    status = Status.BAD_REQUEST;
                    return Response.status(status)
                        .entity(getErrorEntity(status))
                        .build();
                }
            } else {
                if (isStartMode) {
                    parameters = logbookInitialisation(containerGUID, containerGUID, LogbookTypeProcess.INGEST);
                    if (contentType == null) {
                        throw new IngestInternalException("mimeType null");
                    }
                    mediaType = CommonMediaType.valueOf(contentType);
                    archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);

                    prepareToStartProcess(uploadedInputStream, parameters, archiveMimeType, logbookOperationsClient,
                        containerGUID);
                }
            }
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error("unable to create container", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();

        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error("unable to create container", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (LogbookClientNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (LogbookClientServerException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error("unable to create container", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (ContentAddressableStorageCompressedFileException e) {
            LOGGER.error("unable to create container", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("unable to create container", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (IngestInternalException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (InvalidGuidOperationException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }

        return Response.status(Status.OK).entity(resp).build();

    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext("ingest")
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }

    /**
     * get the operation status
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        Status status;
        ItemStatus itemStatus = null;
        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            itemStatus = processManagementClient.getOperationProcessStatus(id);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NO_CONTENT;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();

        } catch (InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        return Response.status(Status.OK).entity(itemStatus).build();
    }

    /**
     * get the workflow status
     *
     * @param id operation identifier
     * @param query body
     * @return http response
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowStatus(@PathParam("id") String id, JsonNode query) {
        Status status;
        ItemStatus itemStatus = null;
        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            itemStatus = processManagementClient.getOperationProcessExecutionDetails(id, query);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NO_CONTENT;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();

        } catch (InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (Exception e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        return Response.status(Status.OK).entity(itemStatus).build();
    }



    /**
     * Interrupt the process of an operation identified by Id.
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {
        Status status;
        Response response = null;
        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            response = processManagementClient.cancelOperationProcessExecution(id);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }

        return response;
    }



    /**
     * Download object stored by Ingest operation (currently ATR and manifest)
     * 
     * Return the object as stream asynchronously
     * 
     * @param objectId the object id
     * @param type the collection type
     * @param asyncResponse the asynchronized response 
     */
    @GET
    @Path("/ingests/{objectId}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadObjectAsStream(@PathParam("objectId") String objectId, @PathParam("type") String type,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> downloadObjectAsync(asyncResponse, objectId, type));
    }

    /**
     * @param guid the object guid
     * @param atr the inputstream ATR
     * @return the status of the request (OK)
     */
    @POST
    @Path("/ingests/{objectId}/report")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response storeATR(@PathParam("objectId") String guid, InputStream atr) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient();
                WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {

            LOGGER.error("storage atr internal");
            workspaceClient.createContainer(guid);
            workspaceClient.putObject(guid, FOLDERNAME + guid + XML, atr);

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(guid);
            description.setWorkspaceObjectURI(FOLDERNAME + guid + XML);
            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY,
                StorageCollectionType.REPORTS, guid + XML, description);
            return Response.status(Status.OK).build();

        } catch (StorageClientException | ContentAddressableStorageServerException |
            ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private void downloadObjectAsync(final AsyncResponse asyncResponse, String objectId,
        String type) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            StorageCollectionType documentType = StorageCollectionType.valueOf(type.toUpperCase());
            if (documentType == StorageCollectionType.MANIFESTS || documentType == StorageCollectionType.REPORTS) {
                objectId += XML;
            } else {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.METHOD_NOT_ALLOWED).build());
                return;
            }
            final Response response = storageClient.getContainerAsync(DEFAULT_STRATEGY,
                objectId, documentType);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            helper.writeResponse(Response.status(Status.OK));
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
        } catch (StorageNotFoundException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.NOT_FOUND).build());
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage error was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    private void ingestAsync(final AsyncResponse asyncResponse, String contentType,
        InputStream uploadedInputStream, String contextId, String actionId) {

        LogbookOperationParameters parameters = null;
        MediaType mediaType;
        String archiveMimeType = null;
        boolean isCompletedProcess = false;

        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {

            try {
                VitamThreadUtils.getVitamSession().checkValidRequestId();
                ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
                ParametersChecker.checkParameter("actionId is a mandatory parameter", actionId);
                ParametersChecker.checkParameter("contextId is a mandatory parameter", contextId);

                final GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
                ProcessContext process = createProcessContextObject(PROCESS_CONTEXT_FILE, contextId);
                if (process == null) {
                    throw new IngestInternalException("Processing Context not found");
                }

                LogbookTypeProcess logbookTypeProcess = process.getLogbookTypeProcess();
                ParametersChecker.checkParameter("logbookTypeProcess is a mandatory parameter", logbookTypeProcess);

                boolean isInitMode =
                    ProcessAction.INIT.equals(ProcessAction.valueOf(actionId));
                boolean isStartMode =
                    ProcessAction.START.equals(ProcessAction.valueOf(actionId));


                if (isInitMode) {
                    try (ProcessingManagementClient processManagementClient =
                        ProcessingManagementClientFactory.getInstance().getClient()) {

                        // Initialize a new process
                        processManagementClient.initVitamProcess(logbookTypeProcess.toString(), containerGUID.getId(),
                            process.getWorkFlowId());

                        // Successful initialization
                        AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                            Response.status(Status.ACCEPTED).build());
                    }
                } else {

                    // Start process
                    if (isStartMode) {
                        // Get Execution Mode from processContext.json file
                        actionId = process.getExecutionContext();

                        // Get MimeType
                        mediaType = CommonMediaType.valueOf(contentType);
                        archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);

                        parameters = logbookInitialisation(containerGUID, containerGUID, logbookTypeProcess);
                        prepareToStartProcess(uploadedInputStream, parameters, archiveMimeType, logbookOperationsClient,
                            containerGUID);
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
                        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            "Try to call processing...");
                    }

                    try {
                        Response processResponse =
                            startProcessing(parameters, logbookOperationsClient, containerGUID.getId(), actionId,
                                process.getWorkFlowId(), logbookTypeProcess);

                        Status processStatus = Status.fromStatusCode(processResponse.getStatus());
                        ProcessExecutionStatus processExecutionStatus =
                            ProcessExecutionStatus
                                .valueOf(processResponse.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS));
                        isCompletedProcess = isCompletedProcess(processExecutionStatus);

                        if (isCompletedProcess) {
                            // Get ATR
                            try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                                processResponse =
                                    storageClient.getContainerAsync(DEFAULT_STRATEGY,
                                        containerGUID.getId() + XML,
                                        StorageCollectionType.REPORTS);
                                processResponse.getHeaders().add(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS,
                                    processExecutionStatus.toString());
                            }
                        }


                        final AsyncInputStreamHelper helper =
                            new AsyncInputStreamHelper(asyncResponse, processResponse);
                        helper.writeAsyncResponse(Response.fromResponse(processResponse), processStatus);
                    } finally {
                        if (isCompletedProcess) {
                            cleanWorkspace(containerGUID.getId());
                        }
                    }
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
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
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
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
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
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            } catch (final IngestInternalException | IllegalArgumentException | VitamClientException |
                BadRequestException | InternalServerException e) {
                // if an IngestInternalException is thrown, that means logbook has already been updated (with a fatal
                // State)
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            } finally {
                if (logbookOperationsClient != null) {
                    logbookOperationsClient.close();
                }
                StreamUtils.closeSilently(uploadedInputStream);
            }
        }
    }

    private Response startProcessing(final LogbookOperationParameters parameters, final LogbookOperationsClient client,
        final String containerName, final String actionId, final String workflowId, LogbookTypeProcess logbookTypeProcess)
        throws IngestInternalException, ProcessingException, LogbookClientNotFoundException,
        LogbookClientBadRequestException, LogbookClientServerException, InternalServerException, VitamClientException {

        ProcessingManagementClient processingClient = processingManagementClientMock;
        try {
            if (processingClient == null) {
                processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            }

            Response response = processingClient.executeOperationProcess(containerName, workflowId,
                logbookTypeProcess.toString(), actionId);


            // Check global execution status
            String globalExecutionStatus = response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS);
            if (globalExecutionStatus == null) {
                throw new IngestInternalException("Global Execution Status not found.");
            }


            if (isCompletedProcess(ProcessExecutionStatus.valueOf(globalExecutionStatus))) {
                callLogbookUpdate(client, parameters, fromStatusToStatusCode(response.getStatus()),
                    VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, fromStatusToStatusCode(response.getStatus())));
            }

            return response;
        } catch (WorkflowNotFoundException | IllegalArgumentException | BadRequestException exc) {
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

    private StatusCode fromStatusToStatusCode(int status) {
        StatusCode statusCode = StatusCode.OK;
        if (Status.OK.getStatusCode() != status) {
            if (Status.PARTIAL_CONTENT.getStatusCode() == status) {
                statusCode = StatusCode.WARNING;
            } else if (Status.INTERNAL_SERVER_ERROR.getStatusCode() == status) {
                statusCode = StatusCode.FATAL;
            } else {
                statusCode = StatusCode.KO;
            }
        }

        return statusCode;
    }

    /**
     * Executes starting instructions on a process : Updates logbookOperation and pushes the SIP to WorkSpace
     * 
     * @param uploadedInputStream
     * @param parameters
     * @param archiveMimeType
     * @param logbookOperationsClient
     * @param containerGUID
     * @throws LogbookClientNotFoundException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientServerException
     * @throws ContentAddressableStorageException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageAlreadyExistException
     * @throws ContentAddressableStorageCompressedFileException
     * @throws ContentAddressableStorageServerException
     */
    private void prepareToStartProcess(InputStream uploadedInputStream, LogbookOperationParameters parameters,
        String archiveMimeType, LogbookOperationsClient logbookOperationsClient, final GUID containerGUID)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
        ContentAddressableStorageException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException, ContentAddressableStorageCompressedFileException,
        ContentAddressableStorageServerException {

        LOGGER.debug("Starting up the save file sip");
        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.STARTED));

        // start method
        // push uploaded sip as stream
        pushSipStreamToWorkspace(containerGUID.getId(), archiveMimeType,
            uploadedInputStream,
            parameters);
        final String uploadSIPMsg = VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.OK);
        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.OK, uploadSIPMsg);
    }

    /**
     * Executes an action on the given process
     * 
     * @param asyncResponse Async response
     * @param actionId the action to execute
     * @param parameters logbookOperation parameters
     * @param logbookOperationsClient a
     * @param containerGUID
     * @throws LogbookClientAlreadyExistsException
     * @throws LogbookClientBadRequestException
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws IngestInternalException
     * @throws ProcessingException
     * @throws LogbookClientNotFoundException
     * @throws LogbookClientBadRequestExceptionu
     * @throws LogbookClientServerException
     * @throws StorageServerClientException
     * @throws StorageNotFoundException
     */
    private void executeAction(final AsyncResponse asyncResponse, String actionId,
        final GUID containerGUID) {
        ProcessingManagementClient processingClient = processingManagementClientMock;
        LogbookTypeProcess logbookTypeProcess = null;
        try {
            if (processingClient == null) {
                processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            }

            // Execute the given action
            Response updateResponse = processingClient.updateOperationActionProcess(actionId, containerGUID.getId());

            if (Status.UNAUTHORIZED.getStatusCode() == updateResponse.getStatus()) {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.UNAUTHORIZED).build());
                return;
            }

            // Check mandatory headers
            // Check global execution status
            String globalExecutionStatus = updateResponse.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS);
            if (globalExecutionStatus == null) {
                throw new IngestInternalException("Global Execution Status not found.");
            }

            // Check logbookTypeProcess
            String logbookTypeProcessHeader = updateResponse.getHeaderString(GlobalDataRest.X_CONTEXT_ID);
            if (logbookTypeProcessHeader == null) {
                throw new IngestInternalException("Logbook Type Process not found.");
            }
            logbookTypeProcess = LogbookTypeProcess.valueOf(logbookTypeProcessHeader);


            // Process the returned response
            ProcessExecutionStatus processExecutionStatus = ProcessExecutionStatus.valueOf(globalExecutionStatus);
            int stepExecutionStatus = updateResponse.getStatus();

            Response response = Response.status(stepExecutionStatus).build();
            if (isCompletedProcess(processExecutionStatus)) {
                // 1- Add last log
                addFinalLogbookOperationEvent(containerGUID, logbookTypeProcess,
                    fromStatusToStatusCode(stepExecutionStatus));


                // 2- Get ATR file
                try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                    response =
                        storageClient.getContainerAsync(DEFAULT_STRATEGY,
                            containerGUID.getId() + XML,
                            StorageCollectionType.REPORTS);
                }
            }

            // Add Global execution status to response
            response.getHeaders().add(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, processExecutionStatus.toString());
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            helper.writeAsyncResponse(Response.fromResponse(response), Status.fromStatusCode(stepExecutionStatus));

        } catch (final
            LogbookClientNotFoundException | LogbookClientServerException |
            LogbookClientBadRequestException | LogbookClientAlreadyExistsException | StorageClientException |
            StorageNotFoundException e) {

            try {
                addFinalLogbookOperationEvent(containerGUID, logbookTypeProcess,
                    StatusCode.KO);
            } catch (LogbookClientException e1) {
                LOGGER.error("Unexpected error was thrown : " + e1.getMessage(), e1);
            }

            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());

        } catch (IngestInternalException | IllegalArgumentException | InternalServerException | VitamClientException |
            BadRequestException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } finally {
            if (processingClient != null) {
                processingClient.close();
            }
        }
    }


    private void addFinalLogbookOperationEvent(final GUID containerGUID, LogbookTypeProcess logbookTypeProcess,
        StatusCode statusCode)
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientAlreadyExistsException,
        LogbookClientBadRequestException {
            LogbookOperationParameters parameters = logbookInitialisation(containerGUID, containerGUID,
                logbookTypeProcess);
            parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
        callLogbookUpdate(null, parameters, statusCode,
            VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, statusCode));

    }

    private LogbookOperationParameters logbookInitialisation(final GUID ingestGuid, final GUID containerGUID,
        final LogbookTypeProcess logbookTypeProcess)
        throws LogbookClientNotFoundException,
        LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException {
        return LogbookParametersFactory.newLogbookOperationParameters(
            ingestGuid, INGEST_INT_UPLOAD, containerGUID,
            logbookTypeProcess, StatusCode.STARTED,
            ingestGuid != null ? ingestGuid.toString() : "outcomeDetailMessage",
            ingestGuid);
    }

    private void callLogbookUpdate(LogbookOperationsClient client, LogbookOperationParameters parameters,
        StatusCode logbookOutcome, String outcomeDetailMessage)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {

        LogbookOperationsClient logbookOperationsClient = client;
        boolean isNewClientInstance = false;
        try {
            if (logbookOperationsClient == null) {
                isNewClientInstance = true;
                logbookOperationsClient =
                    LogbookOperationsClientFactory.getInstance().getClient();
            }

            if (parameters != null) {
                parameters.setStatus(logbookOutcome);

                parameters.putParameterValue(LogbookParameterName.outcomeDetail, 
                    VitamLogbookMessages.getOutcomeDetail(INGEST_WORKFLOW, logbookOutcome));
                parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
                logbookOperationsClient.update(parameters);
            }
        } finally {
            if (isNewClientInstance && logbookOperationsClient != null) {
                logbookOperationsClient.close();
            }
        }
    }

    /**
     * Pushes the inputStream to Workspace
     * 
     * @param containerName the containerName
     * @param uploadedInputStream the inputStream to store in workspace
     * @param archiveMimeType inputStream mimeType
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



    private boolean isCompletedProcess(ProcessExecutionStatus processExecutionStatus) {
        return processExecutionStatus != null &&
            (ProcessExecutionStatus.COMPLETED.equals(processExecutionStatus) ||
                ProcessExecutionStatus.FAILED.equals(processExecutionStatus));
    }


    /**
     * creates ProcessContext object : parse JSON file
     *
     * @param fileName filename of Json file
     * @param contextId the context id
     * @return ProcessContext's object
     * @throws WorkflowNotFoundException if there is no workflow found
     * @throws ContextNotFoundException if context not found from file data
     */
    public ProcessContext createProcessContextObject(String fileName, String contextId)
        throws WorkflowNotFoundException, ContextNotFoundException {
        ParametersChecker.checkParameter("fileName is a mandatory parameter", fileName);
        ParametersChecker.checkParameter("contextId is a mandatory parameter", contextId);
        ProcessContext processCtx = new ProcessContext();
        try {
            final InputStream inputJSON = getFileAsInputStream(PROCESS_CONTEXT_FILE);
            JsonNode context = JsonHandler.getFromInputStream(inputJSON);
            if (context == null || context.get(contextId) == null) {
                throw new ContextNotFoundException("Context id :" + contextId + " not found in " + context);
            }

            if (!context.get(contextId).isNull()) {
                JsonNode data = context.get(contextId);
                processCtx.setExecutionContext(data.get(EXECUTION_CONTEXT).asText());
                processCtx.setWorkFlowId(data.get(WORKFLOW_ID).asText());
                processCtx.setLogbookTypeProcess(LogbookTypeProcess.valueOf(data.get(LOGBOOK_TYPE_PROCESS).asText()));
                return processCtx;
            } else {
                throw new WorkflowNotFoundException("WorkFlow Not Found");
            }
        } catch (IOException e) {
            LOGGER.error("IOException thrown when creating Process Context Object", e);
            throw new WorkflowNotFoundException("IOException thrown when creating Process Context Object", e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("InvalidParseOperationException thrown ", e);
            throw new WorkflowNotFoundException(
                "InvalidParseOperationException thrown when creating Process Context Object", e);
        } catch (ContextNotFoundException e) {
            LOGGER.error(contextId, e);
            throw new ContextNotFoundException(e);
        }
    }

    private static InputStream getFileAsInputStream(String workflowFile) throws IOException {
        return PropertiesUtils.getConfigAsStream(workflowFile);
    }


    /**
     * @param headers the http header for request
     * @return Response
     */
    @GET
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listOperationsDetails(@Context HttpHeaders headers) {
        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            Response response;
            try {
                response = processManagementClient.listOperationsDetails();
            } catch (VitamClientException e) {
                return Response.serverError().entity(e).build();
            }
            return Response.fromResponse(response).build();
        }
    }
}
