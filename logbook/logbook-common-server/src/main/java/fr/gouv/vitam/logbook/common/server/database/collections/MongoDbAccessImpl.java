/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.logbook.common.server.database.collections;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.hashed;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.core.database.collections.VitamDocumentCodec;
import fr.gouv.vitam.core.database.collections.translator.mongodb.QueryToMongodb;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.database.collections.request.SelectParser;
import fr.gouv.vitam.logbook.common.server.database.collections.request.SelectToMongoDb;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDb Access implementation base class
 *
 */
public final class MongoDbAccessImpl implements MongoDbAccess {
    /**
     * SLICE command to optimize listing
     */
    public static final String SLICE = "$slice";
    private static final String UPDATE_NOT_FOUND_ITEM = "Update not found item: ";
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MongoDbAccessImpl.class);
    private static final String SELECT_PARAMETER_IS_NULL = "select parameter is null";
    private static final String LIFECYCLE_ITEM = "lifecycleItem";
    private static final String OPERATION_ITEM = "operationItem";
    private static final String CREATION_ISSUE = "Creation issue";
    private static final String UPDATE_ISSUE = "Update issue";
    /**
     * Quick projection for ID Only
     */
    static final BasicDBObject ID_PROJECTION = new BasicDBObject(LogbookDocument.ID, 1);
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    static {
        DEFAULT_SLICE.putObject(LogbookDocument.EVENTS).put(SLICE, -1);
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            DEFAULT_ALLKEYS.put(name.getDbname(), 1);
        }
    }

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final MongoDatabase mongoAdmin;

    /**
     *
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     * @throws IllegalArgumentException if mongoClient or dbname is null
     */
    MongoDbAccessImpl(MongoClient mongoClient, final String dbname, final boolean recreate) {
        ParametersChecker.checkParameter("Parameter of MongoDbAccess", mongoClient, dbname);
        this.mongoClient = mongoClient;
        mongoDatabase = mongoClient.getDatabase(dbname);
        mongoAdmin = mongoClient.getDatabase("admin");
        LogbookCollections.OPERATION.initialize(mongoDatabase, recreate);
        LogbookCollections.LIFECYCLE.initialize(mongoDatabase, recreate);
    }

    /**
     *
     * @return The MongoCLientOptions to apply to MongoClient
     */
    static final MongoClientOptions getMongoClientOptions() {
        final VitamDocumentCodec<LogbookOperation> operationCodec = new VitamDocumentCodec<>(LogbookOperation.class);
        final VitamDocumentCodec<LogbookLifeCycle> lifecycleCodec = new VitamDocumentCodec<>(LogbookLifeCycle.class);
        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(operationCodec, lifecycleCodec));
        return MongoClientOptions.builder().codecRegistry(codecRegistry).build();
    }

    /**
     * Close database access
     */
    @Override
    public final void close() {
        mongoClient.close();
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
        final MongoIterable<String> collectionNames = mongoDatabase.listCollectionNames();
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

    @Override
    public final long getLogbookOperationSize() {
        return LogbookCollections.OPERATION.getCollection().count();
    }

    @Override
    public final long getLogbookLifeCyleSize() {
        return LogbookCollections.LIFECYCLE.getCollection().count();
    }

    /**
     * Force flush on disk (MongoDB): should not be used
     */
    final void flushOnDisk() {
        mongoAdmin.runCommand(new BasicDBObject("fsync", 1).append("async", true)
            .append("lock", false));
    }

    /**
     * @return the mongoDatabase
     */
    final MongoDatabase getMongoDatabase() {
        return mongoDatabase;
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
    public MongoCursor<LogbookOperation> getLogbookOperations(String select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.OPERATION, select);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycle> getLogbookLifeCycles(String select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.LIFECYCLE, select);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookOperation> getLogbookOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.OPERATION, select);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycle> getLogbookLifeCycles(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.LIFECYCLE, select);
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
        if (id == null || id.length() == 0) {
            return false;
        }
        try {
            return collection.getCollection().find(eq(LogbookDocument.ID,
                id)).projection(ID_PROJECTION).first() != null;
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException("Exists issue" + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        "Exists issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        "Exists issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    final String getOperationId(final LogbookOperationParameters operationItem) {
        return operationItem.getMapParameters()
            .get(LogbookOperation.getIdName().getLogbookParameterName());
    }

    @Override
    public final boolean existsLogbookOperation(final LogbookOperationParameters operationItem)
        throws LogbookDatabaseException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationItem);
        return exists(LogbookCollections.OPERATION, getOperationId(operationItem));
    }

    final String getLifeCycleId(final LogbookParameters lifecycleItem) {
        return lifecycleItem.getMapParameters()
            .get(LogbookLifeCycle.getIdName().getLogbookParameterName());
    }

    @Override
    public final boolean existsLogbookLifeCycle(final LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, lifecycleItem);
        return exists(LogbookCollections.LIFECYCLE, getLifeCycleId(lifecycleItem));
    }


    @Override
    public LogbookOperation getLogbookOperation(String eventIdentifierProcess)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(OPERATION_ITEM, eventIdentifierProcess);
        LogbookOperation operation = null;
        try {
            operation = LogbookCollections.getOperationCollection()
                .find(eq(LogbookDocument.ID, eventIdentifierProcess)).first();
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException("Select issue" + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        "Select issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        "Select issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
        if (operation == null) {
            throw new LogbookNotFoundException("LogbookOperation not found");
        }
        return operation;
    }

    @Override
    public LogbookLifeCycle getLogbookLifeCycle(String objectIdentifier)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, objectIdentifier);
        LogbookLifeCycle lifecycle = null;
        try {
            lifecycle = LogbookCollections.getLifeCycleCollection()
                .find(eq(LogbookDocument.ID, objectIdentifier)).first();
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException("Select issue" + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        "Select issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        "Select issue" + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
        if (lifecycle == null) {
            throw new LogbookNotFoundException("LogbookLifeCycle not found");
        }
        return lifecycle;
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
    private final MongoCursor select(final LogbookCollections collection, final String select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        try {
            final SelectParser parser = new SelectParser(new LogbookVarNameAdapter());
            parser.parse(select);
            parser.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
            return selectExecute(collection, parser);
        } catch (final InvalidParseOperationException e) {
            throw new LogbookDatabaseException(e);
        }
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
    private final MongoCursor select(final LogbookCollections collection, final JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        try {
            final SelectParser parser = new SelectParser(new LogbookVarNameAdapter());
            parser.parse(select);
            parser.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
            return selectExecute(collection, parser);
        } catch (final InvalidParseOperationException e) {
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
    private MongoCursor selectExecute(final LogbookCollections collection, SelectParser parser)
        throws InvalidParseOperationException {
        final SelectToMongoDb selectToMongoDb = new SelectToMongoDb(parser);
        final Bson condition = QueryToMongodb.getCommand(selectToMongoDb.getSelect().getQuery());
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

    private static final JsonWriterSettings JWS = new JsonWriterSettings(true);

    /**
     * Utility to get Bson to String (Json format)
     *
     * @param bson
     * @param indent if True, output will be indented.
     * @return the String Json representation of the Bson
     */
    public static String bsonToString(Bson bson, boolean indent) {
        if (bson == null) {
            return "";
        }
        if (indent) {
            return bson.toBsonDocument(BsonDocument.class,
                MongoClient.getDefaultCodecRegistry()).toJson(JWS);
        } else {
            return bson.toBsonDocument(BsonDocument.class,
                MongoClient.getDefaultCodecRegistry()).toJson();
        }
    }

    @Override
    public void createLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationItem);
        try {
            LogbookCollections.getOperationCollection().insertOne(new LogbookOperation(operationItem));
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + " (element already exists)", e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public void createLogbookLifeCycle(LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, lifecycleItem);
        try {
            LogbookCollections.getLifeCycleCollection().insertOne(new LogbookLifeCycle(lifecycleItem));
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + " (element already exists)", e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationItem);
        final LogbookOperation operation = new LogbookOperation(operationItem);
        try {
            final UpdateResult result = LogbookCollections.getOperationCollection().updateOne(
                eq(LogbookDocument.ID, operation.getId()),
                Updates.push(LogbookDocument.EVENTS, operation));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + operation.getId());
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

    @Override
    public void updateLogbookLifeCycle(LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, lifecycleItem);
        final LogbookLifeCycle lifeCycle = new LogbookLifeCycle(lifecycleItem);
        try {
            final UpdateResult result = LogbookCollections.getLifeCycleCollection().updateOne(
                eq(LogbookDocument.ID, lifeCycle.getId()),
                Updates.push(LogbookDocument.EVENTS, lifeCycle));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + lifeCycle.getId());
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

    @Override
    public final void createBulkLogbookOperation(final LogbookOperationParameters operationItem,
        final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(OPERATION_ITEM, operationItem);
        final LogbookOperation operation = new LogbookOperation(operationItem);
        if (operationItems != null && operationItems.length > 0) {
            final List<LogbookOperation> operations = new ArrayList<>(operationItems.length);
            for (final LogbookOperationParameters item : operationItems) {
                operations.add(new LogbookOperation(item));
            }
            operation.append(LogbookDocument.EVENTS, operations);
        }
        try {
            LogbookCollections.getOperationCollection().insertOne(operation);
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + " (element already exists)", e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public final void createBulkLogbookLifeCycle(final LogbookParameters lifecycleItem,
        final LogbookParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter(LIFECYCLE_ITEM, lifecycleItem);
        final LogbookLifeCycle lifeCycle = new LogbookLifeCycle(lifecycleItem);
        if (lifecycleItems != null && lifecycleItems.length > 0) {
            final List<LogbookLifeCycle> lifeCycles = new ArrayList<>(lifecycleItems.length);
            for (final LogbookParameters item : lifecycleItems) {
                lifeCycles.add(new LogbookLifeCycle(item));
            }
            lifeCycle.append(LogbookDocument.EVENTS, lifeCycles);
        }
        try {
            LogbookCollections.getLifeCycleCollection().insertOne(lifeCycle);
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case DUPLICATE_KEY:
                    throw new LogbookAlreadyExistsException(CREATION_ISSUE + " (element already exists)", e);
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(CREATION_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        CREATION_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() +
                            ")",
                        e);
            }
        }
    }

    @Override
    public final void updateBulkLogbookOperation(final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (operationItems == null || operationItems.length == 0) {
            throw new IllegalArgumentException("At least one item is needed");
        }
        final List<LogbookOperation> operations = new ArrayList<>(operationItems.length);
        for (final LogbookOperationParameters item : operationItems) {
            operations.add(new LogbookOperation(item));
        }
        try {
            final LogbookOperation operation = operations.get(0);
            final UpdateResult result = LogbookCollections.getOperationCollection().updateOne(
                eq(LogbookDocument.ID, operation.getId()),
                Updates.pushEach(LogbookDocument.EVENTS, operations));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + operation.getId());
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

    @Override
    public final void updateBulkLogbookLifeCycle(final LogbookParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (lifecycleItems == null || lifecycleItems.length == 0) {
            throw new IllegalArgumentException("At least one item is needed");
        }
        final List<LogbookLifeCycle> lifecycles = new ArrayList<>(lifecycleItems.length);
        for (final LogbookParameters item : lifecycleItems) {
            lifecycles.add(new LogbookLifeCycle(item));
        }
        try {
            final LogbookLifeCycle lifecycle = lifecycles.get(0);
            final UpdateResult result = LogbookCollections.getLifeCycleCollection().updateOne(
                eq(LogbookDocument.ID, lifecycle.getId()),
                Updates.pushEach(LogbookDocument.EVENTS, lifecycles));
            if (result.getModifiedCount() != 1) {
                throw new LogbookNotFoundException(UPDATE_NOT_FOUND_ITEM + lifecycle.getId());
            }
        } catch (final MongoException e) {
            switch (getErrorCategory(e)) {
                case EXECUTION_TIMEOUT:
                    throw new LogbookDatabaseException(UPDATE_ISSUE + " (timeout operation)", e);
                case UNCATEGORIZED:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
                default:
                    throw new LogbookDatabaseException(
                        UPDATE_ISSUE + " (" + e.getClass().getName() + " " + e.getMessage() + ": " + e.getCode() + ")",
                        e);
            }
        }
    }

}
