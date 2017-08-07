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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDbAccessFactory Test
 */
public class LogbookMongoDbAccessFactoryTest {

    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    static LogbookDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;

    private static final Integer TENANT_ID = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    // ES
    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private final static String ES_HOST_NAME = "localhost";
    private static ElasticsearchTestConfiguration config = null;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(esTempFolder, ES_CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }


        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ES_HOST_NAME, config.getTcpPort()));

        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, DATABASE_NAME, ES_CLUSTER_NAME, esNodes);
        logbookConfiguration.setTenants(tenantList);

        mongoDbAccess =
            LogbookMongoDbAccessFactory
                .create(logbookConfiguration);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    @RunWithCustomExecutor
    public void testStructure() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(((LogbookMongoDbAccessImpl) mongoDbAccess).getEsClient());
        assertTrue(((LogbookMongoDbAccessImpl) mongoDbAccess).getEsClient().checkConnection());

        assertEquals(LogbookMongoDbName.agentIdentifier,
            LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.agentIdentifier));
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            assertEquals(name,
                LogbookMongoDbName.getFromDbname(name.getDbname()));
        }

        assertEquals(LogbookLifeCycleMongoDbName.eventDetailData,
            LogbookLifeCycleMongoDbName.getLogbookLifeCycleMongoDbName(LogbookParameterName.eventDetailData));

        for (final LogbookLifeCycleMongoDbName name : LogbookLifeCycleMongoDbName.values()) {
            assertEquals(name,
                LogbookLifeCycleMongoDbName.getFromDbname(name.getDbname()));
        }

        try {
            LogbookMongoDbName.getFromDbname("fakeValue");
            fail("Should throw an exception");
        } catch (final IllegalArgumentException e) {}

        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookOperation operation = new LogbookOperation(parameters);
        final LogbookOperation operation1 = new LogbookOperation(parameters, true);
        final LogbookOperation operation2 = new LogbookOperation(operation.toJson());
        final LogbookOperation operation3 = new LogbookOperation(parameters, false);
        assertEquals(operation.getId(), operation2.getId());
        assertEquals(operation.getTenantId(), operation2.getTenantId());
        assertNotNull(MongoDbHelper.bsonToString(operation, true));
        assertNotNull(MongoDbHelper.bsonToString(operation, false));
        assertTrue(MongoDbHelper.bsonToString(null, false).isEmpty());

        final String operation1String = MongoDbHelper.bsonToString(operation1, true);
        assertTrue(operation1String.contains(LogbookMongoDbName.eventDetailData.getDbname()));
        assertFalse(operation1String.contains(LogbookMongoDbName.agentIdentifierApplication.getDbname()));
        assertFalse(operation1String.contains(LogbookMongoDbName.agentIdentifierApplicationSession.getDbname()));
        assertFalse(operation1String.contains(LogbookMongoDbName.agentIdentifierOriginating.getDbname()));
        assertFalse(operation1String.contains(LogbookMongoDbName.agentIdentifierSubmission.getDbname()));

        final String operation3String = MongoDbHelper.bsonToString(operation3, true);
        assertTrue(operation3String.contains(LogbookMongoDbName.eventDetailData.getDbname()));
        assertTrue(operation3String.contains(LogbookMongoDbName.agentIdentifierApplication.getDbname()));
        assertTrue(operation3String.contains(LogbookMongoDbName.agentIdentifierApplicationSession.getDbname()));
        assertTrue(operation3String.contains(LogbookMongoDbName.agentIdentifierOriginating.getDbname()));
        assertTrue(operation3String.contains(LogbookMongoDbName.agentIdentifierSubmission.getDbname()));

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
    @RunWithCustomExecutor
    public void testCreateFn() throws VitamException, InvalidCreateOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((LogbookMongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        String status = mongoDbAccess.toString();
        ((LogbookMongoDbAccessImpl) mongoDbAccess).flushOnDisk();

        LogbookMongoDbAccessImpl.ensureIndex();
        status = mongoDbAccess.toString();
        assertTrue(status.indexOf("LogbookOperation") > 0);
        assertEquals(status, mongoDbAccess.toString());
        LogbookMongoDbAccessImpl.removeIndexBeforeImport();;
        assertEquals(status, mongoDbAccess.toString());
        LogbookMongoDbAccessImpl.resetIndexAfterImport();;
        assertEquals(status, mongoDbAccess.toString());
        assertEquals(0, mongoDbAccess.getLogbookLifeCyleUnitSize());
        assertEquals(0, mongoDbAccess.getLogbookOperationSize());
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        try (MongoCursor<LogbookOperation> cursorOperation =
            mongoDbAccess.getLogbookOperations(select.getFinalSelect(), true)) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursorOperation =
            mongoDbAccess.getLogbookLifeCycleUnits(select.getFinalSelect(), true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertFalse(cursorOperation.hasNext());
        }
        try (MongoCursor<LogbookLifeCycle> cursorOperation =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(select.getFinalSelect(), true,
                LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
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
            JsonNode queryDsl =
                JsonHandler.getFromString(
                    "{ $query: { $eq: {obId: \"" + GUIDFactory.newGUID().getId() +
                        "\"} }, $projection: {}, $filter: {} }");
            mongoDbAccess.getLogbookLifeCycleUnit(queryDsl, LogbookCollections.LIFECYCLE_UNIT);
            fail("Should throw an exception");
        } catch (final VitamException e) {}
        try {
            mongoDbAccess.getLogbookOperation(GUIDFactory.newGUID().getId());
            fail("Should throw an exception");
        } catch (final VitamException e) {}
    }

    @Test
    @RunWithCustomExecutor
    public void testFunctionalOperation() throws VitamException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((LogbookMongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleUnitSize();
        final long nbo = mongoDbAccess.getLogbookOperationSize();

        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            if (LogbookParameterName.eventDetailData.equals(name)) {
                parameters.putParameterValue(name, "{\"value\":\"" +
                    GUIDFactory.newEventGUID(TENANT_ID).getId() + "\"}");
            } else {
                parameters.putParameterValue(name,
                    GUIDFactory.newEventGUID(TENANT_ID).getId());
            }
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        final LogbookOperationParameters parameters2 = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            if (LogbookParameterName.eventDetailData.equals(name)) {
                parameters2.putParameterValue(name, "{\"value\":\"" +
                    GUIDFactory.newEventGUID(TENANT_ID).getId() + "\"}");
            } else {
                parameters2.putParameterValue(name,
                    GUIDFactory.newEventGUID(TENANT_ID).getId());
            }
        }
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final LogbookOperationParameters parametersWrong = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
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
        final LogbookOperation ragnarLogbook = mongoDbAccess.getLogbookOperation(eip);
        assertNotNull(ragnarLogbook);
        final List<LogbookOperationParameters> listeOperations = ragnarLogbook.getOperations(true);
        // Operation 0 is the insertion, so it should contain all the fields
        final LogbookOperationParameters operation0 = listeOperations.get(0);
        assertNotNull(operation0);
        assertNotNull(operation0.getParameterValue(LogbookParameterName.agentIdentifierApplication));
        assertNotNull(operation0.getParameterValue(LogbookParameterName.agentIdentifierApplicationSession));
        assertNotNull(operation0.getParameterValue(LogbookParameterName.agentIdentifierOriginating));
        assertNotNull(operation0.getParameterValue(LogbookParameterName.agentIdentifierSubmission));
        assertNotNull(operation0.getParameterValue(LogbookParameterName.eventDetailData));
        // Operation 1 is the update, so it should contain restricted fields
        final LogbookOperationParameters operation1 = listeOperations.get(1);
        assertNotNull(operation1);
        assertNull(operation1.getParameterValue(LogbookParameterName.agentIdentifierApplication));
        assertNull(operation1.getParameterValue(LogbookParameterName.agentIdentifierApplicationSession));
        assertNull(operation1.getParameterValue(LogbookParameterName.agentIdentifierOriginating));
        assertNull(operation1.getParameterValue(LogbookParameterName.agentIdentifierSubmission));
        assertNotNull(operation1.getParameterValue(LogbookParameterName.eventDetailData));
        try {
            mongoDbAccess.updateLogbookOperation(parametersWrong);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
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
            mongoDbAccess.getLogbookOperations(node, true)) {
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
            mongoDbAccess.getLogbookOperations(node, true)) {
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
            LogbookOperation.ID + " : 1, " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookOperations(node, true);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
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
            mongoDbAccess.getLogbookOperations(node, true)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node, true)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertTrue(cursor.hasNext());
        }

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters.putParameterValue(LogbookParameterName.eventTypeProcess, "TRACEABILITY");
        mongoDbAccess.createBulkLogbookOperation(parameters);

        node = JsonHandler.getFromString("{ $query: { $eq: { 'evTypeProc' : 'TRACEABILITY' } }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");

        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node, true)) {
            assertTrue(cursor.hasNext());
            final LogbookOperation operation = cursor.next();
            assertNotNull(operation);
            assertFalse(cursor.hasNext());
        }

        node = JsonHandler.getFromString(
            "{ $query: { $and : [{ $eq: { 'evTypeProc' : 'TRACEABILITY' }}, { $eq: { 'outcome' : 'XXXXX' }} ]} }, $projection: { " +
                LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");

        try (MongoCursor<LogbookOperation> cursor =
            mongoDbAccess.getLogbookOperations(node, true)) {
            assertFalse(cursor.hasNext());
            assertNull(cursor.next());
        }

    }

    @Test
    @RunWithCustomExecutor
    public void testFunctionalLifeCycleUnit() throws VitamException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((LogbookMongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleUnitSize();
        final LogbookLifeCycleUnitParameters parameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters.setTypeProcess(LogbookTypeProcess.INGEST);

        final String oi = parameters.getParameterValue(LogbookParameterName.objectIdentifier);

        final LogbookLifeCycleUnitParameters parameters2 = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.setTypeProcess(LogbookTypeProcess.INGEST);

        final LogbookLifeCycleUnitParameters parametersWrong =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parametersWrong.setTypeProcess(LogbookTypeProcess.INGEST);

        try {
            mongoDbAccess.updateLogbookLifeCycleUnit(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        commitUnit(oi, true, parameters);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());
        final String oi2 = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        JsonNode queryDsl =
            JsonHandler.getFromString(
                "{ $query: { $eq: {obId: \"" + oi2 + "\"} }, $projection: {}, $filter: {} }");
        assertNotNull(mongoDbAccess.getLogbookLifeCycleUnit(queryDsl, LogbookCollections.LIFECYCLE_UNIT));
        try {
            commitUnit(oi2, true, parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        try {
            commitUnit(oi2, true, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        // From now on, typeProcess will be UPDATE
        parameters.setTypeProcess(LogbookTypeProcess.UPDATE);
        parameters2.setTypeProcess(LogbookTypeProcess.UPDATE);
        parametersWrong.setTypeProcess(LogbookTypeProcess.UPDATE);

        mongoDbAccess.updateLogbookLifeCycleUnit(
            parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);

        // Commit the last update
        commitUnit(oi2, false, parameters2);
        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());

        try {
            mongoDbAccess.updateLogbookLifeCycleUnit(
                parametersWrong.getParameterValue(LogbookParameterName.eventIdentifierProcess), parametersWrong);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, oi2);
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier, oi2);
        mongoDbAccess.updateBulkLogbookLifeCycleUnit(parameters, parameters2);

        // Commit the last bulk update
        commitUnit(oi2, false, parameters2);

        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleUnitSize());


        assertTrue(mongoDbAccess.existsLogbookLifeCycleUnit(oi));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: {_id: \"" + oi2 + "\"} }, $projection: {}, $filter: {} }");
        // sliced
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        // non sliced
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, false, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(5, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        // full
        final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
        parser.parse(node);
        try (MongoCursor<LogbookLifeCycleUnit> cursor =
            mongoDbAccess.getLogbookLifeCycleUnitsFull(LogbookCollections.LIFECYCLE_UNIT, parser.getRequest())) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(5, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }


        LogbookLifeCycleUnit lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(oi2);
        assertNotNull(lifeCycle);
        assertEquals(5, lifeCycle.getLifeCycles(true).size());
        assertEquals(2, lifeCycle.getLifeCycles(false).size());
        lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(oi2);
        assertNotNull(lifeCycle);
        lifeCycle = mongoDbAccess
            .getLogbookLifeCycleUnit(parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), oi2);
        assertNotNull(lifeCycle);
        assertNotNull(lifeCycle);
        try {
            lifeCycle = mongoDbAccess.getLogbookLifeCycleUnit(GUIDFactory.newGUID().getId(), oi2);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }

        node = JsonHandler.getFromString("{ $query: { $unknown : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try {
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT);
            fail("Should throw an exception");
        } catch (final VitamException e) {}


        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final String oi3 = parameters.getMapParameters().get(LogbookParameterName.objectIdentifier);
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

        // Commit new lifeCycle
        commitUnit(oi3, true, parameters, parameters2);

        assertEquals(nbl + 2, mongoDbAccess.getLogbookLifeCyleUnitSize());
        try {
            commitUnit(oi3, true, parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1, " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1 " + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleUnits(node, true, LogbookCollections.LIFECYCLE_UNIT)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
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

    private void commitUnit(String objectIdentifier, boolean isCreationProcess,
        LogbookLifeCycleUnitParameters... parameters)
        throws LogbookDatabaseException, LogbookAlreadyExistsException, LogbookNotFoundException {

        if (isCreationProcess) {
            // Create a unit lifeCycle in working collection
            mongoDbAccess.createBulkLogbookLifeCycleUnit(parameters);
        }

        LogbookLifeCycleUnitInProcess inProcessUnit = mongoDbAccess
            .getLogbookLifeCycleUnitInProcess(objectIdentifier);

        if (isCreationProcess) {
            mongoDbAccess.createLogbookLifeCycleUnit(inProcessUnit);
        } else {
            mongoDbAccess.updateLogbookLifeCycleUnit(inProcessUnit);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testFunctionalLifeCycleObjectGroup() throws VitamException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", ((LogbookMongoDbAccessImpl) mongoDbAccess).getMongoDatabase().getName());
        final long nbl = mongoDbAccess.getLogbookLifeCyleObjectGroupSize();
        final LogbookLifeCycleObjectGroupParameters parameters =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters.setTypeProcess(LogbookTypeProcess.INGEST);

        final String oi = parameters.getParameterValue(LogbookParameterName.objectIdentifier);

        final LogbookLifeCycleObjectGroupParameters parameters2 =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters2.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.setTypeProcess(LogbookTypeProcess.INGEST);

        final LogbookLifeCycleObjectGroupParameters parametersWrong =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parametersWrong.putParameterValue(name,
                GUIDFactory.newEventGUID(TENANT_ID).getId());
        }
        parametersWrong.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parametersWrong.setTypeProcess(LogbookTypeProcess.INGEST);

        try {
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(
                parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        commitObjectGroup(oi, true, parameters);

        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        assertNotNull(mongoDbAccess.getLogbookLifeCycleObjectGroup(oi));
        try {
            commitObjectGroup(oi, true, parameters);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        try {
            commitObjectGroup(oi, true, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {}

        // From now on, typeProcess will be UPDATE
        parameters.setTypeProcess(LogbookTypeProcess.UPDATE);
        parameters2.setTypeProcess(LogbookTypeProcess.UPDATE);
        parametersWrong.setTypeProcess(LogbookTypeProcess.UPDATE);

        mongoDbAccess.updateLogbookLifeCycleObjectGroup(
            parameters2.getParameterValue(LogbookParameterName.eventIdentifierProcess), parameters2);

        // Commit the last update
        commitObjectGroup(oi, false, parameters2);

        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        try {
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(
                parametersWrong.getParameterValue(LogbookParameterName.eventIdentifierProcess), parametersWrong);
            fail("Should throw an exception");
        } catch (final VitamException e) {}


        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier, oi);
        mongoDbAccess.updateBulkLogbookLifeCycleObjectGroup(parameters, parameters2);

        // Commit the last bulk update
        commitObjectGroup(oi, false, parameters2);

        assertEquals(nbl + 1, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());


        assertTrue(mongoDbAccess.existsLogbookLifeCycleObjectGroup(oi));
        JsonNode node =
            JsonHandler.getFromString(
                "{ $query: { $eq: {_id: \"" + oi + "\"} }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifeCycle = cursor.next();
            assertNotNull(lifeCycle);
            assertEquals(2, lifeCycle.getLifeCycles(true).size());
            assertEquals(2, lifeCycle.getLifeCycles(false).size());
        }
        LogbookLifeCycleObjectGroup lifeCycle = mongoDbAccess.getLogbookLifeCycleObjectGroup(oi);
        assertNotNull(lifeCycle);
        assertEquals(5, lifeCycle.getLifeCycles(true).size());
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
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP);
            fail("Should throw an exception");
        } catch (final VitamException e) {}


        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(TENANT_ID).getId());
        parameters2.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            parameters.getMapParameters().get(LogbookParameterName.eventIdentifierProcess));
        parameters2.putParameterValue(LogbookParameterName.objectIdentifier,
            parameters.getMapParameters().get(LogbookParameterName.objectIdentifier));
        parameters2.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());

        final String oi2 = parameters.getMapParameters().get(LogbookParameterName.objectIdentifier);

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

        // Commit new lifeCycle
        commitObjectGroup(oi2, true, parameters, parameters2);

        assertEquals(nbl + 2, mongoDbAccess.getLogbookLifeCyleObjectGroupSize());
        try {
            commitObjectGroup(oi2, true, parameters, parameters2);
            fail("Should throw an exception");
        } catch (final VitamException e) {

        }

        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1, " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1 " + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifierRequest.getDbname() + " : -1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node = JsonHandler.getFromString("{ $query: { $exists : '_id' }, $projection: { " +
            LogbookMongoDbName.eventIdentifier.getDbname() + " : 1" + " }, $filter: { $limit : 1 } }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
            assertNotNull(lifecycle);
            assertFalse(cursor.hasNext());
        }
        node =
            JsonHandler.getFromString(
                "{ $query: { $exists : '_id' }, $projection: {}, $filter: {} }");
        try (MongoCursor<LogbookLifeCycle> cursor =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(node, true, LogbookCollections.LIFECYCLE_OBJECTGROUP)) {
            assertTrue(cursor.hasNext());
            final LogbookLifeCycle lifecycle = cursor.next();
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

    private void commitObjectGroup(String objectIdentifier, boolean isCreationProcess,
        LogbookLifeCycleObjectGroupParameters... parameters)
        throws LogbookDatabaseException, LogbookAlreadyExistsException, LogbookNotFoundException {

        if (isCreationProcess) {
            // Create an object group lifeCycle in working collection
            mongoDbAccess.createBulkLogbookLifeCycleObjectGroup(parameters);
        }

        LogbookLifeCycleObjectGroupInProcess inProcessObjectGroup = mongoDbAccess
            .getLogbookLifeCycleObjectGroupInProcess(objectIdentifier);

        if (isCreationProcess) {
            mongoDbAccess.createLogbookLifeCycleObjectGroup(inProcessObjectGroup);
        } else {
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(inProcessObjectGroup);
        }
    }
}
