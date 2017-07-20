/**
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
package fr.gouv.vitam.common.database.server;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getConcernedDiffLines;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getUnifiedDiff;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.NopQuery;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
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
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.elasticsearch.SelectToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.HeaderIdHelper;

/**
 * This class execute all request single in Vitam
 */
public class DbRequestSingle {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestSingle.class);

    private final VitamCollection vitamCollection;
    private final VarNameAdapter vaNameAdapter;
    private long count = 0;
    private long total = 0;
    private long offset = 0;
    private long limit = 0;

    /**
     * Constructor with VitamCollection
     *
     * @param collection
     */
    public DbRequestSingle(VitamCollection collection) {
        this.vitamCollection = collection;
        vaNameAdapter = new SingleVarNameAdapter();
    }

    /**
     * execute all request
     *
     * @param request
     * @return DbRequestResult
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public DbRequestResult execute(RequestSingle request)
        throws InvalidParseOperationException, DatabaseException, InvalidCreateOperationException {
        return execute(request, 0);
    }
    /**
     * execute all request
     * @param request
     * @param version
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public DbRequestResult execute(RequestSingle request, Integer version)
        throws InvalidParseOperationException, DatabaseException, InvalidCreateOperationException {
        if (request instanceof Insert) {
            ArrayNode data = ((Insert) request).getDatas();
            return insertDocuments(data, version);
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
     * Helper to detect an insert that should be an Update
     *
     * @param e exception catched
     * @return true if an insert that should be an Update, else False
     */
    public static boolean checkInsertOrUpdate(Exception e) {
        if (e instanceof DatabaseException &&
            (((DatabaseException) e).getCause() instanceof MongoBulkWriteException |
            ((DatabaseException) e).getCause() instanceof MongoWriteException)) {
            LOGGER.info("Document existed, updating ...");
            return true;
        }
        Throwable d = e.getCause();
        if (d instanceof DatabaseException &&
            (((DatabaseException) d).getCause() instanceof MongoBulkWriteException |
            ((DatabaseException) d).getCause() instanceof MongoWriteException)) {
            LOGGER.info("Document existed, updating ...");
            return true;
        }
        return false;
    }


    /**
     * Main method for Multiple Insert
     *
     * @param arrayNode
     * @return DbRequestResult
     * @param version
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     */
    @SuppressWarnings("unchecked")
    private DbRequestResult insertDocuments(ArrayNode arrayNode, Integer version)
        throws InvalidParseOperationException, DatabaseException {
        final List<VitamDocument<?>> vitamDocumentList = new ArrayList<>();
        for (final JsonNode objNode : arrayNode) {
            // TODO : How to add _v in all VitamDocuments make by JsonHandler ?
            VitamDocument<?> obj = (VitamDocument<?>) JsonHandler.getFromJsonNode(objNode, vitamCollection.getClasz());
            obj.append(VitamDocument.VERSION, version);
            obj.remove(VitamDocument.SCORE);
            if (vitamCollection.isMultiTenant()) {
                obj.append(VitamDocument.TENANT_ID, obj.getTenantFixJunit());
            }
            vitamDocumentList.add(obj);
        }
        MongoCollection<VitamDocument<?>> collection =
            (MongoCollection<VitamDocument<?>>) vitamCollection.getCollection();
        try {
            collection.insertMany(vitamDocumentList);
        } catch (final MongoException e) {
            LOGGER.warn(e);
            throw new DatabaseException(e);
        }
        insertToElasticsearch(vitamDocumentList);
        return new DbRequestResult().setCount(vitamDocumentList.size()).setTotal(vitamDocumentList.size());
    }
    /**
     * @param arrayNode
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     */
    @SuppressWarnings("unchecked")
    private DbRequestResult insertDocuments(ArrayNode arrayNode )
        throws InvalidParseOperationException, DatabaseException {
        return  insertDocuments ( arrayNode, 0);
    }
    /**
     * Private Elasticsearch insert method
     *
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
        int max = VitamConfiguration.MAX_ELASTICSEARCH_BULK;
        for (VitamDocument<?> document : vitamDocumentList) {
            max--;
            String id = document.getString(VitamDocument.ID);
            document.remove(VitamDocument.ID);
            document.remove(VitamDocument.SCORE);
            final String mongoJson = document.toJson(new JsonWriterSettings(JsonMode.STRICT));
            document.clear();
            final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();
            mapIdJson.put(id, esJson);
            if (max == 0) {
                max = VitamConfiguration.MAX_ELASTICSEARCH_BULK;
                final BulkResponse bulkResponse = addEntryIndexes(mapIdJson);
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("Insert Documents Exception");
                    throw new DatabaseException("Insert Document Exception");
                }
                mapIdJson.clear();
            }
        }
        if (!mapIdJson.isEmpty()) {
            final BulkResponse bulkResponse = addEntryIndexes(mapIdJson);
            if (bulkResponse.hasFailures()) {
                // Add usefull information
                StringBuffer sb = new StringBuffer();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.getFailure() != null) {
                        sb.append(bulkItemResponse.getFailure().getCause());
                    }
                }
                LOGGER.error(String.format("Insert Documents Exception caused by : %s", sb.toString()));
                throw new DatabaseException("Insert Document Exception");
            }
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
            bulkRequest.setRefresh(true)
                .add(client.prepareIndex(vitamCollection.getName().toLowerCase(), VitamCollection.getTypeunique(),
                    val.getKey()).setSource(val.getValue()));
        }
        return bulkRequest.execute().actionGet();
    }

    /**
     * Main Select method
     *
     * @param select
     * @return DbRequestResult
     * @throws DatabaseException
     */
    private DbRequestResult findDocuments(JsonNode select) throws DatabaseException {
        MongoCursor<VitamDocument<?>> cursor = search(select);
        return new DbRequestResult().setCursor(cursor).setTotal(total > 0 ? total : count).setCount(count)
            .setLimit(limit).setOffset(offset);
    }

    /**
     * Private method to select Elasticsearch or MongoDb
     *
     * @param select
     * @return MongoCursor<VitamDocument<?>>
     * @throws DatabaseException
     */
    private MongoCursor<VitamDocument<?>> search(JsonNode select) throws DatabaseException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(vaNameAdapter);
            parser.parse(select);
            if (vitamCollection.isMultiTenant()) {
                parser.addCondition(QueryHelper.eq(VitamFieldsHelper.tenant(), HeaderIdHelper.getTenantId()));
            }
            if (vitamCollection.getEsClient() != null) {
                return selectElasticsearchExecute(parser);
            } else {
                return selectMongoDbExecute(parser);
            }
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Private method for select using Elasticsearch
     *
     * @param parser
     * @return MongoCursor<VitamDocument<?>>
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     * @throws DatabaseException
     */
    private MongoCursor<VitamDocument<?>> selectElasticsearchExecute(SelectParserSingle parser)
        throws InvalidParseOperationException, InvalidCreateOperationException, DatabaseException {
        SelectToElasticsearch requestToEs = new SelectToElasticsearch(parser);
        QueryBuilder query = QueryToElasticsearch.getCommand(requestToEs.getNthQuery(0));
        List<SortBuilder> sorts = requestToEs.getFinalOrderBy(vitamCollection.isUseScore());
        offset = requestToEs.getFinalOffset();
        limit = requestToEs.getFinalLimit();
        SearchResponse elasticSearchResponse =
            search(query, null, sorts, requestToEs.getFinalOffset(), requestToEs.getFinalLimit());
        if (elasticSearchResponse.status() != RestStatus.OK) {
            return new EmptyMongoCursor<>();
        }
        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits() == 0) {
            return new EmptyMongoCursor<>();
        }
        total = hits.getTotalHits();
        final Iterator<SearchHit> iterator = hits.iterator();
        count = hits.getHits().length;
        final List<String> list = new ArrayList<>((int) count);
        final List<Float> listFloat = new ArrayList<>((int) count);
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            list.add(hit.getId());
            listFloat.add(hit.getScore());
        }
        parser.getRequest().setQuery(new NopQuery());
        return selectMongoDbExecute(parser, list, listFloat);

    }

    /**
     * Private method for select using MongoDb from Elasticsearch result
     *
     * @param parser
     * @param list list of Ids
     * @param list list of scores for each Ids
     * @return MongoCursor<VitamDocument<?>>
     * @throws InvalidParseOperationException when query is not correct
     * @throws InvalidCreateOperationException
     */
    private MongoCursor<VitamDocument<?>> selectMongoDbExecute(SelectParserSingle parser, List<String> list,
        List<Float> score)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        return DbRequestHelper.selectMongoDbExecuteThroughFakeMongoCursor(vitamCollection, parser,
            list, score);
    }

    /**
     * Private method for select using MongoDb
     *
     * @param parser
     * @return MongoCursor<VitamDocument<?>>
     * @throws InvalidParseOperationException when query is not correctBson
     */
    @SuppressWarnings("unchecked")
    private MongoCursor<VitamDocument<?>> selectMongoDbExecute(SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongodb selectToMongoDb = new SelectToMongodb(parser);
        Bson initialCondition = QueryToMongodb.getCommand(selectToMongoDb.getSingleSelect().getQuery());
        Bson condition = initialCondition;
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset2 = selectToMongoDb.getFinalOffset();
        final int limit2 = selectToMongoDb.getFinalLimit();
        FindIterable<VitamDocument<?>> find =
            (FindIterable<VitamDocument<?>>) vitamCollection.getCollection().find(condition).skip(offset2);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit2 > 0) {
            find = find.limit(limit2);
        }
        if (offset == 0) {
            offset = offset2;
        }
        if (limit == 0) {
            limit = limit2;
        }
        return find.iterator();
    }

    /**
     * Private sub method for Elasticsearch search
     *
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     *        values" : [list of id] } }"
     * @param filter the filter
     * @param sorts the list of sort
     * @param offset the offset
     * @param limit the limit
     * @return a structure as ResultInterface
     * @throws DatabaseException
     */
    private final SearchResponse search(final QueryBuilder query,
        final QueryBuilder filter, List<SortBuilder> sorts, final int offset, final int limit)
        throws DatabaseException {
        final SearchRequestBuilder request =
            vitamCollection.getEsClient().getClient()
                .prepareSearch(vitamCollection.getName().toLowerCase()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(VitamCollection.getTypeunique()).setExplain(false).setFrom(offset)
                .setSize(GlobalDatas.LIMIT_LOAD < limit ? GlobalDatas.LIMIT_LOAD : limit)
                .setFetchSource(VitamDocument.ES_FILTER_OUT, null);
        if (sorts != null) {
            sorts.stream().forEach(sort -> request.addSort(sort));
        }
        if (filter != null) {
            request.setQuery(QueryBuilders.boolQuery().must(query).must(filter));
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
     * Main Update method
     *
     * @param request
     * @return DbRequestResult
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     */
    private DbRequestResult updateDocuments(JsonNode request)
        throws InvalidParseOperationException, DatabaseException, InvalidCreateOperationException {
        final UpdateParserSingle parser = new UpdateParserSingle(vaNameAdapter);
        parser.parse(request);
        if (vitamCollection.isMultiTenant()) {
            parser.addCondition(QueryHelper.eq(VitamFieldsHelper.tenant(), HeaderIdHelper.getTenantId()));
        }
        final Select selectQuery = new Select();
        selectQuery.setQuery(parser.getRequest().getQuery());
        MongoCursor<VitamDocument<?>> searchResult = search(selectQuery.getFinalSelect());

        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        List<VitamDocument<?>> listDocuments = new ArrayList<>();

        while (searchResult.hasNext()) {
            listDocuments.add(searchResult.next());
        }
        searchResult.close();
        final Map<String, List<String>> diffs = new HashMap<>();
        List<VitamDocument<?>> listUpdatedDocuments = new ArrayList<>();
        for (VitamDocument<?> document : listDocuments) {
            document.remove(VitamDocument.SCORE);
            final String documentId = document.getId();
            final Integer documentVersion = document.getVersion();
            final String documentBeforeUpdate = JsonHandler.prettyPrint(document);

            VitamDocument<?> updatedDocument = null;
            int nbTry = 0;
            boolean modified = false;

            while (updatedDocument == null && nbTry < 3) {
                nbTry++;
                JsonNode jsonDocument = JsonHandler.toJsonNode(document);
                MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument);
                ObjectNode updatedJsonDocument =
                    (ObjectNode) mongoInMemory.getUpdateJson(request, false, vaNameAdapter);
                updatedDocument = document.newInstance(updatedJsonDocument);
                if (!updatedDocument.equals(document)) {
                    modified = true;
                    updatedDocument.put(VitamDocument.VERSION, documentVersion.intValue() + 1);
                    MongoCollection collection = vitamCollection.getCollection();
                    Bson condition = and(eq(VitamDocument.ID, documentId), eq(VitamDocument.VERSION, documentVersion));
                    // Note: cannot do bulk since we need to check each and every update
                    try {
                        UpdateResult result = collection.replaceOne(condition, updatedDocument);
                        if (result.getModifiedCount() != 1) {
                            updatedDocument = null;
                        }
                    } catch (final MongoException e) {
                        LOGGER.warn("Update Document error : " + e.getMessage());
                        updatedDocument = null;
                    }
                } else {
                    break;
                }
            }

            if (modified && updatedDocument == null) {
                // Throw Error after 3 try
                throw new DatabaseException("Can not modify Document");
            }
            if (modified) {
                listUpdatedDocuments.add(updatedDocument);
                final String documentAfterUpdate = JsonHandler.prettyPrint(updatedDocument);
                diffs.put(documentId,
                    getConcernedDiffLines(getUnifiedDiff(documentBeforeUpdate, documentAfterUpdate)));
            }
        }
        if (!listUpdatedDocuments.isEmpty()) {
            insertToElasticsearch(listUpdatedDocuments);
        }
        return new DbRequestResult()
            .setCount(listUpdatedDocuments.size()).setTotal(listUpdatedDocuments.size())
            .setDiffs(diffs);
    }

    /**
     * Main Delete method
     *
     * @param request
     * @return DbRequestResult
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    private DbRequestResult deleteDocuments(JsonNode request)
        throws DatabaseException, InvalidCreateOperationException, InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(vaNameAdapter);
        parser.parse(request);
        parser.addProjection(JsonHandler.createObjectNode(), JsonHandler.createObjectNode().put(VitamDocument.ID, 1));
        final MongoCursor<VitamDocument<?>> searchResult = search(parser.getRequest().getFinalSelect());
        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        PathQuery newQuery = null;
        final List<String> ids = new ArrayList<>();
        while (searchResult.hasNext()) {
            final String documentId = searchResult.next().getId();
            ids.add(documentId);
        }
        newQuery = QueryHelper.path(ids.toArray(new String[0]));
        searchResult.close();
        final Bson filter = QueryToMongodb.getCommand(newQuery);
        DeleteResult result = null;
        try {
            result = vitamCollection.getCollection().deleteMany(filter);
        } catch (final MongoException e) {
            LOGGER.warn(e);
            throw new DatabaseException(e);
        }
        deleteToElasticsearch(ids);
        return new DbRequestResult().setCount(result.getDeletedCount()).setTotal(result.getDeletedCount());
    }


    /**
     * Private method for delete in Elasticsearch
     *
     * @param list list of Ids to delete
     * @return the number of deleted items
     * @throws DatabaseException if error occurs
     */
    private int deleteToElasticsearch(List<String> list) throws DatabaseException {
        if (vitamCollection.getEsClient() == null || list.isEmpty()) {
            return 0;
        }
        final Client client = vitamCollection.getEsClient().getClient();
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.MAX_ELASTICSEARCH_BULK;
        int count = 0;
        for (final String id : list) {
            max--;
            bulkRequest
                .add(
                client.prepareDelete(vitamCollection.getName().toLowerCase(), VitamCollection.getTypeunique(), id));
            if (max == 0) {
                max = VitamConfiguration.MAX_ELASTICSEARCH_BULK;
                final BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute().actionGet(); // new
                // thread
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                    throw new DatabaseException(bulkResponse.buildFailureMessage());
                }
                count += bulkResponse.getItems().length;
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute().actionGet(); // new
            // thread
            if (bulkResponse.hasFailures()) {
                LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                throw new DatabaseException(bulkResponse.buildFailureMessage());
            }
            return count + bulkResponse.getItems().length;
        }
        return count;
    }
}
