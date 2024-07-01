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
package fr.gouv.vitam.worker.server.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.WorkerAccessRequest;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.impl.WorkerImpl;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class WorkerResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerResourceTest.class);

    private static final String WORKER_RESOURCE_URI = "/worker/v1";
    private static final String WORKER_STATUS_URI = BasicClient.STATUS_URL;
    private static final String WORKER_STEP_URI = "/tasks";

    private static JunitHelper junitHelper;
    private static int serverPort;
    private static File newWorkerConf;
    private static WorkerMain application;

    private static Worker worker;

    private static final String BODY_TEST_NOT_JSON = "body_test";

    private static final String WORKER_CONF = "worker-test.conf";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        worker = Mockito.mock(WorkerImpl.class);

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        final File workerFile = PropertiesUtils.findFile(WORKER_CONF);
        final WorkerConfiguration realWorker = PropertiesUtils.readYaml(workerFile, WorkerConfiguration.class);
        // -1 to ignore register
        realWorker
            .setRegisterServerPort(serverPort)
            .setRegisterServerHost("localhost")
            .setRegisterDelay(1)
            .setRegisterRetry(-1)
            .setProcessingUrl("http://localhost:8888")
            .setUrlMetadata("http://localhost:8888")
            .setUrlWorkspace("http://localhost:8888");

        newWorkerConf = File.createTempFile("test", WORKER_CONF, workerFile.getParentFile());
        PropertiesUtils.writeYaml(newWorkerConf, realWorker);

        RestAssured.port = serverPort;
        RestAssured.basePath = WORKER_RESOURCE_URI;

        try {
            BusinessApplication.mock = worker;
            application = new WorkerMain(newWorkerConf.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Worker Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        newWorkerConf.delete();
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void testGetStatus() {
        get(WORKER_STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public final void testSubmitEmptyStepThenBadRequest() {
        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testSubmitIncorrectStepThenBadRequest() {
        // since resteasy -> 500 is thrown
        given()
            .contentType(ContentType.JSON)
            .body(BODY_TEST_NOT_JSON)
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testSubmitStepOK() throws IOException, IllegalArgumentException, ProcessingException {
        final ItemStatus itemStatus = new ItemStatus("ID");
        itemStatus.setMessage("message");
        final StatusCode status = StatusCode.OK;
        itemStatus.increment(status);
        final ItemStatus responses = new ItemStatus("ID");
        Mockito.reset(worker);

        when(worker.run(any(), any())).thenReturn(responses);

        final InputStream stream = PropertiesUtils.getResourceAsStream("descriptionStep.json");
        final String body = StreamUtils.toString(stream);

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testSubmitStepWrongHandler() throws IOException, IllegalArgumentException, ProcessingException {
        Mockito.reset(worker);
        when(worker.run(any(), any())).thenThrow(new HandlerNotFoundException(""));

        final InputStream stream = PropertiesUtils.getResourceAsStream("descriptionStep_wrong_handler.json");
        final String body = StreamUtils.toString(stream);

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testSubmitStepProcessingException()
        throws IOException, IllegalArgumentException, ProcessingException {
        Mockito.reset(worker);
        when(worker.run(any(), any())).thenThrow(new ProcessingException(""));

        final InputStream stream = PropertiesUtils.getResourceAsStream("descriptionStep_wrong_handler.json");
        final String body = StreamUtils.toString(stream);

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testSubmitStepAsyncDataUnavailableOK()
        throws IOException, IllegalArgumentException, ProcessingException, InvalidParseOperationException {
        Map<AccessRequestContext, List<String>> accessRequestIdByContext = new TreeMap<>(
            Comparator.comparing(AccessRequestContext::getStrategyId).thenComparing((s1, s2) -> {
                return StringUtils.compare(s1.getOfferId(), s2.getOfferId());
            })
        );

        accessRequestIdByContext.put(
            new AccessRequestContext("strategyId", "offerId"),
            List.of("AccessRequestId1", "AccessRequestId2")
        );
        accessRequestIdByContext.put(new AccessRequestContext("strategyId", null), List.of("AccessRequestId3"));
        ProcessingRetryAsyncException prae = new ProcessingRetryAsyncException(accessRequestIdByContext);

        Mockito.reset(worker);

        when(worker.run(any(), any())).thenThrow(prae);

        final InputStream stream = PropertiesUtils.getResourceAsStream("descriptionStep.json");
        final String body = StreamUtils.toString(stream);

        JsonNode responseBody = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(WORKER_STEP_URI)
            .then()
            .statusCode(CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode())
            .extract()
            .body()
            .as(JsonNode.class);

        List<WorkerAccessRequest> accessRequests = JsonHandler.getFromJsonNode(responseBody, new TypeReference<>() {});
        assertThat(accessRequests.size()).isEqualTo(3);
        assertThat(accessRequests.get(0).getStrategyId()).isEqualTo("strategyId");
        assertThat(accessRequests.get(0).getOfferId()).isEqualTo(null);
        assertThat(accessRequests.get(0).getAccessRequestId()).isEqualTo("AccessRequestId3");
        assertThat(accessRequests.get(1).getStrategyId()).isEqualTo("strategyId");
        assertThat(accessRequests.get(1).getOfferId()).isEqualTo("offerId");
        assertThat(accessRequests.get(1).getAccessRequestId()).isEqualTo("AccessRequestId1");
        assertThat(accessRequests.get(2).getStrategyId()).isEqualTo("strategyId");
        assertThat(accessRequests.get(2).getOfferId()).isEqualTo("offerId");
        assertThat(accessRequests.get(2).getAccessRequestId()).isEqualTo("AccessRequestId2");
    }
}
