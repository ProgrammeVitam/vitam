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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;


/**
 * ElasticSearch model with MongoDB as main database
 *
 */
public class ElasticsearchAccessMetadata extends ElasticsearchAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessMetadata.class);

    /**
     * @param clusterName
     * @param nodes
     * @throws VitamException
     */
    public ElasticsearchAccessMetadata(final String clusterName, List<ElasticsearchNode> nodes) throws VitamException {
        super(clusterName, nodes);
    }

    /**
     * Delete one index
     *
     * @param collection
     * @return True if ok
     */
    public final boolean deleteIndex(final MetadataCollections collection) {
        try {
            if (client.admin().indices().prepareExists(collection.getName().toLowerCase()).get().isExists()) {
                if (!client.admin().indices().prepareDelete(collection.getName().toLowerCase()).get()
                    .isAcknowledged()) {
                    LOGGER.error("Error on index delete");
                }
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            return true;
        }
    }

    /**
     * Add a type to an index
     *
     * @param collection
     * @return True if ok
     */
    public final boolean addIndex(final MetadataCollections collection) {
        LOGGER.debug("addIndex: " + collection.getName().toLowerCase());
        if (!client.admin().indices().prepareExists(collection.getName().toLowerCase()).get().isExists()) {
            LOGGER.debug("createIndex");
            client.admin().indices().prepareCreate(collection.getName().toLowerCase()).get();
        }
        final String mapping = collection == MetadataCollections.C_UNIT ? Unit.MAPPING : ObjectGroup.MAPPING;
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;

        LOGGER.debug("setMapping: " + collection.getName().toLowerCase() + " type: " + type + "\n\t" + mapping);
        try {
            final PutMappingResponse response =
                client.admin().indices().preparePutMapping().setIndices(collection.getName().toLowerCase())
                    .setType(type)
                    .setSource(mapping).get();
            LOGGER.debug(type + ":" + response.isAcknowledged());
            return response.isAcknowledged();
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return false;
        }
    }

    /**
     * refresh an index
     *
     * @param collection
     */
    public final void refreshIndex(final MetadataCollections collection) {
        LOGGER.debug("refreshIndex: " + collection.getName().toLowerCase());
        client.admin().indices().prepareRefresh(collection.getName().toLowerCase())
            .execute().actionGet();

    }


    /**
     * Add an entry in the ElasticSearch index
     *
     * @param collection
     * @param id
     * @param json
     * @return True if ok
     */
    final boolean addEntryIndex(final MetadataCollections collection,
        final String id, final String json) {
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        return client.prepareIndex(collection.getName().toLowerCase(), type, id)
            .setSource(json).setOpType(OpType.INDEX).get()
            .getVersion() > 0;
    }

    /**
     * Add a set of entries in the ElasticSearch index. <br>
     * Used in reload from scratch.
     *
     * @param collection
     * @param mapIdJson
     * @return the listener on bulk insert
     */
    final ListenableActionFuture<BulkResponse> addEntryIndexes(final MetadataCollections collection,
        final Map<String, String> mapIdJson) {
        final BulkRequestBuilder bulkRequest = client.prepareBulk();
        // either use client#prepare, or use Requests# to directly build index/delete requests
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        for (final Entry<String, String> val : mapIdJson.entrySet()) {
            bulkRequest.add(client.prepareIndex(collection.getName().toLowerCase(), type,
                val.getKey()).setSource(val.getValue()));
        }
        return bulkRequest.execute(); // new thread
    }

    /**
     * Add a set of entries in the ElasticSearch index in blocking mode. <br>
     * Used in reload from scratch.
     *
     * @param collection
     * @param mapIdJson
     * @return True if ok
     */
    final boolean addEntryIndexesBlocking(final MetadataCollections collection,
        final Map<String, String> mapIdJson) {
        final BulkResponse bulkResponse = addEntryIndexes(collection, mapIdJson).actionGet();
        if (bulkResponse.hasFailures()) {
            LOGGER.error("ES previous insert in error: " + bulkResponse.buildFailureMessage());
        }
        return !bulkResponse.hasFailures();
        // Should process failures by iterating through each bulk response item
    }

    /**
     * Method to filter what should never be indexed
     * 
     * @param unit
     * @return the new Unit without the unwanted fields
     */
    private static final Unit getFiltered(final Unit unit) {
        final Unit eunit = new Unit(unit);
        eunit.remove(VitamLinks.UNIT_TO_UNIT.field1to2);
        return eunit;
    }


    /**
     * Add one VitamDocument to indexation immediately
     *
     * @param document
     * @return True if inserted in ES
     */
    public final boolean addEntryIndex(final MetadataDocument<?> document) {
        MetadataDocument<?> newdoc = document;
        MetadataCollections collection = MetadataCollections.C_OBJECTGROUP;
        if (newdoc instanceof Unit) {
            collection = MetadataCollections.C_UNIT;
            newdoc = getFiltered((Unit) newdoc);
        }
        final String id = newdoc.getId();
        newdoc.remove(VitamDocument.ID);
        final String mongoJson = newdoc.toJson(new JsonWriterSettings(JsonMode.STRICT));
        newdoc.clear();
        // TODO test bson4jackson
        // ( https://github.com/michel-kraemer/bson4jackson)
        final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
        final String esJson = dbObject.toString();
        return addEntryIndex(collection, id, esJson);
    }


    /**
     * Used for iterative reload in restore operation (using bulk).
     *
     * @param indexes
     * @param document
     * @return the number of Unit incorporated (0 if none)
     */
    public final int addBulkEntryIndex(final Map<String, String> indexes,
        final MetadataDocument<?> document) {
        MetadataDocument<?> newdoc = document;
        MetadataCollections collection = MetadataCollections.C_OBJECTGROUP;
        if (newdoc instanceof Unit) {
            collection = MetadataCollections.C_UNIT;
            newdoc = getFiltered((Unit) newdoc);
        }
        final String id = newdoc.getId();
        newdoc.remove(VitamDocument.ID);

        final String mongoJson = newdoc.toJson(new JsonWriterSettings(JsonMode.STRICT));
        final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
        indexes.put(id, dbObject.toString());
        int nb = 0;
        if (indexes.size() > GlobalDatasDb.LIMIT_ES_NEW_INDEX) {
            nb = indexes.size();
            addEntryIndexes(collection, indexes);
            indexes.clear();
        }
        newdoc.clear();
        return nb;
    }


    /**
     * Update an entry in the ElasticSearch index
     *
     * @param collection
     * @param id
     * @param json
     * @return True if ok
     * @throws Exception
     */
    final boolean updateEntryIndex(final MetadataCollections collection,
        final String id, final String json) throws Exception {
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        return client.prepareUpdate(collection.getName().toLowerCase(), type, id)
            .setDoc(json).execute()
            .actionGet().getVersion() > 0;
    }

    /**
     * updateBulkUnitsEntriesIndexes
     * 
     * Update a set of entries in the ElasticSearch index based in Cursor Result. <br>
     * 
     * @param cursor :containing all Units to be indexed
     */
    final void updateBulkUnitsEntriesIndexes(MongoCursor<Unit> cursor) {
        final BulkRequestBuilder bulkRequest = client.prepareBulk();
        while (cursor.hasNext()) {
            final Unit unit = getFiltered(cursor.next());
            final String id = unit.getId();
            unit.remove(VitamDocument.ID);

            final String mongoJson = unit.toJson(new JsonWriterSettings(JsonMode.STRICT));
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);

            bulkRequest.add(client.prepareUpdate(MetadataCollections.C_UNIT.getName().toLowerCase(), Unit.TYPEUNIQUE,
                id).setDoc(dbObject.toString()));
        }
        final BulkResponse bulkResponse = bulkRequest.execute().actionGet(); // new thread
        if (bulkResponse.hasFailures()) {
            LOGGER.error("ES previous update in error: " + bulkResponse.buildFailureMessage());
        }
    }

    /**
     * 
     * updateBulkOGEntriesIndexes
     * 
     * Update a set of entries in the ElasticSearch index based in Cursor Result. <br>
     * 
     * @param cursor :containing all OG to be indexed
     */
    final void updateBulkOGEntriesIndexes(MongoCursor<ObjectGroup> cursor) {
        final BulkRequestBuilder bulkRequest = client.prepareBulk();
        while (cursor.hasNext()) {
            final ObjectGroup objectGroup = cursor.next();

            final String id = objectGroup.getId();
            objectGroup.remove(VitamDocument.ID);

            final String mongoJson = objectGroup.toJson(new JsonWriterSettings(JsonMode.STRICT));
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);

            bulkRequest.add(
                client.prepareUpdate(MetadataCollections.C_OBJECTGROUP.getName().toLowerCase(), ObjectGroup.TYPEUNIQUE,
                    id).setDoc(dbObject.toString()));
        }
        final BulkResponse bulkResponse = bulkRequest.execute().actionGet(); // new thread
        if (bulkResponse.hasFailures()) {
            LOGGER.error("ES previous update in error: " + bulkResponse.buildFailureMessage());
        }
    }

    /**
     *
     * @param collection
     * @param currentNodes current parent nodes
     * @param subdepth where subdepth >= 1
     * @param condition
     * @param filterCond
     * @return the Result associated with this request. Note that the exact depth is not checked, so it must be checked
     *         after (using MongoDB)
     * @throws MetaDataExecutionException
     */
    public final Result getSubDepth(final MetadataCollections collection,
        final Set<String> currentNodes, final int subdepth, final QueryBuilder condition, final QueryBuilder filterCond)
        throws MetaDataExecutionException {
        QueryBuilder query = null;
        QueryBuilder filter = null;
        if (GlobalDatasDb.USE_FILTER) {
            filter = getSubDepthFilter(filterCond, currentNodes, subdepth);
            query = condition;
        } else {
            /*
             * filter where _ud (currentNodes as (grand)parents, depth<=subdepth)
             */
            QueryBuilder domdepths = null;
            if (subdepth == 1) {
                domdepths = QueryBuilders.boolQuery()
                    .should(QueryBuilders.termsQuery(VitamLinks.UNIT_TO_UNIT.field2to1, currentNodes));
            } else {
                domdepths = QueryBuilders.termsQuery(Unit.UNITUPS, currentNodes);
            }
            /*
             * Condition query
             */
            query = QueryBuilders.boolQuery().must(domdepths).must(condition);
            filter = filterCond;
        }

        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        return search(collection, type, query, filter);
    }

    /**
     * Build the filter for subdepth and currentNodes
     *
     * @param filterCond
     * @param currentNodes
     * @param key
     * @param subdepth where subdepth >= 1
     * @return the associated filter
     */
    private final QueryBuilder getSubDepthFilter(final QueryBuilder filterCond, final Set<String> currentNodes,
        final int subdepth) {
        /*
         * filter where domdepths (currentNodes as (grand)parents, depth<=subdepth)
         */
        QueryBuilder domdepths = null;
        QueryBuilder filter = null;
        if (subdepth == 1) {
            filter = QueryBuilders.boolQuery()
                .should(QueryBuilders.termsQuery(VitamLinks.UNIT_TO_UNIT.field2to1, currentNodes));
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Filter: terms {} = {}", VitamLinks.UNIT_TO_OBJECTGROUP.field2to1,
                    currentNodes);
            }
        } else {
            filter = QueryBuilders.termsQuery(Unit.UNITUPS, currentNodes);
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("ESReq: terms {} = {}", Unit.UNITUPS, currentNodes);
            }
        }
        if (filterCond != null) {
            domdepths = QueryBuilders.boolQuery().must(filter).must(filterCond);
        } else {
            domdepths = filter;
        }
        return domdepths;
    }

    /**
     * Build the filter for depth = 0
     *
     * @param collection
     * @param currentNodes current parent nodes
     * @param condition
     * @param filterCond
     * @return the Result associated with this request. Note that the exact depth is not checked, so it must be checked
     *         after (using MongoDb)
     * @throws MetaDataExecutionException
     */
    public final Result getStart(final MetadataCollections collection,
        final Set<String> currentNodes,
        final QueryBuilder condition, final QueryBuilder filterCond) throws MetaDataExecutionException {
        QueryBuilder query = null;
        QueryBuilder filter = null;
        if (GlobalDatasDb.USE_FILTER) {
            filter = getFilterStart(filterCond, currentNodes);
            query = condition;
        } else {
            /*
             * filter where _id in (currentNodes as list of ids)
             */
            final QueryBuilder domdepths = QueryBuilders.idsQuery((String[]) currentNodes.toArray());
            /*
             * Condition query
             */
            query = QueryBuilders.boolQuery().must(domdepths).must(condition);
            filter = filterCond;
        }
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        return search(collection, type, query, filter);
    }

    /**
     * Build the filter for depth = 0
     *
     * @param filterCond
     * @param currentNodes
     * @param key
     * @return the associated filter
     */
    private final QueryBuilder getFilterStart(final QueryBuilder filterCond, final Set<String> currentNodes) {
        /*
         * filter where _id in (currentNodes as list of ids)
         */
        QueryBuilder domdepths = null;
        final IdsQueryBuilder filter = QueryBuilders.idsQuery((String[]) currentNodes.toArray());
        if (filterCond != null) {
            domdepths = QueryBuilders.boolQuery().must(filter).must(filterCond);
        } else {
            domdepths = filter;
        }
        return domdepths;
    }

    /**
     * Build the filter for negative depth
     *
     * @param collection
     * @param subset subset of valid nodes in the negative depth
     * @param condition
     * @param filterCond
     * @return the Result associated with this request. The final result should be checked using MongoDb.
     * @throws MetaDataExecutionException
     */
    public final Result getNegativeSubDepth(final MetadataCollections collection, final Set<String> subset,
        final QueryBuilder condition, final QueryBuilder filterCond) throws MetaDataExecutionException {
        QueryBuilder query = null;
        QueryBuilder filter = null;

        if (GlobalDatasDb.USE_FILTER) {
            /*
             * filter where id from subset
             */
            TermsQueryBuilder filterTerms = null;
            filterTerms = QueryBuilders.termsQuery(MetadataDocument.ID, subset);
            if (filterCond != null) {
                filter = QueryBuilders.boolQuery().must(filterTerms).must(filterCond);
            } else {
                filter = filterTerms;
            }
            query = condition;
        } else {
            /*
             * filter where id from subset
             */
            QueryBuilder domdepths = null;
            domdepths = QueryBuilders.termsQuery(MetadataDocument.ID, subset);
            /*
             * Condition query
             */
            query = QueryBuilders.boolQuery().must(domdepths).must(condition);
            filter = filterCond;
        }
        final String type = collection == MetadataCollections.C_UNIT ? Unit.TYPEUNIQUE : ObjectGroup.TYPEUNIQUE;
        return search(collection, type, query, filter);
    }

    /**
     *
     * @param collection
     * @param type
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     *        values" : [list of id] } }"
     * @param filter
     * @return a structure as ResultInterface
     * @throws MetaDataExecutionException
     */
    protected final Result search(final MetadataCollections collection, final String type, final QueryBuilder query,
        final QueryBuilder filter) throws MetaDataExecutionException {
        // Note: Could change the code to allow multiple indexes and multiple types
        final SearchRequestBuilder request =
            client.prepareSearch(collection.getName().toLowerCase()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(type).setExplain(false).setSize(GlobalDatas.LIMIT_LOAD);
        if (filter != null) {
            if (GlobalDatasDb.USE_FILTERED_REQUEST) {
                final BoolQueryBuilder filteredQueryBuilder = QueryBuilders.boolQuery().must(query).filter(filter);
                request.setQuery(filteredQueryBuilder);
            } else {
                request.setQuery(query).setPostFilter(filter);
            }
        } else {
            request.setQuery(query);
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("ESReq: {}", request);
        } else {
            LOGGER.debug("ESReq: {}", request);
        }
        final SearchResponse response;
        try {
            response = request.get();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            throw new MetaDataExecutionException(e.getMessage(), e);
        }
        if (response.status() != RestStatus.OK) {
            LOGGER.error("Error " + response.status() + " from : " + request + ":" + query + " # " + filter);
            return null;
        }
        final SearchHits hits = response.getHits();
        if (hits.getTotalHits() > GlobalDatas.LIMIT_LOAD) {
            LOGGER.warn("Warning, more than " + GlobalDatas.LIMIT_LOAD + " hits: " + hits.getTotalHits());
        }
        if (hits.getTotalHits() == 0) {
            LOGGER.error("No result from : " + request);
            return null;
        }
        long nb = 0;
        final boolean isUnit = collection == MetadataCollections.C_UNIT;
        final Result resultRequest = isUnit ? MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS)
            : MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        final Iterator<SearchHit> iterator = hits.iterator();
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            final String id = hit.getId();
            final Map<String, Object> src = hit.getSource();
            if (src != null && isUnit) {
                final Object val = src.get(Unit.NBCHILD);
                if (val == null) {
                    LOGGER.error("Not found " + Unit.NBCHILD);
                } else if (val instanceof Integer) {
                    nb += (Integer) val;
                    if (GlobalDatasDb.PRINT_REQUEST) {
                        LOGGER.debug("Result: {} : {}", id, val);
                    }
                } else {
                    LOGGER.error("Not Integer: " + val.getClass().getName());
                }
            }
            resultRequest.addId(id);
        }
        resultRequest.setNbResult(nb);
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("FinalEsResult: {} : {}", resultRequest.getCurrentIds(), resultRequest.getNbResult());
        }
        return resultRequest;
    }

    /**
     *
     * @param collections
     * @param type
     * @param id
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     */
    public final void deleteEntryIndex(final MetadataCollections collections, final String type,
        final String id) throws MetaDataExecutionException, MetaDataNotFoundException {
        final DeleteRequestBuilder builder = client.prepareDelete(collections.getName().toLowerCase(), type, id);
        final DeleteResponse response;
        try {
            response = builder.get();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            throw new MetaDataExecutionException(e.getMessage(), e);
        }
        if (!response.isFound()) {
            throw new MetaDataNotFoundException("Item not found when trying to delete");
        }
    }
}
