/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientIllegalOperationException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
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
import fr.gouv.vitam.common.dsl.schema.validator.SelectMultipleSchemaValidator;
import fr.gouv.vitam.common.error.DomainName;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.ExpectationFailedClientException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.common.model.revertupdate.RevertUpdateOptions;
import fr.gouv.vitam.common.model.storage.AccessRequestReference;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.StatusByAccessRequest;
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
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.CollectionUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.GlobalDataRest.X_ACCESS_CONTRAT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTENT_LENGTH;
import static fr.gouv.vitam.common.GlobalDataRest.X_OBJECTS_COUNT;
import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_UNITS_COUNT;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nestedSearch;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.ACCESS_REQUESTS_CHECK;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.ACCESS_REQUESTS_REMOVE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.COMPUTEINHERITEDRULES_ACTION;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.COMPUTEINHERITEDRULES_DELETE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.DELETE_GOT_VERSIONS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.DIPEXPORT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.DIPEXPORT_ID_DIP_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.ELIMINATION_ACTION;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.ELIMINATION_ANALYSIS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.OBJECTS_PERSISTENT_IDENTIFIER_OBJECTS_READ_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.OBJECTS_PERSISTENT_IDENTIFIER_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.OBJECTS_PERSISTENT_IDENTIFIER_READ_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.OBJECTS_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.OBJECTS_STREAM;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PRESERVATION_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.RECLASSIFICATION_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.REVERT_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.STORAGEACCESSLOG_READ_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSFERS_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSFERS_ID_SIP_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSFERS_REPLY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITSWITHINHERITEDRULES_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_BULK_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_ID_OBJECTS_ACCESS_REQUESTS_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_ID_OBJECTS_READ_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_ID_OBJECTS_READ_JSON;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_ID_READ_JSON;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_ID_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_PERSISTENT_IDENTIFIER_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_RULES_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_STREAM;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.UNITS_UPDATE;
import static io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER;

