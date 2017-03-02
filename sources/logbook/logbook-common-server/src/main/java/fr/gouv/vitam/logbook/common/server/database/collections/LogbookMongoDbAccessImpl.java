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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Indexes.hashed;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectToMongoDb;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookExecutionException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDb Access implementation base class
 *
 */
public final class LogbookMongoDbAccessImpl extends MongoDbAccess implements LogbookDbAccess {
    private static final String ITEM_CANNOT_BE_NULL = "Item cannot be null";
    private static final String AT_LEAST_ONE_ITEM_IS_NEEDED = "At least one item is needed";
    private static final String LOGBOOK_LIFE_CYCLE_NOT_FOUND = "LogbookLifeCycle not found";
    private static final String SELECT_ISSUE = "Select issue";
    private static final String ELEMENT_ALREADY_EXISTS = " (element already exists)";
    private static final String TIMEOUT_OPERATION = " (timeout operation)";
    private static final String EXISTS_ISSUE = "Exists issue";
    /**
     * SLICE command to optimize listing
     */
    private static final String SLICE = "$slice";
    private static final String UPDATE_NOT_FOUND_ITEM = "Update not found item: ";
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(LogbookMongoDbAccessImpl.class);
    private static final String SELECT_PARAMETER_IS_NULL = "select parameter is null";
    private static final String LIFECYCLE_ITEM = "lifecycleItem";
    private static final String OPERATION_ITEM = "operationItem";
    private static final String CREATION_ISSUE = "Creation issue";
    private static final String UPDATE_ISSUE = "Update issue";
    private static final String ROLLBACK_ISSUE = "Rollback issue";
    private static final String INIT_UPDATE_LIFECYCLE = "Initialize update lifeCycle process";
    private static final String ANOTHER_UPDATE_OPERATION_INPROCESS = "An update operation already in process";


    /**
     * Quick projection for ID Only
     */
    static final BasicDBObject ID_PROJECTION = new BasicDBObject(LogbookDocument.ID, 1);
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_SLICE_WITH_ALL_EVENTS = JsonHandler.createObjectNode().put("events", 1);
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    static final int LAST_EVENT_SLICE = -1;
    static final int TWO_LAST_EVENTS_SLICE = -2;

    private static final String OB_ID = "obId";

