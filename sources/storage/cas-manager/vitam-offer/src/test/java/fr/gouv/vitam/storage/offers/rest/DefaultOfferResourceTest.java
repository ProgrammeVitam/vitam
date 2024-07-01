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
package fr.gouv.vitam.storage.offers.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.model.storage.ObjectEntryReader;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.assertj.core.groups.Tuple;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DefaultOfferResource Test
 */
public class DefaultOfferResourceTest {

    private static final String WORKSPACE_OFFER_CONF = "storage-default-offer.conf";

    private static final String REST_URI = "/offer/v1";
    private static int serverPort;
    private static JunitHelper junitHelper;
    private static final String OBJECTS_URI = "/objects";
    private static final String OBJECT_TYPE_URI = "/{type}";
    private static final String OBJECT_ID_URI = "/{id}";
    private static final String STATUS_URI = "/status";
    private static final String LOG_URI = "/logs";
    private static final String UNIT_CODE = "UNIT";
    private static final String OBJECT_CODE = "OBJECT";
    private static final String METADATA = "/metadatas";

    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String ARCHIVE_FILE_V2_TXT = "archivefile_v2.txt";
    private static final String PREFIX = GUIDFactory.newGUID().getId();

    private static final ObjectMapper OBJECT_MAPPER;
    private static DefaultOfferMain application;

