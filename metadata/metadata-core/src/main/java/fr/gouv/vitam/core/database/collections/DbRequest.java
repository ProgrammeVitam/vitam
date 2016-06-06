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
import fr.gouv.vitam.api.exception.MetaDataMaxDepthException;
import fr.gouv.vitam.builder.request.construct.Delete;
import fr.gouv.vitam.builder.request.construct.Insert;
import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.Update;
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
import fr.gouv.vitam.core.database.collections.translator.mongodb.QueryToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.RequestToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.SelectToMongodb;
import fr.gouv.vitam.core.database.collections.translator.mongodb.UpdateToMongodb;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.parser.request.construct.query.QueryDepthHelper;
import fr.gouv.vitam.parser.request.parser.RequestParser;
import fr.gouv.vitam.parser.request.parser.query.PathQuery;

/**
 * DB Request using MongoDB only
 */
public class DbRequest {
	private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);

    boolean debug = true;
    
    /**
     * Constructor
     */
    public DbRequest() {
    }

    /**
     * @param debug
     *            If True, in debug mode
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
    
    /**
     * The request should be already analyzed.
     *
     * @param requestParser
     * @param defaultStartSet
     *            the set of id from which the request should start, whatever the roots set
     * @return the Result
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException 
     * @throws MetaDataMaxDepthException 
     */
    public Result execRequest(final RequestParser requestParser, final Result defaultStartSet)
            throws InstantiationException, IllegalAccessException, MetaDataExecutionException, InvalidParseOperationException, MetaDataMaxDepthException {
        final Request request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        int maxQuery = request.getNbQueries();
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
                LOGGER.error("No result at rank: " + rank + " from " + requestParser
                        + " \n\twhere previous is " + result);
                // XXX TODO should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                        .addError(newResult != null ? newResult.getCurrentIds().toString() : "no_result: true")
                        .addError("no_result_at_rank: " + rank).addError("from: " + requestParser)
                        .addError("where_previous_result_was: " + result);
                
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
            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.error("No result at rank: " + rank + " from " + requestParser
                        + " \n\twhere previous is " + result);
                // XXX TODO should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                        .addError(newResult != null ? newResult.getCurrentIds().toString() : "no_result: true")
                        .addError("no_result_at_rank: " + rank).addError("from: " + requestParser)
                        .addError("where_previous_result_was: " + result);
                return result;
            }
            if (debug) {
                LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            }
        }
        // Result contains the selection on which to act
        // Insert allow to have no result
        if (request instanceof Insert) {
            Result newResult = lastInsertFilterProjection((InsertToMongodb) requestToMongodb, result);
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
        	LOGGER.error("No result at rank: " + rank + " from " + requestParser
                    + " \n\twhere previous is " + result);
            // XXX TODO should be adapted to have a correct error feedback
            result = new ResultError(result.type)
                    .addError(result != null ? result.getCurrentIds().toString() : "no_result: true")
                    .addError("no_result_at_rank: " + rank).addError("from: " + requestParser)
                    .addError("where_previous_result_was: " + result);
            return result;
        }
        if (request instanceof Update) {
            Result newResult = lastUpdateFilterProjection((UpdateToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        } else if (request instanceof Delete) {
            Result newResult = lastDeleteFilterProjection((DeleteToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
                // Clean cache
                for (String id : result.getCurrentIds()) {
                    MongoDbAccess.LRU.remove(id);
                }
            }
        } else {
            // Select part
            Result newResult = lastSelectFilterProjection((SelectToMongodb) requestToMongodb, result);
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
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkUnitStartupRoots(final RequestParser request, final Result defaultStartSet)
            throws InvalidParseOperationException {
        Set<String> roots = request.getRequest().getRoots();
        Set<String> newRoots = checkUnitAgainstRoots(roots, defaultStartSet);
        if (newRoots.isEmpty()) {
            return MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        }
        if (! newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbAccess.createOneResult(FILTERARGS.UNITS, newRoots);
    }
    
    /**
     * Check ObjectGroup at startup against Roots
     * @param request
     * @param defaultStartSet
     * @return the valid root ids
     * @throws InvalidParseOperationException
     */
    protected Result checkObjectGroupStartupRoots(final RequestParser request, final Result defaultStartSet)
            throws InvalidParseOperationException {
        Set<String> roots = request.getRequest().getRoots();
    	if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
    		// no limitation: using roots
            return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
    	}
    	if (roots.isEmpty()) {
    		return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
    	}
        @SuppressWarnings("unchecked")
        FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbHelper.select(VitamCollections.Cobjectgroup, 
        		MongoDbHelper.queryForAncestorsOrSame(roots, defaultStartSet.getCurrentIds()),
                ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        MongoCursor<ObjectGroup> cursor = iterable.iterator();
        Set<String> newRoots = new HashSet<String>();
        try {
            while (cursor.hasNext()) {
            	ObjectGroup og = cursor.next();
                newRoots.add(og.getId());
            }
        } finally {
            cursor.close();
        }
        if (newRoots.isEmpty()) {
            return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
        }
        if (! newRoots.containsAll(roots)) {
            LOGGER.debug("Not all roots are preserved");
        }
        return MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS, newRoots);
    }
    
    /**
     * Check Unit parents against Roots
     * @param current set of result id
     * @param defaultStartSet
     * @return the valid root ids set
     * @throws InvalidParseOperationException
     */
    protected Set<String> checkUnitAgainstRoots(final Set<String> current, final Result defaultStartSet)
            throws InvalidParseOperationException {
    	// TODO: was: || defaultStartSet.getCurrentIds().isEmpty() in order to allow emptyStartSet => default roots
    	if (defaultStartSet == null) {
    		// no limitation: using roots
            return current;
    	}
        @SuppressWarnings("unchecked")
        FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbHelper.select(VitamCollections.Cunit, 
        		MongoDbHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
        		MongoDbHelper.ID_PROJECTION);
        MongoCursor<Unit> cursor = iterable.iterator();
        Set<String> newRoots = new HashSet<String>();
        try {
            while (cursor.hasNext()) {
                Unit unit = cursor.next();
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
     * @param previous
     *            previous Result from previous level (except in level == 0
     *            where it is the subset of valid roots)
     * @return the new Result from this request
     * @throws MetaDataExecutionException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvalidParseOperationException 
     */
    protected Result executeQuery(final RequestToAbstract requestToMongodb, final int rank, final Result previous)
            throws MetaDataExecutionException, InstantiationException,
            IllegalAccessException, InvalidParseOperationException {
        Query realQuery = requestToMongodb.getNthQuery(rank);
        if (GlobalDatasDb.PRINT_REQUEST) {
            String query = realQuery.getCurrentQuery().toString();
            LOGGER.warn("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + query);
        }
        QUERY type = realQuery.getQUERY();
        FILTERARGS collectionType = requestToMongodb.model();
        if (type == QUERY.PATH) {
        	// TODO REVIEW why removing this?
            // Check if path is compatible with previous
//        	if (previous.getCurrentIds().isEmpty()) {
//                previous.clear();
//        		return MongoDbAccess.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
//        	}
            Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
            previous.clear();
            if (newRoots.isEmpty()) {
                return MongoDbAccess.createOneResult(collectionType);
            }
            return MongoDbAccess.createOneResult(collectionType, newRoots);
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatasDb.MAXDEPTH;
        }
        int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
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
	            case OBJECTS:
	                // Need to investigate: object is a subelement of objectgroup (_uses.versions.)
	                // Possibility: request contains full information of path (_uses) so query is similar 
	                // to ObjectGroup but not result (filtering on Object)
	            default:
	                // XXX FXME ???
	            	throw new MetaDataExecutionException("Cannot execute this operation on the model: " + collectionType);
	        }
        } finally {
            previous.clear();
        }
        return result;
    }
    /**
     * Execute one Unit Query using exact Depth
     * @param realQuery
     * @param previous
     * @param exactDepth
     * @return the associated Result
     * @throws InvalidParseOperationException
     */
    protected Result exactDepthUnitQuery(Query realQuery, Result previous, int exactDepth)
    		throws InvalidParseOperationException {
        Result result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        Bson query = QueryToMongodb.getCommand(realQuery);
        Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
        Bson finalQuery = and(query, roots, lte(Unit.MINDEPTH, exactDepth), gte(Unit.MAXDEPTH, exactDepth));
        previous.clear();
        LOGGER.debug("query: "+MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbHelper.select(
                MongoDbAccess.VitamCollections.Cunit, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                Unit unit = cursor.next();
                String id = unit.getId();
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
        	// TODO: why removing this
        	// Change to MAX DEPTH <= relativeDepth
        	//roots = lte(Unit.MAXDEPTH, relativeDepth);
        } else {
	        if (relativeDepth < 0) {
	            // Relative parent: previous has future result in their _up
	            // so future result ids are in previous UNITDEPTHS
	            Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
	            roots = QueryToMongodb.getRoots(Unit.ID, fathers);
	        } else if (relativeDepth == 0) {
	            // same level: previous is in IDs of result
	            roots = QueryToMongodb.getRoots(Unit.ID, previous.getCurrentIds());
	        } else if (relativeDepth == 1) {
	            // immediate step: previous is in Unit2Unit of result
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
    	// TODO REVIEW now query could be null! you need to not use query if null
        LOGGER.debug("query: "+MongoDbHelper.bsonToString(query, false));
        result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("Req1LevelMD: {}", realQuery);
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = 
            (FindIterable<Unit>) MongoDbHelper.select(MongoDbAccess.VitamCollections.Cunit, query, Unit.UNIT_VITAM_PROJECTION);
        MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                if (tocheck) {
                    // now check for relativeDepth > 1
                    Map<String, Integer> depths = unit.getDepths();
                    boolean check = false;
                    for (String pid : previous.getCurrentIds()) {
                        Integer depth = depths.get(pid);
                        if (depth != null && depth <= relativeDepth) {
                            check = true;
                            break;
                        }
                    }
                    if (! check) {
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
     * @param ids
     * @param relativeDepth
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Set<String> ids, int relativeDepth) {
        // Select all items from ids
        Bson match = match(in(Unit.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        Bson group = group(new BasicDBObject(Unit.ID, "all"),
                addToSet("deptharray", "$"+Unit.UNITDEPTHS));
        LOGGER.debug("Depth: "+MongoDbHelper.bsonToString(match, false)+" "+
        		MongoDbHelper.bsonToString(group, false));
        List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
		AggregateIterable<Unit> aggregateIterable = 
        		(AggregateIterable<Unit>) MongoDbAccess.VitamCollections.Cunit.getCollection().aggregate(pipeline);
        Unit aggregate = aggregateIterable.first();
        Set<String> set = new HashSet<String>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Integer>> array = (List<Map<String, Integer>>) aggregate.get("deptharray");
            for (Map<String, Integer> map : array) {
                for (String key : map.keySet()) {
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
        Result result = MongoDbAccess.createOneResult(FILTERARGS.UNITS);
        Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
        	finalQuery = query;
        } else {
        	Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
        	finalQuery = and(query, roots);
        }
        previous.clear();
        LOGGER.debug("query: "+MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        FindIterable<Unit> iterable = (FindIterable<Unit>) 
        	MongoDbHelper.select(MongoDbAccess.VitamCollections.Cunit, finalQuery, Unit.UNIT_VITAM_PROJECTION);
        MongoCursor<Unit> cursor = iterable.iterator();
        try {
            while (cursor.hasNext()) {
                Unit unit = cursor.next();
                String id = unit.getId();
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
     * @param realQuery
     * @param previous units, Note: only immediate Unit parents are allowed 
     * @return the associated Result 
     * @throws InvalidParseOperationException 
     */
    protected Result objectGroupQuery(Query realQuery, Result previous) throws InvalidParseOperationException {
        Result result = MongoDbAccess.createOneResult(FILTERARGS.OBJECTGROUPS);
        Bson query = QueryToMongodb.getCommand(realQuery);
        Bson finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
        	finalQuery = query;
        } else {
        	Bson roots = QueryToMongodb.getRoots(VitamDocument.UP, previous.getCurrentIds());
        	finalQuery = and(query, roots);
        }
        previous.clear();
        LOGGER.debug("query: "+MongoDbHelper.bsonToString(finalQuery, false));
        @SuppressWarnings("unchecked")
        FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbHelper.select(
                MongoDbAccess.VitamCollections.Cobjectgroup, finalQuery, 
                ObjectGroup.OBJECTGROUP_VITAM_PROJECTION);
        MongoCursor<ObjectGroup> cursor = iterable.iterator();
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
     * @param requestToMongodb
     * @param last
     * @return the final Result 
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     */
    protected Result lastSelectFilterProjection(SelectToMongodb requestToMongodb, Result last) 
    		throws InvalidParseOperationException, MetaDataExecutionException {
        Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        Bson projection = requestToMongodb.getFinalProjection();
        Bson orderBy = requestToMongodb.getFinalOrderBy();
        int offset = requestToMongodb.getFinalOffset();
        int limit = requestToMongodb.getFinalLimit();
        FILTERARGS model = requestToMongodb.model();
        LOGGER.debug("To Select: "+MongoDbHelper.bsonToString(roots, false)+" "+
        		(projection != null ? MongoDbHelper.bsonToString(projection, false) : "") +" "+
        		MongoDbHelper.bsonToString(orderBy, false)+" "+offset+" "+limit);
        switch (model) {
            case UNITS: {
                @SuppressWarnings("unchecked")
                FindIterable<Unit> iterable = 
                    (FindIterable<Unit>) MongoDbHelper.select(MongoDbAccess.VitamCollections.Cunit,
                            roots, projection, orderBy, offset, limit);
                MongoCursor<Unit> cursor = iterable.iterator();
                try {
                    while (cursor.hasNext()) {
                    	Unit unit = cursor.next();
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
                FindIterable<ObjectGroup> iterable = 
                    (FindIterable<ObjectGroup>) MongoDbHelper.select(MongoDbAccess.VitamCollections.Cobjectgroup,
                            roots, projection, orderBy, offset, limit);
                MongoCursor<ObjectGroup> cursor = iterable.iterator();
                try {
                    while (cursor.hasNext()) {
                    	ObjectGroup og = cursor.next();
                        last.addId(og.getId());
                        last.addFinal(og);
                    }
                } finally {
                    cursor.close();
                }
                last.setNbResult(last.getCurrentIds().size());
                return last;
            }
            case OBJECTS:
                // XXX TODO
            default:
            	throw new MetaDataExecutionException("Model not supported: " + model);
        }
    }

    /**
     * Finalize the queries with last True Update
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     */
    protected Result lastUpdateFilterProjection(UpdateToMongodb requestToMongodb, Result last)
    		throws InvalidParseOperationException, MetaDataExecutionException {
        Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        Bson update = requestToMongodb.getFinalUpdate();
        FILTERARGS model = requestToMongodb.model();
        LOGGER.debug("To Update: "+MongoDbHelper.bsonToString(roots, false)+" "+MongoDbHelper.bsonToString(update, false));
        try {
	        switch (model) {
	            case UNITS: {
	                UpdateResult result = MongoDbHelper.update(MongoDbAccess.VitamCollections.Cunit, 
	                        roots, update, last.getCurrentIds().size());
	                last.setNbResult(result.getModifiedCount());
	                return last;
	            }
	            case OBJECTGROUPS: {
	                UpdateResult result = MongoDbHelper.update(MongoDbAccess.VitamCollections.Cobjectgroup, 
	                        roots, update, last.getCurrentIds().size());
	                last.setNbResult(result.getModifiedCount());
	                return last;
	            }
	            case OBJECTS:
	                // XXX TODO
	            default:
	            	throw new MetaDataExecutionException("Model not supported: " + model);
	        }
        } catch (MetaDataExecutionException e) {
        	throw e;
        } catch (Exception e) {
        	throw new MetaDataExecutionException("Update concern", e);
        }
    }

    /**
     * Finalize the queries with last True Insert
     * @param requestToMongodb
     * @param last
     * @return the final Result 
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     * @throws MetaDataMaxDepthException 
     */
    protected Result lastInsertFilterProjection(InsertToMongodb requestToMongodb, Result last)
    		throws InvalidParseOperationException, MetaDataExecutionException, MetaDataMaxDepthException {
    	Document data = requestToMongodb.getFinalData();
    	LOGGER.debug("To Insert: " + data);
        FILTERARGS model = requestToMongodb.model();
        try {
            switch (model) {
                case UNITS: {
                    Unit unit = new Unit(data);
                    unit.insert();
                    for (String id : last.getCurrentIds()) {
                        Unit parentUnit = MongoDbAccess.LRU.get(id);
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
                    ObjectGroup og = new ObjectGroup(data);
                    og.save();
                    for (String id : last.getCurrentIds()) {
                        Unit parentUnit = MongoDbAccess.LRU.get(id);
                        if (parentUnit != null) {
                            parentUnit.addObjectGroup(og);
                        }
                    }
                    last.clear();
                    last.addId(og.getId());
                    last.setNbResult(1);
                    return last;
                }
                case OBJECTS:
                    // XXX TODO
                default:
                	throw new MetaDataExecutionException("Model not supported: " + model);
            }
        } catch (MongoWriteException e) {
        	throw e;
        } catch (Exception e) {
        	throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    /**
     * Finalize the queries with last True Delete
     * @param requestToMongodb
     * @param last
     * @return the final Result 
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException 
     */
    protected Result lastDeleteFilterProjection(DeleteToMongodb requestToMongodb, Result last)
    		throws InvalidParseOperationException, MetaDataExecutionException {
        Bson roots = QueryToMongodb.getRoots(VitamDocument.ID, last.getCurrentIds());
        LOGGER.debug("To Delete: "+MongoDbHelper.bsonToString(roots, false));
        FILTERARGS model = requestToMongodb.model();
        try {
	        switch (model) {
	            case UNITS: {
	                DeleteResult result = MongoDbHelper.delete(MongoDbAccess.VitamCollections.Cunit, 
	                        roots, last.getCurrentIds().size());
	                last.setNbResult(result.getDeletedCount());
	                return last;
	            }
	            case OBJECTGROUPS: {
	                DeleteResult result = MongoDbHelper.delete(MongoDbAccess.VitamCollections.Cobjectgroup, 
	                        roots, last.getCurrentIds().size());
	                last.setNbResult(result.getDeletedCount());
	                return last;
	            }
	            case OBJECTS:
	                // XXX TODO
	            default:
	            	throw new MetaDataExecutionException("Model not supported: " + model);
	        }
        } catch (MetaDataExecutionException e) {
        	throw e;
        } catch (Exception e) {
        	throw new MetaDataExecutionException("Delete concern", e);
        }
    }

}