    static {
        DEFAULT_SLICE.putObject(LogbookDocument.EVENTS).put(SLICE, LAST_EVENT_SLICE);
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            DEFAULT_ALLKEYS.put(name.getDbname(), 1);
        }
    }


    private final LogbookElasticsearchAccess esClient;


    /**
     * Constructor
     * 
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     * @throws IllegalArgumentException if mongoClient or dbname is null
     */
    public LogbookMongoDbAccessImpl(MongoClient mongoClient, final String dbname, final boolean recreate,
        LogbookElasticsearchAccess esClient, List<Integer> tenants) {
        super(mongoClient, dbname, recreate);
        this.esClient = esClient;

        LogbookCollections.OPERATION.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_UNIT.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_OBJECTGROUP.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.initialize(getMongoDatabase(), recreate);


        // init Logbook Operation Mapping for ES
        LogbookCollections.OPERATION.initialize(this.esClient);
        for (Integer tenant : tenants) {
            LogbookCollections.OPERATION.getEsClient().addIndex(LogbookCollections.OPERATION, tenant);
        }
    }

    /**
     *
     * @return The MongoCLientOptions to apply to MongoClient
     */
    static final MongoClientOptions getMongoClientOptions() {
        final VitamDocumentCodec<LogbookOperation> operationCodec = new VitamDocumentCodec<>(LogbookOperation.class);

        final VitamDocumentCodec<LogbookLifeCycleUnit> lifecycleUnitCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleUnit.class);

        final VitamDocumentCodec<LogbookLifeCycleObjectGroup> lifecycleObjectGroupCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleObjectGroup.class);

        final VitamDocumentCodec<LogbookLifeCycleUnitInProcess> lifecycleUnitInProcessCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleUnitInProcess.class);

        final VitamDocumentCodec<LogbookLifeCycleObjectGroupInProcess> lifecycleObjectGroupInProcessCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleObjectGroupInProcess.class);


        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(operationCodec, lifecycleUnitCodec, lifecycleObjectGroupCodec,
                lifecycleUnitInProcessCodec, lifecycleObjectGroupInProcessCodec));


        return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
    }

    /**
     * Close database access
     */
    @Override
    public final void close() {
        getMongoClient().close();
    }

    /**
     * Ensure that all MongoDB database schema are indexed
     */
    static final void ensureIndex() {
        for (final LogbookCollections col : LogbookCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().createIndex(hashed(LogbookDocument.ID));
            }
        }
        LogbookOperation.addIndexes();
        LogbookLifeCycle.addIndexes();
    }

    /**
     * Remove temporarily the MongoDB Index (import optimization?)
     */
    static final void removeIndexBeforeImport() {
        LogbookOperation.dropIndexes();
        LogbookLifeCycle.dropIndexes();
    }

    /**
     * Reset MongoDB Index (import optimization?)
     */
    static final void resetIndexAfterImport() {
        LOGGER.info("Rebuild indexes");
        ensureIndex();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        // get a list of the collections in this database and print them out
        final MongoIterable<String> collectionNames = getMongoDatabase().listCollectionNames();
        for (final String s : collectionNames) {
            builder.append(s).append('\n');
        }
        for (final LogbookCollections coll : LogbookCollections.values()) {
            if (coll != null && coll.getCollection() != null) {
                final MongoCollection<?> mcoll = coll.getCollection();
                builder.append(coll.getName()).append(" [").append(mcoll.count()).append('\n');
                final ListIndexesIterable<Document> list = mcoll.listIndexes();
                for (final Document dbObject : list) {
                    builder.append("\t").append(mcoll.count()).append(' ').append(dbObject).append('\n');
                }
            }
        }
        return builder.toString();
    }


    /**
     * @return the Elasticsearch Acess Logbook client
     */
    public LogbookElasticsearchAccess getEsClient() {
        return esClient;
    }

    @Override
    public final long getLogbookOperationSize() {
        return LogbookCollections.OPERATION.getCollection().count();
    }

    @Override
    public final long getLogbookLifeCyleUnitSize() {
        return LogbookCollections.LIFECYCLE_UNIT.getCollection().count();
    }

    @Override
    public final long getLogbookLifeCyleObjectGroupSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().count();
    }

    @Override
    public long getLogbookLifeCyleUnitInProcessSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getCollection().count();
    }

    @Override
    public long getLogbookLifeCyleObjectGroupInProcessSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.getCollection().count();
    }

    /**
     * Force flush on disk (MongoDB): should not be used
     */
    final void flushOnDisk() {
        getMongoAdmin().runCommand(new BasicDBObject("fsync", 1).append("async", true)
            .append("lock", false));
    }

    /**
     * Example code to get the reason from MongoException. <br>
     * <br>
     * Results are: DUPLICATE_KEY (duplicate entry), EXECUTION_TIMEOUT, UNCATEGORIZED
     *
     * @param e
     * @return the ErrorCategory
     */
    private ErrorCategory getErrorCategory(MongoException e) {
        return ErrorCategory.fromErrorCode(e.getCode());
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookOperation> getLogbookOperations(JsonNode select, boolean sliced)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);

        // TODO P1 Temporary fix as the obIdIn (MessageIdentifier in the SEDA manifest) is only available on the 2 to
        // last
        // Logbook operation event . Must be removed when the processing will be reworked
        if (sliced) {
            final ObjectNode operationSlice = JsonHandler.createObjectNode();
            operationSlice.putObject(LogbookDocument.EVENTS).put(SLICE, TWO_LAST_EVENTS_SLICE);
            return select(LogbookCollections.OPERATION, select, operationSlice);
        } else {
            return select(LogbookCollections.OPERATION, select, DEFAULT_SLICE_WITH_ALL_EVENTS);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycle> getLogbookLifeCycleUnits(JsonNode select, boolean sliced,
        LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        if (sliced) {
            final ObjectNode operationSlice = JsonHandler.createObjectNode();
            operationSlice.putObject(LogbookDocument.EVENTS).put(SLICE, LAST_EVENT_SLICE);
            return select(collection, select, operationSlice);
        } else {
            return select(collection, select, DEFAULT_SLICE_WITH_ALL_EVENTS);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycleUnit> getLogbookLifeCycleUnitsFull(LogbookCollections collection, Select select)
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        try {
            return selectExecute(collection, select);
        } catch (final InvalidParseOperationException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycle> getLogbookLifeCycleObjectGroups(JsonNode select, boolean sliced,
        LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(collection, select, sliced);

    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifeCycleObjectGroupsFull(LogbookCollections collection,
        Select select)
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        try {
            return selectExecute(collection, select);
        } catch (final InvalidParseOperationException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    /**
     * Check if one id exists already
     *
     * @param collection
     * @param id
     * @return True if one LogbookDocument<?> object exists with this id
     * @throws LogbookDatabaseException
     */
    final boolean exists(final LogbookCollections collection, final String id)
        throws LogbookDatabaseException {
        try {
            return collection.getCollection().find(eq(LogbookDocument.ID,
                id)).projection(ID_PROJECTION).first() != null;
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(EXISTS_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        EXISTS_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public final boolean existsLogbookOperation(final String operationItem)
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationItem);
        return exists(LogbookCollections.OPERATION, operationItem);
    }

    @Override
    public boolean existsLogbookLifeCycleUnit(String unitId)
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, unitId);
        return exists(LogbookCollections.LIFECYCLE_UNIT, unitId);
    }

    @Override
    public boolean existsLogbookLifeCycleObjectGroup(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, objectGroupId);
        return exists(LogbookCollections.LIFECYCLE_OBJECTGROUP, objectGroupId);
    }

    @SuppressWarnings("rawtypes")
    final VitamDocument getLogbook(final LogbookCollections collection, final String id)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter("Logbook item", id);
        VitamDocument item = null;
        try {
            item = (VitamDocument) collection.getCollection().find(eq(LogbookDocument.ID, id)).first();
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(SELECT_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        SELECT_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
        if (item == null) {
            throw new LogbookNotFoundException("Logbook item not found");
        }
        return item;
    }

    @Override
    public LogbookOperation getLogbookOperation(String eventIdentifierProcess)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookOperation) getLogbook(LogbookCollections.OPERATION, eventIdentifierProcess);
    }

    @Override
    public LogbookLifeCycleUnit getLogbookLifeCycleUnit(String unitId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleUnit) getLogbook(LogbookCollections.LIFECYCLE_UNIT, unitId);
    }

    @Override
    public LogbookLifeCycleUnit getLogbookLifeCycleUnit(JsonNode queryDsl, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleUnit) getLogbook(collection, queryDsl.findValue(
            LogbookMongoDbName.objectIdentifier.getDbname()).asText());
    }

    @Override
    public LogbookLifeCycleObjectGroup getLogbookLifeCycleObjectGroup(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleObjectGroup) getLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP, objectGroupId);
    }

    @SuppressWarnings("rawtypes")
    final VitamDocument getLogbookPerOperation(final LogbookCollections collection, String idOperation, String id)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, idOperation, id);
        VitamDocument lifecycle = null;
        try {
            lifecycle = (VitamDocument) collection.getCollection().find(and(eq(LogbookDocument.ID, id),
                or(eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), idOperation),
                    eq(LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                        idOperation))))
                .first();
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(SELECT_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        SELECT_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
        if (lifecycle == null) {
            throw new LogbookNotFoundException(LOGBOOK_LIFE_CYCLE_NOT_FOUND);
        }
        return lifecycle;
    }

    @Override
    public LogbookLifeCycleUnit getLogbookLifeCycleUnit(String idOperation, String unitId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleUnit) getLogbookPerOperation(LogbookCollections.LIFECYCLE_UNIT, idOperation, unitId);
    }

    @Override
    public LogbookLifeCycleObjectGroup getLogbookLifeCycleObjectGroup(String idOperation, String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleObjectGroup) getLogbookPerOperation(LogbookCollections.LIFECYCLE_OBJECTGROUP,
            idOperation, objectGroupId);
    }

    /**
     * Internal select
     *
     * @param collection domain of request
     * @param select
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws LogbookException
     */
    @SuppressWarnings("rawtypes")
    private final MongoCursor select(final LogbookCollections collection, final JsonNode select, boolean sliced)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (sliced) {
            return select(collection, select, DEFAULT_SLICE);
        } else {
            return select(collection, select, DEFAULT_SLICE_WITH_ALL_EVENTS);
        }
    }

    /**
     * Select with slice possibility
     *
     * @param collection
     * @param select
     * @param slice may be null
     * @return the closeable MongoCursor
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    @SuppressWarnings("rawtypes")
    private final MongoCursor select(final LogbookCollections collection, final JsonNode select, final ObjectNode slice)
        throws LogbookDatabaseException, LogbookNotFoundException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(select);
            if (slice == null) {
                parser.addProjection(JsonHandler.createObjectNode(), DEFAULT_ALLKEYS);
            } else {
                parser.addProjection(slice, DEFAULT_ALLKEYS);
            }
            // FIXME filter on traceability to adapt
            if (LogbookCollections.OPERATION.equals(collection) &&
                parser.getRequest().getFinalSelect().toString()
                    .contains(QueryHelper
                        .eq(LogbookMongoDbName.eventTypeProcess.getDbname(), LogbookTypeProcess.TRACEABILITY.name())
                        .toString())) {
                return findDocumentsElasticsearch(collection, parser);
            } else {
                return selectExecute(collection, parser);
            }
        } catch (final InvalidParseOperationException | InvalidCreateOperationException | LogbookException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    /**
     * @param collection
     * @param parser
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException
     */
    @SuppressWarnings("rawtypes")
    private MongoCursor selectExecute(final LogbookCollections collection, SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongoDb selectToMongoDb = new SelectToMongoDb(parser);
        // FIXME - add a method to VitamDocument to specify if the tenant should be filtered for collection.
        // if the collection should not be filtered, then the method should be overridden
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson condition = and(QueryToMongodb.getCommand(selectToMongoDb.getSelect().getQuery()),
            eq(VitamDocument.TENANT_ID, tenantId));
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset = selectToMongoDb.getFinalOffset();
        final int limit = selectToMongoDb.getFinalLimit();
        FindIterable<?> find = collection.getCollection().find(condition).skip(offset);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit > 0) {
            find = find.limit(limit);
        }
        return find.iterator();
    }

    /**
     * @param collection
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException
     */
    @SuppressWarnings("rawtypes")
    private MongoCursor selectExecute(final LogbookCollections collection, Select select)
        throws InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
        parser.parse(select.getFinalSelect());
        parser.addProjection(DEFAULT_SLICE_WITH_ALL_EVENTS, DEFAULT_ALLKEYS);
        return selectExecute(collection, parser);
    }

    @SuppressWarnings("rawtypes")
    final VitamDocument getDocument(LogbookParameters item) {
        if (item instanceof LogbookOperationParameters) {
            return new LogbookOperation((LogbookOperationParameters) item);
        } else if (item instanceof LogbookLifeCycleUnitParameters) {
            return new LogbookLifeCycleUnitInProcess((LogbookLifeCycleUnitParameters) item);
        } else {
            return new LogbookLifeCycleObjectGroupInProcess((LogbookLifeCycleObjectGroupParameters) item);
        }
    }

    @SuppressWarnings("rawtypes")
    final VitamDocument getDocumentForUpdate(LogbookParameters item) {
        if (item instanceof LogbookOperationParameters) {
            return new LogbookOperation((LogbookOperationParameters) item, true);
        } else if (item instanceof LogbookLifeCycleUnitParameters) {
            return new LogbookLifeCycleUnitInProcess((LogbookLifeCycleUnitParameters) item);
        } else {
            return new LogbookLifeCycleObjectGroupInProcess((LogbookLifeCycleObjectGroupParameters) item);
        }
    }

    @SuppressWarnings("unchecked")
    final void createLogbook(LogbookCollections collection, LogbookParameters item)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, item);
        try {
            VitamDocument vitamDocument = getDocument(item);
            collection.getCollection().insertOne(vitamDocument);

            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                insertIntoElasticsearch(collection, vitamDocument);
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + ELEMENT_ALREADY_EXISTS, e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        } catch (final LogbookExecutionException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    @Override
    public void createLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createLogbook(LogbookCollections.OPERATION, operationItem);
    }

    @Override
    public void createLogbookLifeCycleUnit(String idOperation, LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to create the LifeCycle");
        }
        createLogbook(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, lifecycleItem);
    }

    @Override
    public void createLogbookLifeCycleObjectGroup(String idOperation,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to create the LifeCycle");
        }
        createLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, lifecycleItem);
    }

    final void updateLogbook(LogbookCollections collection, LogbookParameters item)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, item);
        @SuppressWarnings("rawtypes")
        final VitamDocument document = getDocumentForUpdate(item);
        try {
            // Save the _id content before removing it
            final String mainLogbookDocumentId = document.getId();

            // Remove _id and events fields
            document.remove(LogbookDocument.EVENTS);
            document.remove(LogbookDocument.ID);

            final UpdateResult result = collection.getCollection().updateOne(
                eq(LogbookDocument.ID, mainLogbookDocumentId),
                Updates.push(LogbookDocument.EVENTS, document));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + mainLogbookDocumentId);
            }
            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                updateIntoElasticsearch(collection, mainLogbookDocumentId);
            }

        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        } catch (LogbookExecutionException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    private LogbookCollections fromInProcessToProdCollection(LogbookCollections collection) {
        if (LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.equals(collection)) {
            return LogbookCollections.LIFECYCLE_UNIT;
        } else if (LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.equals(collection)) {
            return LogbookCollections.LIFECYCLE_OBJECTGROUP;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes"})
    private void updateLogbookLifeCycle(LogbookCollections inProcessCollection, LogbookParameters... parameters)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, inProcessCollection);
        if (parameters == null || parameters.length == 0) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }

        LogbookTypeProcess processMode = parameters[0].getTypeProcess();
        String objectId = parameters[0].getParameterValue(LogbookParameterName.objectIdentifier);
        LogbookCollections prodCollection = fromInProcessToProdCollection(inProcessCollection);

        // 1- Check if it exists in Production Collection before proceeding to update
        LogbookLifeCycle lifeCycleInProd = null;

        try {
            lifeCycleInProd = (LogbookLifeCycle) getLogbook(prodCollection, objectId);
        } catch (LogbookNotFoundException e) {
            if (LogbookTypeProcess.UPDATE.equals(processMode)) {
                throw e;
            }
        }

        if (lifeCycleInProd != null) {

            // Check if there are other operations in process for the given item
            LogbookLifeCycle lifeCycleInProcess = null;
            try {
                lifeCycleInProcess = (LogbookLifeCycle) getLogbook(inProcessCollection, objectId);
            } catch (LogbookNotFoundException e) {
                LOGGER.info(INIT_UPDATE_LIFECYCLE);
            }

            if (lifeCycleInProcess == null) {
                // This is the first and the only operation on the current object
                // So add an element in temporary collection
                // Copy the main part of the lifeCycle saved on production collection and append given parameters to
                // events
                createLogbookLifeCycleForUpdate(inProcessCollection, lifeCycleInProd);
            }
        }

        // Update the temporary lifeCycle
        updateBulkLogbook(inProcessCollection, parameters);
    }



    @Override
    public void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateLogbook(LogbookCollections.OPERATION, operationItem);
    }

    @Override
    public void updateLogbookLifeCycleUnit(String idOperation, LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }

        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, lifecycleItem);
    }

    @Override
    public void updateLogbookLifeCycleObjectGroup(String idOperation,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }

        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, lifecycleItem);
    }

    final void rollbackLogbookLifeCycle(LogbookCollections collection, String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, lifecycleItem);
        try {
            final DeleteResult result = collection.getCollection().deleteOne(
                and(eq(LogbookDocument.ID, lifecycleItem),
                    or(eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), idOperation),
                        eq(LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                            idOperation))));
            if (result.getDeletedCount() != 1) {
                throw new LogbookNotFoundException(ROLLBACK_ISSUE + " not found: " + lifecycleItem);
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(ROLLBACK_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        ROLLBACK_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public void rollbackLogbookLifeCycleUnit(String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        rollbackLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, idOperation, lifecycleItem);
    }

    @Override
    public void rollbackLogbookLifeCycleObjectGroup(String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        rollbackLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, idOperation, lifecycleItem);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    final void createBulkLogbook(LogbookCollections collection, final LogbookParameters... items)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }
        int i = 0;
        final VitamDocument document = getDocument(items[i]);
        final List<VitamDocument> events = new ArrayList<>(items.length - 1);
        for (i = 1; i < items.length; i++) {
            final VitamDocument currentEvent = getDocumentForUpdate(items[i]);
            currentEvent.remove(LogbookDocument.EVENTS);
            currentEvent.remove(LogbookDocument.ID);
            events.add(currentEvent);
        }
        document.append(LogbookDocument.EVENTS, events);
        try {
            collection.getCollection().insertOne(document);
            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                insertIntoElasticsearch(collection, document);
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + ELEMENT_ALREADY_EXISTS, e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        } catch (final LogbookExecutionException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    @Override
    public final void createBulkLogbookOperation(final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.OPERATION, operationItems);
    }

    @Override
    public final void createBulkLogbookLifeCycleUnit(final LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, lifecycleItems);
    }

    @Override
    public final void createBulkLogbookLifeCycleObjectGroup(
        final LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, lifecycleItems);
    }

    @SuppressWarnings("rawtypes")
    final void updateBulkLogbook(final LogbookCollections collection, final LogbookParameters... items)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }
        final List<VitamDocument> events = new ArrayList<>(items.length);
        // Get the first event to preserve the _id field value
        final VitamDocument firstEvent = getDocumentForUpdate(items[0]);
        final String mainLogbookDocumentId = firstEvent.getId();

        firstEvent.remove(LogbookDocument.EVENTS);
        firstEvent.remove(LogbookDocument.ID);
        events.add(firstEvent);

        for (int i = 1; i < items.length; i++) {
            // Remove _id and events fields
            final VitamDocument currentEvent = getDocument(items[i]);
            currentEvent.remove(LogbookDocument.EVENTS);
            currentEvent.remove(LogbookDocument.ID);

            events.add(currentEvent);
        }

        try {
            final UpdateResult result = collection.getCollection().updateOne(
                eq(LogbookDocument.ID, mainLogbookDocumentId),
                Updates.pushEach(LogbookDocument.EVENTS, events));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + mainLogbookDocumentId);
            }
            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                updateIntoElasticsearch(collection, mainLogbookDocumentId);
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        } catch (final LogbookExecutionException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    @Override
    public final void updateBulkLogbookOperation(final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateBulkLogbook(LogbookCollections.OPERATION, operationItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleUnit(LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, lifecycleItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleObjectGroup(LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, lifecycleItems);
    }

    // Not check, test feature !
    @Override
    public void deleteCollection(LogbookCollections collection) throws DatabaseException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        final long count = collection.getCollection().count();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            final DeleteResult result = collection.getCollection().deleteMany(new Document().append("_tenant", tenantId));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getDeletedCount());
            }
            if (LogbookCollections.OPERATION.equals(collection)) {
                esClient.deleteIndex(LogbookCollections.OPERATION, tenantId);
                esClient.addIndex(LogbookCollections.OPERATION, tenantId);
            }
            if (result.getDeletedCount() != count) {
                throw new DatabaseException(String.format("%s: Delete %s from %s elements", collection.getName(), result
                    .getDeletedCount(), count));
            }
        }
    }

    @Override
    public LogbookLifeCycleUnitInProcess getLogbookLifeCycleUnitInProcess(String unitId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleUnitInProcess) getLogbook(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, unitId);
    }

    @Override
    public LogbookLifeCycleObjectGroupInProcess getLogbookLifeCycleObjectGroupInProcess(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return (LogbookLifeCycleObjectGroupInProcess) getLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS,
            objectGroupId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void createLogbookLifeCycleUnit(
        LogbookLifeCycleUnitInProcess logbookLifeCycleUnitInProcess)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {

        // Create Unit lifeCycle from LogbookLifeCycleUnitInProcess instance
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleUnitInProcess);

        try {
            LogbookLifeCycleUnit logbookLifeCycleUnit =
                new LogbookLifeCycleUnit(logbookLifeCycleUnitInProcess.toJson());

            LogbookCollections.LIFECYCLE_UNIT.getCollection()
                .insertOne(logbookLifeCycleUnit);
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + ELEMENT_ALREADY_EXISTS, e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void createLogbookLifeCycleObjectGroup(
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGrouptInProcess)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {

        // Create ObjectGRoup lifeCycle from LogbookLifeCycleObjectGroupInProcess instance
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleObjectGrouptInProcess);

        try {
            LogbookLifeCycleObjectGroup logbookLifeCycleObjectGroup =
                new LogbookLifeCycleObjectGroup(logbookLifeCycleObjectGrouptInProcess.toJson());

            LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection()
                .insertOne(logbookLifeCycleObjectGroup);
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + ELEMENT_ALREADY_EXISTS, e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    private void rollBackLifeCyclesByOperation(LogbookCollections isProcessCollection, String operationId)
        throws LogbookNotFoundException, LogbookDatabaseException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationId);
        try {

            // 1- Delete temporary lifeCycles
            DeleteResult result = isProcessCollection.getCollection()
                .deleteMany(or(eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), operationId),
                    eq(LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                        operationId)));

            if (result.getDeletedCount() == 0) {
                throw new LogbookNotFoundException(ROLLBACK_ISSUE + " not found: " + operationId);
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(ROLLBACK_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        ROLLBACK_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void createLogbookLifeCycleForUpdate(LogbookCollections inProccessCollection,
        LogbookLifeCycle logbookLifeCycleInProd)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {

        // Create Unit lifeCycle from LogbookLifeCycleUnitInProcess instance
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleInProd);

        try {
            LogbookLifeCycle logbookLifeCycleInProcess = null;

            if (LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.equals(inProccessCollection)) {
                logbookLifeCycleInProcess =
                    new LogbookLifeCycleUnitInProcess(logbookLifeCycleInProd.toJson());
            } else if (LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.equals(inProccessCollection)) {
                logbookLifeCycleInProcess =
                    new LogbookLifeCycleObjectGroup(logbookLifeCycleInProd.toJson());
            }

            if (logbookLifeCycleInProcess == null) {
                throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION);
            }

            logbookLifeCycleInProcess.remove(LogbookDocument.EVENTS);
            logbookLifeCycleInProcess.append(LogbookLifeCycleMongoDbName.eventTypeProcess.getDbname(),
                LogbookTypeProcess.UPDATE.toString());
            logbookLifeCycleInProcess.append(LogbookDocument.EVENTS, Arrays.asList(new String[0]));


            inProccessCollection.getCollection()
                .insertOne(logbookLifeCycleInProcess);
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + ELEMENT_ALREADY_EXISTS, e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void updateLogbookLifeCycleUnit(LogbookLifeCycleUnitInProcess logbookLifeCycleUnitInProcess)
        throws LogbookDatabaseException, LogbookNotFoundException {

        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleUnitInProcess);
        String logbookLifeCycleId = logbookLifeCycleUnitInProcess.getId();

        try {
            final UpdateResult result = LogbookCollections.LIFECYCLE_UNIT.getCollection().updateOne(
                eq(LogbookDocument.ID, logbookLifeCycleId),
                Updates.pushEach(LogbookDocument.EVENTS,
                    (List<VitamDocument>) logbookLifeCycleUnitInProcess.get(LogbookDocument.EVENTS)));

            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + logbookLifeCycleId);
            }

            // Do not delete the temporary lifeCycle when it is on an INGEST process
            List<VitamDocument> newEvents =
                (List<VitamDocument>) logbookLifeCycleUnitInProcess.get(LogbookDocument.EVENTS);
            if (newEvents != null && newEvents.size() != 0) {
                LogbookTypeProcess typeProcess = LogbookTypeProcess
                    .valueOf(
                        ((Document) newEvents.get(0))
                            .get(LogbookLifeCycleMongoDbName.eventTypeProcess.getDbname())
                            .toString());
                if (!LogbookTypeProcess.INGEST.equals(typeProcess)) {
                    // Delete the temporary lifeCycle
                    LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getCollection()
                        .deleteOne(logbookLifeCycleUnitInProcess);
                }
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void updateLogbookLifeCycleObjectGroup(
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGrouptInProcess)
        throws LogbookDatabaseException, LogbookNotFoundException {

        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleObjectGrouptInProcess);
        String logbookLifeCycleId = logbookLifeCycleObjectGrouptInProcess.getId();

        try {
            final UpdateResult result = LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().updateOne(
                eq(LogbookDocument.ID, logbookLifeCycleId),
                Updates.pushEach(LogbookDocument.EVENTS,
                    (List<VitamDocument>) logbookLifeCycleObjectGrouptInProcess.get(LogbookDocument.EVENTS)));

            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + logbookLifeCycleId);
            }

            // Do not delete the temporary lifeCycle when it is on an INGEST process
            List<VitamDocument> newEvents =
                (List<VitamDocument>) logbookLifeCycleObjectGrouptInProcess.get(LogbookDocument.EVENTS);
            if (newEvents != null && newEvents.size() != 0) {
                LogbookTypeProcess typeProcess = LogbookTypeProcess
                    .valueOf(
                        ((Document) newEvents.get(0))
                            .get(LogbookLifeCycleMongoDbName.eventTypeProcess.getDbname())
                            .toString());
                if (!LogbookTypeProcess.INGEST.equals(typeProcess)) {
                    // Delete the temporary lifeCycle
                    LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.getCollection()
                        .deleteOne(logbookLifeCycleObjectGrouptInProcess);
                }
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + TIMEOUT_OPERATION, e);
                case UNCATEGORIZED:
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

    @Override
    public void rollBackUnitLifeCyclesByOperation(String operationId)
        throws LogbookNotFoundException, LogbookDatabaseException {
        rollBackLifeCyclesByOperation(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, operationId);
    }

    @Override
    public void rollBackObjectGroupLifeCyclesByOperation(String operationId)
        throws LogbookNotFoundException, LogbookDatabaseException {
        rollBackLifeCyclesByOperation(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, operationId);
    }

    @Override
    public boolean existsLogbookLifeCycleUnitInProcess(String unitId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, unitId);
        return exists(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, unitId);
    }

    @Override
    public boolean existsLogbookLifeCycleObjectGroupInProcess(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, objectGroupId);
        return exists(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, objectGroupId);
    }


    /**
     * Search in elastic search then get object detail in MongoDb.
     * 
     * @param collection the collection
     * @param parser the parser containing the query
     * @return the cursor on the result datas
     * @throws InvalidParseOperationException if the MongoDb query can't be translated to ES a valid query
     * @throws InvalidCreateOperationException if a MongoDb query can't be created from ES results
     * @throws LogbookException if an exception occured while executing the ES query
     */
    private MongoCursor<?> findDocumentsElasticsearch(LogbookCollections collection,
        SelectParserSingle parser)
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        final SelectToMongodb requestToMongodb = new SelectToMongodb(parser);
        QueryBuilder query = QueryToElasticsearch.getCommand(requestToMongodb.getNthQuery(0));
        List<SortBuilder> sorts = QueryToElasticsearch.getSorts(requestToMongodb.getFinalOrderBy());
        SearchResponse elasticSearchResponse =
            collection.getEsClient().search(collection, tenantId, query, null, sorts, requestToMongodb.getFinalOffset(),
                requestToMongodb.getFinalLimit());
        if (elasticSearchResponse.status() != RestStatus.OK) {
            return new EmptyMongoCursor();
        }
        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits() == 0) {
            return new EmptyMongoCursor();
        }
        final Iterator<SearchHit> iterator = hits.iterator();
        final BooleanQuery newQuery = or();
        // get document with Elasticsearch then create a new request to mongodb with unique object's attribute
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            final Map<String, Object> src = hit.getSource();
            LOGGER.debug("findDocumentsElasticsearch result" + src.toString());
            if (src.get(LogbookMongoDbName.eventIdentifierProcess.getDbname()) != null) {
                newQuery.add(QueryHelper.eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                    src.get(LogbookMongoDbName.eventIdentifierProcess.getDbname()).toString()));
            }
        }
        // replace query with list of ids from es
        parser.getRequest().setQuery(newQuery);
        return selectExecute(collection, parser);
    }


    /**
     * Insert a new document in ES.
     * 
     * @param collection the collection
     * @param vitamDocument the document to save in ES
     * @throws LogbookExecutionException if the ES insert was in error
     */
    private void insertIntoElasticsearch(LogbookCollections collection, VitamDocument vitamDocument)
        throws LogbookExecutionException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        LOGGER.debug("insertToElasticsearch");
        Map<String, String> mapIdJson = new HashMap<>();
        String id = vitamDocument.getId();
        vitamDocument.remove(LogbookCollections.ID);
        tranformEvDetDataForElastic(vitamDocument);
        final String mongoJson = vitamDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));
        vitamDocument.clear();
        final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();
        mapIdJson.put(id, esJson);
        final BulkResponse bulkResponse = collection.getEsClient().addEntryIndexes(collection, tenantId, mapIdJson);
        if (bulkResponse.hasFailures()) {
            throw new LogbookExecutionException("Index Elasticsearch has errors");
        }
    }

    /**
     * Update a document in ES by loading its value in mongodb and saving it in it's corresponding ES index.
     * 
     * @param collection the collection
     * @param id the id of the document to update
     * @throws LogbookExecutionException if the ES update was in error
     * @throws LogbookNotFoundException if the document was not found in mongodb
     */
    private void updateIntoElasticsearch(LogbookCollections collection, String id)
        throws LogbookExecutionException, LogbookNotFoundException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        LOGGER.debug("updateIntoElasticsearch");
        VitamDocument existingDocument =
            (VitamDocument) collection.getCollection().find(eq(VitamDocument.ID, id)).first();
        if (existingDocument == null) {
            throw new LogbookNotFoundException("Logbook item not found");
        }
        existingDocument.remove(LogbookCollections.ID);
        tranformEvDetDataForElastic(existingDocument);
        final String mongoJson = existingDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));
        existingDocument.clear();
        final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();
        final boolean response = collection.getEsClient().updateEntryIndex(collection, tenantId, id, esJson);
        if (response == false) {
            throw new LogbookExecutionException("Update Elasticsearch has errors");
        }
    }

    /**
     * Replace the "evDetData" value in the document and the sub-events from a string by a json object
     * 
     * @param vitamDocument logbook vitam document
     */
    private void tranformEvDetDataForElastic(VitamDocument vitamDocument) {
        if (vitamDocument.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
            String evDetDataString = (String) vitamDocument.get(LogbookMongoDbName.eventDetailData.getDbname());
            LOGGER.error(evDetDataString);
            try {
                JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
                vitamDocument.remove(LogbookMongoDbName.eventDetailData.getDbname());
                vitamDocument.put(LogbookMongoDbName.eventDetailData.getDbname(), evDetData);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("EvDetData is not a json compatible field");
            }
        }
        List<Document> eventDocuments = (List<Document>) vitamDocument.get(LogbookDocument.EVENTS);
        if (eventDocuments != null) {
            for (Document eventDocument : eventDocuments) {
                if (eventDocument.getString(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                    String eventEvDetDataString =
                        eventDocument.getString(LogbookMongoDbName.eventDetailData.getDbname());
                    Document eventEvDetDataDocument = Document.parse(eventEvDetDataString);
                    eventDocument.remove(LogbookMongoDbName.eventDetailData.getDbname());
                    eventDocument.put(LogbookMongoDbName.eventDetailData.getDbname(), eventEvDetDataDocument);
                }
            }
        }
        vitamDocument.remove(LogbookDocument.EVENTS);
        vitamDocument.put(LogbookDocument.EVENTS, eventDocuments);

    }

}
