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
package fr.gouv.vitam.collect.internal.resource;

import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.CollectUnitModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import io.restassured.http.ContentType;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.hamcrest.Matchers;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class CollectMetadataInternalResourceImplTest extends CollectInternalResourceBaseTest {

    private static final int TENANT = 0;
    public static final String UNITS = "units";

    public static final String QUERY_OBJECT = "{ \"_id\" : \"1\",  \"FileInfo\" : {\"Filename\" : \"name\"} }";
    public static final String OBJECTS = "objects";
    private static final String OBJECT_ZIP_PATH = "streamZip/transaction.zip";
    public static final String OK_RESULT = "{\n" +
        "        \"httpCode\": 200,\n" +
        "        \"$hits\": {\n" +
        "          \"total\": 52,\n" +
        "          \"size\": 2,\n" +
        "          \"offset\": 0,\n" +
        "          \"limit\": 100\n" +
        "        }}";

    @Test
    public void getUnitById() throws Exception {
        when(metadataService.selectUnitById("1")).thenReturn(JsonHandler.getFromString(OK_RESULT));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getUnitById_ko_with_collect_error() throws Exception {
        when(metadataService.selectUnitById("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getUnitById_ko_with_parsing_error() throws Exception {
        when(metadataService.selectUnitById("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void uploadObjectGroup() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenReturn(new CollectUnitModel());
        when(
            collectService.updateOrSaveObjectGroup(any(CollectUnitModel.class), any(DataObjectVersionType.class), eq(1),
                any(ObjectDto.class))).thenReturn(new ObjectDto());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_OBJECT))
            .when()
            .post(UNITS + "/1/objects/BinaryMaster/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void uploadObjectGroup_ko_collect_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_OBJECT))
            .when()
            .post(UNITS + "/1/objects/BinaryMaster/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void uploadObjectGroup_ko_parsing_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_OBJECT))
            .when()
            .post(UNITS + "/1/objects/BinaryMaster/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getObjectById() throws Exception {
        when(metadataService.selectObjectGroupById(any())).thenReturn(JsonHandler.getFromString(OK_RESULT));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(OBJECTS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }


    @Test
    public void getObjectById_ko_collect_error() throws Exception {
        when(metadataService.selectObjectGroupById("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(OBJECTS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getObjectById_ko_parsing_error() throws Exception {
        when(metadataService.selectObjectGroupById("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(OBJECTS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void upload() throws Exception {
        CollectUnitModel uaWithExistingOpi= new CollectUnitModel();
        uaWithExistingOpi.setOpi("1");

        CollectUnitModel uaWithBadTransactionStatus= new CollectUnitModel();
        uaWithBadTransactionStatus.setOpi("2");

        CollectUnitModel uaWithOpiNull= new CollectUnitModel();

        CollectUnitModel uaWithOpiAndBadTransaction= new CollectUnitModel();
        uaWithOpiAndBadTransaction.setOpi("2");

        TransactionModel transactionWithCorrectStatus = new TransactionModel();
        transactionWithCorrectStatus.setStatus(TransactionStatus.OPEN);

        TransactionModel transactionWithBadStatus = new TransactionModel();
        transactionWithBadStatus.setStatus(TransactionStatus.SENT);

        //ua with opi ok & tranction ok
        when(collectService.getArchiveUnitModel("11")).thenReturn(uaWithExistingOpi);
        //ua with opi ok & tranction ko
        when(collectService.getArchiveUnitModel("12")).thenReturn(uaWithBadTransactionStatus);
        //ua with opi ko & tranction ko
        when(collectService.getArchiveUnitModel("21")).thenReturn(uaWithOpiNull);
        //ua with opi ok & tranction ko
        when(collectService.getArchiveUnitModel("22")).thenReturn(uaWithOpiNull);

        when(transactionService.findTransaction("1")).thenReturn(Optional.of(transactionWithCorrectStatus));
        when(transactionService.findTransaction("2")).thenReturn(Optional.of(transactionWithBadStatus));

        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/11/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
        }
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/6/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("message", Matchers.equalTo("UA not found"));
        }
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/12/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("message", Matchers.equalTo("Invalid transaction status"));
        }
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/21/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("message", Matchers.equalTo("Operation Id not found"));
        }
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/22/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body("message", Matchers.equalTo("Operation Id not found"));
        }
    }

    @Test
    public void upload_ko_collect_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectInternalException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/1/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void upload_ko_parsing_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new IllegalArgumentException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/1/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void download() throws Exception {
        CollectUnitModel collectUnitModel = new CollectUnitModel();
        when(collectService.getArchiveUnitModel("1")).thenReturn(collectUnitModel);
        when(collectService.getDbObjectGroup(any(CollectUnitModel.class))).thenReturn(new DbObjectGroupModel());
        when(collectService.getBinaryByUsageAndVersion(collectUnitModel, BINARY_MASTER, 1)).thenReturn(Response.status(
            Response.Status.OK).entity("test download").build());

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1/objects/BinaryMaster/1/binary")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void download_ko_collecterror() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1/objects/BinaryMaster/1/binary")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void download_ko_parsing_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1/objects/BinaryMaster/1/binary")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void download_ko_storage_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenReturn(new CollectUnitModel());
        when(collectService.getDbObjectGroup(any(CollectUnitModel.class))).thenReturn(new DbObjectGroupModel());
        when(collectService.getBinaryByUsageAndVersion(any(CollectUnitModel.class), any(DataObjectVersionType.class),
            eq(1))).thenThrow(new StorageNotFoundException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(UNITS + "/1/objects/BinaryMaster/1/binary")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
