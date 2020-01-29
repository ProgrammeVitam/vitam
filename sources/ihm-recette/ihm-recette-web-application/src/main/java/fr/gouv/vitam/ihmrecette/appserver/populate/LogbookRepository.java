/*
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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Insert into logbook in bulk mode
 */
public class LogbookRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookRepository.class);

    private MongoDatabase mongoDb;
    private ObjectMapper objectMapper;

    private Map<VitamDataType, MongoCollection<Document>> mongoCollections = new HashMap<>();

    public LogbookRepository(MongoDatabase mongoDb) {
        this.mongoDb = mongoDb;
        this.objectMapper = UnitGotMapper.buildObjectMapper();

    }


    /**
     * Store a LogbookLifecycleUnit in database
     *
     * @param tenant tenant identifier
     * @param unitGotList data list of (unit,got) to store
     */
    public void storeLogbookLifecycleUnit(int tenant, List<UnitGotModel> unitGotList) {
        List<Document> docs =
            unitGotList.stream().filter(unitGot -> unitGot.getLogbookLifecycleUnit() != null).map(unitGot ->
                getDocument(unitGot.getLogbookLifecycleUnit())).collect(Collectors.toList());

        if (!docs.isEmpty()) {
            this.store(docs, VitamDataType.LFC_UNIT);
        }
    }


    /**
     * Store a LifeCycleObjectGroup in database
     *
     * @param unitGotList data list of (unit,got) to store
     */
    public void storeLogbookLifeCycleObjectGroup(List<UnitGotModel> unitGotList) {
        List<Document> docs =
            unitGotList.stream().filter(unitGot -> unitGot.getLogbookLifeCycleObjectGroup() != null).map(unitGot ->
                getDocument(unitGot.getLogbookLifeCycleObjectGroup())).collect(Collectors.toList());

        if (!docs.isEmpty()) {
            this.store(docs, VitamDataType.LFC_GOT);
        }
    }



    /**
     * Store a document list
     *
     * @param documents documents to store and index
     * @param vitamDataType dataType of documents
     */
    private void store(List<Document> documents, VitamDataType vitamDataType) {
        storeDocuments(documents, vitamDataType);
    }


    /**
     * Store a list of documents in db
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
     * Get a collection
     *
     * @param vitamDataType to fetch a collection for
     * @return MongoCollection
     */
    private MongoCollection<Document> getCollection(VitamDataType vitamDataType) {
        return mongoCollections.getOrDefault(vitamDataType,
            mongoDb.getCollection(vitamDataType.getCollectionName()));
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
    public Map<String, JsonNode> findRawLfcsByIds(List<String> ids, VitamDataType vitamDataType) {

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
