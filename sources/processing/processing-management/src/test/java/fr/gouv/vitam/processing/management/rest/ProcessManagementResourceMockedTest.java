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

package fr.gouv.vitam.processing.management.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import io.restassured.RestAssured;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
// TODO Check if this is a usefull UT
@Ignore
public class ProcessManagementResourceMockedTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementResourceMockedTest.class);
    private static final String PORCESSING_URI = "/processing/v1";
    private static VitamServer vitamServer;
    private static JunitHelper junitHelper;
    private static int port;
    private static ProcessManagement mock;
    private static final Integer TENANT_ID = 0;
    private static final String CONTEXT_ID = "contextId";
    final GUID processId = GUIDFactory.newGUID();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            vitamServer = buildTestServer();
            vitamServer.start();

            RestAssured.port = port;
            RestAssured.basePath = PORCESSING_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }
    }

    private static VitamServer buildTestServer()
        throws VitamApplicationServerException, StateNotAllowedException, ProcessingException {
        final VitamServer vitamServer = VitamServerFactory.newVitamServer(port);


        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        mock = Mockito.mock(ProcessManagement.class);


        final ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setUrlWorkspace("http://localhost:8083");
        serverConfiguration.setUrlMetadata("http://localhost:8083");
        resourceConfig.register(new ProcessManagementResource(mock, serverConfiguration));
        resourceConfig.register(new HeaderIdContainerFilter());

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

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }

    @Test
    public void executeVitamProcessPreconditionFailed() throws Exception {
        reset(mock);
        when(mock.resume(anyObject(), anyInt(), anyBoolean()))
            .thenThrow(new ProcessingException(""));
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake")).when().post("operations").then()
            .statusCode(Response.Status.PRECONDITION_FAILED
                .getStatusCode());
    }

    // XXX: Why Unauthorize ? see implementation
    @Test
    public void executeVitamProcessUnauthorize() throws Exception {
        reset(mock);
        when(mock.resume(anyObject(), anyInt(), anyBoolean()))
            .thenThrow(new StateNotAllowedException(""));
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake")).when().post("operations").then()
            .statusCode(Response.Status.UNAUTHORIZED
                .getStatusCode());
    }

    @Test
    public void executeVitamProcessInternalServerError() throws Exception {
        reset(mock);
        when(mock.resume(anyObject(), anyInt(), anyBoolean()))
            .thenThrow(new RuntimeException(""));

        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake")).when().post("operations").then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode());
    }

    @Test
    public void executeVitamProcessAccepted() throws Exception {
        reset(mock);
        when(mock.resume(any(), anyInt(), anyBoolean())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.RUNNING).increment(StatusCode.OK).setLogbookTypeProcess("fake"));


        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake"))
            .when()
            .post("operations")
            .then().statusCode(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void executeVitamProcessAcceptedWarning() throws Exception {
        reset(mock);

        when(mock.resume(any(), anyInt(), anyBoolean())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.RUNNING).increment(StatusCode.WARNING).setLogbookTypeProcess("fake"));


        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake"))
            .when()
            .post("operations")
            .then().statusCode(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void executeVitamProcessAcceptedSubmitted() throws Exception {
        reset(mock);

        when(mock.resume(any(), anyInt(), anyBoolean())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.RUNNING).increment(StatusCode.OK).setLogbookTypeProcess("fake"));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .headers(GlobalDataRest.X_CONTEXT_ID, CONTEXT_ID, GlobalDataRest.X_ACTION, ProcessAction.RESUME.getValue(),
                GlobalDataRest.X_REQUEST_ID, processId, GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessingEntry("fake", "fake"))
            .when()
            .post("operations")
            .then().statusCode(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void listAllProcessTest() throws Exception {

        reset(mock);
        ProcessWorkflow processWorkflow1 = new ProcessWorkflow();
        processWorkflow1.setState(ProcessState.COMPLETED);
        ProcessWorkflow processWorkflow2 = new ProcessWorkflow();
        processWorkflow2.setState(ProcessState.COMPLETED);
        when(mock.findAllProcessWorkflow(anyObject())).thenReturn(Arrays.asList(processWorkflow1, processWorkflow2));

        given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get("operations")
            .then().statusCode(Response.Status.OK.getStatusCode());

    }

}
