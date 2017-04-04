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

import static fr.gouv.vitam.common.client.DefaultClient.staticConsumeAnyEntityAndClose;

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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;
import fr.gouv.vitam.ingest.external.core.PreUploadResume;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;

/**
 * The Ingest External Resource
 */
@Path("/ingest-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestExternalResource extends ApplicationStatusResource {

    // AuthorizationFilter
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);
    private final IngestExternalConfiguration ingestExternalConfiguration;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration
     */
    public IngestExternalResource(IngestExternalConfiguration ingestExternalConfiguration) {
        this.ingestExternalConfiguration = ingestExternalConfiguration;
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * Starts the process of ingest for one Stream
     *
     * @param contextId
     * @param action may specify CONTINUE or PAUSE
     * @param uploadedInputStream data input stream
     * @param asyncResponse
     */
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
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
        try {
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final IngestExternalImpl ingestExternal = new IngestExternalImpl(ingestExternalConfiguration);
            PreUploadResume preUploadResume =
                ingestExternal.preUploadAndResume(uploadedInputStream, contextId, action, guid, asyncResponse);
            ingestExternal.upload(preUploadResume, guid);
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.BAD_REQUEST)
                    .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, ProcessExecutionStatus.FAILED)
                    .build());
        } catch (final Exception exc) {
            LOGGER.error(exc);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_REQUEST_ID, guid.getId())
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, ProcessExecutionStatus.FAILED)
                    .build());
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
    }

    /**
     * Download object stored by Ingest operation (currently ATR and manifest)
     * <p>
     * Return the object as stream asynchronously. <br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param objectId
     * @param type
     * @param asyncResponse
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
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
        } catch (VitamClientException e) {
            LOGGER.error("VitamClientException was thrown : ", e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext("ingest")
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }

    /**
     * Return the list of current active operations
     * 
     * @param headers
     * @return the Response of the currect active operations
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
        } finally {
            staticConsumeAnyEntityAndClose(response);
        }
    }

    /**
     * get the operation status
     *
     * @param id operation identifier
     * @return http response as status only
     */
    @Path("operations/{id}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        Status status;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            ingestInternalClient.getOperationProcessExecutionDetails(id, null);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .build();
        } catch (final WorkflowNotFoundException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.NO_CONTENT;
            return Response.status(status)
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .build();
        } catch (InternalServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .build();
        }

        return Response.status(Status.OK).build();
    }

    /**
     * get the complete workflow status
     *
     * @param id operation identifier
     * @param query body
     * @return http response with the status in the body
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowStatus(@PathParam("id") String id) {
        Status status;
        ItemStatus pwork = null;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            pwork = ingestInternalClient.getOperationProcessExecutionDetails(id, null);

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
     * @param id operation identifier
     * @param asyncResponse asyncResponse
     */
    @Path("operations/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> updateOperationActionProcessAsync(asyncResponse, id, xAction));
    }

    // async
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
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.UNAUTHORIZED).build());
        } catch (InternalServerException | VitamClientException e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
        } catch (BadRequestException e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.BAD_REQUEST).build());
        } catch (Exception e) {
            LOGGER.error(e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
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
            return Response.fromResponse(response).build();

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

        } catch (InternalServerException | VitamClientException e) {
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
        } finally {
            staticConsumeAnyEntityAndClose(response);
        }
    }


}
