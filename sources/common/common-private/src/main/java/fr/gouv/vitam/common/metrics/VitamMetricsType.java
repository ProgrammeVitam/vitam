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
package fr.gouv.vitam.common.metrics;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.server.application.configuration.VitamMetricsConfiguration;

/**
 * Enums to use for the configuration of {@link VitamMetrics} through the {@link VitamMetricsConfiguration} class.
 *
 */
public enum VitamMetricsType {

    REST("REST", "metrics-vitam-rest"),

    JVM("JVM", "metrics-vitam-jvm"),

    BUSINESS("BUSINESS", "metrics-vitam-business");

    private final String name;
    private final String elasticsearchIndex;

    /**
     * Constructor
     *
     * @param String name
     */
    VitamMetricsType(final String name, final String elasticsearchIndex) {
        this.name = name;
        this.elasticsearchIndex = elasticsearchIndex;
    }

    /**
     * Return the name of the enum as a String
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Return the elasticsearch index of this metric type
     *
     * @return String
     */
    public String getElasticsearchIndex() {
        return elasticsearchIndex;
    }

    /**
     * Retrieve an {@link VitamMetricsType} given a name. Throws an {@code IllegalArgumentException} if the name doesn't
     * exists.
     *
     * @param metricTypeName
     * @return VitamMetricsType
     * @throws IllegalArgumentException
     */
    public static VitamMetricsType get(String metricTypeName) throws IllegalArgumentException {
        ParametersChecker.checkParameterNullOnly("VitamMetricsType name", metricTypeName);

        for (final VitamMetricsType v : values()) {
            if (v.getName().equals(metricTypeName)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Invalid VitamMetricsType name");
    }
}
