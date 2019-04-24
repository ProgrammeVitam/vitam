package fr.gouv.vitam.storage;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for offer storage. <br/>
 */
public class StorageOfferIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageOfferIT.class);

    private static final String DEFAULT_OFFER_CONF = "integration-storage-offer/storage-default-offer.conf";
    private static final String STORAGE_CONF = "integration-storage-offer/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-storage-offer/workspace.conf";

    private static final String OFFER_FOLDER = "offer";
    private static final int TENANT_0 = 0;

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String BINARY_1 = "binary1";
    private static final String UNIT_1 = "unit1.json";
    private static final String UNIT_FILE_1 = "integration-storage-offer/data/unit1.json";
    private static final String UNIT_FILE_1_V2 = "integration-storage-offer/data/unit1_v2.json";
    private static final String BINARY_FILE_1 = "integration-storage-offer/data/binary1";
    private static final String BINARY_FILE_1_V2 = "integration-storage-offer/data/binary1_v2";

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;

    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferMain;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION, OffsetRepository.COLLECTION_NAME,
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
        storageClient = StorageClientFactory.getInstance().getClient();

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

        // launch offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);

        defaultOfferMain = new DefaultOfferMain(offerConfig.getAbsolutePath());
        defaultOfferMain.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // launch storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace("http://localhost:" + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);
    }


    @AfterClass
    public static void afterClass() throws Exception {

        if (workspaceClient != null) {
            workspaceClient.close();
        }
        if (workspaceMain != null) {
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
        if (storageClient != null) {
            storageClient.close();
        }
        if (defaultOfferMain != null) {
            defaultOfferMain.stop();
        }
        if (storageMain != null) {
            storageMain.stop();
        }
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        VitamConfiguration.setAdminTenant(1);

        // ReconstructionService delete all unit and GOT without _tenant and older than 1 month.
        VitamConfiguration.setDeleteIncompleteReconstructedUnitDelay(Integer.MAX_VALUE);
    }

    @After
    public void tearDown() {
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageBasic() throws Exception {

        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Store data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileInOffer(container, UNIT_1, UNIT_FILE_1, DataCategory.UNIT);
        storeFileInOffer(container, BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);

        // Then
        checkFileContent(UNIT_1, UNIT_FILE_1, DataCategory.UNIT);
        checkFileContent(BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageBasicOverrideUpdatableContainer() throws Exception {

        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Store data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileInOffer(container, UNIT_1, UNIT_FILE_1, DataCategory.UNIT);
        storeFileInOffer(container, UNIT_1, UNIT_FILE_1_V2, DataCategory.UNIT);

        // Then
        checkFileContent(UNIT_1, UNIT_FILE_1_V2, DataCategory.UNIT);
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageBasicOverrideNonUpdatableContainerWithSameContent() throws Exception {

        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Store data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileInOffer(container, BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);
        storeFileInOffer(container, BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);

        // Then
        checkFileContent(BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageBasicOverrideNonUpdatableContainerWithDifferentContent() throws Exception {

        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Store data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileInOffer(container, BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);
        assertThatThrownBy(() ->
            storeFileInOffer(container, BINARY_1, BINARY_FILE_1_V2, DataCategory.OBJECT)
        ).isInstanceOf(StorageClientException.class);

        // Then
        checkFileContent(BINARY_1, BINARY_FILE_1, DataCategory.OBJECT);
    }

    private void storeFileInOffer(String container, String fileName, String file, DataCategory type)
        throws ContentAddressableStorageServerException, IOException,
        StorageAlreadyExistsClientException, StorageNotFoundClientException,
        StorageServerClientException {
        final ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceContainerGUID(container);
        objectDescription.setObjectName(fileName);
        objectDescription.setType(type);
        objectDescription.setWorkspaceObjectURI(fileName);

        try (FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(file))) {
            workspaceClient.putObject(container, fileName, stream);
        }
        storageClient.storeFileFromWorkspace("default", type, fileName, objectDescription);
    }

    private void checkFileContent(String objectName, String filePath, DataCategory dataCategory)
        throws FileNotFoundException {
        File offerFolder = new File(OFFER_FOLDER);
        File unit1File = new File(offerFolder, TENANT_0 + "_" + dataCategory.getFolder() + "/" + objectName);
        assertThat(unit1File).hasSameContentAs(PropertiesUtils.getResourceFile(filePath));
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
            }
        }
    }
}
