package fr.gouv.vitam.functional.administration.common.server;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class MongoDbAccessAdminFactoryTest {
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), "vitam-test");

    private static MongoDbAccessAdminImpl mongoDbAccess;

    @Test
    public void testCreateAdmin() throws IOException, VitamException {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName(), true, "user", "pwd"));

        assertNotNull(mongoDbAccess);
        assertEquals("vitam-test", mongoDbAccess.getMongoDatabase().getName());

        mongoDbAccess.close();
    }
}
