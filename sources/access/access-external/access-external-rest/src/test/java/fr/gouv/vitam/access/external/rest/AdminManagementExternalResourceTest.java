package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.mockito.Matchers.anyBoolean;
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
import java.util.stream.IntStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamClientFactory;
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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.Contexts;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AdminManagementClientFactory.class, IngestInternalClientFactory.class})
public class AdminManagementExternalResourceTest {

    private static final String CODE_VALIDATION_DSL = VitamCodeHelper.getCode(VitamCode.GLOBAL_INVALID_DSL);

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(AdminManagementExternalResourceTest.class);

    private static final String RESOURCE_URI = "/admin-external/v1";

    private static final String FORMAT_URI = "/" + AdminCollections.FORMATS.getName();
    private static final String FORMAT_CHECK_URI = "/" + AdminCollections.FORMATS.getCheckURI();

    private static final String RULES_URI = "/" + AdminCollections.RULES.getName();
    private static final String RULES_CHECK_URI = "/" + AdminCollections.RULES.getCheckURI();

    private static final String AGENCIES_URI = "/" + AdminCollections.AGENCIES.getName();
    private static final String AGENCIES_CHECK_URI = "/" + AdminCollections.AGENCIES.getCheckURI();


    private static final String DOCUMENT_ID = "/1";

    private static final String RULE_ID = "/APP-00001";

    private static final String AGENCY_ID = "/AG-000001";


    private static final String WRONG_URI = "/wrong-uri";

    private static final String TENANT_ID = "0";

    private static final String UNEXISTING_TENANT_ID = "25";
    private static final String CONTEXT_URI = "/contexts";
    private static final String PROFILE_URI = "/profiles";
    private static final String ARCHIVE_UNIT_PROFILE_URI = "/archiveunitprofiles";
    private static final String ONTOLOGIES_URI = "/ontologies";
    private static final String AGENCY_URI = "/agencies";
    private static final String CONTRACT_ID = "NAME";

    private static final String TRACEABILITY_OPERATION_ID = "op_id";
    private static final String TRACEABILITY_OPERATION_BASE_URI = AccessExtAPI.TRACEABILITY_API + "/";
    private static final String ACCESSION_REGISTER_DETAIL_URI = AccessExtAPI.ACCESSION_REGISTERS_API +
        "/FR_ORG_AGEC/" +
        AccessExtAPI.ACCESSION_REGISTERS_DETAIL;
    private static final String CHECK_TRACEABILITY_OPERATION_URI = AccessExtAPI.TRACEABILITY_API + "checks";


