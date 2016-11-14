package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.junit.JunitHelper;

public class ServerApplicationTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String IHM_DEMO_CONF = "ihm-demo.conf";
    private static final String IHM_DEMO_CONF_NO_PORT = "ihm-demo-test-noPort.conf";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"user\", \"credentials\": \"user\"}}";

    @Test(expected = IllegalStateException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        new ServerApplication("src/test/resources/notFound.conf");
    }

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        config.setPort(port);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        new ServerApplication(newConf.getAbsolutePath());
        newConf.delete();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenFileWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        config.setPort(port);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        ServerApplication application = new ServerApplication(newConf.getAbsolutePath());
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

    @Test
    public void givenConfigFileNoPortAndStopWhenStartApplicationThenStopVitamServer() throws Exception {
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF_NO_PORT);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF_NO_PORT, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        new ServerApplication(newConf.getAbsolutePath());
        newConf.delete();
    }

    @Test(expected = IllegalStateException.class)
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        new ServerApplication((String) null);
    }

    @Test
    public void givenConfigFileFailedWhenConfigureApplicationThenRaiseAnException() throws Exception {
        new ServerApplication("ihm-demo-test-noPort.conf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenConfigFileWithoutJettyConfigThenRaiseAnException() throws Exception {
        new ServerApplication(new WebApplicationConfig());
    }

    @Test(expected = IllegalStateException.class)
    public void givenConfigFileWithoutConfigThenRaiseAnException() throws Exception {
        new ServerApplication((WebApplicationConfig) null);
    }
}
