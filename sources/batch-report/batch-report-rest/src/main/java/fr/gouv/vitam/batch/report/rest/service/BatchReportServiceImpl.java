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
package fr.gouv.vitam.batch.report.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.exception.BatchReportException;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditObjectModel;
import fr.gouv.vitam.batch.report.model.EvidenceStatus;
import fr.gouv.vitam.batch.report.model.MergeSortedIterator;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel;
import fr.gouv.vitam.batch.report.model.PurgeObjectGroupModel;
import fr.gouv.vitam.batch.report.model.PurgeUnitModel;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.TraceabilityObjectModel;
import fr.gouv.vitam.batch.report.model.TransferReplyUnitModel;
import fr.gouv.vitam.batch.report.model.UnitComputedInheritedRulesInvalidationModel;
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
import fr.gouv.vitam.batch.report.rest.repository.AuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.BulkUpdateUnitMetadataReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.DeleteGotVersionsReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.EvidenceAuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.ExtractedMetadataRepository;
import fr.gouv.vitam.batch.report.rest.repository.PreservationReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.PurgeUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.TraceabilityReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.TransferReplyUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.UnitComputedInheritedRulesInvalidationRepository;
import fr.gouv.vitam.batch.report.rest.repository.UpdateUnitReportRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.OPI;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.ORIGINATING_AGENCY;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECTS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECT_GROUPS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_SIZE;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_UNITS;

