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

        evidenceAuditOperation = runProbativeValueAudit(query.getFinalSelect());

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

    private String runProbativeValueAudit(JsonNode query) throws VitamClientException {
        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<?> requestResponse = adminExternalClient.exportProbativeValue(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT_ID),
                new ProbativeValueRequest(query, "BinaryMaster", "1")
            );

            assertThat(requestResponse.isOk()).isTrue();
            String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            waitOperation(operationId);
            verifyOperation(operationId, OK);
            return operationId;
        }
    }
}
