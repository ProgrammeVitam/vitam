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
package fr.gouv.vitam.common.server.application.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.DefaultAdminClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;

/**
 * StatusResourceImplTest Class Test Admin Status and Internal STatus Implementation
 *
 */
public class AdminAutotestStatusResourceImplTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminAutotestStatusResourceImplTest.class);

    // URI
    private static final String ADMIN_STATUS_URI = "/admin/v1";
    private static final String TEST_CONF = "test-multiple-connector.conf";

    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    /**
     * Temp folder for Elasticsearch
     */
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster-autotest";
    private final static String HOST_NAME = "127.0.0.1";
    private static int dataBasePort;
    private static JunitHelper junitHelper;

    private static int serverAdminPort;

    private static TestApplication application;
    private static TestVitamAdminClientFactory factory;
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        VitamConfiguration.setConnectTimeout(100);
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));
        final ElasticsearchAccess databaseEs = new ElasticsearchAccess(CLUSTER_NAME, nodes);

        dataBasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(dataBasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        final MongoDbAccess databaseMd = new MyMongoDbAccess(new MongoClient(new ServerAddress(
            HOST_NAME, dataBasePort),
            VitamCollection.getMongoClientOptions(new ArrayList<>())), CLUSTER_NAME, false);

        TestApplication.serviceRegistry = new VitamServiceRegistry();
        TestApplication.serviceRegistry.register(databaseMd).register(databaseEs);
        final MinimalTestVitamApplicationFactory<TestApplication> testFactory =
            new MinimalTestVitamApplicationFactory<TestApplication>() {

                @Override
                public StartApplicationResponse<TestApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    final TestApplication application = new TestApplication(TEST_CONF);
                    return startAndReturn(application);
                }

            };

        serverAdminPort = junitHelper.findAvailablePort(JunitHelper.PARAMETER_JETTY_SERVER_PORT_ADMIN);

        final StartApplicationResponse<TestApplication> response =
            testFactory.findAvailablePortSetToApplication();

        application = response.getApplication();
        factory = new TestVitamAdminClientFactory(serverAdminPort, ADMIN_STATUS_URI);
        TestApplication.serviceRegistry.register(factory);
        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VitamConfiguration.setConnectTimeout(1000);
        if (config == null) {
            return;
        }
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverAdminPort);
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(dataBasePort);
    }

    private static class MyMongoDbAccess extends MongoDbAccess {

        public MyMongoDbAccess(MongoClient mongoClient, String dbname, boolean recreate) {
            super(mongoClient, dbname, recreate);
        }
    }

    private static class TestVitamAdminClientFactory extends TestVitamClientFactory<DefaultAdminClient> {

        public TestVitamAdminClientFactory(int serverPort, String resourcePath) {
            super(serverPort, resourcePath);
        }

        @Override
        public DefaultAdminClient getClient() {
            return new DefaultAdminClient(this);
        }

    }

    private static class TestWrongVitamAdminClientFactory extends TestVitamClientFactory<DefaultAdminClient> {

        public TestWrongVitamAdminClientFactory(int serverPort, String resourcePath) {
            super(serverPort, resourcePath);
        }

        @Override
        public DefaultAdminClient getClient() {
            return new DefaultAdminClient(this);
        }

    }

    /**
     * Tests the state of the module through autotest
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnStatus() throws Exception {
        // Test OK
        LOGGER.warn("TEST OK");
        assertEquals(4, TestApplication.serviceRegistry.getRegisteredServices());
        int realTotal = TestApplication.serviceRegistry.getRegisteredServices();
        int realOK = realTotal;
        int realKO = 0;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(true, message.getStatus());
            final VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.OK.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError sub : error.getErrors()) {
                if (sub.getHttpCode() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    nbKO++;
                } else if (sub.getHttpCode() == Status.OK.getStatusCode()) {
                    nbOK++;
                } else {
                    nbUbknown++;
                }
            }
            assertEquals(realKO, nbKO);
            assertEquals(0, nbUbknown);
            assertEquals(realOK, nbOK);
        }

        // Now shutdown one by one

        // Add a fake clientFactory
        LOGGER.warn("TEST Fake client Factory KO");
        TestApplication.serviceRegistry.register(new TestWrongVitamAdminClientFactory(1, ADMIN_STATUS_URI));
        realKO++;
        realTotal++;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(true, message.getStatus());
            final VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError sub : error.getErrors()) {
                if (sub.getHttpCode() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    nbKO++;
                } else if (sub.getHttpCode() == Status.OK.getStatusCode()) {
                    nbOK++;
                } else {
                    nbUbknown++;
                }
            }
            assertEquals(realKO, nbKO);
            assertEquals(0, nbUbknown);
            assertEquals(realOK, nbOK);
        }

        // ES
        LOGGER.warn("TEST ELASTICSEARCH KO");
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        realKO++;
        realOK--;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(true, message.getStatus());
            final VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError sub : error.getErrors()) {
                if (sub.getHttpCode() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    nbKO++;
                } else if (sub.getHttpCode() == Status.OK.getStatusCode()) {
                    nbOK++;
                } else {
                    nbUbknown++;
                }
            }
            assertEquals(realKO, nbKO);
            assertEquals(0, nbUbknown);
            assertEquals(realOK, nbOK);
        }

        // MongoDB
        LOGGER.warn("TEST MONGO KO");
        mongod.stop();
        mongodExecutable.stop();
        realKO++;
        realOK--;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(true, message.getStatus());
            final VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError sub : error.getErrors()) {
                if (sub.getHttpCode() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    nbKO++;
                } else if (sub.getHttpCode() == Status.OK.getStatusCode()) {
                    nbOK++;
                } else {
                    nbUbknown++;
                }
            }
            assertEquals(realKO, nbKO);
            assertEquals(0, nbUbknown);
            assertEquals(realOK, nbOK);
        }

        // Application
        LOGGER.warn("TEST APPLICATION KO");
        if (application != null) {
            application.stop();
        }
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(false, message.getStatus());
            final VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            assertEquals(0, error.getErrors().size());
        }
    }
}
