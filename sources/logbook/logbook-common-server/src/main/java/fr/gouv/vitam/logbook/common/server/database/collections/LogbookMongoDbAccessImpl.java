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
package fr.gouv.vitam.logbook.common.server.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.NopQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestHelper;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.server.mongodb.VitamMongoCursor;
import fr.gouv.vitam.common.database.translators.elasticsearch.SelectToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
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
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Indexes.hashed;
import static com.mongodb.client.model.Updates.combine;
import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.ID;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.LAST_PERSISTED_DATE;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.TENANT_ID;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.VERSION;

/**
 * MongoDb Access implementation base class
 */
public final class LogbookMongoDbAccessImpl extends MongoDbAccess implements LogbookDbAccess {
    private static final String ITEM_CANNOT_BE_NULL = "Item cannot be null";
    private static final String AT_LEAST_ONE_ITEM_IS_NEEDED = "At least one item is needed";
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

    /**
     * Quick projection for ID Only
     */
    static final BasicDBObject ID_PROJECTION = new BasicDBObject(LogbookDocument.ID, 1);
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_SLICE_WITH_ALL_EVENTS = JsonHandler.createObjectNode().put("events", 1);
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    static final int LAST_EVENT_SLICE = -1;
    static final int TWO_LAST_EVENTS_SLICE = -2;

