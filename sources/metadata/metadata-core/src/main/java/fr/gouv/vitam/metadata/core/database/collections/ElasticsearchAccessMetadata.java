/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchFacetResultHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ElasticSearch model with MongoDB as main database
 */
public class ElasticsearchAccessMetadata extends ElasticsearchAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessMetadata.class);

    private final MappingLoader mappingLoader;
    /**
     * @param clusterName cluster name
     * @param nodes list of elasticsearch node
     * @throws VitamException if nodes list is empty
     */
    public ElasticsearchAccessMetadata(final String clusterName, List<ElasticsearchNode> nodes,
        MappingLoader mappingLoader)
        throws VitamException, IOException {
        super(clusterName, nodes);
        this.mappingLoader = mappingLoader;
    }

    /**
     * Add a type to an index
     *
     * @param collection the working metadata collection
     * @param tenantId the tenant for operation
     * @return key aliasName value indexName or empty
     */
    public final Map<String, String> addIndex(final MetadataCollections collection, Integer tenantId) {
        try {
            return super.createIndexAndAliasIfAliasNotExists(collection.getName().toLowerCase(),
                getMapping(collection, mappingLoader),
                tenantId);
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return new HashMap<>();
        }
    }


    /**
     * @param collection
     * @param tenantId
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     * values" : [list of id] } }"
     * @param sorts the list of sort
     * @param facets the list of facet
     * @return a structure as ResultInterface
     * @throws MetaDataExecutionException
     */
    protected final Result search(final MetadataCollections collection, final Integer tenantId,
        final QueryBuilder query, final List<SortBuilder> sorts, int offset, Integer limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws MetaDataExecutionException, BadRequestException {

        final SearchResponse response;
        try {
            response = super
                .search(collection.getName().toLowerCase(), tenantId, query, null, MetadataDocument.ES_PROJECTION,
                    sorts,
                    offset,
                    limit, facets, scrollId, scrollTimeout);
        } catch (DatabaseException e) {
            throw new MetaDataExecutionException(e);
        }

        switch (response.status()) {
            case OK:
            case NOT_FOUND:
            case NO_CONTENT:
                break;

        }

        if (response.status() != RestStatus.OK) {
            throw new MetaDataExecutionException(
                "Error collection : " + collection.getName() + ", tenant: " + tenantId + ", query: " + query);
        }


        final boolean isUnit = collection == MetadataCollections.UNIT;
        final Result<?> resultRequest =
            isUnit ? MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS)
                : MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);


        if (scrollId != null && !scrollId.isEmpty()) {
            resultRequest.setScrollId(response.getScrollId());
            SearchHits hits = response.getHits();
            if (hits.getHits().length == 0) {
                //Release search contexts as soon as they are not necessary anymore using the Clear Scroll API.
                try {
                    super.clearScroll(response.getScrollId());
                } catch (DatabaseException e) {
                    // Should be automatically cleared after timeout reached
                    LOGGER.warn(e);
                }
            }

        }

        final SearchHits hits = response.getHits();
        if (hits.getHits().length > GlobalDatas.LIMIT_LOAD) {
            LOGGER.warn("Warning, more than " + GlobalDatas.LIMIT_LOAD + " hits: " + hits.getTotalHits());
        }
        if (hits.getTotalHits().value == 0) {
            LOGGER.debug(
                "No result found collection : " + collection.getName() + ", tenant: " + tenantId + ", query: " + query);
            return isUnit ? MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS)
                : MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }

        for (SearchHit hit : hits) {
            final String id = hit.getId();
            resultRequest.addId(id, hit.getScore());
        }

        LOGGER.debug("FinalEsResult: {} : {}", resultRequest.getCurrentIds(), resultRequest.getNbResult());

        resultRequest.setTotal(hits.getTotalHits().value);

        // facets
        Aggregations aggregations = response.getAggregations();
        if (aggregations != null) {
            for (Aggregation aggregation : aggregations) {
                resultRequest
                    .addFacetResult(ElasticsearchFacetResultHelper.transformFromEsAggregation(aggregation));
            }
        }

        return resultRequest;
    }

    /**
     * Makes a search request on elasticsearch on a collection with aggregations and a query
     *
     * @param collection on which the request is made
     * @param tenantId on which the request is made
     * @param aggregations elasticsearch
     * @param query elasticsearch
     * @return the elasticsearch SearchResponse
     */
    public SearchResponse basicSearch(MetadataCollections collection, Integer tenantId,
        List<AggregationBuilder> aggregations, QueryBuilder query)
        throws MetaDataExecutionException {
        try {
            return super.search(collection.getName().toLowerCase(), tenantId, query, null, null,
                Lists.newArrayList(SortBuilders.fieldSort(FieldSortBuilder.DOC_FIELD_NAME).order(SortOrder.ASC)), 0,
                GlobalDatas.LIMIT_LOAD, aggregations, null, null);
        } catch (DatabaseException | BadRequestException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    /**
     * Insert one element
     *
     * @param collection
     * @param tenantId
     * @param id
     * @param doc full document to insert
     */
    public void insertFullDocument(MetadataCollections collection, Integer tenantId, String id, MetadataDocument doc)
        throws MetaDataExecutionException {
        try {
            super.indexEntry(collection.getName().toLowerCase(), tenantId, id, doc);
        } catch (DatabaseException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    public void insertFullDocuments(MetadataCollections collection, Integer tenantId,
        Collection<? extends MetadataDocument> documents)
        throws MetaDataExecutionException {

        try {
            super.indexEntries(collection.getName().toLowerCase(), tenantId, documents);
        } catch (DatabaseException e) {
            throw new MetaDataExecutionException(e);
        }
    }


    /**
     * Update one element fully
     *
     * @param collection
     * @param tenantId
     * @param id
     * @param metadataDocument full document to update
     */
    public void updateFullDocument(MetadataCollections collection, Integer tenantId, String id,
        MetadataDocument metadataDocument)
        throws MetaDataExecutionException {
        try {
            super.updateEntry(collection.getName().toLowerCase(), tenantId, id, metadataDocument);
        } catch (DatabaseException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    public void deleteBulkOGEntriesIndexes(List<String> ids, final Integer tenantId)
        throws MetaDataExecutionException {
        if (ids.isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            throw new MetaDataExecutionException("No result to delete");
        }
        try {
            super.delete(MetadataCollections.OBJECTGROUP.getName().toLowerCase(), ids, tenantId);
        } catch (DatabaseException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
    }

    public void deleteBulkUnitsEntriesIndexes(List<String> ids, final Integer tenantId)
        throws MetaDataExecutionException {
        if (ids.isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            throw new MetaDataExecutionException("No result to delete");
        }

        try {
            super.delete(MetadataCollections.UNIT.getName().toLowerCase(), ids, tenantId);
        } catch (DatabaseException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
    }

    private String getMapping(MetadataCollections collection,
        MappingLoader mappingLoader)
        throws IOException {

        switch (collection) {
            case UNIT:
                return ElasticsearchUtil.transferJsonToMapping(mappingLoader.loadMapping(collection.name()));
            case OBJECTGROUP:
                return ElasticsearchUtil.transferJsonToMapping(mappingLoader.loadMapping(collection.name()));
            default:
                throw new IOException("The given collection is not a metadata collection");
        }
    }
}
