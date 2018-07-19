/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.functional.administration.context.core;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.context.api.ContextService;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.security.profile.core.SecurityProfileService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ContextServiceImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "Context";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;
    private static FunctionalBackupService functionalBackupService;

    private static ElasticsearchTestConfiguration esConfig = null;
    private final static String HOST_NAME = "127.0.0.1";
    private final static String CLUSTER_NAME = "vitam-cluster";
    static Map<Integer, List<String>> externalIdentifiers;

    static ContextService contextService;

    static SecurityProfileService securityProfileService;
    static ContractService<IngestContractModel> ingestContractService;
    static ContractService<AccessContractModel> accessContractService;
    static int mongoPort;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        File tmpFolder = tempFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tmpFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongoPort = 12346;
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));

        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        try {
            esConfig = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));
        ElasticsearchAccessAdminFactory.create(CLUSTER_NAME, esNodes);

        final List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        tenants.add(new Integer(EXTERNAL_TENANT));
        VitamConfiguration.setTenants(tenants);
        VitamConfiguration.setAdminTenant(TENANT_ID);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("CONTEXT");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
        LogbookOperationsClientFactory.changeMode(null);

        securityProfileService = mock(SecurityProfileService.class);
        String securityProfileIdentifier = "SEC_PROFILE-000001";
        when(securityProfileService.findOneByIdentifier(securityProfileIdentifier)).thenReturn(
            Optional.of(new SecurityProfileModel("guid", securityProfileIdentifier, "SEC PROFILE", true,
                Collections.EMPTY_SET)));

        functionalBackupService = mock(FunctionalBackupService.class);
        ingestContractService = mock(ContractService.class);
        accessContractService = mock(ContractService.class);

        contextService =
            new ContextServiceImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
                vitamCounterService, ingestContractService, accessContractService, securityProfileService,
                functionalBackupService);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        contextService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
        reset(ingestContractService);
        reset(functionalBackupService);
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

    }

    @Test
    @RunWithCustomExecutor
    public void givenContextImportAndUpdateTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileIngest = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> ingestContractModels =
            JsonHandler.getFromFileAsTypeRefence(fileIngest, new TypeReference<List<IngestContractModel>>() {
            });
        String INGEST_CONTRACT_ID = "IC-000001";
        when(ingestContractService.findByIdentifier(INGEST_CONTRACT_ID)).thenReturn(ingestContractModels.get(0));

        final File fileContexts = PropertiesUtils.getResourceFile("contexts_empty.json");
        final List<ContextModel> ModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
            });

        RequestResponse<ContextModel> response = contextService.createContexts(ModelList);
        assertThat(response.isOk()).isTrue();

        verifyZeroInteractions(ingestContractService);
        verifyZeroInteractions(accessContractService);

        verify(functionalBackupService).saveCollectionAndSequence(any(),
            Mockito.eq(ContextServiceImpl.CONTEXTS_BACKUP_EVENT), Mockito.eq(FunctionalAdminCollections.CONTEXT), any());
        verifyNoMoreInteractions(functionalBackupService);
        reset(functionalBackupService);

        final ObjectNode permissionsNode = JsonHandler.createObjectNode();
        final ObjectNode permissionNode = JsonHandler.createObjectNode();
        permissionNode.put("tenant", TENANT_ID);

        permissionNode.set("IngestContracts", JsonHandler.createArrayNode().add(INGEST_CONTRACT_ID));
        permissionNode.set("AccessContracts", JsonHandler.createArrayNode());
        permissionsNode.set("Permissions", JsonHandler.createArrayNode().add(permissionNode));
        final SetAction setPermission = UpdateActionHelper.set(permissionsNode);
        final Select select = new Select();
        select.setQuery(eq("Name", "My_Context_1"));
        final ContextModel context =
            contextService
                .findContexts(select.getFinalSelect())
                .getDocuments(Context.class, ContextModel.class).get(0);


        final Update update = new Update();
        update.addActions(setPermission);
        update.setQuery(and().add(eq("Permissions.tenant", 0))
            .add(eq("#id", context.getId())));

        JsonNode queryDslForUpdate = update.getFinalUpdate();
        final RequestResponse<ContextModel> updateResponse =
            contextService.updateContext(context.getIdentifier(), queryDslForUpdate);

        assertTrue(updateResponse.isOk());

        verify(functionalBackupService).saveCollectionAndSequence(any(),
            Mockito.eq(ContextServiceImpl.CONTEXTS_BACKUP_EVENT), Mockito.eq(FunctionalAdminCollections.CONTEXT), any());
        verifyNoMoreInteractions(functionalBackupService);
        reset(functionalBackupService);

        verify(ingestContractService).findByIdentifier(INGEST_CONTRACT_ID);
        reset(ingestContractService);

        String INVALID_INGEST_CONTRACT_IDENTIFIER = "IC-000001500000";
        permissionNode.set("IngestContracts", JsonHandler.createArrayNode().add(INVALID_INGEST_CONTRACT_IDENTIFIER));
        permissionsNode.set("Permissions", JsonHandler.createArrayNode().add(permissionNode));
        final SetAction setInvalidPermission = UpdateActionHelper.set(permissionsNode);
        update.getActions().clear();
        update.addActions(setInvalidPermission);
        final RequestResponse<ContextModel> updateError =
            contextService.updateContext(context.getIdentifier(), update.getFinalUpdate());
        assertFalse(updateError.isOk());

        verifyZeroInteractions(functionalBackupService);

        verify(ingestContractService).findByIdentifier(INVALID_INGEST_CONTRACT_IDENTIFIER);
        verifyZeroInteractions(ingestContractService);
    }


    @Test
    @RunWithCustomExecutor
    public void givenContextImportAndDeleteTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileIngest = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> ingestContractModels =
                JsonHandler.getFromFileAsTypeRefence(fileIngest, new TypeReference<List<IngestContractModel>>() {
                });
        String INGEST_CONTRACT_ID = "IC-000001";
        when(ingestContractService.findByIdentifier(INGEST_CONTRACT_ID)).thenReturn(ingestContractModels.get(0));

        final File fileContexts = PropertiesUtils.getResourceFile("contexts_empty.json");
        final List<ContextModel> ModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
                });

        RequestResponse<ContextModel> response = contextService.createContexts(ModelList);
        assertThat(response.isOk()).isTrue();

        final Select select = new Select();
        select.setQuery(eq("Name", "My_Context_1"));
        final ContextModel context =
                contextService
                        .findContexts(select.getFinalSelect())
                        .getDocuments(Context.class, ContextModel.class).get(0);

        reset(functionalBackupService);

        final RequestResponse<ContextModel> deleteError = contextService.deleteContext(context.getIdentifier());
        assertTrue(deleteError.isOk());
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(any(), any(), any(), any());
    }


    @Test
    @RunWithCustomExecutor
    public void givenContextImportedWithInvalidIngestContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String INGEST_CONTRACT_ID = "NON-EXISTING-CONTRACT";
        when(ingestContractService.findByIdentifier(INGEST_CONTRACT_ID)).thenReturn(null);

        final File fileContexts = PropertiesUtils.getResourceFile("KO_contexts_invalid_contract.json");
        final List<ContextModel> ModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
                });

        RequestResponse<ContextModel> response = contextService.createContexts(ModelList);
        assertThat(response.isOk()).isFalse();

        verifyZeroInteractions(accessContractService);
        verifyZeroInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);

        final File fileContexts = PropertiesUtils.getResourceFile("contexts_empty_1.json");
        final List<ContextModel> ModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
            });

        RequestResponse<ContextModel> response = contextService.createContexts(ModelList);
        assertThat(response.isOk()).isTrue();

        verify(functionalBackupService).saveCollectionAndSequence(any(),
            Mockito.eq(ContextServiceImpl.CONTEXTS_BACKUP_EVENT), Mockito.eq(FunctionalAdminCollections.CONTEXT), any());
        verifyNoMoreInteractions(functionalBackupService);

        verifyZeroInteractions(ingestContractService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContexts = PropertiesUtils.getResourceFile("contexts_empty.json");
        final List<ContextModel> ModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
            });

        RequestResponse<ContextModel> response = contextService.createContexts(ModelList);
        assertThat(response.isOk()).isFalse();

        verifyZeroInteractions(ingestContractService);
        verifyZeroInteractions(functionalBackupService);
    }
}
