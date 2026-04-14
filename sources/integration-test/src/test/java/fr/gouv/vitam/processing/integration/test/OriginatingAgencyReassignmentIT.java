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
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.TestZipUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequestParameters;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.model.reassignment.ReassignmentOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ResultPreservation;
import fr.gouv.vitam.worker.core.plugin.reassignment.OriginatingAgencyReassignmentPreparationPlugin;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.VitamTestHelper.awaitForWorkflowTerminationWithStatus;
import static fr.gouv.vitam.common.VitamTestHelper.computeInheritedRules;
import static fr.gouv.vitam.common.VitamTestHelper.getTransferSip;
import static fr.gouv.vitam.common.VitamTestHelper.printDebutInformation;
import static fr.gouv.vitam.common.VitamTestHelper.readReportFile;
import static fr.gouv.vitam.common.VitamTestHelper.startTransferReplyWorkflow;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.FILING_SCHEME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.HOLDING_SCHEME;
import static fr.gouv.vitam.processing.integration.test.IntegrationTestUtils.launchOriginatingAgencyReassignmentOperation;
import static fr.gouv.vitam.processing.integration.test.IntegrationTestUtils.replaceStringInFile;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
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
            StorageMain.class,
            DefaultOfferMain.class,
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

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private static final Integer tenantId = 0;

    private static final String SIP_FOLDER = "SIP";

    private WorkspaceClient workspaceClient;

    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME = "integration-processing/OK_LINK_AU_TO_EXISTING_GOT";
    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET = "integration-processing";
    private static final String SIP_COMPLEX_RULES_V2 = "integration-processing/OK_RULES_COMPLEXE_COMPLETE_V2.zip";

    private static final String REASSIGNMENT_COMPLEX_SIP = "integration-processing/OK_images_2BinaryMaster_elim.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

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
        // FIXME : To be removed once temporary v91 restrictions are removed
        OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = true;

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
        // FIXME : To be removed once temporary v91 restrictions are removed
        OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = false;

        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
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
            partialSelect.getFinalSelect(),
            Set.of(StatusCode.OK, StatusCode.WARNING)
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

        //check reassignment history
        assertReassignmentOperationsHistory(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            reassignOperationId,
            currentOriginatingAgency,
            targetOriginatingAgency
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
    public void testOriginatingAgencyReassignmentWithPropagationOnObjectGroups() throws Exception {
        // FIXME : To be removed once temporary v91 restrictions are removed
        OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = false;

        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
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
            partialSelect.getFinalSelect(),
            Set.of(StatusCode.OK, StatusCode.WARNING)
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

        //check reassignment history
        assertReassignmentOperationsHistory(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            reassignOperationId,
            currentOriginatingAgency,
            targetOriginatingAgency
        );

        //check reassignment history
        assertReassignmentOperationsHistory(
            objectGroupsNodes,
            objectGroupIdsForUpdatedUnits,
            reassignOperationId,
            currentOriginatingAgency,
            targetOriginatingAgency
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
        // FIXME : To be removed once temporary v91 restrictions are removed
        OriginatingAgencyReassignmentPreparationPlugin._____Enable_Temporary_V91_Restrictions_____ = false;

        prepareVitamSession();

        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
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
            partialSelect.getFinalSelect(),
            Set.of(StatusCode.OK, StatusCode.WARNING)
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

        //check reassignment history
        assertReassignmentOperationsHistory(
            unitsByTitle.values(),
            unitsIdsToUpdate,
            reassignOperationId,
            currentOriginatingAgency,
            targetOriginatingAgency
        );

        //check reassignment history
        assertReassignmentOperationsHistory(
            objectGroupsNodes,
            Collections.emptySet(),
            reassignOperationId,
            currentOriginatingAgency,
            targetOriginatingAgency
        );

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

    private static void assertReassignmentOperationsHistory(
        Collection<JsonNode> metadataNodes,
        Collection<String> concernedToBeUpdated,
        String operationId,
        String sourceOriginatingAgency,
        String targetOriginatingAgency
    ) throws InvalidParseOperationException {
        for (JsonNode metadataNode : metadataNodes) {
            String id = metadataNode.get(VitamFieldsHelper.id()).asText();
            if (concernedToBeUpdated.contains(id)) {
                assertThat(metadataNode.has(VitamFieldsHelper.reassignments())).isTrue();

                ArrayNode reassignmentsNode = (ArrayNode) metadataNode.get(VitamFieldsHelper.reassignments());

                assertThat(reassignmentsNode).isNotNull();
                assertThat(reassignmentsNode).isNotEmpty();
                JsonNode lastReassignmentOperationNode = reassignmentsNode.get(reassignmentsNode.size() - 1);

                ReassignmentOperation lastReassignmentOperation = JsonHandler.getFromJsonNode(
                    lastReassignmentOperationNode,
                    ReassignmentOperation.class
                );
                assertThat(lastReassignmentOperation.getOperationId()).isEqualTo(operationId);
                assertThat(lastReassignmentOperation.getTargetOriginatingAgency()).isEqualTo(targetOriginatingAgency);
                assertThat(lastReassignmentOperation.getSourceOriginatingAgency()).isEqualTo(sourceOriginatingAgency);
                assertThat(lastReassignmentOperation.getReassignmentDate()).isNotNull();
            } else {
                assertThat(metadataNode.has(VitamFieldsHelper.reassignments())).isFalse();
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
    public void testWorkflowOriginatingAgencyReassignmentWithPartialDetachement() throws Exception {
        prepareVitamSession();
        workspaceClient = WorkspaceClientFactory.getInstance(WorkFlowExecutionContext.VITAM).getClient();
        // Given ingest
        final String ingestOperation = IntegrationTestUtils.ingestSIP(
            tenantId,
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
            workspaceClient,
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
            reassignmentDslRequest.getFinalSelect(),
            Set.of(StatusCode.OK, StatusCode.WARNING)
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
            reassignmentDslRequest2.getFinalSelect(),
            Set.of(StatusCode.OK, StatusCode.WARNING)
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

    @RunWithCustomExecutor
    @Test
    public void originatingAgencyReassignmentComplexTreeTests() throws Exception {
        prepareVitamSession();
        workspaceClient = WorkspaceClientFactory.getInstance(WorkFlowExecutionContext.VITAM).getClient();

        // Freeze time
        logicalClock.freezeTime();
        String initDateTime = LocalDateUtil.nowFormatted();

        // Given complex setup (cf test/resources/reassignment/ComplexReassignmentGraph.excalidraw - https://excalidraw.com/)
        final String opi1 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_1.zip",
            HOLDING_SCHEME.name(),
            OK,
            SIP_FOLDER
        );
        final String opi2 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_2.zip",
            FILING_SCHEME.name(),
            OK,
            SIP_FOLDER
        );

        final String opi3 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_3.zip",
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );

        // Ingest attached unit to existing object group
        Path opi4ZipFolder = tempFolder.newFolder("Complex_Originating_Agency_Reassignment_OPI_4").toPath();
        TestZipUtils.unzipFile(
            PropertiesUtils.getResourceFile(
                "reassignment/Complex_Originating_Agency_Reassignment_OPI_4.zip"
            ).getAbsolutePath(),
            opi4ZipFolder.toString()
        );
        ArrayNode opi3Units = selectUnits(opi3);
        Map<String, String> opi3OgIdByUnitTitle = new HashMap<>();
        opi3Units
            .iterator()
            .forEachRemaining(
                unit ->
                    opi3OgIdByUnitTitle.put(
                        unit.get("Title").asText(),
                        unit.has(VitamFieldsHelper.object()) ? unit.get(VitamFieldsHelper.object()).asText() : null
                    )
            );
        replaceStringInFile(
            opi4ZipFolder.resolve("manifest.xml"),
            "#####GUID_OG_4#####",
            opi3OgIdByUnitTitle.get("Unit_9")
        );
        replaceStringInFile(
            opi4ZipFolder.resolve("manifest.xml"),
            "#####GUID_OG_5#####",
            opi3OgIdByUnitTitle.get("Unit_10")
        );

        String opi4ZipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        TestZipUtils.zipFolder(opi4ZipFolder, opi4ZipName);

        final String opi4 = IntegrationTestUtils.ingestSIP(
            tenantId,
            new FileInputStream(opi4ZipName),
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );

        final String opi5 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_5.zip",
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );
        final String opi6 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_6.zip",
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );
        final String opi7 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_7.zip",
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );
        final String opi8 = IntegrationTestUtils.ingestSIP(
            tenantId,
            "reassignment/Complex_Originating_Agency_Reassignment_OPI_8.zip",
            DEFAULT_WORKFLOW.name(),
            OK,
            SIP_FOLDER
        );

        final SelectMultiQuery eliminationQuery = new SelectMultiQuery();
        eliminationQuery.addQueries(
            QueryHelper.and()
                .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), opi3), QueryHelper.eq("Tag", "Unit_10"))
        );
        String opi9 = eliminationAction(eliminationQuery.getFinalSelect());

        SelectMultiQuery transferQuery = new SelectMultiQuery();
        transferQuery.addQueries(
            QueryHelper.and()
                .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), opi4), QueryHelper.eq("Tag", "Unit_12"))
        );
        String transferOperation = transferRequest(transferQuery.getFinalSelect());
        String ingestOfTransferredSip;
        try (InputStream sipInputStream = getTransferSip(transferOperation)) {
            ingestOfTransferredSip = IntegrationTestUtils.ingestSIP(
                tenantId,
                sipInputStream,
                DEFAULT_WORKFLOW.name(),
                OK,
                SIP_FOLDER
            );
        }
        String atr = readReportFile(ingestOfTransferredSip + ".xml");
        String opi10 = startTransferReplyWorkflow(new ByteArrayInputStream(atr.getBytes(StandardCharsets.UTF_8)), OK);

        SelectMultiQuery deleteGotVersionQuery = new SelectMultiQuery();
        deleteGotVersionQuery.addQueries(
            QueryHelper.and()
                .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), opi4), QueryHelper.eq("Tag", "Unit_13"))
        );
        DeleteGotVersionsRequest deleteGotVersionsRequest = new DeleteGotVersionsRequest(
            deleteGotVersionQuery.getFinalSelect(),
            BINARY_MASTER.getName(),
            List.of(2)
        );

        String opi11 = deleteGotVersions(deleteGotVersionsRequest);

        SelectMultiQuery preservationSelectQuery = new SelectMultiQuery();
        preservationSelectQuery.setQuery(
            QueryHelper.and()
                .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), opi8), QueryHelper.eq("Tag", "Unit_19"))
        );

        String opi12 = preservation(preservationSelectQuery);

        // Check accession registers - Before reassignments
        assertThat(getAccessionRegisterDetail(opi1)).isNull();
        checkAccessionRegisterDetails("Identifier1", 2, 0, 0, 0, 0, 0, 0L, 0L, opi2);
        checkAccessionRegisterDetails("Identifier1", 7, 1, 5, 0, 7, 0, 5000L, 0L, opi3, opi9);
        checkAccessionRegisterDetails("Identifier2", 4, 1, 2, 1, 4, 2, 4000L, 2000L, opi4, opi10, opi11);
        checkAccessionRegisterDetails("Identifier1", 2, 0, 1, 0, 1, 0, 1000L, 0L, opi5);
        checkAccessionRegisterDetails("Identifier1", 1, 0, 1, 0, 2, 0, 2000L, 0L, opi6);
        checkAccessionRegisterDetails("Identifier3", 1, 0, 1, 0, 1, 0, 1000L, 0L, opi7);
        checkAccessionRegisterDetails("Identifier1", 1, 0, 1, 0, 1, 0, 1000L, 0L, opi8);
        checkAccessionRegisterDetails("Identifier1", 0, 0, 0, 0, 1, 0, 1000L, 0L, opi12);

        assertAccessionRegisterSummaryStats("Identifier1", 13, 1, 8, 0, 12, 0, 10000L, 0L);
        assertAccessionRegisterSummaryStats("Identifier2", 4, 1, 2, 1, 4, 2, 4000L, 2000L);
        assertAccessionRegisterSummaryStats("Identifier3", 1, 0, 1, 0, 1, 0, 1000L, 0L);
        assertThat(getAccessionRegisterSummary("Identifier4")).isNull();

        // Originating agency reassignment tests
        logicalClock.logicalSleep(10, ChronoUnit.SECONDS);
        String reassignmentDateTime = LocalDateUtil.nowFormatted();

        // Test 1 : source originating agency = target ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier3",
            "Identifier3",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.KO)
        );

        // Test 2 : Unknown target originating agency ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier3",
            "NoSuchAgency",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.KO)
        );

        // Test 3 : Originating agency must match source or target ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier4",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.KO)
        );

        // Test 4 : No metadata found ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi("unknown_opi"),
            Set.of(StatusCode.KO)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 5 : partial scope - missing units ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpiAndTag(opi2, "Unit_2"),
            Set.of(StatusCode.KO)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 6 : partial scope - missing object groups ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier1",
            false,
            queryByOpi(opi7),
            Set.of(StatusCode.KO)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 7 : partial scope - missing object attached to another object group ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi6),
            Set.of(StatusCode.KO)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 8 : object group has object added by another ingest ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi5),
            Set.of(StatusCode.KO)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 9 : object group has object added by another preservation operation ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi8),
            Set.of(StatusCode.KO)
        );

        // Test 10 : Holding scheme ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.KO)
        );

        // Test 11 : Filling plan - OK
        String opiReassign1 = launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            false,
            queryByOpi(opi2),
            Set.of(StatusCode.OK)
        );

        // Test 12 : OK (updating AccessionRegisterDetail 1)
        String opiReassign2 = launchOriginatingAgencyReassignmentOperation(
            "Identifier2",
            "Identifier1",
            true,
            queryByOpi(opi4),
            Set.of(StatusCode.OK)
        );

        // FIXME : To be removed once temporary v91 restrictions are removed
        // Test 13 : Reverting reassignment from Test 12 is impossible - Object groups OG_4 & OG_5 from OPI_3 should also be included ==> KO
        launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi4),
            Set.of(StatusCode.KO)
        );

        // Test 14 : OK (creating a new AccessionRegisterDetail 4)
        String opiReassign3 = launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier4",
            true,
            queryByOpi(opi3, opi4),
            Set.of(StatusCode.OK)
        );

        // Test 15 : OK multiple circular reassignments (Identifier3 -> Identifier1 --> Identifier2 --> Identifier3)
        String opiReassign4 = launchOriginatingAgencyReassignmentOperation(
            "Identifier3",
            "Identifier1",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.OK)
        );
        String opiReassign5 = launchOriginatingAgencyReassignmentOperation(
            "Identifier1",
            "Identifier2",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.OK)
        );
        String opiReassign6 = launchOriginatingAgencyReassignmentOperation(
            "Identifier2",
            "Identifier3",
            true,
            queryByOpi(opi7),
            Set.of(StatusCode.OK)
        );

        // Check accession registers - After reassignments
        assertThat(getAccessionRegisterDetail(opi1)).isNull();
        checkAccessionRegisterDetails("Identifier2", 2, 0, 0, 0, 0, 0, 0L, 0L, opi2, opiReassign1);
        checkAccessionRegisterDetails("Identifier4", 7, 1, 5, 0, 7, 0, 5000L, 0L, opi3, opi9, opiReassign3);
        checkAccessionRegisterDetails(
            "Identifier4",
            4,
            1,
            2,
            1,
            4,
            2,
            4000L,
            2000L,
            opi4,
            opi10,
            opi11,
            opiReassign2,
            opiReassign3
        );
        checkAccessionRegisterDetails("Identifier1", 2, 0, 1, 0, 1, 0, 1000L, 0L, opi5);
        checkAccessionRegisterDetails("Identifier1", 1, 0, 1, 0, 2, 0, 2000L, 0L, opi6);
        checkAccessionRegisterDetails(
            "Identifier3",
            1,
            0,
            1,
            0,
            1,
            0,
            1000L,
            0L,
            opi7,
            opiReassign4,
            opiReassign5,
            opiReassign6
        );
        checkAccessionRegisterDetails("Identifier1", 1, 0, 1, 0, 1, 0, 1000L, 0L, opi8);
        checkAccessionRegisterDetails("Identifier1", 0, 0, 0, 0, 1, 0, 1000L, 0L, opi12);

        assertAccessionRegisterSummaryStats("Identifier1", 4, 0, 3, 0, 5, 0, 5000L, 0L);
        assertAccessionRegisterSummaryStats("Identifier2", 2, 0, 0, 0, 0, 0, 0L, 0L);
        assertAccessionRegisterSummaryStats("Identifier3", 1, 0, 1, 0, 1, 0, 1000L, 0L);
        assertAccessionRegisterSummaryStats("Identifier4", 11, 2, 7, 1, 11, 2, 9000L, 2000L);

        // Check unit & object group graph
        ArrayNode units = selectUnits(opi1, opi2, opi3, opi4, opi5, opi6, opi7, opi8);
        ArrayNode objectGroups = selectObjectGroups(opi1, opi2, opi3, opi4, opi5, opi6, opi7, opi8);

        Map<String, ObjectNode> unitByTitle = new HashMap<>();
        units.iterator().forEachRemaining(unit -> unitByTitle.put(unit.get("Title").asText(), (ObjectNode) unit));

        Map<String, ObjectNode> ogById = new HashMap<>();
        objectGroups
            .iterator()
            .forEachRemaining(og -> ogById.put(og.get(VitamFieldsHelper.id()).asText(), (ObjectNode) og));

        Map<String, ObjectNode> ogByUnitTitle = new HashMap<>();
        units
            .iterator()
            .forEachRemaining(unit -> {
                if (unit.has(VitamFieldsHelper.object())) {
                    ogByUnitTitle.put(
                        unit.get("Title").asText(),
                        ogById.get(unit.get(VitamFieldsHelper.object()).asText())
                    );
                }
            });

        // OPI_1
        checkUnit(unitByTitle.get("Unit_1"), null, emptySet(), 0, List.of(opi1), emptyList(), initDateTime);
        // OPI_2
        checkUnit(
            unitByTitle.get("Unit_2"),
            "Identifier2",
            Set.of("Identifier2"),
            1,
            List.of(opi2, opiReassign1),
            List.of(opiReassign1),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_3"),
            "Identifier2",
            Set.of("Identifier2"),
            1,
            List.of(opi2, opiReassign1),
            List.of(opiReassign1),
            reassignmentDateTime
        );

        // OPI_3
        checkUnit(
            unitByTitle.get("Unit_4"),
            "Identifier4",
            Set.of("Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_4"),
            "Identifier4",
            Set.of("Identifier4"),
            2,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_5"),
            "Identifier4",
            Set.of("Identifier2", "Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_6"),
            "Identifier4",
            Set.of("Identifier2", "Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_6"),
            "Identifier4",
            Set.of("Identifier2", "Identifier4"),
            3,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_7"),
            "Identifier4",
            Set.of("Identifier2", "Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_7"),
            "Identifier4",
            Set.of("Identifier2", "Identifier4"),
            2,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_8"),
            "Identifier4",
            Set.of("Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_9"),
            "Identifier4",
            Set.of("Identifier4"),
            1,
            List.of(opi3, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_9"),
            "Identifier4",
            Set.of("Identifier4"),
            3,
            List.of(opi3, opi4, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );
        // Unit_10 deleted (elimination)

        // OPI_4
        checkUnit(
            unitByTitle.get("Unit_11"),
            "Identifier4",
            Set.of("Identifier4"),
            2,
            List.of(opi4, opiReassign2, opiReassign3),
            List.of(opiReassign2, opiReassign3),
            reassignmentDateTime
        );
        // Unit_12 deleted (transfer reply)
        checkUnit(
            unitByTitle.get("Unit_13"),
            "Identifier4",
            Set.of("Identifier4"),
            2,
            List.of(opi4, opiReassign2, opiReassign3),
            List.of(opiReassign2, opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_13"),
            "Identifier4",
            Set.of("Identifier4"),
            4,
            List.of(opi4, opi11, opiReassign2, opiReassign3),
            List.of(opiReassign2, opiReassign3),
            reassignmentDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_14"),
            "Identifier4",
            Set.of("Identifier4"),
            2,
            List.of(opi4, opiReassign2, opiReassign3),
            List.of(opiReassign2, opiReassign3),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_14"),
            "Identifier4",
            Set.of("Identifier4"),
            4,
            List.of(opi3, opi4, opi9, opiReassign3),
            List.of(opiReassign3),
            reassignmentDateTime
        );

        // OPI_5
        checkUnit(
            unitByTitle.get("Unit_15"),
            "Identifier1",
            Set.of("Identifier1"),
            0,
            List.of(opi5),
            emptyList(),
            initDateTime
        );
        checkUnit(
            unitByTitle.get("Unit_16"),
            "Identifier1",
            Set.of("Identifier1", "Identifier2", "Identifier4"),
            0,
            List.of(opi5),
            emptyList(),
            initDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_16"),
            "Identifier1",
            Set.of("Identifier1", "Identifier2", "Identifier4"),
            2,
            List.of(opi5, opi6),
            emptyList(),
            initDateTime
        );

        // OPI_6
        checkUnit(
            unitByTitle.get("Unit_17"),
            "Identifier1",
            Set.of("Identifier1"),
            0,
            List.of(opi6),
            emptyList(),
            initDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_17"),
            "Identifier1",
            Set.of("Identifier1"),
            1,
            List.of(opi6),
            emptyList(),
            initDateTime
        );

        // OPI_7
        checkUnit(
            unitByTitle.get("Unit_18"),
            "Identifier3",
            Set.of("Identifier1", "Identifier2", "Identifier3", "Identifier4"),
            3,
            List.of(opi7, opiReassign4, opiReassign5, opiReassign6),
            List.of(opiReassign4, opiReassign5, opiReassign6),
            reassignmentDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_18"),
            "Identifier3",
            Set.of("Identifier1", "Identifier2", "Identifier3", "Identifier4"),
            4,
            List.of(opi7, opiReassign4, opiReassign5, opiReassign6),
            List.of(opiReassign4, opiReassign5, opiReassign6),
            reassignmentDateTime
        );

        // OPI_8
        checkUnit(
            unitByTitle.get("Unit_19"),
            "Identifier1",
            Set.of("Identifier1"),
            0,
            List.of(opi8),
            emptyList(),
            initDateTime
        );
        checkOG(
            ogByUnitTitle.get("Unit_19"),
            "Identifier1",
            Set.of("Identifier1"),
            2,
            List.of(opi8, opi12),
            emptyList(),
            initDateTime
        );
    }

    private void checkOG(
        ObjectNode og,
        String sp,
        Set<String> sps,
        int version,
        List<String> ops,
        List<String> reassignmentOps,
        String lastUpdateDateTime
    ) throws InvalidParseOperationException, LogbookClientException {
        // Get LFC
        LogbookLifecycle lfc;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            lfc = JsonHandler.getFromJsonNode(
                client.getRawObjectGroupLifeCycleById(og.get(VitamFieldsHelper.id()).asText()),
                LogbookLifecycle.class
            );
        }

        // Check metadata
        checkMetadata(og, lfc, sp, sps, version, ops, reassignmentOps, lastUpdateDateTime);
    }

    private void checkUnit(
        ObjectNode unit,
        String sp,
        Set<String> sps,
        int version,
        List<String> ops,
        List<String> reassignmentOps,
        String lastUpdateDateTime
    ) throws InvalidParseOperationException, LogbookClientException {
        // Get LFC
        LogbookLifecycle lfc;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            lfc = JsonHandler.getFromJsonNode(
                client.getRawUnitLifeCycleById(unit.get(VitamFieldsHelper.id()).asText()),
                LogbookLifecycle.class
            );
        }

        // Check metadata
        checkMetadata(unit, lfc, sp, sps, version, ops, reassignmentOps, lastUpdateDateTime);
    }

    private void checkMetadata(
        ObjectNode metadata,
        LogbookLifecycle lfc,
        String sp,
        Set<String> sps,
        int version,
        List<String> ops,
        List<String> reassignmentOps,
        String lastUpdateDateTime
    ) throws InvalidParseOperationException {
        // SP & SPS
        assertThat(
            metadata.has(VitamFieldsHelper.originatingAgency())
                ? metadata.get(VitamFieldsHelper.originatingAgency()).asText()
                : null
        ).isEqualTo(sp);
        List<String> originatingAgencies = metadata.has(VitamFieldsHelper.originatingAgencies())
            ? JsonHandler.getFromJsonNode(
                metadata.get(VitamFieldsHelper.originatingAgencies()),
                new TypeReference<>() {}
            )
            : emptyList();
        assertThat(originatingAgencies).containsExactlyInAnyOrderElementsOf(sps);

        // Version
        assertThat(metadata.get(VitamFieldsHelper.version()).asInt()).isEqualTo(version);

        // Operations
        List<String> operationIds = JsonHandler.getFromJsonNode(
            metadata.get(VitamFieldsHelper.operations()),
            new TypeReference<>() {}
        );
        assertThat(operationIds).containsExactlyInAnyOrderElementsOf(ops);

        // Update dates
        assertThat(metadata.get(VitamFieldsHelper.approximateUpdateDate()).asText()).isEqualTo(lastUpdateDateTime);
        //assertThat(unit.get(VitamFieldsHelper.graph_last_persisted_date()).asText()).isEqualTo(lastUpdateDateTime);

        // Check reassignments
        List<ReassignmentOperation> reassignmentOperations = metadata.has(VitamFieldsHelper.reassignments())
            ? JsonHandler.getFromJsonNode(metadata.get(VitamFieldsHelper.reassignments()), new TypeReference<>() {})
            : emptyList();
        for (ReassignmentOperation reassignmentOperation : reassignmentOperations) {
            assertThat(reassignmentOperation.getSourceOriginatingAgency()).isNotNull();
            assertThat(reassignmentOperation.getTargetOriginatingAgency()).isNotNull();
            assertThat(reassignmentOperation.getReassignmentDate()).isEqualTo(lastUpdateDateTime);
        }
        assertThat(
            reassignmentOperations.stream().map(ReassignmentOperation::getOperationId)
        ).containsExactlyElementsOf(reassignmentOps);

        // Check LFCs
        assertThat(
            lfc.getEvents().stream().map(LogbookEvent::getEvIdProc).distinct()
        ).containsExactlyInAnyOrderElementsOf(ops);
    }

    private void checkAccessionRegisterDetails(
        String originatingAgency,
        int ingestedUnits,
        int deletedUnits,
        int ingestedOGs,
        int deletedOGs,
        int ingestedObjects,
        int deletedObjets,
        long ingestedObjectSize,
        long deletedObjectSite,
        String... operationIds
    ) throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException {
        AccessionRegisterDetailModel detail = getAccessionRegisterDetail(operationIds[0]);
        assertThat(detail.getOriginatingAgency()).isEqualTo(originatingAgency);
        assertThat(detail.getTotalUnits().getIngested()).isEqualTo(ingestedUnits);
        assertThat(detail.getTotalUnits().getDeleted()).isEqualTo(deletedUnits);
        assertThat(detail.getTotalObjectsGroups().getIngested()).isEqualTo(ingestedOGs);
        assertThat(detail.getTotalObjectsGroups().getDeleted()).isEqualTo(deletedOGs);
        assertThat(detail.getTotalObjects().getIngested()).isEqualTo(ingestedObjects);
        assertThat(detail.getTotalObjects().getDeleted()).isEqualTo(deletedObjets);
        assertThat(detail.getObjectSize().getIngested()).isEqualTo(ingestedObjectSize);
        assertThat(detail.getObjectSize().getDeleted()).isEqualTo(deletedObjectSite);
        assertThat(detail.getOpi()).isEqualTo(operationIds[0]);
        assertThat(detail.getEvents().stream().map(RegisterValueEventModel::getOperation)).containsExactly(
            operationIds
        );
    }

    private void assertAccessionRegisterSummaryStats(
        String originatingAgency,
        int ingestedUnits,
        int deletedUnits,
        int ingestedOGs,
        int deletedOGs,
        int ingestedObjects,
        int deletedObjets,
        long ingestedObjectSize,
        long deletedObjectSite
    )
        throws ReferentialException, InvalidCreateOperationException, InvalidParseOperationException, AccessUnauthorizedException {
        AccessionRegisterSummaryModel summary = getAccessionRegisterSummary(originatingAgency);
        assertThat(summary.getTotalUnits().getIngested()).isEqualTo(ingestedUnits);
        assertThat(summary.getTotalUnits().getDeleted()).isEqualTo(deletedUnits);
        assertThat(summary.getTotalObjectsGroups().getIngested()).isEqualTo(ingestedOGs);
        assertThat(summary.getTotalObjectsGroups().getDeleted()).isEqualTo(deletedOGs);
        assertThat(summary.getTotalObjects().getIngested()).isEqualTo(ingestedObjects);
        assertThat(summary.getTotalObjects().getDeleted()).isEqualTo(deletedObjets);
        assertThat(summary.getObjectSize().getIngested()).isEqualTo(ingestedObjectSize);
        assertThat(summary.getObjectSize().getDeleted()).isEqualTo(deletedObjectSite);
    }

    private String eliminationAction(ObjectNode query) throws AccessInternalClientServerException {
        try (AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient()) {
            GUID eliminationActionOperationGuid = newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(eliminationActionOperationGuid);
            EliminationRequestBody eliminationRequestBody = new EliminationRequestBody("2020-01-01", query);
            RequestResponse<JsonNode> actionResult = accessInternalClient.startEliminationAction(
                eliminationRequestBody
            );
            assertThat(actionResult.isOk()).isTrue();

            awaitForWorkflowTerminationWithStatus(eliminationActionOperationGuid, StatusCode.OK);
            return eliminationActionOperationGuid.getId();
        }
    }

    private String transferRequest(ObjectNode query) throws AccessInternalClientServerException {
        TransferRequest transferRequest = new TransferRequest(new DataObjectVersions(), query, false);
        transferRequest.setMaxSizeThreshold(10_000_000L);
        transferRequest.setSedaVersion(SupportedSedaVersions.SEDA_2_3.getVersion());

        TransferRequestParameters transferRequestParameters = new TransferRequestParameters();
        transferRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        transferRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        transferRequestParameters.setOriginatingAgencyIdentifier("RATP");
        transferRequestParameters.setSubmissionAgencyIdentifier("RATP");
        transferRequest.setTransferRequestParameters(transferRequestParameters);

        ExportRequest exportRequest = ExportRequest.from(transferRequest);

        GUID transferOperation = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(transferOperation);

        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);
            awaitForWorkflowTerminationWithStatus(transferOperation, StatusCode.OK);
            return transferOperation.getId();
        }
    }

    private String deleteGotVersions(DeleteGotVersionsRequest deleteGotVersionsRequest)
        throws AccessInternalClientServerException {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            final RequestResponse<JsonNode> actionResult = accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, OK);
            return operationGuid.getId();
        } catch (AssertionError e) {
            printDebutInformation(operationGuid.toString());
            throw e;
        }
    }

    private String preservation(SelectMultiQuery preservationSelectQuery)
        throws AccessInternalClientServerException, IOException, InvalidParseOperationException, AdminManagementClientServerException {
        // Setup preservation
        File griffinsExecFolder = PropertiesUtils.getResourceFile("preservation" + File.separator);
        VitamConfiguration.setVitamGriffinExecFolder(griffinsExecFolder.getAbsolutePath());
        File griffinFolder = tempFolder.newFolder("griffinInput");
        VitamConfiguration.setVitamGriffinInputFilesFolder(griffinFolder.getAbsolutePath());

        Path griffinExecutable = griffinsExecFolder.toPath().resolve("griffin-libreoffice/griffin");
        boolean griffinIsExecutable = griffinExecutable.toFile().setExecutable(true);
        if (!griffinIsExecutable) {
            throw new VitamRuntimeException("Wrong execution right for griffin-libreoffice/griffin.");
        }

        Path griffinExtractionAuExecutable = griffinsExecFolder.toPath().resolve("griffin-extraction-au/griffin");
        boolean griffinExtractAuIsExecutable = griffinExtractionAuExecutable.toFile().setExecutable(true);
        if (!griffinExtractAuIsExecutable) {
            throw new VitamRuntimeException("Wrong execution right for griffin-extraction-au/griffin.");
        }

        importGriffins();

        importPreservationScenarios();

        PreservationRequest preservationRequest = new PreservationRequest(
            preservationSelectQuery.getFinalSelect(),
            "PSC-000001",
            "BinaryMaster",
            FIRST,
            "BinaryMaster"
        );
        String operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            buildAndSavePreservationResultFile(BINARY_MASTER.getName());
            accessClient.startPreservation(preservationRequest);
            waitOperation(NB_TRY, SLEEP_TIME, operationGuid);
            verifyOperation(operationGuid, OK);
            return operationGuid;
        } catch (AssertionError e) {
            printDebutInformation(operationGuid);
            throw e;
        }
    }

    private void importGriffins()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setRequestId(newGUID());
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            File resourceFile = PropertiesUtils.getResourceFile("preservation/griffins.json");
            adminClient.importGriffins(JsonHandler.getFromFileAsTypeReference(resourceFile, new TypeReference<>() {}));
        }
    }

    private void importPreservationScenarios()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setRequestId(newGUID());
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            File resourceFile = PropertiesUtils.getResourceFile("preservation/scenarios.json");
            VitamThreadUtils.getVitamSession().setRequestId(newGUID());
            adminClient.importPreservationScenarios(
                JsonHandler.getFromFileAsTypeReference(resourceFile, new TypeReference<>() {})
            );
        }
    }

    private void buildAndSavePreservationResultFile(String sourceUsageName)
        throws IOException, InvalidParseOperationException {
        Map<String, String> objectIdsToFormat = getAllBinariesIds(sourceUsageName);

        ResultPreservation resultPreservation = new ResultPreservation();

        resultPreservation.setId("batchId");
        resultPreservation.setRequestId(getVitamSession().getRequestId());

        Map<String, List<OutputPreservation>> values = new HashMap<>();

        for (Map.Entry<String, String> entry : objectIdsToFormat.entrySet()) {
            List<OutputPreservation> outputPreservationList = new ArrayList<>();
            for (ActionTypePreservation action : singletonList(GENERATE)) {
                OutputPreservation outputPreservation = new OutputPreservation();

                outputPreservation.setStatus(PreservationStatus.OK);
                outputPreservation.setAnalyseResult("VALID_ALL");
                outputPreservation.setAction(action);

                outputPreservation.setInputPreservation(new InputPreservation(entry.getKey(), entry.getValue()));
                outputPreservation.setOutputName("GENERATE-" + entry.getKey() + ".pdf");
                outputPreservationList.add(outputPreservation);
            }

            values.put(entry.getKey(), outputPreservationList);
        }

        resultPreservation.setOutputs(values);
        Path griffinIdDirectory = tempFolder.newFolder("griffinInput", "griffin-libreoffice").toPath();
        writeAsFile(resultPreservation, griffinIdDirectory.resolve("result.json").toFile());
    }

    private Map<String, String> getAllBinariesIds(String sourceUsageName) {
        List<ObjectGroupResponse> objectModelsForUnitResults = getAllObjectModels();

        Map<String, String> allObjectIds = new HashMap<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {
            Optional<VersionsModel> versionsModelOptional = objectGroup.getFirstVersionsModel(sourceUsageName);
            versionsModelOptional.ifPresent(
                model -> allObjectIds.put(model.getId(), model.getFormatIdentification().getFormatId())
            );
        }
        return allObjectIds;
    }

    private List<ObjectGroupResponse> getAllObjectModels() {
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = client.selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeReference(results.toString(), new TypeReference<>() {});
        } catch (VitamException | InvalidFormatException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ArrayNode selectUnits(String... operationIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException {
        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.and().add(QueryHelper.in(VitamFieldsHelper.initialOperation(), operationIds)));
        return (ArrayNode) metaDataClient.selectUnits(select.getFinalSelect()).get(TAG_RESULTS);
    }

    private ArrayNode selectObjectGroups(String... operationIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException {
        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.and().add(QueryHelper.in(VitamFieldsHelper.initialOperation(), operationIds)));
        return (ArrayNode) metaDataClient.selectObjectGroups(select.getFinalSelect()).get(TAG_RESULTS);
    }

    private ObjectNode queryByOpi(String... operationIds) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.initialOperation(), operationIds));
        return select.getFinalSelect();
    }

    private ObjectNode queryByOpiAndTag(String operationId, String... tags) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(
            QueryHelper.and()
                .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), operationId), QueryHelper.in("Tag", tags))
        );
        return select.getFinalSelect();
    }

    private AccessionRegisterDetailModel getAccessionRegisterDetail(String opi)
        throws InvalidParseOperationException, ReferentialException, InvalidCreateOperationException {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(AccessionRegisterDetailModel.OPI, opi));
            ObjectNode query = select.getFinalSelect();
            return (
                (RequestResponseOK<AccessionRegisterDetailModel>) client.getAccessionRegisterDetail(query)
            ).getFirstResult();
        }
    }

    private AccessionRegisterSummaryModel getAccessionRegisterSummary(String originatingAgency)
        throws InvalidParseOperationException, ReferentialException, AccessUnauthorizedException, InvalidCreateOperationException {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(AccessionRegisterSummaryModel.ORIGINATING_AGENCY, originatingAgency));
            return (
                (RequestResponseOK<AccessionRegisterSummaryModel>) client.getAccessionRegister(select.getFinalSelect())
            ).getFirstResult();
        }
    }
}
