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
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.GraphComputeResponse.GraphComputeAction;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.model.ReclassificationChildNodeExportRequest;
import fr.gouv.vitam.metadata.core.ExportsPurge.ExportsPurgeService;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.graph.GraphComputeServiceImpl;
import fr.gouv.vitam.metadata.core.graph.ReclassificationDistributionService;
import fr.gouv.vitam.metadata.core.graph.api.GraphComputeService;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.COMPUTE_INHERITED_RULES;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.PRESERVATION;
import static fr.gouv.vitam.metadata.core.ExportsPurge.ExportsPurgeService.DIP_CONTAINER;
import static fr.gouv.vitam.metadata.core.ExportsPurge.ExportsPurgeService.TRANSFERS_CONTAINER;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1")
@Tag(name = "Metadata")
public class MetadataManagementResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataManagementResource.class);

    public static final String OBJECTGROUP = "OBJECTGROUP";
    public static final String UNIT = "UNIT";

    private static final String CODE_VITAM = "code_vitam";

    private static final String UNIT_OBJECTGROUP = UNIT + "_" + OBJECTGROUP;
    private static final String EXPORT_RECLASSIFICATION_CHILD_NODES = "exportReclassificationChildNodes";

    private static final String COMPUTE_GRAPH_URI = "/computegraph";
    private static final String COMPUTE_GRAPH_PROGRESS_URI = "/computegraph/progress";
    private static final String COMPUTED_INHERITED_RULES_OBSOLETE_URI =
        "/units/computedInheritedRules/processObsoletes";
    private static final String PURGE_EXPIRED_DIP_FILES_URI = "/purgeDIP";
    private static final String PURGE_EXPIRED_TRANSFER_SIP_FILES_URI = "/purgeTransfersSIP";
    private static final String MIGRATION_PURGE_EXPIRED_FROM_OFFERS = "/migrationDeleteDipFromOffers";

    /**
     * Error/Exceptions messages.
     */
    private static final String COMPUTE_GRAPH_EXCEPTION_MSG = "ERROR: Exception has been thrown when compute graph: ";
    private static final String ERROR_MSG = "{\"ErrorMsg\":\"";

    private final GraphComputeService graphComputeService;
    private final ReclassificationDistributionService reclassificationDistributionService;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final ExportsPurgeService exportsPurgeService;

    MetadataManagementResource(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metadata,
        MetaDataConfiguration configuration,
        ElasticsearchMetadataIndexManager indexManager
    ) {
        this(
            GraphComputeServiceImpl.initialize(vitamRepositoryProvider, metadata, indexManager),
            new ReclassificationDistributionService(metadata),
            ProcessingManagementClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(),
            configuration,
            new ExportsPurgeService(configuration.getTimeToLiveConfiguration())
        );
    }

    @VisibleForTesting
    MetadataManagementResource(
        GraphComputeService graphComputeService,
        ReclassificationDistributionService reclassificationDistributionService,
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        MetaDataConfiguration configuration,
        ExportsPurgeService exportsPurgeService
    ) {
        this.graphComputeService = graphComputeService;
        this.reclassificationDistributionService = reclassificationDistributionService;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.exportsPurgeService = exportsPurgeService;

        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getUrlProcessing());
    }

    /**
     * API to access and launch the Vitam graph builder service for metadata.<br/>
     *
     * @return the response
     */
    @Path(COMPUTE_GRAPH_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response computeGraphByDSL(@HeaderParam(GlobalDataRest.X_TENANT_ID) Integer xTenantId, JsonNode queryDsl) {
        try {
            ParametersChecker.checkParameter("X_TENANT_ID header is required and mustn't be null", xTenantId);
            VitamThreadUtils.getVitamSession().setTenantId(xTenantId);

            VitamThreadUtils.getVitamSession().initIfAbsent(xTenantId);

            GraphComputeResponse response = this.graphComputeService.computeGraph(queryDsl);
            return Response.ok()
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .entity(response)
                .build();
        } catch (Exception e) {
            LOGGER.error(COMPUTE_GRAPH_EXCEPTION_MSG, e);
            return Response.serverError().entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Check if graph builder is in progress.<br/>
     *
     * @return the response
     */
    @Path(COMPUTE_GRAPH_PROGRESS_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response computeGraphByDSLInProgress(@HeaderParam(GlobalDataRest.X_TENANT_ID) Integer xTenantId) {
        ParametersChecker.checkParameter("X_TENANT_ID header is required and mustn't be null", xTenantId);
        VitamThreadUtils.getVitamSession().setTenantId(xTenantId);

        VitamThreadUtils.getVitamSession().initIfAbsent(xTenantId);

        boolean inProgress = this.graphComputeService.isInProgress();
        if (inProgress) {
            LOGGER.info("Graph compute in progress ...");
            return Response.ok("{\"msg\": \"Graph compute in progress ...\"}").build();
        } else {
            LOGGER.info("No active graph builder");
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"msg\": \"No active graph compute service\"}")
                .build();
        }
    }

    /**
     * API to access and launch the Vitam graph builder service for metadata.<br/>
     *
     * @return the response
     */
    @Path(COMPUTE_GRAPH_URI + "/{collection:" + UNIT + "|" + OBJECTGROUP + "|" + UNIT_OBJECTGROUP + "}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response computeGraph(@PathParam("collection") GraphComputeAction action, Set<String> documentsId) {
        try {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

            MetadataCollections metadataCollections = MetadataCollections.UNIT;
            boolean computeObjectGroupGraph = GraphComputeAction.UNIT_OBJECTGROUP.equals(action);

            if (GraphComputeAction.OBJECTGROUP.equals(action)) {
                metadataCollections = MetadataCollections.OBJECTGROUP;
            }

            GraphComputeResponse response =
                this.graphComputeService.computeGraph(metadataCollections, documentsId, computeObjectGroupGraph, true);
            return Response.ok().entity(response).build();
        } catch (Exception e) {
            LOGGER.error(COMPUTE_GRAPH_EXCEPTION_MSG, e);
            return Response.serverError().entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Export child nodes of units to reclassify for graph update into workspaces.
     *
     * @return the response (200 or KO)
     */
    @Path(EXPORT_RECLASSIFICATION_CHILD_NODES)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportReclassificationChildNodes(ReclassificationChildNodeExportRequest request) {
        try {
            this.reclassificationDistributionService.exportReclassificationChildNodes(
                    request.getUnitIds(),
                    request.getUnitsToUpdateJsonLineFileName(),
                    request.getObjectGroupsToUpdateJsonLineFileName()
                );

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Could not export child nodes for reclassification graph update", e);
            return Response.serverError().entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }

    @Path(COMPUTED_INHERITED_RULES_OBSOLETE_URI)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response processObsoleteComputedInheritedRules() {
        try {
            boolean isError = false;
            for (int tenant : VitamConfiguration.getTenants()) {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                JsonNode dslQuery = getObsoleteComputedInheritedRulesDsl();
                Response response = computedInheritedRulesCalculation(dslQuery);
                if (!response.getStatusInfo().equals(Response.Status.ACCEPTED)) {
                    isError = true;
                }
            }

            if (isError) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(ACCEPTED).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return Response.status(BAD_REQUEST).entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }

    @Path(PURGE_EXPIRED_DIP_FILES_URI)
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response purgeExpiredDipFiles() {
        try {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

            this.exportsPurgeService.purgeExpiredFiles(DIP_CONTAINER);

            return Response.status(OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.METADATA_INTERNAL_SERVER_ERROR, e.getMessage()).toResponse();
        }
    }

    @Path(PURGE_EXPIRED_TRANSFER_SIP_FILES_URI)
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response purgeExpiredTransfersSIPFiles() {
        try {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

            this.exportsPurgeService.purgeExpiredFiles(TRANSFERS_CONTAINER);

            return Response.status(OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.METADATA_INTERNAL_SERVER_ERROR, e.getMessage()).toResponse();
        }
    }

    @Path(MIGRATION_PURGE_EXPIRED_FROM_OFFERS)
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response migrationPurgeDipFilesFromOffers() {
        try {
            for (Integer tenant : VitamConfiguration.getTenants()) {
                LOGGER.info("Running DIP cleanup from offers for tenant " + tenant);

                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenant));

                this.exportsPurgeService.migrationPurgeDipFilesFromOffers();

                LOGGER.info("Running DIP finished successfully");
            }

            return Response.status(OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.METADATA_INTERNAL_SERVER_ERROR, e.getMessage()).toResponse();
        }
    }

    private JsonNode getObsoleteComputedInheritedRulesDsl() throws InvalidCreateOperationException {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        BooleanQuery obsoleteQuery = QueryHelper.or();
        obsoleteQuery.add(QueryHelper.eq(VitamFieldsHelper.validComputedInheritedRules(), false));

        BooleanQuery incoherentValuesQuery = QueryHelper.and();
        incoherentValuesQuery.add(
            QueryHelper.not().add(QueryHelper.exists(VitamFieldsHelper.computedInheritedRules()))
        );
        incoherentValuesQuery.add(QueryHelper.exists(VitamFieldsHelper.validComputedInheritedRules()));

        obsoleteQuery.add(incoherentValuesQuery);
        selectMultiQuery.setQuery(obsoleteQuery);
        selectMultiQuery.setThreshold(VitamConfiguration.getComputedInheritedRulesThreshold());
        return selectMultiQuery.getFinalSelect();
    }

    private Response computedInheritedRulesCalculation(JsonNode dslQuery) {
        GUID operationGuid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            LOGGER.debug("Accessing computedInheritedRulesCalculation");
            String message =
                VitamLogbookMessages.getLabelOp(COMPUTE_INHERITED_RULES.getEventType() + ".STARTED") +
                " : " +
                operationGuid;
            LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                operationGuid,
                COMPUTE_INHERITED_RULES.getEventType(),
                operationGuid,
                LogbookTypeProcess.COMPUTE_INHERITED_RULES,
                STARTED,
                message,
                operationGuid
            );
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationGuid.getId());

            workspaceClient.putObject(
                operationGuid.getId(),
                OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(dslQuery))
            );
            workspaceClient.putObject(operationGuid.getId(), "query.json", writeToInpustream(dslQuery));

            OperationContextMonitor.compressInWorkspace(
                workspaceClientFactory,
                operationGuid.getId(),
                Contexts.COMPUTE_INHERITED_RULES.getLogbookTypeProcess(),
                OperationContextMonitor.OperationContextFileName
            );

            processingClient.initVitamProcess(
                new ProcessingEntry(operationGuid.getId(), COMPUTE_INHERITED_RULES.name())
            );

            RequestResponse<ItemStatus> response = processingClient.executeOperationProcess(
                operationGuid.getId(),
                COMPUTE_INHERITED_RULES.name(),
                RESUME.getValue()
            );
            LOGGER.debug("End computedInheritedRulesCalculation");
            return response.toResponse();
        } catch (BadRequestException e) {
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, null);
        } catch (
            LogbookClientBadRequestException
            | LogbookClientAlreadyExistsException
            | LogbookClientServerException
            | ContentAddressableStorageServerException
            | InvalidParseOperationException
            | InternalServerException
            | VitamClientException
            | OperationContextException e
        ) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(
                    getErrorEntity(
                        INTERNAL_SERVER_ERROR,
                        String.format("An error occurred during %s workflow", PRESERVATION.getEventType())
                    )
                )
                .build();
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String msg = getErrorStreamMessage(status, message);
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(ServiceName.METADATA.getName())
            .setState(CODE_VITAM)
            .setMessage(msg);
    }

    private String getErrorStreamMessage(Response.Status status, String message) {
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
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(VitamCodeHelper.getCode(vitamCode))
                            .setContext(vitamCode.getService().getName())
                            .setState(vitamCode.getDomain().getName())
                            .setMessage(vitamCode.getMessage())
                            .setDescription(description)
                    )
                    .toString()
            )
            .build();
    }
}
