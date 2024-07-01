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

package fr.gouv.vitam.logbook.common.server.database.collections;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.GroupedTenantConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import org.bson.Document;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@VisibleForTesting
public final class LogbookCollectionsTestUtils {

    private LogbookCollectionsTestUtils() {
        // Private constructor for static class
    }

    @VisibleForTesting
    public static ElasticsearchLogbookIndexManager createTestIndexManager(
        List<Integer> dedicatedTenants,
        Map<String, List<Integer>> tenantGroups
    ) {
        List<Integer> allTenants = Streams.concat(
            dedicatedTenants.stream(),
            tenantGroups.values().stream().flatMap(Collection::stream)
        ).collect(Collectors.toList());

        List<GroupedTenantConfiguration> tenantGroupConfiguration = tenantGroups
            .entrySet()
            .stream()
            .map(
                entry ->
                    new GroupedTenantConfiguration()
                        .setName(entry.getKey())
                        .setTenants(entry.getValue().stream().map(Object::toString).collect(Collectors.joining(",")))
            )
            .collect(Collectors.toList());

        LogbookConfiguration logbookConfiguration = new LogbookConfiguration()
            .setLogbookTenantIndexation(
                new LogbookIndexationConfiguration()
                    .setDefaultCollectionConfiguration(
                        new DefaultCollectionConfiguration().setLogbookoperation(new CollectionConfiguration(1, 0))
                    )
                    .setGroupedTenantConfiguration(tenantGroupConfiguration)
            );
        return new ElasticsearchLogbookIndexManager(logbookConfiguration, allTenants);
    }

    @VisibleForTesting
    public static void beforeTestClass(
        final MongoDatabase db,
        String prefix,
        final LogbookElasticsearchAccess esClient
    ) {
        for (LogbookCollections collection : LogbookCollections.values()) {
            collection.getVitamCollection().setName(prefix + collection.getClasz().getSimpleName());
            collection.initialize(db, false);
            if (collection == LogbookCollections.OPERATION) {
                collection.initialize(esClient);
            }
        }

        if (esClient != null) {
            esClient.createIndexesAndAliases();
        }
    }

    @VisibleForTesting
    public static void afterTestClass(ElasticsearchLogbookIndexManager indexManager, boolean deleteEsIndexes) {
        try {
            for (LogbookCollections collection : LogbookCollections.values()) {
                if (null != collection.getVitamCollection().getCollection()) {
                    collection.getVitamCollection().getCollection().deleteMany(new Document());
                }

                if (collection == LogbookCollections.OPERATION && null != collection.getEsClient()) {
                    for (Integer tenant : indexManager.getDedicatedTenants()) {
                        if (deleteEsIndexes) {
                            collection
                                .getEsClient()
                                .deleteIndexByAliasForTesting(
                                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenant)
                                );
                        } else {
                            collection
                                .getEsClient()
                                .purgeIndexForTesting(
                                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenant)
                                );
                        }
                    }

                    for (String tenantGroupName : indexManager.getTenantGroups()) {
                        // Select first tenant
                        Integer tenant = indexManager.getTenantGroupTenants(tenantGroupName).iterator().next();
                        if (deleteEsIndexes) {
                            collection
                                .getEsClient()
                                .deleteIndexByAliasForTesting(
                                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenant)
                                );
                        } else {
                            collection
                                .getEsClient()
                                .purgeIndexForTesting(
                                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenant)
                                );
                        }
                    }
                }
            }
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public static void afterTest(ElasticsearchLogbookIndexManager indexManager) {
        afterTestClass(indexManager, false);
    }
}
