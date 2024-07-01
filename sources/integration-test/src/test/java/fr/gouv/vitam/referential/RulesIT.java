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
package fr.gouv.vitam.referential;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientBadRequestException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.core.rules.FileRulesManagementReport;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.shiro.util.CollectionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for rule referential
 */
public class RulesIT extends VitamRuleRunner {

    private static final int TENANT_0 = 0;
    private static final String RULES_SIMPLE_RULES_SET_1_CSV = "rules/simple_rules_set_1.csv";
    private static final String RULES_SIMPLE_RULES_SET_2_CSV = "rules/simple_rules_set_2.csv";
    private static final TypeReference<List<String>> STRING_LIST_TYPE_REFERENCE = new TypeReference<>() {};

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        RulesIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AdminManagementMain.class,
            ProcessManagementMain.class,
            BatchReportMain.class
        )
    );

    @BeforeClass
    public static void beforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-processing").prepareData();
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        cleanupDb();
    }

    private void cleanupDb() {
        // We need to keep some referentials (eg Ontology for rule unit update workflow)
        FunctionalAdminCollectionsTestUtils.afterTest(Collections.singletonList(FunctionalAdminCollections.RULES));
        handleAfter();
    }

    @After
    public void tearDown() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void givenEmptyDbWhenCheckRulesThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // When
        FileRulesManagementReport fileRulesManagementReport = checkCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        // Then
        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder(
            "APP-00001",
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "APP-00006",
            "HOL-00001",
            "HOL-00002",
            "HOL-00003",
            "HOL-00004",
            "HOL-00005"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenEmptyDbWhenImportRulesThenImportOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // When
        String importRequestId = importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(importRequestId);
        checkStatusCode(logbookOperation, StatusCode.OK);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.OK,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        checkCommittedRules(logbookOperation, 0, 0, 11);

        checkRuleSet1();

        checkCsvFileBackup(logbookOperation, RULES_SIMPLE_RULES_SET_1_CSV);
        checkJsonFileBackup(logbookOperation);
        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder(
            "APP-00001",
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "APP-00006",
            "HOL-00001",
            "HOL-00002",
            "HOL-00003",
            "HOL-00004",
            "HOL-00005"
        );

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndNoUsedRuleWhenCheckRulesThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        // When
        FileRulesManagementReport fileRulesManagementReport = checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndNoUsedRuleWhenImportRulesThenImportOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        // When
        String updateRulesRequestId = importCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.OK);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.OK,
            Collections.singletonList("APP-00006"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        checkCommittedRules(logbookOperation, 1, 7, 1);

        checkRuleSet2();

        checkCsvFileBackup(logbookOperation, RULES_SIMPLE_RULES_SET_2_CSV);
        checkJsonFileBackup(logbookOperation);
        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndNonUpdatedUsedRuleWhenCheckRulesThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00001_rule_reference.json",
            "rules/lfc_with_APP-00001_rule_reference.json"
        );

        // When
        FileRulesManagementReport fileRulesManagementReport = checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndNonUpdatedUsedRuleWhenImportRulesThenImportOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00001_rule_reference.json",
            "rules/lfc_with_APP-00001_rule_reference.json"
        );

        // When
        String updateRulesRequestId = importCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.OK);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.OK,
            Collections.singletonList("APP-00006"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        checkCommittedRules(logbookOperation, 1, 7, 1);

        checkRuleSet2();

        checkCsvFileBackup(logbookOperation, RULES_SIMPLE_RULES_SET_2_CSV);
        checkJsonFileBackup(logbookOperation);
        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUpdatedUsedRuleDescriptionOnlyWhenCheckRulesThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00002_rule_reference.json",
            "rules/lfc_with_APP-00002_rule_reference.json"
        );

        // When
        FileRulesManagementReport fileRulesManagementReport = checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate().get(0)).contains("APP-00002");
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUpdatedUsedRuleDescriptionOnlyWhenImportRulesThenImportOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00002_rule_reference.json",
            "rules/lfc_with_APP-00002_rule_reference.json"
        );

        // When
        String updateRulesRequestId = importCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.OK);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.OK,
            Collections.singletonList("APP-00006"),
            Collections.emptyList(),
            Collections.singletonList("APP-00002"),
            Collections.emptyList()
        );
        checkCommittedRules(logbookOperation, 1, 7, 1);

        checkRuleSet2();

        checkCsvFileBackup(logbookOperation, RULES_SIMPLE_RULES_SET_2_CSV);
        checkJsonFileBackup(logbookOperation);
        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate().get(0)).contains("APP-00002");
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUpdatedUsedRuleDurationWhenCheckRulesThenWarning() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        importUnitAndLfc(
            "rules/unit_with_APP-00005_rule_reference.json",
            "rules/unit_with_APP-00005_rule_reference.json"
        );

        // When
        FileRulesManagementReport fileRulesManagementReport = checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate().get(0)).contains("APP-00005");
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUpdatedUsedRuleDurationWhenImportRulesThenImportWarning() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);

        importUnitAndLfc(
            "rules/unit_with_APP-00005_rule_reference.json",
            "rules/unit_with_APP-00005_rule_reference.json"
        );

        // When
        String updateRulesRequestId = importCsvFile(RULES_SIMPLE_RULES_SET_2_CSV);

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.WARNING);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.WARNING,
            Collections.singletonList("APP-00006"),
            Collections.emptyList(),
            Collections.singletonList("APP-00005"),
            Collections.emptyList()
        );
        checkCommittedRules(logbookOperation, 1, 7, 1);

        checkRuleSet2();

        checkCsvFileBackup(logbookOperation, RULES_SIMPLE_RULES_SET_2_CSV);
        checkJsonFileBackup(logbookOperation);
        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate().get(0)).contains("APP-00005");
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).containsExactlyInAnyOrder(
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "HOL-00002",
            "HOL-00003",
            "HOL-00005"
        );
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).containsExactlyInAnyOrder("APP-00006");
        assertThat(fileRulesManagementReport.getFileRulesToImport()).containsExactlyInAnyOrder("APP-00007");

        checkUnitRuleUpdateWorkflow();

        // Check unit rule end date update
        JsonNode unit = selectUnitById("aeaqaaaaaahb5utaaagnyalvagpbo2qaaaaq");
        RuleModel appraisalRule = JsonHandler.getFromJsonNode(
            unit.get(VitamFieldsHelper.management()).get("AppraisalRule").get("Rules").get(0),
            RuleModel.class
        );
        assertThat(appraisalRule.getRule()).isEqualTo("APP-00005");
        assertThat(appraisalRule.getStartDate()).isEqualTo("2016-01-01");
        assertThat(appraisalRule.getEndDate()).isEqualTo("2016-02-01");
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleToDeleteWhenCheckRulesThenKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00006_rule_reference.json",
            "rules/lfc_with_APP-00006_rule_reference.json"
        );

        // When / Then
        assertThatThrownBy(() -> checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV)).isInstanceOf(
            AdminManagementClientBadRequestException.class
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleToDeleteWhenImportRulesThenImportKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_APP-00006_rule_reference.json",
            "rules/lfc_with_APP-00006_rule_reference.json"
        );

        // When / Then
        String updateRulesRequestId = newOperationLogbookGUID(TENANT_0).getId();
        VitamThreadUtils.getVitamSession().setRequestId(updateRulesRequestId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    client.importRulesFile(
                        PropertiesUtils.getResourceAsStream(RULES_SIMPLE_RULES_SET_2_CSV),
                        "file.csv"
                    )
            ).isInstanceOf(FileRulesException.class);
        }

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.KO);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.KO,
            Collections.emptyList(),
            Collections.singletonList("APP-00006"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        checkNoCommittedRules(logbookOperation);
        checkNoCsvFileBackup(logbookOperation);
        checkNoJsonFileBackup(logbookOperation);

        checkRuleSet1();

        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete().get(0)).contains("APP-00006");
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToImport()).isNullOrEmpty();

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleWithoutDurationToBeUpdatedWithDurationWhenCheckRulesThenKO()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_HOL-00003_rule_reference.json",
            "rules/lfc_with_HOL-00003_rule_reference.json"
        );

        // When / Then
        assertThatThrownBy(() -> checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV)).isInstanceOf(
            AdminManagementClientBadRequestException.class
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleWithoutDurationToBeUpdatedWithDurationWhenImportRulesThenImportKO()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_HOL-00003_rule_reference.json",
            "rules/lfc_with_HOL-00003_rule_reference.json"
        );

        // When / Then
        String updateRulesRequestId = newOperationLogbookGUID(TENANT_0).getId();
        VitamThreadUtils.getVitamSession().setRequestId(updateRulesRequestId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    client.importRulesFile(
                        PropertiesUtils.getResourceAsStream(RULES_SIMPLE_RULES_SET_2_CSV),
                        "file.csv"
                    )
            ).isInstanceOf(FileRulesException.class);
        }

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.KO);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.KO,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("HOL-00003")
        );

        checkNoCommittedRules(logbookOperation);
        checkNoCsvFileBackup(logbookOperation);
        checkNoJsonFileBackup(logbookOperation);

        checkRuleSet1();

        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate().get(0)).contains("HOL-00003");
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToImport()).isNullOrEmpty();

        checkNoUnitRuleUpdateWorkflow();
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleWithDurationToBeUpdatedWithoutDurationWhenCheckRulesThenKO()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_HOL-00005_rule_reference.json",
            "rules/lfc_with_HOL-00005_rule_reference.json"
        );

        // When / Then
        assertThatThrownBy(() -> checkCsvFile(RULES_SIMPLE_RULES_SET_2_CSV)).isInstanceOf(
            AdminManagementClientBadRequestException.class
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenExitingRulesAndUsedRuleWithDurationToBeUpdatedWithoutDurationWhenImportRulesThenImportKO()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        importCsvFile(RULES_SIMPLE_RULES_SET_1_CSV);
        importUnitAndLfc(
            "rules/unit_with_HOL-00005_rule_reference.json",
            "rules/lfc_with_HOL-00005_rule_reference.json"
        );

        // When / Then
        String updateRulesRequestId = newOperationLogbookGUID(TENANT_0).getId();
        VitamThreadUtils.getVitamSession().setRequestId(updateRulesRequestId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    client.importRulesFile(
                        PropertiesUtils.getResourceAsStream(RULES_SIMPLE_RULES_SET_2_CSV),
                        "file.csv"
                    )
            ).isInstanceOf(FileRulesException.class);
        }

        // Then
        LogbookOperation logbookOperation = selectLogbookOperation(updateRulesRequestId);
        checkStatusCode(logbookOperation, StatusCode.KO);
        checkCheckRulesStep(
            logbookOperation,
            StatusCode.KO,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("HOL-00005")
        );

        checkNoCommittedRules(logbookOperation);
        checkNoCsvFileBackup(logbookOperation);
        checkNoJsonFileBackup(logbookOperation);

        checkRuleSet1();

        FileRulesManagementReport fileRulesManagementReport = checkReportGeneration(logbookOperation);

        assertThat(fileRulesManagementReport.getErrors()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate()).hasSize(1);
        assertThat(fileRulesManagementReport.getUsedRulesWithDurationModeUpdate().get(0)).contains("HOL-00005");
        assertThat(fileRulesManagementReport.getFileRulesToUpdate()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToDelete()).isNullOrEmpty();
        assertThat(fileRulesManagementReport.getFileRulesToImport()).isNullOrEmpty();

        checkNoUnitRuleUpdateWorkflow();
    }

    private void importUnitAndLfc(String unitResourceFile, String lfcResourceFile)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException, MetaDataDocumentSizeException, MetaDataClientServerException, FileNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            metaDataClient.insertUnitBulk(
                new BulkUnitInsertRequest(
                    Collections.singletonList(
                        new BulkUnitInsertEntry(
                            Collections.emptySet(),
                            JsonHandler.getFromInputStream(PropertiesUtils.getConfigAsStream(unitResourceFile))
                        )
                    )
                )
            );
        }

        try (LogbookLifeCyclesClient logbookLfcClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            logbookLfcClient.createRawbulkUnitlifecycles(
                Collections.singletonList(
                    JsonHandler.getFromInputStream(PropertiesUtils.getConfigAsStream(lfcResourceFile))
                )
            );
        }
    }

    private void checkRuleSet1()
        throws FileRulesException, InvalidParseOperationException, IOException, AdminManagementClientServerException {
        Map<String, FileRulesModel> fileRulesModelMap = loadFilesFromDb();

        assertThat(fileRulesModelMap).hasSize(11);
        assertThat(fileRulesModelMap.keySet()).containsExactlyInAnyOrder(
            "APP-00001",
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "APP-00006",
            "HOL-00001",
            "HOL-00002",
            "HOL-00003",
            "HOL-00004",
            "HOL-00005"
        );
        assertThat(fileRulesModelMap.get("APP-00001").getRuleType().name()).isEqualTo("AppraisalRule");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleValue()).isEqualTo("RuleValue1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleDescription()).isEqualTo("Rule description 1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("APP-00002").getRuleDescription()).isEqualTo("Rule description 2");

        assertThat(fileRulesModelMap.get("APP-00003").getRuleDescription()).isNull();

        assertThat(fileRulesModelMap.get("APP-00004").getRuleValue()).isEqualTo("RuleValue4");

        assertThat(fileRulesModelMap.get("APP-00005").getRuleDuration()).isEqualTo("0");
        assertThat(fileRulesModelMap.get("APP-00005").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("APP-00006")).isNotNull();

        assertThat(fileRulesModelMap.get("APP-00007")).isNull();

        assertThat(fileRulesModelMap.get("HOL-00001").getRuleType().name()).isEqualTo("HoldRule");
        assertThat(fileRulesModelMap.get("HOL-00001").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("HOL-00001").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("HOL-00002").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("HOL-00002").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("HOL-00003").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("HOL-00003").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("HOL-00004").getRuleDuration()).isNull();
        assertThat(fileRulesModelMap.get("HOL-00004").getRuleMeasurement()).isNull();

        assertThat(fileRulesModelMap.get("HOL-00005").getRuleDuration()).isNull();
        assertThat(fileRulesModelMap.get("HOL-00005").getRuleMeasurement()).isNull();
    }

    private void checkRuleSet2()
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException, IOException {
        Map<String, FileRulesModel> fileRulesModelMap = loadFilesFromDb();
        assertThat(fileRulesModelMap).hasSize(11);
        assertThat(fileRulesModelMap.keySet()).containsExactlyInAnyOrder(
            "APP-00001",
            "APP-00002",
            "APP-00003",
            "APP-00004",
            "APP-00005",
            "APP-00007",
            "HOL-00001",
            "HOL-00002",
            "HOL-00003",
            "HOL-00004",
            "HOL-00005"
        );
        // Unchanged APP-000001
        assertThat(fileRulesModelMap.get("APP-00001").getRuleType().name()).isEqualTo("AppraisalRule");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleValue()).isEqualTo("RuleValue1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleDescription()).isEqualTo("Rule description 1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("APP-00001").getRuleMeasurement().name()).isEqualTo("YEAR");
        // Description update
        assertThat(fileRulesModelMap.get("APP-00002").getRuleDescription()).isEqualTo("Rule description 2 UPDATED");
        assertThat(fileRulesModelMap.get("APP-00003").getRuleDescription()).isEqualTo("Rule description 3 UPDATED");
        // Rule value update
        assertThat(fileRulesModelMap.get("APP-00004").getRuleValue()).isEqualTo("RuleValue4 UPDATED");
        // Rule duration update
        assertThat(fileRulesModelMap.get("APP-00005").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("APP-00005").getRuleMeasurement().name()).isEqualTo("MONTH");
        // Rule delete
        assertThat(fileRulesModelMap.get("APP-00006")).isNull();
        // Rule insert
        assertThat(fileRulesModelMap.get("APP-00007").getRuleType().name()).isEqualTo("AppraisalRule");
        assertThat(fileRulesModelMap.get("APP-00007").getRuleValue()).isEqualTo("RuleValue7");
        assertThat(fileRulesModelMap.get("APP-00007").getRuleDescription()).isEqualTo("Rule description 7 INSERTED");
        assertThat(fileRulesModelMap.get("APP-00007").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("APP-00007").getRuleMeasurement().name()).isEqualTo("YEAR");

        // Hold Rule duration changes
        assertThat(fileRulesModelMap.get("HOL-00001").getRuleDuration()).isEqualTo("1");
        assertThat(fileRulesModelMap.get("HOL-00001").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("HOL-00002").getRuleDuration()).isEqualTo("2");
        assertThat(fileRulesModelMap.get("HOL-00002").getRuleMeasurement().name()).isEqualTo("YEAR");

        assertThat(fileRulesModelMap.get("HOL-00003").getRuleDuration()).isNull();
        assertThat(fileRulesModelMap.get("HOL-00003").getRuleMeasurement()).isNull();

        assertThat(fileRulesModelMap.get("HOL-00004").getRuleDuration()).isNull();
        assertThat(fileRulesModelMap.get("HOL-00004").getRuleMeasurement()).isNull();

        assertThat(fileRulesModelMap.get("HOL-00005").getRuleDuration()).isEqualTo("5");
        assertThat(fileRulesModelMap.get("HOL-00005").getRuleMeasurement().name()).isEqualTo("MONTH");
    }

    private String importCsvFile(String csvFileResource)
        throws ReferentialException, DatabaseConflictException, FileNotFoundException {
        String importRequestId = newOperationLogbookGUID(TENANT_0).getId();
        VitamThreadUtils.getVitamSession().setRequestId(importRequestId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importRulesFile(PropertiesUtils.getResourceAsStream(csvFileResource), "file.csv");
        }
        return importRequestId;
    }

    private FileRulesManagementReport checkCsvFile(String csvFileResource)
        throws ReferentialException, FileNotFoundException, InvalidParseOperationException {
        String checkRulesRequestId = newOperationLogbookGUID(TENANT_0).getId();
        VitamThreadUtils.getVitamSession().setRequestId(checkRulesRequestId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            InputStream inputStream = client
                .checkRulesFile(PropertiesUtils.getResourceAsStream(csvFileResource))
                .readEntity(InputStream.class);
            return JsonHandler.getFromInputStream(inputStream, FileRulesManagementReport.class);
        }
    }

    private Map<String, FileRulesModel> loadFilesFromDb()
        throws FileRulesException, InvalidParseOperationException, IOException, AdminManagementClientServerException {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            JsonNode rules = client.getRules(new Select().getFinalSelect());
            List<FileRulesModel> results = RequestResponseOK.getFromJsonNode(rules, FileRulesModel.class).getResults();
            return results.stream().collect(Collectors.toMap(FileRulesModel::getRuleId, r -> r));
        }
    }

    private LogbookOperation selectLogbookOperation(String importRequestId)
        throws LogbookClientException, InvalidParseOperationException {
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode result = client.selectOperationById(importRequestId);
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(
                result
            );
            return JsonHandler.getFromJsonNode(
                logbookOperationVersionModelResponseOK.getFirstResult(),
                LogbookOperation.class
            );
        }
    }

    private void checkStatusCode(LogbookOperation logbookOperation, StatusCode statusCode) {
        List<LogbookEventOperation> events = logbookOperation.getEvents();
        LogbookEventOperation lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.getOutcome()).isEqualTo(statusCode.name());
    }

    private void checkJsonFileBackup(LogbookOperation logbookOperation)
        throws StorageNotFoundClientException, StorageServerClientException, InvalidParseOperationException {
        LogbookEventOperation csvBackupEvent = findEventByEventType(logbookOperation, "STP_IMPORT_RULES_BACKUP");
        assertThat(csvBackupEvent.getOutDetail()).isEqualTo("STP_IMPORT_RULES_BACKUP.OK");
        JsonNode evDetData = JsonHandler.getFromString(csvBackupEvent.getEvDetData());

        String fileName = evDetData.get("FileName").asText();
        String digest = evDetData.get("Digest").asText();

        checkFileBackup(fileName, DataCategory.BACKUP, digest);
    }

    private void checkNoJsonFileBackup(LogbookOperation logbookOperation) {
        checkNoEventByEventType(logbookOperation, "STP_IMPORT_RULES_BACKUP");
    }

    private void checkCsvFileBackup(LogbookOperation logbookOperation, String csvFileResource)
        throws StorageNotFoundClientException, StorageServerClientException, InvalidParseOperationException, IOException {
        LogbookEventOperation csvBackupEvent = findEventByEventType(logbookOperation, "STP_IMPORT_RULES_BACKUP_CSV");
        assertThat(csvBackupEvent.getOutDetail()).isEqualTo("STP_IMPORT_RULES_BACKUP_CSV.OK");
        JsonNode evDetData = JsonHandler.getFromString(csvBackupEvent.getEvDetData());

        String fileName = evDetData.get("FileName").asText();
        String digest = evDetData.get("Digest").asText();

        String expectedDigest = new Digest(VitamConfiguration.getDefaultDigestType())
            .update(PropertiesUtils.getResourceAsStream(csvFileResource))
            .digestHex();
        assertThat(digest).isEqualTo(expectedDigest);

        checkFileBackup(fileName, DataCategory.RULES, expectedDigest);
    }

    private void checkNoCsvFileBackup(LogbookOperation logbookOperation) {
        checkNoEventByEventType(logbookOperation, "STP_IMPORT_RULES_BACKUP_CSV");
    }

    private FileRulesManagementReport checkReportGeneration(LogbookOperation logbookOperation)
        throws StorageNotFoundClientException, StorageServerClientException, InvalidParseOperationException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException {
        LogbookEventOperation ruleStorageEvent = findEventByEventType(logbookOperation, "RULES_REPORT");
        assertThat(ruleStorageEvent.getOutDetail()).isEqualTo("RULES_REPORT.OK");
        JsonNode evDetData = JsonHandler.getFromString(ruleStorageEvent.getEvDetData());

        String fileName = evDetData.get("FileName").asText();
        String digest = evDetData.get("Digest").asText();

        checkFileBackup(fileName, DataCategory.REPORT, digest);

        return readReportFile(fileName);
    }

    private void checkFileBackup(String fileName, DataCategory dataCategory, String expectedDigest)
        throws StorageNotFoundClientException, StorageServerClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            List<String> offerIds = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
            JsonNode information = storageClient.getInformation(
                VitamConfiguration.getDefaultStrategy(),
                dataCategory,
                fileName,
                offerIds,
                true
            );
            for (String offerId : offerIds) {
                assertThat(information.get(offerId).get("digest").asText()).isEqualTo(expectedDigest);
            }
        }
    }

    private FileRulesManagementReport readReportFile(String fileName)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            InputStream inputStream = storageClient
                .getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    fileName,
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                )
                .readEntity(InputStream.class);

            FileRulesManagementReport report = JsonHandler.getFromInputStream(
                inputStream,
                FileRulesManagementReport.class
            );
            return report;
        }
    }

    private void checkCommittedRules(LogbookOperation logbookOperation, int nbDeleted, int nbUpdated, int nbInserted)
        throws InvalidParseOperationException {
        LogbookEventOperation commitRulesEvent = findEventByEventType(logbookOperation, "COMMIT_RULES");

        assertThat(commitRulesEvent.getOutDetail()).isEqualTo("COMMIT_RULES.OK");
        JsonNode evDetData = JsonHandler.getFromString(commitRulesEvent.getEvDetData());

        assertThat(evDetData.get("nbDeleted").asInt()).isEqualTo(nbDeleted);
        assertThat(evDetData.get("nbUpdated").asInt()).isEqualTo(nbUpdated);
        assertThat(evDetData.get("nbInserted").asInt()).isEqualTo(nbInserted);
    }

    private void checkNoCommittedRules(LogbookOperation logbookOperation) {
        checkNoEventByEventType(logbookOperation, "COMMIT_RULES");
    }

    private void checkCheckRulesStep(
        LogbookOperation logbookOperation,
        StatusCode statusCode,
        List<String> deletedRuleIds,
        List<String> usedDeletedRuleIds,
        List<String> usedUpdatedRuleIds,
        List<String> usedRuleIdsWithDurationModeUpdate
    ) throws InvalidParseOperationException {
        LogbookEventOperation csvBackupEvent = findEventByEventType(logbookOperation, "CHECK_RULES");
        assertThat(csvBackupEvent.getOutDetail()).isEqualTo("CHECK_RULES." + statusCode.name());
        JsonNode evDetData = JsonHandler.getFromString(csvBackupEvent.getEvDetData());

        if (CollectionUtils.isEmpty(deletedRuleIds)) {
            assertThat(evDetData.get("deletedRuleIds")).isNull();
        } else {
            assertThat(evDetData.get("deletedRuleIds")).isNotNull();
            assertThat(
                JsonHandler.getFromJsonNode(evDetData.get("deletedRuleIds"), STRING_LIST_TYPE_REFERENCE)
            ).containsExactlyElementsOf(deletedRuleIds);
        }

        if (CollectionUtils.isEmpty(usedDeletedRuleIds)) {
            assertThat(evDetData.get("usedDeletedRuleIds")).isNull();
        } else {
            assertThat(evDetData.get("usedDeletedRuleIds")).isNotNull();
            assertThat(
                JsonHandler.getFromJsonNode(evDetData.get("usedDeletedRuleIds"), STRING_LIST_TYPE_REFERENCE)
            ).containsExactlyElementsOf(usedDeletedRuleIds);
        }

        if (CollectionUtils.isEmpty(usedUpdatedRuleIds)) {
            assertThat(evDetData.get("usedUpdatedRuleIds")).isNull();
        } else {
            assertThat(evDetData.get("usedUpdatedRuleIds")).isNotNull();
            assertThat(
                JsonHandler.getFromJsonNode(evDetData.get("usedUpdatedRuleIds"), STRING_LIST_TYPE_REFERENCE)
            ).containsExactlyElementsOf(usedUpdatedRuleIds);
        }

        if (CollectionUtils.isEmpty(usedRuleIdsWithDurationModeUpdate)) {
            assertThat(evDetData.get("usedRuleIdsWithDurationModeUpdate")).isNull();
        } else {
            assertThat(evDetData.get("usedRuleIdsWithDurationModeUpdate")).isNotNull();
            assertThat(
                JsonHandler.getFromJsonNode(
                    evDetData.get("usedRuleIdsWithDurationModeUpdate"),
                    STRING_LIST_TYPE_REFERENCE
                )
            ).containsExactlyElementsOf(usedRuleIdsWithDurationModeUpdate);
        }
    }

    private LogbookEventOperation findEventByEventType(LogbookOperation logbookOperation, String eventType) {
        return logbookOperation
            .getEvents()
            .stream()
            .filter(e -> e.getEvType().equals(eventType))
            .findFirst()
            .orElseThrow();
    }

    private void checkNoEventByEventType(LogbookOperation logbookOperation, String eventType) {
        assertThat(logbookOperation.getEvents().stream().noneMatch(e -> e.getEvType().equals(eventType))).isTrue();
    }

    private void checkNoUnitRuleUpdateWorkflow()
        throws LogbookClientException, InvalidParseOperationException, InvalidCreateOperationException {
        Optional<String> operationId = findUnitRuleUpdateWorkflowOperationId();
        assertThat(operationId).isEmpty();
    }

    private void checkUnitRuleUpdateWorkflow()
        throws LogbookClientException, InvalidParseOperationException, InvalidCreateOperationException {
        Optional<String> operationId = findUnitRuleUpdateWorkflowOperationId();
        assertThat(operationId).isPresent();

        waitOperation(operationId.get());

        LogbookOperation logbookOperation = selectLogbookOperation(operationId.get());
        checkStatusCode(logbookOperation, StatusCode.OK);
    }

    private Optional<String> findUnitRuleUpdateWorkflowOperationId()
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        Select select = new Select();
        select.setQuery(
            QueryHelper.eq(LogbookMongoDbName.eventType.getDbname(), Contexts.UPDATE_RULES_ARCHIVE_UNITS.name())
        );

        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode result = client.selectOperation(select.getFinalSelect());
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(
                result
            );
            if (logbookOperationVersionModelResponseOK.getResults().isEmpty()) {
                return Optional.empty();
            }
            assertThat(logbookOperationVersionModelResponseOK.getResults()).hasSize(1);
            return Optional.of(
                logbookOperationVersionModelResponseOK
                    .getResults()
                    .get(0)
                    .get(LogbookMongoDbName.eventIdentifier.getDbname())
                    .asText()
            );
        } catch (LogbookClientNotFoundException ig) {
            return Optional.empty();
        }
    }

    private JsonNode selectUnitById(String unitId)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException {
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            JsonNode result = client.selectUnitbyId(select.getFinalSelectById(), unitId);
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(
                result
            );
            return logbookOperationVersionModelResponseOK.getFirstResult();
        }
    }
}
