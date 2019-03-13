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
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.PreservationReportRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
    private WorkspaceClientFactory workspaceClientFactory;
    private BackupService backupService;

    public BatchReportServiceImpl(EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository,
        WorkspaceClientFactory workspaceClientFactory, PreservationReportRepository preservationReportRepository) {
        this(eliminationActionUnitRepository, eliminationActionObjectGroupRepository, new BackupService(), workspaceClientFactory, preservationReportRepository);
    }

    @VisibleForTesting
    BatchReportServiceImpl(EliminationActionUnitRepository eliminationActionUnitRepository,
        EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository, BackupService backupService,
        WorkspaceClientFactory workspaceClientFactory, PreservationReportRepository preservationReportRepository) {
        this.eliminationActionUnitRepository = eliminationActionUnitRepository;
        this.eliminationActionObjectGroupRepository = eliminationActionObjectGroupRepository;
        this.workspaceClientFactory = workspaceClientFactory;
        this.preservationReportRepository = preservationReportRepository;
        this.backupService = backupService;
    }

    public void appendEliminationActionUnitReport(String processId, List<EliminationActionUnitReportEntry> entries, int tenantId) {
        List<EliminationActionUnitModel> documents =
            entries.stream()
                .map(unitEntry -> new EliminationActionUnitModel(
                    GUIDFactory.newGUID().toString(), processId, tenantId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
                    unitEntry))
                .collect(Collectors.toList());
        eliminationActionUnitRepository.bulkAppendReport(documents);
    }

    public void appendEliminationActionObjectGroupReport(String processId, List<EliminationActionObjectGroupReportEntry> entries, int tenantId) {
        List<EliminationActionObjectGroupModel> documents =
            entries.stream()
                .map(ogEntry -> new EliminationActionObjectGroupModel(
                    GUIDFactory.newGUID().toString(), processId,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()), ogEntry, tenantId))
                .collect(Collectors.toList());
        eliminationActionObjectGroupRepository.bulkAppendReport(documents);
    }

    public void appendPreservationReport(String processId, List<PreservationReportEntry> preservationEntries, int tenantId)
        throws BatchReportException {
        List<PreservationReportEntry> documents = preservationEntries.stream()
            .map(preservationEntry -> checkValuesAndGetNewPreservationReportEntry(processId, tenantId, preservationEntry))
            .collect(Collectors.toList());
        preservationReportRepository.bulkAppendReport(documents);
    }

    private PreservationReportEntry checkValuesAndGetNewPreservationReportEntry(String processId, int tenantId, PreservationReportEntry entry)
        throws BatchReportException {

        checkIfPresent("UnitId", entry.getUnitId());
        checkIfPresent("ObjectGroupId", entry.getObjectGroupId());
        checkIfPresent("Action", entry.getAction());
        checkIfPresent("InputObjectId", entry.getInputObjectId());

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
            entry.getOutcome()
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

    private void storeReport(String operationId, File report) throws IOException, BackupServiceException {
        backupService.backup(new FileInputStream(report), DataCategory.REPORT, operationId + ".jsonl");
    }

    private JsonNode getExtendedInfo(Report reportInfo) throws InvalidParseOperationException {
        JsonNode extendedInfo = JsonHandler.createObjectNode();

        switch(reportInfo.getReportSummary().getReportType()) {
            case PRESERVATION:
                extendedInfo = JsonHandler.toJsonNode(
                    preservationReportRepository.stats(reportInfo.getOperationSummary().getEvId(), reportInfo.getOperationSummary().getTenant()));
                break;
            default:
                // Nothing to do, keep empty extended info
        }
        return extendedInfo;
    }

    public void storeReport(Report reportInfo)
        throws IllegalArgumentException, IOException, BackupServiceException, InvalidParseOperationException {

        String processId = reportInfo.getOperationSummary().getEvId();
        Integer tenantId = reportInfo.getOperationSummary().getTenant();

        JsonNode extendedInfo = getExtendedInfo(reportInfo);
        reportInfo.getReportSummary().setExtendedInfo(extendedInfo);

        File tempReport = File.createTempFile(REPORT_JSONL, JSONL_EXTENSION, new File(VitamConfiguration.getVitamTmpFolder()));

        try (JsonLineWriter reportWriter = new JsonLineWriter(new FileOutputStream(tempReport))) {
            reportWriter.addEntry(reportInfo.getOperationSummary());
            reportWriter.addEntry(reportInfo.getReportSummary());
            reportWriter.addEntry(reportInfo.getContext());

            switch(reportInfo.getReportSummary().getReportType()) {
                case ELIMINATION_ACTION:
                    MongoCursor<Document> archiveUnitIterator = eliminationActionUnitRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, archiveUnitIterator);

                    MongoCursor<Document> objectGroupIterator = eliminationActionObjectGroupRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, objectGroupIterator);
                    break;
                case PRESERVATION:
                    MongoCursor<Document> preservationIterator = preservationReportRepository.findCollectionByProcessIdTenant(processId, tenantId);
                    writeDocumentsInFile(reportWriter, preservationIterator);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported report type yo store: " + reportInfo.getReportSummary().getReportType());
            }
        }

        storeReport(reportInfo.getOperationSummary().getEvId(), tempReport);
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

    private PreservationReportEntry mapToModel(Document document) {
        try {
            return JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(document), PreservationReportEntry.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDocument(String processId, int tenantId, File file) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(file);
            OutputStreamWriter streamOut = new OutputStreamWriter(fileOut);
            BufferedWriter writer = new BufferedWriter(streamOut)) {

            PreservationStatsModel stats = preservationReportRepository.stats(processId, tenantId);
            addDocumentToFile(stats, writer);

            try (MongoCursor<Document> reports = preservationReportRepository.findCollectionByProcessIdTenant(processId, tenantId)) {
                reports.forEachRemaining(d -> addDocumentToFile(mapToModel(d), writer));
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

    private void deleteQuietly(File tempFile) {
        if (!tempFile.delete()) {
            LOGGER.warn("Could not delete file " + tempFile.getAbsolutePath());
        }
    }
}
