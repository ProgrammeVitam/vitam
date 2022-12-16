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

import fr.gouv.vitam.collect.external.dto.ObjectDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.CollectUnitModel;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import io.restassured.http.ContentType;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class CollectMetadataResourceTest extends CollectResourceBaseTest {

    private static final int TENANT = 0;
    public static final String UNITS = "units";

    public static final String QUERY_OBJECT = "{ \"id\" : \"1\",  \"fileInfo\" : {\"filename\" : \"name\"} }";
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
        when(metadataService.selectUnitById("1")).thenThrow(new CollectException("error"));
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
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectException("error"));
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
        when(metadataService.selectObjectGroupById(any(), eq(true))).thenReturn(JsonHandler.getFromString(OK_RESULT));
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
        when(metadataService.selectObjectGroupById("1", true)).thenThrow(new CollectException("error"));
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
        when(metadataService.selectObjectGroupById("1", true)).thenThrow(new IllegalArgumentException("error"));
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
        when(collectService.getArchiveUnitModel("1")).thenReturn(new CollectUnitModel());
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(OBJECT_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(UNITS + "/1/objects/BinaryMaster/1/binary")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
        }
    }

    @Test
    public void upload_ko_collect_error() throws Exception {
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectException("error"));
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
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
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
        when(collectService.getArchiveUnitModel("1")).thenThrow(new CollectException("error"));
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
