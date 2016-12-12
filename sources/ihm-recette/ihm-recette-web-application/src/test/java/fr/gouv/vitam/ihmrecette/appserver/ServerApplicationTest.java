package fr.gouv.vitam.ihmrecette.appserver;


import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.util.ArrayList;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

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
        new ServerApplicationWithoutMongo(newConf.getAbsolutePath());
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
        final ServerApplicationWithoutMongo application = new ServerApplicationWithoutMongo(newConf.getAbsolutePath());
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

    @Test(expected = IllegalStateException.class)
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        new ServerApplication((String) null);
    }

    @Test
    public void givenFileWithoutPortWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_NO_PORT_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test-noPort", IHM_RECETTE_NO_PORT_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        final ServerApplicationWithoutMongo application = new ServerApplicationWithoutMongo(newConf.getAbsolutePath());
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
    public void givenConfigFileWithoutJettyConfigThenRaiseAnException() throws Exception {
        WebApplicationConfig config = new WebApplicationConfig();
        config.setMongoDbNodes(new ArrayList<MongoDbNode>());
        new ServerApplication(config);
    }

    @Test(expected = IllegalStateException.class)
    public void givenConfigFileWithoutConfigThenRaiseAnException() throws Exception {
        new ServerApplication((WebApplicationConfig) null);
    }
}