    private static final String GOOD_ID = "goodId";
    public static final String SECURITY_PROFILES_URI = "/securityprofiles";
    private static final String RULE_FILE = "jeu_donnees_OK_regles_CSV_regles.csv";


    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static AccessExternalMain application;
    private static AdminManagementClient adminClient;
    private static IngestInternalClient ingestInternalClient;

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
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void testCheckDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream(RULE_FILE);
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_CHECK_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_CHECK_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(RULES_CHECK_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AGENCIES_CHECK_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(AGENCIES_CHECK_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(AGENCIES_CHECK_URI)
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
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doThrow(new ReferentialException("Referential Exception")).when(adminClient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(FORMAT_CHECK_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void insertDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        int tenantaDMIN = VitamConfiguration.getAdminTenant();
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

        stream = PropertiesUtils.getResourceAsStream(RULE_FILE);
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
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doThrow(new ReferentialException("")).when(adminClient).importFormat(anyObject(), anyObject());
        doReturn(Response.ok().build()).when(adminClient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new DatabaseConflictException("")).when(adminClient).importFormat(anyObject(), anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.CONFLICT.getStatusCode());

    }

    @Test
    public void testFindRules() throws InvalidCreateOperationException, FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);

        final Select select = new Select();
        select.setQuery(eq("Identifier", "APP-00001"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Identifier", "APP-00001"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void testFindFormats() throws InvalidCreateOperationException, FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);

        final Select select = new Select();
        select.setQuery(eq("PUID", "x-fmt/348"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("PUID", "x-fmt/348"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void testFindAccessionRegister() throws InvalidCreateOperationException, FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);

        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(AccessExtAPI.ACCESSION_REGISTERS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testFindAccessionRegisterDetail() throws InvalidCreateOperationException, FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);

        final Select select = new Select();
        select.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("OriginatingAgency", "RATP"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(ACCESSION_REGISTER_DETAIL_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
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
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI + DOCUMENT_ID)
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
            .header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .body(select.getFinalSelect())
            .when().post(AccessExtAPI.ACCESSION_REGISTERS_API + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void testGetDocumentsError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doThrow(new ReferentialException("")).when(adminClient).getFormats(anyObject());
        doThrow(new ReferentialException("")).when(adminClient).getFormatByID(anyObject());
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


        doThrow(new InvalidParseOperationException("")).when(adminClient).getFormats(anyObject());
        doThrow(new InvalidParseOperationException("")).when(adminClient).getFormatByID(anyObject());

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
        when(adminClient.getAccessionRegister(anyObject())).thenReturn(rsp);
        when(adminClient.getAccessionRegisterDetail(anyObject(), anyObject())).thenReturn(rsp);

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
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
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

        doThrow(new InvalidParseOperationException("")).when(adminClient)
            .getAccessionRegister(anyObject());
        doThrow(new InvalidParseOperationException("")).when(adminClient)
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

        doThrow(new IllegalArgumentException("")).when(adminClient)
            .getAccessionRegister(anyObject());
        doThrow(new IllegalArgumentException("")).when(adminClient)
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
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.BAD_REQUEST).when(adminClient).importIngestContracts(anyObject());

        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }

    @Test
    public void testImportCSVDocumentWithHTMLContent() throws Exception {
        stream = PropertiesUtils.getResourceAsStream("CSV_HTML.csv");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "CSV_HTML.csv")
            .when().post(AGENCIES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("CSV_HTML.csv");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_FILENAME, "CSV_HTML.csv")
            .when().post(RULES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testImportJSONDocumentWithHTMLContent() throws Exception {
        File file = PropertiesUtils.getResourceFile("JSON_HTML.json");
        JsonNode json = JsonHandler.getFromFile(file);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testimportValidIngestContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.CREATED).when(adminClient).importIngestContracts(anyObject());
        File contractFile = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        JsonNode json = JsonHandler.getFromFile(contractFile);
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindIngestContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);

        doReturn(new RequestResponseOK<>().addAllResults(getIngestContracts())).when(adminClient)
            .findIngestContracts(anyObject());

        Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));

        given().contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());

        Select emptySelect = new Select();
        given().contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());

        SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Status", "ACTIVE"));
        given().contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(AccessExtAPI.INGEST_CONTRACT_API)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

    }


    @Test
    public void testImportAccessContractsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.BAD_REQUEST).when(adminClient).importAccessContracts(anyObject());

        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }

    @Test
    public void testimportValidAccessContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.CREATED).when(adminClient).importAccessContracts(anyObject());
        File contractFile = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        JsonNode json = JsonHandler.getFromFile(contractFile);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindAccessContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminClient)
            .findAccessContracts(anyObject());

        Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));

        given().contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());

        Select emptySelect = new Select();
        given().contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());

        SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Status", "ACTIVE"));
        given().contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given().contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());
    }



    @Test
    public void testCreateProfileWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode())).when(adminClient)
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
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).when(adminClient)
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
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminClient)
            .findProfiles(anyObject());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post(PROFILE_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }


    @Test
    public void testFindAgencies() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<AgenciesModel>().addAllResults(getAgencies()).toJsonNode()).when(adminClient)
            .getAgencies(anyObject());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(AGENCY_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(AGENCY_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void testFindAgenciesById() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<AgenciesModel>().addResult(getAgencies().get(0))).when(adminClient)
            .getAgencyById(anyObject());

        given()
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        doThrow(new ReferentialNotFoundException("Agency not found")).when(adminClient).getAgencyById(anyObject());
        given()
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        doThrow(new AdminManagementClientServerException("Exception")).when(adminClient).getAgencyById(anyObject());
        given()
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        doThrow(new InvalidParseOperationException("Exception")).when(adminClient).getAgencyById(anyObject());
        given()
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AGENCY_URI + "/id")
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

    }

    @Test
    public void testCheckTraceabilityOperation()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        // given()
        // .contentType(ContentType.JSON)
        // .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
        // .body(JsonHandler.getFromString(request))
        // .when()
        // .post(CHECK_TRACEABILITY_OPERATION_URI)
        // .then().statusCode(Status.OK.getStatusCode());



        final Select select = new Select();
        select.setQuery(eq("evType", "TRACEABILITY"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("evType", "TRACEABILITY"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(CHECK_TRACEABILITY_OPERATION_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());


    }

    @Test
    public void testDownloadTraceabilityOperationFile() throws InvalidParseOperationException {
        given().accept(MediaType.APPLICATION_OCTET_STREAM)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get(TRACEABILITY_OPERATION_BASE_URI + TRACEABILITY_OPERATION_ID + "/datafiles")
            .then().statusCode(Status.OK.getStatusCode());
    }


    private List<Object> getIngestContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }

    private List<Object> getContexts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContexts = PropertiesUtils.getResourceAsStream("context.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContexts);
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

    private List<AgenciesModel> getAgencies() throws FileNotFoundException, InvalidParseOperationException {
        List<AgenciesModel> res = new ArrayList<>();
        IntStream.range(1, 5).forEach(i -> {
            AgenciesModel agenciesModel = new AgenciesModel();
            agenciesModel.setIdentifier("AG-00000" + i);
            agenciesModel.setName("aName" + i);
            agenciesModel.setDescription("aDescription" + i);
            res.add(agenciesModel);
        });

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
            .body(CoreMatchers.containsString("formatsfile:check"));
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


        // Test empty query
        final Select emptySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptySelect.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        // Test multi query
        final SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.setQuery(eq("Identifier", securityProfileIdentifier));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMulti.getFinalSelect())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        // Test no query
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(SECURITY_PROFILES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

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
        final SetAction setActionPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionPermission);
        updateParser.parse(update.getFinalUpdate());

        String securityProfileIdentifier = "SEC_PROFILE-00001";
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(SECURITY_PROFILES_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

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
    public void testUpdateProfile()
        throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {
        String NewPermission = "new_permission:update:json";

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        final SetAction setActionAddPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionAddPermission);
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when().put(PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateContext()
        throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(CONTEXT_URI + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(CONTEXT_URI + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(CONTEXT_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when().put(CONTEXT_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateAccessContract()
        throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when().put(AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testUpdateIngestContract()
        throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "CT-000001"));
        final SetAction setActionDescription = UpdateActionHelper.set("Name", "admin-context");
        update.addActions(setActionDescription);
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when().put(AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void listOperations()
        throws Exception {

        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when().get("operations")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void cancelOperationTest()
        throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().delete("operations/1")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getWorkFlowExecutionStatusTest()
        throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().head("operations/1")
            .then().statusCode(Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void getWorkFlowStatusTest()
        throws Exception {
        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when().get("operations/1")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getWorkFlowNotFoundTest()
        throws Exception {
        PowerMockito.mockStatic(IngestInternalClientFactory.class);
        ingestInternalClient = PowerMockito.mock(IngestInternalClient.class);
        final IngestInternalClientFactory ingestClientFactory = PowerMockito.mock(IngestInternalClientFactory.class);
        when(IngestInternalClientFactory.getInstance()).thenReturn(ingestClientFactory);
        when(IngestInternalClientFactory.getInstance().getClient()).thenReturn(ingestInternalClient);
        doThrow(new WorkflowNotFoundException("WorkflowNotFoundException")).when(ingestInternalClient)
            .getOperationProcessExecutionDetails(anyObject());
        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when().get("operations/1")
            .then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void getWorkflowDefinitionsTest()
        throws Exception {
        RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get("workflows/")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void updateWorkFlowStatusTest()
        throws Exception {
        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_ACTION, ProcessAction.PAUSE.getValue())
            .body(new ProcessQuery())
            .when().put("operations/1")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void updateWorkFlowStatusWithoutHeadersTest()
        throws Exception {
        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new ProcessQuery())
            .when().put("operations/1")
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testimportValidContextFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.CREATED).when(adminClient).importContexts(anyObject());
        File contextFile = PropertiesUtils.getResourceFile("context.json");
        JsonNode json = JsonHandler.getFromFile(contextFile);
        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindContextFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);

        doReturn(new RequestResponseOK<>().addAllResults(getContexts())).when(adminClient)
            .findContexts(anyObject());

        final Select select = new Select();
        select.setQuery(eq("STATUS", true));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("STATUS", true));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }


    @Test
    public void testImportContextsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(Status.BAD_REQUEST).when(adminClient).importContexts(anyObject());

        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

        doThrow(new ReferentialException("ReferentialException"))
            .when(adminClient).importContexts(anyObject());

        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.CONTEXTS_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }

    @Test
    public void downloadIngestReportsAsStream()
        throws Exception {
        RestAssured.given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_CONTEXT_ID, Contexts.DEFAULT_WORKFLOW)
            .when().get(AccessExtAPI.RULES_REPORT_API + "/id")
            .then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testCreateArchiveUnitProfileWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode())).when(adminClient)
            .createArchiveUnitProfiles(anyObject());

        File fileArchiveUnitProfiles = PropertiesUtils.getResourceFile("AUP_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileArchiveUnitProfiles);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");
    }


    @Test
    public void testCreateValidArchiveUnitProfileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).when(adminClient)
            .createArchiveUnitProfiles(anyObject());

        File fileArchiveUnitProfiles = PropertiesUtils.getResourceFile("archive_unit_profiles_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileArchiveUnitProfiles);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.CREATED.getStatusCode()).contentType("application/json");
    }

    @Test
    public void testFindArchiveUnitProfiles() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminClient)
            .findArchiveUnitProfiles(anyObject());

        final Select select = new Select();
        select.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final Select emptyQuerySelect = new Select();
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(emptyQuerySelect.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());

        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        selectMultiple.setQuery(eq("Status", "ACTIVE"));
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(selectMultiple.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode())
            .body(CoreMatchers.containsString(CODE_VALIDATION_DSL));

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .and().header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(X_HTTP_METHOD_OVERRIDE, "GET")
            .when().post(ARCHIVE_UNIT_PROFILE_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }
    
    
    
    @Test
    public void testUpdateArchiveUnitProfile()
        throws InvalidCreateOperationException, InvalidParseOperationException, AdminManagementClientServerException {
        String NewPermission = "new_permission:update:json";

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "aName"));
        final SetAction setActionAddPermission = UpdateActionHelper.set("Permissions", NewPermission);
        update.addActions(setActionAddPermission);
        AdminManagementClientFactory.changeMode(null);

        // valid query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.OK.getStatusCode());

        // wrong query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        // no query
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .and().header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // no tenant
        given()
            .contentType(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdateById())
            .when().put(ARCHIVE_UNIT_PROFILE_URI + "/" + GOOD_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void should_respond_no_content_when_status() {
        given()
            .accept(ContentType.JSON)
            .when()
            .get("/status")
            .then()
            .statusCode(Status.NO_CONTENT.getStatusCode());
    }


    @Test
    public void testImportValidOntologyReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).when(adminClient)
            .importOntologies(anyBoolean(), anyObject());

        File fileOntologies = PropertiesUtils.getResourceFile("ontologies_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileOntologies);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ONTOLOGIES_URI)
            .then().statusCode(Status.CREATED.getStatusCode()).contentType("application/json");
    }

    @Test
    public void testImportOntologyWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode())).when(adminClient)
            .importOntologies(anyBoolean(), anyObject());

        File fileOntologies = PropertiesUtils.getResourceFile("ontologies_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileOntologies);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(ONTOLOGIES_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");
    }

}
