/**
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
 */
package fr.gouv.vitam.common.server.application.junit;

import static org.mockito.Mockito.mock;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory;
import fr.gouv.vitam.common.server.application.VitamApplicationInterface;

/**
 * Class to extend for Junit as JerseyTest. </br>
 *
 * @param <A> Application class </br>
 *        <P>
 *        What is already implemented:
 *        <ul>
 *        <li>A mocked ExpectedResults interface</li>
 *        <li>The start and stop of the application at each run</li>
 *        <li>_client containing MockOrRestClient interface that will be closed at each run</li>
 *        <li>The reservation and free of network port through JunitHelper used to start the application (the port is
 *        accessible through getServerPort() method)</li>
 *        </ul>
 *        </br>
 *        You must implement the following:
 *        <ul>
 *        <li>_getClient() method that returns the client to use and close at each run</li>
 *        <li>setup() method (for instance to set your own mock if default one is not enough)</li>
 *        <li>startVitamApplication(int reservedPort) to create the application and starting it (method
 *        application.start()) using the reserved port</li>
 *        <li>Define your own Application class (to register your own Resource) based on AbstractVitamApplication</li>
 *        <li>Define your own ApplicationConfiguration (or simply extend DefaultVitamApplicationConfiguration)</li>
 *        </ul>
 *        You may implement:
 *        <ul>
 *        <li>Your own Mock</li>
 *        </ul>
 *        Example:
 *
 *        <pre>
 * <code>
 * public class MyClientTest extends VitamJerseyTest {
 *  private static final String RESOURCE_PATH = "...";
 *  // You own factory
 *  TestVitamClientFactory<TestClient> factory =
 *    new TestVitamClientFactory<TestClient>(8080, RESOURCE_PATH);
 *  // Store your own client
 *  TestClient client;
 *
 *  // ************************************** //
 *  // Start of VitamJerseyTest configuration //
 *  // ************************************** //
 *  // Override the beforeTest if necessary (not mandatory)
 *  &#64;Override
 *  public void beforeTest() throws VitamApplicationServerException {
 *    // anything before test
 *  }
 *
 *  // Override the afterTest if necessary (not mandatory)
 *  &#64;Override
 *  public void afterTest() throws VitamApplicationServerException {
 *   // anything after test
 *  }
 *
 *  // This method MUST be created. Assign your own client (keep it) and returns it.
 *  &#64;Override
 *  protected MockOrRestClient _getClient() throws VitamApplicationServerException {
 *    try {
 *       // Fix your factory
 *       factory.changeServerPort(getServerPort());
 *       client = factory.getClient();
 *       return client;
 *    } catch (final VitamClientException e) {
 *       throw new VitamApplicationServerException(e);
 *    }
 *  }
 *
 *  // Setup test to setup anything if necessary (not mandatory)
 *  &#64;Override
 *  public void setup() {
 *    // nothing
 *  }
 *
 *  // Define the getApplication to return your Application using the correct Configuration
 *  &#64;Override
 *  public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
 *    // Use your own ApplicationConfiguration if necessary, or just copy the following
 *    final TestVitamApplicationConfiguration configuration =
 *      new TestVitamApplicationConfiguration();
 *
 *    // DEFAULT_XML_CONFIGURATION_FILE could be used as default XML file, but you can specify another
 *    configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
 *
 *    // Use your own application or just copy the following
 *    final AbstractApplication application = new AbstractApplication(configuration);
 *    // Start the application (mandatory)
 *    try {
 *       application.start();
 *    } catch (final VitamApplicationServerException e) {
 *       SysErrLogger.FAKE_LOGGER.ignoreLog(e);
 *       throw new IllegalStateException("Cannot start the application", e);
 *    }
 *    // Return the response as follow
 *    return new StartApplicationResponse<AbstractApplication>()
 *       .setServerPort(application.getVitamServer().getPort())
 *       .setApplication(application);
 *  }
 *
 *  // Define your Application class (or reuse it), in particular set your own MockResources or just copy the following
 *  public final class AbstractApplication
 *    extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
 *    protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
 *       super(TestVitamApplicationConfiguration.class, configuration);
 *    }
 *
 *    &#64;Override
 *    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
 *       resourceConfig.registerInstances(new MockResource(mock));
 *    }
 *  }
 *
 *  // Define your Configuration class (or reuse it) or just copy the following
 *  public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
 *    // Empty for this example
 *  }
 *
 *  // Define your Resource class
 *  &#64;Path(RESOURCE_PATH)
 *  public static class MockResource extends ApplicationStatusResource {
 *    private final ExpectedResults expectedResponse;
 *
 *    public MockResource(ExpectedResults expectedResponse) {
 *       this.expectedResponse = expectedResponse;
 *    }
 *    ...
 *  }
 *  // ************************************ //
 *  // End of VitamJerseyTest configuration //
 *  // ************************************ //
 *
 *  // Use client to act on AbstractApplication
 *
 * }</code>
 *        </pre>
 *        </P>
 */
