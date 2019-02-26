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
package fr.gouv.vitam.access.internal.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({LogbookOperationsClientFactory.class, ProcessingManagementClientFactory.class,
    WorkspaceClientFactory.class})
public class LogbookInternalResourceImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImplTest.class);

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access-internal/v1";

    private static AccessInternalMain application;
    private static final Integer TENANT_ID = 0;


    private static JunitHelper junitHelper;
    private static int port;

    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private static LogbookOperationsClient logbookOperationClient;
    private static ProcessingManagementClient processManagementClient;
    private static ProcessingManagementClientFactory processManagementClientFactory;
    private static WorkspaceClient workspaceClient;
    private static WorkspaceClientFactory workspaceClientFactory;

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new AccessInternalMain(ACCESS_CONF);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;
            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }
    }

    @Before
    public void setUp() {
        logbookOperationClient = mock(LogbookOperationsClient.class);
        PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
        PowerMockito.when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationClient);

        processManagementClient = mock(ProcessingManagementClient.class);
        PowerMockito.mockStatic(ProcessingManagementClientFactory.class);
        processManagementClientFactory = mock(ProcessingManagementClientFactory.class);
        PowerMockito.when(ProcessingManagementClientFactory.getInstance()).thenReturn(processManagementClientFactory);
        PowerMockito.when(processManagementClientFactory.getClient()).thenReturn(processManagementClient);

        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }


    /**
     * Test the check traceability method
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServerWhenCheckTraceabilityThenOK() throws Exception {
        LOGGER.warn("Reset logbook");
        reset(logbookOperationClient);
        LOGGER.warn("Reset process");
        reset(processManagementClient);
        LOGGER.warn("Reset workspace");
        reset(workspaceClient);
        LOGGER.warn("Start Mock");
        Mockito.doNothing().when(logbookOperationClient).bulkCreate(any(), any());
        Mockito.doNothing().when(logbookOperationClient).bulkUpdate(any(), any());
        Mockito.doNothing().when(processManagementClient).initVitamProcess(any(),  any());
        when(logbookOperationClient.selectOperationById(any()))
            .thenReturn(ClientMockResultHelper.getLogbookOperation());
        when(processManagementClient.executeCheckTraceabilityWorkFlow(any(), any(),
            any(), any())).thenReturn(Response.ok().build());
        when(processManagementClient.isOperationCompleted(any())).thenReturn(true);
        when(workspaceClient.isExistingContainer(any())).thenReturn(true);
        Mockito.doNothing().when(workspaceClient).deleteContainer(any(), anyBoolean());
        LOGGER.warn("Start Check");
        given().contentType(ContentType.JSON).body(JsonHandler.getFromString(queryDsql))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post("/traceability/check").then().statusCode(Status.OK.getStatusCode());
        LOGGER.warn("End Check");

    }

    @Test
    public void givenStartedServerWhenSearchLogbookThenOK() throws Exception {
        reset(logbookOperationClient);
        reset(processManagementClient);
        reset(workspaceClient);
        when(logbookOperationClient.selectOperation(any()))
        .thenReturn(JsonHandler.getFromString(queryDsql));

        given().contentType(ContentType.JSON).body(new Select().getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get("/operations").then().statusCode(Status.OK.getStatusCode());

    }
    
    /**
     * Test the check traceability method
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServerWhenCheckTraceabilityWithInvalidQueryThenBadRequest() throws Exception {
        reset(logbookOperationClient);
        reset(processManagementClient);
        reset(workspaceClient);
        Mockito.doNothing().when(logbookOperationClient).bulkCreate(any(), any());
        Mockito.doNothing().when(logbookOperationClient).bulkUpdate(any(), any());
        Mockito.doThrow(new BadRequestException("Bad Request")).when(processManagementClient)
            .initVitamProcess(any(), any());
        given().contentType(ContentType.JSON).body(JsonHandler.getFromString(queryDsql))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post("/traceability/check").then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    /**
     * Test the check traceability method
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServerWhenCheckTraceabilityWithInternalServerErrorThenInternalServerError()
        throws Exception {
        reset(logbookOperationClient);
        reset(processManagementClient);
        reset(workspaceClient);
        Mockito.doNothing().when(logbookOperationClient).bulkCreate(any(), any());
        Mockito.doNothing().when(logbookOperationClient).bulkUpdate(any(), any());
        Mockito.doNothing().when(processManagementClient).initVitamProcess(any(), any());
        when(logbookOperationClient.selectOperationById(any()))
            .thenReturn(ClientMockResultHelper.getLogbookOperation());
        when(processManagementClient.executeCheckTraceabilityWorkFlow(any(), any(),
            any(), any())).thenThrow(new InternalServerException("InternalServerException"));

        given().contentType(ContentType.JSON).body(JsonHandler.getFromString(queryDsql))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post("/traceability/check").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    /**
     * Test the check traceability method
     *
     * @throws Exception
     */
    @Test
    public void givenStartedServerWhenCheckTraceabilityWithContainerNotFoundThenNotFound() throws Exception {
        reset(logbookOperationClient);
        reset(processManagementClient);
        reset(workspaceClient);
        Mockito.doNothing().when(logbookOperationClient).bulkCreate(any(), any());
        Mockito.doNothing().when(logbookOperationClient).bulkUpdate(any(), any());
        Mockito.doNothing().when(processManagementClient).initVitamProcess(any(), any());
        when(logbookOperationClient.selectOperationById(any()))
            .thenReturn(ClientMockResultHelper.getLogbookOperation());
        when(processManagementClient.executeCheckTraceabilityWorkFlow(any(), any(),
            any(), any())).thenThrow(new WorkflowNotFoundException("Workflow not found"));

        given().contentType(ContentType.JSON).body(JsonHandler.getFromString(queryDsql))
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, "all").header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post("/traceability/check").then().statusCode(Status.NOT_FOUND.getStatusCode());

    }

}


