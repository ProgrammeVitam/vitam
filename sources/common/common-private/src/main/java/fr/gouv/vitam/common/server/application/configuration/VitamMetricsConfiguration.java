/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.common.server.application.configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.metrics.VitamMetricsReporterType;

/**
 * Class to use for reading the metrics configuration file.
 */
public final class VitamMetricsConfiguration {

    private boolean metricsJersey = false;
    private boolean metricsJVM = false;
    private VitamMetricsReporterType metricReporter = VitamMetricsReporterType.NONE;
    private int metricReporterInterval = 1;
    private TimeUnit metricReporterIntervalUnit = TimeUnit.MINUTES;
    private String[] metricReporterHosts = {};
    private VitamLogLevel metricLogLevel = VitamLogLevel.INFO;

    /**
     * DbConfiguration empty constructor for YAMLFactory
     */
    public VitamMetricsConfiguration() {
        // empty
    }

    /**
     * Determines whether or not Jersey metrics should be activated
     *
     * @return boolean
     */
    public boolean hasMetricsJersey() {
        return metricsJersey;
    }

    /**
     * Set whether or not Jersey metrics should be activated
     *
     * @param metricsJersey
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricsJersey(boolean metricsJersey) {
        this.metricsJersey = metricsJersey;
        return this;
    }

    /**
     * Determines whether or not JVM metrics should be activated
     *
     * @return boolean
     */
    public boolean hasMetricsJVM() {
        return metricsJVM;
    }

    /**
     * Set whether or not JVM metrics should be activated
     *
     * @param metricsJVM
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricsJVM(boolean metricsJVM) {
        this.metricsJVM = metricsJVM;
        return this;
    }

    /**
     * Get the metric reporter type
     *
     * @return VitamMetricsReporterType
     */
    public VitamMetricsReporterType getMetricReporter() {
        return metricReporter;
    }

    /**
     * Set the metric reporter type
     *
     * @param metricReporter
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricReporter(VitamMetricsReporterType metricReporter) {
        this.metricReporter = metricReporter;
        return this;
    }

    /**
     * Get the metric reporter host
     *
     * @return String[]
     */
    public String[] getMetricReporterHosts() {
        return metricReporterHosts;
    }

    /**
     * Set the metric reporter hosts
     *
     * @param metricReporterHosts
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricReporterHosts(List<String> metricReporterHosts) {
        this.metricReporterHosts = metricReporterHosts.toArray(this.metricReporterHosts);
        return this;
    }

    /**
     * Get the metric log level
     *
     * @return VitamLogLevel
     */
    public VitamLogLevel getMetricLogLevel() {
        return metricLogLevel;
    }

    /**
     * Set the metric log level
     *
     * @param metricLogLevel
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricLogLevel(VitamLogLevel metricLogLevel) {
        this.metricLogLevel = metricLogLevel;
        return this;
    }

    /**
     * Get the metric reporter interval
     *
     * @return int
     */
    public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    /**
     * Set the metric reporter interval
     *
     * @param metricReporterInterval
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricReporterInterval(int metricReporterInterval) {
        this.metricReporterInterval = metricReporterInterval;
        return this;
    }

    /**
     * Get the metric reporter interval unit
     *
     * @return TimeUnit
     */
    public TimeUnit getMetricReporterIntervalUnit() {
        return metricReporterIntervalUnit;
    }

    /**
     * Set the metric reporter interval unit
     *
     * @param metricReporterIntervalUnit
     * @return VitamMetricsConfiguration
     */
    public VitamMetricsConfiguration setMetricReporterIntervalUnit(TimeUnit metricReporterIntervalUnit) {
        this.metricReporterIntervalUnit = metricReporterIntervalUnit;
        return this;
    }

}
