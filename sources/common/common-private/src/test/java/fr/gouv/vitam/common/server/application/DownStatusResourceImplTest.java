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

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * StatusResourceImplTest Class Test Admin Status and Internal STatus Implementation
 * 
 */
public class DownStatusResourceImplTest {

    // URI
    private static final String ADMIN_RESOURCE_URI = "/";
    private static final String ADMIN_STATUS_URI = "admin/v1/status";
    private static final String MODULE_STATUS_URI = "/status";
    private static final String result = "{'status': [{'Name': 'Vitam07'," +
        "'Role': 'UnknownRole'," +
        "'PlatformId': 4231009," +
        "'LoggerMessagePrepend': '[Vitam07:UnknownRole:4231009]'," +
        "'JsonIdentity': ''{\'name\':\'Vitam07\',\'role\':\'UnknownRole\',\'pid\':4231009}'}," + "{}]}";

    private static VitamServer vitamServer;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StatusResourceImplTest.class);

    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            vitamServer = buildTestServer();
            ((BasicVitamServer) vitamServer).start();

            RestAssured.port = port;
            RestAssured.basePath = ADMIN_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the StatusTest Application Server", e);
        }
    }

    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        VitamServer vitamServer = VitamServerFactory.newVitamServer(port);
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        VitamStatusService service = new VitamStatusService() {
            @Override
            public boolean getResourcesStatus() {
                return false;
            }

            @Override
            public ObjectNode getAdminStatus() throws InvalidParseOperationException {
                return JsonHandler.createObjectNode();
            }
        };
        resourceConfig.register(new AdminStatusResource(service));
        resourceConfig.register(new ApplicationStatusResource(service));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {contextHandler});
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }

    // Status
    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusAdmin_ThenReturnServiceUnavailable() throws Exception {
        RestAssured.get(ADMIN_STATUS_URI).then().statusCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnStatus() throws Exception {
        String jsonAsString;
        com.jayway.restassured.response.Response response;
        response = RestAssured.when().get(ADMIN_STATUS_URI).then().contentType(ContentType.JSON).extract().response();
        jsonAsString = response.asString();
        JsonNode result = JsonHandler.getFromString(jsonAsString);
        assertEquals(result.get("status").toString(), "false");
    }

    // Status
    /**
     * Tests the state of the module service API by get
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServer_WhenGetStatusModule_ThenReturnServiceUnavailable() throws Exception {
        RestAssured.get(MODULE_STATUS_URI).then().statusCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
    }
}
