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
package fr.gouv.vitam.collect.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.external.exception.CollectExternalInvalidRequestException;
import fr.gouv.vitam.collect.external.exception.CollectExternalNotFoundException;
import fr.gouv.vitam.collect.external.exception.CollectExternalServerSideException;
import fr.gouv.vitam.collect.external.service.CollectExternalIngestService;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientInvalidRequestException;
import fr.gouv.vitam.collect.internal.client.exceptions.CollectInternalClientNotFoundException;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.junit.FixedPatternFakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.Set;

import static fr.gouv.vitam.common.CommonMediaType.TEXT_CSV;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private static final IngestExternalClientFactory ingestExternalClientFactory =
        businessApplicationTest.getIngestExternalClientFactory();
    private static final CollectExternalIngestService collectExternalIngestService =
        businessApplicationTest.getCollectExternalIngestService();
    private static final CollectInternalClient collectInternalClient = mock(CollectInternalClient.class);
    private static final IngestExternalClient ingestExternalClient = mock(IngestExternalClient.class);

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
        Mockito.reset(ingestExternalClient);
        Mockito.reset(ingestExternalClientFactory);
        when(collectInternalClientFactory.getClient()).thenReturn(collectInternalClient);
        when(ingestExternalClientFactory.getClient()).thenReturn(ingestExternalClient);
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
            .post("/transactions/bad/path")
            .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void updateUnitsCsvDeprecated_BadRequest() throws Exception {
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
    public void updateUnitsCsvDeprecated_OK() throws Exception {
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

    @Test
    public void updateUnitsCsv_BadRequest() throws Exception {
        doThrow(new CollectInternalClientInvalidRequestException("Error"))
            .when(collectInternalClient)
            .updateUnitsWithCsvMetadata(eq("myTx"), any());
        given()
            .accept(ContentType.JSON)
            .contentType(TEXT_CSV)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("CSV_REQ")
            .when()
            .put("/transactions/myTx/units/metadata/csv")
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
            .contentType(TEXT_CSV)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("CSV_REQ")
            .when()
            .put("/transactions/myTx/units/metadata/csv")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void updateUnitsJsonl_BadRequest() throws Exception {
        doThrow(new CollectInternalClientInvalidRequestException("Error"))
            .when(collectInternalClient)
            .updateUnitsWithJsonlMetadata(eq("myTx"), any());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("JSONL_REQ".getBytes())
            .when()
            .put("/transactions/myTx/units/metadata/jsonl")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void updateUnitsJsonl_OK() throws Exception {
        doReturn(new RequestResponseOK<JsonNode>())
            .when(collectInternalClient)
            .updateUnitsWithCsvMetadata(eq("myTx"), any());
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("JSONL_REQ".getBytes())
            .when()
            .put("/transactions/myTx/units/metadata/jsonl")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void upload_zip_to_project_OK() throws Exception {
        doNothing().when(collectInternalClient).uploadZipToTransaction(eq("transaction-id"), any(), any(), eq(null));

        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .header(GlobalDataRest.X_ENCODING, "MacRoman")
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/upload")
            .then()
            .statusCode(OK.getStatusCode());

        verify(collectInternalClient).uploadZipToTransaction(
            eq("transaction-id"),
            any(InputStream.class),
            eq("MacRoman"),
            eq(null)
        );
    }

    @Test
    public void upload_zip_to_project_OK_with_attachement() throws Exception {
        doNothing()
            .when(collectInternalClient)
            .uploadZipToTransaction(eq("transaction-id"), any(), any(), eq("attachement-id"));

        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .header(GlobalDataRest.X_ENCODING, "MacRoman")
            .header(GlobalDataRest.X_ATTACHEMENT_ID, "attachement-id")
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/upload")
            .then()
            .statusCode(OK.getStatusCode());

        verify(collectInternalClient).uploadZipToTransaction(
            eq("transaction-id"),
            any(InputStream.class),
            eq("MacRoman"),
            eq("attachement-id")
        );
    }

    @Test
    public void upload_zip_to_project_with_unsupported_encoding() throws Exception {
        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .header(GlobalDataRest.X_ENCODING, "imaginary-encoding")
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/upload")
            .then()
            .statusCode(BAD_REQUEST.getStatusCode())
            .body("message", Matchers.equalTo("Unsupported encoding imaginary-encoding"));

        verify(collectInternalClient, never()).uploadZipToTransaction(any(), any(), any(), any());
    }

    @Test
    public void download_sip_ok() throws Exception {
        doReturn(new FixedPatternFakeInputStream(10)).when(collectInternalClient).downloadSIP("transaction-id");

        InputStream inputStream = given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .get("/transactions/transaction-id/downloadSIP")
            .then()
            .statusCode(OK.getStatusCode())
            .contentType(ContentType.BINARY)
            .extract()
            .response()
            .asInputStream();

        assertThat(inputStream).hasSameContentAs(new FixedPatternFakeInputStream(10));
    }

    @Test
    public void download_sip_not_found() throws Exception {
        doThrow(new CollectInternalClientNotFoundException("no such sip"))
            .when(collectInternalClient)
            .downloadSIP("transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .get("/transactions/transaction-id/downloadSIP")
            .then()
            .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void download_sip_bad_request() throws Exception {
        doThrow(new CollectInternalClientInvalidRequestException("bad transaction status"))
            .when(collectInternalClient)
            .downloadSIP("transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .get("/transactions/transaction-id/downloadSIP")
            .then()
            .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void download_sip_ko() throws Exception {
        doThrow(new VitamClientInternalException("prb")).when(collectInternalClient).downloadSIP("transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .get("/transactions/transaction-id/downloadSIP")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void sendSIP_ok() throws Exception {
        doReturn("ingestOperationId")
            .when(collectExternalIngestService)
            .ingestSip(collectInternalClient, ingestExternalClient, "transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/send")
            .then()
            .statusCode(OK.getStatusCode());

        doReturn("ingestOperationId")
            .when(collectExternalIngestService)
            .ingestSip(collectInternalClient, ingestExternalClient, "transaction-id");
    }

    @Test
    public void sendSIP_NotFound() throws Exception {
        doThrow(new CollectExternalNotFoundException("HTTP 404"))
            .when(collectExternalIngestService)
            .ingestSip(collectInternalClient, ingestExternalClient, "transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/send")
            .then()
            .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void sendSIP_BadRequest() throws Exception {
        doThrow(new CollectExternalInvalidRequestException("HTTP 400"))
            .when(collectExternalIngestService)
            .ingestSip(collectInternalClient, ingestExternalClient, "transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/send")
            .then()
            .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void sendSIP_InternalServerError() throws Exception {
        doThrow(new CollectExternalServerSideException("HTTP 500"))
            .when(collectExternalIngestService)
            .ingestSip(collectInternalClient, ingestExternalClient, "transaction-id");

        given()
            .contentType(CommonMediaType.APPLICATION_JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .body(new NullInputStream(100))
            .when()
            .post("/transactions/transaction-id/send")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
