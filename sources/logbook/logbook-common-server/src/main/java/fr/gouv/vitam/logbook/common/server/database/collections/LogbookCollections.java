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
package fr.gouv.vitam.logbook.common.server.database.collections;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.collections.VitamDescriptionLoader;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;

import java.util.ArrayList;
import java.util.List;


/**
 * All collections
 */
public enum LogbookCollections {
    /**
     * Operation Collection
     */
    OPERATION(LogbookOperation.class),
    /**
     * LifeCycle unit Collection
     */
    LIFECYCLE_UNIT(LogbookLifeCycleUnit.class),
    /**
     * LifeCycle object group Collection
     */
    LIFECYCLE_OBJECTGROUP(LogbookLifeCycleObjectGroup.class),
    /**
     * LifeCycle unit in process
     */
    LIFECYCLE_UNIT_IN_PROCESS(LogbookLifeCycleUnitInProcess.class),
    /**
     * LifeCycle object group in process
     */
    LIFECYCLE_OBJECTGROUP_IN_PROCESS(LogbookLifeCycleObjectGroupInProcess.class);

    private final VitamDescriptionResolver vitamDescriptionResolver;
    private VitamCollection vitamCollection;

    LogbookCollections(final Class<?> clasz) {
        VitamDescriptionLoader vitamDescriptionLoader = new VitamDescriptionLoader(clasz.getSimpleName());
        vitamDescriptionResolver = vitamDescriptionLoader.getVitamDescriptionResolver();
        vitamCollection = VitamCollectionHelper.getCollection(clasz, true, false, "", vitamDescriptionResolver);
    }

    public static List<Class<?>> getClasses() {
        List<Class<?>> classes = new ArrayList<>();
        for (LogbookCollections collection : LogbookCollections.values()) {
            classes.add(collection.getClasz());
        }

        return classes;
    }

    /**
     * Initialize the collection
     *
     * @param db the mongo database
     * @param recreate if needs to be recreated
     */
    protected void initialize(final MongoDatabase db, final boolean recreate) {
        vitamCollection.initialize(db, recreate);
    }

    /**
     * Initialize the collection
     *
     * @param esClient the ElasticsearchAccess
     */

    protected void initialize(final LogbookElasticsearchAccess esClient) {
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
    public MongoCollection getCollection() {
        return vitamCollection.getCollection();
    }

    /**
     * @return the associated VitamCollection
     */
    public VitamCollection getVitamCollection() {
        return vitamCollection;
    }

    /**
     * @return the associated class
     */
    public Class<?> getClasz() {
        return vitamCollection.getClasz();
    }

    /**
     * @return the associated ES Client
     */
    public LogbookElasticsearchAccess getEsClient() {
        return (LogbookElasticsearchAccess) vitamCollection.getEsClient();
    }

    public VitamDescriptionResolver getVitamDescriptionResolver() {
        return vitamDescriptionResolver;
    }

    public ElasticsearchCollections getElasticsearchCollection() {
        switch (this) {
            case OPERATION:
                return ElasticsearchCollections.OPERATION;
            case LIFECYCLE_UNIT:
            case LIFECYCLE_UNIT_IN_PROCESS:
            case LIFECYCLE_OBJECTGROUP:
            case LIFECYCLE_OBJECTGROUP_IN_PROCESS:
                throw new IllegalStateException("No ES index for collection " + this);
            default:
                throw new IllegalStateException("Unknown collection " + this);
        }
    }
}
