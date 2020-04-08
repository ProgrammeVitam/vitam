/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.engine.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.server.application.GenericExceptionMapper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.serverv2.VitamStarter;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.common.serverv2.application.ApplicationParameter;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StreamAndInfo;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 *
 */
public class StorageResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResourceTest.class);
    private static final String LOGS = "/logs";

    private static VitamStarter vitamStarter;

    private static int serverPort;

    private static final String REST_URI = "/storage/v1";
    private static final String DELETE_URI = "/delete";
    private static final String OBJECTS_URI = "/objects";
    private static final String REPORTS_URI = "/reports";
    private static final String PROFILE_URI = "/profiles";
    private static final String OBJECT_ID_URI = "/{id_object}";
    private static final String PROFILE_ID_URI = "/{profile_file_name}";
    private static final String REPORT_ID_URI = "/{id_report}";
    private static final String LOGBOOK_ID_URI = "/{id_logbook}";
    private static final String UNITS_URI = "/units";
    private static final String METADATA_ID_URI = "/{id_md}";
    private static final String OBJECT_GROUPS_URI = "/objectgroups";
    private static final String STATUS_URI = "/status";
    private static final String MANIFESTS_URI = "/manifests";
    private static final String STORAGELOG = "/storagelog";
    private static final String STORAGERULE = "/rules";
    private static final String STORAGELOG_ID_URI = "/{storagelogname}";
    private static final String STORAGERULE_ID_URI = "/{rulefile}";
    private static final String GET_RULEID = "/{id_object}";
    private static final String MANIFEST_ID_URI = "/{id_manifest}";
    private static final String STORAGE_BACKUP = "/backup";
    private static final String STORAGE_BACKUP_ID_URI = "/{backupfile}";

    private static final String STORAGE_BACKUP_OPERATION = "/backupoperations";
    private static final String STORAGE_BACKUP_OPERATION_ID_URI = "/{operationId}";

    private static final String ID_O1 = "idO1";
    private static final String OFFER_ID = "offerId";
    private static final String OFFER_ID_KO = "offerIdKo";

    private static JunitHelper junitHelper;

    private static final String STRATEGY_ID = "strategyId";

    private static final Integer TENANT_ID = 0;
    private static final Integer TENANT_ID_E = 1;
    private static final Integer TENANT_ID_A_E = 2;
    private static final Integer TENANT_ID_Ardyexist = 3;
    private static final Integer TENANT_ID_BAD_REQUEST = -1;

    @BeforeClass
    public static void setUpBeforeClass() {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        try {
            vitamStarter = buildTestServer();
            vitamStarter.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Storage Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            vitamStarter.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        } finally {
            junitHelper.releasePort(serverPort);
            VitamClientFactory.resetConnections();
        }
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public final void testObjects() {

        // GET (download)
        given().contentType(ContentType.JSON).body("").accept(MediaType.APPLICATION_OCTET_STREAM).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON).body("").accept(MediaType.APPLICATION_OCTET_STREAM).when()
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E, VitamHttpHeader.OFFERS_IDS.getName(), OFFER_ID)
            .get(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        // POST
        given().contentType(ContentType.JSON).body("").when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET, VitamHttpHeader.OFFERS_IDS.getName(),
                OFFER_ID, VitamHttpHeader.OFFER_NO_CACHE.getName(), "true")
            .when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.OK.getStatusCode());
        
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.PUT)
            .body("").when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID).when()
            .post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // DELETE
        given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_DATA_CATEGORY, DataCategory.OBJECT).
            body("").when().delete(DELETE_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON).
            body("").when().delete(DELETE_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                VitamHttpHeader.X_DIGEST.getName(), "digest", VitamHttpHeader.X_DIGEST_ALGORITHM.getName(),
                VitamConfiguration.getDefaultDigestType().getName(),
                GlobalDataRest.X_DATA_CATEGORY, DataCategory.OBJECT
            )
            .body("").when().delete(DELETE_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.NO_CONTENT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E,
                VitamHttpHeader.X_DIGEST.getName(), "digest",
                VitamHttpHeader.X_DIGEST_ALGORITHM.getName(), VitamConfiguration.getDefaultDigestType().getName(),
                GlobalDataRest.X_DATA_CATEGORY, DataCategory.OBJECT

            )
            .body("").when().delete(DELETE_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        // HEAD
        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
                VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E
            )
            .when().head(DataCategory.OBJECT.name() + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(ContentType.JSON)
        .headers(
            VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
            VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E,
            VitamHttpHeader.OFFERS_IDS.getName(), OFFER_ID+","+OFFER_ID_KO
        )
        .when().head(DataCategory.OBJECT.name() + OBJECT_ID_URI, ID_O1)
        .then().statusCode(Status.NOT_FOUND.getStatusCode())
        .and().header(OFFER_ID, "true").and().header(OFFER_ID_KO, "false");

        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
                VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E,
                VitamHttpHeader.OFFERS_IDS.getName(), OFFER_ID
            )
            .when().head(DataCategory.OBJECT.name() + OBJECT_ID_URI, ID_O1)
            .then().statusCode(Status.NO_CONTENT.getStatusCode())
            .and().header(OFFER_ID, "true");

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
        given().contentType(ContentType.JSON).body(createObjectDescription).when()
            .post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(REPORTS_URI + REPORT_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
        given().contentType(ContentType.JSON).body(createObjectDescription).when()
            .post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1).then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(MANIFESTS_URI + MANIFEST_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public final void testProfileCreation() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("mm");
        createObjectDescription.setWorkspaceContainerGUID("mm");
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .body(createObjectDescription).when().post(PROFILE_URI + PROFILE_ID_URI, ID_O1).then()
            .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON).body(createObjectDescription).when()
            .post(PROFILE_URI + PROFILE_ID_URI, ID_O1)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(PROFILE_URI + PROFILE_ID_URI, ID_O1).then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(PROFILE_URI + PROFILE_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public final void testObjectNotFound() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E)
            .body(createObjectDescription).when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E)
            .body(createObjectDescription).when().post(STORAGERULE + STORAGERULE_ID_URI, ID_O1).then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void testObjectTechnicalError() {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_A_E)
            .body(createObjectDescription).when().post(OBJECTS_URI + OBJECT_ID_URI, ID_O1).then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testLogStorage() {
        final ObjectDescription createObjectDescription = new ObjectDescription();

        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .body(createObjectDescription).when().post(STORAGELOG + STORAGELOG_ID_URI, ID_O1).then()
            .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON).body(createObjectDescription).when()
            .post(STORAGELOG + STORAGELOG_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(STORAGELOG + STORAGELOG_ID_URI, ID_O1).then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(STORAGELOG + STORAGELOG_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public final void testruleStorage() {
        final ObjectDescription createObjectDescription = new ObjectDescription();

        createObjectDescription.setWorkspaceObjectURI("dd");
        createObjectDescription.setWorkspaceContainerGUID("dd");
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .body(createObjectDescription).when().post(STORAGERULE + STORAGERULE_ID_URI, ID_O1).then()
            .statusCode(Status.CREATED.getStatusCode());
        given().contentType(ContentType.JSON).body(createObjectDescription).when()
            .post(STORAGERULE + STORAGERULE_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(STORAGERULE + STORAGERULE_ID_URI, ID_O1).then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(STORAGERULE + STORAGERULE_ID_URI, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void should_retrieve_rules_file() throws Exception {
        // Given
        // When
        // Then
        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().get(STORAGERULE + GET_RULEID, ID_O1).then()
            .statusCode(Status.OK.getStatusCode());

    }

    @Test
    public final void testBackupStorage() {

        testBAckup(STORAGE_BACKUP, STORAGE_BACKUP_ID_URI);
    }

    private void testBAckup(String storageBackup, String storageBackupIdUri) {
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceObjectURI("bb");
        createObjectDescription.setWorkspaceContainerGUID("bb");

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .body(createObjectDescription).when().post(storageBackup + storageBackupIdUri, ID_O1).then()
            .statusCode(Status.CREATED.getStatusCode());

        given().contentType(ContentType.JSON)
            .body(createObjectDescription).when()
            .post(storageBackup + storageBackupIdUri, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_Ardyexist)
            .body(createObjectDescription).when().post(storageBackup + storageBackupIdUri, ID_O1)
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        given().contentType(ContentType.JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().post(storageBackup + storageBackupIdUri, ID_O1).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getBackupOk() {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(STORAGE_BACKUP + STORAGE_BACKUP_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(STORAGE_BACKUP + STORAGE_BACKUP_ID_URI, "id0")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(STORAGE_BACKUP + STORAGE_BACKUP_ID_URI, "id0").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(STORAGE_BACKUP + STORAGE_BACKUP_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getBackupOperationOk() {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(STORAGE_BACKUP_OPERATION + STORAGE_BACKUP_OPERATION_ID_URI, "id0").then()
            .statusCode(Status.OK.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM).when()
            .get(STORAGE_BACKUP_OPERATION + STORAGE_BACKUP_OPERATION_ID_URI, "id0").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(STORAGE_BACKUP_OPERATION + STORAGE_BACKUP_OPERATION_ID_URI, "id0").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(STORAGE_BACKUP_OPERATION + STORAGE_BACKUP_OPERATION_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testLogbooks() {


        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
                VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                GlobalDataRest.X_DATA_CATEGORY, DataCategory.LOGBOOK
            )
            .body("").when().delete(DELETE_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
                VitamHttpHeader.TENANT_ID.getName(), TENANT_ID,
                GlobalDataRest.X_DATA_CATEGORY, DataCategory.LOGBOOK

            )
            .body("").when().delete(DELETE_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
        given().contentType(ContentType.JSON)
            .headers(
                VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID,
                VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E,
                GlobalDataRest.X_DATA_CATEGORY, DataCategory.LOGBOOK
            )
            .body("").when()
            .delete(DELETE_URI + LOGBOOK_ID_URI, "idl1").then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void getObjectIllegalArgumentException() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).body(AccessLogUtils.getNoLogAccessLog())
            .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .body(AccessLogUtils.getNoLogAccessLog()).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM).header(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .body(AccessLogUtils.getNoLogAccessLog()).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectNotFoundException() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID).body(AccessLogUtils.getNoLogAccessLog())
            .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getObjectTechnicalException() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID).body(AccessLogUtils.getNoLogAccessLog())
            .when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getObjectOk() {
        given().contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_OCTET_STREAM)
                .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(),
                        STRATEGY_ID)
                .body(AccessLogUtils.getNoLogAccessLog()).when().get(OBJECTS_URI + OBJECT_ID_URI, "id0").then()
                .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getReportOk() {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(REPORTS_URI + REPORT_ID_URI, "id0").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(REPORTS_URI + REPORT_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getProfileOk() {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(PROFILE_URI + PROFILE_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(PROFILE_URI + PROFILE_ID_URI, "id0").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(PROFILE_URI + PROFILE_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(PROFILE_URI + PROFILE_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getManifestOk() {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID, VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then().statusCode(Status.OK.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E, VitamHttpHeader.STRATEGY_ID.getName(),
                STRATEGY_ID)
            .when().get(MANIFESTS_URI + MANIFEST_ID_URI, "id0").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public final void testUnitsById() {

        testMedatdata(UNITS_URI);

    }


    @Test
    public final void testObjectGroupsById() {

        testMedatdata(OBJECT_GROUPS_URI);

    }

    private void testMedatdata(String objectGroupsUri) {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().get(objectGroupsUri + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.OK.getStatusCode());
        // missing headers
        given().accept(MediaType.APPLICATION_OCTET_STREAM).headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().get(objectGroupsUri + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        given().accept(MediaType.APPLICATION_OCTET_STREAM).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .when().get(objectGroupsUri + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // storage distribution errors
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_A_E)
            .when().get(objectGroupsUri + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E)
            .when().get(objectGroupsUri + METADATA_ID_URI, "idmd1").then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
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
            VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_BAD_REQUEST).when().head().then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getContainerInformationWrongHeaders() {
        given().accept(MediaType.APPLICATION_JSON).headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID).when()
            .head()
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getContainerInformationStorageNotFoundException() {
        given().accept(MediaType.APPLICATION_JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_E)
            .when().head().then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getContainerInformationStorageTechnicalException() {
        given().accept(MediaType.APPLICATION_JSON)
            .headers(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID, VitamHttpHeader.TENANT_ID.getName(),
                TENANT_ID_A_E)
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
    public void backupStorageLogbook() {
        given().headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
        .when().post("/storage/backup").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void backupOperationPreconditionFailed() {
        given().header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID).accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON).when().post(STORAGE_BACKUP_OPERATION +
            STORAGE_BACKUP_OPERATION_ID_URI, "id")
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void backupOperationOK() {
        ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceObjectURI("fake");
        objectDescription.setObjectName("oName");
        objectDescription.setType(DataCategory.BACKUP_OPERATION);
        objectDescription.setWorkspaceContainerGUID("fake");

        given().header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID).header(VitamHttpHeader.STRATEGY_ID.getName(),
            VitamConfiguration.getDefaultStrategy()).accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON).body(objectDescription).when().post(STORAGE_BACKUP_OPERATION +
            STORAGE_BACKUP_OPERATION_ID_URI, "id")
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void getOfferLogs() {
        OfferLogRequest offerLogRequest = new OfferLogRequest(2L, 10, Order.DESC);
        given()
            .header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .header(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .contentType(ContentType.JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(offerLogRequest)
            .when().get("/" + DataCategory.OBJECT.name() + LOGS)
            .then().statusCode(Status.OK.getStatusCode());

        offerLogRequest = new OfferLogRequest(null, 1, Order.DESC);
        given()
            .header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .header(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .contentType(ContentType.JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(offerLogRequest)
            .when().get("/" + DataCategory.OBJECT.name() + LOGS)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
            .header(VitamHttpHeader.STRATEGY_ID.getName(), STRATEGY_ID)
            .contentType(ContentType.JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(offerLogRequest)
            .when().get("/" + DataCategory.OBJECT.name() + LOGS)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
            .header(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .header(VitamHttpHeader.STRATEGY_ID.getName(), "")
            .contentType(ContentType.JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(offerLogRequest)
            .when().get("/" + DataCategory.OBJECT.name() + LOGS)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getStorageStrategies() {
        String strResponse = given()
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID)
            .when().get("/strategies").then().statusCode(Status.OK.getStatusCode())
            .extract().asString();
        
        assertThatCode(() -> {
            RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(strResponse), StorageStrategy.class);
        }).doesNotThrowAnyException();
        
        
        given()
            .headers(VitamHttpHeader.TENANT_ID.getName(), TENANT_ID_A_E)
            .when().get("/strategies").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private static VitamStarter buildTestServer() {
        return new VitamStarter(StorageConfiguration.class, "storage-engine.conf",
            BusinessApplicationInner.class, AdminApplication.class);
    }

    public static class BusinessApplicationInner extends Application {

        private Set<Object> singletons;
        private Set<Class<?>> classes;
        private String configurationFile;

        public BusinessApplicationInner(@Context ServletConfig servletConfig) {
            classes = new HashSet<>();
            classes.add(HeaderIdContainerFilter.class);
            this.configurationFile =
                servletConfig.getInitParameter(ApplicationParameter.CONFIGURATION_FILE_APPLICATION);
        }


        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }

        @Override
        public Set<Object> getSingletons() {
            // Cannot use public constructor here because VitamApplicationConfiguration is needed in context
            if (singletons == null) {
                singletons = new HashSet<>();
                singletons.add(new GenericExceptionMapper());

                TimeStampSignature timeStampSignature;
                try {
                    final File file = PropertiesUtils.findFile("keystore_logbook.p12");
                    timeStampSignature =
                        new TimeStampSignatureWithKeystore(file, "azerty8".toCharArray());
                } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
                    NoSuchAlgorithmException e) {
                    LOGGER.error("unable to instantiate TimeStampGenerator", e);
                    throw new RuntimeException(e);
                }

                StorageResource storageResource = new StorageResource(new StorageDistributionInnerClass(),
                    new TimestampGenerator(timeStampSignature));
                singletons.add(storageResource);
            }
            return singletons;
        }
    }


    public static class StorageDistributionInnerClass implements StorageDistribution {



        @Override
        public StoredInfoResult copyObjectFromOfferToOffer(DataContext context, String destinationOffer,
            String sourceOffer)
            throws StorageException {
            throw new UnsupportedOperationException("UnsupportedOperationException");
        }

        @Override
        public List<String> getOfferIds(String strategyId) throws StorageException {
            throw new UnsupportedOperationException("UnsupportedOperationException");

        }

        @Override
        public JsonNode getContainerInformation(String strategyId)
            throws StorageNotFoundException, StorageTechnicalException {
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
        public CloseableIterator<ObjectEntry> listContainerObjects(String strategyId, DataCategory category)
            throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            }
            return null;
        }

        @Override
        public CloseableIterator<ObjectEntry> listContainerObjectsForOffer(DataCategory category, String offerId,
            boolean includeDisabled)
            throws StorageException {
            return null;
        }

        @Override
        public Response getContainerByCategory(String strategyId, String objectId, DataCategory category, AccessLogInfoModel logInfo)
            throws StorageException {
            return getContainerByCategoryResponse();
        }

        @Override
        public StoredInfoResult storeDataInAllOffers(String strategyId, String objectId,
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
        public StoredInfoResult storeDataInOffers(String strategyId, String objectId, DataCategory category,
            String requester,
            List<String> offerIds, Response response)
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
        public StoredInfoResult storeDataInOffers(String strategyId, StreamAndInfo streamAndInfo, String objectId,
            DataCategory category, String requester, List<String> offerIds) throws StorageException {
            throw new UnsupportedOperationException("Not implemented");
        }

        /**
         * Get a specific Object binary data as an input stream
         * <p>
         *
         * @param strategyId id of the strategy
         * @param objectId   id of the object
         * @param category
         * @param offerId
         * @return an object as a Response with an InputStream
         * @throws StorageNotFoundException  Thrown if the Container or the object does not exist
         * @throws StorageTechnicalException thrown if a technical error happened
         */
        @Override public Response getContainerByCategory(String strategyId, String objectId, DataCategory category,
            String offerId) throws StorageException {

            return getContainerByCategoryResponse();
        }

        private Response getContainerByCategoryResponse() throws StorageNotFoundException, StorageTechnicalException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Object not found");
            }
            if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical exception");
            }

            final Response response = new AbstractMockClient.FakeInboundResponse(Status.OK,
                new ByteArrayInputStream("test".getBytes()), MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            return response;
        }

        @Override
        public JsonNode getContainerInformation(String strategyId, DataCategory type, String objectId,
            List<String> offerIds, boolean noCache)
            throws StorageNotFoundException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_E.equals(tenantId)) {
                throw new StorageNotFoundException("Not Found");
            }
            StorageMetadataResult res = new StorageMetadataResult(objectId, "object", "abcdef", 6096,
                "Tue Aug 31 10:20:56 SGT 2016", "Tue Aug 31 10:20:56 SGT 2016");
            try {
                return JsonHandler.toJsonNode(res);
            } catch (InvalidParseOperationException e) {
                LOGGER.debug(e);
            }
            return null;
        }

        @Override
        public Map<String, Boolean> checkObjectExisting(String strategyId, String objectId, DataCategory category,
            List<String> offerIds) {
            return offerIds.stream().collect(Collectors.toMap(offer -> offer, offer -> offer.equals(OFFER_ID) ? Boolean.TRUE:Boolean.FALSE));
        }

        @Override
        public void deleteObjectInAllOffers(String strategyId, DataContext context)
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
        public void deleteObjectInOffers(String strategyId, DataContext context,  List <String> offerId) {
            throw new UnsupportedOperationException("UnsupportedOperationException");
        }

        @Override
        public List<BatchObjectInformationResponse> getBatchObjectInformation(String strategyId, DataCategory type,
            List<String> objectIds, List<String> offerIds) {
            throw new UnsupportedOperationException("UnsupportedOperationException");
        }

        @Override
        public void close() {
            // Nothing
        }

        @Override
        public RequestResponse<OfferLog> getOfferLogs(String strategyId, DataCategory category, Long offset, int limit,
            Order order)
            throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical error");
            }
            return new RequestResponseOK<OfferLog>().setHttpCode(Status.OK.getStatusCode());
        }

        /**
         * Get offer log from the given offer
         *
         * @param strategyId the strategy id to get offers
         * @param offerId
         * @param category   the object type to list
         * @param offset     offset of the excluded object
         * @param limit      the number of result wanted
         * @param order      order
         * @return list of offer log
         * @throws StorageException thrown in case of any technical problem
         */
        @Override public RequestResponse<OfferLog> getOfferLogsByOfferId(String strategyId, String offerId,
            DataCategory category,
            Long offset, int limit, Order order) throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical error");
            }
            return new RequestResponseOK<OfferLog>().setHttpCode(Status.OK.getStatusCode());
        }

        @Override
        public BulkObjectStoreResponse bulkCreateFromWorkspace(String strategyId,
            BulkObjectStoreRequest bulkObjectStoreRequest, String requester) {
            throw new UnsupportedOperationException("UnsupportedOperationException");
        }

        @Override
        public Map<String, StorageStrategy> getStrategies() throws StorageException {
            Integer tenantId = ParameterHelper.getTenantParameter();
            if (TENANT_ID_A_E.equals(tenantId)) {
                throw new StorageTechnicalException("Technical exception");
            } else {
                Map<String, StorageStrategy> strategies = new HashMap<String, StorageStrategy>();
                StorageStrategy mockStrategy = new StorageStrategy();
                mockStrategy.setId(VitamConfiguration.getDefaultStrategy());
                strategies.put(VitamConfiguration.getDefaultStrategy(), mockStrategy);
                return strategies;
            }
        }
    }

}
