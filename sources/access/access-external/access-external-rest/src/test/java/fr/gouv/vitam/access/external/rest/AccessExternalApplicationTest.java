package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;

public class AccessExternalApplicationTest {
    private AccessExternalApplication application;
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
        if (application != null && application.getVitamServer() != null &&
            application.getVitamServer().getServer() != null) {

            application.stop();
        }
        junitHelper.releasePort(portAvailable);
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application = new AccessExternalApplication("");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application = new AccessExternalApplication("notFound.conf");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenConfigureApplicationWithFileErr1() throws Exception {
        application = new AccessExternalApplication("access-external-test-err1.conf");
        Assert.assertFalse(application.getVitamServer().getServer().isStarted());
    }

    @Test
    public void shouldStartAndStopServerWhenStopApplicationWithFileExistsAndRun() throws Exception {
        application = new AccessExternalApplication("access-external-test.conf");
        application.start();
        Assert.assertTrue(application.getVitamServer().getServer().isStarted());

        application.stop();
        Assert.assertTrue(application.getVitamServer().getServer().isStopped());
    }

    @Test
    public void shouldHeaderStripXSSWhenFilterThenReturnReturnNotAcceptable() throws VitamException {
        application = new AccessExternalApplication("src/test/resources/access-external-test.conf");
        application.start();
        
        given()
            .contentType(ContentType.JSON)
            .header("test", "<script>(.*?)</script>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .param("test", "<?php echo\" Hello \" ?>")
            .body("{\"name\":\"123\"}")
            .when()
            .put("/units/1")
            .then()
            .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());

    }

    @Test
    public void shouldActivateShiroFilter() throws VitamException {
        application = new AccessExternalApplication("src/test/resources/access-external-test-ssl.conf");
        application.start();
    }
}
