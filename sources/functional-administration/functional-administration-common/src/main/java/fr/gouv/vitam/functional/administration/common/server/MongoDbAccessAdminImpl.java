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
package fr.gouv.vitam.functional.administration.common.server;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectToMongoDb;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessAdminImpl extends MongoDbAccess
    implements MongoDbAccessReferential {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbAccessAdminImpl.class);
    private static final ArrayList<String> INDEX_ES_LIST = new ArrayList<String>() {{
        add("FileFormat");
        add("FileRules");
    }
    };

    /**
     * @param mongoClient client of mongo
     * @param dbname name of database
     * @param recreate true if recreate type
     */
    protected MongoDbAccessAdminImpl(MongoClient mongoClient, String dbname, boolean recreate) {
        super(mongoClient, dbname, recreate);
        for (final FunctionalAdminCollections collection : FunctionalAdminCollections.values()) {
            collection.initialize(super.getMongoDatabase(), recreate);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)
        throws ReferentialException {
        final List<VitamDocument> vitamDocumentList = new ArrayList<>();
        try {
            for (final JsonNode objNode : arrayNode) {
                //((ObjectNode) objNode).put("_id", GUIDFactory.newGUID().toString());
                VitamDocument obj = (VitamDocument) JsonHandler.getFromJsonNode(objNode, collection.getClasz());

                vitamDocumentList.add(obj);
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Insert Documents Exception", e);
            throw new ReferentialException(e);
        }
        collection.getCollection().insertMany(vitamDocumentList);
      //FIXME : to be refactor in the collection class (schema)
        if (INDEX_ES_LIST.contains(collection.getName())) {
            insertToElasticsearch(collection, vitamDocumentList);
        }
    }

    /**
     * @param collection
     * @param vitamDocumentList
     * @throws ReferentialException if error occurs
     */
    private void insertToElasticsearch(FunctionalAdminCollections collection, List<VitamDocument> vitamDocumentList) throws ReferentialException {
        Map<String, String> mapIdJson = new HashMap<>();
        for (VitamDocument document : vitamDocumentList) {
            String id = document.getId();
            document.remove(FunctionalAdminCollections.ID);
            final String mongoJson = document.toJson(new JsonWriterSettings(JsonMode.STRICT));
            document.clear();
            final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();
            mapIdJson.put(id, esJson);
        }
        final BulkResponse bulkResponse = collection.getEsClient().addEntryIndexes(collection, mapIdJson);
        if (bulkResponse.hasFailures()) {
            throw new ReferentialException("Index Elasticsearch has errors");
        }
    }

    // Not check, test feature !
    @Override
    public void deleteCollection(FunctionalAdminCollections collection) throws DatabaseException, ReferentialException {
        final long count = collection.getCollection().count();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            final DeleteResult result = collection.getCollection().deleteMany(new Document());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getDeletedCount());
            }
            if (result.getDeletedCount() != count) {
                throw new DatabaseException(String.format("%s: Delete %s from %s elements", collection.getName(), result
                    .getDeletedCount(), count));
            }
            if (INDEX_ES_LIST.contains(collection.getName())) {
                collection.getEsClient().deleteIndex(collection);
                collection.getEsClient().addIndex(collection);
            }
        }
    }

    @Override
    public VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection)
        throws ReferentialException {
        return (VitamDocument<?>) collection.getCollection().find(eq(VitamDocument.ID, id)).first();
    }

    @Override
    public MongoCursor<?> findDocuments(JsonNode select, FunctionalAdminCollections collection)
        throws ReferentialException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            parser.parse(select);
            final SelectToMongodb requestToMongodb = new SelectToMongodb(parser);
            if (requestToMongodb.hasFullTextQuery() && INDEX_ES_LIST.contains(collection.getName())) {
                return findDocumentsElasticsearch(collection, parser);
            } else {
                return selectMongoDbExecute(collection, parser);
            }
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
    }

    /**
     * @param collection
     * @param parser
     * @return
     * @throws InvalidParseOperationException 
     * @throws ReferentialException 
     * @throws InvalidCreateOperationException 
     */
    private MongoCursor<?> findDocumentsElasticsearch(FunctionalAdminCollections collection,
        SelectParserSingle parser) throws InvalidParseOperationException, ReferentialException, InvalidCreateOperationException {
        final SelectToMongodb requestToMongodb = new SelectToMongodb(parser);
        QueryBuilder query = QueryToElasticsearch.getCommand(requestToMongodb.getNthQuery(0));
        SearchResponse elasticSearchResponse = 
            collection.getEsClient().search(collection, query, null);
        if (elasticSearchResponse.status() != RestStatus.OK) {
            return null;
        }
        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits() == 0) {
            return null;
        }
        final BooleanQuery newQuery = or();
        final Iterator<SearchHit> iterator = hits.iterator();
        // get document with Elasticsearch then create a new request to mongodb with unique object's attribute   
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            final Map<String, Object> src = hit.getSource();
            LOGGER.error("findDocumentsElasticsearch result" + src.toString());
            //FIXME : to be refactor in the collection class (schema)
            if (FunctionalAdminCollections.FORMATS.equals(collection)) {
                newQuery.add(QueryHelper.eq(FileFormat.PUID, src.get(FileFormat.PUID).toString()));
            }
            if (FunctionalAdminCollections.RULES.equals(collection)) {
                newQuery.add(QueryHelper.eq(FileRules.RULEID, src.get(FileRules.RULEID).toString()));
            }
        }
        Select newSelectRequest = new Select();
        newSelectRequest.setQuery(newQuery);
        parser.parse(newSelectRequest.getFinalSelect());
        return selectMongoDbExecute(collection, parser);
    }

    /**
     * @param collection
     * @param parser
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException when query is not correct
     */
    private MongoCursor<?> selectMongoDbExecute(final FunctionalAdminCollections collection, SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongoDb selectToMongoDb = new SelectToMongoDb(parser);
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();        
        Bson initialCondition = QueryToMongodb.getCommand(selectToMongoDb.getSelect().getQuery());
        // FIXME - add a method to VitamDocument to specify if the tenant should be filtered for collection.
        // if the collection should not be filtered, then the method should be overridden
        Bson condition = and(initialCondition, eq(VitamDocument.TENANT_ID, tenantId));        
        if (FunctionalAdminCollections.FORMATS.equals(collection)) {
            condition = initialCondition;
        }
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset = selectToMongoDb.getFinalOffset();
        final int limit = selectToMongoDb.getFinalLimit();
        FindIterable<?> find = collection.getCollection().find(condition).skip(offset);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit > 0) {
            find = find.limit(limit);
        }
        return find.iterator();
    }

    @Override
    public void updateDocumentByMap(Map<String, Object> map, JsonNode objNode,
        FunctionalAdminCollections collection, UPDATEACTION operator)
        throws ReferentialException {
        final BasicDBObject incQuery = new BasicDBObject();
        final BasicDBObject updateFields = new BasicDBObject();
        for (final Entry<String, Object> entry : map.entrySet()) {
            updateFields.append(entry.getKey(), entry.getValue());
        }
        incQuery.append(operator.exactToken(), updateFields);
        final UpdateResult result = collection.getCollection().updateOne(eq(AccessionRegisterSummary.ORIGINATING_AGENCY,
            objNode.get(AccessionRegisterSummary.ORIGINATING_AGENCY).textValue()), incQuery);
        if (result.getModifiedCount() == 0 && result.getMatchedCount() == 0) {
            throw new ReferentialException("Document is not updated");
        }
    }

    @Override
    public void insertDocument(JsonNode json, FunctionalAdminCollections collection) throws ReferentialException {
        try {
            final VitamDocument obj = (VitamDocument) JsonHandler.getFromJsonNode(json, collection.getClasz());
            collection.getCollection().insertOne(obj);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Documents not conformed Exception", e);
            throw new ReferentialException(e);
        }
    }

}
