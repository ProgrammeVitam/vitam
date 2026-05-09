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
package fr.gouv.vitam.audit.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeCheck;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportEntry;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportV2;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.readReportFile;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Probative Value audit integration test
 */

public class ProbativeValueAuditIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "Context_IT";
    private static final String SIP_1_UNIT_1_GOTS = "ProbativeValue/1_UNIT_1_GOT.zip";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProbativeValueAuditIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            BatchReportMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            AccessExternalMain.class,
            IngestInternalMain.class
        )
    );

    private static final DataLoader dataLoader = new DataLoader("integration-ingest-internal");

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String configurationPath = PropertiesUtils.getResourcePath(
            "integration-ingest-internal/format-identifiers.conf"
        ).toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        dataLoader.prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
    }

    @After
    public void afterTest() {
        handleAfter();
        // Reset SecurisationVersion to default for other tests
        VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
        VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
        VitamConfiguration.setLogbookOperationTraceabilityVersion(
            TENANT_ID,
            VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_probative_value_audit_without_detached_signing_information() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ingestOperationId = VitamTestHelper.doIngest(
            TENANT_ID,
            "ProbativeValue/ProbativeValue_SigningInformation.zip"
        );
        verifyOperation(ingestOperationId, OK);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        // When
        String evidenceAuditOperation;
        SelectMultiQuery query = new SelectMultiQuery();
        query.setQuery(
            QueryHelper.and()
                .add(
                    QueryHelper.in("OriginatingSystemId", "Unit1", "Unit2", "Unit6", "Unit10", "Unit16"),
                    QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId)
                )
        );

        evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect(), false);

        ProbativeReportV2 report = JsonHandler.getFromString(
            readReportFile(evidenceAuditOperation + ".json"),
            ProbativeReportV2.class
        );

        // There might be WARNING due to no previous traceability operations in the system
        assertThat(report.getOperationSummary().getOutcome()).isIn("OK", "WARNING");
        assertThat(report.getReportEntries()).hasSize(4);
        assertThat(
            report.getReportSummary().getVitamResults().getNbOk() +
            report.getReportSummary().getVitamResults().getNbWarning()
        ).isEqualTo(4);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);

        List<String> unitIds = report
            .getReportEntries()
            .stream()
            .flatMap(i -> i.getUnitIds().stream())
            .collect(Collectors.toList());
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(QueryHelper.in("#id", unitIds.toArray(String[]::new)));
            selectMultiQuery.addUsedProjection("Title");
            JsonNode results = client.selectUnits(selectMultiQuery.getFinalSelect()).get("$results");
            List<String> titles = new ArrayList<>();
            for (JsonNode result : results) {
                titles.add(result.get("Title").asText());
            }
            assertThat(titles).containsExactlyInAnyOrder("Unit2", "Unit6", "Unit10", "Unit16");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_probative_value_audit_with_detached_signing_information() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ingestOperationId = VitamTestHelper.doIngest(
            TENANT_ID,
            "ProbativeValue/ProbativeValue_SigningInformation.zip"
        );
        verifyOperation(ingestOperationId, OK);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        // When
        String evidenceAuditOperation;
        SelectMultiQuery query = new SelectMultiQuery();
        query.setQuery(
            QueryHelper.and()
                .add(
                    QueryHelper.in("OriginatingSystemId", "Unit1", "Unit2", "Unit6", "Unit10", "Unit16"),
                    QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId)
                )
        );

        evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect(), true);

        ProbativeReportV2 report = JsonHandler.getFromString(
            readReportFile(evidenceAuditOperation + ".json"),
            ProbativeReportV2.class
        );

        // There might be WARNING due to no previous traceability operations in the system
        assertThat(report.getOperationSummary().getOutcome()).isIn("OK", "WARNING");
        assertThat(report.getReportEntries()).hasSize(7);
        assertThat(
            report.getReportSummary().getVitamResults().getNbOk() +
            report.getReportSummary().getVitamResults().getNbWarning()
        ).isEqualTo(7);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);

        List<String> unitIds = report
            .getReportEntries()
            .stream()
            .flatMap(i -> i.getUnitIds().stream())
            .collect(Collectors.toList());
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(QueryHelper.in("#id", unitIds.toArray(String[]::new)));
            selectMultiQuery.addUsedProjection("Title");
            JsonNode results = client.selectUnits(selectMultiQuery.getFinalSelect()).get("$results");
            List<String> titles = new ArrayList<>();
            for (JsonNode result : results) {
                titles.add(result.get("Title").asText());
            }
            assertThat(titles).containsExactlyInAnyOrder(
                "Unit2",
                "Unit6",
                "Unit10",
                "Unit13",
                "Unit14",
                "Unit16",
                "Unit17"
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_probative_value_audit_bug_13066() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        List<String> ingestOperationIds = new ArrayList<>();

        // First ingest
        String ingestOperationId1 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
        verifyOperation(ingestOperationId1, OK);
        ingestOperationIds.add(ingestOperationId1);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        for (int i = 0; i < 3; i++) {
            String ingestOperationId = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId, OK);
            ingestOperationIds.add(ingestOperationId);

            logicalClock.logicalSleep(4, ChronoUnit.MINUTES);

            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();
        }

        // One last traceability
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        // When
        String evidenceAuditOperation;
        SelectMultiQuery query = new SelectMultiQuery();
        query.setQuery(
            QueryHelper.and()
                .add(QueryHelper.in(VitamFieldsHelper.initialOperation(), ingestOperationIds.toArray(String[]::new)))
        );

        evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect(), false);

        ProbativeReportV2 report = JsonHandler.getFromString(
            readReportFile(evidenceAuditOperation + ".json"),
            ProbativeReportV2.class
        );

        // First traceability will always have a WARNING status (no previous traceability to check)
        assertThat(report.getReportSummary().getVitamResults().getNbWarning()).isEqualTo(1);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);
        assertThat(report.getReportSummary().getVitamResults().getNbOk()).isEqualTo(ingestOperationIds.size() - 1);

        List<ProbativeReportEntry> warningEntries = report
            .getReportEntries()
            .stream()
            .filter(e -> WARNING.equals(e.getStatus()))
            .collect(Collectors.toList());
        assertThat(warningEntries).hasSize(1);

        assertThat(warningEntries.get(0).getChecks().stream().map(ProbativeCheck::getStatus)).containsOnly(OK, WARNING);

        assertThat(
            warningEntries
                .get(0)
                .getChecks()
                .stream()
                .filter(c -> WARNING.equals(c.getStatus()))
                .map(ProbativeCheck::getName)
        ).containsAnyOf(
            PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION.name(),
            PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON.name(),
            PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION.name(),
            PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON.name()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_probative_value_audit_with_securisation_version_v2_OK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, "V2");
        VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, "V2");
        VitamConfiguration.setLogbookOperationTraceabilityVersion(TENANT_ID, "V2");

        String ingestOperationId1 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
        verifyOperation(ingestOperationId1, OK);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        String ingestOperationId2 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
        verifyOperation(ingestOperationId2, OK);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        SelectMultiQuery query = new SelectMultiQuery();
        query.setQuery(QueryHelper.and().add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId1)));

        String evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect(), false);

        // Then
        ProbativeReportV2 report = JsonHandler.getFromString(
            readReportFile(evidenceAuditOperation + ".json"),
            ProbativeReportV2.class
        );

        // Verify the audit completed successfully (audit might be WARNING for the very first traceability)
        assertThat(report.getOperationSummary().getOutcome()).isIn("OK", "WARNING");
        assertThat(report.getReportEntries()).hasSize(1);
        assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);

        // When - Run probative value audit for the second ingest (V2)
        SelectMultiQuery query2 = new SelectMultiQuery();
        query2.setQuery(
            QueryHelper.and().add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId2))
        );

        String evidenceAuditOperation2 = runProbativeValueAudit(query2.getFinalSelect(), false);

        verifyOperation(evidenceAuditOperation2, OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_probative_value_audit_when_securisation_v1_and_audit_v2() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Ingest with default SecurisationVersion (V1)
        String ingestOperationId = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
        verifyOperation(ingestOperationId, OK);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // Securisation (traceability) with V1
        VitamTestHelper.doTraceabilityUnits();
        VitamTestHelper.doTraceabilityGots();
        VitamTestHelper.doTraceabilityOperations();

        // Switch to SecurisationVersion V2 for audit
        VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, "V2");
        VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, "V2");
        VitamConfiguration.setLogbookOperationTraceabilityVersion(TENANT_ID, "V2");

        // When - Run probative value audit with V2 (should fail)
        SelectMultiQuery query = new SelectMultiQuery();
        query.setQuery(QueryHelper.and().add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId)));

        String operationId;

        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<?> requestResponse = adminExternalClient.exportProbativeValue(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT_ID),
                new ProbativeValueRequest(query.getFinalSelect(), "BinaryMaster", "1", false)
            );

            assertThat(requestResponse.isOk()).isTrue();
            operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            // Await termination and check status ==> should be due to version mismatch
            waitOperation(operationId);
            verifyOperation(operationId, KO);

            ProbativeReportV2 report = JsonHandler.getFromString(
                readReportFile(operationId + ".json"),
                ProbativeReportV2.class
            );
            assertThat(report.getReportSummary().getVitamResults().getNbOk()).isEqualTo(0);
            assertThat(report.getReportSummary().getVitamResults().getNbWarning()).isEqualTo(0);
            assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(1);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_probative_value_audit_with_mixed_securisation_versions() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        List<String> ingestOperationIds = new ArrayList<>();

        try {
            // First cycle: ingest with default SecurisationVersion (V1)
            String ingestOperationId1 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId1, OK);
            ingestOperationIds.add(ingestOperationId1);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // First traceability with V1
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Second cycle: Switch to SecurisationVersion V2
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLogbookOperationTraceabilityVersion(TENANT_ID, "V2");

            // Second ingest with V2
            String ingestOperationId2 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId2, OK);
            ingestOperationIds.add(ingestOperationId2);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Second traceability with V2
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Third cycle: Switch back to V1
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
            VitamConfiguration.setLfcUnitTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );
            VitamConfiguration.setLogbookOperationTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );

            // Third ingest with V1 again
            String ingestOperationId3 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId3, OK);
            ingestOperationIds.add(ingestOperationId3);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Third traceability with V1
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Fourth cycle: Switch to V2 again
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLogbookOperationTraceabilityVersion(TENANT_ID, "V2");

            // Fourth ingest with V2
            String ingestOperationId4 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId4, OK);
            ingestOperationIds.add(ingestOperationId4);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Fourth traceability with V2
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Fifth cycle: Switch back to V1
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
            VitamConfiguration.setLfcUnitTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );
            VitamConfiguration.setLogbookOperationTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );

            // Fifth ingest with V1
            String ingestOperationId5 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId5, OK);
            ingestOperationIds.add(ingestOperationId5);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Fifth traceability with V1
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Sixth cycle: Switch to V2 one more time
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLfcUnitTraceabilityVersion(TENANT_ID, "V2");
            VitamConfiguration.setLogbookOperationTraceabilityVersion(TENANT_ID, "V2");

            // Sixth ingest with V2
            String ingestOperationId6 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId6, OK);
            ingestOperationIds.add(ingestOperationId6);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Sixth traceability with V2
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // Seventh cycle: Final switch back to V1
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
            VitamConfiguration.setLfcUnitTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );
            VitamConfiguration.setLogbookOperationTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );

            // Seventh ingest with V1
            String ingestOperationId7 = VitamTestHelper.doIngest(TENANT_ID, SIP_1_UNIT_1_GOTS);
            verifyOperation(ingestOperationId7, OK);
            ingestOperationIds.add(ingestOperationId7);

            logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

            // Seventh traceability with V1
            VitamTestHelper.doTraceabilityUnits();
            VitamTestHelper.doTraceabilityGots();
            VitamTestHelper.doTraceabilityOperations();

            // When - Run probative value audit for all ingests
            SelectMultiQuery query = new SelectMultiQuery();
            query.setQuery(
                QueryHelper.and()
                    .add(
                        QueryHelper.in(VitamFieldsHelper.initialOperation(), ingestOperationIds.toArray(new String[0]))
                    )
            );

            String evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect(), false);

            // Then
            ProbativeReportV2 report = JsonHandler.getFromString(
                readReportFile(evidenceAuditOperation + ".json"),
                ProbativeReportV2.class
            );

            // Verify the audit completed successfully for all ingests
            assertThat(report.getOperationSummary().getOutcome()).isIn("OK", "WARNING");
            assertThat(report.getReportEntries()).hasSize(7);
            assertThat(report.getReportSummary().getVitamResults().getNbKo()).isEqualTo(0);
        } finally {
            // Reset SecurisationVersion to default for other tests (audit might be WARNING for the very first traceability)
            VitamConfiguration.setLfcGotTraceabilityVersion(TENANT_ID, VitamConfiguration.DEFAULT_TRACEABILITY_VERSION);
            VitamConfiguration.setLfcUnitTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );
            VitamConfiguration.setLogbookOperationTraceabilityVersion(
                TENANT_ID,
                VitamConfiguration.DEFAULT_TRACEABILITY_VERSION
            );
        }
    }

    private String runProbativeValueAudit(JsonNode query, boolean includeDetachedSigningInformation)
        throws VitamClientException {
        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<?> requestResponse = adminExternalClient.exportProbativeValue(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT_ID),
                new ProbativeValueRequest(query, "BinaryMaster", "1", includeDetachedSigningInformation)
            );

            assertThat(requestResponse.isOk()).isTrue();
            String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            waitOperation(operationId);
            verifyOperation(operationId, OK);
            return operationId;
        }
    }
}
