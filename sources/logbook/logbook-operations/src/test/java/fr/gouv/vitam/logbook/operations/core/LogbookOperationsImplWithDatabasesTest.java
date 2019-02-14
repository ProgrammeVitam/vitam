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
package fr.gouv.vitam.logbook.operations.core;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class LogbookOperationsImplWithDatabasesTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final int tenantId = 0;
    private static final List<Integer> tenantList = Collections.singletonList(0);
    private final static GUID eip = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip1 = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip2 = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip3 = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip4 = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip5 = GUIDFactory.newEventGUID(tenantId);
    private final static GUID eip6 = GUIDFactory.newEventGUID(1);
    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static LogbookDbAccess mongoDbAccess;
    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookOperationParameters logbookParameters1;
    private static LogbookOperationParameters logbookParameters2;
    private static LogbookOperationParameters logbookParameters3;
    private static LogbookOperationParameters logbookParameters4;
    private static LogbookOperationParameters logbookParameters5;
    private static LogbookOperationParameters event;
    private static LogbookOperationParameters event2;
    private static LogbookOperationParameters securityEvent;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    private LogbookOperationsImpl logbookOperationsImpl;
    @Mock
    private WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;
    @Mock
    private IndexationHelper indexationHelper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), 0, 1);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));
        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, mongoRule.getMongoDatabase().getName(), ElasticsearchRule.VITAM_CLUSTER,
                esNodes);
        VitamConfiguration.setTenants(tenantList);

        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration);

        final String datestring1 = "2015-01-01";
        final String datestring2 = "2016-12-12";
        final String datestring3 = "1990-10-01";
        final String datestring4 = "2017-09-01";
        final String datestring5 = "2017-09-02";
        final String datestring6 = "2017-11-02";

        final String dateStringSecurity = "2017-10-04";

        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(

            GUIDFactory.newEventGUID(0),
            "eventType", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        logbookParameters1 = LogbookParametersFactory.newLogbookOperationParameters(
            eip1, "eventType", eip1, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParameters1.putParameterValue(LogbookParameterName.eventDateTime, datestring1);
        logbookParameters2 = LogbookParametersFactory.newLogbookOperationParameters(
            eip2, "eventType", eip2, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParameters2.putParameterValue(LogbookParameterName.eventDateTime, datestring2);
        logbookParameters3 = LogbookParametersFactory.newLogbookOperationParameters(
            eip3, "eventType", eip3, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParameters3.putParameterValue(LogbookParameterName.eventDateTime, datestring3);

        logbookParameters4 = LogbookParametersFactory.newLogbookOperationParameters(
            eip4,
            "STP_OP_SECURISATION", eip4, LogbookTypeProcess.TRACEABILITY,
            StatusCode.STARTED, null, null, eip4);

        logbookParameters4.putParameterValue(LogbookParameterName.eventDateTime, datestring4);

        logbookParameters5 = LogbookParametersFactory.newLogbookOperationParameters(
            eip6,
            "STP_OP_SECURISATION", eip6, LogbookTypeProcess.TRACEABILITY,
            StatusCode.STARTED, null, null, eip6);

        logbookParameters5.putParameterValue(LogbookParameterName.eventDateTime, datestring6);
        event =
            LogbookParametersFactory.newLogbookOperationParameters(eip4, "eventType", eip4, LogbookTypeProcess.INGEST,
                StatusCode.STARTED, "start ingest", eip4);
        event2 =
            LogbookParametersFactory.newLogbookOperationParameters(eip6, "eventType", eip6, LogbookTypeProcess.INGEST,
                StatusCode.STARTED, "start ingest", eip6);
        event.putParameterValue(LogbookParameterName.eventDateTime, datestring5);
        event2.putParameterValue(LogbookParameterName.eventDateTime, datestring6);


        securityEvent = LogbookParametersFactory.newLogbookOperationParameters(
            eip5, "STP_OP_SECURISATION", eip4, LogbookTypeProcess.TRACEABILITY,
            StatusCode.OK, null, null, eip4);
        securityEvent.putParameterValue(LogbookParameterName.eventDateTime, dateStringSecurity);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LogbookCollections.afterTestClass(true, 0, 1);
        mongoDbAccess.close();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void before() {
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @After
    public void clean() {
        LogbookCollections.afterTest(0, 1);
    }

    @Test
    public void givenCreateAndUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        mockWorkspaceClient();
        logbookOperationsImpl =
            new LogbookOperationsImpl(mongoDbAccess, workspaceClientFactory, storageClientFactory, indexationHelper);
        logbookOperationsImpl.create(logbookParametersStart);
        logbookOperationsImpl.update(logbookParametersAppend);
        try {
            logbookOperationsImpl.create(logbookParametersWrongStart);
            fail("Should failed");
        } catch (final LogbookAlreadyExistsException ignored) {
        }
        try {
            logbookOperationsImpl.update(logbookParametersWrongAppend);
            fail("Should failed");
        } catch (final LogbookNotFoundException ignored) {
        }
        try {
            logbookOperationsImpl.create(LogbookParametersFactory.newLogbookOperationParameters());
            fail("Should failed");
        } catch (final IllegalArgumentException ignored) {
        }
        try {

            final Select select = new Select();
            select.setQuery(exists("notExistVariable"));
            logbookOperationsImpl.select(JsonHandler.getFromString(select.getFinalSelect().toString()));
            fail("Should failed");
        } catch (final LogbookNotFoundException ignored) {
        }
    }

    @Test
    public void givenCreateAndSelect() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        mockWorkspaceClient();
        logbookOperationsImpl =
            new LogbookOperationsImpl(mongoDbAccess, workspaceClientFactory, storageClientFactory, indexationHelper);
        logbookOperationsImpl.create(logbookParameters1);
        logbookOperationsImpl.create(logbookParameters2);
        logbookOperationsImpl.create(logbookParameters3);

        final Select select = new Select();
        select.setQuery(new CompareQuery(QUERY.EQ, "evId", eip1.toString()));
        List<LogbookOperation> res1;
        res1 = logbookOperationsImpl.select(select.getFinalSelect());
        assertNotNull(res1);
        assertTrue(res1.get(0).containsValue(eip1.getId()));

        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "eventType"));
        List<LogbookOperation> res3;
        select.addOrderByDescFilter("evDateTime");
        res3 = logbookOperationsImpl.select(select.getFinalSelect());
        assertTrue(res3.get(0).containsValue("2016-12-12"));
        assertTrue(res3.get(1).containsValue("2015-01-01"));
        assertTrue(res3.get(2).containsValue("1990-10-01"));

        // Update new events
        Thread.sleep(100);

        LocalDateTime snapshotDate1 = LocalDateUtil.now();
        logbookOperationsImpl.create(logbookParameters4);
        logbookOperationsImpl.update(event);
        LocalDateTime snapshotDate2 = LocalDateUtil.now();

        MongoCursor<LogbookOperation> cursor;
        cursor = logbookOperationsImpl.selectOperationsByLastPersistenceDateInterval(snapshotDate1, snapshotDate2);
        assertTrue(cursor.hasNext());
        final LogbookOperation op = cursor.next();

        assertDateBetween(
            LocalDateUtil.parseMongoFormattedDate(op.getString(LogbookDocument.LAST_PERSISTED_DATE)),
            snapshotDate1,
            snapshotDate2);

        assertEquals(op.get("evDateTime"), "2017-09-01");
        final List<Document> list = (List<Document>) op.get(LogbookDocument.EVENTS);
        assertEquals(list.get(0).get("evDateTime"), "2017-09-02");
        assertFalse(cursor.hasNext());

        logbookOperationsImpl.update(securityEvent);
        final LogbookOperation secureOperation =
            logbookOperationsImpl.findFirstTraceabilityOperationOKAfterDate(LocalDateTime.parse("2017-08-02T12:01:00"));

        assertEquals(secureOperation.get("evTypeProc"), LogbookTypeProcess.TRACEABILITY.toString());
    }

    @Test
    public void selectOperationsByLastPersistenceDateIntervalTest() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        mockWorkspaceClient();
        logbookOperationsImpl =
            new LogbookOperationsImpl(mongoDbAccess, workspaceClientFactory, storageClientFactory, indexationHelper);

        logbookOperationsImpl.create(logbookParameters1);
        logbookOperationsImpl.create(logbookParameters2);

        Thread.sleep(10);

        LocalDateTime snapshot1 = LocalDateUtil.now();
        Thread.sleep(10);
        logbookOperationsImpl.create(logbookParameters3);

        LocalDateTime snapshot2 = LocalDateUtil.now();
        Thread.sleep(10);

        logbookOperationsImpl.create(logbookParameters4);

        MongoCursor<LogbookOperation> cursor;
        cursor = logbookOperationsImpl.selectOperationsByLastPersistenceDateInterval(snapshot1, snapshot2);

        assertEquals(eip3.toString(), cursor.next().get("evId"));
        assertFalse(cursor.hasNext());
    }

    private void mockWorkspaceClient() throws Exception {
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        doNothing().when(workspaceClient).createContainer(any());
        doNothing().when(workspaceClient)
            .putObject(any(), any(), any());
    }

    private static void assertDateBetween(LocalDateTime localDateTime, LocalDateTime gte, LocalDateTime lte) {
        assert (localDateTime.isEqual(gte) || localDateTime.isAfter(gte));
        assert (localDateTime.isEqual(lte) || localDateTime.isBefore(lte));
    }
}
