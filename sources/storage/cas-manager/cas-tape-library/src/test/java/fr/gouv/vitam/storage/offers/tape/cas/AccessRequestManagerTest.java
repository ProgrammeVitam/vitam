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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.storage.engine.common.model.EntryType;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeAccessRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageBadRequestException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager.generateAccessRequestId;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWithCustomExecutor
public class AccessRequestManagerTest {

    private static final int TENANT_ID = 0;
    private static final int ADMIN_TENANT = 1;
    private static final int TENANT_ID_2 = 2;

    private static final String CONTAINER_1 = "0_object";
    private static final String CONTAINER_2 = "1_unit";

    private static final String FILE_BUCKET_1 = "test-objects";
    private static final String FILE_BUCKET_2 = "admin-metadata";

    private static final String BUCKET_1 = "test";
    private static final String BUCKET_2 = "admin";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    ObjectReferentialRepository objectReferentialRepository;

    @Mock
    ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    AccessRequestReferentialRepository accessRequestReferentialRepository;

    @Mock
    ArchiveCacheStorage archiveCacheStorage;

    @Mock
    BucketTopologyHelper bucketTopologyHelper;

    @Mock
    QueueRepository readWriteQueue;

    private AccessRequestManager instance;

    @Before
    public void before() {
        instance = new AccessRequestManager(
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestReferentialRepository,
            archiveCacheStorage,
            bucketTopologyHelper,
            readWriteQueue,
            10_000,
            30,
            TimeUnit.MINUTES,
            60,
            TimeUnit.MINUTES,
            3,
            TimeUnit.SECONDS
        );

        doReturn(FILE_BUCKET_1).when(bucketTopologyHelper).getFileBucketFromContainerName(CONTAINER_1);
        doReturn(FILE_BUCKET_2).when(bucketTopologyHelper).getFileBucketFromContainerName(CONTAINER_2);
        doReturn(BUCKET_1).when(bucketTopologyHelper).getBucketFromFileBucket(FILE_BUCKET_1);
        doReturn(BUCKET_2).when(bucketTopologyHelper).getBucketFromFileBucket(FILE_BUCKET_2);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamConfiguration.setAdminTenant(ADMIN_TENANT);
    }

    @After
    public void after() {
        instance.shutdown();
        verifyNoMoreInteractions(
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestReferentialRepository,
            archiveCacheStorage,
            readWriteQueue
        );
    }

