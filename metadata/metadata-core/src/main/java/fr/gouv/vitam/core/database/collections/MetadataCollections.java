package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Indexes.hashed;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;

/**
 * Metadata Collection
 */
public enum MetadataCollections {
    /**
     * Unit Collection
     */
    C_UNIT(Unit.class),
    /**
     * ObjectGroup Collection
     */
    C_OBJECTGROUP(ObjectGroup.class);

    private VitamCollection vitamCollection;

    private MetadataCollections(final Class<?> clasz) {
        vitamCollection = VitamCollectionHelper.getCollection(clasz);
    }

    /**
     * Initialize the collection
     *
     * @param db database type
     * @param recreate true is as recreate type
     */
    
    protected void initialize(final MongoDatabase db, final boolean recreate) {
        vitamCollection.initialize(db, recreate);
    }

    /**
     *
     * @return the name of the collection
     */
    protected String getName() {
        return vitamCollection.getName();
    }

    /**
     *
     * @return the associated MongoCollection
     */    
    @SuppressWarnings("rawtypes")
    public MongoCollection getCollection() {
        return vitamCollection.getCollection();
    }

    /**
     *
     * @return the associated class
     */
    public Class<?> getClasz() {
        return vitamCollection.getClasz();
    }         
}

