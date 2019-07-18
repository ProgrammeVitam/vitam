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
package fr.gouv.vitam.common.database.collections;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Indexes.hashed;

/**
 * Vitam Collection for mongodb
 */
public class VitamCollection {
    private final Class<?> clasz;
    private final VitamDescriptionLoader descriptionLoader;
    private String name;
    private MongoCollection<?> collection;
    private ElasticsearchAccess esClient;
    public static final String TYPEUNIQUE = "typeunique";
    private final boolean isMultiTenant;
    private final boolean useScore;
    private boolean createIndexByTenant = false;
    /**
     * Used by different parser places (isArray, score)
     */
    private static final ThreadLocal<Boolean> CONTAINS_FINALLY_MATCH =
            new ThreadLocal<Boolean>() {

                @Override
                protected Boolean initialValue() {
                    return false;
                }

            };

    /**
     * @return true if the real query contains match
     */
    public static Boolean containMatch() {
        return CONTAINS_FINALLY_MATCH.get();
    }

    /**
     * @param match if the final query contains match
     */
    public static void setMatch(Boolean match) {
        CONTAINS_FINALLY_MATCH.set(match);
    }

    /**
     * @return the typeunique
     */
    public static String getTypeunique() {
        return TYPEUNIQUE;
    }

    protected VitamCollection(final Class<?> clasz, final boolean isMultiTenant, final boolean useScore, String prefix, VitamDescriptionLoader descriptionLoader) {
        this.clasz = clasz;
        this.descriptionLoader = descriptionLoader;
        name = prefix + clasz.getSimpleName();
        this.isMultiTenant = isMultiTenant;
        this.useScore = useScore;
    }

    /**
     * Initialize the collection
     *
     * @param db       mongodb database
     * @param recreate boolean if recreate the database
     */
    public void initialize(final MongoDatabase db, final boolean recreate) {
        collection = db.getCollection(getName(), getClasz());
        if (recreate) {
            collection.createIndex(hashed(VitamDocument.ID));
        }
    }

    /**
     * Initialize the ES Client
     *
     * @param esClient ElasticsearchAccess ES Client
     */
    public void initialize(final ElasticsearchAccess esClient) {
        this.initialize(esClient, false);
    }

    /**
     * Initialize the ES Client
     *
     * @param esClient ElasticsearchAccess ES Client
     */
    public void initialize(final ElasticsearchAccess esClient, boolean createIndexByTenant) {
        this.esClient = esClient;
        this.createIndexByTenant = createIndexByTenant;
    }

    /**
     * @return the name of the collection
     */
    public String getName() {
        return name;
    }

    @VisibleForTesting
    public void setName(String name) { // NOSONAR
        this.name = name;
    }

    /**
     * @return the associated MongoCollection
     */
    public MongoCollection<?> getCollection() {
        return collection;
    }

    /**
     * @return the associated MongoCollection
     */
    public <T> MongoCollection<T> getTypedCollection() {
        return (MongoCollection<T>) collection;
    }

    /**
     * @return the associated class
     */
    public Class<?> getClasz() {
        return clasz;
    }

    /**
     * @return the esClient
     */
    public ElasticsearchAccess getEsClient() {
        return esClient;
    }

    /**
     * @param claszList Vitam document extended class list
     * @return MongoClientOptions for mongoClient
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MongoClientOptions getMongoClientOptions(List<Class<?>> claszList) {
        if (claszList == null || claszList.isEmpty()) {
            final CodecRegistry codecRegistry =
                    CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());

            return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
        }
        final List<CodecRegistry> codecs = new ArrayList<>();
        for (final Class<?> clasz : claszList) {
            codecs.add(CodecRegistries.fromCodecs(new VitamDocumentCodec(clasz)));
        }
        final CodecRegistry vitamCodecRegistry = CodecRegistries.fromRegistries(codecs);
        final CodecRegistry codecRegistry =
                CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), vitamCodecRegistry);

        return getMongoClientOptions(codecRegistry);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MongoClientOptions getMongoClientOptions() {

        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());

        return getMongoClientOptions(codecRegistry);
    }

    private static MongoClientOptions getMongoClientOptions(CodecRegistry codecRegistry) {
        return MongoClientOptions.builder().codecRegistry(codecRegistry)
                .connectTimeout(VitamConfiguration.getConnectTimeout())
                .minConnectionsPerHost(1).connectionsPerHost(VitamConfiguration.getNumberDbClientThread())
                .maxConnectionIdleTime(VitamConfiguration.getMaxDelayUnusedConnection())
                .threadsAllowedToBlockForConnectionMultiplier(
                        VitamConfiguration.getThreadsAllowedToBlockForConnectionMultipliers())
                .socketKeepAlive(true).socketTimeout(VitamConfiguration.getReadTimeout())
                .writeConcern(WriteConcern.MAJORITY)
                .readConcern(ReadConcern.MAJORITY)
                .build();
    }

    /**
     * @return isMultiTenant value
     */
    public boolean isMultiTenant() {
        return isMultiTenant;
    }

    /**
     * @return the useScore
     */
    public boolean isUseScore() {
        return useScore;
    }

    public boolean isCreateIndexByTenant() {
        return createIndexByTenant;
    }

    public void setCreateIndexByTenant(boolean createIndexByTenant) {
        this.createIndexByTenant = createIndexByTenant;
    }

    public VitamDescriptionLoader getDescriptionLoader() {
        return descriptionLoader;
    }
}
