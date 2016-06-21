package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.Response.Status;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class WebApplicationResourceTest {

    private static final int SERVER_PORT = 8111;
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm/api";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String OPTIONS = "{name: \"myName\"}";

    @BeforeClass
    public static void setup() throws Exception {
        ServerApplication.run(new WebApplicationConfig()
            .setPort(SERVER_PORT)
            .setDefaultContext(DEFAULT_WEB_APP_CONTEXT)
            .setVirtualHosts(new String[] {})
            .setStaticContent(DEFAULT_STATIC_CONTENT));
        RestAssured.port = SERVER_PORT;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT;
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
