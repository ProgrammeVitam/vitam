package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.mockito.Matchers.anyObject;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AdminManagementClientFactory.class})
public class AdminManagementExternalResourceImplTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResourceImplTest.class);

    private static final String RESOURCE_URI = "/admin-external/v1";

    private static final String FORMAT_URI = "/" + AdminCollections.FORMATS.getName();

    private static final String RULES_URI = "/" + AdminCollections.RULES.getName();

    private static final String DOCUMENT_ID = "/1";

    private static final String WRONG_URI = "/wrong-uri";


    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static AccessExternalApplication application;
    private static AdminManagementClient adminCLient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(serverPort));
            application =  new AccessExternalApplication("access-external-test.conf");
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
            application.getVitamServer().getServer() != null) {

            application.stop();
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public void deleteCollection() throws ReferentialException {
        AdminManagementClientFactory.changeMode(null);
        given()
        .when().delete(FORMAT_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .when().delete(RULES_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .when().delete(WRONG_URI)
        .then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void deleteCollectionError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        PowerMockito.doThrow(new ReferentialException("")).when(adminCLient).deleteFormat();

        given()
        .when().delete(FORMAT_URI)
        .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

    }

    @Test
    public void testCheckDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().put(FORMAT_URI)
        .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().put(RULES_URI)
        .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().put(WRONG_URI)
        .then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testCheckDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        PowerMockito.doThrow(new ReferentialException("")).when(adminCLient).checkFormat(anyObject());

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
        .when().post(FORMAT_URI)
        .then().statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(RULES_URI)
        .then().statusCode(Status.CREATED.getStatusCode());

        given().contentType(ContentType.BINARY)
        .when().post(WRONG_URI)
        .then().statusCode(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public void insertDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        PowerMockito.doThrow(new ReferentialException("")).when(adminCLient).importFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(FORMAT_URI)
        .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        PowerMockito.doThrow(new DatabaseConflictException("")).when(adminCLient).importFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
        .when().post(FORMAT_URI)
        .then().statusCode(Status.CONFLICT.getStatusCode());
        
    }

    @Test
    public void testGetDocuments() throws InvalidCreateOperationException, FileNotFoundException {
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));
        AdminManagementClientFactory.changeMode(null);

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(RULES_URI + DOCUMENT_ID)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(RULES_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI + DOCUMENT_ID)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI)
        .then().statusCode(Status.OK.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(WRONG_URI + DOCUMENT_ID)
        .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(WRONG_URI)
        .then().statusCode(Status.NOT_FOUND.getStatusCode());

    }
    
    @Test
    public void testGetDocumentsError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        PowerMockito.doThrow(new ReferentialException("")).when(adminCLient).getFormats(anyObject());
        PowerMockito.doThrow(new ReferentialException("")).when(adminCLient).getFormatByID(anyObject());
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI + DOCUMENT_ID)
        .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI)
        .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        

        PowerMockito.doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormats(anyObject());
        PowerMockito.doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormatByID(anyObject());
        
        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI + DOCUMENT_ID)
        .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
        .contentType(ContentType.JSON)
        .body(select.getFinalSelect())
        .when().post(FORMAT_URI)
        .then().statusCode(Status.BAD_REQUEST.getStatusCode());

    }


}
