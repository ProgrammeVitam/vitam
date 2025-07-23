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

import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.rest.ProjectInternalResource;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.json.JsonHandler;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectInternalResourceTest extends CollectInternalResourceBaseTest {

    private static final int TENANT = 0;

    public static final String QUERY_SEARCH = "{ \"$query\" : \"search\" }";
    public static final String QUERY_INIT =
        "{ " +
        "\"ArchivalAgencyIdentifier\": \"Identifier0\"," +
        "\"TransferringAgencyIdentifier\": \"Identifier3\"," +
        "\"OriginatingAgencyIdentifier\": \"FRAN_NP_009915\"," +
        "\"SubmissionAgencyIdentifier\": \"FRAN_NP_005061\"," +
        "\"MessageIdentifier\": \"20220302-000005\"," +
        "\"Name\": \"This is my Name\"," +
        "\"LegalStatus\": \"Archive privée\"," +
        "\"AcquisitionInformation\": \"Versement\"," +
        "\"ArchivalAgreement\":\"IC-00001\"," +
        "\"Comment\": \"Versement du service producteur : Cabinet de Michel Mercier\"," +
        "\"UnitUp\": \"aeaqaaaaaahgnz5dabg42amava5kfoqaaaba\"," +
        "\"TransformationRules\":\"{}\"}";

    public static final String QUERY_UA_BY_ID =
        "{ " +
        "\"$roots\": []," +
        "\"$query\": [" +
        "{ " +
        "    \"$match\": {" +
        "   \"Title\": \"Saint\"" +
        " }" +
        "}" +
        "]," +
        "\"$filter\": {}," +
        "\"$projection\": {}" +
        "}";

    public static final String EMPTY_QUERY =
        "{ " + "\"$roots\": []," + "\"$query\": []," + "\"$filter\": {}," + "\"$projection\": {}" + "}";

    public static final String ROOT_QUERY =
        "{ " +
        "\"$roots\": [{\"root\": \"root\"}]," +
        "\"$query\": []," +
        "\"$filter\": {}," +
        "\"$projection\": {}" +
        "}";

    public static final String QUERY_INIT_TRANSACTION =
        "{ " +
        "\"Name\": \"Versement des objets binaires\", " +
        "\"ArchivalAgreement\": \"IC-000001\"," +
        "\"MessageIdentifier\": \"Transaction de test\"," +
        "\"ArchivalAgencyIdentifier\": \"ArchivalAgencyIdentifier5\"," +
        "\"TransferringAgencyIdentifier\": \"TransferingAgencyIdentifier5\"," +
        "\"OriginatingAgencyIdentifier\": \"FRAN_NP_009913\"," +
        "\"SubmissionAgencyIdentifier\": \"FRAN_NP_005761\"," +
        "\"ArchiveProfile\": \"ArchiveProfile5\"," +
        "\"Comment\": \"Commentaire\"}";
    public static final String PROJECTS = "/projects";

    @Test
    public void getProjects_ok() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTenant(0);
        projectDto.setId("1");
        projectDto.setName("name");
        List<ProjectDto> projectDtoList = new ArrayList<>();
        projectDtoList.add(projectDto);
        when(projectService.searchProject(isNull())).thenReturn(projectDtoList);
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getProjects_nok() throws CollectInternalException {
        when(projectService.searchProject(isNull())).thenThrow(new CollectInternalException("error"));
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void searchProject_ok() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTenant(0);
        projectDto.setId("1");
        projectDto.setName("name");
        List<ProjectDto> projectDtoList = new ArrayList<>();
        projectDtoList.add(projectDto);
        when(projectService.searchProject(any())).thenReturn(projectDtoList);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_SEARCH))
            .when()
            .get(PROJECTS)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void searchProject_ko_collect_error() throws Exception {
        when(projectService.searchProject(any())).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_SEARCH))
            .when()
            .get(PROJECTS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void initProject_ok() throws Exception {
        doReturn(new ProjectDto()).when(projectService).createProject(any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .post(PROJECTS)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void initProject_nok_with_collect_exception() throws Exception {
        doThrow(new CollectInternalException("error")).when(projectService).createProject(any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .post(PROJECTS)
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void initProject_nok_with_parsing_exception() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("")
            .when()
            .post(PROJECTS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void updateProject() throws Exception {
        ProjectDto projectDto = new ProjectDto().setName("name");
        ProjectDto updatedProjectDto = new ProjectDto().setName("updatedName");
        doReturn(updatedProjectDto).when(projectService).updateProject(any(), any());
        when(projectService.findProject(any())).thenReturn(Optional.of(projectDto));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(PROJECTS)
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body(Matchers.containsString("updatedName"));
    }

    @Test
    public void updateProject_ko() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("")
            .when()
            .put(PROJECTS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        verify(projectService, never()).findProject(any());
        verify(projectService, never()).updateProject(any(), any());
    }

    @Test
    public void updateProject_ko_project_not_found() throws Exception {
        when(projectService.findProject(any())).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(PROJECTS)
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
        verify(projectService, never()).updateProject(any(), any());
    }

    @Test
    public void getProjectById() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        when(projectService.findProject(any())).thenReturn(Optional.of(projectDto));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getProjectById_ko_project_not_found() throws Exception {
        when(projectService.findProject(any())).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getProjectById_ko_collect_error() throws Exception {
        when(projectService.findProject(any())).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getProjectById_ko_parsing_error() throws Exception {
        when(projectService.findProject(any())).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void deleteProjectById() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        when(projectService.findProject(any())).thenReturn(Optional.of(projectDto));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void deleteProjectById_ko_project_not_found() throws Exception {
        when(projectService.findProject(any())).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void deleteProjectById_ok_with_transaction() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId("1");
        when(projectService.findProject(any())).thenReturn(Optional.of(projectDto));
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setProjectId("1");
        transactionModel.setId("1");
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.of(transactionModel));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
        verify(transactionService, times(1)).deleteTransaction("1");
        verify(projectService, times(1)).deleteProjectById("1");
    }

    @Test
    public void deleteProjectById_ko_with_collect_error() throws Exception {
        when(projectService.findProject(any())).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void deleteProjectById_ko_with_parsing_error() throws Exception {
        when(projectService.findProject(any())).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .delete(PROJECTS + "/1")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getUnitsByProjectId() throws Exception {
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.of(new TransactionModel()));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_UA_BY_ID))
            .when()
            .get(PROJECTS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getUnitsByProjectId_ko_transaction_not_found() throws Exception {
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_UA_BY_ID))
            .when()
            .get(PROJECTS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getUnitsByProjectId_ko_parsing_error() throws Exception {
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS + "/1/units")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getUnitsByProjectId_ko_empty_query() throws Exception {
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(EMPTY_QUERY))
            .when()
            .get(PROJECTS + "/1/units")
            .then()
            .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void getUnitsByProjectId_ko_root_query() throws Exception {
        when(transactionService.findLastTransactionByProjectId("1")).thenReturn(Optional.empty());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(ROOT_QUERY))
            .when()
            .get(PROJECTS + "/1/units")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getAllTransactions() throws Exception {
        when(transactionService.findTransactionsByProjectId("1")).thenReturn(List.of(new TransactionDto()));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_UA_BY_ID))
            .when()
            .get(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getAllTransactions_ko_with_collect_error() throws Exception {
        when(transactionService.findTransactionsByProjectId("1")).thenThrow(new CollectInternalException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_UA_BY_ID))
            .when()
            .get(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void getAllTransactions_ko_with_parsing_error() throws Exception {
        when(transactionService.findTransactionsByProjectId("1")).thenThrow(new IllegalArgumentException("error"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_UA_BY_ID))
            .when()
            .get(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void initTransaction() throws Exception {
        ArgumentCaptor<TransactionDto> transactionDtoArgumentCaptor = forClass(TransactionDto.class);

        ProjectDto project = new ProjectDto();
        Mockito.doReturn(Optional.of(project)).when(projectService).findProject(eq("1"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT_TRANSACTION))
            .when()
            .post(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
        verify(projectService).findProject(eq("1"));
        verify(transactionService, times(1)).createTransaction(transactionDtoArgumentCaptor.capture(), eq(project));
    }

    @Test
    public void initTransaction_ko_with_collect_error() throws Exception {
        Mockito.doReturn(Optional.of(new ProjectDto())).when(projectService).findProject(eq("1"));
        doThrow(new CollectInternalException("error")).when(transactionService).createTransaction(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT_TRANSACTION))
            .when()
            .post(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void initTransaction_ko_with_project_not_found() throws Exception {
        Mockito.doReturn(Optional.empty()).when(projectService).findProject(eq("1"));
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT_TRANSACTION))
            .when()
            .post(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
            .body("message", Matchers.equalTo(ProjectInternalResource.PROJECT_NOT_FOUND));
        verify(transactionService, never()).createTransaction(any(), any());
    }

    @Test
    public void initTransaction_ko_with_parsing_error() throws Exception {
        doThrow(new CollectInternalException("error")).when(transactionService).createTransaction(any(), any());
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("")
            .when()
            .post(PROJECTS + "/1/transactions")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void upload_zip_to_project_OK() throws Exception {
        ProjectDto project = new ProjectDto();
        project.setId("prId");
        doReturn(Optional.of(project)).when(projectService).findProject("prId");
        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .header(GlobalDataRest.X_ENCODING, "x-mac-greek")
            .body(new NullInputStream(100))
            .when()
            .post(PROJECTS + "/prId/upload")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
        verify(fluxService).processStream(any(), eq("prId"), eq("VIRTUAL_TX_prId"), eq("x-mac-greek"), any());
    }

    @Test
    public void upload_zip_to_project_not_found_project() throws Exception {
        doReturn(Optional.empty()).when(projectService).findProject("prId");
        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(new NullInputStream(100))
            .when()
            .post(PROJECTS + "/prId/upload")
            .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
        verify(fluxService, never()).processStream(any(), eq("prId"), eq("VIRTUAL_TX_prId"), any(), any());
    }

    @Test
    public void upload_zip_to_project_error() throws Exception {
        ProjectDto project = new ProjectDto();
        project.setId("prId");
        doReturn(Optional.of(project)).when(projectService).findProject("prId");
        doThrow(new CollectInternalException("error"))
            .when(fluxService)
            .processStream(any(), eq("prId"), eq("VIRTUAL_TX_prId"), any(), any());
        given()
            .contentType(CommonMediaType.ZIP)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(new NullInputStream(100))
            .when()
            .post(PROJECTS + "/prId/upload")
            .then()
            .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
