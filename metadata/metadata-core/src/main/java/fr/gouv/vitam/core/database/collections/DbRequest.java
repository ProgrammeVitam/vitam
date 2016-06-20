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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.builder.request.construct.Delete;
import fr.gouv.vitam.builder.request.construct.Insert;
import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.Update;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;
import fr.gouv.vitam.core.database.collections.translator.RequestToAbstract;
import fr.gouv.vitam.core.database.collections.translator.mongodb.DeleteToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.InsertToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.MongoDbHelper;
import fr.gouv.vitam.core.database.collections.translator.mongodb.QueryToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.RequestToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.SelectToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.UpdateToMongodb;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.parser.request.construct.query.QueryDepthHelper;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;
import fr.gouv.vitam.parser.request.parser.RequestParser;
import fr.gouv.vitam.parser.request.parser.query.PathQuery;

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
     */
    public Result execRequest(final RequestParser requestParser, final Result defaultStartSet)
        throws InstantiationException, IllegalAccessException, MetaDataExecutionException,
        InvalidParseOperationException {
        final Request request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        Result roots;
        switch (requestParser.model()) {
            case OBJECTGROUPS:
                roots = checkObjectGroupStartupRoots(requestParser, defaultStartSet);
                break;
            case UNITS:
                roots = checkUnitStartupRoots(requestParser, defaultStartSet);
                break;
            default:
                throw new MetaDataExecutionException("Model not yet requestable: " + requestParser.model());
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
            }
            if (GlobalDatasDb.PRINT_REQUEST) {
                LOGGER.warn("Results: " + result);
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
                    MongoDbAccess.LRU.remove(id);
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
            LOGGER.warn("Results: " + result);
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
    protected Result checkUnitStartupRoots(final RequestParser request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        final Set<String> roots = request.getRequest().getRoots();
        final Set<String> newRoots = checkUnitAgainstRoots(roots, defaultStartSet);
        if (newRoots.isEmpty()) {
            return MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbAccess.createOneResult(FILTERARGS.UNITS, newRoots);
    }

    /**
     * Check ObjectGroup at startup against Roots
     *
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkObjectGroupStartupRoots(final RequestParser request, final Result defaultStartSet)
        throws InvalidParseOperationException {
        final Set<String> roots = request.getRequest().getRoots();
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
        }
        if (roots.isEmpty()) {
            return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(VitamCollections.C_OBJECTGROUP,
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
            return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        if (!newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS, newRoots);
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
        // FIXME REVIEW: was: || defaultStartSet.getCurrentIds().isEmpty() in order to allow emptyStartSet => default
        // roots
        if (defaultStartSet == null) {
            // no limitation: using roots
            return current;
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(VitamCollections.C_UNIT,
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
            LOGGER.warn("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + query);
        }
        final QUERY type = realQuery.getQUERY();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (type == QUERY.PATH) {
            // FIXME REVIEW why removing this?
            // Check if path is compatible with previous
            // if (previous.getCurrentIds().isEmpty()) {
            // previous.clear();
            // return MongoDbAccess.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
            // }
            final Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
            previous.clear();
            if (newRoots.isEmpty()) {
                return MongoDbAccess.createOneResult(collectionType);
            }
            return MongoDbAccess.createOneResult(collectionType, newRoots);
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatasParser.MAXDEPTH;
        }
        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result result;
        try {
            switch (collectionType) {
                case UNITS:
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
                    break;
                case OBJECTGROUPS:
                    // No depth at all
                    LOGGER.debug("ObjectGroup No depth at all");
                    result = objectGroupQuery(realQuery, previous);
                    break;
                default:
                    throw new MetaDataExecutionException(
                        "Cannot execute this operation on the model: " + collectionType);
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
        final Result result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        final Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
        final Bson finalQuery = and(query, roots, lte(Unit.MINDEPTH, exactDepth), gte(Unit.MAXDEPTH, exactDepth));
        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(
            MongoDbAccess.VitamCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        final MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                final String id = unit.getId();
                MongoDbAccess.LRU.put(id, unit);
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
     */
    protected Result relativeDepthUnitQuery(Query realQuery, Result previous, int relativeDepth)
        throws InvalidParseOperationException {
        Result result = null;
        Bson query = QueryToMongodb.getCommand(realQuery);
        Bson roots = null;
        boolean tocheck = false;
        if (previous.getCurrentIds().isEmpty()) {
            // FIXME REVIEW : why removing this
            // Change to MAX DEPTH <= relativeDepth
            // roots = lte(Unit.MAXDEPTH, relativeDepth);
        } else {
            if (relativeDepth < 0) {
                // Relative parent: previous has future result in their _up
                // so future result ids are in previous UNITDEPTHS
                final Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
                roots = QueryToMongodb.getRoots(VitamDocument.ID, fathers);
            } else if (relativeDepth == 0) {
                // same level: previous is in IDs of result
                roots = QueryToMongodb.getRoots(VitamDocument.ID, previous.getCurrentIds());
            } else if (relativeDepth == 1) {
                // immediate step: previous is in UNIT_TO_UNIT of result
                roots = QueryToMongodb.getRoots(VitamDocument.UP,
                    previous.getCurrentIds());
            } else {
                // relative depth: previous is in UNITUPS of result
                // Will need an extra test on result
                roots = QueryToMongodb.getRoots(Unit.UNITUPS, previous.getCurrentIds());
                tocheck = true;
            }
        }
        if (roots != null) {
            query = QueryToMongodb.getFullCommand(query, roots);
        }
        // FIXME REVIEW now query could be null! you need to not use query if null
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(query, false));
        result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("Req1LevelMD: {}", realQuery);
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable =
            (FindIterable<Unit>) MongoDbMetadataHelper.select(MongoDbAccess.VitamCollections.C_UNIT, query,
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
                MongoDbAccess.LRU.put(id, unit);
                result.addId(id);
            }
        } finally {
            previous.clear();
            cursor.close();
        }
        result.setNbResult(result.getCurrentIds().size());
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("UnitRelative: {}", result);
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
    protected Set<String> aggregateUnitDepths(Set<String> ids, int relativeDepth) {
        // Select all items from ids
        final Bson match = match(in(VitamDocument.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(new BasicDBObject(VitamDocument.ID, "all"),
            addToSet("deptharray", ParserTokens.DEFAULT_PREFIX + Unit.UNITDEPTHS));
        LOGGER.debug("Depth: " + MongoDbHelper.bsonToString(match, false) + " " +
            MongoDbHelper.bsonToString(group, false));
        final List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
        final AggregateIterable<Unit> aggregateIterable =
            (AggregateIterable<Unit>) MongoDbAccess.VitamCollections.C_UNIT.getCollection().aggregate(pipeline);
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
     */
    protected Result sameDepthUnitQuery(Query realQuery, Result previous) throws InvalidParseOperationException {
        final Result result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
            finalQuery = and(query, roots);
        }
        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
            .select(MongoDbAccess.VitamCollections.C_UNIT, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        final MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                final String id = unit.getId();
                MongoDbAccess.LRU.put(id, unit);
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

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous units, Note: only immediate Unit parents are allowed
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result objectGroupQuery(Query realQuery, Result previous) throws InvalidParseOperationException {
        final Result result = MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
        final Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
            finalQuery = and(query, roots);
        }
        previous.clear();
        LOGGER.debug(QUERY2 + MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
            MongoDbAccess.VitamCollections.C_OBJECTGROUP, finalQuery,
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
        final Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final Bson orderBy = requestToMongodb.getFinalOrderBy();
        final int offset = requestToMongodb.getFinalOffset();
        final int limit = requestToMongodb.getFinalLimit();
        final FILTERARGS model = requestToMongodb.model();
        LOGGER.debug("To Select: " + MongoDbHelper.bsonToString(roots, false) + " " +
            (projection != null ? MongoDbHelper.bsonToString(projection, false) : "") + " " +
            MongoDbHelper.bsonToString(orderBy, false) + " " + offset + " " + limit);
        switch (model) {
            case UNITS: {
                @SuppressWarnings("unchecked")
                final FindIterable<Unit> iterable =
                    (FindIterable<Unit>) MongoDbMetadataHelper.select(MongoDbAccess.VitamCollections.C_UNIT,
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
            case OBJECTGROUPS: {
                @SuppressWarnings("unchecked")
                final FindIterable<ObjectGroup> iterable =
                    (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
                        MongoDbAccess.VitamCollections.C_OBJECTGROUP,
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
            default:
                throw new MetaDataExecutionException("Model not supported: " + model);
        }
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
        final Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        final Bson update = requestToMongodb.getFinalUpdate();
        final FILTERARGS model = requestToMongodb.model();
        LOGGER.debug(
            "To Update: " + MongoDbHelper.bsonToString(roots, false) + " " + MongoDbHelper.bsonToString(update, false));
        try {
            switch (model) {
                case UNITS: {
                    final UpdateResult result = MongoDbMetadataHelper.update(MongoDbAccess.VitamCollections.C_UNIT,
                        roots, update, last.getCurrentIds().size());
                    last.setNbResult(result.getModifiedCount());
                    return last;
                }
                case OBJECTGROUPS: {
                    final UpdateResult result =
                        MongoDbMetadataHelper.update(MongoDbAccess.VitamCollections.C_OBJECTGROUP,
                            roots, update, last.getCurrentIds().size());
                    last.setNbResult(result.getModifiedCount());
                    return last;
                }
                default:
                    throw new MetaDataExecutionException("Model not supported: " + model);
            }
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
     * @throws MetaDataExecutionException
     */
    protected Result lastInsertFilterProjection(InsertToMongodb requestToMongodb, Result last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Document data = requestToMongodb.getFinalData();
        LOGGER.debug("To Insert: " + data);
        final FILTERARGS model = requestToMongodb.model();
        try {
            switch (model) {
                case UNITS: {
                    final Unit unit = new Unit(data);
                    unit.insert();
                    for (final String id : last.getCurrentIds()) {
                        final Unit parentUnit = MongoDbAccess.LRU.get(id);
                        if (parentUnit != null) {
                            parentUnit.addUnit(unit);
                        }
                    }
                    last.clear();
                    last.addId(unit.getId());
                    last.setNbResult(1);
                    return last;
                }
                case OBJECTGROUPS: {
                    final ObjectGroup og = new ObjectGroup(data);
                    og.save();
                    for (final String id : last.getCurrentIds()) {
                        final Unit parentUnit = MongoDbAccess.LRU.get(id);
                        if (parentUnit != null) {
                            parentUnit.addObjectGroup(og);
                        }
                    }
                    last.clear();
                    last.addId(og.getId());
                    last.setNbResult(1);
                    return last;
                }
                default:
                    throw new MetaDataExecutionException("Model not supported: " + model);
            }
        } catch (final MongoWriteException e) {
            throw e;
        } catch (final Exception e) {
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
        final Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        final FILTERARGS model = requestToMongodb.model();
        try {
            switch (model) {
                case UNITS: {
                    final DeleteResult result = MongoDbMetadataHelper.delete(MongoDbAccess.VitamCollections.C_UNIT,
                        roots, last.getCurrentIds().size());
                    last.setNbResult(result.getDeletedCount());
                    return last;
                }
                case OBJECTGROUPS: {
                    final DeleteResult result =
                        MongoDbMetadataHelper.delete(MongoDbAccess.VitamCollections.C_OBJECTGROUP,
                            roots, last.getCurrentIds().size());
                    last.setNbResult(result.getDeletedCount());
                    return last;
                }
                default:
                    throw new MetaDataExecutionException("Model not supported: " + model);
            }
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Delete concern", e);
        }
    }

}
