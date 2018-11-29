package fr.gouv.vitam.ihmrecette.appserver;


import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.junit.JunitHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.File;

import static io.restassured.RestAssured.given;

public class ServerApplicationTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String IHM_RECETTE_CONF = "ihm-recette.conf";
    private static final String IHM_RECETTE_NO_PORT_CONF = "ihm-recette-noPort.conf";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"user\", \"credentials\": \"user\"}}";

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_RECETTE_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        newConf.delete();
    }

    @Test
    public void givenFileWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_RECETTE_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        final IhmRecetteMainWithoutMongo application = new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
        given().contentType(ContentType.JSON).body(CREDENTIALS)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .post("login");
        application.stop();
        newConf.delete();
        junitHelper.releasePort(port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        new IhmRecetteMainWithoutMongo((String) null);
    }

    @Test
    public void givenFileWithoutPortWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_NO_PORT_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test-noPort", IHM_RECETTE_NO_PORT_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        final IhmRecetteMainWithoutMongo application = new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
        given().contentType(ContentType.JSON).body(CREDENTIALS)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .post("login");
        application.stop();
        newConf.delete();
        junitHelper.releasePort(port);
    }
}