    static {
        DEFAULT_SLICE.putObject(LogbookDocument.EVENTS).put(SLICE, LAST_EVENT_SLICE);
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            DEFAULT_ALLKEYS.put(name.getDbname(), 1);
        }
        DEFAULT_ALLKEYS.put(TENANT_ID, 1);
        DEFAULT_ALLKEYS.put(VERSION, 1);
        DEFAULT_ALLKEYS.put(LAST_PERSISTED_DATE, 1);
        DEFAULT_ALLKEYS.put(ID, 1);
    }

    private final LogbookElasticsearchAccess esClient;
    private final LogbookTransformData logbookTransformData;
    private final OntologyLoader ontologyLoader;

    /**
     * Constructor
     *
     * @param ontologyLoader
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     * @param esClient elastic search client
     * @param tenants the tenants list
     * @throws IllegalArgumentException if mongoClient or dbname is null
     */
    public LogbookMongoDbAccessImpl(MongoClient mongoClient, final String dbname, final boolean recreate,
        LogbookElasticsearchAccess esClient, List<Integer> tenants, LogbookTransformData logbookTransformData,
        OntologyLoader ontologyLoader) {
        super(mongoClient, dbname, recreate);
        this.esClient = esClient;
        this.logbookTransformData = logbookTransformData;
        this.ontologyLoader = ontologyLoader;

        // FIXME : externalize initialization of collections to avoid being dependant of current class instanciation
        // when using the static LogbookCollections


        LogbookCollections.OPERATION.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_UNIT.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_OBJECTGROUP.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.initialize(getMongoDatabase(), recreate);
        LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.initialize(getMongoDatabase(), recreate);


        // init Logbook Operation Mapping for ES
        LogbookCollections.OPERATION.initialize(this.esClient);
        for (Integer tenant : tenants) {
            Map<String, String> map =
                LogbookCollections.OPERATION.getEsClient().addIndex(LogbookCollections.OPERATION, tenant);
            if (map.isEmpty()) {
                throw new RuntimeException(
                    "Index not created for the collection " + LogbookCollections.OPERATION.getName() +
                        " and tenant :" +
                        tenant);
            }
        }
    }

    /**
     * @return The MongoCLientOptions to apply to MongoClient
     */
    public static final MongoClientOptions getMongoClientOptions() {
        final VitamDocumentCodec<LogbookOperation> operationCodec = new VitamDocumentCodec<>(LogbookOperation.class);

        final VitamDocumentCodec<LogbookLifeCycleUnit> lifecycleUnitCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleUnit.class);

        final VitamDocumentCodec<LogbookLifeCycleObjectGroup> lifecycleObjectGroupCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleObjectGroup.class);

        final VitamDocumentCodec<LogbookLifeCycleUnitInProcess> lifecycleUnitInProcessCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleUnitInProcess.class);

        final VitamDocumentCodec<LogbookLifeCycleObjectGroupInProcess> lifecycleObjectGroupInProcessCodec =
            new VitamDocumentCodec<>(LogbookLifeCycleObjectGroupInProcess.class);


        final CodecRegistry codecRegistry = CodecRegistries
            .fromRegistries(CodecRegistries.fromCodecs(operationCodec, lifecycleUnitCodec, lifecycleObjectGroupCodec,
                lifecycleUnitInProcessCodec, lifecycleObjectGroupInProcessCodec,
                new VitamDocumentCodec<>(VitamDocument.class)), MongoClient.getDefaultCodecRegistry());


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
    public String getInfo() {
        final StringBuilder builder = new StringBuilder();
        // get a list of the collections in this database and print them out
        final MongoIterable<String> collectionNames = getMongoDatabase().listCollectionNames();
        for (final String s : collectionNames) {
            builder.append(s).append('\n');
        }
        for (final LogbookCollections coll : LogbookCollections.values()) {
            if (coll != null && coll.getCollection() != null) {
                final MongoCollection<?> mcoll = coll.getCollection();
                builder.append(coll.getName()).append(" [").append(mcoll.countDocuments()).append('\n');
                final ListIndexesIterable<Document> list = mcoll.listIndexes();
                for (final Document dbObject : list) {
                    builder.append("\t").append(mcoll.countDocuments()).append(' ').append(dbObject).append('\n');
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
        return LogbookCollections.OPERATION.getCollection().countDocuments();
    }

    @Override
    public final long getLogbookLifeCyleUnitSize() {
        return LogbookCollections.LIFECYCLE_UNIT.getCollection().countDocuments();
    }

    @Override
    public final long getLogbookLifeCyleObjectGroupSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().countDocuments();
    }

    @Override
    public long getLogbookLifeCyleUnitInProcessSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getCollection().countDocuments();
    }

    @Override
    public long getLogbookLifeCyleObjectGroupInProcessSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.getCollection().countDocuments();
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
    public VitamMongoCursor<LogbookOperation> getLogbookOperations(JsonNode select, boolean sliced)
        throws LogbookDatabaseException, VitamDBException {
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
    public LogbookLifeCycle getOneLogbookLifeCycle(JsonNode select, boolean sliced, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, VitamDBException {
        MongoCursor<LogbookLifeCycle> result = getLogbookLifeCycles(select, sliced, collection);
        if (result.hasNext()) {
            LogbookLifeCycle logbookLifeCycle = result.next();
            if (result.hasNext()) {
                throw new LogbookDatabaseException("Result size more than 1.");
            } else {
                return logbookLifeCycle;
            }
        }
        throw new LogbookNotFoundException("Logbook lifecycle was not found");
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycle> getLogbookLifeCycles(JsonNode select, boolean sliced,
        LogbookCollections collection)
        throws LogbookDatabaseException, VitamDBException {
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
    public VitamMongoCursor<LogbookLifeCycleUnit> getLogbookLifeCycleUnitsFull(LogbookCollections collection,
        Select select)
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
    public VitamMongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifeCycleObjectGroupsFull(
        LogbookCollections collection,
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
        throws LogbookDatabaseException {
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
        throws LogbookDatabaseException, VitamDBException {
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
    private final VitamMongoCursor select(final LogbookCollections collection, final JsonNode select,
        final ObjectNode slice)
        throws LogbookDatabaseException, VitamDBException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(select);
            if (slice == null) {
                parser.addProjection(JsonHandler.createObjectNode(), DEFAULT_ALLKEYS);
            } else {
                parser.addProjection(slice, DEFAULT_ALLKEYS);
            }
            if (LogbookCollections.OPERATION.equals(collection)) {
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
    private VitamMongoCursor selectExecute(final LogbookCollections collection, SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongodb selectToMongoDb = new SelectToMongodb(parser);
        Integer tenantId = ParameterHelper.getTenantParameter();
        final Bson condition = and(QueryToMongodb.getCommand(selectToMongoDb.getSingleSelect().getQuery()),
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
        return new VitamMongoCursor(find.iterator());
    }

    /**
     * @param collection
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     * @throws LogbookException
     */
    @SuppressWarnings("rawtypes")
    private VitamMongoCursor selectExecute(final LogbookCollections collection, Select select)
        throws InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
        parser.parse(select.getFinalSelect());
        parser.addProjection(DEFAULT_SLICE_WITH_ALL_EVENTS, DEFAULT_ALLKEYS);
        return selectExecute(collection, parser);
    }

    final VitamDocument<?> getDocument(LogbookParameters item) {
        if (item instanceof LogbookOperationParameters) {
            return new LogbookOperation((LogbookOperationParameters) item);
        } else if (item instanceof LogbookLifeCycleUnitParameters) {
            return new LogbookLifeCycleUnitInProcess((LogbookLifeCycleUnitParameters) item);
        } else {
            return new LogbookLifeCycleObjectGroupInProcess((LogbookLifeCycleObjectGroupParameters) item);
        }
    }

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
            VitamDocument<?> vitamDocument = getDocument(item);

            vitamDocument.append(TENANT_ID, ParameterHelper.getTenantParameter());
            vitamDocument.append(VERSION, 0);
            vitamDocument.append(LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(now()));

            collection.getCollection().insertOne(vitamDocument);

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

    private String createUnitaryUpdateForMaster(LogbookCollections collection, String mainLogbookDocumentId,
        String masterData)
        throws LogbookNotFoundException {
        try {
            Document oldValue =
                (Document) collection.getCollection().find(eq(LogbookDocument.ID, mainLogbookDocumentId)).first();
            // the test shouldn't be necessary, but...
            if (oldValue != null) {
                Object evdevObj = oldValue.get(LogbookMongoDbName.eventDetailData.getDbname());
                ObjectNode oldEvDetData = (ObjectNode) JsonHandler.getFromString("{}");
                String old;
                if (evdevObj != null) {
                    if (evdevObj instanceof String) {
                        old = (String) evdevObj;
                    } else {
                        old = JsonHandler.unprettyPrint(evdevObj);
                    }
                    JsonNode node = JsonHandler.getFromString(old);
                    if (node instanceof ObjectNode) {
                        oldEvDetData = (ObjectNode) node;
                    } else {
                        LOGGER.warn("Bad evDevData : {}", old);
                    }
                }
                if (ParametersChecker.isNotEmpty(masterData)) {
                    ObjectNode master = (ObjectNode) JsonHandler.getFromString(masterData);
                    oldEvDetData.setAll(master);
                    return JsonHandler.unprettyPrint(oldEvDetData);
                }
            } else {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + mainLogbookDocumentId);
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.warn("masterData is not parsable as a json. Analyse cancelled: " + masterData, e);
        }
        return null;
    }

    final void updateLogbookOperation(LogbookCollections collection, LogbookParameters item)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, item);
        @SuppressWarnings("rawtypes")
        final VitamDocument event = getDocumentForUpdate(item);
        try {
            // Save the _id content before removing it
            final String mainLogbookDocumentId = event.getId();

            List<Bson> listUpdates = new ArrayList<>();
            Bson mainUpdate = Updates.push(LogbookDocument.EVENTS, event);
            listUpdates.add(mainUpdate);
            // add 1 to version
            listUpdates.add(Updates.inc(LogbookDocument.VERSION, 1));
            String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
            // Update last persisted date
            listUpdates
                .add(Updates.set(LAST_PERSISTED_DATE, lastPersistedDate));

            if (item.getParameterValue(LogbookParameterName.masterData) != null &&
                !item.getParameterValue(LogbookParameterName.masterData).isEmpty()) {
                String evDetDataFinalValue = createUnitaryUpdateForMaster(collection, mainLogbookDocumentId,
                    item.getParameterValue(LogbookParameterName.masterData));
                if (evDetDataFinalValue != null && !evDetDataFinalValue.isEmpty()) {
                    Bson masterUpdate =
                        Updates.set(LogbookMongoDbName.eventDetailData.getDbname(), evDetDataFinalValue);
                    listUpdates.add(masterUpdate);
                }
            }
            // Remove _id and events fields
            removeDuplicatedInformation(event);

            final VitamDocument<?> result = (VitamDocument<?>) collection.getCollection().findOneAndUpdate(
                eq(LogbookDocument.ID, mainLogbookDocumentId),
                combine(listUpdates),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (result == null) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + mainLogbookDocumentId);
            }
            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                updateIntoElasticsearch(collection, result);
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
    private void updateLogbookLifeCycle(LogbookCollections inProcessCollection, String idLfc,
        LogbookParameters... parameters)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, inProcessCollection);
        if (parameters == null || parameters.length == 0) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }

        LogbookTypeProcess processMode = parameters[0].getTypeProcess();
        String documentId =
            (idLfc != null) ? idLfc : parameters[0].getParameterValue(LogbookParameterName.objectIdentifier);
        LogbookCollections prodCollection = fromInProcessToProdCollection(inProcessCollection);

        // 1- Check if it exists in Production Collection before proceeding to update
        LogbookLifeCycle lifeCycleInProd = null;

        try {
            if (prodCollection != null && documentId != null) {
                lifeCycleInProd = (LogbookLifeCycle) getLogbook(prodCollection, documentId);
            }
        } catch (LogbookNotFoundException e) {
            if (LogbookTypeProcess.UPDATE.equals(processMode)) {
                throw e;
            }
        }

        if (lifeCycleInProd != null) {

            // Check if there are other operations in process for the given item
            LogbookLifeCycle lifeCycleInProcess = null;
            try {
                lifeCycleInProcess = (LogbookLifeCycle) getLogbook(inProcessCollection, documentId);
            } catch (LogbookNotFoundException e) {
                LOGGER.info(INIT_UPDATE_LIFECYCLE, e);
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
        updateBulkLogbook(inProcessCollection, documentId, parameters);
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public void updateLogbookLifeCycle(LogbookCollections collection,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, collection);
        if (logbookLifeCycleParametersBulk == null || logbookLifeCycleParametersBulk.isEmpty()) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }

        String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());

        ArrayList<UpdateOneModel> updates = new ArrayList<>();

        for (LogbookLifeCycleParametersBulk lifeCycleParametersBulk : logbookLifeCycleParametersBulk) {

            String id = lifeCycleParametersBulk.getId();
            List<LogbookLifeCycleParameters> lifeCycleParameters = lifeCycleParametersBulk.getLifeCycleParameters();

            List<LogbookLifeCycle> events = lifeCycleParameters.stream()
                .map(event -> {
                    if (event.getParameterValue(LogbookParameterName.eventDateTime) == null) {
                        event.putParameterValue(LogbookParameterName.eventDateTime, now().toString());
                    }
                    LogbookLifeCycle logbookLifeCycle = new LogbookLifeCycle(event);
                    removeDuplicatedInformation(logbookLifeCycle);
                    logbookLifeCycle.append(LAST_PERSISTED_DATE, lastPersistedDate);
                    return logbookLifeCycle;
                }).collect(Collectors.toList());

            List<Bson> listMaster = new ArrayList<>();

            listMaster.add(Updates.pushEach(LogbookDocument.EVENTS, events));
            // add 1 to version
            listMaster.add(Updates.inc(LogbookDocument.VERSION, 1));
            // Update last persisted date
            listMaster.add(Updates.set(LAST_PERSISTED_DATE, lastPersistedDate));

            updates.add(new UpdateOneModel(eq("_id", id), combine(listMaster)));
            // modifie le master pour rajouter de nouvelles info au master.
            // checkCopyToMaster(collection, items[i]);
        }
        BulkWriteResult bulkWriteResult =
            collection.getCollection().bulkWrite(updates, new BulkWriteOptions().ordered(false));

        if (bulkWriteResult.getModifiedCount() != logbookLifeCycleParametersBulk.size()) {
            throw new VitamRuntimeException(
                String.format("Error while bulk update document count : %s != size : %s :",
                    bulkWriteResult.getModifiedCount(), logbookLifeCycleParametersBulk.size()));
        }
    }



    @Override
    public void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateLogbookOperation(LogbookCollections.OPERATION, operationItem);
    }

    @Override
    public void updateLogbookLifeCycleUnit(String idOperation, String idLfc,
        LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycleUnit(idOperation, idLfc, lifecycleItem, false);
    }


    @Override
    public void updateLogbookLifeCycleUnit(String idOperation, String idLfc,
        LogbookLifeCycleUnitParameters lifecycleItem, boolean commit)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }

        if (commit) {
            updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT, idLfc, lifecycleItem);
        } else {
            updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, idLfc, lifecycleItem);
        }
    }

    @Override
    public void updateLogbookLifeCycleObjectGroup(String idOperation, String idLfc,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycleObjectGroup(idOperation, idLfc, lifecycleItem, false);
    }

    @Override
    public void updateLogbookLifeCycleObjectGroup(String idOperation, String idLfc,
        LogbookLifeCycleObjectGroupParameters lifecycleItem,
        boolean commit)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }

        if (commit) {
            updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP, idLfc, lifecycleItem);
        } else {
            updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, idLfc, lifecycleItem);
        }
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

        final VitamDocument document = initializeVitamDocument(Lists.newArrayList(items));

        try {
            collection.getCollection().insertOne(document);
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
    final void updateBulkLogbook(final LogbookCollections collection, final String documentId,
        final LogbookParameters... items)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException(AT_LEAST_ONE_ITEM_IS_NEEDED);
        }
        final List<VitamDocument> events = new ArrayList<>(items.length);
        // Get the first event to preserve the _id field value
        final VitamDocument firstEvent = getDocumentForUpdate(items[0]);
        final String mainLogbookDocumentId = documentId != null ? documentId : firstEvent.getId();

        String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
        removeDuplicatedInformation(firstEvent);
        events.add(firstEvent);

        List<Bson> listMaster = new ArrayList<>();
        for (int i = 1; i < items.length; i++) {
            // Remove _id and events fields
            final VitamDocument currentEvent = getDocument(items[i]);
            removeDuplicatedInformation(currentEvent);
            listMaster.addAll(checkCopyToMaster(collection, items[i]));

            events.add(currentEvent);
        }
        if (!LogbookCollections.OPERATION.equals(collection)) {
            for (Document event : events) {
                event.append(LAST_PERSISTED_DATE, lastPersistedDate);
            }
        }

        listMaster.add(Updates.pushEach(LogbookDocument.EVENTS, events));
        // add 1 to version
        listMaster.add(Updates.inc(LogbookDocument.VERSION, 1));
        // Update last persisted date
        listMaster.add(Updates.set(LAST_PERSISTED_DATE, lastPersistedDate));
        try {
            final VitamDocument<?> result = (VitamDocument<?>) collection.getCollection().findOneAndUpdate(
                eq(LogbookDocument.ID, mainLogbookDocumentId),
                combine(listMaster),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (result == null) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + mainLogbookDocumentId);
            }
            // FIXME : to be refactor when other collection are indexed in ES
            if (LogbookCollections.OPERATION.equals(collection)) {
                updateIntoElasticsearch(collection, result);
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
        updateBulkLogbook(LogbookCollections.OPERATION, null, operationItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleUnit(LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, null, lifecycleItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleObjectGroup(LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        updateLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, null, lifecycleItems);
    }

    // Not check, test feature !
    @Override
    public void deleteCollection(LogbookCollections collection) throws DatabaseException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        final long count = collection.getCollection().countDocuments(Filters.eq(VitamDocument.TENANT_ID, tenantId));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            final DeleteResult result =
                collection.getCollection().deleteMany(new Document().append("_tenant", tenantId));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getDeletedCount());
            }
            if (LogbookCollections.OPERATION.equals(collection)) {
                esClient.deleteIndexByAlias(LogbookCollections.OPERATION.getName().toLowerCase(), tenantId);
                Map<String, String> map = esClient.addIndex(LogbookCollections.OPERATION, tenantId);
                if (map.isEmpty()) {
                    throw new RuntimeException(
                        "Index not created for the collection " + LogbookCollections.OPERATION.getName() +
                            " and tenant :" +
                            tenantId);
                }

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
                new LogbookLifeCycleUnit(BsonHelper.stringify(logbookLifeCycleUnitInProcess));
            String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
            // Update last persisted date
            logbookLifeCycleUnit.append(LAST_PERSISTED_DATE, lastPersistedDate);
            List<Document> events =
                (List<Document>) logbookLifeCycleUnit.get(LogbookDocument.EVENTS);
            for (Document event : events) {
                event.append(LAST_PERSISTED_DATE, lastPersistedDate);
            }

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
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroupInProcess)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {

        // Create ObjectGroup lifeCycle from LogbookLifeCycleObjectGroupInProcess instance
        ParametersChecker.checkParameter(ITEM_CANNOT_BE_NULL, logbookLifeCycleObjectGroupInProcess);

        try {
            LogbookLifeCycleObjectGroup logbookLifeCycleObjectGroup =
                new LogbookLifeCycleObjectGroup(BsonHelper.stringify(logbookLifeCycleObjectGroupInProcess));
            String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
            // Update last persisted date
            logbookLifeCycleObjectGroup.append(LAST_PERSISTED_DATE, lastPersistedDate);
            List<Document> events =
                (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS);
            for (Document event : events) {
                event.append(LAST_PERSISTED_DATE, lastPersistedDate);
            }

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

    /**
     * @param inProccessCollection collection of logbook in process
     * @param logbookLifeCycleInProd to create logbook lfc Unit/GroupObject
     * @throws LogbookDatabaseException if mongo execution error
     * @throws LogbookAlreadyExistsException if duplicated key in mongo
     */
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
                    new LogbookLifeCycleUnitInProcess(BsonHelper.stringify(logbookLifeCycleInProd));
            } else if (LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS.equals(inProccessCollection)) {
                logbookLifeCycleInProcess =
                    new LogbookLifeCycleObjectGroup(BsonHelper.stringify(logbookLifeCycleInProd));
            }

            if (logbookLifeCycleInProcess == null) {
                throw new LogbookDatabaseException(CREATION_ISSUE + TIMEOUT_OPERATION);
            }

            logbookLifeCycleInProcess.remove(LogbookDocument.EVENTS);
            logbookLifeCycleInProcess.append(LogbookLifeCycleMongoDbName.eventTypeProcess.getDbname(),
                LogbookTypeProcess.UPDATE.toString());
            logbookLifeCycleInProcess.append(LogbookDocument.EVENTS, Arrays.asList(new String[0]));
            // Update last persisted date
            logbookLifeCycleInProcess.append(LAST_PERSISTED_DATE,
                LocalDateUtil.getFormattedDateForMongo(now()));

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
        String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
        try {
            List<Bson> listUpdates = new ArrayList<>();

            List<Document> events =
                (List<Document>) logbookLifeCycleUnitInProcess.get(LogbookDocument.EVENTS);
            for (Document event : events) {
                event.append(LAST_PERSISTED_DATE, lastPersistedDate);
            }

            listUpdates.add(Updates.addEachToSet(LogbookDocument.EVENTS, events));
            listUpdates.add(Updates.inc(LogbookDocument.VERSION, 1));
            // Update last persisted date
            listUpdates.add(Updates.set(LAST_PERSISTED_DATE, lastPersistedDate));
            final Bson update = combine(listUpdates);

            // Make Update
            final UpdateResult result = LogbookCollections.LIFECYCLE_UNIT.getCollection().updateOne(
                eq(LogbookDocument.ID, logbookLifeCycleId), update);

            if (result.getMatchedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + logbookLifeCycleId);
            }

            // Do not delete the temporary lifeCycle when it is on an INGEST process
            List<Document> newEvents =
                (List<Document>) logbookLifeCycleUnitInProcess.get(LogbookDocument.EVENTS);
            if (newEvents != null && !newEvents.isEmpty()) {
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
        String lastPersistedDate = LocalDateUtil.getFormattedDateForMongo(now());
        try {

            List<Bson> listUpdates = new ArrayList<>();

            List<Document> events =
                (List<Document>) logbookLifeCycleObjectGrouptInProcess.get(LogbookDocument.EVENTS);
            for (Document event : events) {
                event.append(LAST_PERSISTED_DATE, lastPersistedDate);
            }

            listUpdates.add(Updates.addEachToSet(LogbookDocument.EVENTS, events));
            listUpdates.add(Updates.inc(LogbookDocument.VERSION, 1));
            // Update last persisted date
            listUpdates.add(Updates.set(LAST_PERSISTED_DATE, lastPersistedDate));
            final Bson update = combine(listUpdates);

            final UpdateResult result = LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().updateOne(
                eq(LogbookDocument.ID, logbookLifeCycleId), update);

            if (result.getMatchedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + logbookLifeCycleId);
            }

            // Do not delete the temporary lifeCycle when it is on an INGEST process
            List<VitamDocument> newEvents =
                (List<VitamDocument>) logbookLifeCycleObjectGrouptInProcess.get(LogbookDocument.EVENTS);
            if (newEvents != null && !newEvents.isEmpty()) {
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
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, unitId);
        return exists(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, unitId);
    }

    @Override
    public boolean existsLogbookLifeCycleObjectGroupInProcess(String objectGroupId)
        throws LogbookDatabaseException {
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
    private VitamMongoCursor findDocumentsElasticsearch(LogbookCollections collection,
        SelectParserSingle parser)
        throws InvalidParseOperationException, InvalidCreateOperationException, LogbookException, VitamDBException {
        Integer tenantId = HeaderIdHelper.getTenantId();
        final SelectToElasticsearch requestToEs = new SelectToElasticsearch(parser);
        DynamicParserTokens parserTokens =
            new DynamicParserTokens(collection.getVitamDescriptionResolver(), ontologyLoader.loadOntologies());
        List<SortBuilder> sorts =
            requestToEs.getFinalOrderBy(collection.getVitamCollection().isUseScore(), parserTokens);
        SearchResponse elasticSearchResponse =
            collection.getEsClient()
                .search(collection, tenantId, requestToEs.getNthQueries(0, new LogbookVarNameAdapter(), parserTokens),
                    null,
                    sorts, requestToEs.getFinalOffset(),
                    requestToEs.getFinalLimit());

        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits().value == 0) {
            return new VitamMongoCursor<>(new EmptyMongoCursor());
        }
        final Iterator<SearchHit> iterator = hits.iterator();
        // get document with Elasticsearch then create a new request to mongodb with unique object's attribute
        List<String> idsSorted = new ArrayList<>();
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            idsSorted.add(hit.getId());
        }

        // replace query with list of ids from es
        parser.getRequest().setQuery(new NopQuery());
        return new VitamMongoCursor(
            DbRequestHelper.selectMongoDbExecuteThroughFakeMongoCursor(collection.getVitamCollection(), parser,
                idsSorted, null), hits.getTotalHits().value, elasticSearchResponse.getScrollId());
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
        Integer tenantId = HeaderIdHelper.getTenantId();
        LOGGER.debug("insert to elasticsearch");
        String id = vitamDocument.getId();
        vitamDocument.remove(VitamDocument.ID);
        vitamDocument.remove(VitamDocument.SCORE);
        logbookTransformData.transformDataForElastic(vitamDocument);
        try {
            collection.getEsClient().indexEntry(collection.getName().toLowerCase(), tenantId, id, vitamDocument);
        } catch (DatabaseException e) {
            throw new LogbookExecutionException("Index to Elasticsearch has errors", e);
        }
    }

    /**
     * Update a document in ES
     *
     * @param collection the collection
     * @param existingDocument the document to update
     * @throws LogbookExecutionException if the ES update was in error
     * @throws LogbookNotFoundException if the document was not found in mongodb
     */
    private void updateIntoElasticsearch(LogbookCollections collection, VitamDocument<?> existingDocument)
        throws LogbookExecutionException, LogbookNotFoundException {
        Integer tenantId = HeaderIdHelper.getTenantId();
        LOGGER.debug("updateIntoElasticsearch");
        String id = (String) existingDocument.remove(VitamDocument.ID);
        existingDocument.remove(VitamDocument.SCORE);
        logbookTransformData.transformDataForElastic(existingDocument);

        collection.getEsClient().updateFullDocument(collection, tenantId, id, existingDocument);
    }


    private List<Bson> checkCopyToMaster(LogbookCollections collection, LogbookParameters item) {
        final String mainLogbookDocumentId = getDocumentForUpdate(item).getId();
        Document oldValue =
            (Document) collection.getCollection().find(eq(LogbookDocument.ID, mainLogbookDocumentId)).first();
        String masterData = item.getParameterValue(LogbookParameterName.masterData);
        List<Bson> updates = new ArrayList<>();
        if (ParametersChecker.isNotEmpty(masterData)) {
            try {
                JsonNode master = JsonHandler.getFromString(masterData);
                ObjectNode oldEvDetData = (ObjectNode) JsonHandler.getFromString("{}");
                Object evdevObj =
                    oldValue == null ? null : oldValue.get(LogbookMongoDbName.eventDetailData.getDbname());
                if (evdevObj != null) {
                    String old;
                    if (evdevObj instanceof String) {
                        old = (String) evdevObj;
                    } else {
                        old = JsonHandler.unprettyPrint(evdevObj);
                    }
                    try {
                        JsonNode node = JsonHandler.getFromString(old);
                        if (node instanceof ObjectNode) {
                            oldEvDetData = (ObjectNode) node;
                        } else {
                            LOGGER.warn("Bad evDevData : {}", old);
                        }
                    } catch (InvalidParseOperationException e) {
                        LOGGER.warn("Bad evDevData : {}", old, e);
                    }
                }
                boolean updateEvDevData = false;
                Iterator<String> fieldNames = master.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    String fieldValue = master.get(fieldName).asText();
                    String mongoDbName =
                        LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.valueOf(fieldName)).getDbname();
                    if (mongoDbName.equals(LogbookMongoDbName.eventDetailData.getDbname())) {
                        JsonNode masterNode = ((ObjectNode) master).get(fieldName);
                        if (masterNode != null) {
                            String masterField = masterNode.asText();
                            oldEvDetData.setAll((ObjectNode) JsonHandler.getFromString(masterField));
                            updateEvDevData = true;
                        }
                    } else {
                        updates.add(Updates.set(mongoDbName, fieldValue));
                    }
                }
                if (updateEvDevData) {
                    String fieldValue = JsonHandler.writeAsString(oldEvDetData);
                    updates.add(Updates.set(LogbookMongoDbName.eventDetailData.getDbname(), fieldValue));
                }
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("masterData is not parsable as a json. Analyse cancelled: " + masterData, e);
            }
        }
        return updates;
    }

    private void removeDuplicatedInformation(VitamDocument document) {
        document.remove(LogbookDocument.EVENTS);
        document.remove(LogbookDocument.ID);
        document.remove(TENANT_ID);
        document.remove(LogbookDocument.LAST_PERSISTED_DATE);
        if (document.get(LogbookMongoDbName.rightsStatementIdentifier.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.rightsStatementIdentifier.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.rightsStatementIdentifier.getDbname());
        }

        document.remove(LogbookMongoDbName.agentIdentifierApplication.getDbname());
        document.remove(LogbookMongoDbName.agentIdentifierApplicationSession.getDbname());

        if (document.get(LogbookMongoDbName.agIdExt.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.agIdExt.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.agIdExt.getDbname());
        }
        if (document.get(LogbookMongoDbName.objectIdentifier.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.objectIdentifier.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.objectIdentifier.getDbname());
        }

        if (document.get(LogbookMongoDbName.eventIdentifierRequest.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.eventIdentifierRequest.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.eventIdentifierRequest.getDbname());
        }

        if (document.get(LogbookMongoDbName.objectIdentifierRequest.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.objectIdentifierRequest.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.objectIdentifierRequest.getDbname());
        }

        if (document.get(LogbookMongoDbName.objectIdentifierIncome.getDbname()) == null ||
            ((String) document.get(LogbookMongoDbName.objectIdentifierIncome.getDbname())).isEmpty()) {
            document.remove(LogbookMongoDbName.objectIdentifierIncome.getDbname());
        }
    }

    @Override
    public void bulkInsert(LogbookCollections lifecycleUnit,
        List<? extends LogbookLifeCycleModel> logbookLifeCycleModels)
        throws DatabaseException {
        List<InsertOneModel<VitamDocument>> vitamDocuments = logbookLifeCycleModels
            .stream()
            .map(item -> {
                List<? extends LogbookLifeCycleParameters> items =
                    new ArrayList<>(item.getLogbookLifeCycleParameters());
                final VitamDocument document = initializeVitamDocument(items);

                return new InsertOneModel<>(document);
            }).collect(Collectors.toList());

        if (!vitamDocuments.isEmpty()) {
            BulkWriteOptions options = new BulkWriteOptions();
            options.ordered(false);
            BulkWriteResult bulkWriteResult = lifecycleUnit.getCollection().bulkWrite(vitamDocuments, options);

            int count = bulkWriteResult.getInsertedCount();
            int size = vitamDocuments.size();

            if (count != size) {
                LOGGER
                    .error(String.format("Error while bulk save document count : %s != size : %s :", count, size));
                throw new DatabaseException(
                    String.format("Error while bulk save document count : %s != size : %s :", count, size));
            }

        }
    }

    /**
     * create a Vitam Document for logbook lifecycle, add tenantId and version field
     *
     * @param items
     * @return
     */
    private VitamDocument initializeVitamDocument(List<? extends LogbookParameters> items) {
        int i = 0;
        final VitamDocument document = getDocument(items.get(i));
        final List<VitamDocument> events1 = new ArrayList<>(items.size() - 1);
        for (i = 1; i < items.size(); i++) {
            final VitamDocument currentEvent = getDocumentForUpdate(items.get(i));
            removeDuplicatedInformation(currentEvent);
            events1.add(currentEvent);
        }
        document.append(LogbookDocument.EVENTS, events1);
        document.append(TENANT_ID, ParameterHelper.getTenantParameter());
        document.append(VERSION, 0);
        document.append(LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(now()));
        return document;
    }

}
