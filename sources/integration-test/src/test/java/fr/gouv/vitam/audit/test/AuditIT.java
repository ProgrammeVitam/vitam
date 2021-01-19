/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.audit.AuditExistenceService;
import fr.gouv.vitam.worker.core.plugin.audit.AuditIntegrityService;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Audit existence/integrity integration test
 */

public class AuditIT extends VitamRuleRunner {
    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "Context_IT";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(AuditIT.class, mongoRule.getMongoDatabase().getName(),
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
                IngestInternalMain.class
            ));

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String configurationPath = PropertiesUtils
            .getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        new DataLoader("integration-ingest-internal").prepareData();
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

    @Test
    @RunWithCustomExecutor
    public void should_execute_audit_workflow_existence_without_error() throws Exception {

        String ingestOperationId1 = VitamTestHelper.doIngest(TENANT_ID, "elimination/TEST_ELIMINATION_V2.zip");
        String ingestOperationId2 = VitamTestHelper.doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            AuditOptions options = new AuditOptions();
            options.setAuditActions(AuditExistenceService.CHECK_EXISTENCE_ID);
            options.setAuditType("tenant");
            options.setObjectId(String.valueOf(TENANT_ID));

            String operationId = runAudit(options);

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationId, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results").get(0).get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report
            List<JsonNode> reportLines = VitamTestHelper.getReports(operationId);
            assertThat(reportLines.size()).isEqualTo(3);
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(8);
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").isArray()).isTrue();
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").toString()).contains(ingestOperationId1);
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").toString()).contains(ingestOperationId2);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("OK").asInt())
                .isEqualTo(7);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("WARNING")
                    .asInt()).isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("KO").asInt())
                .isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("OK").asInt())
                .isEqualTo(8);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("WARNING").asInt())
                .isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("KO").asInt())
                .isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP")
                .get("objectGroupsCount").get("OK").asInt()).isEqualTo(3);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP")
                .get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP")
                .get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount")
                    .get("OK").asInt()).isEqualTo(4);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount")
                    .get("WARNING").asInt()).isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount")
                    .get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(2).get("auditActions").asText()).isEqualTo("AUDIT_FILE_EXISTING");
            assertThat(reportLines.get(2).get("auditType").asText()).isEqualTo("tenant");
            assertThat(reportLines.get(2).get("objectId").asText()).isEqualTo("0");

        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_audit_workflow_integrity_without_error() throws Exception {

        VitamTestHelper.doIngest(TENANT_ID, "elimination/TEST_ELIMINATION_V2.zip");
        VitamTestHelper.doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {

            AuditOptions options = new AuditOptions();
            options.setAuditActions(AuditIntegrityService.CHECK_INTEGRITY_ID);
            options.setAuditType("originatingagency");
            options.setObjectId("FRAN_NP_009913");

            String operationId = runAudit(options);

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationId, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results").get(0).get("events");
            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report
            List<JsonNode> reportLines = VitamTestHelper.getReports(operationId);
            assertThat(reportLines.size()).isEqualTo(3);
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(4);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("OK").asInt())
                .isEqualTo(4);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("WARNING")
                    .asInt()).isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("KO").asInt())
                .isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("OK").asInt())
                .isEqualTo(4);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("WARNING").asInt())
                .isEqualTo(0);
            assertThat(
                reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("KO").asInt())
                .isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913")
                .get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(2).get("auditActions").asText()).isEqualTo("AUDIT_FILE_INTEGRITY");
            assertThat(reportLines.get(2).get("auditType").asText()).isEqualTo("originatingagency");
            assertThat(reportLines.get(2).get("objectId").asText()).isEqualTo("FRAN_NP_009913");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_audit_workflow_existence_with_error() throws Exception {

        String ingestOperationId1 = VitamTestHelper.doIngest(TENANT_ID, "elimination/TEST_ELIMINATION_V2.zip");
        String binaryObjectId;
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.in("#operations", ingestOperationId1));
            JsonNode resp = metadataClient.selectObjectGroups(select.getFinalSelect());
            RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(resp);
            ObjectGroupResponse got =
                JsonHandler.getFromJsonNode(responseOK.getFirstResult(), ObjectGroupResponse.class);
            Optional<QualifiersModel> binaryMasters =
                got.getQualifiers().stream().filter(qualifier -> "BinaryMaster".equals(qualifier.getQualifier()))
                    .findFirst();

            binaryObjectId = binaryMasters.get().getVersions().get(0).getId();
            storageClient.delete(binaryMasters.get().getVersions().get(0).getStorage().getStrategyId(),
                DataCategory.OBJECT, binaryObjectId);
        }

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            AuditOptions options = new AuditOptions();
            options.setAuditActions(AuditExistenceService.CHECK_EXISTENCE_ID);
            options.setAuditType("tenant");
            options.setObjectId("" + TENANT_ID);

            RequestResponse<JsonNode> response = adminClient.launchAuditWorkflow(options);
            assertThat(response.isOk()).isTrue();
            waitOperation(operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results").get(0).get("events");
            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .anyMatch(outcome -> outcome.equals(StatusCode.KO.name()));

            // Check report
            List<JsonNode> reportLines = VitamTestHelper.getReports(operationGuid.toString());


            for (JsonNode report : reportLines) {

                if (report.has("vitamResults")) {
                    JsonAssert.assertJsonEquals(report.get("vitamResults"), JsonHandler.createObjectNode()
                        .put("OK", 2)
                        .put("KO", 1)
                        .put("WARNING", 0)
                        .put("total", 3)
                    );
                }
                if (report.has("objectVersions")) {
                    JsonNode objectVersions = report.get("objectVersions");
                    JsonNode offers = objectVersions.get("offerIds");
                    assertThat(objectVersions)
                        .extracting("id", "opi", "qualifier", "version", "strategyId", "offerIds")
                        .contains(
                            tuple(binaryObjectId, ingestOperationId1, "BinaryMaster", "BinaryMaster_1", "default"));

                    assertThat(offers)
                        .extracting("id", "status")
                        .contains(tuple("default", "KO"));
                }
            }
        }
    }

    private String runAudit(AuditOptions options) throws AdminManagementClientServerException {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> response = adminClient.launchAuditWorkflow(options);
            assertThat(response.isOk()).isTrue();
            waitOperation(operationGuid.toString());
        }
        return operationGuid.toString();
    }

    @After
    public void afterTest() {
        ProcessDataAccessImpl.getInstance().clearWorkflow();
        handleAfter();
    }

}
