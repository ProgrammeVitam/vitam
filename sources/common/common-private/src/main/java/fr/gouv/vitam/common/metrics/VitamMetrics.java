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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.metrics.ElasticsearchReporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.configuration.VitamMetricsConfiguration;

/**
 * A basic class that acts as a container between a {@link VitamMetricRegistry} and a {@link ScheduledReporter}. This
 * class provides an access to the {@code VitamMetricRegistry} and the possibility to start/stop the reporting.
 *
 */
public class VitamMetrics {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamMetrics.class);
    private static final String ELASTICSEARCH_DATE_FORMAT = "YY.MM.dd";

    private final VitamMetricsType type;
    private final int interval;
    private final TimeUnit intervalUnit;
    private final VitamMetricRegistry registry = new VitamMetricRegistry();
    private ScheduledReporter reporter;
    private boolean isReporting = false;

    /**
     * A constructor to instantiate a {@see VitamMetrics} with the configuration object
     * {@link VitamMetricConfiguration}. The configuration object must be returned from the method {link
     * {@link VitamMetricsConfiguration#getMetricsConfigurations()}
     *
     * @param configuration {@link VitamMetricConfiguration}
     */
    public VitamMetrics(VitamMetricsType type, VitamMetricsConfiguration configuration) {
        ParametersChecker.checkParameter("VitamMetricsType", type);
        ParametersChecker.checkParameter("VitamMetricConfiguration", configuration);
        this.type = type;
        interval = configuration.getMetricReporterInterval();
        intervalUnit = configuration.getMetricReporterIntervalUnit();

        if (type.equals(VitamMetricsType.JVM)) {
            configureJVMMetrics();
        }
        switch (configuration.getMetricReporter()) {
            case CONSOLE:
                configureConsoleReporter();
                break;
            case ELASTICSEARCH:
                configureElasticsearchReporter(configuration);
                break;
            case LOGBACK:
                configureLogbackReporter(configuration);
                break;
            default:
                LOGGER.warn("VitamMetrics instanciated without reporter.");
                break;
        }
    }

    /**
     * Return the underlying metric registry
     *
     * @return {@link VitamMetricRegistry}
     */
    public VitamMetricRegistry getRegistry() {
        return registry;
    }

    /**
     * Return the type of this {@code VitamMetrics}
     *
     * @return {@link VitamMetricsType}
     */
    public VitamMetricsType getType() {
        return type;
    }

    /**
     * Indicates whether or not this {@code VitamMetrics} is currently reporting.
     *
     * @return boolean
     */
    public boolean isReporting() {
        return isReporting;
    }

    private void configureConsoleReporter() {
        reporter = ConsoleReporter.forRegistry(registry).build();
    }

    private void configureLogbackReporter(VitamMetricsConfiguration configuration) {
        reporter = LogbackReporter.forRegistry(registry).build();
    }

    private void configureElasticsearchReporter(VitamMetricsConfiguration configuration) {
        final Map<String, Object> additionalFields = new HashMap<>();

        additionalFields.put("hostname", ServerIdentity.getInstance().getName());
        additionalFields.put("role", ServerIdentity.getInstance().getRole());
        try {
            checkElasticsearchConnection(configuration.getMetricReporterHost(), configuration.getMetricReporterPort());
            reporter = ElasticsearchReporter.forRegistry(registry)
                .hosts(configuration.getMetricReporterHost() + ":" + configuration.getMetricReporterPort())
                .index(type.getElasticsearchIndex())
                .indexDateFormat(ELASTICSEARCH_DATE_FORMAT)
                .additionalFields(additionalFields)
                .build();
        } catch (final IOException e) {
            LOGGER.warn("Unable to reach ElasticSearch log host: " + e.getMessage());
        }
    }

    private void checkElasticsearchConnection(final String host, final int port) throws IOException {
        final URL templateUrl = new URL("http://" + host + ":" + port + "/");
        final HttpURLConnection connection = (HttpURLConnection) templateUrl.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(200);
        connection.connect();
        // Only by calling getResponseCode causes the connection to throw an exception if the host does not exists.
        connection.getResponseCode();
        connection.disconnect();
    }

    private void configureJVMMetrics() {
        final BufferPoolMetricSet bufferPool = new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer());
        final CachedThreadStatesGaugeSet cachedThreadStates = new CachedThreadStatesGaugeSet(1, TimeUnit.MINUTES);
        final ClassLoadingGaugeSet classLoading = new ClassLoadingGaugeSet();
        final FileDescriptorRatioGauge fileDescriptor = new FileDescriptorRatioGauge();
        final GarbageCollectorMetricSet garbageCollector = new GarbageCollectorMetricSet();
        final MemoryUsageGaugeSet memoryGauges = new MemoryUsageGaugeSet();

        registry.register("fileDescriptorRatioGauge", fileDescriptor);
        registry.registerAll(bufferPool);
        registry.registerAll(cachedThreadStates);
        registry.registerAll(classLoading);
        registry.registerAll(garbageCollector);
        registry.registerAll(memoryGauges);
        // ThreadStatesGaugeSet not working because duplicate metrics names.
        // TODO P2 open a github issue demanding a fix
    }

    /**
     * Start the reporting.
     */
    final public void start() {
        if (!isReporting && reporter != null) {
            reporter.start(interval, intervalUnit);
            isReporting = true;
        }
    }

    /**
     * Stop the reporting.
     */
    final public void stop() {
        if (isReporting && reporter != null) {
            reporter.stop();
            isReporting = false;
        }
    }


}
