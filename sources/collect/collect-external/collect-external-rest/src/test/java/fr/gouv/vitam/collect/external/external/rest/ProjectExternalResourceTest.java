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
package fr.gouv.vitam.collect.external.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ProjectExternalResourceTest extends ResteasyTestApplication {

    static final String COLLECT_CONF = "collect-external-test.conf";
    // URI
    private static final String COLLECT_RESOURCE_URI = "collect-external/v1";
    private static CollectExternalMain application;

    // LOGGER
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectExternalResourceTest.class);
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final BusinessApplicationTest businessApplicationTest = new BusinessApplicationTest();
    public static final String QUERY_SEARCH = "{ \"$query\" : \"search\" }";

    private static final String PROJECTS_URI = "/projects";
    private static final CollectInternalClientFactory collectInternalClientFactory =
        businessApplicationTest.getCollectInternalClientFactory();
    private static final CollectInternalClient collectInternalClient = mock(CollectInternalClient.class);
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
        "\"UnitUp\": \"aeaqaaaaaahgnz5dabg42amava5kfoqaaaba\"}";

    private static final int TENANT = 0;
    private static final String PROJECT_RESPONSE_TEST =
        "{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":0,\"size\":1},\"$results\":[{\"#id\":\"aeaaaaaaaae72dhcat4tqamgmokmdliaaaaq\",\"Name\":\"ThisismyName\",\"ArchivalAgreement\":\"IC-000001\",\"MessageIdentifier\":\"20220302-000005\",\"ArchivalAgencyIdentifier\":\"Identifier0\",\"TransferringAgencyIdentifier\":\"Identifier3\",\"OriginatingAgencyIdentifier\":\"FRAN_NP_009915\",\"SubmissionAgencyIdentifier\":\"FRAN_NP_005061\",\"AcquisitionInformation\":\"Versement\",\"LegalStatus\":\"Archivesprivées\",\"Comment\":\"Versementduserviceproducteur:CabinetdeMichelMercier\",\"UnitUp\":\"aeaqaaaaaahgnz5dabg42amava5kfoqaaaba\",\"#tenant\":1,\"CreationDate\":\"2023-02-18T08:12:18.738\",\"LastUpdate\":\"2023-02-18T08:12:18.738\",\"Status\":\"OPEN\"}],\"$facetResults\":[],\"$context\":{}}";

    @Override
    public Set<Object> getResources() {
        return businessApplicationTest.getSingletons();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return businessApplicationTest.getClasses();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new CollectExternalMain(COLLECT_CONF, ProjectExternalResourceTest.class, null);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = COLLECT_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Collect Application Server", e);
        }
    }

    @Before
    public void setUpBefore() {
        reset(collectInternalClient);
        reset(collectInternalClientFactory);
        when(collectInternalClientFactory.getClient()).thenReturn(collectInternalClient);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        junitHelper.releasePort(port);
        if (application != null) {
            application.stop();
        }
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void getProjects_ok() throws Exception {
        final RequestResponse<JsonNode> responseProjects = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromString(PROJECT_RESPONSE_TEST))
            .setHttpCode(200);

        when(collectInternalClient.getProjects()).thenReturn(responseProjects);
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .header(EXPECT, EXPECT_CONTINUE)
            .when()
            .get(PROJECTS_URI)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getProject_ById_ok_When_Id_found() throws Exception {
        final RequestResponse<JsonNode> responseProjects = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromString(PROJECT_RESPONSE_TEST))
            .setHttpCode(200);
        String PROJECT_ID = "SOME_ID";
        when(collectInternalClient.getProjectById(PROJECT_ID)).thenReturn(responseProjects);
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .header(EXPECT, EXPECT_CONTINUE)
            .when()
            .get(PROJECTS_URI + "/" + PROJECT_ID)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void getProject_ById_ok_When_Id_Not_found() throws Exception {
        String PROJECT_ID = "SOME_ID";
        when(collectInternalClient.getProjectById(PROJECT_ID)).thenThrow(
            new VitamClientException("Unable to find project Id or invalid status")
        );
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .header(EXPECT, EXPECT_CONTINUE)
            .when()
            .get(PROJECTS_URI + "/" + PROJECT_ID)
            .then()
            .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void getProjects_nok() throws VitamClientException {
        when(collectInternalClient.getProjects()).thenThrow(new VitamClientException("error"));
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS_URI)
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
        final RequestResponseOK<JsonNode> responseProjects = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.toJsonNode(projectDtoList))
            .setHttpCode(200);

        when(collectInternalClient.searchProject(any())).thenReturn(responseProjects);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_SEARCH))
            .when()
            .get(PROJECTS_URI)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void initProject_ok() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTenant(0);
        projectDto.setId("1");
        projectDto.setName("name");
        final RequestResponseOK<JsonNode> responseProject = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.toJsonNode(projectDto))
            .setHttpCode(200);

        when(collectInternalClient.initProject(any())).thenReturn(responseProject);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .post(PROJECTS_URI)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void updateProject() throws Exception {
        ProjectDto projectDto = new ProjectDto();
        final RequestResponseOK<JsonNode> responseProjects = new RequestResponseOK<JsonNode>().addResult(
            JsonHandler.toJsonNode(projectDto)
        );
        when(collectInternalClient.updateProject(any())).thenReturn(responseProjects);
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body(JsonHandler.getFromString(QUERY_INIT))
            .when()
            .put(PROJECTS_URI)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void deleteProjectById_ok_When_Id_found() throws Exception {
        final RequestResponse<JsonNode> responseProjects = new RequestResponseOK<JsonNode>()
            .addResult(JsonHandler.getFromString(PROJECT_RESPONSE_TEST))
            .setHttpCode(200);
        String PROJECT_ID = "SOME_ID";
        when(collectInternalClient.deleteProjectById(PROJECT_ID)).thenReturn(responseProjects);
        given()
            .accept(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .header(EXPECT, EXPECT_CONTINUE)
            .when()
            .delete(PROJECTS_URI + "/" + PROJECT_ID)
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldGetInternalServerErrorWhenSearchProjectWithoutQuery() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON) // If Content-Type: application/json is set, searchProject is called instead getProjects
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .when()
            .get(PROJECTS_URI)
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
            .body("message", Matchers.equalTo("An error occurred while converting the DTO to a JsonNode"));
    }

    @Test
    public void shouldGetInternalServerErrorWhenSearchProjectWithNonJsonQuery() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON) // If Content-Type: application/json is set, searchProject is called instead getProjects
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("no_json_content")
            .when()
            .get(PROJECTS_URI)
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
            .body("description", Matchers.containsString("Unrecognized token 'no_json_content'"));
    }

    @Test
    public void shouldGetInternalServerErrorWhenSearchProjectWithInsaneJsonQuery() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON) // If Content-Type: application/json is set, searchProject is called instead getProjects
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("{ \"$query\": \"<div>malicious input</div>\" }")
            .when()
            .get(PROJECTS_URI)
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
            .body("message", Matchers.containsString("HTML PATTERN found"));
    }

    @Test
    public void shouldThrowWhenCreateProjectWithNullProject() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT)
                .body((String) null)
                .when()
                .post(PROJECTS_URI);
        });
    }

    @Test
    public void shouldThrowWhenCreateProjectWithEmptyStringProject() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("")
            .when()
            .post(PROJECTS_URI)
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
            .body("message", Matchers.equalTo("Internal Server Error"))
            .body("description", Matchers.equalTo("You must supply projects data!"));
    }

    @Test
    public void shouldCreateProjectWithEmptyProject() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT)
            .body("{}")
            .when()
            .post(PROJECTS_URI)
            .then()
            .statusCode(OK.getStatusCode());
    }
}