    @Test
    public void givenAvailableAndNonAvailableObjectsWhenCreatingAccessRequestThenAccessRequestOK() throws Exception {
        // Given :
        doReturn(
            List.of(
                // obj1 : still in input_file
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj1"),
                    100L,
                    "SHA-512",
                    "digest1",
                    "obj1-guid1",
                    new TapeLibraryInputFileObjectStorageLocation(),
                    nextDate(),
                    nextDate()
                ),
                // obj2 : in tars
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj2"),
                    400L,
                    "SHA-512",
                    "digest2",
                    "obj2-guid2",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId1", "obj2-0", 1500L, 100L, "digest2-1"),
                            new TarEntryDescription("tarId2", "obj2-1", 0L, 100L, "digest2-2"),
                            new TarEntryDescription("tarId3", "obj2-2", 0L, 100L, "digest2-3"),
                            new TarEntryDescription("tarId4", "obj2-3", 0L, 100L, "digest2-4")
                        )
                    ),
                    nextDate(),
                    nextDate()
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));

        doReturn(
            List.of(
                // Tar1 : Ready on disk
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    2000L,
                    "digest-tarId1",
                    nextDate()
                ),
                // Tar2 : Building on disk
                new TapeArchiveReferentialEntity(
                    "tarId2",
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    null,
                    null,
                    nextDate()
                ),
                // Tar3 : On tape + in cache
                new TapeArchiveReferentialEntity(
                    "tarId3",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape1", 1234),
                    EntryType.DATA,
                    5000L,
                    "digest-tarId3",
                    nextDate()
                ),
                // Tar4 : Only on tape
                new TapeArchiveReferentialEntity(
                    "tarId4",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape2", 4321),
                    EntryType.DATA,
                    8000L,
                    "digest-tarId4",
                    nextDate()
                )
            )
        )
            .when(archiveReferentialRepository)
            .bulkFind(Set.of("tarId1", "tarId2", "tarId3", "tarId4"));

        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId4");

        logicalClock.freezeTime();

        // When
        String accessRequestId = instance.createAccessRequest(CONTAINER_1, List.of("obj1", "obj2"));

        // Then
        assertThat(accessRequestId).isNotNull();

        ArgumentCaptor<TapeAccessRequestReferentialEntity> accessRequestReferentialEntityArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository).insert(accessRequestReferentialEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(accessRequestReferentialRepository);

        TapeAccessRequestReferentialEntity accessRequest = accessRequestReferentialEntityArgumentCaptor.getValue();
        assertThat(accessRequest.getRequestId()).isEqualTo(accessRequestId);
        assertThat(accessRequest.getObjectNames()).containsExactlyInAnyOrder("obj1", "obj2");
        assertThat(accessRequest.getUnavailableArchiveIds()).containsExactly("tarId4");
        assertThat(accessRequest.getCreationDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())
        );
        assertThat(accessRequest.getReadyDate()).isNull();
        assertThat(accessRequest.getExpirationDate()).isNull();
        assertThat(accessRequest.getPurgeDate()).isNull();
        assertThat(accessRequest.getTenant()).isEqualTo(0);
        assertThat(accessRequest.getVersion()).isEqualTo(0);

        verify(objectReferentialRepository).bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));
        verifyNoMoreInteractions(objectReferentialRepository);

        verify(archiveReferentialRepository).bulkFind(Set.of("tarId1", "tarId2", "tarId3", "tarId4"));
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId4");
        verifyNoMoreInteractions(archiveCacheStorage);

        ArgumentCaptor<QueueMessageEntity> queueMessageEntityArgumentCaptor = ArgumentCaptor.forClass(
            QueueMessageEntity.class
        );
        verify(readWriteQueue).addIfAbsent(any(), queueMessageEntityArgumentCaptor.capture());

        QueueMessageEntity readOrderMessage = queueMessageEntityArgumentCaptor.getValue();
        assertThat(readOrderMessage).isNotNull();
        assertThat(readOrderMessage.getMessageType()).isEqualTo(QueueMessageType.ReadOrder);
        assertThat(readOrderMessage).isInstanceOf(ReadOrder.class);
        assertThat(((ReadOrder) readOrderMessage).getFileName()).isEqualTo("tarId4");
        assertThat(((ReadOrder) readOrderMessage).getSize()).isEqualTo(8000L);
        assertThat(((ReadOrder) readOrderMessage).getBucket()).isEqualTo(BUCKET_1);
        assertThat(((ReadOrder) readOrderMessage).getFileBucketId()).isEqualTo(FILE_BUCKET_1);
        assertThat(((ReadOrder) readOrderMessage).getFilePosition()).isEqualTo(4321);
        assertThat(((ReadOrder) readOrderMessage).getTapeCode()).isEqualTo("tape2");

        verifyNoMoreInteractions(readWriteQueue);
    }

    @Test
    public void givenAvailableObjectsWhenCreatingAccessRequestThenAccessRequestOK() throws Exception {
        // Given :

        doReturn(
            List.of(
                // obj1 : still in input_file
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj1"),
                    100L,
                    "SHA-512",
                    "digest1",
                    "obj1-guid1",
                    new TapeLibraryInputFileObjectStorageLocation(),
                    nextDate(),
                    nextDate()
                ),
                // obj2 : in tars
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj2"),
                    400L,
                    "SHA-512",
                    "digest2",
                    "obj2-guid2",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId1", "obj2-0", 1500L, 100L, "digest2-1"),
                            new TarEntryDescription("tarId2", "obj2-1", 0L, 100L, "digest2-2"),
                            new TarEntryDescription("tarId3", "obj2-2", 0L, 100L, "digest2-3")
                        )
                    ),
                    nextDate(),
                    nextDate()
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));

        doReturn(
            List.of(
                // Tar1 : Ready on disk
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    2000L,
                    "digest-tarId1",
                    nextDate()
                ),
                // Tar2 : Building on disk
                new TapeArchiveReferentialEntity(
                    "tarId2",
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    null,
                    null,
                    nextDate()
                ),
                // Tar3 : On tape + in cache
                new TapeArchiveReferentialEntity(
                    "tarId3",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape1", 1234),
                    EntryType.DATA,
                    5000L,
                    "digest-tarId3",
                    nextDate()
                )
            )
        )
            .when(archiveReferentialRepository)
            .bulkFind(Set.of("tarId1", "tarId2", "tarId3"));

        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");

        logicalClock.freezeTime();

        // When
        String accessRequestId = instance.createAccessRequest(CONTAINER_1, List.of("obj1", "obj2"));

        // Then
        assertThat(accessRequestId).isNotNull();

        ArgumentCaptor<TapeAccessRequestReferentialEntity> accessRequestReferentialEntityArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository).insert(accessRequestReferentialEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(accessRequestReferentialRepository);

        TapeAccessRequestReferentialEntity accessRequest = accessRequestReferentialEntityArgumentCaptor.getValue();
        assertThat(accessRequest.getRequestId()).isEqualTo(accessRequestId);
        assertThat(accessRequest.getObjectNames()).containsExactlyInAnyOrder("obj1", "obj2");
        assertThat(accessRequest.getUnavailableArchiveIds()).isEmpty();
        assertThat(accessRequest.getCreationDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())
        );
        assertThat(accessRequest.getReadyDate()).isEqualTo(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        assertThat(accessRequest.getExpirationDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().plusMinutes(30))
        );
        assertThat(accessRequest.getPurgeDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().plusMinutes(60))
        );
        assertThat(accessRequest.getVersion()).isEqualTo(0);

        verify(objectReferentialRepository).bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));
        verifyNoMoreInteractions(objectReferentialRepository);

        verify(archiveReferentialRepository).bulkFind(Set.of("tarId1", "tarId2", "tarId3"));
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        verifyNoMoreInteractions(archiveCacheStorage);

        verifyNoMoreInteractions(readWriteQueue);
    }

    @Test
    public void givenUnavailableObjectsInSameTarsWhenCreatingAccessRequestThenAccessRequestOKWithoutDuplicateOrderRequests()
        throws Exception {
        // Given :

        doReturn(
            List.of(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj1"),
                    400L,
                    "SHA-512",
                    "digest1",
                    "obj1-guid1",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId1", "obj1-0", 1500L, 100L, "digest1-1"),
                            new TarEntryDescription("tarId1", "obj1-1", 0L, 100L, "digest1-2"),
                            new TarEntryDescription("tarId2", "obj1-2", 0L, 100L, "digest1-3")
                        )
                    ),
                    nextDate(),
                    nextDate()
                ),
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj2"),
                    400L,
                    "SHA-512",
                    "digest2",
                    "obj2-guid1",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId2", "obj2-0", 1500L, 100L, "digest2-1"),
                            new TarEntryDescription("tarId3", "obj2-1", 0L, 100L, "digest2-2")
                        )
                    ),
                    nextDate(),
                    nextDate()
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));

        doReturn(
            List.of(
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape2", 4321),
                    EntryType.DATA,
                    8000L,
                    "digest-tarId1",
                    nextDate()
                ),
                new TapeArchiveReferentialEntity(
                    "tarId2",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape2", 1234),
                    EntryType.DATA,
                    10000L,
                    "digest-tarId2",
                    nextDate()
                ),
                new TapeArchiveReferentialEntity(
                    "tarId3",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape3", 5643),
                    EntryType.DATA,
                    12000L,
                    "digest-tarId3",
                    nextDate()
                )
            )
        )
            .when(archiveReferentialRepository)
            .bulkFind(Set.of("tarId1", "tarId2", "tarId3"));

        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId1");
        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId2");
        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");

        logicalClock.freezeTime();

        // When
        String accessRequestId = instance.createAccessRequest(CONTAINER_1, List.of("obj1", "obj2"));

        // Then
        assertThat(accessRequestId).isNotNull();

        ArgumentCaptor<TapeAccessRequestReferentialEntity> accessRequestReferentialEntityArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository).insert(accessRequestReferentialEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(accessRequestReferentialRepository);

        TapeAccessRequestReferentialEntity accessRequest = accessRequestReferentialEntityArgumentCaptor.getValue();
        assertThat(accessRequest.getRequestId()).isEqualTo(accessRequestId);
        assertThat(accessRequest.getObjectNames()).containsExactlyInAnyOrder("obj1", "obj2");
        assertThat(accessRequest.getUnavailableArchiveIds()).containsExactlyInAnyOrder("tarId1", "tarId2", "tarId3");
        assertThat(accessRequest.getCreationDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())
        );
        assertThat(accessRequest.getReadyDate()).isNull();
        assertThat(accessRequest.getExpirationDate()).isNull();
        assertThat(accessRequest.getPurgeDate()).isNull();
        assertThat(accessRequest.getVersion()).isEqualTo(0);

        verify(objectReferentialRepository).bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));
        verifyNoMoreInteractions(objectReferentialRepository);

        verify(archiveReferentialRepository).bulkFind(Set.of("tarId1", "tarId2", "tarId3"));
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(archiveCacheStorage, times(3)).containsArchive(eq(FILE_BUCKET_1), anyString());
        verifyNoMoreInteractions(archiveCacheStorage);

        ArgumentCaptor<QueueMessageEntity> queueMessageEntityArgumentCaptor = ArgumentCaptor.forClass(
            QueueMessageEntity.class
        );
        verify(readWriteQueue, times(3)).addIfAbsent(any(), queueMessageEntityArgumentCaptor.capture());

        assertThat(queueMessageEntityArgumentCaptor.getAllValues())
            .extracting(msg -> ((ReadOrder) msg).getFileName())
            .containsExactlyInAnyOrder("tarId1", "tarId2", "tarId3");

        verifyNoMoreInteractions(readWriteQueue);
    }

    @Test
    public void givenMissingParametersWhenCreatingAccessRequestThenKO() {
        Assertions.assertThatThrownBy(() -> instance.createAccessRequest(CONTAINER_1, null)).isInstanceOf(
            IllegalArgumentException.class
        );
        Assertions.assertThatThrownBy(
            () -> instance.createAccessRequest(CONTAINER_1, Arrays.asList("obj1", null))
        ).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> instance.createAccessRequest(null, List.of("obj1", "obj2"))).isInstanceOf(
            IllegalArgumentException.class
        );
        Assertions.assertThatThrownBy(() -> instance.createAccessRequest(CONTAINER_1, emptyList())).isInstanceOf(
            ContentAddressableStorageBadRequestException.class
        );
    }

    @Test
    public void givenDuplicateObjectsWhenCreatingAccessRequestThenKO() {
        // Given
        List<String> objectIds = List.of("obj1", "obj2", "obj1");

        // When / Then
        Assertions.assertThatThrownBy(() -> instance.createAccessRequest(CONTAINER_1, objectIds)).isInstanceOf(
            ContentAddressableStorageBadRequestException.class
        );
    }

    @Test
    public void givenTooManyObjectsWhenCreatingAccessRequestThenKO() {
        // Given
        List<String> objectIds = IntStream.rangeClosed(1, 10_001).mapToObj(i -> "obj" + i).collect(Collectors.toList());

        // When / Then
        Assertions.assertThatThrownBy(() -> instance.createAccessRequest(CONTAINER_1, objectIds)).isInstanceOf(
            ContentAddressableStorageBadRequestException.class
        );
    }

    @Test
    public void givenExistingAndNonExistingAccessRequestsWhenCheckAccessRequestStatusesThenOK() throws Exception {
        // Given
        logicalClock.freezeTime();

        String accessRequest1 = generateAccessRequestId();
        String accessRequest2 = generateAccessRequestId();
        String accessRequest3 = generateAccessRequestId();
        String unknownAccessRequestId = generateAccessRequestId();
        doReturn(
            List.of(
                // Non-ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID,
                    0
                ),
                // Ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest2,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID,
                    1
                ),
                // Expired access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest3,
                    CONTAINER_2,
                    List.of("obj3"),
                    getNowMinusMinutes(50),
                    getNowMinusMinutes(40),
                    getNowMinusMinutes(10),
                    getNowPlusMinutes(20),
                    List.of(),
                    TENANT_ID,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestIds(Set.of(accessRequest1, accessRequest2, accessRequest3, unknownAccessRequestId));

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = instance.checkAccessRequestStatuses(
            List.of(accessRequest1, accessRequest2, accessRequest3, unknownAccessRequestId),
            false
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(
            Map.of(
                accessRequest1,
                AccessRequestStatus.NOT_READY,
                accessRequest2,
                AccessRequestStatus.READY,
                accessRequest3,
                AccessRequestStatus.EXPIRED,
                unknownAccessRequestId,
                AccessRequestStatus.NOT_FOUND
            )
        );

        verify(accessRequestReferentialRepository).findByRequestIds(anySet());
    }

    @Test
    public void givenExistingAccessRequestFromOtherTenantWhenCheckAccessRequestStatusThenNotFound() throws Exception {
        // Given
        String accessRequest1 = generateAccessRequestId();
        doReturn(
            List.of(
                // Non-ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID_2,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestIds(Set.of(accessRequest1));

        // When
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, AccessRequestStatus> accessRequestStatuses = instance.checkAccessRequestStatuses(
            List.of(accessRequest1),
            false
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(Map.of(accessRequest1, AccessRequestStatus.NOT_FOUND));

        verify(accessRequestReferentialRepository).findByRequestIds(anySet());
    }

    @Test
    public void givenExistingAccessRequestFromOtherTenantWhenCheckAccessRequestStatusWithAdminTenantThenNotFound()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        String accessRequest1 = generateAccessRequestId();
        doReturn(
            List.of(
                // Non-ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID_2,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestIds(Set.of(accessRequest1));

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = instance.checkAccessRequestStatuses(
            List.of(accessRequest1),
            false
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(Map.of(accessRequest1, AccessRequestStatus.NOT_FOUND));

        verify(accessRequestReferentialRepository).findByRequestIds(anySet());
    }

    @Test
    public void givenExistingAccessRequestFromOtherTenantWhenCheckAccessRequestStatusWithNonAdminTenantAndCrossTenantModeEnabledThenNotFound()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String accessRequest1 = generateAccessRequestId();
        doReturn(
            List.of(
                // Non-ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID_2,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestIds(Set.of(accessRequest1));

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = instance.checkAccessRequestStatuses(
            List.of(accessRequest1),
            true
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(Map.of(accessRequest1, AccessRequestStatus.NOT_FOUND));

        verify(accessRequestReferentialRepository).findByRequestIds(anySet());
    }

    @Test
    public void givenExistingAccessRequestFromOtherTenantWhenCheckAccessRequestStatusWithAdminTenantAndCrossTenantModeEnabledThenFound()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        String accessRequest1 = generateAccessRequestId();
        doReturn(
            List.of(
                // Non-ready access request
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID_2,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestIds(Set.of(accessRequest1));

        // When
        Map<String, AccessRequestStatus> accessRequestStatuses = instance.checkAccessRequestStatuses(
            List.of(accessRequest1),
            true
        );

        // Then
        assertThat(accessRequestStatuses).isEqualTo(Map.of(accessRequest1, AccessRequestStatus.NOT_READY));

        verify(accessRequestReferentialRepository).findByRequestIds(anySet());
    }

    @Test
    public void givenNonExistingAccessRequestWhenRemoveAccessRequestThenOKAndNoReadOrderCanceled() throws Exception {
        // Given
        String accessRequest = generateAccessRequestId();
        doReturn(Optional.empty()).when(accessRequestReferentialRepository).findByRequestId(anyString());

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenReadyAccessRequestWhenRemoveAccessRequestThenOKAndNoReadOrderCanceled() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenAccessRequestFromOtherTenantWhenRemoveAccessRequestWithNonAdminTenantThenNotDeleted()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID_2,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository, never()).deleteAccessRequestById(any());
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenAccessRequestFromOtherTenantWhenRemoveAccessRequestWithAdminTenantThenNotDeleted()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID_2,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository, never()).deleteAccessRequestById(any());
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenAccessRequestFromOtherTenantWhenRemoveAccessRequestWithNonAdminTenantAndCrossTenantModeEnabledThenNotDeleted()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID_2,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);

        // When
        instance.removeAccessRequest(accessRequest, true);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository, never()).deleteAccessRequestById(any());
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenAccessRequestFromOtherTenantWhenRemoveAccessRequestWithAdminTenantAndCrossTenantModeEnabledThenNotDeleted()
        throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_2,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    2,
                    1
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);
        doReturn(true).when(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);

        // When
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        instance.removeAccessRequest(accessRequest, true);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenNonReadyAccessRequestWithStillInUseArchiveIdWhenRemoveAccessRequestThenOKAndReadOrderNotCanceled()
        throws Exception {
        // Given
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);
        doReturn(true).when(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);

        doReturn(emptySet())
            .when(accessRequestReferentialRepository)
            .excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);
        verify(accessRequestReferentialRepository).excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenNonReadyAccessRequestWithNoMoreRequiredArchiveIdWhenRemoveAccessRequestThenOKAndReadOrderCanceled()
        throws Exception {
        // Given
        String accessRequest = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest);
        doReturn(true).when(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);

        doReturn(Set.of("tarId1"))
            .when(accessRequestReferentialRepository)
            .excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));

        doReturn(emptyList()).when(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId1");

        // When
        instance.removeAccessRequest(accessRequest, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest);
        verify(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest);
        verify(accessRequestReferentialRepository).excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));
        verify(readWriteQueue).tryCancelIfNotStarted(anyList());
        verify(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId1");
        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenNonReadyAccessRequestWithNoMoreRequiredArchiveIdWhenRemoveAccessRequestAndConcurrentReadOrderCreatedThenOKAndReadOrderCanceledAndReinserted()
        throws Exception {
        // Given

        String accessRequest1 = generateAccessRequestId();
        doReturn(
            Optional.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest1,
                    CONTAINER_1,
                    List.of("obj1"),
                    getNowMinusMinutes(30),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequest1);
        doReturn(true).when(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest1);

        doReturn(Set.of("tarId1"))
            .when(accessRequestReferentialRepository)
            .excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));

        String accessRequest2 = generateAccessRequestId();
        doReturn(
            List.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest2,
                    CONTAINER_1,
                    List.of("obj1"),
                    now(),
                    null,
                    null,
                    null,
                    List.of("tarId1"),
                    TENANT_ID,
                    0
                )
            )
        )
            .when(accessRequestReferentialRepository)
            .findByUnavailableArchiveId("tarId1");

        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape2", 4321),
                    10L,
                    "digest1",
                    "date1"
                )
            )
        )
            .when(archiveReferentialRepository)
            .find("tarId1");

        // When
        instance.removeAccessRequest(accessRequest1, false);

        // Then
        verify(accessRequestReferentialRepository).findByRequestId(accessRequest1);
        verify(accessRequestReferentialRepository).deleteAccessRequestById(accessRequest1);
        verify(accessRequestReferentialRepository).excludeArchiveIdsStillRequiredByAccessRequests(Set.of("tarId1"));
        verify(readWriteQueue).tryCancelIfNotStarted(anyList());
        verify(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId1");
        verify(archiveReferentialRepository).find("tarId1");

        ArgumentCaptor<QueueMessageEntity> queueMessageEntityArgumentCaptor = ArgumentCaptor.forClass(
            QueueMessageEntity.class
        );
        verify(readWriteQueue).addIfAbsent(any(), queueMessageEntityArgumentCaptor.capture());

        QueueMessageEntity readOrderMessage = queueMessageEntityArgumentCaptor.getValue();
        assertThat(readOrderMessage).isNotNull();
        assertThat(readOrderMessage.getMessageType()).isEqualTo(QueueMessageType.ReadOrder);
        assertThat(readOrderMessage).isInstanceOf(ReadOrder.class);
        assertThat(((ReadOrder) readOrderMessage).getFileName()).isEqualTo("tarId1");
        assertThat(((ReadOrder) readOrderMessage).getSize()).isEqualTo(10L);
        assertThat(((ReadOrder) readOrderMessage).getBucket()).isEqualTo(BUCKET_1);
        assertThat(((ReadOrder) readOrderMessage).getFileBucketId()).isEqualTo(FILE_BUCKET_1);
        assertThat(((ReadOrder) readOrderMessage).getFilePosition()).isEqualTo(4321);
        assertThat(((ReadOrder) readOrderMessage).getTapeCode()).isEqualTo("tape2");

        verifyNoMoreInteractions(accessRequestReferentialRepository, readWriteQueue);
    }

    @Test
    public void givenOldAccessRequestsWhenCleanupOccursThenOldAccessRequestWillBeRemoved() throws Exception {
        // Given
        String accessRequest = generateAccessRequestId();
        CountDownLatch timerExecuted = new CountDownLatch(1);
        doAnswer(args -> {
            timerExecuted.countDown();
            return List.of(
                new TapeAccessRequestReferentialEntity(
                    accessRequest,
                    CONTAINER_1,
                    List.of("obj2"),
                    getNowMinusMinutes(30),
                    getNowMinusMinutes(20),
                    getNowPlusMinutes(10),
                    getNowPlusMinutes(40),
                    List.of(),
                    TENANT_ID,
                    1
                )
            );
        })
            .when(accessRequestReferentialRepository)
            .cleanupAndGetExpiredAccessRequests();

        // When
        instance.startExpirationHandler();

        Uninterruptibles.awaitUninterruptibly(timerExecuted, 10, TimeUnit.SECONDS);
        instance.shutdown();

        // Then
        verify(accessRequestReferentialRepository, atLeastOnce()).cleanupAndGetExpiredAccessRequests();
        verify(accessRequestReferentialRepository, atLeastOnce()).findNonReadyAccessRequests();
        verifyNoMoreInteractions(accessRequestReferentialRepository);
    }

    @Test
    public void givenArchiveAvailableOnCacheWhenCleanupOccursThenAccessRequestsRequiringArchiveAreUpdated()
        throws Exception {
        // Given
        logicalClock.freezeTime();

        String accessRequestId1 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            accessRequestId1,
            CONTAINER_1,
            List.of("obj1"),
            getNowMinusMinutes(30),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            TENANT_ID,
            0
        );
        String accessRequestId2 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            accessRequestId2,
            CONTAINER_1,
            List.of("obj2"),
            getNowMinusMinutes(20),
            null,
            null,
            null,
            List.of("tarId2"),
            TENANT_ID,
            0
        );
        String accessRequestId3 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest3 = new TapeAccessRequestReferentialEntity(
            accessRequestId3,
            CONTAINER_1,
            List.of("obj3"),
            getNowMinusMinutes(20),
            null,
            null,
            null,
            List.of("tarId1", "tarId3"),
            TENANT_ID,
            0
        );

        CountDownLatch timerExecuted = new CountDownLatch(1);
        doAnswer(args -> {
            timerExecuted.countDown();
            return List.of(accessRequest1, accessRequest2, accessRequest3);
        })
            .when(accessRequestReferentialRepository)
            .findNonReadyAccessRequests();

        doReturn(List.of(accessRequest1, accessRequest2))
            .when(accessRequestReferentialRepository)
            .findByUnavailableArchiveId("tarId2");

        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId1");
        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId2");
        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");

        doReturn(true).when(accessRequestReferentialRepository).updateAccessRequest(any(), anyInt());

        // When
        instance.startExpirationHandler();

        Uninterruptibles.awaitUninterruptibly(timerExecuted, 10, TimeUnit.SECONDS);
        instance.shutdown();

        // Then
        verify(accessRequestReferentialRepository, atLeastOnce()).findNonReadyAccessRequests();
        verify(accessRequestReferentialRepository, atLeastOnce()).findByUnavailableArchiveId("tarId2");
        verify(accessRequestReferentialRepository, atLeastOnce()).cleanupAndGetExpiredAccessRequests();

        ArgumentCaptor<TapeAccessRequestReferentialEntity> updatedAccessRequestsArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository, times(2)).updateAccessRequest(
            updatedAccessRequestsArgumentCaptor.capture(),
            anyInt()
        );

        TapeAccessRequestReferentialEntity updatedAccessRequest1 = updatedAccessRequestsArgumentCaptor
            .getAllValues()
            .get(0);
        TapeAccessRequestReferentialEntity updatedAccessRequest2 = updatedAccessRequestsArgumentCaptor
            .getAllValues()
            .get(1);

        assertThat(updatedAccessRequest1.getRequestId()).isEqualTo(accessRequest1.getRequestId());
        assertThat(updatedAccessRequest1.getObjectNames()).isEqualTo(List.of("obj1"));
        assertThat(updatedAccessRequest1.getCreationDate()).isEqualTo(accessRequest1.getCreationDate());
        assertThat(updatedAccessRequest1.getReadyDate()).isNull();
        assertThat(updatedAccessRequest1.getExpirationDate()).isNull();
        assertThat(updatedAccessRequest1.getPurgeDate()).isNull();
        assertThat(updatedAccessRequest1.getUnavailableArchiveIds()).containsExactlyInAnyOrder("tarId1");
        assertThat(updatedAccessRequest1.getVersion()).isEqualTo(1);

        assertThat(updatedAccessRequest2.getRequestId()).isEqualTo(accessRequest2.getRequestId());
        assertThat(updatedAccessRequest2.getObjectNames()).isEqualTo(List.of("obj2"));
        assertThat(updatedAccessRequest2.getCreationDate()).isEqualTo(accessRequest2.getCreationDate());
        assertThat(updatedAccessRequest2.getReadyDate()).isEqualTo(now());
        assertThat(updatedAccessRequest2.getExpirationDate()).isEqualTo(getNowPlusMinutes(30));
        assertThat(updatedAccessRequest2.getPurgeDate()).isEqualTo(getNowPlusMinutes(60));
        assertThat(updatedAccessRequest2.getUnavailableArchiveIds()).isEmpty();
        assertThat(updatedAccessRequest2.getVersion()).isEqualTo(1);

        verifyNoMoreInteractions(accessRequestReferentialRepository);

        verify(archiveCacheStorage, times(3)).containsArchive(anyString(), anyString());
        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void givenNoAccessRequestAwaitingArchiveAvailabilityWhenArchiveAvailableThenAccessRequestUpdated()
        throws AccessRequestReferentialException {
        // Given
        doReturn(emptyList()).when(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId1");

        // When
        instance.updateAccessRequestWhenArchiveReady("tarId1");

        // Then
        verify(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId1");
        verifyNoMoreInteractions(accessRequestReferentialRepository);
    }

    @Test
    public void givenAccessRequestsAwaitingArchiveAvailabilityWhenArchiveAvailableThenAccessRequestUpdated()
        throws AccessRequestReferentialException {
        // Given
        logicalClock.freezeTime();

        String accessRequestId1 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest1 = new TapeAccessRequestReferentialEntity(
            accessRequestId1,
            CONTAINER_1,
            List.of("obj1"),
            getNowMinusMinutes(30),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            TENANT_ID,
            0
        );
        String accessRequestId2 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest2 = new TapeAccessRequestReferentialEntity(
            accessRequestId2,
            CONTAINER_1,
            List.of("obj2"),
            getNowMinusMinutes(20),
            null,
            null,
            null,
            List.of("tarId2"),
            TENANT_ID,
            0
        );
        doReturn(List.of(accessRequest1, accessRequest2))
            .when(accessRequestReferentialRepository)
            .findByUnavailableArchiveId("tarId2");

        doReturn(true).when(accessRequestReferentialRepository).updateAccessRequest(any(), anyInt());

        // When
        instance.updateAccessRequestWhenArchiveReady("tarId2");

        // Then
        verify(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId2");

        ArgumentCaptor<TapeAccessRequestReferentialEntity> updatedAccessRequestsArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository, times(2)).updateAccessRequest(
            updatedAccessRequestsArgumentCaptor.capture(),
            anyInt()
        );

        TapeAccessRequestReferentialEntity updatedAccessRequest1 = updatedAccessRequestsArgumentCaptor
            .getAllValues()
            .get(0);
        TapeAccessRequestReferentialEntity updatedAccessRequest2 = updatedAccessRequestsArgumentCaptor
            .getAllValues()
            .get(1);

        assertThat(updatedAccessRequest1.getRequestId()).isEqualTo(accessRequest1.getRequestId());
        assertThat(updatedAccessRequest1.getObjectNames()).isEqualTo(List.of("obj1"));
        assertThat(updatedAccessRequest1.getCreationDate()).isEqualTo(accessRequest1.getCreationDate());
        assertThat(updatedAccessRequest1.getReadyDate()).isNull();
        assertThat(updatedAccessRequest1.getExpirationDate()).isNull();
        assertThat(updatedAccessRequest1.getPurgeDate()).isNull();
        assertThat(updatedAccessRequest1.getUnavailableArchiveIds()).containsExactlyInAnyOrder("tarId1");
        assertThat(updatedAccessRequest1.getVersion()).isEqualTo(1);

        assertThat(updatedAccessRequest2.getRequestId()).isEqualTo(accessRequest2.getRequestId());
        assertThat(updatedAccessRequest2.getObjectNames()).isEqualTo(List.of("obj2"));
        assertThat(updatedAccessRequest2.getCreationDate()).isEqualTo(accessRequest2.getCreationDate());
        assertThat(updatedAccessRequest2.getReadyDate()).isEqualTo(now());
        assertThat(updatedAccessRequest2.getExpirationDate()).isEqualTo(getNowPlusMinutes(30));
        assertThat(updatedAccessRequest2.getPurgeDate()).isEqualTo(getNowPlusMinutes(60));
        assertThat(updatedAccessRequest2.getUnavailableArchiveIds()).isEmpty();
        assertThat(updatedAccessRequest2.getVersion()).isEqualTo(1);

        verifyNoMoreInteractions(accessRequestReferentialRepository);
    }

    @Test
    public void givenAccessRequestAwaitingArchiveAvailabilityWithConcurrentUpdateWhenArchiveAvailableThenAccessRequestUpdated()
        throws AccessRequestReferentialException {
        // Given
        logicalClock.freezeTime();

        // V0 of access request
        String accessRequestId1 = generateAccessRequestId();
        TapeAccessRequestReferentialEntity accessRequest1V0 = new TapeAccessRequestReferentialEntity(
            accessRequestId1,
            CONTAINER_1,
            List.of("obj1"),
            getNowMinusMinutes(30),
            null,
            null,
            null,
            List.of("tarId1", "tarId2"),
            TENANT_ID,
            0
        );
        doReturn(List.of(accessRequest1V0))
            .when(accessRequestReferentialRepository)
            .findByUnavailableArchiveId("tarId2");

        // First update fails, second succeeds
        doReturn(false).when(accessRequestReferentialRepository).updateAccessRequest(any(), eq(0));
        doReturn(true).when(accessRequestReferentialRepository).updateAccessRequest(any(), eq(1));

        // V1 of access request
        TapeAccessRequestReferentialEntity accessRequest1V1 = new TapeAccessRequestReferentialEntity(
            accessRequest1V0.getRequestId(),
            accessRequest1V0.getContainerName(),
            accessRequest1V0.getObjectNames(),
            accessRequest1V0.getCreationDate(),
            null,
            null,
            null,
            List.of("tarId2"),
            TENANT_ID,
            1
        );
        doReturn(Optional.of(accessRequest1V1))
            .when(accessRequestReferentialRepository)
            .findByRequestId(accessRequestId1);

        // When
        instance.updateAccessRequestWhenArchiveReady("tarId2");

        // Then
        verify(accessRequestReferentialRepository).findByUnavailableArchiveId("tarId2");

        verify(accessRequestReferentialRepository).findByRequestId(accessRequestId1);

        ArgumentCaptor<TapeAccessRequestReferentialEntity> updatedAccessRequestsArgumentCaptor =
            ArgumentCaptor.forClass(TapeAccessRequestReferentialEntity.class);
        verify(accessRequestReferentialRepository, times(2)).updateAccessRequest(
            updatedAccessRequestsArgumentCaptor.capture(),
            anyInt()
        );

        TapeAccessRequestReferentialEntity firstUpdate = updatedAccessRequestsArgumentCaptor.getAllValues().get(0);
        TapeAccessRequestReferentialEntity updatedAccessRequest2 = updatedAccessRequestsArgumentCaptor
            .getAllValues()
            .get(1);

        assertThat(firstUpdate.getRequestId()).isEqualTo(accessRequestId1);
        assertThat(firstUpdate.getObjectNames()).isEqualTo(List.of("obj1"));
        assertThat(firstUpdate.getCreationDate()).isEqualTo(accessRequest1V0.getCreationDate());
        assertThat(firstUpdate.getReadyDate()).isNull();
        assertThat(firstUpdate.getExpirationDate()).isNull();
        assertThat(firstUpdate.getPurgeDate()).isNull();
        assertThat(firstUpdate.getUnavailableArchiveIds()).containsExactlyInAnyOrder("tarId1");
        assertThat(firstUpdate.getVersion()).isEqualTo(1);

        assertThat(updatedAccessRequest2.getRequestId()).isEqualTo(accessRequestId1);
        assertThat(updatedAccessRequest2.getObjectNames()).isEqualTo(List.of("obj1"));
        assertThat(updatedAccessRequest2.getCreationDate()).isEqualTo(accessRequest1V0.getCreationDate());
        assertThat(updatedAccessRequest2.getReadyDate()).isEqualTo(now());
        assertThat(updatedAccessRequest2.getExpirationDate()).isEqualTo(getNowPlusMinutes(30));
        assertThat(updatedAccessRequest2.getPurgeDate()).isEqualTo(getNowPlusMinutes(60));
        assertThat(updatedAccessRequest2.getUnavailableArchiveIds()).isEmpty();
        assertThat(updatedAccessRequest2.getVersion()).isEqualTo(2);

        verifyNoMoreInteractions(accessRequestReferentialRepository);
    }

    @Test
    public void givenAvailableAndNonAvailableObjectsWhenCheckObjectAvailabilityThenFalse() throws Exception {
        // Given :

        doReturn(
            List.of(
                // obj1 : still in input_file
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj1"),
                    100L,
                    "SHA-512",
                    "digest1",
                    "obj1-guid1",
                    new TapeLibraryInputFileObjectStorageLocation(),
                    nextDate(),
                    nextDate()
                ),
                // obj2 : in tars
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj2"),
                    400L,
                    "SHA-512",
                    "digest2",
                    "obj2-guid2",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId1", "obj2-0", 1500L, 100L, "digest2-1"),
                            new TarEntryDescription("tarId2", "obj2-1", 0L, 100L, "digest2-2"),
                            new TarEntryDescription("tarId3", "obj2-2", 0L, 100L, "digest2-3"),
                            new TarEntryDescription("tarId4", "obj2-3", 0L, 100L, "digest2-4")
                        )
                    ),
                    nextDate(),
                    nextDate()
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));

        doReturn(
            List.of(
                // Tar1 : Ready on disk
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    2000L,
                    "digest-tarId1",
                    nextDate()
                ),
                // Tar2 : Building on disk
                new TapeArchiveReferentialEntity(
                    "tarId2",
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    null,
                    null,
                    nextDate()
                ),
                // Tar3 : On tape + in cache
                new TapeArchiveReferentialEntity(
                    "tarId3",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape1", 1234),
                    EntryType.DATA,
                    5000L,
                    "digest-tarId3",
                    nextDate()
                ),
                // Tar4 : Only on tape
                new TapeArchiveReferentialEntity(
                    "tarId4",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape2", 4321),
                    EntryType.DATA,
                    8000L,
                    "digest-tarId4",
                    nextDate()
                )
            )
        )
            .when(archiveReferentialRepository)
            .bulkFind(Set.of("tarId1", "tarId2", "tarId3", "tarId4"));

        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId4");

        // When
        boolean objectAvailability = instance.checkObjectAvailability(CONTAINER_1, List.of("obj1", "obj2"));

        // Then
        assertThat(objectAvailability).isFalse();
        verifyNoMoreInteractions(accessRequestReferentialRepository);

        verify(objectReferentialRepository).bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));
        verifyNoMoreInteractions(objectReferentialRepository);

        verify(archiveReferentialRepository).bulkFind(Set.of("tarId1", "tarId2", "tarId3", "tarId4"));
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId4");
        verifyNoMoreInteractions(archiveCacheStorage);
        verifyNoMoreInteractions(readWriteQueue);
    }

    @Test
    public void givenAvailableObjectsWhenCheckObjectAvailabilityThenTrue() throws Exception {
        // Given :

        doReturn(
            List.of(
                // obj1 : still in input_file
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj1"),
                    100L,
                    "SHA-512",
                    "digest1",
                    "obj1-guid1",
                    new TapeLibraryInputFileObjectStorageLocation(),
                    nextDate(),
                    nextDate()
                ),
                // obj2 : in tars
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(CONTAINER_1, "obj2"),
                    400L,
                    "SHA-512",
                    "digest2",
                    "obj2-guid2",
                    new TapeLibraryTarObjectStorageLocation(
                        List.of(
                            new TarEntryDescription("tarId1", "obj2-0", 1500L, 100L, "digest2-1"),
                            new TarEntryDescription("tarId2", "obj2-1", 0L, 100L, "digest2-2"),
                            new TarEntryDescription("tarId3", "obj2-2", 0L, 100L, "digest2-3")
                        )
                    ),
                    nextDate(),
                    nextDate()
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));

        doReturn(
            List.of(
                // Tar1 : Ready on disk
                new TapeArchiveReferentialEntity(
                    "tarId1",
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    2000L,
                    "digest-tarId1",
                    nextDate()
                ),
                // Tar2 : Building on disk
                new TapeArchiveReferentialEntity(
                    "tarId2",
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    EntryType.DATA,
                    null,
                    null,
                    nextDate()
                ),
                // Tar3 : On tape + in cache
                new TapeArchiveReferentialEntity(
                    "tarId3",
                    new TapeLibraryOnTapeArchiveStorageLocation("tape1", 1234),
                    EntryType.DATA,
                    5000L,
                    "digest-tarId3",
                    nextDate()
                )
            )
        )
            .when(archiveReferentialRepository)
            .bulkFind(Set.of("tarId1", "tarId2", "tarId3"));

        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");

        // When
        boolean objectAvailability = instance.checkObjectAvailability(CONTAINER_1, List.of("obj1", "obj2"));

        // Then
        assertThat(objectAvailability).isTrue();

        verifyNoMoreInteractions(accessRequestReferentialRepository);

        verify(objectReferentialRepository).bulkFind(CONTAINER_1, Set.of("obj1", "obj2"));
        verifyNoMoreInteractions(objectReferentialRepository);

        verify(archiveReferentialRepository).bulkFind(Set.of("tarId1", "tarId2", "tarId3"));
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, "tarId3");
        verifyNoMoreInteractions(archiveCacheStorage);

        verifyNoMoreInteractions(readWriteQueue);
    }

    @Test
    public void givenMissingParametersWhenCheckObjectAvailabilityThenKO() {
        Assertions.assertThatThrownBy(() -> instance.checkObjectAvailability(CONTAINER_1, null)).isInstanceOf(
            IllegalArgumentException.class
        );
        Assertions.assertThatThrownBy(
            () -> instance.checkObjectAvailability(CONTAINER_1, Arrays.asList("obj1", null))
        ).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(
            () -> instance.checkObjectAvailability(null, List.of("obj1", "obj2"))
        ).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> instance.checkObjectAvailability(CONTAINER_1, emptyList())).isInstanceOf(
            ContentAddressableStorageBadRequestException.class
        );
    }

    @Test
    public void givenDuplicateObjectsWhenCheckingObjectAvailabilityThenKO() {
        // Given
        List<String> objectIds = List.of("obj1", "obj2", "obj1");

        // When / Then
        Assertions.assertThatThrownBy(() -> instance.checkObjectAvailability(CONTAINER_1, objectIds)).isInstanceOf(
            ContentAddressableStorageBadRequestException.class
        );
    }

    private String nextDate() {
        logicalClock.logicalSleep(1, ChronoUnit.SECONDS);
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }

    private String now() {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }

    private String getNowPlusMinutes(int plusMinutes) {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().plusMinutes(plusMinutes));
    }

    private String getNowMinusMinutes(int minusMinutes) {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now().minusMinutes(minusMinutes));
    }
}
