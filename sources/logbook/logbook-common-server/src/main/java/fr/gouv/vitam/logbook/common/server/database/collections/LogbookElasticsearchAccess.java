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
package fr.gouv.vitam.logbook.common.server.database.collections;

import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ElasticSearch model with MongoDB as main database with management of index and index entries
 */
public class LogbookElasticsearchAccess extends ElasticsearchAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookElasticsearchAccess.class);
    public static final String MAPPING_LOGBOOK_OPERATION_FILE = "/logbook-es-mapping.json";

    /**
     * @param clusterName cluster name
     * @param nodes elasticsearch node
     * @throws VitamException if elasticsearch nodes list is empty/null
     */
    public LogbookElasticsearchAccess(final String clusterName, List<ElasticsearchNode> nodes)
        throws VitamException, IOException {
        super(clusterName, nodes);
    }

    /**
     * Delete an index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @return True if deleted
     */
    public final boolean deleteIndex(final LogbookCollections collection, final Integer tenantId) {
        LOGGER.debug("deleteIndex: " + getAliasName(collection, tenantId));
        try {
            if (getClient().admin().indices().prepareExists(getAliasName(collection, tenantId)).get().isExists()) {
                String indexName =
                    getClient().admin().indices().prepareGetAliases(getAliasName(collection, tenantId)).get()
                        .getAliases()
                        .iterator().next().key;
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);

                if (!getClient().admin().indices().delete(deleteIndexRequest).get().isAcknowledged()) {
                    LOGGER.error("Error on index delete");
                    return false;
                }
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            return false;
        }
    }

    /**
     * Add a type to an index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @return key aliasName value indexName or empty
     */
    public final Map<String, String> addIndex(final LogbookCollections collection, final Integer tenantId) {
        try {
            return super.createIndexAndAliasIfAliasNotExists(collection.getName().toLowerCase(), getMapping(),
                VitamCollection.getTypeunique(), tenantId);
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return new HashMap<>();
        }
    }


    /**
     * Refresh an index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     */
    public final void refreshIndex(final LogbookCollections collection, final Integer tenantId) {
        LOGGER.debug("refreshIndex: " + getAliasName(collection, tenantId));
        getClient().admin().indices().prepareRefresh(getAliasName(collection, tenantId))
            .execute().actionGet();

    }

    /**
     * Add a set of entries in the ElasticSearch index. <br>
     * Used in reload from scratch.
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @param mapIdJson map of documents as json by id
     * @return the listener on bulk insert
     */
    final BulkResponse addEntryIndexes(final LogbookCollections collection, final Integer tenantId,
        final Map<String, String> mapIdJson) {
        final BulkRequestBuilder bulkRequest = getClient().prepareBulk();

        // either use client#prepare, or use Requests# to directly build index/delete requests
        final String type = getTypeUnique(collection);
        for (final Entry<String, String> val : mapIdJson.entrySet()) {
            bulkRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .add(getClient().prepareIndex(getAliasName(collection, tenantId), type,
                    val.getKey()).setSource(val.getValue(), XContentType.JSON));// .setSource(val.getValue()));
        }
        return bulkRequest.execute().actionGet();
    }

    /**
     * Update an entry in the ElasticSearch index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @param id the id of the entry
     * @param json the entry document as a json
     * @return True if updated
     */
    final boolean updateEntryIndex(final LogbookCollections collection, final Integer tenantId,
        final String id, final String json) {
        final String type = LogbookOperation.TYPEUNIQUE;
        return getClient().prepareUpdate(getAliasName(collection, tenantId), type, id)
            .setDoc(json, XContentType.JSON).setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute()
            .actionGet().getVersion() > 1;
    }

    /**
     * Search entries in the ElasticSearch index.
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     * values" : [list of id] } }"
     * @param filter the filter
     * @param sorts the list of sort
     * @param offset the offset
     * @param limit the limit
     * @return a structure as SearchResponse
     * @throws LogbookException thrown of an error occurred while executing the request
     */
    public final SearchResponse search(final LogbookCollections collection, final Integer tenantId,
        final QueryBuilder query,
        final QueryBuilder filter, final List<SortBuilder> sorts, final int offset, final int limit)
        throws LogbookException {
        final String type = getTypeUnique(collection);

        final SearchRequestBuilder request =
            getClient().prepareSearch(getAliasName(collection, tenantId)).setSearchType(SearchType.DEFAULT)
                .setTypes(type).setExplain(false).setFrom(offset)
                .setFetchSource(VitamDocument.ES_FILTER_OUT, null)
                .setSize(GlobalDatas.LIMIT_LOAD < limit ? GlobalDatas.LIMIT_LOAD : limit);

        if (sorts != null) {
            sorts.stream().forEach(sort -> request.addSort(sort));
        }
        if (filter != null) {
            request.setQuery(query).setPostFilter(filter);
        } else {
            request.setQuery(query);
        }
        try {
            LOGGER.debug(request.toString());
            return request.get();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new LogbookDatabaseException(e);
        }
    }


    private String getTypeUnique(LogbookCollections collection) {
        switch (collection) {
            case OPERATION:
                return LogbookOperation.TYPEUNIQUE;
            case LIFECYCLE_UNIT:
            case LIFECYCLE_UNIT_IN_PROCESS:
            case LIFECYCLE_OBJECTGROUP:
            case LIFECYCLE_OBJECTGROUP_IN_PROCESS:
            default:
                return "";
        }
    }

    private String getAliasName(final LogbookCollections collection, Integer tenantId) {
        return collection.getName().toLowerCase() + "_" + tenantId.toString();
    }

    private String getMapping() throws IOException {
        return ElasticsearchUtil
            .transferJsonToMapping(LogbookOperation.class.getResourceAsStream(MAPPING_LOGBOOK_OPERATION_FILE));
    }
}
