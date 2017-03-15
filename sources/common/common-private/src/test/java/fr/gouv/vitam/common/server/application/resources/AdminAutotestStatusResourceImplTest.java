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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.DefaultAdminClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.server.application.TestApplication;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;

/**
 * StatusResourceImplTest Class Test Admin Status and Internal STatus Implementation
 *
 */
public class AdminAutotestStatusResourceImplTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminAutotestStatusResourceImplTest.class);

    // URI
    private static final String TEST_RESOURCE_URI = TestApplication.TEST_RESOURCE_URI;
    private static final String ADMIN_STATUS_URI = "/admin/v1";
    private static final String TEST_CONF = "test-multiple-connector.conf";

    private static JunitHelper junitHelper;

    private static int serverPort;
    private static int serverAdminPort;
    private static TestApplication application;
    private static TestVitamAdminClientFactory factory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        VitamConfiguration.setConnectTimeout(100);
        TestApplication.serviceRegistry = new VitamServiceRegistry();
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
        serverPort = response.getServerPort();
        application = response.getApplication();
        factory = new TestVitamAdminClientFactory(serverAdminPort, ADMIN_STATUS_URI);
        final DatabaseConnectionImpl fakeDb = new DatabaseConnectionImpl();
        TestApplication.serviceRegistry.register(factory).register((DatabaseConnection) null)
                .register((VitamStatusService) null).register(fakeDb);
        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VitamConfiguration.setConnectTimeout(1000);
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        junitHelper.releasePort(serverAdminPort);
    }

    private static class TestVitamAdminClientFactory extends TestVitamClientFactory<DefaultAdminClient> {

        public TestVitamAdminClientFactory(int serverPort, String resourcePath) {
            super(serverPort, resourcePath);
            super.disableUseAuthorizationFilter();
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

    private static class DatabaseConnectionImpl implements DatabaseConnection {
        private static boolean status = true;

        @Override
        public boolean checkConnection() {
            return status;
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
        assertEquals(3, TestApplication.serviceRegistry.getRegisteredServices());
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
            assertTrue(TestApplication.serviceRegistry.getResourcesStatus());
            try {
                TestApplication.serviceRegistry.checkDependencies(1, 10);
            } catch (final VitamApplicationServerException e) {
                fail("Should not raized an exception");
            }
        }

        // Test OK
        LOGGER.warn("TEST OK with one Optional KO");
        TestApplication.serviceRegistry.registerOptional(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
        realKO++;
        realTotal++;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
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
            assertTrue(TestApplication.serviceRegistry.getResourcesStatus());
            try {
                TestApplication.serviceRegistry.checkDependencies(1, 10);
            } catch (final VitamApplicationServerException e) {
                fail("Should not raized an exception");
            }
        }

        // Now shutdown one by one

        // Fake DB
        LOGGER.warn("TEST DB KO");
        DatabaseConnectionImpl.status = false;
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
            assertFalse(TestApplication.serviceRegistry.getResourcesStatus());
            try {
                TestApplication.serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {

            }
        }

        // Add a fake clientFactory
        LOGGER.warn("TEST Fake client Factory KO");
        TestApplication.serviceRegistry.register(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
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
            assertFalse(TestApplication.serviceRegistry.getResourcesStatus());
            try {
                TestApplication.serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {

            }
        }

        // Add a fake optional clientFactory
        LOGGER.warn("TEST Fake client Factory KO");
        TestApplication.serviceRegistry.registerOptional(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
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
            assertFalse(TestApplication.serviceRegistry.getResourcesStatus());
            try {
                TestApplication.serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {

            }
        }

        // Application
        LOGGER.warn("TEST APPLICATION KO");
        if (application != null) {
            application.stop();
        }
        realKO++;
        realOK--;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, TestApplication.serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(false, message.getStatus());
            VitamError error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            assertEquals(0, error.getErrors().size());
            final ObjectNode node = TestApplication.serviceRegistry.getAutotestStatus();
            error = VitamError.getFromJsonNode(node);
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
    }
}
