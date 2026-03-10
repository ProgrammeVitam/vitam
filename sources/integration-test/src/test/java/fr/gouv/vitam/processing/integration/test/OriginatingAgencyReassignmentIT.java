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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static fr.gouv.vitam.common.VitamTestHelper.awaitForWorkflowTerminationWithStatus;
import static fr.gouv.vitam.common.VitamTestHelper.computeInheritedRules;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.processing.integration.test.IntegrationTestUtils.launchOriginatingAgencyReassignmentOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Processing integration test
 */
public class OriginatingAgencyReassignmentIT extends VitamRuleRunner {

    public static final List<String> UNIT_TITLES_TO_UPDATE = List.of(
        "3_Gallieni",
        "4_ Porte de Clignancourt",
        "Botzaris",
        "Stalingrad.txt",
        "Montparnasse.txt",
        "Pereire.txt"
    );

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        OriginatingAgencyReassignmentIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            BatchReportMain.class,
            AccessInternalMain.class
        )
    );

    private static final Integer tenantId = 0;

    private static final String SIP_FOLDER = "SIP";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME = "integration-processing/OK_LINK_AU_TO_EXISTING_GOT";
    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET = "integration-processing";
    private static final String SIP_COMPLEX_RULES_V2 = "integration-processing/OK_RULES_COMPLEXE_COMPLETE_V2.zip";

    private static final String REASSIGNMENT_COMPLEX_SIP = "integration-processing/OK_images_2BinaryMaster_elim.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

        processMonitoring = ProcessMonitoringImpl.getInstance();

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientType.MOCK);
        new DataLoader("integration-processing").prepareData();
    }

    @Before
    public void beforeTest() {
        VitamConfiguration.setProcessEngineWaitForStepTimeout(
            new VitamConfigurationParameters().getProcessEngineWaitForStepTimeout()
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
        VitamConfiguration.setProcessEngineWaitForStepTimeout(
            new VitamConfigurationParameters().getProcessEngineWaitForStepTimeout()
        );
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(
            Sets.newHashSet(
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                LogbookCollections.OPERATION.getName(),
                LogbookCollections.LIFECYCLE_UNIT.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            )
        );
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @RunWithCustomExecutor
    @Test
    public void testOriginatingAgencyReassignmentWithImpactOnComputedRules() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
            processingClient,
            workspaceClient,
            processMonitoring,
            SIP_COMPLEX_RULES_V2,
            DEFAULT_WORKFLOW.name(),
            StatusCode.OK,
            SIP_FOLDER
        );

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery ingestSelect = new SelectMultiQuery();
        CompareQuery operationQuery = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        ingestSelect.setQuery(operationQuery);

        JsonNode selectUnitsAfterIngest = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get(TAG_RESULTS);
        assertThat(selectUnitsAfterIngest.elements())
            .toIterable()
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterIngest.elements())
            .toIterable()
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);

        // When
        computeInheritedRules(ingestSelect);

        // Then
        JsonNode selectUnitsAfterComputedInheritedRules = metaDataClient
            .selectUnits(ingestSelect.getFinalSelect())
            .get(TAG_RESULTS);

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .toIterable()
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .toIterable()
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);

        // update originating agencies
        String currentOriginatingAgency = "RATP";
        String targetOriginatingAgency = "FRAN_NP_050239";
        SelectMultiQuery partialSelect = new SelectMultiQuery();
        BooleanQuery orQuery = QueryHelper.or();

        for (String title : UNIT_TITLES_TO_UPDATE) {
            orQuery.add(QueryHelper.eq("Title", title));
        }

        partialSelect.setQuery(QueryHelper.and().add(orQuery, operationQuery));

        // When
        String reassignOperationId = launchOriginatingAgencyReassignmentOperation(
            currentOriginatingAgency,
            targetOriginatingAgency,
            true,
            partialSelect
        );

        // Then
        Map<String, JsonNode> unitsByTitle = new HashMap<>();

        JsonNode resultsUnits = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode result : resultsUnits) {
            unitsByTitle.put(result.get("Title").asText(), result);
        }

        List<JsonNode> objectGroupsNodes = new ArrayList<>();
        JsonNode resultsObjectGroups = metaDataClient.selectObjectGroups(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode resultObjectGroup : resultsObjectGroups) {
            objectGroupsNodes.add(resultObjectGroup);
        }
        Set<String> objectGroupIdsForUpdatedUnits = new HashSet<>();
        List<String> unitsIdsToUpdate = new ArrayList<>();
        for (Map.Entry<String, JsonNode> titleToUnitEntry : unitsByTitle.entrySet()) {
            JsonNode unitNode = titleToUnitEntry.getValue();
            String title = titleToUnitEntry.getKey();

            if (UNIT_TITLES_TO_UPDATE.contains(title)) {
                unitsIdsToUpdate.add(unitNode.get(VitamFieldsHelper.id()).asText());
                if (unitNode.has(VitamFieldsHelper.object())) {
                    objectGroupIdsForUpdatedUnits.add(unitNode.get(VitamFieldsHelper.object()).asText());
                }
            }
        }

        //Check SP
        assertObjectGroupOriginatingAgency(
            objectGroupsNodes,
            objectGroupIdsForUpdatedUnits,
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        assertUnitsOriginatingAgency(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        //check Invalidate computed inherited Rules
        assertUnitsComputedInheritedRulesInvalidated(unitsByTitle.values(), unitsIdsToUpdate);

        //check operation in OPS
        assertOperationInOps(unitsByTitle.values(), unitsIdsToUpdate, reassignOperationId);
        assertOperationInOps(objectGroupsNodes, objectGroupIdsForUpdatedUnits, reassignOperationId);

        //check lfc
        assertLfcOnUnits(unitsByTitle.values(), unitsIdsToUpdate, targetOriginatingAgency, currentOriginatingAgency);

        assertLfcOnObjectGroups(
            objectGroupsNodes,
            objectGroupIdsForUpdatedUnits,
            targetOriginatingAgency,
            currentOriginatingAgency
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testOriginatingAgencyReassignmentWithPropagationOnObjectGroups() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
            processingClient,
            workspaceClient,
            processMonitoring,
            SIP_COMPLEX_RULES_V2,
            DEFAULT_WORKFLOW.name(),
            StatusCode.OK,
            SIP_FOLDER
        );

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery ingestSelect = new SelectMultiQuery();
        CompareQuery operationQuery = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        ingestSelect.setQuery(operationQuery);

        // update originating agencies
        String currentOriginatingAgency = "RATP";
        String targetOriginatingAgency = "FRAN_NP_050239";
        SelectMultiQuery partialSelect = new SelectMultiQuery();
        BooleanQuery orQuery = QueryHelper.or();

        for (String title : UNIT_TITLES_TO_UPDATE) {
            orQuery.add(QueryHelper.eq("Title", title));
        }

        partialSelect.setQuery(QueryHelper.and().add(orQuery, operationQuery));

        // When
        String reassignOperationId = launchOriginatingAgencyReassignmentOperation(
            currentOriginatingAgency,
            targetOriginatingAgency,
            true,
            partialSelect
        );

        // Then
        Map<String, JsonNode> unitsByTitle = new HashMap<>();

        JsonNode resultsUnits = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode result : resultsUnits) {
            unitsByTitle.put(result.get("Title").asText(), result);
        }

        List<JsonNode> objectGroupsNodes = new ArrayList<>();
        JsonNode resultsObjectGroups = metaDataClient.selectObjectGroups(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode resultObjectGroup : resultsObjectGroups) {
            objectGroupsNodes.add(resultObjectGroup);
        }
        Set<String> objectGroupIdsForUpdatedUnits = new HashSet<>();
        List<String> unitsIdsToUpdate = new ArrayList<>();
        for (Map.Entry<String, JsonNode> titleToUnitEntry : unitsByTitle.entrySet()) {
            JsonNode unitNode = titleToUnitEntry.getValue();
            String title = titleToUnitEntry.getKey();

            if (UNIT_TITLES_TO_UPDATE.contains(title)) {
                unitsIdsToUpdate.add(unitNode.get(VitamFieldsHelper.id()).asText());
                if (unitNode.has(VitamFieldsHelper.object())) {
                    objectGroupIdsForUpdatedUnits.add(unitNode.get(VitamFieldsHelper.object()).asText());
                }
            }
        }

        //Check SP
        assertObjectGroupOriginatingAgency(
            objectGroupsNodes,
            objectGroupIdsForUpdatedUnits,
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        assertUnitsOriginatingAgency(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        //check operation in OPS
        assertOperationInOps(unitsByTitle.values(), unitsIdsToUpdate, reassignOperationId);
        assertOperationInOps(objectGroupsNodes, objectGroupIdsForUpdatedUnits, reassignOperationId);

        //check lfc
        assertLfcOnUnits(unitsByTitle.values(), unitsIdsToUpdate, targetOriginatingAgency, currentOriginatingAgency);

        assertLfcOnObjectGroups(
            objectGroupsNodes,
            objectGroupIdsForUpdatedUnits,
            targetOriginatingAgency,
            currentOriginatingAgency
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testOriginatingAgencyReassignmentOnUnitsOnly() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
            processingClient,
            workspaceClient,
            processMonitoring,
            SIP_COMPLEX_RULES_V2,
            DEFAULT_WORKFLOW.name(),
            StatusCode.OK,
            SIP_FOLDER
        );

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery ingestSelect = new SelectMultiQuery();
        CompareQuery operationQuery = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        ingestSelect.setQuery(operationQuery);

        // Then

        // update originating agencies
        String currentOriginatingAgency = "RATP";
        String targetOriginatingAgency = "FRAN_NP_050239";
        SelectMultiQuery partialSelect = new SelectMultiQuery();
        BooleanQuery orQuery = QueryHelper.or();
        for (String title : UNIT_TITLES_TO_UPDATE) {
            orQuery.add(QueryHelper.eq("Title", title));
        }

        partialSelect.setQuery(QueryHelper.and().add(orQuery, operationQuery));

        // When
        String reassignOperationId = launchOriginatingAgencyReassignmentOperation(
            currentOriginatingAgency,
            targetOriginatingAgency,
            false,
            partialSelect
        );

        // Then
        Map<String, JsonNode> unitsByTitle = new HashMap<>();

        JsonNode resultsUnits = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode result : resultsUnits) {
            unitsByTitle.put(result.get("Title").asText(), result);
        }

        List<String> unitsIdsToUpdate = new ArrayList<>();
        for (Map.Entry<String, JsonNode> titleToUnitEntry : unitsByTitle.entrySet()) {
            JsonNode unitNode = titleToUnitEntry.getValue();
            String title = titleToUnitEntry.getKey();

            if (UNIT_TITLES_TO_UPDATE.contains(title)) {
                unitsIdsToUpdate.add(unitNode.get(VitamFieldsHelper.id()).asText());
            }
        }

        //Reassignment without ObjectGroup propagation

        unitsByTitle = new HashMap<>();

        resultsUnits = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode result : resultsUnits) {
            unitsByTitle.put(result.get("Title").asText(), result);
        }

        List<JsonNode> objectGroupsNodes = new ArrayList<>();
        JsonNode resultsObjectGroups = metaDataClient.selectObjectGroups(ingestSelect.getFinalSelect()).get("$results");
        for (JsonNode resultObjectGroup : resultsObjectGroups) {
            objectGroupsNodes.add(resultObjectGroup);
        }

        //Check SP
        assertObjectGroupOriginatingAgency(
            objectGroupsNodes,
            Collections.emptySet(),
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        assertUnitsOriginatingAgency(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            targetOriginatingAgency,
            currentOriginatingAgency
        );

        //check operation in OPS
        assertOperationInOps(unitsByTitle.values(), unitsIdsToUpdate, reassignOperationId);
        assertOperationInOps(objectGroupsNodes, Collections.emptySet(), reassignOperationId);

        //check lfc
        assertLfcOnUnits(unitsByTitle.values(), unitsIdsToUpdate, targetOriginatingAgency, currentOriginatingAgency);

        assertLfcOnObjectGroups(
            objectGroupsNodes,
            Collections.emptySet(),
            targetOriginatingAgency,
            currentOriginatingAgency
        );
    }

    private static void assertObjectGroupOriginatingAgency(
        List<JsonNode> objectGroupsNodes,
        Set<String> concernedObjectGroupToBeUpdated,
        String targetOriginatingAgency,
        String currentOriginatingAgency
    ) {
        for (JsonNode objectGroup : objectGroupsNodes) {
            String objectGroupId = objectGroup.get(VitamFieldsHelper.id()).asText();
            String originatingAgency = objectGroup.get(VitamFieldsHelper.originatingAgency()).asText();
            List<String> originatingAgencies = IntegrationTestUtils.getUnitOriginatingAgencies(objectGroup);
            if (concernedObjectGroupToBeUpdated.contains(objectGroupId)) {
                assertThat(originatingAgency).isEqualTo(targetOriginatingAgency);
                assertThat(originatingAgencies).contains(targetOriginatingAgency);
            } else {
                assertThat(originatingAgency).isEqualTo(currentOriginatingAgency);
                assertThat(originatingAgencies).contains(currentOriginatingAgency);
            }
        }
    }

    private static void assertUnitsOriginatingAgency(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated,
        String targetOriginatingAgency,
        String currentOriginatingAgency
    ) {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            String originatingAgency = metadataNode.get(VitamFieldsHelper.originatingAgency()).asText();
            List<String> originatingAgencies = IntegrationTestUtils.getUnitOriginatingAgencies(metadataNode);
            if (concernedToBeUpdated.contains(id)) {
                assertThat(originatingAgency).isEqualTo(targetOriginatingAgency);
                assertThat(originatingAgencies).contains(targetOriginatingAgency);
            } else {
                assertThat(originatingAgency).isEqualTo(currentOriginatingAgency);
                assertThat(originatingAgencies).contains(currentOriginatingAgency);
            }
        }
    }

    private static void assertUnitsComputedInheritedRulesInvalidated(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated
    ) {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            Boolean validComputedInheritedRules = metadataNode
                .get(VitamFieldsHelper.validComputedInheritedRules())
                .asBoolean();
            JsonNode computedInheritedRulesNode = metadataNode.get(VitamFieldsHelper.computedInheritedRules());
            if (concernedToBeUpdated.contains(id)) {
                assertThat(validComputedInheritedRules).isFalse();
                assertThat(computedInheritedRulesNode).isNull();
            }
        }
    }

    private static void assertOperationInOps(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated,
        String operationId
    ) {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            List<String> operations = IntegrationTestUtils.getUnitOperations(metadataNode);
            if (concernedToBeUpdated.contains(id)) {
                assertThat(operations).contains(operationId);
            } else {
                assertThat(operations).doesNotContain(operationId);
            }
        }
    }

    private static void assertLfcOnUnits(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated,
        String targetOriginatingAgency,
        String currentOriginatingAgency
    ) {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            if (concernedToBeUpdated.contains(id)) {
                try {
                    //Check lfc
                    JsonNode unitLfc;
                    LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
                        .getClient();

                    unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(id, new Select().getFinalSelectById());

                    JsonNode lfcEvents = unitLfc.get("$results").get(0).get("events");
                    final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
                    JsonNode evDetData = lastEvent.get("evDetData");
                    JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());

                    String expectedAddSp = "+  \"_sp\" : \"" + targetOriginatingAgency + "\"";
                    String expectedRemoveSp = "-  \"_sp\" : \"" + currentOriginatingAgency + "\"";

                    assertThat(jsoned.get("Event").textValue()).contains(expectedAddSp);
                    assertThat(jsoned.get("Event").textValue()).contains(expectedRemoveSp);
                } catch (InvalidParseOperationException | LogbookClientException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void assertLfcOnObjectGroups(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated,
        String targetOriginatingAgency,
        String currentOriginatingAgency
    ) {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            if (concernedToBeUpdated.contains(id)) {
                try {
                    //Check lfc
                    LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
                        .getClient();

                    JsonNode objectGroupLfc = logbookLifeCyclesClient.selectObjectGroupLifeCycleById(
                        id,
                        new Select().getFinalSelectById()
                    );

                    JsonNode lfcEvents = objectGroupLfc.get("$results").get(0).get("events");
                    final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
                    JsonNode evDetData = lastEvent.get("evDetData");
                    JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());

                    String expectedAddSp = "+  \"_sp\" : \"" + targetOriginatingAgency + "\"";
                    String expectedRemoveSp = "-  \"_sp\" : \"" + currentOriginatingAgency + "\"";

                    assertThat(jsoned.get("Event").textValue()).contains(expectedAddSp);
                    assertThat(jsoned.get("Event").textValue()).contains(expectedRemoveSp);
                } catch (InvalidParseOperationException | LogbookClientException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowOriginatingAgencyReassignmentWithPartialDettachement() throws Exception {
        prepareVitamSession();
        workspaceClient = WorkspaceClientFactory.getInstance(WorkFlowExecutionContext.VITAM).getClient();
        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
            processingClient,
            workspaceClient,
            processMonitoring,
            REASSIGNMENT_COMPLEX_SIP,
            DEFAULT_WORKFLOW.name(),
            WARNING,
            SIP_FOLDER
        );

        final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        SelectMultiQuery ingestSelect = new SelectMultiQuery();
        CompareQuery operationQuery = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        ingestSelect.setQuery(operationQuery);

        JsonNode selectUnitsAfterIngest = metaDataClient.selectUnits(ingestSelect.getFinalSelect()).get(TAG_RESULTS);

        List<String> unitsIds = new ArrayList<>();
        String someUnitId = null;
        String someObjectGroupId = null;
        for (JsonNode result : selectUnitsAfterIngest) {
            unitsIds.add(result.get(VitamFieldsHelper.id()).asText());
            assertEquals("Vitam", result.get(VitamFieldsHelper.originatingAgency()).asText());
            if (result.has(VitamFieldsHelper.object()) && "Image de lac - 1".equals(result.get("Title").asText())) {
                someObjectGroupId = result.get(VitamFieldsHelper.object()).asText();
                someUnitId = result.get(VitamFieldsHelper.id()).asText();
            }
        }
        assertThat(unitsIds).isNotEmpty();
        assertNotNull(someUnitId);
        assertNotNull(someObjectGroupId);
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        //Attach a new unit to existing got someObjectGroupId

        IntegrationTestUtils.simulateAttachUnitToExistingGOT(
            tenantId,
            processMonitoring,
            workspaceClient,
            processingClient,
            LINK_AU_TO_EXISTING_GOT_OK_NAME,
            LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET,
            someObjectGroupId,
            zipName,
            SIP_FOLDER
        );

        //Eliminate first unit

        SelectMultiQuery analysisDslRequest = new SelectMultiQuery();
        analysisDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.id(), someUnitId));

        // elimination action
        final GUID eliminationActionOperationGuid = newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(eliminationActionOperationGuid.toString());

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2026-02-11",
            analysisDslRequest.getFinalSelect()
        );

        final RequestResponse<JsonNode> actionResult = accessInternalClient.startEliminationAction(
            eliminationRequestBody
        );

        OperationContextMonitor operationContextMonitor = new OperationContextMonitor();
        assertThat(actionResult.isOk()).isTrue();

        JsonNode info = operationContextMonitor.getInformation(
            VitamConfiguration.getDefaultStrategy(),
            eliminationActionOperationGuid.toString(),
            LogbookTypeProcess.ELIMINATION
        );

        assertThat(info).isNotNull();

        awaitForWorkflowTerminationWithStatus(eliminationActionOperationGuid, StatusCode.OK);

        JsonNode selectUnitsAfterElimination = metaDataClient
            .selectUnits(ingestSelect.getFinalSelect())
            .get(TAG_RESULTS);

        List<String> unitsIdsAfterElimination = new ArrayList<>();

        for (JsonNode result : selectUnitsAfterElimination) {
            unitsIdsAfterElimination.add(result.get(VitamFieldsHelper.id()).asText());
        }
        assertThat(unitsIdsAfterElimination).doesNotContain(someUnitId);

        SelectMultiQuery objectGroupDslRequest = new SelectMultiQuery();
        objectGroupDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.id(), someObjectGroupId));

        JsonNode objectGroupNode = metaDataClient
            .selectObjectGroups(objectGroupDslRequest.getFinalSelect())
            .get("$results");

        String newUnitId = null;
        for (JsonNode resultOG : objectGroupNode) {
            assertEquals("Vitam", resultOG.get(VitamFieldsHelper.originatingAgency()).asText());
            List<String> gotParents = IntegrationTestUtils.getUnitParents(resultOG);
            assertThat(gotParents).hasSize(1);
            newUnitId = gotParents.getFirst();
        }

        SelectMultiQuery newUnitDslRequest = new SelectMultiQuery();
        newUnitDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.id(), newUnitId));

        JsonNode unitsNodes = metaDataClient.selectUnits(newUnitDslRequest.getFinalSelect()).get("$results");
        List<String> ingestOperations = new ArrayList<>();
        ingestOperations.add(ingestOperation);
        for (JsonNode unitNode : unitsNodes) {
            assertEquals("CCCCCCCCCC", unitNode.get(VitamFieldsHelper.originatingAgency()).asText());
            ingestOperations.addAll(IntegrationTestUtils.getUnitOperations(unitNode));
        }
        //Reassignment on unit attached to not origin got

        SelectMultiQuery reassignmentDslRequest = new SelectMultiQuery();
        reassignmentDslRequest.addQueries(QueryHelper.in(VitamFieldsHelper.id(), newUnitId));

        String currentOriginatingAgency = "CCCCCCCCCC";
        String targetOriginatingAgency = "RATP";
        boolean propagateToObjectGroups = true;
        // When
        launchOriginatingAgencyReassignmentOperation(
            currentOriginatingAgency,
            targetOriginatingAgency,
            propagateToObjectGroups,
            reassignmentDslRequest
        );
        //check SP on unit
        JsonNode newUnitsNodes = metaDataClient.selectUnits(newUnitDslRequest.getFinalSelect()).get("$results");
        for (JsonNode unitNode : newUnitsNodes) {
            assertEquals("RATP", unitNode.get(VitamFieldsHelper.originatingAgency()).asText());
        }

        //check sp on got, the got SP should be not updated
        JsonNode gotsAfterReassignment = metaDataClient
            .selectObjectGroups(reassignmentDslRequest.getFinalSelect())
            .get("$results");

        for (JsonNode resultGot : gotsAfterReassignment) {
            assertEquals("Vitam", resultGot.get(VitamFieldsHelper.originatingAgency()).asText());
        }

        //Reassignment operation with some unit sp = target and got not, Got should be updated

        SelectMultiQuery reassignmentDslRequest2 = new SelectMultiQuery();
        reassignmentDslRequest2.addQueries(
            QueryHelper.in(VitamFieldsHelper.operations(), ingestOperations.toArray(String[]::new))
        );

        currentOriginatingAgency = "Vitam";
        targetOriginatingAgency = "RATP";
        // When
        String reassignOperationId2 = launchOriginatingAgencyReassignmentOperation(
            currentOriginatingAgency,
            targetOriginatingAgency,
            propagateToObjectGroups,
            reassignmentDslRequest2
        );

        JsonNode resultsUnitsAfterReassignment2 = metaDataClient
            .selectUnits(reassignmentDslRequest2.getFinalSelect())
            .get("$results");
        for (JsonNode result : resultsUnitsAfterReassignment2) {
            String unitId = result.get(VitamFieldsHelper.id()).asText();
            String originatingAgency = result.get(VitamFieldsHelper.originatingAgency()).asText();
            assertEquals("RATP", originatingAgency);
            List<String> operations = IntegrationTestUtils.getUnitOperations(result);
            if (unitId.equals(newUnitId)) {
                //the newUnitId should not be updated, but the got yes
                assertThat(operations).doesNotContain(reassignOperationId2);
            } else {
                assertThat(operations).contains(reassignOperationId2);
            }
        }

        JsonNode gotsAfterReassignment2 = metaDataClient
            .selectObjectGroups(reassignmentDslRequest2.getFinalSelect())
            .get("$results");

        for (JsonNode resultGot : gotsAfterReassignment2) {
            String originatingAgency = resultGot.get(VitamFieldsHelper.originatingAgency()).asText();
            assertEquals("RATP", originatingAgency);
        }
    }
}