@Path("/access-external/v1")
@Tag(name = "Access")
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
    private static final String UNITS_COUNT_EXCEED_THRESHOLD = "The number of units exceed threshold";
    private static final String OBJECTS_COUNT_EXCEED_THRESHOLD = "The number of objects exceed threshold";
    private static final String STREAM_UNITS_LIMIT_REACHED = "You reach the limit of stream units. try next day";
    private static final String STREAM_OBJECTS_LIMIT_REACHED = "You reach the limit of stream objects. try next day";
    private static final String TECHNICAL_EXCEPTION = "Technical Exception ";
    private static final String COULD_NOT_VALIDATE_REQUEST = "Could not validate request";
    private static final String REQUEST_RESOURCES_DOES_NOT_EXISTS = "Request resources does not exits";
    private static final String WRITING_PERMISSIONS_INVALID = "Writing permission invalid";
    private static final String ERROR_ON_PRESERVATION = "Error on preservation request";
    private static final String UNAUTHORIZED_DSL_PARAMETER = "DSL parameter is unauthorized";

    private static final String QUALIFIERS = "#qualifiers";
    private static final String VERSIONS = "versions";
    private static final String UNIT_UPS = "#unitups";
    private static final String PERSISTENT_IDENTIFIERS = "PersistentIdentifier";
    private static final String PERSISTENT_IDENTIFIER_CONTENT = "PersistentIdentifierContent";
    public static final String DATA_OBJECT_VERSION = "DataObjectVersion";

    private final SecureEndpointRegistry secureEndpointRegistry;
    private final AccessInternalClientFactory accessInternalClientFactory;
    private final AccessExternalConfiguration configuration;

    private Map<Integer, List<String>> objectGroupBlackListedFieldsForVisualizationByTenant;

    AccessExternalResource(SecureEndpointRegistry secureEndpointRegistry, AccessExternalConfiguration configuration) {
        this(secureEndpointRegistry, AccessInternalClientFactory.getInstance(), configuration);
    }

    @VisibleForTesting
    AccessExternalResource(SecureEndpointRegistry secureEndpointRegistry,
        AccessInternalClientFactory accessInternalClientFactory,
        AccessExternalConfiguration configuration) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.accessInternalClientFactory = accessInternalClientFactory;
        this.configuration = configuration;
        this.objectGroupBlackListedFieldsForVisualizationByTenant =
            configuration.getObjectGroupBlackListedFieldsForVisualizationByTenant();
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
     * get units list by query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = UNITS_READ, description = "Récupérer la liste des unités archivistiques")
    @Operation(
        description = "Requête qui retourne des résultats contenant des Unités d'archives. La requête utilise le langage de requête DSL de type **recherche multiple (SELECT MULTIPLE)** de Vitam en entrée et retourne une liste d'Unités d'archives selon le DSL Vitam en cas de succès.",
        requestBody = @RequestBody(description = "A SELECT MULTIPLE query.", content = @Content(examples = @ExampleObject("{\"$projection\":{\"$fields\":{\"#id\":1}}}"))),
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie la liste des résultats d'Unités d'archives correspondant à la requête DSL", content = @Content(examples = @ExampleObject("{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":125,\"size\":1},\"$results\":[{\"#id\":\"aeaqaaaaaahftfesaabpcalqddm7ndiaaacq\"}],\"$facetResults\":[],\"$context\":{\"$roots\":[],\"$query\":[{\"$or\":[{\"$match\":{\"Title\":\"ratp\"}},{\"$match\":{\"Title_.fr\":\"ratp\"}},{\"$match\":{\"Description\":\"ratp\"}}]}],\"$filter\":{\"$orderby\":{\"TransactedDate\":1}},\"$projection\":{\"$fields\":{\"#id\":1}},\"$facets\":[]}}"))),
            @ApiResponse(responseCode = "412", description = "Precondition failed."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "401", description = "Un authorized."),
            @ApiResponse(responseCode = "400", description = "Bad request.")
        }
    )
    public Response getUnits(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            SelectMultipleSchemaValidator.checkAuthorizeTrackTotalHits(queryJson,
                configuration.isAuthorizeTrackTotalHits());

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
        } catch (ValidationException e) {
            LOGGER.error(UNAUTHORIZED_DSL_PARAMETER, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }


    /**
     * Retrieve archive units by unit persistent identifier
     *
     * @param persistentIdentifier Persistent Identifier value, the name should be persistentIdentifier to accept the custom validation, otherwise, ark format identifier will not be accepted
     * @param queryJson the query to get object
     * @return response
     */

    @GET
    @Path("/units/byunitspersistentidentifier/{persistentIdentifier:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(permission = UNITS_PERSISTENT_IDENTIFIER_READ, description = "Récupérer une unité archivistique par identifiant pérenne")
    @Operation(
        description = "Requête qui retourne une unité d'archives correspondante à l'identifiant pérenne fourni en entrée.",
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie l'Unité d'archives correspondant à l'identifiant pérenne fourni.", content = @Content(examples = @ExampleObject("{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":125,\"size\":1},\"$results\":[{\"#id\":\"aeaqaaaaaahftfesaabpcalqddm7ndiaaacq\"}],\"$facetResults\":[],\"$context\":{\"$roots\":[],\"$query\":[{\"$or\":[{\"$match\":{\"Title\":\"ratp\"}},{\"$match\":{\"Title_.fr\":\"ratp\"}},{\"$match\":{\"Description\":\"ratp\"}}]}],\"$filter\":{\"$orderby\":{\"TransactedDate\":1}},\"$projection\":{\"$fields\":{\"#id\":1}},\"$facets\":[]}}"))),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "301", description = "Moved to another system."),
            @ApiResponse(responseCode = "410", description = "Deleted."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error."),
            @ApiResponse(responseCode = "401", description = "Un authorized.")
        }
    )
    public Response getUnitByPersistentIdentifier(
        @PathParam("persistentIdentifier") String persistentIdentifier,
        @Dsl(value = DslSchema.GET_BY_ID) JsonNode queryJson) {
        try {
            SanityChecker.checkJsonAll(queryJson);

            Status status;
            try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
                // Create a DSL query to retrieve the unit by ARK identifier
                SanityChecker.checkParameter(persistentIdentifier);
                SelectParserMultiple query = new SelectParserMultiple();
                query.parse(queryJson);
                SelectMultiQuery selectMultiQuery = query.getRequest();
                selectMultiQuery.addQueries(
                    QueryHelper.eq(PERSISTENT_IDENTIFIERS + "." + PERSISTENT_IDENTIFIER_CONTENT, persistentIdentifier));

                RequestResponse<JsonNode> result = client.selectUnits(selectMultiQuery.getFinalSelect());
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();

                return Response.status(st).entity(result).build();
            } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
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
        } catch (InvalidParseOperationException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        }
    }



    /**
     * get units list by query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/units/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = UNITS_STREAM, description = "Récupérer la liste des unités archivistiques")
    @Operation(
        description = "Requête qui retourne un stream des résultats contenant des Unités d'archives. La requête utilise le langage de requête DSL de type **recherche multiple (SELECT MULTIPLE)** de Vitam en entrée et retourne une liste d'Unités d'archives selon le DSL Vitam en cas de succès.",
        requestBody = @RequestBody(description = "A SELECT MULTIPLE query.", content = @Content(examples = @ExampleObject("{\"$projection\":{\"$fields\":{\"#id\":1}}}"))),
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie un stream contient la liste des résultats d'Unités d'archives correspondant à la requête DSL", content = @Content(examples = @ExampleObject("{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":125,\"size\":1},\"$results\":[{\"#id\":\"aeaqaaaaaahftfesaabpcalqddm7ndiaaacq\"}],\"$facetResults\":[],\"$context\":{\"$roots\":[],\"$query\":[{\"$or\":[{\"$match\":{\"Title\":\"ratp\"}},{\"$match\":{\"Title_.fr\":\"ratp\"}},{\"$match\":{\"Description\":\"ratp\"}}]}],\"$filter\":{\"$orderby\":{\"TransactedDate\":1}},\"$projection\":{\"$fields\":{\"#id\":1}},\"$facets\":[]}}"))),
            @ApiResponse(responseCode = "417", description = "Expectation Failed."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
        }
    )
    public Response streamUnits(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);

            SelectMultipleSchemaValidator.validateStreamQuery(queryJson);

            final Response response = client.streamUnits(queryJson);

            final HashMap<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            headers.put(X_UNITS_COUNT, response.getHeaderString(X_UNITS_COUNT));
            headers.put(X_CONTENT_LENGTH, response.getHeaderString(X_CONTENT_LENGTH));
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (ValidationException e) {
            LOGGER.error(UNAUTHORIZED_DSL_PARAMETER, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).build();
        } catch (ExpectationFailedClientException e) {
            LOGGER.error(UNITS_COUNT_EXCEED_THRESHOLD, e);
            status = Status.EXPECTATION_FAILED;
            return Response.status(status).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(STREAM_UNITS_LIMIT_REACHED, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).build();
        }
    }

    /**
     * get objects list by query
     *
     * @param queryJson the query to get objects
     * @return Response
     */
    @GET
    @Path("/objects/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = OBJECTS_STREAM, description = "Récupérer la liste des objects groups")
    @Operation(
        description = "Requête qui retourne un stream des résultats contenant des Group d'objets. La requête utilise le langage de requête DSL de type **recherche multiple (SELECT MULTIPLE)** de Vitam en entrée et retourne une liste d'objects selon le DSL Vitam en cas de succès.",
        requestBody = @RequestBody(description = "A SELECT MULTIPLE query.", content = @Content(examples = @ExampleObject("{\"$projection\":{\"$fields\":{\"#id\":1}}}"))),
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie un stream contient la liste des résultats d'objets correspondant à la requête DSL", content = @Content(examples = @ExampleObject("{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":125,\"size\":1},\"$results\":[{\"#id\":\"aeaqaaaaaahftfesaabpcalqddm7ndiaaacq\"}],\"$facetResults\":[],\"$context\":{\"$roots\":[],\"$query\":[{\"$or\":[{\"$match\":{\"Title\":\"ratp\"}},{\"$match\":{\"Title_.fr\":\"ratp\"}},{\"$match\":{\"Description\":\"ratp\"}}]}],\"$filter\":{\"$orderby\":{\"TransactedDate\":1}},\"$projection\":{\"$fields\":{\"#id\":1}},\"$facets\":[]}}"))),
            @ApiResponse(responseCode = "417", description = "Expectation Failed."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
        }
    )
    public Response streamObjects(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            SelectMultipleSchemaValidator.validateStreamQuery(queryJson);
            final Response response = client.streamObjects(queryJson);
            final HashMap<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            headers.put(X_OBJECTS_COUNT, response.getHeaderString(X_OBJECTS_COUNT));
            headers.put(X_CONTENT_LENGTH, response.getHeaderString(X_CONTENT_LENGTH));
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (ValidationException e) {
            LOGGER.error(UNAUTHORIZED_DSL_PARAMETER, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Status.PRECONDITION_FAILED).build();
        } catch (ExpectationFailedClientException e) {
            LOGGER.error(OBJECTS_COUNT_EXCEED_THRESHOLD, e);
            return Response.status(Status.EXPECTATION_FAILED).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(STREAM_OBJECTS_LIMIT_REACHED, e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(REQUEST_UNAUTHORIZED, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * get a DIP by dsl query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = DIPEXPORT_CREATE, description = "Générer le DIP à partir d'un DSL")
    public Response exportDIP(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);
            RequestResponse<JsonNode> response = client.exportDIP(queryJson);
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


    @POST
    @Path("/transfers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSFERS_CREATE, description = "Générer le SIP pour transfer à partir d'un DSL")
    public Response transfer(TransferRequest transferRequest) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(transferRequest.getDslRequest());
            // Validate DSL query & seda Version
            SelectMultipleSchemaValidator validator = new SelectMultipleSchemaValidator();
            validator.validate(transferRequest.getDslRequest());
            if (transferRequest.getSedaVersion() != null &&
                !SupportedSedaVersions.isSedaVersionValid(transferRequest.getSedaVersion())) {
                return Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED, "The Seda version is invalid!")).build();
            }

            RequestResponse<JsonNode> response = client.exportByUsageFilter(ExportRequest.from(transferRequest));
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

    @POST
    @Path("/transfers/reply")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSFERS_REPLY, description = "Start transfer reply workflow.")
    public Response transferReply(InputStream transferReply) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            return client.startTransferReplyWorkflow(transferReply).toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage()))
                .build();
        }
    }

    @GET
    @Path("/transfers/{id}/sip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = TRANSFERS_ID_SIP_READ, description = "Récupérer le SIP du transfer")
    public Response findTransferByID(@PathParam("id") String id) {

        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            Response response = client.findTransferSIPByID(id);
            if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
                return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            } else {
                return response;
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
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
    @Secured(permission = RECLASSIFICATION_UPDATE, description = "Mise à jour d'arborescence des unités archivistiques")
    public Response reclassification(@Dsl(DslSchema.RECLASSIFICATION_QUERY) JsonNode queryJson) {

        ParametersChecker.checkParameter("Missing reclassification request", queryJson);

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> response = client.reclassification(queryJson);
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
    @Secured(permission = ELIMINATION_ANALYSIS, description = "Analyse de l'élimination d'unités archivistiques")
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
            RequestResponse<JsonNode> response = client.startEliminationAnalysis(eliminationRequestBody);
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
    @Secured(permission = ELIMINATION_ACTION, description = "Elimination définitive d'unités archivistiques")
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
            RequestResponse<JsonNode> response = client.startEliminationAction(eliminationRequestBody);
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
     * @param id operationId correponding to the current dip
     * @return Response
     */
    @GET
    @Path("/dipexport/{id}/dip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = DIPEXPORT_ID_DIP_READ, description = "Récupérer le DIP")
    public Response findExportByID(@PathParam("id") String id) {

        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            Response response = client.findExportByID(id);
            if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
                return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            } else {
                return response;
            }
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
    @Secured(permission = UNITS_ID_READ_JSON,
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
            if (!(result instanceof VitamError) && (((RequestResponseOK<JsonNode>) result).getResults() == null ||
                ((RequestResponseOK<JsonNode>) result).getResults().isEmpty())) {
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
     * Update archive units by Id with Json query
     *
     * @param queryJson the update query (null not allowed)
     * @param idUnit units identifier
     * @return a archive unit result list
     * @deprecated This Method is no longer maintained and is no longer acceptable for updating units.
     * <p> Use {@link AccessExternalResource#massUpdateUnits(JsonNode)} or
     * {@link AccessExternalResource#massUpdateUnitsRules(MassUpdateUnitRuleRequest)} instead.
     */
    @Deprecated
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = UNITS_ID_UPDATE, description = "Réaliser la mise à jour d'une unité archivistique")
    public Response updateUnitById(@Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryJson, @PathParam("idu") String idUnit) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
            updateParserMultiple.parse(queryJson);
            UpdateMultiQuery updateMultiQuery = updateParserMultiple.getRequest();
            updateMultiQuery.addRoots(idUnit);
            RequestResponse<JsonNode> response = client.updateUnitbyId(updateMultiQuery.getFinalUpdate(), idUnit);
            if (!response.isOk() && response instanceof VitamError) {
                VitamError<JsonNode> error = (VitamError<JsonNode>) response;
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
    @Secured(permission = UNITS_ID_OBJECTS_READ_JSON,
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
            RequestResponse<JsonNode> selectedObjectGroupsByUnit = client.selectObjectbyId(queryJson, idObjectGroup);

            if (!((RequestResponseOK) selectedObjectGroupsByUnit).getResults().isEmpty()) {
                excludeBlackListedFieldsForGot(((RequestResponseOK) selectedObjectGroupsByUnit).getResults());
            }

            int st = selectedObjectGroupsByUnit.isOk() ?
                Status.OK.getStatusCode() :
                selectedObjectGroupsByUnit.getHttpCode();

            if (!selectedObjectGroupsByUnit.isOk()) {
                VitamError<JsonNode> error = (VitamError<JsonNode>) selectedObjectGroupsByUnit;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR, error.getMessage(),
                    error);
            } else if (((RequestResponseOK<JsonNode>) selectedObjectGroupsByUnit).getResults() == null ||
                ((RequestResponseOK<JsonNode>) selectedObjectGroupsByUnit).getResults().isEmpty()) {
                throw new AccessInternalClientNotFoundException(UNIT_NOT_FOUND);
            }

            return Response.status(st).entity(selectedObjectGroupsByUnit).build();
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
     * @param headers the http header defined parameters of request. Headers X-Qualifier and X-Version must be defined with target object qualifier and version in the object group container associated with the unit.
     * @param unitId the id of archive unit
     * @return object content as response body stream with HTTP 200 when OK, HTTP 404 when object not found, HTTP 460 when object is not available for immediate access and requires Access Request. HTTP 40X / 50X on error.
     */
    @GET
    @Path("/units/{idu}/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = UNITS_ID_OBJECTS_READ_BINARY, description = "Télecharger un objet")
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
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param persistentIdentifier unit Persistent Identifier
     * @param qualifier the qualifier, not mondatory, default value BinaryMaster
     * @param version the version, not mondatory, default value 1
     * @param headers the http header defined parameters of request. Headers X-Qualifier and X-Version must be defined with target object qualifier and version in the object group container associated with the unit.
     * @return object content as response body stream with HTTP 200 when OK, HTTP 404 when object not found, HTTP 460 when object is not available for immediate access and requires Access Request. HTTP 40X / 50X on error.
     */
    @GET
    @Path("/objects/byunitspersistentidentifier/{persistentIdentifier:.+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = OBJECTS_PERSISTENT_IDENTIFIER_OBJECTS_READ_BINARY, description = "Télécharger un objet")
    @Operation(
        description = "Requête qui retourne un stream du premier objet qui correspond aux critères . ",
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie un stream contient l'objet correspondant aux critères liés à l'unité avec l'identifiant pérenne à la requête DSL et l'identifiant pérenne"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed."),
            @ApiResponse(responseCode = "301", description = "Moved to another system."),
            @ApiResponse(responseCode = "410", description = "Deleted."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
        }
    )
    public Response getDataObjectByUnitPersistentIdentifier(@Context HttpHeaders headers,
        @PathParam("persistentIdentifier") String persistentIdentifier,
        @DefaultValue("BinaryMaster") @QueryParam("qualifier") String qualifier,
        @DefaultValue("1") @QueryParam("version") Integer version) {
        try {
            ParametersChecker.checkParameter("Missing persistent identifier", persistentIdentifier);
            return getDataObjectByUnitPersistentIdentifier(persistentIdentifier,
                DataObjectVersionType.fromName(qualifier).getName(), version);

        } catch (IllegalArgumentException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        }
    }


    /**
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param headers the http header defined parameters of request. Headers X-Qualifier and X-Version must be defined with target object qualifier and version in the object group container associated with the unit.
     * @param persistentIdentifier object Persistent Identifier value
     * @return object content as response body stream with HTTP 200 when OK, HTTP 404 when object not found, HTTP 460 when object is not available for immediate access and requires Access Request. HTTP 40X / 50X on error.
     */
    @GET
    @Path("/objects/byobjectspersistentidentifier/{persistentIdentifier:.+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = OBJECTS_PERSISTENT_IDENTIFIER_READ_BINARY, description = "Télécharger un objet par son identifiant pérenne")
    @Operation(
        description = "Requête qui retourne un stream du premier objet qui correspond aux critères.",
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie un stream contient l'objet correspondant à l'identifiant pérenne"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed."),
            @ApiResponse(responseCode = "301", description = "Moved to another system."),
            @ApiResponse(responseCode = "410", description = "Deleted."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
        }
    )

    public Response getDataObjectByObjectPersistentIdentifier(@Context HttpHeaders headers,
        @PathParam("persistentIdentifier") String persistentIdentifier) {

        Status status;
        try {
            List<JsonNode> objectsFound = findObjectGroupByPersistentIdentifier(persistentIdentifier, null);
            String idObjectGroup;
            if (objectsFound == null || objectsFound.isEmpty()) {
                throw new AccessInternalClientNotFoundException("Object not found");
            }
            ObjectNode firstObjectFound = (ObjectNode) objectsFound.get(0);
            if (firstObjectFound == null || firstObjectFound.get(VitamFieldsHelper.id()) == null) {
                throw new AccessInternalClientNotFoundException("Object not found");
            }
            idObjectGroup = firstObjectFound.get(VitamFieldsHelper.id()).textValue();

            //check all versions to retrieve the right object to download 

            List<JsonNode> dataObjectUpsValues = firstObjectFound.findValues(UNIT_UPS);

            if (dataObjectUpsValues == null || dataObjectUpsValues.isEmpty() || dataObjectUpsValues.get(0).isEmpty()) {
                throw new AccessInternalClientNotFoundException("Could not find UPS fields of object");
            }
            String unitId = dataObjectUpsValues.get(0).get(0).textValue();
            String objectId = null;
            String dataObjectVersionValue = null;
            List<JsonNode> dataObjectQualifiers = firstObjectFound.findValues(QUALIFIERS);
            for (JsonNode dataObjectQualifier : dataObjectQualifiers) {
                List<JsonNode> dataObjectVersions = dataObjectQualifier.findValues(VERSIONS);
                for (JsonNode version : dataObjectVersions) {
                    JsonNode persistentIdsFields = version.get(0).get(PERSISTENT_IDENTIFIERS);
                    for (JsonNode persistentId : persistentIdsFields) {
                        String persistentIdentifierInVersion =
                            persistentId.get(PERSISTENT_IDENTIFIER_CONTENT).textValue();
                        if (persistentIdentifier.equals(persistentIdentifierInVersion)) {
                            objectId = version.get(0).get("#id").textValue();
                            dataObjectVersionValue = version.get(0).get(DATA_OBJECT_VERSION).textValue();
                            break;
                        }
                    }
                    if (objectId != null) {
                        break;
                    }
                }
                if (objectId != null) {
                    break;
                }
            }
            if (objectId == null) {
                throw new AccessInternalClientNotFoundException(
                    "could not find the right object with the persistent identifier");
            }
            if (dataObjectVersionValue == null) {
                throw new AccessInternalClientNotFoundException("dataObjectVersion is empty");
            }
            String[] dataObjectVersionValueTokens = dataObjectVersionValue.split("_");
            if (dataObjectVersionValueTokens.length < 2) {
                throw new AccessInternalClientNotFoundException("dataObjectVersion does not respect qualifier_version");
            }

            return asyncObjectStream(dataObjectVersionValueTokens[0], dataObjectVersionValueTokens[1], idObjectGroup,
                unitId);
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
     * get Objects list based on persistent identifier
     *
     * @param persistentIdentifier the persistent identifier to get Object
     * @return Response
     */
    @GET
    @Path("/objects/byobjectspersistentidentifier/{persistentIdentifier:.+}")
    @Operation(
        description = "Requête qui retourne la liste des objets qui correspondent aux critères d'identifiant pérenne . ",
        parameters = {
            @Parameter(name = X_ACCESS_CONTRAT_ID, in = HEADER, description = "The contract name", required = true, example = "ACC-0001"),
            @Parameter(name = X_TENANT_ID, in = HEADER, description = "The tenant id", required = true, example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Renvoie la liste des objets correspondants aux critères liés à la requête DSL et l'identifiant pérenne de l'objet"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed."),
            @ApiResponse(responseCode = "301", description = "Moved to another system."),
            @ApiResponse(responseCode = "410", description = "Deleted."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
        }
    )

    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = OBJECTS_PERSISTENT_IDENTIFIER_READ, description = "Récupérer la liste des objets par identifiant pérenne")
    public Response getObjectsByPersistentIdentifier(@PathParam("persistentIdentifier") String persistentIdentifier,
        @Dsl(value = DslSchema.GET_BY_ID) JsonNode queryJson) {
        Status status;
        try {
            SanityChecker.checkJsonAll(queryJson);
            List<JsonNode> objectsFound = findObjectGroupByPersistentIdentifier(persistentIdentifier, queryJson);
            return Response.status(Status.OK).entity(objectsFound).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
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

    private Response getDataObjectByUnitPersistentIdentifier(String unitPersistentIdentifier, String qualifier,
        Integer version) {

        Status status;
        try {
            JsonNode unit = findUnitByPersistentIdentifier(unitPersistentIdentifier, List.of(OBJECT_TAG));
            if (unit == null) {
                throw new AccessInternalClientNotFoundException("No unit found with criteria");
            }
            if (unit.get(OBJECT_TAG) == null) {
                throw new AccessInternalClientNotFoundException("objectGroup not found");
            }
            String idObjectGroup = unit.get(OBJECT_TAG).textValue();

            if (idObjectGroup == null) {
                throw new AccessInternalClientNotFoundException("ObjectGroup of Unit not found");
            }

            return asyncObjectStream(qualifier, String.valueOf(version), idObjectGroup,
                unit.get(VitamFieldsHelper.id()).textValue());
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
     * Create an access request for accessing the object of an archive unit stored on an async storage offer (tape storage offer)
     * Access requests for objects stored on non-asynchronous storage offers does NOT require Access Request creation.
     *
     * The status of returned Access Requests should be monitoring periodically (typically every few minutes) to check the availability of data.
     * Once an Access Request status is {@link fr.gouv.vitam.common.model.storage.AccessRequestStatus#READY}, the data can be downloaded within the Access Request expiration delay defined in the tape storage offer configuration by the administrator.
     *
     * CAUTION : After successful download of object, caller SHOULD remove the Access Request to free up disk resources on tape storage offer.
     *
     * @param headers the http header defined parameters of request. Headers X-Qualifier and X-Version must be defined with target object qualifier and version in the object group container associated with the unit.
     * @param unitId the id of archive unit
     * @return HTTP 200 with RequestResponse body with an {@link AccessRequestReference} entry when Access Request created, or an RequestResponse when no Access Request required (synchronous storage offer). HTTP 404 when not found. HTTP 40X / 50X on error.
     */
    @POST
    @Path("/units/{idu}/objects/accessRequests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = UNITS_ID_OBJECTS_ACCESS_REQUESTS_CREATE, description = "Créer une demande d'accès à un objet persisté sur une offre froide (bande magnétique)")
    public Response createObjectAccessRequestByUnitId(@Context HttpHeaders headers, @PathParam("idu") String unitId) {

        Status status;
        try {
            Optional<Response> errorResponse = checkQualifierAndVersionHeaders(headers.getRequestHeaders(),
                VitamCode.ACCESS_EXTERNAL_CREATE_OBJECT_ACCESS_REQUEST);
            if (errorResponse.isPresent()) {
                return errorResponse.get();
            }

            String idObjectGroup = idObjectGroup(unitId);
            if (idObjectGroup == null) {
                throw new AccessInternalClientNotFoundException("ObjectGroup of Unit not found");
            }

            final String xQualifier = headers.getHeaderString(GlobalDataRest.X_QUALIFIER);
            final String xVersion = headers.getHeaderString(GlobalDataRest.X_VERSION);
            final int version = Integer.parseInt(xVersion);

            try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
                Optional<AccessRequestReference> objectAccessRequest =
                    client.createObjectAccessRequest(idObjectGroup, xQualifier, version);
                RequestResponseOK<AccessRequestReference> requestResponseOK = new RequestResponseOK<>();
                objectAccessRequest.ifPresent(requestResponseOK::addResult);
                requestResponseOK.setHttpCode(Status.OK.getStatusCode());
                return requestResponseOK.toResponse();
            }

        } catch (InvalidParseOperationException | BadRequestException | InvalidCreateOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CREATE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();

        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.warn(REQUEST_RESOURCES_DOES_NOT_EXISTS, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CREATE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CREATE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (Exception e) {
            LOGGER.error("Internal server error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CREATE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        }
    }

    /**
     * Bulk check of the status of a set of Access Requests.
     * Once Access Request status is {@link AccessRequestStatus#READY}, the object is guaranteed to be available for download within the Access Request expiration delay defined in the tape storage offer configuration by the administrator.
     *
     * Access Requests are only visible within their tenant. Attempts to check status of Access Requests of another tenant will return a {@link AccessRequestStatus#NOT_FOUND}.
     * Attempts to check an Access Request status for a non-asynchronous storage offer (tape storage offer) causes an HTTP 406 Not-Acceptable error code.
     *
     * @param accessRequestReferences the Access Requests whose status is to be checked
     * @return HTTP 200 with a RequestResponse of {@link StatusByAccessRequest} entries, one per {@link AccessRequestReference}. HTTP 40X / 50X on error.
     */
    @GET
    @Path("/accessRequests/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = ACCESS_REQUESTS_CHECK, description = "Vérifier l'état d'un ensemble de demandes d'accès sur une offre froide (bande magnétique)")
    public Response checkAccessRequestStatuses(List<AccessRequestReference> accessRequestReferences) {

        Status status;
        try {
            ParametersChecker.checkParameter("Access requests required", accessRequestReferences);
            if (accessRequestReferences.isEmpty()) {
                throw new IllegalArgumentException("Empty query");
            }
            for (AccessRequestReference accessRequestReference : accessRequestReferences) {
                ParametersChecker.checkParameter("Access requests required", accessRequestReference);
                ParametersChecker.checkParameter("Required accessRequestId",
                    accessRequestReference.getAccessRequestId());
                ParametersChecker.checkParameter("Required storageStrategyId",
                    accessRequestReference.getStorageStrategyId());
            }
            int distinctAccessRequestIds = accessRequestReferences.stream()
                .map(AccessRequestReference::getAccessRequestId)
                .collect(Collectors.toSet()).size();
            if (accessRequestReferences.size() != distinctAccessRequestIds) {
                throw new IllegalArgumentException("Duplicate access request ids " + accessRequestReferences);
            }

            try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
                List<StatusByAccessRequest> statusByAccessRequests =
                    client.checkAccessRequestStatuses(accessRequestReferences);

                return new RequestResponseOK<StatusByAccessRequest>()
                    .setHttpCode(Status.OK.getStatusCode())
                    .addAllResults(statusByAccessRequests)
                    .toResponse();
            }

        } catch (IllegalArgumentException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CHECK_OBJECT_ACCESS_REQUEST_STATUSES,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (AccessInternalClientIllegalOperationException e) {
            LOGGER.error("Illegal operation on Access Request", e);
            status = Status.NOT_ACCEPTABLE;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CHECK_OBJECT_ACCESS_REQUEST_STATUSES,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error while checking Access Request statuses", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CHECK_OBJECT_ACCESS_REQUEST_STATUSES,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        }
    }

    /**
     * Removes an Access Request from an async storage offer (tape storage offer)
     * After removing an Access Request, object in not more guaranteed to be accessible for download.
     *
     * Attempts to remove an Access Request from a non-asynchronous storage offer causes an HTTP 406 Not-Acceptable error code.
     * Attempts to remove an already removed Access Request does NOT fail (idempotency).
     * Access Requests are only visible within their tenant. Attempts to remove an Access Request of another tenant does NOT delete access request.
     *
     * @param accessRequestReference the access request to remove
     * @return HTTP 200 on success or in case of Access Request not found (already removed, purged after timeout, non-visible from current tenant). HTTP 40x / 50x on error.
     */
    @DELETE
    @Path("/accessRequests/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = ACCESS_REQUESTS_REMOVE, description = "Supprimer une demande d'accès à un objet persisté sur une offre froide (bande magnétique)")
    public Response removeAccessRequest(AccessRequestReference accessRequestReference) {

        Status status;
        try {
            ParametersChecker.checkParameter("Required objectAccessRequest", accessRequestReference);
            ParametersChecker.checkParameter("Required accessRequestId", accessRequestReference.getAccessRequestId());
            ParametersChecker.checkParameter("Required strategyId", accessRequestReference.getStorageStrategyId());

            try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
                client.removeAccessRequest(accessRequestReference);
            }

            return new RequestResponseOK<Void>()
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();

        } catch (IllegalArgumentException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REMOVE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (AccessInternalClientIllegalOperationException e) {
            LOGGER.error("Illegal operation on Access Request", e);
            status = Status.NOT_ACCEPTABLE;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REMOVE_OBJECT_ACCESS_REQUEST,
                        e.getLocalizedMessage()).setHttpCode(status.getStatusCode())))
                .build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error while removing Access Request " + accessRequestReference, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REMOVE_OBJECT_ACCESS_REQUEST,
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
    @Secured(permission = UNITS_UPDATE, description = "Mise à jour en masse des unités archivistiques")
    public Response massUpdateUnits(@Dsl(DslSchema.MASS_UPDATE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
            updateParserMultiple.parse(queryJson);
            UpdateMultiQuery updateMultiQuery = updateParserMultiple.getRequest();
            RequestResponse<JsonNode> response = client.updateUnits(updateMultiQuery.getFinalUpdate());

            if (!response.isOk() && response instanceof VitamError) {
                VitamError<JsonNode> error = (VitamError<JsonNode>) response;
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
     * Revert an update of archive units with json query.
     *
     * @param revertUpdateOptions the revert_update query (null not allowed)
     */
    @POST
    @Path("/revert/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = REVERT_UPDATE, description = "Restauration des metadonnées essentielles")
    public Response revertUpdateUnits(RevertUpdateOptions revertUpdateOptions) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(revertUpdateOptions.getDslRequest());
            BatchProcessingQuerySchemaValidator validator = new BatchProcessingQuerySchemaValidator();
            validator.validate(revertUpdateOptions.getDslRequest());
            RequestResponse<JsonNode> response = client.revertUnits(revertUpdateOptions);

            if (!response.isOk() && response instanceof VitamError) {
                VitamError<JsonNode> error = (VitamError<JsonNode>) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_REVERT_UPDATE_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException | ValidationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REVERT_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException | IOException e) {
            LOGGER.error("Internal request error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REVERT_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error(WRITING_PERMISSIONS_INVALID, e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REVERT_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_REVERT_UPDATE_ERROR,
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
    @Secured(permission = UNITS_RULES_UPDATE, description = "Mise à jour en masse des règles de gestion")
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
                VitamError<JsonNode> error = (VitamError<JsonNode>) response;
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
     * Bulk atomic update of archive units with json queries.
     *
     * @param updateQueriesJson the bulk update queries (null not allowed)
     * @return
     */
    @POST
    @Path("/units/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = UNITS_BULK_UPDATE, description = "Mise à jour par lot de requêtes unitaires des unités archivistiques")
    public Response bulkAtomicUpdateUnits(@Dsl(DslSchema.BULK_UPDATE) JsonNode updateQueriesJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> response = client.bulkAtomicUpdateUnits(updateQueriesJson);

            if (!response.isOk() && response instanceof VitamError) {
                VitamError<JsonNode> error = (VitamError<JsonNode>) response;
                return buildErrorFromError(VitamCode.ACCESS_EXTERNAL_BULK_ATOMIC_UPDATE_ERROR, error.getMessage(),
                    error);
            }
            return Response.status(Status.OK).entity(response).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_BULK_ATOMIC_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Internal request error ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_BULK_ATOMIC_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error(WRITING_PERMISSIONS_INVALID, e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_BULK_ATOMIC_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error(CONTRACT_ACCESS_NOT_ALLOW, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_BULK_ATOMIC_UPDATE_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }


    @Path("/units/computedInheritedRules")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = COMPUTEINHERITEDRULES_ACTION, description = "Lancer le processus de calcul des règles hérité pour la recherche")
    public Response startComputeInheritedRules(@Dsl(value = DslSchema.BATCH_PROCESSING) JsonNode dslQuery) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.startComputeInheritedRules(dslQuery);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error(ERROR_ON_PRESERVATION, e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }


    @Path("/units/computedInheritedRules")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = COMPUTEINHERITEDRULES_DELETE, description = "Lancer le processus de Suppression calcul des règles hérité pour la recherche")
    public Response deleteComputeInheritedRules(@Dsl(value = DslSchema.BATCH_PROCESSING) JsonNode dslQuery) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.deleteComputeInheritedRules(dslQuery);
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
    @Secured(permission = UNITSWITHINHERITEDRULES_READ, description = "Récupérer la liste des unités archivistiques avec leurs règles de gestion héritées")
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


    private JsonNode findUnitByPersistentIdentifier(String persistentIdentifier,
        List<String> projectionFields)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException,
        BadRequestException, InvalidCreateOperationException {
        // Select "Object from ArchiveUNit persistentIdentifier
        ParametersChecker.checkParameter("persistent Identifier is required", persistentIdentifier);
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SelectParserMultiple query = new SelectParserMultiple();
            SelectMultiQuery selectMultiQuery = query.getRequest();
            selectMultiQuery.addQueries(
                QueryHelper.eq(PERSISTENT_IDENTIFIERS + "." + PERSISTENT_IDENTIFIER_CONTENT, persistentIdentifier));
            selectMultiQuery.addUsedProjection(VitamFieldsHelper.id());
            if (projectionFields != null && !projectionFields.isEmpty()) {
                for (String projectionField : projectionFields) {
                    selectMultiQuery.addUsedProjection(projectionField);
                }
            }

            RequestResponse<JsonNode> response = client.selectUnits(selectMultiQuery.getFinalSelect());
            SanityChecker.checkJsonAll(response.toJsonNode());
            if (response.isOk()) {
                return ((RequestResponseOK<JsonNode>) response).getFirstResult();
            } else {
                throw new AccessInternalClientNotFoundException(UNIT_NOT_FOUND);
            }
        }
    }

    private List<JsonNode> findObjectGroupByPersistentIdentifier(String persistentIdentifier, JsonNode queryJson)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException,
        BadRequestException, InvalidCreateOperationException {

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {

            SelectParserMultiple query = new SelectParserMultiple();
            if (queryJson != null) {
                query.parse(queryJson);
            }
            SelectMultiQuery selectMultiQuery = query.getRequest();
            BooleanQuery nestedSubQuery = and();
            nestedSubQuery.add(
                QueryHelper.search(
                    QUALIFIERS + "." + VERSIONS + "." + PERSISTENT_IDENTIFIERS + "." + PERSISTENT_IDENTIFIER_CONTENT,
                    persistentIdentifier));
            selectMultiQuery.setQuery(nestedSearch(QUALIFIERS + "." + VERSIONS, nestedSubQuery.getCurrentQuery()));

            RequestResponse<JsonNode> selectedObjectGroups = client.selectObjects(selectMultiQuery.getFinalSelect());
            if (!((RequestResponseOK) selectedObjectGroups).getResults().isEmpty()) {
                excludeBlackListedFieldsForGot(((RequestResponseOK) selectedObjectGroups).getResults());
            }

            if (selectedObjectGroups.isOk()) {
                return ((RequestResponseOK<JsonNode>) selectedObjectGroups).getResults();
            } else {
                throw new AccessInternalClientNotFoundException(REQ_RES_DOES_NOT_EXIST);
            }
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

    private Optional<Response> checkQualifierAndVersionHeaders(MultivaluedMap<String, String> multipleMap,
        VitamCode vitamCode) {
        try {
            if (!multipleMap.containsKey(GlobalDataRest.X_QUALIFIER) ||
                !multipleMap.containsKey(GlobalDataRest.X_VERSION)) {
                LOGGER.error("At least one required header is missing. Required headers: (" +
                    VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
                return Optional.ofNullable(Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorStream(VitamCodeHelper
                        .toVitamError(vitamCode, "QUALIFIER or VERSION missing")
                        .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())))
                    .build());
            }

            // Check version format
            Integer.parseInt(multipleMap.getFirst(GlobalDataRest.X_VERSION));

        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Optional.ofNullable(Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        e.getLocalizedMessage())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())))
                .build());
        }
        return Optional.empty();
    }

    private Response asyncObjectStream(MultivaluedMap<String, String> multipleMap, String idObjectGroup,
        String unitId) {

        Optional<Response> errorResponse = checkQualifierAndVersionHeaders(multipleMap,
            VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        final String xQualifier = multipleMap.get(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = multipleMap.get(GlobalDataRest.X_VERSION).get(0);

        HttpHeaderHelper.checkVitamHeadersMap(multipleMap);
        return asyncObjectStream(xQualifier, xVersion, idObjectGroup, unitId);
    }

    private Response asyncObjectStream(final String xQualifier, final String xVersion, String idObjectGroup,
        String unitId) {


        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {

            final Response response =
                client.getObject(idObjectGroup, xQualifier, Integer.parseInt(xVersion), unitId);
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
        } catch (AccessInternalClientUnavailableDataFromAsyncOfferException e) {
            String msg = "Object is not currently available from async offer. Access request required";
            LOGGER.warn(msg, e);
            return Response.status(AccessExtAPI.UNAVAILABLE_DATA_FROM_ASYNC_OFFER_STATUS_CODE)
                .entity(getErrorStream(
                    new VitamError<JsonNode>("UNAVAILABLE_DATA_FROM_ASYNC_OFFER")
                        .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                        .setHttpCode(AccessExtAPI.UNAVAILABLE_DATA_FROM_ASYNC_OFFER_STATUS_CODE)
                        .setState(DomainName.STORAGE.getName())
                        .setMessage(msg)
                        .setDescription(e.getLocalizedMessage())
                )).build();
        } catch (final AccessInternalClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(VitamCodeHelper
                    .toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR,
                        exc.getLocalizedMessage())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())))
                .build();
        } catch (final AccessInternalClientNotFoundException exc) {
            LOGGER.warn(exc.getMessage(), exc);
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
    private VitamError<JsonNode> getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError<JsonNode>(status.name()).setHttpCode(status.getStatusCode())
            .setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    private InputStream getErrorStream(VitamError<JsonNode> vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage(), e);
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private Response buildErrorFromError(VitamCode vitamCode, String message, VitamError<JsonNode> oldVitamError) {
        LOGGER.info("Description: " + message);
        VitamError<JsonNode> newVitamError = new VitamError<JsonNode>(VitamCodeHelper.getCode(vitamCode))
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
    @Secured(permission = OBJECTS_READ, description = "Récupérer la liste des groupes d'objets")
    public Response getObjects(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(queryJson);

            SelectMultipleSchemaValidator.checkAuthorizeTrackTotalHits(queryJson,
                configuration.isAuthorizeTrackTotalHits());

            RequestResponse<JsonNode> selectedObjectGroups = client.selectObjects(queryJson);

            if (!((RequestResponseOK) selectedObjectGroups).getResults().isEmpty()) {
                excludeBlackListedFieldsForGot(((RequestResponseOK) selectedObjectGroups).getResults());
            }

            int st = selectedObjectGroups.isOk() ? Status.OK.getStatusCode() : selectedObjectGroups.getHttpCode();
            return Response.status(st).entity(selectedObjectGroups).build();
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
        } catch (ValidationException e) {
            LOGGER.error(UNAUTHORIZED_DSL_PARAMETER, e);
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }


    private void excludeBlackListedFieldsForGot(List<ObjectNode> gotResults) {
        if (objectGroupBlackListedFieldsForVisualizationByTenant != null &&
            !objectGroupBlackListedFieldsForVisualizationByTenant.isEmpty()) {
            List<String> fieldsToExcludeForCurrentTenant = objectGroupBlackListedFieldsForVisualizationByTenant.get(
                VitamThreadUtils.getVitamSession().getTenantId());
            if (CollectionUtils.isNotEmpty(fieldsToExcludeForCurrentTenant)) {
                gotResults.forEach(got -> {
                    fieldsToExcludeForCurrentTenant.forEach(fieldToExclude -> {
                        Iterator<Map.Entry<String, JsonNode>> gotFields = got.deepCopy().fields();
                        while (gotFields.hasNext()) {
                            Map.Entry<String, JsonNode> nextGotField = gotFields.next();
                            JsonHandler.removeFieldFromNode(got, fieldToExclude, nextGotField.getValue());
                        }
                    });
                });
            } else {
                LOGGER.debug("No BlackList Fields of ObjectGroup are declared for tenant " +
                    VitamThreadUtils.getVitamSession().getTenantId());
            }
        }
    }

    @Path("/storageaccesslog")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = STORAGEACCESSLOG_READ_BINARY, description = "Télécharger les journaux d'accès")
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
    @Secured(permission = PRESERVATION_UPDATE, description = "Lancer le processus de préservation")
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

    @Path("/deleteGotVersions")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = DELETE_GOT_VERSIONS, description = "Suppression des version de GOTs")
    public Response deleteGotVersions(DeleteGotVersionsRequest deleteGotVersionsRequest) {
        Status status;
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            ParametersChecker.checkParameter("Missing request", deleteGotVersionsRequest);
            ParametersChecker.checkParameter("Missing dslQuery in request", deleteGotVersionsRequest.getDslQuery());
            // Validate DSL Query
            BatchProcessingQuerySchemaValidator validator = new BatchProcessingQuerySchemaValidator();
            validator.validate(deleteGotVersionsRequest.getDslQuery());

            RequestResponse<JsonNode> requestResponse = client.deleteGotVersions(deleteGotVersionsRequest);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (IllegalArgumentException | IOException | ValidationException e) {
            LOGGER.warn(COULD_NOT_VALIDATE_REQUEST, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage())).build();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error("Error on deleting got versions request", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR,
                    e.getLocalizedMessage()).setHttpCode(status.getStatusCode()))
                .build();
        }
    }
}
