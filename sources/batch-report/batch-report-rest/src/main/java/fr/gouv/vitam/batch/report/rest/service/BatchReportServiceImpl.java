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
import fr.gouv.vitam.batch.report.model.EliminationActionAccessionRegisterModel;
import fr.gouv.vitam.batch.report.model.EliminationActionObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.batch.report.model.MergeSortedIterator;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.PreservationReportRepository;
import fr.gouv.vitam.batch.report.rest.repository.UpdateUnitReportRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
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

/**
 * BatchReportService
 */
public class BatchReportServiceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BatchReportServiceImpl.class);
    private static final String JSONL_EXTENSION = ".jsonl";
    private static final String OPI_GOT = "opi_got";
    private static final String STATUS = "status";
    private static final String REPORT_JSONL = "report.jsonl";

    private EliminationActionUnitRepository eliminationActionUnitRepository;
    private EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository;
    private PreservationReportRepository preservationReportRepository;
    private final UpdateUnitReportRepository updateUnitReportRepository;
    private WorkspaceClientFactory workspaceClientFactory;
    private final BackupService backupService;

    public BatchReportServiceImpl(EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        WorkspaceClientFactory workspaceClientFactory,
        PreservationReportRepository preservationReportRepository,
        UpdateUnitReportRepository updateUnitReportRepository) {

        this(
            eliminationActionUnitRepository,
            eliminationActionObjectGroupRepository,
            updateUnitReportRepository,
            new BackupService(),
            workspaceClientFactory,
            preservationReportRepository
        );
    }

    @VisibleForTesting
    BatchReportServiceImpl(
        EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        UpdateUnitReportRepository updateUnitReportRepository,
        BackupService backupService,
        WorkspaceClientFactory workspaceClientFactory,
        PreservationReportRepository preservationReportRepository) {
        this.eliminationActionUnitRepository = eliminationActionUnitRepository;
        this.eliminationActionObjectGroupRepository = eliminationActionObjectGroupRepository;
        this.updateUnitReportRepository = updateUnitReportRepository;
        this.workspaceClientFactory = workspaceClientFactory;
        this.preservationReportRepository = preservationReportRepository;
        this.backupService = backupService;
    }

    public void appendEliminationActionUnitReport(String processId, List<JsonNode> entries, int tenantId) {
        List<EliminationActionUnitModel> documents =
            entries.stream()
                .map(entry -> new EliminationActionUnitModel(
                    GUIDFactory.newGUID().toString(), processId, tenantId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                    entry))
                .collect(Collectors.toList());
        eliminationActionUnitRepository.bulkAppendReport(documents);
    }

    public void appendEliminationActionObjectGroupReport(String processId, List<JsonNode> entries, int tenantId) {

        List<EliminationActionObjectGroupModel> documents =
            entries.stream()
                .map(entry -> new EliminationActionObjectGroupModel(
                    GUIDFactory.newGUID().toString(), processId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), entry, tenantId))
                .collect(Collectors.toList());
        eliminationActionObjectGroupRepository.bulkAppendReport(documents);
    }

    public void appendPreservationReport(String processId, List<JsonNode> entries, int tenantId)
        throws BatchReportException {
        List<PreservationReportModel> documents = entries.stream()
            .map(entry -> createPreservationReportModel(processId, tenantId, entry))
            .collect(Collectors.toList());
        preservationReportRepository.bulkAppendReport(documents);
    }

    private PreservationReportModel createPreservationReportModel(String processId, int tenantId, JsonNode entry)
        throws BatchReportException {
        JsonNode outputName = entry.get("outputName");
        String output = "";
        if (outputName != null && !outputName.isNull()) {
            output = outputName.asText();
        }
        return new PreservationReportModel(
            GUIDFactory.newGUID().toString(),
            processId,
            tenantId,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            PreservationStatus.valueOf(entry.get(STATUS).asText()),
            checkValuePresent("unitId", entry).asText(),
            checkValuePresent("objectGroupId", entry).asText(),
            ActionTypePreservation.valueOf(checkValuePresent("action", entry).asText()),
            entry.get("analyseResult") != null && entry.get("analyseResult").isTextual() ? entry.get("analyseResult").asText() : null,
            checkValuePresent("inputName", entry).asText(), output);
    }

    private JsonNode checkValuePresent(String fieldName, JsonNode entry) throws BatchReportException {
        if (entry.get(fieldName) == null) {
            throw new BatchReportException(String.format("field Name mandatory %s", fieldName));
        }
        return entry.get(fieldName);
    }


    public void exportEliminationActionUnitReport(String processId, String fileName, int tenantId)
        throws InvalidParseOperationException, IOException, ContentAddressableStorageServerException {
        File tempFile = File.createTempFile(fileName, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));
        try (MongoCursor<Document> iterator =
            eliminationActionUnitRepository.findCollectionByProcessIdTenant(processId, tenantId)) {

            createFileFromMongoCursorWithDocument(tempFile, iterator);
            transferDocumentToWorkspace(processId, fileName, tempFile);

        } finally {
            deleteQuietly(tempFile);
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
        backupService.backup(new FileInputStream(report), DataCategory.REPORT, processId + ".jsonl");
    }

    private JsonNode getExtendedInfo(Report reportInfo) throws InvalidParseOperationException {
        switch (reportInfo.getReportSummary().getReportType()) {
            default:
                return reportInfo.getReportSummary().getExtendedInfo();
        }
    }

    private ReportResults getReportResults(Report reportInfo) {
        return reportInfo.getReportSummary().getVitamResults();
    }

    public void storeReport(Report reportInfo)
        throws IllegalArgumentException, IOException, BackupServiceException, InvalidParseOperationException {

        OperationSummary operationSummary = reportInfo.getOperationSummary();
        String processId = operationSummary.getEvId();
        int tenantId = operationSummary.getTenant();

        ReportSummary reportSummary = reportInfo.getReportSummary();
        reportSummary.setExtendedInfo(getExtendedInfo(reportInfo));
        reportSummary.setVitamResults(getReportResults(reportInfo));


        File tempReport = File.createTempFile(REPORT_JSONL, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (JsonLineWriter reportWriter = new JsonLineWriter(new FileOutputStream(tempReport))) {
            reportWriter.addEntry(operationSummary);
            reportWriter.addEntry(reportSummary);
            reportWriter.addEntry(reportInfo.getContext());

            switch (reportSummary.getReportType()) {
                case UPDATE_UNIT:
                    MongoCursor<Document> updates = updateUnitReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, updates);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Unsupported report type : '%s'.", reportSummary.getReportType()));
            }
        }

        storeReport(processId, tempReport);
    }

    public void exportEliminationActionObjectGroupReport(String processId, String fileName, int tenantId)
        throws InvalidParseOperationException, ContentAddressableStorageServerException, IOException {

        File tempFile = File.createTempFile(fileName, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

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

    private void createDocument(String processId, int tenantId, File file) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(file);
            OutputStreamWriter streamOut = new OutputStreamWriter(fileOut);
            BufferedWriter writer = new BufferedWriter(streamOut)) {

            PreservationStatsModel stats = preservationReportRepository.stats(processId, tenantId);
            addDocumentToFile(stats, writer);

            try (MongoCursor<PreservationReportModel> reports = preservationReportRepository
                .findCollectionByProcessIdTenant(processId, tenantId)) {
                reports.forEachRemaining(d -> addDocumentToFile(d, writer));
            }
        }
    }

    private <T> void addDocumentToFile(T d, BufferedWriter writer) {
        try {
            writer.append(JsonHandler.unprettyPrint(d));
            writer.append("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            new MergeSortedIterator(unitCursor, objectGroupCursor, comparator, mergeFunction);

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

        File tempFile = File.createTempFile(filename, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

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

        File tempFile = File.createTempFile(filename, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

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

    public void deleteUpdateUnitByIdAndTenant(String processId, int tenantId) {
        updateUnitReportRepository.deleteReportByIdAndTenant(processId, tenantId);
    }

    private void deleteQuietly(File tempFile) {
        if (!tempFile.delete()) {
            LOGGER.warn("Could not delete file " + tempFile.getAbsolutePath());
        }
    }
}
