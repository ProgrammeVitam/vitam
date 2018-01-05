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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryFactory;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryProvider;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.impl.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class ReconstructionIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionIT.class);

    private static final String DEFAULT_OFFER_CONF = "integration-reconstruction/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-reconstruction/logbook.conf";
    private static final String LOGBOOK_CONF_FILE_NAME = "logbook.conf";
    private static final String STORAGE_CONF = "integration-reconstruction/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-reconstruction/workspace.conf";
    private static final String ADMIN_MANAGEMENT_CONF =
        "integration-reconstruction/functional-administration.conf";

    private static final String OFFER_FOLDER = "offer";
    private static final String BACKUP_COPY_FOLDER = "integration-reconstruction/backup";
    private static final int TENANT_ID = 0;
    private static final String STRATEGY_ID = "default";
    private static String CONTAINER = "0_rules";
    private static String containerName = "";

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



    private static RestoreBackupService recoverBuckupService;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "vitam-test",
            FunctionalAdminCollections.SECURITY_PROFILE.getName(), FunctionalAdminCollections.RULES.getName());


    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), FunctionalAdminCollections.SECURITY_PROFILE.getName(),
            FunctionalAdminCollections.RULES.getName());


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        File workspaceFolder = new File(CONTAINER);
        if (workspaceFolder.exists()) {
            try {
                // if clean workspace delete did not work
                FileUtils.cleanDirectory(workspaceFolder);
                FileUtils.deleteDirectory(workspaceFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning workspace:", e);
            }
        }

        containerName = String.format("%s_%s", TENANT_ID, StorageCollectionType.BACKUP.toString().toLowerCase());
        recoverBuckupService = new RestoreBackupServiceImpl();

        // launch functional Admin server
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceMain = new WorkspaceMain(PropertiesUtils.getResourcePath(WORKSPACE_CONF).toString());
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
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
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
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
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
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
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

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        storageClient = StorageClientFactory.getInstance().getClient();
    }


    @AfterClass
    public static void afterClass() throws Exception {

        // Ugly style but necessary because this is the folder representing the workspace
        File workspaceFolder = new File(CONTAINER);
        if (workspaceFolder.exists()) {
            try {
                // if clean workspace delete did not work
                FileUtils.cleanDirectory(workspaceFolder);
                FileUtils.deleteDirectory(workspaceFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning workspace:", e);
            }
        }
        workspaceClient.close();
        workspaceMain.stop();
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
        storageClient.close();
        defaultOfferApplication.stop();
        storageMain.stop();
        adminManagementMain.stop();
        elasticsearchRule.afterClass();
    }

    @Before
    public void setup() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // create a container on the Workspace
        workspaceClient.createContainer(containerName);
    }

    @After
    public void tearDown() throws Exception {
        // delete the container on the Workspace
        workspaceClient.deleteContainer(containerName, true);
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionOk() throws Exception {

        // create and save some backup files for reconstruction.
        prepareBackupStorage();

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration configuration =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(configuration, vitamRepository,
                new RestoreBackupServiceImpl());

        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID);


    }

    /**
     * Creation and storage of backup files copies.
     *
     * @throws Exception
     */
    private void prepareBackupStorage() throws Exception {

        // create and store different backup copies from the backup folder.
        File backupFolder = PropertiesUtils.findFile(BACKUP_COPY_FOLDER);
        LOGGER.debug(String.format("Start storage backup copies -> Folder : %s", backupFolder));
        if (backupFolder.exists()) {
            LOGGER.debug(String.format("Storage of %s backup copies.", backupFolder.list().length));
            Arrays.stream(backupFolder.listFiles()).forEach(f -> storeBackupCopies(f));
        }
    }

    /**
     * Save backup copies of files.
     *
     * @param file
     */
    private void storeBackupCopies(final File file) {
        final String extension = "json";
        try (InputStream in = new BufferedInputStream(new FileInputStream(file));) {
            final String uri = file.getName();

            // add the object on the container on the Workspace
            workspaceClient.putObject(containerName, uri, in);

            // store on the storage
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(uri);
            storageClient.storeFileFromWorkspace(STRATEGY_ID, StorageCollectionType.BACKUP, uri, description);

        } catch (Exception e) {
            LOGGER.error("ERROR: Exception has been thrown when storing the backup copy.", e);
        }
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest here
        File directory = new File(OFFER_FOLDER + "/" + containerName);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
        }
    }

}
