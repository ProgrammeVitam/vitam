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

package fr.gouv.vitam.common.server2.application.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.server2.application.VitamMetricsReporterType;
import fr.gouv.vitam.common.server2.application.VitamMetricsType;

/**
 * Class to use for reading the metrics configuration file.
 */
public final class VitamMetricsConfigurationImpl {

    private List<VitamMetricConfigurationImpl> metricsConfigurations = new ArrayList<>();

    /**
     * DbConfiguration empty constructor for YAMLFactory
     */
    public VitamMetricsConfigurationImpl() {
        // empty
    }

    /**
     *
     * @param metricsConfigurations The metrics configurations read in the YAML configuration file
     * @return this
     */
    public VitamMetricsConfigurationImpl setMetrics(List<VitamMetricConfigurationImpl> metricsConfigurations) {
        if (metricsConfigurations != null) {
            this.metricsConfigurations = metricsConfigurations;
        }
        return this;
    }

    /**
     * Return the list of metrics configurations that were read by the YAML parser in the metrics configuration file.
     *
     * @return List<VitamMetricConfigurationImpl>
     */
    public List<VitamMetricConfigurationImpl> getMetricsConfigurations() {
        return metricsConfigurations;
    }

    /**
     * Implementation of VitamMetricConfiguration Interface
     */
    final private static class VitamMetricConfigurationImpl implements VitamMetricConfiguration {

        private int interval = 1;
        private VitamMetricsType type = VitamMetricsType.JERSEY;
        private String elasticsearchHost = "localhost";
        private String elasticsearchIndex = "metrics-vitam-";
        private int elasticsearchPort = 9201;
        private String elasticsearchIndexDateFormat = "YYYY.MM.dd";
        private VitamMetricsReporterType reporterType = null;
        private TimeUnit intervalUnit = TimeUnit.MINUTES;

        /**
         * VitamMetricConfigurationImpl constructor
         *
         * @param type
         * @param reporterType
         * @param interval
         * @param intervalUnit
         * @param elasticsearchHost
         * @param elasticsearchPort
         * @param elasticsearchIndex
         * @param elasticsearchIndexDateFormat
         */
        @JsonCreator
        public VitamMetricConfigurationImpl(
            @JsonProperty("type") final VitamMetricsType type,
            @JsonProperty("reporter_type") final VitamMetricsReporterType reporterType,
            @JsonProperty("interval") final int interval,
            @JsonProperty("interval_unit") final TimeUnit intervalUnit,
            @JsonProperty("elasticsearch_host") final String elasticsearchHost,
            @JsonProperty("elasticsearch_port") final int elasticsearchPort,
            @JsonProperty("elasticsearch_index") final String elasticsearchIndex,
            @JsonProperty("elasticsearch_index_date_format") final String elasticsearchIndexDateFormat) {

            setType(type);
            setReporterType(reporterType);
            setInterval(interval);
            setIntervalUnit(intervalUnit);
            setElasticsearchHost(elasticsearchHost);
            setElasticsearchPort(elasticsearchPort);
            setElasticsearchIndex(elasticsearchIndex);
            setElasticsearchIndexDateFormat(elasticsearchIndexDateFormat);
        }

        private void setType(final VitamMetricsType type) {
            if (type != null) {
                this.type = type;
            }
        }

        private void setReporterType(final VitamMetricsReporterType reporterType) {
            if (reporterType != null) {
                this.reporterType = reporterType;
            }
        }

        private void setInterval(final int interval) {
            if (interval > 0) {
                this.interval = interval;
            }
        }

        private void setIntervalUnit(TimeUnit intervalUnit) {
            if (intervalUnit != null) {
                this.intervalUnit = intervalUnit;
            }
        }

        private void setElasticsearchHost(final String host) {
            if (host != null) {
                elasticsearchHost = host;
            }
        }

        private void setElasticsearchPort(final int port) {
            if (port > 0) {
                elasticsearchPort = port;
            }
        }

        private void setElasticsearchIndex(final String index) {
            if (index != null) {
                elasticsearchIndex = index;
            }
        }

        private void setElasticsearchIndexDateFormat(final String format) {
            if (format != null) {
                elasticsearchIndexDateFormat = format;
            }
        }

        @Override
        public String getElasticsearchHost() {
            return elasticsearchHost;
        }
        
        @Override
        public int getElasticSearchPort() {
            return elasticsearchPort;
        }

        @Override
        public String getElasticsearchIndex() {
            return elasticsearchIndex;
        }

        @Override
        public String getElasticsearchIndexDateFormat() {
            return elasticsearchIndexDateFormat;
        }

        @Override
        public VitamMetricsType getType() {
            return type;
        }

        @Override
        public VitamMetricsReporterType getReporterType() {
            return reporterType;
        }

        @Override
        public int getInterval() {
            return interval;
        }

        @Override
        public TimeUnit getIntervalUnit() {
            return intervalUnit;
        }
    }

}
