/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.common.model.config;

public final class CollectionConfigurationUtils {

    private static final int MAX_REPLICAS = 100;
    private static final int MAX_SHARDS = 1000;
    private static final int MIN_SHARDS = 1;
    private static final int MIN_REPLICAS = 0;

    private CollectionConfigurationUtils() {
        // Private constructor for static class
    }

    public static void validate(CollectionConfiguration collectionConfiguration, boolean allowNulls)
        throws IllegalStateException {
        if (!allowNulls && collectionConfiguration.getNumberOfShards() == null) {
            throw new IllegalStateException("Invalid configuration. Missing shards");
        }

        if (!allowNulls && collectionConfiguration.getNumberOfReplicas() == null) {
            throw new IllegalStateException("Invalid configuration. Missing replicas");
        }

        if (collectionConfiguration.getNumberOfShards() != null) {
            if (
                collectionConfiguration.getNumberOfShards() < MIN_SHARDS ||
                collectionConfiguration.getNumberOfShards() > MAX_SHARDS
            ) {
                throw new IllegalStateException(
                    String.format(
                        "Invalid configuration. Invalid number of shards %d. Expected between %d and %d (inclusive)",
                        collectionConfiguration.getNumberOfShards(),
                        MIN_SHARDS,
                        MAX_SHARDS
                    )
                );
            }
        }

        if (collectionConfiguration.getNumberOfReplicas() != null) {
            if (
                collectionConfiguration.getNumberOfReplicas() < MIN_REPLICAS ||
                collectionConfiguration.getNumberOfReplicas() > MAX_REPLICAS
            ) {
                throw new IllegalStateException(
                    String.format(
                        "Invalid configuration. Invalid number of replicas %d. Expected between %d and %d (inclusive)",
                        collectionConfiguration.getNumberOfReplicas(),
                        MIN_REPLICAS,
                        MAX_REPLICAS
                    )
                );
            }
        }
    }

    public static CollectionConfiguration merge(
        CollectionConfiguration customConfig,
        CollectionConfiguration defaultConfig
    ) {
        if (
            customConfig == null ||
            (customConfig.getNumberOfShards() == null && customConfig.getNumberOfReplicas() == null)
        ) {
            return defaultConfig;
        }

        if (customConfig.getNumberOfShards() != null && customConfig.getNumberOfReplicas() != null) {
            return customConfig;
        }

        return new CollectionConfiguration()
            .setNumberOfShards(
                customConfig.getNumberOfShards() != null
                    ? customConfig.getNumberOfShards()
                    : defaultConfig.getNumberOfShards()
            )
            .setNumberOfReplicas(
                customConfig.getNumberOfReplicas() != null
                    ? customConfig.getNumberOfReplicas()
                    : defaultConfig.getNumberOfReplicas()
            );
    }
}
