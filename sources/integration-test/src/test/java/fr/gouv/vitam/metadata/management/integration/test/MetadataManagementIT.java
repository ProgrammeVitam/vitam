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
package fr.gouv.vitam.metadata.management.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.rest.AccessInternalResourceImpl;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.configuration.ClassificationLevel;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.api.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the reconstruction of metadata. <br/>
 */
public class MetadataManagementIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        MetadataManagementIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            WorkerMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class
        )
    );

    private static final String unit_with_graph_0 = "integration-metadata-management/data/unit_with_graph_0.json";
    private static final String unit_with_graph_1 = "integration-metadata-management/data/unit_with_graph_1.json";
    private static final String unit_with_graph_2 = "integration-metadata-management/data/unit_with_graph_2.json";
    private static final String unit_with_graph_2_v2 = "integration-metadata-management/data/unit_with_graph_2_v2.json";
    private static final String unit_with_graph_3 = "integration-metadata-management/data/unit_with_graph_3.json";

    private static final String unit_with_graph_0_guid = "aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq";
    private static final String unit_with_graph_1_guid = "aeaqaaaaaahmtusqabktwaldc34sm6aaaada";
    private static final String unit_with_graph_2_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca";
    private static final String unit_with_graph_3_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6laaaaaq";

    private static final String got_with_graph_0 = "integration-metadata-management/data/got_0.json";
    private static final String got_with_graph_1 = "integration-metadata-management/data/got_1.json";
    private static final String got_with_graph_2 = "integration-metadata-management/data/got_2.json";
    private static final String got_with_graph_2_v2 = "integration-metadata-management/data/got_2_v2.json";

    private static final String got_with_graph_0_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrpqaaaaq";
    private static final String got_with_graph_1_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrtqaaaaq";
    private static final String got_with_graph_2_guid = "aebaaaaaaahlm6sdabzmoalc4pzxztyaaaaq";

    private static final String unit_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";
    private static final String unit_graph_zip_file =
        "integration-metadata-management/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";

    private static final String got_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";
    private static final String got_graph_zip_file =
        "integration-metadata-management/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";

    private static final String reclassification_units = "integration-metadata-management/reclassification/units.json";
    private static final String reclassification_units_lfc =
        "integration-metadata-management/reclassification/units_lfc.json";
    private static final String reclassification_gots = "integration-metadata-management/reclassification/gots.json";
    private static final String dsl_attach_arbre_ingest_ko =
        "integration-metadata-management/reclassification/dsl_attach_arbre_ingest_ko.json";
    private static final String dsl_attach_arbre_plan_ko =
        "integration-metadata-management/reclassification/dsl_attach_arbre_plan_ko.json";
    private static final String dsl_attach_plan_ingest_ko =
        "integration-metadata-management/reclassification/dsl_attach_plan_ingest_ko.json";
    private static final String dsl_cycle_ko = "integration-metadata-management/reclassification/dsl_cycle_ko.json";
    private static final String dsl_attach_detach_ok_parallel =
        "integration-metadata-management/reclassification/dsl_attach_detach_ok_parallel.json";
    private static final String dsl_attach_detach_ok =
        "integration-metadata-management/reclassification/dsl_attach_detach_ok.json";

    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;

    private static final int PORT_SERVICE_METADATA = 8020;

    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    public static final String JSON_EXTENTION = ".json";
    public static final String OFFER_ID = "default";

    private static WorkspaceClient workspaceClient;
    private static StorageClient storageClient;

    private static MetadataManagementResource metadataManagementResource;

    static OffsetRepository offsetRepository;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(METADATA_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        metadataManagementResource = retrofit.create(MetadataManagementResource.class);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        storageClient = StorageClientFactory.getInstance().getClient();

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
        VitamConfiguration.setElasticSearchScrollLimit(10_000);
    }

    @Before
    public void setup() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        VitamConfiguration.setAdminTenant(1);

        // ReconstructionService delete all unit and GOT without _tenant and older than 1 month.
        VitamConfiguration.setDeleteIncompleteReconstructedUnitDelay(Integer.MAX_VALUE);

        // Clean offerLog
        mongoRule.getMongoDatabase().getCollection(OfferCollections.OFFER_LOG.getName()).drop();
        mongoRule.getMongoDatabase().getCollection(OfferCollections.COMPACTED_OFFER_LOG.getName()).drop();
        mongoRule.getMongoDatabase().getCollection(OfferCollections.OFFER_SEQUENCE.getName()).drop();
    }

    @After
    public void tearDown() throws Exception {
        handleAfterClassExceptReferential();
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionOfMetadataThenOK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        LogbookLifeCyclesClient lifecycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        checkOfferLogSize(DataCategory.UNIT, 3);

        // 1. Reconstruct units
        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            2L
        );
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_0_guid, 0, 5);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_1_guid, 0, 4);

        // 2. Reconstruct object groups
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            5L
        );

        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_0_guid, 0, 8);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_1_guid, 0, 8);

        // 3. Reset offset and relaunch reconstruct for unit and got
        offsetRepository.createOrUpdateOffset(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            MetadataCollections.UNIT.getName(),
            0L
        );
        offsetRepository.createOrUpdateOffset(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            MetadataCollections.OBJECTGROUP.getName(),
            0L
        );

        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            2L
        );
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            5L
        );

        // 4. Reconstruct next for unit and got
        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            3L
        );
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            6L
        );

        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_2_guid, 0, 5);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_2_guid, 0, 8);

        // 5. Reconstruct nothing for unit and got
        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            3L
        );
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            2,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            6L
        );

        // 6. Reconstruct on unused tenants
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            MetadataCollections.UNIT.getName(),
            0L
        );
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            MetadataCollections.OBJECTGROUP.getName(),
            0L
        );

        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            2,
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            0L
        );
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            2,
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            0L
        );

        // 7. Reset Unit offset and reconstruct on unit and another invalid collection
        offsetRepository.createOrUpdateOffset(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            MetadataCollections.UNIT.getName(),
            0L
        );

        List<ReconstructionRequestItem> reconstructionItems = Arrays.asList(
            new ReconstructionRequestItem().setCollection(DataCategory.UNIT.name()).setTenant(TENANT_0).setLimit(2),
            new ReconstructionRequestItem().setCollection(DataCategory.MANIFEST.name()).setTenant(TENANT_0).setLimit(2)
        );

        List<ReconstructionResponseItem> reconstructionResponseItems = reconstructCollection(reconstructionItems);
        assertThat(reconstructionResponseItems.get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reconstructionResponseItems.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.UNIT.getName()
            )
        ).isEqualTo(2L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.MANIFEST.name()
            )
        ).isEqualTo(0L);

        // Purge reconstructed documents with only graph data and older than a configured delay
        Call<Void> purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataUNIT();
        assertThat(purge.execute().isSuccessful()).isTrue();

        purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataOBJECTGROUP();
        assertThat(purge.execute().isSuccessful()).isTrue();

        purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataUNITOBJECTGROUP();
        assertThat(purge.execute().isSuccessful()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionOfMetadataWithDeletedEntriesThenOK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        LogbookLifeCyclesClient lifecycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();

        // 0. Prepare data set
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);

        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        deleteFileFromOffer(unit_with_graph_0_guid, JSON_EXTENTION, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        deleteFileFromOffer(got_with_graph_0_guid, JSON_EXTENTION, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        checkOfferLogSize(DataCategory.UNIT, 4);
        checkOfferLogSize(DataCategory.OBJECTGROUP, 4);

        // 1. Reconstruct units
        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            1000,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            4L
        );
        ensureUnitNotExists(metadataClient, lifecycleClient, unit_with_graph_0_guid);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_1_guid, 0, 4);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_2_guid, 0, 5);

        // 2. Reconstruct object groups
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            1000,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            8L
        );
        ensureGotNotExists(metadataClient, lifecycleClient, got_with_graph_0_guid);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_1_guid, 0, 8);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_2_guid, 0, 8);

        // 3. New data set
        deleteFileFromOffer(unit_with_graph_1_guid, JSON_EXTENTION, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2_v2, DataCategory.UNIT);

        deleteFileFromOffer(got_with_graph_1_guid, JSON_EXTENTION, DataCategory.OBJECTGROUP);
        storeFileToOffer(
            container,
            got_with_graph_2_guid,
            JSON_EXTENTION,
            got_with_graph_2_v2,
            DataCategory.OBJECTGROUP
        );

        checkOfferLogSize(DataCategory.UNIT, 6);
        checkOfferLogSize(DataCategory.OBJECTGROUP, 6);

        // 4. Reconstruct units
        reconstruction(
            DataCategory.UNIT,
            MetadataCollections.UNIT,
            1000,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            10L
        );
        ensureUnitNotExists(metadataClient, lifecycleClient, unit_with_graph_0_guid);
        ensureUnitNotExists(metadataClient, lifecycleClient, unit_with_graph_1_guid);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_2_guid, 1, 6);

        // 5. Reconstruct object groups
        reconstruction(
            DataCategory.OBJECTGROUP,
            MetadataCollections.OBJECTGROUP,
            1000,
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            12L
        );
        ensureGotNotExists(metadataClient, lifecycleClient, got_with_graph_0_guid);
        ensureGotNotExists(metadataClient, lifecycleClient, got_with_graph_1_guid);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_2_guid, 1, 9);
    }

    private void checkOfferLogSize(DataCategory dataCategory, int size) throws StorageServerClientException {
        RequestResponse<OfferLog> offerLogResponse1 = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            dataCategory,
            null,
            Integer.MAX_VALUE,
            Order.ASC
        );
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults()).hasSize(size);
    }

    private void reconstruction(
        DataCategory dataCategory,
        MetadataCollections metadataCollection,
        int limit,
        int tenant,
        String strategy,
        long expectedOffset
    ) {
        List<ReconstructionRequestItem> unitReconstructionItems = singletonList(
            new ReconstructionRequestItem().setCollection(dataCategory.name()).setLimit(limit).setTenant(tenant)
        );
        List<ReconstructionResponseItem> reconstructionResponseItems = reconstructCollection(unitReconstructionItems);
        assertThat(reconstructionResponseItems).hasSize(1);
        assertThat(reconstructionResponseItems.get(0).getTenant()).isEqualTo(tenant);
        assertThat(reconstructionResponseItems.get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(tenant, strategy, metadataCollection.getName())).isEqualTo(
            expectedOffset
        );
    }

    private void ensureUnitNotExists(
        MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient,
        String unitId
    ) throws VitamClientException {
        RequestResponse<JsonNode> findGuid0Response = metadataClient.getUnitByIdRaw(unitId);
        assertThat(findGuid0Response.getStatus()).isEqualTo(404);
        assertThatThrownBy(() -> lifecycleClient.getRawUnitLifeCycleById(unitId)).isInstanceOf(
            LogbookClientNotFoundException.class
        );
    }

    private void ensureUnitExists(
        MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient,
        String unitId,
        int metadataVersion,
        int lfcVersion
    ) throws VitamClientException, LogbookClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unitId);
        assertThat(metadataResponse.isOk()).isTrue();

        RequestResponseOK<JsonNode> metadataResponse1 = (RequestResponseOK<JsonNode>) metadataResponse;
        assertThat(metadataResponse1.getResults().size()).isEqualTo(1);
        assertThat(metadataResponse1.getFirstResult().get("_id").asText()).isEqualTo(unitId);
        assertThat(metadataResponse1.getFirstResult().get("_v").asInt()).isEqualTo(metadataVersion);

        JsonNode lifecycleResponse = lifecycleClient.getRawUnitLifeCycleById(unitId);
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("_id").asText()).isEqualTo(unitId);
        assertThat(lifecycleResponse.get("_v").asInt()).isEqualTo(lfcVersion);
        assertThat(lifecycleResponse.get("evParentId")).isExactlyInstanceOf(NullNode.class);
    }

    private void ensureGotNotExists(
        MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient,
        String gotId
    ) throws VitamClientException {
        RequestResponse<JsonNode> findGuid0Response = metadataClient.getObjectGroupByIdRaw(gotId);
        assertThat(findGuid0Response.getStatus()).isEqualTo(404);
        assertThatThrownBy(() -> lifecycleClient.getRawObjectGroupLifeCycleById(gotId)).isInstanceOf(
            LogbookClientNotFoundException.class
        );
    }

    private void ensureGotExists(
        MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient,
        String gotId,
        int metadataVersion,
        int lfcVersion
    ) throws VitamClientException, LogbookClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> metadataResponse = metadataClient.getObjectGroupByIdRaw(gotId);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText()).isEqualTo(
            gotId
        );
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(
            metadataVersion
        );

        JsonNode lifecycleResponse = lifecycleClient.getRawObjectGroupLifeCycleById(gotId);
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("_id").asText()).isEqualTo(gotId);
        assertThat(lifecycleResponse.get("_v").asInt()).isEqualTo(lfcVersion);
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreUnitGraphThenStoreThenOK() throws Exception {
        VitamConfiguration.setStoreGraphElementsPerFile(5);

        LocalDateTime dateTime = LocalDateUtil.now()
            .minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);

        String dateInMongo = LocalDateUtil.getFormattedDateTimeForMongo(dateTime);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        MetadataCollections.UNIT.getCollection().insertMany(documents);

        assertThat(MetadataCollections.UNIT.getCollection().countDocuments()).isEqualTo(10);

        Map<MetadataCollections, Integer> ok = metadataManagementResource.tryStoreGraph().execute().body();

        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(10);

        dateTime = LocalDateUtil.now().minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);
        dateInMongo = LocalDateUtil.getFormattedDateTimeForMongo(dateTime);
        documents = new ArrayList<>();
        for (int i = 10; i < 15; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        MetadataCollections.UNIT.getCollection().insertMany(documents);

        assertThat(MetadataCollections.UNIT.getCollection().countDocuments()).isEqualTo(15);

        ok = metadataManagementResource.tryStoreGraph().execute().body();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(5);

        ok = metadataManagementResource.tryStoreGraph().execute().body();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(0);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unit_then_unitgraph_in_two_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        RequestResponse<OfferLog> offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);
        // 1. reconstruct unit
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        List<ReconstructionResponseItem> response = reconstructCollection(reconstructionItems);
        assertThat(response.size()).isEqualTo(1);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.UNIT.getName()
            )
        ).isEqualTo(4L);
        assertThat(response.get(0).getTenant()).isEqualTo(0);
        assertThat(response.get(0).getStatus()).isEqualTo(StatusCode.OK);

        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText()).isEqualTo(
            unit_with_graph_1_guid
        );
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_sps").toString()).contains(
            "SHOULD_BE_REMOVED_AFTER_RECONTRUCT"
        );

        ///////////////////////////////////////////////////////////////
        //        Reconstruction Graph
        ///////////////////////////////////////////////////////////////
        VitamThreadUtils.getVitamSession().setTenantId(1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT_GRAPH,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        // 2. reconstruct object group
        reconstructionItems = new ArrayList<>();
        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(2);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        response = reconstructCollection(reconstructionItems);
        assertThat(response.size()).isEqualTo(1);

        // Check Unit
        String expectedUnitJson = "integration-metadata-management/expected/units_1.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson, true);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_1.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson, true);

        assertThat(
            offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH.name())
        ).isEqualTo(5L);
        assertThat(response.get(0).getStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unit_then_got_then_unitgraph_then_gotgraph_in_one_phase_query_OK() throws Exception {
        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        RequestResponse<OfferLog> offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.OBJECTGROUP,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(3);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        storeFileToOffer(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT_GRAPH,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.OBJECTGROUP_GRAPH,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        // Reconstruct ALL (Unit, ObjectGroup, Unit_Graph, ObjectGroup_Graph)
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        List<ReconstructionResponseItem> response = reconstructCollection(reconstructionItems);
        assertThat(response.size()).isEqualTo(4);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.UNIT.getName()
            )
        ).isEqualTo(4L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.OBJECTGROUP.getName()
            )
        ).isEqualTo(7L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.UNIT_GRAPH.name()
            )
        ).isEqualTo(8L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECTGROUP_GRAPH.name()
            )
        ).isEqualTo(9L);
        assertThat(response.get(0).getTenant()).isEqualTo(0);
        assertThat(response.get(0).getCollection()).isEqualTo(DataCategory.UNIT.name());
        assertThat(response.get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.get(1).getTenant()).isEqualTo(0);
        assertThat(response.get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP.name());
        assertThat(response.get(1).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.get(2).getTenant()).isEqualTo(1);
        assertThat(response.get(2).getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        assertThat(response.get(2).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.get(3).getTenant()).isEqualTo(1);
        assertThat(response.get(3).getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        assertThat(response.get(3).getStatus()).isEqualTo(StatusCode.OK);

        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        // Check Unit
        String expectedUnitJson = "integration-metadata-management/expected/units_2.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson, true);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_2.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson, true);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unitgraph_then_gotgraph_then_unit_then_got_in_two_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        storeFileToOffer(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

        RequestResponse<OfferLog> offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT_GRAPH,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.OBJECTGROUP_GRAPH,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        // Reconstruct Unit_Graph, ObjectGroup_Graph
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        List<ReconstructionResponseItem> response = reconstructCollection(reconstructionItems);
        assertThat(response.size()).isEqualTo(2);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.UNIT_GRAPH.name()
            )
        ).isEqualTo(1L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECTGROUP_GRAPH.name()
            )
        ).isEqualTo(2L);
        assertThat(response.get(0).getTenant()).isEqualTo(1);
        assertThat(response.get(0).getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        assertThat(response.get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.get(1).getTenant()).isEqualTo(1);
        assertThat(response.get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        assertThat(response.get(1).getStatus()).isEqualTo(StatusCode.OK);

        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        String expectedUnitJson = "integration-metadata-management/expected/units_3.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson, false);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_3.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson, false);

        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.UNIT,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);

        offerLogResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.OBJECTGROUP,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(3);

        // Reconstruct Unit, ObjectGroup
        reconstructionItems = new ArrayList<>();
        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        response = reconstructCollection(reconstructionItems);
        assertThat(response.size()).isEqualTo(2);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.UNIT.getName()
            )
        ).isEqualTo(6L);
        assertThat(
            offsetRepository.findOffsetBy(
                TENANT_0,
                VitamConfiguration.getDefaultStrategy(),
                MetadataCollections.OBJECTGROUP.getName()
            )
        ).isEqualTo(9L);
        assertThat(response.get(0).getTenant()).isEqualTo(0);
        assertThat(response.get(0).getCollection()).isEqualTo(DataCategory.UNIT.name());
        assertThat(response.get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.get(1).getTenant()).isEqualTo(0);
        assertThat(response.get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP.name());
        assertThat(response.get(1).getStatus()).isEqualTo(StatusCode.OK);

        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        expectedUnitJson = "integration-metadata-management/expected/units_4.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson, true);
        // Check Got
        expectedGotJson = "integration-metadata-management/expected/gots_4.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson, true);
    }

    @Test
    @RunWithCustomExecutor
    public void testComputeUnitAndObjectGroupGraphUsingQueryDsl() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());

        //Given
        initializeDbWithUnitAndObjectGroupData();

        //Then
        assertThat(MetadataCollections.UNIT.getCollection().countDocuments()).isEqualTo(10);
        assertThat(MetadataCollections.OBJECTGROUP.getCollection().countDocuments()).isEqualTo(5);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);

        final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();

        // Compute Graph
        final Select select = new Select();
        select.setQuery(QueryHelper.missing("fakefake"));
        GraphComputeResponse body = metaDataClient.computeGraph(select.getFinalSelect());

        assertThat(body.getUnitCount()).isEqualTo(4);
        assertThat(body.getGotCount()).isEqualTo(3);

        // Check Unit
        String expectedUnitJson = "integration-metadata-management/expected/units_5.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson, false);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_5.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson, false);

        body = metaDataClient.computeGraph(
            GraphComputeResponse.GraphComputeAction.UNIT_OBJECTGROUP,
            Sets.newHashSet("AU_3", "AU_6", "AU_7", "AU_10")
        );

        assertThat(body.getUnitCount()).isEqualTo(4);
        assertThat(body.getGotCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void testComputeUnitAndObjectGroupGraphForTrivialCases() throws Exception {
        Document au_without_parents = new Document(Unit.ID, "au_without_parents")
            .append(Unit.TENANT_ID, 0)
            .append(Unit.ORIGINATING_AGENCY, "OA4")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));
        Document got_without_unit = new Document(ObjectGroup.ID, "got_without_unit")
            .append(Unit.TENANT_ID, 0)
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2")
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        MetadataCollections.UNIT.getCollection().insertOne(au_without_parents);
        MetadataCollections.OBJECTGROUP.getCollection().insertOne(got_without_unit);

        final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();

        // Compute Graph
        final Select select = new Select();
        select.setQuery(QueryHelper.exists(VitamFieldsHelper.id()));
        GraphComputeResponse body = metaDataClient.computeGraph(
            GraphComputeResponse.GraphComputeAction.UNIT,
            Sets.newHashSet("au_without_parents")
        );

        assertThat(body.getUnitCount()).isEqualTo(1);
        assertThat(body.getGotCount()).isEqualTo(0);

        Document computedUnit = MetadataCollections.UNIT.getCollection()
            .find(new Document("_id", "au_without_parents"))
            .iterator()
            .next();
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA4");
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA4");

        body = metaDataClient.computeGraph(
            GraphComputeResponse.GraphComputeAction.OBJECTGROUP,
            Sets.newHashSet("got_without_unit")
        );

        assertThat(body.getUnitCount()).isEqualTo(0);
        assertThat(body.getGotCount()).isEqualTo(1);

        Document computedGot = MetadataCollections.OBJECTGROUP.getCollection()
            .find(new Document("_id", "got_without_unit"))
            .iterator()
            .next();
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA2");
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA2");

        // PURGE mongo
        MetadataCollections.UNIT.getCollection().deleteMany(new Document());
        // Re-insert
        Document got_with_unit_up = got_without_unit.append(ObjectGroup.UP, Lists.newArrayList("au_without_parents"));
        got_with_unit_up.put(ObjectGroup.ID, "got_with_unit_up");
        au_without_parents.put(Unit.OG, "got_with_unit_up");

        MetadataCollections.UNIT.getCollection().insertOne(au_without_parents);
        MetadataCollections.OBJECTGROUP.getCollection().insertOne(got_with_unit_up);

        body = metaDataClient.computeGraph(
            GraphComputeResponse.GraphComputeAction.UNIT_OBJECTGROUP,
            Sets.newHashSet("au_without_parents")
        );

        assertThat(body.getUnitCount()).isEqualTo(1);
        assertThat(body.getGotCount()).isEqualTo(1);

        computedUnit = MetadataCollections.UNIT.getCollection()
            .find(new Document("_id", "au_without_parents"))
            .iterator()
            .next();
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA4");
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA4");

        computedGot = MetadataCollections.OBJECTGROUP.getCollection()
            .find(new Document("_id", "got_with_unit_up"))
            .iterator()
            .next();
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA2");
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(2).contains("OA4", "OA2");
    }

    @Test
    @RunWithCustomExecutor
    public void testComputeUnitAndObjectGroupWithScrollForLargeDataSet() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());

        final MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();

        initLargeDataSetWithoutGraph();

        VitamConfiguration.setElasticSearchScrollLimit(100);

        // When
        final SelectMultiQuery select = new SelectMultiQuery();
        // For historical reasons, we need to explicitly set depth to 0
        select.setQuery(QueryHelper.exists("#id").setDepthLimit(0));

        GraphComputeResponse body = metaDataClient.computeGraph(select.getFinalSelect());

        // Then
        assertThat(body.getUnitCount()).isEqualTo(111);
        assertThat(body.getGotCount()).isEqualTo(110);

        JsonNode some_unit = metaDataClient
            .selectUnitbyId(new Select().getFinalSelect(), "AU_88")
            .get("$results")
            .iterator()
            .next();
        assertThat(some_unit.get(VitamFieldsHelper.allunitups()).size()).isEqualTo(1);
        assertThat(some_unit.get(VitamFieldsHelper.originatingAgencies()).size()).isEqualTo(2);
        assertThat(some_unit.get(VitamFieldsHelper.min()).asInt()).isEqualTo(1);
        assertThat(some_unit.get(VitamFieldsHelper.max()).asInt()).isEqualTo(2);

        JsonNode some_og = metaDataClient
            .selectObjectGrouptbyId(new Select().getFinalSelect(), "GOT_101")
            .get("$results")
            .iterator()
            .next();
        assertThat(some_og.get(VitamFieldsHelper.allunitups()).size()).isEqualTo(2);
        assertThat(some_unit.get(VitamFieldsHelper.originatingAgencies()).size()).isEqualTo(2);
        assertThat(some_unit.get(VitamFieldsHelper.min()).asInt()).isEqualTo(1);
        assertThat(some_unit.get(VitamFieldsHelper.max()).asInt()).isEqualTo(2);
    }

    private void initLargeDataSetWithoutGraph() throws DatabaseException {
        Document auRoot = new Document(Unit.ID, "ROOT_AU")
            .append(Unit.TENANT_ID, 0)
            .append(Unit.UP, Collections.emptyList())
            .append(Unit.ORIGINATING_AGENCY, "SP0")
            .append("fakefake", "fakefake");

        List<Document> units = new ArrayList<>();
        units.add(auRoot);

        for (int i = 0; i < 110; i++) {
            Document au = new Document(Unit.ID, "AU_" + i)
                .append(Unit.TENANT_ID, 0)
                .append(Unit.UP, List.of("ROOT_AU"))
                .append(Unit.ORIGINATING_AGENCY, "SP1")
                .append(Unit.OG, "GOT_" + i);
            units.add(au);
        }

        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        List<Document> gots = new ArrayList<>();

        for (int i = 0; i < 110; i++) {
            Document got = new Document(Unit.ID, "GOT_" + i)
                .append(Unit.TENANT_ID, 0)
                .append(Unit.UP, List.of("AU_" + i))
                .append(Unit.ORIGINATING_AGENCY, "SP1");
            gots.add(got);
        }

        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
            .save(gots);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
            )
            .save(gots);
    }

    //@see graph.png
    @Test
    @RunWithCustomExecutor
    public void test_all_case_of_reclassification() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            VitamThreadUtils.getVitamSession().setContractId("aName5");
            AccessContractModel accessContract = new AccessContractModel().setEveryOriginatingAgency(true);
            accessContract.setIdentifier("aName5");
            VitamThreadUtils.getVitamSession().setContract(accessContract);
            VitamThreadUtils.getVitamSession().setContextId("FakeContext");

            // Given initial unit data
            final List<Document> unitList = JsonHandler.getFromFileAsTypeReference(
                PropertiesUtils.getResourceFile(reclassification_units),
                new TypeReference<>() {}
            );

            final List<Document> gotList = JsonHandler.getFromFileAsTypeReference(
                PropertiesUtils.getResourceFile(reclassification_gots),
                new TypeReference<>() {}
            );

            final List<JsonNode> unitsLfcJsonNodes = JsonHandler.getFromFileAsTypeReference(
                PropertiesUtils.getResourceFile(reclassification_units_lfc),
                new TypeReference<>() {}
            );

            List<Document> unitsLfc = unitsLfcJsonNodes
                .stream()
                .map(item -> Document.parse(JsonHandler.unprettyPrint(item)))
                .collect(Collectors.toList());
            // Save units
            VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .save(unitList);
            VitamRepositoryFactory.get()
                .getVitamESRepository(
                    MetadataCollections.UNIT.getVitamCollection(),
                    metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
                )
                .save(unitList);

            // Save gots
            VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
                .save(gotList);
            VitamRepositoryFactory.get()
                .getVitamESRepository(
                    MetadataCollections.OBJECTGROUP.getVitamCollection(),
                    metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
                )
                .save(gotList);

            // Save LFC
            new VitamMongoRepository(LogbookCollections.LIFECYCLE_UNIT.getCollection()).save(unitsLfc);

            AccessInternalConfiguration accessInternalConfiguration = new AccessInternalConfiguration();
            accessInternalConfiguration.setUrlMetaData(METADATA_URL);
            accessInternalConfiguration.setUrlProcessing(VitamServerRunner.PROCESSING_URL);
            accessInternalConfiguration.setUrlWorkspace(VitamServerRunner.WORKSPACE_URL);
            List<String> allowList = new ArrayList<>();
            allowList.add("Secret Défense");
            ClassificationLevel classificationLevel = new ClassificationLevel();
            classificationLevel.setAllowList(allowList);
            classificationLevel.setAuthorizeNotDefined(true);
            AccessInternalResourceImpl accessInternalResource = new AccessInternalResourceImpl(
                accessInternalConfiguration
            );
            // When attach HOLDING_UNIT -> FILING_UNIT then KO
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64kaqaaaaq");
            String operation = VitamThreadUtils.getVitamSession().getRequestId();
            JsonNode dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_attach_arbre_plan_ko));
            accessInternalResource.startReclassificationWorkflow(dsl);
            waitOperation(operation);
            ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
                .findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbook = logbookClient.selectOperationById(operation);
            JsonNode logbookNode = logbook.get("$results").get(0);
            JsonNode preparationEvent = logbookNode.get("events");
            assertEquals(preparationEvent.get(2).get("outDetail").asText(), "CHECK_CONCURRENT_WORKFLOW_LOCK.OK");
            assertEquals(
                preparationEvent.get(3).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_LOAD_REQUEST.OK"
            );
            assertEquals(
                preparationEvent.get(4).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_CHECK_GRAPH.KO"
            );
            assertThat(preparationEvent.get(4).get("evDetData").asText()).contains("illegalUnitTypeAttachments");

            // When attach HOLDING_UNIT -> INGEST then KO
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64kbqaaaaq");
            operation = VitamThreadUtils.getVitamSession().getRequestId();
            dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_attach_arbre_ingest_ko));
            accessInternalResource.startReclassificationWorkflow(dsl);
            waitOperation(operation);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
            logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            logbook = logbookClient.selectOperationById(operation);
            logbookNode = logbook.get("$results").get(0);
            preparationEvent = logbookNode.get("events");
            assertEquals(preparationEvent.get(2).get("outDetail").asText(), "CHECK_CONCURRENT_WORKFLOW_LOCK.OK");
            assertEquals(
                preparationEvent.get(3).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_LOAD_REQUEST.OK"
            );
            assertEquals(
                preparationEvent.get(4).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_CHECK_GRAPH.KO"
            );
            assertThat(preparationEvent.get(4).get("evDetData").asText()).contains("illegalUnitTypeAttachments");

            // When attach FILING_UNIT -> INGEST then KO
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64kcqaaaaq");
            operation = VitamThreadUtils.getVitamSession().getRequestId();
            dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_attach_plan_ingest_ko));
            accessInternalResource.startReclassificationWorkflow(dsl);
            waitOperation(operation);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
            logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            logbook = logbookClient.selectOperationById(operation);
            logbookNode = logbook.get("$results").get(0);
            preparationEvent = logbookNode.get("events");
            assertEquals(preparationEvent.get(2).get("outDetail").asText(), "CHECK_CONCURRENT_WORKFLOW_LOCK.OK");
            assertEquals(
                preparationEvent.get(3).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_LOAD_REQUEST.OK"
            );
            assertEquals(
                preparationEvent.get(4).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_CHECK_GRAPH.KO"
            );
            assertThat(preparationEvent.get(4).get("evDetData").asText()).contains("illegalUnitTypeAttachments");

            // When cycle exists then KO
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64kdqaaaaq");
            operation = VitamThreadUtils.getVitamSession().getRequestId();
            dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_cycle_ko));
            accessInternalResource.startReclassificationWorkflow(dsl);
            waitOperation(operation);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
            logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            logbook = logbookClient.selectOperationById(operation);
            logbookNode = logbook.get("$results").get(0);
            preparationEvent = logbookNode.get("events");
            assertEquals(preparationEvent.get(2).get("outDetail").asText(), "CHECK_CONCURRENT_WORKFLOW_LOCK.OK");
            assertEquals(
                preparationEvent.get(3).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_LOAD_REQUEST.OK"
            );
            assertEquals(
                preparationEvent.get(4).get("outDetail").asText(),
                "RECLASSIFICATION_PREPARATION_CHECK_GRAPH.KO"
            );
            assertThat(preparationEvent.get(4).get("evDetData").asText()).contains("Cycle detected");
            //When access contract restriction then KO

            //When parallel reclassification then KO
            // 1. Start first reclassification en mode step by step
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64keqaaaaq");
            operation = VitamThreadUtils.getVitamSession().getRequestId();
            dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_attach_detach_ok));
            accessInternalResource.startReclassificationWorkflow(dsl, ProcessAction.NEXT);
            waitOperation(operation);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
            // 2. Start parallel reclassification KO
            VitamThreadUtils.getVitamSession().setRequestId("aedqaaaaacfnrnfpaao4galeht64kfqaaaaq");
            String operation_parallel = VitamThreadUtils.getVitamSession().getRequestId();
            dsl = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(dsl_attach_detach_ok_parallel));
            accessInternalResource.startReclassificationWorkflow(dsl);
            waitOperation(operation_parallel);
            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation_parallel, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            logbook = logbookClient.selectOperationById(operation_parallel);
            logbookNode = logbook.get("$results").get(0);
            preparationEvent = logbookNode.get("events");
            assertEquals(preparationEvent.get(2).get("outDetail").asText(), "CHECK_CONCURRENT_WORKFLOW_LOCK.KO");
            assertThat(preparationEvent.get(2).get("evDetData").asText()).contains("Concurrent process(es) found");

            // 3. Start initial Reclassification en mode continue
            VitamThreadUtils.getVitamSession().setRequestId(operation);
            ProcessingManagementClientFactory.getInstance()
                .getClient()
                .executeOperationProcess(operation, Contexts.RECLASSIFICATION.name(), ProcessAction.RESUME.getValue());

            waitOperation(operation);

            processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operation, TENANT_0);
            Assertions.assertThat(processWorkflow).isNotNull();
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            // Get unit (au_d)
            Optional<Document> optionalUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aedqaaaaacfnrnfpaao4galeht64zgqaaaaq", TENANT_0);
            assertThat(optionalUnit).isPresent();
            Document au = optionalUnit.get();
            Integer _max = au.get(Unit.MAXDEPTH, Integer.class);
            assertThat(_max).isEqualTo(4);

            // Check all originating agencies
            List<String> _sps = au.get(Unit.ORIGINATING_AGENCIES, List.class);
            assertThat(_sps).contains("Identifier0", "Identifier1", "Identifier2");

            // Check all parent
            List<String> _us = au.get(Unit.UNITUPS, List.class);
            assertThat(_us).contains(
                "aedqaaaaacfnrnfpaao4galeht65vjiaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64zeqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64kjaaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64khqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65zpiaaaaq"
            );

            // Check all parents depths
            Map<String, List<String>> _uds = au.get(Unit.UNITDEPTHS, Map.class);
            assertThat(_uds).hasSize(3);
            // depth 1 > au_b, au_c
            assertThat(_uds.get("1")).contains(
                "aedqaaaaacfnrnfpaao4galeht64zeqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65zpiaaaaq"
            );
            // depth 2 > au_c, au_a, au_g
            assertThat(_uds.get("2")).contains(
                "aedqaaaaacfnrnfpaao4galeht64kjaaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64zeqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65vjiaaaaq"
            );
            // depth 3 > au_f
            assertThat(_uds.get("3")).contains("aedqaaaaacfnrnfpaao4galeht64khqaaaaq");

            // Check graph
            List<String> _graph = au.get(Unit.GRAPH, List.class);
            // au_a -> au_f, au_c_-> au_g, au_b -> au_a, au_b -> au_c, au_d -> au_b, au_d -> au_c
            assertThat(_graph).contains(
                "aedqaaaaacfnrnfpaao4galeht64zeqaaaaq/aedqaaaaacfnrnfpaao4galeht64kjaaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65zpiaaaaq/aedqaaaaacfnrnfpaao4galeht65vjiaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65zpiaaaaq/aedqaaaaacfnrnfpaao4galeht64zeqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64zgqaaaaq/aedqaaaaacfnrnfpaao4galeht65zpiaaaaq",
                "aedqaaaaacfnrnfpaao4galeht64zgqaaaaq/aedqaaaaacfnrnfpaao4galeht64zeqaaaaq",
                "aedqaaaaacfnrnfpaao4galeht65vjiaaaaq/aedqaaaaacfnrnfpaao4galeht64khqaaaaq"
            );

            // Get unit (au_e) remove arbre
            optionalUnit = VitamRepositoryFactory.get()
                .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                .getByID("aedqaaaaacfnrnfpaao4galeht64riyaaaaq", TENANT_0);
            assertThat(optionalUnit).isPresent();
            au = optionalUnit.get();
            _max = au.get(Unit.MAXDEPTH, Integer.class);
            assertThat(_max).isEqualTo(1);

            // Check all originating agencies
            _sps = au.get(Unit.ORIGINATING_AGENCIES, List.class);
            assertThat(_sps).contains("Identifier0");

            // Check all parent
            _us = au.get(Unit.UNITUPS, List.class);
            assertThat(_us).isEmpty();

            // Check all parents depths
            _uds = au.get(Unit.UNITDEPTHS, Map.class);
            assertThat(_uds).isEmpty();

            // Check graph
            _graph = au.get(Unit.GRAPH, List.class);
            assertThat(_graph).isEmpty();

            //Check global Units
            String expectedJson = "integration-metadata-management/expected/units_6.json";
            assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedJson, true);

            //Check global Gots
            expectedJson = "integration-metadata-management/expected/gots_6.json";
            assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedJson, true);
        } finally {
            StorageClientFactory.changeMode(
                new ClientConfigurationImpl("localhost", VitamServerRunner.PORT_SERVICE_STORAGE)
            );
        }
    }

    private <T> void assertDataSetEqualsExpectedFile(
        MongoCollection<T> mongoCollection,
        String expectedDataSetFile,
        boolean validateMetadata
    ) throws InvalidParseOperationException, FileNotFoundException {
        ArrayNode unitDataSet = dumpDataSet(mongoCollection, validateMetadata);

        String updatedUnitDataSet = JsonHandler.unprettyPrint(unitDataSet);
        String expectedUnitDataSet = JsonHandler.unprettyPrint(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expectedDataSetFile))
        );

        JsonAssert.assertJsonEquals(
            expectedUnitDataSet,
            updatedUnitDataSet,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection, boolean validateMetadata)
        throws InvalidParseOperationException {
        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find();

        for (T document : documents) {
            ObjectNode jsonUnit = (ObjectNode) JsonHandler.getFromString(JsonHandler.unprettyPrint(document));

            // Replace _glpd, _acd and _aud with marker
            assertThat(jsonUnit.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
            jsonUnit.put(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, "#TIMESTAMP#");
            if (validateMetadata) {
                assertThat(jsonUnit.get(MetadataDocument.APPROXIMATE_CREATION_DATE)).isNotNull();
                assertThat(jsonUnit.get(MetadataDocument.APPROXIMATE_UPDATE_DATE)).isNotNull();
                jsonUnit.put(MetadataDocument.APPROXIMATE_CREATION_DATE, "#TIMESTAMP#");
                jsonUnit.put(MetadataDocument.APPROXIMATE_UPDATE_DATE, "#TIMESTAMP#");
            }

            dataSet.add(jsonUnit);
        }

        return dataSet;
    }

    // See computegraph.png for more information
    private void initializeDbWithUnitAndObjectGroupData() throws DatabaseException {
        // Create units with or without graph data
        Document au1 = new Document(Unit.ID, "AU_1")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA1")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA1"));

        Document au2 = new Document(Unit.ID, "AU_2")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au3 = new Document(Unit.ID, "AU_3")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_1"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA1")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA1"));

        Document au4 = new Document(Unit.ID, "AU_4")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_4")
            .append(Unit.UP, Lists.newArrayList("AU_1", "AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA4")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au5 = new Document(Unit.ID, "AU_5")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList("AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au6 = new Document(Unit.ID, "AU_6")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_6")
            .append(Unit.UP, Lists.newArrayList("AU_2", "AU_5"))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au7 = new Document(Unit.ID, "AU_7")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_4"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA4")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au8 = new Document(Unit.ID, "AU_8")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_6", "AU_4"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au9 = new Document(Unit.ID, "AU_9")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_9")
            .append(Unit.UP, Lists.newArrayList("AU_5", "AU_6"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au10 = new Document(Unit.ID, "AU_10")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_10")
            .append(Unit.UP, Lists.newArrayList("AU_8", "AU_9"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        List<Document> units = Lists.newArrayList(au1, au2, au3, au4, au5, au6, au7, au8, au9, au10);
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        ////////////////////////////////////////////////
        // Create corresponding ObjectGroup (only 4 GOT subject of compute graph as no _glpd defined on them)
        ///////////////////////////////////////////////
        Document got4 = new Document(ObjectGroup.ID, "GOT_4")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_4"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA4")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA4"));

        // Got 6 have Graph Data
        Document got6 = new Document(ObjectGroup.ID, "GOT_6")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(ObjectGroup.UP, Lists.newArrayList("AU_6"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2")
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        //Unit "AU_8", "AU_3", "AU_7" attached to got 8
        Document got8 = new Document(ObjectGroup.ID, "GOT_8")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_8", "AU_3", "AU_7"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got9 = new Document(ObjectGroup.ID, "GOT_9")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_9"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got10 = new Document(ObjectGroup.ID, "GOT_10")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_10"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        List<Document> gots = Lists.newArrayList(got4, got6, got8, got9, got10);
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
            .save(gots);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
            )
            .save(gots);
    }

    private void storeFileToOffer(String container, String fileName, String extension, String file, DataCategory type)
        throws ContentAddressableStorageServerException, IOException, StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        final ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceContainerGUID(container);
        objectDescription.setObjectName(fileName + extension);
        objectDescription.setType(type);
        objectDescription.setWorkspaceObjectURI(fileName + extension);

        try (FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(file))) {
            workspaceClient.putObject(container, fileName + extension, stream);
        }
        storageClient.storeFileFromWorkspace(
            VitamConfiguration.getDefaultStrategy(),
            type,
            fileName,
            objectDescription
        );
    }

    private void deleteFileFromOffer(String fileName, String extension, DataCategory type)
        throws StorageServerClientException {
        storageClient.delete(VitamConfiguration.getDefaultStrategy(), type, fileName + extension);
    }

    private static List<ReconstructionResponseItem> reconstructCollection(
        List<ReconstructionRequestItem> reconstructionItems
    ) {
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            return metadataClient.reconstructCollection(reconstructionItems);
        } catch (MetaDataNotFoundException | MetaDataClientServerException | InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public interface MetadataManagementResource {
        @GET("/metadata/v1/storegraph")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Map<MetadataCollections, Integer>> tryStoreGraph();

        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/UNIT")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataUNIT();

        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/OBJECTGROUP")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataOBJECTGROUP();

        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/UNIT_OBJECTGROUP")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataUNITOBJECTGROUP();
    }
}
