package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.junit.JunitHelper;

public class WebApplicationResourceTest {

    private static final int SERVER_PORT = 8111;
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm/api";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String OPTIONS = "{name: \"myName\"}";
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        ServerApplication.run(new WebApplicationConfig()
            .setPort(port)
            .setDefaultContext(DEFAULT_WEB_APP_CONTEXT)
            .setVirtualHosts(new String[] {})
            .setStaticContent(DEFAULT_STATIC_CONTENT));
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(port);
    }

    @Test
    public void givenEmptyPayloadWhenSearchOperationsThenReturnBadRequest() {
        given().contentType(ContentType.JSON).body("{}").expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when().post("/logbook/operations");
    }

    @Ignore
    @Test
    public void givenNoOperationsWhenSearchOperationsThenReturnNotFound() {
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when().post("/logbook/operations");
    }

    @Ignore
    @Test
    public void givenNoOperationsWhenSearchOperationsWithIdThenReturnNotFound() {
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when().post("/logbook/operations/1");
    }

    @Test
    public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() {
        given().contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when().post("/archivesearch/units");
    }

}
