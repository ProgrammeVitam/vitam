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

package fr.gouv.vitam.common.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.TestApplication;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;

public class VitamInstrumentedResourceMethodApplicationListenerTest {
    private static final String TEST_CONF = "test.conf";
    private static int serverPort;
    private static TestVitamApplication application;

    /**
     * The test application that will load a test resource.
     */
    public static class TestVitamApplication extends TestApplication {

        public TestVitamApplication(String configFile) {
            super(configFile);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.register(SimpleJerseyMetricsResource.class);
            resourceConfig.register(AdvancedJerseyMetricsResource.class);
            resourceConfig.register(ShouldNotWorkJerseyMetricsResource.class);
            resourceConfig.register(MediaTypeJerseyMetricsResource.class);
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MinimalTestVitamApplicationFactory<TestVitamApplication> testFactory =
            new MinimalTestVitamApplicationFactory<TestVitamApplication>() {

                @Override
                public StartApplicationResponse<TestVitamApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    final TestVitamApplication application = new TestVitamApplication(TEST_CONF);
                    return startAndReturn(application);
                }

            };
        final StartApplicationResponse<TestVitamApplication> response =
            testFactory.findAvailablePortSetToApplication();

        serverPort = response.getServerPort();
        application = response.getApplication();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            e.printStackTrace();
        }
        JunitHelper.getInstance().releasePort(serverPort);
    }

    private Set<String> formatMetricsNames(final Set<String> names) {
        final Set<String> formattedNames = new HashSet<>();

        for (final String name : names) {
            formattedNames.addAll(Arrays.asList(
                VitamInstrumentedResourceMethodApplicationListener.metricMeterName(name),
                VitamInstrumentedResourceMethodApplicationListener.metricTimerName(name),
                VitamInstrumentedResourceMethodApplicationListener.metricExceptionMeterName(name)));
        }

        return formattedNames;
    }

    @Test
    public void testSimpleJerseyMetricsResource() {
        final Set<String> formattedExpectedNames = formatMetricsNames(SimpleJerseyMetricsResource.expectedNames);
        final VitamMetrics metrics = AbstractVitamApplication.getVitamMetrics(VitamMetricsType.JERSEY);

        assertNotNull("VitamMetrics", metrics);
        assertTrue("SimpleJerseyMetricsResource",
            metrics.getRegistry().getMetrics().keySet().containsAll(formattedExpectedNames));
    }

    @Test
    public void testAdvancedJerseyMetricsResource() {
        final Set<String> formattedExpectedNames = formatMetricsNames(AdvancedJerseyMetricsResource.expectedNames);
        final VitamMetrics metrics = AbstractVitamApplication.getVitamMetrics(VitamMetricsType.JERSEY);

        assertNotNull("VitamMetrics", metrics);
        assertTrue("AdvancedJerseyMetricsResource",
            metrics.getRegistry().getMetrics().keySet().containsAll(formattedExpectedNames));
    }

    @Test
    public void testMediaTypeJerseyMetricsResource() {
        final Set<String> formattedExpectedNames = formatMetricsNames(MediaTypeJerseyMetricsResource.expectedNames);
        final VitamMetrics metrics = AbstractVitamApplication.getVitamMetrics(VitamMetricsType.JERSEY);

        assertNotNull("VitamMetrics", metrics);
        assertTrue("MediaTypeJerseyMetricsResource",
            metrics.getRegistry().getMetrics().keySet().containsAll(formattedExpectedNames));
    }

    @Test
    public void testRegistrySize() {
        final int expectedSize =
            SimpleJerseyMetricsResource.expectedNames.size() +
                AdvancedJerseyMetricsResource.expectedNames.size() +
                ShouldNotWorkJerseyMetricsResource.expectedNames.size() +
                MediaTypeJerseyMetricsResource.expectedNames.size(); // Default in test
        final VitamMetrics metrics = AbstractVitamApplication.getVitamMetrics(VitamMetricsType.JERSEY);

        assertNotNull("VitamMetrics", metrics);

        // Multiply the expectedSize by 3 because we expect a timer, a meter and an exceptionMeter per name.
        assertEquals("MetricRegistry size", expectedSize * 3, metrics.getRegistry().getMetrics().size());
    }
}
