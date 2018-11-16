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
package fr.gouv.vitam.common.server.application;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.DefaultAdminClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * StatusResourceImplTest Class Test Admin Status and Internal STatus Implementation
 *
 */
public class StatusResourceImplTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StatusResourceImplTest.class);

    // URI
    private static final String ADMIN_RESOURCE_URI = "/";
    private static final String TEST_RESOURCE_URI = TestApplication.TEST_RESOURCE_URI;
    private static final String ADMIN_STATUS_URI = "/admin/v1" + BasicClient.STATUS_URL;
    private static final String ADMIN_URI = "/admin/v1";
    private static final String MODULE_STATUS_URI = TEST_RESOURCE_URI + BasicClient.STATUS_URL;
    private static final String TEST_CONF = "test-multiple-connector.conf";

    private static int serverPort;
    private static int serverAdminPort;
    private static TestApplication application;
    private static TestVitamAdminClientFactory factory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MinimalTestVitamApplicationFactory<TestApplication> testFactory =
                new MinimalTestVitamApplicationFactory<TestApplication>() {


                    @Override
                    public StartApplicationResponse<TestApplication> startVitamApplication(int reservedPort)
                            throws IllegalStateException {
                        final TestApplication application = new TestApplication(TEST_CONF);
                        return startAndReturn(application);
                    }

                };

        serverAdminPort = JunitHelper.getInstance().findAvailablePort(JunitHelper.PARAMETER_JETTY_SERVER_PORT_ADMIN);

        final StartApplicationResponse<TestApplication> response =
                testFactory.findAvailablePortSetToApplication();
        serverPort = response.getServerPort();
        application = response.getApplication();

        RestAssured.basePath = ADMIN_RESOURCE_URI;

        factory = new TestVitamAdminClientFactory(serverAdminPort, ADMIN_STATUS_URI);

        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        JunitHelper.getInstance().releasePort(serverPort);
        JunitHelper.getInstance().releasePort(serverAdminPort);
        VitamClientFactory.resetConnections();
    }

    private static class TestVitamAdminClientFactory extends TestVitamClientFactory<DefaultAdminClient> {

        public TestVitamAdminClientFactory(int serverPort, String resourcePath) {
            super(serverPort, resourcePath);
        }

        @Override
        public DefaultAdminClient getClient() {
            return new DefaultAdminClient(this);
        }

        @Override
        public void changeResourcePath(String resourcePath) {
            super.changeResourcePath(resourcePath);
        }

        @Override
        public void changeServerPort(int port) {
            super.changeServerPort(port);
        }

    }

    /**
     * Test that the server is lunched with at least tow port number, admin port and business port number
     * @throws Exception
     */
    @Test
    public void givenStartedServer_assert_application_lunched_on_multiple_port_number() throws Exception {
        int portBusiness = application.getVitamServer().getPort();
        assertThat(portBusiness).isPositive();
        int portAdmin = application.getVitamServer().getAdminPort();
        assertThat(portAdmin).isPositive();
    }

    // Status
    /**
     * when GET :adminPortNumber/admin/v1/status on admin port number we should got OK (200)
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusAdmin_ThenReturnStatusOk() throws Exception {
        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, ADMIN_STATUS_URI);

        RestAssured.port = serverAdminPort;

        RestAssured.given()
                .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                .when()
                .get(ADMIN_STATUS_URI).then().statusCode(Status.OK.getStatusCode());

        factory.changeServerPort(serverAdminPort);

        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(message.getStatus(), true);
        }
    }

    /**
     * Tests when GET :businessPortNumber/admin/v1/status on business port number we should got NOT_FOUND (404)
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusAdmin_OnBusinessPort_ThenReturnStatusNotFound() throws Exception {
        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, ADMIN_STATUS_URI);

        RestAssured.port = serverPort;

        RestAssured.given()
                .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                .when()
                .get(ADMIN_STATUS_URI).then().statusCode(Status.NOT_FOUND.getStatusCode());

        factory.changeServerPort(serverPort);

        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            final AdminStatusMessage message = clientAdmin.adminStatus();
            assertEquals(message.getStatus(), false);
        }
    }

    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnStatus() throws Exception {
        String jsonAsString;
        Response response;

        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, ADMIN_STATUS_URI);

        RestAssured.port = serverAdminPort;

        response =
                RestAssured.given()
                        .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                        .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                        .when().get(ADMIN_STATUS_URI).then().contentType(ContentType.JSON).extract().response();
        jsonAsString = response.asString();
        final JsonNode result = JsonHandler.getFromString(jsonAsString);
        assertEquals(result.get("status").toString(), "true");
        AdminStatusMessage message = JsonHandler.getFromInputStream(response.asInputStream(), AdminStatusMessage.class);
        assertEquals(message.getStatus(), true);
        LOGGER.debug(message.toString());

        factory.changeServerPort(serverAdminPort);

        try (DefaultAdminClient clientAdmin = factory.getClient()) {
            message = clientAdmin.adminStatus();
            assertEquals(message.getStatus(), true);
            assertTrue(clientAdmin.getAdminUrl().endsWith("/admin/v1"));
        }
    }

    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnStatusNoContent() throws Exception {
        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, MODULE_STATUS_URI);

        RestAssured.port = serverPort;

        RestAssured.given()
                .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                .when()
                .get(MODULE_STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());

        factory.changeServerPort(serverPort);

        factory.changeResourcePath(TEST_RESOURCE_URI);

        try (DefaultAdminClient client = factory.getClient()) {
            client.checkStatus();
        }
    }



    /**
     * Tests the state of the module service API by get
     *  When GET business resource on GET with adminPort number we should got Not_Found (404)
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturn404() throws Exception {
        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, MODULE_STATUS_URI);

        RestAssured.port = serverAdminPort;

        RestAssured.given()
                .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                .when()
                .get(MODULE_STATUS_URI).then().statusCode(Status.NOT_FOUND.getStatusCode());

        factory.changeServerPort(serverAdminPort);

        factory.changeResourcePath(TEST_RESOURCE_URI);

        try (DefaultAdminClient client = factory.getClient()) {
            client.checkStatus();
        } catch (VitamApplicationServerException e) {
            assertTrue(e.getMessage().contains("Not Found"));
        }
    }

    /**
     * Check sendServerVersion in error case
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetNotFoundResource_ThenReturnNotFoudWithoutJettyVersion() throws Exception {
        final Map<String, String> headersMap =
                AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, MODULE_STATUS_URI + "/NotFound");

        RestAssured.port = serverPort;

        final Response response = RestAssured.given()
                .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
                .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
                .when()
                .get(MODULE_STATUS_URI + "/NotFound").then().statusCode(Status.NOT_FOUND.getStatusCode())
                .extract().response();

        final String body = response.getBody().asString();
        assertFalse(body.contains("Powered by Jetty"));

    }
}
