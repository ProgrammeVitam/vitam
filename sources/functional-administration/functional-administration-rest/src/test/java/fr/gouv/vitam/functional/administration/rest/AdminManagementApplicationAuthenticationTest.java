package fr.gouv.vitam.functional.administration.rest;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class AdminManagementApplicationAuthenticationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-auth-test.conf";
    private static File adminConfigFile;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))));

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollections.afterTestClass(true);
    }

    @Test
    public void testApplicationLaunch() throws IOException, VitamException {
        try {
            new AdminManagementMain(adminConfigFile.getAbsolutePath());
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

}
