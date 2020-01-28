/*
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
 */
package fr.gouv.vitam.metadata.migration.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.metadata.rest.MetadataMigrationAdminResource;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.IteratorUtils;
import org.assertj.core.api.Assertions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertEquals;

/**
 * Integration test of metadata migration services.
 */
public class MigrationIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(MigrationIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkspaceMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                AdminManagementMain.class
            ));


    private static final String METADATA_URL = "http://localhost:" + VitamServerRunner.PORT_SERVICE_METADATA_ADMIN;
    private static final String ADMIN_MANAGEMENT_URL =
        "http://localhost:" + VitamServerRunner.PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN;

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    private MetadataAdminMigrationService metadataAdminMigrationService;
    private AccessionRegisterAdminMigrationService accessionRegisterAdminMigrationService;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        MetaDataClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", VitamServerRunner.PORT_SERVICE_METADATA));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void before() {
        // Metadata migration service interface - replace non existing client
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit_metadata =
            new Retrofit.Builder().client(okHttpClient).baseUrl(METADATA_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();

        Retrofit retrofit_admin_management =
            new Retrofit.Builder().client(okHttpClient).baseUrl(ADMIN_MANAGEMENT_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        metadataAdminMigrationService = retrofit_metadata.create(MetadataAdminMigrationService.class);
        accessionRegisterAdminMigrationService =
            retrofit_admin_management.create(AccessionRegisterAdminMigrationService.class);
    }

    @After
    public void tearDown() throws Exception {
        runAfter();
        handleAfterClass(0, 1);
    }

    @Test
    public void testSerializeDocumentForMongoDriverMigration() throws Exception {

        // Given
        String initialDocument = PropertiesUtils.getResourceAsString("serialize-test.json");
        MetadataCollections.UNIT.getCollection()
            .insertOne(Document.parse(initialDocument));

        Bson eq = Filters.eq("_id", "MyUniqueDocId");
        Document doc = (Document) MetadataCollections.UNIT.getCollection().find(eq).first();

        // When
        String serializedDoc = BsonHelper.stringify(doc);
        String legacySerializedDoc = JSON.serialize(doc);

        // Then
        // Delete document from db
        MetadataCollections.UNIT.getCollection().deleteOne(eq);

        JsonAssert.assertJsonEquals(serializedDoc, initialDocument);
        JsonAssert.assertJsonEquals(legacySerializedDoc, initialDocument);
    }

    private static final String test = "{\"data\": 1}";

    @Test
    public void testBsonToStringMongoMigration() throws ParseException, IOException, InvalidParseOperationException {
        final Bson bson = BsonDocument.parse(test);
        assertEquals(test, MongoDbHelper.bsonToString(bson, false));
        assertEquals("{\n  \"data\": 1\n}", MongoDbHelper.bsonToString(bson, true));

        FakeObjet fakeObjet = new FakeObjet();

        JsonWriterSettings writerSettingsRelaxed = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        JsonWriterSettings writerSettingsExtended = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
        JsonWriterSettings writerSettingsStrict = JsonWriterSettings.builder().outputMode(JsonMode.STRICT).build();

        final String serialize = JSON.serialize(fakeObjet);
        final String relaxed = fakeObjet.toJson(writerSettingsRelaxed);
        final String extended = fakeObjet.toJson(writerSettingsExtended);
        final String strict = fakeObjet.toJson(writerSettingsStrict);

        // RELAXED Mode
        JsonNode relaxedJson = JsonHandler.getFromString(relaxed);
        InputStream in = CanonicalJsonFormatter.serialize(relaxedJson);
        String str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":{\"$date\":\"1982-10-19T23:00:00Z\"},\"doubleVal\":-2.2,\"doubleVal1\":-2000000.2,\"floatVal\":2.200000047683716,\"intVal\":-2,\"longVal\":2,\"longVal1\":1000000000,\"nullVal\":null,\"strVal\":\"2\"}");

        // ToJson
        in = CanonicalJsonFormatter.serialize(JsonHandler.getFromString(fakeObjet.toJson()));
        str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":{\"$date\":403916400000},\"doubleVal\":-2.2,\"doubleVal1\":-2000000.2,\"floatVal\":2.200000047683716,\"intVal\":-2,\"longVal\":{\"$numberLong\":\"2\"},\"longVal1\":{\"$numberLong\":\"1000000000\"},\"nullVal\":null,\"strVal\":\"2\"}");

        // Jackson
        in = CanonicalJsonFormatter.serialize(JsonHandler.toJsonNode(fakeObjet));
        str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":\"1982-10-19T23:00:00.000+0000\",\"doubleVal\":-2.2,\"doubleVal1\":-2000000.2,\"floatVal\":2.2,\"intVal\":-2,\"longVal\":2,\"longVal1\":1000000000,\"strVal\":\"2\"}");

        // STRICT Mode
        JsonNode strictJson = JsonHandler.getFromString(strict);
        in = CanonicalJsonFormatter.serialize(strictJson);
        str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":{\"$date\":403916400000},\"doubleVal\":-2.2,\"doubleVal1\":-2000000.2,\"floatVal\":2.200000047683716,\"intVal\":-2,\"longVal\":{\"$numberLong\":\"2\"},\"longVal1\":{\"$numberLong\":\"1000000000\"},\"nullVal\":null,\"strVal\":\"2\"}");

        // Extended Mode
        JsonNode extendedJson = JsonHandler.getFromString(extended);
        in = CanonicalJsonFormatter.serialize(extendedJson);
        str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":{\"$date\":{\"$numberLong\":\"403916400000\"}},\"doubleVal\":{\"$numberDouble\":\"-2.2\"},\"doubleVal1\":{\"$numberDouble\":\"-2000000.2\"},\"floatVal\":{\"$numberDouble\":\"2.200000047683716\"},\"intVal\":{\"$numberInt\":\"-2\"},\"longVal\":{\"$numberLong\":\"2\"},\"longVal1\":{\"$numberLong\":\"1000000000\"},\"nullVal\":null,\"strVal\":\"2\"}");

        // Legacy Mode
        JsonNode legacyJson = JsonHandler.getFromString(serialize);
        in = CanonicalJsonFormatter.serialize(legacyJson);
        str = StringUtils.getStringFromInputStream(in);
        Assertions.assertThat(str).isEqualTo(
            "{\"dateStrVal\":\"1982-10-20T00:00:00+0100\",\"dateVal\":{\"$date\":\"1982-10-19T23:00:00.000Z\"},\"doubleVal\":-2.2,\"doubleVal1\":-2000000.2,\"floatVal\":2.2,\"intVal\":-2,\"longVal\":2,\"longVal1\":1000000000,\"nullVal\":null,\"strVal\":\"2\"}");


    }



    class FakeObjet extends Document {
        public FakeObjet() throws ParseException {
            append("floatVal", 2.2f);
            append("longVal", 2l);
            append("longVal1", 1_000_000_000l);
            append("intVal", -2);
            append("doubleVal", -2.2);
            append("doubleVal1", -2_000_000.2);
            append("strVal", "2");
            append("nullVal", null);
            append("dateVal", LocalDateUtil.getDate("1982-10-20"));
            append("dateStrVal", LocalDateUtil.getFormattedDate(LocalDateUtil.getDate("1982-10-20")));
        }
    }

    @Test
    public void startMetadataDataMigration_failedAuthn() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration("BAD TOKEN").execute();
        assertThat(response.isSuccessful()).isFalse();

        Response<MetadataMigrationAdminResource.ResponseMessage>
            responseMigrationInProgress = metadataAdminMigrationService.checkDataMigrationInProgress().execute();
        assertThat(responseMigrationInProgress.code())
            .isEqualTo(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void startMetadataDataMigration_emptyDb() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    @Test
    public void startMetadataDataMigration_emptyDataSet() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = metadataAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    @Test
    public void startMetadataDataMigration_fullDataSet() throws Exception {

        // Given
        importDataSetFile(MetadataCollections.UNIT.getCollection(),
            "integration-metadata-migration/30UnitDataSet/R6UnitDataSet.json");
        importDataSetFile(MetadataCollections.OBJECTGROUP.getCollection(),
            "integration-metadata-migration/15ObjectGroupDataSet/R6ObjectGroupDataSet.json");

        // When
        Response<MetadataMigrationAdminResource.ResponseMessage> response =
            metadataAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();
        awaitTermination();

        // Then
        String expectedUnitDataSetFile = "integration-metadata-migration/30UnitDataSet/ExpectedR7UnitDataSet.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitDataSetFile, true);

        String expectedOGDataSetFile =
            "integration-metadata-migration/15ObjectGroupDataSet/ExpectedR7ObjectGroupDataSet.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedOGDataSetFile, true);
    }


    @Test
    public void startAccessionRegisterMigration_failedAuthn() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = accessionRegisterAdminMigrationService.startDataMigration("BAD TOKEN").execute();
        assertThat(response.isSuccessful()).isFalse();

        Response<MetadataMigrationAdminResource.ResponseMessage>
            responseMigrationInProgress =
            accessionRegisterAdminMigrationService.checkDataMigrationInProgress().execute();
        assertThat(responseMigrationInProgress.code())
            .isEqualTo(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void startAccessionRegisterMigration_emptyDb() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = accessionRegisterAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    @Test
    public void startAccessionRegisterMigration_emptyDataSet() throws Exception {

        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = accessionRegisterAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();

        awaitTermination();
    }

    @Test
    @RunWithCustomExecutor
    public void startAccessionRegisterMigration_fullDataSet() throws Exception {
        // Given
        String accessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail.json";
        importDataSetFile(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection(),
            accessionRegisterDetailDataSetFile);

        String accessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary.json";
        importDataSetFile(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection(),
            accessionRegisterSummaryDataSetFile);

        // When
        Response<MetadataMigrationAdminResource.ResponseMessage>
            response = accessionRegisterAdminMigrationService.startDataMigration(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();
        awaitTermination();

        // Then
        String expectedAccessionRegisterDetailDataSetFile = "migration_r7_r8/accession_register_detail_EXPECTED.json";
        assertDataSetEqualsExpectedFile(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection(),
            expectedAccessionRegisterDetailDataSetFile, false);

        // Check persisted in ES
        SearchResponse search = elasticsearchRule.getClient()
            .prepareSearch(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase())
            .setQuery(QueryBuilders.matchAllQuery()).get();
        assertThat(search.getHits().getTotalHits()).isEqualTo(2);

        String expectedAccessionRegisterSummaryDataSetFile = "migration_r7_r8/accession_register_summary_EXPECTED.json";
        assertDataSetEqualsExpectedFile(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection(),
            expectedAccessionRegisterSummaryDataSetFile, false);

        // Check persisted in ES
        search = elasticsearchRule.getClient()
            .prepareSearch(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase())
            .setQuery(QueryBuilders.matchAllQuery()).get();
        assertThat(search.getHits().getTotalHits()).isEqualTo(2);


        // Check storage
        VitamThreadUtils.getVitamSession().setTenantId(0);
        RestoreBackupServiceImpl restoreBackupService = new RestoreBackupServiceImpl();
        Iterator<List<OfferLog>> listingIterator =
            restoreBackupService
                .getListing(VitamConfiguration.getDefaultStrategy(), DataCategory.ACCESSION_REGISTER_DETAIL, 0L, 100,
                    Order.ASC);
        List<OfferLog> listing =
            IteratorUtils.toList(listingIterator).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(listing).hasSize(2);
        assertThat(listing).extracting("Container", "FileName")
            .contains(
                tuple("0_accessionregisterdetail", "0_aehaaaaaaehdfg3uabrxcale57asp2qaaaaq.json"),
                tuple("0_accessionregisterdetail", "0_aehaaaaaaehdfg3uabrxcale57t4pqiaaaaq.json")
            );


        // When purge
        response = accessionRegisterAdminMigrationService.startDataPurge(getBasicAuthToken()).execute();
        assertThat(response.isSuccessful()).isTrue();
        awaitTermination();

        // Then Mongo and Elasticsearch purged
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments()).isEqualTo(0);
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().countDocuments()).isEqualTo(0);

        search = elasticsearchRule.getClient()
            .prepareSearch(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase())
            .setQuery(QueryBuilders.matchAllQuery()).get();
        assertThat(search.getHits().getTotalHits()).isEqualTo(0);

        search = elasticsearchRule.getClient()
            .prepareSearch(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase())
            .setQuery(QueryBuilders.matchAllQuery()).get();
        assertThat(search.getHits().getTotalHits()).isEqualTo(0);
    }


    private void awaitTermination() throws InterruptedException, IOException {

        // Wait for 60 seconds max
        for (int i = 0; i < 120; i++) {
            TimeUnit.MILLISECONDS.sleep(500);

            Response<MetadataMigrationAdminResource.ResponseMessage> responseMigrationInProgress =
                metadataAdminMigrationService.checkDataMigrationInProgress().execute();
            if (responseMigrationInProgress.code() == javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode())
                return;
        }

        fail("Migration termination took too long");
    }


    private void importDataSetFile(MongoCollection collection, String dataSetFile)
        throws FileNotFoundException, InvalidParseOperationException {

        InputStream inputDataSet = PropertiesUtils.getResourceAsStream(dataSetFile);
        ArrayNode jsonDataSet = (ArrayNode) JsonHandler.getFromInputStream(inputDataSet);
        for (JsonNode jsonNode : jsonDataSet) {
            collection.insertOne(Document.parse(JsonHandler.unprettyPrint(jsonNode)));
        }
    }

    private <T> void assertDataSetEqualsExpectedFile(MongoCollection<T> mongoCollection, String expectedDataSetFile,
        boolean replaceFields)
        throws InvalidParseOperationException, FileNotFoundException {

        ArrayNode unitDataSet = dumpDataSet(mongoCollection, replaceFields);

        String updatedUnitDataSet = JsonHandler.unprettyPrint(unitDataSet);
        String expectedUnitDataSet =
            JsonHandler.unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
                expectedDataSetFile)));

        JsonAssert.assertJsonEquals(expectedUnitDataSet, updatedUnitDataSet,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection, boolean replaceFields)
        throws InvalidParseOperationException {

        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find()
            .sort(orderBy(ascending(MetadataDocument.ID)));

        for (T document : documents) {
            ObjectNode docObjectNode = (ObjectNode) JsonHandler.getFromString(BsonHelper.stringify(document));
            if (replaceFields) {
                // Replace _glpd with marker
                assertThat(docObjectNode.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
                docObjectNode.put(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, "#TIMESTAMP#");
            }
            dataSet.add(docObjectNode);
        }

        return dataSet;
    }

    private String getBasicAuthToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface MetadataAdminMigrationService {

        @POST("/metadata/v1/migration")
        Call<MetadataMigrationAdminResource.ResponseMessage> startDataMigration(
            @Header("Authorization") String basicAuthToken);

        @GET("/metadata/v1/migration/status")
        Call<MetadataMigrationAdminResource.ResponseMessage> checkDataMigrationInProgress();
    }


    public interface AccessionRegisterAdminMigrationService {

        @POST("/adminmanagement/v1/migration/accessionregister/migrate")
        Call<MetadataMigrationAdminResource.ResponseMessage> startDataMigration(
            @Header("Authorization") String basicAuthToken);

        @POST("/adminmanagement/v1/migration/accessionregister/purge")
        Call<MetadataMigrationAdminResource.ResponseMessage> startDataPurge(
            @Header("Authorization") String basicAuthToken);


        @GET("/adminmanagement/v1/migration/accessionregister/status")
        Call<MetadataMigrationAdminResource.ResponseMessage> checkDataMigrationInProgress();
    }
}
