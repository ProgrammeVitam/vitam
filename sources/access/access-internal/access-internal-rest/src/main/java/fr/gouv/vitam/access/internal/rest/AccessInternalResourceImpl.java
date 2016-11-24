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
package fr.gouv.vitam.access.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
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

import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server2.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server2.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server2.application.VitamHttpHeader;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;


/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessInternalResourceImpl extends ApplicationStatusResource implements AccessInternalResource {

    private static final String THERE_IS_NO_X_HTTP_METHOD_OVERRIDE_GET_AS_A_HEADER =
        "There is no 'X-Http-Method-Override:GET' as a header";
    private static final String END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS = "End of execution of DSL Vitam from Access";
    private static final String EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING =
        "Execution of DSL Vitam from Access ongoing...";
    private static final String BAD_REQUEST_EXCEPTION = "Bad request Exception ";
    private static final String ACCESS_MODULE = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImpl.class);


    private final AccessInternalModule accessModule;

    /**
     *
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessInternalResourceImpl(AccessInternalConfiguration configuration) {
        accessModule = new AccessInternalModuleImpl(configuration);
        LOGGER.debug("AccessResource initialized");
    }

    /**
     * Test constructor
     *
     * @param accessModule
     */
    AccessInternalResourceImpl(AccessInternalModule accessModule) {
        this.accessModule = accessModule;
        LOGGER.debug("AccessResource initialized");
    }

    /**
     * get units list by query
     */
    @Override
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(JsonNode queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        JsonNode result = null;
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(queryDsl);
                result = accessModule.selectUnit(queryDsl);
            } else {
                throw new AccessInternalExecutionException(THERE_IS_NO_X_HTTP_METHOD_OVERRIDE_GET_AS_A_HEADER);
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * get units list by query based on identifier
     */
    @Override
    @POST
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("id_unit") String idUnit) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);

        Status status;
        JsonNode result = null;
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(queryDsl);
                SanityChecker.checkParameter(idUnit);
                result = accessModule.selectUnitbyId(queryDsl, idUnit);
            } else {
                throw new AccessInternalExecutionException(THERE_IS_NO_X_HTTP_METHOD_OVERRIDE_GET_AS_A_HEADER);
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryDsl DSK, null not allowed
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @Override
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(JsonNode queryDsl, @PathParam("id_unit") String idUnit) {

        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);

        Status status;
        JsonNode result = null;
        try {
            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(idUnit);
            result = accessModule.updateUnitbyId(queryDsl, idUnit);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, JsonNode query) {
        JsonNode result;
        Status status;
        try {
            SanityChecker.checkJsonAll(query);
            SanityChecker.checkParameter(idObjectGroup);
            result = accessModule.selectObjectGroupById(query, idObjectGroup);
        } catch (final InvalidParseOperationException |IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    @Override
    @POST
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
        @PathParam("id_object_group") String idObjectGroup, JsonNode query) {
        if (!"GET".equalsIgnoreCase(xHttpOverride)) {
            final Status status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        return getObjectGroup(idObjectGroup, query);
    }

    private void asyncObjectStream(AsyncResponse asyncResponse, HttpHeaders headers, String idObjectGroup,
        JsonNode query,
        boolean post) {
        if (post) {
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
                return;
            }
            final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
            if (!HttpMethod.GET.equalsIgnoreCase(xHttpOverride)) {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.METHOD_NOT_ALLOWED).entity(getErrorEntity(Status.METHOD_NOT_ALLOWED)
                        .toString()).build());
                return;
            }
        }
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
            return;
        }
        final String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        final String xTenantId = headers.getRequestHeader(GlobalDataRest.X_TENANT_ID).get(0);
        try {
            SanityChecker.checkHeaders(headers);
            HttpHeaderHelper.checkVitamHeaders(headers);
            SanityChecker.checkJsonAll(query);
            SanityChecker.checkParameter(idObjectGroup);
            accessModule.getOneObjectFromObjectGroup(asyncResponse, idObjectGroup, query, xQualifier,
                Integer.valueOf(xVersion), xTenantId);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString())
                .build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            Response errorResponse =
                Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR)
                    .toString()).build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        } catch (MetaDataNotFoundException | StorageNotFoundException exc) {
            LOGGER.error(exc);
            Response errorResponse =
                Response.status(Status.NOT_FOUND).entity(getErrorEntity(Status.NOT_FOUND).toString()).build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        }
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectStreamAsync(@Context HttpHeaders headers, @PathParam("id_object_group") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                asyncObjectStream(asyncResponse, headers, idObjectGroup, query, false);
            }
        });
    }

    @Override
    @POST
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectStreamPostAsync(@Context HttpHeaders headers,
        @PathParam("id_object_group") String idObjectGroup, JsonNode query,
        @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                asyncObjectStream(asyncResponse, headers, idObjectGroup, query, true);
            }
        });
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setContext(ACCESS_MODULE)

            .setHttpCode(status.getStatusCode()).setState(CODE_VITAM).setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }
}
