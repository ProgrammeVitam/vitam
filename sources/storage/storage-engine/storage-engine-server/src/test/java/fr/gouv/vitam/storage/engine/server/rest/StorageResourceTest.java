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
package fr.gouv.vitam.storage.engine.server.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

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
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.TenantIdContainerFilter;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

/**
 *
 */
public class StorageResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResourceTest.class);

    private static VitamServer vitamServer;

    private static int serverPort;

    private static final String REST_URI = "/storage/v1";
    private static final String OBJECTS_URI = "/objects";
    private static final String REPORTS_URI = "/reports";
    private static final String OBJECT_ID_URI = "/{id_object}";
    private static final String REPORT_ID_URI = "/{id_report}";
    private static final String LOGBOOKS_URI = "/logbooks";
    private static final String LOGBOOK_ID_URI = "/{id_logbook}";
    private static final String UNITS_URI = "/units";
    private static final String METADATA_ID_URI = "/{id_md}";
    private static final String OBJECT_GROUPS_URI = "/objectgroups";
    private static final String STATUS_URI = "/status";
    private static final String MANIFESTS_URI = "/manifests";
    private static final String MANIFEST_ID_URI = "/{id_manifest}";

    private static final String ID_O1 = "idO1";

    private static JunitHelper junitHelper;

    private static final String STRATEGY_ID = "strategyId";
    private static final Integer TENANT_ID = 0;
    private static final Integer TENANT_ID_E = 1;
    private static final Integer TENANT_ID_A_E = 2;
    private static final Integer TENANT_ID_Ardyexist = 3;
    private static final Integer TENANT_ID_BAD_REQUEST = -1;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        try {
            vitamServer = buildTestServer();
            ((BasicVitamServer) vitamServer).start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Storage Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ((BasicVitamServer) vitamServer).stop();
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public final void testContainers() {
        // TODO: review api endpoint
        // given().contentType(ContentType.JSON).body("").when().get("").then()
        // .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        // given().contentType(ContentType.JSON)
        // .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
        // VitamHttpHeader.TENANT_ID.getName(),
        // TENANT_ID)
        // .body("").when().get().then()
        // .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        // given().contentType(ContentType.JSON)
        // .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
        // VitamHttpHeader.TENANT_ID.getName(),
        // TENANT_ID_E)
        // .body("").when().get().then()
        // .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        // given().contentType(ContentType.JSON)
        // .body("").when().post().then()
        // .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete().then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete().then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NO_CONTENT.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body("").when().delete().then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON).when().head().then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head().then().statusCode(Status.OK.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E)
            .when().head().then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void testObjects() {
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("").accept(MediaType.APPLICATION_OCTET_STREAM).when()
                .get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body("").when().get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON).body("").when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET)
                .when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.PUT)
                .body("").when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.BAD_REQUEST.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID).when()
                .post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON).body("").when().delete(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.X_DIGEST.getName(), "digest", VitamHttpHeader.X_DIGEST_ALGORITHM.getName(),
                        VitamConfiguration.getDefaultDigestType().getName())
                .body("").when().delete(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.NO_CONTENT.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E,
                        VitamHttpHeader.X_DIGEST.getName(), "digest", VitamHttpHeader.X_DIGEST_ALGORITHM.getName(),
                        VitamConfiguration.getDefaultDigestType().getName())
                .body("").when().delete(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON).when().head(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().head(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public final void testObjectCreated() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body(createObjectDescription).when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public final void testReportCreation() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body(createObjectDescription).when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
                .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON).body(createObjectDescription).when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                        TENANT_ID_Ardyexist)
                .body(createObjectDescription).when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public final void testManifestCreation() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("mm");
        createObjectDescription.setWorkspaceContainerGUID("mm");
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body(createObjectDescription).when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1).then()
                .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON).body(createObjectDescription).when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1)
                .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                        TENANT_ID_Ardyexist)
                .body(createObjectDescription).when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1).then()
                .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1).then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public final void testObjectNotFound() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body(createObjectDescription).when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void testObjectTechnicalError() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .body(createObjectDescription).when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
                .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testLogbooks() {

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(LOGBOOKS_URI).then()
                // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(LOGBOOKS_URI).then()
                // .statusCode(Status.OK.getStatusCode());
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body("").when().get(LOGBOOKS_URI).then()
                // .statusCode(Status.NOT_FOUND.getStatusCode());
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().get(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                // .statusCode(Status.OK.getStatusCode());
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().get(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.PUT)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body("").when().post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NO_CONTENT.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .body("").when().delete(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .when().headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                        TENANT_ID, LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1")
                .then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().head(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectIllegalArgumentException() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID).when()
                .get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).header(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID).when()
                .get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectNotFoundException() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectTechnicalException() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getObjectOk() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getReportOk() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(REPORTS_URI + REPORT_ID_URI, "id0").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getManifestOk() throws Exception {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then()
                .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
                .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then()
                .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testUnits() {

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(UNITS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().get(UNITS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().get(UNITS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(UNITS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().get(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().get(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET)
                .when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.PUT)
                .when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")

                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .when().post(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.CONFLICT.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().put(UNITS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().put(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().put(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .when().put(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete(UNITS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().delete(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NO_CONTENT.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().delete(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .when().headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                        TENANT_ID, UNITS_URI + METADATA_ID_URI, "idmd1")
                .then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().head(UNITS_URI + METADATA_ID_URI, "idmd1").then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public final void testObjectGroups() {

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(OBJECT_GROUPS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().get(OBJECT_GROUPS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().get(OBJECT_GROUPS_URI).then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().get(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().get(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().get(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET)
                .when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                        VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.PUT)
                .when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .when().post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().put(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().put(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().put(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .when().put(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .body("").when().delete(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().delete(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NO_CONTENT.getStatusCode());
        given().contentType(ContentType.JSON).body("")
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().delete(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.JSON)
                .when().headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                        TENANT_ID, OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1")
                .then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.OK.getStatusCode());
        given().contentType(ContentType.JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().head(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
                .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        // .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getContainerInformationOk() {
        given().accept(MediaType.APPLICATION_JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
                .when().head().then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getContainerInformationIllegalArgument() {
        given().accept(MediaType.APPLICATION_JSON).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
            VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_BAD_REQUEST).when().head().then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }
    
    @Test
    public void getContainerInformationWrongHeaders() {
        given().accept(MediaType.APPLICATION_JSON).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID).when().head()
                .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getContainerInformationStorageNotFoundException() {
        given().accept(MediaType.APPLICATION_JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E)
                .when().head().then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getContainerInformationStorageTechnicalException() {
        given().accept(MediaType.APPLICATION_JSON)
                .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
                .when().head().then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Ignore
    @Test
    public void listObjectsTest() {
        // TODO: make it work
        given().accept(MediaType.APPLICATION_JSON).when().get("{type}", DataCategory.OBJECT.getFolder()).then()
                .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void secureStorageLogbookOk(){
        given().headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID).when().post("/storage/secure").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    private static VitamServer buildTestServer() throws VitamApplicationServerException {
        final VitamServer vitamServer = VitamServerFactory.newVitamServer(serverPort);

        final ResourceConfig resourceConfig = new ResourceConfig();
        final StorageResourceTest outer = new StorageResourceTest();
        resourceConfig.register(JacksonFeature.class);
        final StorageDistributionInnerClass storage = outer.new StorageDistributionInnerClass();
        resourceConfig.register(new StorageResource(storage));
        resourceConfig.register(TenantIdContainerFilter.class);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(sh, "/*");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { contextHandler });
        vitamServer.configure(contextHandler);
        return vitamServer;
    }

    private class StorageDistributionInnerClass implements StorageDistribution {

        @Override
        public StoredInfoResult storeData(String strategyId, String objectId,
            ObjectDescription createObjectDescription, DataCategory category, String requester)
            throws StorageTechnicalException, StorageNotFoundException, StorageAlreadyExistsException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            } else if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical error");
            } else if (TENANT_ID_Ardyexist.equals(tenantId)) {
                throw new StorageAlreadyExistsException("Already Exists Exception");
            }
            return null;
        }

        @Override
        public JsonNode getContainerInformation(String strategyId) throws StorageNotFoundException, StorageTechnicalException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            } else if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical error");
            } else if (TENANT_ID_BAD_REQUEST.equals(tenantId)) {
                throw new IllegalArgumentException("IllegalArgumentException");
            }
            return null;
        }

        @Override
        public InputStream getStorageContainer(String strategyId) throws StorageNotFoundException {
            return null;
        }

        @Override
        public JsonNode createContainer(String strategyId) throws UnsupportedOperationException {
            return null;
        }

        @Override
        public void deleteContainer(String strategyId) throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
        }

        @Override
        public RequestResponse<JsonNode> listContainerObjects(String strategyId, DataCategory category, String cursorId) throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            }
            return null;
        }

        @Override
        public Response getContainerByCategory(String strategyId, String objectId, DataCategory category,
                AsyncResponse asyncResponse) throws StorageNotFoundException, StorageTechnicalException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Object not found");
            }
            if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical exception");
            }

            final Response response = new AbstractMockClient.FakeInboundResponse(Status.OK,
                    new ByteArrayInputStream("test".getBytes()), MediaType.APPLICATION_OCTET_STREAM_TYPE, null);

            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder = Response.status(Status.OK).type(MediaType.APPLICATION_OCTET_STREAM);
            helper.writeResponse(responseBuilder);
            return response;
        }

        @Override
        public JsonNode getContainerObjectInformations(String strategyId, String objectId) throws StorageNotFoundException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            } 
            return null;
        }

        @Override
        public void deleteObject(String strategyId, String objectId, String digest, DigestType digestAlgorithm)
            throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            } else if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical error");
            } else if (TENANT_ID_BAD_REQUEST.equals(tenantId)) {
                throw new IllegalArgumentException("IllegalArgumentException");
            }
        }

        @Override
        public JsonNode getContainerLogbooks(String strategyId) throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public JsonNode getContainerLogbook(String strategyId, String logbookId)
            throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public void deleteLogbook(String strategyId, String logbookId)
            throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
        }

        @Override
        public JsonNode getContainerUnits(String strategyId) throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public JsonNode getContainerUnit(String strategyId, String unitId)
            throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public void deleteUnit(String strategyId, String unitId) throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
        }

        @Override
        public JsonNode getContainerObjectGroups(String strategyId) throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public JsonNode getContainerObjectGroup(String strategyId, String objectGroupId)
            throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
            return null;
        }

        @Override
        public void deleteObjectGroup(String strategyId, String objectGroupId)
            throws UnsupportedOperationException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new UnsupportedOperationException("UnsupportedOperationException");
            }
        }

        @Override
        public JsonNode status() throws UnsupportedOperationException {
            return null;
        }

        @Override
        public void close() {
            // Nothing
        }

    }

}
