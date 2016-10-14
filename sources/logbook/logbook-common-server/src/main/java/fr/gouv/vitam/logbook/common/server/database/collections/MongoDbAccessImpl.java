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

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

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
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectToMongoDb;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.VitamDocumentCodec;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDb Access implementation base class
 *
 */
public final class MongoDbAccessImpl implements MongoDbAccess {
    private static final String AT_LEAST_ONE_ITEM_IS_NEEDED = "At least one item is needed";
    private static final String LOGBOOK_LIFE_CYCLE_NOT_FOUND = "LogbookLifeCycle not found";
    private static final String SELECT_ISSUE = "Select issue";
    private static final String ELEMENT_ALREADY_EXISTS = " (element already exists)";
    private static final String TIMEOUT_OPERATION = " (timeout operation)";
    private static final String EXISTS_ISSUE = "Exists issue";
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
    private static final String ROLLBACK_ISSUE = "Rollback issue";
    /**
     * Quick projection for ID Only
     */
    static final BasicDBObject ID_PROJECTION = new BasicDBObject(LogbookDocument.ID, 1);
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    static final int LAST_EVENT_SLICE = -1;
    static final int TWO_LAST_EVENTS_SLICE = -2;

    static {
        DEFAULT_SLICE.putObject(LogbookDocument.EVENTS).put(SLICE, LAST_EVENT_SLICE);
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
        LogbookCollections.LIFECYCLE_UNIT.initialize(mongoDatabase, recreate);
        LogbookCollections.LIFECYCLE_OBJECTGROUP.initialize(mongoDatabase, recreate);
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
        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(operationCodec, lifecycleUnitCodec, lifecycleObjectGroupCodec));
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
        LogbookLifeCycle.addIndexes();
    }

    /**
     * Remove temporarily the MongoDB Index (import optimization?)
     */
    static final void removeIndexBeforeImport() {
        LogbookOperation.dropIndexes();
        LogbookLifeCycle.dropIndexes();
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
    public final long getLogbookLifeCyleUnitSize() {
        return LogbookCollections.LIFECYCLE_UNIT.getCollection().count();
    }

    @Override
    public final long getLogbookLifeCyleObjectGroupSize() throws LogbookDatabaseException, LogbookNotFoundException {
        return LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().count();
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
    public MongoCursor<LogbookOperation> getLogbookOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);

        // Temporary fix as the obIdIn (MessageIdentifier in the SEDA manifest) is only available on the 2 to last
        // Logbook operation event . Must be removed when the processing will be reworked
        final ObjectNode operationSlice = JsonHandler.createObjectNode();
        operationSlice.putObject(LogbookDocument.EVENTS).put(SLICE, TWO_LAST_EVENTS_SLICE);
        return select(LogbookCollections.OPERATION, select, operationSlice);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycleUnit> getLogbookLifeCycleUnits(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.LIFECYCLE_UNIT, select);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifeCycleObjectGroups(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter(SELECT_PARAMETER_IS_NULL, select);
        return select(LogbookCollections.LIFECYCLE_OBJECTGROUP, select);
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
    public boolean existsLogbookLifeCycleUnit(String unitId) throws LogbookDatabaseException, LogbookNotFoundException {
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
    private final MongoCursor select(final LogbookCollections collection, final JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return select(collection, select, DEFAULT_SLICE);
    }

    @SuppressWarnings("rawtypes")
    private final MongoCursor select(final LogbookCollections collection, final JsonNode select, final ObjectNode slice)
        throws LogbookDatabaseException, LogbookNotFoundException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(select);
            parser.addProjection(slice, DEFAULT_ALLKEYS);
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
    private MongoCursor selectExecute(final LogbookCollections collection, SelectParserSingle parser)
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

    @SuppressWarnings("rawtypes")
    final VitamDocument getDocument(LogbookParameters item) {
        if (item instanceof LogbookOperationParameters) {
            return new LogbookOperation((LogbookOperationParameters) item);
        } else if (item instanceof LogbookLifeCycleUnitParameters) {
            return new LogbookLifeCycleUnit((LogbookLifeCycleUnitParameters) item);
        } else {
            return new LogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters) item);
        }
    }

    @SuppressWarnings("rawtypes")
    final VitamDocument getDocumentForUpdate(LogbookParameters item) {
        if (item instanceof LogbookOperationParameters) {
            return new LogbookOperation((LogbookOperationParameters) item, true);
        } else if (item instanceof LogbookLifeCycleUnitParameters) {
            return new LogbookLifeCycleUnit((LogbookLifeCycleUnitParameters) item);
        } else {
            return new LogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters) item);
        }
    }

    @SuppressWarnings("unchecked")
    final void createLogbook(LogbookCollections collection, LogbookParameters item)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("Item cannot be null", item);
        try {
            collection.getCollection().insertOne(getDocument(item));
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
        createLogbook(LogbookCollections.LIFECYCLE_UNIT, lifecycleItem);
    }

    @Override
    public void createLogbookLifeCycleObjectGroup(String idOperation,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to create the LifeCycle");
        }
        createLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP, lifecycleItem);
    }

    final void updateLogbook(LogbookCollections collection, LogbookParameters item)
        throws LogbookDatabaseException, LogbookNotFoundException {
        ParametersChecker.checkParameter("Item cannot be null", item);
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
    public void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateLogbook(LogbookCollections.OPERATION, operationItem);
    }

    @Override
    public void updateLogbookLifeCycleUnit(String idOperation, LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }
        updateLogbook(LogbookCollections.LIFECYCLE_UNIT, lifecycleItem);
    }

    @Override
    public void updateLogbookLifeCycleObjectGroup(String idOperation,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        if (!lifecycleItem.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            throw new IllegalArgumentException("Wrong IdOperation set to update the LifeCycle");
        }
        updateLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP, lifecycleItem);
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
        rollbackLogbookLifeCycle(LogbookCollections.LIFECYCLE_UNIT, idOperation, lifecycleItem);
    }

    @Override
    public void rollbackLogbookLifeCycleObjectGroup(String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException {
        rollbackLogbookLifeCycle(LogbookCollections.LIFECYCLE_OBJECTGROUP, idOperation, lifecycleItem);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    final void createBulkLogbook(LogbookCollections collection, final LogbookParameters item,
        final LogbookParameters... items)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("Item cannot be null", item);
        final VitamDocument document = getDocument(item);
        if (items != null && items.length > 0) {
            final List<VitamDocument> events = new ArrayList<>(items.length);
            for (final LogbookParameters item2 : items) {
                final VitamDocument currentEvent = getDocumentForUpdate(item2);
                currentEvent.remove(LogbookDocument.EVENTS);
                currentEvent.remove(LogbookDocument.ID);
                events.add(currentEvent);
            }
            document.append(LogbookDocument.EVENTS, events);
        }
        try {
            collection.getCollection().insertOne(document);
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

    @Override
    public final void createBulkLogbookOperation(final LogbookOperationParameters operationItem,
        final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.OPERATION, operationItem, operationItems);
    }

    @Override
    public final void createBulkLogbookLifeCycleUnit(final LogbookLifeCycleUnitParameters lifecycleItem,
        final LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.LIFECYCLE_UNIT, lifecycleItem, lifecycleItems);
    }

    @Override
    public final void createBulkLogbookLifeCycleObjectGroup(final LogbookLifeCycleObjectGroupParameters lifecycleItem,
        final LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        createBulkLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP, lifecycleItem, lifecycleItems);
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
    public final void updateBulkLogbookOperation(final LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateBulkLogbook(LogbookCollections.OPERATION, operationItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleUnit(LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateBulkLogbook(LogbookCollections.LIFECYCLE_UNIT, lifecycleItems);
    }

    @Override
    public void updateBulkLogbookLifeCycleObjectGroup(LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException {
        updateBulkLogbook(LogbookCollections.LIFECYCLE_OBJECTGROUP, lifecycleItems);
    }
}
