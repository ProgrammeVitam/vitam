package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.google.common.collect.Lists;
import com.mongodb.Block;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.bson.Document;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), "metadata",
            Arrays.stream(MetadataType.values()).map(MetadataType::getCollectionName).toArray(String[]::new));

    private PopulateService populateService;
    private MetadataRepository metadataRepository;
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
        UnitGraph unitGraph = new UnitGraph(metadataRepository);
        populateService = new PopulateService(metadataRepository, new DescriptiveMetadataGenerator(), unitGraph);
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

        UnitModel unitModel = new UnitModel();

        DescriptiveMetadataModel content = new DescriptiveMetadataModel();
        content.setTitle("1234");

        unitModel.setDescriptiveMetadataModel(content);
        unitModel.setId("1234");

        metadataRepository.store(0, Lists.newArrayList(new UnitGotModel(unitModel)));

        // When
        populateService.populateVitam(populateModel);

        // Then
        int i = 0;
        while (populateService.inProgress() && i < 100) {
            Thread.sleep(100L);
            i+=1;
        }

        int[] idx = { 0 };
        assertThat(mongoRule.getMongoCollection(MetadataType.UNIT.getCollectionName()).count()).isEqualTo(11);
        mongoRule.getMongoCollection(MetadataType.UNIT.getCollectionName()).find().skip(1).
            forEach((Block<? super Document>) document -> {
                assertThat(document.getString("Title").equals("Title: " + (idx[0]++)));
                assertThat(!document.getString("_up").equals("1234"));
                assertThat(document.get("_us", List.class).size() == 1);
                assertThat(document.get("_sps", List.class).size() == 1);
                assertThat(document.get("_uds", Document.class).getInteger("1234").equals(1));
        });

        int[] jdx = { 0 };
        assertThat(mongoRule.getMongoCollection(MetadataType.GOT.getCollectionName()).count()).isEqualTo(10);
        mongoRule.getMongoCollection(MetadataType.GOT.getCollectionName()).find().
            forEach((Block<? super Document>) document -> {
                assertThat(document.get("FileInfo", Document.class).
                        getString("Filename").equals("Filename: " + (jdx[0]++)));
                assertThat(document.get("_sps", List.class).size() == 1);
                assertThat(!document.getString("_up").isEmpty());
        });
        
    }

}