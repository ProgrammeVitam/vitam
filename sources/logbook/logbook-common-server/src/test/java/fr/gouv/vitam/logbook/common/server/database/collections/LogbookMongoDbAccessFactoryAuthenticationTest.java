package fr.gouv.vitam.logbook.common.server.database.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class LogbookMongoDbAccessFactoryAuthenticationTest {
    private static final String DATABASE_HOST = "localhost";
    static LogbookMongoDbAccessImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-logbook";
    private static final String user = "user-logbook";
    private static final String pwd = "user-logbook";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();

        // Starting the embedded services within temporary dir
        mongo = new MongoEmbeddedService(
            DATABASE_HOST + ":" + port, databaseName, user, pwd, "localreplica");
        mongo.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongo.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testCreateLogbook() throws LogbookDatabaseException, LogbookAlreadyExistsException {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        new LogbookMongoDbAccessFactory();
        mongoDbAccess = LogbookMongoDbAccessFactory.create(
            new DbConfigurationImpl(nodes, databaseName, true, user, pwd));
        assertNotNull(mongoDbAccess);
        assertEquals("db-logbook", mongoDbAccess.getMongoDatabase().getName());
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            parameters.putParameterValue(name,
                GUIDFactory.newEventGUID(0).getId());
        }
        mongoDbAccess.createLogbookOperation(parameters);
        mongoDbAccess.close();
    }

}
