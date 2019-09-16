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
package fr.gouv.vitam.batch.report.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.exception.BatchReportException;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel;
import fr.gouv.vitam.batch.report.model.EliminationActionObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditObjectModel;
import fr.gouv.vitam.batch.report.model.EvidenceStatus;
import fr.gouv.vitam.batch.report.model.MergeSortedIterator;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.UnitComputedInheritedRulesInvalidationModel;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UpdateUnitMetadataReportEntry;
import fr.gouv.vitam.batch.report.rest.repository.AuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.EvidenceAuditReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.PreservationReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.UnitComputedInheritedRulesInvalidationRepository;
import fr.gouv.vitam.batch.report.rest.repository.UpdateUnitReportRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.OPI;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.ORIGINATING_AGENCY;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.TOTAL_OBJECTS;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.TOTAL_OBJECT_GROUPS;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.TOTAL_SIZE;
import static fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel.TOTAL_UNITS;
import static fr.gouv.vitam.batch.report.model.ReportType.AUDIT;
import static fr.gouv.vitam.batch.report.model.ReportType.EVIDENCE_AUDIT;

/**
 * BatchReportService
 */
public class BatchReportServiceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BatchReportServiceImpl.class);
    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String REPORT_JSONL = "report.jsonl";

    private final EliminationActionUnitRepository eliminationActionUnitRepository;
    private final EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository;
    private final PreservationReportRepository preservationReportRepository;
    private final UpdateUnitReportRepository updateUnitReportRepository;
    private final AuditReportRepository auditReportRepository;
    private final UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository;
    private final EvidenceAuditReportRepository evidenceAuditReportRepository;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final BackupService backupService;

    public BatchReportServiceImpl(EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        WorkspaceClientFactory workspaceClientFactory,
        PreservationReportRepository preservationReportRepository,
        AuditReportRepository auditReportRepository,
        UpdateUnitReportRepository updateUnitReportRepository,
        UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository,
        EvidenceAuditReportRepository evidenceAuditReportRepository) {

        this(
            eliminationActionUnitRepository,
            eliminationActionObjectGroupRepository,
            updateUnitReportRepository,
            new BackupService(),
            workspaceClientFactory,
            preservationReportRepository,
            auditReportRepository,
            unitComputedInheritedRulesInvalidationRepository,
            evidenceAuditReportRepository
        );
    }

    @VisibleForTesting
    BatchReportServiceImpl(
        EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        UpdateUnitReportRepository updateUnitReportRepository,
        BackupService backupService,
        WorkspaceClientFactory workspaceClientFactory,
        PreservationReportRepository preservationReportRepository,
        AuditReportRepository auditReportRepository,
        UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository,
        EvidenceAuditReportRepository evidenceAuditReportRepository) {
        this.eliminationActionUnitRepository = eliminationActionUnitRepository;
        this.eliminationActionObjectGroupRepository = eliminationActionObjectGroupRepository;
        this.updateUnitReportRepository = updateUnitReportRepository;
        this.workspaceClientFactory = workspaceClientFactory;
        this.preservationReportRepository = preservationReportRepository;
        this.auditReportRepository = auditReportRepository;
        this.unitComputedInheritedRulesInvalidationRepository = unitComputedInheritedRulesInvalidationRepository;
        this.backupService = backupService;
        this.evidenceAuditReportRepository = evidenceAuditReportRepository;
    }

    public void appendEliminationActionUnitReport(String processId, List<EliminationActionUnitReportEntry> entries,
        int tenantId) {
        List<EliminationActionUnitModel> documents =
            entries.stream()
                .map(unitEntry -> new EliminationActionUnitModel(
                    processId, tenantId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                    unitEntry))
                .collect(Collectors.toList());
        eliminationActionUnitRepository.bulkAppendReport(documents);
    }

    public void appendEliminationActionObjectGroupReport(String processId,
        List<EliminationActionObjectGroupReportEntry> entries, int tenantId) {
        List<EliminationActionObjectGroupModel> documents =
            entries.stream()
                .map(ogEntry -> new EliminationActionObjectGroupModel(
                    processId, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), ogEntry, tenantId))
                .collect(Collectors.toList());
        eliminationActionObjectGroupRepository.bulkAppendReport(documents);
    }

    public void appendPreservationReport(String processId, List<PreservationReportEntry> preservationEntries,
        int tenantId)
        throws BatchReportException {
        List<PreservationReportEntry> documents = preservationEntries.stream()
            .map(preservationEntry -> checkValuesAndGetNewPreservationReportEntry(processId, tenantId,
                preservationEntry))
            .collect(Collectors.toList());
        preservationReportRepository.bulkAppendReport(documents);
    }

    public void appendUnitReport(List<UpdateUnitMetadataReportEntry> unitEntries) {
        updateUnitReportRepository.bulkAppendReport(unitEntries);
    }

    public void appendUnitComputedInheritedRulesInvalidationReport(String processId,
        List<UnitComputedInheritedRulesInvalidationReportEntry> unitEntries, int tenantId)
        throws BatchReportException {
        List<UnitComputedInheritedRulesInvalidationModel> documents = unitEntries.stream()
            .map(entry -> new UnitComputedInheritedRulesInvalidationModel(
                processId, tenantId,
                LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), entry))
            .collect(Collectors.toList());
        unitComputedInheritedRulesInvalidationRepository.bulkAppendReport(documents);
    }

    public void deleteUnitComputedInheritedRulesInvalidationReport(String processId, int tenantId) {
        unitComputedInheritedRulesInvalidationRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void exportUnitsToInvalidate(String processId, int tenantId, ReportExportRequest reportExportRequest)
        throws IOException, ContentAddressableStorageServerException {

        File file =
            Files.createFile(Paths.get(VitamConfiguration.getVitamTmpFolder(), reportExportRequest.getFilename()))
                .toFile();

        try {
            try (
                OutputStream outputStream = new FileOutputStream(file);
                JsonLineWriter jsonLineWriter = new JsonLineWriter(outputStream);
                CloseableIterator<Document> units = unitComputedInheritedRulesInvalidationRepository
                    .findCollectionByProcessIdTenant(processId, tenantId)) {

                while (units.hasNext()) {
                    Document unit = units.next();
                    jsonLineWriter.addEntry(new JsonLineModel((String) unit.get("id")));
                }
            }

            transferDocumentToWorkspace(processId, reportExportRequest.getFilename(), file);

        } finally {
            deleteQuietly(file);
        }
    }

    private PreservationReportEntry checkValuesAndGetNewPreservationReportEntry(String processId, int tenantId,
        PreservationReportEntry entry) {
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
            entry.getPreservationScenarioId());
    }

    public void appendAuditReport(String processId, List<AuditObjectGroupReportEntry> auditEntries, int tenantId) {
        List<AuditObjectGroupModel> documents = auditEntries.stream()
            .map(auditEntry -> checkValuesAndGetAuditObjectGroupModel(processId, tenantId, auditEntry))
            .collect(Collectors.toList());
        auditReportRepository.bulkAppendReport(documents);
    }

    public void appendEvidenceAuditReport(String processId, List<EvidenceAuditReportEntry> auditEntries, int tenantId)
        throws BatchReportException {
        List<EvidenceAuditObjectModel> documents = auditEntries.stream()
            .map(auditEntry -> checkValuesAndGetEvidenceAuditObjectGroupModel(processId, tenantId, auditEntry))
            .collect(Collectors.toList());
        evidenceAuditReportRepository.bulkAppendReport(documents);
    }

    private AuditObjectGroupModel checkValuesAndGetAuditObjectGroupModel(String processId, int tenantId,
        AuditObjectGroupReportEntry auditEntry) {
        checkIfPresent("DetailId", auditEntry.getDetailId());
        checkIfPresent("Outcome", auditEntry.getOutcome());
        checkIfPresent("DetailType", auditEntry.getDetailType());
        checkIfPresent("Opi", auditEntry.getOpi());
        checkIfPresent("OriginatingAgency", auditEntry.getOriginatingAgency());
        checkIfPresent("ParentUnitIds", auditEntry.getParentUnitIds());
        checkIfPresent("Status", auditEntry.getStatus());

        return new AuditObjectGroupModel(processId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), auditEntry, tenantId);
    }

    private EvidenceAuditObjectModel checkValuesAndGetEvidenceAuditObjectGroupModel(String processId, int tenantId,
        EvidenceAuditReportEntry evidenceAuditEntry) {
        checkIfPresent("identifier", evidenceAuditEntry.getIdentifier());
        checkIfPresent("status", evidenceAuditEntry.getEvidenceStatus());
        checkIfPresent("message", evidenceAuditEntry.getMessage());
        checkIfPresent("strategyId", evidenceAuditEntry.getStrategyId());
        checkIfPresent("objectType", evidenceAuditEntry.getObjectType());

        return new EvidenceAuditObjectModel(processId, tenantId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), evidenceAuditEntry);
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

    private void storeReport(String processId, File report) throws IOException, BackupServiceException {
        backupService.backup(new FileInputStream(report), DataCategory.REPORT, processId + JSONL_EXTENSION);
    }

    private JsonNode getExtendedInfo(Report reportInfo) throws InvalidParseOperationException {
        switch (reportInfo.getReportSummary().getReportType()) {
            case PRESERVATION:
                return JsonHandler.toJsonNode(
                    preservationReportRepository.stats(reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()));
            case AUDIT:
                return JsonHandler.toJsonNode(
                    auditReportRepository.stats(reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()));
            case EVIDENCE_AUDIT:
                return JsonHandler.toJsonNode(
                    evidenceAuditReportRepository.stats(reportInfo.getOperationSummary().getEvId(),
                        reportInfo.getOperationSummary().getTenant()));
            default:
                return reportInfo.getReportSummary().getExtendedInfo();
        }
    }

    private ReportResults getReportResults(Report reportInfo) {
        if (reportInfo.getReportSummary().getReportType() == AUDIT) {
            return auditReportRepository.computeVitamResults(reportInfo.getOperationSummary().getEvId(),
                reportInfo.getOperationSummary().getTenant());
        }
        if (reportInfo.getReportSummary().getReportType() == EVIDENCE_AUDIT) {
            return evidenceAuditReportRepository.computeVitamResults(reportInfo);
        }
        return reportInfo.getReportSummary().getVitamResults();
    }

    public void storeReport(Report reportInfo)
        throws IOException, BackupServiceException, InvalidParseOperationException {

        OperationSummary operationSummary = reportInfo.getOperationSummary();
        String processId = operationSummary.getEvId();
        int tenantId = operationSummary.getTenant();

        ReportSummary reportSummary = reportInfo.getReportSummary();
        reportSummary.setExtendedInfo(getExtendedInfo(reportInfo));
        reportSummary.setVitamResults(getReportResults(reportInfo));

        File tempReport =
            File.createTempFile(REPORT_JSONL, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (JsonLineWriter reportWriter = new JsonLineWriter(new FileOutputStream(tempReport))) {
            reportWriter.addEntry(operationSummary);
            reportWriter.addEntry(reportSummary);
            reportWriter.addEntry(reportInfo.getContext());

            switch (reportSummary.getReportType()) {
                case ELIMINATION_ACTION:
                    MongoCursor<Document> archiveUnitIterator =
                        eliminationActionUnitRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, archiveUnitIterator);

                    MongoCursor<Document> objectGroupIterator =
                        eliminationActionObjectGroupRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, objectGroupIterator);
                    break;
                case PRESERVATION:
                    MongoCursor<Document> preservationIterator =
                        preservationReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, preservationIterator);
                    break;
                case AUDIT:
                    MongoCursor<Document> auditIterator =
                        auditReportRepository
                            .findCollectionByProcessIdTenantAndStatus(processId, tenantId, "WARNING", "KO");
                    writeDocumentsInFile(reportWriter, auditIterator);
                    break;
                case EVIDENCE_AUDIT:
                    MongoCursor<Document> evidenceAuditIterator =
                        evidenceAuditReportRepository
                            .findCollectionByProcessIdTenantAndStatus(processId, tenantId, EvidenceStatus.WARN.name(),
                                EvidenceStatus.KO.name());
                    writeDocumentsInFile(reportWriter, evidenceAuditIterator);
                    break;
                case UPDATE_UNIT:
                    MongoCursor<Document> updates =
                        updateUnitReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, updates);
                    break;
                default:
                    throw new UnsupportedOperationException(
                        String.format("Unsupported report type : '%s'.", reportSummary.getReportType()));
            }
        }

        storeReport(processId, tempReport);
    }

    void exportEliminationActionObjectGroupReport(String processId, String fileName, int tenantId)
        throws InvalidParseOperationException, ContentAddressableStorageServerException, IOException {

        File tempFile =
            File.createTempFile(fileName, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (MongoCursor<Document> iterator = eliminationActionObjectGroupRepository
            .findCollectionByProcessIdTenant(processId, tenantId)) {

            createFileFromMongoCursorWithDocument(tempFile, iterator);

            transferDocumentToWorkspace(processId, fileName, tempFile);

        } finally {
            deleteQuietly(tempFile);
        }
    }

    public void exportPreservationReport(String processId, String fileName, int tenantId)
        throws IOException, ContentAddressableStorageServerException {
        File file = Files.createFile(Paths.get(VitamConfiguration.getVitamTmpFolder(), fileName)).toFile();

        try {
            createDocument(processId, tenantId, file);
            transferDocumentToWorkspace(processId, fileName, file);
        } finally {
            deleteQuietly(file);
        }
    }

    private PreservationReportEntry mapToModel(Document document) {
        try {
            return JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(document), PreservationReportEntry.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void createDocument(String processId, int tenantId, File file) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(file);
            OutputStreamWriter streamOut = new OutputStreamWriter(fileOut);
            BufferedWriter writer = new BufferedWriter(streamOut)) {

            PreservationStatsModel stats = preservationReportRepository.stats(processId, tenantId);
            addDocumentToFile(stats, writer);

            try (MongoCursor<Document> reports = preservationReportRepository
                .findCollectionByProcessIdTenant(processId, tenantId)) {
                reports.forEachRemaining(d -> addDocumentToFile(mapToModel(d), writer));
            }
        }
    }

    private <T> void addDocumentToFile(T d, BufferedWriter writer) {
        try {
            writer.append(JsonHandler.unprettyPrint(d));
            writer.append("\n");
        } catch (IOException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void createFileFromMongoCursorWithDocument(File tempFile, MongoCursor<Document> iterator)
        throws IOException, InvalidParseOperationException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {
            while (iterator.hasNext()) {
                Document document = iterator.next();
                JsonLineModel jsonLineModel =
                    JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(document), JsonLineModel.class);
                jsonLineWriter.addEntry(jsonLineModel);
            }
        }
    }

    /**
     * Merge two sorted iterators and write result to json file
     *
     * @param tempFile
     * @param unitCursor
     * @param objectGroupCursor
     * @throws IOException
     * @throws InvalidParseOperationException
     */
    private void createFileFromTwoMongoCursorWithDocument(File tempFile, MongoCursor<Document> unitCursor,
        MongoCursor<Document> objectGroupCursor)
        throws IOException, InvalidParseOperationException {

        Comparator<Document> comparator = compareDocument();

        BiFunction<Document, Document, EliminationActionAccessionRegisterModel> mergeFunction = mergeDocuments();

        MergeSortedIterator<Document, EliminationActionAccessionRegisterModel> mergeSortedIterator =
            new MergeSortedIterator<>(unitCursor, objectGroupCursor, comparator, mergeFunction);

        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(tempFile))) {

            while (mergeSortedIterator.hasNext()) {
                EliminationActionAccessionRegisterModel eliminationActionAccessionRegisterModel =
                    mergeSortedIterator.next();
                JsonLineModel jsonLineModel = new JsonLineModel();
                jsonLineModel.setId(eliminationActionAccessionRegisterModel.getOpi());

                jsonLineModel.setParams(JsonHandler.toJsonNode(eliminationActionAccessionRegisterModel));
                jsonLineWriter.addEntry(jsonLineModel);

            }
        }
    }

    private BiFunction<Document, Document, EliminationActionAccessionRegisterModel> mergeDocuments() {
        return (unit, objectGroup) -> {
            EliminationActionAccessionRegisterModel eliminationAccessionRegisterModel =
                new EliminationActionAccessionRegisterModel();

            if (unit != null) {
                eliminationAccessionRegisterModel.setOpi(unit.getString(OPI));
                eliminationAccessionRegisterModel.setOriginatingAgency(unit.getString(ORIGINATING_AGENCY));
                eliminationAccessionRegisterModel.setTotalUnits(((Number) unit.get(TOTAL_UNITS)).longValue());
            }

            if (objectGroup != null) {
                eliminationAccessionRegisterModel.setOpi(objectGroup.getString(OPI));
                eliminationAccessionRegisterModel.setOriginatingAgency(objectGroup.getString(ORIGINATING_AGENCY));
                eliminationAccessionRegisterModel
                    .setTotalObjectGroups(((Number) objectGroup.get(TOTAL_OBJECT_GROUPS)).longValue());
                eliminationAccessionRegisterModel
                    .setTotalObjects(((Number) objectGroup.get(TOTAL_OBJECTS)).longValue());
                eliminationAccessionRegisterModel.setTotalSize(((Number) objectGroup.get(TOTAL_SIZE)).longValue());

            }

            return eliminationAccessionRegisterModel;
        };
    }

    private Comparator<Document> compareDocument() {
        return (unit, objectGroup) -> {
            String opiUnit = unit == null ? null : unit.getString(OPI);
            String opiObjectGroup = objectGroup == null ? null : objectGroup.getString(OPI);
            return StringUtils.compare(opiUnit, opiObjectGroup);
        };
    }


    private void createFileFromMongoCursorWithString(File tempFile, MongoCursor<String> iterator)
        throws IOException {
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

    private void transferDocumentToWorkspace(String processId, String fileName, File tempFile)
        throws IOException, ContentAddressableStorageServerException {
        try (WorkspaceClient client = workspaceClientFactory.getClient();
            FileInputStream fileInputStream = new FileInputStream(tempFile)) {
            client.putObject(processId, fileName, fileInputStream);
        }
    }

    public void exportEliminationActionDistinctObjectGroupOfDeletedUnits(String processId, String filename,
        int tenantId)
        throws IOException, ContentAddressableStorageServerException {

        File tempFile =
            File.createTempFile(filename, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (MongoCursor<String> iterator = eliminationActionUnitRepository
            .distinctObjectGroupOfDeletedUnits(processId, tenantId)) {
            createFileFromMongoCursorWithString(tempFile, iterator);
            transferDocumentToWorkspace(processId, filename, tempFile);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public void exportEliminationActionAccessionRegister(String processId, String filename, int tenantId)
        throws IOException, ContentAddressableStorageServerException, InvalidParseOperationException {

        File tempFile =
            File.createTempFile(filename, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (MongoCursor<Document> unitCursor = eliminationActionUnitRepository
            .computeOwnAccessionRegisterDetails(processId, tenantId);
            MongoCursor<Document> gotCursor = eliminationActionObjectGroupRepository
                .computeOwnAccessionRegisterDetails(processId, tenantId)) {

            createFileFromTwoMongoCursorWithDocument(tempFile, unitCursor, gotCursor);
            transferDocumentToWorkspace(processId, filename, tempFile);
        } finally {
            deleteQuietly(tempFile);
        }
    }


    public void deleteEliminationUnitByProcessId(String processId, int tenantId) {
        eliminationActionUnitRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    public void deleteEliminationObjectGroupByIdAndTenant(String processId, int tenantId) {
        eliminationActionObjectGroupRepository.deleteReportByIdAndTenant(processId, tenantId);
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

    public void deleteEvidenceAuditByIdAndTenant(String processId, int tenantId) {
        evidenceAuditReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    private void deleteQuietly(File tempFile) {
        try {
            Files.delete(tempFile.toPath());
        } catch (IOException e) {
            LOGGER.warn("Could not delete file " + tempFile.getAbsolutePath());
            throw new VitamRuntimeException(e);
        }
    }
}
