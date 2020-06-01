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
package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.ParametersChecker;
import org.bson.Document;

import java.util.Collection;
import java.util.Map;

@VisibleForTesting
public final class MetadataCollectionsTestUtils {

    private MetadataCollectionsTestUtils() {
        // Private constructor for static class
    }

    @VisibleForTesting
    public static void beforeTestClass(final MongoDatabase db, String prefix,
        final ElasticsearchAccessMetadata esClient, Integer... tenants) {
        beforeTestClass(db, prefix, esClient, Lists.newArrayList(MetadataCollections.values()), tenants);
    }

    @VisibleForTesting
    public static void beforeTestClass(final MongoDatabase db, String prefix,
        final ElasticsearchAccessMetadata esClient,
        Collection<MetadataCollections> metadataCollections,
        Integer... tenants) {
        ParametersChecker.checkParameter("metadataCollections is required", metadataCollections);
        for (MetadataCollections collection : metadataCollections) {
            collection.getVitamCollection()
                .setName(prefix + collection.getVitamCollection().getClasz().getSimpleName());
            collection.initialize(db, false);
            if (collection.getEsClient() == null) {
                collection.initialize(esClient);
            }

            if (null != collection.getEsClient()) {
                for (Integer tenant : tenants) {
                    Map<String, String> map = collection.getEsClient().addIndex(collection, tenant);
                    if (map.isEmpty()) {
                        throw new RuntimeException(
                            "Index not created for the collection " + collection.getName() + " and tenant :" + tenant);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public static void afterTestClass(boolean deleteEsIndex, Integer... tenants) {
        afterTestClass(Lists.newArrayList(MetadataCollections.values()), deleteEsIndex, tenants);
    }

    @VisibleForTesting
    public static void afterTestClass(Collection<MetadataCollections> metadataCollections, boolean deleteEsIndex,
        Integer... tenants) {
        if (null == metadataCollections) {
            return;
        }
        for (MetadataCollections collection : metadataCollections) {
            if (null != collection.getVitamCollection().getCollection()) {
                collection.getVitamCollection().getCollection().deleteMany(new Document());
            }

            if (null != collection.getEsClient()) {
                for (Integer tenant : tenants) {
                    if (deleteEsIndex) {
                        collection.getEsClient().deleteIndexByAlias(collection.getName().toLowerCase(), tenant);
                    } else {
                        collection.getEsClient().purgeIndex(collection.getName().toLowerCase(), tenant);
                    }
                }
            }
        }
    }



    @VisibleForTesting
    public static void afterTest(Integer... tenants) {
        afterTestClass(false, tenants);
    }

    @VisibleForTesting
    public static void afterTest(Collection<MetadataCollections> metadataCollections, Integer... tenants) {
        afterTestClass(metadataCollections, false, tenants);
    }
}

