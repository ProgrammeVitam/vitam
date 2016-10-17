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
package fr.gouv.vitam.logbook.lifecycles.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

public class LogbookLifeCyclesImplWithMongoTest {
    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;

    private LogbookLifeCyclesImpl logbookLifeCyclesImpl;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersBAD;
    // ObjectGroup

    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersBAD;

    final static GUID eip = GUIDFactory.newEventGUID(0);
    final static GUID iop = GUIDFactory.newEventGUID(2);
    final static GUID ioL = GUIDFactory.newEventGUID(2);
    final static GUID eip3 = GUIDFactory.newEventGUID(3);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
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

        logbookLifeCyclesUnitParametersStart = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        /**
         * Bad request
         */
        logbookLifeCyclesUnitParametersBAD = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier,
            eip3.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

        // *****************************object Group************************************


        logbookLifeCyclesObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        /**
         * Bad request
         */
        logbookLifeCyclesObjectGroupParametersBAD = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier,
            eip3.toString());
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenCreateAndUpdateUnit() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersStart);

        // update unit ok
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.OK);

        logbookLifeCyclesImpl.updateUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersStart);

        try {
            logbookLifeCyclesImpl.createUnit(
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesUnitParametersStart);

            fail("Should failed");
        } catch (final LogbookAlreadyExistsException e) {}

        // get objectgroup
        final LogbookLifeCycleUnit logbookLifeCycle = logbookLifeCyclesImpl.getUnitById(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier));
        assertNotNull(logbookLifeCycle);
        final LogbookLifeCycleUnit logbookLifeCycle2 =
            logbookLifeCyclesImpl
                .getUnitByOperationIdAndByUnitId(logbookLifeCyclesUnitParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                    logbookLifeCyclesUnitParametersStart
                        .getParameterValue(LogbookParameterName.objectIdentifier));
        assertNotNull(logbookLifeCycle2);

    }

    public void given_existUnit_when_rollBack_return_OK()
        throws LogbookAlreadyExistsException, LogbookDatabaseException, IllegalArgumentException,
        LogbookNotFoundException {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersStart);
        logbookLifeCyclesImpl.rollbackUnit(
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier));
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
            "bad_id", logbookLifeCyclesUnitParametersStart);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_IncoherenceParameters_When_UpdateUni_tThenThrow_IllegalArgumentException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit("bad_id", "bad_id", logbookLifeCyclesUnitParametersBAD);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_BAdParameters_When_UpdateUni_tThenThrow_NotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(
            logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesUnitParametersBAD);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_Select_When_UnitNotExist_ThenThrow_NotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        logbookLifeCyclesImpl.selectUnit(JsonHandler.getFromString(select.getFinalSelect().toString()));
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_find_When_UnitNotExist_ThenThrow_NotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.getUnitById("notExist");
    }

    /**
     * object Group test
     */

    @Test
    public void givenCreateAndUpdateObjectGroup() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesObjectGroupParametersStart);

        // update unit ok
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.OK);
        logbookLifeCyclesImpl.updateObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesObjectGroupParametersStart);

        try {
            logbookLifeCyclesImpl.createObjectGroup(
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
                logbookLifeCyclesObjectGroupParametersStart);

            fail("Should failed");
        } catch (final LogbookAlreadyExistsException e) {}


        // get objectgroup
        final LogbookLifeCycleObjectGroup logbookLifeCycle = logbookLifeCyclesImpl.getObjectGroupById(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier));
        assertNotNull(logbookLifeCycle);
        final LogbookLifeCycleObjectGroup logbookLifeCycle2 =
            logbookLifeCyclesImpl
                .getObjectGroupByOperationIdAndByObjectGroupId(logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                    logbookLifeCyclesObjectGroupParametersStart
                        .getParameterValue(LogbookParameterName.objectIdentifier));
        assertNotNull(logbookLifeCycle2);
    }

    public void given_existLifeCyclesObjectGroup_when_rollBack_return_OK()
        throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier),
            logbookLifeCyclesObjectGroupParametersStart);
        logbookLifeCyclesImpl.rollbackUnit(
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier));
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
    public void given_Select_When_ObjectGroupNotExist_ThenThrow_NotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        logbookLifeCyclesImpl.selectObjectGroup(JsonHandler.getFromString(select.getFinalSelect().toString()));
    }

    @Test(expected = LogbookNotFoundException.class)
    public void given_find_When_ObjectGroupNotExist_ThenThrow_NotFoundException() throws Exception {
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.getObjectGroupById("notExist");
    }

}
