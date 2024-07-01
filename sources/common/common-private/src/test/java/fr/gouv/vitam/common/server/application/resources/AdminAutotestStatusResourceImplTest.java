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
package fr.gouv.vitam.common.server.application.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.DefaultAdminClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.server.application.TestResourceImpl;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdminAutotestStatusResourceImplTest extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminAutotestStatusResourceImplTest.class);

    // URI
    private static final String TEST_RESOURCE_URI = "/test/v1";
    private static final String ADMIN_STATUS_URI = "/admin/v1";

    static JunitHelper junitHelper = JunitHelper.getInstance();

    private static final TestVitamAdminClientFactory factory = new TestVitamAdminClientFactory(1, ADMIN_STATUS_URI);

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        AdminAutotestStatusResourceImplTest.class,
        factory
    );

    private static final VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        VitamConfiguration.setConnectTimeout(100);

        final DatabaseConnectionImpl fakeDb = new DatabaseConnectionImpl();
        serviceRegistry
            .register(factory)
            .register((DatabaseConnection) null)
            .register((VitamStatusService) null)
            .register(fakeDb);
        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        VitamConfiguration.setConnectTimeout(1000);
        vitamServerTestRunner.runAfter();
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

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new AdminStatusResource(serviceRegistry), new TestResourceImpl());
    }

    private static class DatabaseConnectionImpl implements DatabaseConnection {

        private static boolean status = true;

        @Override
        public boolean checkConnection() {
            return status;
        }

        @Override
        public String getInfo() {
            return "info";
        }
    }

    /**
     * Tests the state of the module through autotest
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnStatus() throws Throwable {
        // Test OK
        LOGGER.warn("TEST OK");
        assertEquals(3, serviceRegistry.getRegisteredServices());
        int realTotal = serviceRegistry.getRegisteredServices();
        int realOK = realTotal;
        int realKO = 0;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertTrue(message.getStatus());
            final VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.OK.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
            assertTrue(serviceRegistry.getResourcesStatus());
            try {
                serviceRegistry.checkDependencies(1, 10);
            } catch (final VitamApplicationServerException e) {
                fail("Should not raized an exception");
            }
        }

        // Test OK
        LOGGER.warn("TEST OK with one Optional KO");
        serviceRegistry.registerOptional(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
        realKO++;
        realTotal++;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertTrue(message.getStatus());
            final VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.OK.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
            assertTrue(serviceRegistry.getResourcesStatus());
            try {
                serviceRegistry.checkDependencies(1, 10);
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
            assertEquals(realTotal, serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertTrue(message.getStatus());
            final VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
            assertFalse(serviceRegistry.getResourcesStatus());
            try {
                serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {}
        }

        // Add a fake clientFactory
        LOGGER.warn("TEST Fake client Factory KO");
        serviceRegistry.register(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
        realKO++;
        realTotal++;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertTrue(message.getStatus());
            final VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
            assertFalse(serviceRegistry.getResourcesStatus());
            try {
                serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {}
        }

        // Add a fake optional clientFactory
        LOGGER.warn("TEST Fake client Factory KO");
        serviceRegistry.registerOptional(new TestWrongVitamAdminClientFactory(1, TEST_RESOURCE_URI));
        realKO++;
        realTotal++;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertTrue(message.getStatus());
            final VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
            assertFalse(serviceRegistry.getResourcesStatus());
            try {
                serviceRegistry.checkDependencies(1, 10);
                fail("Should raized an exception");
            } catch (final VitamApplicationServerException e) {}
        }

        // Application
        LOGGER.warn("TEST APPLICATION KO");
        vitamServerTestRunner.runAfter();
        realKO++;
        realOK--;
        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            assertEquals(realTotal, serviceRegistry.getRegisteredServices());
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertFalse(message.getStatus());
            VitamError<JsonNode> error = clientAdmin.adminAutotest();
            LOGGER.warn(JsonHandler.prettyPrint(error));
            assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), error.getHttpCode());
            assertEquals(0, error.getErrors().size());
            final ObjectNode node = serviceRegistry.getAutotestStatus();
            error = VitamError.getFromJsonNode(node);
            int nbOK = 0;
            int nbKO = 0;
            int nbUbknown = 0;
            for (final VitamError<JsonNode> sub : error.getErrors()) {
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
