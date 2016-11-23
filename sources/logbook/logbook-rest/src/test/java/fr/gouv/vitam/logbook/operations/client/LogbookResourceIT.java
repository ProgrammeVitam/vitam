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
package fr.gouv.vitam.logbook.operations.client;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.client2.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server2.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.logbook.rest.LogbookConfiguration;

public class LogbookResourceIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResourceIT.class);

    private static final String LOGBOOK_CONF = "logbook-test.conf";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String DATABASE_HOST = "localhost";
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;
    private static final String REST_URI = "/logbook/v1";
    private static final String SERVER_HOST = "localhost";
    private static JunitHelper junitHelper;
    private static int databasePort;
    private static int serverPort;
    private static LogbookApplication application;
    private static final int NB_TEST = 100;

    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersWrongStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersWrongAppend;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();


        databasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());

        mongod = mongodExecutable.start();
        serverPort = junitHelper.findAvailablePort();

        try {
            JunitHelper.setJettyPortSystemProperty(serverPort);
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            List<MongoDbNode> nodes = new ArrayList<MongoDbNode>();
            nodes.add(new MongoDbNode(DATABASE_HOST, databasePort));
            logbookConf.setMongoDbNodes(nodes).setDbName("vitam-test");
            logbookConf.setJettyConfig(JETTY_CONFIG);
            logbookConf.setP12LogbookFile("tsa.p12");
            logbookConf.setP12LogbookPassword("1234");
            application = new LogbookApplication(logbookConf);
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();

            RestAssured.port = serverPort;
            RestAssured.basePath = REST_URI;
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl(DATABASE_HOST, serverPort));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl(DATABASE_HOST, serverPort));
        LOGGER.debug("Initialize client: " + DATABASE_HOST + ":" + serverPort);


    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
        junitHelper.releasePort(serverPort);
    }


    @Test
    public final void testOperation() throws LogbookClientException, VitamApplicationServerException {
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        try (final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookParametersStart);

            // Update OK
            client.update(logbookParametersAppend);

            // Create KO since already exists
            try {
                client.create(logbookParametersWrongStart);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }

            // Update KO since not found
            try {
                client.update(logbookParametersWrongAppend);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }
            // Create KO since Bad Request
            final LogbookOperationParameters empty = LogbookParametersFactory.newLogbookOperationParameters();
            empty.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                logbookParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess));
            try {
                client.create(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
            try {
                client.update(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        }
    }

    @Test
    public final void testOperationMultiple() throws LogbookClientException, VitamApplicationServerException {
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        try (final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookParametersStart);

            // Update multiple OK
            long start = System.nanoTime();
            int i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.update(logbookParametersAppend);
                }
            } catch (LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
            long stop = System.nanoTime();
            long start2 = System.nanoTime();
            i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.updateDelegate(logbookParametersAppend);
                }
                client.commitUpdateDelegate(eip.getId());
            } catch (LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
            long stop2 = System.nanoTime();
            LOGGER.warn("Multiple updates vs bulk updates: {} ms vs {} ms", (stop - start) / 1000000,
                (stop2 - start2) / 1000000);
            client.checkStatus();
        }
    }

    @Test
    public final void testLifeCycle() throws LogbookClientException, VitamApplicationServerException {
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookLcParametersStart = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);
        logbookLcParametersWrongStart = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersWrongAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", GUIDFactory.newEventGUID(0));

        try (final LogbookLifeCyclesClient client =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookLcParametersStart);

            // Update OK
            client.update(logbookLcParametersAppend);

            // Create KO since already exists
            try {
                client.create(logbookLcParametersWrongStart);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }

            // Update KO since not found
            try {
                client.update(logbookLcParametersWrongAppend);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }
            // Create KO since Bad Request
            final LogbookLifeCycleObjectGroupParameters empty =
                LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
            empty.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                logbookLcParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess));
            try {
                client.create(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
            try {
                client.update(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        }
    }

    @Test
    public final void testLifeCycleMultiple() throws LogbookClientException, VitamApplicationServerException {
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookLcParametersStart = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);
        logbookLcParametersWrongStart = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersWrongAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);

        try (final LogbookLifeCyclesClient client =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookLcParametersStart);

            // Update multiple OK
            int i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.update(logbookLcParametersAppend);
                }
            } catch (LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
        }
    }
}
