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
/**
 *
 */
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.query.PathQuery;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.server.MongoDbInMemory;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.DeleteToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.InsertToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.RequestToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.UpdateToMongodb;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationStatus;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidUpdateException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.metadata.core.graph.GraphService;
import org.apache.commons.lang.StringUtils;
import org.bson.conversions.Bson;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * DB Request using MongoDB only
 */
public class DbRequest {

    private static final String QUERY2 = "query: ";

    private static final String WHERE_PREVIOUS_RESULT_WAS = "where_previous_result_was: ";

    private static final String FROM2 = "from: ";

    private static final String NO_RESULT_AT_RANK2 = "no_result_at_rank: ";

    private static final String NO_RESULT_TRUE = "no_result: true";

    private static final String WHERE_PREVIOUS_IS = " \n\twhere previous is ";

    private static final String FROM = " from ";

    private static final String NO_RESULT_AT_RANK = "No result at rank: ";

    private static final String DEPTH_ARRAY = "deptharray";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);
    private static final String
        CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S =
        "[Consistency Error] : The document guid=%s in ES is not in MongoDB anymore, tenant : %s, requestId : %s";

    private GraphService graphService;

    @VisibleForTesting
    DbRequest(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Constructor
     */
    public DbRequest() {
        this(new GraphService(new MongoDbMetadataRepository(MetadataCollections.UNIT.getCollection())));
    }

    /**
     * The request should be already analyzed.
     *
     * @param requestParser the RequestParserMultiple to execute
     * @return the Result
     * @throws MetaDataExecutionException     when select/update/delete on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws BadRequestException
     */
    public Result execRequest(final RequestParserMultiple requestParser)
        throws MetaDataExecutionException,
        InvalidParseOperationException, BadRequestException,
        VitamDBException {
        final RequestMultiple request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        boolean checkConsistency = false;
        Result<MetadataDocument<?>> roots;
        if (requestParser.model() == FILTERARGS.UNITS) {
            roots = checkUnitStartupRoots(requestParser);
        } else {
            // OBJECTGROUPS:
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("OBJECTGROUPS DbRequest: %s", requestParser.toString()));
            }
            roots = checkObjectGroupStartupRoots(requestParser);
        }

        Result<MetadataDocument<?>> result = roots;
        int rank = 0;
        // if roots is empty, check if first query gives a non empty roots (empty query allowed for insert)
        if (result.getCurrentIds().isEmpty() && maxQuery > 0) {
            final Result<MetadataDocument<?>> newResult = executeQuery(requestParser, requestToMongodb, rank, result);

            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
                checkConsistency = true;
            } else {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(requestParser.model())
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result).setTotal(newResult != null ? newResult.total : 0);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            rank++;
        }
        // Stops if no result (empty)
        for (; !result.getCurrentIds().isEmpty() && rank < maxQuery; rank++) {
            final Result<MetadataDocument<?>> newResult = executeQuery(requestParser, requestToMongodb, rank, result);
            if (newResult == null) {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(result.type)
                    .addError(result.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (!newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
        }
        // others do not allow empty result
        if (result.getCurrentIds().isEmpty()) {
            LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
            // XXX TODO P1 should be adapted to have a correct error feedback
            result = new ResultError(result.type)
                .addError(result != null ? result.getCurrentIds().toString() : NO_RESULT_TRUE)
                .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                .addError(WHERE_PREVIOUS_RESULT_WAS + result);
            return result;
        }
        if (request instanceof UpdateMultiQuery) {
            final Result<MetadataDocument<?>> newResult =
                lastUpdateFilterProjection((UpdateToMongodb) requestToMongodb, result, requestParser);
            if (newResult != null) {
                result = newResult;
            }
        } else if (request instanceof DeleteMultiQuery) {
            final Result<MetadataDocument<?>> newResult =
                lastDeleteFilterProjection((DeleteToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        } else {
            // Select part
            // get facets
            // get result
            final Result<MetadataDocument<?>> newResult =
                lastSelectFilterProjection((SelectToMongodb) requestToMongodb, result, checkConsistency);

            if (newResult != null) {
                result = newResult;
            }
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Results: {}", result);
        }
        return result;
    }


    /**
     * Check Unit at startup against Roots
     *
     * @param request
     * @return the valid root ids
     */
    protected Result<MetadataDocument<?>> checkUnitStartupRoots(final RequestParserMultiple request) {
        final Set<String> roots = request.getRequest().getRoots();
        if (roots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS, roots);
    }

    /**
     * Check ObjectGroup at startup against Roots
     *
     * @param request
     * @return the valid root ids
     */
    protected Result<MetadataDocument<?>> checkObjectGroupStartupRoots(final RequestParserMultiple request) {
        // TODO P1 add unit tests
        final Set<String> roots = request.getRequest().getRoots();
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
    }

    /**
     * Check Unit parents against Roots
     *
     * @param current         set of result id
     * @param defaultStartSet
     * @return the valid root ids set
     */
    protected Set<String> checkUnitAgainstRoots(final Set<String> current,
        final Result<MetadataDocument<?>> defaultStartSet) {
        // roots
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return current;
        }
        // TODO P1 add unit tests
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable =
            (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.UNIT,
                MongoDbMetadataHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
                MongoDbMetadataHelper.ID_PROJECTION);
        final Set<String> newRoots = new HashSet<>();
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                newRoots.add(unit.getId());
            }
        }
        return newRoots;
    }

    /**
     * Execute one request
     *
     * @param requestToMongodb
     * @param rank             current rank query
     * @param previous         previous Result from previous level (except in level == 0 where it is the subset of valid roots)
     * @return the new Result from this request
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> executeQuery(final RequestParserMultiple requestParser,
        final RequestToAbstract requestToMongodb, final int rank,
        final Result<MetadataDocument<?>> previous)
        throws MetaDataExecutionException, InvalidParseOperationException, BadRequestException {
        final Query realQuery = requestToMongodb.getNthQuery(rank);
        final boolean isLastQuery = requestToMongodb.getNbQueries() == rank + 1;
        List<SortBuilder> sorts = null;
        List<AggregationBuilder> facets = null;
        int limit = -1;
        int offset = -1;
        String scrollId = requestParser.getFinalScrollId();
        Integer scrollTimeout = requestParser.getFinalScrollTimeout();
        final Integer tenantId = ParameterHelper.getTenantParameter();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (requestToMongodb instanceof SelectToMongodb && isLastQuery) {
            VitamCollection.setMatch(false);
            sorts =
                QueryToElasticsearch.getSorts(requestParser, realQuery.isFullText() || VitamCollection.containMatch(),
                    collectionType.equals(FILTERARGS.UNITS) ? MetadataCollections.UNIT.useScore()
                        : MetadataCollections.OBJECTGROUP.useScore());
            if (FILTERARGS.UNITS.equals(collectionType)) {
                facets = QueryToElasticsearch.getFacets(requestParser);
            }
            VitamCollection.setMatch(false);
            limit = requestToMongodb.getFinalLimit();
            offset = requestToMongodb.getFinalOffset();
        }

        if (GlobalDatasDb.PRINT_REQUEST && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + realQuery.getCurrentQuery());
        }
        final QUERY type = realQuery.getQUERY();
        if (type == QUERY.PATH) {
            // Check if path is compatible with previous
            if (previous.getCurrentIds().isEmpty()) {
                previous.clear();
                return MongoDbMetadataHelper.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
            }
            if (collectionType.equals(FILTERARGS.UNITS)) {
                final Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
                previous.clear();
                if (newRoots.isEmpty()) {
                    return MongoDbMetadataHelper.createOneResult(collectionType);
                }
                return MongoDbMetadataHelper.createOneResult(collectionType, newRoots);
            } else {
                // FIXME TODO check against _up of OG
                return previous;
            }
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatas.MAXDEPTH;
        }
        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result result;
        try {
            if (collectionType == FILTERARGS.UNITS) {
                if (exactDepth > 0) {
                    // Exact Depth request (descending)
                    LOGGER.debug("Unit Exact Depth request (descending)");
                    result = exactDepthUnitQuery(realQuery, previous, exactDepth, tenantId, sorts,
                        offset, limit, facets, scrollId, scrollTimeout);
                } else if (relativeDepth != 0) {
                    // Relative Depth request (ascending or descending)
                    LOGGER.debug("Unit Relative Depth request (ascending or descending)");
                    result =
                        relativeDepthUnitQuery(realQuery, previous, relativeDepth, tenantId, sorts,
                            offset, limit, facets, scrollId, scrollTimeout);
                } else {
                    // Current sub level request
                    LOGGER.debug("Unit Current sub level request");
                    result = sameDepthUnitQuery(realQuery, previous, tenantId, sorts, offset,
                        limit, facets, scrollId, scrollTimeout);
                }
            } else {
                // OBJECTGROUPS
                // No depth at all
                // FIXME later on see if we should support depth
                LOGGER.debug("ObjectGroup No depth at all");
                result = objectGroupQuery(realQuery, previous, tenantId, sorts, offset,
                    limit, scrollId, scrollTimeout);
            }
        } finally {
            previous.clear();
        }
        return result;
    }

    /**
     * Execute one Unit Query using exact Depth
     *
     * @param realQuery
     * @param previous
     * @param exactDepth
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> exactDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        int exactDepth, Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        final BoolQueryBuilder roots =
            new BoolQueryBuilder().must(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(exactDepth).gte(0))
                .must(QueryBuilders.rangeQuery(Unit.MINDEPTH).lte(exactDepth).gte(0));
        if (!previous.getCurrentIds().isEmpty()) {
            roots.must(QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds()));
        }

        // lets add the query on the tenant
        BoolQueryBuilder query = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(realQuery))
            .filter(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId))
            .filter(QueryBuilders.termQuery(Unit.UNITUPS + "." + exactDepth, previous.getCurrentIds()));

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Req1LevelMD: {}", query);
        }

        final Result<MetadataDocument<?>> result =
            MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
                VitamCollection.getTypeunique(), query, sorts, offset, limit, facets, scrollId, scrollTimeout);

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("UnitExact: {}", result);
        }
        return result;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @param relativeDepth
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> relativeDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        int relativeDepth, Integer tenantId, final List<SortBuilder> sorts, final int offset,
        final int limit, final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        QueryBuilder roots;

        // lets add the query on the tenant
        BoolQueryBuilder query = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(realQuery));

        if (previous.getCurrentIds().isEmpty()) {
            if (relativeDepth < 1) {
                query.filter(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(1).gte(0));
            } else {
                query.filter(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(relativeDepth + 1).gte(0));
            }
        } else {
            if (relativeDepth == 1) {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds());
                query.filter(roots);
            } else if (relativeDepth > 1) {
                QueryToElasticsearch.addRoots(query, Unit.UNITDEPTHS, previous.getCurrentIds(), relativeDepth);

            } else {
                // Relative parent: previous has future result in their _up
                // so future result ids are in previous UNITDEPTHS
                final Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
                roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, fathers);
                query.filter(roots);
            }
        }

        query.filter(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Req1LevelMD: {}", query);
        }

        final Result<MetadataDocument<?>> result =
            MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
                VitamCollection.getTypeunique(), query, sorts, offset, limit, facets, scrollId, scrollTimeout);

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("UnitRelative: {}", result);
        }

        return result;
    }

    /**
     * Aggregate Unit Depths according to parent relative Depth
     *
     * @param ids
     * @param relativeDepth
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Collection<String> ids, int relativeDepth) {
        // TODO P1 add unit tests
        // Select all items from ids
        final Bson match = match(in(MetadataDocument.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(new BasicDBObject(MetadataDocument.ID, "all"),
            addToSet(DEPTH_ARRAY, BuilderToken.DEFAULT_PREFIX + Unit.UNITDEPTHS));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Depth: {} {}", MongoDbHelper.bsonToString(match, false),
                MongoDbHelper.bsonToString(group, false));
        }
        final List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
        final AggregateIterable<Unit> aggregateIterable =
            MetadataCollections.UNIT.getCollection().aggregate(pipeline);
        final Unit aggregate = aggregateIterable.first();
        final Set<String> set = new HashSet<>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            final List<Map<String, List<String>>> array =
                (List<Map<String, List<String>>>) aggregate.get(DEPTH_ARRAY);
            relativeDepth = Math.abs(relativeDepth);
            for (final Map<String, List<String>> map : array) {
                for (final String key : map.keySet()) {
                    int depth = Integer.parseInt(key);
                    if (depth <= relativeDepth) {
                        set.addAll(map.get(key));
                    }
                }
                map.clear();
            }
            array.clear();
        }
        return set;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> sameDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
        QueryBuilder finalQuery;
        LOGGER.debug("DEBUG prev {} RealQuery {}", previous.getCurrentIds(), realQuery);
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final QueryBuilder roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
            finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
        }
        if (tenantId != null) {
            // lets add the query on the tenant
            finalQuery = new BoolQueryBuilder().must(finalQuery)
                .must(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
            VitamCollection.getTypeunique(), finalQuery, sorts, offset, limit, facets, scrollId, scrollTimeout);
    }

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous  units, Note: only immediate Unit parents are allowed
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> objectGroupQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
        QueryBuilder finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final QueryBuilder roots;
            if (FILTERARGS.UNITS.equals(previous.getType())) {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds());
            } else {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
            }
            finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
        }
        if (tenantId != null) {
            // lets add the query on the tenant
            finalQuery = new BoolQueryBuilder().must(finalQuery)
                .must(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.OBJECTGROUP.getEsClient().search(MetadataCollections.OBJECTGROUP, tenantId,
            VitamCollection.getTypeunique(), finalQuery, sorts, offset, limit, null, scrollId, scrollTimeout);
    }

    /**
     * Finalize the queries with last True Select
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     */
    protected Result<MetadataDocument<?>> lastSelectFilterProjection(SelectToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last, boolean checkConsistency)
        throws InvalidParseOperationException, VitamDBException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final boolean isIdIncluded = requestToMongodb.idWasInProjection();
        final FILTERARGS model = requestToMongodb.model();
        final List<String> desynchronizedResults = new ArrayList<>();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("To Select: " + MongoDbHelper.bsonToString(roots, false) + " " +
                (projection != null ? MongoDbHelper.bsonToString(projection, false) : ""));
        }
        if (model == FILTERARGS.UNITS) {
            final Map<String, Unit> units = new HashMap<>();
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.UNIT,
                    roots, projection, null, -1, -1);
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    units.put(unit.getId(), unit);
                }
            }
            int nbScore = last.scores == null ? -1 : last.scores.size();
            for (int i = 0; i < last.getCurrentIds().size(); i++) {
                final String id = last.getCurrentIds().get(i);
                Unit unit = units.get(id);
                if (unit != null) {
                    if (VitamConfiguration.isExportScore() && MetadataCollections.UNIT.useScore() &&
                        requestToMongodb.isScoreIncluded()) {
                        Float score = Float.valueOf(1);
                        try {
                            score = last.scores.get(i);
                            if (score.isNaN()) {
                                score = Float.valueOf(1);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e);

                        }
                        unit.append(VitamDocument.SCORE, score);
                    }
                    if (!isIdIncluded) {
                        unit.remove(VitamDocument.ID);
                    }
                    last.addFinal(unit);
                } else if (checkConsistency) {
                    // check the consistency between elasticSearch and MongoDB
                    desynchronizedResults.add(id);
                    // desynchronization logs
                    LOGGER.error(String.format(
                        CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                        id, ParameterHelper.getTenantParameter(), VitamThreadUtils.getVitamSession().getRequestId()));
                }
            }

            // As soon as we detect a synchronization error MongoDB / ES, we return an error.
            if (!desynchronizedResults.isEmpty()) {
                throw new VitamDBException(
                    "[Consistency ERROR] : An internal data consistency error has been detected !");
            }

            return last;
        }
        // OBJECTGROUPS:
        final Map<String, ObjectGroup> obMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
                MetadataCollections.OBJECTGROUP,
                roots, projection, null, -1, -1);
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                obMap.put(og.getId(), og);
            }
        }
        int nbScore = last.scores == null ? -1 : last.scores.size();
        for (int i = 0; i < last.getCurrentIds().size(); i++) {
            final String id = last.getCurrentIds().get(i);
            ObjectGroup og = obMap.get(id);
            if (og != null) {
                if (VitamConfiguration.isExportScore() && MetadataCollections.OBJECTGROUP.useScore() &&
                    requestToMongodb.isScoreIncluded()) {
                    Float score = Float.valueOf(1);
                    try {
                        score = last.scores.get(i);
                        if (score.isNaN()) {
                            score = Float.valueOf(1);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                    og.append(VitamDocument.SCORE, score);
                }
                if (!isIdIncluded) {
                    og.remove(VitamDocument.ID);
                }
                last.addFinal(og);
            } else if (checkConsistency) {
                // check the consistency between elasticSearch and MongoDB
                desynchronizedResults.add(id);
                // desynchronization logs
                LOGGER.error(String.format(
                    CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                    id, ParameterHelper.getTenantParameter(), VitamThreadUtils.getVitamSession().getRequestId()));
            }
        }

        // As soon as we detect a synchronization error MongoDB / ES, we return an error.
        if (!desynchronizedResults.isEmpty()) {
            throw new VitamDBException(
                "[Consistency ERROR] : An internal data consistency error has been detected !");
        }

        return last;
    }

    /**
     * Finalize the queries with last True Update
     *
     * @param requestToMongodb
     * @param last
     * @param requestParser
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetadataInvalidUpdateException
     */
    protected Result<MetadataDocument<?>> lastUpdateFilterProjection(UpdateToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last,
        RequestParserMultiple requestParser)
        throws InvalidParseOperationException, MetaDataExecutionException, MetadataInvalidUpdateException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final FILTERARGS model = requestToMongodb.model();

        MongoCollection<MetadataDocument<?>> collection;
        if (model == FILTERARGS.UNITS) {
            collection = MetadataCollections.UNIT.getCollection();
        } else {
            collection = MetadataCollections.OBJECTGROUP.getCollection();
        }

        if (!requestToMongodb.isMultiple() && last.getNbResult() > 1) {
            throw new MetadataInvalidUpdateException(
                "Update Request is not multiple but found multiples entities to update");
        }
        final List<MetadataDocument<?>> listDocuments = new ArrayList<>();
        final FindIterable<MetadataDocument<?>> searchResults = collection.find(roots);
        final Iterator<MetadataDocument<?>> it = searchResults.iterator();
        while (it.hasNext()) {
            listDocuments.add(it.next());
        }

        last.clear();
        SchemaValidationUtils validator;
        try {
            validator = new SchemaValidationUtils();
        } catch (FileNotFoundException | ProcessingException e) {
            LOGGER.debug("Unable to initialize Json Validator");
            throw new MetaDataExecutionException(e);
        }

        for (final MetadataDocument<?> document : listDocuments) {
            final String documentId = document.getId();
            final Integer documentVersion = document.getVersion();

            UpdateResult result = null;
            int tries = 0;
            boolean modified = false;
            MetadataDocument<?> documentFinal = null;

            while (result == null && tries < 3) {
                final JsonNode jsonDocument = JsonHandler.toJsonNode(document);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("DEBUG update {} to udate to {}", jsonDocument,
                        MongoDbHelper.bsonToString(requestToMongodb.getFinalUpdateActions(), false));
                }
                final MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument);
                requestToMongodb.getFinalUpdateActions();
                final ObjectNode updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJson(requestParser);
                documentFinal = (MetadataDocument<?>) document.newInstance(updatedJsonDocument);
                if (documentId.equals(document.get(MetadataDocument.ID))) {
                    modified = true;
                    documentFinal.put(VitamDocument.VERSION, documentVersion.intValue() + 1);

                    if (model == FILTERARGS.UNITS) {
                        JsonNode externalSchema =
                            updatedJsonDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
                        JsonNode ontologyFields =
                            updatedJsonDocument.remove(SchemaValidationUtils.TAG_ONTOLOGY_FIELDS);
                        if (ontologyFields != null && ontologyFields.size() > 0) {
                            validateAndUpdateOntology(updatedJsonDocument, ontologyFields, validator);
                            documentFinal = (MetadataDocument<?>) document.newInstance(updatedJsonDocument);
                            documentFinal.put(VitamDocument.VERSION, documentVersion.intValue() + 1);
                            documentFinal.remove(SchemaValidationUtils.TAG_ONTOLOGY_FIELDS);
                        }
                        SchemaValidationStatus status = validator.validateInsertOrUpdateUnit(updatedJsonDocument.deepCopy());
                        if (!SchemaValidationStatusEnum.VALID.equals(status.getValidationStatus())) {
                            throw new MetaDataExecutionException(
                                "Unable to validate updated Unit " + status.getValidationMessage());
                        }
                        if (externalSchema != null && externalSchema.size() > 0) {
                            validateOtherExternalSchema(updatedJsonDocument, externalSchema);
                            documentFinal.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
                        }
                    }

                    // Make Update
                    final Bson condition =
                        and(eq(MetadataDocument.ID, documentId), eq(MetadataDocument.VERSION, documentVersion));
                    updatedJsonDocument.remove(VitamDocument.SCORE);
                    LOGGER.debug("DEBUG update {}", updatedJsonDocument);
                    result = collection.replaceOne(condition, documentFinal);
                    if (result.getModifiedCount() != 1) {
                        result = null;
                    }
                    tries++;
                } else {
                    break;
                }
            }

            if (modified && result == null) {
                throw new MetaDataExecutionException("Can not modify Document");
            }
            if (modified) {
                last.addId(documentId, (float) 1);

                try {
                    if (model == FILTERARGS.UNITS) {
                        indexFieldsUpdated(last, tenantId);
                    } else {
                        indexFieldsOGUpdated(last, tenantId);
                    }
                } catch (final Exception e) {
                    throw new MetaDataExecutionException("Update concern", e);
                }
            }
        }
        last.setTotal(last.getNbResult());
        return last;
    }

    private void validateOtherExternalSchema(ObjectNode updatedJsonDocument, JsonNode schema)
        throws InvalidParseOperationException, MetaDataExecutionException {
        try {
            SchemaValidationUtils validatorSecond =
                new SchemaValidationUtils(
                    schema.isArray()
                        ? schema.get(0).asText()
                        : schema.asText(),
                    true);
            updatedJsonDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
            SchemaValidationStatus secondstatus =
                    validatorSecond.validateInsertOrUpdateUnit(updatedJsonDocument.deepCopy());
            if (!SchemaValidationStatusEnum.VALID.equals(secondstatus.getValidationStatus())) {
                throw new MetaDataExecutionException(
                    "Unable to validate updated Unit " + secondstatus.getValidationMessage());
            }
        } catch (FileNotFoundException | ProcessingException e) {
            LOGGER.debug("Unable to initialize External Json Validator");
            throw new MetaDataExecutionException(e);
        }
    }

    private void validateAndUpdateOntology(ObjectNode updatedJsonDocument, JsonNode ontologyList,
        SchemaValidationUtils validator)
        throws MetaDataExecutionException {
        String finalOntologyAsString = ontologyList.isArray() ? ontologyList.get(0).asText() : ontologyList.asText();
        Map<String, OntologyModel> ontologyModelMap = new HashMap<String, OntologyModel>();
        try {
            ((ArrayNode) JsonHandler.getFromString(finalOntologyAsString)).forEach(ontology -> {
                try {
                    OntologyModel model = JsonHandler.getFromJsonNode(ontology, OntologyModel.class);
                    ontologyModelMap.put(model.getIdentifier(), model);
                } catch (InvalidParseOperationException e) {
                    LOGGER.warn("Unable to serialize ontology field");
                }
            });
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Could not parse ontologies", e);
            throw new MetaDataExecutionException(e);
        }
        List<String> errors = new ArrayList<String>();
        // that means a transformation could be done so we need to process the full json
        validator.verifyAndReplaceFields(updatedJsonDocument, ontologyModelMap, errors);

        if (!errors.isEmpty()) {
            // archive unit could not be transformed, so the error would be thrown later by the schema
            // validation verification
            String error = "Archive unit contains fields declared in ontology with a wrong format : " +
                String.join(",", errors.toString());
            LOGGER.error(error);
            throw new MetaDataExecutionException(error);
        }
    }

    /**
     * indexFieldsUpdated : Update index related to Fields updated
     *
     * @param last : contains the Result to be indexed
     * @throws Exception
     */
    private void indexFieldsUpdated(Result<MetadataDocument<?>> last, Integer tenantId) throws Exception {
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = and(eq(MetadataDocument.ID, last.getCurrentIds().iterator().next()),
                eq(MetadataDocument.TENANT_ID, tenantId));
        } else {
            finalQuery = and(in(MetadataDocument.ID, last.getCurrentIds()),
                eq(MetadataDocument.TENANT_ID, tenantId));
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
            .select(MetadataCollections.UNIT, finalQuery, Unit.UNIT_ES_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            MetadataCollections.UNIT.getEsClient().updateBulkUnitsEntriesIndexes(cursor, tenantId);
        }

    }

    /**
     * indexFieldsOGUpdated : Update index OG related to Fields updated
     *
     * @param last : contains the Result to be indexed
     * @throws Exception
     */
    private void indexFieldsOGUpdated(Result<MetadataDocument<?>> last, Integer tenantId) throws Exception {
        final Bson finalQuery;
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES update in error since no results to update");
            // no result to update
            return;
        }
        if (last.getCurrentIds().size() == 1) {
            finalQuery = eq(MetadataDocument.ID, last.getCurrentIds().iterator().next());
        } else {
            finalQuery = in(MetadataDocument.ID, last.getCurrentIds());
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper
                .select(MetadataCollections.OBJECTGROUP, finalQuery, ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        // TODO maybe retry once if in error ?
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            MetadataCollections.OBJECTGROUP.getEsClient().updateBulkOGEntriesIndexes(cursor, tenantId);
        }

    }


    /**
     * removeOGIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     * @throws Exception
     */
    private void removeOGIndexFields(Result<MetadataDocument<?>> last) throws Exception {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        MetadataCollections.OBJECTGROUP.getEsClient().deleteBulkOGEntriesIndexes(last.getCurrentIds(), tenantId);
    }

    /**
     * removeUnitIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     * @throws Exception
     */
    private void removeUnitIndexFields(Result<MetadataDocument<?>> last) throws Exception {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        MetadataCollections.UNIT.getEsClient().deleteBulkUnitsEntriesIndexes(last.getCurrentIds(), tenantId);
    }

    /**
     * Inserts a unit
     *
     * @param requestParser the InsertParserMultiple to execute
     * @throws MetaDataExecutionException     when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException  when insert metadata exception
     * @throws MetaDataNotFoundException      when metadata not found exception
     */
    public void execInsertUnitRequest(InsertParserMultiple requestParser)
        throws MetaDataExecutionException, MetaDataNotFoundException, InvalidParseOperationException,
        MetaDataAlreadyExistException {

        LOGGER.debug("Exec db insert unit request: %s", requestParser);

        try {

            final InsertToMongodb requestToMongodb =
                (InsertToMongodb) RequestToMongodb.getRequestToMongoDb(requestParser);

            final Unit unit = new Unit(requestToMongodb.getFinalData());

            String unitId = unit.getId();

            Set<String> roots = requestParser.getRequest().getRoots();
            graphService.compute(unit, roots);
            MetaDataAlreadyExistException metaDataAlreadyExistException = null;
            try {
                unit.insert();
            } catch (MetaDataAlreadyExistException e) {
                metaDataAlreadyExistException = e;
            }

            // Even if MetaDataAlreadyExistException occurs, reindex in elastic search and update object group
            final Integer tenantId = ParameterHelper.getTenantParameter();
            MetadataCollections.UNIT.getEsClient()
                .insertFullDocument(MetadataCollections.UNIT, tenantId, unitId,
                    unit);

            String ogId = unit.getString(MetadataDocument.OG);
            if (StringUtils.isNotEmpty(ogId)) {
                ObjectGroupGraphUpdates objectGroupGraphUpdates = new ObjectGroupGraphUpdates();
                objectGroupGraphUpdates.buildParentGraph(unit);
                Bson updates = objectGroupGraphUpdates.toBsonUpdate();

                ObjectGroup objectGroup =
                    (ObjectGroup) MetadataCollections.OBJECTGROUP.getCollection().findOneAndUpdate(
                        eq(VitamDocument.ID, ogId), updates,
                        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
                MetadataCollections.OBJECTGROUP.getEsClient()
                    .updateFullDocument(MetadataCollections.OBJECTGROUP, tenantId, ogId,
                        objectGroup);
            }

            if (null != metaDataAlreadyExistException) {
                LOGGER.warn(metaDataAlreadyExistException);
                throw metaDataAlreadyExistException;
            }

        } catch (final MongoWriteException e) {
            throw e;
        } catch (final MongoException e) {
            throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    /**
     * Inserts an object group
     *
     * @param requestParser the InsertParserMultiple to execute
     * @throws MetaDataExecutionException     when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException  when insert metadata exception
     */
    public void execInsertObjectGroupRequest(InsertParserMultiple requestParser)
        throws MetaDataExecutionException, InvalidParseOperationException, MetaDataAlreadyExistException {

        LOGGER.debug("Exec db insert object group request: %s", requestParser);

        try {

            final InsertToMongodb requestToMongodb =
                (InsertToMongodb) RequestToMongodb.getRequestToMongoDb(requestParser);

            final ObjectGroup og = new ObjectGroup(requestToMongodb.getFinalData());

            String odId = og.getId();

            MetaDataAlreadyExistException metaDataAlreadyExistException = null;
            try {
                og.insert();
            } catch (MetaDataAlreadyExistException e) {
                metaDataAlreadyExistException = e;
            }

            // Even if MetaDataAlreadyExistException occurs, reindex in elastic search
            final Integer tenantId = ParameterHelper.getTenantParameter();
            MetadataCollections.OBJECTGROUP.getEsClient()
                .insertFullDocument(MetadataCollections.OBJECTGROUP, tenantId, odId, og);

            if (null != metaDataAlreadyExistException) {
                LOGGER.warn(metaDataAlreadyExistException);
                throw metaDataAlreadyExistException;
            }

        } catch (final MongoWriteException e) {
            throw e;
        } catch (final MongoException e) {
            throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    /**
     * Finalize the queries with last True Delete
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result<MetadataDocument<?>> lastDeleteFilterProjection(DeleteToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        }
        if (!requestToMongodb.isMultiple() && last.getNbResult() > 1) {
            throw new MetaDataExecutionException(
                "Delete Request is not multiple but found multiples entities to delete");
        }
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final DeleteResult result = MongoDbMetadataHelper.delete(MetadataCollections.UNIT,
                    roots, last.getCurrentIds().size());
                if (result.getDeletedCount() != last.getNbResult()) {
                    LOGGER.warn("Deleted items different than specified");
                }
                removeUnitIndexFields(last);
                return last;
            }
            // TODO P1 add unit tests
            // OBJECTGROUPS:
            final DeleteResult result =
                MongoDbMetadataHelper.delete(MetadataCollections.OBJECTGROUP,
                    roots, last.getCurrentIds().size());
            if (result.getDeletedCount() != last.getNbResult()) {
                LOGGER.warn("Deleted items different than specified");
            }
            removeOGIndexFields(last);
            last.setTotal(last.getNbResult());
            return last;
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Delete concern", e);
        }
    }
}
