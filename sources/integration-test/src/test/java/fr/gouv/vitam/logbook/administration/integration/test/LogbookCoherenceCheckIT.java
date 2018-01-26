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
package fr.gouv.vitam.logbook.administration.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
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
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.administration.core.impl.LogbookCheckConsistencyServiceImpl;
import fr.gouv.vitam.logbook.common.model.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;

/**
 * Test the LogbookDetailsCheck services.
 */
public class LogbookCoherenceCheckIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCoherenceCheckIT.class);

    private static final String DEFAULT_OFFER_CONF = "integration-logbook/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-logbook/logbook.conf";
    private static final String STORAGE_CONF = "integration-logbook/storage-engine.conf";
    private static final String PROCESSING_CONF = "integration-logbook/processing.conf";
    private static final String WORKSPACE_CONF = "integration-logbook/workspace.conf";
    private static final String ADMIN_MANAGEMENT_CONF = "integration-logbook/functional-administration.conf";

    private static final String OFFER_FOLDER = "offer";
    private static final String STRATEGY_ID = "default";
    public static final int TENANT_0 = 0;
    public static final String OBJECT_ID = "objectId";
    public static final String LOCALHOST = "localhost";
    public static final String HTTP_LOCALHOST = "http://localhost:";
    public static final String VITAM_TMP_FOLDER = "vitam.tmp.folder";
    public static final String AGENCIES_CSV = "agencies.csv";

    private static String CONTAINER = "checklogbookreports";
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;
    public static final String VITAM_TEST = "Vitam-Test";

    public static final String CHECK_LOGBOOK_DATA_AGENCIES = "integration-logbook/data/agencies.csv";

    public static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS =
        "ERROR: Exception has been thrown when cleaning offers.";
    public static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANNING_WORKSPACE =
        "ERROR: Exception has been thrown when cleanning workspace:";

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static ProcessManagementMain processManagementApplication;
    private static LogbookMain logbookApplication;


    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferApplication;

    private static AdminManagementMain adminManagementMain;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), VITAM_TEST,
            FunctionalAdminCollections.SECURITY_PROFILE.getName(), FunctionalAdminCollections.AGENCIES.getName());

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
        // launch logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));

        final File logbookConfigFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookConfiguration =
            PropertiesUtils.readYaml(logbookConfigFile, LogbookConfiguration.class);
        logbookConfiguration.setElasticsearchNodes(nodesEs);
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        logbookApplication = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_LOGBOOK));

        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementMain(PROCESSING_CONF);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realAdminConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(elasticsearchRule.getClusterName());
        realAdminConfig.setWorkspaceUrl(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(adminConfig, realAdminConfig);

        // prepare functional admin
        adminManagementMain = new AdminManagementMain(adminConfig.getAbsolutePath());
        adminManagementMain.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_FUNCTIONAL_ADMIN));

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
        if (processManagementApplication != null) {
            processManagementApplication.stop();
        }
        if (adminManagementMain != null) {
            adminManagementMain.stop();
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
     * Logbook's properties check service.
     */
    private LogbookCheckConsistencyService coherenceCheckService;

    @Test
    @RunWithCustomExecutor
    public void testStoreReportsInStorage() throws Exception {
        LOGGER.debug("Starting store logbook coherence checks reports test.");

        List<LogbookCheckResult> logbookCheckResults = Arrays.asList(
            new LogbookCheckResult("operationId01", "lfcId01", "checkedProperty01", "savedLogbookMsg01",
                "expectedLogbookMsg01"),
            new LogbookCheckResult("operationId02", "lfcId02", "checkedProperty02", "savedLogbookMsg02",
                "expectedLogbookMsg02"),
            new LogbookCheckResult("operationId03", "lfcId03", "checkedProperty03", "savedLogbookMsg03",
                "expectedLogbookMsg03")
        );

        final File logbookConfig = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        coherenceCheckService =
            new LogbookCheckConsistencyServiceImpl(configuration, VitamRepositoryFactory.getInstance());

        coherenceCheckService.storeReportsInStorage(logbookCheckResults);

        // verify offer content
        VitamRequestIterator<JsonNode> result =
            storageClient.listContainer(STRATEGY_ID, DataCategory.CHECKLOGBOOKREPORTS);

        TestCase.assertNotNull(result);

        Assert.assertTrue(result.hasNext());
        JsonNode node = result.next();
        Assert.assertNotNull(node.get(OBJECT_ID));
    }

    /**
     * test Logbook Coherence Check.
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testLogbookCoherenceCheck_withoutIncoherentLogbook() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        LOGGER.debug("Starting integration tests for logbook coherence checks.");

        // Import of the agencies referential.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                CHECK_LOGBOOK_DATA_AGENCIES), AGENCIES_CSV);
        }

        // logbook configuration
        final File logbookConfig = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        // get vitamRepository instance.
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        // call the logbook coherence check service
        coherenceCheckService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepository);
        coherenceCheckService.logbookCoherenceCheckByTenant(TENANT_0);

        // verify offer content
        VitamRequestIterator<JsonNode> result =
            storageClient.listContainer(STRATEGY_ID, DataCategory.CHECKLOGBOOKREPORTS);

        Assert.assertFalse(result.hasNext());
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
