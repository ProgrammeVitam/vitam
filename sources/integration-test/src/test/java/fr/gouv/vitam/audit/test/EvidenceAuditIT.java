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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.evidence.DataRectificationCheckResourceAvailability;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Audit evidence integration test
 */

public class EvidenceAuditIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "Context_IT";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        EvidenceAuditIT.class,
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
            IngestInternalMain.class
        )
    );

    private static DataLoader dataLoader = new DataLoader("integration-ingest-internal");

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
    public void should_execute_evidence_audit_workflow_without_error() throws Exception {
        // Given
        String ingestOperationId = VitamTestHelper.doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        doTraceabilityUnits();
        doTraceabilityGots();

        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery query = new SelectMultiQuery();
            query.setQuery(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            String evidenceAuditOperation = runEvidenceAudit(query.getFinalSelect());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(evidenceAuditOperation)
                .toJsonNode()
                .get("$results")
                .get(0)
                .get("events");
            // Then
            assertThat(jsonNode.iterator())
                .extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report exists
            List<JsonNode> reportLines = VitamTestHelper.getReports(evidenceAuditOperation);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbArchiveUnits").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(4);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_evidence_audit_workflow_with_ko_report() throws Exception {
        // Given
        String ingestOperationId = VitamTestHelper.doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        doTraceabilityUnits();
        doTraceabilityGots();
        String deletedGotId = deleteObjectInStorage(ingestOperationId);
        String gotIdOfUnitModified = changeMetadataUnitInMongo(ingestOperationId, deletedGotId);
        String gotIdModified = changeMetadataGotInMongo(ingestOperationId, deletedGotId, gotIdOfUnitModified);
        assertNotEquals(deletedGotId, gotIdModified);
        assertNotEquals(gotIdOfUnitModified, gotIdModified);

        try (
            AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()
        ) {
            // EVIDENCE AUDIT

            SelectMultiQuery query = new SelectMultiQuery();
            query.setQuery(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));
            String evidenceAuditOperation = runEvidenceAudit(query.getFinalSelect());

            // When
            ArrayNode evidenceAuditOperationEvents = (ArrayNode) accessClient
                .selectOperationById(evidenceAuditOperation)
                .toJsonNode()
                .get("$results")
                .get(0)
                .get("events");
            // Then
            assertThat(evidenceAuditOperationEvents.iterator())
                .extracting(j -> j.get("outcome").asText())
                .anyMatch(outcome -> outcome.equals(StatusCode.KO.name()));

            // Check report exists
            List<JsonNode> reportLines = VitamTestHelper.getReports(evidenceAuditOperation);

            assertThat(reportLines.size()).isEqualTo(6);
            assertThat(reportLines.get(3).get("strategyId").asText()).isEqualTo(
                VitamConfiguration.getDefaultStrategy()
            );
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(3).get("strategyId").asText()).isEqualTo(
                VitamConfiguration.getDefaultStrategy()
            );
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(11);
            assertThat(reportLines.get(3).get("strategyId").asText()).isEqualTo(
                VitamConfiguration.getDefaultStrategy()
            );
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbArchiveUnits").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(4);

            // CORRECTIVE WORKFLOW

            // given
            GUID rectificationAuditOperationGUID = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(rectificationAuditOperationGUID);

            adminClient.rectificationAudit(evidenceAuditOperation);
            waitOperation(rectificationAuditOperationGUID.getId());

            // When
            ArrayNode rectificationAuditOperationEvents = (ArrayNode) accessClient
                .selectOperationById(rectificationAuditOperationGUID.getId())
                .toJsonNode()
                .get("$results")
                .get(0)
                .get("events");
            // Then
            assertThat(rectificationAuditOperationEvents.iterator())
                .extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));
            assertThat(rectificationAuditOperationEvents.iterator())
                .extracting(j -> j.get("outDetail").asText())
                .contains(DataRectificationCheckResourceAvailability.PLUGIN_NAME + ".OK");

            // Check report exists
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(
                        VitamConfiguration.getDefaultStrategy(),
                        rectificationAuditOperationGUID.toString() + ".json",
                        DataCategory.REPORT,
                        AccessLogUtils.getNoLogAccessLog()
                    );
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_evidence_audit_workflow_with_warning() throws Exception {
        String ingestOperationId2 = VitamTestHelper.doIngest(TENANT_ID, "evidence/3_UNITS_2_GOTS.zip");

        // Given
        JsonNode evidenceQuery = constructQuery(ingestOperationId2);
        String operationId = runEvidenceAudit(evidenceQuery);

        // Check report
        List<JsonNode> reportLines = VitamTestHelper.getReports(operationId);
        assertThat(reportLines.size()).isEqualTo(8);
        assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(5);
        assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(2);
        assertThat(reportLines.get(1).get("extendedInfo").get("nbArchiveUnits").asInt()).isEqualTo(3);
        assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(0);
        assertThat(reportLines.get(3).get("message").asText()).contains(
            "No traceability operation found matching date"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_evidence_audit_workflow_with_fatal_error() throws Exception {
        String ingestOperationId2 = VitamTestHelper.doIngest(TENANT_ID, "evidence/3_UNITS_2_GOTS.zip");
        setFakeStrategyInMetadatasUnit();

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            JsonNode evidenceQuery = constructQuery(ingestOperationId2);
            // When
            String operationId = runEvidenceAudit(evidenceQuery);
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationId)
                .toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            //Then
            assertThat(jsonNode.iterator())
                .extracting(j -> j.get("outcome").asText())
                .anyMatch(outcome -> outcome.equals(StatusCode.FATAL.name()));
        }
    }

    private String runEvidenceAudit(JsonNode query) throws AdminManagementClientServerException {
        GUID evidenceAuditOperationGUID = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(evidenceAuditOperationGUID);
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> evidenceAuditResponse = adminClient.evidenceAudit(query);
            waitOperation(evidenceAuditOperationGUID.toString());
        }
        return evidenceAuditOperationGUID.toString();
    }

    private String changeMetadataUnitInMongo(String ingestOperationId, String... deletedGotId) throws Exception {
        try (final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(
                QueryHelper.and()
                    .add(QueryHelper.in(VitamFieldsHelper.initialOperation(), ingestOperationId))
                    .add(QueryHelper.not().add(QueryHelper.in(VitamFieldsHelper.object(), deletedGotId)))
                    .add(QueryHelper.not().add(QueryHelper.exists(VitamFieldsHelper.object())))
            );
            // Get AU and update it
            final JsonNode unitResult = metaDataClient.selectUnits(select.getFinalSelect());
            if (unitResult == null || unitResult.get("$results").size() <= 0) {
                throw new VitamException("Could not find unit");
            }
            JsonNode unit = unitResult.get("$results").get(0);
            assertThat(unit).isNotNull();
            final String unitId = unit.get("#id").asText();
            String gotId = "EMPTY";
            if (unit.has("#object")) {
                gotId = unit.get("#object").asText();
            }
            Bson filterUnit = Filters.eq("_id", unitId);
            Bson updateUnit = Updates.set("Title", "title");
            UpdateResult updateUnitResult = MetadataCollections.UNIT.getCollection().updateOne(filterUnit, updateUnit);
            assertEquals(updateUnitResult.getModifiedCount(), 1);
            return gotId;
        }
    }

    private String changeMetadataGotInMongo(String initialOperationId, String... gotIdExcluded) throws Exception {
        try (final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            GUID accessGuid = GUIDFactory.newRequestIdGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(accessGuid);

            SelectMultiQuery selectQuery = new SelectMultiQuery();
            selectQuery.setQuery(
                QueryHelper.and()
                    .add(QueryHelper.in(VitamFieldsHelper.initialOperation(), initialOperationId))
                    .add(QueryHelper.not().add(QueryHelper.in(VitamFieldsHelper.id(), gotIdExcluded)))
                    .add(QueryHelper.gte(VitamFieldsHelper.nbobjects(), 1))
            );
            selectQuery.setLimitFilter(0, 1);
            JsonNode gotResult = metaDataClient.selectObjectGroups(selectQuery.getFinalSelect());
            JsonNode got = gotResult.get("$results").get(0);
            assertThat(got).isNotNull();
            final String gotId = got.get("#id").asText();
            Bson filterGot = Filters.eq("_id", gotId);
            Bson updateGot = Updates.combine(
                Updates.set("FileInfo.FileName", "test_audit_wrong.pdf"),
                Updates.set("FileInfo.Filename", "test_audit_wrong.pdf"),
                Updates.set("_qualifiers.0.versions.0.FileInfo.Filename", "test_audit_wrong.pdf")
            );
            UpdateResult updateGotResult = MetadataCollections.OBJECTGROUP.getCollection()
                .updateOne(filterGot, updateGot);
            assertEquals(updateGotResult.getModifiedCount(), 1);
            FindIterable<Document> gots = MetadataCollections.OBJECTGROUP.getCollection().find(filterGot);
            JsonNode gotModified = JsonHandler.getFromString(gots.first().toJson());
            String fileNameGlobal = gotModified.get("FileInfo").get("FileName").asText();
            String fileNameVersion = gotModified
                .get("_qualifiers")
                .get(0)
                .get("versions")
                .get(0)
                .get("FileInfo")
                .get("Filename")
                .asText();
            assertEquals("test_audit_wrong.pdf", fileNameGlobal);
            assertEquals("test_audit_wrong.pdf", fileNameVersion);
            return gotId;
        }
    }

    private void setFakeStrategyInMetadatasUnit() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.match("Title", "dossier2"));
        // Get AU and update it
        final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode unitResult = metaDataClient.selectUnits(select.getFinalSelect());
        if (unitResult == null || unitResult.get("$results").size() <= 0) {
            throw new VitamException("Could not find unit");
        }
        JsonNode unit = unitResult.get("$results").get(0);
        final String unitId = unit.get("#id").asText();
        assertThat(unit).isNotNull();
        Bson filter = Filters.eq("_id", unitId);
        Bson update = Updates.set("_storage.strategyId", "fake-strategy");
        UpdateResult updateResult = MetadataCollections.UNIT.getCollection().updateOne(filter, update);
        assertEquals(updateResult.getModifiedCount(), 1);
    }

    private JsonNode constructQuery(String operationGuid) throws InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addRoots(JsonHandler.createArrayNode());
        select.setQuery(and().add(QueryHelper.in(BuilderToken.PROJECTIONARGS.OPERATIONS.exactToken(), operationGuid)));
        select.addProjection(JsonHandler.createObjectNode());
        return select.getFinalSelect();
    }

    private String doTraceabilityGots() throws VitamException {
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK traceabilityObjectGroupResponse = logbookOperationsClient.traceabilityLfcObjectGroup();
            String traceabilityGotOperationId = traceabilityObjectGroupResponse.getHeaderString(
                GlobalDataRest.X_REQUEST_ID
            );
            waitOperation(traceabilityGotOperationId);
            return traceabilityGotOperationId;
        }
    }

    private String doTraceabilityUnits() throws VitamException {
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK traceabilityUnitResponse = logbookOperationsClient.traceabilityLfcUnit();
            String traceabilityUnitOperationId = traceabilityUnitResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(traceabilityUnitOperationId);
            return traceabilityUnitOperationId;
        }
    }

    private String deleteObjectInStorage(String initialOperationId) throws Exception {
        try (
            AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            StorageClient storageClient = StorageClientFactory.getInstance().getClient()
        ) {
            GUID accessGuid = GUIDFactory.newRequestIdGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(accessGuid);
            SelectMultiQuery selectQuery = new SelectMultiQuery();

            selectQuery.setQuery(
                QueryHelper.and()
                    .add(QueryHelper.in(VitamFieldsHelper.initialOperation(), initialOperationId))
                    .add(QueryHelper.gte(VitamFieldsHelper.nbobjects(), 1))
            );
            selectQuery.setLimitFilter(0, 1);
            RequestResponse<JsonNode> gots = accessClient.selectObjects(selectQuery.getFinalSelect());
            if (!gots.isOk()) {
                throw new VitamException("Could not find got with at least one object for operation");
            }
            JsonNode got = ((RequestResponseOK<JsonNode>) gots).getFirstResult();
            ObjectGroupResponse gotPojo = JsonHandler.getFromJsonNode(got, ObjectGroupResponse.class);
            String objectId = gotPojo.getQualifiers().get(0).getVersions().get(0).getId();

            GUID storageGuid = GUIDFactory.newRequestIdGUID(TENANT_ID);
            VitamThreadUtils.getVitamSession().setRequestId(storageGuid);
            storageClient.delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, objectId);
            return gotPojo.getId();
        }
    }
}
