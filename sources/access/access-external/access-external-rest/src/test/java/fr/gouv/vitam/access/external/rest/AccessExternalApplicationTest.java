package fr.gouv.vitam.access.external.rest;

import fr.gouv.vitam.common.logging.SysErrLogger;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;

public class AccessExternalApplicationTest {
    private AccessExternalMain application;
    private final JunitHelper junitHelper = JunitHelper.getInstance();
    private int portAvailable;

    @Before
    public void setUpBeforeMethod() throws Exception {
        portAvailable = junitHelper.findAvailablePort();
        RestAssured.port = portAvailable;
        RestAssured.basePath = "access-external/v1";
    }

    @After
    public void tearDown() throws Exception {
       try {
           if (application != null && application.getVitamServer() != null &&
               application.getVitamServer() != null) {

               application.stop();
           }
       } catch (Exception e) {
           SysErrLogger.FAKE_LOGGER.syserr("", e);
       }

        junitHelper.releasePort(portAvailable);
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application = new AccessExternalMain("");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application = new AccessExternalMain("notFound.conf");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenConfigureApplicationWithFileErr1() throws Exception {
        application = new AccessExternalMain("access-external-test-err1.conf");
        Assert.assertFalse(application.getVitamServer().isStarted());
    }

    @Test
    public void shouldStartAndStopServerWhenStopApplicationWithFileExistsAndRun() throws Exception {
        application = new AccessExternalMain("access-external-test.conf");
        application.start();
        Assert.assertTrue(application.getVitamServer().isStarted());

        application.stop();
    }

    @Test
    public void shouldHeaderStripXSSWhenFilterThenReturnReturnNotAcceptable() throws VitamException {
        application = new AccessExternalMain("src/test/resources/access-external-test.conf", BusinessApplicationTest.class,null);
        application.start();

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .header("test", "<script>(.*?)</script>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // without X-Tenant-Id --> Precondition Failed
        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        // Incorrect Tenant Id --> UNAUTHORIZED
        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .header(GlobalDataRest.X_TENANT_ID, "7")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.UNAUTHORIZED.getStatusCode());

    }

    @Test
    public void shouldActivateShiroFilter() throws VitamException {
        application = new AccessExternalMain("src/test/resources/access-external-test-ssl.conf");
        application.start();
    }
}
