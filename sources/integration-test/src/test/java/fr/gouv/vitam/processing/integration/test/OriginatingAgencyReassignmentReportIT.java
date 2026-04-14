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
package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.model.OriginatingAgencyReassignmentReportLine;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.deleteGotVersions.DeleteGotVersionsIT;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.reassignment.OriginatingAgencyReassignmentPreparationPlugin;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.doIngest;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.processing.integration.test.IntegrationTestUtils.launchOriginatingAgencyReassignmentOperation;
import static fr.gouv.vitam.purge.EndToEndEliminationAndTransferReplyIT.prepareVitamSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class OriginatingAgencyReassignmentReportIT extends VitamRuleRunner {

    private static final HashSet<Class<?>> SERVERS = Sets.newHashSet(
        AccessInternalMain.class,
        AdminManagementMain.class,
        ProcessManagementMain.class,
        LogbookMain.class,
        WorkspaceMain.class,
        MetadataMain.class,
        WorkerMain.class,
        IngestInternalMain.class,
        StorageMain.class,
        DefaultOfferMain.class,
        BatchReportMain.class
    );

    private static final int TENANT_ID = 0;
    private static final String CONTEXT_ID = "Context_IT";
    private static final String MONGO_NAME = mongoRule.getMongoDatabase().getName();
    private static final String ES_NAME = ElasticsearchRule.getClusterName();
    public static final String OPI = "Opi";

    String ingestOperationId;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        DeleteGotVersionsIT.class,
        MONGO_NAME,
        ES_NAME,
        SERVERS
    );

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @Before
    public void setUpBefore() throws Exception {
        getVitamSession().setRequestId(newOperationLogbookGUID(0));
        getVitamSession().setTenantId(TENANT_ID);

        prepareVitamSession();
        ingestOperationId = doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");

        FormatIdentifierFactory.getInstance()
            .changeConfigurationFile(
                PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString()
            );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        // FIXME : To be removed once temporary v91 restrictions are removed
        OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = true;

        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);
        ProcessDataAccessImpl.getInstance().clearWorkflow();

        runAfterMongo(
            Sets.newHashSet(
                FunctionalAdminCollections.GRIFFIN.getName(),
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                "ExtractedMetadata"
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.GRIFFIN.getName())
        );
    }

    @RunWithCustomExecutor
    @Test
    public void givenReassignmentsOkThen_Report_OK() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // FIXME : To be removed once temporary v91 restrictions are removed
            OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = false;
            String sourceOriginatingAgency = "FRAN_NP_009913";
            String targetOriginatingAgency = "RATP";

            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> unitsResp = accessClient.selectUnits(getGotsRequest.getFinalSelect());

            List<JsonNode> units = ((RequestResponseOK<JsonNode>) unitsResp).getResults();
            Map<String, OriginatingAgencyReassignmentReportLine> reportLinesExpectedByUnitId = new HashMap<>();
            for (JsonNode unitNode : units) {
                String unitId = unitNode.get(VitamFieldsHelper.id()).asText();
                String unitOriginatingAgency = unitNode.get(VitamFieldsHelper.originatingAgency()).asText();
                String unitOpi = unitNode.get(VitamFieldsHelper.initialOperation()).asText();
                String objectId = null;
                if (unitNode.has((VitamFieldsHelper.object()))) {
                    objectId = unitNode.get(VitamFieldsHelper.object()).asText();
                }
                OriginatingAgencyReassignmentReportLine expectedLine = new OriginatingAgencyReassignmentReportLine(
                    unitId,
                    OriginatingAgencyReassignmentReportLine.ReportElementLineType.Unit,
                    unitOpi,
                    unitOriginatingAgency,
                    targetOriginatingAgency,
                    objectId
                );
                reportLinesExpectedByUnitId.put(unitId, expectedLine);
            }

            RequestResponse<JsonNode> objectGroupsResp = accessClient.selectObjects(getGotsRequest.getFinalSelect());

            List<JsonNode> objectGroups = ((RequestResponseOK<JsonNode>) objectGroupsResp).getResults();
            Map<String, OriginatingAgencyReassignmentReportLine> reportLinesExpectedByOGId = new HashMap<>();
            for (JsonNode objectGroup : objectGroups) {
                ObjectGroupResponse gotPojo = JsonHandler.getFromJsonNode(objectGroup, ObjectGroupResponse.class);
                String objectGroupId = gotPojo.getId();
                String objectGroupOriginatingAgency = gotPojo.getOriginatingAgency();
                String objectGroupOpi = gotPojo.getOpi();
                OriginatingAgencyReassignmentReportLine expectedLine = new OriginatingAgencyReassignmentReportLine(
                    objectGroupId,
                    OriginatingAgencyReassignmentReportLine.ReportElementLineType.ObjectGroup,
                    objectGroupOpi,
                    objectGroupOriginatingAgency,
                    targetOriginatingAgency
                );
                reportLinesExpectedByOGId.put(objectGroupId, expectedLine);
            }

            // When
            SelectMultiQuery ingestSelect = new SelectMultiQuery();
            CompareQuery operationQuery = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId);
            ingestSelect.setQuery(operationQuery);
            String reassignmentOperationId = launchOriginatingAgencyReassignmentOperation(
                sourceOriginatingAgency,
                targetOriginatingAgency,
                true,
                ingestSelect.getFinalSelect(),
                Set.of(StatusCode.OK, StatusCode.WARNING)
            );

            List<JsonNode> reportsLines = Objects.requireNonNull(VitamTestHelper.getReports(reassignmentOperationId));
            Assertions.assertThat(reportsLines).isNotEmpty();

            JsonNode summaryNode = reportsLines.get(1);
            assertTrue(summaryNode.has("reportType"));
            assertEquals("REASSIGNMENT_ORIGINATING_AGENCIES", summaryNode.get("reportType").asText());

            int nbUnits = summaryNode.get("extendedInfo").get("nbUnits").asInt();
            int nbObjectGroups = summaryNode.get("extendedInfo").get("nbObjectGroups").asInt();
            assertEquals(nbUnits, reportLinesExpectedByUnitId.size());
            assertEquals(nbObjectGroups, reportLinesExpectedByOGId.size());

            JsonNode queryNode = reportsLines.get(2);
            ObjectNode jsonNode = (ObjectNode) queryNode.get("query");
            OriginatingAgencyReassignmentRequest requestFromJsonNode = getFromJsonNode(
                jsonNode,
                OriginatingAgencyReassignmentRequest.class
            );
            assertEquals(sourceOriginatingAgency, requestFromJsonNode.getSourceOriginatingAgency());
            assertEquals(targetOriginatingAgency, requestFromJsonNode.getTargetOriginatingAgency());
            assertTrue(requestFromJsonNode.isPropagateToObjectGroups());

            // reporting
            JsonNode unitReportLines = JsonHandler.toJsonNode(
                Objects.requireNonNull(VitamTestHelper.getReports(reassignmentOperationId))
                    .stream()
                    .filter(elt -> elt.has("type") && ("Unit".equals(elt.get("type").asText())))
                    .collect(Collectors.toList())
            );

            List<OriginatingAgencyReassignmentReportLine> reportsUnitsList = getFromJsonNode(
                unitReportLines,
                new TypeReference<>() {}
            );
            Assertions.assertThat(reportsUnitsList).hasSameSizeAs(reportLinesExpectedByUnitId.keySet());

            for (OriginatingAgencyReassignmentReportLine unitReportLine : reportsUnitsList) {
                assertEquals(sourceOriginatingAgency, unitReportLine.getSourceOriginatingAgency());
                assertEquals(targetOriginatingAgency, unitReportLine.getTargetOriginatingAgency());
                String unitId = unitReportLine.getId();
                OriginatingAgencyReassignmentReportLine reportLinesExpected = reportLinesExpectedByUnitId.get(unitId);
                assertNotNull(reportLinesExpected);
                assertEquals(unitReportLine.getId(), reportLinesExpected.getId());
                assertEquals(
                    unitReportLine.getSourceOriginatingAgency(),
                    reportLinesExpected.getSourceOriginatingAgency()
                );
                assertEquals(unitReportLine.getOpi(), reportLinesExpected.getOpi());
                assertEquals(unitReportLine.getObjectGroupId(), reportLinesExpected.getObjectGroupId());
            }

            JsonNode objectGroupReportLines = JsonHandler.toJsonNode(
                reportsLines
                    .stream()
                    .filter(elt -> elt.has("type") && ("ObjectGroup".equals(elt.get("type").asText())))
                    .collect(Collectors.toList())
            );

            List<OriginatingAgencyReassignmentReportLine> reportsList = getFromJsonNode(
                objectGroupReportLines,
                new TypeReference<>() {}
            );

            Assertions.assertThat(reportsList).hasSameSizeAs(reportLinesExpectedByOGId.values());

            for (OriginatingAgencyReassignmentReportLine objectGroupReportLine : reportsList) {
                assertEquals(sourceOriginatingAgency, objectGroupReportLine.getSourceOriginatingAgency());
                assertEquals(targetOriginatingAgency, objectGroupReportLine.getTargetOriginatingAgency());

                String objectGroupId = objectGroupReportLine.getId();
                OriginatingAgencyReassignmentReportLine reportLinesExpected = reportLinesExpectedByOGId.get(
                    objectGroupId
                );
                assertNotNull(reportLinesExpected);
                assertEquals(objectGroupReportLine.getId(), reportLinesExpected.getId());
                assertEquals(
                    objectGroupReportLine.getSourceOriginatingAgency(),
                    reportLinesExpected.getSourceOriginatingAgency()
                );
                assertEquals(objectGroupReportLine.getOpi(), reportLinesExpected.getOpi());
            }
        }
    }
}
