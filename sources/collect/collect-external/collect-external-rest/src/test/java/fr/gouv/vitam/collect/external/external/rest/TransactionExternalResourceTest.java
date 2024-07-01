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
package fr.gouv.vitam.collect.external.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientInvalidRequestException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionExternalResourceTest extends ResteasyTestApplication {

    static final String COLLECT_CONF = "collect-external-test.conf";
    // URI
    private static final String COLLECT_RESOURCE_URI = "collect-external/v1";
    private static CollectExternalMain application;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectExternalResourceTest.class);
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final BusinessApplicationTest businessApplicationTest = new BusinessApplicationTest();
    private static final CollectInternalClientFactory collectInternalClientFactory =
        businessApplicationTest.getCollectInternalClientFactory();
    private static final CollectInternalClient collectInternalClient = mock(CollectInternalClient.class);

    @Override
    public Set<Object> getResources() {
        return businessApplicationTest.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplicationTest.getClasses();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new CollectExternalMain(COLLECT_CONF, TransactionExternalResourceTest.class, null);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = COLLECT_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Collect Application Server", e);
        }
    }

    @Before
    public void setUpBefore() {
        Mockito.reset(collectInternalClient);
        Mockito.reset(collectInternalClientFactory);
        when(collectInternalClientFactory.getClient()).thenReturn(collectInternalClient);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        junitHelper.releasePort(port);
        if (application != null) {
            application.stop();
        }
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void uploadArchiveUnit_OK() throws Exception {
        String transactionId = "myTxId";
        doReturn(new RequestResponseOK<>()).when(collectInternalClient).uploadArchiveUnit(any(), eq(transactionId));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body(
                "{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}"
            )
            .when()
            .post("/transactions/" + transactionId + "/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit_with_bad_transaction_id() throws Exception {
        doThrow(new VitamClientException("Error")).when(collectInternalClient).uploadArchiveUnit(any(), anyString());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body(
                "{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}"
            )
            .when()
            .post("/transactions/BAD_TRANSACTION_ID/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void bad_endpoint_match_pattern() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body(
                "{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}"
            )
            .when()
            .post("/transactions/units")
            .then()
            .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void bad_endpoint_no_match() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body(
                "{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}"
            )
            .when()
            .post("/transactions//units")
            .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void updateUnitsCsv_BadRequest() throws Exception {
        doThrow(new CollectInternalClientInvalidRequestException("Error"))
            .when(collectInternalClient)
            .updateUnitsWithCsvMetadata(eq("myTx"), any());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("CSV_REQ".getBytes())
            .when()
            .put("/transactions/myTx/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void updateUnitsCsv_OK() throws Exception {
        doReturn(new RequestResponseOK<JsonNode>())
            .when(collectInternalClient)
            .updateUnitsWithCsvMetadata(eq("myTx"), any());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("CSV_REQ".getBytes())
            .when()
            .put("/transactions/myTx/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }
}
