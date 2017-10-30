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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;


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

    private final SecureEndpointRegistry secureEndpointRegistry;

    /**
     * Constructor
     *
     * @param secureEndpointRegistry endpoint list registry
     */
    public AccessExternalResourceImpl(SecureEndpointRegistry secureEndpointRegistry) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        LOGGER.debug("AccessExternalResource initialized");
    }

    /**
     * List secured resource end points
     *
     * @return response
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response listResourceEndpoints() {

        String resourcePath = AccessExternalResourceImpl.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = this.secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * get a DIP by dsl query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:read", description = "Récupérer la liste des unités archivistiques")
    public Response getUnits(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse<JsonNode> result = client.selectUnits(queryJson);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Request unauthorized ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits ", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error("No search query specified, this is mandatory", e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * get units list by query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "dipexport:create", description = "Générer le DIP à partir d'un DSL")
    public Response exportDIP(JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
        	SanityChecker.checkJsonAll(queryJson);
            RequestResponse response = client.exportDIP(queryJson);
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
            	return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            return Response.status(Status.PRECONDITION_FAILED).entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error("Technical Exception ", e);
        	return Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage())).build();
        }
    }

    /**
     * get units list by query
     *
     * @param id operationId correponding to the current dip
     * @return Response
     */
    @GET
    @Path("/dipexport/{id}/dip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "dipexport:read", description = "Récupérer le DIP")
    public Response findDIPByID(@PathParam("id") String id) {

        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            Response response = client.findDIPByID(id);
            return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }


    /**
     * get units list by query based on identifier
     *
     * @param queryJson query as String
     * @param idUnit    the id of archive unit to get
     * @return Archive Unit
     */
    @GET
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:read:json",
        description = "Obtenir le détail d'une unité archivistique au format json")
    public Response getUnitById(@Dsl(value = DslSchema.GET_BY_ID) JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        ParametersChecker.checkParameter("unit id is required", idUnit);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(idUnit);
            SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
            selectParserMultiple.parse(queryJson);
            SelectMultiQuery selectMultiQuery = selectParserMultiple.getRequest();
            selectMultiQuery.addRoots(idUnit);
            RequestResponse<JsonNode> result = client.selectUnitbyId(selectMultiQuery.getFinalSelect(), idUnit);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryJson the update query (null not allowed)
     * @param idUnit    units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:update", description = "Réaliser la mise à jour d'une unité archivistique")
    public Response updateUnitById(JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse<JsonNode> response = client.updateUnitbyId(queryJson, idUnit);
            if (!response.isOk() && response instanceof VitamError) {
                VitamError error = (VitamError) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Internal request error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error("Writing permission invalid", e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * check existence of an unit
     *
     * @param idUnit the archive unit id
     * @return check result response
     */
    @HEAD
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:read:status", description = "Vérifier l'existence d'une unité archivistique")
    public Response checkExitsUnitById(@PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, NOT_YET_SUPPORTED)).build();
    }

    /**
     * Retrieve Object group list by query based on identifier of the unit
     *
     * @param headers   the http header defined parameters of request
     * @param unitId    the id of archive unit
     * @param queryJson the query to get object
     * @return Response
     */
    @GET
    @Path("/units/{idu}/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:objects:read:json",
        description = "Télécharger le groupe d'objet technique de l'unité archivistique donnée")
    public Response getObjectGroupMetadataByUnitId(@Context HttpHeaders headers, @PathParam("idu") String unitId,
        JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            String idObjectGroup = idObjectGroup(unitId);
            RequestResponse<JsonNode> result = client.selectObjectbyId(queryJson, idObjectGroup);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }


    /**
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param headers the http header defined parameters of request
     * @param unitId  the id of archive unit
     * @param query   the query to get object
     * @return response
     */
    @GET
    @Path("/units/{idu}/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "units:id:objects:read:binary", description = "Télecharger un objet")
    public Response getDataObjectByUnitId(@Context HttpHeaders headers, @PathParam("idu") String unitId,
        JsonNode query) {

        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        Status status;
        try {
            SanityChecker.checkJsonAll(query);
            String idObjectGroup = idObjectGroup(unitId);
            Integer tenantId = ParameterHelper.getTenantParameter();
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
            MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();
            return asyncObjectStream(multipleMap, idObjectGroup, query);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        }
    }

    private String idObjectGroup(String idu)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException {
        // Select "Object from ArchiveUNit idu
        ParametersChecker.checkParameter("unit id is required", idu);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addUsedProjection("#object");
            RequestResponse<JsonNode> result = client.selectUnitbyId(select.getFinalSelect(), idu);
            JsonNode jsonNode = result.toJsonNode();
            SanityChecker.checkJsonAll(jsonNode);
            return jsonNode.findValue("#object").textValue();
        }
    }

    private Response asyncObjectStream(MultivaluedMap<String, String> multipleMap, String idObjectGroup,
        JsonNode query) {

        try {
            if (!multipleMap.containsKey(GlobalDataRest.X_QUALIFIER) ||
                !multipleMap.containsKey(GlobalDataRest.X_VERSION)) {
                LOGGER.error("At least one required header is missing. Required headers: (" +
                    VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
                return Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorStream(VitamCodeHelper
                        .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                            "QUALIFIER or VERSION missing")
                        .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())))
                    .build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())))
                .build();
        }

        final String xQualifier = multipleMap.get(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = multipleMap.get(GlobalDataRest.X_VERSION).get(0);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(query);
            HttpHeaderHelper.checkVitamHeadersMap(multipleMap);
            final Response response =
                client.getObject(query, idObjectGroup, xQualifier,
                    Integer.valueOf(xVersion));
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            headers.put(GlobalDataRest.X_QUALIFIER, xQualifier);
            headers.put(GlobalDataRest.X_VERSION, xVersion);
            return new VitamAsyncInputStreamResponse(response,
                Status.OK, headers);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        exc.getLocalizedMessage())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())))
                .build();
        } catch (final AccessInternalClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        exc.getLocalizedMessage())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())))
                .build();
        } catch (final AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        exc.getLocalizedMessage())
                    .setHttpCode(Status.NOT_FOUND.getStatusCode())))
                .build();
        } catch (AccessUnauthorizedException exc) {
            return Response.status(Status.UNAUTHORIZED)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        exc.getLocalizedMessage())
                    .setHttpCode(Status.UNAUTHORIZED.getStatusCode())))
                .build();
        }
    }

    @Deprecated
    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    private InputStream getErrorStream(VitamError vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private Response buildErrorFromError(VitamCode vitamCode, String message, VitamError oldVitamError) {
        LOGGER.info("Description: " + message);
        VitamError newVitamError = new VitamError(VitamCodeHelper.getCode(vitamCode))
            .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
            .setMessage(vitamCode.getMessage()).setDescription(message);

        oldVitamError.addToErrors(newVitamError);

        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(oldVitamError).toString())
            .build();
    }

}