public abstract class VitamJerseyTest<A> implements VitamApplicationTestFactory<A> {
    /**
     * Default XML Configuration to use (from JunitHelper)
     */
    public static final String DEFAULT_XML_CONFIGURATION_FILE = "jetty-config-base-test.xml";
    /**
     * Localhost
     */
    public static final String HOSTNAME = "localhost";

    private final StartApplicationResponse<A> response;
    final MockOrRestClient _client;
    final VitamClientFactoryInterface<?> factory;

    protected ExpectedResults mock;

    /**
     * Default ExpectedResults
     */
    public interface ExpectedResults {
        /**
         *
         * @return Mock Response
         */
        Response post();

        /**
         *
         * @return Mock Response
         */
        Response get();

        /**
         *
         * @return Mock Response
         */
        Response put();

        /**
         *
         * @return Mock Response
         */
        Response delete();

        /**
         *
         * @return Mock Response
         */
        Response head();

        /**
         *
         * @return Mock Response
         */
        Response options();
    }

    /**
     * Create the VitamJerseyTest, calling setup first.</br>
     * Note: will call factory.changeServerPort(getServerPort()) automatically before creating the client.
     *
     * @param factory the Client Factory to use
     *
     * @throws VitamApplicationServerException when starting server exception occurred
     *
     * @throws IllegalStateException if the start is incorrect
     */
    @SuppressWarnings("unchecked")
    public VitamJerseyTest(VitamClientFactoryInterface<?> factory) {
        mock = mock(ExpectedResults.class);
        this.factory = factory;
        final ClientConfiguration clientConfiguration = this.factory.getClientConfiguration();
        if (clientConfiguration != null) {
            clientConfiguration.setServerHost(HOSTNAME);
        }
        setup();
        response = (StartApplicationResponse<A>) JunitHelper.getInstance().findAvailablePortSetToApplication(this);
        factory.changeServerPort(getServerPort());
        _client = factory.getClient();
    }

    /**
     *
     * @return the factory to use
     */
    public VitamClientFactoryInterface<?> getFactory() {
        return factory;
    }

    /**
     *
     * @return the client using the factory
     */
    public MockOrRestClient getClient() {
        return _client;
    }

    /**
     * Do anything necessary before starting the application and before getting the server port. Called before
     * startVitamApplication and _getClient()
     */
    public void setup() {
        // Default do nothing more
    }

    /**
     *
     * @return the current port
     */
    public final int getServerPort() {
        return response.getServerPort();
    }

    /**
     *
     * @return the current application
     */
    public final VitamApplicationInterface<?, ?> getApplication() {
        return (VitamApplicationInterface<?, ?>) response.getApplication();
    }

    /**
     * Start the Application.
     *
     * @throws VitamApplicationServerException when starting server exception occurred
     */
    @Before
    public final void startTest() throws VitamApplicationServerException {
        beforeTest();
    }

    /**
     * To be extended if necessary (equivalent to @Before)
     *
     * @throws VitamApplicationServerException when starting server exception occurred
     */
    public void beforeTest() throws VitamApplicationServerException {
        // Empty
    }

    /**
     * End the Application.
     *
     * @throws VitamApplicationServerException stopping server exception occurred
     */
    @After
    public final void endTest() throws VitamApplicationServerException {
        if (response.getApplication() != null) {
            ((VitamApplicationInterface<?, ?>) response.getApplication()).stop();
        }
        JunitHelper.getInstance().releasePort(response.getServerPort());
        if (_client != null) {
            _client.close();
        }
        afterTest();
    }

    /**
     * To be extended if necessary (equivalent to @After)
     *
     * @throws VitamApplicationServerException when stopping server exception occurred
     */
    public void afterTest() throws VitamApplicationServerException {
        // Empty
    }

}
