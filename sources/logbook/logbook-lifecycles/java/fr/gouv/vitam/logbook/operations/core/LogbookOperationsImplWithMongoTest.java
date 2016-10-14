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

import static fr.gouv.vitam.builder.request.construct.QueryHelper.exists;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

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
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.query.CompareQuery;
import fr.gouv.vitam.builder.singlerequest.Select;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

public class LogbookOperationsImplWithMongoTest {

    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;

    private LogbookOperationsImpl logbookOperationsImpl;
    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookOperationParameters logbookParameters1;
    private static LogbookOperationParameters logbookParameters2;
    private static LogbookOperationParameters logbookParameters3;

    final static GUID eip = GUIDFactory.newOperationIdGUID(0);
    final static GUID eip1 = GUIDFactory.newOperationIdGUID(2);
    final static GUID eip2 = GUIDFactory.newOperationIdGUID(2);
    final static GUID eip3 = GUIDFactory.newOperationIdGUID(3);

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

        final String datestring1 = "2015-01-01";
        final String datestring2 = "2016-12-12";
        final String datestring3 = "1990-10-01";

        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0),
            "eventType", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip.getId(),
            "eventType", eip.getId(), LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "x-request-id");
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0).getId(),
            "eventType", GUIDFactory.newOperationIdGUID(0).getId(), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "x-request-id");

        logbookParameters1 = LogbookParametersFactory.newLogbookOperationParameters(
            eip1.getId(),
            "eventType", eip1.getId(), LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "x-request-id");
        logbookParameters1.putParameterValue(LogbookParameterName.eventDateTime, datestring1);
        logbookParameters2 = LogbookParametersFactory.newLogbookOperationParameters(
            eip2.getId(),
            "eventType", eip2.getId(), LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "x-request-id");
        logbookParameters2.putParameterValue(LogbookParameterName.eventDateTime, datestring2);
        logbookParameters3 = LogbookParametersFactory.newLogbookOperationParameters(
            eip3.getId(),
            "eventType", eip3.getId(), LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "x-request-id");
        logbookParameters3.putParameterValue(LogbookParameterName.eventDateTime, datestring3);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenCreateAndUpdate() throws Exception {
        logbookOperationsImpl = new LogbookOperationsImpl(mongoDbAccess);
        logbookOperationsImpl.create(logbookParametersStart);
        logbookOperationsImpl.update(logbookParametersAppend);

        try {
            logbookOperationsImpl.create(logbookParametersWrongStart);
            fail("Should failed");
        } catch (final LogbookAlreadyExistsException e) {}
        try {
            logbookOperationsImpl.update(logbookParametersWrongAppend);
            fail("Should failed");
        } catch (final LogbookNotFoundException e) {}
        try {
            logbookOperationsImpl.create(LogbookParametersFactory.newLogbookOperationParameters());
            fail("Should failed");
        } catch (final IllegalArgumentException e) {}
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenSelectWhenOperationNotExistThenThrowNotFoundException() throws Exception {
        logbookOperationsImpl = new LogbookOperationsImpl(mongoDbAccess);
        final Select select = new Select();
        select.setQuery(exists("mavar1"));
        logbookOperationsImpl.select(JsonHandler.getFromString(select.getFinalSelect().toString()));
    }

    @Test
    public void givenCreateAndSelect() throws Exception {
        logbookOperationsImpl = new LogbookOperationsImpl(mongoDbAccess);
        logbookOperationsImpl.create(logbookParameters1);
        logbookOperationsImpl.create(logbookParameters2);
        logbookOperationsImpl.create(logbookParameters3);


        final Select select = new Select();
        select.setQuery(new CompareQuery(QUERY.EQ, "evId", eip1.toString()));

        List<LogbookOperation> res1 = new ArrayList<LogbookOperation>();
        res1 = logbookOperationsImpl.select(select.getFinalSelect());
        assertNotNull(res1);
        assertTrue(res1.get(0).containsValue(eip1.getId()));

        List<LogbookOperation> res2 = new ArrayList<LogbookOperation>();
        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "eventType"));
        res2 = logbookOperationsImpl.select(select.getFinalSelect());
        assertNotNull(res2);
        assertTrue(res2.get(0).containsValue(eip1.getId()));
        assertTrue(res2.get(1).containsValue(eip2.getId()));
        assertTrue(res2.get(2).containsValue(eip3.getId()));

        List<LogbookOperation> res3 = new ArrayList<LogbookOperation>();
        select.addOrderByDescFilter("evDateTime");
        res3 = logbookOperationsImpl.select(select.getFinalSelect());

        assertTrue(res3.get(0).containsValue("2016-12-12"));
        assertTrue(res3.get(1).containsValue("2015-01-01"));
        assertTrue(res3.get(2).containsValue("1990-10-01"));
    }

}
