package fr.gouv.vitam.logbook.common.server.database.collections;

import static fr.gouv.vitam.builder.request.construct.QueryHelper.exists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.builder.singlerequest.Select;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;

/**
 * MongoDbAccessFactory Test
 */
public class MongoDbAccessFactoryTest {

    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12346;
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccess =
            MongoDbAccessFactory.create(
                new DbConfigurationImpl(DATABASE_HOST, DATABASE_PORT,
                    "vitam-test"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
    }

    @Test
    public void testStructure() {
        assertEquals(LogbookMongoDbName.agentIdentifier,
            LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.agentIdentifier));
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            assertEquals(name,
                LogbookMongoDbName.getFromDbname(name.getDbname()));
        }
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookOperation operation = new LogbookOperation(parameters);
        final LogbookOperation operation2 = new LogbookOperation(operation.toJson());
        assertEquals(operation.getId(), operation2.getId());
        assertEquals(operation.getTenantId(), operation2.getTenantId());
        assertNotNull(MongoDbAccessImpl.bsonToString(operation, true));
        assertNotNull(MongoDbAccessImpl.bsonToString(operation, false));
        assertTrue(MongoDbAccessImpl.bsonToString(null, false).isEmpty());
    }

    @Test
    public void testCreateFn() throws VitamException, InvalidCreateOperationException {
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((MongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final String status = mongoDbAccess.toString();
        assertTrue(status.indexOf("LogbookOperation") > 0);
        ((MongoDbAccessImpl) mongoDbAccess).flushOnDisk();

        MongoDbAccessImpl.ensureIndex();
        assertEquals(status, mongoDbAccess.toString());
        MongoDbAccessImpl.removeIndexBeforeImport();;
        assertEquals(status, mongoDbAccess.toString());
        MongoDbAccessImpl.resetIndexAfterImport();;
        assertEquals(status, mongoDbAccess.toString());
        assertEquals(0, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(0, mongoDbAccess.getLogbookOperationSize());
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        try (MongoCursor<LogbookOperation> cursorOperation =
            mongoDbAccess.getLogbookOperations(select.getFinalSelect().toString())) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookOperation> cursorOperation =
            mongoDbAccess.getLogbookOperations(select.getFinalSelect())) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursorOperation =
            mongoDbAccess.getLogbookLifeCycles(select.getFinalSelect().toString())) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursorOperation =
            mongoDbAccess.getLogbookLifeCycles(select.getFinalSelect())) {
            assertFalse(cursorOperation.hasNext());
        }
        final LogbookOperationParameters params =
            LogbookParametersFactory.newLogbookOperationParameters();
        params.getMapParameters().put(LogbookParameterName.objectIdentifier,
            "");
        assertFalse(mongoDbAccess.existsLogbookLifeCycle(params));
        assertFalse(mongoDbAccess.existsLogbookLifeCycle(
            LogbookParametersFactory.newLogbookOperationParameters()
                .putParameterValue(LogbookParameterName.objectIdentifier,
                    GUIDFactory.newGUID().toString())));
        assertFalse(mongoDbAccess.existsLogbookOperation(
            LogbookParametersFactory.newLogbookOperationParameters()
                .putParameterValue(LogbookParameterName.eventIdentifierProcess,
                    GUIDFactory.newGUID().toString())));
        try {
            mongoDbAccess.getLogbookLifeCycle(GUIDFactory.newGUID().getId());
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.getLogbookOperation(GUIDFactory.newGUID().getId());
            fail("Should throw an exception");
        } catch (final VitamException e) {}
    }

    @Test
    public void testFunctionalOperation() throws VitamException {
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((MongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleSize();
        final long nbo = mongoDbAccess.getLogbookOperationSize();

        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        final LogbookOperationParameters parameters2 = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookOperationParameters parametersWrong = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateLogbookOperation(parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.createLogbookOperation(parameters);
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo + 1, mongoDbAccess.getLogbookOperationSize());
        try {
            mongoDbAccess.createLogbookOperation(parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.createLogbookOperation(parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.updateLogbookOperation(parameters2);
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo + 1, mongoDbAccess.getLogbookOperationSize());
        final String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        assertNotNull(mongoDbAccess.getLogbookOperation(eip));
        try {
            mongoDbAccess.updateLogbookOperation(parametersWrong);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        mongoDbAccess.updateBulkLogbookOperation(parameters, parameters2);
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo + 1, mongoDbAccess.getLogbookOperationSize());

        assertTrue(mongoDbAccess.existsLogbookOperation(parameters));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: { _id: \"" + eip + "\" } }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertNotNull(operation.toString());
            assertNotNull(operation.toStringDirect());
            assertEquals(2, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertEquals(2, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        node = JsonHandler.getFromString("{ $query: { $eq: { _id: \"" + eip + "\"} }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertEquals(2, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertEquals(2, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        final LogbookOperation operation2 = mongoDbAccess.getLogbookOperation(eip);
        assertNotNull(operation2);
        assertEquals(4, operation2.getOperations(true).size());
        assertEquals(2, operation2.getOperations(false).size());

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookOperations(node.toString());
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.getLogbookOperations(node);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateBulkLogbookOperation(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.updateBulkLogbookOperation();
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            mongoDbAccess.updateBulkLogbookOperation(new LogbookOperationParameters[0]);
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        mongoDbAccess.createBulkLogbookOperation(parameters, parameters2);
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo + 2, mongoDbAccess.getLogbookOperationSize());
        try {
            mongoDbAccess.createBulkLogbookOperation(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertFalse(cursor.hasNext());
        }
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertTrue(cursor.hasNext());
        }
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertTrue(cursor.hasNext());
        }
    }

    @Test
    public void testFunctionalLifeCycle() throws VitamException {
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((MongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleSize();
        final long nbo = mongoDbAccess.getLogbookOperationSize();
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        final LogbookOperationParameters parameters2 = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookOperationParameters parametersWrong = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateLogbookLifeCycle(parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.createLogbookLifeCycle(parameters);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo, mongoDbAccess.getLogbookOperationSize());
        final String oi = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        assertNotNull(mongoDbAccess.getLogbookLifeCycle(oi));
        try {
            mongoDbAccess.createLogbookLifeCycle(parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.createLogbookLifeCycle(parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.updateLogbookLifeCycle(parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo, mongoDbAccess.getLogbookOperationSize());
        try {
            mongoDbAccess.updateLogbookLifeCycle(parametersWrong);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        mongoDbAccess.updateBulkLogbookLifeCycle(parameters, parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo, mongoDbAccess.getLogbookOperationSize());

        assertTrue(mongoDbAccess.existsLogbookLifeCycle(parameters));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: {_id: \"" + oi + "\"} }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        final LogbookLifeCycle lifeCycle = mongoDbAccess.getLogbookLifeCycle(oi);
        assertNotNull(lifeCycle);
        assertEquals(4, lifeCycle.getLifeCycles(true).size());
        assertEquals(2, lifeCycle.getLifeCycles(false).size());

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookLifeCycles(node.toString());
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.getLogbookLifeCycles(node);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(0).getId());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateBulkLogbookLifeCycle(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycle();;
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycle(new LogbookParameters[0]);;
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        mongoDbAccess.createBulkLogbookLifeCycle(parameters, parameters2);
        assertEquals(nbl + 2, mongoDbAccess.getLogbookLifeCyleSize());
        assertEquals(nbo, mongoDbAccess.getLogbookOperationSize());
        try {
            mongoDbAccess.createBulkLogbookLifeCycle(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1, " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1 " + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertTrue(cursor.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycles(node.toString())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertTrue(cursor.hasNext());
        }
    }
}
