/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.common.database.server;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getConcernedDiffLines;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getUnifiedDiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.builder.request.single.RequestSingle;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectToMongoDb;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.UpdateToMongodb;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;

/**
 * This class execute all request single in Vitam
 */
public class DbRequestSingle {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestSingle.class);

    private final VitamCollection vitamCollection;

    /**
     * Constructor with VitamCollection
     * @param collection
     */
    public DbRequestSingle(VitamCollection collection) {
        this.vitamCollection = collection;
    }

    /**
     * execute all request 
     * 
     * @param request
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     */
    public DbRequestResult execute(RequestSingle request) throws InvalidParseOperationException, DatabaseException, InvalidCreateOperationException {
        if (request instanceof Insert) {
            ArrayNode data = ((Insert) request).getData();
            return insertDocuments(data);
        } else if (request instanceof Select) {
            return findDocuments(((Select) request).getFinalSelect());
        } else if (request instanceof Update) {
            return updateDocuments(((Update) request).getFinalUpdate());
        } else if (request instanceof Delete) {
            return deleteDocuments(((Delete) request).getFinalDelete());
        }
        return new DbRequestResult();
    }



    /**
     * @param arrayNode
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     */
    @SuppressWarnings("unchecked")
    private DbRequestResult insertDocuments(ArrayNode arrayNode) throws InvalidParseOperationException, DatabaseException {
        final List<VitamDocument<?>> vitamDocumentList = new ArrayList<VitamDocument<?>>();
        for (final JsonNode objNode : arrayNode) {
            VitamDocument<?> obj = (VitamDocument<?>) JsonHandler.getFromJsonNode(objNode, vitamCollection.getClasz());
            vitamDocumentList.add(obj);
        }
        MongoCollection<VitamDocument<?>> collection = (MongoCollection<VitamDocument<?>>) vitamCollection.getCollection();
        collection.insertMany(vitamDocumentList);
        insertToElasticsearch(vitamDocumentList);
        return new DbRequestResult().setCount(vitamDocumentList.size());
    }

    /**
     * @param vitamDocumentList
     * @param vitamDocumentList
     * @throws DatabaseException 
     * @throws DatabaseException if error occurs
     */
    private void insertToElasticsearch(List<VitamDocument<?>> vitamDocumentList) throws DatabaseException {
        if (vitamCollection.getEsClient() == null) {
            return;
        }
        Map<String, String> mapIdJson = new HashMap<>();
        for (VitamDocument<?> document : vitamDocumentList) {
            String id = document.getString(VitamDocument.ID);
            document.remove(VitamDocument.ID);
            final String mongoJson = document.toJson(new JsonWriterSettings(JsonMode.STRICT));
            document.clear();
            final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();
            mapIdJson.put(id, esJson);
        }
        final BulkResponse bulkResponse = addEntryIndexes(mapIdJson);
        if (bulkResponse.hasFailures()) {
            LOGGER.error("Insert Documents Exception");
            throw new DatabaseException("Insert Document Exception");
        }
    }


    /**
     * Add a set of entries in the ElasticSearch index. <br>
     * Used in reload from scratch.
     *
     * @param mapIdJson
     * @return the listener on bulk insert
     */
    private final BulkResponse addEntryIndexes(final Map<String, String> mapIdJson) {

        Client client = vitamCollection.getEsClient().getClient();
        final BulkRequestBuilder bulkRequest = client.prepareBulk();

        // either use client#prepare, or use Requests# to directly build index/delete requests
        for (final Entry<String, String> val : mapIdJson.entrySet()) {
            bulkRequest.setRefresh(true).add(client.prepareIndex(vitamCollection.getName().toLowerCase(), vitamCollection.getTypeunique(),
                val.getKey()).setSource(val.getValue()));
        }
        return bulkRequest.execute().actionGet(); 
    }

    private DbRequestResult findDocuments(JsonNode select) throws DatabaseException {
        MongoCursor<VitamDocument<?>> cursor = search(select);
        return new DbRequestResult().setCursor(cursor);
    }

    private MongoCursor<VitamDocument<?>> search(JsonNode select) throws DatabaseException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
            parser.parse(select);
            final SelectToMongodb requestToMongodb = new SelectToMongodb(parser);
            if (requestToMongodb.hasFullTextQuery() && vitamCollection.getEsClient() != null) {
                return findDocumentsElasticsearch(parser);
            } else {
                return selectMongoDbExecute(parser);
            }
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new DatabaseException(e);
        }
    }

    /**
     * @param parser
     * @return
     * @throws InvalidParseOperationException 
     * @throws DatabaseException 
     * @throws InvalidCreateOperationException 
     * @throws DatabaseException 
     */
    private MongoCursor<VitamDocument<?>> findDocumentsElasticsearch(SelectParserSingle parser) throws InvalidParseOperationException, InvalidCreateOperationException, DatabaseException {
        final SelectToMongodb requestToMongodb = new SelectToMongodb(parser);
        QueryBuilder query = QueryToElasticsearch.getCommand(requestToMongodb.getNthQuery(0));
        SearchResponse elasticSearchResponse = search(query, null);
        if (elasticSearchResponse.status() != RestStatus.OK) {
            return null;
        }
        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits() == 0) {
            return null;
        }
        PathQuery newQuery = null;
        final Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            if (newQuery == null) {
                newQuery = QueryHelper.path(hit.getId());
            } else {
                newQuery.add(hit.getId());
            }
        }
        Select newSelectRequest = new Select();
        newSelectRequest.setQuery(newQuery);
        final SelectParserSingle newParser = new SelectParserSingle(new VarNameAdapter());
        newParser.parse(newSelectRequest.getFinalSelect());
        return selectMongoDbExecute(newParser);
    }

    /**
     * @param parser
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException when query is not correct
     */
    @SuppressWarnings("unchecked")
    private MongoCursor<VitamDocument<?>> selectMongoDbExecute(SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongoDb selectToMongoDb = new SelectToMongoDb(parser);
        int tenantId = ParameterHelper.getTenantParameter();        
        Bson initialCondition = QueryToMongodb.getCommand(selectToMongoDb.getSelect().getQuery());
        Bson condition = initialCondition;        
        if (vitamCollection.isMultiTenant()) {
            condition = and(initialCondition, eq(VitamDocument.TENANT_ID, tenantId));
        }
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset = selectToMongoDb.getFinalOffset();
        final int limit = selectToMongoDb.getFinalLimit();
        FindIterable<VitamDocument<?>> find = (FindIterable<VitamDocument<?>>) vitamCollection.getCollection().find(condition).skip(offset);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit > 0) {
            find = find.limit(limit);
        }
        VitamDocument<?> document = find.first();
        return find.iterator();
    }


    /**
     *
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     *        values" : [list of id] } }"
     * @param filter
     * @return a structure as ResultInterface
     * @throws DatabaseException
     */
    private final SearchResponse search(final QueryBuilder query,
        final QueryBuilder filter) throws DatabaseException {
        final SearchRequestBuilder request =
            vitamCollection.getEsClient().getClient()
            .prepareSearch(vitamCollection.getName().toLowerCase()).setSearchType(SearchType.DEFAULT)
            .setTypes(vitamCollection.getTypeunique()).setExplain(false).setSize(GlobalDatas.LIMIT_LOAD);
        if (filter != null) {
            request.setQuery(query).setPostFilter(filter);
        } else {
            request.setQuery(query);
        }
        try {
            return request.get();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            throw new DatabaseException(e);
        }
    }

    /**
     * @param condition where condition
     * @param actions update
     * @param isMultiple nb of item to update
     * @return the UpdateResult on the update request based on the given collection
     * @throws DatabaseException
     */
    private final UpdateResult updateMongoDb(final Bson condition, final Bson actions, final boolean isMultiple)
        throws DatabaseException {
        try {
            if (isMultiple) {
                return vitamCollection.getCollection().updateMany(condition, actions);
            } else {
                return vitamCollection.getCollection().updateOne(condition, actions);
            }
        } catch (final MongoException e) {
            throw new DatabaseException(e);
        }
    }

    private DbRequestResult updateDocuments(JsonNode request) throws InvalidParseOperationException, DatabaseException, InvalidCreateOperationException {
        final UpdateParserSingle parser = new UpdateParserSingle(new VarNameAdapter());
        parser.parse(request);
        final UpdateToMongodb requestToMongodb = new UpdateToMongodb(parser);

        Select selectQuery = new Select();
        selectQuery.setQuery(parser.getRequest().getQuery());
        MongoCursor<VitamDocument<?>> searchResult = search(selectQuery.getFinalSelect());

        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        List<VitamDocument<?>> listDocuments = new ArrayList<VitamDocument<?>>();

        while (searchResult.hasNext()) {
            listDocuments.add(searchResult.next());
        }
        searchResult.close();
        final Map<String, List<String>> diffs = new HashMap<>();
        List<VitamDocument<?>> listUpdatedDocuments = new ArrayList<VitamDocument<?>>();
        for (VitamDocument<?> document : listDocuments) {
            final String documentId = document.getId();
            final String documentBeforeUpdate = JsonHandler.prettyPrint(document);
            Bson condition = eq(VitamDocument.ID, documentId);        
            UpdateResult updateResult = updateMongoDb(condition, requestToMongodb.getFinalUpdateActions(), false);
            if (updateResult.getModifiedCount() < 1) {
                throw new DatabaseException("Can not modified Document");  
            }
            VitamDocument<?> updatedDocument = (VitamDocument<?>) vitamCollection.getCollection().find(condition, vitamCollection.getClasz()).first();
            listUpdatedDocuments.add(updatedDocument);
            final String documentAfterUpdate = JsonHandler.prettyPrint(updatedDocument);
            diffs.put(documentId, 
                getConcernedDiffLines(getUnifiedDiff(documentBeforeUpdate, documentAfterUpdate)));
        }
        insertToElasticsearch(listUpdatedDocuments);
        return new DbRequestResult()
            .setCount(listDocuments.size())
            .setDiffs(diffs);
    }

    private DbRequestResult deleteDocuments(JsonNode request) throws DatabaseException, InvalidCreateOperationException, InvalidParseOperationException {
        MongoCursor<VitamDocument<?>> searchResult = search(request);
        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        PathQuery newQuery = null;
        while (searchResult.hasNext()) {
            final String documentId = searchResult.next().getId();
            if (newQuery == null) {
                newQuery = QueryHelper.path(documentId);
            } else {
                newQuery.add(documentId);
            }
            if (vitamCollection.getEsClient() != null) {
                deleteEntry(documentId);
            }
        }
        searchResult.close();
        Bson filter = QueryToMongodb.getCommand(newQuery);
        DeleteResult result = vitamCollection.getCollection().deleteMany(filter);
        return new DbRequestResult().setCount(result.getDeletedCount());
    }


    /**
     * Delete one index
     *
     * @throws DatabaseException
     */
    private final void deleteIndex() throws DatabaseException {
        final Client client = vitamCollection.getEsClient().getClient();
        try {
            if (client.admin().indices().prepareExists(vitamCollection.getName().toLowerCase()).get().isExists()) {
                if (!client.admin().indices().prepareDelete(vitamCollection.getName().toLowerCase()).get()
                    .isAcknowledged()) {
                    LOGGER.error("Error on index delete");
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Delete one index
     *
     * @param id
     * @throws DatabaseException 
     */
    private final void deleteEntry(String id) throws DatabaseException {
        final Client client = vitamCollection.getEsClient().getClient();
        final DeleteResponse response;
        try {
            if (client.admin().indices().prepareExists(vitamCollection.getName().toLowerCase()).get().isExists()) {
                final DeleteRequestBuilder builder = client.prepareDelete(vitamCollection.getName().toLowerCase(), vitamCollection.getTypeunique(), id);
                response = builder.setRefresh(true).get();
                if (!response.isFound()) {
                    throw new DatabaseException("Item not found when trying to delete");
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            throw new DatabaseException(e);
        }
    }
}
