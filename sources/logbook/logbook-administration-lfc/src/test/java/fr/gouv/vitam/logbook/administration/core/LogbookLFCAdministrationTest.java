package fr.gouv.vitam.logbook.administration.core;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.administration.audit.core.LogbookAuditAdministration;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.operations.core.LogbookOperationsImpl;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogbookLFCAdministrationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String DATABASE_HOST = "localhost";
    private static final Integer TEMPORIZATION_DELAY = 300;
    private static final Integer MAX_ENTRIES = 100_000;
    static LogbookDbAccess mongoDbAccess;
    private static fr.gouv.vitam.common.junit.JunitHelper junitHelper;

    private static WorkspaceClientFactory workspaceClientFactory;
    private static WorkspaceClient workspaceClient;
    private static StorageClientFactory storageClientFactory;
    private static StorageClient storageClient;


    private static ProcessingManagementClientFactory processingManagementClientFactory;
    private static ProcessingManagementClient processingManagementClient;

    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();
    private final static String ES_HOST_NAME = "localhost";

    private static final Integer tenantId = 0;
    static final List<Integer> tenantList = Arrays.asList(0);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
   private LogbookOperationsImpl logbookOperations;


    @BeforeClass
    public static void init() throws IOException, VitamException {
        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), tenantId);

        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        storageClientFactory = mock(StorageClientFactory.class);
        storageClient = mock(StorageClient.class);

        when(storageClientFactory.getClient()).thenReturn(storageClient);

        processingManagementClientFactory = mock(ProcessingManagementClientFactory.class);
        processingManagementClient = mock(ProcessingManagementClient.class);

        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(processingManagementClientFactory.getClient()).willReturn(processingManagementClient);
        junitHelper = JunitHelper.getInstance();

        List<MongoDbNode> nodes = new ArrayList<MongoDbNode>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoRule.getDataBasePort()));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ES_HOST_NAME, ElasticsearchRule.TCP_PORT));
        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, MongoRule.VITAM_DB, ElasticsearchRule.VITAM_CLUSTER, esNodes);
        VitamConfiguration.setTenants(tenantList);
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration, Collections::emptyList);
    }


    @AfterClass
    public static void tearDownAfterClass() {
        LogbookCollections.afterTestClass(true, tenantId);

        mongoDbAccess.close();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void before() {
        reset(storageClient);
        reset(workspaceClient);
        reset(processingManagementClient);
        logbookOperations = new LogbookOperationsImpl(mongoDbAccess, workspaceClientFactory, storageClientFactory, IndexationHelper.getInstance());
    }
    @After
    public void tearDown() {
        LogbookCollections.afterTest(Arrays.asList(LogbookCollections.OPERATION), tenantId);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldGenerateSecure() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        reset(workspaceClient);
        reset(processingManagementClient);
        doNothing().when(workspaceClient).createContainer(anyString());

        doNothing().when(processingManagementClient).initVitamProcess(anyString(), any());
        RequestResponseOK<ItemStatus> req = new RequestResponseOK<ItemStatus>().addResult(new ItemStatus());
        req.setHttpCode(Status.ACCEPTED.getStatusCode());
        when(processingManagementClient.updateOperationActionProcess(anyString(), anyString())).thenReturn(req);

        LogbookLFCAdministration logbookAdministration =
            new LogbookLFCAdministration(logbookOperations, processingManagementClientFactory,
                workspaceClientFactory, TEMPORIZATION_DELAY, MAX_ENTRIES);

        // When
        GUID operationGuid = logbookAdministration.generateSecureLogbookLFC(LfcTraceabilityType.Unit);
        assertNotNull(operationGuid);

        ArgumentCaptor<ProcessingEntry> processingEntryArgumentCaptor = ArgumentCaptor.forClass(ProcessingEntry.class);
        verify(processingManagementClient).initVitamProcess(processingEntryArgumentCaptor.capture());

        assertThat(processingEntryArgumentCaptor.getValue().getWorkflow())
            .isEqualTo("UNIT_LFC_TRACEABILITY");
        assertThat(processingEntryArgumentCaptor.getValue().getExtraParams().
            get(WorkerParameterName.lifecycleTraceabilityTemporizationDelayInSeconds.name())).isEqualTo(
            TEMPORIZATION_DELAY.toString());
        assertThat(processingEntryArgumentCaptor.getValue().getExtraParams().
            get(WorkerParameterName.lifecycleTraceabilityMaxEntries.name())).isEqualTo(
            MAX_ENTRIES.toString());
    }

    @Test
    @RunWithCustomExecutor
    public void shouldGetExceptionWhenWorkspaceIsDown() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        doThrow(new ContentAddressableStorageServerException(""))
            .when(workspaceClient).createContainer(anyString());

        LogbookLFCAdministration logbookAdministration =
            new LogbookLFCAdministration(logbookOperations, processingManagementClientFactory,
                workspaceClientFactory, TEMPORIZATION_DELAY, MAX_ENTRIES);

        try {
            logbookAdministration.generateSecureLogbookLFC(LfcTraceabilityType.Unit);
            fail("should throw an exception");
        } catch (VitamClientException e) {
        }
    }

    @Test
    @RunWithCustomExecutor
    public void traceabilityAuditTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        reset(workspaceClient);
        reset(processingManagementClient);
        doNothing().when(workspaceClient).createContainer(anyString());
        AlertService alertService = mock(AlertService.class);

        doNothing().when(processingManagementClient).initVitamProcess(anyString(), any());
        RequestResponseOK<ItemStatus> req = new RequestResponseOK<ItemStatus>().addResult(new ItemStatus());
        req.setHttpCode(Status.ACCEPTED.getStatusCode());
        when(processingManagementClient.updateOperationActionProcess(anyString(), anyString())).thenReturn(req);

        LogbookLFCAdministration logbookAdministration =
            new LogbookLFCAdministration(logbookOperations, processingManagementClientFactory,
                workspaceClientFactory, TEMPORIZATION_DELAY, MAX_ENTRIES);

        for (int i = 0; i < 23; i++) {
            logbookAdministration.generateSecureLogbookLFC(LfcTraceabilityType.Unit);
            logbookAdministration.generateSecureLogbookLFC(LfcTraceabilityType.ObjectGroup);
        }

        LogbookAuditAdministration logbookAuditAdministration =
            new LogbookAuditAdministration(logbookOperations);
        assertEquals(23,
            logbookAuditAdministration.auditTraceability(Contexts.UNIT_LFC_TRACEABILITY.getEventType(), 1, 24));
        assertEquals(23,
            logbookAuditAdministration.auditTraceability(Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType(), 1, 24));

        logbookAdministration.generateSecureLogbookLFC(LfcTraceabilityType.Unit);

        assertEquals(24,
            logbookAuditAdministration.auditTraceability(Contexts.UNIT_LFC_TRACEABILITY.getEventType(), 1, 24));
        verify(alertService, never()).createAlert(anyString());
        assertEquals(23,
            logbookAuditAdministration.auditTraceability(Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType(), 1, 24));
        verify(alertService, never()).createAlert(anyString());
    }
}
