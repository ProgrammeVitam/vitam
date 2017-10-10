package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.AddAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AdminManagementClientFactory.class})
public class AdminManagementExternalResourceImplTest {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(AdminManagementExternalResourceImplTest.class);

    private static final String RESOURCE_URI = "/admin-external/v1";

    private static final String FORMAT_URI = "/" + AdminCollections.FORMATS.getName();

    private static final String RULES_URI = "/" + AdminCollections.RULES.getName();
    private static final String AGENCIES_URI = "/" + AdminCollections.AGENCIES.getName();


    private static final String DOCUMENT_ID = "/1";

    private static final String RULE_ID = "/APP-00001";

    private static final String AGENCY_ID = "/AG-000001";


    private static final String WRONG_URI = "/wrong-uri";

    private static final String TENANT_ID = "0";

    private static final String UNEXISTING_TENANT_ID = "25";
    private static final String PROFILE_URI = "/profiles";
    private static final String AGENCY_URI = "/agencies";
    private static final String CONTRACT_ID = "NAME";

    private static final String TRACEABILITY_OPERATION_ID = "op_id";
    private static final String TRACEABILITY_OPERATION_BASE_URI = AccessExtAPI.TRACEABILITY_API + "/";
    private static final String ACCESSION_REGISTER_DETAIL_URI = AccessExtAPI.ACCESSION_REGISTERS_API +
        "/FR_ORG_AGEC/" +
        AccessExtAPI.ACCESSION_REGISTERS_DETAIL;
    private static final String CHECK_TRACEABILITY_OPERATION_URI = AccessExtAPI.TRACEABILITY_API + "/check";

    private static final String request = "{ $query: {} , $projection: {}, $filter: {} }";

