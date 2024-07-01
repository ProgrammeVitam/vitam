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
package fr.gouv.vitam.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ReportManagementIT
 */
public class ReportManagementIT extends VitamRuleRunner {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};
    private static final String PROCESS_ID = "123456789";
    private static final int TENANT_0 = 0;
    private static BatchReportClient batchReportClient;
    private static WorkspaceClient workspaceClient;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ReportManagementIT.class,
        mongoRule.getMongoDatabase().getName(),
        elasticsearchRule.getClusterName(),
        Sets.newHashSet(WorkspaceMain.class, BatchReportMain.class)
    );

    @AfterClass
    public static void afterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() throws Exception {
        workspaceClient.deleteContainer(PROCESS_ID, true);
        runAfter();
    }

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        BatchReportClientFactory batchReportClientFactory = BatchReportClientFactory.getInstance();
        batchReportClient = batchReportClientFactory.getClient();
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        workspaceClient.createContainer("123456789");
    }

    @Test
    @RunWithCustomExecutor
    public void should_append_elimination_action_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/eliminationUnitModel.json");
        ReportBody<EliminationActionUnitReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            EliminationActionUnitReportEntry.class
        );
        // When
        // Then
        assertThatCode(() -> batchReportClient.appendReportEntries(reportBody)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_append_purge_action_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/purgeUnitModel.json");
        ReportBody<PurgeUnitReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PurgeUnitReportEntry.class
        );
        // When
        // Then
        assertThatCode(() -> batchReportClient.appendReportEntries(reportBody)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_append_purge_objectGroup_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/purgeObjectGroupModel.json");
        ReportBody<PurgeObjectGroupReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PurgeObjectGroupReportEntry.class
        );
        // When
        // Then
        assertThatCode(() -> batchReportClient.appendReportEntries(reportBody)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_append_preservation_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/preservationModel.json");
        ReportBody<PreservationReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PreservationReportEntry.class
        );
        // When
        // Then
        assertThatCode(() -> batchReportClient.appendReportEntries(reportBody)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_append_audit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/auditObjectGroupModel.json");
        ReportBody<AuditObjectGroupReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            AuditObjectGroupReportEntry.class
        );
        // When / Then
        assertThatCode(() -> batchReportClient.appendReportEntries(reportBody)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_distinct_objectgroup_in_unit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/purgeUnitModel.json");
        ReportBody<EliminationActionUnitReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            EliminationActionUnitReportEntry.class
        );
        ReportExportRequest reportExportRequest = new ReportExportRequest("test.json");
        batchReportClient.appendReportEntries(reportBody);
        // When / Then
        assertThatCode(
            () ->
                batchReportClient.generatePurgeDistinctObjectGroupInUnitReport(
                    reportBody.getProcessId(),
                    reportExportRequest
                )
        ).doesNotThrowAnyException();

        checkGeneratedReportEqualsExpectedJsonl(
            "test.json",
            "report/purgeUnitModel_expectedDistinctObjectGroupReport.jsonl"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_preservation_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/preservationModel.json");
        ReportBody<PreservationReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PreservationReportEntry.class
        );
        batchReportClient.appendReportEntries(reportBody);

        Integer tenant = TENANT_0;
        String evId = PROCESS_ID;
        JsonNode evDetData = JsonHandler.createObjectNode(); // Will be set later by appended status data
        OperationSummary operationSummary = new OperationSummary(
            tenant,
            evId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            evDetData
        );

        String date = LocalDateUtil.getString(LocalDateUtil.now());
        ReportType reportType = ReportType.PRESERVATION;
        ReportResults vitamResults = new ReportResults();
        JsonNode extendedInfo = JsonHandler.createObjectNode();
        ReportSummary reportSummary = new ReportSummary(date, date, reportType, vitamResults, extendedInfo);

        JsonNode context = JsonHandler.createObjectNode();

        Report report = new Report(operationSummary, reportSummary, context);

        // When / Then
        assertThatCode(() -> batchReportClient.storeReportToWorkspace(report)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_elimination_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/purgeObjectGroupModel.json");
        ReportBody<PurgeObjectGroupReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            PurgeObjectGroupReportEntry.class
        );
        batchReportClient.appendReportEntries(reportBody);

        InputStream stream2 = getClass().getResourceAsStream("/report/eliminationUnitModel.json");
        ReportBody<EliminationActionUnitReportEntry> reportBody2 = JsonHandler.getFromInputStream(
            stream2,
            ReportBody.class,
            EliminationActionUnitReportEntry.class
        );
        batchReportClient.appendReportEntries(reportBody2);

        InputStream stream3 = getClass().getResourceAsStream("/report/purgeUnitModel.json");
        ReportBody<PurgeUnitReportEntry> reportBody3 = JsonHandler.getFromInputStream(
            stream3,
            ReportBody.class,
            PurgeUnitReportEntry.class
        );
        batchReportClient.appendReportEntries(reportBody3);

        Integer tenant = TENANT_0;
        String evId = PROCESS_ID;
        JsonNode evDetData = JsonHandler.createObjectNode(); // Will be set later by appended status data
        OperationSummary operationSummary = new OperationSummary(
            tenant,
            evId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            evDetData
        );

        String date = LocalDateUtil.getString(LocalDateUtil.now());
        ReportType reportType = ReportType.ELIMINATION_ACTION;
        ReportResults vitamResults = new ReportResults();
        JsonNode extendedInfo = JsonHandler.createObjectNode();
        ReportSummary reportSummary = new ReportSummary(date, date, reportType, vitamResults, extendedInfo);

        JsonNode context = JsonHandler.createObjectNode();

        Report report = new Report(operationSummary, reportSummary, context);

        // When / Then
        assertThatCode(() -> batchReportClient.storeReportToWorkspace(report)).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_audit_report() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/report/auditObjectGroupModel.json");
        ReportBody<AuditObjectGroupReportEntry> reportBody = JsonHandler.getFromInputStream(
            stream,
            ReportBody.class,
            AuditObjectGroupReportEntry.class
        );
        batchReportClient.appendReportEntries(reportBody);

        Integer tenant = TENANT_0;
        String evId = PROCESS_ID;
        JsonNode evDetData = JsonHandler.createObjectNode(); // Will be set later by appended status data
        OperationSummary operationSummary = new OperationSummary(
            tenant,
            evId,
            "",
            "",
            "",
            "",
            JsonHandler.createObjectNode(),
            evDetData
        );

        String date = LocalDateUtil.getString(LocalDateUtil.now());
        ReportType reportType = ReportType.AUDIT;
        ReportResults vitamResults = new ReportResults();
        JsonNode extendedInfo = JsonHandler.createObjectNode();
        ReportSummary reportSummary = new ReportSummary(date, date, reportType, vitamResults, extendedInfo);

        JsonNode context = JsonHandler.createObjectNode();

        Report report = new Report(operationSummary, reportSummary, context);

        // When / Then
        assertThatCode(() -> batchReportClient.storeReportToWorkspace(report)).doesNotThrowAnyException();
    }

    private void checkGeneratedReportEqualsExpectedJsonl(String workspaceReportFile, String expectedJsonlResources)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        try (
            InputStream reportIS = new VitamAsyncInputStream(
                workspaceClient.getObject(PROCESS_ID, workspaceReportFile)
            );
            InputStream expectedIS = PropertiesUtils.getResourceAsStream(expectedJsonlResources)
        ) {
            Map<String, String> reportEntriesById = parseJsonLineReport(reportIS);
            Map<String, String> expectedEntriesById = parseJsonLineReport(expectedIS);

            assertThat(reportEntriesById.keySet()).containsExactlyInAnyOrderElementsOf(expectedEntriesById.keySet());

            for (String id : expectedEntriesById.keySet()) {
                JsonAssert.assertJsonEquals(
                    expectedEntriesById.get(id),
                    reportEntriesById.get(id),
                    JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
                );
            }
        }
    }

    private Map<String, String> parseJsonLineReport(InputStream inputStream) {
        Map<String, String> reportEntriesById = new HashMap<>();

        try (
            JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
                inputStream,
                TYPE_REFERENCE
            )
        ) {
            while (jsonLineIterator.hasNext()) {
                JsonLineModel entry = jsonLineIterator.next();

                assertThat(reportEntriesById).doesNotContainKeys(entry.getId());
                reportEntriesById.put(entry.getId(), JsonHandler.unprettyPrint(entry));
            }
        }

        return reportEntriesById;
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_units_to_invalidate_distribution_file() throws Exception {
        // Given
        String processId = VitamThreadUtils.getVitamSession().getRequestId();
        workspaceClient.createContainer(processId);

        List<String> ids1 = Arrays.asList("id1", "id2");
        List<String> ids2 = Arrays.asList("id1", "id3");
        List<String> ids3 = Collections.emptyList();
        List<String> ids4 = Collections.singletonList("id4");
        String unitsJsonlFileName = "myFileName.jsonl";

        // When
        batchReportClient.appendReportEntries(getReportBody(processId, ids1));
        batchReportClient.appendReportEntries(getReportBody(processId, ids2));
        batchReportClient.appendReportEntries(getReportBody(processId, ids3));
        batchReportClient.appendReportEntries(getReportBody(processId, ids4));
        batchReportClient.exportUnitsToInvalidate(processId, new ReportExportRequest(unitsJsonlFileName));

        // Then
        try (
            InputStream reportIS = new VitamAsyncInputStream(workspaceClient.getObject(processId, unitsJsonlFileName));
            JsonLineGenericIterator<JsonLineModel> lineGenericIterator = new JsonLineGenericIterator<>(
                reportIS,
                TYPE_REFERENCE
            )
        ) {
            Set<String> expectedDeduplicatedIds = new HashSet<>();
            expectedDeduplicatedIds.addAll(ids1);
            expectedDeduplicatedIds.addAll(ids2);
            expectedDeduplicatedIds.addAll(ids3);
            expectedDeduplicatedIds.addAll(ids4);

            assertThat(
                lineGenericIterator.stream().map(JsonLineModel::getId).collect(Collectors.toList())
            ).containsExactlyInAnyOrder(expectedDeduplicatedIds.toArray(new String[0]));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_Extracted_Metadata_For_Au() throws Exception {
        // Given
        List<ExtractedMetadata> extractedMetadata = Collections.singletonList(
            new ExtractedMetadata(
                "BATMAN",
                "processId",
                0,
                Collections.singletonList("unitId"),
                Collections.singletonMap("key", Collections.singletonList("value"))
            )
        );

        // When
        // Then
        assertThatCode(
            () -> batchReportClient.storeExtractedMetadataForAu(extractedMetadata)
        ).doesNotThrowAnyException();
    }

    private ReportBody getReportBody(String processId, List<String> unitsIds) {
        List<UnitComputedInheritedRulesInvalidationReportEntry> entries = unitsIds
            .stream()
            .distinct()
            .map(UnitComputedInheritedRulesInvalidationReportEntry::new)
            .collect(Collectors.toList());
        return new ReportBody<>(processId, ReportType.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION, entries);
    }
}
