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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.functional.administration.core.reconstruction.RestoreBackupServiceImpl;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class RestoreBackupIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RestoreBackupIT.class);

    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    private static final String STORAGE_CONF = "storage-test/storage-engine.conf";
    private static final String OFFER_ID = "default";

    // ports are overriden
    private static int workspacePort = 8987;
    private static int defaultOfferPort = 8575;
    private static int storageEnginePort = 8583;

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static DefaultOfferMain defaultOfferApplication;
    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static final String OFFER_FOLDER = "offer";
    private static final String BACKUP_COPY_FOLDER = "backup";
    private static final int TENANT_ID = 0;
    private static final String CONTAINER = "0_rules";
    private static String containerName = "";

    static TemporaryFolder folder = new TemporaryFolder();

    private static RestoreBackupService recoverBackupService;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        OfferCollections.OFFER_LOG.setPrefix(GUIDFactory.newGUID().getId());
        OfferCollections.OFFER_SEQUENCE.setPrefix(GUIDFactory.newGUID().getId());
        mongoRule.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRule.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());

        containerName = String.format("%s_%s", TENANT_ID, DataCategory.BACKUP.getCollectionName().toLowerCase());
        recoverBackupService = new RestoreBackupServiceImpl();

        // prepare workspace
        workspacePort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT, workspacePort);
        workspaceMain = new WorkspaceMain(WORKSPACE_CONF);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // prepare offer
        defaultOfferPort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, defaultOfferPort);
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String[] seg = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration.setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + workspacePort);

        folder.create();
        serverConfiguration.setZippingDirecorty(folder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(folder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        File staticOffersFile = PropertiesUtils.findFile("static-offer.json");
        ArrayNode staticOffers = (ArrayNode) JsonHandler.getFromFile(staticOffersFile);
        ObjectNode defaultOffer = (ObjectNode) staticOffers.get(0);
        defaultOffer.put("baseUrl", "http://localhost:" + defaultOfferPort);
        try (FileOutputStream outputStream = new FileOutputStream(staticOffersFile)) {
            IOUtils.write(JsonHandler.prettyPrint(staticOffers), outputStream, CharsetUtils.UTF_8);
        }

        // prepare storage
        storageEnginePort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(StorageMain.PARAMETER_JETTY_SERVER_PORT, storageEnginePort);
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        StorageClientFactory.changeMode("http://localhost:" + storageEnginePort);
        storageClient = StorageClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mongoRule.handleAfterClass();

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
        JunitHelper.getInstance().releasePort(workspacePort);
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
        JunitHelper.getInstance().releasePort(defaultOfferPort);
        storageMain.stop();
        JunitHelper.getInstance().releasePort(storageEnginePort);
        folder.delete();
        VitamClientFactory.resetConnections();
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
        mongoRule.handleAfter();
        // delete the container on the Workspace
        workspaceClient.deleteContainer(containerName, true);
        // clean offers
        cleanOffers();
    }

    @Test
    @RunWithCustomExecutor
    public void testRetrievalLastBackupVersion_emptyResult() throws Exception {
        // get the last version of the json backup files.
        Optional<String> lastBackupVersion = recoverBackupService.getLatestSavedFileName(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.RULES,
            FunctionalAdminCollections.RULES
        );

        LOGGER.debug("No backup version found.");
        Assert.assertEquals(Optional.empty(), lastBackupVersion);
    }

    @Test
    @RunWithCustomExecutor
    public void testRetrievalLastBackupVersion() throws Exception {
        // create and save some backup files for reconstruction.
        prepareBackupStorage();

        // get the last version of the json backup files.
        Optional<String> lastBackupVersion = recoverBackupService.getLatestSavedFileName(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.BACKUP,
            FunctionalAdminCollections.RULES
        );

        Assert.assertTrue(lastBackupVersion.isPresent());
        LOGGER.debug(String.format("Last backup version -> %s", lastBackupVersion.get()));
        Assert.assertEquals("0_FileRules_101.json", lastBackupVersion.get());
    }

    @Test
    @RunWithCustomExecutor
    public void testRecoverBackupCopy_WithoutResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // get the backup copy file.
        Optional<CollectionBackupModel> collectionBackup = recoverBackupService.readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            FunctionalAdminCollections.RULES
        );

        LOGGER.debug("No backup copy found.");
        Assert.assertEquals(Optional.empty(), collectionBackup);
    }

    @Test
    @RunWithCustomExecutor
    public void testRecoverBackupCopy() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // create and save some backup files for reconstruction.
        prepareBackupStorage();

        // get the backup copy file.
        Optional<CollectionBackupModel> collectionBackup = recoverBackupService.readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            FunctionalAdminCollections.RULES
        );

        Assert.assertTrue(collectionBackup.isPresent());
        Assert.assertEquals(52, collectionBackup.get().getDocuments().size());
        Assert.assertEquals(36543, collectionBackup.get().getSequence().getCounter().intValue());
    }

    private void prepareBackupStorage() throws Exception {
        // create and store different backup copies from the backup folder.
        File backupFolder = PropertiesUtils.findFile(BACKUP_COPY_FOLDER);
        LOGGER.debug(String.format("Start storage backup copies -> Folder : %s", backupFolder));
        if (backupFolder.exists()) {
            LOGGER.debug(String.format("Storage of %s backup copies.", backupFolder.list().length));
            Arrays.stream(backupFolder.listFiles()).forEach(f -> storeBackupCopies(f));
        }
    }

    private void storeBackupCopies(final File file) {
        final String extension = "json";
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            final String uri = file.getName();

            // add the object on the container on the Workspace
            workspaceClient.putObject(containerName, uri, in);

            // store on the storage
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(uri);
            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.BACKUP,
                uri,
                description
            );
        } catch (Exception e) {
            LOGGER.error("ERROR: Exception has been thrown when storing the backup copy.", e);
        }
    }

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
