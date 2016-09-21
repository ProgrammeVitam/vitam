package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.VitamServer;

public class ServerApplicationTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String IHM_DEMO_CONF = "ihm-demo.conf";
    private static final String IHM_DEMO_CONF_NO_PORT = "ihm-demo-test-noPort.conf";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"user\", \"credentials\": \"user\"}}";

    private static final ServerApplication application = new ServerApplication();

    @Test(expected = FileNotFoundException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test(expected = Exception.class)
    public void givenFileNotFoundWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        final JunitHelper junitHelper = new JunitHelper();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        config.setPort(port);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        application.configure(newConf.getAbsolutePath());
        newConf.delete();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenFileWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = new JunitHelper();
        final int port = junitHelper.findAvailablePort();
        SystemPropertyUtil.set(VitamServer.PARAMETER_JETTY_SERVER_PORT, Integer.toString(port));
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        config.setPort(port);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        application.configure(newConf.getAbsolutePath());
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
        given().contentType(ContentType.JSON).body(CREDENTIALS)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .post("login");
        ServerApplication.stop();
        newConf.delete();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenConfigFileNoPortAndStopWhenStartApplicationThenStopVitamServer() throws Exception {
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF_NO_PORT);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF_NO_PORT, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        application.configure(newConf.getAbsolutePath());
        ServerApplication.stop();
        newConf.delete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        application.configure(null);
    }

    @Test
    public void givenConfigFileFailedWhenConfigureApplicationThenRaiseAnException() throws Exception {
        application.configure("ihm-demo-test-noPort.conf");
    }

    @Test(expected = VitamApplicationServerException.class)
    public void givenConfigFileWithoutJettyConfigThenRaiseAnException() throws Exception {
        application.run(new WebApplicationConfig());
    }

    @Test(expected = VitamApplicationServerException.class)
    public void givenConfigFileWithoutConfigThenRaiseAnException() throws Exception {
        application.run(null);
    }
}
