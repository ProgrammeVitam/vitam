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
package fr.gouv.vitam.access.internal.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LogbookInternalResourceImplTest extends ResteasyTestApplication {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final ProcessingManagementClientFactory processingManagementClientFactory = mock(
        ProcessingManagementClientFactory.class
    );
    private static final ProcessingManagementClient processingManagementClient = mock(ProcessingManagementClient.class);

    private static final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = mock(
        LogbookLifeCyclesClientFactory.class
    );
    private static final LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);

    private static final LogbookOperationsClientFactory logbookOperationsClientFactory = mock(
        LogbookOperationsClientFactory.class
    );
    private static final LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);

    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private static final WorkspaceClient workspaceClient = mock(WorkspaceClient.class);

    private static final AdminManagementClientFactory adminManagementClientFactory = mock(
        AdminManagementClientFactory.class
    );
    private static final AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);

    private static final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
    private static final StorageClient storageClient = mock(StorageClient.class);

    private static final MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);
    private static final MetaDataClient metaDataClient = mock(MetaDataClient.class);

    private static final BusinessApplication businessApplication = new BusinessApplication(
        logbookLifeCyclesClientFactory,
        logbookOperationsClientFactory,
        storageClientFactory,
        workspaceClientFactory,
        adminManagementClientFactory,
        metaDataClientFactory,
        processingManagementClientFactory
    );

    @Override
    public Set<Object> getResources() {
        return businessApplication.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplication.getClasses();
    }

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImplTest.class);

    // URI
    private static final String ACCESS_CONF = "access-test.conf";
    private static final String ACCESS_RESOURCE_URI = "access-internal/v1";

    private static AccessInternalMain application;
    private static final Integer TENANT_ID = 0;

    private static JunitHelper junitHelper;
    private static int port;

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
            application = new AccessInternalMain(ACCESS_CONF, LogbookInternalResourceImplTest.class, null);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = ACCESS_RESOURCE_URI;
            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Access Application Server", e);
        }
    }

    @Before
    public void setUp() {
        reset(logbookOperationsClient);
        reset(workspaceClient);
        reset(processingManagementClient);
        reset(metaDataClient);
        reset(storageClient);
        reset(adminManagementClient);
        reset(logbookLifeCyclesClient);

        reset(logbookOperationsClientFactory);
        reset(workspaceClientFactory);
        reset(processingManagementClientFactory);
        reset(metaDataClientFactory);
        reset(storageClientFactory);
        reset(adminManagementClientFactory);
        reset(logbookLifeCyclesClientFactory);

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(port);
        VitamClientFactory.resetConnections();
    }

    @RunWithCustomExecutor
    @Test
    public void givenStartedServerWhenSearchLogbookThenOK() throws Exception {
        reset(logbookOperationsClient);
        reset(processingManagementClient);
        reset(workspaceClient);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(JsonHandler.getFromString(queryDsql));

        given()
            .contentType(ContentType.JSON)
            .body(new Select().getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .get("/operations")
            .then()
            .statusCode(Status.OK.getStatusCode());
    }
}