    static {
        OBJECT_MAPPER = new ObjectMapper(new JsonFactory());
        OBJECT_MAPPER.disable(SerializationFeature.INDENT_OUTPUT);
    }

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResourceTest.class);

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        for (OfferCollections o : OfferCollections.values()) {
            o.setPrefix(PREFIX);
            mongoRule.addCollectionToBePurged(o.getName());
        }

        File confFile = PropertiesUtils.findFile(DEFAULT_STORAGE_CONF);
        final ObjectNode conf = PropertiesUtils.readYaml(confFile, ObjectNode.class);
        conf.put("storagePath", tempFolder.getRoot().getAbsolutePath());
        PropertiesUtils.writeYaml(confFile, conf);

        final File workspaceOffer = PropertiesUtils.findFile(WORKSPACE_OFFER_CONF);
        final OfferConfiguration realWorkspaceOffer = PropertiesUtils.readYaml(
            workspaceOffer,
            OfferConfiguration.class
        );
        List<MongoDbNode> mongoDbNodes = realWorkspaceOffer.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        realWorkspaceOffer.setMongoDbNodes(mongoDbNodes);
        realWorkspaceOffer.setDbName(MongoRule.VITAM_DB);
        File newWorkspaceOfferConf = File.createTempFile("test", WORKSPACE_OFFER_CONF, workspaceOffer.getParentFile());
        PropertiesUtils.writeYaml(newWorkspaceOfferConf, realWorkspaceOffer);

        try {
            junitHelper = JunitHelper.getInstance();
            serverPort = junitHelper.findAvailablePort();

            RestAssured.port = serverPort;
            RestAssured.basePath = REST_URI;

            application = new DefaultOfferMain(newWorkspaceOfferConf.getAbsolutePath());
            application.start();
            ContentAddressableStorageAbstract.disableContainerCaching();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Wokspace Offer Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongoRule.handleAfterClass();

        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void initCollections() throws VitamException {
        try {
            // restart server to reinit collection sequence
            application.stop();
            VitamClientFactory.resetConnections();
            application.start();
        } catch (VitamApplicationServerException e) {
            throw new VitamException("could not restart server");
        }
    }

    @After
    public void deleteExistingFiles() throws Exception {
        final StorageConfiguration conf = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        // delete directories recursively
        FileUtils.deleteDirectory((new File(conf.getStoragePath() + "/1_unit")));
        FileUtils.deleteDirectory((new File(conf.getStoragePath() + "/2_unit")));
        FileUtils.deleteDirectory((new File(conf.getStoragePath() + "/0_object")));
        FileUtils.deleteDirectory((new File(conf.getStoragePath() + "/1_object")));
        FileUtils.deleteDirectory((new File(conf.getStoragePath() + "/2_object")));

        mongoRule.handleAfter();
    }

    @Test
    public void getCapacityTestBadRequest() {
        given().head(OBJECTS_URI + "/" + UNIT_CODE).then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getCapacityTestOk() {
        // test
        given().header(GlobalDataRest.X_TENANT_ID, 1).when().head(OBJECTS_URI + "/" + UNIT_CODE).then().statusCode(200);
    }

    @Test
    public void getCapacityTestNoContainers() {
        // test
        given()
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .when()
            .head(OBJECTS_URI + "/" + UNIT_CODE)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getObjectTestPreconditionFailed() {
        // no tenant id
        given().get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1").then().statusCode(412);
    }

    @Test
    public void getObjectTestNotFound() {
        // not found
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
    }

    @Test
    public void getObjectTestOK() throws Exception {
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with()
                .header(GlobalDataRest.X_TENANT_ID, "1")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
        }

        checkOfferDatabaseExistingDocument("1_object", "id1");

        // found
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
    }

    @Test
    public void getObjectWithdot() throws Exception {
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with()
                .header(GlobalDataRest.X_TENANT_ID, "1")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1.xml");
        }

        checkOfferDatabaseExistingDocument("1_object", "id1.xml");

        // found
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1.xml");
    }

    @Test
    public void putObjectTest() throws Exception {
        checkOfferDatabaseEmptiness();

        // no tenant id
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, 0)
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new ByteArrayInputStream(new byte[0]))
            .when()
            .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
            .then()
            .statusCode(400);

        // no size
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new ByteArrayInputStream(new byte[0]))
            .when()
            .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
            .then()
            .statusCode(400);

        // no digest type
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, 0)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new ByteArrayInputStream(new byte[0]))
            .when()
            .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
            .then()
            .statusCode(400);

        // ok
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            given()
                .header(GlobalDataRest.X_TENANT_ID, "2")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
                .then()
                .statusCode(201);
        }

        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        final File container = new File(conf.getStoragePath() + "/2_unit");
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
        final File object = new File(container.getAbsolutePath(), "/id1");
        assertTrue(object.exists());
        assertFalse(object.isDirectory());

        checkOfferDatabaseExistingDocument("2_unit", "id1");

        assertTrue(com.google.common.io.Files.equal(PropertiesUtils.findFile(ARCHIVE_FILE_TXT), object));
    }

    @Test
    public void putObjectWithBodySmallerThanExpectedSizeThenKO() throws Exception {
        checkOfferDatabaseEmptiness();

        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            given()
                .header(GlobalDataRest.X_TENANT_ID, "2")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "1000000")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
                .then()
                .statusCode(500);
        }
    }

    @Test
    public void putObjectWithBodyLargerThanExpectedSizeThenKO() throws Exception {
        checkOfferDatabaseEmptiness();

        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            given()
                .header(GlobalDataRest.X_TENANT_ID, "2")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "1766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
                .then()
                .statusCode(500);
        }
    }

    @Test
    public void putObjectOverrideExistingInRewritableContainer() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File file2 = PropertiesUtils.findFile(ARCHIVE_FILE_V2_TXT);

        // When
        putObject(DataCategory.UNIT, "myfile", file1, Status.CREATED);
        putObject(DataCategory.UNIT, "myfile", file2, Status.CREATED);

        // Then
        checkWrittenFile(file2, "2_unit", "myfile");
        checkOfferDatabaseExistingDocument("2_unit", "myfile", 2);
    }

    @Test
    public void putObjectOverrideExistingWithSameContentInNonRewritableContainer() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File file = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);

        // When
        putObject(DataCategory.OBJECT, "myfile", file, Status.CREATED);
        putObject(DataCategory.OBJECT, "myfile", file, Status.CREATED);

        // Then
        checkWrittenFile(file, "2_object", "myfile");
        checkOfferDatabaseExistingDocument("2_object", "myfile", 2);
    }

    @Test
    public void putObjectTryOverrideExistingWithNewContentInNonRewritableContainerFails() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File file2 = PropertiesUtils.findFile(ARCHIVE_FILE_V2_TXT);

        // When
        putObject(DataCategory.OBJECT, "myfile", file1, Status.CREATED);
        putObject(DataCategory.OBJECT, "myfile", file2, Status.CONFLICT);

        // Then
        checkWrittenFile(file1, "2_object", "myfile");
        checkOfferDatabaseExistingDocument("2_object", "myfile", 1);
    }

    private void checkWrittenFile(File file, String container, String objectId) throws IOException {
        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        final File object = new File(conf.getStoragePath() + "/" + container + "/" + objectId);
        assertThat(Files.equal(file, object)).isTrue();
    }

    private void putObject(DataCategory dataCategory, String id, File file, Status expectedStatus)
        throws IOException, InvalidParseOperationException {
        io.restassured.response.Response response;
        try (FileInputStream in = new FileInputStream(file)) {
            response = given()
                .header(GlobalDataRest.X_TENANT_ID, "2")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, file.length())
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, dataCategory.name(), id)
                .andReturn();
        }

        assertThat(response.statusCode()).isEqualTo(expectedStatus.getStatusCode());

        if (expectedStatus == Status.CREATED) {
            JsonNode content = JsonHandler.getFromInputStream(response.body().asInputStream());
            assertThat(content.get("size").longValue()).isEqualTo(file.length());
            Digest digest = new Digest(DigestType.SHA512);
            digest.update(file);
            assertThat(content.get("digest").textValue()).isEqualTo(digest.digestHex());
        }
    }

    @Test
    public void headObjectTest() throws Exception {
        // no tenant id
        given().head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1").then().statusCode(400);

        // no object
        given()
            .header(GlobalDataRest.X_TENANT_ID, 2)
            .and()
            .head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
            .then()
            .statusCode(404);

        // object
        final StorageConfiguration conf = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        final File container = new File(conf.getStoragePath() + "/1_unit");

        container.mkdir();
        final File object = new File(container.getAbsolutePath(), "/id1");
        object.createNewFile();
        given()
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .and()
            .head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, "id1")
            .then()
            .statusCode(204);
    }

    @Test
    public void deleteObjectTestNotExisting() {
        // no object found -> 404
        given()
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_DIGEST, "digest")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, VitamConfiguration.getDefaultDigestType().getName())
            .delete(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1")
            .then()
            .statusCode(404);
    }

    @Test
    public void deleteObjectTestBadRequests() {
        // bad requests (missing headers) -> 400
        given()
            .header(GlobalDataRest.X_DIGEST, "digest")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, VitamConfiguration.getDefaultDigestType().getName())
            .delete(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1")
            .then()
            .statusCode(400);
    }

    @Test
    public void deleteObjectTest() throws Exception {
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with()
                .header(GlobalDataRest.X_TENANT_ID, "1")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
        }

        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());

        // object is found, creation worked
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");

        // object is found, delete has failed, for sure
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");

        // object is found, delete has failed, for sure
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");

        String responseAsJson = "{\"id\":\"" + "id1" + "\",\"status\":\"" + Response.Status.OK.toString() + "\"}";
        // good combo digest algorithm + digest -> object found and deleted
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.X_DIGEST, digest.toString())
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, VitamConfiguration.getDefaultDigestType().getName())
            .delete(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1")
            .then()
            .statusCode(200)
            .body(Matchers.equalTo(responseAsJson));

        // lets check that we cant find the object again, meaning we re sure
        // that the object has been deleted
        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
    }

    @Test
    public void statusTest() {
        given().get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void checkObjectExistenceTestNotExisting() {
        // no object -> 404
        given()
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1")
            .then()
            .statusCode(404);
    }

    @Test
    public void checkObjectExistenceTestBadRequest() {
        // Missing tenant
        given().head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1").then().statusCode(400);
    }

    @Test
    public void checkObjectTest() throws Exception {
        checkOfferDatabaseEmptiness();

        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with()
                .header(GlobalDataRest.X_TENANT_ID, "1")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1");
        }

        checkOfferDatabaseExistingDocument("1_object", "id1");

        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .head(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, OBJECT_CODE, "id1")
            .then()
            .statusCode(204);
    }

    @Test
    public void getObjectMetadataOK() throws IOException {
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with()
                .header(GlobalDataRest.X_TENANT_ID, "1")
                .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "8766")
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in)
                .when()
                .put(OBJECTS_URI + "/" + DataCategory.UNIT.name() + OBJECT_ID_URI, "id1");
        }

        checkOfferDatabaseExistingDocument("1_unit", "id1");

        // test
        given()
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .header(GlobalDataRest.X_OFFER_NO_CACHE, "false")
            .when()
            .get(OBJECTS_URI + "/" + UNIT_CODE + "/" + "id1" + METADATA)
            .then()
            .statusCode(200);
    }

    @Test
    public void getObjectMetadataKO() {
        // test
        given()
            .header(GlobalDataRest.X_TENANT_ID, 1)
            .when()
            .get(OBJECTS_URI + "/" + UNIT_CODE + "/" + "" + METADATA)
            .then()
            .statusCode(404);
    }

    @Test
    public void listObjectsTest() {
        given().when().get(OBJECTS_URI + "/" + DataCategory.OBJECT.name()).then().statusCode(400);

        io.restassured.response.Response emptyContainerListObjectResponse = given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name())
            .andReturn();
        assertThat(emptyContainerListObjectResponse.getStatusCode()).isEqualTo(200);

        ObjectEntryReader emptyContainerObjectEntryReader = new ObjectEntryReader(
            emptyContainerListObjectResponse.asInputStream()
        );
        assertThat(emptyContainerObjectEntryReader).isEmpty();

        for (int i = 0; i < 10; i++) {
            try (FakeInputStream fin = new FakeInputStream(50)) {
                given()
                    .header(GlobalDataRest.X_TENANT_ID, "1")
                    .header(GlobalDataRest.VITAM_CONTENT_LENGTH, "50")
                    .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fin)
                    .when()
                    .put(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + OBJECT_ID_URI, "id" + i);
            }

            checkOfferDatabaseExistingDocument("1_object", "id" + i);
        }

        io.restassured.response.Response response = given()
            .header(GlobalDataRest.X_CURSOR, true)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name())
            .andReturn();
        assertThat(response.statusCode()).isEqualTo(200);

        ObjectEntryReader objectEntryReader = new ObjectEntryReader(response.asInputStream());
        Set<String> objectIds = IteratorUtils.toList(objectEntryReader)
            .stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedObjectIds = IntStream.range(0, 10).mapToObj(i -> "id" + i).collect(Collectors.toSet());
        assertThat(objectIds).isEqualTo(expectedObjectIds);
    }

    @Test
    public void getOfferLogTestBadRequest() {
        final OfferLogRequest getOfferLog = new OfferLogRequest();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(getOfferLog)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .then()
            .statusCode(400);

        given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .then()
            .statusCode(400);
    }

    @Test
    public void getOfferLogTestOk() throws InvalidParseOperationException {
        for (int i = 0; i < 10; i++) {
            try (FakeInputStream fin = new FakeInputStream(50)) {
                assertNotNull(fin);
                given()
                    .header(GlobalDataRest.X_TENANT_ID, "1")
                    .header(GlobalDataRest.VITAM_CONTENT_LENGTH, 50)
                    .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fin)
                    .when()
                    .put(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + OBJECT_ID_URI, "id" + i);
            }

            checkOfferDatabaseExistingDocument("1_object", "id" + i);
        }

        final OfferLogRequest getOfferLogNoResult = new OfferLogRequest(50L, 10, Order.ASC);
        ResponseBody responseBody1 = given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(getOfferLogNoResult)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .getBody();
        final RequestResponseOK<OfferLog> response1 = JsonHandler.getFromInputStream(
            responseBody1.asInputStream(),
            RequestResponseOK.class,
            OfferLog.class
        );
        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getResults().size()).isEqualTo(0);

        OfferLogRequest getOfferLogWithOffsetWithLimit = new OfferLogRequest(7L, 10, Order.ASC);
        ResponseBody responseBody2 = given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(getOfferLogWithOffsetWithLimit)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .getBody();
        final RequestResponseOK<OfferLog> response2 = JsonHandler.getFromInputStream(
            responseBody2.asInputStream(),
            RequestResponseOK.class,
            OfferLog.class
        );
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getResults().size()).isEqualTo(4);

        OfferLogRequest getOfferLogNoOffsetWithLimit = new OfferLogRequest(0L, 10, Order.ASC);
        ResponseBody responseBody3 = given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(getOfferLogNoOffsetWithLimit)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .getBody();
        final RequestResponseOK<OfferLog> response3 = JsonHandler.getFromInputStream(
            responseBody3.asInputStream(),
            RequestResponseOK.class,
            OfferLog.class
        );
        assertThat(response3.getStatus()).isEqualTo(200);
        assertThat(response3.getResults().size()).isEqualTo(10);

        OfferLogRequest getOfferLogOffsetLimitDesc = new OfferLogRequest(5L, 3, Order.DESC);
        getOfferLogNoOffsetWithLimit.setLimit(10);
        ResponseBody responseBody4 = given()
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(getOfferLogOffsetLimitDesc)
            .when()
            .get(OBJECTS_URI + "/" + DataCategory.OBJECT.name() + LOG_URI)
            .getBody();
        final RequestResponseOK<OfferLog> response4 = JsonHandler.getFromInputStream(
            responseBody4.asInputStream(),
            RequestResponseOK.class,
            OfferLog.class
        );
        assertThat(response4.getStatus()).isEqualTo(200);
        assertThat(response4.getResults().size()).isEqualTo(3);
    }

    private void checkOfferDatabaseEmptiness() {
        FindIterable<Document> results = mongoRule
            .getMongoClient()
            .getDatabase(MongoRule.VITAM_DB)
            .getCollection(OfferCollections.OFFER_LOG.getName())
            .find();
        assertThat(results).hasSize(0);
    }

    private void checkOfferDatabaseExistingDocument(String container, String filename) {
        checkOfferDatabaseExistingDocument(container, filename, 1);
    }

    private void checkOfferDatabaseExistingDocument(String container, String filename, int count) {
        FindIterable<Document> results = mongoRule
            .getMongoClient()
            .getDatabase(MongoRule.VITAM_DB)
            .getCollection(OfferCollections.OFFER_LOG.getName())
            .find(Filters.and(Filters.eq("Container", container), Filters.eq("FileName", filename)));

        assertThat(results).hasSize(count);
    }

    @Test
    public void bulkPutObjectsTest() throws Exception {
        checkOfferDatabaseEmptiness();

        File file = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        byte[] multiplexedStreamBody = getMultiplexedStreamBody(Arrays.asList("id1", "id"), Arrays.asList(file, file));

        // no tenant id
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, multiplexedStreamBody.length)
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(400);

        // no size
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(400);

        // no digest type
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, multiplexedStreamBody.length)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(400);

        // bad size (size smaller than content size)
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, multiplexedStreamBody.length - 50)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(500);

        // bad size (size greater than content size)
        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "1")
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, multiplexedStreamBody.length + 50)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(500);

        // ok
        given()
            .header(GlobalDataRest.X_TENANT_ID, "2")
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, multiplexedStreamBody.length)
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(multiplexedStreamBody)
            .when()
            .put("/bulk/objects/{type}", UNIT_CODE)
            .then()
            .statusCode(201);

        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        final File container = new File(conf.getStoragePath() + "/2_unit");
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
        final File object = new File(container.getAbsolutePath(), "/id1");
        assertTrue(object.exists());
        assertFalse(object.isDirectory());

        checkOfferDatabaseExistingDocument("2_unit", "id1");

        assertThat(object).hasSameContentAs(file);
    }

    @Test
    public void bulkPutObjectsOverrideExistingInRewritableContainer() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File testFileV1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File testFileV2 = PropertiesUtils.findFile(ARCHIVE_FILE_V2_TXT);

        // When (try override file1 & file2 with new content)
        bulkPutObjects(
            DataCategory.UNIT,
            Arrays.asList("file1", "file2"),
            Arrays.asList(testFileV1, testFileV1),
            Status.CREATED
        );
        bulkPutObjects(
            DataCategory.UNIT,
            Arrays.asList("file1", "file2"),
            Arrays.asList(testFileV2, testFileV2),
            Status.CREATED
        );

        // Then
        checkWrittenFile(testFileV2, "2_unit", "file1");
        checkWrittenFile(testFileV2, "2_unit", "file2");
        checkOfferDatabaseExistingDocument("2_unit", "file1", 2);
        checkOfferDatabaseExistingDocument("2_unit", "file2", 2);
    }

    @Test
    public void bulkPutObjectsOverrideExistingWithSameContentInNonRewritableContainer() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File testFileV1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);

        // When (try override file1 & file2 with same content)
        bulkPutObjects(
            DataCategory.OBJECT,
            Arrays.asList("file1", "file2"),
            Arrays.asList(testFileV1, testFileV1),
            Status.CREATED
        );
        bulkPutObjects(
            DataCategory.OBJECT,
            Arrays.asList("file1", "file2"),
            Arrays.asList(testFileV1, testFileV1),
            Status.CREATED
        );

        // Then
        checkWrittenFile(testFileV1, "2_object", "file1");
        checkWrittenFile(testFileV1, "2_object", "file2");
        checkOfferDatabaseExistingDocument("2_object", "file1", 2);
        checkOfferDatabaseExistingDocument("2_object", "file2", 2);
    }

    @Test
    public void bulkPutObjectsOverrideExistingWitNewContentInNonRewritableContainer() throws Exception {
        checkOfferDatabaseEmptiness();

        // Given
        File testFileV1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File testFileV2 = PropertiesUtils.findFile(ARCHIVE_FILE_V2_TXT);

        // When (try override file2 with different content)
        bulkPutObjects(
            DataCategory.OBJECT,
            Arrays.asList("file1", "file2"),
            Arrays.asList(testFileV1, testFileV1),
            Status.CREATED
        );
        bulkPutObjects(
            DataCategory.OBJECT,
            Arrays.asList("file3", "file2", "file4"),
            Arrays.asList(testFileV1, testFileV2, testFileV1),
            Status.CONFLICT
        );

        // Then
        checkWrittenFile(testFileV1, "2_object", "file2");
        checkOfferDatabaseExistingDocument("2_object", "file1", 1);
        checkOfferDatabaseExistingDocument("2_object", "file2", 1);
        checkOfferDatabaseExistingDocument("2_object", "file3", 1);
        // file4 may have not been logged if file2 digest checks failed concurrently
    }

    private void bulkPutObjects(DataCategory dataCategory, List<String> ids, List<File> files, Status expectedStatus)
        throws IOException, InvalidParseOperationException {
        byte[] content = getMultiplexedStreamBody(ids, files);
        InputStream inputStream = new ByteArrayInputStream(content);

        io.restassured.response.Response response = given()
            .header(GlobalDataRest.X_TENANT_ID, "2")
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, content.length)
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(inputStream)
            .when()
            .put("/bulk/objects/{type}", dataCategory.name())
            .andReturn();

        assertThat(response.statusCode()).isEqualTo(expectedStatus.getStatusCode());

        if (expectedStatus == Status.CREATED) {
            StorageBulkPutResult result = JsonHandler.getFromInputStream(
                response.body().asInputStream(),
                StorageBulkPutResult.class
            );

            assertThat(result.getEntries()).hasSize(files.size());

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                assertThat(result.getEntries().get(i).getObjectId()).isEqualTo(ids.get(i));
                assertThat(result.getEntries().get(i).getSize()).isEqualTo(files.get(i).length());

                Digest digest = new Digest(DigestType.SHA512);
                digest.update(file);
                assertThat(result.getEntries().get(i).getDigest()).isEqualTo(digest.digestHex());
            }
        }
    }

    private byte[] getMultiplexedStreamBody(List<String> ids, List<File> files)
        throws InvalidParseOperationException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(byteArrayOutputStream);

        // Serialize Ids as header entry
        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        JsonHandler.writeAsOutputStream(ids, headerOutputStream);
        multiplexedStreamWriter.appendEntry(headerOutputStream.size(), headerOutputStream.toInputStream());

        // Append content entries
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                multiplexedStreamWriter.appendEntry(file.length(), fis);
            }
        }
        multiplexedStreamWriter.appendEndOfFile();
        return byteArrayOutputStream.toByteArray();
    }

    @Test
    public void getBulkObjectMetadataWithNullTenantThenKO() {
        checkOfferDatabaseEmptiness();

        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_OFFER_NO_CACHE, false)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Arrays.asList("guid1", "guid2"))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(400);
    }

    @Test
    public void getBulkObjectMetadataWithMissingNoCacheHeaderThenKO() {
        checkOfferDatabaseEmptiness();

        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Arrays.asList("guid1", "guid2"))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(400);
    }

    @Test
    public void getBulkObjectMetadataWithMissingObjectIdThenKO() {
        checkOfferDatabaseEmptiness();

        given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_OFFER_NO_CACHE, false)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Arrays.asList("guid1", null))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(400);
    }

    @Test
    public void getBulkObjectMetadataWithExistingObjectIdThenOK() {
        checkOfferDatabaseEmptiness();

        putFile("guid1", 8766, new NullInputStream(8766));

        StorageBulkMetadataResult result = given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_OFFER_NO_CACHE, false)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Collections.singletonList("guid1"))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(StorageBulkMetadataResult.class);

        assertThat(result.getObjectMetadata()).hasSize(1);
        assertThat(result.getObjectMetadata().get(0).getObjectName()).isEqualTo("guid1");
        assertThat(result.getObjectMetadata().get(0).getDigest()).isEqualTo(
            "88e82e41dc33c69f16ffc6b5039dfc935b140235f2429ae2c15264486d8e33a2c3d28426b9b5a47723f4b5754d5029dfbf15926046c8a0fed87f433bed8f420c"
        );
        assertThat(result.getObjectMetadata().get(0).getSize()).isEqualTo(8766);
    }

    @Test
    public void getBulkObjectMetadataWithUnknownObjectIdThenOK() {
        checkOfferDatabaseEmptiness();

        StorageBulkMetadataResult result = given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_OFFER_NO_CACHE, false)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Collections.singletonList("unknown_guid1"))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(StorageBulkMetadataResult.class);

        assertThat(result.getObjectMetadata()).hasSize(1);
        assertThat(result.getObjectMetadata().get(0).getObjectName()).isEqualTo("unknown_guid1");
        assertThat(result.getObjectMetadata().get(0).getDigest()).isNull();
        assertThat(result.getObjectMetadata().get(0).getSize()).isNull();
    }

    @Test
    public void getBulkObjectMetadataMultipleObjects() {
        checkOfferDatabaseEmptiness();

        putFile("guid1", 0, new NullInputStream(0));
        putFile("guid2", 1024, new NullInputStream(1024));
        putFile("guid3", 8766, new NullInputStream(8766));

        StorageBulkMetadataResult result = given()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.X_OFFER_NO_CACHE, false)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Arrays.asList("guid1", "unknown_guid", "guid2", "guid3"))
            .when()
            .get("/bulk/objects/{type}/metadata", UNIT_CODE)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(StorageBulkMetadataResult.class);

        assertThat(result.getObjectMetadata()).hasSize(4);
        assertThat(result.getObjectMetadata())
            .extracting(
                StorageBulkMetadataResultEntry::getObjectName,
                StorageBulkMetadataResultEntry::getDigest,
                StorageBulkMetadataResultEntry::getSize
            )
            .containsExactlyInAnyOrder(
                new Tuple(
                    "guid1",
                    "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                    0L
                ),
                new Tuple(
                    "guid2",
                    "8efb4f73c5655351c444eb109230c556d39e2c7624e9c11abc9e3fb4b9b9254218cc5085b454a9698d085cfa92198491f07a723be4574adc70617b73eb0b6461",
                    1024L
                ),
                new Tuple("unknown_guid", null, null),
                new Tuple(
                    "guid3",
                    "88e82e41dc33c69f16ffc6b5039dfc935b140235f2429ae2c15264486d8e33a2c3d28426b9b5a47723f4b5754d5029dfbf15926046c8a0fed87f433bed8f420c",
                    8766L
                )
            );
    }

    private void putFile(String id, int size, InputStream inputStream) {
        given()
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header(GlobalDataRest.VITAM_CONTENT_LENGTH, size)
            .header(GlobalDataRest.X_DIGEST_ALGORITHM, DigestType.SHA512.getName())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(inputStream)
            .when()
            .put(OBJECTS_URI + OBJECT_TYPE_URI + OBJECT_ID_URI, UNIT_CODE, id)
            .then()
            .statusCode(201);
    }
}
