package fr.gouv.vitam.functional.administration.common.server;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import org.assertj.core.util.Lists;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

public class MongoDbAccessAdminFactoryTest {
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(), "Vitam-Test");


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoRule.handleAfter();
    }

    @Test
    public void testCreateAdmin() {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        MongoDbAccessAdminImpl mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName(), true, "user", "pwd"));
        assertNotNull(mongoDbAccess);
        assertEquals("Vitam-Test", mongoDbAccess.getMongoDatabase().getName());
        mongoDbAccess.close();
    }
}
