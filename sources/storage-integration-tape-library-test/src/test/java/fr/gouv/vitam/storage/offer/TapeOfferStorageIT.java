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
package fr.gouv.vitam.storage.offer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientOfferLogIterator;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageIllegalOperationClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectAvailabilityRequest;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferDiffRequest;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.storage.engine.server.offerdiff.OfferDiffStatus;
import fr.gouv.vitam.storage.engine.server.offerdiff.ReportEntry;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncStatus;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferConfiguration;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.ObjectReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import fr.gouv.vitam.storage.offers.tape.simulator.TapeLibrarySimulatorRule;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CircularInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.ACCESSION_REGISTER_SYMBOLIC;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.LOGBOOK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECTGROUP_GRAPH;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.STORAGETRACEABILITY;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.buildContainerName;
import static fr.gouv.vitam.storage.engine.server.rest.StorageMain.PARAMETER_JETTY_SERVER_PORT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertNotNull;

/**
 * Warning : All tests are run in the same service context.
 * Tests need to write random file names to avoid write conflicts
 * Tests cannot assume data expect if they reserve exclusive usage to a specific container (tenant / data category)
 * Tests may need to wait a few seconds for data to be written tape library OR for cache eviction to occur.
 */
@RunWithCustomExecutor
public class TapeOfferStorageIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeOfferStorageIT.class);

    private static final TypeReference<ReportEntry> OFFER_DIFF_ENTRY_TYPE = new TypeReference<>() {};

    private static final int PORT_SERVICE_WORKSPACE = 8987;
    private static final String WORKSPACE_CONF_DIR = "workspace-conf/";
    private static final String WORKSPACE_CONF_FILE = "workspace-conf/workspace.conf";
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private static final int PORT_SERVICE_STORAGE = 8583;
    private static final int PORT_ADMIN_STORAGE = 28583;
    private static final String JETTY_STORAGE_ADMIN_PORT = "jetty.storage.admin";
    private static final String STORAGE_CONF_DIR = "storage-conf/";
    private static final String STORAGE_CONF_FILE = "storage-conf/storage.conf";

    private static final int PORT_SERVICE_OFFER1 = 8757;
    private static final int PORT_ADMIN_OFFER1 = 28757;
    private static final String JETTY_OFFER1_ADMIN_PORT = "jetty.offer.admin.port";
    private static final String OFFER1_CONF_DIR = "offer1-conf/";
    private static final String OFFER1_CONF_FILE = "offer1-conf/offer.conf";
    private static final String OFFER1_STORAGE_CONF_FILE = "offer1-conf/default-storage.conf";

    private static final int PORT_SERVICE_OFFER2 = 8758;
    private static final String OFFER2_CONF_DIR = "offer2-conf/";
    private static final String OFFER2_CONF_FILE = "offer2-conf/offer.conf";
    private static final String OFFER2_STORAGE_CONF_FILE = "offer2-conf/default-storage.conf";

    private static final String DB_OFFER1 = "offer1";
    private static final String DB_OFFER2 = "offer2";
    private static final String OFFER_1 = "offer1";
    private static final String OFFER_2 = "offer2";
    private static final List<String> OFFER_IDS = List.of(OFFER_1, OFFER_2);
    private static final String DEFAULT_STRATEGY = "default";
    private static final String OFFER1_ONLY_STRATEGY = "tape_only";
    private static final String OFFER2_ONLY_STRATEGY = "fs_only";
    private static final String TEST_BUCKET = "test";
    private static final String ADMIN_BUCKET = "admin";
    private static final String PROD_BUCKET = "prod";
    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";
    private static final String LTO_6 = "LTO-6";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static MongoRule mongoRuleOffer1 = new MongoRule(DB_OFFER1, MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static MongoRule mongoRuleOffer2 = new MongoRule(DB_OFFER2, MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static TempFolderRule tempFolder = new TempFolderRule();

    @ClassRule
    public static TapeLibrarySimulatorRule tapeLibrarySimulatorRule;

    @ClassRule
    public static LogicalClockRule logicalClock = new LogicalClockRule();

    private static WorkspaceMain workspaceMain;

    private static DefaultOfferMain offer2Main;

    private static StorageMain storageMain;

    private static DefaultOfferMain offer1Main;

    private static OfferSyncAdminResource offerSyncAdminResource;
    private static OfferDiffAdminResource offerDiffAdminResource;
    private static TapeBackupAdminResource tapeBackupAdminResource;

    private WorkspaceClient workspaceClient;
    private StorageClient storageClient;
    private ObjectReferentialRepository objectReferentialRepository;
    private ArchiveReferentialRepository archiveReferentialRepository;
    private TapeCatalogRepository tapeCatalogRepository;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        setupDbCollections();
        setupWorkspace();
        setupOffer1();
        setupOffer2();
        setupStorageEngine();
    }

    private static void setupDbCollections() {
        for (OfferCollections offerCollection : OfferCollections.values()) {
            offerCollection.setPrefix(GUIDFactory.newGUID().getId());
            mongoRuleOffer1.addCollectionToBePurged(offerCollection.getName());
            mongoRuleOffer2.addCollectionToBePurged(offerCollection.getName());
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws VitamApplicationServerException {
        shutdownWorkspace();
        shutdownOffer1();
        shutdownOffer2();
        shutdownStorageEngine();

        mongoRuleOffer1.handleAfterClass();
        mongoRuleOffer2.handleAfterClass();

        VitamClientFactory.resetConnections();
    }

    @Before
    public void init() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
        tapeLibrarySimulatorRule.getTapeLibrarySimulator().setSleepDelayMillis(10);

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        storageClient = StorageClientFactory.getInstance().getClient();

        objectReferentialRepository = new ObjectReferentialRepository(
            mongoRuleOffer1.getMongoCollection(OfferCollections.TAPE_OBJECT_REFERENTIAL.getName())
        );
        archiveReferentialRepository = new ArchiveReferentialRepository(
            mongoRuleOffer1.getMongoCollection(OfferCollections.TAPE_ARCHIVE_REFERENTIAL.getName())
        );
        tapeCatalogRepository = new TapeCatalogRepository(
            mongoRuleOffer1.getMongoCollection(OfferCollections.TAPE_CATALOG.getName())
        );
    }

    public static void setupWorkspace() throws IOException, VitamApplicationServerException {
        String workspaceConfDir = PropertiesUtils.getResourceFile(WORKSPACE_CONF_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", workspaceConfDir);
        VitamConfiguration.getConfiguration().setConfig(workspaceConfDir);

        String workspaceTmpDir = tempFolder.newFolder().getAbsolutePath();
        SystemPropertyUtil.set("vitam.tmp.folder", workspaceTmpDir);

        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF_FILE);

        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration = PropertiesUtils.readYaml(
            workspaceConfigFile,
            fr.gouv.vitam.common.storage.StorageConfiguration.class
        );
        String workspaceDataDir = tempFolder.newFolder().getAbsolutePath();
        workspaceConfiguration.setStoragePath(workspaceDataDir);

        writeYaml(workspaceConfigFile, workspaceConfiguration);

        workspaceMain = new WorkspaceMain(WORKSPACE_CONF_FILE);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
        SystemPropertyUtil.clear("vitam.conf.folder");
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);
    }

    private static void shutdownWorkspace() throws VitamApplicationServerException {
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
    }

    private static void setupOffer1() throws IOException, VitamApplicationServerException {
        for (OfferCollections offerCollection : OfferCollections.values()) {
            mongoRuleOffer1.addCollectionToBePurged(offerCollection.getName());
        }

        Path offer1InputFileStorageFolder = tempFolder.newFolder("offer1-data", "inputFiles").toPath();
        Path offer1InputTarStorageFolder = tempFolder.newFolder("offer1-data", "inputTars").toPath();
        Path offer1TmpTarOutputStorageFolder = tempFolder.newFolder("offer1-data", "tmpTarOutput").toPath();
        Path offer1CachedTarStorageFolder = tempFolder.newFolder("offer1-data", "cachedTars").toPath();
        tapeLibrarySimulatorRule = new TapeLibrarySimulatorRule(
            offer1InputTarStorageFolder,
            offer1TmpTarOutputStorageFolder,
            4,
            50,
            50,
            5_000_000,
            LTO_6,
            10
        );

        String offer1ConfDir = PropertiesUtils.getResourceFile(OFFER1_CONF_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", offer1ConfDir);
        VitamConfiguration.getConfiguration().setConfig(offer1ConfDir);

        String offer1TmpDir = tempFolder.newFolder().getAbsolutePath();
        SystemPropertyUtil.set("vitam.tmp.folder", offer1TmpDir);

        // Rewrite default-storage.conf file
        File offerStorageConfigFile = PropertiesUtils.findFile(OFFER1_STORAGE_CONF_FILE);
        ObjectNode offerStorageConfig = readYaml(offerStorageConfigFile, ObjectNode.class);

        ObjectNode tapeLibraryConfiguration = (ObjectNode) offerStorageConfig.get("tapeLibraryConfiguration");
        tapeLibraryConfiguration.put(
            "inputFileStorageFolder",
            offer1InputFileStorageFolder.toAbsolutePath().toString()
        );
        tapeLibraryConfiguration.put("inputTarStorageFolder", offer1InputTarStorageFolder.toAbsolutePath().toString());
        tapeLibraryConfiguration.put(
            "tmpTarOutputStorageFolder",
            offer1TmpTarOutputStorageFolder.toAbsolutePath().toString()
        );
        tapeLibraryConfiguration.put(
            "cachedTarStorageFolder",
            offer1CachedTarStorageFolder.toAbsolutePath().toString()
        );
        writeYaml(offerStorageConfigFile, offerStorageConfig);

        // Rewrite offer.conf file
        final File offerConfig = PropertiesUtils.findFile(OFFER1_CONF_FILE);
        final OfferConfiguration offerConfiguration = readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        offerConfiguration.setStoragePath(tempFolder.newFolder().getAbsolutePath());
        writeYaml(offerConfig, offerConfiguration);

        SystemPropertyUtil.set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, PORT_SERVICE_OFFER1);
        SystemPropertyUtil.set(JETTY_OFFER1_ADMIN_PORT, Integer.toString(PORT_ADMIN_OFFER1));

        offer1Main = new DefaultOfferMain(offerConfig.getAbsolutePath());
        offer1Main.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://localhost:" + PORT_ADMIN_OFFER1)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        tapeBackupAdminResource = retrofit.create(TapeBackupAdminResource.class);
    }

    private static void shutdownOffer1() throws VitamApplicationServerException {
        if (offer1Main != null) {
            offer1Main.stop();
        }
        tapeLibrarySimulatorRule.after();
    }

    private static void setupOffer2() throws IOException, VitamApplicationServerException {
        for (OfferCollections offerCollection : OfferCollections.values()) {
            mongoRuleOffer2.addCollectionToBePurged(offerCollection.getName());
        }

        String offer2ConfDir = PropertiesUtils.getResourceFile(OFFER2_CONF_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", offer2ConfDir);
        VitamConfiguration.getConfiguration().setConfig(offer2ConfDir);

        String offer2TmpDir = tempFolder.newFolder().getAbsolutePath();
        SystemPropertyUtil.set("vitam.tmp.folder", offer2TmpDir);

        // Rewrite default-storage.conf file
        String offer2DataDir = tempFolder.newFolder().getAbsolutePath();

        File offerStorageConfigFile = PropertiesUtils.findFile(OFFER2_STORAGE_CONF_FILE);
        writeYaml(
            offerStorageConfigFile,
            JsonHandler.createObjectNode()
                .put("storagePath", offer2DataDir)
                .put("provider", StorageProvider.FILESYSTEM.getValue())
        );

        // Rewrite offer.conf file
        final File offerConfig = PropertiesUtils.findFile(OFFER2_CONF_FILE);
        final OfferConfiguration offerConfiguration = readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        offerConfiguration.setStoragePath(offer2DataDir);
        writeYaml(offerConfig, offerConfiguration);

        SystemPropertyUtil.set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, PORT_SERVICE_OFFER2);

        offer2Main = new DefaultOfferMain(offerConfig.getAbsolutePath());
        offer2Main.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    private static void shutdownOffer2() throws VitamApplicationServerException {
        if (offer2Main != null) {
            offer2Main.stop();
        }
    }

    private static void setupStorageEngine() throws Exception {
        String storageConfDir = PropertiesUtils.getResourceFile(STORAGE_CONF_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", storageConfDir);
        VitamConfiguration.getConfiguration().setConfig(storageConfDir);

        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF_FILE);

        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration.setUrlWorkspace(WORKSPACE_URL);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        SystemPropertyUtil.set(PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        SystemPropertyUtil.set(JETTY_STORAGE_ADMIN_PORT, Integer.toString(PORT_ADMIN_STORAGE));

        storageMain = new StorageMain(STORAGE_CONF_FILE);
        storageMain.start();

        // Force drive loading
        for (String offerId : OFFER_IDS) {
            Connection connect = DriverManager.getDriverFor(offerId).connect(offerId);
            connect.close();
        }

        SystemPropertyUtil.clear(PARAMETER_JETTY_SERVER_PORT);
        SystemPropertyUtil.clear(JETTY_STORAGE_ADMIN_PORT);
        SystemPropertyUtil.clear("vitam.conf.folder");

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://localhost:" + PORT_ADMIN_STORAGE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        offerSyncAdminResource = retrofit.create(OfferSyncAdminResource.class);
        offerDiffAdminResource = retrofit.create(OfferDiffAdminResource.class);

        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
    }

    private static void shutdownStorageEngine() throws VitamApplicationServerException {
        if (storageMain != null) {
            storageMain.stop();
        }
    }

    @Test
    public void testWriteObjectsToANonExpirableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 4;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(800_000, 1_200_000)));
            objectNames.add(prefix + "unit" + i + ".json");
            writeToOffers(files.get(i), DEFAULT_STRATEGY, UNIT, objectNames.get(i));
        }

        // Override unit2 content
        files.set(2, createRandomFile(RandomUtils.nextInt(800_000, 1_200_000)));
        writeToOffers(files.get(2), DEFAULT_STRATEGY, UNIT, objectNames.get(2));

        // Write dummy objects to drain incomplete tars
        writeToOffers(createRandomFile(1_000_000), DEFAULT_STRATEGY, UNIT, prefix + "someDataForIncompleteTarDraining");

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, UNIT, objectNames, TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check existence & digests in both offers
        for (int i = 0; i < nbObjects; i++) {
            checkObjectExistence(OFFER_IDS, UNIT, objectNames.get(i));
            checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(i), files.get(i));
        }

        // Bulk check digests
        checkBulkObjectDigests(OFFER_IDS, UNIT, objectNames, files);

        // Ensure objects in UNIT containers are available in cache for immediate access (no cache expiration for metadata)
        checkObjectAvailabilityFromTapeOffer(UNIT, objectNames, true);

        // Immediate access without access requests
        for (int i = 0; i < nbObjects; i++) {
            readAndValidateObjectFromTapeOffer(UNIT, objectNames.get(i), files.get(i));
        }

        // Create access request and check its immediate availability
        String accessRequestId = createAccessRequest(OFFER_1, UNIT, objectNames);
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.READY);

        // Access with access requests still active anyway
        for (int i = 0; i < nbObjects; i++) {
            readAndValidateObjectFromTapeOffer(UNIT, objectNames.get(i), files.get(i));
        }

        // Ensure access request expires after a while
        logicalClock.logicalSleep(16, ChronoUnit.MINUTES);
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.EXPIRED);

        // Cleanup access request for UNIT container
        removeAccessRequest(accessRequestId);

        // Ensure that access request not found anymore
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.NOT_FOUND);

        // Force cache eviction
        forceCacheEviction();

        // Ensure all unit objects are still available in cache for immediate access
        checkObjectAvailabilityFromTapeOffer(UNIT, objectNames, true);

        // Immediate access without access requests
        for (int i = 0; i < nbObjects; i++) {
            readAndValidateObjectFromTapeOffer(UNIT, objectNames.get(i), files.get(i));
        }
    }

    @Test
    public void testWriteObjectsToAnExpirableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 6;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(800_000, 1_200_000)));
            objectNames.add(prefix + "obj" + i);
            writeToOffers(files.get(i), DEFAULT_STRATEGY, OBJECT, objectNames.get(i));
        }

        // Try override obj2 content
        File file2_bis = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        assertThatThrownBy(() -> writeToOffers(file2_bis, DEFAULT_STRATEGY, OBJECT, objectNames.get(2))).isInstanceOf(
            StorageAlreadyExistsClientException.class
        );

        // Override obj1 with same content
        writeToOffers(files.get(1), DEFAULT_STRATEGY, OBJECT, objectNames.get(1));

        // Write dummy objects to drain incomplete tars
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, objectNames, TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check existence & digests in both offers
        for (int i = 0; i < nbObjects; i++) {
            checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(i));
            checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(i), files.get(i));
        }

        // Ensure that objects in OBJECT expire in cache / are not available for immediate access
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, false);

        // Create access request and wait for data availability
        String accessRequestId = createAccessRequest(OFFER_1, OBJECT, objectNames);
        awaitAccessRequestReadiness(accessRequestId);

        // Ensure all objects became available again
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, true);
        for (int i = 0; i < nbObjects; i++) {
            readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(i), files.get(i));
        }

        // Force cache eviction
        forceCacheEviction();

        // Ensure all objects are still available
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, true);
        for (int i = 0; i < nbObjects; i++) {
            readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(i), files.get(i));
        }

        // Ensure access request expires after a while
        logicalClock.logicalSleep(16, ChronoUnit.MINUTES);
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.EXPIRED);

        // Force cache eviction
        forceCacheEviction();

        // Ensure objects are no more available after access request expiration
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, false);

        // Cleanup access request
        removeAccessRequest(accessRequestId);

        // Ensure that access request not found anymore
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.NOT_FOUND);

        // Recreate another access request
        String accessRequestId2 = createAccessRequest(OFFER_1, OBJECT, objectNames);
        awaitAccessRequestReadiness(accessRequestId2);

        // Ensure objects are available again
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, true);

        // Cleanup access request
        removeAccessRequest(accessRequestId2);

        // Ensure that access request not found anymore
        checkAccessRequestStatus(accessRequestId2, AccessRequestStatus.NOT_FOUND);
    }

    @Test
    public void testBulkWriteAndAwaitObjectsToBeAvailable() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 50;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(80_000, 120_000)));
            objectNames.add(prefix + "obj" + i);
        }

        bulkWriteToOffers(DEFAULT_STRATEGY, OBJECT, files, objectNames);

        // Check object existence & digests
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(3), files.get(3));
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(3));
        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(nbObjects - 1));

        // Write dummy objects to drain incomplete tars
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, objectNames, TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Ensure archives evicted from cache
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, false);

        // Ensure data is available once access request created
        String accessRequestId = createAccessRequest(null, OBJECT, objectNames);
        awaitAccessRequestReadiness(accessRequestId);

        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, true);

        // Read objects
        readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(3), files.get(3));
        readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        // Cleanup
        removeAccessRequest(accessRequestId);
    }

    @Test
    public void testBulkWriteIdempotency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 8;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(400_000, 600_000)));
            objectNames.add(prefix + "obj" + i);
        }

        bulkWriteToOffers(DEFAULT_STRATEGY, OBJECT, files, objectNames);

        // Write dummy objects to drain incomplete tars
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, objectNames, TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check object digests OK
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(3), files.get(3));
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(3));
        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(nbObjects - 1));

        // Ensure archives evicted from cache
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, false);

        // Ensure data is available once access request created
        String accessRequestId = createAccessRequest(null, OBJECT, objectNames);
        awaitAccessRequestReadiness(accessRequestId);

        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, true);

        // Read objects
        readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(3), files.get(3));
        readAndValidateObjectFromTapeOffer(OBJECT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        // Cleanup
        removeAccessRequest(accessRequestId);
    }

    @Test
    public void testBulkWriteOverrideRewritableContainerObjects() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 8;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(400_000, 600_000)));
            objectNames.add(prefix + "unit" + i + ".json");
        }

        bulkWriteToOffers(DEFAULT_STRATEGY, UNIT, files, objectNames);

        files.set(3, createRandomFile(RandomUtils.nextInt(400_000, 600_000)));

        assertThatCode(() -> bulkWriteToOffers(DEFAULT_STRATEGY, UNIT, files, objectNames)).doesNotThrowAnyException();

        // Check object existence & digests
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(3), files.get(3));
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        checkObjectExistence(OFFER_IDS, UNIT, objectNames.get(3));
        checkObjectExistence(OFFER_IDS, UNIT, objectNames.get(nbObjects - 1));

        // Write dummy objects to drain incomplete tars
        writeToOffers(createRandomFile(1_000_000), DEFAULT_STRATEGY, UNIT, prefix + "someDataForIncompleteTarDraining");

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, UNIT, objectNames, TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check object existence & digests once again
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(3), files.get(3));
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        checkObjectExistence(OFFER_IDS, UNIT, objectNames.get(3));
        checkObjectExistence(OFFER_IDS, UNIT, objectNames.get(nbObjects - 1));

        // Check objects are still available in cache (no cache expiration for metadata)
        checkObjectAvailabilityFromTapeOffer(UNIT, objectNames, true);

        // Check objects are readable
        readAndValidateObjectFromTapeOffer(UNIT, objectNames.get(3), files.get(3));
        readAndValidateObjectFromTapeOffer(UNIT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));
    }

    @Test
    public void testDeleteObject() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        int nbObjects = 4;
        for (int i = 0; i < nbObjects; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(10_000, 500_000)));
            objectNames.add(prefix + "unit" + i + ".json");
        }

        bulkWriteToOffers(DEFAULT_STRATEGY, UNIT, files, objectNames);

        // When deleting an existing object
        deleteObject(DEFAULT_STRATEGY, UNIT, objectNames.get(2));

        // Then
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(3), files.get(3));
        checkObjectDigest(OFFER_IDS, UNIT, objectNames.get(nbObjects - 1), files.get(nbObjects - 1));

        checkObjectNotFound(OFFER_IDS, UNIT, objectNames.get(2));

        // Immediate access to delete file is still available
        checkObjectAvailabilityFromTapeOffer(UNIT, objectNames, true);

        // Creating an access request to a deleted object is always possible
        String accessRequestId = createAccessRequest(OFFER_1, UNIT, objectNames);
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.READY);

        // Cleanup access request
        removeAccessRequest(accessRequestId);

        // When delete an already deleted object, or non-existing object, or non-existing container, then OK
        assertThatCode(() -> deleteObject(DEFAULT_STRATEGY, UNIT, objectNames.get(2))).doesNotThrowAnyException();

        assertThatCode(
            () -> deleteObject(DEFAULT_STRATEGY, UNIT, "someNonExistingObjectName")
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> deleteObject(DEFAULT_STRATEGY, ACCESSION_REGISTER_SYMBOLIC, "anObjectFromNonExistingContainer")
        ).doesNotThrowAnyException();

        // When deleting an object from a non-deletable container then KO
        File someTraceabilityObject = createRandomFile(RandomUtils.nextInt(10_000, 500_000));
        writeToOffers(someTraceabilityObject, DEFAULT_STRATEGY, STORAGETRACEABILITY, "someTraceabilityObject.zip");

        assertThatThrownBy(
            () -> deleteObject(DEFAULT_STRATEGY, STORAGETRACEABILITY, "someTraceabilityObject.zip")
        ).isInstanceOf(StorageServerClientException.class);

        readAndValidateObjectFromTapeOffer(STORAGETRACEABILITY, "someTraceabilityObject.zip", someTraceabilityObject);
    }

    @Test
    public void testListContainerObjects() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(2);

        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(10_000, 50_000)));
            objectNames.add(prefix + "obj" + i);
        }
        bulkWriteToOffers(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, files, objectNames);

        deleteObject(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(2));

        writeToOffers(files.get(1), DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(1));

        deleteObject(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, "unknown");

        for (int i = 5; i < 10; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(10_000, 50_000)));
            objectNames.add(prefix + "obj" + i);
            writeToOffers(files.get(i), DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(i));
        }

        deleteObject(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(0));

        deleteObject(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(9));

        deleteObject(DEFAULT_STRATEGY, OBJECTGROUP_GRAPH, objectNames.get(2));

        // When
        try (
            CloseableIterator<ObjectEntry> objectIterator = storageClient.listContainer(
                DEFAULT_STRATEGY,
                null,
                OBJECTGROUP_GRAPH
            )
        ) {
            assertThat(objectIterator)
                .usingFieldByFieldElementComparator()
                .containsExactly(
                    new ObjectEntry(prefix + "obj1", files.get(1).length()),
                    new ObjectEntry(prefix + "obj3", files.get(3).length()),
                    new ObjectEntry(prefix + "obj4", files.get(4).length()),
                    new ObjectEntry(prefix + "obj5", files.get(5).length()),
                    new ObjectEntry(prefix + "obj6", files.get(6).length()),
                    new ObjectEntry(prefix + "obj7", files.get(7).length()),
                    new ObjectEntry(prefix + "obj8", files.get(8).length())
                );
        }

        Iterator<OfferLog> objectIterator = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            DEFAULT_STRATEGY,
            null,
            Order.ASC,
            OBJECTGROUP_GRAPH,
            5,
            0L
        );

        assertThat(objectIterator)
            .extracting(OfferLog::getFileName, OfferLog::getAction)
            .containsExactly(
                tuple(prefix + "obj0", OfferLogAction.WRITE),
                tuple(prefix + "obj1", OfferLogAction.WRITE),
                tuple(prefix + "obj2", OfferLogAction.WRITE),
                tuple(prefix + "obj3", OfferLogAction.WRITE),
                tuple(prefix + "obj4", OfferLogAction.WRITE),
                tuple(prefix + "obj2", OfferLogAction.DELETE),
                tuple(prefix + "obj1", OfferLogAction.WRITE),
                tuple("unknown", OfferLogAction.DELETE),
                tuple(prefix + "obj5", OfferLogAction.WRITE),
                tuple(prefix + "obj6", OfferLogAction.WRITE),
                tuple(prefix + "obj7", OfferLogAction.WRITE),
                tuple(prefix + "obj8", OfferLogAction.WRITE),
                tuple(prefix + "obj9", OfferLogAction.WRITE),
                tuple(prefix + "obj0", OfferLogAction.DELETE),
                tuple(prefix + "obj9", OfferLogAction.DELETE),
                tuple(prefix + "obj2", OfferLogAction.DELETE)
            );
    }

    @Test
    public void testAccessFreshlyWrittenObjects() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        File randomFile = createRandomFile(RandomUtils.nextInt(50_000, 120_000));

        ExecutorService executorService = Executors.newFixedThreadPool(5, VitamThreadFactory.getInstance());
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String objectName = prefix + "obj" + i;
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(
                () -> {
                    try {
                        VitamThreadUtils.getVitamSession().setTenantId(0);
                        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());

                        writeToOffers(randomFile, DEFAULT_STRATEGY, UNIT, objectName);
                        readAndValidateObjectFromTapeOffer(UNIT, objectName, randomFile);
                    } catch (Exception e) {
                        LOGGER.error("Writing object " + objectName + " failed with exception", e);
                        throw new RuntimeException("Writing object " + objectName + " failed with exception", e);
                    }
                },
                executorService
            );
            completableFutures.add(completableFuture);
        }

        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();

        for (CompletableFuture<Void> completableFuture : completableFutures) {
            assertThat(completableFuture).isCompleted();
            assertThat(completableFuture).hasNotFailed();
        }

        executorService.shutdown();
    }

    @Test
    public void fullOfferSyncFromExpirableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(3);

        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            files.add(createRandomFile(120_000));
            objectNames.add(prefix + "obj" + i);
        }
        bulkWriteToOffers(OFFER1_ONLY_STRATEGY, OBJECT, files, objectNames);

        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(2));

        writeToOffers(files.get(1), OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(1));

        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, "unknown");

        for (int i = 5; i < 10; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(500_000, 600_000)));
            objectNames.add(prefix + "obj" + i);
            writeToOffers(files.get(i), OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(i));
        }

        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(0));

        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(9));

        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, objectNames.get(2));

        // Wait for some archives to be written to tapes
        awaitFullObjectArchivalOnTape(3, OBJECT, List.of(objectNames.get(1), objectNames.get(3)), PROD_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check that (at least some) objects are not available for immediate access
        checkObjectAvailabilityFromTapeOffer(OBJECT, objectNames, false);

        // Check offer2 is empty
        try (
            CloseableIterator<ObjectEntry> objectIterator = storageClient.listContainer(
                OFFER2_ONLY_STRATEGY,
                null,
                OBJECT
            )
        ) {
            assertThat(objectIterator).isEmpty();
        }

        Iterator<OfferLog> objectIterator = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            OFFER2_ONLY_STRATEGY,
            null,
            Order.ASC,
            OBJECT,
            5,
            0L
        );
        assertThat(objectIterator).isEmpty();

        // When
        OfferSyncRequest offerSyncRequest = new OfferSyncRequest()
            .setSourceOffer(OFFER_1)
            .setTargetOffer(OFFER_2)
            .setContainer(OBJECT.getCollectionName())
            .setTenantId(VitamThreadUtils.getVitamSession().getTenantId())
            .setStrategyId(DEFAULT_STRATEGY)
            .setOffset(0L);

        Response<Void> offerSyncStatus = offerSyncAdminResource
            .startSynchronization(offerSyncRequest, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(offerSyncStatus.code()).isEqualTo(200);

        awaitSynchronizationTermination();

        verifyOfferSyncStatus();

        // Check object existence on both offers
        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(1));
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(1), files.get(1));

        checkObjectExistence(OFFER_IDS, OBJECT, objectNames.get(3));
        checkObjectDigest(OFFER_IDS, OBJECT, objectNames.get(3), files.get(3));

        checkObjectNotFound(OFFER_IDS, OBJECT, objectNames.get(0));
        checkObjectNotFound(OFFER_IDS, OBJECT, objectNames.get(2));
        checkObjectNotFound(OFFER_IDS, OBJECT, "unknown");

        // Check container entries & offer log
        try (
            CloseableIterator<ObjectEntry> objectIteratorAfterSync = storageClient.listContainer(
                OFFER2_ONLY_STRATEGY,
                null,
                OBJECT
            )
        ) {
            assertThat(objectIteratorAfterSync)
                .usingFieldByFieldElementComparator()
                .containsExactly(
                    new ObjectEntry(prefix + "obj1", files.get(1).length()),
                    new ObjectEntry(prefix + "obj3", files.get(3).length()),
                    new ObjectEntry(prefix + "obj4", files.get(4).length()),
                    new ObjectEntry(prefix + "obj5", files.get(5).length()),
                    new ObjectEntry(prefix + "obj6", files.get(6).length()),
                    new ObjectEntry(prefix + "obj7", files.get(7).length()),
                    new ObjectEntry(prefix + "obj8", files.get(8).length())
                );
        }

        Iterator<OfferLog> objectIteratorAfterSync = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            OFFER2_ONLY_STRATEGY,
            null,
            Order.ASC,
            OBJECT,
            5,
            0L
        );

        assertThat(objectIteratorAfterSync)
            .extracting(OfferLog::getFileName, OfferLog::getAction)
            .containsExactlyInAnyOrder(
                tuple(prefix + "obj0", OfferLogAction.DELETE),
                tuple(prefix + "obj9", OfferLogAction.DELETE),
                tuple(prefix + "obj2", OfferLogAction.DELETE),
                tuple("unknown", OfferLogAction.DELETE),
                tuple(prefix + "obj1", OfferLogAction.WRITE),
                tuple(prefix + "obj3", OfferLogAction.WRITE),
                tuple(prefix + "obj4", OfferLogAction.WRITE),
                tuple(prefix + "obj5", OfferLogAction.WRITE),
                tuple(prefix + "obj6", OfferLogAction.WRITE),
                tuple(prefix + "obj7", OfferLogAction.WRITE),
                tuple(prefix + "obj8", OfferLogAction.WRITE)
            );
    }

    @Test
    public void partialOfferSyncFromExpirableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(4);

        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        // write obj1 on 4_object container of offer1
        File objFile1 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile1, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj1");

        // write obj2 on 4_object container of offer1
        File objFile2 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile2, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj2");

        // write obj3 on 4_object container of offer1 & offer2
        File objFile3 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile3, DEFAULT_STRATEGY, OBJECT, prefix + "obj3");

        // write obj4 on 4_object container of offer2
        File objFile4 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile4, OFFER2_ONLY_STRATEGY, OBJECT, prefix + "obj4");

        // write obj5 on 4_object container of offer2
        File objFile5 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile5, OFFER2_ONLY_STRATEGY, OBJECT, prefix + "obj5");

        // write unit1 on 4_unit container of offer1
        File unitFile1 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(unitFile1, OFFER1_ONLY_STRATEGY, UNIT, prefix + "unit1.json");

        // write unit2 on 5_unit container of offer1
        VitamThreadUtils.getVitamSession().setTenantId(5);
        File unitFile2 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(unitFile2, OFFER1_ONLY_STRATEGY, UNIT, prefix + "unit2.json");

        // Wait for (at least some) archives to be written to tapes
        awaitFullObjectArchivalOnTape(4, OBJECT, List.of(prefix + "obj1"), PROD_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Check that at least some objects are not available for immediate access from container 4_objects
        VitamThreadUtils.getVitamSession().setTenantId(4);
        checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(prefix + "obj1", prefix + "obj2", prefix + "obj3"), false);

        // When
        OfferPartialSyncRequest offerSyncRequest = new OfferPartialSyncRequest()
            .setSourceOffer(OFFER_1)
            .setTargetOffer(OFFER_2)
            .setItemsToSynchronize(
                List.of(
                    new OfferPartialSyncItem()
                        .setContainer(OBJECT.getCollectionName())
                        .setFilenames(List.of(prefix + "obj2", prefix + "obj3", prefix + "obj4"))
                        .setTenantId(4),
                    new OfferPartialSyncItem()
                        .setContainer(UNIT.getCollectionName())
                        .setFilenames(List.of(prefix + "unit1.json", prefix + "unit2.json"))
                        .setTenantId(5)
                )
            )
            .setStrategyId(DEFAULT_STRATEGY);

        VitamThreadUtils.getVitamSession().setTenantId(1);
        Response<Void> offerSyncStatus = offerSyncAdminResource
            .startSynchronization(offerSyncRequest, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(offerSyncStatus.code()).isEqualTo(200);

        awaitSynchronizationTermination();

        verifyOfferSyncStatus();

        // Check obj1 not synchronized
        VitamThreadUtils.getVitamSession().setTenantId(4);
        checkObjectExistence(List.of(OFFER_1), OBJECT, prefix + "obj1");
        checkObjectDigest(List.of(OFFER_1), OBJECT, prefix + "obj1", objFile1);
        checkObjectNotFound(List.of(OFFER_2), OBJECT, prefix + "obj1");

        // Check obj2 & obj3 existence on both offers
        checkObjectExistence(OFFER_IDS, OBJECT, prefix + "obj2");
        checkObjectDigest(OFFER_IDS, OBJECT, prefix + "obj2", objFile2);

        checkObjectExistence(OFFER_IDS, OBJECT, prefix + "obj3");
        checkObjectDigest(OFFER_IDS, OBJECT, prefix + "obj3", objFile3);

        // Check obj4 removed from offer2 since not existing in offer1
        checkObjectNotFound(OFFER_IDS, OBJECT, prefix + "obj4");

        // Check obj5 not removed from offer2 since not synchronized
        checkObjectExistence(List.of(OFFER_2), OBJECT, prefix + "obj5");
        checkObjectDigest(List.of(OFFER_2), OBJECT, prefix + "obj5", objFile5);

        // Check unit1 not copied (wrong tenant)
        VitamThreadUtils.getVitamSession().setTenantId(5);
        checkObjectNotFound(OFFER_IDS, UNIT, prefix + "obj4");

        // Check unit2 copied
        checkObjectExistence(OFFER_IDS, UNIT, prefix + "unit2.json");
        checkObjectDigest(OFFER_IDS, UNIT, prefix + "unit2.json", unitFile2);

        // Ensure obj2 & obj3 copied to 4_object container, obj4 removed from 4_object container
        VitamThreadUtils.getVitamSession().setTenantId(4);
        try (
            CloseableIterator<ObjectEntry> iterator = storageClient.listContainer(OFFER2_ONLY_STRATEGY, null, OBJECT)
        ) {
            assertThat(iterator)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                    new ObjectEntry(prefix + "obj2", objFile2.length()),
                    new ObjectEntry(prefix + "obj3", objFile3.length()),
                    new ObjectEntry(prefix + "obj5", objFile5.length())
                );
        }

        Iterator<OfferLog> objectIteratorAfterSync = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            OFFER2_ONLY_STRATEGY,
            null,
            Order.ASC,
            OBJECT,
            5,
            0L
        );

        assertThat(objectIteratorAfterSync)
            .extracting(OfferLog::getFileName, OfferLog::getAction)
            .containsExactlyInAnyOrder(
                // Initial OfferLogs (before offer sync)
                tuple(prefix + "obj3", OfferLogAction.WRITE),
                tuple(prefix + "obj4", OfferLogAction.WRITE),
                tuple(prefix + "obj5", OfferLogAction.WRITE),
                // New OfferLogs
                tuple(prefix + "obj2", OfferLogAction.WRITE),
                tuple(prefix + "obj3", OfferLogAction.WRITE),
                tuple(prefix + "obj4", OfferLogAction.DELETE)
            );

        // Ensure unit1 NOT copied to 4_unit container
        VitamThreadUtils.getVitamSession().setTenantId(4);
        try (CloseableIterator<ObjectEntry> iterator = storageClient.listContainer(OFFER2_ONLY_STRATEGY, null, UNIT)) {
            assertThat(iterator).isEmpty();
        }

        VitamThreadUtils.getVitamSession().setTenantId(4);
        Iterator<OfferLog> unit4IteratorAfterSync = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            OFFER2_ONLY_STRATEGY,
            null,
            Order.ASC,
            UNIT,
            5,
            0L
        );
        assertThat(unit4IteratorAfterSync).isEmpty();

        // Ensure unit2 copied to 5_unit container, and unit1 removed since not found (wrong tenant)
        VitamThreadUtils.getVitamSession().setTenantId(5);
        try (CloseableIterator<ObjectEntry> iterator = storageClient.listContainer(OFFER2_ONLY_STRATEGY, null, UNIT)) {
            assertThat(iterator)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(new ObjectEntry(prefix + "unit2.json", unitFile2.length()));
        }

        VitamThreadUtils.getVitamSession().setTenantId(5);
        Iterator<OfferLog> unit5IteratorAfterSync = new StorageClientOfferLogIterator(
            StorageClientFactory.getInstance(),
            OFFER2_ONLY_STRATEGY,
            null,
            Order.ASC,
            UNIT,
            5,
            0L
        );

        assertThat(unit5IteratorAfterSync)
            .extracting(OfferLog::getFileName, OfferLog::getAction)
            .containsExactlyInAnyOrder(
                tuple(prefix + "unit1.json", OfferLogAction.DELETE),
                tuple(prefix + "unit2.json", OfferLogAction.WRITE)
            );
    }

    @Test
    public void testOfferDiff() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        VitamThreadUtils.getVitamSession().setTenantId(0);

        // write obj1 on 0_logbook container of offer1
        File objFile1 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile1, OFFER1_ONLY_STRATEGY, LOGBOOK, prefix + "logbook1");

        // write obj2 on 0_logbook container of offer1 & offer2
        File objFile2 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile2, DEFAULT_STRATEGY, LOGBOOK, prefix + "logbook2");

        // write obj3 on 0_logbook container of offer1 & offer2 with distinct content
        File objFile3Offer1 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile3Offer1, OFFER1_ONLY_STRATEGY, LOGBOOK, prefix + "logbook3");

        File objFile3Offer2 = createRandomFile(RandomUtils.nextInt(800_000, 900_000));
        writeToOffers(objFile3Offer2, OFFER2_ONLY_STRATEGY, LOGBOOK, prefix + "logbook3");

        // write obj4 on 0_logbook container of offer2
        File objFile4 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(objFile4, OFFER2_ONLY_STRATEGY, LOGBOOK, prefix + "logbook4");

        // write unit1 on 1_logbook container of offer1
        VitamThreadUtils.getVitamSession().setTenantId(1);
        File unitFile2 = createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000));
        writeToOffers(unitFile2, OFFER1_ONLY_STRATEGY, LOGBOOK, prefix + "unit1.json");

        // Wait for (at least some) archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, LOGBOOK, List.of(prefix + "logbook1"), TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // When
        OfferDiffRequest offerDiffRequest = new OfferDiffRequest()
            .setOffer1(OFFER_1)
            .setOffer2(OFFER_2)
            .setContainer(LOGBOOK.getCollectionName())
            .setTenantId(0);

        VitamThreadUtils.getVitamSession().setTenantId(0);
        Response<Void> offerSyncStatus = offerDiffAdminResource
            .startOfferDiff(offerDiffRequest, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(offerSyncStatus.code()).isEqualTo(200);

        awaitDiffTermination();

        Response<OfferDiffStatus> offerDiffStatusResponse = offerDiffAdminResource
            .getLastOfferDiffStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerDiffStatusResponse.code()).isEqualTo(200);
        OfferDiffStatus offerDiffStatus = offerDiffStatusResponse.body();
        assertNotNull(offerDiffStatus);
        assertThat(offerDiffStatus.getStartDate()).isNotNull();
        assertThat(offerDiffStatus.getEndDate()).isNotNull();
        assertThat(offerDiffStatus.getOffer1()).isEqualTo(OFFER_1);
        assertThat(offerDiffStatus.getOffer2()).isEqualTo(OFFER_2);
        assertThat(offerDiffStatus.getStatusCode()).isEqualTo(StatusCode.WARNING);
        assertThat(offerDiffStatus.getErrorCount()).isEqualTo(3);

        // Check anomalies in report file
        String reportFileName = offerDiffStatus.getReportFileName();
        assertThat(reportFileName).startsWith(VitamConfiguration.getVitamTmpFolder());
        SafeFileChecker.checkSafeFilePath(
            VitamConfiguration.getVitamTmpFolder(),
            reportFileName.substring(VitamConfiguration.getVitamTmpFolder().length() + 1).split(File.separator)
        );

        try (
            FileInputStream inputStream = new FileInputStream(reportFileName);
            JsonLineGenericIterator<ReportEntry> iterator = new JsonLineGenericIterator<>(
                inputStream,
                OFFER_DIFF_ENTRY_TYPE
            )
        ) {
            assertThat(iterator)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                    new ReportEntry()
                        .setObjectId(prefix + "logbook1")
                        .setSizeInOffer1(objFile1.length())
                        .setSizeInOffer2(null),
                    new ReportEntry()
                        .setObjectId(prefix + "logbook3")
                        .setSizeInOffer1(objFile3Offer1.length())
                        .setSizeInOffer2(objFile3Offer2.length()),
                    new ReportEntry()
                        .setObjectId(prefix + "logbook4")
                        .setSizeInOffer1(null)
                        .setSizeInOffer2(objFile4.length())
                );
        }
    }

    @Test
    public void ensureUnloadedTapeIsProperlyLoadedWhenDataAccessRequired() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        // Write some data to "admin" bucket data
        VitamThreadUtils.getVitamSession().setTenantId(1);

        File file1 = createRandomFile(RandomUtils.nextInt(10_000, 20_000));
        String objName1 = prefix + "obj1";
        writeToOffers(file1, OFFER1_ONLY_STRATEGY, OBJECT, objName1);

        // Write dummy objects to drain incomplete tars
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(1, OBJECT, List.of(objName1), ADMIN_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Make tape library really slow so that all drives are active
        tapeLibrarySimulatorRule.getTapeLibrarySimulator().setSleepDelayMillis(500);

        List<File> objectFiles = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            objectFiles.add(createRandomFile(RandomUtils.nextInt(1_000_000, 1_200_000)));
            objectNames.add(prefix + "obj" + i);
        }

        // Flooding the tape library with "test" & "prod" bucket data so that "admin" bucket tapes are unloaded
        VitamThreadUtils.getVitamSession().setTenantId(0);
        bulkWriteToOffers(OFFER1_ONLY_STRATEGY, OBJECT, objectFiles, objectNames);

        VitamThreadUtils.getVitamSession().setTenantId(2);
        bulkWriteToOffers(OFFER1_ONLY_STRATEGY, OBJECT, objectFiles, objectNames);

        // Write dummy objects to drain incomplete tars
        VitamThreadUtils.getVitamSession().setTenantId(0);
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        VitamThreadUtils.getVitamSession().setTenantId(2);
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, objectNames, TEST_BUCKET);
        awaitFullObjectArchivalOnTape(2, OBJECT, objectNames, PROD_BUCKET);

        // Ensure that "admin" tapes are no more loaded
        List<TapeCatalog> tapes = tapeCatalogRepository.findTapes(
            List.of(new QueryCriteria(TapeCatalog.BUCKET, ADMIN_BUCKET, QueryCriteriaOperator.EQ))
        );
        assertThat(tapes).isNotEmpty();
        assertThat(tapes).allMatch(tape -> tape.getCurrentLocation().getLocationType() == TapeLocationType.SLOT);

        // Reset tape library speed
        tapeLibrarySimulatorRule.getTapeLibrarySimulator().setSleepDelayMillis(10);

        // Await data to be available
        VitamThreadUtils.getVitamSession().setTenantId(1);

        String accessRequestId = createAccessRequest(OFFER_1, OBJECT, List.of(objName1));
        awaitAccessRequestReadiness(accessRequestId);

        // Ensure data is available for immediate access
        checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(objName1), true);

        readAndValidateObjectFromTapeOffer(OBJECT, objName1, file1);

        // Cleanup
        removeAccessRequest(accessRequestId);
    }

    @Test
    public void checkDigestValidation() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        VitamThreadUtils.getVitamSession().setTenantId(0);

        List<File> files = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            files.add(createRandomFile(RandomUtils.nextInt(500_000, 600_000)));
            objectNames.add(prefix + "unit" + i);
        }
        bulkWriteToOffers(OFFER1_ONLY_STRATEGY, UNIT, files, objectNames);

        // Await for object to become fully archived on tape / in cache
        String objectName = objectNames.get(0);
        awaitFullObjectArchivalOnTape(0, UNIT, List.of(objectName), TEST_BUCKET);

        // Check availability in cache
        checkObjectAvailabilityFromTapeOffer(UNIT, List.of(objectName), true);

        // Corrupt digest in DB
        TapeObjectReferentialEntity tapeObjectReferentialEntity = objectReferentialRepository
            .find(buildContainerName(UNIT, "0"), objectName)
            .orElseThrow();
        tapeObjectReferentialEntity.setDigest("BAD");
        objectReferentialRepository.insertOrUpdate(tapeObjectReferentialEntity);

        // Try read
        javax.ws.rs.core.Response response = storageClient.getContainerAsync(
            DEFAULT_STRATEGY,
            objectName,
            UNIT,
            AccessLogUtils.getNoLogAccessLog()
        );

        try (InputStream inputStream = response.readEntity(InputStream.class)) {
            // assert that object cannot be read fully (an exception is thrown / logged by offer service)
            assertThatThrownBy(
                () -> IOUtils.consume(new ExactSizeInputStream(inputStream, files.get(0).length()))
            ).isInstanceOf(IOException.class);
        }
    }

    @Test
    public void testAccessRequestToDataBeingUpdated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        File file1 = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        writeToOffers(file1, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj1");

        File file2 = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        writeToOffers(file2, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj2");

        // Write dummy objects to drain incomplete tars
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, List.of(prefix + "obj1", prefix + "obj2"), TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Ensure that objects in OBJECT expired / are not available for immediate access
        checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(prefix + "obj1", prefix + "obj2", prefix + "obj3"), false);

        // Create access request and wait for data availability
        String accessRequestId = createAccessRequest(
            OFFER_1,
            OBJECT,
            List.of(prefix + "obj1", prefix + "obj2", prefix + "obj3", prefix + "obj4")
        );
        awaitAccessRequestReadiness(accessRequestId);

        // Write obj3
        File file3 = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        writeToOffers(file3, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj3");

        // Delete obj2
        deleteObject(OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj2");

        // Ensure access request is still ready
        checkAccessRequestStatus(accessRequestId, AccessRequestStatus.READY);

        // Ensure all objects became available for immediate access
        checkObjectAvailabilityFromTapeOffer(
            OBJECT,
            List.of(prefix + "obj1", prefix + "obj2", prefix + "obj3", prefix + "obj4"),
            true
        );

        readAndValidateObjectFromTapeOffer(OBJECT, prefix + "obj1", file1);
        checkObjectNotFound(List.of(OFFER_1), OBJECT, prefix + "obj2");
        readAndValidateObjectFromTapeOffer(OBJECT, prefix + "obj3", file3);
        checkObjectNotFound(List.of(OFFER_1), OBJECT, prefix + "obj4");

        // Cleanup access request
        removeAccessRequest(accessRequestId);
    }

    @Test
    public void readObjectBeingConcurrentlyUpdated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        File file1 = createRandomFile(RandomUtils.nextInt(10_000_000, 12_000_000));
        File file1_bis = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));

        // Write V1
        writeToOffers(file1, OFFER1_ONLY_STRATEGY, UNIT, prefix + "obj1");

        // Write dummy objects to drain incomplete tars
        writeToOffers(createRandomFile(1_000_000), DEFAULT_STRATEGY, UNIT, prefix + "someDataForIncompleteTarDraining");

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, UNIT, List.of(prefix + "obj1"), TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        try (
            InputStream inputStream = storageClient
                .getContainerAsync(OFFER1_ONLY_STRATEGY, prefix + "obj1", UNIT, AccessLogUtils.getNoLogAccessLog())
                .readEntity(InputStream.class)
        ) {
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);

            // Partial read of object
            IOUtils.consume(new BoundedInputStream(CloseShieldInputStream.wrap(digestInputStream), 123_456L));

            // Update concurrently object with V2
            writeToOffers(file1_bis, OFFER1_ONLY_STRATEGY, UNIT, prefix + "obj1");

            // Continue reading the rest of inputStream
            IOUtils.consume(digestInputStream);

            String receivedDigest = digest.digestHex();

            String expectedDigest = new Digest(VitamConfiguration.getDefaultDigestType()).update(file1).digestHex();

            assertThat(receivedDigest).isEqualTo(expectedDigest);
        }

        readAndValidateObjectFromTapeOffer(UNIT, prefix + "obj1", file1_bis);
    }

    @Test
    public void readFreshlyWrittenObjectBeingConcurrentlyArchivedToTape() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        // Write obj1
        File file1 = createRandomFile(RandomUtils.nextInt(10_000_000, 12_000_000));
        writeToOffers(file1, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj1");

        // Read object right away
        try (
            InputStream inputStream = storageClient
                .getContainerAsync(OFFER1_ONLY_STRATEGY, prefix + "obj1", OBJECT, AccessLogUtils.getNoLogAccessLog())
                .readEntity(InputStream.class)
        ) {
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);

            // Partial read of object
            IOUtils.consume(new BoundedInputStream(CloseShieldInputStream.wrap(digestInputStream), 123_456L));

            // Write dummy objects to drain incomplete tars of file-bucket
            writeToOffers(
                createRandomFile(1_000_000),
                DEFAULT_STRATEGY,
                OBJECT,
                prefix + "someDataForIncompleteTarDraining"
            );

            // Wait for archives to be written to tapes
            awaitFullObjectArchivalOnTape(0, OBJECT, List.of(prefix + "obj1"), TEST_BUCKET);

            // Force cache eviction
            forceCacheEviction();

            // Ensure object is no more available for further downloads
            checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(prefix + "obj1"), false);

            // Continue reading the rest of inputStream
            IOUtils.consume(digestInputStream);

            String receivedDigest = digest.digestHex();

            String expectedDigest = new Digest(VitamConfiguration.getDefaultDigestType()).update(file1).digestHex();

            assertThat(receivedDigest).isEqualTo(expectedDigest);
        }
    }

    @Test
    public void readObjectPreventsCacheExpiration() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String prefix = RandomStringUtils.randomAlphabetic(10) + "_";

        // Write large obj1
        File file1 = createRandomFile(RandomUtils.nextInt(15_000_000, 16_000_000));
        writeToOffers(file1, OFFER1_ONLY_STRATEGY, OBJECT, prefix + "obj1");

        // Write dummy objects to drain incomplete tars of file-bucket
        writeToOffers(
            createRandomFile(1_000_000),
            DEFAULT_STRATEGY,
            OBJECT,
            prefix + "someDataForIncompleteTarDraining"
        );

        // Wait for archives to be written to tapes
        awaitFullObjectArchivalOnTape(0, OBJECT, List.of(prefix + "obj1"), TEST_BUCKET);

        // Force cache eviction
        forceCacheEviction();

        // Ensure object is not available for immediate access
        checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(prefix + "obj1"), false);

        // Create access request for force object availability in cache
        String accessRequestId = createAccessRequest(OFFER_1, OBJECT, List.of(prefix + "obj1"));
        awaitAccessRequestReadiness(accessRequestId);

        // Read object from cache
        try (
            InputStream inputStream = storageClient
                .getContainerAsync(OFFER1_ONLY_STRATEGY, prefix + "obj1", OBJECT, AccessLogUtils.getNoLogAccessLog())
                .readEntity(InputStream.class)
        ) {
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);

            // Partial read of object
            IOUtils.consume(new BoundedInputStream(CloseShieldInputStream.wrap(digestInputStream), 123_456L));

            // Remove access request
            removeAccessRequest(accessRequestId);

            // Force cache eviction
            forceCacheEviction();

            // Continue reading the rest of inputStream
            IOUtils.consume(digestInputStream);

            String receivedDigest = digest.digestHex();

            String expectedDigest = new Digest(VitamConfiguration.getDefaultDigestType()).update(file1).digestHex();

            assertThat(receivedDigest).isEqualTo(expectedDigest);
        }

        // Force cache eviction
        forceCacheEviction();

        // Ensure object have been evicted now since no more being actively accessed
        checkObjectAvailabilityFromTapeOffer(OBJECT, List.of(prefix + "obj1"), false);
    }

    @Test
    public void testPutBackupArchives() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        // Insert 2 backup  archives
        File backupFile1 = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        Response<Void> response1 = tapeBackupAdminResource.putBackupObject("backupArchive1.zip", backupFile1).execute();

        File backupFile2 = createRandomFile(RandomUtils.nextInt(800_000, 1_200_000));
        Response<Void> response2 = tapeBackupAdminResource.putBackupObject("backupArchive2.zip", backupFile2).execute();

        // Check response status codes
        assertThat(response1.code()).isEqualTo(CREATED.getStatusCode());
        assertThat(response2.code()).isEqualTo(CREATED.getStatusCode());

        // Ensure archives end up being persisted on tape
        StopWatch stopWatch = StopWatch.createStarted();
        while (true) {
            Thread.sleep(100);

            List<TapeArchiveReferentialEntity> tapeArchiveReferentialEntities = archiveReferentialRepository.bulkFind(
                Set.of("backupArchive1.zip", "backupArchive2.zip")
            );

            assertThat(tapeArchiveReferentialEntities).hasSize(2);

            boolean allBackupArchivesPersistedOnTape = tapeArchiveReferentialEntities
                .stream()
                .allMatch(archive -> archive.getLocation() instanceof TapeLibraryOnTapeArchiveStorageLocation);
            if (allBackupArchivesPersistedOnTape) {
                break;
            }

            assertThat(stopWatch.getTime(TimeUnit.SECONDS))
                .withFailMessage("Backup archives persistence took too long")
                .isLessThan(60);
        }
    }

    private void awaitFullObjectArchivalOnTape(
        int tenantId,
        DataCategory dataCategory,
        List<String> objectNames,
        String bucket
    ) throws ObjectReferentialException, ArchiveReferentialException, InterruptedException, TapeCatalogException {
        StopWatch stopWatch = StopWatch.createStarted();

        do {
            Thread.sleep(1000);

            List<TapeObjectReferentialEntity> objectEntities = objectReferentialRepository.bulkFind(
                buildContainerName(dataCategory, Integer.toString(tenantId)),
                new HashSet<>(objectNames)
            );

            assertThat(objectEntities).hasSize(objectNames.size());

            boolean allObjectsOnTar = objectEntities
                .stream()
                .map(TapeObjectReferentialEntity::getLocation)
                .allMatch(location -> location instanceof TapeLibraryTarObjectStorageLocation);

            if (!allObjectsOnTar) {
                LOGGER.warn("Some objects are not yet packaged into tars");
                continue;
            }
            LOGGER.info("All objects are packaged into tars");

            Set<String> tarIds = objectEntities
                .stream()
                .map(TapeObjectReferentialEntity::getLocation)
                .map(location -> (TapeLibraryTarObjectStorageLocation) location)
                .map(TapeLibraryTarObjectStorageLocation::getTarEntries)
                .flatMap(Collection::stream)
                .map(TarEntryDescription::getTarFileId)
                .collect(Collectors.toSet());

            List<TapeArchiveReferentialEntity> tapeArchiveReferentialEntities = archiveReferentialRepository.bulkFind(
                tarIds
            );
            assertThat(tapeArchiveReferentialEntities).hasSameSizeAs(tarIds);

            boolean allTarsOnTape = tapeArchiveReferentialEntities
                .stream()
                .map(TapeArchiveReferentialEntity::getLocation)
                .allMatch(location -> location instanceof TapeLibraryOnTapeArchiveStorageLocation);

            if (!allTarsOnTape) {
                LOGGER.warn("Some tars are not yet archived into tapes");
                continue;
            }

            LOGGER.info("All tars are archived into tapes");

            // Check bucket
            Set<String> tapeCodes = tapeArchiveReferentialEntities
                .stream()
                .map(
                    archiveEntity ->
                        ((TapeLibraryOnTapeArchiveStorageLocation) archiveEntity.getLocation()).getTapeCode()
                )
                .collect(Collectors.toSet());

            for (String tapeCode : tapeCodes) {
                List<TapeCatalog> tapeCatalogs = tapeCatalogRepository.findTapes(
                    List.of(new QueryCriteria(TapeCatalog.CODE, tapeCode, QueryCriteriaOperator.EQ))
                );

                assertThat(tapeCatalogs).hasSize(1);
                assertThat(tapeCatalogs.get(0).getBucket()).isEqualTo(bucket);
                assertThat(tapeCatalogs.get(0).getTapeState()).isIn(TapeState.OPEN, TapeState.FULL);
            }

            return;
        } while (stopWatch.getTime(TimeUnit.SECONDS) < 30);

        fail("Object archival to tars took too long");
    }

    private void checkObjectDigest(
        List<String> offerIds,
        DataCategory dataCategory,
        String objectName,
        File expectedContent
    ) throws StorageClientException, IOException {
        String expectedObjectDigest = new Digest(DigestType.SHA512).update(expectedContent).toString();

        checkObjectDigest(offerIds, dataCategory, objectName, expectedObjectDigest, false);
        checkObjectDigest(offerIds, dataCategory, objectName, expectedObjectDigest, true);
    }

    private void checkObjectDigest(
        List<String> offerIds,
        DataCategory dataCategory,
        String objectName,
        String expectedObjectDigest,
        boolean noCache
    ) throws StorageClientException {
        JsonNode objectDigests = storageClient.getInformation(
            DEFAULT_STRATEGY,
            dataCategory,
            objectName,
            offerIds,
            noCache
        );

        assertThat(objectDigests.size()).isEqualTo(offerIds.size());
        for (String offerId : offerIds) {
            assertThat(objectDigests.get(offerId).get("digest").asText()).isEqualTo(expectedObjectDigest);
        }
    }

    private void checkBulkObjectDigests(
        List<String> offerIds,
        DataCategory dataCategory,
        List<String> objectNames,
        List<File> files
    ) throws IOException, StorageServerClientException {
        RequestResponseOK<BatchObjectInformationResponse> batchObjectInformation = (RequestResponseOK<
                BatchObjectInformationResponse
            >) storageClient.getBatchObjectInformation(DEFAULT_STRATEGY, dataCategory, offerIds, objectNames);

        assertThat(batchObjectInformation.getResults()).hasSize(objectNames.size());

        Map<String, Map<String, String>> offerDigestsByObjectId = batchObjectInformation
            .getResults()
            .stream()
            .collect(
                Collectors.toMap(
                    BatchObjectInformationResponse::getObjectId,
                    BatchObjectInformationResponse::getOfferDigests
                )
            );

        assertThat(offerDigestsByObjectId.keySet()).containsExactlyInAnyOrderElementsOf(objectNames);

        for (int i = 0; i < objectNames.size(); i++) {
            String objectName = objectNames.get(i);
            String expectedObjectDigest = new Digest(DigestType.SHA512).update(files.get(i)).toString();

            Map<String, String> offerDigests = offerDigestsByObjectId.get(objectName);
            assertThat(offerDigests).containsOnlyKeys(offerIds.toArray(String[]::new));
            assertThat(offerDigests.values()).containsOnly(expectedObjectDigest);
        }
    }

    private void awaitAccessRequestReadiness(String accessRequestId) throws StorageClientException {
        StopWatch stopWatch = StopWatch.createStarted();

        do {
            Map<String, AccessRequestStatus> accessRequestStatusMap = storageClient.checkAccessRequestStatuses(
                DEFAULT_STRATEGY,
                OFFER_1,
                List.of(accessRequestId),
                false
            );

            if (accessRequestStatusMap.get(accessRequestId) == AccessRequestStatus.READY) {
                return;
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        } while (stopWatch.getTime(TimeUnit.SECONDS) < 10);

        fail("AccessRequestId " + accessRequestId + " took too long to become ready");
    }

    private String createAccessRequest(String explicitOfferId, DataCategory dataCategory, List<String> objectNames)
        throws StorageServerClientException {
        Optional<String> accessRequestForUnits = storageClient.createAccessRequestIfRequired(
            DEFAULT_STRATEGY,
            explicitOfferId,
            dataCategory,
            objectNames
        );

        return accessRequestForUnits.orElseThrow(
            () -> new IllegalStateException("Expected accessRequestId to be created for offer " + explicitOfferId)
        );
    }

    private void checkAccessRequestStatus(String accessRequestId, AccessRequestStatus expectedStatus)
        throws StorageServerClientException, StorageIllegalOperationClientException {
        Map<String, AccessRequestStatus> accessRequestStatusMap = storageClient.checkAccessRequestStatuses(
            DEFAULT_STRATEGY,
            OFFER_1,
            List.of(accessRequestId),
            false
        );

        assertThat(accessRequestStatusMap.get(accessRequestId)).isEqualTo(expectedStatus);
    }

    private void removeAccessRequest(String accessRequestId)
        throws StorageServerClientException, StorageIllegalOperationClientException {
        storageClient.removeAccessRequest(DEFAULT_STRATEGY, OFFER_1, accessRequestId, false);
    }

    private void writeToOffers(File file, String strategy, DataCategory dataCategory, String objectName)
        throws IOException, ContentAddressableStorageException, StorageClientException {
        String containerName = "container-" + RandomStringUtils.randomAlphabetic(10);

        workspaceClient.createContainer(containerName);

        try (InputStream inputStream = new FileInputStream(file)) {
            workspaceClient.putObject(containerName, objectName, inputStream);
        }

        storageClient.storeFileFromWorkspace(
            strategy,
            dataCategory,
            objectName,
            new ObjectDescription(dataCategory, containerName, objectName, objectName)
        );

        workspaceClient.deleteContainer(containerName, true);
    }

    private void bulkWriteToOffers(
        String strategy,
        DataCategory dataCategory,
        List<File> files,
        List<String> objectNames
    ) throws IOException, ContentAddressableStorageException, StorageClientException {
        String containerName = "container1";
        workspaceClient.createContainer(containerName);

        assertThat(files).hasSameSizeAs(objectNames);

        for (int i = 0, filesSize = files.size(); i < filesSize; i++) {
            File file = files.get(i);
            String objectName = objectNames.get(i);
            try (InputStream inputStream = new FileInputStream(file)) {
                workspaceClient.putObject(containerName, objectName, inputStream);
            }
        }

        storageClient.bulkStoreFilesFromWorkspace(
            strategy,
            new BulkObjectStoreRequest(containerName, objectNames, dataCategory, objectNames)
        );

        workspaceClient.deleteContainer(containerName, true);
    }

    private void checkObjectAvailabilityFromTapeOffer(
        DataCategory dataCategory,
        List<String> objectNames,
        boolean shouldBeAvailable
    ) throws StorageServerClientException {
        BulkObjectAvailabilityRequest request = new BulkObjectAvailabilityRequest(dataCategory, objectNames);

        BulkObjectAvailabilityResponse bulkObjectAvailabilityResponse = storageClient.checkBulkObjectAvailability(
            OFFER1_ONLY_STRATEGY,
            OFFER_1,
            request
        );

        assertThat(bulkObjectAvailabilityResponse.getAreObjectsAvailable())
            .withFailMessage(
                "Expected objects " +
                objectNames +
                " of " +
                dataCategory +
                " to" +
                (shouldBeAvailable ? "" : " NOT") +
                " be available"
            )
            .isEqualTo(shouldBeAvailable);
    }

    private void readAndValidateObjectFromTapeOffer(
        DataCategory dataCategory,
        String objectName,
        File expectedFileContent
    ) throws IOException, StorageException, StorageClientException {
        try (
            InputStream is = storageClient
                .getContainerAsync(
                    OFFER1_ONLY_STRATEGY,
                    OFFER_1,
                    objectName,
                    dataCategory,
                    AccessLogUtils.getNoLogAccessLog()
                )
                .readEntity(InputStream.class);
            InputStream expectedContent = new FileInputStream(expectedFileContent)
        ) {
            assertThat(is).hasSameContentAs(expectedContent);
        }
    }

    private void checkObjectExistence(List<String> offerIds, DataCategory dataCategory, String objectName)
        throws StorageClientException {
        Map<String, Boolean> objectByOfferId = storageClient.exists(
            DEFAULT_STRATEGY,
            dataCategory,
            objectName,
            offerIds
        );

        assertThat(objectByOfferId.keySet()).containsExactlyInAnyOrderElementsOf(offerIds);
        assertThat(objectByOfferId.values()).containsOnly(true);
    }

    private void checkObjectNotFound(List<String> offerIds, DataCategory dataCategory, String objectName)
        throws StorageClientException {
        Map<String, Boolean> objectByOfferId = storageClient.exists(
            DEFAULT_STRATEGY,
            dataCategory,
            objectName,
            offerIds
        );

        assertThat(objectByOfferId.keySet()).containsExactlyInAnyOrderElementsOf(offerIds);
        assertThat(objectByOfferId.values())
            .withFailMessage(
                "Expecting " +
                objectName +
                " to not be found in any the the offers " +
                offerIds +
                " but some offers do contain the object: " +
                objectByOfferId
            )
            .containsOnly(false);

        for (String offerId : offerIds) {
            assertThatThrownBy(
                () ->
                    storageClient
                        .getContainerAsync(
                            TapeOfferStorageIT.DEFAULT_STRATEGY,
                            offerId,
                            objectName,
                            dataCategory,
                            AccessLogUtils.getNoLogAccessLog()
                        )
                        .readEntity(InputStream.class)
            ).isInstanceOf(StorageNotFoundException.class);
        }
    }

    private void deleteObject(String strategyId, DataCategory dataCategory, String objectName)
        throws StorageServerClientException {
        storageClient.delete(strategyId, dataCategory, objectName);
    }

    private File createRandomFile(int size) throws IOException {
        byte[] buffer = RandomStringUtils.randomAlphabetic(100).getBytes(StandardCharsets.UTF_8);
        File file = tempFolder.newFile();
        FileUtils.copyToFile(new CircularInputStream(buffer, size), file);
        return file;
    }

    private void verifyOfferSyncStatus() throws IOException {
        Response<OfferSyncStatus> offerSyncStatusResponse = offerSyncAdminResource
            .getLastOfferSynchronizationStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerSyncStatusResponse.code()).isEqualTo(200);
        OfferSyncStatus offerSyncStatus = offerSyncStatusResponse.body();
        assertNotNull(offerSyncStatus);
        assertThat(offerSyncStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerSyncStatus.getStartDate()).isNotNull();
        assertThat(offerSyncStatus.getEndDate()).isNotNull();
        assertThat(offerSyncStatus.getSourceOffer()).isEqualTo(OFFER_1);
        assertThat(offerSyncStatus.getTargetOffer()).isEqualTo(OFFER_2);
    }

    private void awaitSynchronizationTermination() throws IOException, InterruptedException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < 120) {
            Response<Void> offerSynchronizationRunning = offerSyncAdminResource
                .isOfferSynchronizationRunning(getBasicAuthnToken())
                .execute();
            assertThat(offerSynchronizationRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerSynchronizationRunning.headers().get("Running"));
            if (isRunning) {
                LOGGER.warn("Offer sync still running...");
                Thread.sleep(1000);
            }
        }
        if (isRunning) {
            fail("Synchronization took too long");
        }
    }

    private void awaitDiffTermination() throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < 15) {
            Response<Void> offerDiffRunning = offerDiffAdminResource.isOfferDiffRunning(getBasicAuthnToken()).execute();
            assertThat(offerDiffRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerDiffRunning.headers().get("Running"));
        }
        if (isRunning) {
            fail("Diff took too long");
        }
    }

    private void forceCacheEviction() throws IllegalPathException, InterruptedException {
        ArchiveCacheStorage archiveCacheStorage = TapeLibraryFactory.getInstance().getArchiveCacheStorage();
        archiveCacheStorage.reserveArchiveStorageSpace("test-metadata", "dummyTarForCachePressure.tar", 2_000_000L);
        while (archiveCacheStorage.isCacheEvictionRunning()) {
            Thread.sleep(100);
        }
        archiveCacheStorage.cancelReservedArchive("test-metadata", "dummyTarForCachePressure.tar");
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface OfferSyncAdminResource {
        @POST("/storage/v1/offerPartialSync")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startSynchronization(
            @Body OfferPartialSyncRequest offerPartialSyncRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @POST("/storage/v1/offerSync")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startSynchronization(
            @Body OfferSyncRequest offerSyncRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @HEAD("/storage/v1/offerSync")
        Call<Void> isOfferSynchronizationRunning(@Header("Authorization") String basicAuthnToken);

        @GET("/storage/v1/offerSync")
        @Headers({ "Content-Type: application/json" })
        Call<OfferSyncStatus> getLastOfferSynchronizationStatus(@Header("Authorization") String basicAuthnToken);
    }

    public interface OfferDiffAdminResource {
        @POST("/storage/v1/diff")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startOfferDiff(
            @Body OfferDiffRequest offerDiffRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @HEAD("/storage/v1/diff")
        Call<Void> isOfferDiffRunning(@Header("Authorization") String basicAuthnToken);

        @GET("/storage/v1/diff")
        @Headers({ "Content-Type: application/json" })
        Call<OfferDiffStatus> getLastOfferDiffStatus(@Header("Authorization") String basicAuthnToken);
    }

    public interface TapeBackupAdminResource {
        @PUT("/offer/v1/backup/{objectId}")
        @Headers({ "Accept: application/json", "Content-Type: application/octet-stream" })
        Call<Void> putBackupObject(@retrofit2.http.Path("objectId") String objectId, @Body File archiveFile);
    }
}
