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

package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class ReadOnlyStorageIT {

    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    private static final String DEFAULT_SECOND_CONF = "storage-test/storage-default-offer2-ssl.conf";
    private static final String STORAGE_CONF = "storage-test/storage-engine-read-only.conf";
    private static final String DEFAULT_STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;
    private static final String STRATEGY_ID = "default";
    private static final String CONTAINER_NAME = "test";
    private static final String OBJ_1 = "obj1";
    private static final String OBJ_2 = "obj2";
    private static final String OBJ_3 = "obj3";
    private static final String OFFER_ID_1 = "default";
    private static final String OFFER_ID_2 = "default2";
    private static final String DIGEST = "digest";
    private static final String DB_OFFER1 = "vitamoffer1";
    private static final String DB_OFFER2 = "vitamoffer2";

    @ClassRule
    public static TempFolderRule tempFolder = new TempFolderRule();

    @ClassRule
    public static MongoRule mongoRuleOffer1 = new MongoRule(DB_OFFER1, MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static MongoRule mongoRuleOffer2 = new MongoRule(DB_OFFER2, MongoDbAccess.getMongoClientSettingsBuilder());

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static SetupStorageAndOffers setupStorageAndOffers;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        OfferCollections.OFFER_LOG.setPrefix(GUIDFactory.newGUID().getId());
        OfferCollections.OFFER_SEQUENCE.setPrefix(GUIDFactory.newGUID().getId());
        mongoRuleOffer1.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRuleOffer1.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());
        mongoRuleOffer2.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRuleOffer2.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());

        VitamConfiguration.setRestoreBulkSize(15);

        setupStorageAndOffers = new SetupStorageAndOffers();
        setupStorageAndOffers.setupStorageAndTwoOffer(
            StorageTwoOffersIT.tempFolder,
            DEFAULT_STORAGE_CONF_FILE_NAME,
            WORKSPACE_CONF,
            STORAGE_CONF,
            DEFAULT_OFFER_CONF,
            DEFAULT_SECOND_CONF
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws VitamApplicationServerException {
        setupStorageAndOffers.close();
        mongoRuleOffer1.handleAfterClass();
        mongoRuleOffer2.handleAfterClass();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void init() throws IOException {
        setupStorageAndOffers.cleanOffers();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void cleanup() throws Exception {
        postTestControls();
        setupStorageAndOffers.cleanOffers();
        mongoRuleOffer1.handleAfter();
        mongoRuleOffer2.handleAfter();
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer(CONTAINER_NAME, true);
        } catch (ContentAddressableStorageNotFoundException e) {
            // NOP
        }
    }

    private void postTestControls() throws Exception {
        Stream<Path> accessAndStorageLogsListing = Files.list(
            Paths.get(setupStorageAndOffers.getStorageLogDirectory())
        );
        assertThat(accessAndStorageLogsListing).isEmpty();
    }

    @RunWithCustomExecutor
    @Test
    public void testGetStorageInformation() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            JsonNode storageInformation = storageClient.getStorageInformation(OFFER_ID_1);
            assertThat(storageInformation).isNotNull();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetStorageStrategies() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponse<StorageStrategy> response = storageClient.getStorageStrategies();
            assertThat(response.isOk()).isTrue();
            List<StorageStrategy> strategies = ((RequestResponseOK<StorageStrategy>) response).getResults();
            assertThat(strategies.get(0).getId()).isEqualTo(STRATEGY_ID);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testExists() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_3,
                List.of(OFFER_ID_1, OFFER_ID_2)
            );
            assertThat(existsByOffer).isEqualTo(ImmutableMap.of(OFFER_ID_1, true, OFFER_ID_2, false));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetInformation() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            JsonNode objectInformation = storageClient.getInformation(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_3,
                List.of(OFFER_ID_1, OFFER_ID_2),
                true
            );
            assertThat(objectInformation.get(OFFER_ID_1).get(DIGEST).asText()).isEqualTo(
                "7914d7f6e7cd09aabeb3a2f9fb484d11bf30216a691427e2a8ae59b5a1fb276d2558f2520c5beb8c814808af4c2e74a28bb7fde11eaffeef12daf4ded26018a7"
            );
            assertThat(objectInformation.get(OFFER_ID_2)).isNull();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testBatchObjectInformation() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_1, "data1", OFFER_ID_2);
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponse<BatchObjectInformationResponse> batchObjectInformation =
                storageClient.getBatchObjectInformation(
                    STRATEGY_ID,
                    DataCategory.OBJECT,
                    List.of(OFFER_ID_1, OFFER_ID_2),
                    List.of(OBJ_1, OBJ_3)
                );

            assertThat(batchObjectInformation.isOk()).isTrue();

            Map<String, Map<String, String>> offerDigestsByObjectId =
                ((RequestResponseOK<BatchObjectInformationResponse>) batchObjectInformation).getResults()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            BatchObjectInformationResponse::getObjectId,
                            BatchObjectInformationResponse::getOfferDigests
                        )
                    );
            assertThat(offerDigestsByObjectId).containsOnlyKeys(OBJ_1, OBJ_3);
            assertThat(offerDigestsByObjectId.get(OBJ_1)).containsOnlyKeys(OFFER_ID_1, OFFER_ID_2);
            assertThat(offerDigestsByObjectId.get(OBJ_1).get(OFFER_ID_1)).isNull();
            assertThat(offerDigestsByObjectId.get(OBJ_1).get(OFFER_ID_2)).isEqualTo(
                "9731b541b22c1d7042646ab2ee17685bbb664bced666d8ecf3593f3ef46493deef651b0f31b6cff8c4df8dcb425a1035e86ddb9877a8685647f39847be0d7c01"
            );
            assertThat(offerDigestsByObjectId.get(OBJ_3)).containsOnlyKeys(OFFER_ID_1, OFFER_ID_2);
            assertThat(offerDigestsByObjectId.get(OBJ_3).get(OFFER_ID_1)).isEqualTo(
                "7914d7f6e7cd09aabeb3a2f9fb484d11bf30216a691427e2a8ae59b5a1fb276d2558f2520c5beb8c814808af4c2e74a28bb7fde11eaffeef12daf4ded26018a7"
            );
            assertThat(offerDigestsByObjectId.get(OBJ_3).get(OFFER_ID_2)).isNull();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetReferentOffer() throws Exception {
        // Given

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            String referentOffer = storageClient.getReferentOffer(STRATEGY_ID);

            assertThat(referentOffer).isEqualTo(OFFER_ID_1);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testDeleteNonExistingObject() {
        // Given

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient.delete(STRATEGY_ID, DataCategory.OBJECT, OBJ_1)).isInstanceOf(
                StorageServerClientException.class
            );

            assertThatThrownBy(
                () -> storageClient.delete(STRATEGY_ID, DataCategory.OBJECT, OBJ_1, List.of(OFFER_ID_1))
            ).isInstanceOf(StorageServerClientException.class);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testDeleteExistingObject() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient.delete(STRATEGY_ID, DataCategory.OBJECT, OBJ_3)).isInstanceOf(
                StorageServerClientException.class
            );

            assertThatThrownBy(
                () -> storageClient.delete(STRATEGY_ID, DataCategory.OBJECT, OBJ_3, List.of(OFFER_ID_1))
            ).isInstanceOf(StorageServerClientException.class);

            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_3,
                List.of(OFFER_ID_1)
            );
            assertThat(existsByOffer).isEqualTo(Map.of(OFFER_ID_1, true));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStoreFileFromWorkspace() throws Exception {
        // Given
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.putObject(
                CONTAINER_NAME,
                OBJ_1,
                new ByteArrayInputStream("data1".getBytes(StandardCharsets.UTF_8))
            );
        }

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            final ObjectDescription objectDescription = new ObjectDescription();
            objectDescription.setWorkspaceContainerGUID(CONTAINER_NAME);
            objectDescription.setWorkspaceObjectURI(OBJ_1);

            assertThatThrownBy(
                () -> storageClient.storeFileFromWorkspace(STRATEGY_ID, DataCategory.OBJECT, OBJ_1, objectDescription)
            ).isInstanceOf(StorageServerClientException.class);

            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_1,
                List.of(OFFER_ID_1, OFFER_ID_2)
            );
            assertThat(existsByOffer).isEqualTo(Map.of(OFFER_ID_1, false, OFFER_ID_2, false));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testCreate() throws Exception {
        // Given
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.putObject(
                CONTAINER_NAME,
                OBJ_1,
                new ByteArrayInputStream("data1".getBytes(StandardCharsets.UTF_8))
            );
        }

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    storageClient.create(
                        STRATEGY_ID,
                        OBJ_1,
                        DataCategory.OBJECT,
                        new ByteArrayInputStream("data1".getBytes(StandardCharsets.UTF_8)),
                        5L,
                        List.of(OFFER_ID_1, OFFER_ID_2)
                    )
            ).isInstanceOf(StorageServerClientException.class);

            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_1,
                List.of(OFFER_ID_1, OFFER_ID_2)
            );
            assertThat(existsByOffer).isEqualTo(Map.of(OFFER_ID_1, false, OFFER_ID_2, false));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testBulkStoreFilesFromWorkspace() throws Exception {
        // Given
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.putObject(
                CONTAINER_NAME,
                OBJ_1,
                new ByteArrayInputStream("data1".getBytes(StandardCharsets.UTF_8))
            );
            workspaceClient.putObject(
                CONTAINER_NAME,
                OBJ_2,
                new ByteArrayInputStream("data2".getBytes(StandardCharsets.UTF_8))
            );
        }

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
                CONTAINER_NAME,
                List.of(OBJ_1, OBJ_2),
                DataCategory.OBJECT,
                List.of(OBJ_1, OBJ_2)
            );
            assertThatThrownBy(
                () -> storageClient.bulkStoreFilesFromWorkspace(STRATEGY_ID, bulkObjectStoreRequest)
            ).isInstanceOf(StorageServerClientException.class);

            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.OBJECT,
                OBJ_1,
                List.of(OFFER_ID_1, OFFER_ID_2)
            );
            assertThat(existsByOffer).isEqualTo(Map.of(OFFER_ID_1, false, OFFER_ID_2, false));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerAsyncWithoutAccessLog() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Non-existing object
            assertThatThrownBy(
                () ->
                    storageClient.getContainerAsync(
                        STRATEGY_ID,
                        OBJ_1,
                        DataCategory.OBJECT,
                        AccessLogUtils.getNoLogAccessLog()
                    )
            ).isInstanceOf(StorageNotFoundException.class);

            // Existing object
            Response obj3ContainerAsync = storageClient.getContainerAsync(
                STRATEGY_ID,
                OFFER_ID_1,
                OBJ_3,
                DataCategory.OBJECT,
                AccessLogUtils.getNoLogAccessLog()
            );
            assertThat(obj3ContainerAsync.getStatusInfo().getStatusCode()).isEqualTo(200);
            try (InputStream inputStream = obj3ContainerAsync.readEntity(InputStream.class)) {
                assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).isEqualTo("data3");
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerAsyncWithAccessLog() throws Exception {
        // Given
        AccessContractModel loggedAccessContract = new AccessContractModel().setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(loggedAccessContract);

        givenObjectWrittenToOffer(DataCategory.OBJECT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    storageClient.getContainerAsync(
                        STRATEGY_ID,
                        OBJ_3,
                        DataCategory.OBJECT,
                        AccessLogUtils.getInfoForAccessLog(
                            "qualifier",
                            0,
                            VitamThreadUtils.getVitamSession(),
                            5L,
                            "unit"
                        )
                    )
            ).isExactlyInstanceOf(StorageServerClientException.class);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testListContainer() throws Exception {
        AccessContractModel loggedAccessContract = new AccessContractModel().setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(loggedAccessContract);

        // Given
        givenObjectWrittenToOffer(DataCategory.UNIT, OBJ_1, "data1", OFFER_ID_1);
        givenObjectWrittenToOffer(DataCategory.UNIT, OBJ_2, "data2", OFFER_ID_1);
        givenObjectDeletedFromOffer(DataCategory.UNIT, OBJ_2, OFFER_ID_1);
        givenObjectWrittenToOffer(DataCategory.UNIT, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            CloseableIterator<ObjectEntry> objectEntryCloseableIterator = storageClient.listContainer(
                STRATEGY_ID,
                OFFER_ID_1,
                DataCategory.UNIT
            );

            assertThat(objectEntryCloseableIterator)
                .extracting(ObjectEntry::getObjectId, ObjectEntry::getSize)
                .containsExactlyInAnyOrder(tuple(OBJ_1, 5L), tuple(OBJ_3, 5L));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetOfferLogs() throws Exception {
        AccessContractModel loggedAccessContract = new AccessContractModel().setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(loggedAccessContract);

        // Given
        givenObjectWrittenToOffer(DataCategory.OBJECTGROUP, OBJ_1, "data1", OFFER_ID_1);
        givenObjectWrittenToOffer(DataCategory.OBJECTGROUP, OBJ_2, "data2", OFFER_ID_1);
        givenObjectDeletedFromOffer(DataCategory.OBJECTGROUP, OBJ_3, OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponse<OfferLog> offerLogs = storageClient.getOfferLogs(
                STRATEGY_ID,
                OFFER_ID_1,
                DataCategory.OBJECTGROUP,
                0L,
                100,
                Order.ASC
            );

            assertThat(offerLogs.isOk()).isTrue();
            assertThat(((RequestResponseOK<OfferLog>) offerLogs).getResults())
                .extracting(OfferLog::getFileName, OfferLog::getAction)
                .containsExactlyInAnyOrder(
                    tuple(OBJ_1, OfferLogAction.WRITE),
                    tuple(OBJ_2, OfferLogAction.WRITE),
                    tuple(OBJ_3, OfferLogAction.DELETE)
                );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testCopyObjectFromOfferToOffer() throws Exception {
        // Given
        givenObjectWrittenToOffer(DataCategory.MANIFEST, OBJ_3, "data3", OFFER_ID_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(
                () ->
                    storageClient.copyObjectFromOfferToOffer(
                        OBJ_3,
                        DataCategory.MANIFEST,
                        OFFER_ID_1,
                        OFFER_ID_2,
                        STRATEGY_ID
                    )
            ).isExactlyInstanceOf(StorageServerClientException.class);

            Map<String, Boolean> existsByOffer = storageClient.exists(
                STRATEGY_ID,
                DataCategory.MANIFEST,
                OBJ_3,
                List.of(OFFER_ID_1, OFFER_ID_2)
            );
            assertThat(existsByOffer).isEqualTo(Map.of(OFFER_ID_1, true, OFFER_ID_2, false));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageAccessLogBackup() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient.storageAccessLogBackup(List.of(TENANT_0))).isExactlyInstanceOf(
                StorageServerClientException.class
            );

            CloseableIterator<ObjectEntry> storageAccessLogEntries = storageClient.listContainer(
                STRATEGY_ID,
                OFFER_ID_1,
                DataCategory.STORAGEACCESSLOG
            );
            assertThat(storageAccessLogEntries).isEmpty();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageLogBackup() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient.storageLogBackup(List.of(TENANT_0))).isExactlyInstanceOf(
                StorageServerClientException.class
            );

            CloseableIterator<ObjectEntry> storageAccessLogEntries = storageClient.listContainer(
                STRATEGY_ID,
                OFFER_ID_1,
                DataCategory.STORAGELOG
            );
            assertThat(storageAccessLogEntries).isEmpty();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageLogTraceability() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);

        // When / Then
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient.storageLogTraceability(List.of(TENANT_0))).isExactlyInstanceOf(
                StorageServerClientException.class
            );

            CloseableIterator<ObjectEntry> storageAccessLogEntries = storageClient.listContainer(
                STRATEGY_ID,
                OFFER_ID_1,
                DataCategory.STORAGETRACEABILITY
            );
            assertThat(storageAccessLogEntries).isEmpty();
        }
    }

    private Connection createDirectOfferConnection(String offerId)
        throws StorageDriverException, StorageDriverNotFoundException {
        return DriverManager.getDriverFor(offerId).connect(offerId);
    }

    private void givenObjectWrittenToOffer(
        DataCategory dataCategory,
        String objectName,
        String content,
        String offerId
    ) throws StorageDriverException, StorageDriverNotFoundException {
        try (Connection connect = createDirectOfferConnection(offerId)) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            StoragePutRequest storagePutRequest = new StoragePutRequest(
                0,
                dataCategory.getFolder(),
                objectName,
                VitamConfiguration.getDefaultDigestType().getName(),
                new ByteArrayInputStream(bytes)
            );
            storagePutRequest.setSize(bytes.length);
            connect.putObject(storagePutRequest);
        }
    }

    private void givenObjectDeletedFromOffer(DataCategory dataCategory, String objectName, String offerId)
        throws StorageDriverException, StorageDriverNotFoundException {
        try (Connection connect = createDirectOfferConnection(offerId)) {
            StorageRemoveRequest storageRemoveRequest = new StorageRemoveRequest(
                0,
                dataCategory.getFolder(),
                objectName
            );
            connect.removeObject(storageRemoveRequest);
        } catch (fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException ignored) {
            // NOP
        }
    }
}
