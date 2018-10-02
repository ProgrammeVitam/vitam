/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.metadata.migration.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.metadata.rest.MetadataMigrationAdminResource;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.OkHttpClient;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.assertj.core.util.Files;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration test of metadata migration services.
 */
public class MetadataMigrationIT {

    private static final String METADATA_CONF = "integration-metadata-migration/metadata.conf";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            MetadataCollections.UNIT.getName(), MetadataCollections.OBJECTGROUP.getName());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName());

    private static MetadataMain metadataMain;

    private static final int PORT_ADMIN_METADATA = 28098;
    private static final int PORT_SERVICE_METADATA = 8098;

    private static final String METADATA_URL = "http://localhost:" + PORT_ADMIN_METADATA;

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    private MetadataAdminMigrationService metadataAdminMigrationService;
    private MongoCollection<Unit> unitCollection;
    private MongoCollection<ObjectGroup> ogCollection;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        // launch metadata
        SystemPropertyUtil.set("jetty.metadata.port", Integer.toString(PORT_SERVICE_METADATA));
        SystemPropertyUtil.set("jetty.metadata.admin", Integer.toString(PORT_ADMIN_METADATA));
        final File metadataConfig = PropertiesUtils.findFile(METADATA_CONF);
        final MetaDataConfiguration realMetadataConfig =
            PropertiesUtils.readYaml(metadataConfig, MetaDataConfiguration.class);
        realMetadataConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realMetadataConfig.setDbName(mongoRule.getMongoDatabase().getName());

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));
        realMetadataConfig.setElasticsearchNodes(nodesEs);
        realMetadataConfig.setClusterName(elasticsearchRule.getClusterName());

        PropertiesUtils.writeYaml(metadataConfig, realMetadataConfig);

        metadataMain = new MetadataMain(metadataConfig.getAbsolutePath());
        metadataMain.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        if (metadataMain != null) {
            metadataMain.stop();
        }
        elasticsearchRule.afterClass();
    }

    @Before
    public void before() {

        // Metadata migration service interface - replace non existing client
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient).baseUrl(METADATA_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        metadataAdminMigrationService = retrofit.create(MetadataAdminMigrationService.class);

        unitCollection = MetadataCollections.UNIT.getCollection();
        ogCollection = MetadataCollections.OBJECTGROUP.getCollection();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    public void startDataMigration_failedAuthn() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration("BAD TOKEN").execute();
        assertThat(response.isSuccessful()).isFalse();

        Response<MetadataMigrationAdminResource.ResponseMessage>
            responseMigrationInProgress = metadataAdminMigrationService.checkDataMigrationInProgress().execute();
        assertThat(responseMigrationInProgress.code())
            .isEqualTo(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void startDataMigration_emptyDb() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration(getBasicAuthnToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    @Test
    public void startDataMigration_emptyDataSet() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration(getBasicAuthnToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    private void awaitTermination() throws InterruptedException, IOException {

        // Wait for 30 seconds max
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);

            Response<MetadataMigrationAdminResource.ResponseMessage> responseMigrationInProgress =
                metadataAdminMigrationService.checkDataMigrationInProgress().execute();
            if (responseMigrationInProgress.code() == javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode())
                return;
        }

        fail("Migration termination took too long");
    }

    @Test
    public void startDataMigration_fullDataSet() throws Exception {

        // Given
        importUnitDataSetFile("integration-metadata-migration/30UnitDataSet/R6UnitDataSet.json");
        importObjectGroupDataSetFile("integration-metadata-migration/15ObjectGroupDataSet/R6ObjectGroupDataSet.json");

        // When
        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration(getBasicAuthnToken()).execute();
        assertThat(response.isSuccessful()).isTrue();
        awaitTermination();

        // Then
        String expectedUnitDataSetFile = "integration-metadata-migration/30UnitDataSet/ExpectedR7UnitDataSet.json";
        assertDataSetEqualsExpectedFile(unitCollection, expectedUnitDataSetFile);

        String expectedOGDataSetFile =
            "integration-metadata-migration/15ObjectGroupDataSet/ExpectedR7ObjectGroupDataSet.json";
        assertDataSetEqualsExpectedFile(ogCollection, expectedOGDataSetFile);
    }

    private void importUnitDataSetFile(String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {

        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            unitCollection.insertOne(new Unit(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private void importObjectGroupDataSetFile(String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {

        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            ogCollection.insertOne(new ObjectGroup(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private <T> void assertDataSetEqualsExpectedFile(MongoCollection<T> mongoCollection, String expectedDataSetFile)
            throws InvalidParseOperationException, IOException, XmlPullParserException {

        ArrayNode unitDataSet = dumpDataSet(mongoCollection);

        String updatedUnitDataSet = JsonHandler.unprettyPrint(unitDataSet);
        String expectedUnitDataSet =
            JsonHandler.unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
                expectedDataSetFile)));

        if(updatedUnitDataSet.indexOf("\"_implementationVersion\":\"\"") == -1) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader("pom.xml"));
            expectedUnitDataSet = expectedUnitDataSet.replaceAll("implVersionValue", model.getParent().getVersion());
        } else {
            expectedUnitDataSet = expectedUnitDataSet.replaceAll("implVersionValue", "" );
        }

        JsonAssert.assertJsonEquals(expectedUnitDataSet, updatedUnitDataSet,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection) throws InvalidParseOperationException {

        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find()
            .sort(orderBy(ascending(MetadataDocument.ID)));

        for (T document : documents) {
            ObjectNode docObjectNode = (ObjectNode) JsonHandler.getFromString(JSON.serialize(document));

            // Replace _glpd with marker
            assertThat(docObjectNode.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
            docObjectNode.put(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, "#TIMESTAMP#");

            dataSet.add(docObjectNode);
        }

        return dataSet;
    }

    private String getBasicAuthnToken() {
        return "Basic " + Base64.getEncoder()
            .encodeToString((BASIC_AUTHN_USER + ":" + BASIC_AUTHN_PWD).getBytes(StandardCharsets.UTF_8));
    }

    public interface MetadataAdminMigrationService {

        @POST("/metadata/v1/migration")
        Call<MetadataMigrationAdminResource.ResponseMessage> startDataMigration(
            @Header("Authorization") String basicAuthnToken);

        @GET("/metadata/v1/migration/status")
        Call<MetadataMigrationAdminResource.ResponseMessage> checkDataMigrationInProgress();
    }
}
