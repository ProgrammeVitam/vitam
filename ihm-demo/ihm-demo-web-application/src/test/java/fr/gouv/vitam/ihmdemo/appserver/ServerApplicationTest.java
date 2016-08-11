package fr.gouv.vitam.ihmdemo.appserver;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.junit.JunitHelper;

public class ServerApplicationTest {

	private static final String IHM_DEMO_CONF = "ihm-demo.conf";
	private static final String IHM_DEMO_CONF_NO_PORT = "ihm-demo-test-noPort.conf";
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
		final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
		final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
		config.setPort(port);
		final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
		PropertiesUtils.writeYaml(newConf, config);
		application.configure(newConf.getAbsolutePath());
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




	@Test
	public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
		application.configure(null);
	}

	@Test
	public void givenConfigFileFailedWhenConfigureApplicationThenRaiseAnException() throws Exception {
		application.configure("src/test/resources/ihm-demo-test-noPort.conf");
	}


}
