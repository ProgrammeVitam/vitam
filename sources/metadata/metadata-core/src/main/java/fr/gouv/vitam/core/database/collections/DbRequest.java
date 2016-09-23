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
/**
 *
 */
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.database.builder.request.multiple.Delete;
import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.Update;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.DeleteToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.InsertToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.RequestToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.UpdateToMongodb;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.query.PathQuery;

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

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);
    
    boolean debug = true;

    /**
     * Constructor
     */
    public DbRequest() {
        // Empty constructor
    }

    /**
     * @param debug If True, in debug mode
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    /**
     * The request should be already analyzed.
     *
     * @param requestParser
     * @param defaultStartSet the set of id from which the request should start, whatever the roots set
     * @return the Result
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     * @throws MetaDataAlreadyExistException 
     * @throws MetaDataNotFoundException 
     */
    public Result execRequest(final RequestParserMultiple requestParser, final Result defaultStartSet)
        throws InstantiationException, IllegalAccessException, MetaDataExecutionException,
        InvalidParseOperationException, MetaDataAlreadyExistException, MetaDataNotFoundException {
        final RequestMultiple request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        Result roots;
        if (requestParser.model() == FILTERARGS.UNITS) {
            roots = checkUnitStartupRoots(requestParser, defaultStartSet);
        } else {
            // OBJECTGROUPS:
            roots = checkObjectGroupStartupRoots(requestParser, defaultStartSet);
        }
        Result result = roots;
        int rank = 0;
        // if roots is empty, check if first query gives a non empty roots (empty query allowed for insert)
        if (result.getCurrentIds().isEmpty() && maxQuery > 0) {
            final Result newResult = executeQuery(requestToMongodb, rank, result);
            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO should be adapted to have a correct error feedback
                result = new ResultError(requestParser.model())
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);

                return result;
            }
            if (debug) {
                LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            }
            rank++;
        }
        // Stops if no result (empty)
        for (; !result.getCurrentIds().isEmpty() && rank < maxQuery; rank++) {
            final Result newResult = executeQuery(requestToMongodb, rank, result);
            if (newResult == null) {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO should be adapted to have a correct error feedback
                result = new ResultError(result.type)
                    .addError(result.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (!newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.error(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (debug) {
                LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            }
        }
        // Result contains the selection on which to act
        // Insert allow to have no result
        if (request instanceof Insert) {
            final Result newResult = lastInsertFilterProjection((InsertToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
                // index Metadata
                Set<String> ids = result.getCurrentIds();

                final FILTERARGS model = requestToMongodb.model();
                // index Unit
                if (model == FILTERARGS.UNITS) {
                    final Bson finalQuery = in(Unit.ID, ids);
                    @SuppressWarnings("unchecked")
                    final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
                        .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_ES_PROJECTION);
                    final MongoCursor<Unit> cursor = iterable.iterator();
                    try {
                        while (cursor.hasNext()) {
                            final Unit unit = cursor.next();
                            // TODO use Bulk
                            MetadataCollections.C_UNIT.getEsClient().addEntryIndex(unit);
                            }
                        } finally {
                            cursor.close();
                        }
                }
               // TODO index ObjectGroup
            }
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Results: " + result);
            }
            return result;
        }
        // others do not allow empty result
        if (result.getCurrentIds().isEmpty()) {
            LOGGER.error(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
            // XXX TODO should be adapted to have a correct error feedback
            result = new ResultError(result.type)
                .addError(result != null ? result.getCurrentIds().toString() : NO_RESULT_TRUE)
                .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                .addError(WHERE_PREVIOUS_RESULT_WAS + result);
            return result;
        }
        if (request instanceof Update) {
            final Result newResult = lastUpdateFilterProjection((UpdateToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        } else if (request instanceof Delete) {
            final Result newResult = lastDeleteFilterProjection((DeleteToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
                // Clean cache
                for (final String id : result.getCurrentIds()) {
                    MongoDbMetadataHelper.LRU.remove(id);
                }
            }
        } else {
            // Select part
            final Result newResult = lastSelectFilterProjection((SelectToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Results: " + result);
        }
        return result;
    }

    /**
     * Check Unit at startup against Roots
     *
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkUnitStartupRoots(final RequestParserMultiple request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        final Set<String> roots = request.getRequest().getRoots();
        final Set<String> newRoots = checkUnitAgainstRoots(roots, defaultStartSet);
        if (newRoots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS, newRoots);
    }

    /**
     * Check ObjectGroup at startup against Roots
     *
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkObjectGroupStartupRoots(final RequestParserMultiple request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        // TODO add unit tests
        final Set<String> roots = request.getRequest().getRoots();
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
        }
        if (roots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(MetadataCollections.C_OBJECTGROUP,
                MongoDbMetadataHelper.queryForAncestorsOrSame(roots, defaultStartSet.getCurrentIds()),
                ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        final MongoCursor<ObjectGroup> cursor = iterable.iterator();
        final Set<String> newRoots = new HashSet<String>();
        try {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                newRoots.add(og.getId());
            }
        } finally {
            cursor.close();
        }
        if (newRoots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, newRoots);
    }

    /**
     * Check Unit parents against Roots
     *
     * @param current set of result id
     * @param defaultStartSet
     * @return the valid root ids set
     * @throws InvalidParseOperationException
     */
    protected Set<String> checkUnitAgainstRoots(final Set<String> current, final Result defaultStartSet)
        throws InvalidParseOperationException {
        // roots
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return current;
        }
        // TODO add unit tests
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
            MongoDbMetadataHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
            MongoDbMetadataHelper.ID_PROJECTION);
        final MongoCursor<Unit> cursor = iterable.iterator();
        final Set<String> newRoots = new HashSet<String>();
        try {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                newRoots.add(unit.getId());
            }
        } finally {
            cursor.close();
        }
        return newRoots;
    }

    /**
     * Execute one request
     *
     * @param requestToMongodb
     * @param rank current rank query
     * @param previous previous Result from previous level (except in level == 0 where it is the subset of valid roots)
     * @return the new Result from this request
     * @throws MetaDataExecutionException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvalidParseOperationException
     */
    protected Result executeQuery(final RequestToAbstract requestToMongodb, final int rank, final Result previous)
        throws MetaDataExecutionException, InstantiationException,
        IllegalAccessException, InvalidParseOperationException {
        final Query realQuery = requestToMongodb.getNthQuery(rank);
        if (GlobalDatasDb.PRINT_REQUEST) {
            final String query = realQuery.getCurrentQuery().toString();
            LOGGER.debug("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + query);
        }
        final QUERY type = realQuery.getQUERY();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (type == QUERY.PATH) {
            // Check if path is compatible with previous
            if (previous.getCurrentIds().isEmpty()) {
                previous.clear();
                return MongoDbMetadataHelper.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
            }
            final Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
            previous.clear();
            if (newRoots.isEmpty()) {
                return MongoDbMetadataHelper.createOneResult(collectionType);
            }
            return MongoDbMetadataHelper.createOneResult(collectionType, newRoots);
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatasParser.MAXDEPTH;
        }
        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result result;
        try {
            if (collectionType == FILTERARGS.UNITS) {
                if (exactDepth > 0) {
                    // Exact Depth request (descending)
                    LOGGER.debug("Unit Exact Depth request (descending)");
                    result = exactDepthUnitQuery(realQuery, previous, exactDepth);
                } else if (relativeDepth != 0) {
                    // Relative Depth request (ascending or descending)
                    LOGGER.debug("Unit Relative Depth request (ascending or descending)");
                    result = relativeDepthUnitQuery(realQuery, previous, relativeDepth);
                } else {
                    // Current sub level request
                    LOGGER.debug("Unit Current sub level request");
                    result = sameDepthUnitQuery(realQuery, previous);
                }
            } else {
                // OBJECTGROUPS
                // No depth at all
                LOGGER.debug("ObjectGroup No depth at all");
                result = objectGroupQuery(realQuery, previous);
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
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result exactDepthUnitQuery(Query realQuery, Result previous, int exactDepth)
        throws InvalidParseOperationException {
        
        // TODO add unit tests
        final Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.UP, previous.getCurrentIds());
        final Bson finalQuery = and(query, roots, lte(Unit.MINDEPTH, exactDepth), gte(Unit.MAXDEPTH, exactDepth));
        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(
            MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        final MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                final String id = unit.getId();
                MongoDbMetadataHelper.LRU.put(id, unit);
                result.addId(id);
            }
        } finally {
            cursor.close();
        }
        result.setNbResult(result.getCurrentIds().size());
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
     * @param notimeout
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     */
    protected Result relativeDepthUnitQuery(Query realQuery, Result previous, int relativeDepth)
        throws InvalidParseOperationException, MetaDataExecutionException {
       
        if(realQuery.isFullText()){
            //ES
            QueryBuilder roots = null;
            
            if (previous.getCurrentIds().isEmpty()) {
                if(relativeDepth<0){
                    roots = QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(1) ;
                }else{
                    roots = QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(relativeDepth+1) ;
                }
            } else {
                if (relativeDepth == 1) {
                    roots = QueryToElasticsearch.getRoots(MetadataDocument.UP,
                        previous.getCurrentIds());
                } else if (relativeDepth >= 1) {
                    roots = QueryToElasticsearch.getRoots(Unit.UNITUPS, previous.getCurrentIds());
                }
                
            }
            
            QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
            if (roots != null) {
                query = QueryToElasticsearch.getFullCommand(query, roots);
            }
            LOGGER.debug(QUERY2 + query.toString());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Req1LevelMD: {}", query);
            }
            previous.clear();
            
            return MetadataCollections.C_UNIT.getEsClient().search(MetadataCollections.C_UNIT, Unit.TYPEUNIQUE, query, null);
            
        }else {
            // MongoDB
            Bson roots = null;
            boolean tocheck = false;
            if (previous.getCurrentIds().isEmpty()) {
                if (relativeDepth == 1) {
                    roots = lte(Unit.MAXDEPTH, 1);
                }else {
                    roots = lte(Unit.MAXDEPTH, relativeDepth+1);
                }
                
            } else {
                if (relativeDepth < 0) {
                    // Relative parent: previous has future result in their _up
                    // so future result ids are in previous UNITDEPTHS
                    final Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
                    roots = QueryToMongodb.getRoots(MetadataDocument.ID, fathers);
                } else if (relativeDepth == 1) {
                    // immediate step: previous is in UNIT_TO_UNIT of result
                    roots = QueryToMongodb.getRoots(MetadataDocument.UP,
                        previous.getCurrentIds());
                } else {
                    // relative depth: previous is in UNITUPS of result
                    // Will need an extra test on result
                    roots = QueryToMongodb.getRoots(Unit.UNITUPS, previous.getCurrentIds());
                    tocheck = true;
                }
            }
            
            Result result = null;
            Bson query = QueryToMongodb.getCommand(realQuery);
            if (roots != null) {
                query = QueryToMongodb.getFullCommand(query, roots);
            }
            LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(query, false));
            result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("Req1LevelMD: {}", realQuery);
            }
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT, query,
                    Unit.UNIT_VITAM_PROJECTION);
            final MongoCursor<Unit> cursor = iterable.iterator();
            try {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    if (tocheck) {
                        // now check for relativeDepth > 1
                        final Map<String, Integer> depths = unit.getDepths();
                        boolean check = false;
                        for (final String pid : previous.getCurrentIds()) {
                            final Integer depth = depths.get(pid);
                            if (depth != null && depth <= relativeDepth) {
                                check = true;
                                break;
                            }
                        }
                        if (!check) {
                            // ignore since false positive
                            continue;
                        }
                    }
                    final String id = unit.getId();
                    MongoDbMetadataHelper.LRU.put(id, unit);
                    result.addId(id);
                }
            } finally {
                previous.clear();
                cursor.close();
            }
            result.setNbResult(result.getCurrentIds().size());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.debug("UnitRelative: {}", result);
            }
            return result;
        }
    }

    /**
     * Aggregate Unit Depths according to parent relative Depth
     *
     * @param ids
     * @param relativeDepth
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Set<String> ids, int relativeDepth) {
        // TODO add unit tests
        // Select all items from ids
        final Bson match = match(in(MetadataDocument.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(new BasicDBObject(MetadataDocument.ID, "all"),
            addToSet("deptharray", ParserTokens.DEFAULT_PREFIX + Unit.UNITDEPTHS));
        LOGGER.debug("Depth: " + MongoDbHelper.bsonToString(match, false) + " " +
            MongoDbHelper.bsonToString(group, false));
        final List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
        final AggregateIterable<Unit> aggregateIterable =
            (AggregateIterable<Unit>) MetadataCollections.C_UNIT.getCollection().aggregate(pipeline);
        final Unit aggregate = aggregateIterable.first();
        final Set<String> set = new HashSet<String>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            final List<Map<String, Integer>> array = (List<Map<String, Integer>>) aggregate.get("deptharray");
            for (final Map<String, Integer> map : array) {
                for (final String key : map.keySet()) {
                    if (map.get(key) <= relativeDepth) {
                        set.add(key);
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
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     */
    protected Result sameDepthUnitQuery(Query realQuery, Result previous) throws InvalidParseOperationException, MetaDataExecutionException {
        
        Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
       
        if(realQuery.isFullText()) {
           
            // ES 
            final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery);
            QueryBuilder finalQuery;
            if (previous.getCurrentIds().isEmpty()) {
                finalQuery = query;
            } else {
                final QueryBuilder roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
                finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
            }
            
            previous.clear();
            LOGGER.debug(QUERY2 + finalQuery.toString());
            return MetadataCollections.C_UNIT.getEsClient().search(MetadataCollections.C_UNIT, Unit.TYPEUNIQUE, finalQuery, null);
            
        }else {
        
            // Mongo
            // TODO add unit tests
            final Bson query = QueryToMongodb.getCommand(realQuery);
            Bson finalQuery;
            if (previous.getCurrentIds().isEmpty()) {
                finalQuery = query;
            } else {
                final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, previous.getCurrentIds());
                finalQuery = and(query, roots);
            }
            previous.clear();
            LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
                .select(MetadataCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
            final MongoCursor<Unit> cursor = iterable.iterator();
            try {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    final String id = unit.getId();
                    MongoDbMetadataHelper.LRU.put(id, unit);
                    result.addId(id);
                }
            } finally {
                cursor.close();
            }
            result.setNbResult(result.getCurrentIds().size());
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.warn("UnitSameDepth: {}", result);
            }
            return result;
        }
    }

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous units, Note: only immediate Unit parents are allowed
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result objectGroupQuery(Query realQuery, Result previous) throws InvalidParseOperationException {
        final Result result = MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final Bson roots = QueryToMongodb.getRoots(MetadataDocument.UP, previous.getCurrentIds());
            finalQuery = and(query, roots);
        }
        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
            MetadataCollections.C_OBJECTGROUP, finalQuery,
            ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        final MongoCursor<ObjectGroup> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                result.addId(cursor.next().getId());
            }
        } finally {
            cursor.close();
        }
        result.setNbResult(result.getCurrentIds().size());
        return result;
    }

    /**
     * Finalize the queries with last True Select
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result lastSelectFilterProjection(SelectToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final Bson orderBy = requestToMongodb.getFinalOrderBy();
        final int offset = requestToMongodb.getFinalOffset();
        final int limit = requestToMongodb.getFinalLimit();
        final FILTERARGS model = requestToMongodb.model();
        LOGGER.debug("To Select: " + MongoDbHelper.bsonToString(roots, false) + " " +
            (projection != null ? MongoDbHelper.bsonToString(projection, false) : "") + " " +
            MongoDbHelper.bsonToString(orderBy, false) + " " + offset + " " + limit);
        if (model == FILTERARGS.UNITS) {
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.C_UNIT,
                    roots, projection, orderBy, offset, limit);
            final MongoCursor<Unit> cursor = iterable.iterator();
            try {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    last.addId(unit.getId());
                    last.addFinal(unit);
                }
            } finally {
                cursor.close();
            }
            last.setNbResult(last.getCurrentIds().size());
            return last;
        }
        // OBJECTGROUPS:
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
                MetadataCollections.C_OBJECTGROUP,
                roots, projection, orderBy, offset, limit);
        final MongoCursor<ObjectGroup> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                last.addId(og.getId());
                last.addFinal(og);
            }
        } finally {
            cursor.close();
        }
        last.setNbResult(last.getCurrentIds().size());
        return last;
    }

    /**
     * Finalize the queries with last True Update
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result lastUpdateFilterProjection(UpdateToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final Bson update = requestToMongodb.getFinalUpdate();
        final FILTERARGS model = requestToMongodb.model();
        LOGGER.debug(
            "To Update: " + MongoDbHelper.bsonToString(roots, false) + " " + MongoDbHelper.bsonToString(update, false));
        try {
            if (model == FILTERARGS.UNITS) {
                final UpdateResult result = MongoDbMetadataHelper.update(MetadataCollections.C_UNIT,
                    roots, update, last.getCurrentIds().size());
                last.setNbResult(result.getModifiedCount());
                return last;
            }
            // OBJECTGROUPS:
            // TODO add unit tests
            final UpdateResult result =
                MongoDbMetadataHelper.update(MetadataCollections.C_OBJECTGROUP,
                    roots, update, last.getCurrentIds().size());
            last.setNbResult(result.getModifiedCount());
            return last;
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Update concern", e);
        }
    }

    /**
     * Finalize the queries with last True Insert
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException 
     */
    protected Result lastInsertFilterProjection(InsertToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataNotFoundException {
        final Document data = requestToMongodb.getFinalData();
        LOGGER.debug("To Insert: " + data);
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final Unit unit = new Unit(data);
                if (MongoDbMetadataHelper.exists(MetadataCollections.C_UNIT, unit.getId())) {
                    // Should not exist
                    throw new MetaDataAlreadyExistException("Unit already exists: " + unit.getId());
                }
                unit.save();
                for (final String id : last.getCurrentIds()) {
                    final Unit parentUnit = MongoDbMetadataHelper.LRU.get(id);
                    if (parentUnit != null) {
                        parentUnit.addUnit(unit);
                    } else {
                        LOGGER.debug("Cannot find parent: " + id);
                        throw new MetaDataNotFoundException("Cannot find Parent: " + id);
                    }
                }
                last.clear();
                last.addId(unit.getId());
                last.setNbResult(1);
                return last;
            }
            // OBJECTGROUPS:
            // TODO add unit tests
            final ObjectGroup og = new ObjectGroup(data);
            if (MongoDbMetadataHelper.exists(MetadataCollections.C_OBJECTGROUP, og.getId())) {
                // Should not exist
                throw new MetaDataAlreadyExistException("ObjectGroup already exists: " + og.getId());
            }
            if (last.getCurrentIds().isEmpty() && og.getFathersUnitIds(false).isEmpty()) {
                // Must not be
                LOGGER.debug("No Unit parent defined");
                throw new MetaDataNotFoundException("No Unit parent defined");
            }
            og.save();
            for (final String id : last.getCurrentIds()) {
                final Unit parentUnit = MongoDbMetadataHelper.LRU.get(id);
                if (parentUnit != null) {
                    parentUnit.addObjectGroup(og);
                } else {
                    LOGGER.debug("Cannot find parent: " + id);
                    throw new MetaDataNotFoundException("Cannot find Parent: " + id);
                }
            }
            last.clear();
            last.addId(og.getId());
            last.setNbResult(1);
            return last;
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
    protected Result lastDeleteFilterProjection(DeleteToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final DeleteResult result = MongoDbMetadataHelper.delete(MetadataCollections.C_UNIT,
                    roots, last.getCurrentIds().size());
                last.setNbResult(result.getDeletedCount());
                return last;
            }
            // TODO add unit tests
            // OBJECTGROUPS:
            final DeleteResult result =
                MongoDbMetadataHelper.delete(MetadataCollections.C_OBJECTGROUP,
                    roots, last.getCurrentIds().size());
            last.setNbResult(result.getDeletedCount());
            return last;
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Delete concern", e);
        }
    }

}
