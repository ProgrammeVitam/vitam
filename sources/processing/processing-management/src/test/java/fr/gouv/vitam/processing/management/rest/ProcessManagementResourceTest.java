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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.metadata.api.model.RequestResponseError;
import fr.gouv.vitam.metadata.api.model.VitamError;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;

public class ProcessManagementResourceTest {

    private static final String DATA_URI = "/processing/v1";
    private static final String NOT_EXITS_WORKFLOW_ID = "workflowJSONv3";
    private static final String EXITS_WORKFLOW_ID = "workflowJSONv2";
    private static final String URL_METADATA = "http://localhost:8086";
    private static final String URL_WORKSPACE = "http://localhost:8084";
    private static final String CONTAINER_NAME = "sipContainer";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(port));

        final ServerConfiguration configuration = new ServerConfiguration();
        configuration.setUrlMetada(URL_METADATA);
        configuration.setUrlWorkspace(URL_WORKSPACE);
        configuration.setJettyConfig(JETTY_CONFIG);
        ProcessManagementApplication.run(configuration);
        RestAssured.port = port;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void shutdownAfterClass() {
        try {
            ProcessManagementApplication.stop();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        junitHelper.releasePort(port);
    }

    /**
     * Test server status should return 200
     */
    @Test
    public void shouldGetStatusReturnNoContent() throws Exception {
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void shouldReturnErrorNotFoundWhenNotExistWorkFlow() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(new ProcessingEntry(CONTAINER_NAME, NOT_EXITS_WORKFLOW_ID)).when()
            .post("/operations").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.NOT_FOUND)))
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Ignore
    @Test
    public void shouldReturnResponseOKIfWorkflowExecuted() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(new ProcessingEntry(CONTAINER_NAME, EXITS_WORKFLOW_ID)).when()
            .post("/operations").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    private static String generateResponseErrorFromStatus(Status status) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new RequestResponseError()
            .setError(new VitamError(status.getStatusCode()).setContext("ingest").setState("code_vitam")
                .setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase())));
    }
}
