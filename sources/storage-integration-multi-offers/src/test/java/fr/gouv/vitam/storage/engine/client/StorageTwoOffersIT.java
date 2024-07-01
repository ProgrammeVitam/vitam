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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferDiffRequest;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.offerdiff.OfferDiffStatus;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncStatus;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * StorageTwoOffersIT class
 */
public class StorageTwoOffersIT {

    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    private static final String DEFAULT_SECOND_CONF = "storage-test/storage-default-offer2-ssl.conf";
    private static final String STORAGE_CONF = "storage-test/storage-engine.conf";
    private static final String DEFAULT_STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 0;
    private static final String DIGEST = "digest";
    private static final String SECOND_OFFER_ID = "default2";
    private static final String OFFER_ID = "default";
    private static final String STRATEGY_ID = VitamConfiguration.getDefaultStrategy();
    private static final String DB_OFFER1 = "vitamoffer1";
    private static final String DB_OFFER2 = "vitamoffer2";

    static StorageClient storageClient;
    static WorkspaceClient workspaceClient;

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    @ClassRule
    public static TempFolderRule tempFolder = new TempFolderRule();

    @ClassRule
    public static MongoRule mongoRuleOffer1 = new MongoRule(DB_OFFER1, MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static MongoRule mongoRuleOffer2 = new MongoRule(DB_OFFER2, MongoDbAccess.getMongoClientSettingsBuilder());

    private static OfferSyncAdminResource offerSyncAdminResource;
    private static OfferDiffAdminResource offerDiffAdminResource;

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

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://localhost:" + SetupStorageAndOffers.PORT_ADMIN_STORAGE)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        offerSyncAdminResource = retrofit.create(OfferSyncAdminResource.class);
        offerDiffAdminResource = retrofit.create(OfferDiffAdminResource.class);

        storageClient = StorageClientFactory.getInstance().getClient();
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws VitamApplicationServerException {
        setupStorageAndOffers.close();
        mongoRuleOffer1.handleAfterClass();
        mongoRuleOffer2.handleAfterClass();
        if (storageClient != null) {
            storageClient.close();
        }
        if (workspaceClient != null) {
            workspaceClient.close();
        }
        VitamClientFactory.resetConnections();
    }

    @Before
    public void init() throws IOException {
        setupStorageAndOffers.cleanOffers();
    }

    @After
    public void cleanup() throws IOException {
        setupStorageAndOffers.cleanOffers();
        mongoRuleOffer1.handleAfter();
        mongoRuleOffer2.handleAfter();
    }

    private void storeObjectInAllOffers(String id, DataCategory category, InputStream inputStream) throws Exception {
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID(id);
        description.setWorkspaceObjectURI(id);
        workspaceClient.createContainer(id);
        workspaceClient.putObject(id, id, inputStream);
        StreamUtils.closeSilently(inputStream);
        storageClient.storeFileFromWorkspace(STRATEGY_ID, category, id, description);
    }

    private void storeObjectInOffers(String objectId, DataCategory dataCategory, byte[] data, String... offerIds)
        throws InvalidParseOperationException, StorageServerClientException {
        storageClient.create(
            VitamConfiguration.getDefaultStrategy(),
            objectId,
            dataCategory,
            new ByteArrayInputStream(data),
            (long) data.length,
            Arrays.asList(offerIds)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkStrategyConfigutation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        RequestResponse<StorageStrategy> response = storageClient.getStorageStrategies();
        assertTrue(response.isOk());
        final StorageStrategy firstResult = ((RequestResponseOK<StorageStrategy>) response).getFirstResult();
        assertNotNull(firstResult);
        assertThat(firstResult.getOffers().size()).isEqualTo(2);
        assertThat(firstResult.getOffers().get(0).isReferent()).isFalse();
        assertThat(firstResult.getOffers().get(0).getId()).isEqualTo("default2");
        assertThat(firstResult.getOffers().get(0).getStatus()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(firstResult.getOffers().get(0).getRank()).isEqualTo(0);

        assertThat(firstResult.getOffers().get(1).isReferent()).isTrue();
        assertThat(firstResult.getOffers().get(1).getId()).isEqualTo("default");
        assertThat(firstResult.getOffers().get(1).getStatus()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(firstResult.getOffers().get(1).getRank()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void checkStoreInOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        //Given
        String id = GUIDFactory.newGUID().getId();

        // When
        storeObjectInOffers(id, OBJECT, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent(id, OBJECT, true, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void testCopyFromOneOfferToAnother() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String id = GUIDFactory.newGUID().getId();
        String id2 = GUIDFactory.newGUID().getId();
        //WHEN
        storeObjectInAllOffers(id, OBJECT, new ByteArrayInputStream(id.getBytes()));
        storeObjectInAllOffers(id2, OBJECT, new ByteArrayInputStream(id2.getBytes()));

        JsonNode information;
        information = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            id,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        alterFileInSecondOffer(id);
        //verify that offer2 is modified
        information = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            id,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );
        assertThat(information.get(OFFER_ID).get(DIGEST)).isNotEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        // correct the offer 2
        storageClient.copyObjectFromOfferToOffer(id, DataCategory.OBJECT, OFFER_ID, SECOND_OFFER_ID, STRATEGY_ID);

        // verify That the copy has been correctly done
        information = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            id,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));
    }

    // write directly in file of second offer
    private void alterFileInSecondOffer(String id) throws IOException {
        String path =
            setupStorageAndOffers.getSecondStorageFolder() + File.separator + "0_object" + File.separator + id;
        try (Writer writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(id + "test");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void whenDeleteInOneOfferVerifyObjectDoNotExist() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String object = GUIDFactory.newGUID().getId();
        String object2 = GUIDFactory.newGUID().getId();
        storeObjectInAllOffers(object, OBJECT, new ByteArrayInputStream(object.getBytes()));
        storeObjectInAllOffers(object2, OBJECT, new ByteArrayInputStream(object2.getBytes()));

        JsonNode informationObject1 = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            object,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );
        JsonNode informationObject2 = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            object2,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );

        //just verify the  object  stored and equal
        assertThat(informationObject1.get(OFFER_ID).get(DIGEST)).isEqualTo(
            informationObject1.get(SECOND_OFFER_ID).get(DIGEST)
        );

        String digestObject1SecondOffer = informationObject1.get(SECOND_OFFER_ID).get(DIGEST).textValue();
        String digestObject2SecondOffer = informationObject2.get(SECOND_OFFER_ID).get(DIGEST).textValue();

        //WHEN
        //delete object1 in second offer
        deleteObjectFromOffers(object, OBJECT, SECOND_OFFER_ID);

        deleteObjectFromOffers(object2, OBJECT, OFFER_ID, SECOND_OFFER_ID);

        //THEN
        //delete object2 in the two offers
        informationObject2 = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            object2,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );

        informationObject1 = storageClient.getInformation(
            STRATEGY_ID,
            OBJECT,
            object,
            newArrayList(OFFER_ID, SECOND_OFFER_ID),
            true
        );
        //verify Object2 is  deleted in the two offers
        assertThat(informationObject2.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject2.get(OFFER_ID)).isNull();

        //verify Object1 is  deleted in only offer 2
        assertThat(informationObject1.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject1.get(OFFER_ID)).isNotNull();

        // Try to download object & object2 in STRATEGY
        javax.ws.rs.core.Response objectToGetFromStrategy = storageClient.getContainerAsync(
            STRATEGY_ID,
            OFFER_ID,
            object,
            OBJECT,
            AccessLogUtils.getNoLogAccessLog()
        );
        assertThat(objectToGetFromStrategy.getStatus()).isEqualTo(OK.getStatusCode());

        assertThrows(
            StorageNotFoundException.class,
            () -> storageClient.getContainerAsync(STRATEGY_ID, object2, OBJECT, AccessLogUtils.getNoLogAccessLog())
        );

        // Try to download object & object2 in EVERY OFFER
        javax.ws.rs.core.Response objectToGetFromOffer1 = storageClient.getContainerAsync(
            STRATEGY_ID,
            OFFER_ID,
            object,
            OBJECT,
            AccessLogUtils.getNoLogAccessLog()
        );
        assertThat(objectToGetFromOffer1.getStatus()).isEqualTo(OK.getStatusCode());
        assertThrows(
            StorageNotFoundException.class,
            () ->
                storageClient.getContainerAsync(
                    STRATEGY_ID,
                    SECOND_OFFER_ID,
                    object,
                    OBJECT,
                    AccessLogUtils.getNoLogAccessLog()
                )
        );

        assertThrows(
            StorageNotFoundException.class,
            () ->
                storageClient.getContainerAsync(
                    STRATEGY_ID,
                    OFFER_ID,
                    object2,
                    OBJECT,
                    AccessLogUtils.getNoLogAccessLog()
                )
        );
        assertThrows(
            StorageNotFoundException.class,
            () ->
                storageClient.getContainerAsync(
                    STRATEGY_ID,
                    SECOND_OFFER_ID,
                    object2,
                    OBJECT,
                    AccessLogUtils.getNoLogAccessLog()
                )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersNewFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithDifferentContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When / Then
        Assertions.assertThatThrownBy(
            () -> storeObjectInOffers("file1", OBJECT, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID)
        ).isInstanceOf(StorageServerClientException.class);
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithSameContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithSameContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithDifferentContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithSameContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithSameContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithDifferentContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1_V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1_V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherFromScratch() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        SecureRandom random = new SecureRandom();
        int NB_ACTIONS = 200;
        List<String> existingFileNames = new ArrayList<>();
        int cpt = 0;

        for (int i = 0; i < NB_ACTIONS; i++) {
            if (existingFileNames.isEmpty() || random.nextInt(5) != 0) {
                cpt++;
                String filename = "ObjectId" + cpt;
                byte[] data = ("Data" + cpt).getBytes(StandardCharsets.UTF_8);
                storeObjectInOffers(filename, OBJECT, data, OFFER_ID);
                existingFileNames.add(filename);
            } else {
                int fileIndexToDelete = random.nextInt(existingFileNames.size());
                String filename = existingFileNames.remove(fileIndexToDelete);
                deleteObjectFromOffers(filename, OBJECT, OFFER_ID);
            }
        }

        // When
        Response<Void> offerSyncResponseItemCall = startSynchronization(null);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall, true, null, NB_ACTIONS);

        for (int i = 0; i < cpt; i++) {
            String filename = "ObjectId" + i;

            boolean exists = existingFileNames.contains(filename);
            byte[] expectedData = ("Data" + i).getBytes(StandardCharsets.UTF_8);

            checkFileExistenceAndContent(filename, OBJECT, exists, expectedData, SECOND_OFFER_ID);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testPartialSynchronizationOneOfferFromAnother() throws Exception {
        // Given
        OfferPartialSyncRequest offerPartialSyncRequest = new OfferPartialSyncRequest()
            .setSourceOffer(OFFER_ID)
            .setTargetOffer(SECOND_OFFER_ID)
            .setStrategyId(STRATEGY_ID)
            .setItemsToSynchronize(newArrayList());

        // Write 5 file in source offer tenant 0
        OfferPartialSyncItem offerPartialSyncItem = new OfferPartialSyncItem()
            .setContainer(OBJECT.getCollectionName())
            .setFilenames(newArrayList())
            .setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        for (int i = 0; i < 5; i++) {
            // Save in offer source
            String filename = "ObjectId" + i;
            byte[] data = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            storeObjectInOffers(filename, OBJECT, data, OFFER_ID);
            offerPartialSyncItem.getFilenames().add(filename);
        }
        offerPartialSyncRequest.getItemsToSynchronize().add(offerPartialSyncItem);

        // Write 5 file in source offer tenant 1
        offerPartialSyncItem = new OfferPartialSyncItem()
            .setContainer(OBJECT.getCollectionName())
            .setFilenames(newArrayList())
            .setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        for (int i = 5; i < 10; i++) {
            // Save in offer source
            String filename = "ObjectId" + i;
            byte[] data = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            storeObjectInOffers(filename, OBJECT, data, OFFER_ID);
            offerPartialSyncItem.getFilenames().add(filename);
        }
        offerPartialSyncRequest.getItemsToSynchronize().add(offerPartialSyncItem);

        // Write 5 file in target offer tenant 0
        offerPartialSyncItem = new OfferPartialSyncItem()
            .setContainer(OBJECT.getCollectionName())
            .setFilenames(newArrayList())
            .setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        for (int i = 10; i < 15; i++) {
            // Save in offer source
            String filename = "ObjectId" + i;
            byte[] data = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            storeObjectInOffers(filename, OBJECT, data, SECOND_OFFER_ID);
            offerPartialSyncItem.getFilenames().add(filename);
        }
        offerPartialSyncRequest.getItemsToSynchronize().add(offerPartialSyncItem);

        // Write 5 file in target offer tenant 1
        offerPartialSyncItem = new OfferPartialSyncItem()
            .setContainer(OBJECT.getCollectionName())
            .setFilenames(newArrayList())
            .setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        for (int i = 15; i < 20; i++) {
            // Save in offer source
            String filename = "ObjectId" + i;
            byte[] data = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            storeObjectInOffers(filename, OBJECT, data, SECOND_OFFER_ID);
            offerPartialSyncItem.getFilenames().add(filename);
        }
        offerPartialSyncRequest.getItemsToSynchronize().add(offerPartialSyncItem);

        // Write 5 file in source and target offer tenant 0
        offerPartialSyncItem = new OfferPartialSyncItem()
            .setContainer(OBJECT.getCollectionName())
            .setFilenames(newArrayList())
            .setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        for (int i = 20; i < 25; i++) {
            // Save in offer source
            String filename = "ObjectId" + i;
            byte[] data = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            storeObjectInOffers(filename, OBJECT, data, OFFER_ID, SECOND_OFFER_ID);
            offerPartialSyncItem.getFilenames().add(filename);
        }
        offerPartialSyncRequest.getItemsToSynchronize().add(offerPartialSyncItem);

        // When
        Response<Void> offerSyncResponseItemCall = offerSyncAdminResource
            .startSynchronization(offerPartialSyncRequest, getBasicAuthnToken())
            .execute();
        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall, false, null, 25);

        for (int i = 0; i < 25; i++) {
            String filename = "ObjectId" + i;
            byte[] expectedData = ("Data" + i).getBytes(StandardCharsets.UTF_8);
            if ((i >= 5 && i < 10) || (i >= 15 && i < 20)) {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
            } else {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            }

            // files exists only in target offer. synchro from source offer should remove them as they does not exists in source offer
            if (i >= 10 && i < 20) {
                checkFileExistenceAndContent(filename, OBJECT, false, expectedData, SECOND_OFFER_ID);
            } else {
                checkFileExistenceAndContent(filename, OBJECT, true, expectedData, SECOND_OFFER_ID);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherAlreadySynchronized() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);
        storeObjectInOffers("file2", OBJECT, "data2".getBytes(), OFFER_ID);
        storeObjectInOffers("file3", OBJECT, "data3".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file2", OBJECT, OFFER_ID);

        for (int i = 0; i < 2; i++) {
            // When
            Response<Void> offerSyncResponseItemCall = startSynchronization(null);

            // Then
            verifyOfferSyncStatus(offerSyncResponseItemCall, true, null, 4);

            checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), SECOND_OFFER_ID);
            checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
            checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherStartingFromOffset() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);
        storeObjectInOffers("file2", OBJECT, "data2".getBytes(), OFFER_ID);
        storeObjectInOffers("file3", OBJECT, "data3".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file2", OBJECT, OFFER_ID);

        // When
        Response<Void> offerSyncResponseItemCall = startSynchronization(null);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall, true, null, 4L);
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), SECOND_OFFER_ID);
        checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);

        // Given
        storeObjectInOffers("file4", OBJECT, "data4".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file1", OBJECT, OFFER_ID);

        // When
        Response<Void> offerSyncResponseItemCall2 = startSynchronization(4L);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall2, true, 4L, 6L);
        checkFileExistenceAndContent("file1", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);
        checkFileExistenceAndContent("file4", OBJECT, true, "data4".getBytes(), SECOND_OFFER_ID);
    }

