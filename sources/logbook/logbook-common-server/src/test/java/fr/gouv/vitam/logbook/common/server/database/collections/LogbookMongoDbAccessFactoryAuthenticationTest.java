package fr.gouv.vitam.logbook.common.server.database.collections;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

// FIXME: 30/01/19 "Already tested in @see MongoDbAccessMetadataFactoryTest. This test should be reactivated when mongo docker with an authenticated user is used"
@Ignore("Already tested in @see MongoDbAccessMetadataFactoryTest. This test should be reactivated when mongo docker with an authenticated user is used")
public class LogbookMongoDbAccessFactoryAuthenticationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    static LogbookMongoDbAccessImpl mongoDbAccess;

    private static final Integer TENANT_ID = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);
    private static final String user = "user-logbook";
    private static final String pwd = "user-logbook";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), TENANT_ID);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LogbookCollections.afterTestClass(true, TENANT_ID);
        VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void testCreateLogbook() throws LogbookDatabaseException, LogbookAlreadyExistsException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // mongo
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        // es
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));

        LogbookConfiguration config =
            new LogbookConfiguration(nodes, mongoRule.getMongoDatabase().getName(), ElasticsearchRule.VITAM_CLUSTER,
                esNodes, true, user, pwd);
        VitamConfiguration.setTenants(tenantList);
        new LogbookMongoDbAccessFactory();
        mongoDbAccess = LogbookMongoDbAccessFactory.create(config, Collections::emptyList);
        assertNotNull(mongoDbAccess);
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        for (final LogbookParameterName name : LogbookParameterName.values()) {
            if (LogbookParameterName.eventDateTime.equals(name)) {
                parameters.putParameterValue(name, LocalDateUtil.now().toString());
            } else {
                parameters.putParameterValue(name,
                    GUIDFactory.newEventGUID(0).getId());
            }
        }
        mongoDbAccess.createLogbookOperation(parameters);
        mongoDbAccess.close();
    }

}
