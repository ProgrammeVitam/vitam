/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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

package fr.gouv.vitam.audit.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
import org.bson.BsonTimestamp;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TMP_FILE_NAME_FOR_SHARDS_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DataConsistencyAuditIT extends VitamRuleRunner {

    public static final String INCOHERANT_DATA_SIZE = "IncoherantDataSize";
    public static final String REQUEST_ID = "requestId";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Integer TENANT_ID = 0;
    private static final String OPI = "aecaaaaaaceaaaababbxoalviwghooqaaaaq";
    private static final String UNIT_RESOURCE_FILE = "database/unit.json";
    private static final String OBJECTGROUP_RESOURCE_FILE = "database/got.json";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        DataConsistencyAuditIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(MetadataMain.class, AdminManagementMain.class, LogbookMain.class, WorkspaceMain.class)
    );

    private static MetadataAuditResource metadataAuditResource;

    private static DataLoader dataLoader = new DataLoader("integration-ingest-internal");

    private static final String AUDIT_CONTAINER_NAME = "dataConsistencyAuditContainer";
    private static final String SHARD_KEY = "shard0:127.0.0.1:27017";

    private static final int PORT_SERVICE_METADATA_ADMIN = 28098;
    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA_ADMIN;

    private enum PopulateMode {
        FULL,
        ES_ONLY,
        MONGO_ONLY,
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        dataLoader.prepareData();
        //
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(METADATA_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        metadataAuditResource = retrofit.create(DataConsistencyAuditIT.MetadataAuditResource.class);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() throws Exception {
        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
        // clean
        MetadataCollections.UNIT.getCollection().deleteMany(Filters.ne(MetadataDocument.OPI, null));
        MetadataCollections.OBJECTGROUP.getCollection().deleteMany(Filters.ne(MetadataDocument.OPI, null));
        MetadataCollections.UNIT.getEsClient().deleteIndexByAliasForTesting(MetadataCollections.UNIT, TENANT_ID);
        MetadataCollections.OBJECTGROUP.getEsClient()
            .deleteIndexByAliasForTesting(MetadataCollections.OBJECTGROUP, TENANT_ID);

        // create workspace file
        WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance();
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(AUDIT_CONTAINER_NAME)) {
                workspaceClient.createContainer(AUDIT_CONTAINER_NAME);
            }
            if (
                !workspaceClient.isExistingObject(
                    AUDIT_CONTAINER_NAME,
                    TMP_FILE_NAME_FOR_SHARDS_CONFIG + JSON_EXTENSION
                )
            ) {
                File file = folder.newFile();
                Map<String, BsonTimestamp> stringBsonTimestampMap = Map.of(
                    SHARD_KEY,
                    new BsonTimestamp((int) (new Date().getTime() / 1000L), 1)
                );
                JsonHandler.writeAsFile(JsonHandler.toJsonNode(stringBsonTimestampMap), file);
                workspaceClient.putObject(AUDIT_CONTAINER_NAME, TMP_FILE_NAME_FOR_SHARDS_CONFIG + JSON_EXTENSION, file);
            }
        }
    }

    @After
    public void after() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void given_coherent_data_shoud_run_audit_without_error() throws InvalidParseOperationException, IOException {
        populateData(PopulateMode.FULL);

        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        JsonNode result = metadataAuditResource.tryRunAuditDataConsistencyMongoEs().execute().body();
        LinkedHashMap<String, JsonNode> responseResults = JsonHandler.getFromJsonNode(result, new TypeReference<>() {});
        assertEquals(0, responseResults.get(INCOHERANT_DATA_SIZE).asInt());
        JsonNode logbookJsonNode = VitamTestHelper.findLogbook(responseResults.get(REQUEST_ID).asText());
        LogbookOperation logbookOperation = JsonHandler.getFromJsonNode(
            logbookJsonNode.get(RequestResponseOK.TAG_RESULTS),
            LogbookOperation.class
        );
        assertEquals(StatusCode.OK.name(), Iterables.getLast(logbookOperation.getEvents()).getOutcome());
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_data_on_elasticsearch_when_run_audit_then_warning()
        throws InvalidParseOperationException, IOException {
        populateData(PopulateMode.MONGO_ONLY);

        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        JsonNode result = metadataAuditResource.tryRunAuditDataConsistencyMongoEs().execute().body();
        LinkedHashMap<String, JsonNode> responseResults = JsonHandler.getFromJsonNode(result, new TypeReference<>() {});
        assertEquals(5, responseResults.get(INCOHERANT_DATA_SIZE).asInt());
        JsonNode logbookJsonNode = VitamTestHelper.findLogbook(responseResults.get(REQUEST_ID).asText());
        LogbookOperation logbookOperation = JsonHandler.getFromJsonNode(
            logbookJsonNode.get(RequestResponseOK.TAG_RESULTS),
            LogbookOperation.class
        );
        assertEquals(StatusCode.WARNING.name(), Iterables.getLast(logbookOperation.getEvents()).getOutcome());
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_data_on_mongodb_when_run_audit_then_warning()
        throws InvalidParseOperationException, IOException {
        populateData(PopulateMode.FULL);

        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        MetadataCollections.UNIT.getCollection().deleteMany(Filters.eq(MetadataDocument.OPI, OPI));
        MetadataCollections.OBJECTGROUP.getCollection().deleteMany(Filters.eq(MetadataDocument.OPI, OPI));

        JsonNode result = metadataAuditResource.tryRunAuditDataConsistencyMongoEs().execute().body();
        LinkedHashMap<String, JsonNode> responseResults = JsonHandler.getFromJsonNode(result, new TypeReference<>() {});
        assertEquals(5, responseResults.get(INCOHERANT_DATA_SIZE).asInt());
        JsonNode logbookJsonNode = VitamTestHelper.findLogbook(responseResults.get(REQUEST_ID).asText());
        LogbookOperation logbookOperation = JsonHandler.getFromJsonNode(
            logbookJsonNode.get(RequestResponseOK.TAG_RESULTS),
            LogbookOperation.class
        );
        assertEquals(StatusCode.WARNING.name(), Iterables.getLast(logbookOperation.getEvents()).getOutcome());
    }

    private void populateData(PopulateMode mode) {
        try {
            List<Unit> units = getMetadatas(UNIT_RESOURCE_FILE, new TypeReference<>() {});
            List<ObjectGroup> objectGroups = getMetadatas(OBJECTGROUP_RESOURCE_FILE, new TypeReference<>() {});

            if (mode.equals(PopulateMode.FULL) || mode.equals(PopulateMode.MONGO_ONLY)) {
                MetadataCollections.UNIT.getCollection().insertMany(units);
                MetadataCollections.OBJECTGROUP.getCollection().insertMany(objectGroups);
            }

            if (mode.equals(PopulateMode.FULL) || mode.equals(PopulateMode.ES_ONLY)) {
                MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_ID, units);
                MetadataCollections.OBJECTGROUP.getEsClient()
                    .insertFullDocuments(MetadataCollections.OBJECTGROUP, TENANT_ID, objectGroups);
            }
        } catch (
            InvalidParseOperationException
            | InvalidFormatException
            | FileNotFoundException
            | MetaDataExecutionException e
        ) {
            fail("Cannot populate data");
        }
    }

    private <T extends MetadataDocument<T>> List<T> getMetadatas(String resourcesFile, TypeReference<List<T>> type)
        throws InvalidParseOperationException, InvalidFormatException, FileNotFoundException {
        InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(resourcesFile);
        List<T> metadataDocuments = JsonHandler.getFromInputStreamAsTypeReference(resourceAsStream, type);

        metadataDocuments.forEach(t -> t.replace(VitamDocument.ID, GUIDFactory.newUnitGUID(TENANT_ID).getId()));
        return metadataDocuments;
    }

    public interface MetadataAuditResource {
        @GET("/v1/auditDataConsistency")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<JsonNode> tryRunAuditDataConsistencyMongoEs();
    }
}
