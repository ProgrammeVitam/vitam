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

import java.io.InputStream;

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
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.core.AtrKoBuilder;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalException;

/**
 * The Ingest External Resource
 */
@Path("/ingest-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestExternalResource extends ApplicationStatusResource {

    // FIXME P0 : Add a filter to protect the tenantId (check if it exists + return Unauthorized response or so). @see :
    // AuthorizationFilter
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);
    private final IngestExternalConfiguration ingestExternalConfiguration;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration the configuration of server resource
     *
     */
    public IngestExternalResource(IngestExternalConfiguration ingestExternalConfiguration) {
        this.ingestExternalConfiguration = ingestExternalConfiguration;
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * upload the file in local
     *
     * @param contextId the context id of upload
     * @param action in workflow
     * @param uploadedInputStream data input stream 
     * @param asyncResponse the asynchronized response
     * 
     */
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // TODO P2 : add file name
    public void upload(@HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String action, InputStream uploadedInputStream,
        @Suspended final AsyncResponse asyncResponse) {

        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> uploadAsync(asyncResponse, uploadedInputStream, tenantId, contextId, action));
    }

    private void uploadAsync(final AsyncResponse asyncResponse, InputStream uploadedInputStream,
        Integer tenantId, String contextId, String action) {
        try {
            // TODO ? ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final IngestExternalImpl ingestExtern = new IngestExternalImpl(ingestExternalConfiguration);
            ingestExtern.upload(uploadedInputStream, asyncResponse, contextId, action);
        } catch (final IngestExternalException exc) {
            LOGGER.error(exc);
            try {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(AtrKoBuilder.buildAtrKo(GUIDFactory.newRequestIdGUID(0).getId(),
                            "ArchivalAgencyToBeDefined", "TransferringAgencyToBeDefined",
                            "PROCESS_SIP_UNITARY", exc.getMessage(), StatusCode.FATAL))
                        .type(MediaType.APPLICATION_XML_TYPE).build());
            } catch (final IngestExternalException e) {
                // Really bad
                LOGGER.error(e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            }
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
    }

    /**
     * Download object stored by Ingest operation (currently ATR and manifest)
     * 
     * Return the object as stream asynchronously
     * 
     * @param objectId the id of object to download
     * @param type of collection
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

    private void downloadObjectAsync(final AsyncResponse asyncResponse, String objectId,
        String type) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            IngestCollection collection = IngestCollection.valueOf(type.toUpperCase());
            final Response response = ingestInternalClient.downloadObjectAsync(objectId, collection);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            helper.writeResponse(Response.status(response.getStatus()));
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
        } catch (VitamClientException e) {
            LOGGER.error("VitamClientException was thrown : ", e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
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
        ParametersChecker.checkParameter("content Type Request must not be null",
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE));
        ParametersChecker.checkParameter("context Id Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID));

        final String xContextId = headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID).get(0);
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            ingestInternalClient.executeOperationProcess(id, null, xContextId, null);

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
            status = Status.BAD_REQUEST;
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

        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            itemStatus = ingestInternalClient.getOperationProcessStatus(id);

        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (final WorkflowNotFoundException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.NOT_FOUND;
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
        ItemStatus pwork = null;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            pwork = ingestInternalClient.getOperationProcessExecutionDetails(id, query);

        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();

        } catch (final WorkflowNotFoundException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.NO_CONTENT;
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
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }

        return Response.status(Status.OK).entity(pwork).build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> updateOperationActionProcessAsync(asyncResponse, id, xAction));

        return Response.status(Status.OK).build();
    }

    private void updateOperationActionProcessAsync(final AsyncResponse asyncResponse, String operationId,
        String action) {
        Response response = null;

        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            response = ingestInternalClient.updateOperationActionProcess(action, operationId);

            AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            helper.writeAsyncResponse(Response.fromResponse(response), Status.fromStatusCode(response.getStatus()));
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, Response.status(Status.UNAUTHORIZED).build());
        } catch (InternalServerException | VitamClientException e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (BadRequestException e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {

        ParametersChecker.checkParameter("operationId must not be null", id);
        Status status;
        Response response = null;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            response = ingestInternalClient.cancelOperationProcessExecution(id);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
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
        } catch (VitamClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }

        return response;
    }


    /**
     * @param headers the http header of request
     * @return Response
     */
    @GET
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listOperationsDetails(@Context HttpHeaders headers) {
        Response response = null;
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            response = client.listOperationsDetails();
            return Response.fromResponse(response).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }



}
