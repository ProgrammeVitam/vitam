package fr.gouv.vitam.common.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.eclipse.jetty.server.Handler;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

public class AbstractVitamApplicationTest {

    private static final String TEST_CONF_CONF = "test-conf.conf";

    private static class TestVitamApplication
        extends AbstractVitamApplication<TestVitamApplication, DbConfigurationImpl> {

        String conf = TEST_CONF_CONF;

        protected TestVitamApplication(Class<TestVitamApplication> applicationType,
            Class<DbConfigurationImpl> configurationType) {
            super(applicationType, configurationType);
        }

        @Override
        protected Handler buildApplicationHandler() {
            return null;
        }

        @Override
        protected String getConfigFilename() {
            return conf;
        }

    }

    @Test
    public final void testBuild() throws VitamApplicationServerException, FileNotFoundException {
        final TestVitamApplication testVitamApplication =
            new TestVitamApplication(TestVitamApplication.class, DbConfigurationImpl.class);
        final String filename1 = testVitamApplication.getConfigFilename();
        assertTrue(filename1.equals(TEST_CONF_CONF));
        final Path path0 = testVitamApplication.computeConfigurationPathFromInputArguments();
        final Path path1 = testVitamApplication.computeConfigurationPathFromInputArguments(
            PropertiesUtils.getResourcesFile(filename1).getAbsolutePath());
        System.out.println(path0);
        System.out.println(path1);
        assertTrue(path0.equals(path1));
        final TestVitamApplication testVitamApplication2 = testVitamApplication.configure(path0);
        assertNotNull(testVitamApplication2);
        assertNull(testVitamApplication.getApplicationHandler());
        final DbConfigurationImpl configuration = testVitamApplication.getConfiguration();
        assertNotNull(configuration);
        assertEquals(45678, configuration.getDbPort());
        assertEquals("localhost", configuration.getDbHost());
        assertEquals("Vitam-test", configuration.getDbName());
        testVitamApplication.conf = "fake-file-not-exist.conf";
        try {
            testVitamApplication.computeConfigurationPathFromInputArguments();
            fail("Should raized an exception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
    }

}
