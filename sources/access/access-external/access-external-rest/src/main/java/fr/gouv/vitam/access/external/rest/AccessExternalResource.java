/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.access.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.dsl.schema.validator.BatchProcessingQuerySchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.DslValidator;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

@Path("/access-external/v1")
public class AccessExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResource.class);

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String ACCESS_EXTERNAL_MODULE = "ACCESS_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";
    private static final String CONTRACT_ACCESS_NOT_ALLOW = "Contract access does not allow ";
    private static final String UNIT_NOT_FOUND = "Unit not found";
    private static final String REQ_RES_DOES_NOT_EXIST = "Request resource does not exist";
    private static final String OBJECT_TAG = "#object";
    private static final String REQUEST_UNAUTHORIZED = "Request unauthorized ";
    private static final String NO_SEARCH_QUERY = "No search query specified, this is mandatory";
    private static final String TECHNICAL_EXCEPTION = "Technical Exception ";
    private static final String COULD_NOT_VALIDATE_REQUEST = "Could not validate request";
    private static final String REQUEST_RESOURCES_DOES_NOT_EXISTS = "Request resources does not exits";
    private static final String WRITING_PERMISSIONS_INVALID = "Writing permission invalid";
    private static final String ERROR_ON_PRESERVATION = "Error on preservation request";

    private final SecureEndpointRegistry secureEndpointRegistry;
    private final AccessInternalClientFactory accessInternalClientFactory;

    AccessExternalResource(SecureEndpointRegistry secureEndpointRegistry) {
        this(secureEndpointRegistry, AccessInternalClientFactory.getInstance());
    }

    @VisibleForTesting
    AccessExternalResource(SecureEndpointRegistry secureEndpointRegistry,
        AccessInternalClientFactory accessInternalClientFactory) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.accessInternalClientFactory = accessInternalClientFactory;
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

        String resourcePath = AccessExternalResource.class.getAnnotation(Path.class).value();

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
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse<JsonNode> result = client.selectUnits(queryJson);

            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(REQ_RES_DOES_NOT_EXIST, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(NO_SEARCH_QUERY, e);
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
    public Response exportDIP(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse response = client.exportDIP(queryJson);
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
                return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(TECHNICAL_EXCEPTION, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage())).build();
        }
    }

    /**
     * Performs a reclassification workflow.
     *
     * @param queryJson List of reclassification DSL queries.
     * @return Response
     */
    @POST
    @Path("/reclassification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "reclassification:update", description = "Mise à jour d'arborescence des unités archivistiques")
    public Response reclassification(@Dsl(DslSchema.RECLASSIFICATION_QUERY) JsonNode queryJson) {

        ParametersChecker.checkParameter("Missing reclassification request", queryJson);

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse response = client.reclassification(queryJson);
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
                return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(Status.PRECONDITION_FAILED.getReasonPhrase(), e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(TECHNICAL_EXCEPTION, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage())).build();
        }
    }

    /**
     * Performs an elimination analysis workflow.
     *
     * @param eliminationRequestBody object that contain dsl request and a given date.
     * @return Response
     */
    @POST
    @Path("/elimination/analysis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "elimination:analysis", description = "Analyse de l'élimination d'unités archivistiques")
    public Response startEliminationAnalysis(EliminationRequestBody eliminationRequestBody) {

        try {

            ParametersChecker.checkParameter("Missing dslRequest request", eliminationRequestBody.getDslRequest());
            ParametersChecker.checkDateParam("Bad formatted date", eliminationRequestBody.getDate());

            BatchProcessingQuerySchemaValidator validator =
                new BatchProcessingQuerySchemaValidator();
            validator.validate(eliminationRequestBody.getDslRequest());

        } catch (IllegalArgumentException | IOException | ValidationException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        }

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse response = client.startEliminationAnalysis(eliminationRequestBody);
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
                return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(Status.PRECONDITION_FAILED.getReasonPhrase(), e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(TECHNICAL_EXCEPTION, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage())).build();
        }
    }

    /**
     * Performs an elimination action workflow.
     *
     * @param eliminationRequestBody object that contain dsl request and a given date.
     * @return Response
     */
    @POST
    @Path("/elimination/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "elimination:action", description = "Elimination définitive d'unités archivistiques")
    public Response startEliminationAction(EliminationRequestBody eliminationRequestBody) {

        try {

            ParametersChecker.checkParameter("Missing dslRequest request", eliminationRequestBody.getDslRequest());
            ParametersChecker.checkDateParam("Bad formatted date", eliminationRequestBody.getDate());

            BatchProcessingQuerySchemaValidator validator =
                new BatchProcessingQuerySchemaValidator();
            validator.validate(eliminationRequestBody.getDslRequest());

        } catch (IllegalArgumentException | IOException | ValidationException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        }

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse response = client.startEliminationAction(eliminationRequestBody);
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
                return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(Status.PRECONDITION_FAILED.getReasonPhrase(), e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(TECHNICAL_EXCEPTION, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage())).build();
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
    @Secured(permission = "dipexport:id:dip:read", description = "Récupérer le DIP")
    public Response findDIPByID(@PathParam("id") String id) {

        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            Response response = client.findDIPByID(id);
            return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }


    /**
     * get units list by query based on identifier
     *
     * @param queryJson query as String
     * @param idUnit the id of archive unit to get
     * @return Archive Unit
     */
    @GET
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:read:json",
        description = "Obtenir le détail d'une unité archivistique au format json")
    public Response getUnitById(@Dsl(value = DslSchema.GET_BY_ID) JsonNode queryJson, @PathParam("idu") String idUnit) {
        ParametersChecker.checkParameter("unit id is required", idUnit);
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(idUnit);
            SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
            selectParserMultiple.parse(queryJson);
            SelectMultiQuery selectMultiQuery = selectParserMultiple.getRequest();
            selectMultiQuery.addQueries(eq(VitamFieldsHelper.id(), idUnit));
            RequestResponse<JsonNode> result = client.selectUnits(selectMultiQuery.getFinalSelect());
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            // FIXME hack for bug in Metadata when DSL contains unexisting root id without query
            if (((RequestResponseOK<JsonNode>) result).getResults() == null ||
                    ((RequestResponseOK<JsonNode>) result).getResults().isEmpty()) {
                throw new AccessInternalClientNotFoundException(UNIT_NOT_FOUND);
            }

            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException | BadRequestException e) {
            LOGGER.debug(PREDICATES_FAILED_EXCEPTION, e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                e.getLocalizedMessage()).setHttpCode(Status.PRECONDITION_FAILED.getStatusCode()).toResponse();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                e.getLocalizedMessage()).setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).toResponse();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.debug(REQUEST_RESOURCES_DOES_NOT_EXISTS, e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                e.getLocalizedMessage()).setHttpCode(Status.NOT_FOUND.getStatusCode()).toResponse();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR,
                e.getLocalizedMessage()).setHttpCode(Status.UNAUTHORIZED.getStatusCode()).toResponse();
        }
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryJson the update query (null not allowed)
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:id:update", description = "Réaliser la mise à jour d'une unité archivistique")
    public Response updateUnitById(@Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryJson, @PathParam("idu") String idUnit) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
            updateParserMultiple.parse(queryJson);
            UpdateMultiQuery updateMultiQuery = updateParserMultiple.getRequest();
            updateMultiQuery.addRoots(idUnit);
            RequestResponse<JsonNode> response = client.updateUnitbyId(updateMultiQuery.getFinalUpdate(), idUnit);
            if (!response.isOk() && response instanceof VitamError) {
                VitamError error = (VitamError) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.debug(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.debug(Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.debug(REQUEST_RESOURCES_DOES_NOT_EXISTS, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UNIT_NOT_FOUND,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.debug(WRITING_PERMISSIONS_INVALID, e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.debug(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * Retrieve Object group list by query based on identifier of the unit
     *
     * @param headers the http header defined parameters of request
     * @param unitId the id of archive unit
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
        @Dsl(value = DslSchema.GET_BY_ID) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            String idObjectGroup = idObjectGroup(unitId);
            if (idObjectGroup == null) {
                throw new AccessInternalClientNotFoundException("ObjectGroup of Unit not found");
            }
            RequestResponse<JsonNode> result = client.selectObjectbyId(queryJson, idObjectGroup);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();

            if (!result.isOk()) {
                VitamError error = (VitamError) result;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR, error.getMessage(),
                    error);
            } else if (((RequestResponseOK<JsonNode>) result).getResults() == null ||
                ((RequestResponseOK<JsonNode>) result).getResults().isEmpty()) {
                throw new AccessInternalClientNotFoundException(UNIT_NOT_FOUND);
            }

            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException | IllegalArgumentException |
                BadRequestException | InvalidCreateOperationException e) {
            LOGGER.debug(e);
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
            LOGGER.debug(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
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
     * @param unitId the id of archive unit
     * @return response
     */
    @GET
    @Path("/units/{idu}/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "units:id:objects:read:binary", description = "Télecharger un objet")
    public Response getDataObjectByUnitId(@Context HttpHeaders headers, @PathParam("idu") String unitId) {

        Status status;
        try {
            String idObjectGroup = idObjectGroup(unitId);
            if (idObjectGroup == null) {
                throw new AccessInternalClientNotFoundException("ObjectGroup of Unit not found");
            }
            MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();
            return asyncObjectStream(multipleMap, idObjectGroup, unitId);
        } catch (final InvalidParseOperationException | BadRequestException | InvalidCreateOperationException e) {
            LOGGER.debug(PREDICATES_FAILED_EXCEPTION, e);
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
            LOGGER.debug(REQUEST_RESOURCES_DOES_NOT_EXISTS, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        }
    }

    /**
     * Mass update of archive units with json query.
     *
     * @param queryJson the mass_update query (null not allowed)
     * @return
     */
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:update", description = "Mise à jour en masse des unités archivistiques")
    public Response massUpdateUnits(@Dsl(DslSchema.MASS_UPDATE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
            updateParserMultiple.parse(queryJson);
            UpdateMultiQuery updateMultiQuery = updateParserMultiple.getRequest();
            RequestResponse<JsonNode> response = client.updateUnits(updateMultiQuery.getFinalUpdate());

            if (!response.isOk() && response instanceof VitamError) {
                VitamError error = (VitamError) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Internal request error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error(WRITING_PERMISSIONS_INVALID, e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * Mass update of archive units rules with json request.
     *
     * @param massUpdateUnitRuleRequest the mass update rules request (null not allowed)
     * @return
     */
    @POST
    @Path("/units/rules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "units:rules:update", description = "Mise à jour en masse des règles de gestion")
    public Response massUpdateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest) {
        Status status;
        // Manually schema validation of DSL Query
        try {
            DslValidator validator = new BatchProcessingQuerySchemaValidator();
            validator.validate(massUpdateUnitRuleRequest.getDslRequest());
        } catch (ValidationException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return e.getVitamError().toResponse();
        } catch (IOException e) {
            LOGGER.warn("Can not read Dsl query", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR,
                    "Can not read Dsl query").setHttpCode(status.getStatusCode())).build();
        }

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> response = client.updateUnitsRules(massUpdateUnitRuleRequest);

            if (!response.isOk() && response instanceof VitamError) {
                VitamError error = (VitamError) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Internal request error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error(WRITING_PERMISSIONS_INVALID, e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_MASS_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * Select units with inherited rules
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/unitsWithInheritedRules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "unitsWithInheritedRules:read", description = "Récupérer la liste des unités archivistiques avec leurs règles de gestion héritées")
    public Response selectUnitsWithInheritedRules(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> result = client.selectUnitsWithInheritedRules(queryJson);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(REQ_RES_DOES_NOT_EXIST, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(NO_SEARCH_QUERY, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    private String idObjectGroup(String idu)
            throws InvalidParseOperationException, AccessInternalClientServerException,
            AccessInternalClientNotFoundException, AccessUnauthorizedException,
            BadRequestException, InvalidCreateOperationException {
        // Select "Object from ArchiveUNit idu
        ParametersChecker.checkParameter("unit id is required", idu);
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addUsedProjection(OBJECT_TAG);
            select.addQueries(eq(VitamFieldsHelper.id(), idu));
            RequestResponse<JsonNode> response = client.selectUnits(select.getFinalSelect());
            SanityChecker.checkJsonAll(response.toJsonNode());
            if (response.isOk()) {
                JsonNode unit = ((RequestResponseOK<JsonNode>) response).getFirstResult();
                if (unit == null || unit.findValue(OBJECT_TAG) == null) {
                    throw new AccessInternalClientNotFoundException("Unit with objectGroup not found");
                } else {
                    return unit.findValue(OBJECT_TAG).textValue();
                }
            } else {
                throw new AccessInternalClientNotFoundException(UNIT_NOT_FOUND);
            }
        }
    }

    private Response asyncObjectStream(MultivaluedMap<String, String> multipleMap, String idObjectGroup,
        String unitId) {

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
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            HttpHeaderHelper.checkVitamHeadersMap(multipleMap);
            final Response response =
                client.getObject(idObjectGroup, xQualifier, Integer.valueOf(xVersion), unitId);
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
            LOGGER.error(exc.getMessage(), exc);
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
            LOGGER.error(e.getMessage(), e);
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

    /**
     * get Objects group list based on DSL query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "objects:read", description = "Récupérer la liste des groupes d'objets")
    public Response getObjects(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse<JsonNode> result = client.selectObjects(queryJson);

            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(REQ_RES_DOES_NOT_EXIST, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error(NO_SEARCH_QUERY, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    @Path("/storageaccesslog")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "storageaccesslog:read:binary", description = "Télécharger les journaux d'accès")
    public Response getAccessLog(JsonNode params) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            final Response response = client.downloadAccessLogFile(params);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            return new VitamAsyncInputStreamResponse(response,
                Status.OK, headers);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESS_LOG,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESS_LOG,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(REQ_RES_DOES_NOT_EXIST, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESS_LOG,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESS_LOG,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    @Path("/preservation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "preservation:update", description = "Lancer le processus de préservation")
    public Response startPreservation(PreservationRequest preservationRequest) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.startPreservation(preservationRequest);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error("Error on preservation request", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }
}
