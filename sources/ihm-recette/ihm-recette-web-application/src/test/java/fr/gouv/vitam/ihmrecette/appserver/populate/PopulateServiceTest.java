package fr.gouv.vitam.ihmrecette.appserver.populate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PopulateServiceTest {

    private static final String STORAGE_CONF_FILE = "storage.conf";

    public static final String PREFIX = GUIDFactory.newGUID().getId();
    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private PopulateService populateService;
    private MetadataRepository metadataRepository;
    private MasterdataRepository masterdataRepository;
    private LogbookRepository logbookRepository;

    @BeforeClass
    public static void beforeClass() {
        for (VitamDataType vitamDataType : VitamDataType.values()) {
            vitamDataType.setCollectionName(PREFIX + vitamDataType.getCollectionName());
            vitamDataType.setIndexName(PREFIX + vitamDataType.getIndexName());
            elasticsearchRule.addIndexToBePurged(vitamDataType.getIndexName());
            mongoRule.addCollectionToBePurged(vitamDataType.getCollectionName());
        }

    }

    @AfterClass
    public static void afterClass() {
        elasticsearchRule.deleteIndexes();
        mongoRule.handleAfterClass();
    }

    @Before
    public void setUp() throws Exception {

        StoragePopulateImpl storagePopulateService;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(STORAGE_CONF_FILE)) {
            final fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration
                configuration =
                PropertiesUtils.readYaml(yamlIS, fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration.class);
            storagePopulateService = new StoragePopulateImpl(configuration);
        }

        this.metadataRepository =
            new MetadataRepository(mongoRule.getMongoDatabase(), elasticsearchRule.getClient(), storagePopulateService);
        this.masterdataRepository =
            new MasterdataRepository(mongoRule.getMongoDatabase(), elasticsearchRule.getClient());
        this.logbookRepository = new LogbookRepository(mongoRule.getMongoDatabase());
        MetadataStorageService metadataStorageService =
            new MetadataStorageService(metadataRepository, logbookRepository, storagePopulateService);
        UnitGraph unitGraph = new UnitGraph(metadataRepository);
        populateService =
            new PopulateService(metadataRepository, masterdataRepository, logbookRepository, unitGraph, 4,
                metadataStorageService);
    }

    @After
    public void after() throws Exception {
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
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

        UnitModel unitModel = new UnitModel();
        unitModel.setStorageModel(new StorageModel(2, "default", Arrays.asList("offer1", "offer2")));

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
        assertThat(mongoRule.getMongoCollection(VitamDataType.ACCESS_CONTRACT.getCollectionName()).count())
            .isEqualTo(1);
        assertThat(mongoRule.getMongoCollection(VitamDataType.RULES.getCollectionName()).count()).isEqualTo(2);
        mongoRule.getMongoCollection(VitamDataType.UNIT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) document -> {
                assertThat(document.getString("Title").equals("Title: " + (idx[0]++)));
                assertThat(!document.get("_up", List.class).contains("1234"));
                assertThat(document.get("_us", List.class).size() == 1);
                assertThat(document.get("_sps", List.class).size() == 1);
                assertThat(document.get("_uds", Document.class).get("1", List.class)).containsExactly("1234");
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

        UnitModel unitModel = new UnitModel();
        unitModel.setStorageModel(new StorageModel(2, "default", Arrays.asList("offer1", "offer2")));

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

        assertThat(mongoRule.getMongoCollection(VitamDataType.LFC_UNIT.getCollectionName()).count())
            .isEqualTo(10);
        assertThat(mongoRule.getMongoCollection(VitamDataType.LFC_GOT.getCollectionName()).count())
            .isEqualTo(10);
        mongoRule.getMongoCollection(VitamDataType.LFC_UNIT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) doc -> {
                assertThat(doc.getInteger("_tenant").equals(0)).isTrue();
                assertThat(doc.getString("evType").equals("LFC.LFC_CREATION")).isTrue();
                assertThat(doc.get("events", List.class).size() == 10).isTrue();
            });
        mongoRule.getMongoCollection(VitamDataType.LFC_GOT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) doc -> {
                assertThat(doc.getInteger("_tenant").equals(0)).isTrue();
                assertThat(doc.getString("evType").equals("LFC.LFC_CREATION")).isTrue();
                assertThat(doc.get("events", List.class).size() == 5).isTrue();
            });

    }

}
