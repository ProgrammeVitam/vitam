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
package fr.gouv.vitam.processing.distributor.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.processing.distributor.core.WorkerManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.Set;

import static io.restassured.RestAssured.given;

/**
 *
 */
public class ProcessDistributorResourceTest extends ResteasyTestApplication {

    private static final String REST_URI = "/processing/v1";
    private static final String WORKER_FAMILY_URI = "/worker_family";
    private static final String WORKERS_URI = "/workers";
    private static final String ID_FAMILY_URI = "/idFamily";
    private static final String ID_WORKER_URI = "/idWorker";

    private static final String JSON_REGISTER =
        "{ \"name\" : \"workername\", \"family\" : \"idFamily\", \"capacity\" : 10," +
        "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"89102\" } }";
    private static final WorkerManager workerManager = new WorkerManager();
    private static final VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        ProcessDistributorResourceTest.class
    );

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();

        RestAssured.port = vitamServerTestRunner.getBusinessPort();
        RestAssured.basePath = REST_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new ProcessDistributorResource(workerManager));
    }

    @Test
    public final void testRegisterWorkerBadRequest() throws Exception {
        String JSON_INVALID_FILE = "json";
        final File file = PropertiesUtils.findFile(JSON_INVALID_FILE);
        final JsonNode json = JsonHandler.getFromFile(file);
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testRegisterWorkerOK() {
        given()
            .contentType(ContentType.JSON)
            .body(JSON_REGISTER)
            .when()
            .post(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testUnregisterWorkerOK() {
        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .delete(WORKER_FAMILY_URI + ID_FAMILY_URI + WORKERS_URI + ID_WORKER_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testUnregisterWorkerNotFound() {
        String FAMILY_ID_E = "/error";
        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .delete(WORKER_FAMILY_URI + FAMILY_ID_E + WORKERS_URI + ID_WORKER_URI)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }
}
