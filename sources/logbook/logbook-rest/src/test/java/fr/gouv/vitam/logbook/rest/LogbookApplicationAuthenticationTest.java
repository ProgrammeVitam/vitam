package fr.gouv.vitam.logbook.rest;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LogbookApplicationAuthenticationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(VitamCollection.getMongoClientOptions(), "vitam-test");

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String LOGBOOK_CONF = "logbook-auth-test.conf";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static File logbook;
    private static LogbookConfiguration realLogbook;

    private static String configurationFile;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
                new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                        Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), 0, 1);

        logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realLogbook.getElasticsearchNodes().get(0).setTcpPort(ElasticsearchRule.TCP_PORT);

        File file = temporaryFolder.newFile();
        configurationFile = file.getAbsolutePath();
        PropertiesUtils.writeYaml(file, realLogbook);
    }

    @AfterClass
    public static void tearDownAfterClass() throws IOException, VitamException {
        LogbookCollections.afterTestClass(new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))),true, 0, 1);
        VitamClientFactory.resetConnections();
    }

    @Test
    public void testApplicationLaunch() {
        new LogbookMain(configurationFile);
    }

}
