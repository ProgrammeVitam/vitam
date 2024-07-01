/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
package fr.gouv.vitam.logbook.lifecycles.core;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LogbookLifeCyclesImplWithMongoTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    static LogbookDbAccess mongoDbAccess;
    private static JunitHelper junitHelper;

    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();

    private LogbookLifeCyclesImpl logbookLifeCyclesImpl;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersBAD;
    private static final int tenantId = 0;
    private static final List<Integer> tenantList = Collections.singletonList(0);

    private static final ElasticsearchLogbookIndexManager indexManager =
        LogbookCollectionsTestUtils.createTestIndexManager(tenantList, Collections.emptyMap());

    // ObjectGroup
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersBAD;

    private static final GUID eip = GUIDFactory.newEventGUID(tenantId);
    private static final GUID iop = GUIDFactory.newEventGUID(tenantId);
    private static final GUID ioL = GUIDFactory.newEventGUID(tenantId);
    private static final GUID eip3 = GUIDFactory.newEventGUID(tenantId);

    @BeforeClass
    public static void setUpBeforeClass() throws IOException, VitamException {
        junitHelper = JunitHelper.getInstance();
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        LogbookCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));

        LogbookConfiguration logbookConfiguration = new LogbookConfiguration(
            nodes,
            mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes
        );
        VitamConfiguration.setTenants(tenantList);

        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration, Collections::emptyList, indexManager);

        logbookLifeCyclesUnitParametersStart = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        /**
         * Bad request
         */
        logbookLifeCyclesUnitParametersBAD = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier, eip3.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());

        // *****************************object Group************************************

        logbookLifeCyclesObjectGroupParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifier,
            eip.toString()
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.objectIdentifier,
            ioL.toString()
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetail,
            "outcomeDetail"
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        /**
         * Bad request
         */
        logbookLifeCyclesObjectGroupParametersBAD = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(
            LogbookParameterName.eventIdentifier,
            eip3.toString()
        );
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(
            LogbookParameterName.objectIdentifier,
            ioL.toString()
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LogbookCollectionsTestUtils.afterTestClass(indexManager, true);
        mongoDbAccess.close();
        VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void givenCreateAndUpdateUnit() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        createUpdateAndCommitUnitLifecycles();
        try {
            logbookLifeCyclesImpl.createUnit(
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesUnitParametersStart
            );

            logbookLifeCyclesImpl.createUnit(
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesUnitParametersStart
            );

            fail("Should failed");
        } catch (final LogbookAlreadyExistsException e) {}

        // get unit
        final LogbookLifeCycle logbookLifeCycle = logbookLifeCyclesImpl.selectLifeCycleById(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            null,
            false,
            LogbookCollections.LIFECYCLE_UNIT
        );
        assertNotNull(logbookLifeCycle);
    }

    private void createUpdateAndCommitUnitLifecycles()
        throws LogbookAlreadyExistsException, LogbookDatabaseException, LogbookNotFoundException {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersStart
        );

        // update unit ok
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.OK);

        logbookLifeCyclesImpl.updateUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersStart
        );

        // Commit the created lifeCycle
        logbookLifeCyclesImpl.commitUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
        );
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_idNotexists_when_rollback_thenThrow_LogbookNotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackUnit("notExist", "notExist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceParameters_When_CreateUnit_ThenThrow_IllegalArgumentException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit("bad_id", "bad_id", logbookLifeCyclesUnitParametersBAD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceObjectEventParameters_When_CreateUnit_ThenThrow_IllegalArgumentException()
        throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            "bad_id",
            logbookLifeCyclesUnitParametersStart
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceParameters_When_UpdateUni_tThenThrow_IllegalArgumentException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit("bad_id", "bad_id", logbookLifeCyclesUnitParametersBAD);
    }

    @Test(expected = LogbookNotFoundException.class)
    @RunWithCustomExecutor
    public void given_BAdParameters_When_UpdateUni_tThenThrow_NotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(
            logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersBAD
        );
    }

    @Test(expected = LogbookNotFoundException.class)
    @RunWithCustomExecutor
    public void given_Select_When_UnitNotExist_ThenThrow_NotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        logbookLifeCyclesImpl.selectLifeCycles(
            JsonHandler.getFromString(select.getFinalSelect().toString()),
            true,
            LogbookCollections.LIFECYCLE_UNIT
        );
    }

    @Test(expected = LogbookNotFoundException.class)
    @RunWithCustomExecutor
    public void given_find_When_UnitNotExist_ThenThrow_NotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycleById("notExist", null, false, LogbookCollections.LIFECYCLE_UNIT);
    }

    /**
     * object Group test
     */

    @Test
    @RunWithCustomExecutor
    public void givenCreateAndUpdateObjectGroup() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);

        createUpdateAndCommitOGLfc();
        try {
            // Try to create two objects with the same objectIdentifier
            logbookLifeCyclesImpl.createObjectGroup(
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess
                ),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesObjectGroupParametersStart
            );

            logbookLifeCyclesImpl.createObjectGroup(
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess
                ),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesObjectGroupParametersStart
            );

            fail("Should fail");
        } catch (final LogbookAlreadyExistsException e) {}

        // get objectgroup
        final LogbookLifeCycle logbookLifeCycle = logbookLifeCyclesImpl.selectLifeCycleById(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            null,
            false,
            LogbookCollections.LIFECYCLE_OBJECTGROUP
        );
        assertNotNull(logbookLifeCycle);
    }

    private void createUpdateAndCommitOGLfc()
        throws LogbookAlreadyExistsException, LogbookDatabaseException, LogbookNotFoundException {
        logbookLifeCyclesImpl.createObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesObjectGroupParametersStart
        );

        // update objectGroup ok
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage"
        );
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.OK);
        logbookLifeCyclesImpl.updateObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesObjectGroupParametersStart
        );

        // Commit the created lifeCycle
        logbookLifeCyclesImpl.commitObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
        );
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_ObjectGroupIdNotexists_when_rollback_thenThrow_LogbookNotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackObjectGroup("notExist", "notExist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceParameters_When_CreateObjectGroup_ThenThrow_IllegalArgumentException()
        throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup("bad_id", "bad_id", logbookLifeCyclesObjectGroupParametersBAD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceParameters_When_UpdateObjectGroup_tThenThrow_IllegalArgumentException()
        throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup("bad_id", "bad_id", logbookLifeCyclesObjectGroupParametersBAD);
    }

    @Test(expected = LogbookNotFoundException.class)
    @RunWithCustomExecutor
    public void given_Select_When_ObjectGroupNotExist_ThenThrow_NotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        logbookLifeCyclesImpl.selectLifeCycles(
            JsonHandler.getFromString(select.getFinalSelect().toString()),
            false,
            LogbookCollections.LIFECYCLE_OBJECTGROUP
        );
    }

    @Test(expected = LogbookNotFoundException.class)
    @RunWithCustomExecutor
    public void given_find_When_ObjectGroupNotExist_ThenThrow_NotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycleById("notExist", null, false, LogbookCollections.LIFECYCLE_OBJECTGROUP);
    }
}