public class BatchReportServiceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BatchReportServiceImpl.class);
    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String REPORT_JSONL = "report.jsonl";

    private final WorkspaceClientFactory workspaceClientFactory;

    private final EliminationActionUnitRepository eliminationActionUnitRepository;
    private final PurgeUnitRepository purgeUnitRepository;
    private final PurgeObjectGroupRepository purgeObjectGroupRepository;
    private final TransferReplyUnitRepository transferReplyUnitRepository;
    private final PreservationReportRepository preservationReportRepository;
    private final UpdateUnitReportRepository updateUnitReportRepository;
    private final BulkUpdateUnitMetadataReportRepository bulkUpdateUnitMetadataReportRepository;
    private final AuditReportRepository auditReportRepository;
    private final UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository;
    private final EvidenceAuditReportRepository evidenceAuditReportRepository;
    private final TraceabilityReportRepository traceabilityReportRepository;
    private final ExtractedMetadataRepository extractedMetadataRepository;
    private final DeleteGotVersionsReportRepository deleteGotVersionsReportRepository;

    public BatchReportServiceImpl(
        WorkspaceClientFactory workspaceClientFactory,
        EliminationActionUnitRepository eliminationActionUnitRepository,
        PurgeUnitRepository purgeUnitRepository,
        PurgeObjectGroupRepository purgeObjectGroupRepository,
        TransferReplyUnitRepository transferReplyUnitRepository,
        UpdateUnitReportRepository updateUnitReportRepository,
        BulkUpdateUnitMetadataReportRepository bulkUpdateUnitMetadataReportRepository,
        PreservationReportRepository preservationReportRepository,
        AuditReportRepository auditReportRepository,
        UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository,
        EvidenceAuditReportRepository evidenceAuditReportRepository,
        TraceabilityReportRepository traceabilityReportRepository,
        ExtractedMetadataRepository extractedMetadataRepository,
        DeleteGotVersionsReportRepository deleteGotVersionsReportRepository
    ) {
        this.eliminationActionUnitRepository = eliminationActionUnitRepository;
        this.purgeUnitRepository = purgeUnitRepository;
        this.purgeObjectGroupRepository = purgeObjectGroupRepository;
        this.transferReplyUnitRepository = transferReplyUnitRepository;
        this.updateUnitReportRepository = updateUnitReportRepository;
        this.bulkUpdateUnitMetadataReportRepository = bulkUpdateUnitMetadataReportRepository;
        this.workspaceClientFactory = workspaceClientFactory;
        this.preservationReportRepository = preservationReportRepository;
        this.auditReportRepository = auditReportRepository;
        this.unitComputedInheritedRulesInvalidationRepository = unitComputedInheritedRulesInvalidationRepository;
        this.evidenceAuditReportRepository = evidenceAuditReportRepository;
        this.traceabilityReportRepository = traceabilityReportRepository;
        this.extractedMetadataRepository = extractedMetadataRepository;
        this.deleteGotVersionsReportRepository = deleteGotVersionsReportRepository;
    }

    public void appendEliminationActionUnitReport(
        String processId,
        List<EliminationActionUnitReportEntry> entries,
        int tenantId
    ) {
        List<EliminationActionUnitModel> documents = entries
            .stream()
            .map(
                unitEntry ->
                    new EliminationActionUnitModel(
                        processId,
                        tenantId,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                        unitEntry
                    )
            )
            .collect(Collectors.toList());
        eliminationActionUnitRepository.bulkAppendReport(documents);
    }

    public void appendPurgeUnitReport(String processId, List<PurgeUnitReportEntry> entries, int tenantId) {
        List<PurgeUnitModel> documents = entries
            .stream()
            .map(
                unitEntry ->
                    new PurgeUnitModel(
                        processId,
                        tenantId,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                        unitEntry
                    )
            )
            .collect(Collectors.toList());
        purgeUnitRepository.bulkAppendReport(documents);
    }

    public void appendPurgeObjectGroupReport(
        String processId,
        List<PurgeObjectGroupReportEntry> entries,
        int tenantId
    ) {
        List<PurgeObjectGroupModel> documents = entries
            .stream()
            .map(
                ogEntry ->
                    new PurgeObjectGroupModel(
                        processId,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                        ogEntry,
                        tenantId
                    )
            )
            .collect(Collectors.toList());
        purgeObjectGroupRepository.bulkAppendReport(documents);
    }

    public void appendTransferReplyUnitReport(
        String processId,
        List<TransferReplyUnitReportEntry> entries,
        int tenantId
    ) {
        List<TransferReplyUnitModel> documents = entries
            .stream()
            .map(
                unitEntry ->
                    new TransferReplyUnitModel(
                        processId,
                        tenantId,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                        unitEntry
                    )
            )
            .collect(Collectors.toList());
        transferReplyUnitRepository.bulkAppendReport(documents);
    }

    public void appendPreservationReport(
        String processId,
        List<PreservationReportEntry> preservationEntries,
        int tenantId
    ) throws BatchReportException {
        List<PreservationReportEntry> documents = preservationEntries
            .stream()
            .map(
                preservationEntry -> checkValuesAndGetNewPreservationReportEntry(processId, tenantId, preservationEntry)
            )
            .collect(Collectors.toList());
        preservationReportRepository.bulkAppendReport(documents);
    }

    public void appendUnitReport(List<UpdateUnitMetadataReportEntry> unitEntries) {
        updateUnitReportRepository.bulkAppendReport(unitEntries);
    }

    public void appendBulkUpdateUnitMetadataReport(List<BulkUpdateUnitMetadataReportEntry> unitEntries) {
        bulkUpdateUnitMetadataReportRepository.bulkAppendReport(unitEntries);
    }

    public void appendUnitComputedInheritedRulesInvalidationReport(
        String processId,
        List<UnitComputedInheritedRulesInvalidationReportEntry> unitEntries,
        int tenantId
    ) throws BatchReportException {
        List<UnitComputedInheritedRulesInvalidationModel> documents = unitEntries
            .stream()
            .map(
                entry ->
                    new UnitComputedInheritedRulesInvalidationModel(
                        processId,
                        tenantId,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                        entry
                    )
            )
            .collect(Collectors.toList());
        unitComputedInheritedRulesInvalidationRepository.bulkAppendReport(documents);
    }

    public void appendDeleteGotVersionsReport(List<DeleteGotVersionsReportEntry> unitEntries) {
        deleteGotVersionsReportRepository.bulkAppendReport(unitEntries);
    }

    public void deleteUnitComputedInheritedRulesInvalidationReport(String processId, int tenantId) {
        unitComputedInheritedRulesInvalidationRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void exportUnitsToInvalidate(String processId, int tenantId, ReportExportRequest reportExportRequest)
        throws IOException, ContentAddressableStorageServerException, IllegalPathException {
        File file = createTemporaryFile(processId, reportExportRequest.getFilename());

        try {
            try (
                OutputStream outputStream = new FileOutputStream(file);
                JsonLineWriter jsonLineWriter = new JsonLineWriter(outputStream);
                CloseableIterator<Document> units =
                    unitComputedInheritedRulesInvalidationRepository.findCollectionByProcessIdTenant(
                        processId,
                        tenantId
                    )
            ) {
                while (units.hasNext()) {
                    Document unit = units.next();
                    jsonLineWriter.addEntry(new JsonLineModel((String) unit.get("id")));
                }
            }

            storeFileToWorkspace(processId, reportExportRequest.getFilename(), file);
        } finally {
            deleteQuietly(file.getParentFile());
        }
    }

    private PreservationReportEntry checkValuesAndGetNewPreservationReportEntry(
        String processId,
        int tenantId,
        PreservationReportEntry entry
    ) {
        checkIfPresent("UnitId", entry.getUnitId());
        checkIfPresent("ObjectGroupId", entry.getObjectGroupId());
        checkIfPresent("Action", entry.getAction());
        checkIfPresent("InputObjectId", entry.getInputObjectId());
        checkIfPresent("GriffinId", entry.getGriffinId());

        return new PreservationReportEntry(
            GUIDFactory.newGUID().toString(),
            processId,
            tenantId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            entry.getStatus(),
            entry.getUnitId(),
            entry.getObjectGroupId(),
            entry.getAction(),
            entry.getAnalyseResult(),
            entry.getInputObjectId(),
            entry.getOutputObjectId(),
            entry.getOutcome(),
            entry.getGriffinId(),
            entry.getPreservationScenarioId()
        );
    }

    public void appendAuditReport(String processId, List<AuditObjectGroupReportEntry> auditEntries, int tenantId) {
        List<AuditObjectGroupModel> documents = auditEntries
            .stream()
            .map(auditEntry -> checkValuesAndGetAuditObjectGroupModel(processId, tenantId, auditEntry))
            .collect(Collectors.toList());
        auditReportRepository.bulkAppendReport(documents);
    }

    public void appendEvidenceAuditReport(String processId, List<EvidenceAuditReportEntry> auditEntries, int tenantId)
        throws BatchReportException {
        List<EvidenceAuditObjectModel> documents = auditEntries
            .stream()
            .map(auditEntry -> checkValuesAndGetEvidenceAuditObjectGroupModel(processId, tenantId, auditEntry))
            .collect(Collectors.toList());
        evidenceAuditReportRepository.bulkAppendReport(documents);
    }

    public void appendTraceabilityReport(String processId, List<TraceabilityReportEntry> entries, int tenantId) {
        List<TraceabilityObjectModel> documents = entries
            .stream()
            .peek(this::checkTraceabilityObjectModelValues)
            .map(reportEntry -> new TraceabilityObjectModel(processId, reportEntry, tenantId))
            .collect(Collectors.toList());
        traceabilityReportRepository.bulkAppendReport(documents);
    }

    private void checkTraceabilityObjectModelValues(TraceabilityReportEntry traceabilityReportEntry) {
        checkIfPresent("operationId", traceabilityReportEntry.getOperationId());
        checkIfPresent("operationType", traceabilityReportEntry.getOperationType());
        checkIfPresent("status", traceabilityReportEntry.getStatus());
    }

    private AuditObjectGroupModel checkValuesAndGetAuditObjectGroupModel(
        String processId,
        int tenantId,
        AuditObjectGroupReportEntry auditEntry
    ) {
        checkIfPresent("DetailId", auditEntry.getDetailId());
        checkIfPresent("Outcome", auditEntry.getOutcome());
        checkIfPresent("DetailType", auditEntry.getDetailType());
        checkIfPresent("Opi", auditEntry.getOpi());
        checkIfPresent("OriginatingAgency", auditEntry.getOriginatingAgency());
        checkIfPresent("ParentUnitIds", auditEntry.getParentUnitIds());
        checkIfPresent("Status", auditEntry.getStatus());

        return new AuditObjectGroupModel(
            processId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            auditEntry,
            tenantId
        );
    }

    private EvidenceAuditObjectModel checkValuesAndGetEvidenceAuditObjectGroupModel(
        String processId,
        int tenantId,
        EvidenceAuditReportEntry evidenceAuditEntry
    ) {
        checkIfPresent("identifier", evidenceAuditEntry.getIdentifier());
        checkIfPresent("status", evidenceAuditEntry.getEvidenceStatus());
        checkIfPresent("message", evidenceAuditEntry.getMessage());
        checkIfPresent("strategyId", evidenceAuditEntry.getStrategyId());
        checkIfPresent("objectType", evidenceAuditEntry.getObjectType());

        return new EvidenceAuditObjectModel(
            processId,
            tenantId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            evidenceAuditEntry
        );
    }

    private void checkIfPresent(String name, Object value) throws BatchReportException {
        if (value == null) {
            throw new BatchReportException(String.format("field Name mandatory %s", name));
        }
    }

    private void writeDocumentsInFile(JsonLineWriter reportWriter, MongoCursor<Document> documentsIterator)
        throws IOException {
        while (documentsIterator.hasNext()) {
            Document document = documentsIterator.next();
            reportWriter.addEntry(document);
        }
    }

    private JsonNode getExtendedInfo(Report reportInfo) throws InvalidParseOperationException {
        switch (reportInfo.getReportSummary().getReportType()) {
            case PRESERVATION:
                return JsonHandler.toJsonNode(
                    preservationReportRepository.stats(
                        reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()
                    )
                );
            case AUDIT:
                return JsonHandler.toJsonNode(
                    auditReportRepository.stats(
                        reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()
                    )
                );
            case EVIDENCE_AUDIT:
                return JsonHandler.toJsonNode(
                    evidenceAuditReportRepository.stats(
                        reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()
                    )
                );
            case TRACEABILITY:
                return JsonHandler.toJsonNode(
                    traceabilityReportRepository.stats(
                        reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()
                    )
                );
            default:
                return reportInfo.getReportSummary().getExtendedInfo();
        }
    }

    private ReportResults getReportResults(Report reportInfo) {
        switch (reportInfo.getReportSummary().getReportType()) {
            case AUDIT:
                return auditReportRepository.computeVitamResults(
                    reportInfo.getOperationSummary().getEvId(),
                    reportInfo.getOperationSummary().getTenant()
                );
            case EVIDENCE_AUDIT:
                return evidenceAuditReportRepository.computeVitamResults(
                    reportInfo.getOperationSummary().getEvId(),
                    reportInfo.getOperationSummary().getTenant()
                );
            case TRACEABILITY:
                return traceabilityReportRepository.computeVitamResults(
                    reportInfo.getOperationSummary().getEvId(),
                    reportInfo.getOperationSummary().getTenant()
                );
            case DELETE_GOT_VERSIONS:
                return deleteGotVersionsReportRepository.computeVitamResults(
                    reportInfo.getOperationSummary().getEvId(),
                    reportInfo.getOperationSummary().getTenant()
                );
            default:
                return reportInfo.getReportSummary().getVitamResults();
        }
    }

    public void storeFileToWorkspace(Report reportInfo)
        throws IOException, ContentAddressableStorageServerException, InvalidParseOperationException, IllegalPathException {
        OperationSummary operationSummary = reportInfo.getOperationSummary();
        String processId = operationSummary.getEvId();
        int tenantId = operationSummary.getTenant();

        ReportSummary reportSummary = reportInfo.getReportSummary();
        reportSummary.setExtendedInfo(getExtendedInfo(reportInfo));
        reportSummary.setVitamResults(getReportResults(reportInfo));

        File tempReport = createTemporaryFile(processId, REPORT_JSONL);

        try (JsonLineWriter reportWriter = new JsonLineWriter(new FileOutputStream(tempReport))) {
            reportWriter.addEntry(operationSummary);
            reportWriter.addEntry(reportSummary);
            reportWriter.addEntry(reportInfo.getContext());

            switch (reportSummary.getReportType()) {
                case ELIMINATION_ACTION: {
                    // ELIMINATION_ACTION report will contain :
                    // - ELIMINATION_ACTION_UNIT entries
                    // - PURGE_UNIT entries
                    // - PURGE_OBJECTGROUP entries
                    MongoCursor<Document> eliminationArchiveUnitIterator =
                        eliminationActionUnitRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, eliminationArchiveUnitIterator);

                    MongoCursor<Document> purgeUnitIterator = purgeUnitRepository.findCollectionByProcessIdTenant(
                        processId,
                        tenantId
                    );
                    writeDocumentsInFile(reportWriter, purgeUnitIterator);

                    MongoCursor<Document> objectGroupIterator =
                        purgeObjectGroupRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, objectGroupIterator);
                    break;
                }
                case TRANSFER_REPLY: {
                    // TRANSFER_REPLY report will contain :
                    // - TRANSFER_REPLY_UNIT entries
                    // - PURGE_UNIT entries
                    // - PURGE_OBJECTGROUP entries
                    MongoCursor<Document> transferReplyUnitIterator =
                        transferReplyUnitRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, transferReplyUnitIterator);

                    MongoCursor<Document> purgeUnitIterator = purgeUnitRepository.findCollectionByProcessIdTenant(
                        processId,
                        tenantId
                    );
                    writeDocumentsInFile(reportWriter, purgeUnitIterator);

                    MongoCursor<Document> objectGroupIterator =
                        purgeObjectGroupRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, objectGroupIterator);
                    break;
                }
                case PRESERVATION:
                    MongoCursor<Document> preservationIterator =
                        preservationReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, preservationIterator);
                    break;
                case AUDIT:
                    MongoCursor<Document> auditIterator =
                        auditReportRepository.findCollectionByProcessIdTenantAndStatus(
                            processId,
                            tenantId,
                            "WARNING",
                            "KO"
                        );
                    writeDocumentsInFile(reportWriter, auditIterator);
                    break;
                case EVIDENCE_AUDIT:
                    MongoCursor<Document> evidenceAuditIterator =
                        evidenceAuditReportRepository.findCollectionByProcessIdTenantAndStatus(
                            processId,
                            tenantId,
                            EvidenceStatus.WARN.name(),
                            EvidenceStatus.KO.name()
                        );
                    writeDocumentsInFile(reportWriter, evidenceAuditIterator);
                    break;
                case TRACEABILITY:
                    MongoCursor<Document> traceabilityIterator = traceabilityReportRepository.findCollection(
                        processId,
                        tenantId
                    );
                    writeDocumentsInFile(reportWriter, traceabilityIterator);
                    break;
                case UPDATE_UNIT:
                    MongoCursor<Document> updates = updateUnitReportRepository.findCollectionByProcessIdTenant(
                        processId,
                        tenantId
                    );
                    writeDocumentsInFile(reportWriter, updates);
                    break;
                case BULK_UPDATE_UNIT:
                    MongoCursor<Document> bulkUpdates =
                        bulkUpdateUnitMetadataReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, bulkUpdates);
                    break;
                case DELETE_GOT_VERSIONS:
                    MongoCursor<Document> deleteGotVersionsIterator =
                        deleteGotVersionsReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, deleteGotVersionsIterator);
                    break;
                default:
                    throw new UnsupportedOperationException(
                        String.format("Unsupported report type : '%s'.", reportSummary.getReportType())
                    );
            }
        }

        try {
            storeFileToWorkspace(processId, REPORT_JSONL, tempReport);
        } finally {
            deleteQuietly(tempReport.getParentFile());
        }
    }

    public List<DeleteGotVersionsComputedDetails> readDeletedGotVersionsComputedDetailsFromReport(
        String processId,
        int tenantId
    ) throws InvalidParseOperationException {
        List<DeleteGotVersionsComputedDetails> deleteGotVersionsComputedDetails = new ArrayList<>();
        MongoCursor<Document> deleteGotVersionsIteratorComputed =
            deleteGotVersionsReportRepository.computeDeleteGotVersionsEntriesByProcessIdTenant(processId, tenantId);
        while (deleteGotVersionsIteratorComputed.hasNext()) {
            deleteGotVersionsComputedDetails.add(
                (BsonHelper.fromDocumentToObject(
                        deleteGotVersionsIteratorComputed.next(),
                        DeleteGotVersionsComputedDetails.class
                    ))
            );
        }
        return deleteGotVersionsComputedDetails;
    }

    private void createFileFromTwoMongoCursorWithDocument(
        File tempFile,
        MongoCursor<Document> unitCursor,
        MongoCursor<Document> objectGroupCursor
    ) throws IOException, InvalidParseOperationException {
        Comparator<Document> comparator = compareDocument();

        BiFunction<Document, Document, PurgeAccessionRegisterModel> mergeFunction = mergeDocuments();

        MergeSortedIterator<Document, PurgeAccessionRegisterModel> mergeSortedIterator = new MergeSortedIterator<>(
            unitCursor,
            objectGroupCursor,
            comparator,
            mergeFunction
        );

        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
            while (mergeSortedIterator.hasNext()) {
                PurgeAccessionRegisterModel purgeAccessionRegisterModel = mergeSortedIterator.next();
                JsonLineModel jsonLineModel = new JsonLineModel();
                jsonLineModel.setId(purgeAccessionRegisterModel.getOpi());

                jsonLineModel.setParams(JsonHandler.toJsonNode(purgeAccessionRegisterModel));
                jsonLineWriter.addEntry(jsonLineModel);
            }
        }
    }

    private BiFunction<Document, Document, PurgeAccessionRegisterModel> mergeDocuments() {
        return (unit, objectGroup) -> {
            PurgeAccessionRegisterModel purgeAccessionRegisterModel = new PurgeAccessionRegisterModel();

            if (unit != null) {
                purgeAccessionRegisterModel.setOpi(unit.getString(OPI));
                purgeAccessionRegisterModel.setOriginatingAgency(unit.getString(ORIGINATING_AGENCY));
                purgeAccessionRegisterModel.setTotalUnits(((Number) unit.get(TOTAL_UNITS)).longValue());
            }

            if (objectGroup != null) {
                purgeAccessionRegisterModel.setOpi(objectGroup.getString(OPI));
                purgeAccessionRegisterModel.setOriginatingAgency(objectGroup.getString(ORIGINATING_AGENCY));
                purgeAccessionRegisterModel.setTotalObjectGroups(
                    ((Number) objectGroup.get(TOTAL_OBJECT_GROUPS)).longValue()
                );
                purgeAccessionRegisterModel.setTotalObjects(((Number) objectGroup.get(TOTAL_OBJECTS)).longValue());
                purgeAccessionRegisterModel.setTotalSize(((Number) objectGroup.get(TOTAL_SIZE)).longValue());
            }

            return purgeAccessionRegisterModel;
        };
    }

    private Comparator<Document> compareDocument() {
        return (unit, objectGroup) -> {
            String opiUnit = unit == null ? null : unit.getString(OPI);
            String opiObjectGroup = objectGroup == null ? null : objectGroup.getString(OPI);
            return StringUtils.compare(opiUnit, opiObjectGroup);
        };
    }

    private void createFileFromMongoCursorWithString(File tempFile, MongoCursor<String> iterator) throws IOException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
            while (iterator.hasNext()) {
                String objectGroupId = iterator.next();
                JsonLineModel jsonLineModel = getJsonLineModelWithString(objectGroupId);
                jsonLineWriter.addEntry(jsonLineModel);
            }
        }
    }

    private JsonLineModel getJsonLineModelWithString(String next) {
        return new JsonLineModel(next, null, null);
    }

    private void storeFileToWorkspace(String processId, String name, File file)
        throws IOException, ContentAddressableStorageServerException {
        try (
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
            InputStream inputStream = new FileInputStream(file)
        ) {
            workspaceClient.putAtomicObject(processId, name, inputStream, file.length());
        }
    }

    public void exportPurgeDistinctObjectGroupOfDeletedUnits(String processId, String filename, int tenantId)
        throws IOException, ContentAddressableStorageServerException, IllegalPathException {
        File tempFile = createTemporaryFile(processId, filename);

        try (
            MongoCursor<String> iterator = purgeUnitRepository.distinctObjectGroupOfDeletedUnits(processId, tenantId)
        ) {
            createFileFromMongoCursorWithString(tempFile, iterator);
            storeFileToWorkspace(processId, filename, tempFile);
        } finally {
            deleteQuietly(tempFile.getParentFile());
        }
    }

    public void exportPurgeAccessionRegister(String processId, String filename, int tenantId)
        throws IOException, ContentAddressableStorageServerException, InvalidParseOperationException, IllegalPathException {
        File tempFile = createTemporaryFile(processId, filename);

        try (
            MongoCursor<Document> unitCursor = purgeUnitRepository.computeOwnAccessionRegisterDetails(
                processId,
                tenantId
            );
            MongoCursor<Document> gotCursor = purgeObjectGroupRepository.computeOwnAccessionRegisterDetails(
                processId,
                tenantId
            )
        ) {
            createFileFromTwoMongoCursorWithDocument(tempFile, unitCursor, gotCursor);
            storeFileToWorkspace(processId, filename, tempFile);
        } finally {
            deleteQuietly(tempFile.getParentFile());
        }
    }

    public void deleteEliminationUnitByProcessId(String processId, int tenantId) {
        eliminationActionUnitRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deletePurgeUnitByProcessId(String processId, int tenantId) {
        purgeUnitRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deletePurgeObjectGroupByIdAndTenant(String processId, int tenantId) {
        purgeObjectGroupRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteTransferReplyUnitByProcessId(String processId, int tenantId) {
        transferReplyUnitRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deletePreservationByIdAndTenant(String processId, int tenantId) {
        preservationReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteAuditByIdAndTenant(String processId, int tenantId) {
        auditReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteUpdateUnitByIdAndTenant(String processId, int tenantId) {
        updateUnitReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteBulkUpdateUnitMetadataByIdAndTenant(String processId, int tenantId) {
        bulkUpdateUnitMetadataReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteEvidenceAuditByIdAndTenant(String processId, int tenantId) {
        evidenceAuditReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteGotVersionsByIdAndTenant(String processId, int tenantId) {
        deleteGotVersionsReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    private void deleteQuietly(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.warn("Could not delete directory " + directory.getAbsolutePath());
            throw new VitamRuntimeException(e);
        }
    }

    public void storeExtractedMetadataForAu(List<ExtractedMetadata> extractedMetadatas) {
        extractedMetadataRepository.addExtractedMetadataForAu(extractedMetadatas);
    }

    public void createExtractedMetadataDistributionFileForAu(String processId, int tenant)
        throws IOException, ContentAddressableStorageServerException, IllegalPathException {
        File tempFile = createTemporaryFile(processId, processId);
        try (
            MongoCursor<ExtractedMetadata> extractedMetadatas =
                extractedMetadataRepository.getExtractedMetadataByProcessId(processId, tenant)
        ) {
            try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
                while (extractedMetadatas.hasNext()) {
                    ExtractedMetadata metadata = extractedMetadatas.next();
                    for (String unitId : metadata.getUnitIds()) {
                        writeToJsonLine(jsonLineWriter, unitId, metadata.getMetadata());
                    }
                }
            }
            storeFileToWorkspace(processId, "distributionFileAU" + JSONL_EXTENSION, tempFile);
            extractedMetadataRepository.deleteExtractedMetadataByProcessId(processId, tenant);
        } finally {
            deleteQuietly(tempFile.getParentFile());
        }
    }

    private void writeToJsonLine(JsonLineWriter jsonLineWriter, String unitId, Map<String, Object> metadata) {
        try {
            JsonLineModel line = new JsonLineModel(unitId, null, JsonHandler.toJsonNode(metadata));
            jsonLineWriter.addEntry(line);
        } catch (IOException | InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public void deleteTraceabilityByIdAndTenant(String processId, int tenantId) {
        traceabilityReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    @NotNull
    private File createTemporaryFile(@NotNull String processId, @NotNull String filename)
        throws IOException, IllegalPathException {
        File file = SafeFileChecker.checkSafeFilePath(VitamConfiguration.getVitamTmpFolder(), processId, filename);
        FileUtils.forceMkdirParent(file);
        return file;
    }
}
