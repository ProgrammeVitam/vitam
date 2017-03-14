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

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessExternalResourceImpl extends ApplicationStatusResource {


    // FIXME P0 : Add a filter to protect the tenantId (check if it exists + return Unauthorized response or so). @see :
    // AuthorizationFilter
    private static final String ORIGINATING_AGENCY = "OriginatingAgency";
    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String ACCESS_EXTERNAL_MODULE = "ACCESS_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceImpl.class);

    /**
     * Constructor
     */
    public AccessExternalResourceImpl() {
        LOGGER.debug("AccessExternalResource initialized");
    }

    /**
     * get units list by query
     *
     * @param queryJson
     * @return Response
     */
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        JsonNode result = null;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.selectUnits(queryJson);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Request unauthorized ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits ", e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * get units list by query with POST method
     *
     * @param queryJson
     * @param xhttpOverride
     * @return Response
     */
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnits(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        LOGGER.debug("Execution of DSL Vitam from Access ongoing...");
        Status status;
        if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
            return getUnits(queryJson);
        } else {
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status)).build();
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
    public Response updateUnits(JsonNode queryDsl) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * get units list by query based on identifier
     *
     * @param queryJson query as String
     * @param idUnit
     * @return Archive Unit
     */
    @GET
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        JsonNode result = null;
        ParametersChecker.checkParameter("unit id is required", idUnit);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.selectUnitbyId(queryJson, idUnit);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * get units list by query based on identifier
     *
     * @param queryJson
     * @param xhttpOverride
     * @param idUnit
     * @return Response
     */
    @POST
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnitById(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        ParametersChecker.checkParameter("unit id is required", idUnit);
        Status status;
        if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
            return getUnitById(queryJson, idUnit);
        } else {
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryJson null not allowed
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        JsonNode result = null;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.updateUnitbyId(queryJson, idUnit);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status)).build();
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
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * get object group list by query and id
     *
     * @param idObjectGroup
     * @param queryJson
     * @return Response
     */
    @GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("ido") String idObjectGroup, JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        JsonNode result;
        Status status;
        try {
            try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
                result = client.selectObjectbyId(queryJson, idObjectGroup);
                return Response.status(Status.OK).entity(RequestResponseOK.getFromJsonNode(result)).build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * @param headers
     * @param idObjectGroup
     * @param query
     * @param asyncResponse
     * @return Response
     */
    @GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectIdoGet(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        getObject(headers, idObjectGroup, query, asyncResponse, false);
    }

    /**
     * @param headers
     * @param idObjectGroup
     * @param queryJson
     * @return Response
     */
    @POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupPost(@Context HttpHeaders headers,
        @PathParam("ido") String idObjectGroup, JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
        if (xHttpOverride == null || !"GET".equalsIgnoreCase(xHttpOverride)) {
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } else {
            return getObjectGroup(idObjectGroup, queryJson);
        }
    }

    /**
     * @param headers
     * @param idObjectGroup
     * @param query
     * @param asyncResponse
     * @return Response
     */
    @POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectIdoPost(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        getObject(headers, idObjectGroup, query, asyncResponse, true);
    }


    /**
     * @param headers
     * @param idu
     * @param query
     * @param asyncResponse
     */
    @GET
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObject(@Context HttpHeaders headers, @PathParam("idu") String idu,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        Status status;
        try {
            String idObjectGroup = idObjectGroup(idu);
            getObject(headers, idObjectGroup, query, asyncResponse, false);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.UNAUTHORIZED;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        }
    }



    /**
     * @param headers
     * @param idu
     * @param query
     * @param asyncResponse
     */
    @POST
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectPost(@Context HttpHeaders headers, @PathParam("idu") String idu,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        Status status;
        try {
            String idObjectGroup = idObjectGroup(idu);
            getObject(headers, idObjectGroup, query, asyncResponse, true);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.UNAUTHORIZED;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                Response.status(status).build());
        }
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
    public Response getObjectsList(JsonNode queryDsl) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        JsonNode query) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
    }

    private String idObjectGroup(String idu)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        // Select "Object from ArchiveUNit idu
        JsonNode result = null;
        ParametersChecker.checkParameter("unit id is required", idu);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.addUsedProjection("#object");
            result = client.selectUnitbyId(select.getFinalSelect(), idu);
            SanityChecker.checkJsonAll(result);
            return result.findValue("#object").textValue();
        }
    }

    private void getObject(HttpHeaders headers, String idObjectGroup,
        JsonNode query, final AsyncResponse asyncResponse, boolean post) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncObjectStream(asyncResponse, headers, idObjectGroup, query, post));
    }

    private void asyncObjectStream(AsyncResponse asyncResponse, HttpHeaders headers, String idObjectGroup,
        JsonNode query, boolean post) {

        try {
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
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
                !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
                LOGGER.error("At least one required header is missing. Required headers: (" +
                    VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
                return;
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString())
                .build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
            return;
        }

        final String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        AsyncInputStreamHelper helper;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            HttpHeaderHelper.checkVitamHeaders(headers);
            final Response response =
                client.getObject(query, idObjectGroup, xQualifier,
                    Integer.valueOf(xVersion));
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder =
                Response.status(Status.OK).header(GlobalDataRest.X_QUALIFIER, xQualifier)
                    .header(GlobalDataRest.X_VERSION, xVersion)
                    .header("Content-Disposition", response.getHeaderString("Content-Disposition"))
                    .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString())
                .build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        } catch (final AccessInternalClientServerException | AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse =
                Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR)
                    .toString()).build();
            AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse, errorResponse);
        }
    }


    /**
     * findDocuments
     *
     * @param select
     * @param xhttpOverride
     * @return Response
     */
    @Path("/accession-register")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegister(JsonNode select,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));

        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(status).build();
        }
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final RequestResponse result = client.getAccessionRegister(select);
            return Response.status(Status.OK).entity(result).build();
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.OK).entity(new RequestResponseOK()).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }



    /**
     * findDocumentByID
     *
     * @param documentId
     * @return Response
     */
    @POST
    @Path("/accession-register/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterById(@PathParam("id_document") String documentId) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }


    /**
     * findAccessionRegisterDetail
     *
     * @param documentId
     * @param select
     * @param xhttpOverride
     * @return Response
     */
    @POST
    @Path("/accession-register/{id_document}/accession-register-detail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId, JsonNode select,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));

        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        ParametersChecker.checkParameter("accession register id is a mandatory parameter", documentId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final SelectParserSingle parser = new SelectParserSingle();
            parser.parse(select);
            parser.addCondition(eq(ORIGINATING_AGENCY, URLDecoder.decode(documentId, CharsetUtils.UTF_8)));
            RequestResponse accessionRegisterDetail =
                client.getAccessionRegisterDetail(parser.getRequest().getFinalSelect());
            return Response.status(Status.OK).entity(accessionRegisterDetail).build();
        } catch (final ReferentialNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK()).build();
        } catch (InvalidParseOperationException | UnsupportedEncodingException |
            InvalidCreateOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

}
