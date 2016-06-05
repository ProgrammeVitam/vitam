/**
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
 */
package fr.gouv.vitam.logbook.operations.core;

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
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

public class LogbookOperationsImplWithMongoTest {

    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12346;
    static MongoDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private LogbookOperationsImpl logbookOperationsImpl;
    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;

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
        final GUID eip = GUIDFactory.newOperationIdGUID(0);
        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip.getId(),
            "eventType", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.STARTED, "start ingest", "x-request-id");
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0).getId(),
            "eventType", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.OK, "end ingest", "x-request-id");
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip.getId(),
            "eventType", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.STARTED, "start ingest", "x-request-id");
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0).getId(),
            "eventType", GUIDFactory.newOperationIdGUID(0).getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.OK, "end ingest", "x-request-id");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
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

}
