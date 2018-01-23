/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.reconstruction.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryFactory;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryProvider;
import fr.gouv.vitam.functional.administration.common.impl.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class BackupAndReconstructionIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BackupAndReconstructionIT.class);

    private static final String DEFAULT_OFFER_CONF = "integration-reconstruction/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-reconstruction/logbook.conf";
    private static final String STORAGE_CONF = "integration-reconstruction/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-reconstruction/workspace.conf";
    private static final String ADMIN_MANAGEMENT_CONF =
        "integration-reconstruction/functional-administration.conf";

    private static final String OFFER_FOLDER = "offer";
    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;
    private static final String AGENCY_IDENTIFIER_1 = "FR_ORG_AGEN";
    private static final String AGENCY_IDENTIFIER_2 = "FRAN_NP_005568";
    private static final String INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_1_CSV =
        "integration-reconstruction/data/agencies_1.csv";

    private static final String INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_2_CSV =
        "integration-reconstruction/data/agencies_2.csv";

    private static final String INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON =
        "integration-reconstruction/data/security_profile_1.json";
    private static final String INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_2_JSON =
        "integration-reconstruction/data/security_profile_2.json";

    private static final String SECURITY_PROFILE_IDENTIFIER_1 = "SEC_PROFILE-000001";
    private static final String SECURITY_PROFILE_IDENTIFIER_2 = "SEC_PROFILE-000002";

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;

    private static LogbookMain logbookApplication;

    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferApplication;

    private static AdminManagementMain adminManagementMain;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            FunctionalAdminCollections.SECURITY_PROFILE.getName(), FunctionalAdminCollections.AGENCIES.getName(),
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), FunctionalAdminCollections.SECURITY_PROFILE.getName(),
            FunctionalAdminCollections.AGENCIES.getName());


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        // launch functional Admin server
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF);

        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());

        writeYaml(workspaceConfigFile, workspaceConfiguration);

        workspaceMain = new WorkspaceMain(workspaceConfigFile.getAbsolutePath());
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        // launch logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));

        final File logbookConfigFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookConfiguration =
            PropertiesUtils.readYaml(logbookConfigFile, LogbookConfiguration.class);
        logbookConfiguration.setElasticsearchNodes(nodesEs);
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        logbookApplication = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));


        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realAdminConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(elasticsearchRule.getClusterName());
        realAdminConfig.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(adminConfig, realAdminConfig);

        // prepare functional admin
        adminManagementMain = new AdminManagementMain(adminConfig.getAbsolutePath());
        adminManagementMain.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));

        // prepare offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);

        defaultOfferApplication = new DefaultOfferMain(offerConfig.getAbsolutePath());
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace("http://localhost:" + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        // prepare storage
        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
        storageClient = StorageClientFactory.getInstance().getClient();
    }


    @AfterClass
    public static void afterClass() throws Exception {

        if(workspaceClient != null ) {
            workspaceClient.close();
        }
        if(workspaceMain != null) {
            workspaceMain.stop();
        }
        File offerFolder = new File(OFFER_FOLDER);
        if (offerFolder.exists()) {
            try {
                // if clean offer delete did not work
                FileUtils.cleanDirectory(offerFolder);
                FileUtils.deleteDirectory(offerFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning offer:", e);
            }
        }
        if(storageClient != null) {
            storageClient.close();
        }
        if(defaultOfferApplication != null) {
            defaultOfferApplication.stop();
        }
        if(storageMain != null) {
            storageMain.stop();
        }
        if(adminManagementMain !=null) {
            adminManagementMain.stop();
        }
        if (logbookApplication != null) {
            logbookApplication.stop();
        }
        elasticsearchRule.afterClass();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void tearDown() {
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    /**
     * Test reconstruction of agencies
     * For tenant 0
     * 1. Import one agency using import service
     * 2. Check that imported
     * 3. purge mongo and es
     * 4. Check that purged
     * 5. reconstruct
     * 6. Check that document reconstructed
     * 7. check that initial document is equal to the reconstructed one
     * 8. import agencies containing the first one + an other agency
     * 9. Check that imported two documents
     * 10. purge mongo and es
     * 11. Check that purged
     * 12. reconstruct
     * 13. Check that two documents are reconstructed
     * 14. check that initial documents are equal to the reconstructed documents
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testReconstructionAgenciesOk() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Import 1 document agencies
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_1_CSV), "agencies_1.csv");
        }

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        final VitamMongoRepository agenciesMongo =
            vitamRepository.getVitamMongoRepository(FunctionalAdminCollections.AGENCIES);

        final VitamElasticsearchRepository agenciesEs =
            vitamRepository.getVitamESRepository(FunctionalAdminCollections.AGENCIES);

        Optional<Document> agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo11 = agencyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("agency 1");


        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs11 = agencyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("agency 1");

        agenciesMongo.purge(TENANT_0);
        agenciesEs.purge(TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(vitamRepository,
                new RestoreBackupServiceImpl());

        reconstructionService.reconstruct(FunctionalAdminCollections.AGENCIES, TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo11Reconstructed = agencyDoc.get();
        assertThat(inMogo11Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo11Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs11Reconstructed = agencyDoc.get();
        assertThat(inEs11Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs11Reconstructed.getString("Name")).isEqualTo("agency 1");


        assertThat(inMogo11).isEqualTo(inMogo11Reconstructed);
        assertThat(inEs11).isEqualTo(inEs11Reconstructed);

        // Import 2 documents agencies

        // Create and save some backup files for reconstruction.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_2_CSV), "agencies_2.csv");
        }


        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo12 = agencyDoc.get();
        assertThat(inMogo12.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo12.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs12 = agencyDoc.get();
        assertThat(inEs12.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs12.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo22 = agencyDoc.get();
        assertThat(inMogo22.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inMogo22.getString("Name")).isEqualTo("agency 2");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs22 = agencyDoc.get();
        assertThat(inEs22.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inEs22.getString("Name")).isEqualTo("agency 2");

        agenciesMongo.purge(TENANT_0);
        agenciesEs.purge(TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isEmpty();


        reconstructionService.reconstruct(FunctionalAdminCollections.AGENCIES, TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo12Reconstructed = agencyDoc.get();
        assertThat(inMogo12Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo12Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs12Reconstructed = agencyDoc.get();
        assertThat(inEs12Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs12Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo22Reconstructed = agencyDoc.get();
        assertThat(inMogo22Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inMogo22Reconstructed.getString("Name")).isEqualTo("agency 2");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs22Reconstructed = agencyDoc.get();
        assertThat(inEs22Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inEs22Reconstructed.getString("Name")).isEqualTo("agency 2");

        assertThat(inMogo12).isEqualTo(inMogo12Reconstructed);
        assertThat(inEs12).isEqualTo(inEs12Reconstructed);
        assertThat(inMogo22).isEqualTo(inMogo22Reconstructed);
        assertThat(inEs22).isEqualTo(inEs22Reconstructed);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionSecurityProfileOk() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);

        // Import 1 document securityProfile.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles =
                PropertiesUtils.getResourceFile(INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON);
            List<SecurityProfileModel> securityProfileModelList =
                JsonHandler
                    .getFromFileAsTypeRefence(securityProfileFiles, new TypeReference<List<SecurityProfileModel>>() {
                    });
            client.importSecurityProfiles(securityProfileModelList);
        }
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        final VitamMongoRepository securityProfileMongo =
            vitamRepository.getVitamMongoRepository(FunctionalAdminCollections.SECURITY_PROFILE);

        final VitamElasticsearchRepository securityProfileEs =
            vitamRepository.getVitamESRepository(FunctionalAdminCollections.SECURITY_PROFILE);

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11 = securityProfileyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11 = securityProfileyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(vitamRepository,
                new RestoreBackupServiceImpl());
        reconstructionService.reconstruct(FunctionalAdminCollections.SECURITY_PROFILE, TENANT_1);

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo11Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11Reconstructed = securityProfileyDoc.get();
        assertThat(inEs11Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");


        assertThat(inMogo11).isEqualTo(inMogo11Reconstructed);
        assertThat(inEs11).isEqualTo(inEs11Reconstructed);

        // Import 2 document securityProfile.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles =
                PropertiesUtils.getResourceFile(INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_2_JSON);
            List<SecurityProfileModel> securityProfileModelList =
                JsonHandler
                    .getFromFileAsTypeRefence(securityProfileFiles, new TypeReference<List<SecurityProfileModel>>() {
                    });
            client.importSecurityProfiles(securityProfileModelList);
        }

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo12 = securityProfileyDoc.get();
        assertThat(inMogo12.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo12.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs12 = securityProfileyDoc.get();
        assertThat(inEs12.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs12.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo22 = securityProfileyDoc.get();
        assertThat(inMogo22.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inMogo22.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs22 = securityProfileyDoc.get();
        assertThat(inEs22.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inEs22.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isEmpty();

        reconstructionService.reconstruct(FunctionalAdminCollections.SECURITY_PROFILE, TENANT_1);

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo12Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo12Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo12Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs12Reconstructed = securityProfileyDoc.get();
        assertThat(inEs12Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs12Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo22Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo22Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inMogo22Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs22Reconstructed = securityProfileyDoc.get();
        assertThat(inEs22Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inEs22Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_2");

        assertThat(inMogo12).isEqualTo(inMogo12Reconstructed);
        assertThat(inEs12).isEqualTo(inEs12Reconstructed);
        assertThat(inMogo22).isEqualTo(inMogo22Reconstructed);
        assertThat(inEs22).isEqualTo(inEs22Reconstructed);
    }

    @Test
    @RunWithCustomExecutor
    public void testBackupOperationOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        final GUID eip = GUIDFactory.newEventGUID(TENANT_0);
        final GUID eiEvent = GUIDFactory.newEventGUID(TENANT_0);
        final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
            .newLogbookOperationParameters(eip, "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        final LogbookOperationParameters logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            eiEvent,"eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        Path backup0Folder = Paths.get(OFFER_FOLDER, TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        Path backup1Folder = Paths.get(OFFER_FOLDER, TENANT_1 + "_" + DataCategory.BACKUP_OPERATION.getFolder());

        assertThat(java.nio.file.Files.exists(backup0Folder)).isFalse();
        assertThat(java.nio.file.Files.exists(backup1Folder)).isFalse();

        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), eip.getId()))).isFalse();

        LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

        client.create(logbookParametersStart);
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), eip.getId()))).isTrue();

        client.update(logbookParametersAppend);
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), eip.getId()))).isTrue();
        
        RequestResponse<OfferLog> offerLogResponse1 = storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, 0L, 10, Order.ASC);
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().size()).isEqualTo(2);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getContainer()).isEqualTo(TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getFileName()).isEqualTo(eip.getId());
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getSequence()).isEqualTo(2L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getContainer()).isEqualTo(TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getFileName()).isEqualTo(eip.getId());
        
        RequestResponse<OfferLog> offerLogResponse2 = storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, 1L, 10, Order.DESC);
        assertThat(offerLogResponse2).isNotNull();
        assertThat(offerLogResponse2.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getFileName()).isEqualTo(eip.getId());
        
        RequestResponse<OfferLog> offerLogResponse3 = storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, null, 10, Order.DESC);
        assertThat(offerLogResponse3).isNotNull();
        assertThat(offerLogResponse3.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse3).getResults().size()).isEqualTo(2);
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
        }
    }

}
