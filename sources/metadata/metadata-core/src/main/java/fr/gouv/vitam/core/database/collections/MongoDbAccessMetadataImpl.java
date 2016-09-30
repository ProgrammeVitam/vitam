/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Indexes.hashed;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoIterable;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessMetadataImpl extends MongoDbAccess{

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MongoDbAccess.class);

    /**
     *
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     */
    
    public MongoDbAccessMetadataImpl(MongoClient mongoClient, String dbname, boolean recreate, ElasticsearchAccessMetadata esClient) {
        super(mongoClient, dbname, recreate);    
        
        MetadataCollections.C_UNIT.initialize(this.getMongoDatabase(), recreate);
        MetadataCollections.C_OBJECTGROUP.initialize(this.getMongoDatabase(), recreate);
        // Compute roots
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable =
            (FindIterable<Unit>) MetadataCollections.C_UNIT.getCollection().find(new Document(MetadataDocument.UP, null));
        iterable.forEach(new Block<Unit>() {
            @Override
            public void apply(Unit t) {
                t.setRoot();
            }
        });
        
        // init Unit Mapping for ES
        MetadataCollections.C_UNIT.initialize(esClient);
        MetadataCollections.C_UNIT.getEsClient().addIndex(MetadataCollections.C_UNIT);
    }

    /**
     *
     * @return The MongoCLientOptions to apply to MongoClient
     */
    public static MongoClientOptions getMongoClientOptions() {
        final VitamDocumentCodec<Unit> unitCodec = new VitamDocumentCodec<>(Unit.class);
        final VitamDocumentCodec<ObjectGroup> objectGroupCodec = new VitamDocumentCodec<>(ObjectGroup.class);
        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(unitCodec, objectGroupCodec));
        return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
    }


    /**
     * Ensure that all MongoDB database schema are indexed
     */
    public static void ensureIndex() {
        for (final MetadataCollections col : MetadataCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().createIndex(hashed(MetadataDocument.ID));
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
        final MongoIterable<String> collectionNames = this.getMongoDatabase().listCollectionNames();
        for (final String s : collectionNames) {
            builder.append(s);
            builder.append('\n');
        }
        for (final MetadataCollections coll : MetadataCollections.values()) {
            if (coll != null && coll.getCollection() != null) {
                @SuppressWarnings("unchecked")
                final ListIndexesIterable<Document> list = coll.getCollection().listIndexes();
                for (final Document dbObject : list) {
                    builder.append(coll.getName()).append(' ').append(dbObject).append('\n');
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
        return MetadataCollections.C_UNIT.getCollection().count();
    }

    /**
     *
     * @return the current number of ObjectGroup
     */
    public static long getObjectGroupSize() {
        return MetadataCollections.C_OBJECTGROUP.getCollection().count();
    }

    /**
     * Force flush on disk (MongoDB): should not be used
     */
    protected void flushOnDisk() {
        this.getMongoAdmin().runCommand(new BasicDBObject("fsync", 1).append("async", true)
            .append("lock", false));
    }

    
}
