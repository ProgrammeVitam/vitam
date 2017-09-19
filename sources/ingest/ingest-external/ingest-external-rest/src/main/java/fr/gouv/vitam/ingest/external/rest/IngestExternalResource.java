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
package fr.gouv.vitam.ingest.external.rest;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;
import fr.gouv.vitam.ingest.external.core.PreUploadResume;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

/**
 * The Ingest External Resource
 */
@Path("/ingest-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);
    private final IngestExternalConfiguration ingestExternalConfiguration;
    private final SecureEndpointRegistry secureEndpointRegistry;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration the configuration of server resource
     * @param secureEndpointRegistry 
     */
    public IngestExternalResource(
            IngestExternalConfiguration ingestExternalConfiguration,
            SecureEndpointRegistry secureEndpointRegistry
            ) {
        this.ingestExternalConfiguration = ingestExternalConfiguration;
        this.secureEndpointRegistry = secureEndpointRegistry;
        LOGGER.info("init Ingest External Resource server");
    }


    /**
     * List secured resource end points
     * @return response
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response listResourceEndpoints() {

        String resourcePath = IngestExternalResource.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * upload the file in local
     *
     * @param contextId           the context id of upload
     * @param action              in workflow
     * @param uploadedInputStream data input stream
     * @param asyncResponse       the asynchronized response
     */
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "ingests:create", description = "Envoyer un SIP à Vitam afin qu'il en réalise l'entrée")
    public void upload(@HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String action, InputStream uploadedInputStream,
        @Suspended final AsyncResponse asyncResponse) {
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        Integer tenantId = ParameterHelper.getTenantParameter();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> uploadAsync(uploadedInputStream, asyncResponse, tenantId, contextId, action, guid));

    }

    private void uploadAsync(InputStream uploadedInputStream, AsyncResponse asyncResponse,
        Integer tenantId, String contextId, String action, GUID guid) {

        final IngestExternalImpl ingestExternal = new IngestExternalImpl(ingestExternalConfiguration);
        try {
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            PreUploadResume preUploadResume = null;
            try {
                preUploadResume =
                    ingestExternal.preUploadAndResume(uploadedInputStream, contextId, action, guid, asyncResponse);
            } catch (WorkspaceClientServerException e) {
                LOGGER.error(e);
                ingestExternal.createATRFatalWorkspace(contextId, guid, asyncResponse);
                return;
            }
            Response response = ingestExternal.upload(preUploadResume, guid);
            response.close();
        } catch (final Exception exc) {
            LOGGER.error(exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL)
                    .entity(getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                            exc.getLocalizedMessage())))
                    .build(),
                uploadedInputStream);
        }
    }

    /**
     * Download reports stored by Ingest operation (currently reports and manifests)
     * <p>
     * Return the reports as stream asynchronously<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param objectId the id of object to download
     * @return response
     */
    @GET
    @Path("/ingests/{objectId}/reports")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "ingests:id:reports:read", description = "Récupérer l'accusé de récéption pour une opération d'entrée donnée")
    public Response downloadIngestReportsAsStream(@PathParam("objectId") String objectId) {
        return downloadObjectAsync(objectId, IngestCollection.REPORTS);
    }

    /**
     * Download manifest stored by Ingest operation (currently reports and manifests)
     * <p>
     * Return the manifest as stream asynchronously<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     *
     * @param objectId the id of object to download
     * @param asyncResponse the asynchronized response
     * @return response
     *
     */
    @GET
    @Path("/ingests/{objectId}/manifests")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "ingests:id:manifests:read", description = "Récupérer le bordereau de versement pour une opération d'entrée donnée")
    public Response downloadIngestManifestsAsStream(@PathParam("objectId") String objectId) {
        return downloadObjectAsync(objectId, IngestCollection.MANIFESTS);
    }

    private Response downloadObjectAsync(String objectId, IngestCollection collection) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            final Response response = ingestInternalClient.downloadObjectAsync(objectId, collection);
            return new VitamAsyncInputStreamResponse(response);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_BAD_REQUEST, e.getLocalizedMessage())))
                    .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicates Failed Exception", e);
            return Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_PRECONDITION_FAILED,
                            e.getLocalizedMessage())))
                    .build();
        } catch (final IngestInternalClientServerException e) {
            LOGGER.error("Internal Server Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                            e.getLocalizedMessage())))
                    .build();
        } catch (final IngestInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            return Response.status(Status.NOT_FOUND)
                    .entity(getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, e.getLocalizedMessage())))
                    .build();
        }
    }

    /**
     * @param headers the http header of request
     * @param query   the filter query
     * @return the list of Operations details
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:read", description = "Récupérer les informations sur une opération donnée")
    public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            return client.listOperationsDetails(query).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.serverError()
                .entity(
                    VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR,
                        e.getLocalizedMessage()))
                .build();
        }
    }

    // FIXME: 4/18/17 why uploadedInputStream ? should we remove this parameter? ItemStatus resp also is not used ?!!

    /**
     * Execute the process of an operation related to the id.
     *
     * @param headers             contain X-Action and X-Context-ID
     * @param id                  operation identifier
     * @param uploadedInputStream input stream to upload
     * @return http response
     * @throws InternalServerException       if request resources server exception
     * @throws VitamClientException          if the server is unreachable
     * @throws InvalidGuidOperationException if error when create guid
     * @throws ProcessingException           if error in workflow execution
     * @deprecated use PUT method /operation/id
     */
    @Path("/operations/{id}")
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP, CommonMediaType.GZIP, CommonMediaType.TAR,
        CommonMediaType.BZIP2})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "operations:id:update #2", description = "Gérer le cycle de vie d'un workflow donné")
    @Deprecated // FIXME see 2745 to decide if we need the method or not
    public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
        InputStream uploadedInputStream) {
        Status status;
        ItemStatus resp = null;
        ParametersChecker.checkParameter("content Type Request must not be null",
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE));
        ParametersChecker.checkParameter("context Id Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID));

        final String xContextId = headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID).get(0);
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(id);
            ingestInternalClient.executeOperationProcess(id, null, xContextId, null);

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorStream(status, e.getLocalizedMessage()))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(status, e.getLocalizedMessage()))
                .build();
        } catch (InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(status, e.getLocalizedMessage()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorStream(status, e.getLocalizedMessage()))
                .build();
        }

        return Response.status(Status.OK).entity(resp).build();

    }

    private InputStream getErrorStream(VitamError vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    @Deprecated
    private InputStream getErrorStream(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        try {
            return JsonHandler.writeToInpustream(new VitamError(status.name())
                .setHttpCode(status.getStatusCode()).setContext(ServiceName.EXTERNAL_INGEST.getName())
                .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    /**
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:read:status", description = "Récupérer le code HTTP d'une opération donnée")
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(id);
            final ItemStatus itemStatus = ingestInternalClient.getOperationProcessStatus(id);

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
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NO_CONTENT).build();
        } catch (VitamClientException | InternalServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * get the workflow status
     *
     * @param id    operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:read", description = "Récupérer le statut d'une opération donnée")
    public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
        Status status;
        ItemStatus itemStatus = null;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            itemStatus = ingestInternalClient.getOperationProcessExecutionDetails(id);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Illegal argument: " + e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (InternalServerException e) {
            LOGGER.error("Cound get operation detail: " + e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to get operation detail: " + e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param headers       contain X-Action and X-Context-ID
     * @param id            operation identifier
     * @param asyncResponse asyncResponse
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:update #1", description = "Changer le statut d'une opération donnée")
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        return updateOperationActionProcessAsync(id, xAction);
    }

    private Response updateOperationActionProcessAsync(String operationId, String action) {

        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            RequestResponse<ItemStatus> itemStatusRequestResponse =
                ingestInternalClient.updateOperationActionProcess(action, operationId);
            return itemStatusRequestResponse.toResponse();
        } catch (final ProcessingException e) {
            LOGGER.error("Unauthorized action for update ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, e.getLocalizedMessage()).toResponse();
        } catch (InternalServerException e) {
            LOGGER.error("Could not update operation process ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Client exception while trying to update operation process ", e);
            return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR, e.getLocalizedMessage())
                    .toResponse();
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to update operation process ", e);
            return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_BAD_REQUEST, e.getLocalizedMessage())
                    .toResponse();
        }
    }

    /**
     * Interrupt the process of an operation identified by Id.
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission ="operations:id:delete", description = "Annuler une opération donnée")
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {

        ParametersChecker.checkParameter("operationId must not be null", id);
        VitamError vitamError;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(id);
            VitamThreadUtils.getVitamSession().setRequestId(id);

            final ItemStatus itemStatus = ingestInternalClient.cancelOperationProcessExecution(id);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Illegal argument: " + e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_ILLEGAL_ARGUMENT, e.getLocalizedMessage());
        } catch (WorkflowNotFoundException e) {
            LOGGER.error("Cound not find workflow: " + e);
            vitamError = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, e.getLocalizedMessage());
        } catch (InternalServerException e) {
            LOGGER.error("Cound not cancel operation: " + e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to cancel operation: " + e);
            vitamError = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, e.getLocalizedMessage());
        } catch (VitamClientException e) {
            LOGGER.error("Client exception while trying to cancel operation: " + e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR, e.getLocalizedMessage());
        }

        return Response.status(vitamError.getHttpCode()).entity(vitamError).build();
    }

    /**
     * @param headers the http header of request
     * @return Response
     */
    @GET
    @Path("/workflows")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "workflows:read", description = "Récupérer la liste des tâches des workflows")
    public Response getWorkflowDefinitions(@Context HttpHeaders headers) {
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            return client.getWorkflowDefinitions().toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.serverError()
                .entity(VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR,
                    e.getLocalizedMessage()))
                .build();
        }
    }
}
