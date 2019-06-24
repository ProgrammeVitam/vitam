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
package fr.gouv.vitam.metadata.core.database.collections;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;
import java.util.Map;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessMetadataImpl extends MongoDbAccess {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MongoDbAccess.class);

    private final ElasticsearchAccessMetadata esClient;

    /**
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     * @param esClient Elasticsearch client
     * @param tenants the tenant list
     */

    public MongoDbAccessMetadataImpl(MongoClient mongoClient, String dbname, boolean recreate,
        ElasticsearchAccessMetadata esClient, List<Integer> tenants) {
        super(mongoClient, dbname, recreate);
        this.esClient = esClient;

        MetadataCollections.UNIT.initialize(getMongoDatabase(), recreate);
        MetadataCollections.OBJECTGROUP.initialize(getMongoDatabase(), recreate);

        // init Unit Mapping for ES
        MetadataCollections.UNIT.initialize(this.esClient);

        // init OG Mapping for ES
        MetadataCollections.OBJECTGROUP.initialize(this.esClient);

        for (Integer tenant : tenants) {
            Map<String, String> map =
                MetadataCollections.UNIT.getEsClient().addIndex(MetadataCollections.UNIT, tenant);
            if (map.isEmpty()) {
                throw new RuntimeException(
                    "Index not created for the collection " + MetadataCollections.UNIT.getName() + " and tenant :" +
                        tenant);
            }
            map = MetadataCollections.OBJECTGROUP.getEsClient().addIndex(MetadataCollections.OBJECTGROUP, tenant);
            if (map.isEmpty()) {
                throw new RuntimeException(
                    "Index not created for the collection " + MetadataCollections.OBJECTGROUP.getName() +
                        " and tenant :" + tenant);
            }
        }
    }

    /**
     * @return The MongoCLientOptions to apply to MongoClient
     */
    public static MongoClientOptions getMongoClientOptions() {
        final VitamDocumentCodec<Unit> unitCodec = new VitamDocumentCodec<>(Unit.class);
        final VitamDocumentCodec<ObjectGroup> objectGroupCodec = new VitamDocumentCodec<>(ObjectGroup.class);
        final CodecRegistry codecRegistry = CodecRegistries
            .fromRegistries(CodecRegistries.fromCodecs(unitCodec, objectGroupCodec),
                MongoClient.getDefaultCodecRegistry());
        return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
    }

    @Override
    public String getInfo() {
        final StringBuilder builder = new StringBuilder();
        // get a list of the collections in this database and print them out
        final MongoIterable<String> collectionNames = getMongoDatabase().listCollectionNames();
        for (final String s : collectionNames) {
            builder.append(s);
            builder.append('\n');
        }
        for (final MetadataCollections coll : MetadataCollections.values()) {
            if (coll != null && coll.getCollection() != null) {
                @SuppressWarnings("unchecked")
                final ListIndexesIterable<Document> list =
                    coll.getCollection().listIndexes();
                for (final Document dbObject : list) {
                    builder.append(coll.getName()).append(' ').append(dbObject).append('\n');
                }
            }
        }
        return builder.toString();
    }

    /**
     * @return the current number of Unit
     */
    public static long getUnitSize() {
        return MetadataCollections.UNIT.getCollection().countDocuments();
    }

    /**
     * @return the current number of ObjectGroup
     */
    public static long getObjectGroupSize() {
        return MetadataCollections.OBJECTGROUP.getCollection().countDocuments();
    }

    /**
     * @return the Elasticsearch Acess Metadata client
     */
    public ElasticsearchAccessMetadata getEsClient() {
        return esClient;
    }

    /**
     * Delete Unit metadata by tenant Not check, test feature !
     *
     * @param tenantIds the list of tenants
     * @throws DatabaseException thrown when error on delete
     */
    public void deleteUnitByTenant(Integer... tenantIds) throws DatabaseException {
        final long count = MetadataCollections.UNIT.getCollection().countDocuments();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MetadataCollections.UNIT.getName() + " count before: " + count);
        }
        for (Integer tenantId : tenantIds) {
            if (count > 0) {
                final DeleteResult result =
                    MetadataCollections.UNIT.getCollection().deleteMany(new Document().append("_tenant", tenantId));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MetadataCollections.UNIT.getName() + " result.result.getDeletedCount(): " + result
                        .getDeletedCount());
                }

                esClient.deleteIndex(MetadataCollections.UNIT, tenantId);
                Map<String, String> map = esClient.addIndex(MetadataCollections.UNIT, tenantId);
                if (map.isEmpty()) {
                    throw new RuntimeException(
                        "Index not created for the collection " + MetadataCollections.UNIT.getName() + " and tenant :" +
                            tenantId);
                }
            }
        }
    }

    /**
     * Delete Object Group metadata by Tenant Not check, test feature !
     *
     * @param tenantIds the list of tenants
     * @throws DatabaseException thrown when error on delete
     */
    public void deleteObjectGroupByTenant(Integer... tenantIds) throws DatabaseException {
        final long count = MetadataCollections.OBJECTGROUP.getCollection().countDocuments();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MetadataCollections.OBJECTGROUP.getName() + " count before: " + count);
        }
        for (Integer tenantId : tenantIds) {
            if (count > 0) {
                final DeleteResult result =
                    MetadataCollections.OBJECTGROUP.getCollection()
                        .deleteMany(new Document().append("_tenant", tenantId));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                        MetadataCollections.OBJECTGROUP.getName() + " result.result.getDeletedCount(): " + result
                            .getDeletedCount());
                }
                esClient.deleteIndex(MetadataCollections.OBJECTGROUP, tenantId);
                Map<String, String> map = esClient.addIndex(MetadataCollections.OBJECTGROUP, tenantId);
                if (map.isEmpty()) {
                    throw new RuntimeException(
                        "Index not created for the collection " + MetadataCollections.OBJECTGROUP.getName() +
                            " and tenant :" + tenantId);
                }
            }
        }
    }
}
