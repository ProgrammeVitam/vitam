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

package fr.gouv.vitam.functional.administration.common.config;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ElasticsearchFunctionalAdminIndexManager {

    private final Map<FunctionalAdminCollections, ElasticsearchIndexSettings> elasticsearchIndexSettingsMap;

    public ElasticsearchFunctionalAdminIndexManager(AdminManagementConfiguration configuration) {
        FunctionalAdminIndexationConfiguration indexationConfiguration = configuration.getIndexationConfiguration();

        Map<FunctionalAdminCollections, CollectionConfiguration> collectionConfigurationMap = Arrays.stream(
            FunctionalAdminCollections.values()
        ).collect(
            Collectors.toMap(
                functionalAdminCollection -> functionalAdminCollection,
                functionalAdminCollection ->
                    new CollectionConfiguration()
                        .setNumberOfShards(
                            indexationConfiguration.getCollectionConfiguration(functionalAdminCollection) != null &&
                                indexationConfiguration
                                        .getCollectionConfiguration(functionalAdminCollection)
                                        .getNumberOfShards() !=
                                    null
                                ? indexationConfiguration
                                    .getCollectionConfiguration(functionalAdminCollection)
                                    .getNumberOfShards()
                                : indexationConfiguration.getDefaultConfiguration().getNumberOfShards()
                        )
                        .setNumberOfReplicas(
                            indexationConfiguration.getCollectionConfiguration(functionalAdminCollection) != null &&
                                indexationConfiguration
                                        .getCollectionConfiguration(functionalAdminCollection)
                                        .getNumberOfReplicas() !=
                                    null
                                ? indexationConfiguration
                                    .getCollectionConfiguration(functionalAdminCollection)
                                    .getNumberOfReplicas()
                                : indexationConfiguration.getDefaultConfiguration().getNumberOfReplicas()
                        )
            )
        );

        this.elasticsearchIndexSettingsMap = buildElasticsearchIndexSettingsMap(collectionConfigurationMap);
    }

    private static Map<FunctionalAdminCollections, ElasticsearchIndexSettings> buildElasticsearchIndexSettingsMap(
        Map<FunctionalAdminCollections, CollectionConfiguration> collectionConfigurationMap
    ) {
        Map<FunctionalAdminCollections, ElasticsearchIndexSettings> elasticsearchIndexSettingsMap = new HashMap<>();
        for (FunctionalAdminCollections functionalAdminCollection : FunctionalAdminCollections.values()) {
            if (functionalAdminCollection == FunctionalAdminCollections.VITAM_SEQUENCE) {
                continue;
            }
            ElasticsearchIndexSettings elasticsearchIndexSettings = new ElasticsearchIndexSettings(
                collectionConfigurationMap.get(functionalAdminCollection).getNumberOfShards(),
                collectionConfigurationMap.get(functionalAdminCollection).getNumberOfReplicas(),
                getMappingLoader(functionalAdminCollection)
            );
            elasticsearchIndexSettingsMap.put(functionalAdminCollection, elasticsearchIndexSettings);
        }
        return elasticsearchIndexSettingsMap;
    }

    public ElasticsearchIndexAliasResolver getElasticsearchIndexAliasResolver(FunctionalAdminCollections collection) {
        return tenant -> ElasticsearchIndexAlias.ofCrossTenantCollection(collection.getVitamCollection().getName());
    }

    public ElasticsearchIndexSettings getElasticsearchIndexSettings(FunctionalAdminCollections collection) {
        return elasticsearchIndexSettingsMap.get(collection);
    }

    private static Supplier<String> getMappingLoader(FunctionalAdminCollections collection) {
        return () -> {
            try {
                return ElasticsearchUtil.transferJsonToMapping(
                    collection.getElasticsearchCollection().getMappingAsInputStream()
                );
            } catch (IOException e) {
                throw new VitamFatalRuntimeException("Could not load mapping file for collection " + collection);
            }
        };
    }
}
