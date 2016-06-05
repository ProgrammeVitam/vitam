package fr.gouv.vitam.logbook.common.server.database.collections;

import static com.mongodb.client.model.Indexes.hashed;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * All collections
 *
 */
enum LogbookCollections {
    /**
     * Operation Collection
     */
    OPERATION(LogbookOperation.class),
    /**
     * LifeCycle Collection
     */
    LIFECYCLE(LogbookLifeCycle.class);

    private final Class<?> clasz;
    private final String name;
    private MongoCollection<?> collection;

    private LogbookCollections(final Class<?> clasz) {
        this.clasz = clasz;
        name = clasz.getSimpleName();
    }

    /**
     * Initialize the collection
     *
     * @param db
     * @param recreate
     */
    protected void initialize(final MongoDatabase db, final boolean recreate) {
        collection = db.getCollection(name, getClasz());
        if (recreate) {
            collection.createIndex(hashed(LogbookDocument.ID));
        }
    }

    /**
     *
     * @return the name of the collection
     */
    protected String getName() {
        return name;
    }

    /**
     *
     * @return the associated MongoCollection
     */
    @SuppressWarnings("rawtypes")
    protected MongoCollection getCollection() {
        return collection;
    }

    /**
     *
     * @return the casted MongoCollection
     */
    @SuppressWarnings("unchecked")
    protected static final MongoCollection<LogbookOperation> getOperationCollection() {
        return (MongoCollection<LogbookOperation>) OPERATION.collection;
    }

    /**
     *
     * @return the casted MongoCollection
     */
    @SuppressWarnings("unchecked")
    protected static final MongoCollection<LogbookLifeCycle> getLifeCycleCollection() {
        return (MongoCollection<LogbookLifeCycle>) LIFECYCLE.collection;
    }

    /**
     *
     * @return the associated class
     */
    protected Class<?> getClasz() {
        return clasz;
    }
}
