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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.security.filter.AuthorizationFilterHelper;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server2.application.resources.BasicVitamStatusServiceImpl;

public class VitamMetricsConfigurationImplTest {
    private static final String BASE_PATH = "/";
    private static final String TEST_RESOURCE_URI = "/home";
    private static final String TEST_GAUGE_NAME = "Test gauge";
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
            resourceConfig.register(new TestResourceImpl());
        }
    }

    /**
     * TestResourceImpl implements ApplicationStatusResource
     */
    @Path(TEST_RESOURCE_URI)
    public static class TestResourceImpl extends ApplicationStatusResource {

        private int counter = 0;
        // Get the business metric registry
        final private VitamMetricRegistry registry = AbstractVitamApplication.getBusinessVitamMetrics().getRegistry();

        /**
         *
         * @param configuration to associate with TestResourceImpl
         */
        public TestResourceImpl() {
            super(new BasicVitamStatusServiceImpl());
        }

        /**
         * This function registers a gauge metric that monitors the value of the private field counter
         */
        @GET
        public Response simpleGET() {
            // increment the counter each time this function is called
            counter++;
            // register the gauge if it doesn't exist
            registry.register(TEST_GAUGE_NAME, new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return counter;
                }
            });

            return Response.status(Status.OK).build();
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
        RestAssured.port = response.getServerPort();
        RestAssured.basePath = BASE_PATH;
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

    @Test
    public final void testJerseyMetrics() {
        testVitamMetrics(VitamMetricsType.JERSEY);
    }

    @Test
    public final void testJvmMetrics() {
        testVitamMetrics(VitamMetricsType.JVM);
    }

    @Test
    public final void testBusinessMetrics() {
        testVitamMetrics(VitamMetricsType.BUSINESS);
        testBusinessGaugeValue();
    }

    @SuppressWarnings("rawtypes")
    private void testBusinessGaugeValue() {
        Map<String, Gauge> gauges = AbstractVitamApplication.getBusinessVitamMetrics().getRegistry().getGauges();
        final Map<String, String> headersMap =
            AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, TEST_RESOURCE_URI);

        assertFalse(TEST_GAUGE_NAME, gauges.containsKey(TEST_GAUGE_NAME));
        // Calling the resource should set the gauge and increment the counter
        RestAssured.given()
            .header(GlobalDataRest.X_TIMESTAMP, headersMap.get(GlobalDataRest.X_TIMESTAMP))
            .header(GlobalDataRest.X_PLATFORM_ID, headersMap.get(GlobalDataRest.X_PLATFORM_ID))
            .when()
            .get(TEST_RESOURCE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
        gauges = AbstractVitamApplication.getBusinessVitamMetrics().getRegistry().getGauges();
        assertTrue(TEST_GAUGE_NAME, gauges.containsKey(TEST_GAUGE_NAME));
        assertTrue(TEST_GAUGE_NAME + " value", gauges.get(TEST_GAUGE_NAME).getValue().equals(1));
    }

    private void testVitamMetrics(VitamMetricsType type) {
        final VitamMetrics metric = AbstractVitamApplication.getVitamMetrics(type);

        assertNotNull("Metric " + type.getName(), metric);
        assertTrue("Metric " + type.getName() + " reporting", metric.isReporting());
    }
}
