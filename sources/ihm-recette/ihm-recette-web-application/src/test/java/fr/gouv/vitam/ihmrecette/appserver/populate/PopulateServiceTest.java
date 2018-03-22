package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.google.common.collect.Lists;
import com.mongodb.Block;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.mongo.MongoRule;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), "metadata",
        Arrays.stream(VitamDataType.values()).map(VitamDataType::getCollectionName).toArray(String[]::new));


    @Rule
    public MongoRule logbookMongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), "logbook",
        Arrays.stream(VitamDataType.values()).map(VitamDataType::getCollectionName).toArray(String[]::new));

    private PopulateService populateService;
    private MetadataRepository metadataRepository;
    private MasterdataRepository masterdataRepository;
    private LogbookRepository logbookRepository;
    private TransportClient client;

    @Before
    public void setUp() throws Exception {
        int tcpPort = JunitHelper.getInstance().findAvailablePort();
        int httPort = JunitHelper.getInstance().findAvailablePort();
        String clusterName = "elasticsearch-data";
        JunitHelper.startElasticsearchForTest(temporaryFolder, clusterName, tcpPort, httPort);
        Settings settings = ElasticsearchAccess.getSettings(clusterName);

        client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), tcpPort));

        this.metadataRepository = new MetadataRepository(mongoRule.getMongoDatabase(), client);
        this.masterdataRepository = new MasterdataRepository(mongoRule.getMongoDatabase(), client);
        this.logbookRepository = new LogbookRepository(logbookMongoRule.getMongoDatabase());
        UnitGraph unitGraph = new UnitGraph(metadataRepository);
        populateService = new PopulateService(metadataRepository, masterdataRepository, logbookRepository, unitGraph, 4);
    }

    @Test
    public void should_populate_mongo_and_es() throws Exception {
        // Given
        PopulateModel populateModel = new PopulateModel();
        populateModel.setBulkSize(1000);
        populateModel.setNumberOfUnit(10);
        populateModel.setRootId("1234");
        populateModel.setSp("vitam");
        populateModel.setTenant(0);
        populateModel.setWithGots(true);
        populateModel.setWithRules(true);
        populateModel.setObjectSize(1024);
        Map<String, Integer> ruleMap = new HashMap<>();
        ruleMap.put("STR-00059", 100);
        ruleMap.put("ACC-000111", 20);
        populateModel.setRuleTemplatePercent(ruleMap);

        UnitModel unitModel = new UnitModel(2, "default");

        DescriptiveMetadataModel content = new DescriptiveMetadataModel();
        content.setTitle("1234");

        unitModel.setDescriptiveMetadataModel(content);
        unitModel.setId("1234");

        metadataRepository.store(0, Lists.newArrayList(new UnitGotModel(unitModel)), true, true);

        // When
        populateService.populateVitam(populateModel);

        // Then
        int i = 0;
        while (populateService.inProgress() && i < 100) {
            Thread.sleep(100L);
            i += 1;
        }

        int[] idx = {0};
        int portMongo = MongoRule.getDataBasePort();
        assertThat(mongoRule.getMongoCollection(VitamDataType.UNIT.getCollectionName()).count()).isEqualTo(11);
        Bson filter = Filters.eq("_mgt.StorageRule.Rules.Rule", "STR-00059");
        assertThat(mongoRule.getMongoCollection(VitamDataType.UNIT.getCollectionName()).count(filter)).isEqualTo(10);
        assertThat(mongoRule.getMongoCollection(VitamDataType.AGENCIES.getCollectionName()).count()).isEqualTo(1);
        assertThat(mongoRule.getMongoCollection(VitamDataType.ACCESS_CONTRACT.getCollectionName()).count()).isEqualTo(1);
        assertThat(mongoRule.getMongoCollection(VitamDataType.RULES.getCollectionName()).count()).isEqualTo(2);
        mongoRule.getMongoCollection(VitamDataType.UNIT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) document -> {
                assertThat(document.getString("Title").equals("Title: " + (idx[0]++)));
                assertThat(!document.get("_up", List.class).contains("1234"));
                assertThat(document.get("_us", List.class).size() == 1);
                assertThat(document.get("_sps", List.class).size() == 1);
                assertThat(document.get("_uds", Document.class).getInteger("1234").equals(1));
            });

        int[] jdx = {0};
        assertThat(mongoRule.getMongoCollection(VitamDataType.GOT.getCollectionName()).count()).isEqualTo(10);
        mongoRule.getMongoCollection(VitamDataType.GOT.getCollectionName()).find().
            forEach((Block<? super Document>) document -> {
                assertThat(document.get("FileInfo", Document.class).
                    getString("Filename").equals("Filename: " + (jdx[0]++)));
                assertThat(document.get("_sps", List.class).size() == 1);
                assertThat(!document.get("_up", List.class).isEmpty());
            });

    }


    @Test
    public void should_populate_logbook_collections() throws Exception {
        // Given
        PopulateModel populateModel = new PopulateModel();
        populateModel.setBulkSize(1000);
        populateModel.setNumberOfUnit(10);
        populateModel.setRootId("1234");
        populateModel.setSp("vitam");
        populateModel.setTenant(0);
        populateModel.setWithGots(true);
        populateModel.setWithRules(true);
        populateModel.setObjectSize(1024);
        populateModel.setWithLFCGots(true);
        populateModel.setWithLFCUnits(true);
        Map<String, Integer> ruleMap = new HashMap<>();
        populateModel.setRuleTemplatePercent(ruleMap);
        populateModel.setLFCUnitsEventsSize(10);
        populateModel.setLFCGotsEventsSize(5);

        UnitModel unitModel = new UnitModel(2, "default");
        ObjectGroupModel got = new ObjectGroupModel();
        DescriptiveMetadataModel content = new DescriptiveMetadataModel();
        content.setTitle("1234");
        unitModel.setDescriptiveMetadataModel(content);
        unitModel.setId("1234");
        UnitGotModel unitGotModel = new UnitGotModel(unitModel, got);
        LogbookLifecycle logbookLifecycle = new LogbookLifecycle();
        unitGotModel.setLogbookLifeCycleObjectGroup(logbookLifecycle);
        unitGotModel.setLogbookLifecycleUnit(logbookLifecycle);

        metadataRepository.store(0, Lists.newArrayList(new UnitGotModel(unitModel)), true, true);

        populateService.populateVitam(populateModel);

        // Then
        int i = 0;
        while (populateService.inProgress() && i < 100) {
            Thread.sleep(100L);
            i += 1;
        }

        assertThat(logbookMongoRule.getMongoCollection(VitamDataType.LFC_UNIT.getCollectionName()).count())
            .isEqualTo(10);
        assertThat(logbookMongoRule.getMongoCollection(VitamDataType.LFC_GOT.getCollectionName()).count())
            .isEqualTo(10);
        logbookMongoRule.getMongoCollection(VitamDataType.LFC_UNIT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) doc -> {
                assertThat(doc.getInteger("_tenant").equals(0)).isTrue();
                assertThat(doc.getString("evType").equals("LFC.LFC_CREATION")).isTrue();
                assertThat(doc.get("events", List.class).size() == 10).isTrue();
            });
        logbookMongoRule.getMongoCollection(VitamDataType.LFC_GOT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) doc -> {
                assertThat(doc.getInteger("_tenant").equals(0)).isTrue();
                assertThat(doc.getString("evType").equals("LFC.LFC_CREATION")).isTrue();
                assertThat(doc.get("events", List.class).size() == 5).isTrue();
            });

    }

}
