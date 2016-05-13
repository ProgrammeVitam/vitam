/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Indexes.hashed;

import java.util.Set;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * MongoDb Access base class
 *
 */
public class MongoDbAccess {
    private static final VitamLogger LOGGER =
            VitamLoggerFactory.getInstance(MongoDbAccess.class);

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
	private final MongoDatabase mongoAdmin;

    /**
     * All collections
     *
     */
    public static enum VitamCollections {
        /**
         * Unit Collection
         */
        Cunit(Unit.class),
        /**
         * ObjectGroup Collection
         */
        Cobjectgroup(ObjectGroup.class);

        private final Class<?> clasz;
        private final String name;
        private MongoCollection<?> collection;

        private VitamCollections(final Class<?> clasz) {
            this.clasz = clasz;
            this.name = clasz.getSimpleName();
        }

        protected void initialize(final MongoDatabase db, final boolean recreate) {
            collection = (MongoCollection<?>) db.getCollection(name, getClasz());
            if (recreate) {
                collection.createIndex(hashed(VitamDocument.ID));
            }
        }

        /**
         * 
         * @return the name of the collection
         */
        public String getName() {
            return name;
        }

        /**
         * 
         * @return the associated MongoCollection
         */
        public MongoCollection<?> getCollection() {
            return collection;
        }

		public Class<?> getClasz() {
			return clasz;
		}
    }

    /**
     * LRU Unit cache (limited to VITAM PROJECTION)
     */
    public static final UnitLRU LRU = new UnitLRU();

    /**
     * 
     * @return The MongoCLientOptions to apply to MongoClient
     */
    public static MongoClientOptions getMongoClientOptions() {
    	VitamDocumentCodec<Unit> unitCodec = new VitamDocumentCodec<>(Unit.class);
        VitamDocumentCodec<ObjectGroup> objectGroupCodec = new VitamDocumentCodec<>(ObjectGroup.class);
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                        CodecRegistries.fromCodecs(unitCodec, objectGroupCodec));
        return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
    }
    /**
     * 
     * @param mongoClient
     *            MongoCliet
     * @param dbname
     *            MongoDB database name
     * @param recreate
     *            True to recreate the index
     */
    public MongoDbAccess(MongoClient mongoClient, final String dbname, final boolean recreate) {
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoClient.getDatabase(dbname);
        this.mongoAdmin = mongoClient.getDatabase("admin");
        VitamCollections.Cunit.initialize(mongoDatabase, recreate);
        VitamCollections.Cobjectgroup.initialize(mongoDatabase, recreate);
        // Compute roots
        @SuppressWarnings("unchecked")
		FindIterable<Unit> iterable = (FindIterable<Unit>) VitamCollections.Cunit.getCollection().find(new Document(VitamDocument.UP, null));
        iterable.forEach(new Block<Unit>() {
            @Override
            public void apply(Unit t) {
                t.setRoot();
            }
        });
    }

    /**
     * Close database access
     */
    public final void closeMongoDB() {
        mongoClient.close();
    }

    /**
     * To be called once only when closing the application
     */
    public final void closeFinal() {
    	closeMongoDB();
    }

    /**
     * Drop all data and index from MongoDB
     *
     * @param model
     */
    public static void reset() {
        for (VitamCollections col : VitamCollections.values()) {
            if (col.collection != null) {
                col.collection.drop();
            }
        }
        ensureIndex();
    }

    /**
     * Ensure that all MongoDB database schema are indexed
     */
    public static void ensureIndex() {
        for (VitamCollections col : VitamCollections.values()) {
            if (col.collection != null) {
                col.collection.createIndex(hashed(VitamDocument.ID));
            }
        }
        Unit.addIndexes();
        ObjectGroup.addIndexes();
    }

    /**
     * Remove temporarily the MongoDB Index (import optimization?)
     */
    public static void removeIndexBeforeImport() {
        Unit.dropIndexes();
        ObjectGroup.dropIndexes();
    }

    /**
     * Reset MongoDB Index (import optimization?)
     */
    public static void resetIndexAfterImport() {
        LOGGER.info("Rebuild indexes");
        ensureIndex();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        // get a list of the collections in this database and print them out
        final MongoIterable<String> collectionNames = mongoDatabase.listCollectionNames();
        for (final String s : collectionNames) {
            builder.append(s);
            builder.append('\n');
        }
        for (final VitamCollections coll : VitamCollections.values()) {
            if (coll != null && coll.collection != null) {
                final ListIndexesIterable<Document> list = coll.collection.listIndexes();
                for (final Document dbObject : list) {
                    builder.append(coll.name).append(' ').append(dbObject).append('\n');
                }
            }
        }
        return builder.toString();
    }

    /**
     *
     * @return the current number of Unit
     */
    public static long getUnitSize() {
        return VitamCollections.Cunit.collection.count();
    }

    /**
     *
     * @return the current number of ObjectGroup
     */
    public static long getObjectGroupSize() {
        return VitamCollections.Cobjectgroup.collection.count();
    }

    /**
     * Force flush on disk (MongoDB): should not be used
     */
    protected void flushOnDisk() {
        mongoAdmin.runCommand(new BasicDBObject("fsync", 1).append("async", true)
                .append("lock", false));
    }

    /**
	 * @return the mongoDatabase
	 */
	public MongoDatabase getMongoDatabase() {
		return mongoDatabase;
	}

    /**
     * @param type to use
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type) {
        return new ResultDefault(type);
    }

    /**
     * @param type
     * @param collection
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type, Set<String> collection) {
        return new ResultDefault(type, collection);
    }

}
