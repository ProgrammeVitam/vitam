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

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCIES;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.access.internal.core.ObjectGroupDipServiceImpl;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.mapping.dip.DipService;
import fr.gouv.vitam.common.mapping.dip.UnitDipServiceImpl;
import fr.gouv.vitam.common.mapping.serializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.common.mapping.serializer.LevelTypeDeserializer;
import fr.gouv.vitam.common.mapping.serializer.TextByLangDeserializer;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessInternalResourceImpl extends ApplicationStatusResource implements AccessInternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImpl.class);
    public static final String EXPORT_DIP = "EXPORT_DIP";

    // DIP
    private DipService unitDipService;
    private DipService objectDipService;


    private static final String END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS = "End of execution of DSL Vitam from Access";
    private static final String EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING =
        "Execution of DSL Vitam from Access ongoing...";
    private static final String BAD_REQUEST_EXCEPTION = "Bad request Exception ";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception ";
    private static final String ACCESS_MODULE = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";
    private static final String ACCESS_RESOURCE_INITIALIZED = "AccessResource initialized";

    private final AccessInternalModule accessModule;
    private ArchiveUnitMapper archiveUnitMapper;
    private ObjectGroupMapper objectGroupMapper;

    private ObjectMapper objectMapper;
    private WorkspaceClientFactory workspaceClientFactory;
    private ProcessingManagementClientFactory processingManagementClientFactory;

    /**
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessInternalResourceImpl(AccessInternalConfiguration configuration) {
        accessModule = new AccessInternalModuleImpl(configuration);
        WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
        archiveUnitMapper = new ArchiveUnitMapper();
        objectGroupMapper = new ObjectGroupMapper();
        LOGGER.debug(ACCESS_RESOURCE_INITIALIZED);
        this.objectMapper = buildObjectMapper();
        this.unitDipService = new UnitDipServiceImpl(archiveUnitMapper, objectMapper);
        this.objectDipService = new ObjectGroupDipServiceImpl(objectGroupMapper, objectMapper);
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getUrlProcessing());
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
    }

    /**
     * Test constructor
     *
     * @param accessModule
     */
    AccessInternalResourceImpl(AccessInternalModule accessModule) {
        this.accessModule = accessModule;
        archiveUnitMapper = new ArchiveUnitMapper();
        objectGroupMapper = new ObjectGroupMapper();
        LOGGER.debug(ACCESS_RESOURCE_INITIALIZED);
        this.objectMapper = buildObjectMapper();
        this.unitDipService = new UnitDipServiceImpl(archiveUnitMapper, objectMapper);
        this.objectDipService = new ObjectGroupDipServiceImpl(objectGroupMapper, objectMapper);
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
    }


    /**
     * get Archive Unit list by query based on identifier
     *
     * @param queryDsl as JsonNode
     * @return an archive unit result list
     */
    @Override
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(JsonNode queryDsl) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        LOGGER.debug("DEBUG: start selectUnits {}", queryDsl);

        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);
            JsonNode result = accessModule.selectUnit(applyAccessContractRestriction(queryDsl));
            LOGGER.debug("DEBUG {}", result);
            resetQuery(result, queryDsl);

            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error("Empty query is impossible", e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        }
    }


    /**
     * get Archive Unit list by query based on identifier
     *
     * @param queryDsl as JsonNode
     * @return an archive unit result list
     */
    @Override
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportDIP(JsonNode queryDsl) {

        Status status;
        LOGGER.debug("DEBUG: start selectUnits {}", queryDsl);

        try {
            checkEmptyQuery(queryDsl);
            GUID logbookId = GUIDFactory.newOperationLogbookGUID(VitamThreadUtils.getVitamSession().getTenantId());
            String operationId = VitamThreadUtils.getVitamSession().getRequestId();


            try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
                LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
                ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {

                final LogbookOperationParameters initParameters =
                    LogbookParametersFactory.newLogbookOperationParameters(
                        logbookId,
                        "EXPORT_DIP",
                        GUIDReader.getGUID(operationId),
                        LogbookTypeProcess.EXPORT_DIP,
                        StatusCode.STARTED,
                        logbookId.toString(),
                        logbookId);

                logbookClient.create(initParameters);

                workspaceClient.createContainer(operationId);
                workspaceClient.putObject(operationId, "query.json", JsonHandler.writeToInpustream(queryDsl));

                processingClient.initVitamProcess(Contexts.EXPORT_DIP.name(), operationId, EXPORT_DIP);
                // When
                RequestResponse<JsonNode> jsonNodeRequestResponse =
                    processingClient.executeOperationProcess(operationId, EXPORT_DIP,
                        Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
                return jsonNodeRequestResponse.toResponse();
            } catch (ContentAddressableStorageServerException | ContentAddressableStorageAlreadyExistException |
                LogbookClientServerException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                InvalidGuidOperationException | VitamClientException | InternalServerException e) {
                LOGGER.error("", e);
                return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
            }

        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }  catch (BadRequestException e) {
            LOGGER.error("Empty query is impossible", e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        }
    }

    @Override
    @GET
    @Path("/dipexport/{id}/dip")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response findDIPByID(@PathParam("id") String id) {
        try {
            return accessModule.findDIPByOperationId(id);
        } catch (AccessInternalExecutionException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    /**
     * get Archive Unit list by query based on identifier
     *
     * @param queryDsl as JsonNode
     * @param idUnit identifier
     * @return an archive unit result list
     */
    @Override
    @GET
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode queryDsl,
        @PathParam("id_unit") String idUnit) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);

        Status status;
        try {

            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(idUnit);
            JsonNode result = accessModule.selectUnitbyId(applyAccessContractRestriction(queryDsl), idUnit);
            resetQuery(result, queryDsl);

            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    @GET
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    @Override
    public Response getUnitByIdWithXMLFormat(JsonNode queryDsl, @PathParam("id_unit") String idUnit) {
        Status status;
        try {
            SanityChecker.checkParameter(idUnit);
            SanityChecker.checkJsonAll(queryDsl);

            JsonNode result = accessModule.selectUnitbyId(applyAccessContractRestriction(queryDsl), idUnit);
            ArrayNode results = (ArrayNode) result.get("$results");
            JsonNode unit = results.get(0);
            Response responseXmlFormat = unitDipService.jsonToXml(unit, idUnit);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return responseXmlFormat;
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        }
    }

    /**
     * update archive units by Id with Json query
     *
     * @param requestId request identifier
     * @param queryDsl DSK, null not allowed
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @Override
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(JsonNode queryDsl,
        @PathParam("id_unit") String idUnit, @HeaderParam(GlobalDataRest.X_REQUEST_ID) String requestId) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        try {
            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(idUnit);
            SanityChecker.checkParameter(requestId);
            if (!VitamThreadUtils.getVitamSession().getContract().getWritingPermission()) {
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, "Write permission not allowed")).build();
            }
            JsonNode result = accessModule.updateUnitbyId(queryDsl, idUnit, requestId);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return Response.status(Status.OK).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final AccessInternalRuleExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            return buildErrorResponse(VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CHECK_RULES, e.getMessage());
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(NOT_FOUND_EXCEPTION, e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, JsonNode query) {
        Status status;
        try {
            SanityChecker.checkJsonAll(query);
            SanityChecker.checkParameter(idObjectGroup);
            final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();
            Set<String> prodServices = contract.getOriginatingAgencies();
            JsonNode result;

            if (contract.getEveryOriginatingAgency()) {
                result = accessModule.selectObjectGroupById(query, idObjectGroup);
            } else {
                final SelectParserMultiple parser = new SelectParserMultiple();
                parser.parse(query);
                parser.getRequest().addQueries(
                    QueryHelper.in(ORIGINATING_AGENCIES.exactToken(), prodServices.toArray(new String[0]))
                        .setDepthLimit(0));
                result = accessModule.selectObjectGroupById(parser.getRequest().getFinalSelect(), idObjectGroup);
            }
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException | IllegalArgumentException |
            InvalidCreateOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, exc.getMessage())).build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc);
            status = INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, exc.getMessage())).build();
        }
    }

    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    @Override
    public Response getObjectByIdWithXMLFormat(JsonNode dslQuery, @PathParam("id_object_group") String objectId) {
        Status status;
        try {
            SanityChecker.checkParameter(objectId);
            SanityChecker.checkJsonAll(dslQuery);
            final JsonNode result =
                accessModule.selectObjectGroupById(addProdServicesToQueryForObjectGroup(dslQuery), objectId);
            ArrayNode results = (ArrayNode) result.get("$results");
            JsonNode objectGroup = results.get(0);
            Response responseXmlFormat = objectDipService.jsonToXml(objectGroup, objectId);
            return responseXmlFormat;
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        }
    }

    @GET
    @Path("/units/{id_unit}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    @Override
    public Response getObjectByUnitIdWithXMLFormat(JsonNode queryDsl, @PathParam("id_unit") String idUnit) {
        Status status;
        try {
            SanityChecker.checkParameter(idUnit);
            SanityChecker.checkJsonAll(queryDsl);
            //
            JsonNode result = accessModule.selectUnitbyId(applyAccessContractRestriction(queryDsl), idUnit);
            ArrayNode results = (ArrayNode) result.get("$results");
            JsonNode objectGroup = results.get(0);
            // Response responseXmlFormat = unitDipService.jsonToXml(unit, idUnit);
            Response responseXmlFormat = objectDipService.jsonToXml(objectGroup, idUnit);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return responseXmlFormat;
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        }
    }



    private Response asyncObjectStream(MultivaluedMap<String, String> multipleMap,
        String idObjectGroup, boolean post) {

        if (post) {
            if (!multipleMap.containsKey(GlobalDataRest.X_HTTP_METHOD_OVERRIDE)) {
                return Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorStream(Status.PRECONDITION_FAILED, "method POST without Override = GET"))
                    .build();
            }
        }
        if (!multipleMap.containsKey(GlobalDataRest.X_TENANT_ID) ||
            !multipleMap.containsKey(GlobalDataRest.X_QUALIFIER) ||
            !multipleMap.containsKey(GlobalDataRest.X_VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED,
                    "At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                        .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() +
                        ")"))
                .build();
        }
        final String xQualifier = multipleMap.get(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = multipleMap.get(GlobalDataRest.X_VERSION).get(0);

        if (!VitamThreadUtils.getVitamSession().getContract().isEveryDataObjectVersion() &&
            !validUsage(xQualifier.split("_")[0])) {
            return Response.status(Status.UNAUTHORIZED)
                .entity(getErrorStream(Status.UNAUTHORIZED, "Qualifier unallowed"))
                .build();
        }

        try {
            SanityChecker.checkHeadersMap(multipleMap);
            HttpHeaderHelper.checkVitamHeadersMap(multipleMap);
            SanityChecker.checkParameter(idObjectGroup);
            return accessModule.getOneObjectFromObjectGroup(idObjectGroup, xQualifier,
                Integer.valueOf(xVersion));
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage()))
                .build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        } catch (MetaDataNotFoundException | StorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity(getErrorStream(Status.NOT_FOUND, exc.getMessage())).build();
        }
    }

    private boolean validUsage(String s) {
        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        Set<String> versions = vitamSession.getContract().getDataObjectVersion();

        if (versions == null || versions.isEmpty()) {
            return true;
        }
        for (String version : versions) {
            if (version.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode applyAccessContractRestriction(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();
        Set<String> rootUnits = contract.getRootUnits();
        if (null != rootUnits && !rootUnits.isEmpty()) {
            String[] rootUnitsArray = rootUnits.toArray(new String[rootUnits.size()]);
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);

            Query rootUnitsRestriction = QueryHelper
                .or().add(QueryHelper.in(PROJECTIONARGS.ID.exactToken(), rootUnitsArray),
                    QueryHelper.in(PROJECTIONARGS.ALLUNITUPS.exactToken(), rootUnitsArray));

            List<Query> queryList = parser.getRequest().getQueries();
            if (queryList.isEmpty()) {
                queryList.add(rootUnitsRestriction.setDepthLimit(0));
            } else {
                Query firstQuery = queryList.get(0);
                int depth = firstQuery.getParserRelativeDepth();
                Query restrictedQuery = QueryHelper.and().add(rootUnitsRestriction, firstQuery);
                restrictedQuery.setDepthLimit(depth);
                parser.getRequest().getQueries().set(0, restrictedQuery);
            }
            queryDsl = parser.getRequest().getFinalSelect();
        }

        return addProdServicesToQuery(queryDsl);
    }

    private JsonNode addProdServicesToQuery(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();
        Set<String> prodServices = contract.getOriginatingAgencies();
        if (contract.getEveryOriginatingAgency()) {
            return queryDsl;
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            parser.getRequest().addQueries(QueryHelper.or()
                .add(QueryHelper.in(
                    ORIGINATING_AGENCIES.exactToken(), prodServices.toArray(new String[0])))
                .add(QueryHelper.eq(PROJECTIONARGS.UNITTYPE.exactToken(), UnitType.HOLDING_UNIT.name()))
                .setDepthLimit(0));
            return parser.getRequest().getFinalSelect();
        }
    }

    private JsonNode addProdServicesToQueryForObjectGroup(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();
        Set<String> prodServices = contract.getOriginatingAgencies();
        if (contract.getEveryOriginatingAgency()) {
            return queryDsl;
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            parser.getRequest().addQueries(QueryHelper.or()
                .add(QueryHelper.in(
                    ORIGINATING_AGENCIES.exactToken(), prodServices.toArray(new String[0])))
                .setDepthLimit(0));
            return parser.getRequest().getFinalSelect();
        }
    }

    private void checkEmptyQuery(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cant be empty");
        }
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectStreamAsync(@Context HttpHeaders headers,
        @PathParam("id_object_group") String idObjectGroup) {
        MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();
        return asyncObjectStream(multipleMap, idObjectGroup, false);
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    private InputStream getErrorStream(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        try {
            return JsonHandler.writeToInpustream(new VitamError(status.name())
                .setHttpCode(status.getStatusCode()).setContext(ACCESS_MODULE)
                .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }


    private Response buildErrorResponse(VitamCode vitamCode, String description) {
        if (description == null) {
            description = vitamCode.getMessage();
        }

        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(description)).toString())
            .build();
    }

    private void resetQuery(JsonNode result, JsonNode queryDsl) {
        if (result != null && result.has(RequestResponseOK.TAG_CONTEXT)) {
            ((ObjectNode) result).set(RequestResponseOK.TAG_CONTEXT, queryDsl);
        }
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }

}
