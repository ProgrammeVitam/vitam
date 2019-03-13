package fr.gouv.vitam.batch.report.rest.service; /*******************************************************************************
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
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.ReportEntry;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionObjectGroupRepository;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.repository.PreservationReportRepository;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.FakeMongoCursor;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.*;
import static fr.gouv.vitam.batch.report.model.entry.ReportEntry.*;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class BatchReportServiceImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EliminationActionUnitRepository eliminationActionUnitRepository;

    @Mock
    private EliminationActionObjectGroupRepository eliminationActionObjectGroupRepository;

    @Mock
    private PreservationReportRepository preservationReportRepository;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private BackupService backupService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private EmptyMongoCursor emptyMongoCursor;

    private BatchReportServiceImpl batchReportServiceImpl;

    private String PROCESS_ID = "123456789";
    private int TENANT_ID = 0;

    @Before
    public void setUp() throws Exception {
        backupService = new BackupService(workspaceClientFactory, storageClientFactory);

        batchReportServiceImpl = new BatchReportServiceImpl(eliminationActionUnitRepository,
            eliminationActionObjectGroupRepository, backupService, workspaceClientFactory,
            preservationReportRepository);
    }


    @Test
    public void should_append_elimination_object_group_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/eliminationObjectGroupModel.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(stream, ReportBody.class, EliminationActionObjectGroupReportEntry.class);
        // When
        // Then
        assertThatCode(() ->
            batchReportServiceImpl
                .appendEliminationActionObjectGroupReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID))
            .doesNotThrowAnyException();
    }

    @Test
    public void should_append_elimination_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/eliminationUnitModel.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(stream, ReportBody.class, EliminationActionUnitReportEntry.class);
        // When
        // Then
        assertThatCode(() ->
            batchReportServiceImpl.appendEliminationActionUnitReport(PROCESS_ID, reportBody.getEntries(), TENANT_ID))
            .doesNotThrowAnyException();
    }

    @Test
    public void should_append_preservation_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/preservationReport.json");
        ReportBody reportBody = JsonHandler.getFromInputStream(stream, ReportBody.class, PreservationReportEntry.class);

        // When
        ThrowingCallable append = () -> batchReportServiceImpl.appendPreservationReport(reportBody.getProcessId(), reportBody.getEntries(), TENANT_ID);

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

        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(storageClient.storeFileFromWorkspace(anyString(), any(), anyString(), any())).thenReturn(null);

        PreservationStatsModel preservationStatus = new PreservationStatsModel(0, 1, 0, 1, 0, 0, 0, new HashMap<>());
        Document preservationData = getPreservationDocument(processId);
        FakeMongoCursor<Document> fakeMongoCursor = new FakeMongoCursor<>(Collections.singletonList(preservationData));

        initialiseMockWhenPutObjectInWorkspace(report);
        when(preservationReportRepository.findCollectionByProcessIdTenant(processId, TENANT_ID))
            .thenReturn(fakeMongoCursor);
        when(preservationReportRepository.stats(processId, TENANT_ID)).thenReturn(preservationStatus);

        OperationSummary operationSummary = new OperationSummary(TENANT_ID, processId, "", "", "", JsonHandler.createObjectNode(), JsonHandler.createObjectNode());
        ReportResults reportResults = new ReportResults(1, 0, 0, 1);
        ReportSummary reportSummary = new ReportSummary(null, null, ReportType.PRESERVATION, reportResults, JsonHandler.createObjectNode());
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeReport(reportInfo);

        // Then
        reportSummary.setExtendedInfo(JsonHandler.toJsonNode(preservationStatus));
        String accumulatorExpected = JsonHandler.unprettyPrint(operationSummary)
            + "\n" + JsonHandler.unprettyPrint(reportSummary)
            + "\n" + JsonHandler.unprettyPrint(context)
            + "\n" + JsonHandler.unprettyPrint(preservationData);

        assertThat(new String(Files.readAllBytes(report))).isEqualTo(accumulatorExpected);
    }

    private Path initialisePathWithFileName(String filename) throws IOException {
        File folder = this.folder.newFolder();
        return Paths.get(folder.getAbsolutePath(), filename);
    }


    @Test
    public void should_store_elimination_report() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workspaceClient.isExistingContainer(PROCESS_ID)).thenReturn(true);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(storageClient.storeFileFromWorkspace(anyString(), any(), anyString(), any())).thenReturn(null);

        String filename = "report.jsonl";
        Path report = initialisePathWithFileName(filename);
        initialiseMockWhenPutObjectInWorkspace(report);

        Document unitData = getUnitDocument();
        FakeMongoCursor<Document> fakeUnitMongoCursor = new FakeMongoCursor<>(Collections.singletonList(unitData));
        Document objectGroupData = getObjectGroupDocument();
        FakeMongoCursor<Document> fakeObjectGroupMongoCursor = new FakeMongoCursor<>(Collections.singletonList(objectGroupData));

        when(eliminationActionUnitRepository
            .findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID))
            .thenReturn(fakeUnitMongoCursor);
        when(eliminationActionObjectGroupRepository
            .findCollectionByProcessIdTenant(PROCESS_ID, TENANT_ID))
            .thenReturn(fakeObjectGroupMongoCursor);



        OperationSummary operationSummary = new OperationSummary(TENANT_ID, PROCESS_ID, "", "", "", JsonHandler.createObjectNode(), JsonHandler.createObjectNode());
        ReportResults reportResults = new ReportResults(1, 0, 0, 1);
        ReportSummary reportSummary = new ReportSummary(null, null, ReportType.ELIMINATION_ACTION, reportResults, JsonHandler.createObjectNode());
        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        // When
        batchReportServiceImpl.storeReport(reportInfo);

        // Then
        String accumulatorExpected = JsonHandler.unprettyPrint(operationSummary)
            + "\n" + JsonHandler.unprettyPrint(reportSummary)
            + "\n" + JsonHandler.unprettyPrint(context)
            + "\n" + JsonHandler.unprettyPrint(unitData)
            + "\n" + JsonHandler.unprettyPrint(objectGroupData);

        assertThat(new String(Files.readAllBytes(report))).isEqualTo(accumulatorExpected);
    }

    @Test
    public void should_export_distinct_object_group_of_deleted_units() throws Exception {
        // Given
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        String objectGroupId = "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq";
        File folder = this.folder.newFolder();
        Path report = Paths.get(folder.getAbsolutePath(), "distinct_objectgroup.json");
        initialiseMockWhenPutObjectInWorkspace(report);
        when(emptyMongoCursor.next()).thenReturn(objectGroupId);
        when(emptyMongoCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(eliminationActionUnitRepository
            .distinctObjectGroupOfDeletedUnits(PROCESS_ID, TENANT_ID))
            .thenReturn(emptyMongoCursor);
        when(workspaceClient.isExistingContainer(PROCESS_ID)).thenReturn(true);
        // When
        batchReportServiceImpl.exportEliminationActionDistinctObjectGroupOfDeletedUnits(PROCESS_ID, "distinct_objectgroup_report",
                TENANT_ID);
        // Then
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(report.toFile())))) {
            while (reader.ready()) {
                String line = reader.readLine();
                assertThat(line).isNotNull();
            }
        }
    }

    private Document getUnitDocument() {
        Document document = new Document();
        document.put("id", "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq");
        document.put("distribGroup", null);
        document.put("params", JsonHandler.createObjectNode()
            .put("id", "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq")
            .put("opi", "test")
            .put("objectGroupId", "12345687")
            .put("unitId", "unitId")
            .put("status", "DESTROY"));
        return document;
    }

    private Document getObjectGroupDocument() throws InvalidParseOperationException {
        Document document = new Document();
        document.put("id", "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaap");
        document.put("distribGroup", null);
        document.put("params", JsonHandler.createObjectNode()
            .put("id", "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq")
            .put("opi", "test")
            .put("objectGroupId", "12345687")
            .put("status", "DELETED")
            .putPOJO("deletedParentUnitIds",
                Arrays.asList("aeaaaaaaaafr5hk6abgeualfvd6jsniaaaaq", "aeaaaaaaaafr5hk6abgeualfvd6jsniaaaar"))
        );
        return document;
    }

    private Document getPreservationDocument(String processId) {
        Document document = new Document();
        document.put(OUTCOME, "Outcome - TEST");
        document.put(DETAIL_TYPE, "preservation");
        document.put(DETAIL_ID, "aeaaaaaaaagw45nxabw2ualhc4jvawqaaaaq");
        document.put(ID, "aeaaaaaaaagw45nxabw2ualhc4jvawqaaaaq");
        document.put(PreservationReportEntry.PROCESS_ID, processId);
        document.put(TENANT, TENANT_ID);
        document.put(CREATION_DATE_TIME, "2018-11-15T11:13:20.986");
        document.put(STATUS, PreservationStatus.OK);
        document.put(UNIT_ID, "unitId");
        document.put(OBJECT_GROUP_ID, "objectGroupId");
        document.put(ACTION, ANALYSE);
        document.put(ANALYSE_RESULT, "VALID_ALL");
        document.put(INPUT_OBJECT_ID, "aeaaaaaaaagh65wtab27ialg5fopxnaaaaaq");
        document.put(OUTPUT_OBJECT_ID, "");
        return document;
    }

    private void initialiseMockWhenPutObjectInWorkspace(Path report) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream argumentAt = invocation.getArgument(2);
            Files.copy(argumentAt, report);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));
    }
}
