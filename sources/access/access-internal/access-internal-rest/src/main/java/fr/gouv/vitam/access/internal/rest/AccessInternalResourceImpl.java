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
package fr.gouv.vitam.access.internal.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalIllegalOperationException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.access.internal.core.ObjectGroupDipServiceImpl;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.UpdatePermissionException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.deserializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.LevelTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.TextByLangDeserializer;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.mapping.dip.DipService;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import fr.gouv.vitam.common.mapping.dip.UnitDipServiceImpl;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.revertupdate.RevertUpdateOptions;
import fr.gouv.vitam.common.model.storage.AccessRequestReference;
import fr.gouv.vitam.common.model.storage.StatusByAccessRequest;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataScrollLimitExceededException;
import fr.gouv.vitam.metadata.api.exception.MetadataScrollThresholdExceededException;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect;
import static fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForUpdate;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.model.WorkspaceConstants.OPTIONS_FILE;
import static fr.gouv.vitam.common.model.export.ExportRequest.EXPORT_QUERY_FILE_NAME;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.COMPUTE_INHERITED_RULES;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.COMPUTE_INHERITED_RULES_DELETE;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DELETE_GOT_VERSIONS;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.PRESERVATION;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.TRANSFER_REPLY;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("/access-internal/v1")
@Tag(name = "Access")
public class AccessInternalResourceImpl extends ApplicationStatusResource implements AccessInternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImpl.class);

    /**
     * UNITS
     */
    private static final String UNITS = "units";
    private static final String RESULTS = "$results";

    private static final String UNITS_URI = "/units";
    private static final String UNITS_ATOMIC_BULK_URI = "/units/atomicbulk";
    private static final String UNITS_RULES_URI = "/units/rules";

    private static final String ACCESS_CONTRACT = "AccessContract";
    private static final String REQUEST_IS_NOT_AN_UPDATE_OPERATION = "Request is not an update operation";
    private static final String WRITE_PERMISSION_NOT_ALLOWED = "Write permission not allowed";
    private static final String EMPTY_QUERY_IS_IMPOSSIBLE = "Empty query is impossible";
    private static final String DEBUG = "DEBUG {}";
    private static final String QUERY_FILE = "query.json";
    private static final String ACCESS_CONTRACT_FILE = "accessContract.json";

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
    private static final String START_SELECT_UNITS_DEBUG_MSG = "DEBUG: start selectUnits {}";

    private final AccessInternalModule accessModule;

    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    /**
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessInternalResourceImpl(AccessInternalConfiguration configuration) {
        this(new AccessInternalModuleImpl(), LogbookOperationsClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(), ProcessingManagementClientFactory.getInstance());
        WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getUrlProcessing());
    }

    @VisibleForTesting
    AccessInternalResourceImpl(LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory, StorageClientFactory storageClientFactory,
        WorkspaceClientFactory workspaceClientFactory, AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory) {
        this(new AccessInternalModuleImpl(logbookLifeCyclesClientFactory, logbookOperationsClientFactory,
                storageClientFactory,
                workspaceClientFactory, adminManagementClientFactory,
                metaDataClientFactory), logbookOperationsClientFactory, workspaceClientFactory,
            processingManagementClientFactory);
    }

    /**
     * Test constructor
     *
     * @param accessModule accessModule
     * @param logbookOperationsClientFactory logbookOperationsClientFactory
     * @param workspaceClientFactory workspaceClientFactory
     * @param processingManagementClientFactory processingManagementClientFactory
     */
    @VisibleForTesting
    private AccessInternalResourceImpl(AccessInternalModule accessModule,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory) {
        this.accessModule = accessModule;
        LOGGER.debug(ACCESS_RESOURCE_INITIALIZED);
        ObjectMapper objectMapper = buildObjectMapper();
        this.unitDipService = new UnitDipServiceImpl(new ArchiveUnitMapper(), objectMapper);
        this.objectDipService = new ObjectGroupDipServiceImpl(new ObjectGroupMapper(), objectMapper);
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
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
        JsonNode result = null;
        LOGGER.debug(START_SELECT_UNITS_DEBUG_MSG, queryDsl);
        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);
            result =
                accessModule
                    .selectUnit(applyAccessContractRestrictionForUnitForSelect(queryDsl,
                        getVitamSession().getContract()));
            LOGGER.debug(DEBUG, result);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (final Exception ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(UNITS)
                    .setState(CODE_VITAM)
                    .setMessage(ve.getMessage())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * get Archive Unit list by query based on identifier
     *
     * @param queryDsl as JsonNode
     * @return an archive unit result list
     */
    @Override
    @GET
    @Path("/units/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response streamUnits(JsonNode queryDsl) {
        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);
            return accessModule
                .streamUnits(applyAccessContractRestrictionForUnitForSelect(queryDsl, getVitamSession().getContract()));
        } catch (final MetadataScrollThresholdExceededException e) {
            return Response.status(Status.EXPECTATION_FAILED).build();
        } catch (final MetadataScrollLimitExceededException e) {
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (final Exception ve) {
            LOGGER.error(ve);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Select units with inherited rules
     *
     * @param queryDsl as JsonNode
     * @return an archive unit result list with inherited rules
     */
    @Override
    @GET
    @Path("/unitsWithInheritedRules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnitsWithInheritedRules(JsonNode queryDsl) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        JsonNode result;
        LOGGER.debug("DEBUG: start selectUnitsWithInheritedRules {}", queryDsl);
        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);
            result = accessModule.selectUnitsWithInheritedRules(
                applyAccessContractRestrictionForUnitForSelect(queryDsl,
                    getVitamSession().getContract()));
            LOGGER.debug(DEBUG, result);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (final Exception ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(UNITS)
                    .setState(CODE_VITAM)
                    .setMessage(ve.getMessage())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * get Archive Unit list by query based on identifier
     *
     * @param dslRequest as DipExportRequest
     * @return an archive unit result list
     */
    @Override
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportDIP(JsonNode dslRequest) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setDslRequest(dslRequest);
        exportRequest.setExportWithLogBookLFC(false);
        exportRequest.setExportType(ExportType.MinimalArchiveDeliveryRequestReply);

        return exportByRequest(exportRequest, false);
    }

    /**
     * get Archive Unit list by query based on identifier
     *
     * @param exportRequest as DipExportRequest
     * @return an archive unit result list
     */
    @Override
    @POST
    @Path("/export/usagefilter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportByUsageFilter(ExportRequest exportRequest) {
        return exportByRequest(exportRequest, true);
    }

    private Response exportByRequest(ExportRequest exportRequest, boolean applyFilter) {
        Status status;
        LOGGER.debug(START_SELECT_UNITS_DEBUG_MSG, exportRequest.getDslRequest());
        LOGGER.debug("DEBUG: usage list to export {}", exportRequest.getDataObjectVersionToExport());

        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            Contexts contexts;
            LogbookTypeProcess logbookTypeProcess;
            switch (exportRequest.getExportType()) {
                case ArchiveDeliveryRequestReply:
                case MinimalArchiveDeliveryRequestReply:
                    contexts = Contexts.EXPORT_DIP;
                    logbookTypeProcess = LogbookTypeProcess.EXPORT_DIP;
                    break;
                case ArchiveTransfer:
                    contexts = Contexts.ARCHIVE_TRANSFER;
                    logbookTypeProcess = LogbookTypeProcess.ARCHIVE_TRANSFER;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Not implemented ExportType :" + exportRequest.getExportType());
            }
            checkEmptyQuery(exportRequest.getDslRequest());
            String operationId = getVitamSession().getRequestId();

            final LogbookOperationParameters initParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    contexts.getEventType(),
                    GUIDReader.getGUID(operationId),
                    logbookTypeProcess,
                    STARTED,
                    VitamLogbookMessages.getLabelOp(contexts + ".STARTED") + " : " + GUIDReader.getGUID(operationId),
                    GUIDReader.getGUID(operationId));

            if (applyFilter) {
                // Add access contract rights
                addRightsStatementIdentifier(initParameters);
            }

            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(exportRequest)));

            JsonNode filteredQueryQsl = applyAccessContractRestrictionForUnitForSelect(exportRequest.getDslRequest(),
                getVitamSession().getContract());
            exportRequest.setDslRequest(filteredQueryQsl);
            // QUERY_FILE is required for the first step that check Threshold
            // EXPORT_QUERY_FILE_NAME is required for step
            workspaceClient.putObject(operationId, QUERY_FILE, writeToInpustream(filteredQueryQsl));
            workspaceClient.putObject(operationId, EXPORT_QUERY_FILE_NAME, writeToInpustream(exportRequest));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId, logbookTypeProcess,
                    OperationContextMonitor.OperationContextFileName);

            ProcessingEntry processingEntry = new ProcessingEntry(operationId, contexts.name());
            boolean mustLog = ActivationStatus.ACTIVE.equals(getVitamSession().getContract().getAccessLog());
            processingEntry.getExtraParams()
                .put(WorkerParameterName.mustLogAccessOnObject.name(), Boolean.toString(mustLog));
            processingClient.initVitamProcess(processingEntry);

            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationId, contexts.name(), RESUME.getValue());
            return jsonNodeRequestResponse.toResponse();
        } catch (ContentAddressableStorageServerException | OperationContextException |
            InvalidGuidOperationException | LogbookClientServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException |
            VitamClientException | InternalServerException | InvalidCreateOperationException e) {
            LOGGER.error("Error while generating " + exportRequest.getExportType(), e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        }
    }

    private void addRightsStatementIdentifier(LogbookOperationParameters initParameters) {
        ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
        rightsStatementIdentifier
            .put(ACCESS_CONTRACT, getVitamSession().getContract().getIdentifier());
        initParameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier,
            rightsStatementIdentifier.toString());
    }

    @POST
    @Path("/transfers/reply")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transferReply(InputStream transferReply) {
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            String operationId = getVitamSession().getRequestId();

            LogbookOperationParameters masterTransferReplyLfcEvent =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    TRANSFER_REPLY.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.TRANSFER_REPLY,
                    STARTED,
                    String.format("%s : %s", VitamLogbookMessages.getLabelOp("TRANSFER_REPLY.STARTED"),
                        GUIDReader.getGUID(operationId)),
                    GUIDReader.getGUID(operationId)
                );
            addRightsStatementIdentifier(masterTransferReplyLfcEvent);

            logbookOperationsClient.create(masterTransferReplyLfcEvent);

            workspaceClient.createContainer(operationId);
            String objectName = "ATR-for-transfer-reply-in-workspace.xml";
            workspaceClient.putObject(operationId, objectName, transferReply);

            workspaceClient.putObject(operationId, OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(objectName)));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId, LogbookTypeProcess.TRANSFER_REPLY,
                    OperationContextMonitor.OperationContextFileName, objectName);

            processingClient.initVitamProcess(operationId, TRANSFER_REPLY.name());
            return processingClient.executeOperationProcess(operationId, TRANSFER_REPLY.name(), RESUME.getValue())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        }
    }

    @Override
    @GET
    @Path("/dipexport/{id}/dip")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response findDIPByID(@PathParam("id") String id) {
        try {
            // check on Id is done by SanityCheckerCommonFilter
            return accessModule.findDIPByOperationId(id);
        } catch (AccessInternalExecutionException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    @Override
    @GET
    @Path("/transferexport/{id}/sip")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response findTransferSIPByID(@PathParam("id") String id) {
        try {
            return accessModule.findTransferSIPByOperationId(id);
        } catch (AccessInternalExecutionException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }


    /**
     * Starts a reclassification workflow.
     */
    @Override
    @POST
    @Path("/reclassification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startReclassificationWorkflow(JsonNode reclassificationRequestJson) {
        return startReclassificationWorkflow(reclassificationRequestJson, RESUME);
    }

    /**
     * @param reclassificationRequestJson
     * @param processAction
     * @return
     */
    public Response startReclassificationWorkflow(JsonNode reclassificationRequestJson, ProcessAction processAction) {
        Status status;

        try {

            ParametersChecker.checkParameter("Missing reclassification request", reclassificationRequestJson);

            // Start workflow
            String operationId = getVitamSession().getRequestId();

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
                LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                final LogbookOperationParameters initParameters =
                    LogbookParameterHelper.newLogbookOperationParameters(
                        GUIDReader.getGUID(operationId),
                        Contexts.RECLASSIFICATION.getEventType(),
                        GUIDReader.getGUID(operationId),
                        LogbookTypeProcess.RECLASSIFICATION,
                        STARTED,
                        VitamLogbookMessages.getLabelOp("RECLASSIFICATION.STARTED") + " : " +
                            GUIDReader.getGUID(operationId),
                        GUIDReader.getGUID(operationId));

                // Add access contract rights
                addRightsStatementIdentifier(initParameters);

                logbookOperationsClient.create(initParameters);

                workspaceClient.createContainer(operationId);

                String objectName = "request.json";
                workspaceClient.putObject(operationId, objectName,
                    writeToInpustream(reclassificationRequestJson));

                // store original query in workspace
                workspaceClient
                    .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                        OperationContextModel.get(reclassificationRequestJson)));

                // compress file to backup
                OperationContextMonitor
                    .compressInWorkspace(workspaceClientFactory, operationId, LogbookTypeProcess.RECLASSIFICATION,
                        OperationContextMonitor.OperationContextFileName);

                processingClient.initVitamProcess(operationId, Contexts.RECLASSIFICATION.name());

                RequestResponse<ItemStatus> jsonNodeRequestResponse =
                    processingClient.executeOperationProcess(operationId, Contexts.RECLASSIFICATION.name(),
                        processAction.getValue());
                return jsonNodeRequestResponse.toResponse();
            }

        } catch (ContentAddressableStorageServerException |
            InvalidGuidOperationException | LogbookClientServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException |
            VitamClientException | InternalServerException | OperationContextException e) {
            LOGGER.error("Error while starting unit reclassification workflow", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        }
    }

    /**
     * Starts a elimination analysis workflow.
     */
    @Override
    @POST
    @Path("/elimination/analysis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEliminationAnalysisWorkflow(EliminationRequestBody eliminationRequestBody) {

        return startEliminationWorkflow(eliminationRequestBody, Contexts.ELIMINATION_ANALYSIS);
    }

    /**
     * Starts a elimination action workflow.
     */
    @Override
    @POST
    @Path("/elimination/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEliminationActionWorkflow(EliminationRequestBody eliminationRequestBody) {

        return startEliminationWorkflow(eliminationRequestBody, Contexts.ELIMINATION_ACTION);
    }

    private Response startEliminationWorkflow(EliminationRequestBody eliminationRequestBody,
        Contexts eliminationWorkflowContext) {

        Status status;

        try {

            ParametersChecker.checkParameter("Missing elimination request", eliminationRequestBody);

            EliminationRequestBody eliminationRequestBodyWithAccessContractRestriction =
                new EliminationRequestBody(
                    eliminationRequestBody.getDate(),
                    applyAccessContractRestrictionForUnitForSelect(
                        eliminationRequestBody.getDslRequest().deepCopy(),
                        getVitamSession().getContract()));

            // Start workflow
            String operationId = getVitamSession().getRequestId();

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
                LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                final LogbookOperationParameters initParameters =
                    LogbookParameterHelper.newLogbookOperationParameters(
                        GUIDReader.getGUID(operationId),
                        eliminationWorkflowContext.getEventType(),
                        GUIDReader.getGUID(operationId),
                        LogbookTypeProcess.ELIMINATION,
                        STARTED,
                        VitamLogbookMessages.getLabelOp(eliminationWorkflowContext.getEventType() + ".STARTED") +
                            " : " +
                            GUIDReader.getGUID(operationId),
                        GUIDReader.getGUID(operationId));

                // Add access contract rights
                addRightsStatementIdentifier(initParameters);

                logbookOperationsClient.create(initParameters);

                workspaceClient.createContainer(operationId);

                workspaceClient.putObject(operationId, "request.json",
                    writeToInpustream(eliminationRequestBodyWithAccessContractRestriction));

                // store original query in workspace
                workspaceClient
                    .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                        OperationContextModel.get(eliminationRequestBody)));

                // compress file to backup
                OperationContextMonitor
                    .compressInWorkspace(workspaceClientFactory, operationId,
                        eliminationWorkflowContext.getLogbookTypeProcess(),
                        OperationContextMonitor.OperationContextFileName);

                processingClient.initVitamProcess(new ProcessingEntry(operationId, eliminationWorkflowContext.name()));

                RequestResponse<ItemStatus> jsonNodeRequestResponse =
                    processingClient
                        .executeOperationProcess(operationId, eliminationWorkflowContext.name(), RESUME.getValue());
                return jsonNodeRequestResponse.toResponse();
            }

        } catch (ContentAddressableStorageServerException | OperationContextException |
            InvalidGuidOperationException | LogbookClientServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | VitamClientException | InternalServerException e) {
            LOGGER.error("An error occurred during " + eliminationWorkflowContext.getEventType() + " workflow", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
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
            JsonNode result =
                accessModule
                    .selectUnitbyId(
                        applyAccessContractRestrictionForUnitForSelect(queryDsl,
                            getVitamSession().getContract()), idUnit);
            resetQuery(result, queryDsl);

            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
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

            JsonNode result =
                accessModule
                    .selectUnitbyId(
                        applyAccessContractRestrictionForUnitForSelect(queryDsl,
                            getVitamSession().getContract()), idUnit);
            ArrayNode results = (ArrayNode) result.get(RESULTS);
            JsonNode unit = results.get(0);
            Response responseXmlFormat = unitDipService.jsonToXml(unit, idUnit);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return responseXmlFormat;
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
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
            accessModule.checkClassificationLevel(queryDsl);
            JsonNode result = accessModule
                .updateUnitById(applyAccessContractRestrictionForUnitForUpdate(queryDsl,
                    getVitamSession().getContract()), idUnit, requestId);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return Response.status(Status.OK).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final AccessInternalRuleExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            return buildErrorResponse(VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CHECK_RULES, e.getMessage());
        } catch (final UpdatePermissionException e) {
            LOGGER.error(e.getMessage(), e);
            return buildErrorResponse(VitamCode.UPDATE_UNIT_PERMISSION, e.getMessage());
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
            JsonNode result = accessModule
                .selectObjectGroupById(AccessContractRestrictionHelper
                        .applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(query,
                            getVitamSession().getContract()),
                    idObjectGroup);
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
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
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
            final JsonNode result = accessModule.selectObjectGroupById(
                AccessContractRestrictionHelper.applyAccessContractRestrictionForObjectGroupForSelect(dslQuery,
                    getVitamSession().getContract()),
                objectId);
            ArrayNode results = (ArrayNode) result.get(RESULTS);
            JsonNode objectGroup = results.get(0);
            return objectDipService.jsonToXml(objectGroup, objectId);
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
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
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
            JsonNode result =
                accessModule
                    .selectUnitbyId(
                        applyAccessContractRestrictionForUnitForSelect(queryDsl,
                            getVitamSession().getContract()), idUnit);
            ArrayNode results = (ArrayNode) result.get(RESULTS);
            JsonNode objectGroup = results.get(0);

            Response responseXmlFormat = objectDipService.jsonToXml(objectGroup, idUnit);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
            return responseXmlFormat;
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(JsonHandler.unprettyPrint(getErrorEntity(status, e.getMessage())))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    private Optional<Response> checkObjectQualifierAndUsageParams(MultivaluedMap<String, String> multipleMap,
        String idObjectGroup) {
        if (!multipleMap.containsKey(GlobalDataRest.X_TENANT_ID) ||
            !multipleMap.containsKey(GlobalDataRest.X_QUALIFIER) ||
            !multipleMap.containsKey(GlobalDataRest.X_VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            return Optional.of(Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED,
                    "At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                        .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() +
                        ")"))
                .build());
        }
        final String xQualifier = multipleMap.get(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = multipleMap.get(GlobalDataRest.X_VERSION).get(0);

        if (!getVitamSession().getContract().isEveryDataObjectVersion() &&
            !validUsage(xQualifier.split("_")[0])) {
            return Optional.of(Response.status(Status.UNAUTHORIZED)
                .entity(getErrorStream(Status.UNAUTHORIZED, "Qualifier not allowed"))
                .build());
        }
        try {

            SanityChecker.checkHeadersMap(multipleMap);
            HttpHeaderHelper.checkVitamHeadersMap(multipleMap);
            SanityChecker.checkParameter(idObjectGroup);
            // Validate version format
            Integer.parseInt(xVersion);
            return Optional.empty();
        } catch (final IllegalStateException | InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Optional.of(Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage()))
                .build());
        }
    }

    private Response asyncObjectStream(MultivaluedMap<String, String> multipleMap,
        String idObjectGroup, String idUnit) {

        Optional<Response> errorResponse = checkObjectQualifierAndUsageParams(multipleMap, idObjectGroup);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        final String xQualifier = multipleMap.get(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = multipleMap.get(GlobalDataRest.X_VERSION).get(0);
        final int version = Integer.parseInt(xVersion);

        try {
            return accessModule.getOneObjectFromObjectGroup(idObjectGroup, xQualifier,
                version, idUnit);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage()))
                .build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        } catch (AccessInternalUnavailableDataFromAsyncOfferException e) {
            LOGGER.warn("Object " + xQualifier + "_" + xVersion + " of ObjectGroup: " + idObjectGroup + " / UnitId: " +
                idUnit + " is not currently available from async offer. Access request required", e);
            return Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER.getStatusCode()).build();
        } catch (StorageNotFoundException | MetaDataNotFoundException exc) {
            LOGGER.warn(exc);
            return Response.status(Status.NOT_FOUND).entity(getErrorStream(Status.NOT_FOUND, exc.getMessage())).build();
        }
    }

    private boolean validUsage(String s) {
        final VitamSession vitamSession = getVitamSession();
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

    private void checkEmptyQuery(JsonNode queryDsl)
        throws InvalidParseOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cant be empty");
        }
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectStreamAsync(@Context HttpHeaders headers,
        @PathParam("id_object_group") String idObjectGroup, @PathParam("id_unit") String idUnit) {
        MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();
        return asyncObjectStream(multipleMap, idObjectGroup, idUnit);
    }

    @Override
    @POST
    @Path("/objects/{id_object_group}/accessRequest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createObjectAccessRequestIfRequired(@Context HttpHeaders headers,
        @PathParam("id_object_group") String idObjectGroup) {
        MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();

        Optional<Response> errorResponse = checkObjectQualifierAndUsageParams(multipleMap, idObjectGroup);
        if (errorResponse.isPresent()) {
            return errorResponse.get();
        }

        try {
            final String xQualifier = headers.getHeaderString(GlobalDataRest.X_QUALIFIER);
            final int version = Integer.parseInt(headers.getHeaderString(GlobalDataRest.X_VERSION));

            Optional<AccessRequestReference> objectAccessRequest =
                accessModule.createObjectAccessRequestIfRequired(idObjectGroup, xQualifier, version);

            RequestResponseOK<AccessRequestReference> requestResponseOK = new RequestResponseOK<>();
            objectAccessRequest.ifPresent(requestResponseOK::addResult);
            requestResponseOK.setHttpCode(Status.OK.getStatusCode());
            return requestResponseOK.toResponse();
        } catch (final IllegalStateException | InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage())).build();
        } catch (MetaDataNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity(getErrorStream(Status.NOT_FOUND, exc.getMessage())).build();
        } catch (final Exception exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        }
    }

    @Override
    @GET
    @Path("/accessRequests/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAccessRequestStatuses(@Context HttpHeaders headers,
        List<AccessRequestReference> accessRequestReferences) {

        try {

            ParametersChecker.checkParameter("Tenant required", headers.getHeaderString(GlobalDataRest.X_TENANT_ID));
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

            List<StatusByAccessRequest> statusByAccessRequests =
                accessModule.checkAccessRequestStatuses(accessRequestReferences);

            return new RequestResponseOK<StatusByAccessRequest>()
                .addAllResults(statusByAccessRequests)
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();

        } catch (final IllegalStateException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage())).build();
        } catch (final AccessInternalIllegalOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_ACCEPTABLE)
                .entity(getErrorStream(Status.NOT_ACCEPTABLE, exc.getMessage())).build();
        } catch (final Exception exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        }
    }

    @Override
    @DELETE
    @Path("/accessRequests/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAccessRequest(@Context HttpHeaders headers,
        AccessRequestReference accessRequestReference) {

        try {

            ParametersChecker.checkParameter("Tenant required", headers.getHeaderString(GlobalDataRest.X_TENANT_ID));
            ParametersChecker.checkParameter("Access request required", accessRequestReference);
            ParametersChecker.checkParameter("Required accessRequestId", accessRequestReference.getAccessRequestId());
            ParametersChecker.checkParameter("Required storageStrategyId",
                accessRequestReference.getStorageStrategyId());

            accessModule.removeAccessRequest(accessRequestReference.getStorageStrategyId(),
                accessRequestReference.getAccessRequestId());

            return new RequestResponseOK<>()
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();

        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage())).build();
        } catch (final AccessInternalIllegalOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_ACCEPTABLE)
                .entity(getErrorStream(Status.NOT_ACCEPTABLE, exc.getMessage())).build();
        } catch (final Exception exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        }
    }

    @Override
    @GET
    @Path("/storageaccesslog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAccessLogStreamAsync(@Context HttpHeaders headers, JsonNode params) {
        MultivaluedMap<String, String> multipleMap = headers.getRequestHeaders();

        if (!multipleMap.containsKey(GlobalDataRest.X_TENANT_ID)) {
            LOGGER.error("Header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID.name() + ")");
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED,
                    "Header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID.name() + ")"))
                .build();
        }
        try {
            Integer tenantId = ParameterHelper.getTenantParameter();
            ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);

            return accessModule.getAccessLog(params);
        } catch (IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage()))
                .build();
        } catch (ParseException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(getErrorStream(Status.BAD_REQUEST, e.getMessage()))
                .build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(INTERNAL_SERVER_ERROR).entity(getErrorStream(INTERNAL_SERVER_ERROR,
                exc.getMessage())).build();
        } catch (StorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity(getErrorStream(Status.NOT_FOUND, exc.getMessage())).build();
        }
    }

    @Override
    @POST
    @Path(UNITS_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response massUpdateUnits(JsonNode queryDsl) {
        LOGGER.debug("Start mass updating archive units with Dsl query {}", queryDsl);
        Status status;
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            // Check sanity of json
            SanityChecker.checkJsonAll(queryDsl);

            // Check the writing rights
            if (getVitamSession().getContract().getWritingPermission() == null ||
                !getVitamSession().getContract().getWritingPermission()) {
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, WRITE_PERMISSION_NOT_ALLOWED)).build();
            }

            final RequestParserMultiple parser = RequestParserHelper.getParser(queryDsl);
            if (!(parser instanceof UpdateParserMultiple)) {
                parser.getRequest().reset();
                throw new IllegalArgumentException(REQUEST_IS_NOT_AN_UPDATE_OPERATION);
            }

            String operationId = getVitamSession().getRequestId();

            // Init logbook operation
            final LogbookOperationParameters initParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    Contexts.MASS_UPDATE_UNIT_DESC.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.MASS_UPDATE,
                    STARTED,
                    VitamLogbookMessages.getCodeOp(Contexts.MASS_UPDATE_UNIT_DESC.getEventType(), STARTED),
                    GUIDReader.getGUID(operationId));

            // Add access contract rights
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(queryDsl)));


            workspaceClient
                .putObject(operationId, QUERY_FILE, writeToInpustream(
                    applyAccessContractRestrictionForUnitForUpdate(queryDsl,
                        getVitamSession().getContract())));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    Contexts.MASS_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient.initVitamProcess(operationId, Contexts.MASS_UPDATE_UNIT_DESC.name());

            RequestResponse<ItemStatus> requestResponse =
                processingClient
                    .executeOperationProcess(operationId, Contexts.MASS_UPDATE_UNIT_DESC.name(), RESUME.getValue());
            return requestResponse.toResponse();
        } catch (ContentAddressableStorageServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | InvalidGuidOperationException |
            LogbookClientServerException | VitamClientException | InternalServerException |
            OperationContextException e) {
            LOGGER.error("An error occured while mass updating archive units", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException | BadRequestException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();

        }
    }

    @Override
    @POST
    @Path(UNITS_RULES_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response massUpdateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest) {
        JsonNode queryDsl = massUpdateUnitRuleRequest.getDslRequest().deepCopy();
        RuleActions ruleActions = massUpdateUnitRuleRequest.getRuleActions();

        // TODO : refactor with unitMassUpdate
        LOGGER.debug("Start mass updating archive units with Dsl query {}", queryDsl);
        Status status;
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            // Check sanity of json
            SanityChecker.checkJsonAll(queryDsl);

            // Check the writing rights

            if (!isAuthorized()) {
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, WRITE_PERMISSION_NOT_ALLOWED)).build();
            }
            String operationId = getVitamSession().getRequestId();

            // Init logbook operation
            final LogbookOperationParameters initParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    Contexts.MASS_UPDATE_UNIT_RULE.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.MASS_UPDATE,
                    STARTED,
                    VitamLogbookMessages
                        .getCodeOp(Contexts.MASS_UPDATE_UNIT_RULE.getEventType(), STARTED),
                    GUIDReader.getGUID(operationId));

            // Add access contract rights
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(massUpdateUnitRuleRequest)));


            workspaceClient
                .putObject(operationId, QUERY_FILE, writeToInpustream(
                    applyAccessContractRestrictionForUnitForUpdate(queryDsl,
                        getVitamSession().getContract())));
            workspaceClient
                .putObject(operationId, "actions.json", writeToInpustream(ruleActions));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    Contexts.MASS_UPDATE_UNIT_RULE.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);


            processingClient.initVitamProcess(operationId, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> requestResponse =
                processingClient
                    .executeOperationProcess(operationId, Contexts.MASS_UPDATE_UNIT_RULE.name(), RESUME.getValue());
            return requestResponse.toResponse();

        } catch (ContentAddressableStorageServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | InvalidGuidOperationException |
            LogbookClientServerException | VitamClientException | InternalServerException |
            OperationContextException e) {
            LOGGER.error("An error occured while mass updating archive units", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException | BadRequestException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    @Override
    @POST
    @Path(UNITS_ATOMIC_BULK_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkAtomicUpdateUnits(JsonNode query) {
        LOGGER.debug("Start bulk atomic updating archive units with Dsl query {}", query);
        Status status;
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            // Check the writing rights
            if (getVitamSession().getContract().getWritingPermission() == null ||
                !getVitamSession().getContract().getWritingPermission()) {
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, WRITE_PERMISSION_NOT_ALLOWED)).build();
            }

            String operationId = getVitamSession().getRequestId();

            // Init logbook operation
            final LogbookOperationParameters initParameters =
                LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.BULK_UPDATE,
                    STARTED,
                    VitamLogbookMessages.getCodeOp(Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getEventType(), STARTED),
                    GUIDReader.getGUID(operationId));

            // Add access contract rights
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(query)));

            workspaceClient
                .putObject(operationId, QUERY_FILE, writeToInpustream(
                    query));

            workspaceClient
                .putObject(operationId, ACCESS_CONTRACT_FILE, writeToInpustream(
                    JsonHandler.toJsonNode(getVitamSession().getContract())));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient.initVitamProcess(
                new ProcessingEntry(operationId, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name()));

            RequestResponse<ItemStatus> requestResponse =
                processingClient
                    .executeOperationProcess(operationId, Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC.name(),
                        RESUME.getValue());
            return requestResponse.toResponse();
        } catch (ContentAddressableStorageServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | InvalidGuidOperationException |
            LogbookClientServerException | VitamClientException | InternalServerException |
            OperationContextException e) {
            LOGGER.error("An error occured while bulk atomic updating archive units", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();

        }
    }


    @Override
    @POST
    @Path("/revert/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertUpdateUnits(RevertUpdateOptions revertUpdateOptions) {
        LOGGER.debug("Start reverting update archive units with Dsl query {}", revertUpdateOptions);
        Status status;
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            // Check the writing rights
            if (getVitamSession().getContract().getWritingPermission() == null ||
                !getVitamSession().getContract().getWritingPermission()) {
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, WRITE_PERMISSION_NOT_ALLOWED)).build();
            }

            String operationId = getVitamSession().getRequestId();

            // Init logbook operation
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                Contexts.REVERT_ESSENTIAL_METADATA.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.MASS_UPDATE,
                STARTED,
                VitamLogbookMessages.getCodeOp(Contexts.REVERT_ESSENTIAL_METADATA.getEventType(), STARTED),
                GUIDReader.getGUID(operationId));


            // Add access contract rights
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(revertUpdateOptions)));

            workspaceClient.putObject(operationId, OPTIONS_FILE, writeToInpustream(revertUpdateOptions));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    Contexts.REVERT_ESSENTIAL_METADATA.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient.initVitamProcess(operationId, Contexts.REVERT_ESSENTIAL_METADATA.name());

            RequestResponse<ItemStatus> requestResponse =
                processingClient
                    .executeOperationProcess(operationId, Contexts.REVERT_ESSENTIAL_METADATA.name(), RESUME.getValue());
            return requestResponse.toResponse();
        } catch (ContentAddressableStorageServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | InvalidGuidOperationException |
            LogbookClientServerException | VitamClientException | InternalServerException |
            OperationContextException e) {
            LOGGER.error("An error occured while reverting update archive units", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (InvalidParseOperationException | BadRequestException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();

        }
    }

    private boolean isAuthorized() {
        if (getVitamSession().getContract().getWritingPermission() == null ||
            getVitamSession().getContract().getWritingRestrictedDesc() == null) {
            return false;
        }
        return getVitamSession().getContract().getWritingPermission() &&
            !getVitamSession().getContract().getWritingRestrictedDesc();
    }


    @Path("/units/computedInheritedRules")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startComputeInheritedRules(JsonNode dslQuery) {
        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            ParametersChecker.checkParameter("Missing request", dslQuery);

            AccessContractModel contract = getVitamSession().getContract();
            JsonNode restrictedQuery = applyAccessContractRestrictionForUnitForSelect(dslQuery.deepCopy(), contract);

            if (!isAuthorized()) {
                return Response.status(Status.UNAUTHORIZED)
                    .entity(getErrorEntity(Status.UNAUTHORIZED, WRITE_PERMISSION_NOT_ALLOWED))
                    .build();
            }

            String operationId = VitamThreadUtils.getVitamSession().getRequestId();
            String message =
                VitamLogbookMessages.getLabelOp(COMPUTE_INHERITED_RULES.getEventType() + ".STARTED") + " : " +
                    GUIDReader.getGUID(operationId);
            LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
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

            workspaceClient.putObject(operationId, QUERY_FILE, writeToInpustream(restrictedQuery));

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(dslQuery)));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    Contexts.COMPUTE_INHERITED_RULES.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient.initVitamProcess(new ProcessingEntry(operationId, COMPUTE_INHERITED_RULES.name()));

            RequestResponse<ItemStatus> response = processingClient
                .executeOperationProcess(operationId, COMPUTE_INHERITED_RULES.name(), RESUME.getValue());
            return response.toResponse();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage()))
                .build();
        } catch (BadRequestException e) {
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (InvalidGuidOperationException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException | ContentAddressableStorageServerException | OperationContextException |
            InternalServerException | VitamClientException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR,
                    String.format("An error occurred during %s workflow", PRESERVATION.getEventType())))
                .build();
        }

    }



    @Path("/units/computedInheritedRules")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteComputeInheritedRules(JsonNode dslQuery) {

        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            ParametersChecker.checkParameter("Missing request", dslQuery);
            String operationId = getVitamSession().getRequestId();

            String message =
                VitamLogbookMessages.getLabelOp(COMPUTE_INHERITED_RULES_DELETE.getEventType() + ".STARTED") +
                    " : " +
                    GUIDReader.getGUID(operationId);

            LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                COMPUTE_INHERITED_RULES_DELETE.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.COMPUTE_INHERITED_RULES_DELETE,
                STARTED,
                message,
                GUIDReader.getGUID(operationId)
            );
            addRightsStatementIdentifier(initParameters);
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId);

            // store original query in workspace
            workspaceClient
                .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                    OperationContextModel.get(dslQuery)));


            AccessContractModel contract = getVitamSession().getContract();
            JsonNode restrictedQuery = applyAccessContractRestrictionForUnitForSelect(dslQuery, contract);
            //for CheckThresholdHandler
            workspaceClient.putObject(operationId, QUERY_FILE, writeToInpustream(restrictedQuery));

            // compress file to backup
            OperationContextMonitor
                .compressInWorkspace(workspaceClientFactory, operationId,
                    COMPUTE_INHERITED_RULES_DELETE.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName);

            processingClient
                .initVitamProcess(new ProcessingEntry(operationId, COMPUTE_INHERITED_RULES_DELETE.name()));

            return processingClient
                .executeOperationProcess(operationId, COMPUTE_INHERITED_RULES_DELETE.name(), RESUME.getValue())
                .toResponse();
        } catch (BadRequestException e) {
            LOGGER.error("Error on computedInheritedRules delete request", e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (Exception e) {
            LOGGER.error("Error on computedInheritedRules delete request", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR,
                    String
                        .format("An error occurred during %s workflow", COMPUTE_INHERITED_RULES_DELETE.getEventType())))
                .build();
        }
    }

    private VitamError getErrorEntity(Status status, String message) {
        String msg = getErrorStreamMessage(status, message);
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode()).setContext(ACCESS_MODULE)
            .setState(CODE_VITAM)
            .setMessage(msg)
            .setDescription(msg);
    }

    private InputStream getErrorStream(Status status, String message) {
        String msg = getErrorStreamMessage(status, message);
        try {
            return writeToInpustream(new VitamError(status.name())
                .setHttpCode(status.getStatusCode())
                .setContext(ACCESS_MODULE)
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(msg));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
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

    private void resetQuery(JsonNode result, JsonNode queryDsl) {
        if (result != null && result.has(RequestResponseOK.TAG_CONTEXT)) {
            ((ObjectNode) result).set(RequestResponseOK.TAG_CONTEXT, queryDsl);
        }
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());

        mapper.registerModule(module);

        return mapper;
    }

    /**
     * get Groups Objects list based on DSL query
     *
     * @param queryDsl as JsonNode
     * @return a group objects result list
     */
    @Override
    @GET
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjects(JsonNode queryDsl) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        JsonNode result = null;
        LOGGER.debug("DEBUG: start selectObjects {}", queryDsl);
        try {
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);
            result =
                accessModule
                    .selectObjects(AccessContractRestrictionHelper
                        .applyAccessContractRestrictionForObjectGroupForSelect(queryDsl,
                            getVitamSession().getContract()));
            LOGGER.debug(DEBUG, result);
            resetQuery(result, queryDsl);
            LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (final Exception ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(UNITS)
                    .setState(CODE_VITAM)
                    .setMessage(ve.getMessage())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    @Path("/preservation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startPreservation(PreservationRequest preservationRequest) {

        try {
            ParametersChecker.checkParameter("Missing request", preservationRequest);
            String operationId = getVitamSession().getRequestId();

            AccessContractModel contract = getVitamSession().getContract();
            JsonNode dslQuery = preservationRequest.getDslQuery().deepCopy();
            JsonNode restrictedQuery = applyAccessContractRestrictionForUnitForSelect(dslQuery, contract);

            PreservationRequest restrictedRequest =
                new PreservationRequest(restrictedQuery, preservationRequest.getScenarioIdentifier(),
                    preservationRequest.getTargetUsage(), preservationRequest.getVersion(),
                    preservationRequest.getSourceUsage());

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
                LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                String message = VitamLogbookMessages.getLabelOp(PRESERVATION.getEventType() + ".STARTED") + " : " +
                    GUIDReader.getGUID(operationId);

                LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    PRESERVATION.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.PRESERVATION,
                    STARTED,
                    message,
                    GUIDReader.getGUID(operationId)
                );
                addRightsStatementIdentifier(initParameters);
                logbookOperationsClient.create(initParameters);

                workspaceClient.createContainer(operationId);
                workspaceClient.putObject(operationId, "preservationRequest", writeToInpustream(restrictedRequest));

                //for CheckThresholdHandler
                workspaceClient
                    .putObject(operationId, QUERY_FILE, writeToInpustream(restrictedRequest.getDslQuery()));

                // store original query in workspace
                workspaceClient
                    .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                        OperationContextModel.get(preservationRequest)));


                // compress file to backup
                OperationContextMonitor
                    .compressInWorkspace(workspaceClientFactory, operationId, PRESERVATION.getLogbookTypeProcess(),
                        OperationContextMonitor.OperationContextFileName);

                processingClient.initVitamProcess(new ProcessingEntry(operationId, PRESERVATION.name()));

                return processingClient
                    .executeOperationProcess(operationId, PRESERVATION.name(), RESUME.getValue())
                    .toResponse();
            }
        } catch (BadRequestException e) {
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (Exception e) {
            LOGGER.error("Error on preservation request", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR,
                    String.format("An error occurred during %s workflow", PRESERVATION.getEventType())))
                .build();
        }
    }

    @Path("/deleteGotVersions")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGotVersions(DeleteGotVersionsRequest deleteGotVersionsRequest) {

        try {
            ParametersChecker.checkParameter("Missing request", deleteGotVersionsRequest);

            String operationId = getVitamSession().getRequestId();
            AccessContractModel contract = getVitamSession().getContract();

            JsonNode dslQuery = deleteGotVersionsRequest.getDslQuery().deepCopy();
            JsonNode restrictedQuery = applyAccessContractRestrictionForUnitForSelect(dslQuery, contract);

            DeleteGotVersionsRequest restrictedRequest =
                new DeleteGotVersionsRequest(restrictedQuery, deleteGotVersionsRequest.getUsageName(),
                    deleteGotVersionsRequest.getSpecificVersions());

            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
                LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
                WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                String message =
                    VitamLogbookMessages.getLabelOp(DELETE_GOT_VERSIONS.getEventType() + ".STARTED") + " : " +
                        GUIDReader.getGUID(operationId);

                LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                    GUIDReader.getGUID(operationId),
                    DELETE_GOT_VERSIONS.getEventType(),
                    GUIDReader.getGUID(operationId),
                    LogbookTypeProcess.DELETE_GOT_VERSIONS,
                    STARTED,
                    message,
                    GUIDReader.getGUID(operationId)
                );
                addRightsStatementIdentifier(initParameters);
                logbookOperationsClient.create(initParameters);

                workspaceClient.createContainer(operationId);
                workspaceClient.putObject(operationId, "deleteGotVersionsRequest",
                    writeToInpustream(restrictedRequest));

                //for CheckThresholdHandler
                workspaceClient
                    .putObject(operationId, QUERY_FILE, writeToInpustream(restrictedRequest.getDslQuery()));

                // store original query in workspace
                workspaceClient
                    .putObject(operationId, OperationContextMonitor.OperationContextFileName, writeToInpustream(
                        OperationContextModel.get(deleteGotVersionsRequest)));


                // compress file to backup
                OperationContextMonitor
                    .compressInWorkspace(workspaceClientFactory, operationId,
                        DELETE_GOT_VERSIONS.getLogbookTypeProcess(),
                        OperationContextMonitor.OperationContextFileName);

                processingClient.initVitamProcess(new ProcessingEntry(operationId, DELETE_GOT_VERSIONS.name()));

                return processingClient
                    .executeOperationProcess(operationId, DELETE_GOT_VERSIONS.name(), RESUME.getValue())
                    .toResponse();
            }
        } catch (BadRequestException e) {
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (Exception e) {
            LOGGER.error("Error on delete got versions request", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR,
                    String.format("An error occurred during %s workflow", DELETE_GOT_VERSIONS.getEventType())))
                .build();
        }
    }
}
