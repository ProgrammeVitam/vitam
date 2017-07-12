/**
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
 */
package fr.gouv.vitam.functional.tnr.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.access.external.rest.AccessExternalApplication;
import fr.gouv.vitam.access.internal.rest.AccessInternalApplication;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
import fr.gouv.vitam.ingest.external.rest.IngestExternalApplication;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalApplication;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.server.rest.StorageApplication;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferApplication;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * This class aims to help to launch locally the TNR by launching all necessary components
 */
public class TnrLaunchAllApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TnrLaunchAllApplication.class);

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static LogbookElasticsearchAccess esClient;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final static String CLUSTER_NAME = "vitam-cluster";
    static JunitHelper junitHelper;
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final String WORKSPACE = "/vitam/data/workspace";
    private static final String OFFER = "/vitam/data/offer";
    private static final String OFFER_INGEST = "/vitam/data/wksingest";
    private static final String SIEGFRIED_PATH = "/usr/bin/sf";
    
    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8100;
    private static final int PORT_SERVICE_STORAGE_OFFER = 8101;
    private static final int PORT_SERVICE_SIEGFRIED = 8102;
    private static final int PORT_SERVICE_INGEST_INTERNAL = 8095;
    private static final int PORT_SERVICE_INGEST_EXTERNAL = 8090;
    private static final int PORT_SERVICE_ACCESS_INTERNAL = 8092;
    private static final int PORT_SERVICE_ACCESS_EXTERNAL = 8091;

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_INGEST_INTERNAL_PATH = "";
    private static String CONFIG_ACCESS_INTERNAL_PATH = "";
    private static String CONFIG_INGEST_EXTERNAL_PATH = "";
    private static String CONFIG_ACCESS_EXTERNAL_PATH = "";
    private static String DEFAULT_OFFER_CONF = "";
    private static String STORAGE_CONF = "";

    // private static VitamServer workerApplication;
    private static MetaDataApplication medtadataApplication;
    private static WorkerApplication wkrapplication;
    private static AdminManagementApplication adminApplication;
    private static LogbookApplication lgbapplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;
    private static IngestInternalApplication ingestInternalApplication;
    private static AccessInternalApplication accessInternalApplication;
    private static IngestExternalApplication ingestExternalApplication;
    private static AccessExternalApplication accessExternalApplication;
    private static StorageApplication storageApplication;
    private static StorageClient storageClient;
    private static DefaultOfferApplication defaultOfferApplication;
    private static Process siegfried;
    
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;


    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        STORAGE_CONF = PropertiesUtils.getResourcePath("functional-tnr/storage-engine.conf").toString();
        DEFAULT_OFFER_CONF =
            PropertiesUtils.getResourcePath("functional-tnr/storage-default-offer-nossl.conf").toString();
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("functional-tnr/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("functional-tnr/worker.conf").toString();
        CONFIG_WORKSPACE_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/workspace.conf").toString();
        CONFIG_PROCESSING_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/processing.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/functional-administration.conf").toString();

        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("functional-tnr/logbook.conf").toString();

        CONFIG_INGEST_INTERNAL_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/ingest-internal.conf").toString();
        CONFIG_ACCESS_INTERNAL_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/access-internal.conf").toString();

        CONFIG_INGEST_EXTERNAL_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/ingest-external.conf").toString();
        CONFIG_ACCESS_EXTERNAL_PATH =
            PropertiesUtils.getResourcePath("functional-tnr/access-external.conf").toString();

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        SystemPropertyUtil.refresh();

        
        // MoongoDB and ES
        LOGGER.warn("Start MongoDb and Elasticsearch");
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // ES client
        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", config.getTcpPort()));
        esClient = new LogbookElasticsearchAccess(CLUSTER_NAME, nodes);

        File workspaceRoot = new File(WORKSPACE);
        new File(workspaceRoot, "process").mkdirs();
        File storageRoot = new File(OFFER);
        storageRoot.mkdirs();
        new File(storageRoot, "storagezip").mkdirs();
        new File(storageRoot, "storagelog").mkdirs();
        new File(OFFER_INGEST).mkdirs();
        new File("/vitam/data/version/folder").mkdirs();
        
        
        // launch metadata
        LOGGER.warn("Start Metadata");
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        medtadataApplication.start();
        SystemPropertyUtil.clear(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT);

        // launch workspace
        LOGGER.warn("Start Workspace");
        SystemPropertyUtil.set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication(CONFIG_WORKSPACE_PATH);
        workspaceApplication.start();
        SystemPropertyUtil.clear(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT);

        // launch storage

        // first offer
        LOGGER.warn("Start Offer");
        SystemPropertyUtil
            .set("jetty.offer.port", Integer.toString(PORT_SERVICE_STORAGE_OFFER));
        final fr.gouv.vitam.common.storage.StorageConfiguration offerConfiguration = PropertiesUtils
            .readYaml(PropertiesUtils.findFile(DEFAULT_OFFER_CONF),
                fr.gouv.vitam.common.storage.StorageConfiguration.class);
        defaultOfferApplication = new DefaultOfferApplication(offerConfiguration);
        defaultOfferApplication.start();
        SystemPropertyUtil.clear("jetty.offer.port");

        // storage engine
        LOGGER.warn("Start Storage");
        final StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF),
                StorageConfiguration.class);
        try {
            SystemPropertyUtil
                .set("jetty.storage.port", Integer.toString(PORT_SERVICE_STORAGE));
            storageApplication = new StorageApplication(serverConfiguration);
            storageApplication.start();
            SystemPropertyUtil.clear("jetty.storage.port");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Composite Application Server", e);
        }

        // launch logbook
        LOGGER.warn("Start Logbook");
        SystemPropertyUtil
            .set(LogbookApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        lgbapplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        lgbapplication.start();
        SystemPropertyUtil.clear(LogbookApplication.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        LOGGER.warn("Start Processing");
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        // Launch Siegfried
        LOGGER.warn("Start Siegfried");
        ProcessBuilder pb = new ProcessBuilder(SIEGFRIED_PATH, "-serve", "localhost:" + PORT_SERVICE_SIEGFRIED);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        siegfried = pb.start();

        // launch worker
        LOGGER.warn("Start Worker");
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        // launch functional Admin server
        LOGGER.warn("Start Functional Admin");
        SystemPropertyUtil.set("jetty.functional-admin.port", Integer.toString(PORT_SERVICE_FUNCTIONAL_ADMIN));
        adminApplication = new AdminManagementApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminApplication.start();
        SystemPropertyUtil.clear("jetty.functional-admin.port");

        // launch access-internal
        LOGGER.warn("Start Access Internal");
        SystemPropertyUtil.set("jetty.access-internal.port", Integer.toString(PORT_SERVICE_ACCESS_INTERNAL));
        accessInternalApplication =
            new AccessInternalApplication(CONFIG_ACCESS_INTERNAL_PATH);
        accessInternalApplication.start();
        SystemPropertyUtil.clear("jetty.access-internal.port");

        // launch access-external
        LOGGER.warn("Start Access External");
        SystemPropertyUtil.set("jetty.access-external.port", Integer.toString(PORT_SERVICE_ACCESS_EXTERNAL));
        accessExternalApplication =
            new AccessExternalApplication(CONFIG_ACCESS_EXTERNAL_PATH);
        accessExternalApplication.start();
        SystemPropertyUtil.clear("jetty.access-external.port");

        // launch ingest-internal
        LOGGER.warn("Start Ingest Internal");
        SystemPropertyUtil.set("jetty.ingest-internal.port", Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        ingestInternalApplication = new IngestInternalApplication(CONFIG_INGEST_INTERNAL_PATH);
        ingestInternalApplication.start();
        SystemPropertyUtil.clear("jetty.ingest-internal.port");

        // launch ingest-external
        LOGGER.warn("Start Ingest external");
        SystemPropertyUtil.set("jetty.ingest-external.port", Integer.toString(PORT_SERVICE_INGEST_EXTERNAL));
        ingestExternalApplication = new IngestExternalApplication(CONFIG_INGEST_EXTERNAL_PATH);
        ingestExternalApplication.start();
        SystemPropertyUtil.clear("jetty.ingest-external.port");
        
        LOGGER.warn("ALL STARTED");

    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        esClient.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        try {
            ingestInternalApplication.stop();
            workspaceApplication.stop();
            wkrapplication.stop();
            lgbapplication.stop();
            processManagementApplication.stop();
            medtadataApplication.stop();
            adminApplication.stop();
            accessInternalApplication.stop();
            storageClient.close();
            defaultOfferApplication.stop();
            storageApplication.stop();
            siegfried.destroy();
            temporaryFolder.delete();

        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }


    @RunWithCustomExecutor
    @Test
    public void ruenTestTNR() throws Throwable {
        cucumber.api.cli.Main.main(new String[] {"-g", "fr.gouv.vitam.functionaltest.cucumber",
            "-p", "fr.gouv.vitam.functionaltest.cucumber.report.VitamReporter:report.json",
            "/home/vitam/workspace2/vitam-itests/"});
    }

}
