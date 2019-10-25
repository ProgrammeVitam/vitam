/*
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
 */
package fr.gouv.vitam.common.database.server.elasticsearch;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.index.model.IndexKO;
import fr.gouv.vitam.common.database.index.model.IndexOK;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * IndexationHelper useful method for indexation
 */
public class IndexationHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexationHelper.class);
    public static final String TYPEUNIQUE = VitamCollection.getTypeunique();

    private static final IndexationHelper instance = new IndexationHelper();

    public static IndexationHelper getInstance() {
        return instance;
    }

    /**
     * reindex a collection on a tenant list with a esmapping file
     *
     * @param collection the collection to be reindexed
     * @param esClient the elastic client to be used to reindex
     * @param tenants the tenant list on which to reindex
     * @param mapping the es mapping as a string
     * @return the result of the reindexation as a IndexationResult object
     * @throws IOException
     */
    public IndexationResult reindex(MongoCollection<Document> collection, String collectionName,
        ElasticsearchAccess esClient,
        List<Integer> tenants, InputStream mapping)
        throws IOException {
        VitamMongoRepository vitamMongoRepository = new VitamMongoRepository(collection);
        IndexationResult indexationResult = new IndexationResult();
        String collectionMapping = ElasticsearchUtil.transferJsonToMapping(mapping);
        String currentIndexWithoutAlias = null;
        Integer currentTenant = null;
        List<IndexOK> indexesOk = new ArrayList<>();
        try {
            if (tenants != null && !tenants.isEmpty()) {
                LOGGER
                    .debug("reindex collectionName : %s for Tenant : %s", collectionName,
                        String.join(",", tenants.toString()));
                for (Integer tenant : tenants) {
                    currentTenant = tenant;
                    // Create ElasticSearch new index for a given collection
                    currentIndexWithoutAlias = esClient
                        .createIndexWithoutAlias(collectionName.toLowerCase(), collectionMapping, TYPEUNIQUE, tenant);

                    MongoCursor<Document> cursor =
                        vitamMongoRepository.findDocuments(VitamConfiguration.getMaxElasticsearchBulk(), tenant)
                            .iterator();
                    // Create repository for the given indexName
                    VitamElasticsearchRepository vitamElasticsearchRepository =
                        new VitamElasticsearchRepository(esClient.getClient(), currentIndexWithoutAlias,
                            false);
                    List<Document> documents = getDocuments(cursor);
                    // Reindex document with bulk
                    while (!documents.isEmpty()) {
                        // Reindex document with bulk
                        if (collectionName.toLowerCase().equals(ElasticsearchCollections.OPERATION.getIndexName())) {
                            vitamElasticsearchRepository.saveLogbook(documents);
                        } else if (collectionName.toLowerCase()
                            .equals(ElasticsearchCollections.UNIT.getIndexName())) {
                            vitamElasticsearchRepository.saveUnit(documents);
                        } else {
                            vitamElasticsearchRepository.save(documents);
                        }

                        documents = getDocuments(cursor);
                    }
                    createIndexationResult(collectionName, indexationResult, currentIndexWithoutAlias, currentTenant,
                        indexesOk);
                }
            } else {
                currentIndexWithoutAlias = esClient
                    .createIndexWithoutAlias(collectionName, collectionMapping, TYPEUNIQUE, null);

                FindIterable<Document> iterable =
                    vitamMongoRepository.findDocuments(VitamConfiguration.getMaxElasticsearchBulk());
                // Create repository for the given indexName
                VitamElasticsearchRepository vitamElasticsearchRepository =
                    new VitamElasticsearchRepository(esClient.getClient(), currentIndexWithoutAlias,
                        false);
                MongoCursor<Document> cursor;
                cursor = iterable.iterator();
                List<Document> documents = getDocuments(cursor);
                // Reindex document with bulk
                while (!documents.isEmpty()) {
                    vitamElasticsearchRepository.save(documents);
                    documents = getDocuments(cursor);
                }
                createIndexationResult(collectionName, indexationResult, currentIndexWithoutAlias, null,
                    indexesOk);
            }
        } catch (DatabaseException e) {
            LOGGER.error("DatabaseException ", e);
            final String message = e.getMessage();
            List<IndexKO> indexesKo = new ArrayList<>();
            if (currentTenant != null) {
                indexesKo.add(new IndexKO(currentIndexWithoutAlias, currentTenant, message));
            } else {
                indexesKo.add(new IndexKO(currentIndexWithoutAlias, message));
            }
            indexationResult.setIndexOK(indexesOk);
            indexationResult.setIndexKO(indexesKo);
            indexationResult.setCollectionName(collectionName);
        }
        return indexationResult;
    }

    /**
     * switch index, attach a new index to an existing alias
     *
     * @param aliasName the name of the alias
     * @param newIndex the new index name to switch on
     * @param esClient the elastic client
     * @throws DatabaseException if an error occurs
     */
    public void switchIndex(String aliasName, String newIndex, ElasticsearchAccess esClient)
        throws DatabaseException {
        esClient.switchIndex(aliasName, newIndex);
    }

    private void createIndexationResult(String collectionName, IndexationResult indexationResult,
        String currentIndexWithoutAlias, Integer currentTenant, List<IndexOK> indexesOk) {
        if (currentTenant != null) {
            indexesOk.add(new IndexOK(currentIndexWithoutAlias, currentTenant));
        } else {
            indexesOk.add(new IndexOK(currentIndexWithoutAlias));
        }
        indexationResult.setIndexOK(indexesOk);
        indexationResult.setCollectionName(collectionName);
    }

    private List<Document> getDocuments(MongoCursor<Document> cursor) {
        int cpt = 0;
        List<Document> documents = new ArrayList<>();
        while (cpt < VitamConfiguration.getMaxElasticsearchBulk() && cursor.hasNext()) {
            documents.add(cursor.next());
            cpt++;
        }
        return documents;
    }

    /**
     * Get KO Result for reindexation orders
     *
     * @param indexParameters the index parameter
     * @param message the message to be added
     * @return the final result as an IndexationResult object
     */
    public IndexationResult getFullKOResult(IndexParameters indexParameters, String message) {
        IndexationResult result = new IndexationResult();
        List<IndexKO> koList = new ArrayList<>();
        if (indexParameters.getTenants() != null) {
            koList = new ArrayList<>(indexParameters.getTenants().size());
            for (Integer tenant : indexParameters.getTenants()) {
                koList.add(new IndexKO(indexParameters.getCollectionName() + "_" + tenant + "_*",
                    tenant, message));
            }
        } else {
            koList.add(new IndexKO(indexParameters.getCollectionName() + "_*", message));
        }
        result.setIndexKO(koList);
        result.setCollectionName(indexParameters.getCollectionName());
        return result;
    }

    /**
     * Get KO Result for switching order
     *
     * @param switchIndexParameters the switch index parameter
     * @param message the message to be added
     * @return the final result as an IndexationResult object
     */
    public IndexationResult getKOResult(SwitchIndexParameters switchIndexParameters, String message) {
        IndexationResult result = new IndexationResult();
        List<IndexKO> koList = new ArrayList<>();
        koList.add(new IndexKO(switchIndexParameters.getAlias() + "_*", message));
        result.setIndexKO(koList);
        result.setCollectionName(switchIndexParameters.getAlias());
        return result;
    }

}