    private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    private static final String GOOD_ID = "goodId";
    public static final String SECURITY_PROFILES_URI = "/securityprofiles";


    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static AccessExternalMain application;
    private static AdminManagementClient adminCLient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            application = new AccessExternalMain("access-external-test.conf", BusinessApplicationTest.class, null);
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access External Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (application != null && application.getVitamServer() != null &&
            application.getVitamServer() != null) {

            application.stop();
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public void testCheckDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AGENCIES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(AGENCIES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(AGENCIES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(WRONG_URI)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testCheckDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("Referential Exception")).when(adminCLient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void insertDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(FORMAT_URI)
            .then().statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(RULES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        given().contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(WRONG_URI)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given().contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "vitam.conf")
            .when().post(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(ContentType.BINARY)
            .when().post(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void insertDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("")).when(adminCLient).importFormat(anyObject(), anyObject());
        doReturn(Response.ok().build()).when(adminCLient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new DatabaseConflictException("")).when(adminCLient).importFormat(anyObject(), anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.CONFLICT.getStatusCode());

    }

    @Test
    public void testGetDocuments() throws InvalidCreateOperationException, FileNotFoundException {
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));
        AdminManagementClientFactory.changeMode(null);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI + RULE_ID)
            .then().statusCode(Status.OK.getStatusCode());

        
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_URI + AGENCY_ID)
            .then().statusCode(Status.OK.getStatusCode());


        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());


        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(WRONG_URI)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API + "/" + GOOD_ID)
            .then().statusCode(Status.NOT_IMPLEMENTED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testGetDocumentsError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("")).when(adminCLient).getFormats(anyObject());
        doThrow(new ReferentialException("")).when(adminCLient).getFormatByID(anyObject());
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());


        doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormats(anyObject());
        doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormatByID(anyObject());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        RequestResponse rsp = new RequestResponseOK<>().setHttpCode(Status.OK.getStatusCode());
        when(adminCLient.getAccessionRegister(anyObject())).thenReturn(rsp);
        when(adminCLient.getAccessionRegisterDetail(anyObject(), anyObject())).thenReturn(rsp);

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        doThrow(new InvalidParseOperationException("")).when(adminCLient)
            .getAccessionRegister(anyObject());
        doThrow(new InvalidParseOperationException("")).when(adminCLient)
            .getAccessionRegisterDetail(anyString(), anyObject());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new IllegalArgumentException("")).when(adminCLient)
            .getAccessionRegister(anyObject());
        doThrow(new IllegalArgumentException("")).when(adminCLient)
            .getAccessionRegisterDetail(anyString(), anyObject());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(select.getFinalSelect())
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testImportIngestContractsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.BAD_REQUEST).when(adminCLient).importIngestContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ENTRY_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testimportValidIngestContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.CREATED).when(adminCLient).importIngestContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ENTRY_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindIngestContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);

        doReturn(new RequestResponseOK<>().addAllResults(getIngestContracts())).when(adminCLient)
            .findIngestContracts(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ENTRY_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());
    }


    @Test
    public void testImportAccessContractsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.BAD_REQUEST).when(adminCLient).importAccessContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testimportValidAccessContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.CREATED).when(adminCLient).importAccessContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");

        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindAccessContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminCLient)
            .findAccessContracts(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());
    }



    @Test
    public void testCreateProfileWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode())).when(adminCLient)
            .createProfiles(anyObject());

        File fileProfiles = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testcreateValidProfileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).when(adminCLient)
            .createProfiles(anyObject());

        File fileProfiles = PropertiesUtils.getResourceFile("profiles_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);



        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.CREATED.getStatusCode()).contentType("application/json");
    }

    @Test
    public void testfindProfiles() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminCLient)
            .findProfiles(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());
    }


    @Test
    public void testFindAgencies() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().addAllResults(getAgencies())).when(adminCLient)
            .findProfiles(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testCheckTraceabilityOperation() throws InvalidParseOperationException {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(request))
            .when()
            .post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testDownloadTraceabilityOperationFile() throws InvalidParseOperationException {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(TRACEABILITY_OPERATION_BASE_URI + TRACEABILITY_OPERATION_ID)
            .then().statusCode(Status.OK.getStatusCode());
    }


    private List<Object> getIngestContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    private List<Object> getAccessContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    private List<Object> getAgencies() throws FileNotFoundException, InvalidParseOperationException {
        List<Object> res = new ArrayList<>();
        return res;
    }


    @Test
    public void listResourceEndpoints()
        throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().options("/")
            .then().statusCode(Status.OK.getStatusCode())
            .body(CoreMatchers.containsString("formats:check"));
    }

    @Test
    public void insertSecurityProfile() throws FileNotFoundException, InvalidParseOperationException {

        AdminManagementClientFactory.changeMode(null);

        File securityProfileFile = PropertiesUtils.getResourceFile("security_profile_ok.json");
        JsonNode json = JsonHandler.getFromFile(securityProfileFile);

        // Test OK
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(SECURITY_PROFILES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(SECURITY_PROFILES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON).body(json)
            .when().post(SECURITY_PROFILES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindSecurityProfiles() throws InvalidCreateOperationException, FileNotFoundException {
        final Select select = new Select();
        String securityProfileIdentifier = "SEC_PROFILE-00001";
        select.setQuery(eq("Identifier", securityProfileIdentifier));
        AdminManagementClientFactory.changeMode(null);

        // Test OK with GET
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        // Test OK with POST and X-HTTP-Method-Override
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(SECURITY_PROFILES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindSecurityProfilesByIdentifier() throws InvalidCreateOperationException, FileNotFoundException {

        String securityProfileIdentifier = "SEC_PROFILE-00001";

        // Test OK
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.OK.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .when().get(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateSecurityProfiles()
        throws InvalidCreateOperationException, FileNotFoundException, InvalidParseOperationException {

        // Add permission
        String NewPermission = "new_permission:read";

        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "SEC_PROFILE_1"));
        final AddAction setActionAddPermission = UpdateActionHelper.add("Permissions", NewPermission);
        update.addActions(setActionAddPermission);
        updateParser.parse(update.getFinalUpdate());

        String securityProfileIdentifier = "SEC_PROFILE-00001";
        AdminManagementClientFactory.changeMode(null);

        // Test OK
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.OK.getStatusCode());

        // Test unknown tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        // Missing tenant Id
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .when().put(SECURITY_PROFILES_URI + "/" + securityProfileIdentifier)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }
    
    @Test
    public void testUpdateProfile() throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {
        String NewPermission = "new_permission:update:json";
        
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        final AddAction setActionAddPermission = UpdateActionHelper.add("Permissions", NewPermission);
        update.addActions(setActionAddPermission);
        AdminManagementClientFactory.changeMode(null);
        
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(PROFILE_URI + "/" +  GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());
    }
}
