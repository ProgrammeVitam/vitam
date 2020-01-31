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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalException;
import fr.gouv.vitam.logbook.common.MessageLogbookEngineHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Queue;

/**
 * IngestInternalResource
 */
@Path("/ingest/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestInternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResource.class);

    private static final String JSON = ".json";
    private static final String JSONL = ".jsonl";
    private static final String INGEST = "ingest";
    private static final String FOLDER_SIP = "SIP";
    private static final String INGEST_INT_UPLOAD = "STP_UPLOAD_SIP";
    private static final String INGEST_WORKFLOW = "PROCESS_SIP_UNITARY";
    private static final String DEFAULT_STRATEGY = "default";
    private static final String XML = ".xml";
    private static final String DISTRIBUTIONREPORT_SUFFIX = "_report_error.json";
    private static final String FOLDERNAME = "ATR/";
    private static final String CSV = ".csv";
    private static final String INGEST_INTERNAL_MODULE = "INGEST_INTERNAL_MODULE";

    private final WorkspaceClientFactory workspaceClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;

    /**
     * IngestInternalResource constructor
     *
     * @param configuration ingest configuration
     */
    IngestInternalResource(IngestInternalConfiguration configuration) {
        WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getProcessingUrl());
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();

    }

    @VisibleForTesting IngestInternalResource(WorkspaceClientFactory workspaceClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
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
     * @param contentType         the header Content-Type (zip, tar, ...)
     * @param contextId           the header X-Context-Id (steptoStep or not)
     * @param actionId            the header X-ACTION (next,resume,..)
     * @param uploadedInputStream the stream to upload
     */
    @POST
    @Path("/ingests")
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.XGZIP, CommonMediaType.GZIP,
        CommonMediaType.TAR, CommonMediaType.BZIP2})
    public Response uploadSipAsStream(@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType,
        @HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String actionId,
        @HeaderParam(GlobalDataRest.X_ACTION_INIT) String xActionInit,
        @HeaderParam(GlobalDataRest.X_TYPE_PROCESS) LogbookTypeProcess logbookTypeProcess,
        InputStream uploadedInputStream) {
        ParametersChecker.checkParameter("Action Id Request must not be null",
            actionId);

        ParametersChecker.checkParameter("context Id Request must not be null",
            contextId);
        return ingestAsync(contentType, uploadedInputStream, contextId, actionId, xActionInit, logbookTypeProcess);
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param id      operation identifier
     * @return http response
     */
    @Path("/operations/{id}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {
            ParametersChecker.checkParameter("Action Id Request must not be null",
                headers.getRequestHeader(GlobalDataRest.X_ACTION));
            final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
            GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());

            // Execute the given action
            RequestResponse<ItemStatus> updateResponse =
                processingClient.updateOperationActionProcess(xAction, containerGUID.getId());

            if (!updateResponse.isOk()) {
                return updateResponse.toResponse();
            }

            // Check mandatory headers
            // Check global execution status
            String globalExecutionState = updateResponse.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE);
            if (globalExecutionState == null) {
                throw new IngestInternalException("Global Execution Status not found.");
            }

            // Check logbookTypeProcess
            String logbookTypeProcessHeader = updateResponse.getHeaderString(GlobalDataRest.X_CONTEXT_ID);
            if (logbookTypeProcessHeader == null) {
                throw new IngestInternalException("Logbook Type Process not found.");
            }

            LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(logbookTypeProcessHeader);

            // Process the returned response
            ProcessState processState = ProcessState.valueOf(globalExecutionState);
            int stepExecutionStatus = updateResponse.getHttpCode();

            if (isCompletedProcess(processState)) {
                // Add last log
                addFinalLogbookOperationEvent(containerGUID, logbookTypeProcess,
                    fromStatusToStatusCode(stepExecutionStatus));
            }
            return updateResponse.toResponse();
        } catch (Exception e) {
            return Response.serverError().entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(),
                INGEST_INTERNAL_MODULE))
                .build();
        }
    }

    /**
     * Execute the process of an operation related to the id.
     *
     * @param headers             contain X-Action and X-Context-ID
     * @param id                  operation identifier
     * @param uploadedInputStream input stream to upload
     * @return http response
     */
    @Path("/operations/{id}")
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.XGZIP, CommonMediaType.GZIP,
        CommonMediaType.TAR, CommonMediaType.BZIP2})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
        InputStream uploadedInputStream) {
        Status status;
        ItemStatus resp = null;
        MediaType mediaType;
        String archiveMimeType;
        LogbookOperationParameters parameters;

        try {
            ParametersChecker.checkParameter("Action Id Request must not be null",
                headers.getRequestHeader(GlobalDataRest.X_ACTION));
            ParametersChecker.checkParameter("content Type Request must not be null",
                headers.getRequestHeader(HttpHeaders.CONTENT_TYPE));
            ParametersChecker.checkParameter("context Id Request must not be null",
                headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID));
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(getErrorStream(Status.BAD_REQUEST,
                e.getMessage())).build();
        }
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().checkValidRequestId();
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            final String xActionInit = headers.getRequestHeader(GlobalDataRest.X_ACTION_INIT).get(0);
            final String contentType = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).get(0);
            final String contextId = headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID).get(0);

            final GUID containerGUID = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            boolean isInitMode = ProcessAction.INIT.getValue().equalsIgnoreCase(xActionInit);
            boolean isStartMode = ProcessAction.START.getValue().equalsIgnoreCase(xActionInit);

            if (isInitMode) {
                try (ProcessingManagementClient processManagementClient = processingManagementClientFactory
                    .getClient()) {
                    processManagementClient.initVitamProcess(containerGUID.getId(), contextId);
                } catch (BadRequestException e) {
                    LOGGER.error(e);
                    status = Status.BAD_REQUEST;
                    return Response.status(status)
                        .entity(getErrorStream(status, e.getMessage()))
                        .build();
                } catch (InternalServerException e) {
                    LOGGER.error(e);
                    status = Status.INTERNAL_SERVER_ERROR;
                    return Response.status(status)
                        .entity(getErrorStream(status, e.getMessage()))
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
            return Response.status(Status.OK).entity(resp).build();
        } catch (ZipFilesNameNotAllowedException e) {
            LOGGER.error("Error while unzip : ", e);
            status = Status.NOT_ACCEPTABLE;
            return Response.status(status)
                .entity(getErrorStream(status, e.getMessage()))
                .build();

        } catch (LogbookClientNotFoundException | LogbookClientServerException | LogbookClientBadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(getErrorStream(Status.BAD_REQUEST,
                e.getMessage())).build();
        } catch (ContentAddressableStorageException | IngestInternalException | InvalidGuidOperationException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(status, e.getMessage()))
                .build();
        }
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext(INGEST)
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    private InputStream getErrorStream(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        try {
            return JsonHandler.writeToInpustream(new VitamError(status.name())
                .setHttpCode(status.getStatusCode()).setContext(INGEST)
                .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    /**
     * @param id operation identifier
     * @return http response
     */
    @Path("/operations/{id}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            SanityChecker.checkParameter(id);
            final ItemStatus itemStatus = processManagementClient.getOperationProcessStatus(id);

            Response.ResponseBuilder builder = Response.status(Status.ACCEPTED);
            if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                builder.status(Status.OK);

            } else {
                builder.status(Status.ACCEPTED);
            }

            return builder
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, itemStatus.getGlobalState())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalStatus())
                .header(GlobalDataRest.X_CONTEXT_ID, itemStatus.getLogbookTypeProcess())
                .build();

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED).build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NO_CONTENT).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (VitamClientException | InternalServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * get the workflow status
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("/operations/{id}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowStatus(@PathParam("id") String id) {
        Status status;
        ItemStatus itemStatus;
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            itemStatus = processManagementClient.getOperationProcessExecutionDetails(id);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NO_CONTENT;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();

        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
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
    @Path("/operations/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {
        Status status;
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            SanityChecker.checkParameter(id);
            ItemStatus itemStatus = processManagementClient.cancelOperationProcessExecution(id);
            return Response.status(Status.OK)
                .entity(itemStatus)
                .build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();
        } catch (VitamClientException | InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status, e.getMessage()))
                .build();
        }
    }


    /**
     * Download object stored by Ingest operation (currently ATR and manifest)
     * <p>
     * Return the object as stream asynchronously
     *
     * @param objectId the object id
     * @param type     the collection type
     * @response the response
     */
    @GET
    @Path("/ingests/{objectId}/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadObjectAsStream(@PathParam("objectId") String objectId, @PathParam("type") String type) {
        return downloadObjectAsync(objectId, type);
    }

    /**
     * @param guid the object guid
     * @param atr  the inputStream ATR
     * @return the status of the request (OK)
     */
    @POST
    @Path("/ingests/{objectId}/report")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response storeATR(@PathParam("objectId") String guid, InputStream atr) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            SanityChecker.checkParameter(guid);
            try {
                workspaceClient.createContainer(guid);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.debug(e);
            }
            workspaceClient.putObject(guid, FOLDERNAME + guid + XML, atr);

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(guid);
            description.setWorkspaceObjectURI(FOLDERNAME + guid + XML);
            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY,
                DataCategory.REPORT, guid + XML, description);
            workspaceClient.deleteContainer(guid, true);
            return Response.status(Status.OK).build();

        } catch (StorageClientException | ContentAddressableStorageServerException |
            ContentAddressableStorageNotFoundException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private Response downloadObjectAsync(String objectId, String type) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            DataCategory documentType = DataCategory.getByCollectionName(type);

            switch(documentType) {
                case MANIFEST:
                case REPORT:
                    objectId += XML;
                    break;
                case DISTRIBUTIONREPORTS:
                    objectId += DISTRIBUTIONREPORT_SUFFIX;
                    break;
                case BATCH_REPORT:
                    // #5621 Ugly hack for use same container for BATCH_REPORT files (jsonl reports)
                    objectId += JSONL;
                    documentType = DataCategory.REPORT;
                    break;
                case RULES:
                    // #2940 Ugly hack for use the same point of API for all json report
                    objectId += JSON;
                    documentType = DataCategory.REPORT;
                    break;
                case REFERENTIAL_RULES_CSV:
                    // #5621 Ugly hack for share IngestCollection with DataCategory
                    objectId += CSV;
                    documentType = DataCategory.RULES;
                    break;
                case REFERENTIAL_AGENCIES_CSV:
                    // #5621 Ugly hack for share IngestCollection with DataCategory
                    objectId += CSV;
                    documentType = DataCategory.REPORT;
                    break;
                default:
                    return Response.status(Status.METHOD_NOT_ALLOWED).build();
            }
            final Response response = storageClient.getContainerAsync(DEFAULT_STRATEGY,
                objectId, documentType, AccessLogUtils.getNoLogAccessLog());
            return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            return Response.status(Status.BAD_REQUEST).entity(getErrorStream(Status.BAD_REQUEST,
                e.getMessage())).build();
        } catch (StorageNotFoundException e) {
            LOGGER.error("Storage error was thrown : ", e);
            return Response.status(Status.NOT_FOUND).entity(getErrorStream(Status.NOT_FOUND,
                e.getMessage())).build();
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage error was thrown : ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorStream(Status.INTERNAL_SERVER_ERROR,
                e.getMessage())).build();
        }
    }

    private Response ingestAsync(String contentType,
        InputStream uploadedInputStream, String contextId, String actionId, String xActionInit,
        LogbookTypeProcess logbookTypeProcess) {

        LogbookOperationParameters parameters = null;
        MediaType mediaType;
        String archiveMimeType;
        boolean isCompletedProcess = false;
        final String containerId = VitamThreadUtils.getVitamSession().getRequestId();

        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            try {
                VitamThreadUtils.getVitamSession().checkValidRequestId();
                ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
                ParametersChecker.checkParameter("actionId is a mandatory parameter", actionId);
                ParametersChecker.checkParameter("logbookTypeProcess is a mandatory parameter", logbookTypeProcess);
                ParametersChecker.checkParameter("contextId is a mandatory parameter", contextId);

                GUID containerGUID = GUIDReader.getGUID(containerId);

                boolean isInitMode = ProcessAction.INIT.getValue().equalsIgnoreCase(xActionInit);
                boolean isStartMode = ProcessAction.START.getValue().equalsIgnoreCase(xActionInit);

                parameters = logbookInitialisation(containerGUID, containerGUID, logbookTypeProcess);

                if (isInitMode) {
                    workspaceClient.checkStatus();
                    try (ProcessingManagementClient processManagementClient = processingManagementClientFactory
                        .getClient()) {

                        if (containerGUID != null) {
                            // Initialize a new process
                            processManagementClient.initVitamProcess(containerGUID.getId(), contextId);

                            // Successful initialization
                            return Response.status(Status.ACCEPTED).build();
                        }
                    }
                } else {

                    // Start process
                    if (isStartMode) {
                        // Get MimeType
                        mediaType = CommonMediaType.valueOf(contentType);
                        archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);

                        prepareToStartProcess(uploadedInputStream, parameters, archiveMimeType, logbookOperationsClient,
                            containerGUID);
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
                        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            "Try to call processing...");
                    }

                    try {
                        if (containerGUID != null) {
                            ProcessState processState =
                                startProcessing(parameters, logbookOperationsClient, containerGUID, actionId,
                                    logbookTypeProcess, contextId);

                            isCompletedProcess = isCompletedProcess(processState);

                            if (!isInitMode) {
                                // Asynchrone
                                return Response.status(Status.ACCEPTED).build();
                            }
                        }
                    } finally {
                        if (isCompletedProcess) {
                            cleanWorkspace(containerGUID.getId());
                        }
                    }
                }

            } catch (final ZipFilesNameNotAllowedException e) {

                if (parameters != null) {
                    try {

                        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                            GUIDFactory.newEventGUID(GUIDReader.getGUID(containerId)).getId());
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO, INGEST_INT_UPLOAD,
                            VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.KO));
                    } catch (final LogbookClientException | InvalidGuidOperationException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Error while unzip file : " + e.getMessage(), e);
                return Response.status(Status.NOT_ACCEPTABLE).build();

            } catch (final ContentAddressableStorageException | VitamApplicationServerException e) {
                if (parameters != null) {
                    try {
                        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                            GUIDFactory.newEventGUID(GUIDReader.getGUID(containerId)).getId());
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.FATAL, INGEST_INT_UPLOAD,
                            "error workspace");
                    } catch (final LogbookClientException | InvalidGuidOperationException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                return Response.status(Status.SERVICE_UNAVAILABLE).build();
                // FIXME P1 in particular Processing Exception could it be a "normal error" ?
                // Have to determine here if it is an internal error and FATAL result or processing error, so business
                // error and KO result
            } catch (LogbookClientException | InvalidGuidOperationException e) {
                if (parameters != null) {
                    try {
                        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                            GUIDFactory.newEventGUID(GUIDReader.getGUID(containerId)).getId());
                        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
                        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.KO,
                            INGEST_WORKFLOW, VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, StatusCode.KO));

                    } catch (final LogbookClientException | InvalidGuidOperationException e1) {
                        LOGGER.error(e1);
                    }
                }
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (final IngestInternalException | IllegalArgumentException |
                BadRequestException | InternalServerException e) {
                // if an IngestInternalException is thrown, that means logbook has already been updated (with a fatal
                // State)
                LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } finally {
                StreamUtils.closeSilently(uploadedInputStream);
                if (logbookOperationsClient != null) {
                    logbookOperationsClient.close();
                }
            }
        }

        // We should not get here
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    private ProcessState startProcessing(final LogbookOperationParameters parameters,
        final LogbookOperationsClient client,
        final GUID containerGUID, final String actionId,
        LogbookTypeProcess logbookTypeProcess, String contextId)
        throws IngestInternalException, LogbookClientNotFoundException,
        LogbookClientBadRequestException, LogbookClientServerException, InternalServerException {

        MessageLogbookEngineHelper messageLogbookEngineHelper = new MessageLogbookEngineHelper(logbookTypeProcess);
        String containerName = containerGUID.getId();
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {

            RequestResponse<JsonNode> response =
                processingClient.executeOperationProcess(containerName, contextId, actionId);

            // Check global execution status
            String globalExecutionState = response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE);

            if (globalExecutionState == null) {
                throw new IngestInternalException("Global Execution Status not found.");
            }

            if (isCompletedProcess(ProcessState.valueOf(globalExecutionState))) {
                parameters.putParameterValue(LogbookParameterName.eventIdentifier,
                    GUIDFactory.newEventGUID(containerGUID).getId());
                callLogbookUpdate(client, parameters, fromStatusToStatusCode(response.getStatus()),
                    INGEST_WORKFLOW,
                    messageLogbookEngineHelper.getLabelOp(logbookTypeProcess.name(), fromStatusToStatusCode(response
                        .getStatus())));
            }

            return ProcessState.valueOf(globalExecutionState);
        } catch (WorkflowNotFoundException | IllegalArgumentException | VitamClientException exc) {
            LOGGER.error(exc);
            callLogbookUpdate(client, parameters, StatusCode.FATAL,
                INGEST_WORKFLOW, messageLogbookEngineHelper.getLabelOp(logbookTypeProcess.name(), StatusCode.FATAL));
            throw new IngestInternalException(exc);
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
     */
    private void prepareToStartProcess(InputStream uploadedInputStream, LogbookOperationParameters parameters,
        String archiveMimeType, LogbookOperationsClient logbookOperationsClient, final GUID containerGUID)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
        ContentAddressableStorageException {

        LOGGER.debug("Starting up the save file sip");
        LogbookOperationParameters startedParameters = LogbookOperationsClientHelper.copy(parameters);
        String eventTypeStarted = VitamLogbookMessages.getEventTypeStarted(INGEST_INT_UPLOAD);
        startedParameters.putParameterValue(LogbookParameterName.eventType, eventTypeStarted);
        callLogbookUpdate(logbookOperationsClient, startedParameters, StatusCode.OK,
            eventTypeStarted, VitamLogbookMessages.getCodeOp(eventTypeStarted, StatusCode.OK));

        // set eventType and new eventId
        parameters
            .putParameterValue(LogbookParameterName.eventIdentifier, GUIDFactory.newEventGUID(containerGUID).getId());
        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_INT_UPLOAD);

        // start method
        // push uploaded sip as stream
        pushSipStreamToWorkspace(containerGUID.getId(), archiveMimeType, uploadedInputStream, parameters);

        // logbook update
        final String uploadSIPMsg = VitamLogbookMessages.getCodeOp(INGEST_INT_UPLOAD, StatusCode.OK);
        callLogbookUpdate(logbookOperationsClient, parameters, StatusCode.OK, INGEST_INT_UPLOAD, uploadSIPMsg);
    }

    /**
     * Executes an action on the given process
     *
     * @param actionId      the action to start
     * @param containerGUID
     * @return response
     */
    private Response executeAction(String actionId, final GUID containerGUID) {
        ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
        LogbookTypeProcess logbookTypeProcess = null;
        try {
            // Execute the given action
            RequestResponse<ItemStatus> updateResponse =
                processingClient.updateOperationActionProcess(actionId, containerGUID.getId());

            if (Status.UNAUTHORIZED.getStatusCode() == updateResponse.getHttpCode()) {
                return Response.status(Status.UNAUTHORIZED).build();
            }

            // Check mandatory headers
            // Check global execution status
            String globalExecutionState = updateResponse.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE);
            if (globalExecutionState == null) {
                throw new IngestInternalException("Global Execution Status not found.");
            }

            // Check logbookTypeProcess
            String logbookTypeProcessHeader = updateResponse.getHeaderString(GlobalDataRest.X_CONTEXT_ID);
            if (logbookTypeProcessHeader == null) {
                throw new IngestInternalException("Logbook Type Process not found.");
            }
            logbookTypeProcess = LogbookTypeProcess.valueOf(logbookTypeProcessHeader);


            // Process the returned response
            ProcessState processState = ProcessState.valueOf(globalExecutionState);
            int stepExecutionStatus = updateResponse.getHttpCode();

            Response response;
            if (isCompletedProcess(processState)) {
                // 1- Add last log
                addFinalLogbookOperationEvent(containerGUID, logbookTypeProcess,
                    fromStatusToStatusCode(stepExecutionStatus));


                // 2- Get ATR file
                try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                    response =
                        storageClient.getContainerAsync(DEFAULT_STRATEGY,
                            containerGUID.getId() + XML,
                            DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
                }
            } else {
                response = Response.status(stepExecutionStatus).entity(updateResponse).build();
            }
            // Add Global execution status to response
            response.getHeaders().add(GlobalDataRest.X_GLOBAL_EXECUTION_STATE,
                processState != null ? processState.toString() : null);
            return new VitamAsyncInputStreamResponse(response, Status.fromStatusCode(stepExecutionStatus),
                response.getMediaType());
        } catch (LogbookClientNotFoundException | LogbookClientServerException | LogbookClientBadRequestException | StorageClientException | StorageNotFoundException e) {

            try {
                addFinalLogbookOperationEvent(containerGUID, logbookTypeProcess,
                    StatusCode.KO);
            } catch (LogbookClientException e1) {
                LOGGER.error("Unexpected error was thrown : " + e1.getMessage(), e1);
            }

            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();

        } catch (IngestInternalException | IllegalArgumentException | InternalServerException | VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (processingClient != null) {
                processingClient.close();
            }
        }
    }


    private void addFinalLogbookOperationEvent(final GUID containerGUID, LogbookTypeProcess logbookTypeProcess,
        StatusCode statusCode)
        throws LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException {
        LogbookOperationParameters parameters = logbookInitialisation(containerGUID,
            containerGUID, logbookTypeProcess);
        parameters.putParameterValue(LogbookParameterName.eventType, INGEST_WORKFLOW);
        callLogbookUpdate(null, parameters, statusCode,
            INGEST_WORKFLOW, VitamLogbookMessages.getCodeOp(INGEST_WORKFLOW, statusCode));

    }

    private LogbookOperationParameters logbookInitialisation(final GUID ingestGuid, final GUID containerGUID,
        final LogbookTypeProcess logbookTypeProcess) {
        return LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(ingestGuid), INGEST_INT_UPLOAD, containerGUID,
            logbookTypeProcess, StatusCode.STARTED,
            ingestGuid != null ? ingestGuid.toString() : "outcomeDetailMessage",
            ingestGuid);
    }

    private void callLogbookUpdate(LogbookOperationsClient client, LogbookOperationParameters parameters,
        StatusCode logbookOutcome, String outcomeDetail, String outcomeDetailMessage)
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
                    VitamLogbookMessages.getOutcomeDetail(outcomeDetail, logbookOutcome));
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
     * @param containerName       the containerName
     * @param uploadedInputStream the inputStream to store in workspace
     * @param archiveMimeType     inputStream mimeType
     * @throws ContentAddressableStorageException
     */
    private void pushSipStreamToWorkspace(final String containerName,
        final String archiveMimeType,
        final InputStream uploadedInputStream, final LogbookOperationParameters parameters)
        throws ContentAddressableStorageException {


        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "Try to push stream to workspace...");
        LOGGER.debug("Try to push stream to workspace...");

        // call workspace
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.uncompressObject(containerName, FOLDER_SIP, archiveMimeType, uploadedInputStream);
            } else {
                throw new ContentAddressableStorageAlreadyExistException(containerName + "already exist");
            }
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }

        LOGGER.debug(" -> push stream to workspace finished");
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "-> push stream to workspace finished");
    }

    private void cleanWorkspace(final String containerName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        // call workspace
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.deleteContainer(containerName, true);
            }
        }
    }


    private boolean isCompletedProcess(ProcessState processState) {
        return processState != null && (ProcessState.COMPLETED.equals(processState));
    }

    /**
     * @param headers the http header for request
     * @param query   the filter query
     * @return Response
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            try {
                return processManagementClient.listOperationsDetails(query).toResponse();
            } catch (VitamClientException e) {
                return Response.serverError().entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                    .build();
            }
        }
    }

    /**
     * @param headers the http header for request
     * @return Response
     */
    @GET
    @Path("/workflows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkflowDefinitions(@Context HttpHeaders headers) {
        ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
        try {
            return processingClient.getWorkflowDefinitions().toResponse();
        } catch (VitamClientException e) {
            return Response.serverError().entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    @Path("workflows/{workfowId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkflowDetails(@PathParam("workfowId") String workfowId) {
        ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
        try {
            Optional<WorkFlow> optionalWorkflow = processingClient.getWorkflowDetails(workfowId);
            if (optionalWorkflow.isPresent()) {
                return Response.status(Status.OK)
                    .header(GlobalDataRest.X_TYPE_PROCESS, optionalWorkflow.get().getTypeProc())
                    .entity(optionalWorkflow.get())
                    .build();
            }

            return Response.status(Status.NOT_FOUND).build();
        } catch (VitamClientException e) {
            LOGGER.error("Error while retrieving workflow definitions : ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
        }
    }

    /**
     * Construct the error following input
     *
     * @param status  Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @param code    The functional error code, if absent the http code will be used instead
     * @return VitamError
     */
    // FIXME 2905 : refacto as a common code
    private VitamError getErrorEntity(Status status, String message, String code) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode).setHttpCode(status.getStatusCode())
            .setContext(ServiceName.INTERNAL_INGEST.getName())
            .setState("KO").setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }
}
