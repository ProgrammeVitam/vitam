

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
package fr.gouv.vitam.processing.distributor.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.distributor.v2.WorkerManager;
import io.restassured.http.ContentType;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.get;

/**
 *
 */
public class ProcessDistributorResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorResourceTest.class);

    private static VitamServer vitamServer;

    private static int serverPort;
    private final String JSON_INVALID_FILE = "json";

    private static final String REST_URI = "/processing/v1";
    private static final String WORKER_FAMILY_URI = "/worker_family";
    private static final String WORKERS_URI = "/workers";
    private static final String ID_FAMILY_URI = "/idFamily";
    private static final String ID_WORKER_URI = "/idWorker";
    private static final Integer TENANT_ID = 0;

    private final String FAMILY_ID_E = "/error";

    private static final String JSON_REGISTER = "{ \"name\" : \"workername\", \"family\" : \"idFamily\", \"capacity\" : 10, \"storage\" : 100," +
        "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"89102\" } }";

    private static JunitHelper junitHelper;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.getConfiguration().setData(PropertiesUtils.getResourcePath("").toString());
        // WorkerManager.initialize();
        
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        try {
            vitamServer = buildTestServer();
            vitamServer.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Process Distributor Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        vitamServer.stop();
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void testGetWorkerFamilies() {
        get(WORKER_FAMILY_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testPutWorkerFamilies() {
        given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(WORKER_FAMILY_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testGetWorkerFamilyStatus() {
        get(WORKER_FAMILY_URI + ID_FAMILY_URI)
            .then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testCreateWorkerFamily() {
        given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testUpdateWorkerFamily() {
        given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(WORKER_FAMILY_URI + ID_FAMILY_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testDeleteWorkerFamily() {
        given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .delete(WORKER_FAMILY_URI + ID_FAMILY_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testGetFamilyWorkersList() {
        get(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public final void testDeleteFamilyWorkers() {
    	given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID).when()
            .delete(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testGetWorkerStatus() {
        get(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testRegisterWorkerBadRequest() throws Exception {
        final File file = PropertiesUtils.findFile(JSON_INVALID_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        given().contentType(ContentType.JSON).body(json).when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    /*@Test
    public final void testRegisterWorkerNotFound() {
        given().contentType(ContentType.JSON).body(JSON_REGISTER).when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + WORKER_ID_E).then()
            .statusCode(Status.CONFLICT.getStatusCode());
    }*/

    @Test
    public final void testRegisterWorkerOK() {
        given().contentType(ContentType.JSON).body(JSON_REGISTER).when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.OK.getStatusCode());
    }


    @Test
    public final void testUpdateWorker() {
        given().contentType(ContentType.JSON).body("")
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
    }

    @Test
    public final void testUnregisterWorkerOK() {
        given().contentType(ContentType.JSON).body("").when()
            .delete(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testUnregisterWorkerNotFound() {
        given().contentType(ContentType.JSON).body("").when()
            .delete(WORKER_FAMILY_URI + FAMILY_ID_E + WORKERS_URI + ID_WORKER_URI).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        final VitamServer vitamServer = VitamServerFactory.newVitamServer(serverPort);
        WorkerManager workerManager = new WorkerManager();

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        final ProcessDistributorResourceTest outer = new ProcessDistributorResourceTest();

        resourceConfig.register(new ProcessDistributorResource(workerManager));

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {contextHandler});
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

}

