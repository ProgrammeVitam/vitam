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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * insert into metadata in bulk mode
 */
public class MetadataRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataRepository.class);

    private MongoDatabase metadataDb;
    private TransportClient transportClient;
    private ObjectMapper objectMapper;

    private Map<MetadataType, MongoCollection<Document>> mongoCollections = new HashMap<>();

    public MetadataRepository(MongoDatabase metadataDb, TransportClient transportClient) {
        this.metadataDb = metadataDb;
        this.transportClient = transportClient;
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
        FindIterable<Document> models = this.getCollection(MetadataType.UNIT).find(eq("_id", rootId));

        Document first = models.first();
        if (first == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(this.objectMapper.readValue(first.toJson(), UnitModel.class));
        } catch (final IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Store unit and got in database and es
     *
     * @param tenant      tenant identifier
     * @param storeInDb   if true documents will be saved in DB
     * @param indexInEs   if true documents will be indexed in ES
     * @param unitGotList data list of (unit,got) to store
     */
    public void store(int tenant, List<UnitGotModel> unitGotList, boolean storeInDb, boolean indexInEs) {
        List<Document> gots = unitGotList.stream().filter(unitGot -> unitGot.getGot() != null).map(unitGot ->
                getDocument(unitGot.getGot())).collect(Collectors.toList());

        if (!gots.isEmpty()) {
            this.storeAndIndex(tenant, gots, MetadataType.GOT, storeInDb, indexInEs);
        }

        List<Document> units = unitGotList.stream().map(unitGot ->
                getDocument(unitGot.getUnit())).collect(Collectors.toList());
        this.storeAndIndex(tenant, units, MetadataType.UNIT, storeInDb, indexInEs);
    }

    /**
     * Store and index a document list 
     * 
     * @param tenant        tenant identifier
     * @param documents     documents to store and index
     * @param metadataType  dataType of documents
     * @param storeInDb     if true documents will be saved in DB
     * @param indexInEs     if true documents will be indexed in ES
     */
    private void storeAndIndex(int tenant, List<Document> documents, MetadataType metadataType, 
                               boolean storeInDb, boolean indexInEs){
        if(storeInDb){
            storeDocuments(documents, metadataType);
        }
        if(indexInEs) {
            indexDocuments(documents, metadataType, tenant);
        }
    }

    /**
     * store a list of documents in db
     *
     * @param documents    to store
     * @param metadataType of the documents to store
     */
    private void storeDocuments(List<Document> documents, MetadataType metadataType) {
        List<InsertOneModel<Document>> collect =
                documents.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = this.getCollection(metadataType).bulkWrite(collect);
        LOGGER.info("{}", bulkWriteResult.getInsertedCount());
    }

    /**
     * index a list of documents
     *
     * @param documents    to index
     * @param metadataType of the documents
     * @param tenant       related tenant
     */
    private void indexDocuments(List<Document> documents, MetadataType metadataType, int tenant) {
        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();

        documents.forEach(document -> {
            String id = (String) document.remove("_id");
            String source = document.toJson();
            bulkRequestBuilder
                    .add(transportClient.prepareIndex(metadataType.getIndexName(tenant), "type_unique", id)
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
        for (MetadataType mdt : MetadataType.values()) {
            MongoCollection<Document> collection = metadataDb.getCollection(mdt.getCollectionName());
            mongoCollections.put(mdt, collection);
        }
    }

    /**
     * Get a collection
     *
     * @param metadataType to fetch a collection for
     * @return MongoCollection
     */
    private MongoCollection<Document> getCollection(MetadataType metadataType) {
        return mongoCollections.getOrDefault(metadataType,
                metadataDb.getCollection(MetadataType.UNIT.getCollectionName()));
    }

    /**
     * Create a document
     *
     * @param obj to convert
     * @return new document
     */
    private Document getDocument(Object obj) {
        String source = null;
        try {
            source = objectMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        return Document.parse(source);
    }

}
