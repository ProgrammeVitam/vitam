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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.AuditFullStatusCount;
import fr.gouv.vitam.batch.report.model.AuditStatsModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditFullStatusCount;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatsModel;
import fr.gouv.vitam.batch.report.model.EvidenceStatus;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.UnitComputedInheritedRulesInvalidationModel;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.TransferReplyUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.mongo.FakeMongoCursor;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.IteratorUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.util.Streams;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.ACTION;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.ANALYSE_RESULT;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.CREATION_DATE_TIME;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.DETAIL_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.DETAIL_TYPE;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.GRIFFIN_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.INPUT_OBJECT_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OBJECT_GROUP_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OUTCOME;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OUTPUT_OBJECT_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.SCENARIO_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.STATUS;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.TENANT;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.UNIT_ID;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchReportServiceImplTest {

    public static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<>() {};

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    public UpdateUnitReportRepository updateUnitReportRepository;

    @Mock
    public BulkUpdateUnitMetadataReportRepository bulkUpdateUnitMetadataReportRepository;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private EliminationActionUnitRepository eliminationActionUnitRepository;

    @Mock
    private PurgeUnitRepository purgeUnitRepository;

    @Mock
    private PurgeObjectGroupRepository purgeObjectGroupRepository;

    @Mock
    private TransferReplyUnitRepository transferReplyUnitRepository;

    @Mock
    private PreservationReportRepository preservationReportRepository;

    @Mock
    private AuditReportRepository auditReportRepository;

    @Mock
    private EvidenceAuditReportRepository evidenceAuditReportRepository;

    @Mock
    private TraceabilityReportRepository traceabilityReportRepository;

    @Mock
    private UnitComputedInheritedRulesInvalidationRepository unitComputedInheritedRulesInvalidationRepository;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private ExtractedMetadataRepository extractedMetadataRepository;

    @Mock
    private DeleteGotVersionsReportRepository deleteGotVersionsReportRepository;

    private BatchReportServiceImpl batchReportServiceImpl;

    private final String PROCESS_ID = "123456789";
    private final int TENANT_ID = 0;

    private static final TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        batchReportServiceImpl = new BatchReportServiceImpl(
            workspaceClientFactory,
            eliminationActionUnitRepository,
            purgeUnitRepository,
            purgeObjectGroupRepository,
            transferReplyUnitRepository,
            updateUnitReportRepository,
            bulkUpdateUnitMetadataReportRepository,
            preservationReportRepository,
            auditReportRepository,
            unitComputedInheritedRulesInvalidationRepository,
            evidenceAuditReportRepository,
            traceabilityReportRepository,
            extractedMetadataRepository,
            deleteGotVersionsReportRepository
        );
    }

    @Test
    public void should_append_purge_object_group_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/purgeObjectGroupModel.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PurgeObjectGroupReportEntry.class
        );
        // When
        // Then
        assertThatCode(
            () -> batchReportServiceImpl.appendPurgeObjectGroupReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_append_purge_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/purgeUnitModel.json");
        ReportBody<PurgeUnitReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PurgeUnitReportEntry.class
        );
        // When
        // Then
        assertThatCode(
            () -> batchReportServiceImpl.appendPurgeUnitReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_append_elimination_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/eliminationUnitModel.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            EliminationActionUnitReportEntry.class
        );
        // When
        // Then
        assertThatCode(
            () ->
                batchReportServiceImpl.appendEliminationActionUnitReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_append_transfer_reply_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/transferReplyUnitModel.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            TransferReplyUnitReportEntry.class
        );
        // When
        // Then
        assertThatCode(
            () -> batchReportServiceImpl.appendTransferReplyUnitReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_append_preservation_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/preservationReport.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(stream, ReportBody.class, PreservationReportEntry.class);

        // When
        ThrowingCallable append = () ->
            batchReportServiceImpl.appendPreservationReport(
                reportBody.getProcessId(),
                reportBody.getEntries(),
                TENANT_ID
            );

        // Then
        assertThatCode(append).doesNotThrowAnyException();
    }

    @Test
    public void should_append_audit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/auditObjectGroupReport.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            AuditObjectGroupReportEntry.class
        );

        // When
        ThrowingCallable append = () ->
            batchReportServiceImpl.appendAuditReport(reportBody.getProcessId(), reportBody.getEntries(), TENANT_ID);

        // Then
        assertThatCode(append).doesNotThrowAnyException();
    }

    @Test
    public void should_append_evidence_audit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/evidenceAuditObjectReport.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            EvidenceAuditReportEntry.class
        );

        // When
        ThrowingCallable append = () ->
            batchReportServiceImpl.appendEvidenceAuditReport(
                reportBody.getProcessId(),
                reportBody.getEntries(),
                TENANT_ID
            );

        // Then
        assertThatCode(append).doesNotThrowAnyException();
    }

    @Test
    public void should_store_preservation_report() throws Exception {
        // Given
        String processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(processId)).thenReturn(true);
        String filename = String.format("report.jsonl", processId);
        Path report = initialisePathWithFileName(filename);

        PreservationStatsModel preservationStatus = new PreservationStatsModel(0, 1, 0, 1, 0, 0, 0, new HashMap<>(), 0);
        Document preservationData = getPreservationDocument(processId);
        FakeMongoCursor<Document> fakeMongoCursor = new FakeMongoCursor<>(Collections.singletonList(preservationData));

        initialiseMockWhenPutAtomicObjectInWorkspace(report);
        when(preservationReportRepository.findCollectionByProcessIdTenant(processId, TENANT_ID)).thenReturn(
            fakeMongoCursor
        );
        when(preservationReportRepository.stats(processId, TENANT_ID)).thenReturn(preservationStatus);

        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            processId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportResults reportResults = new ReportResults(1, 0, 0);
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.PRESERVATION,
            reportResults,
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeFileToWorkspace(reportInfo);

        // Then
        reportSummary.setExtendedInfo(JsonHandler.toJsonNode(preservationStatus));
        String accumulatorExpected =
            JsonHandler.unprettyPrint(operationSummary) +
            "\n" +
            JsonHandler.unprettyPrint(reportSummary) +
            "\n" +
            JsonHandler.unprettyPrint(context) +
            "\n" +
            BsonHelper.stringify(preservationData);

        assertThat(new String(Files.readAllBytes(report))).isEqualTo(accumulatorExpected);
    }

    private Path initialisePathWithFileName(String filename) throws IOException {
        File folder = this.folder.newFolder();
        return Paths.get(folder.getAbsolutePath(), filename);
    }

    @Test
    public void should_store_audit_report() throws Exception {
        // Given
        String processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(processId)).thenReturn(true);
        String filename = String.format("report.jsonl", processId);
        Path report = initialisePathWithFileName(filename);

        AuditStatsModel auditStatus = new AuditStatsModel(
            1,
            2,
            new HashSet<String>(),
            new AuditFullStatusCount(),
            new HashMap<>()
        );
        Document auditData = getAuditDocument(processId);
        FakeMongoCursor<Document> fakeMongoCursor = new FakeMongoCursor<>(Collections.singletonList(auditData));

        initialiseMockWhenPutAtomicObjectInWorkspace(report);
        when(
            auditReportRepository.findCollectionByProcessIdTenantAndStatus(processId, TENANT_ID, "WARNING", "KO")
        ).thenReturn(fakeMongoCursor);
        when(auditReportRepository.stats(processId, TENANT_ID)).thenReturn(auditStatus);

        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            processId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportResults reportResults = new ReportResults(1, 0, 0);
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.AUDIT,
            reportResults,
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeFileToWorkspace(reportInfo);

        // Then
        reportSummary.setExtendedInfo(JsonHandler.toJsonNode(auditStatus));
        String accumulatorExpected =
            JsonHandler.unprettyPrint(operationSummary) +
            "\n" +
            JsonHandler.unprettyPrint(reportSummary) +
            "\n" +
            JsonHandler.unprettyPrint(context) +
            "\n" +
            BsonHelper.stringify(auditData);

        assertThat(new String(Files.readAllBytes(report))).isEqualTo(accumulatorExpected);
    }

    @Test
    public void should_store_elimination_report() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(PROCESS_ID)).thenReturn(true);

        String filename = "report.jsonl";
        Path report = initialisePathWithFileName(filename);
        initialiseMockWhenPutAtomicObjectInWorkspace(report);

        List<Document> eliminationActionUnitDocs = getDocuments("eliminationAction_nonEliminableUnits.json");
        List<Document> unitPurgeDocs = getDocuments("eliminationAction_unitPurge.json");
        List<Document> objectGroupPurgeDocs = getDocuments("eliminationAction_objectGroupPurge.json");

        when(eliminationActionUnitRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(eliminationActionUnitDocs)
        );
        when(purgeUnitRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(unitPurgeDocs)
        );
        when(purgeObjectGroupRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(objectGroupPurgeDocs)
        );

        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            PROCESS_ID,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportResults reportResults = new ReportResults(1, 0, 0);
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.ELIMINATION_ACTION,
            reportResults,
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeFileToWorkspace(reportInfo);

        // Then
        // FIXME : Fix header in eliminationAction_expectedReport.jsonl
        assertJsonlReportsEqual(
            new FileInputStream(report.toFile()),
            PropertiesUtils.getResourceAsStream("eliminationAction_expectedReport.jsonl")
        );
    }

    @Test
    public void should_store_transfer_reply_report() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(PROCESS_ID)).thenReturn(true);

        String filename = "report.jsonl";
        Path report = initialisePathWithFileName(filename);
        initialiseMockWhenPutAtomicObjectInWorkspace(report);

        List<Document> transferReplyUnitDocs = getDocuments("transferReply_alreadyDeletedUnits.json");
        List<Document> unitPurgeDocs = getDocuments("transferReply_unitPurge.json");
        List<Document> objectGroupPurgeDocs = getDocuments("transferReply_objectGroupPurge.json");

        when(transferReplyUnitRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(transferReplyUnitDocs)
        );
        when(purgeUnitRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(unitPurgeDocs)
        );
        when(purgeObjectGroupRepository.findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID)).thenReturn(
            new FakeMongoCursor<>(objectGroupPurgeDocs)
        );

        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            PROCESS_ID,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportResults reportResults = new ReportResults(1, 0, 0);
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.TRANSFER_REPLY,
            reportResults,
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeFileToWorkspace(reportInfo);

        // Then
        // FIXME : Fix header in transferReply_expectedReport.jsonl
        assertJsonlReportsEqual(
            new FileInputStream(report.toFile()),
            PropertiesUtils.getResourceAsStream("transferReply_expectedReport.jsonl")
        );
    }

    private void assertJsonlReportsEqual(InputStream actualInputStream, InputStream expectedReportInputStream)
        throws InvalidParseOperationException {
        try (
            JsonLineGenericIterator<JsonNode> resultReportIterator = new JsonLineGenericIterator<>(
                actualInputStream,
                JSON_NODE_TYPE_REFERENCE
            );
            JsonLineGenericIterator<JsonNode> expectedReportIterator = new JsonLineGenericIterator<>(
                expectedReportInputStream,
                JSON_NODE_TYPE_REFERENCE
            )
        ) {
            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(IteratorUtils.toList(resultReportIterator)),
                JsonHandler.toJsonNode(IteratorUtils.toList(expectedReportIterator))
            );
        }
    }

    private List<Document> getDocuments(String resourcesFile)
        throws InvalidParseOperationException, FileNotFoundException {
        JsonNode alreadyDeletedUnits = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(resourcesFile)
        );

        return Streams.stream(alreadyDeletedUnits.elements())
            .map(unit -> Document.parse(unit.toString()))
            .collect(Collectors.toList());
    }

    @Test
    public void should_store_evidence_audit_report() throws Exception {
        // Given
        String processId = "aeaaaaaaaafpuagsab43oallwisewyaaaaaq";
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(processId)).thenReturn(true);
        String filename = String.format("report.jsonl", processId);
        Path report = initialisePathWithFileName(filename);

        EvidenceAuditStatsModel auditStatus = new EvidenceAuditStatsModel(1, 0, 1, new EvidenceAuditFullStatusCount());
        Document evidenceAuditData = getEvidenceAuditDocument(processId);
        FakeMongoCursor<Document> fakeMongoCursor = new FakeMongoCursor<>(Collections.singletonList(evidenceAuditData));

        initialiseMockWhenPutAtomicObjectInWorkspace(report);
        when(
            evidenceAuditReportRepository.findCollectionByProcessIdTenantAndStatus(
                processId,
                TENANT_ID,
                EvidenceStatus.WARN.name(),
                EvidenceStatus.KO.name()
            )
        ).thenReturn(fakeMongoCursor);
        when(evidenceAuditReportRepository.stats(processId, TENANT_ID)).thenReturn(auditStatus);

        OperationSummary operationSummary = new OperationSummary(
            TENANT_ID,
            processId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            JsonHandler.createObjectNode()
        );
        ReportResults reportResults = new ReportResults(1, 0, 0);
        ReportSummary reportSummary = new ReportSummary(
            null,
            null,
            ReportType.EVIDENCE_AUDIT,
            reportResults,
            JsonHandler.createObjectNode()
        );
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeFileToWorkspace(reportInfo);

        // Then
        reportSummary.setExtendedInfo(JsonHandler.toJsonNode(auditStatus));
        String accumulatorExpected =
            JsonHandler.unprettyPrint(operationSummary) +
            "\n" +
            JsonHandler.unprettyPrint(reportSummary) +
            "\n" +
            JsonHandler.unprettyPrint(context) +
            "\n" +
            BsonHelper.stringify(evidenceAuditData);

        assertThat(new String(Files.readAllBytes(report))).isEqualTo(accumulatorExpected);
    }

    @Test
    public void should_export_distinct_object_group_of_deleted_units() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        String objectGroupId = "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq";
        File folder = this.folder.newFolder();
        Path report = Paths.get(folder.getAbsolutePath(), "distinct_objectgroup.json");
        initialiseMockWhenPutAtomicObjectInWorkspace(report);

        FakeMongoCursor<String> mongoCursor = new FakeMongoCursor<>(List.of(objectGroupId));
        when(purgeUnitRepository.distinctObjectGroupOfDeletedUnits(PROCESS_ID, TENANT_ID)).thenReturn(mongoCursor);
        when(workspaceClient.isExistingContainer(PROCESS_ID)).thenReturn(true);

        // When
        batchReportServiceImpl.exportPurgeDistinctObjectGroupOfDeletedUnits(
            PROCESS_ID,
            "distinct_objectgroup_report",
            TENANT_ID
        );
        // Then
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(report.toFile())))) {
            while (reader.ready()) {
                String line = reader.readLine();
                assertThat(line).isNotNull();
            }
        }
    }

    @Test
    public void should_append_unit_computed_inherited_rules_invalidation_entries() {
        // Given
        List<UnitComputedInheritedRulesInvalidationReportEntry> entries = Arrays.asList(
            new UnitComputedInheritedRulesInvalidationReportEntry("unit1"),
            new UnitComputedInheritedRulesInvalidationReportEntry("unit2"),
            new UnitComputedInheritedRulesInvalidationReportEntry("unit3")
        );

        // When
        batchReportServiceImpl.appendUnitComputedInheritedRulesInvalidationReport("procId", entries, 1);

        // Then
        ArgumentCaptor<List<UnitComputedInheritedRulesInvalidationModel>> argumentCaptor = ArgumentCaptor.forClass(
            List.class
        );
        verify(this.unitComputedInheritedRulesInvalidationRepository).bulkAppendReport(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue())
            .extracting(
                UnitComputedInheritedRulesInvalidationModel::getProcessId,
                UnitComputedInheritedRulesInvalidationModel::getTenant,
                e -> e.getMetadata().getUnitId()
            )
            .containsExactly(tuple("procId", 1, "unit1"), tuple("procId", 1, "unit2"), tuple("procId", 1, "unit3"));
    }

    @Test
    public void should_cleanup_unit_inherited_rules_invalidation_report() {
        // Given

        // When
        batchReportServiceImpl.deleteUnitComputedInheritedRulesInvalidationReport("procId", 1);

        // Then
        verify(this.unitComputedInheritedRulesInvalidationRepository).deleteReportByIdAndTenant("procId", 1);
    }

    @Test
    public void should_export_unit_inherited_rules_invalidation_report() throws Exception {
        // Given
        String filename = "filename.jsonl";
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        Path report = initialisePathWithFileName(filename);
        initialiseMockWhenPutAtomicObjectInWorkspace(report);
        doReturn(
            CloseableIteratorUtils.toCloseableIterator(
                Arrays.asList(new Document("id", "unit1"), new Document("id", "unit2")).iterator()
            )
        )
            .when(unitComputedInheritedRulesInvalidationRepository)
            .findCollectionByProcessIdTenant("procId", 1);

        // When
        batchReportServiceImpl.exportUnitsToInvalidate("procId", 1, new ReportExportRequest(filename));

        // Then
        try (
            InputStream is = Files.newInputStream(report);
            JsonLineGenericIterator<JsonLineModel> reader = new JsonLineGenericIterator<>(is, TYPE_REFERENCE)
        ) {
            assertThat(reader).extracting(JsonLineModel::getId).containsExactly("unit1", "unit2");
        }
    }

    @Test
    public void should_store_extracted_metadata_for_au() {
        // Given
        ExtractedMetadata extractedMetadata = new ExtractedMetadata(
            "BATMAN",
            PROCESS_ID,
            TENANT_ID,
            Collections.singletonList("unitId"),
            Collections.singletonMap("MetadataKey", Collections.singletonList("MetadataValue"))
        );
        List<ExtractedMetadata> extractedMetadatas = Collections.singletonList(extractedMetadata);
        // When
        batchReportServiceImpl.storeExtractedMetadataForAu(extractedMetadatas);

        // Then
        verify(extractedMetadataRepository, Mockito.times(1)).addExtractedMetadataForAu(extractedMetadatas);
    }

    @Test
    public void should_create_distribution_file_for_au() throws Exception {
        // Given
        ExtractedMetadata extractedMetadata = new ExtractedMetadata(
            "BATMAN",
            PROCESS_ID,
            TENANT_ID,
            Collections.singletonList("unitId"),
            Collections.singletonMap("MetadataKey", Collections.singletonList("MetadataValue"))
        );
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        Path report = initialisePathWithFileName(PROCESS_ID + ".jsonl");
        initialiseMockWhenPutAtomicObjectInWorkspace(report);
        FakeMongoCursor<ExtractedMetadata> mongoCursor = new FakeMongoCursor<>(List.of(extractedMetadata));
        given(extractedMetadataRepository.getExtractedMetadataByProcessId(PROCESS_ID, TENANT_ID)).willReturn(
            mongoCursor
        );

        // When
        batchReportServiceImpl.createExtractedMetadataDistributionFileForAu(PROCESS_ID, TENANT_ID);

        // Then
        try (
            InputStream is = Files.newInputStream(report);
            JsonLineGenericIterator<JsonLineModel> reader = new JsonLineGenericIterator<>(is, TYPE_REFERENCE)
        ) {
            assertThat(reader).extracting(JsonLineModel::getId).containsExactly("unitId");
        }
    }

    @Test
    public void should_append_delete_Got_versions_report_entries()
        throws FileNotFoundException, InvalidParseOperationException {
        // Given
        List<DeleteGotVersionsReportEntry> entries = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile("deleteGotVersionsReport.json"),
            new TypeReference<>() {}
        );

        // When
        batchReportServiceImpl.appendDeleteGotVersionsReport(entries);

        // Then
        ArgumentCaptor<List<DeleteGotVersionsReportEntry>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.deleteGotVersionsReportRepository).bulkAppendReport(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue())
            .extracting(
                DeleteGotVersionsReportEntry::getProcessId,
                DeleteGotVersionsReportEntry::getTenant,
                e -> e.getObjectGroupGlobal().get(0).getStatus()
            )
            .containsExactly(
                tuple("aeeaaaaaacgqyd36abmc4aly7crq7syaaaaq", 0, WARNING),
                tuple("aeeaaaaaacgqyd36abmc4aly7crq7syaaaaq", 0, OK)
            );
    }

    @Test
    public void should_delete_metadata_by_processId_after_creating_distribution_file_for_au() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        given(extractedMetadataRepository.getExtractedMetadataByProcessId(PROCESS_ID, TENANT_ID)).willReturn(
            new EmptyMongoCursor<>()
        );

        // When
        batchReportServiceImpl.createExtractedMetadataDistributionFileForAu(PROCESS_ID, TENANT_ID);
        // Then
        verify(extractedMetadataRepository, times(1)).deleteExtractedMetadataByProcessId(PROCESS_ID, TENANT_ID);
    }

    private Document getPreservationDocument(String processId) {
        Document document = new Document();
        document.put(OUTCOME, "Outcome - TEST");
        document.put(DETAIL_TYPE, "preservation");
        document.put(DETAIL_ID, "aeaaaaaaaagw45nxabw2ualhc4jvawqaaaaq");
        document.put(PreservationReportEntry.PROCESS_ID, processId);
        document.put(TENANT, TENANT_ID);
        document.put(CREATION_DATE_TIME, "2018-11-15T11:13:20.986");
        document.put(STATUS, PreservationStatus.OK);
        document.put(UNIT_ID, "unitId");
        document.put(OBJECT_GROUP_ID, "objectGroupId");
        document.put(GRIFFIN_ID, "griffinId");
        document.put(SCENARIO_ID, "preservationScenarioId");
        document.put(ACTION, ANALYSE);
        document.put(ANALYSE_RESULT, "VALID_ALL");
        document.put(INPUT_OBJECT_ID, "aeaaaaaaaagh65wtab27ialg5fopxnaaaaaq");
        document.put(OUTPUT_OBJECT_ID, "");
        return document;
    }

    private Document getAuditDocument(String processId) throws InvalidParseOperationException {
        String reportDoc = JsonHandler.unprettyPrint(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/auditObjectGroupDocument.json"))
        );
        return Document.parse(reportDoc);
    }

    private Document getEvidenceAuditDocument(String processId)
        throws InvalidParseOperationException, FileNotFoundException {
        String reportDoc = JsonHandler.unprettyPrint(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/evidenceAuditObjectDocument.json"))
        );
        return Document.parse(reportDoc);
    }

    private void initialiseMockWhenPutAtomicObjectInWorkspace(Path report)
        throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream argumentAt = invocation.getArgument(2);
            Files.copy(argumentAt, report);
            return null;
        })
            .when(workspaceClient)
            .putAtomicObject(anyString(), anyString(), any(InputStream.class), anyLong());
    }
}
