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
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerConfiguration;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MassUpdateIT extends VitamRuleRunner {

    private static final String TITLE = "Title";
    private static final String TITLE_ = "Title_";
    private static final String EV_ID_PROC = "evIdProc";
    private static final String STP_MASS_UPDATE_UNIT = "STP_MASS_UPDATE_UNIT";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON =
        "integration-processing/mass-update/unit_00.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_01_JSON =
        "integration-processing/mass-update/unit_01.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_02_JSON =
        "integration-processing/mass-update/unit_02.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_03_JSON =
        "integration-processing/mass-update/unit_03.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_04_JSON =
        "integration-processing/mass-update/unit_04.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON =
        "integration-processing/mass-update/unit_05.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_06_JSON =
        "integration-processing/mass-update/unit_06.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON =
        "integration-processing/mass-update/unit_07.json";
    private static final String MASS_UPDATE_QUERY_KO = "integration-processing/mass-update/update_query_KO.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_01_JSON =
        "integration-processing/mass-update/update_query_01.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON =
        "integration-processing/mass-update/update_query_02.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_03_JSON =
        "integration-processing/mass-update/update_query_03.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_04_JSON =
        "integration-processing/mass-update/update_query_04.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_05_JSON =
        "integration-processing/mass-update/update_query_05.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON =
        "integration-processing/mass-update/update_query_06.json";
    private static final String QUERY = "query.json";
    private static final String ACTION = "actions.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON =
        "integration-processing/mass-update/unit_lfc_00.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON =
        "integration-processing/mass-update/unit_lfc_01.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_02_JSON =
        "integration-processing/mass-update/unit_lfc_02.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_04_JSON =
        "integration-processing/mass-update/unit_lfc_04.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON =
        "integration-processing/mass-update/unit_lfc_05.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_06_JSON =
        "integration-processing/mass-update/unit_lfc_06.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON =
        "integration-processing/mass-update/unit_lfc_07.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_ADD_RULE =
        "integration-processing/mass-update/Action_add_rules.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_RULE =
        "integration-processing/mass-update/Action_update_rules.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_DELETE_RULE =
        "integration-processing/mass-update/Action_delete_rules.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_COMPLEX_ADD_UPDATE_DELETE_RULES =
        "integration-processing/mass-update/Action_complex_add_update_delete_rules.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_RULE =
        "/integration-processing/mass-update/rules_referential.csv";
    public static final String EXPECTED_UNIT_7_MGT_AFTER_COMPLEX_ADD_UPDATE_DELETE_RULES_JSON =
        "integration-processing/mass-update/expected_unit7_mgt_after_complex_add_update_delete_rules.json";
    private static final String CONTEXT_ID = "fakeContextId";
    private static final String CONTRACT_ID = "AC-000001";
    private static final int TENANT_0 = 0;
    private static final String RESULTS = "$results";
    private static final String EVENTS = "events";
    private static final String OUT_DETAIL = "outDetail";
    private static final String MGT = "_mgt";
    private static final String REUSERULE = "ReuseRule";
    private static final String RULES = "Rules";
    private static final String RULE = "Rule";
    private static final String STARTDATE = "StartDate";

    private static final String UNIT_ID = "aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_ADD_PREVENT_RULES =
        "integration-processing/mass-update/action_add_prevent_rules_id.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_DELETE_PREVENT_RULES =
        "integration-processing/mass-update/action_delete_prevent_rules_id.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_BAD_DELETE_PREVENT_RULES =
        "integration-processing/mass-update/action_bad_delete_prevent_rules_id.json";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_BAD_ADD_PREVENT_RULES =
        "integration-processing/mass-update/action_bad_add_prevent_rules_id.json";

    private static final String EXPECTED_UNIT_MGT_AFTER_ADD_PREVENT_RULES_JSON =
        "integration-processing/mass-update/expected_unit7_after_add_prevent_rules_id.json";

    private static final String EXPECTED_UNIT_MGT_AFTER_DELETE_PREVENT_RULES_JSON =
        "integration-processing/mass-update/expected_unit7_after_delete_prevent_rules_id.json";

    private static final String EXPECTED_UNIT_MGT_AFTER_BAD_UPDATE_PREVENT_RULES_JSON =
        "integration-processing/mass-update/expected_unit7_after_bad_update_prevent_rules_request.json";

    private static final String UNIT_ID_UPDATE = "aeaqaaaaaagbcaacaang6ak2ds5paliaaaba";

    private static final String MASS_UPDATE_OK = "MASS_UPDATE_FINALIZE.OK";

    private static final String MASS_UPDATE_UNIT_RULE_KO = "MASS_UPDATE_UNIT_RULE.KO";

    private static final String RULES_FILE = "Rules.json";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProcessingIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    private static final TypeReference<List<Document>> TYPE_LIST_UNIT = new TypeReference<>() {};

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        PropertiesUtils.getResourcePath("integration-processing/bigworker.conf");
        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);
        processMonitoring = ProcessMonitoringImpl.getInstance();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-processing").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

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

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_WARNING_when_nothing_to_update() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<Document> units = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON),
            TYPE_LIST_UNIT
        );
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_01_JSON)
        ); // <- no units matching this query
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus()); // <- workflow WARNING
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_WARNING_when_at_least_one_KO_occurs() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setCapacity(2);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert 2 units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_CHECK_AND_COMPUTE");
        // delete unit to create a KO
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList(UNIT_ID), TENANT_0);

        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_UNITS", StatusCode.KO.name());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID("aeaqaaaaaagbcaacaang6ak4ts6paliaaaas", TENANT_0);
        assertTrue(updatedUnit.isPresent());
        String expected = "update old title sous fonds \"émouvant สวัสดี";
        assertThat(updatedUnit.get()).containsEntry(TITLE, expected);

        Optional<Document> deletedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID(UNIT_ID, TENANT_0);
        assertTrue(deletedUnit.isEmpty());
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_WARNING_when_at_least_one_KO_occurs_by_lots() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setCapacity(1);
        VitamConfiguration.setWorkerBulkSize(1);
        VitamConfiguration.setDistributeurBatchSize(1);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_05_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_05_JSON
        );
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_06_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_06_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        VitamTestHelper.runStepByStepUntilStepReached(containerName, "STP_CHECK_AND_COMPUTE");
        // delete unit to create a KO
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList(UNIT_ID), TENANT_0);
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .delete(Collections.singletonList("aeaqaaaaaagbcaacaang6ak4ts6paliaaaah"), TENANT_0);

        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_UNITS", StatusCode.KO.name());
        VitamTestHelper.verifyLogbook(containerName, "MASS_UPDATE_FINALIZE", StatusCode.WARNING.name());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID("aeaqaaaaaagbcaacaang6ak4ts6paliaaaas", TENANT_0);
        assertTrue(updatedUnit.isPresent());
        String expected = "update old title sous fonds \"émouvant สวัสดี";
        assertThat(updatedUnit.get()).containsEntry(TITLE, expected);

        Optional<Document> deletedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID(UNIT_ID, TENANT_0);
        assertTrue(deletedUnit.isEmpty());
    }

    @RunWithCustomExecutor
    @Test
    public void should_update_workflow_KO_when_classification_error() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<Document> units = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON),
            TYPE_LIST_UNIT
        );
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        // When
        JsonNode query = JsonHandler.getFromFile(PropertiesUtils.findFile(MASS_UPDATE_QUERY_KO));
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );

        // Then
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void updateWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_02_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID(UNIT_ID, TENANT_0);
        assertTrue(updatedUnit.isPresent());
        String expected = "update old title sous fonds \"émouvant สวัสดี";

        assertThat(updatedUnit.get()).containsEntry(TITLE, expected);
        assertThat(updatedUnit.get().getList(Unit.OPS, String.class)).contains(operationGuid.getId());

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
        JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
        JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);
        verifyEvent(events, "MASS_UPDATE_UNIT_DESC.OK");

        LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        JsonNode unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(
            updatedUnit.get().get("_id", String.class),
            new Select().getFinalSelectById()
        );

        JsonNode lfcEvents = unitLfc.get("$results").get(0).get("events");
        final JsonNode lastEvent = lfcEvents.get(lfcEvents.size() - 1);
        JsonNode evDetData = lastEvent.get("evDetData");
        JsonNode jsoned = JsonHandler.getFromString(evDetData.textValue());

        // Test bug 5490 JsonHandler.getFromString(expected) will return decoded string
        //expected = "update old title sous fonds \\\"émouvant สวัสดี";
        expected = "update old title sous fonds \\\"\\u00E9mouvant \\u0E2A\\u0E27\\u0E31\\u0E2A\\u0E14\\u0E35";

        assertThat(jsoned.get("diff").textValue()).contains(expected);

        // Ensure logbook operation does not contain evDetData for distributed step STP_UPDATE / action MASS_UPDATE_UNITS
        LogbookOperation logbookOperation = getLogbookInformation(containerName);
        List<LogbookEventOperation> updateEvents = logbookOperation
            .getEvents()
            .stream()
            .filter(ev -> ev.getOutDetail().equals("MASS_UPDATE_UNITS.OK"))
            .collect(Collectors.toList());
        assertThat(updateEvents).hasSize(1);
        assertThat(updateEvents.get(0).getEvDetData()).isEqualTo("{}");
    }

    private void verifyEvent(JsonNode events, String s) {
        List<JsonNode> massUpdateFinalized = events
            .findValues(OUT_DETAIL)
            .stream()
            .filter(e -> e.asText().equals(s))
            .collect(Collectors.toList());
        assertThat(massUpdateFinalized).isNotEmpty();
    }

    @RunWithCustomExecutor
    @Test
    public void updateByPatternWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_01_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON
        );

        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_02_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_02_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_03_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
        assertTrue(updatedUnit.isPresent());
        assertThat(updatedUnit.get()).containsEntry(TITLE, "Reportage photographique juillet n°17642");

        Optional<Document> updatedUnit2 = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID("aeaqaaaaaagbcaacaang6ak4ts6paliccccq", TENANT_0);
        assertTrue(updatedUnit2.isPresent());
        assertThat(updatedUnit2.get()).containsEntry(
            TITLE,
            "Le Reportage photographique juillet n°17642 est réalisé en 1789 sous le titre: Reportage photographique juillet n°17642"
        );

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
        JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
        JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);
        verifyEvent(events, "MASS_UPDATE_UNIT_DESC.OK");

        JsonNode logbookResult2 = logbookClient.selectOperation(selectQuery.getFinalSelect());
        events = logbookResult2.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);
        verifyEvent(events, "MASS_UPDATE_UNIT_DESC.OK");
    }

    @RunWithCustomExecutor
    @Test
    public void updateTitleWithLanguageWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_01_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_04_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
        assertTrue(updatedUnit.isPresent());
        assertThat(((Document) updatedUnit.get().get(TITLE_))).containsEntry("fr", "Good title");

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
        JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
        JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);
        verifyEvent(events, "MASS_UPDATE_UNIT_DESC.OK");
    }

    @RunWithCustomExecutor
    @Test
    public void should_not_reinsert_lfc_when_redo_massupdate() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId("OUR_FAILING_CONTRACT");
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        final String unitId = "aeaqaaaaaagbcaacaang6ak4ts6paliacabq";
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        // insert units and LFC
        insertUnitAndLFC(
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_04_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_04_JSON
        );

        // import contract
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));

        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        JsonNode query = JsonHandler.getFromFile(
            PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_03_JSON)
        );
        workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
        workspaceClient.putObject(
            operationGuid.getId(),
            ACTION,
            JsonHandler.writeToInpustream(JsonHandler.createObjectNode())
        );
        processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_DESC.name());

        RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
            ProcessAction.RESUME.getValue(),
            containerName
        );
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.FATAL, processWorkflow.getStatus());

        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        ret = processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        Optional<Document> updatedUnit = VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .getByID(unitId, TENANT_0);
        assertTrue(updatedUnit.isPresent());
        assertThat(updatedUnit.get()).containsEntry(TITLE, "Reportage photographique juillet n°17642");

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
        JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());

        JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);

        JsonNode logbookResult2 = logbookClient.selectOperation(selectQuery.getFinalSelect());

        events = logbookResult2.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, MASS_UPDATE_OK);

        LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        JsonNode unitLfc = logbookLifeCyclesClient.selectUnitLifeCycleById(
            updatedUnit.get().get("_id", String.class),
            new Select().getFinalSelectById()
        );

        JsonNode lfcJsonNode = unitLfc.get("$results").get(0);
        LogbookLifecycle lfc = JsonHandler.getFromJsonNode(lfcJsonNode, LogbookLifecycle.class);
        List<LogbookEvent> lfcEvents = lfc.getEvents();
        List<LogbookEvent> updateEvents = lfcEvents
            .stream()
            .filter(e -> "LFC.UNIT_METADATA_UPDATE".equals(e.getEvType()) && containerName.equals(e.getEvIdProc()))
            .collect(Collectors.toList());

        assertEquals(1, updateEvents.size());
    }

    /**
     * insertUnitAndLFC
     *
     * @param unitFile
     * @param lfcFile
     * @throws fr.gouv.vitam.common.exception.InvalidParseOperationException
     * @throws FileNotFoundException
     * @throws DatabaseException
     */
    private void insertUnitAndLFC(final String unitFile, final String lfcFile)
        throws fr.gouv.vitam.common.exception.InvalidParseOperationException, FileNotFoundException, DatabaseException {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<Document> units = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(unitFile),
            TYPE_LIST_UNIT
        );
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
        List<JsonNode> unitsLfc = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(lfcFile),
            new TypeReference<>() {}
        );
        List<Document> lfcs = unitsLfc
            .stream()
            .map(item -> Document.parse(JsonHandler.unprettyPrint(item)))
            .collect(Collectors.toList());

        new VitamMongoRepository(LogbookCollections.LIFECYCLE_UNIT.getCollection()).save(lfcs);
    }

    /**
     * create a logbook operation
     *
     * @param operationId
     * @param objectId
     * @param type
     * @param typeProc
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientAlreadyExistsException
     * @throws LogbookClientServerException
     */
    private void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = STP_MASS_UPDATE_UNIT;
        }
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            type,
            objectId,
            typeProc,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );
        if (STP_MASS_UPDATE_UNIT.equals(type)) {
            initParameters.putParameterValue(
                LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp(STP_MASS_UPDATE_UNIT + ".STARTED") + " : " + operationId
            );
        }
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Test
    public void AddUnitRuleWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_01_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            adminManagementClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_05_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_ADD_RULE)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            assertThat(
                updatedUnit
                    .get()
                    .get(MGT, Document.class)
                    .get(REUSERULE, Document.class)
                    .getList(RULES, Document.class)
                    .get(0)
                    .getString(RULE)
            ).isEqualTo("REU-00001");

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void updateUnitRuleWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_03_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            adminManagementClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_05_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_RULE)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            assertThat(
                updatedUnit
                    .get()
                    .get(MGT, Document.class)
                    .get(REUSERULE, Document.class)
                    .getList(RULES, Document.class)
                    .get(0)
                    .getString(STARTDATE)
            ).isEqualTo("2019-10-11");

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void deleteUnitRuleWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_03_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_01_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            adminManagementClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_05_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_DELETE_RULE)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aeaqaaaaaagbcaacaang6ak4ts6palibbbbq", TENANT_0);
            assertTrue(updatedUnit.isPresent());
            assertThat(
                updatedUnit.get().get(MGT, Document.class).get(REUSERULE, Document.class).getList(RULES, Document.class)
            ).isEmpty();

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void complexAddUpdateDeleteUnitRuleWorkflowTestOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_COMPLEX_ADD_UPDATE_DELETE_RULES)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID(UNIT_ID_UPDATE, TENANT_0);
            assertTrue(updatedUnit.isPresent());
            JsonNode unitJsonNode = BsonHelper.fromDocumentToJsonNode(updatedUnit.get());
            String actualManagementNode = JsonHandler.unprettyPrint(unitJsonNode.get(MGT));
            String expectedManagementNode = PropertiesUtils.getResourceAsString(
                EXPECTED_UNIT_7_MGT_AFTER_COMPLEX_ADD_UPDATE_DELETE_RULES_JSON
            );

            JsonAssert.assertJsonEquals(expectedManagementNode, actualManagementNode);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testAddPreventRulesIdToUnitWorkflowOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_ADD_PREVENT_RULES)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID(UNIT_ID_UPDATE, TENANT_0);
            assertTrue(updatedUnit.isPresent());
            JsonNode unitJsonNode = BsonHelper.fromDocumentToJsonNode(updatedUnit.get());
            String actualManagementNode = JsonHandler.unprettyPrint(unitJsonNode.get(MGT));
            String expectedManagementNode = PropertiesUtils.getResourceAsString(
                EXPECTED_UNIT_MGT_AFTER_ADD_PREVENT_RULES_JSON
            );

            JsonAssert.assertJsonEquals(expectedManagementNode, actualManagementNode);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testDeletePreventRulesIdToUnitWorkflowOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_DELETE_PREVENT_RULES)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID(UNIT_ID_UPDATE, TENANT_0);
            assertTrue(updatedUnit.isPresent());
            JsonNode unitJsonNode = BsonHelper.fromDocumentToJsonNode(updatedUnit.get());
            String actualManagementNode = JsonHandler.unprettyPrint(unitJsonNode.get(MGT));
            String expectedManagementNode = PropertiesUtils.getResourceAsString(
                EXPECTED_UNIT_MGT_AFTER_DELETE_PREVENT_RULES_JSON
            );

            JsonAssert.assertJsonEquals(expectedManagementNode, actualManagementNode);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testDeletePreventRulesIdToUnitWorkflowKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_BAD_DELETE_PREVENT_RULES)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID(UNIT_ID_UPDATE, TENANT_0);
            assertTrue(updatedUnit.isPresent());
            JsonNode unitJsonNode = BsonHelper.fromDocumentToJsonNode(updatedUnit.get());
            String actualManagementNode = JsonHandler.unprettyPrint(unitJsonNode.get(MGT));
            String expectedManagementNode = PropertiesUtils.getResourceAsString(
                EXPECTED_UNIT_MGT_AFTER_BAD_UPDATE_PREVENT_RULES_JSON
            );

            JsonAssert.assertJsonEquals(expectedManagementNode, actualManagementNode);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "MASS_UPDATE_UNIT_RULE.KO");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testAddPreventRulesIdToUnitWorkflowKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        try (AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            final String containerName = operationGuid.getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, operationGuid, null, LogbookTypeProcess.MASS_UPDATE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);

            // insert units and LFC
            insertUnitAndLFC(
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_07_JSON,
                INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_07_JSON
            );

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            functionalClient.importRulesFile(
                getClass().getResourceAsStream(INTEGRATION_PROCESSING_MASS_UPDATE_RULE),
                RULES_FILE
            );

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            JsonNode query = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_UPDATE_QUERY_06_JSON)
            );
            workspaceClient.putObject(operationGuid.getId(), QUERY, JsonHandler.writeToInpustream(query));
            JsonNode action = JsonHandler.getFromFile(
                PropertiesUtils.findFile(INTEGRATION_PROCESSING_MASS_UPDATE_BAD_ADD_PREVENT_RULES)
            );
            workspaceClient.putObject(operationGuid.getId(), ACTION, JsonHandler.writeToInpustream(action));
            processingClient.initVitamProcess(containerName, Contexts.MASS_UPDATE_UNIT_RULE.name());

            RequestResponse<ItemStatus> ret = processingClient.updateOperationActionProcess(
                ProcessAction.RESUME.getValue(),
                containerName
            );
            assertNotNull(ret);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

            waitOperation(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_0);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            Optional<Document> updatedUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID(UNIT_ID_UPDATE, TENANT_0);
            assertTrue(updatedUnit.isPresent());
            JsonNode unitJsonNode = BsonHelper.fromDocumentToJsonNode(updatedUnit.get());
            String actualManagementNode = JsonHandler.unprettyPrint(unitJsonNode.get(MGT));
            String expectedManagementNode = PropertiesUtils.getResourceAsString(
                EXPECTED_UNIT_MGT_AFTER_BAD_UPDATE_PREVENT_RULES_JSON
            );

            JsonAssert.assertJsonEquals(expectedManagementNode, actualManagementNode);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            Select selectQuery = new Select();
            selectQuery.setQuery(QueryHelper.eq(EV_ID_PROC, containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, MASS_UPDATE_UNIT_RULE_KO);
        }
    }

    private LogbookOperation getLogbookInformation(String operationId)
        throws InvalidParseOperationException, LogbookClientException {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode response = logbookClient.selectOperationById(operationId);
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
        }
    }
}
