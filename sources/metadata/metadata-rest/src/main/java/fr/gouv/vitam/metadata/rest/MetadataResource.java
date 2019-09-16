/*
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
 */
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.BatchRulesUpdateInfo;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.metadata.core.rules.MetadataRuleService;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.elasticsearch.ElasticsearchParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.COMPUTE_INHERITED_RULES;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.PRESERVATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Units resource REST API
 */
@Path("/metadata/v1")
public class MetadataResource extends ApplicationStatusResource {

    private static final String CONTEXT_METADATA = "METADATA";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataResource.class);

    private static final String INGEST = "ingest";
    private static final String ACCESS = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";

    private static final String ACCESS_CONTRACT = "AccessContract";

    private final MetaDataImpl metaData;
    private final MetadataRuleService metadataRuleService;

    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    /**
     * MetaDataResource constructor
     *
     * @param metaData
     * @param metadataRuleService
     */
    MetadataResource(MetaDataImpl metaData, MetadataRuleService metadataRuleService, MetaDataConfiguration configuration) {
        this(metaData,
            metadataRuleService,
            ProcessingManagementClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance()
        );
        LOGGER.info("init MetaData Resource server");
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getUrlProcessing());
    }

    private MetadataResource(MetaDataImpl metaData,
        MetadataRuleService metadataRuleService,
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.metaData = metaData;
        this.metadataRuleService = metadataRuleService;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    /**
     * Insert unit with json request
     *
     * @param insertRequest the insert request in JsonNode format
     * @return Response
     */
    @Path("units")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response insertUnit(JsonNode insertRequest) {
        Status status;
        try {
            metaData.insertUnit(insertRequest);
        } catch (final MetaDataExecutionException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        RequestResponseOK responseOK = new RequestResponseOK(insertRequest);
        responseOK.setHits(1, 0, 1)
            .setHttpCode(Status.CREATED.getStatusCode());
        return Response.status(Status.CREATED)
            .entity(responseOK)
            .build();
    }

    /**
     * Insert unit with json request
     *
     * @param jsonNodes the insert request in JsonNode format
     * @return Response
     */
    @Path("units/bulk")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response insertUnitBulk(List<JsonNode> jsonNodes) {
        Status status;
        try {
            metaData.insertUnits(jsonNodes);
        } catch (final MetaDataExecutionException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }

        // transform request in jsonNode to add it in answer
        ArrayNode arrayNode = JsonHandler.createArrayNode();
        jsonNodes.forEach(arrayNode::add);

        RequestResponseOK responseOK = new RequestResponseOK(arrayNode);
        responseOK.setHits(arrayNode.size(), 0, arrayNode.size())
            .setHttpCode(Status.CREATED.getStatusCode());
        return Response.status(Status.CREATED)
            .entity(responseOK)
            .build();
    }

    /**
     * Update unit with json request
     *
     * @param updateQuery the insert request in JsonNode format
     * @return Response
     */
    @Path("units/updatebulk")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response updateUnitBulk(JsonNode updateQuery) {
        Status status;
        RequestResponse<UpdateUnit> result;
        try {
            result = metaData.updateUnits(updateQuery);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        RequestResponseOK responseOK = new RequestResponseOK(updateQuery);
        responseOK.setHits(1, 0, 1)
            .setHttpCode(OK.getStatusCode());

        return Response.status(OK)
            .entity(result)
            .build();
    }

    /**
     * Update unit rules with json request
     *
     * @param batchRulesUpdateInfo the update rule request
     * @return Response
     */
    @Path("units/updaterulesbulk")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response updateUnitsRulesBulk(BatchRulesUpdateInfo batchRulesUpdateInfo) {

        RequestResponse<UpdateUnit> result = metaData.updateUnitsRules(batchRulesUpdateInfo.getUnitIds(),
            batchRulesUpdateInfo.getRuleActions(), batchRulesUpdateInfo.getRulesToDurationData());

        return Response.status(Status.OK)
            .entity(result)
            .build();
    }

    /**
     * Select unit with json request
     *
     * @param request the request in JsonNode format
     * @return Response
     */
    @Path("units")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response selectUnit(JsonNode request) throws VitamDBException {
        return selectUnitsByQuery(request);
    }

    private Response selectUnitsByQuery(JsonNode selectRequest) {
        Status status;
        try {
            RequestResponse<JsonNode> result = null;
            result = metaData.selectUnitsByQuery(selectRequest);
            int st = result.isOk() ? Status.FOUND.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result.setHttpCode(st)).build();

        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    /**
     * Refresh Unit index
     *
     * @return Response
     */
    @Path("units")
    @PUT
    @Produces(APPLICATION_JSON)
    public Response refreshUnit() {
        try {
            metaData.refreshUnit();
            RequestResponseOK response = new RequestResponseOK();
            response.setHits(1, 0, 1);
            response.setHttpCode(OK.getStatusCode());
            return Response.status(OK).entity(response).build();
        } catch (IllegalArgumentException | VitamThreadAccessException e) {
            Status status = Status.PRECONDITION_FAILED;
            LOGGER.error(e);
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    /**
     * Select objectgroups with json request
     *
     * @param request the request in JsonNode format
     * @return Response
     */
    @Path("objectgroups")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response selectObjectgroups(JsonNode request) {
        return selectObjectgroupsByQuery(request);
    }

    private Response selectObjectgroupsByQuery(JsonNode selectRequest) {
        Status status;
        try {
            RequestResponse<JsonNode> result;
            result = metaData.selectObjectGroupsByQuery(selectRequest);
            int st = result.isOk() ? Status.FOUND.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result.setHttpCode(st)).build();

        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    /**
     * Refresh ObjectGroup index
     *
     * @return Response
     */
    @Path("objectgroups")
    @PUT
    @Produces(APPLICATION_JSON)
    public Response refreshObjectGroup() {
        try {
            metaData.refreshObjectGroup();
            RequestResponseOK response = new RequestResponseOK();
            response.setHits(1, 0, 1);
            response.setHttpCode(OK.getStatusCode());
            return Response.status(OK).entity(response).build();
        } catch (IllegalArgumentException | VitamThreadAccessException e) {
            Status status = Status.PRECONDITION_FAILED;
            LOGGER.error(e);
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    /**
     * @param selectRequest the select request in JsonNode format
     * @param unitId the unit id to get
     * @return {@link Response} will be contains an json filled by unit result
     */
    @Path("units/{id_unit}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getUnitById(JsonNode selectRequest, @PathParam("id_unit") String unitId) {
        return selectUnitById(selectRequest, unitId);
    }

    /**
     * Update unit by query and path parameter unit_id
     *
     * @param updateRequest the update request
     * @param unitId the id of unit to be update
     * @return {@link Response} will be contains an json filled by unit result
     */
    @Path("units/{id_unit}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response updateUnitById(JsonNode updateRequest, @PathParam("id_unit") String unitId) {
        Status status;
        try {
            UpdateUnit result = metaData.updateUnitById(updateRequest, unitId);

            return Response.ok(new RequestResponseOK<UpdateUnit>().addResult(result)
                .setHttpCode(Status.OK.getStatusCode())).build();

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException | MetadataValidationException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    @Path("/units/computedInheritedRules")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response computedInheritedRulesCalculation(JsonNode dslQuery) {
        ParametersChecker.checkParameter("Missing request", dslQuery);

        String operationId = getVitamSession().getRequestId();

        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            String message = VitamLogbookMessages.getLabelOp(COMPUTE_INHERITED_RULES.getEventType() + ".STARTED") + " : " +
                GUIDReader.getGUID(operationId);
            LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                COMPUTE_INHERITED_RULES.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.COMPUTE_INHERITED_RULES,
                STARTED,
                message,
                GUIDReader.getGUID(operationId)
            );
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            workspaceClient.putObject(operationId, "query.json", writeToInpustream(dslQuery));

            processingClient.initVitamProcess(new ProcessingEntry(operationId, COMPUTE_INHERITED_RULES.name()));

            RequestResponse<JsonNode> response = processingClient.executeOperationProcess(operationId, COMPUTE_INHERITED_RULES.name(), RESUME.getValue());
            return response.setHttpCode(Status.OK.getStatusCode()).toResponse();
        } catch (BadRequestException e) {
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (InvalidGuidOperationException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException | ContentAddressableStorageServerException |
            InvalidParseOperationException | InternalServerException | VitamClientException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR,
                    String.format("An error occurred during %s workflow", PRESERVATION.getEventType())))
                .build();
        }
    }

    private void addRightsStatementIdentifier(LogbookOperationParameters initParameters) {
        ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
        rightsStatementIdentifier.put(ACCESS_CONTRACT, getVitamSession().getContractId());
        initParameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier,
            rightsStatementIdentifier.toString());
    }

    private VitamError getErrorEntity(Status status, String message) {
        String msg = getErrorStreamMessage(status, message);
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(CONTEXT_METADATA)
            .setState(CODE_VITAM)
            .setMessage(msg);
    }

    private String getErrorStreamMessage(Status status, String message) {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }

        if (status.getReasonPhrase() != null) {
            return status.getReasonPhrase();
        }

        return status.name();
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

    /**
     * Selects unit by request and unit id
     */
    private Response selectUnitById(JsonNode selectRequest, String unitId) {
        Status status;
        try {
            RequestResponse<JsonNode> result;
            result = metaData.selectUnitsById(selectRequest, unitId);

            int st = result.isOk() ? Status.FOUND.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result.setHttpCode(st)).build();

        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(e.getMessage())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
    }

    private Response metadataExecutionExceptionTrace(final Exception e) {
        Status status;
        LOGGER.error(e);
        status = Status.BAD_REQUEST;
        Throwable e2 = e.getCause();
        if (e2 instanceof IllegalArgumentException || e2 instanceof ElasticsearchParseException) {
            status = Status.PRECONDITION_FAILED;
        } else {
            e2 = e;
        }
        return Response.status(status)
            .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ACCESS)
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(e2.getMessage()))
            .build();
    }

    // OBJECT GROUP RESOURCE. TODO P1 see to externalize it (one resource for units, one resource for object group) to
    // avoid so much lines and complex maintenance

    /**
     * Create unit with json request
     *
     * @param insertRequest the insert query
     * @return the Response
     */
    @Path("objectgroups")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response insertObjectGroup(JsonNode insertRequest) {
        Status status;
        try {
            metaData.insertObjectGroup(insertRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }

        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK<JsonNode>(insertRequest)
                .setHits(1, 0, 1)
                .setHttpCode(Status.CREATED.getStatusCode()))
            .build();
    }

    /**
     * Create unit with json request
     *
     * @param insertRequests the insert query
     * @return the Response
     */
    @Path("objectgroups/bulk")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response insertObjectGroupBulk(List<JsonNode> insertRequests) {
        Status status;
        try {
            metaData.insertObjectGroups(insertRequests);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }

        ArrayNode arrayNode = JsonHandler.createArrayNode();
        insertRequests.forEach(arrayNode::add);

        RequestResponseOK responseOK = new RequestResponseOK(arrayNode);
        responseOK.setHits(arrayNode.size(), 0, arrayNode.size())
            .setHttpCode(Status.CREATED.getStatusCode());
        return Response.status(Status.CREATED)
            .entity(responseOK)
            .build();
    }

    /**
     * Get ObjectGroup
     *
     * @param selectRequest the request
     * @param objectGroupId the objectGroup ID to get
     * @return a response with the select query and the required object group (can be empty)
     */
    @Path("objectgroups/{id_og}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getObjectGroupById(JsonNode selectRequest, @PathParam("id_og") String objectGroupId) {
        Status status;
        try {
            ParametersChecker.checkParameter("Request select required", selectRequest);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return selectObjectGroupById(selectRequest, objectGroupId);
    }

    /**
     * Get ObjectGroup
     *
     * @param updateRequest the query to update the objectgroup
     * @param objectGroupId the objectGroup ID to get
     * @return a response with the select query and the required object group (can be empty)
     */
    @Path("objectgroups/{id_og}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response updateObjectGroupById(JsonNode updateRequest, @PathParam("id_og") String objectGroupId) {
        Status status;
        try {
            ParametersChecker.checkParameter("UpdateQuery required", updateRequest);
            ParametersChecker.checkParameter("ObjectGroupId required", objectGroupId);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        try {
            metaData.updateObjectGroupId(updateRequest, objectGroupId);

            return Response.status(Status.CREATED)
                .entity(new RequestResponseOK<String>().addResult(objectGroupId)
                    .setHttpCode(Status.CREATED.getStatusCode()))
                .build();

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException | MetadataValidationException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (MetaDataNotFoundException e) {
            LOGGER.error("Object group not found " + objectGroupId, e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    /**
     * Selects ObjectGroup by request and ObjectGroup id
     */
    private Response selectObjectGroupById(JsonNode selectRequest, String objectGroupId) {
        Status status;
        try {
            RequestResponse<JsonNode> result;
            result = metaData.selectObjectGroupById(selectRequest, objectGroupId);
            int st = result.isOk() ? OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result.setHttpCode(OK.getStatusCode())).build();

        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }

    @Path("accession-registers/units/{operationId}")
    @Produces(APPLICATION_JSON)
    @GET
    public Response selectAccessionRegisterOnUnitByOperationId(@PathParam("operationId") String operationId) {
        try {
            List<FacetBucket> documents =
                metaData.selectOwnAccessionRegisterOnUnitByOperationId(operationId);

            RequestResponseOK<FacetBucket> responseOK = new RequestResponseOK<>();
            responseOK.setHttpCode(Status.OK.getStatusCode());
            responseOK.addAllResults(documents);

            return responseOK.toResponse();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        }
    }

    @POST
    @Path("accession-registers/symbolic")
    @Produces(APPLICATION_JSON)
    public Response createAccessionRegisterSymbolic() {
        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            List<AccessionRegisterSymbolic> accessionRegisterSymbolic =
                metaData.createAccessionRegisterSymbolic(tenantId)
                    .stream()
                    .map(AccessionRegisterSymbolic::new)
                    .collect(Collectors.toList());

            return new RequestResponseOK<AccessionRegisterSymbolic>()
                .addAllResults(accessionRegisterSymbolic)
                .setHttpCode(OK.getStatusCode())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return new VitamError(String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode()))
                .setMessage(e.getMessage())
                .toResponse();
        }
    }

    @Path("accession-registers/objects/{operationId}")
    @Produces(APPLICATION_JSON)
    @GET
    public Response selectAccessionRegisterOnObjectGroupByOperationId(@PathParam("operationId") String operationId) {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        List<ObjectGroupPerOriginatingAgency> documents =
            metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(tenantId, operationId);

        RequestResponseOK<ObjectGroupPerOriginatingAgency> responseOK = new RequestResponseOK<>();
        responseOK.setHttpCode(Status.OK.getStatusCode());
        responseOK.addAllResults(documents);

        return responseOK.toResponse();
    }

    /**
     * Reindex a collection
     *
     * @param indexParameters parameters specifying what to reindex
     * @return Response
     */
    @Path("/reindex")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response reindex(IndexParameters indexParameters) {
        try {
            ParametersChecker.checkParameter("Parameters are mandatory", indexParameters);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(Status.PRECONDITION_FAILED.name())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        IndexationResult result = metaData.reindex(indexParameters);

        if (result.getIndexKO() == null || result.getIndexKO().isEmpty()) {
            // No KO -> 201
            return Response.status(Status.CREATED).entity(new RequestResponseOK()
                .setHttpCode(Status.CREATED.getStatusCode())).entity(result).build();
        }

        // OK and at least one KO -> 202
        if (result.getIndexOK() != null && !result.getIndexOK().isEmpty()) {
            return Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
                .setHttpCode(Status.ACCEPTED.getStatusCode())).entity(result).build();
        }

        // All KO -> 500
        Status status = Status.INTERNAL_SERVER_ERROR;
        VitamError error = VitamCodeHelper.toVitamError(VitamCode.METADATA_INDEXATION_ERROR,
            status.getReasonPhrase());
        return Response.status(status).entity(error).entity(result).build();
    }

    /**
     * Switch indexes
     *
     * @param switchIndexParameters
     * @return Response
     */
    @Path("/alias")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response changeIndexes(SwitchIndexParameters switchIndexParameters) {
        try {
            ParametersChecker.checkParameter("parameter is mandatory", switchIndexParameters);
            ParametersChecker.checkParameter("alias parameter is mandatory", switchIndexParameters.getAlias());
            ParametersChecker.checkParameter("indexName parameter is mandatory", switchIndexParameters.getIndexName());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(Status.PRECONDITION_FAILED.name())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        try {
            metaData.switchIndex(switchIndexParameters.getAlias(), switchIndexParameters.getIndexName());
            return Response.status(OK).entity(new RequestResponseOK().setHttpCode(OK.getStatusCode()))
                .build();
        } catch (DatabaseException exc) {
            VitamError error = VitamCodeHelper.toVitamError(VitamCode.METADATA_SWITCH_INDEX_ERROR,
                exc.getMessage());
            return Response.status(INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Select units with inherited rules
     *
     * @param selectRequest the select request in JsonNode format
     * @return {@link Response} will be contains an json filled by unit result
     */
    @Path("unitsWithInheritedRules")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response selectUnitsWithInheritedRules(JsonNode selectRequest) {

        Status status;
        try {
            RequestResponse<JsonNode> result = metadataRuleService.selectUnitsWithInheritedRules(selectRequest);

            int st = result.isOk() ? Status.FOUND.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result.setHttpCode(st)).build();

        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(ve.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();

        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
    }


    @DELETE
    @Path("objectGroups/bulkDelete")
    public Response deleteObjectGroups(List<String> ids) {
        Status status;
        JsonNode jsonNode;

        try {
            jsonNode = JsonHandler.toJsonNode(ids);

            metaData.deleteObjectGroups(ids);

        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        return Response.status(Status.OK)
            .entity(new RequestResponseOK<String>(jsonNode)
                .setHits(ids.size(), 0, 1)
                .setHttpCode(Status.OK.getStatusCode()))
            .build();

    }

    @DELETE
    @Path("units/bulkDelete")
    public Response deleteUnits(List<String> ids) {
        Status status;
        JsonNode jsonNode;
        try {
            jsonNode = JsonHandler.toJsonNode(ids);
            metaData.deleteUnits(ids);

        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(CONTEXT_METADATA)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }

        return Response.status(Status.OK)
            .entity(new RequestResponseOK<String>(jsonNode)
                .setHits(ids.size(), 0, 1)
                .setHttpCode(Status.OK.getStatusCode()))
            .build();
    }

}
