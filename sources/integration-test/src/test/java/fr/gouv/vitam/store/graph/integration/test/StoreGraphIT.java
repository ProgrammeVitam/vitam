/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.store.graph.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.graph.StoreGraphException;
import fr.gouv.vitam.metadata.core.graph.StoreGraphService;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
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

/**
 * Integration test of LogbookCheckConsistency services.
 */
public class StoreGraphIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreGraphIT.class);

    private static final String DEFAULT_OFFER_CONF = "integration-store-graph/storage-default-offer.conf";
    private static final String STORAGE_CONF = "integration-store-graph/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-store-graph/workspace.conf";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String VITAM_TMP_FOLDER = "vitam.tmp.folder";
    ;
    private static final String CONTAINER = "checklogbookreports";
    private static final String OFFER_FOLDER = "offer";
    private static final String LOCALHOST = "localhost";

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String VITAM_TEST = "Vitam-Test";

    private static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS =
        "ERROR: Exception has been thrown when cleaning offers.";
    private static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANNING_WORKSPACE =
        "ERROR: Exception has been thrown when cleanning workspace:";

    private static final int TENANT_0 = 0;

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static StorageMain storageMain;
    private static StorageClient storageClient;
    private static DefaultOfferMain defaultOfferApplication;


    private StoreGraphService storeGraphService;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), VITAM_TEST,
            MetadataCollections.UNIT.name(), MetadataCollections.OBJECTGROUP.name(),
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), MetadataCollections.UNIT.name(),
            MetadataCollections.OBJECTGROUP.name());

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set(VITAM_TMP_FOLDER, vitamTempFolder.getAbsolutePath());

        // launch functional Admin server
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode(LOCALHOST, elasticsearchRule.getTcpPort()));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF);
        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());
        writeYaml(workspaceConfigFile, workspaceConfiguration);

        // prepare workspace
        workspaceMain = new WorkspaceMain(WORKSPACE_CONF);
        workspaceMain.start();

        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();


        // prepare offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        // prepare storage
        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        StorageClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_STORAGE));
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
                LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANNING_WORKSPACE, e);
            }
        }
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
                LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS, e);
            }
        }
        if (storageClient != null) {
            storageClient.close();
        }
        if (defaultOfferApplication != null) {
            defaultOfferApplication.stop();
        }
        if (storageMain != null) {
            storageMain.stop();
        }
        elasticsearchRule.afterClass();
    }

    private VitamRepositoryProvider vitamRepositoryProvider;

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        Map<MetadataCollections, VitamMongoRepository> mongoRepository = new HashMap<>();
        mongoRepository.put(MetadataCollections.UNIT,
            new VitamMongoRepository(mongoRule.getMongoCollection(MetadataCollections.UNIT.name())));
        mongoRepository.put(MetadataCollections.OBJECTGROUP,
            new VitamMongoRepository(mongoRule.getMongoCollection(MetadataCollections.OBJECTGROUP.name())));

        vitamRepositoryProvider = VitamRepositoryFactory.getInstance(mongoRepository, new HashMap<>());
        RestoreBackupService restoreBackupService = new RestoreBackupService();
        storeGraphService = new StoreGraphService(
            vitamRepositoryProvider,
            restoreBackupService,
            WorkspaceClientFactory.getInstance(),
            StorageClientFactory.getInstance());
    }

    @After
    public void tearDown() {
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }


    @Test
    @RunWithCustomExecutor
    public void testStoreUnitGraphThenStoreOccurs() throws DatabaseException, StoreGraphException {
        storeGraphService.setMONGO_BATCH_SIZE(5);

        LocalDateTime dateTime = LocalDateTime.now();

        String dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT).save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(MetadataCollections.UNIT.name());
        long count = collection.count();
        assertThat(count).isEqualTo(10);

        Map<MetadataCollections, Integer> ok = storeGraphService.tryStoreGraph();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(10);


        dateTime = LocalDateTime.now();
        dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        documents = new ArrayList<>();
        for (int i = 10; i < 15; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT).save(documents);

        collection = mongoRule.getMongoCollection(MetadataCollections.UNIT.name());
        count = collection.count();
        assertThat(count).isEqualTo(15);

        ok = storeGraphService.tryStoreGraph();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(5);

    }


    @Test
    @RunWithCustomExecutor
    public void testStoreUnitGraphThenNoStoreOccurs() throws StoreGraphException {
        Map<MetadataCollections, Integer> ok = storeGraphService.tryStoreGraph();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(0);
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        File directory = new File(OFFER_FOLDER);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS, e);
        }
    }
}
