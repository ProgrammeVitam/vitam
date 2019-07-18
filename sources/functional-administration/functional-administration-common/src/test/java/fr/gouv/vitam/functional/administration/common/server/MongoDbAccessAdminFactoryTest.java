package fr.gouv.vitam.functional.administration.common.server;

import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MongoDbAccessAdminFactoryTest {
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions());

    private static MongoDbAccessAdminImpl mongoDbAccess;

    @Test
    public void testCreateAdmin() {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName(), true, "user", "pwd"), Collections::emptyList);

        assertNotNull(mongoDbAccess);
        assertEquals(MongoRule.VITAM_DB, mongoDbAccess.getMongoDatabase().getName());

        mongoDbAccess.close();
    }
}