    private Response<Void> startSynchronization(Long offset) throws IOException {
        return offerSyncAdminResource
            .startSynchronization(
                new OfferSyncRequest()
                    .setSourceOffer(OFFER_ID)
                    .setTargetOffer(SECOND_OFFER_ID)
                    .setOffset(offset)
                    .setContainer(DataCategory.OBJECT.getCollectionName())
                    .setStrategyId(STRATEGY_ID)
                    .setTenantId(TENANT_0),
                getBasicAuthnToken()
            )
            .execute();
    }

    private void verifyOfferSyncStatus(
        Response<Void> offerSyncResponseItemCall,
        boolean checkOffsetAndContainer,
        Long startOffset,
        long expectedOffset
    ) throws IOException {
        assertThat(offerSyncResponseItemCall.code()).isEqualTo(200);

        awaitSynchronizationTermination(120);

        Response<OfferSyncStatus> offerSyncStatusResponse = offerSyncAdminResource
            .getLastOfferSynchronizationStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerSyncStatusResponse.code()).isEqualTo(200);
        OfferSyncStatus offerSyncStatus = offerSyncStatusResponse.body();
        assertNotNull(offerSyncStatus);
        assertThat(offerSyncStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerSyncStatus.getStartDate()).isNotNull();
        assertThat(offerSyncStatus.getEndDate()).isNotNull();
        assertThat(offerSyncStatus.getSourceOffer()).isEqualTo(OFFER_ID);
        assertThat(offerSyncStatus.getTargetOffer()).isEqualTo(SECOND_OFFER_ID);
        assertThat(offerSyncStatus.getRequestId()).isEqualTo(offerSyncResponseItemCall.headers().get(X_REQUEST_ID));

