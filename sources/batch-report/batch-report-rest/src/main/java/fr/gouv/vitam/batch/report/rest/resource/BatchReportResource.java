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
package fr.gouv.vitam.batch.report.rest.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.exception.BatchReportException;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsComputedDetails;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.batch.report.model.entry.TransferReplyUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UpdateUnitMetadataReportEntry;
import fr.gouv.vitam.batch.report.rest.service.BatchReportServiceImpl;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.BadRequestException;
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
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/batchreport/v1")
@Tag(name = "Batch-Report")
public class BatchReportResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BatchReportResource.class);

    private static final String EMPTY_PROCESSID_ERROR_MESSAGE = "processId should be filed";
    private static final TypeReference<ReportBody<AuditObjectGroupReportEntry>> reportAuditType = new TypeReference<
        ReportBody<AuditObjectGroupReportEntry>
    >() {};
    private static final TypeReference<ReportBody<EliminationActionUnitReportEntry>> reportEliminationActionUnitType =
        new TypeReference<ReportBody<EliminationActionUnitReportEntry>>() {};
    private static final TypeReference<ReportBody<PurgeUnitReportEntry>> reportPurgeUnitType = new TypeReference<
        ReportBody<PurgeUnitReportEntry>
    >() {};
    private static final TypeReference<ReportBody<PurgeObjectGroupReportEntry>> reportPurgeObjectGroupType =
        new TypeReference<ReportBody<PurgeObjectGroupReportEntry>>() {};
    private static final TypeReference<ReportBody<TransferReplyUnitReportEntry>> reportTransferReplyUnitType =
        new TypeReference<ReportBody<TransferReplyUnitReportEntry>>() {};
    private static final TypeReference<ReportBody<PreservationReportEntry>> reportPreservationType = new TypeReference<
        ReportBody<PreservationReportEntry>
    >() {};
    private static final TypeReference<ReportBody<UpdateUnitMetadataReportEntry>> reportMassUpdateType =
        new TypeReference<ReportBody<UpdateUnitMetadataReportEntry>>() {};
    private static final TypeReference<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportBulkUpdateMetadataType =
        new TypeReference<ReportBody<BulkUpdateUnitMetadataReportEntry>>() {};
    private static final TypeReference<ReportBody<EvidenceAuditReportEntry>> reportEvidenceAuditType =
        new TypeReference<ReportBody<EvidenceAuditReportEntry>>() {};
    private static final TypeReference<
        ReportBody<UnitComputedInheritedRulesInvalidationReportEntry>
    > unitComputedInheritedRuleInvalidationType = new TypeReference<
        ReportBody<UnitComputedInheritedRulesInvalidationReportEntry>
    >() {};
    private static final TypeReference<ReportBody<DeleteGotVersionsReportEntry>> reportDeleteGotVersionsType =
        new TypeReference<>() {};

    private BatchReportServiceImpl batchReportServiceImpl;

    public BatchReportResource(BatchReportServiceImpl batchReportServiceImpl) {
        this.batchReportServiceImpl = batchReportServiceImpl;
    }

    @Path("/append")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response appendReport(JsonNode body, @HeaderParam(GlobalDataRest.X_TENANT_ID) int tenantId) {
        try {
            String type = body.get("reportType").asText();
            ReportType reportType = ReportType.valueOf(type);
            switch (reportType) {
                case ELIMINATION_ACTION_UNIT:
                    ReportBody<EliminationActionUnitReportEntry> eliminationUnitReportBody =
                        JsonHandler.getFromJsonNode(body, reportEliminationActionUnitType);
                    batchReportServiceImpl.appendEliminationActionUnitReport(
                        eliminationUnitReportBody.getProcessId(),
                        eliminationUnitReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case PURGE_UNIT:
                    ReportBody<PurgeUnitReportEntry> purgeUnitReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportPurgeUnitType
                    );
                    batchReportServiceImpl.appendPurgeUnitReport(
                        purgeUnitReportBody.getProcessId(),
                        purgeUnitReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case PURGE_OBJECTGROUP:
                    ReportBody<PurgeObjectGroupReportEntry> purgeObjectGroupReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportPurgeObjectGroupType
                    );
                    batchReportServiceImpl.appendPurgeObjectGroupReport(
                        purgeObjectGroupReportBody.getProcessId(),
                        purgeObjectGroupReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case TRANSFER_REPLY_UNIT:
                    ReportBody<TransferReplyUnitReportEntry> transferReplyUnitReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportTransferReplyUnitType
                    );
                    batchReportServiceImpl.appendTransferReplyUnitReport(
                        transferReplyUnitReportBody.getProcessId(),
                        transferReplyUnitReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case PRESERVATION:
                    ReportBody<PreservationReportEntry> preservationReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportPreservationType
                    );
                    batchReportServiceImpl.appendPreservationReport(
                        preservationReportBody.getProcessId(),
                        preservationReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case AUDIT:
                    ReportBody<AuditObjectGroupReportEntry> auditReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportAuditType
                    );
                    batchReportServiceImpl.appendAuditReport(
                        auditReportBody.getProcessId(),
                        auditReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case UPDATE_UNIT:
                    ReportBody<UpdateUnitMetadataReportEntry> unitReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportMassUpdateType
                    );
                    batchReportServiceImpl.appendUnitReport(unitReportBody.getEntries());
                    break;
                case BULK_UPDATE_UNIT:
                    ReportBody<BulkUpdateUnitMetadataReportEntry> bulkUnitMetadataReportBody =
                        JsonHandler.getFromJsonNode(body, reportBulkUpdateMetadataType);
                    batchReportServiceImpl.appendBulkUpdateUnitMetadataReport(bulkUnitMetadataReportBody.getEntries());
                    break;
                case EVIDENCE_AUDIT:
                    ReportBody<EvidenceAuditReportEntry> evidenceAuditReportBody = JsonHandler.getFromJsonNode(
                        body,
                        reportEvidenceAuditType
                    );
                    batchReportServiceImpl.appendEvidenceAuditReport(
                        evidenceAuditReportBody.getProcessId(),
                        evidenceAuditReportBody.getEntries(),
                        tenantId
                    );
                    break;
                case TRACEABILITY:
                    ReportBody<TraceabilityReportEntry> traceabilityReportEntry = JsonHandler.getFromJsonNode(
                        body,
                        ReportBody.class,
                        TraceabilityReportEntry.class
                    );
                    batchReportServiceImpl.appendTraceabilityReport(
                        traceabilityReportEntry.getProcessId(),
                        traceabilityReportEntry.getEntries(),
                        tenantId
                    );
                    break;
                case UNIT_COMPUTED_INHERITED_RULES_INVALIDATION:
                    ReportBody<UnitComputedInheritedRulesInvalidationReportEntry> unitInvalidationReportEntry =
                        JsonHandler.getFromJsonNode(body, unitComputedInheritedRuleInvalidationType);
                    batchReportServiceImpl.appendUnitComputedInheritedRulesInvalidationReport(
                        unitInvalidationReportEntry.getProcessId(),
                        unitInvalidationReportEntry.getEntries(),
                        tenantId
                    );
                    break;
                case DELETE_GOT_VERSIONS:
                    ReportBody<DeleteGotVersionsReportEntry> deleteGotVersionsReportEntryReportBody =
                        JsonHandler.getFromJsonNode(body, reportDeleteGotVersionsType);
                    batchReportServiceImpl.appendDeleteGotVersionsReport(
                        deleteGotVersionsReportEntryReportBody.getEntries()
                    );
                    break;
                default:
                    throw new IllegalStateException("Unsupported report type " + reportType);
            }
            return Response.status(Response.Status.CREATED).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (BatchReportException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
        }
    }

    @Path("/storeToWorkspace")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeReportToWorkspace(Report reportInfo) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        ParametersChecker.checkParameter(EMPTY_PROCESSID_ERROR_MESSAGE, reportInfo.getOperationSummary().getEvId());
        ParametersChecker.checkParameter("tenantId should be filed", reportInfo.getOperationSummary().getTenant());
        if (tenantId != reportInfo.getOperationSummary().getTenant()) {
            throw new IllegalArgumentException(
                "Tenant id in request should match header. Header: " +
                tenantId +
                ", request: " +
                reportInfo.getOperationSummary().getTenant() +
                "."
            );
        }

        try {
            batchReportServiceImpl.storeFileToWorkspace(reportInfo);
            return Response.status(OK).build();
        } catch (InvalidParseOperationException | IllegalArgumentException | IllegalPathException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ContentAddressableStorageServerException | IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Path("/purge_unit/objectgroup_export/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportDistinctObjectGroup(
        @PathParam("processId") String processId,
        ReportExportRequest reportExportRequest
    ) throws ContentAddressableStorageServerException, IOException {
        try {
            ParametersChecker.checkParameter(EMPTY_PROCESSID_ERROR_MESSAGE, processId);

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            batchReportServiceImpl.exportPurgeDistinctObjectGroupOfDeletedUnits(
                processId,
                reportExportRequest.getFilename(),
                tenantId
            );
            return Response.status(OK).build();
        } catch (IllegalArgumentException | IllegalPathException e) {
            throw new BadRequestException(e);
        }
    }

    @Path("computedInheritedRulesInvalidation/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportUnitsToInvalidate(
        @PathParam("processId") String processId,
        ReportExportRequest reportExportRequest
    ) throws Exception {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        batchReportServiceImpl.exportUnitsToInvalidate(processId, tenantId, reportExportRequest);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/purge/accession_register_export/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportPurgeAccessionRegister(
        @PathParam("processId") String processId,
        ReportExportRequest reportExportRequest
    ) throws ContentAddressableStorageServerException, IOException {
        try {
            ParametersChecker.checkParameter(EMPTY_PROCESSID_ERROR_MESSAGE, processId);

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            batchReportServiceImpl.exportPurgeAccessionRegister(processId, reportExportRequest.getFilename(), tenantId);

            return Response.status(OK).build();
        } catch (InvalidParseOperationException | IllegalArgumentException | IllegalPathException e) {
            throw new BadRequestException(e);
        }
    }

    @Path("/cleanup/{reportType}/{processId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteReport(
        @PathParam("processId") String processId,
        @PathParam("reportType") ReportType reportType
    ) {
        try {
            ParametersChecker.checkParameter("ProcessId, reportType should be filed", processId, reportType);

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            switch (reportType) {
                case ELIMINATION_ACTION_UNIT:
                    batchReportServiceImpl.deleteEliminationUnitByProcessId(processId, tenantId);
                    break;
                case PURGE_UNIT:
                    batchReportServiceImpl.deletePurgeUnitByProcessId(processId, tenantId);
                    break;
                case PURGE_OBJECTGROUP:
                    batchReportServiceImpl.deletePurgeObjectGroupByIdAndTenant(processId, tenantId);
                    break;
                case TRANSFER_REPLY_UNIT:
                    batchReportServiceImpl.deleteTransferReplyUnitByProcessId(processId, tenantId);
                    break;
                case PRESERVATION:
                    batchReportServiceImpl.deletePreservationByIdAndTenant(processId, tenantId);
                    break;
                case AUDIT:
                    batchReportServiceImpl.deleteAuditByIdAndTenant(processId, tenantId);
                    break;
                case UPDATE_UNIT:
                    batchReportServiceImpl.deleteUpdateUnitByIdAndTenant(processId, tenantId);
                    break;
                case BULK_UPDATE_UNIT:
                    batchReportServiceImpl.deleteBulkUpdateUnitMetadataByIdAndTenant(processId, tenantId);
                    break;
                case EVIDENCE_AUDIT:
                    batchReportServiceImpl.deleteEvidenceAuditByIdAndTenant(processId, tenantId);
                    break;
                case TRACEABILITY:
                    batchReportServiceImpl.deleteTraceabilityByIdAndTenant(processId, tenantId);
                    break;
                case UNIT_COMPUTED_INHERITED_RULES_INVALIDATION:
                    batchReportServiceImpl.deleteUnitComputedInheritedRulesInvalidationReport(processId, tenantId);
                    break;
                case DELETE_GOT_VERSIONS:
                    batchReportServiceImpl.deleteGotVersionsByIdAndTenant(processId, tenantId);
                    break;
                default:
                    Response.Status status = Response.Status.BAD_REQUEST;
                    VitamError vitamError = new VitamError(status.name())
                        .setHttpCode(status.getStatusCode())
                        .setMessage("Report type not found")
                        .setDescription("Report type not found");
                    return Response.status(status).entity(vitamError).build();
            }
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    @Path("/storeExtractedMetadataForAu")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeExtractedMetadataForAu(List<ExtractedMetadata> extractedMetadatas) {
        batchReportServiceImpl.storeExtractedMetadataForAu(extractedMetadatas);
        return Response.status(OK).build();
    }

    @Path("/createExtractedMetadataDistributionFileForAu/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createExtractedMetadataDistributionFileForAu(@PathParam("processId") String processId) {
        try {
            batchReportServiceImpl.createExtractedMetadataDistributionFileForAu(
                processId,
                VitamThreadUtils.getVitamSession().getTenantId()
            );
            return Response.ok().build();
        } catch (IOException | ContentAddressableStorageServerException | IllegalPathException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/readComputedDetailsFromReport")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response readComputedDetailsFromReport(JsonNode body) {
        try {
            ReportBody reportBody = JsonHandler.getFromJsonNode(body, ReportBody.class);
            if (reportBody == null) {
                throw new IllegalStateException("Incorrect body report!");
            }
            switch (reportBody.getReportType()) {
                case DELETE_GOT_VERSIONS:
                    List<DeleteGotVersionsComputedDetails> results =
                        batchReportServiceImpl.readDeletedGotVersionsComputedDetailsFromReport(
                            reportBody.getProcessId(),
                            VitamThreadUtils.getVitamSession().getTenantId()
                        );
                    return Response.ok().entity(results).build();
                default:
                    throw new IllegalStateException("Unsupported report type " + reportBody.getReportType());
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
