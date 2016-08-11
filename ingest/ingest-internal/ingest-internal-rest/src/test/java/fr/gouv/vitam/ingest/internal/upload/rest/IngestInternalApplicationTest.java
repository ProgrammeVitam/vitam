package fr.gouv.vitam.ingest.internal.upload.rest;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;

public class IngestInternalApplicationTest {

	private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";

	private static final String INGEST_INTERNAL_CONF = "ingest-internal.conf";

	private static int serverPort;
	private static JunitHelper junitHelper;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		junitHelper = new JunitHelper();
		serverPort = junitHelper.findAvailablePort();
		VitamServerFactory.setDefaultPort(serverPort);
	}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(serverPort);
    }

	@Test
	public final void givenNegativeNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
					PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), "-1"
			})).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}
	}
	    
	@Test
	public final void givenIncorrectPortNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
					PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), "-1xx"
			})).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}

	}

	@Test
	public final void givenFileNotFoundWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
				"src/test/resources/notFound.conf"})).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}
	}

	@Test
	public final void givenCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
				"src/test/resources/ingest-internal.conf"})).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}
	}


	@Test(expected = IllegalStateException.class)
	public final void givenInCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
				"src/test/resources/ingest-internal-err1.conf"})).stop();
		} catch (VitamApplicationServerException e) {
			fail("Exception");
		}
	}

	/*@Test(expected = IllegalStateException.class)
	public final void givenNotConfigFileWhenStartApplicationThenStartOnDefaultPort() {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
				"src/test/resources/ingest-internal-err2.conf"})).stop();
		} catch (VitamApplicationServerException e) {
			fail("Exception");
		}
	}*/


	@Test
	public final void givenCorrectPortNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
					PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), Integer.toString(serverPort)
			})).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}
	}

	@Test
	public final void givenNullParamWhenStartApplicationThenNotRaiseException()throws FileNotFoundException {
		try {
			((BasicVitamServer) IngestInternalApplication.startApplication(new String[0])).stop();
		} catch (final IllegalStateException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		} catch (final VitamApplicationServerException e) {
			fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
		}

	}
}
