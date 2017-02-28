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

package fr.gouv.vitam.common.server.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamInstrumentedResourceMethodApplicationListener;
import fr.gouv.vitam.common.metrics.VitamMetricRegistry;
import fr.gouv.vitam.common.metrics.VitamMetrics;
import fr.gouv.vitam.common.metrics.VitamMetricsType;
import fr.gouv.vitam.common.security.filter.AuthorizationFilter;
import fr.gouv.vitam.common.server.RequestIdContainerFilter;
import fr.gouv.vitam.common.server.TenantIdContainerFilter;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.VitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.configuration.VitamMetricsConfiguration;

/**
 * Abstract implementation of VitamApplication which handle common tasks for all sub-implementation
 *
 * @param <A> VitamApplication final class
 * @param <C> VitamApplicationConfiguration final class
 */
public abstract class AbstractVitamApplication<A extends VitamApplication<A, C>, C extends VitamApplicationConfiguration>
    implements VitamApplication<A, C> {
    private static final String APPLICATION_SERVER = " Application Server";
    private static final String CANNOT_START_THE = "Cannot start the ";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractVitamApplication.class);
    protected static final String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file is a mandatory argument for ";
    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String METRICS_CONF_FILE_NAME = "vitam.metrics.conf";
    private static final Map<VitamMetricsType, VitamMetrics> metrics = new ConcurrentHashMap<>();

    private C configuration;
    private Handler applicationHandler;
    private final Class<C> configurationType;
    private final String configurationFilename;
    private VitamServer vitamServer;
    private final String role = ServerIdentity.getInstance().getRole();

    /**
     * Protected constructor assigning application and configuration types </br>
     * Usage example in sub-implementation : </br>
     * </br>
     * class MyApplication extends AbstractVitamApplication<MyApplication, MyApplicationConfiguration> {</br>
     * protected MyApplication(String configFile) {</br>
     * super(MyApplication.class, MyApplicationConfiguration.class, configFile);</br>
     * }</br>
     * }
     *
     * @param configurationType the configuration class type
     * @param configurationFile the configuration file
     * @throws IllegalStateException
     */
    protected AbstractVitamApplication(Class<C> configurationType, String configurationFile) {
        if (configurationFile == null || configurationFile.isEmpty()) {
            throw new IllegalStateException("Configuration file is mandatory");
        }
        this.configurationType = configurationType;
        this.configurationFilename = configurationFile;
        configure(configurationFile);
    }

    /**
     * Protected constructor assigning application and configuration types</br>
     * </br>
     * Usage example in sub-implementation : for Junit test </br>
     * class MyApplication extends AbstractVitamApplication<MyApplication, MyApplicationConfiguration> {</br>
     * protected MyApplication(C config) {</br>
     * super(MyApplication.class, MyApplicationConfiguration.class, config);</br>
     * }</br>
     * }
     *
     * @param configurationType the configuration class type
     * @param configuration the configuration object
     * @throws IllegalStateException
     */
    protected AbstractVitamApplication(Class<C> configurationType, C configuration) {
        if (configuration == null) {
            throw new IllegalStateException("Configuration Object is mandatory");
        }
        this.configurationType = configurationType;
        this.configurationFilename = null;
        configure(configuration);
    }

    /**
     * From the configuration file, read the configuration, the Jetty Configuration and create the associate VitamServer
     * using Jetty, adding the applicationHandler from buildApplicationHandler
     *
     * @param configurationFile
     * @throws IllegalStateException
     */
    protected final void configure(String configurationFile) {
        try {
            try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
                final C buildConfiguration = PropertiesUtils.readYaml(yamlIS,
                    getConfigurationType());
                configure(buildConfiguration);
            }
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException(CANNOT_START_THE + role + APPLICATION_SERVER, e);
        }
    }

    /**
     * To allow override on non Vitam platform such as IHM
     */
    protected void platformSecretConfiguration() {
        // Load Platform secret from vitam.conf file
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters =
                PropertiesUtils.readYaml(yamlIS, VitamConfigurationParameters.class);

            VitamConfiguration.setSecret(vitamConfigurationParameters.getSecret());
            VitamConfiguration.setFilterActivation(vitamConfigurationParameters.isFilterActivation());

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException(CANNOT_START_THE + role + APPLICATION_SERVER, e);
        }
    }


    /**
     * Used in Junit test
     *
     * @param configuration
     * @throws IllegalStateException
     */
    private final void configure(C configuration) {
        try {
            platformSecretConfiguration();
            setConfiguration(configuration);
            applicationHandler = buildApplicationHandler();
            final String jettyConfig = getConfiguration().getJettyConfig();

            LOGGER.info(role + " Starts with jetty config");
            vitamServer = VitamServerFactory.newVitamServerByJettyConf(jettyConfig);
            if (vitamServer != null) {
                vitamServer.configure(applicationHandler);
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(CANNOT_START_THE + role + APPLICATION_SERVER, e);
        }
    }

    /**
     * Start the reporting of every metrics
     */
    public final void startMetrics() {
        for (final Entry<VitamMetricsType, VitamMetrics> entry : metrics.entrySet()) {
            entry.getValue().start();
        }
    }

    /**
     * Clear the metrics map from any existing {@code VitamMetrics} and reload the configuration from the
     * {@code AbstractVitamApplication#METRICS_CONF_FILE_NAME}
     */
    protected static final void clearAndconfigureMetrics() {
        VitamMetricsConfiguration metricsConfiguration = new VitamMetricsConfiguration();

        metrics.clear();
        // Throws a JsonMappingException when the vitam.metrics.conf file is empty
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(METRICS_CONF_FILE_NAME)) {
            metricsConfiguration = PropertiesUtils.readYaml(yamlIS, VitamMetricsConfiguration.class);
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            LOGGER.warn(e.getMessage());
        }

        if (metricsConfiguration.hasMetricsJersey()) {
            metrics.put(VitamMetricsType.JERSEY, new VitamMetrics(VitamMetricsType.JERSEY, metricsConfiguration));
        }
        if (metricsConfiguration.hasMetricsJVM()) {
            metrics.put(VitamMetricsType.JVM, new VitamMetrics(VitamMetricsType.JVM, metricsConfiguration));
        }
        metrics.put(VitamMetricsType.BUSINESS, new VitamMetrics(VitamMetricsType.BUSINESS, metricsConfiguration));
    }

    protected void checkJerseyMetrics(final ResourceConfig resourceConfig) {
        if (metrics.containsKey(VitamMetricsType.JERSEY)) {
            resourceConfig.register(new VitamInstrumentedResourceMethodApplicationListener(
                metrics.get(VitamMetricsType.JERSEY).getRegistry()));
        }
    }

    /**
     * This method must add the Application resources and eventually registering specific component
     *
     * @param resourceConfig the ResourceConfig to setup
     */
    protected abstract void registerInResourceConfig(final ResourceConfig resourceConfig);

    /**
     *
     * @return 0 for NO_SESSION, 1 for SESSION
     */
    protected int getSession() {
        return ServletContextHandler.NO_SESSIONS;
    }

    /**
     * To set extra filters
     *
     * @param context the context on which to call context.addFilter (as many times as needed)
     * @throws VitamApplicationServerException
     */
    protected void setFilter(ServletContextHandler context) throws VitamApplicationServerException {
        // Default do nothing
    }

    /**
     * Re Implement this method to construct your application specific handler if necessary. </br>
     * </br>
     * If extra register are needed, override the method getResources.</br>
     * If extra filter or action on context are needed, override setFilter.
     *
     * @return the generated Handler
     * @throws VitamApplicationServerException
     */
    protected Handler buildApplicationHandler() throws VitamApplicationServerException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonJsonProvider.class)
            .register(JacksonFeature.class)
            // Register a Generic Exception Mapper
            .register(new GenericExceptionMapper())
            // Register container filters to copy the header's parameters (tenant_id and request_id)
            .register(RequestIdContainerFilter.class)
            .register(TenantIdContainerFilter.class);

        // Register Jersey Metrics Listener
        clearAndconfigureMetrics();
        checkJerseyMetrics(resourceConfig);
        // Use chunk size also in response
        resourceConfig.property(ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, VitamConfiguration.getChunkSize());
        // Cleaner filter
        resourceConfig.register(ConsumeAllAfterResponseFilter.class);
        // Not supported MultiPartFeature.class
        registerInResourceConfig(resourceConfig);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder sh = new ServletHolder(servletContainer);
        final ServletContextHandler context = new ServletContextHandler(getSession());
        context.setContextPath("/");
        context.addServlet(sh, "/*");

        // Authorization Filter
        if (VitamConfiguration.isFilterActivation() && !Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
            context.addFilter(AuthorizationFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        setFilter(context);
        return context;
    }

    @Override
    public String getConfigFilename() {
        return configurationFilename;
    }

    @Override
    public final C getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(C configuration) {
        this.configuration = configuration;
    }

    @Override
    public final Class<C> getConfigurationType() {
        return configurationType;
    }

    @Override
    public final Handler getApplicationHandler() {
        return applicationHandler;
    }

    @Override
    public final VitamServer getVitamServer() {
        return vitamServer;
    }

    @Override
    public final void run() throws VitamApplicationServerException {
        if (vitamServer != null && !vitamServer.isStarted()) {
            startMetrics();
            vitamServer.startAndJoin();
        } else if (vitamServer == null) {
            throw new VitamApplicationServerException("VitamServer is not ready to be started");
        }
    }

    @Override
    public final void start() throws VitamApplicationServerException {
        if (vitamServer != null && !vitamServer.isStarted()) {
            vitamServer.start();
        } else if (vitamServer == null) {
            throw new VitamApplicationServerException("VitamServer is not ready to be started");
        }
    }

    @Override
    public final void stop() throws VitamApplicationServerException {
        if (vitamServer != null && vitamServer.isStarted()) {
            vitamServer.stop();
        }
    }

    /**
     * Return the {@link VitamMetricRegistry} of {@link VitamMetricsType#BUSINESS} type. This
     * {@code VitamMetricRegistry} can be used to register custom metrics wherever in the application.
     *
     * @return {@link VitamMetrics} BUSINESS VitamMetrics
     */
    public static final VitamMetricRegistry getBusinessMetricsRegistry() {
        if (metrics.containsKey(VitamMetricsType.BUSINESS)) {
            return metrics.get(VitamMetricsType.BUSINESS).getRegistry();
        } else {
            LOGGER.warn("AbstractVitamApplication#getBusinessMetricRegistry: empty VitamMetricRegistry.");
            return new VitamMetricRegistry();
        }
    }

    /**
     * Return a {@link VitamMetrics} object for a given {@link VitamMetricsType} or null if the VitamMetrics does not
     * exists.
     *
     * @param type {@link VitamMetricsType} type
     * @return {@link VitamMetrics} VitamMetrics
     */
    public static final VitamMetrics getVitamMetrics(VitamMetricsType type) {
        ParametersChecker.checkParameter("VitamMetricsType", type);

        return metrics.get(type);
    }

}