        if (checkOffsetAndContainer) {
            assertThat(offerSyncStatus.getContainer()).isEqualTo(DataCategory.OBJECT.getCollectionName());
            assertThat(offerSyncStatus.getStartOffset()).isEqualTo(startOffset);
            assertThat(offerSyncStatus.getCurrentOffset()).isEqualTo(expectedOffset);
        }
    }

    private void deleteObjectFromOffers(String filename, DataCategory dataCategory, String... offerIds)
        throws StorageServerClientException {
        storageClient.delete(STRATEGY_ID, dataCategory, filename, Arrays.asList(offerIds));
    }

    private void awaitSynchronizationTermination(int timeoutInSeconds) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < timeoutInSeconds) {
            Response<Void> offerSynchronizationRunning = offerSyncAdminResource
                .isOfferSynchronizationRunning(getBasicAuthnToken())
                .execute();
            assertThat(offerSynchronizationRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerSynchronizationRunning.headers().get("Running"));
        }
        if (isRunning) {
            fail("Synchronization took too long");
        }
    }

    private void checkFileExistenceAndContent(
        String objectId,
        DataCategory dataCategory,
        boolean exists,
        byte[] expectedData,
        String... offerIds
    ) throws StorageServerClientException, StorageNotFoundClientException {
        JsonNode information = storageClient.getInformation(
            STRATEGY_ID,
            dataCategory,
            objectId,
            Arrays.asList(offerIds),
            true
        );
        assertThat(information).hasSize(exists ? offerIds.length : 0);

        if (exists) {
            Digest expectedDigest = new Digest(DigestType.SHA512);
            expectedDigest.update(expectedData);
            for (String offerId : offerIds) {
                assertThat(information.get(offerId).get("digest").textValue()).isEqualTo(expectedDigest.digestHex());
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds)
        );

        // Then
        assertThat(bulkObjectStoreResponse.getDigestType()).isEqualTo(DigestType.SHA512.getName());
        assertThat(bulkObjectStoreResponse.getOfferIds()).containsExactlyInAnyOrder(OFFER_ID, SECOND_OFFER_ID);

        Map<String, String> fileDigests = new HashMap<>();
        for (int i = 0; i < nbFiles; i++) {
            fileDigests.put(objectIds.get(i), new Digest(DigestType.SHA512).update(files.get(i)).digestHex());
        }
        assertThat(bulkObjectStoreResponse.getObjectDigests()).isEqualTo(fileDigests);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(
                objectIds.get(i),
                DataCategory.OBJECT,
                true,
                FileUtils.readFileToByteArray(files.get(i)),
                OFFER_ID,
                SECOND_OFFER_ID
            );
        }

        checkOfferLog(mongoRuleOffer1, nbFiles);
        checkOfferLog(mongoRuleOffer2, nbFiles);
        checkOfferSequence(mongoRuleOffer1, nbFiles);
        checkOfferSequence(mongoRuleOffer2, nbFiles);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // Unknown uri at index 5
        workspaceUris.set(5, GUIDFactory.newGUID().toString());

        // When / Then
        assertThatThrownBy(
            () ->
                storageClient.bulkStoreFilesFromWorkspace(
                    STRATEGY_ID,
                    new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds)
                )
        ).isInstanceOf(StorageServerClientException.class);

        // Check files have NOT been stored in offers
        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(
                objectIds.get(i),
                DataCategory.OBJECT,
                false,
                FileUtils.readFileToByteArray(files.get(i)),
                OFFER_ID,
                SECOND_OFFER_ID
            );
        }

        checkOfferLog(mongoRuleOffer1, 0);
        checkOfferLog(mongoRuleOffer2, 0);
        checkOfferSequence(mongoRuleOffer1, 0);
        checkOfferSequence(mongoRuleOffer2, 0);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceIdempotency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse1 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds)
        );

        BulkObjectStoreResponse bulkObjectStoreResponse2 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds)
        );

        // Then
        assertThat(bulkObjectStoreResponse1).isEqualToComparingFieldByFieldRecursively(bulkObjectStoreResponse2);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(
                objectIds.get(i),
                DataCategory.OBJECT,
                true,
                FileUtils.readFileToByteArray(files.get(i)),
                OFFER_ID,
                SECOND_OFFER_ID
            );
        }

        checkOfferLog(mongoRuleOffer1, nbFiles * 2);
        checkOfferLog(mongoRuleOffer2, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer1, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer2, nbFiles * 2);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceOverrideRewritableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris1 = new ArrayList<>();
        List<String> workspaceUris2 = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris1.add(GUIDFactory.newGUID().toString());
            workspaceUris2.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files1 = createTempFileSet(nbFiles);
        List<File> files2 = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris1, files1);
        writeFilesToWorkspace(workspaceContainer, workspaceUris2, files2);

        storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris1, UNIT, objectIds)
        );

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse2 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris2, UNIT, objectIds)
        );

        // Then
        assertThat(bulkObjectStoreResponse2.getDigestType()).isEqualTo(DigestType.SHA512.getName());
        assertThat(bulkObjectStoreResponse2.getOfferIds()).containsExactlyInAnyOrder(OFFER_ID, SECOND_OFFER_ID);

        Map<String, String> fileDigests = new HashMap<>();
        for (int i = 0; i < nbFiles; i++) {
            fileDigests.put(objectIds.get(i), new Digest(DigestType.SHA512).update(files2.get(i)).digestHex());
        }
        assertThat(bulkObjectStoreResponse2.getObjectDigests()).isEqualTo(fileDigests);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(
                objectIds.get(i),
                DataCategory.UNIT,
                true,
                FileUtils.readFileToByteArray(files2.get(i)),
                OFFER_ID,
                SECOND_OFFER_ID
            );
        }

        checkOfferLog(mongoRuleOffer1, nbFiles * 2);
        checkOfferLog(mongoRuleOffer2, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer1, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer2, nbFiles * 2);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceKoWhenTryOverrideNonRewritableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris1 = new ArrayList<>();
        List<String> workspaceUris2 = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris1.add(GUIDFactory.newGUID().toString());
            workspaceUris2.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files1 = createTempFileSet(nbFiles);
        List<File> files2 = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris1, files1);

        storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID,
            new BulkObjectStoreRequest(workspaceContainer, workspaceUris1, OBJECT, objectIds)
        );

        writeFilesToWorkspace(workspaceContainer, workspaceUris2, files2);

        // When / Then
        assertThatThrownBy(
            () ->
                storageClient.bulkStoreFilesFromWorkspace(
                    STRATEGY_ID,
                    new BulkObjectStoreRequest(workspaceContainer, workspaceUris2, OBJECT, objectIds)
                )
        ).isInstanceOf(StorageAlreadyExistsClientException.class);
        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(
                objectIds.get(i),
                DataCategory.OBJECT,
                true,
                FileUtils.readFileToByteArray(files1.get(i)),
                OFFER_ID,
                SECOND_OFFER_ID
            );
        }

        checkOfferLog(mongoRuleOffer1, nbFiles);
        checkOfferLog(mongoRuleOffer2, nbFiles);
        checkOfferSequence(mongoRuleOffer1, nbFiles);
        checkOfferSequence(mongoRuleOffer2, nbFiles);
    }

    private void writeFilesToWorkspace(String workspaceContainer, List<String> workspaceUris, List<File> files)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException, IOException {
        // Write to workspace
        if (!workspaceClient.isExistingContainer(workspaceContainer)) {
            workspaceClient.createContainer(workspaceContainer);
        }
        for (int i = 0; i < files.size(); i++) {
            try (InputStream inputStream = new FileInputStream(files.get(i))) {
                workspaceClient.putObject(workspaceContainer, workspaceUris.get(i), inputStream);
            }
        }
    }

    private List<File> createTempFileSet(int nbFiles) throws IOException {
        // Create test files
        List<File> files = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            File file = tempFolder.newFile();
            FileUtils.writeStringToFile(
                file,
                RandomStringUtils.random(ThreadLocalRandom.current().nextInt(100, 10000)),
                StandardCharsets.US_ASCII
            );
            files.add(file);
        }
        return files;
    }

    private void checkOfferLog(MongoRule mongoRuleOffer1, int size) {
        assertThat(mongoRuleOffer1.getMongoCollection(OfferCollections.OFFER_LOG.getName()).countDocuments()).isEqualTo(
            size
        );
    }

    private void checkOfferSequence(MongoRule mongoRule, int size) {
        Document seqDoc = mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find().first();
        long offerSeq = seqDoc == null ? 0L : ((Number) seqDoc.get(OfferSequence.COUNTER_FIELD)).longValue();
        assertThat(offerSeq).isEqualTo(size);
    }

    @Test
    @RunWithCustomExecutor
    public void getBatchObjectInformationTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
        storeObjectInOffers("file2", OBJECT, "data2".getBytes(), OFFER_ID);

        // When
        RequestResponse<BatchObjectInformationResponse> batchObjectInformationResponse =
            storageClient.getBatchObjectInformation(
                STRATEGY_ID,
                OBJECT,
                Arrays.asList(OFFER_ID, SECOND_OFFER_ID),
                Arrays.asList("file1", "file2", "file3")
            );

        // When
        assertThat(batchObjectInformationResponse.isOk()).isTrue();
        Map<String, BatchObjectInformationResponse> objectInformationByObjectId =
            ((RequestResponseOK<BatchObjectInformationResponse>) batchObjectInformationResponse).getResults()
                .stream()
                .collect(Collectors.toMap(BatchObjectInformationResponse::getObjectId, i -> i));

        assertThat(objectInformationByObjectId).containsOnlyKeys("file1", "file2", "file3");
        assertThat(objectInformationByObjectId.get("file1").getOfferDigests()).containsOnlyKeys(
            OFFER_ID,
            SECOND_OFFER_ID
        );
        assertThat(objectInformationByObjectId.get("file1").getOfferDigests().get(OFFER_ID)).isEqualTo(
            "9731b541b22c1d7042646ab2ee17685bbb664bced666d8ecf3593f3ef46493deef651b0f31b6cff8c4df8dcb425a1035e86ddb9877a8685647f39847be0d7c01"
        );
        assertThat(objectInformationByObjectId.get("file1").getOfferDigests().get(SECOND_OFFER_ID)).isEqualTo(
            "9731b541b22c1d7042646ab2ee17685bbb664bced666d8ecf3593f3ef46493deef651b0f31b6cff8c4df8dcb425a1035e86ddb9877a8685647f39847be0d7c01"
        );

        assertThat(objectInformationByObjectId.get("file2").getOfferDigests()).containsOnlyKeys(
            OFFER_ID,
            SECOND_OFFER_ID
        );
        assertThat(objectInformationByObjectId.get("file2").getOfferDigests().get(OFFER_ID)).isEqualTo(
            "5067cebe816ea7a7c8a0e015613c732c67c09d9ddc3b88eb0a8e1b481dd54a72b42d0f7f8f6a77f309ecbc3cc2401cd41ed7deefec18dd9de77d67883b95b6bb"
        );
        assertThat(objectInformationByObjectId.get("file2").getOfferDigests().get(SECOND_OFFER_ID)).isNull();

        assertThat(objectInformationByObjectId.get("file3").getOfferDigests()).containsOnlyKeys(
            OFFER_ID,
            SECOND_OFFER_ID
        );
        assertThat(objectInformationByObjectId.get("file3").getOfferDigests().get(OFFER_ID)).isNull();
        assertThat(objectInformationByObjectId.get("file3").getOfferDigests().get(SECOND_OFFER_ID)).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void offerDiffEmptyOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given : Empty offers

        // When
        Response<Void> startOfferDiff = offerDiffAdminResource
            .startOfferDiff(
                new OfferDiffRequest()
                    .setOffer1(OFFER_ID)
                    .setOffer2(SECOND_OFFER_ID)
                    .setContainer(OBJECT.getCollectionName())
                    .setTenantId(TENANT_0),
                getBasicAuthnToken()
            )
            .execute();

        assertThat(startOfferDiff.isSuccessful()).isTrue();

        // Then
        awaitOfferDiffTermination(60);

        Response<OfferDiffStatus> offerDiffStatusResponse = offerDiffAdminResource
            .getLastOfferDiffStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerDiffStatusResponse.code()).isEqualTo(200);
        OfferDiffStatus offerDiffStatus = offerDiffStatusResponse.body();
        assertThat(offerDiffStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiffStatus.getStartDate()).isNotNull();
        assertThat(offerDiffStatus.getEndDate()).isNotNull();
        assertThat(offerDiffStatus.getOffer1()).isEqualTo(OFFER_ID);
        assertThat(offerDiffStatus.getOffer2()).isEqualTo(SECOND_OFFER_ID);
        assertThat(offerDiffStatus.getTotalObjectCount()).isEqualTo(0L);
        assertThat(offerDiffStatus.getErrorCount()).isEqualTo(0L);
        assertThat(offerDiffStatus.getReportFileName()).isNotNull();
        assertThat(new File(offerDiffStatus.getReportFileName())).hasContent("");

        assertThat(offerDiffStatus.getRequestId()).isEqualTo(startOfferDiff.headers().get(X_REQUEST_ID));
        assertThat(offerDiffStatus.getTenantId()).isEqualTo(TENANT_0);
    }

    @Test
    @RunWithCustomExecutor
    public void offerDiffIsoOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //Given
        for (int i = 0; i < 100; i++) {
            String id = GUIDFactory.newGUID().getId();
            storeObjectInOffers(id, OBJECT, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);
        }

        // When
        Response<Void> startOfferDiff = offerDiffAdminResource
            .startOfferDiff(
                new OfferDiffRequest()
                    .setOffer1(OFFER_ID)
                    .setOffer2(SECOND_OFFER_ID)
                    .setContainer(OBJECT.getCollectionName())
                    .setTenantId(TENANT_0),
                getBasicAuthnToken()
            )
            .execute();

        // Then
        assertThat(startOfferDiff.isSuccessful()).isTrue();

        awaitOfferDiffTermination(60);

        Response<OfferDiffStatus> offerDiffStatusResponse = offerDiffAdminResource
            .getLastOfferDiffStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerDiffStatusResponse.code()).isEqualTo(200);
        OfferDiffStatus offerDiffStatus = offerDiffStatusResponse.body();
        assertThat(offerDiffStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiffStatus.getStartDate()).isNotNull();
        assertThat(offerDiffStatus.getEndDate()).isNotNull();
        assertThat(offerDiffStatus.getOffer1()).isEqualTo(OFFER_ID);
        assertThat(offerDiffStatus.getOffer2()).isEqualTo(SECOND_OFFER_ID);
        assertThat(offerDiffStatus.getTotalObjectCount()).isEqualTo(100L);
        assertThat(offerDiffStatus.getErrorCount()).isEqualTo(0L);
        assertThat(offerDiffStatus.getReportFileName()).isNotNull();
        assertThat(new File(offerDiffStatus.getReportFileName())).hasContent("");

        assertThat(offerDiffStatus.getRequestId()).isEqualTo(startOfferDiff.headers().get(X_REQUEST_ID));
        assertThat(offerDiffStatus.getTenantId()).isEqualTo(TENANT_0);
    }

    @Test
    @RunWithCustomExecutor
    public void offerDiffMismatchingOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //Given
        for (int i = 0; i < 100; i++) {
            String id = GUIDFactory.newGUID().getId();
            storeObjectInOffers(id, OBJECT, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);
        }

        String idMissingFromOffer1 = "idMissingFromOffer1";
        storeObjectInOffers(idMissingFromOffer1, OBJECT, idMissingFromOffer1.getBytes(), SECOND_OFFER_ID);

        String idMissingFromOffer2 = "idMissingFromOffer2";
        storeObjectInOffers(idMissingFromOffer2, OBJECT, idMissingFromOffer2.getBytes(), OFFER_ID);

        String idMismatch = "idMismatch";
        storeObjectInOffers(idMismatch, OBJECT, new byte[50], OFFER_ID);
        storeObjectInOffers(idMismatch, OBJECT, new byte[10], SECOND_OFFER_ID);

        // When
        Response<Void> startOfferDiff = offerDiffAdminResource
            .startOfferDiff(
                new OfferDiffRequest()
                    .setOffer1(OFFER_ID)
                    .setOffer2(SECOND_OFFER_ID)
                    .setContainer(OBJECT.getCollectionName())
                    .setTenantId(TENANT_0),
                getBasicAuthnToken()
            )
            .execute();

        // Then
        assertThat(startOfferDiff.isSuccessful()).isTrue();

        awaitOfferDiffTermination(60);

        Response<OfferDiffStatus> offerDiffStatusResponse = offerDiffAdminResource
            .getLastOfferDiffStatus(getBasicAuthnToken())
            .execute();
        assertThat(offerDiffStatusResponse.code()).isEqualTo(200);
        OfferDiffStatus offerDiffStatus = offerDiffStatusResponse.body();
        assertThat(offerDiffStatus.getStatusCode()).isEqualTo(StatusCode.WARNING);
        assertThat(offerDiffStatus.getStartDate()).isNotNull();
        assertThat(offerDiffStatus.getEndDate()).isNotNull();
        assertThat(offerDiffStatus.getOffer1()).isEqualTo(OFFER_ID);
        assertThat(offerDiffStatus.getOffer2()).isEqualTo(SECOND_OFFER_ID);
        assertThat(offerDiffStatus.getTotalObjectCount()).isEqualTo(103L);
        assertThat(offerDiffStatus.getErrorCount()).isEqualTo(3L);
        assertThat(offerDiffStatus.getReportFileName()).isNotNull();
        assertThat(new File(offerDiffStatus.getReportFileName())).hasContent(
            "" +
            "{\"objectId\":\"idMismatch\",\"sizeInOffer1\":50,\"sizeInOffer2\":10}\n" +
            "{\"objectId\":\"idMissingFromOffer1\",\"sizeInOffer1\":null,\"sizeInOffer2\":19}\n" +
            "{\"objectId\":\"idMissingFromOffer2\",\"sizeInOffer1\":19,\"sizeInOffer2\":null}"
        );

        assertThat(offerDiffStatus.getRequestId()).isEqualTo(startOfferDiff.headers().get(X_REQUEST_ID));
        assertThat(offerDiffStatus.getTenantId()).isEqualTo(TENANT_0);
    }

    @Test
    @RunWithCustomExecutor
    public void getObjectsFromOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // GIVEN
        String objectToInsert = GUIDFactory.newGUID().getId();

        // WHEN
        storeObjectInAllOffers(objectToInsert, OBJECT, new ByteArrayInputStream(objectToInsert.getBytes()));
        deleteObjectFromOffers(objectToInsert, OBJECT, SECOND_OFFER_ID);

        // By default we read from the offre who has the higher priority which is default2 in this case
        assertThatThrownBy(
            () ->
                storageClient.getContainerAsync(STRATEGY_ID, objectToInsert, OBJECT, AccessLogUtils.getNoLogAccessLog())
        ).isInstanceOf(StorageNotFoundException.class);

        // Try to get inserted Object from first offer
        javax.ws.rs.core.Response responseStorage = storageClient.getContainerAsync(
            STRATEGY_ID,
            OFFER_ID,
            objectToInsert,
            OBJECT,
            AccessLogUtils.getNoLogAccessLog()
        );
        // THEN
        InputStream inputStream = responseStorage.readEntity(InputStream.class);
        SizedInputStream sizedInputStream = new SizedInputStream(inputStream);
        final long size = StreamUtils.closeSilently(sizedInputStream);

        assertEquals(objectToInsert.getBytes().length, size);
    }

    @Test
    @RunWithCustomExecutor
    public void listContainerObjectsWithMultipleOffersInStrategy() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // GIVEN
        String firstObjectToInsert = GUIDFactory.newGUID().getId();
        String secondObjectToInsert = GUIDFactory.newGUID().getId();

        // WHEN
        storeObjectInAllOffers(firstObjectToInsert, OBJECT, new ByteArrayInputStream(firstObjectToInsert.getBytes()));
        storeObjectInAllOffers(secondObjectToInsert, OBJECT, new ByteArrayInputStream(secondObjectToInsert.getBytes()));

        // Delete inserted object from offer "default2" to check that the file's size is getted from offer "default"
        deleteObjectFromOffers(firstObjectToInsert, OBJECT, SECOND_OFFER_ID);

        // THEN

        CloseableIterator<ObjectEntry> resultForFirstObjectInserted = storageClient.listContainer(
            STRATEGY_ID,
            null,
            DataCategory.OBJECT
        );
        assertNotNull(resultForFirstObjectInserted);
        assertEquals(1, getFilesCountFromIterator(resultForFirstObjectInserted));

        // Delete second inserted object from offer "default" to check that the file's size is getted from offer "default"
        deleteObjectFromOffers(secondObjectToInsert, OBJECT, OFFER_ID);
        CloseableIterator<ObjectEntry> resultForSecondObjectInserted = storageClient.listContainer(
            STRATEGY_ID,
            null,
            DataCategory.OBJECT
        );
        assertNotNull(resultForSecondObjectInserted);
        assertEquals(1, getFilesCountFromIterator(resultForSecondObjectInserted));
    }

    @Test
    @RunWithCustomExecutor
    public void getReferentOffer() throws Exception {
        // GIVEN
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // WHEN
        String referentOfferId = storageClient.getReferentOffer(STRATEGY_ID);

        // THEN
        assertEquals(OFFER_ID, referentOfferId);
    }

    @Test
    @RunWithCustomExecutor
    public void getReferentOfferFromUnknownStrategy() {
        // GIVEN
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // WHEN &THEN
        assertThatThrownBy(() -> storageClient.getReferentOffer("unknown Strategy")).isInstanceOf(
            StorageServerClientException.class
        );
    }

    private int getFilesCountFromIterator(CloseableIterator<ObjectEntry> resultForFirstObjectInserted) {
        int count = 0;
        while (resultForFirstObjectInserted.hasNext()) {
            count++;
            assertNotNull(resultForFirstObjectInserted.next());
        }
        return count;
    }

    private void awaitOfferDiffTermination(int timeoutInSeconds) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < timeoutInSeconds) {
            Response offerSynchronizationRunning = offerDiffAdminResource
                .isOfferDiffRunning(getBasicAuthnToken())
                .execute();
            assertThat(offerSynchronizationRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerSynchronizationRunning.headers().get("Running"));
        }
        if (isRunning) {
            fail("Offer diff took too long");
        }
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface OfferSyncAdminResource {
        @POST("/storage/v1/offerPartialSync")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startSynchronization(
            @Body OfferPartialSyncRequest offerPartialSyncRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @POST("/storage/v1/offerSync")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startSynchronization(
            @Body OfferSyncRequest offerSyncRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @HEAD("/storage/v1/offerSync")
        Call<Void> isOfferSynchronizationRunning(@Header("Authorization") String basicAuthnToken);

        @GET("/storage/v1/offerSync")
        @Headers({ "Content-Type: application/json" })
        Call<OfferSyncStatus> getLastOfferSynchronizationStatus(@Header("Authorization") String basicAuthnToken);
    }

    public interface OfferDiffAdminResource {
        @POST("/storage/v1/diff")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startOfferDiff(
            @Body OfferDiffRequest offerDiffRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @HEAD("/storage/v1/diff")
        Call<Void> isOfferDiffRunning(@Header("Authorization") String basicAuthnToken);

        @GET("/storage/v1/diff")
        @Headers({ "Content-Type: application/json" })
        Call<OfferDiffStatus> getLastOfferDiffStatus(@Header("Authorization") String basicAuthnToken);
    }
}
