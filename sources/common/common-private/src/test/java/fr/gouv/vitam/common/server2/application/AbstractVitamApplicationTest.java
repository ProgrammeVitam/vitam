/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.server2.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.benchmark.BenchmarkConfiguration;

public class AbstractVitamApplicationTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractVitamApplicationTest.class);
    
    private static final String TEST_CONF_CONF = "benchmark-test.conf";

    private static class TestVitamApplication
        extends AbstractVitamApplication<TestVitamApplication, BenchmarkConfiguration> {

        protected TestVitamApplication(Class<BenchmarkConfiguration> configurationType, String config) {
            super(configurationType, config);
        }

        protected TestVitamApplication(Class<BenchmarkConfiguration> configurationType, BenchmarkConfiguration config) {
            super(configurationType, config);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            // Do nothing
        }
    }

    @Test
    public final void testBuild() throws VitamApplicationServerException, FileNotFoundException {
        TestVitamApplication testVitamApplication =
            new TestVitamApplication(BenchmarkConfiguration.class, TEST_CONF_CONF);
        final String filename1 = testVitamApplication.getConfigFilename();
        assertTrue(filename1.equals(TEST_CONF_CONF));
        assertNotNull(testVitamApplication.getApplicationHandler());
        final BenchmarkConfiguration configuration = testVitamApplication.getConfiguration();
        assertNotNull(configuration);
        assertEquals("jetty-config-benchmark-test.xml", configuration.getJettyConfig());
        testVitamApplication =
            new TestVitamApplication(BenchmarkConfiguration.class, configuration);
        final String filename2 = testVitamApplication.getConfigFilename();
        assertNull(filename2);
        assertNotNull(testVitamApplication.getApplicationHandler());
        final BenchmarkConfiguration configuration2 = testVitamApplication.getConfiguration();
        assertEquals(configuration, configuration2);
        assertEquals("jetty-config-benchmark-test.xml", configuration2.getJettyConfig());

        final File file = PropertiesUtils.getResourceFile(TEST_CONF_CONF);
        testVitamApplication =
            new TestVitamApplication(BenchmarkConfiguration.class, file.getAbsolutePath());
        final String filename3 = testVitamApplication.getConfigFilename();
        assertTrue(filename3.equals(file.getAbsolutePath()));

        try {
            testVitamApplication =
                new TestVitamApplication(BenchmarkConfiguration.class, (String) null);
            fail("should raized an exception");
        } catch (final IllegalStateException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        try {
            testVitamApplication =
                new TestVitamApplication(BenchmarkConfiguration.class, (BenchmarkConfiguration) null);
            fail("should raized an exception");
        } catch (final IllegalStateException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Test
    public void testMultipleServerSamePort() {
        int port = JunitHelper.getInstance().findAvailablePort();
        TestVitamApplication testVitamApplication =
            new TestVitamApplication(BenchmarkConfiguration.class, TEST_CONF_CONF);
        LOGGER.warn("Port: " + testVitamApplication.getVitamServer().getPort());
        assertFalse(testVitamApplication.getVitamServer().isStarted());
        try {
            testVitamApplication.start();
        } catch (VitamApplicationServerException e) {
            fail("should not");
        }
        LOGGER.warn("Port: " + testVitamApplication.getVitamServer().getPort());
        assertTrue(testVitamApplication.getVitamServer().isStarted());
        // Start second application
        TestVitamApplication testVitamApplication2 =
            new TestVitamApplication(BenchmarkConfiguration.class, TEST_CONF_CONF);
        LOGGER.warn("Port: " + testVitamApplication2.getVitamServer().getPort());
        assertFalse(testVitamApplication2.getVitamServer().isStarted());
        try {
            testVitamApplication2.start();
            fail("should not");
        } catch (VitamApplicationServerException e) {
            LOGGER.warn("Port: " + testVitamApplication2.getVitamServer().getPort() + " but ", e);
            assertFalse(testVitamApplication2.getVitamServer().isStarted());
        }
        try {
            testVitamApplication.stop();
        } catch (VitamApplicationServerException e) {
            fail("should not");
        }
        LOGGER.warn("Port: " + testVitamApplication.getVitamServer().getPort());
        assertFalse(testVitamApplication.getVitamServer().isStarted());
        try {
            testVitamApplication2.start();
        } catch (VitamApplicationServerException e) {
            fail("should not");
        }
        LOGGER.warn("Port: " + testVitamApplication2.getVitamServer().getPort());
        assertTrue(testVitamApplication2.getVitamServer().isStarted());
        try {
            testVitamApplication2.stop();
        } catch (VitamApplicationServerException e) {
            fail("should not");
        }
        JunitHelper.getInstance().releasePort(port);
    }
}
