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

import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * ElasticSearch model with MongoDB as main database
 */
public class ElasticsearchAccessMetadata extends ElasticsearchAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessMetadata.class);

    public static final String MAPPING_UNIT_FILE = "/unit-es-mapping.json";
    public static final String MAPPING_OBJECT_GROUP_FILE = "/og-es-mapping.json";

    /**
     * @param clusterName cluster name
     * @param nodes       list of elasticsearch node
     * @throws VitamException if nodes list is empty
     */
    public ElasticsearchAccessMetadata(final String clusterName, List<ElasticsearchNode> nodes) throws VitamException, IOException {
        super(clusterName, nodes);
    }

    /**
     * Delete one index
     *
     * @param collection the working metadata collection
     * @param tenantId   the tenant for operation
     * @return True if ok
     */
    public final boolean deleteIndex(final MetadataCollections collection, Integer tenantId) {
        try {
            if (client.admin().indices().prepareExists(getIndexName(collection, tenantId)).get().isExists()) {
                if (!client.admin().indices().prepareDelete(getIndexName(collection, tenantId)).get()
                        .isAcknowledged()) {
                    LOGGER.error("Error on index delete");
                }
            }
            refreshIndex(collection, tenantId);
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            return true;
        }
    }

    /**
     * Add a type to an index
     *
     * @param collection the working metadata collection
     * @param tenantId   the tenant for operation
     * @return True if ok
     */
    public final boolean addIndex(final MetadataCollections collection, Integer tenantId) {
        LOGGER.debug("addIndex: {}", getIndexName(collection, tenantId));
        if (!client.admin().indices().prepareExists(getIndexName(collection, tenantId)).get().isExists()) {
            try {
                LOGGER.debug("createIndex");
                final String mapping = getMapping(collection);
                final String type = VitamCollection.getTypeunique();
                LOGGER.debug("setMapping: " + getIndexName(collection, tenantId) + " type: " + type + "\n\t" + mapping);
                final CreateIndexResponse response = client.admin().indices()
                        .prepareCreate(getIndexName(collection, tenantId))
                        .setSettings(default_builder)
                        .addMapping(type, mapping, XContentType.JSON).get();
                if (!response.isAcknowledged()) {
                    LOGGER.error(type + ":" + response.isAcknowledged());
                    return false;
                }
            } catch (final Exception e) {
                LOGGER.error("Error while set Mapping", e);
                return false;
            }
        }
        return true;
    }

    /**
     * refresh an index
     *
     * @param collection the workking metadata collection
     * @param tenantId   the tenant for operation
     */
    public final void refreshIndex(final MetadataCollections collection, Integer tenantId) {
        String allIndexes = getIndexName(collection, tenantId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("refreshIndex: " + allIndexes);
        }
        FlushResponse response =
                client.admin().indices().flush(new FlushRequest(allIndexes).force(true)).actionGet();
        LOGGER.debug("Flush request executed with {} successfull shards", response.getSuccessfulShards());
    }

    /**
     * Add an entry in the ElasticSearch index
     *
     * @param collection
     * @param tenantId
     * @param id
     * @param json
     * @return True if ok
     */
    final boolean addEntryIndex(final MetadataCollections collection, final Integer tenantId, final String id,
                                final String json) {
        final String type = VitamCollection.getTypeunique();
        return client.prepareIndex(getIndexName(collection, tenantId), type, id).setSource(json)
                .setOpType(OpType.INDEX)
                .get().getVersion() > 0;
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
                                                               final Integer tenantId, final Map<String, String> mapIdJson) {
        final BulkRequestBuilder bulkRequest = client.prepareBulk();
        // either use client#prepare, or use Requests# to directly build
        // index/delete requests
        final String type = VitamCollection.getTypeunique();
        for (final Entry<String, String> val : mapIdJson.entrySet()) {
            bulkRequest.add(client.prepareIndex(getIndexName(collection, tenantId), type, val.getKey())
                    .setSource(val.getValue(), XContentType.JSON));
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
    final boolean addEntryIndexesBlocking(final MetadataCollections collection, final Integer tenantId,
                                          final Map<String, String> mapIdJson) {
        final BulkResponse bulkResponse = addEntryIndexes(collection, tenantId, mapIdJson).actionGet();
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
        eunit.remove(VitamDocument.SCORE);
        return eunit;
    }

    /**
     * Add one VitamDocument to indexation immediately
     *
     * @param document the {@link MetadataDocument} for indexing
     * @param tenantId the tenant for operation
     * @return True if inserted in ES
     */
    public final boolean addEntryIndex(final MetadataDocument<?> document, Integer tenantId) {
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
        // TODO P1 test bson4jackson
        // ( https://github.com/michel-kraemer/bson4jackson)
        final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
        final String esJson = dbObject.toString();
        return addEntryIndex(collection, tenantId, id, esJson);
    }

    /**
     * Used for iterative reload in restore operation (using bulk).
     *
     * @param indexes  set of operation index
     * @param tenantId the tenant for operation
     * @param document the {@link MetadataDocument} for indexing
     * @return the number of Unit incorporated (0 if none)
     */
    public final int addBulkEntryIndex(final Map<String, String> indexes, final Integer tenantId,
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
            addEntryIndexes(collection, tenantId, indexes);
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
    final boolean updateEntryIndex(final MetadataCollections collection, final Integer tenantId, final String id,
                                   final String json) throws Exception {
        final String type = VitamCollection.getTypeunique();
        return client.prepareUpdate(getIndexName(collection, tenantId), type, id)
                .setDoc(json, XContentType.JSON)
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .execute().actionGet().getVersion() > 1;
    }

    /**
     * Insert in bulk mode from cursor<br>
     * <br>
     * Insert a set of entries in the ElasticSearch index based in Cursor Result. <br>
     *
     * @param cursor :containing all Units to be indexed
     * @throws MetaDataExecutionException if the bulk insert failed
     */
    final void insertBulkUnitsEntriesIndexes(MongoCursor<Unit> cursor, final Integer tenantId)
            throws MetaDataExecutionException {
        if (!cursor.hasNext()) {
            LOGGER.error("ES insert in error since no results to insert");
            throw new MetaDataExecutionException("No result to insert");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        while (cursor.hasNext()) {
            max--;
            final Unit unit = getFiltered(cursor.next());
            final String id = unit.getId();
            unit.remove(VitamDocument.ID);
            LOGGER.debug("DEBUG insert {}", unit);

            final String mongoJson = unit.toJson(new JsonWriterSettings(JsonMode.STRICT));
            // TODO Empty variable (null) might be ignore here
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
            final String toInsert = dbObject.toString().trim();
            if (toInsert.isEmpty()) {
                LOGGER.error("ES insert in error since result to insert is empty");
                throw new MetaDataExecutionException("Result to insert is empty");
            }
            bulkRequest.add(client
                    .prepareIndex(getIndexName(MetadataCollections.C_UNIT, tenantId), VitamCollection.getTypeunique(), id)
                    .setSource(toInsert, XContentType.JSON));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new thread

                if (bulkResponse.hasFailures()) {
                    int duplicates = 0;
                    for (final BulkItemResponse bulkItemResponse : bulkResponse) {
                        if (bulkItemResponse.getVersion() > 1) {
                            duplicates++;
                        }
                    }
                    LOGGER.error("ES insert in error with possible duplicates {}: {}", duplicates,
                            bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
            if (bulkResponse.hasFailures()) {
                int duplicates = 0;
                for (final BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.getVersion() > 1) {
                        duplicates++;
                    }
                }
                LOGGER.error("ES insert in error with possible duplicates {}: {}", duplicates,
                        bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
    }

    /**
     * updateBulkUnitsEntriesIndexes
     * <p>
     * Update a set of entries in the ElasticSearch index based in Cursor Result. <br>
     *
     * @param cursor :containing all Units to be indexed
     * @throws MetaDataExecutionException if the bulk update failed
     */
    final void updateBulkUnitsEntriesIndexes(MongoCursor<Unit> cursor, Integer tenantId)
            throws MetaDataExecutionException {
        if (!cursor.hasNext()) {
            LOGGER.error("ES update in error since no results to update");
            throw new MetaDataExecutionException("No result to update");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        while (cursor.hasNext()) {
            max--;
            final Unit unit = getFiltered(cursor.next());
            final String id = unit.getId();
            unit.remove(VitamDocument.ID);

            final String mongoJson = unit.toJson(new JsonWriterSettings(JsonMode.STRICT));
            // TODO Empty variable (null) might be ignore here
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
            final String toUpdate = dbObject.toString().trim();
            if (toUpdate.isEmpty()) {
                LOGGER.error("ES update in error since result to update is empty");
                throw new MetaDataExecutionException("Result to update is empty");
            }

            bulkRequest
                    .add(client.prepareUpdate(getIndexName(MetadataCollections.C_UNIT, tenantId), VitamCollection.getTypeunique(), id)
                            .setDoc(toUpdate, XContentType.JSON));
//                    .setDoc(toUpdate));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("ES update in error: " + bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
            if (bulkResponse.hasFailures()) {
                LOGGER.error("ES update in error: " + bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
    }

    /**
     * @param collection
     * @param tenantId
     * @param type
     * @param query      as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     *                   values" : [list of id] } }"
     * @param filter     the filter
     * @param sorts      the list of sort
     * @return a structure as ResultInterface
     * @throws MetaDataExecutionException
     */
    protected final Result search(final MetadataCollections collection, final Integer tenantId, final String type,
                                  final QueryBuilder query, final QueryBuilder filter, final List<SortBuilder> sorts, int offset, Integer limit,
                                  final String scrollId, final Integer scrollTimeout)
            throws MetaDataExecutionException {

        final SearchResponse response;
        final SearchRequestBuilder request;
        final boolean isUnit = collection == MetadataCollections.C_UNIT;
        final Result<?> resultRequest =
                isUnit ? MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS)
                        : MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        if (scrollId != null && !scrollId.isEmpty()) {
            int limitES = (limit != null && limit > 0) ? limit : GlobalDatasDb.DEFAULT_LIMIT_SCROLL;
            int scrollTimeoutES = (scrollTimeout != null && scrollTimeout > 0) ? scrollTimeout : GlobalDatasDb.DEFAULT_SCROLL_TIMEOUT;
            request = client.prepareSearch(getIndexName(collection, tenantId))
                    .setScroll(new TimeValue(scrollTimeoutES))
                    .setQuery(query)
                    .setSize(limitES);
            if (scrollId.equals(GlobalDatasDb.SCROLL_ACTIVATE_KEYWORD)) {
                response = request.get();
            } else {
                response = client.prepareSearchScroll(scrollId).setScroll(new TimeValue(scrollTimeoutES)).execute().actionGet();
            }
            resultRequest.setScrollId(response.getScrollId());
        } else {
            // Note: Could change the code to allow multiple indexes and multiple
            // types
            request = client.prepareSearch(getIndexName(collection, tenantId))
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setTypes(type).setExplain(false)
                    .setSize(GlobalDatas.LIMIT_LOAD).setFetchSource(MetadataDocument.ES_PROJECTION, null);
            if (offset != -1) {
                request.setFrom(offset);
            }
            if (limit != -1) {
                request.setSize(limit);
            }
            if (sorts != null) {
                sorts.stream().forEach(sort -> request.addSort(sort));
            }
            if (filter != null) {
                if (GlobalDatasDb.USE_FILTERED_REQUEST) {
                    final BoolQueryBuilder filteredQueryBuilder = QueryBuilders.boolQuery().must(query).must(filter);
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
            try {
                response = request.get();
            } catch (final Exception e) {
                LOGGER.debug(e.getMessage(), e);
                throw new MetaDataExecutionException(e.getMessage(), e);
            }
        }

        if (response.status() != RestStatus.OK) {
            LOGGER.error("Error " + response.status() + " from : " + request + ":" + query + " # " + filter);
            throw new MetaDataExecutionException("Error " + response.status());
        }
        final SearchHits hits = response.getHits();
        if (hits.getHits().length > GlobalDatas.LIMIT_LOAD) {
            LOGGER.warn("Warning, more than " + GlobalDatas.LIMIT_LOAD + " hits: " + hits.getTotalHits());
        }
        if (hits.getTotalHits() == 0) {
            LOGGER.error("No result from : " + request);
            return isUnit ? MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS)
                    : MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        // TODO to return the number of Units immediately below
        long nb = 0;
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
            resultRequest.addId(id, hit.getScore());
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("FinalEsResult: {} : {}", resultRequest.getCurrentIds(), resultRequest.getNbResult());
        }
        resultRequest.setTotal(hits.getTotalHits());
        return resultRequest;
    }

    /**
     * @param collections the working collection
     * @param tenantId    the tenant for operation
     * @param type        the type of document to delete
     * @param id          the id of document to delete
     * @throws MetaDataExecutionException if query operation exception occurred
     * @throws MetaDataNotFoundException  if item not found when deleting
     */
    public final void deleteEntryIndex(final MetadataCollections collections, Integer tenantId, final String type,
                                       final String id) throws MetaDataExecutionException, MetaDataNotFoundException {
        final DeleteRequestBuilder builder = client.prepareDelete(getIndexName(collections, tenantId), type, id);
        final DeleteResponse response;
        try {
            response = builder.setRefreshPolicy(RefreshPolicy.IMMEDIATE).get();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            throw new MetaDataExecutionException(e.getMessage(), e);
        }
        if (response.status() == RestStatus.NOT_FOUND) {
            throw new MetaDataNotFoundException("Item not found when trying to delete");
        }
    }

    /**
     * create indexes during Object group insert
     *
     * @param cursor   the {@link MongoCursor} of ObjectGroup
     * @param tenantId the tenant for operation
     * @throws MetaDataExecutionException when insert exception
     */
    public void insertBulkOGEntriesIndexes(MongoCursor<ObjectGroup> cursor, final Integer tenantId)
            throws MetaDataExecutionException {
        if (!cursor.hasNext()) {
            LOGGER.error("ES insert in error since no results to insert");
            throw new MetaDataExecutionException("No result to insert");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        while (cursor.hasNext()) {
            max--;
            final ObjectGroup og = cursor.next();
            final String id = og.getId();
            og.remove(VitamDocument.ID);
            og.remove(VitamDocument.SCORE);

            final String mongoJson = og.toJson(new JsonWriterSettings(JsonMode.STRICT));
            // TODO Empty variable (null) might be ignore here
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
            final String toInsert = dbObject.toString().trim();
            if (toInsert.isEmpty()) {
                LOGGER.error("ES insert in error since result to insert is empty");
                throw new MetaDataExecutionException("Result to insert is empty");
            }
            bulkRequest.add(client
                    .prepareIndex(getIndexName(MetadataCollections.C_OBJECTGROUP, tenantId), VitamCollection.getTypeunique(), id)
                    .setSource(toInsert, XContentType.JSON));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
                if (bulkResponse.hasFailures()) {
                    int duplicates = 0;
                    for (final BulkItemResponse bulkItemResponse : bulkResponse) {
                        if (bulkItemResponse.getVersion() > 1) {
                            duplicates++;
                        }
                    }
                    LOGGER.error("ES insert in error with possible duplicates {}: {}", duplicates,
                            bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
            if (bulkResponse.hasFailures()) {
                int duplicates = 0;
                for (final BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.getVersion() > 1) {
                        duplicates++;
                    }
                }
                LOGGER.error("ES insert in error with possible duplicates {}: {}", duplicates,
                        bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
    }

    /**
     * updateBulkOGEntriesIndexes
     * <p>
     * Update a set of entries in the ElasticSearch index based in Cursor Result. <br>
     *
     * @param cursor :containing all OG to be indexed
     * @throws MetaDataExecutionException if the bulk update failed
     */
    final boolean updateBulkOGEntriesIndexes(MongoCursor<ObjectGroup> cursor, final Integer tenantId)
            throws MetaDataExecutionException {
        if (!cursor.hasNext()) {
            LOGGER.error("ES update in error since no results to update");
            throw new MetaDataExecutionException("No result to update");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        while (cursor.hasNext()) {
            max--;
            final ObjectGroup og = cursor.next();
            final String id = og.getId();
            og.remove(VitamDocument.ID);
            og.remove(VitamDocument.SCORE);
            final String mongoJson = og.toJson(new JsonWriterSettings(JsonMode.STRICT));
            // TODO Empty variable (null) might be ignore here
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
            final String toUpdate = dbObject.toString().trim();
            if (toUpdate.isEmpty()) {
                LOGGER.error("ES update in error since result to update is empty");
                throw new MetaDataExecutionException("Result to update is empty");
            }

            bulkRequest.add(client.prepareUpdate(getIndexName(MetadataCollections.C_OBJECTGROUP, tenantId),
                    VitamCollection.getTypeunique(), id).setDoc(toUpdate));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("ES update in error: " + bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
            if (bulkResponse.hasFailures()) {
                LOGGER.error("ES update in error: " + bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
        return true;
    }

    /**
     * Update one element fully
     *
     * @param collection
     * @param tenantId
     * @param id
     * @param object     full object
     * @return True if updated
     */
    public boolean updateFullOneOG(MetadataCollections collection, Integer tenantId, String id, ObjectGroup og) {
        og.remove(VitamDocument.ID);
        og.remove(VitamDocument.SCORE);
        final String mongoJson = og.toJson(new JsonWriterSettings(JsonMode.STRICT));
        // TODO Empty variable (null) might be ignore here
        final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
        final String toUpdate = dbObject.toString().trim();
        UpdateResponse response = client.prepareUpdate(getIndexName(MetadataCollections.C_OBJECTGROUP, tenantId),
                VitamCollection.getTypeunique(), id)
                .setDoc(toUpdate, XContentType.JSON)
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet();

        return response.getId().equals(id);
    }

    /**
     * deleteBulkOGEntriesIndexes
     * <p>
     * Bulk to delete entry indexes
     *
     * @param ids      list of ids of OG
     * @param tenantId the tenant for operation
     * @return boolean true if delete ok
     * @throws MetaDataExecutionException when delete index exception occurred
     */
    public boolean deleteBulkOGEntriesIndexes(List<String> ids, final Integer tenantId)
            throws MetaDataExecutionException {
        if (ids.isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            throw new MetaDataExecutionException("No result to delete");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        for (String id : ids) {
            max--;
            bulkRequest.add(client.prepareDelete(getIndexName(MetadataCollections.C_OBJECTGROUP, tenantId),
                    VitamCollection.getTypeunique(), id));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet(); // new thread
            if (bulkResponse.hasFailures()) {
                LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
        return true;
    }

    /**
     * deleteBulkUnitEntriesIndexes
     * <p>
     * Bulk to delete entry indexes
     *
     * @param ids      containing all Unit to be delete
     * @param tenantId the tenant of operation
     * @throws MetaDataExecutionException when delete exception occurred
     */
    public void deleteBulkUnitsEntriesIndexes(List<String> ids, final Integer tenantId)
            throws MetaDataExecutionException {
        if (ids.isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            throw new MetaDataExecutionException("No result to delete");
        }
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        int max = VitamConfiguration.getMaxElasticsearchBulk();
        for (String id : ids) {
            max--;
            bulkRequest
                    .add(client.prepareDelete(getIndexName(MetadataCollections.C_UNIT, tenantId), VitamCollection.getTypeunique(), id));
            if (max == 0) {
                max = VitamConfiguration.getMaxElasticsearchBulk();
                final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.NONE).execute().actionGet(); // new
                if (bulkResponse.hasFailures()) {
                    LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                    throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
                }
                bulkRequest = client.prepareBulk();
            }
        }
        if (bulkRequest.numberOfActions() > 0) {
            final BulkResponse bulkResponse = bulkRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute().actionGet(); // new thread
            if (bulkResponse.hasFailures()) {
                LOGGER.error("ES delete in error: " + bulkResponse.buildFailureMessage());
                throw new MetaDataExecutionException(bulkResponse.buildFailureMessage());
            }
        }
    }

    private String getIndexName(final MetadataCollections collection, Integer tenantId) {
        return collection.getName().toLowerCase() + "_" + tenantId.toString();
    }

    private String getMapping(MetadataCollections collection) throws IOException {
        if (collection == MetadataCollections.C_UNIT) {
            return ElasticsearchUtil.transferJsonToMapping(Unit.class.getResourceAsStream(MAPPING_UNIT_FILE));
        } else if (collection == MetadataCollections.C_OBJECTGROUP) {
            return ElasticsearchUtil
                    .transferJsonToMapping(ObjectGroup.class.getResourceAsStream(MAPPING_OBJECT_GROUP_FILE));
        }
        return "";
    }
}
