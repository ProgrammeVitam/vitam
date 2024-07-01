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
package fr.gouv.vitam.storage.offers.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.cas.container.builder.StoreContextBuilder;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.database.OfferLogAndCompactedOfferLogService;
import fr.gouv.vitam.storage.offers.database.OfferLogCompactionDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.rest.OfferLogCompactionConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.VitamConfiguration.getDefaultDigestType;
import static fr.gouv.vitam.storage.engine.common.model.Order.ASC;
import static fr.gouv.vitam.storage.engine.common.model.Order.DESC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultOfferServiceTest {

    private static final String CONTAINER_PATH = "container";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String ARCHIVE_FILE2_TXT = "archivefile2.txt";
    private static final String ARCHIVE_FILE3_TXT = "archivefile3.txt";
    private static final String OBJECT_ID_2_CONTENT = "Vitam Test Content";
    private static final String FAKE_CONTAINER = "fakeContainer";
    private static final String OBJECT = "object_";
    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";

    private static final String OBJECT_ID = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_2 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_3 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_DELETE = GUIDFactory.newObjectGUID(0).getId();

    private static final DataCategory UNIT_TYPE = DataCategory.UNIT;
    private static final DataCategory OBJECT_TYPE = DataCategory.OBJECT;
    public static final int MAX_BATCH_THREAD_POOL_SIZE = 16;
    public static final int BATCH_METADATA_COMPUTATION_TIMEOUT = 600;

    public DefaultOfferServiceImpl offerService;
    private ContentAddressableStorage defaultStorage;
    private StorageConfiguration configuration;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private OfferLogDatabaseService offerDatabaseService;

    @Mock
    private OfferLogCompactionDatabaseService offerLogCompactionDatabaseService;

    @Mock
    private OfferSequenceDatabaseService offerSequenceDatabaseService;

    @Mock
    private OfferLogAndCompactedOfferLogService offerLogAndCompactedOfferLogService;

    @Mock
    MongoDatabase mongoDatabase;

    @BeforeClass
    public static void beforeClass() {
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    @Before
    public void init() throws Exception {
        File confFile = PropertiesUtils.findFile(DEFAULT_STORAGE_CONF);
        ObjectNode conf = PropertiesUtils.readYaml(confFile, ObjectNode.class);
        conf.put("storagePath", tempFolder.getRoot().getAbsolutePath());
        PropertiesUtils.writeYaml(confFile, conf);

        configuration = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class
        );
        if (!Strings.isNullOrEmpty(configuration.getStoragePath())) {
            configuration.setStoragePath(FileUtil.getFileCanonicalPath(configuration.getStoragePath()));
        }

        defaultStorage = StoreContextBuilder.newStoreContext(configuration, mongoDatabase);

        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );
    }

    @Test
    public void createObjectTestNoContainer() throws Exception {
        offerService.createObject(
            FAKE_CONTAINER,
            OBJECT_ID,
            new FakeInputStream(1024),
            OBJECT_TYPE,
            1024L,
            VitamConfiguration.getDefaultDigestType()
        );
    }

    @Test
    public void createContainerTest() throws Exception {
        assertNotNull(offerService);

        offerService.ensureContainerExists(CONTAINER_PATH);

        // check
        final File container = new File(tempFolder.getRoot(), CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        offerService.ensureContainerExists(CONTAINER_PATH);
    }

    @Test
    public void createObjectTest() throws Exception {
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID)
        ).thenReturn(1L);
        String computedDigest;

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigest = offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                OBJECT_TYPE,
                8766L,
                VitamConfiguration.getDefaultDigestType()
            );
        }
        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + OBJECT_ID);
        assertThat(testFile).hasSameContentAs(offerFile);

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString()
        );

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService).save(CONTAINER_PATH, OBJECT_ID, OfferLogAction.WRITE, 1L);
    }

    @Test
    public void createObject_OverrideExistingUpdatableObject() throws Exception {
        // object
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID)
        ).thenReturn(1L);
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                UNIT_TYPE,
                8766L,
                VitamConfiguration.getDefaultDigestType()
            );
        }
        String computedDigestV2;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE2_TXT))) {
            computedDigestV2 = offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                UNIT_TYPE,
                13L,
                VitamConfiguration.getDefaultDigestType()
            );
        }

        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE2_TXT);
        final File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + OBJECT_ID);
        assertThat(testFile).hasSameContentAs(offerFile);

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV2, digest.toString());
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString()
        );

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(2)).save(CONTAINER_PATH, OBJECT_ID, OfferLogAction.WRITE, 1L);
    }

    @Test
    public void createObject_OverrideExistingNonUpdatableObjectWithSameContent() throws Exception {
        // object
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID)
        ).thenReturn(10L);
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                OBJECT_TYPE,
                8766L,
                VitamConfiguration.getDefaultDigestType()
            );
        }
        String computedDigestV2;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigestV2 = offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                OBJECT_TYPE,
                8766L,
                VitamConfiguration.getDefaultDigestType()
            );
        }

        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + OBJECT_ID);
        assertThat(testFile).hasSameContentAs(offerFile);

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV2, digest.toString());
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString()
        );

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(2)).save(CONTAINER_PATH, OBJECT_ID, OfferLogAction.WRITE, 10L);
    }

    @Test
    public void createObject_TryOverrideExistingNonUpdatableObjectWithDifferentContentFails() throws Exception {
        // Given
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID)
        ).thenReturn(1L);
        String computedDigestV1;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigestV1 = offerService.createObject(
                CONTAINER_PATH,
                OBJECT_ID,
                in,
                OBJECT_TYPE,
                8766L,
                VitamConfiguration.getDefaultDigestType()
            );
        }

        // When / Then
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE2_TXT))) {
            assertThatThrownBy(
                () ->
                    offerService.createObject(
                        CONTAINER_PATH,
                        OBJECT_ID,
                        in,
                        OBJECT_TYPE,
                        13L,
                        VitamConfiguration.getDefaultDigestType()
                    )
            ).isInstanceOf(NonUpdatableContentAddressableStorageException.class);
        }

        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + OBJECT_ID);
        assertThat(testFile).hasSameContentAs(offerFile);

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV1, digest.toString());
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString()
        );

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(1)).save(CONTAINER_PATH, OBJECT_ID, OfferLogAction.WRITE, 1L);
    }

    @Test
    public void getObjectTest() throws Exception {
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(
            CONTAINER_PATH,
            OBJECT_ID_2,
            streamToStore,
            OBJECT_TYPE,
            18L,
            VitamConfiguration.getDefaultDigestType()
        );

        final ObjectContent response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_2);
        assertNotNull(response);
        assertNotNull(response.getInputStream());
        response.getInputStream().close();
    }

    @Test
    public void getCapacityOk() throws Exception {
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(
            CONTAINER_PATH,
            OBJECT_ID_2,
            streamToStore,
            OBJECT_TYPE,
            18L,
            VitamConfiguration.getDefaultDigestType()
        );

        // check
        final File container = new File(tempFolder.getRoot(), CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        final ContainerInformation capacity = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(capacity);
        assertThat(capacity.getUsableSpace()).isGreaterThan(0L);
    }

    @Test
    public void getCapacityNoContainerOK() throws Exception {
        assertNotNull(offerService);
        final ContainerInformation capacity = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(capacity);
        assertThat(capacity.getUsableSpace()).isGreaterThan(0L);
    }

    @Test
    public void deleteObjectTest() throws Exception {
        assertNotNull(offerService);

        // creation of an object
        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);

        offerService.createObject(
            CONTAINER_PATH,
            OBJECT_ID_DELETE,
            streamToStore,
            DataCategory.UNIT,
            18L,
            VitamConfiguration.getDefaultDigestType()
        );

        // check if the object has been created
        final ObjectContent response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
        assertNotNull(response);
        assertNotNull(response.getInputStream());
        response.getInputStream().close();

        // check that if we try to delete an object
        // algorithm, it succeeds
        offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, DataCategory.UNIT);

        try {
            // check that the object has been deleted
            offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {}
    }

    @Test
    public void listCursorTest() throws Exception {
        assertNotNull(offerService);
        for (int i = 0; i < 150; i++) {
            offerService.createObject(
                CONTAINER_PATH,
                OBJECT + i,
                new FakeInputStream(50),
                OBJECT_TYPE,
                50L,
                VitamConfiguration.getDefaultDigestType()
            );
        }
        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);
        offerService.listObjects(CONTAINER_PATH, objectListingListener);
        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(150)).handleObjectEntry(objectEntryArgumentCaptor.capture());
        Set<String> objectIds = objectEntryArgumentCaptor
            .getAllValues()
            .stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedObjectIds = IntStream.range(0, 150).mapToObj(i -> OBJECT + i).collect(Collectors.toSet());
        assertThat(objectIds).isEqualTo(expectedObjectIds);
    }

    @Test
    public void listObjectMissingContainer() throws Exception {
        // Given
        String unknownContainer = "unknown-container-" + GUIDFactory.newGUID();
        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        // When
        offerService.listObjects(unknownContainer, objectListingListener);

        // Then
        File containerFolder = new File(tempFolder.getRoot(), unknownContainer);
        assertThat(containerFolder).exists();
        assertThat(containerFolder).isDirectory();
        verifyNoMoreInteractions(objectListingListener);
    }

    @Test
    public void getOfferLogs() {
        when(offerDatabaseService.getDescendingOfferLogsBy(CONTAINER_PATH, 0L, 2)).thenReturn(
            getOfferLogs(CONTAINER_PATH, 0, 2)
        );
        when(offerDatabaseService.getDescendingOfferLogsBy(CONTAINER_PATH, 2L, 3)).thenThrow(MongoWriteException.class);
        assertNotNull(offerService);

        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 0L, 2, Order.DESC);
        }).doesNotThrowAnyException();
        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 2L, 3, Order.DESC);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
    }

    private List<OfferLog> getOfferLogs(String containerName, long offset, int limit) {
        return LongStream.range(offset + 1, offset + 1 + limit)
            .mapToObj(
                l ->
                    new OfferLog(containerName, OBJECT + l, OfferLogAction.WRITE)
                        .setSequence(l)
                        .setTime(LocalDateUtil.now())
            )
            .collect(Collectors.toList());
    }

    @Test
    @RunWithCustomExecutor
    public void bulkPutObjectsSingleEntry() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 1L)
        ).thenReturn(10L);
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        MultiplexedStreamReader multiplexedStreamReader = createMultiplexedStreamReader(file1);

        // When
        StorageBulkPutResult storageBulkPutResult = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Collections.singletonList(OBJECT_ID),
            multiplexedStreamReader,
            OBJECT_TYPE,
            DigestType.SHA512
        );

        // Then
        assertThat(storageBulkPutResult.getEntries()).hasSize(1);
        StorageBulkPutResultEntry entry1 = storageBulkPutResult.getEntries().get(0);
        checkFile(file1, offerService, entry1, OBJECT_ID);

        verify(offerDatabaseService).bulkSave(
            eq(CONTAINER_PATH),
            eq(Collections.singletonList(OBJECT_ID)),
            eq(OfferLogAction.WRITE),
            eq(10L)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkPutObjectsMultipleEntries() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 1L)
        ).thenReturn(10L);
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File file2 = PropertiesUtils.findFile(ARCHIVE_FILE2_TXT);
        File file3 = PropertiesUtils.findFile(ARCHIVE_FILE3_TXT);
        MultiplexedStreamReader multiplexedStreamReader = createMultiplexedStreamReader(file1, file2, file3);

        // When
        StorageBulkPutResult storageBulkPutResult = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Arrays.asList(OBJECT_ID, OBJECT_ID_2, OBJECT_ID_3),
            multiplexedStreamReader,
            OBJECT_TYPE,
            DigestType.SHA512
        );

        // Then
        assertThat(storageBulkPutResult.getEntries()).hasSize(3);
        checkFile(file1, offerService, storageBulkPutResult.getEntries().get(0), OBJECT_ID);
        checkFile(file2, offerService, storageBulkPutResult.getEntries().get(1), OBJECT_ID_2);
        checkFile(file3, offerService, storageBulkPutResult.getEntries().get(2), OBJECT_ID_3);

        verify(offerDatabaseService).bulkSave(
            eq(CONTAINER_PATH),
            eq(Arrays.asList(OBJECT_ID, OBJECT_ID_2, OBJECT_ID_3)),
            eq(OfferLogAction.WRITE),
            eq(0L)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkPutObjectsUpdateNonUpdatableObjectWithSameContent() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 1L)
        ).thenReturn(1L);
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);

        // When
        MultiplexedStreamReader multiplexedStreamReader = createMultiplexedStreamReader(file1);
        StorageBulkPutResult storageBulkPutResult1 = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Collections.singletonList(OBJECT_ID),
            multiplexedStreamReader,
            OBJECT_TYPE,
            DigestType.SHA512
        );

        MultiplexedStreamReader multiplexedStreamReader2 = createMultiplexedStreamReader(file1);
        StorageBulkPutResult storageBulkPutResult2 = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Collections.singletonList(OBJECT_ID),
            multiplexedStreamReader2,
            OBJECT_TYPE,
            DigestType.SHA512
        );

        // Then
        assertThat(storageBulkPutResult1.getEntries()).hasSize(1);
        checkFile(file1, offerService, storageBulkPutResult1.getEntries().get(0), OBJECT_ID);

        assertThat(storageBulkPutResult2.getEntries()).hasSize(1);
        checkFile(file1, offerService, storageBulkPutResult2.getEntries().get(0), OBJECT_ID);

        verify(offerDatabaseService, times(2)).bulkSave(
            eq(CONTAINER_PATH),
            eq(Collections.singletonList(OBJECT_ID)),
            eq(OfferLogAction.WRITE),
            eq(1L)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkPutObjectsUpdateNonUpdatableObjectWithDifferentContent() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 1L)
        ).thenReturn(10L);
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File file2 = PropertiesUtils.findFile(ARCHIVE_FILE2_TXT);

        // When
        MultiplexedStreamReader multiplexedStreamReader = createMultiplexedStreamReader(file1);
        StorageBulkPutResult storageBulkPutResult1 = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Collections.singletonList(OBJECT_ID),
            multiplexedStreamReader,
            OBJECT_TYPE,
            DigestType.SHA512
        );

        // Try import again
        assertThatThrownBy(() -> {
            MultiplexedStreamReader multiplexedStreamReader2 = createMultiplexedStreamReader(file2);
            offerService.bulkPutObjects(
                CONTAINER_PATH,
                Collections.singletonList(OBJECT_ID),
                multiplexedStreamReader2,
                OBJECT_TYPE,
                DigestType.SHA512
            );
        }).isInstanceOf(NonUpdatableContentAddressableStorageException.class);

        // Then (check non updated)
        assertThat(storageBulkPutResult1.getEntries()).hasSize(1);
        checkFile(file1, offerService, storageBulkPutResult1.getEntries().get(0), OBJECT_ID);

        verify(offerDatabaseService).bulkSave(
            eq(CONTAINER_PATH),
            eq(Collections.singletonList(OBJECT_ID)),
            eq(OfferLogAction.WRITE),
            eq(10L)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkPutObjectsUpdateUpdatableObjectWithDifferentContent() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
        when(
            offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, 1L)
        ).thenReturn(1L);
        File file1 = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        File file2 = PropertiesUtils.findFile(ARCHIVE_FILE2_TXT);

        // When
        MultiplexedStreamReader multiplexedStreamReader = createMultiplexedStreamReader(file1);
        StorageBulkPutResult storageBulkPutResult1 = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Arrays.asList(OBJECT_ID),
            multiplexedStreamReader,
            UNIT_TYPE,
            DigestType.SHA512
        );

        MultiplexedStreamReader multiplexedStreamReader2 = createMultiplexedStreamReader(file2);
        StorageBulkPutResult storageBulkPutResult2 = offerService.bulkPutObjects(
            CONTAINER_PATH,
            Arrays.asList(OBJECT_ID),
            multiplexedStreamReader2,
            UNIT_TYPE,
            DigestType.SHA512
        );

        // Then (check updated content)
        assertThat(storageBulkPutResult1.getEntries()).hasSize(1);
        assertThat(storageBulkPutResult2.getEntries()).hasSize(1);
        checkFile(file2, offerService, storageBulkPutResult2.getEntries().get(0), OBJECT_ID);

        verify(offerDatabaseService, times(2)).bulkSave(
            eq(CONTAINER_PATH),
            eq(Collections.singletonList(OBJECT_ID)),
            eq(OfferLogAction.WRITE),
            eq(1L)
        );
    }

    private MultiplexedStreamReader createMultiplexedStreamReader(File... files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(byteArrayOutputStream);

        for (File file : files) {
            try (FileInputStream in = new FileInputStream(file)) {
                multiplexedStreamWriter.appendEntry(file.length(), in);
            }
        }
        multiplexedStreamWriter.appendEndOfFile();
        return new MultiplexedStreamReader(byteArrayOutputStream.toInputStream());
    }

    private void checkFile(
        File testFile,
        DefaultOfferService offerService,
        StorageBulkPutResultEntry entry,
        String objectId
    ) throws IOException, ContentAddressableStorageException {
        File offerFile = new File(tempFolder.getRoot(), CONTAINER_PATH + "/" + objectId);
        assertThat(offerFile).hasSameContentAs(testFile);

        Digest digest = Digest.digest(testFile, getDefaultDigestType());

        assertEquals(entry.getDigest(), digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, objectId, getDefaultDigestType()), digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, objectId));
    }

    @Test
    public void should_compact_logs_into_compaction_collection() throws Exception {
        // Given
        OfferLogCompactionConfiguration config = new OfferLogCompactionConfiguration(1, ChronoUnit.SECONDS, 4);
        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            config,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        List<OfferLog> logs = Arrays.asList(
            new OfferLog(1, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(2, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(3, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(4, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE)
        );

        when(
            offerDatabaseService.getExpiredOfferLogByContainer(config.getExpirationValue(), config.getExpirationUnit())
        ).thenReturn(toCloseableIterable(logs));

        // When
        offerService.compactOfferLogs();

        // Then
        verify(offerLogAndCompactedOfferLogService, times(1)).almostTransactionalSaveAndDelete(
            any(CompactedOfferLog.class),
            eq(logs)
        );
    }

    @Test
    public void should_compaction_contains_4_logs() throws Exception {
        // Given
        OfferLogCompactionConfiguration config = new OfferLogCompactionConfiguration(15, ChronoUnit.SECONDS, 4);
        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            config,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        List<OfferLog> logs = Arrays.asList(
            new OfferLog(1, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(2, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(3, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE),
            new OfferLog(4, LocalDateUtil.now(), "container", "filename", OfferLogAction.WRITE)
        );

        when(
            offerDatabaseService.getExpiredOfferLogByContainer(config.getExpirationValue(), config.getExpirationUnit())
        ).thenReturn(toCloseableIterable(logs));

        CompactedOfferLog compactedOfferLogExpected = new CompactedOfferLog(
            1,
            4,
            LocalDateUtil.now(),
            "container",
            logs
        );
        ArgumentCaptor<CompactedOfferLog> compactionSaved = ArgumentCaptor.forClass(CompactedOfferLog.class);

        doNothing()
            .when(offerLogAndCompactedOfferLogService)
            .almostTransactionalSaveAndDelete(compactionSaved.capture(), anyList());

        // When
        offerService.compactOfferLogs();

        // Then
        verify(offerLogAndCompactedOfferLogService, times(1)).almostTransactionalSaveAndDelete(
            any(CompactedOfferLog.class),
            eq(logs)
        );
        assertThat(compactionSaved.getValue()).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
    }

    @Test
    public void should_save_multiple_compaction_when_different_container() throws Exception {
        // Given
        OfferLogCompactionConfiguration config = new OfferLogCompactionConfiguration(15, ChronoUnit.SECONDS, 4);
        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            config,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        List<OfferLog> logs1 = Arrays.asList(
            new OfferLog(1, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE),
            new OfferLog(3, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE),
            new OfferLog(5, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE)
        );

        List<OfferLog> logs2 = Collections.singletonList(
            new OfferLog(2, LocalDateUtil.now(), "container2", "filename", OfferLogAction.WRITE)
        );
        List<OfferLog> logs3 = Collections.singletonList(
            new OfferLog(4, LocalDateUtil.now(), "container3", "filename", OfferLogAction.WRITE)
        );

        List<OfferLog> logs = Stream.of(logs1, logs2, logs3).flatMap(Collection::stream).collect(Collectors.toList());

        when(
            offerDatabaseService.getExpiredOfferLogByContainer(config.getExpirationValue(), config.getExpirationUnit())
        ).thenReturn(toCloseableIterable(logs));

        CompactedOfferLog compactedOfferLogExpected1 = new CompactedOfferLog(
            1,
            5,
            LocalDateUtil.now(),
            "container1",
            logs1
        );
        CompactedOfferLog compactedOfferLogExpected2 = new CompactedOfferLog(
            2,
            2,
            LocalDateUtil.now(),
            "container2",
            logs2
        );
        CompactedOfferLog compactedOfferLogExpected3 = new CompactedOfferLog(
            4,
            4,
            LocalDateUtil.now(),
            "container3",
            logs3
        );
        ArgumentCaptor<CompactedOfferLog> compactionSaved = ArgumentCaptor.forClass(CompactedOfferLog.class);

        doNothing()
            .when(offerLogAndCompactedOfferLogService)
            .almostTransactionalSaveAndDelete(compactionSaved.capture(), anyList());

        // When
        offerService.compactOfferLogs();

        // Then
        verify(offerLogAndCompactedOfferLogService, times(3)).almostTransactionalSaveAndDelete(
            any(CompactedOfferLog.class),
            anyList()
        );
        assertThat(compactionSaved.getAllValues().get(0)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected1,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
        assertThat(compactionSaved.getAllValues().get(1)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected2,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
        assertThat(compactionSaved.getAllValues().get(2)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected3,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
    }

    @Test
    public void should_only_compact_bulk_of_2_logs() throws Exception {
        // Given

        OfferLogCompactionConfiguration config = new OfferLogCompactionConfiguration(15, ChronoUnit.SECONDS, 2);
        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            config,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        OfferLog offerLog = new OfferLog(1, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);
        OfferLog offerLog1 = new OfferLog(2, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);
        OfferLog offerLog2 = new OfferLog(3, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);
        OfferLog offerLog3 = new OfferLog(4, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);
        OfferLog offerLog4 = new OfferLog(5, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);
        OfferLog offerLog5 = new OfferLog(6, LocalDateUtil.now(), "container1", "filename", OfferLogAction.WRITE);

        List<OfferLog> logs = Arrays.asList(offerLog, offerLog1, offerLog2, offerLog3, offerLog4, offerLog5);

        when(
            offerDatabaseService.getExpiredOfferLogByContainer(config.getExpirationValue(), config.getExpirationUnit())
        ).thenReturn(toCloseableIterable(logs));

        List<OfferLog> logs1 = Arrays.asList(offerLog, offerLog1);
        List<OfferLog> logs2 = Arrays.asList(offerLog2, offerLog3);
        List<OfferLog> logs3 = Arrays.asList(offerLog4, offerLog5);

        CompactedOfferLog compactedOfferLogExpected1 = new CompactedOfferLog(
            1,
            2,
            LocalDateUtil.now(),
            "container1",
            logs1
        );
        CompactedOfferLog compactedOfferLogExpected2 = new CompactedOfferLog(
            3,
            4,
            LocalDateUtil.now(),
            "container1",
            logs2
        );
        CompactedOfferLog compactedOfferLogExpected3 = new CompactedOfferLog(
            5,
            6,
            LocalDateUtil.now(),
            "container1",
            logs3
        );
        ArgumentCaptor<CompactedOfferLog> compactionSaved = ArgumentCaptor.forClass(CompactedOfferLog.class);

        doNothing()
            .when(offerLogAndCompactedOfferLogService)
            .almostTransactionalSaveAndDelete(compactionSaved.capture(), anyList());

        // When
        offerService.compactOfferLogs();

        // Then
        verify(offerLogAndCompactedOfferLogService, times(3)).almostTransactionalSaveAndDelete(
            any(CompactedOfferLog.class),
            anyList()
        );

        assertThat(compactionSaved.getAllValues().get(0)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected1,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
        assertThat(compactionSaved.getAllValues().get(1)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected2,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
        assertThat(compactionSaved.getAllValues().get(2)).isEqualToComparingOnlyGivenFields(
            compactedOfferLogExpected3,
            "SequenceStart",
            "SequenceEnd",
            "Container",
            "Logs"
        );
    }

    @Test
    public void should_do_nothing_when_no_logs_to_compact() throws Exception {
        // Given
        OfferLogCompactionConfiguration config = new OfferLogCompactionConfiguration(1, ChronoUnit.SECONDS, 4);
        offerService = new DefaultOfferServiceImpl(
            defaultStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            config,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        when(
            offerDatabaseService.getExpiredOfferLogByContainer(config.getExpirationValue(), config.getExpirationUnit())
        ).thenReturn(toCloseableIterable(Collections.emptyList()));

        // When
        offerService.compactOfferLogs();

        // Then
        verify(offerLogAndCompactedOfferLogService, times(0)).almostTransactionalSaveAndDelete(
            any(CompactedOfferLog.class),
            anyList()
        );
    }

    @Test
    public void should_search_descending_only_on_offer_log() throws Exception {
        // Given
        List<OfferLog> expectedOfferLogs = Arrays.asList(
            new OfferLog(5, LocalDateUtil.now(), "containerName", "fileName1", OfferLogAction.WRITE),
            new OfferLog(4, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE),
            new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE)
        );
        when(offerDatabaseService.getDescendingOfferLogsBy("containerName", 5L, 3)).thenReturn(expectedOfferLogs);

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 5L, 3, DESC);

        // Then
        assertThat(logs).isEqualTo(expectedOfferLogs);
        verify(offerDatabaseService).getDescendingOfferLogsBy("containerName", 5L, 3);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogCompactionDatabaseService);
    }

    @Test
    public void should_search_descending_on_logs_and_on_compaction() throws Exception {
        // Given
        OfferLog offerLog = new OfferLog(5, LocalDateUtil.now(), "containerName", "fileName5", OfferLogAction.WRITE);
        OfferLog offerLog1 = new OfferLog(4, LocalDateUtil.now(), "containerName", "fileName4", OfferLogAction.WRITE);
        OfferLog offerLog2 = new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE);

        OfferLog offerLog3 = new OfferLog(2, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE);

        List<OfferLog> expectedOfferLogs = Arrays.asList(offerLog, offerLog1, offerLog2);
        when(offerDatabaseService.getDescendingOfferLogsBy("containerName", 5L, 4)).thenReturn(expectedOfferLogs);

        List<OfferLog> compactedOfferLogs = Collections.singletonList(offerLog3);
        when(offerLogCompactionDatabaseService.getDescendingOfferLogCompactionBy("containerName", 2L, 1)).thenReturn(
            compactedOfferLogs
        );

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 5L, 4, DESC);

        // Then
        assertThat(logs).containsExactly(offerLog, offerLog1, offerLog2, offerLog3);
        verify(offerDatabaseService, times(1)).getDescendingOfferLogsBy("containerName", 5L, 4);
        verify(offerLogCompactionDatabaseService, times(1)).getDescendingOfferLogCompactionBy("containerName", 2L, 1);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogCompactionDatabaseService);
    }

    @Test
    public void should_search_ascending_only_on_offer_log_compaction() throws Exception {
        // Given
        List<OfferLog> expectedOfferLogs = Arrays.asList(
            new OfferLog(1, LocalDateUtil.now(), "containerName", "fileName1", OfferLogAction.WRITE),
            new OfferLog(2, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE),
            new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE)
        );
        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 1L, 3)).thenReturn(
            expectedOfferLogs
        );

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 1L, 3, ASC);

        // Then
        assertThat(logs).isEqualTo(expectedOfferLogs);
        verify(offerLogCompactionDatabaseService, times(1)).getAscendingOfferLogCompactionBy("containerName", 1L, 3);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogCompactionDatabaseService);
    }

    @Test
    public void should_search_ascending_on_offer_log_compaction_and_offer_log_with_no_duplicates() throws Exception {
        // Given
        OfferLog offerLog1 = new OfferLog(1, LocalDateUtil.now(), "containerName", "fileName1", OfferLogAction.WRITE);
        OfferLog offerLog2 = new OfferLog(2, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE);

        OfferLog offerLog3 = new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE);
        OfferLog offerLog4 = new OfferLog(4, LocalDateUtil.now(), "containerName", "fileName4", OfferLogAction.WRITE);
        OfferLog offerLog5 = new OfferLog(5, LocalDateUtil.now(), "containerName", "fileName5", OfferLogAction.WRITE);

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 1L, 6)).thenReturn(
            Arrays.asList(offerLog1, offerLog2)
        );

        when(offerDatabaseService.getAscendingOfferLogsBy("containerName", 3L, 4)).thenReturn(
            Arrays.asList(offerLog3, offerLog4, offerLog5)
        );

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 3L, 6)).thenReturn(
            Collections.emptyList()
        );

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 1L, 6, ASC);

        // Then
        assertThat(logs).containsExactly(offerLog1, offerLog2, offerLog3, offerLog4, offerLog5);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 1L, 6);
        verify(offerDatabaseService).getAscendingOfferLogsBy("containerName", 3L, 4);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 3L, 4);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogAndCompactedOfferLogService);
    }

    @Test
    public void should_search_ascending_on_offer_log_compaction_and_offer_log_with_concurrent_compaction_without_duplicates()
        throws Exception {
        // Given
        OfferLog offerLog1 = new OfferLog(1, LocalDateUtil.now(), "containerName", "fileName1", OfferLogAction.WRITE);
        OfferLog offerLog2 = new OfferLog(2, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE);

        OfferLog offerLog3 = new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE);
        OfferLog offerLog4 = new OfferLog(4, LocalDateUtil.now(), "containerName", "fileName4", OfferLogAction.WRITE);
        OfferLog offerLog5 = new OfferLog(5, LocalDateUtil.now(), "containerName", "fileName5", OfferLogAction.WRITE);

        OfferLog offerLog6 = new OfferLog(6, LocalDateUtil.now(), "containerName", "fileName6", OfferLogAction.WRITE);
        OfferLog offerLog7 = new OfferLog(7, LocalDateUtil.now(), "containerName", "fileName7", OfferLogAction.WRITE);

        // On first query compacted offer log only retrieves OfferLog sequences 1 to 2
        // Concurrently, OfferLogs with sequences 3 to 5 will just be compacted, but not sequences 6 to 7

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 1L, 6)).thenReturn(
            Arrays.asList(offerLog1, offerLog2)
        );

        when(offerDatabaseService.getAscendingOfferLogsBy("containerName", 3L, 4)).thenReturn(
            Arrays.asList(offerLog6, offerLog7)
        );

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 3L, 4)).thenReturn(
            Arrays.asList(offerLog3, offerLog4, offerLog5)
        );

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 1L, 6, ASC);

        // Then
        assertThat(logs).containsExactly(offerLog1, offerLog2, offerLog3, offerLog4, offerLog5, offerLog6);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 1L, 6);
        verify(offerDatabaseService).getAscendingOfferLogsBy("containerName", 3L, 4);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 3L, 4);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogAndCompactedOfferLogService);
    }

    @Test
    public void should_search_ascending_on_offer_log_compaction_and_offer_log_with_concurrent_compaction_duplicates()
        throws Exception {
        // Given
        OfferLog offerLog1 = new OfferLog(1, LocalDateUtil.now(), "containerName", "fileName1", OfferLogAction.WRITE);
        OfferLog offerLog2 = new OfferLog(2, LocalDateUtil.now(), "containerName", "fileName2", OfferLogAction.WRITE);

        OfferLog offerLog3 = new OfferLog(3, LocalDateUtil.now(), "containerName", "fileName3", OfferLogAction.WRITE);
        OfferLog offerLog4 = new OfferLog(4, LocalDateUtil.now(), "containerName", "fileName4", OfferLogAction.WRITE);
        OfferLog offerLog5 = new OfferLog(5, LocalDateUtil.now(), "containerName", "fileName5", OfferLogAction.WRITE);

        OfferLog offerLog6 = new OfferLog(6, LocalDateUtil.now(), "containerName", "fileName6", OfferLogAction.WRITE);

        // On first query compacted offer log only retrieves OfferLog sequences 1 to 2
        // Concurrently, OfferLogs with sequences 3 to 5 are being partially compacted (add to CompactedOfferLog, but not yet removed from OfferLog)

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 1L, 6)).thenReturn(
            Arrays.asList(offerLog1, offerLog2)
        );

        when(offerDatabaseService.getAscendingOfferLogsBy("containerName", 3L, 4)).thenReturn(
            Arrays.asList(offerLog3, offerLog4, offerLog5, offerLog6)
        );

        when(offerLogCompactionDatabaseService.getAscendingOfferLogCompactionBy("containerName", 3L, 4)).thenReturn(
            Arrays.asList(offerLog3, offerLog4, offerLog5)
        );

        // When
        List<OfferLog> logs = offerService.getOfferLogs("containerName", 1L, 6, ASC);

        // Then
        assertThat(logs).containsExactly(offerLog1, offerLog2, offerLog3, offerLog4, offerLog5, offerLog6);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 1L, 6);
        verify(offerDatabaseService).getAscendingOfferLogsBy("containerName", 3L, 4);
        verify(offerLogCompactionDatabaseService).getAscendingOfferLogCompactionBy("containerName", 3L, 4);
        verifyNoMoreInteractions(offerDatabaseService);
        verifyNoMoreInteractions(offerLogAndCompactedOfferLogService);
    }

    @Test
    public void getBulkMetadataWithExistingObjects() throws Exception {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        doAnswer(args -> {
            String objectName = args.getArgument(1);
            return new StorageMetadataResult(
                objectName,
                null,
                "digest-" + objectName,
                objectName.hashCode(),
                null,
                null
            );
        })
            .when(contentAddressableStorage)
            .getObjectMetadata(eq(CONTAINER_PATH), anyString(), eq(false));

        // When
        StorageBulkMetadataResult result = offerService.getBulkMetadata(
            CONTAINER_PATH,
            Arrays.asList("guid1", "guid2", "guid3"),
            false
        );

        // Then
        assertThat(result.getObjectMetadata()).hasSize(3);
        assertThat(result.getObjectMetadata())
            .extracting(
                StorageBulkMetadataResultEntry::getObjectName,
                StorageBulkMetadataResultEntry::getDigest,
                StorageBulkMetadataResultEntry::getSize
            )
            .containsExactlyInAnyOrder(
                new Tuple("guid1", "digest-guid1", (long) "guid1".hashCode()),
                new Tuple("guid2", "digest-guid2", (long) "guid2".hashCode()),
                new Tuple("guid3", "digest-guid3", (long) "guid3".hashCode())
            );
    }

    @Test
    public void getBulkMetadataWithMissingObject() throws Exception {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        doAnswer(args -> {
            String objectName = args.getArgument(1);

            if (objectName.equals("guid2")) {
                throw new ContentAddressableStorageNotFoundException("");
            }

            return new StorageMetadataResult(
                objectName,
                null,
                "digest-" + objectName,
                objectName.hashCode(),
                null,
                null
            );
        })
            .when(contentAddressableStorage)
            .getObjectMetadata(eq(CONTAINER_PATH), anyString(), eq(false));

        // When
        StorageBulkMetadataResult result = offerService.getBulkMetadata(
            CONTAINER_PATH,
            Arrays.asList("guid1", "guid2", "guid3"),
            false
        );

        // Then
        assertThat(result.getObjectMetadata()).hasSize(3);
        assertThat(result.getObjectMetadata())
            .extracting(
                StorageBulkMetadataResultEntry::getObjectName,
                StorageBulkMetadataResultEntry::getDigest,
                StorageBulkMetadataResultEntry::getSize
            )
            .containsExactlyInAnyOrder(
                new Tuple("guid1", "digest-guid1", (long) "guid1".hashCode()),
                new Tuple("guid2", null, null),
                new Tuple("guid3", "digest-guid3", (long) "guid3".hashCode())
            );
    }

    @Test
    public void getBulkMetadataWithAtLeastOneStorageExceptionThenThrowException() throws Exception {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            BATCH_METADATA_COMPUTATION_TIMEOUT
        );

        doAnswer(args -> {
            String objectName = args.getArgument(1);

            if (objectName.equals("guid2")) {
                throw new ContentAddressableStorageException("");
            }

            return new StorageMetadataResult(
                objectName,
                null,
                "digest-" + objectName,
                objectName.hashCode(),
                null,
                null
            );
        })
            .when(contentAddressableStorage)
            .getObjectMetadata(eq(CONTAINER_PATH), anyString(), eq(false));

        // When / Then
        assertThatThrownBy(
            () -> offerService.getBulkMetadata(CONTAINER_PATH, Arrays.asList("guid1", "guid2", "guid3"), false)
        ).isInstanceOf(ContentAddressableStorageException.class);
    }

    @Test
    public void getBulkMetadataWithTimeoutThenThrowException() throws Exception {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doAnswer(args -> {
            String objectName = args.getArgument(1);

            if (objectName.equals("guid2")) {
                TimeUnit.SECONDS.sleep(60);
            }

            return new StorageMetadataResult(
                objectName,
                null,
                "digest-" + objectName,
                objectName.hashCode(),
                null,
                null
            );
        })
            .when(contentAddressableStorage)
            .getObjectMetadata(eq(CONTAINER_PATH), anyString(), eq(false));

        // When / Then
        assertThatThrownBy(
            () -> offerService.getBulkMetadata(CONTAINER_PATH, Arrays.asList("guid1", "guid2", "guid3"), false)
        ).isInstanceOf(ContentAddressableStorageException.class);
    }

    @Test
    public void givenTapeOfferWhenCreateAccessRequestThenOK() throws ContentAddressableStorageException {
        // Given
        configuration.setProvider(StorageProvider.TAPE_LIBRARY.getValue());
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn("accessRequestId")
            .when(contentAddressableStorage)
            .createAccessRequest("container", List.of("obj1", "obj2"));

        // When
        String accessRequest = offerService.createAccessRequest("container", List.of("obj1", "obj2"));

        // Then
        assertThat(accessRequest).isEqualTo("accessRequestId");
    }

    @Test
    public void givenNonTapeOfferWhenCreateAccessRequestThenKO() throws ContentAddressableStorageException {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn("accessRequestId")
            .when(contentAddressableStorage)
            .createAccessRequest("container", List.of("obj1", "obj2"));

        // When / Then
        assertThatThrownBy(() -> offerService.createAccessRequest("container", List.of("obj1", "obj2"))).isInstanceOf(
            ContentAddressableStorageException.class
        );
    }

    @Test
    public void givenTapeOfferWhenCheckAccessRequestStatusesThenOK() throws ContentAddressableStorageException {
        // Given
        configuration.setProvider(StorageProvider.TAPE_LIBRARY.getValue());
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn(Map.of("accessRequest1", AccessRequestStatus.EXPIRED, "accessRequest2", AccessRequestStatus.READY))
            .when(contentAddressableStorage)
            .checkAccessRequestStatuses(List.of("accessRequest1", "accessRequest2"), true);

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = offerService.checkAccessRequestStatuses(
            List.of("accessRequest1", "accessRequest2"),
            true
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(
            Map.of("accessRequest1", AccessRequestStatus.EXPIRED, "accessRequest2", AccessRequestStatus.READY)
        );
    }

    @Test
    public void givenNonTapeOfferWhenCheckAccessRequestStatusesThenKO() throws ContentAddressableStorageException {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn(Map.of("accessRequest1", AccessRequestStatus.EXPIRED, "accessRequest2", AccessRequestStatus.READY))
            .when(contentAddressableStorage)
            .checkAccessRequestStatuses(List.of("accessRequest1", "accessRequest2"), true);

        // When / Then
        assertThatThrownBy(
            () -> offerService.checkAccessRequestStatuses(List.of("accessRequest1", "accessRequest2"), true)
        ).isInstanceOf(ContentAddressableStorageException.class);
    }

    @Test
    public void givenTapeOfferWhenRemoveAccessRequestThenOK() throws ContentAddressableStorageException {
        // Given
        configuration.setProvider(StorageProvider.TAPE_LIBRARY.getValue());
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        // When
        offerService.removeAccessRequest("accessRequest1", true);

        // Then
        verify(contentAddressableStorage).removeAccessRequest("accessRequest1", true);
    }

    @Test
    public void givenNonTapeOfferWhenRemoveAccessRequestThenOK() {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        // When / Then
        assertThatThrownBy(() -> offerService.removeAccessRequest("accessRequest1", true)).isInstanceOf(
            ContentAddressableStorageException.class
        );
    }

    @Test
    public void givenTapeOfferWhenCheckObjectAvailabilityThenOK() throws ContentAddressableStorageException {
        // Given
        configuration.setProvider(StorageProvider.TAPE_LIBRARY.getValue());
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn(true).when(contentAddressableStorage).checkObjectAvailability("container", List.of("obj1", "obj2"));

        // When
        boolean areObjectsAvailable = offerService.checkObjectAvailability("container", List.of("obj1", "obj2"));

        // Then
        assertThat(areObjectsAvailable).isTrue();
    }

    @Test
    public void givenNonTapeOfferWhenCheckObjectAvailabilityThenKO() throws ContentAddressableStorageException {
        // Given
        ContentAddressableStorage contentAddressableStorage = mock(ContentAddressableStorage.class);
        offerService = new DefaultOfferServiceImpl(
            contentAddressableStorage,
            offerLogCompactionDatabaseService,
            offerDatabaseService,
            offerSequenceDatabaseService,
            configuration,
            null,
            offerLogAndCompactedOfferLogService,
            MAX_BATCH_THREAD_POOL_SIZE,
            1
        );

        doReturn(true).when(contentAddressableStorage).checkObjectAvailability("container", List.of("obj1", "obj2"));

        // When / Then
        assertThatThrownBy(
            () -> offerService.checkObjectAvailability("container", List.of("obj1", "obj2"))
        ).isInstanceOf(ContentAddressableStorageException.class);
    }

    private <T> CloseableIterable<T> toCloseableIterable(List<T> offerLogs) {
        return new CloseableIterable<>() {
            @Override
            public void close() {
                // NOP
            }

            @Override
            public Iterator<T> iterator() {
                return offerLogs.iterator();
            }
        };
    }
}
