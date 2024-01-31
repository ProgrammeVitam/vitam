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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.rest.TransactionInternalResource;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class TransactionInternalResourceTest extends CollectInternalResourceBaseTest {

    private static final int TENANT = 0;
    public static final String TRANSACTIONS = "/transactions";

    private static final String TRANSACTION_ZIP_PATH = "streamZip/transaction.zip";

    private static final String UNITS_WITH_INHERITED_RULES_URI = "/unitsWithInheritedRules";
    public static final String QUERY_INIT = "{ "
        + "\"#id\": \"1\","
        + "\"ArchivalAgencyIdentifier\": \"Identifier0\","
        + "\"TransferringAgencyIdentifier\": \"Identifier3\","
        + "\"OriginatingAgencyIdentifier\": \"FRAN_NP_009915\","
        + "\"SubmissionAgencyIdentifier\": \"FRAN_NP_005061\","
        + "\"MessageIdentifier\": \"20220302-000005\","
        + "\"Name\": \"This is my Name\","
        + "\"LegalStatus\": \"Archives privées\","
        + "\"AcquisitionInformation\": \"Versement\","
        + "\"ArchivalAgreement\":\"IC-000001\","
        + "\"Comment\": \"Versement du service producteur : Cabinet de Michel Mercier\"}";

    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String DATA_HTML =
        "{ \"#id\": \"<a href='www.culture.gouv.fr'>Culture</a>\"," + "\"data\": \"data2\" }";


    private static final String QUERY_TEST =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    private static final String EMPTY_QUERY = "{ \"$query\" : \"\", \"$roots\" : []  }";

    @Test
    public void getTransactionById() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getTransactionById_ko_with_empty_result() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getTransactionById_ko_with_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getTransactionById_ko_with_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void updateTransaction() throws Exception {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setStatus(TransactionStatus.OPEN);
        transactionModel.setProjectId("1");
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(transactionModel));
        doNothing().when(transactionService).replaceTransaction(any(TransactionDto.class));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(TRANSACTIONS)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void updateTransaction_ko_with_not_found_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(TRANSACTIONS)
            .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void updateTransaction_ko_with_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(TRANSACTIONS)
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void updateTransaction_ko_with_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(TRANSACTIONS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void deleteTransactionById() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void deleteTransactionById_ko_not_found_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void deleteTransactionById_ko_collection_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void deleteTransactionById_ko_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(TRANSACTIONS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(true);
        when(metadataService.saveArchiveUnit(any(), any(TransactionModel.class))).thenReturn(
            JsonHandler.getFromString("{}"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .post(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }


    @Test
    public void uploadArchiveUnit_ko_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .post(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit_ko_not_found_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .post(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit_ko_not_open_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(false);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .post(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit_ko_saving_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(true);
        when(metadataService.saveArchiveUnit(any(), any(TransactionModel.class))).thenReturn(null);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .post(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testSelectUnitsByTransactionId_ko_collect_error() throws Exception {
        when(metadataService.selectUnitsByTransactionId(any(JsonNode.class), eq("15"))).thenThrow(
            new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .get(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void selectUnits() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .get(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void closeTransaction() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/close")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void closeTransaction_ko_collect_error() throws Exception {
        doThrow(new CollectInternalException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/close")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void closeTransaction_ko_parsing_error() throws Exception {
        doThrow(new IllegalArgumentException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/close")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void abortTransaction() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/abort")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void abortTransaction_ko_collect_error() throws Exception {
        doThrow(new CollectInternalException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/abort")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void abortTransaction_ko_parsing_error() throws Exception {
        doThrow(new IllegalArgumentException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/abort")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void reopenTransaction() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/reopen")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void reopenTransaction_ko_collect_error() throws Exception {
        doThrow(new CollectInternalException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/reopen")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void reopenTransaction_ko_parsing_error() throws Exception {
        doThrow(new IllegalArgumentException("error")).when(transactionService).changeTransactionStatus(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .put(TRANSACTIONS + "/1/reopen")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void generateAndSendSip_ko_not_found_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        given()
            .contentType(CommonMediaType.APPLICATION_OCTET_STREAM_TYPE.getType())
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/send")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void generateAndSendSip_ko_not_ready_error() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.READY))).thenReturn(
            false);
        given()
            .contentType(CommonMediaType.APPLICATION_OCTET_STREAM_TYPE.getType())
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/send")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void generateAndSendSip_ko_digest_i_null() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.READY))).thenReturn(true);
        doNothing().when(transactionService).isTransactionContentEmpty(any());
        when(sipService.generateSip(any())).thenReturn(null);
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/send")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void generateAndSendSip_ko_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(CommonMediaType.APPLICATION_OCTET_STREAM_TYPE.getType())
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/send")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void generateAndSendSip_ko_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(CommonMediaType.APPLICATION_OCTET_STREAM_TYPE.getType())
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .post(TRANSACTIONS + "/1/send")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void updateUnits_ko_with_status_not_open() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(false);
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .put(TRANSACTIONS + "/1/units")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void updateUnits_ko_with_not_found_transaction() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .put(TRANSACTIONS + "/1/units")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void updateUnits_ko_with_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .put(TRANSACTIONS + "/1/units")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void updateUnits_ko_with_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .put(TRANSACTIONS + "/1/units")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip() throws Exception {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setProjectId("1");
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(transactionModel));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(true);
        when(projectService.findProject("1")).thenReturn(Optional.of(new ProjectDto()));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_ko_with_status_not_open() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(new TransactionModel()));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(false);
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_with_not_found_transaction() throws Exception {
        when(transactionService.findTransaction("1")).thenReturn(Optional.empty());
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_with_collect_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new CollectInternalException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_ko_with_parsing_error() throws Exception {
        when(transactionService.findTransaction("1")).thenThrow(new IllegalArgumentException("error"));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_ko_with_project_not_found() throws Exception {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setProjectId("1");
        when(transactionService.findTransaction("1")).thenReturn(Optional.of(transactionModel));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(true);
        doThrow(new CollectInternalException("error")).when(fluxService)
            .processStream(any(), any(TransactionModel.class));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                .contentType("application/zip")
                .accept(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body(resourceAsStream)
                .when()
                .post(TRANSACTIONS + "/1/upload")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void uploadTransactionZip_ko_with_mapping_error_in_metadata_csv() throws Exception {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setProjectId("1");
        when(transactionService.findTransaction("1"))
                .thenReturn(Optional.of(transactionModel));
        when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(true);
        doThrow(new CollectInternalException("Mapping for File not found, expected one of [Content.DescriptionLevel, Content.Title]"))
                .when(fluxService).processStream(any(), any(TransactionModel.class));
        try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(TRANSACTION_ZIP_PATH)) {
            given()
                    .contentType("application/zip")
                    .accept(ContentType.JSON)
                    .header(GlobalDataRest.X_TENANT_ID, TENANT)
                    .body(resourceAsStream)
                    .when()
                    .post(TRANSACTIONS + "/1/upload")
                    .then()
                    .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("message", CoreMatchers.equalTo("Mapping for File not found, expected one of [Content.DescriptionLevel, Content.Title]"));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_error_when_update_units_using_empty_stream() {
        // Given
        final InputStream resourceAsStream = new ByteArrayInputStream(new byte[0]);
        when(transactionService.checkStatus(any(), eq(TransactionStatus.OPEN))).thenReturn(true);

        // When - Then
        given()
            .contentType(ContentType.BINARY)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(resourceAsStream)
            .when()
            .put(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void should_upload_transaction_zip_when_transaction_is_open() throws Exception {
        // TODO to redo to follow the model of the other tests
        // Given
        TransactionInternalResource transactionInternalResource =
            new TransactionInternalResource(transactionService, sipService, metadataService, fluxService,
                projectService);

        final ProjectDto projectDto = new ProjectDto();
        String PROJECT_ID = "PROJECT_ID";
        String TRANSACTION_ID = "TRANSACTION_ID";

        projectDto.setId(PROJECT_ID);

        final TransactionModel transactionModel = new TransactionModel();
        transactionModel.setId(TRANSACTION_ID);
        transactionModel.setStatus(TransactionStatus.OPEN);
        transactionModel.setProjectId(PROJECT_ID);
        when(transactionService.findTransaction(eq(TRANSACTION_ID))).thenReturn(Optional.of(transactionModel));
        when(projectService.findProject(eq(PROJECT_ID))).thenReturn(Optional.of(projectDto));
        final InputStream inputStreamZip =
            PropertiesUtils.getResourceAsStream("streamZip/transaction.zip");

        when(transactionService.checkStatus(any(), eq(TransactionStatus.OPEN))).thenReturn(true);

        // When
        Response result = transactionInternalResource.uploadTransactionZip(TRANSACTION_ID, inputStreamZip);
        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void selectUnitsByTransactionId_ko_when_collectService_error() throws Exception {
        when(metadataService.selectUnitsByTransactionId(any(JsonNode.class), eq("1"))).thenThrow(
            new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .get(TRANSACTIONS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void selectUnitsByTransactionId_ok_when_collectServie_ok() throws Exception {
        when(metadataService.selectUnitsByTransactionId(any(JsonNode.class), eq("10"))).thenReturn(
            new RequestResponseOK<>());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString("{}"))
            .when()
            .get(TRANSACTIONS + "/10/units")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }


    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithNotJsonRequest_ThenReturnError_UnsupportedMediaType()
        throws Exception {
        given()
            .contentType(ContentType.XML)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2).asText())
            .when().get(TRANSACTIONS + "/1" + UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithJsonContainsHtml_ThenReturnError_BadRequest()
        throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithRoots(DATA_HTML)).when()
            .get(TRANSACTIONS + "/1" + UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenStartedServer_WhenSelectUnitsWithInheritedRulesWithEmptyQuery_ThenReturnError_Forbidden()
        throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(EMPTY_QUERY)).when()
            .get(TRANSACTIONS + "/1" + UNITS_WITH_INHERITED_RULES_URI).then()
            .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    /**
     * @param data
     * @return query DSL with Options
     * @throws InvalidParseOperationException
     */
    private static JsonNode buildDSLWithOptions(String query, String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [], \"$query\" : [ " + query + " ], \"$data\" : " + data + " }");
    }

    /**
     * @param data
     * @return query DSL with id as Roots
     * @throws InvalidParseOperationException
     */
    private static JsonNode buildDSLWithRoots(String data) throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ \"$roots\" : [ " + data + " ], \"$query\" : [ \"\" ], \"$data\" : " + data + " }");
    }
}