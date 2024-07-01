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
package fr.gouv.vitam.common.server.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.DefaultAdminClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * StatusResourceImplTest Class Test Admin Status and Internal STatus Implementation
 */
public class DownStatusResourceImplTest extends ResteasyTestApplication {

    // URI
    private static final String ADMIN_RESOURCE_URI = "/";
    private static final String ADMIN_STATUS_URI = "/admin/v1" + BasicClient.STATUS_URL;
    private static final String TEST_RESOURCE_URI = "/test/v1";
    private static final String TEST_STATUS_URI = TEST_RESOURCE_URI + BasicClient.STATUS_URL;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StatusResourceImplTest.class);

    private static TestVitamAdminClientFactory factory = new TestVitamAdminClientFactory(1, ADMIN_STATUS_URI);
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        DownStatusResourceImplTest.class,
        factory,
        true
    );

    private static final VitamStatusService statusService = new VitamStatusService() {
        @Override
        public boolean getResourcesStatus() {
            return false;
        }

        @Override
        public ObjectNode getAdminStatus() {
            return JsonHandler.createObjectNode();
        }
    };

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new AdminStatusResource(statusService), new TestResourceImpl(statusService));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        RestAssured.port = vitamServerTestRunner.getBusinessPort();
        RestAssured.basePath = ADMIN_RESOURCE_URI;
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

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        LOGGER.debug("Ending tests");
        vitamServerTestRunner.runAfter();
    }

    // Status

    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusAdmin_ThenReturnServiceUnavailable() throws Exception {
        final Map<String, String> headersMap = AuthorizationFilterHelper.getAuthorizationHeaders(
            HttpMethod.GET,
            ADMIN_STATUS_URI
        );

        RestAssured.port = vitamServerTestRunner.getAdminPort();

        RestAssured.given()
            .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
            .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
            .when()
            .get(ADMIN_STATUS_URI)
            .then()
            .statusCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
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

        final Map<String, String> headersMap = AuthorizationFilterHelper.getAuthorizationHeaders(
            HttpMethod.GET,
            ADMIN_STATUS_URI
        );

        RestAssured.port = vitamServerTestRunner.getAdminPort();

        response = RestAssured.given()
            .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
            .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
            .when()
            .get(ADMIN_STATUS_URI)
            .then()
            .contentType(ContentType.JSON)
            .extract()
            .response();

        jsonAsString = response.asString();
        final JsonNode result = JsonHandler.getFromString(jsonAsString);
        assertEquals("false", result.get("status").toString());
        AdminStatusMessage message = JsonHandler.getFromInputStream(response.asInputStream(), AdminStatusMessage.class);
        assertEquals(message.getStatus(), false);
        try (DefaultAdminClient client = factory.getClient()) {
            message = client.adminStatus();
            assertEquals(message.getStatus(), false);
        }
    }

    // Status

    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnServiceUnavailable() throws Exception {
        final Map<String, String> headersMap = AuthorizationFilterHelper.getAuthorizationHeaders(
            HttpMethod.GET,
            ADMIN_STATUS_URI
        );

        RestAssured.port = vitamServerTestRunner.getBusinessPort();

        RestAssured.given()
            .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
            .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
            .when()
            .get(TEST_STATUS_URI)
            .then()
            .statusCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
        factory.changeServerPort(vitamServerTestRunner.getBusinessPort());
        factory.changeResourcePath(TEST_RESOURCE_URI);

        try (DefaultAdminClient client = factory.getClient()) {
            client.checkStatus();
            fail("Should raized an exception");
        } catch (final VitamApplicationServerException e) {
            // ok
        }
    }
}
