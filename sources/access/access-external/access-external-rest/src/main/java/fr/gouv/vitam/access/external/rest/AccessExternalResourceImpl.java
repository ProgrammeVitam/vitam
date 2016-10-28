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
package fr.gouv.vitam.access.external.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.VitamError;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server2.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessExternalResourceImpl extends ApplicationStatusResource {

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String ACCESS_EXTERNAL_MODULE = "ACCESS_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceImpl.class);

    private int tenantId = 0;

    /**
     *
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessExternalResourceImpl() {
        LOGGER.debug("AccessExternalResource initialized");
    }

    /**
     * get units list by query
     * 
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path("/units")
    public Response getUnits(String queryDsl) {
        Status status;
        JsonNode result = null;
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.getFromString(queryDsl));
            GlobalDatasParser.sanityRequestCheck(queryDsl);
            result = client.selectUnits(queryDsl);
            return Response.status(Status.OK)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(result)
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error("Request unauthorized ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits ", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        }
    }

    /**
     * update units list by query
     * 
     * @param queryDsl
     * @return Response
     */
    @PUT
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnits(String queryDsl) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * get units list by query with POST method
     * 
     * @param queryDsl
     * @param xhttpOverride
     * @return Response
     */
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnits(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        LOGGER.debug("Execution of DSL Vitam from Access ongoing...");
        Status status;
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
                GlobalDatasParser.sanityRequestCheck(queryDsl);
                return getUnits(queryDsl);
            } else {
                status = Status.UNAUTHORIZED;
                return Response.status(status)
                    .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                    .entity(getErrorEntity(status))
                    .build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        }
    }

    /**
     * get units list by query based on identifier
     * 
     * @param queryDsl query as String
     * @param idUnit
     * @return Archive Unit
     */
    @GET
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(String queryDsl, @PathParam("idu") String idUnit) {
        Status status;
        JsonNode result = null;
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        ParametersChecker.checkParameter("unit id is required", idUnit);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
            GlobalDatasParser.sanityRequestCheck(queryDsl);
            result = client.selectUnitbyId(queryDsl, idUnit);
            return Response.status(Status.OK)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(result)
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        }
    }


    /**
     * get units list by query based on identifier
     * 
     * @param queryDsl
     * @param xhttpOverride
     * @param idUnit
     * @return Response
     */
    @POST
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnitById(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("idu") String idUnit) {
        ParametersChecker.checkParameter("unit id is required", idUnit);
        Status status;
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
                GlobalDatasParser.sanityRequestCheck(queryDsl);
                return getUnitById(queryDsl, idUnit);
            } else {
                status = Status.UNAUTHORIZED;
                return Response.status(status)
                    .entity(getErrorEntity(status))
                    .build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
    }

    /**
     * update archive units by Id with Json query
     * 
     * @param queryDsl DSK, null not allowed
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(String queryDsl, @PathParam("idu") String idUnit) {
        Status status;
        JsonNode result = null;
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
            GlobalDatasParser.sanityRequestCheck(queryDsl);
            result = client.updateUnitbyId(queryDsl, idUnit);
            return Response.status(Status.OK)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(result)
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        }
    }

    /**
     * check existence of an unit
     * 
     * @param idUnit
     * @return check result
     */
    @HEAD
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkExitsUnitById(@PathParam("idu") String idUnit) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * get object group list by query and id
     * 
     * @param idObjectGroup
     * @param query
     * @return Response
     */

    @GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("ido") String idObjectGroup, String query) {
        JsonNode result;
        Status status;
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        try {
            ParametersChecker.checkParameter("Must have a dsl query", query);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(query));
            try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
                result = client.selectObjectbyId(query, idObjectGroup);
                return Response.status(Status.OK)
                    .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                    .entity(result)
                    .build();
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        }
    }

    /**
     * @param headers
     * @param idObjectGroup
     * @param query
     * @return Response
     */
    @POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupPost(@Context HttpHeaders headers,
        @PathParam("ido") String idObjectGroup, String query) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build();
        }
        GUID xRequestId = GUIDFactory.newRequestIdGUID(tenantId);
        final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
        if ((xHttpOverride == null) || !"GET".equalsIgnoreCase(xHttpOverride)) {
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, xRequestId)
                .entity(getErrorEntity(status))
                .build();
        } else {
            return getObjectGroup(idObjectGroup, query);
        }
    }

    /**
     * @param headers
     * @param idObjectGroup
     * @param query
     * @param asyncResponse
     */
    @GET
    @Path("/units/{ido}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObject(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        String query, @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getInstance().execute(new Runnable() {

            @Override
            public void run() {
                asyncObjectStream(asyncResponse, headers, idObjectGroup, query, false);
            }
        });
    }


    /**
     * @param headers
     * @param idObjectGroup
     * @param query
     * @param asyncResponse
     */
    @POST
    @Path("/units/{ido}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectPost(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        String query, @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getInstance().execute(new Runnable() {

            @Override
            public void run() {
                asyncObjectStream(asyncResponse, headers, idObjectGroup, query, true);
            }
        });
    }


    /**
     * get object group list by query
     * 
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectsList(String queryDsl) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * @param xhttpOverride
     * @param query
     * @return Response
     */
    @POST
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectListPost(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        String query) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    private RequestResponseError getErrorEntity(Status status) {
        return new RequestResponseError()
            .setError(new VitamError(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
                .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase()));
    }

    private void asyncObjectStream(AsyncResponse asyncResponse, HttpHeaders headers, String idObjectGroup, String query,
        boolean post) {
        if (post) {
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
            }
            final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
            if (!HttpMethod.GET.equalsIgnoreCase(xHttpOverride)) {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.METHOD_NOT_ALLOWED).entity(getErrorEntity(Status.METHOD_NOT_ALLOWED)
                        .toString()).build());
            }
        }
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" +
                VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
        }
        final String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        // FIXME To be passed to client
        @SuppressWarnings("unused")
        final String xTenantId = "0";
        AsyncInputStreamHelper helper = null;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkHeaders(headers);
            GlobalDatasParser.sanityRequestCheck(query);
            final JsonNode queryJson = JsonHandler.getFromString(query);
            HttpHeaderHelper.checkVitamHeaders(headers);
            SanityChecker.checkJsonAll(queryJson);
            SanityChecker.checkParameter(idObjectGroup);
            Response response =
                client.getObject(idObjectGroup, query, xQualifier,
                    Integer.valueOf(xVersion));
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            ResponseBuilder responseBuilder = Response.status(Status.OK).header(GlobalDataRest.X_QUALIFIER, xQualifier)
                .header(GlobalDataRest.X_VERSION, xVersion)
                .header("Content-Disposition", response.getHeaderString("Content-Disposition"))
                .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString())
                .build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        } catch (final AccessInternalClientServerException | AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            Response errorResponse =
                Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR)
                    .toString()).build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        }
    }
}
