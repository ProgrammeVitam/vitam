/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.bson.Document;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * insert into metadata in bulk mode
 */
public class MetadataRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataRepository.class);

    public static final String STRATEGY_ID = "default";

    private MongoDatabase metadataDb;
    private Client transportClient;
    private ObjectMapper objectMapper;
    private final StoragePopulateImpl storagePopulateService;
    private final ExecutorService storageExecutorService
        = Executors.newFixedThreadPool(16, VitamThreadFactory.getInstance());

    private Map<VitamDataType, MongoCollection<Document>> mongoCollections = new HashMap<>();

    public MetadataRepository(MongoDatabase metadataDb, Client transportClient,
        StoragePopulateImpl storagePopulateService) {
        this.metadataDb = metadataDb;
        this.transportClient = transportClient;
        this.storagePopulateService = storagePopulateService;
        this.objectMapper = UnitGotMapper.buildObjectMapper();

        // init collections for available metadataTypes
        initCollections();
    }

    /**
     * Find a unit by id
     *
     * @param rootId unitId to fetch
     * @return UnitModel if unit is found
     */
    public Optional<UnitModel> findUnitById(String rootId) {
        FindIterable<Document> models = this.getCollection(VitamDataType.UNIT).find(eq("_id", rootId));

        Document first = models.first();
        if (first == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(this.objectMapper.readValue(BsonHelper.stringify(first), UnitModel.class));
        } catch (final IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Store unit and got in database and es
     *
     * @param tenant tenant identifier
     * @param storeInDb if true documents will be saved in DB
     * @param indexInEs if true documents will be indexed in ES
     * @param unitGotList data list of (unit,got) to store
     */
    public void store(int tenant, List<UnitGotModel> unitGotList, boolean storeInDb, boolean indexInEs) {
        List<Document> gots = unitGotList.stream().filter(unitGot -> unitGot.getGot() != null).map(unitGot ->
            getDocument(unitGot.getGot())).collect(Collectors.toList());

        if (!gots.isEmpty()) {
            this.storeAndIndex(tenant, gots, VitamDataType.GOT, storeInDb, indexInEs);

            if (unitGotList.get(0).getObjectSize() != 0) {
                LOGGER.debug("######## Write object" + unitGotList.get(0).getObjectSize());
                storeObjects(unitGotList);
            }
        }

        List<Document> units = unitGotList.stream().map(unitGot ->
            getDocument(unitGot.getUnit())).collect(Collectors.toList());
        this.storeAndIndex(tenant, units, VitamDataType.UNIT, storeInDb, indexInEs);
    }

    private void storeObjects(List<UnitGotModel> unitGotList) {

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        for (UnitGotModel unitGotModel : unitGotList) {
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            try {
                this.storagePopulateService.storeData(
                    STRATEGY_ID,
                    unitGotModel.getGot().getQualifiers().get(0).getVersions().get(0).getId(),
                    PopulateService.POPULATE_FILE,
                    DataCategory.OBJECT, unitGotModel.getUnit().getTenant()
                );
            } catch (StorageException | FileNotFoundException e) {
                LOGGER.error("Can not store object of " + unitGotModel.getUnit().getId());
            }}, storageExecutorService);

            completableFutures.add(completableFuture);
        }

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Store and index a document list
     *
     * @param tenant tenant identifier
     * @param documents documents to store and index
     * @param vitamDataType dataType of documents
     * @param storeInDb if true documents will be saved in DB
     * @param indexInEs if true documents will be indexed in ES
     */
    private void storeAndIndex(int tenant, List<Document> documents, VitamDataType vitamDataType,
        boolean storeInDb, boolean indexInEs) {
        if (storeInDb) {
            storeDocuments(documents, vitamDataType);
        }
        if (indexInEs) {
            indexDocuments(documents, vitamDataType, tenant);
        }
    }

    /**
     * store a list of documents in db
     *
     * @param documents to store
     * @param vitamDataType of the documents to store
     */
    private void storeDocuments(List<Document> documents, VitamDataType vitamDataType) {
        List<InsertOneModel<Document>> collect =
            documents.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);

        BulkWriteResult bulkWriteResult = this.getCollection(vitamDataType).bulkWrite(collect, options);
        LOGGER.info("{}", bulkWriteResult.getInsertedCount());
    }

    /**
     * index a list of documents
     *
     * @param documents to index
     * @param vitamDataType of the documents
     * @param tenant related tenant
     */
    private void indexDocuments(List<Document> documents, VitamDataType vitamDataType, int tenant) {
        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();

        documents.forEach(document -> {
            String id = (String) document.remove("_id");
            String source = BsonHelper.stringify(document);
            bulkRequestBuilder
                .add(transportClient.prepareIndex(vitamDataType.getIndexName(tenant), VitamCollection.TYPEUNIQUE, id)
                    .setSource(source, XContentType.JSON));
        });

        BulkResponse bulkRes = bulkRequestBuilder.execute().actionGet();

        LOGGER.info("{}", bulkRes.getItems().length);
        if (bulkRes.hasFailures()) {
            LOGGER.error("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
        }
    }

    /**
     * init collection's list for available metadataTypes
     */
    private void initCollections() {
        for (VitamDataType mdt : VitamDataType.values()) {
            MongoCollection<Document> collection = metadataDb.getCollection(mdt.getCollectionName());
            mongoCollections.put(mdt, collection);
        }
    }

    /**
     * Get a collection
     *
     * @param vitamDataType to fetch a collection for
     * @return MongoCollection
     */
    private MongoCollection<Document> getCollection(VitamDataType vitamDataType) {
        return mongoCollections.getOrDefault(vitamDataType,
            metadataDb.getCollection(VitamDataType.UNIT.getCollectionName()));
    }

    /**
     * Create a document
     *
     * @param obj to convert
     * @return new document
     */
    private Document getDocument(Object obj) {
        String source;
        try {
            source = objectMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        return Document.parse(source);
    }

    /**
     * Find raw metadata by ids
     * @param ids the document ids
     * @param vitamDataType the metadata type
     */
    public Map<String, JsonNode> findRawMetadataByIds(List<String> ids, VitamDataType vitamDataType) {
        Map<String, JsonNode> result = new HashMap<>();
        this.getCollection(vitamDataType).find(
            Filters.in("_id", ids)
        ).forEach((Consumer<? super Document>) i -> {
            try {
                result.put(i.getString("_id"), JsonHandler.getFromString(BsonHelper.stringify(i)) );
            } catch (InvalidParseOperationException e) {
                throw new RuntimeException("Could not deserialize json", e);
            }
        });

        return result;
    }
}
