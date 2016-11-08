package fr.gouv.vitam.functional.administration.common.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.configuration.MongoDbNode;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

public class MongoDbAccessAdminFactoryTest {
    private static final String DATABASE_HOST = "localhost";
    static MongoDbAccessAdminImpl mongoDbAccess;
    private static int port;
    private static JunitHelper junitHelper;
    private static MongoEmbeddedService mongo;
    private static final String databaseName = "db-functional-administration";
    private static final String user = "user-functional-administration";
    private static final String pwd = "user-functional-administration";

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
    public void testCreateAdmin(){
        List<MongoDbNode> nodes = new ArrayList<MongoDbNode>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, databaseName, true, user, pwd));
        assertNotNull(mongoDbAccess);
        assertEquals("db-functional-administration", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }
}
