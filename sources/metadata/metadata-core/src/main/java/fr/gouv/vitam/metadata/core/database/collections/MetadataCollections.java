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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.collections.VitamDescriptionLoader;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Metadata Collection
 */
public enum MetadataCollections {
    /**
     * vitamCollection
     * Unit Collection
     */
    UNIT(Unit.class),
    /**
     * ObjectGroup Collection
     */
    OBJECTGROUP(ObjectGroup.class);

    private final VitamDescriptionResolver vitamDescriptionResolver;
    private VitamCollection vitamCollection;

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
            collection.vitamCollection
                .setName(prefix + collection.vitamCollection.getClasz().getSimpleName());
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
            if (null != collection.vitamCollection.getCollection()) {
                collection.vitamCollection.getCollection().deleteMany(new Document());
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

    MetadataCollections(final Class<?> clasz) {
        VitamDescriptionLoader vitamDescriptionLoader = new VitamDescriptionLoader(clasz.getSimpleName());
        vitamDescriptionResolver = vitamDescriptionLoader.getVitamDescriptionResolver();
        vitamCollection = VitamCollectionHelper.getCollection(clasz, true, clasz.equals(Unit.class), "", vitamDescriptionResolver);
    }

    public static List<Class<?>> getClasses() {
        List<Class<?>> classes = new ArrayList<>();
        for (MetadataCollections collection : MetadataCollections.values()) {
            classes.add(collection.getClasz());
        }

        return classes;
    }

    /**
     * Initialize the collection
     *
     * @param db database type
     * @param recreate true is as recreate type
     */

    protected void initialize(MongoDatabase db, boolean recreate) {
        vitamCollection.initialize(db, recreate);
    }


    /**
     * Initialize the collection
     *
     * @param esClient ElasticsearchAccessMetadata
     */

    protected void initialize(ElasticsearchAccessMetadata esClient) {
        vitamCollection.initialize(esClient, true);
    }

    /**
     * @return the name of the collection
     */
    public String getName() {
        return vitamCollection.getName();
    }

    /**
     * @return the associated MongoCollection
     */
    @SuppressWarnings("rawtypes")
    public MongoCollection getCollection() {
        return vitamCollection.getCollection();
    }

    /**
     * @return the associated class
     */
    public Class<?> getClasz() {
        return vitamCollection.getClasz();
    }


    public VitamCollection getVitamCollection() {
        return vitamCollection;
    }

    /**
     * @return the associated ES Client
     */
    public ElasticsearchAccessMetadata getEsClient() {
        return (ElasticsearchAccessMetadata) vitamCollection.getEsClient();
    }

    /**
     * @return True if score is to be used
     */
    public boolean useScore() {
        return vitamCollection.isUseScore();
    }

    /**
     * get collection from value.
     *
     * @param collection
     * @return the corresponding MetadataCollections
     */
    public static MetadataCollections getFromValue(String collection) {
        for (MetadataCollections coll : MetadataCollections.values()) {
            if (coll.name().equalsIgnoreCase(collection)) {
                return coll;
            }
        }
        throw new IllegalArgumentException(collection + " is not in enum MetadataCollections.");
    }

    public VitamDescriptionResolver getVitamDescriptionResolver() {
        return vitamDescriptionResolver;
    }
}

