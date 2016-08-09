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

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;

/**
 * MongoDbAccessFactory Test
 */
public class MongoDbAccessFactoryTest {

    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoDbAccess =
            MongoDbAccessFactory.create(
                new DbConfigurationImpl(DATABASE_HOST, port,
                    "vitam-test"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
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
        assertNotNull(MongoDbHelper.bsonToString(operation, true));
        assertNotNull(MongoDbHelper.bsonToString(operation, false));
        assertTrue(MongoDbHelper.bsonToString(null, false).isEmpty());

        assertTrue(LogbookOperation.getIdName().equals(LogbookMongoDbName.eventIdentifierProcess));
        assertTrue(LogbookOperation.getIdParameterName().equals(LogbookParameterName.eventIdentifierProcess));
        final LogbookLifeCycleObjectGroup lifeCycleObjectGroup = new LogbookLifeCycleObjectGroup(operation.toJson());
        assertTrue(LogbookLifeCycle.getIdName().equals(LogbookMongoDbName.objectIdentifier));
        assertTrue(LogbookLifeCycle.getIdParameterName().equals(LogbookParameterName.objectIdentifier));
        final LogbookLifeCycleUnit lifeCycleUnit = new LogbookLifeCycleUnit(operation.toJson());
        assertTrue(LogbookLifeCycle.getIdName().equals(LogbookMongoDbName.objectIdentifier));
        assertTrue(LogbookLifeCycle.getIdParameterName().equals(LogbookParameterName.objectIdentifier));
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
        assertEquals(0, mongoDbAccess.getLogbookLifeCyleUnitSize());
        assertEquals(0, mongoDbAccess.getLogbookOperationSize());
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        try (MongoCursor<LogbookOperation> cursorOperation =
            mongoDbAccess.getLogbookOperations(select.getFinalSelect())) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycleUnit> cursorOperation =
            mongoDbAccess.getLogbookLifeCycleUnits(select.getFinalSelect())) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursorOperation =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(select.getFinalSelect())) {
            assertFalse(cursorOperation.hasNext());
        }
        final LogbookOperationParameters params =
            LogbookParametersFactory.newLogbookOperationParameters();
        params.getMapParameters().put(LogbookParameterName.objectIdentifier,
            "");
        assertFalse(mongoDbAccess.existsLogbookOperation("a"));
        assertFalse(mongoDbAccess.existsLogbookOperation(GUIDFactory.newGUID().toString()));
        assertFalse(mongoDbAccess.existsLogbookLifeCycleUnit("a"));
        assertFalse(mongoDbAccess.existsLogbookLifeCycleObjectGroup("a"));
        try {
            mongoDbAccess.getLogbookLifeCycleUnit(GUIDFactory.newGUID().getId());
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
        final long nbl = mongoDbAccess.getLogbookLifeCyleUnitSize();
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
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleUnitSize());
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
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleUnitSize());
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
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleUnitSize());
        assertEquals(nbo + 1, mongoDbAccess.getLogbookOperationSize());

        assertTrue(mongoDbAccess.existsLogbookOperation(eip));
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
            assertEquals(3, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        node = JsonHandler.getFromString("{ $query: { $eq: { _id: \"" + eip + "\"} }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertEquals(3, operation.getOperations(true).size());
            assertEquals(2, operation.getOperations(false).size());
        }
        final LogbookOperation operation2 = mongoDbAccess.getLogbookOperation(eip);
        assertNotNull(operation2);
        assertEquals(4, operation2.getOperations(true).size());
        assertEquals(2, operation2.getOperations(false).size());

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
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
        assertEquals(nbl, mongoDbAccess.getLogbookLifeCyleUnitSize());
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
    }

    @Test
    public void testFunctionalLifeCycleUnit() throws VitamException {
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((MongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleUnitSize();
        final LogbookLifeCycleUnitParameters parameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        final LogbookLifeCycleUnitParameters parameters2 = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookLifeCycleUnitParameters parametersWrong =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateLogbookLifeCycleUnit(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.createLogbookLifeCycleUnit(
            parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());
        final String oi = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        assertNotNull(mongoDbAccess.getLogbookLifeCycleUnit(oi));
        try {
            mongoDbAccess.createLogbookLifeCycleUnit(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.createLogbookLifeCycleUnit(
                parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.updateLogbookLifeCycleUnit(
            parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());
        try {
            mongoDbAccess.updateLogbookLifeCycleUnit(
                parametersWrong.getParameterValue(LogbookParameterName.eventIdentifierProcess), parametersWrong);
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
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        mongoDbAccess.updateBulkLogbookLifeCycleUnit(parameters, parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());


        assertTrue(mongoDbAccess.existsLogbookLifeCycleUnit(oi));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: {_id: \"" + oi + "\"} }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleUnit lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        LogbookLifeCycleUnit lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(oi);
        assertNotNull(lifeCycle);
        assertEquals(4, lifeCycle.getLifeCycles(true).size());
        assertEquals(2, lifeCycle.getLifeCycles(false).size());
        lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(oi);
        assertNotNull(lifeCycle);
        lifeCycle = mongoDbAccess
            .getLogbookLifeCycleUnit(parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), oi);
        assertNotNull(lifeCycle);
        assertNotNull(lifeCycle);
        try {
            lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(GUIDFactory.newGUID().getId(), oi);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookLifeCycleUnits(node);
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
            mongoDbAccess.updateBulkLogbookLifeCycleUnit(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycleUnit();
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycleUnit(LogbookParametersFactory.newLogbookLifeCycleUnitParameters());;
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {

        }
        mongoDbAccess.createBulkLogbookLifeCycleUnit(parameters, parameters2);
        assertEquals(nbl + 2, mongoDbAccess.getLogbookLifeCyleUnitSize());
        try {
            mongoDbAccess.createBulkLogbookLifeCycleUnit(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1, " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1 " + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleUnit lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleUnit lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleUnit lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleUnit lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertTrue(cursor.hasNext());
        }
        mongoDbAccess.rollbackLogbookLifeCycleUnit(
            parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            parameters.getParameterValue(LogbookParameterName.objectIdentifier));
        try {
            mongoDbAccess.rollbackLogbookLifeCycleUnit(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                parameters.getParameterValue(LogbookParameterName.objectIdentifier));
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }
    }

    @Test
    public void testFunctionalLifeCycleObjectGroup() throws VitamException {
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((MongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleObjectGroupSize();
        final LogbookLifeCycleObjectGroupParameters parameters =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        final LogbookLifeCycleObjectGroupParameters parameters2 =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookLifeCycleObjectGroupParameters parametersWrong =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newOperationIdGUID(0).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        try {
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.createLogbookLifeCycleObjectGroup(
            parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        final String oi = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        assertNotNull(mongoDbAccess.getLogbookLifeCycleObjectGroup(oi));
        try {
            mongoDbAccess.createLogbookLifeCycleObjectGroup(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.createLogbookLifeCycleObjectGroup(
                parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        mongoDbAccess.updateLogbookLifeCycleObjectGroup(
            parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        try {
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(
                parametersWrong.getParameterValue(LogbookParameterName.eventIdentifierProcess), parametersWrong);
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
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        mongoDbAccess.updateBulkLogbookLifeCycleObjectGroup(parameters, parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());


        assertTrue(mongoDbAccess.existsLogbookLifeCycleObjectGroup(oi));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: {_id: \"" + oi + "\"} }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleObjectGroup lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        LogbookLifeCycleObjectGroup lifeCycle = mongoDbAccess.getLogbookLifeCycleObjectGroup(oi);
        assertNotNull(lifeCycle);
        assertEquals(4, lifeCycle.getLifeCycles(true).size());
        assertEquals(2, lifeCycle.getLifeCycles(false).size());
        lifeCycle = mongoDbAccess.getLogbookLifeCycleObjectGroup(
            parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), oi);
        assertNotNull(lifeCycle);
        try {
            lifeCycle = mongoDbAccess.getLogbookLifeCycleObjectGroup(GUIDFactory.newGUID().getId(), oi);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node);
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
            mongoDbAccess.updateBulkLogbookLifeCycleObjectGroup(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycleObjectGroup();
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}
        try {
            mongoDbAccess.updateBulkLogbookLifeCycleObjectGroup(
                LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters());;
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {

        }
        mongoDbAccess.createBulkLogbookLifeCycleObjectGroup(parameters, parameters2);
        assertEquals(nbl + 2, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        try {
            mongoDbAccess.createBulkLogbookLifeCycleObjectGroup(parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }

        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1, " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1 " + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleObjectGroup lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleObjectGroup lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleObjectGroup lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycleObjectGroup> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycleObjectGroup lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertTrue(cursor.hasNext());
        }
        mongoDbAccess.rollbackLogbookLifeCycleObjectGroup(
            parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            parameters.getParameterValue(LogbookParameterName.objectIdentifier));
        try {
            mongoDbAccess.rollbackLogbookLifeCycleObjectGroup(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                parameters.getParameterValue(LogbookParameterName.objectIdentifier));
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }
    }
}
