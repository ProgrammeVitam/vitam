/**
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
 */
package fr.gouv.vitam.storage.engine.server.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;

/**
 * 
 */
public class StorageResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResourceTest.class);

    private static final String STORAGE_CONF = "storage.conf";
    private static VitamServer vitamServer;
    private static File newStorageConf;

    private static int serverPort;

    private static final String REST_URI = "/storage/v1";
    private static final String OBJECTS_URI = "/objects";
    private static final String OBJECT_ID_URI = "/{id_object}";
    private static final String LOGBOOKS_URI = "/logbooks";
    private static final String LOGBOOK_ID_URI = "/{id_logbook}";
    private static final String UNITS_URI = "/units";
    private static final String METADATA_ID_URI = "/{id_md}";
    private static final String OBJECT_GROUPS_URI = "/objectgroups";
    private static final String STATUS_URI = "/status";

    private static JunitHelper junitHelper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = new JunitHelper();
        final File storage = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration realStorage = PropertiesUtils.readYaml(storage, StorageConfiguration.class);
        newStorageConf = File.createTempFile("test", STORAGE_CONF, storage.getParentFile());
        PropertiesUtils.writeYaml(newStorageConf, realStorage);

        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        try {
            vitamServer = StorageApplication.startApplication(new String[] {
                newStorageConf.getAbsolutePath(),
                Integer.toString(serverPort)});
            ((BasicVitamServer) vitamServer).start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Storage Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        newStorageConf.delete();
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(200);
    }

    @Test
    public final void testContainers() {

        given().contentType(ContentType.JSON).body("").when().get().then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when().post().then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when().delete().then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).when().head().then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

    }

    @Test
    public final void testObjects() {

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("object.zip");

        given().contentType(ContentType.JSON).body("").when()
            .get(OBJECTS_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .get(OBJECTS_URI + OBJECT_ID_URI, "idO1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.BINARY).body(stream).when()
            .post(OBJECTS_URI + OBJECT_ID_URI, "idO1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .delete(OBJECTS_URI + OBJECT_ID_URI, "idO1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).when()
            .head(OBJECTS_URI + OBJECT_ID_URI, "idO1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

    }

    @Test
    public final void testLogbooks() {

        given().contentType(ContentType.JSON).body("").when()
            .get(LOGBOOKS_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .get(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .post(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .delete(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).when()
            .head(LOGBOOKS_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

    }

    @Test
    public final void testUnits() {

        given().contentType(ContentType.JSON).body("").when()
            .get(UNITS_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .get(UNITS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .post(UNITS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .put(UNITS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .delete(UNITS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).when()
            .head(UNITS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

    }


    @Test
    public final void testObjectGroups() {

        given().contentType(ContentType.JSON).body("").when()
            .get(OBJECT_GROUPS_URI).then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .get(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .post(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .put(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).body("").when()
            .delete(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());
        given().contentType(ContentType.JSON).when()
            .head(OBJECT_GROUPS_URI + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

    }

}
