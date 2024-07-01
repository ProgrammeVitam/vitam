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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCacheEvictionJudge;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ArchiveCacheStorageTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveCacheStorageTest.class);

    private static final String FILE_BUCKET_1 = "fileBucket1";
    private static final String FILE_BUCKET_2 = "fileBucket2";
    private static final String FILE_BUCKET_3 = "fileBucket3";
    private static final String UNKNOWN_FILE_BUCKET = "unknown";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private AlertService alertService;

    @Mock
    private BucketTopologyHelper bucketTopologyHelper;

    @Mock
    private ArchiveCacheEvictionController archiveCacheEvictionController;

    @Mock
    private LRUCacheEvictionJudge<ArchiveCacheEntry> evictionJudge;

    @Mock
    private Executor evictionExecutor;

    private final AtomicBoolean failedExecutor = new AtomicBoolean(false);
    private final CountDownLatch beforeExecution = new CountDownLatch(1);
    private final CountDownLatch afterExecution = new CountDownLatch(1);

    @Before
    public void beforeTest() throws IOException {
        tempFolder.create();

        // Valid fileBucketIds
        List<String> validFilesBucketIds = Arrays.asList(FILE_BUCKET_1, FILE_BUCKET_2, FILE_BUCKET_3);
        doAnswer(args -> validFilesBucketIds.contains(args.getArgument(0, String.class)))
            .when(bucketTopologyHelper)
            .isValidFileBucketId(anyString());

        // Executor
        doAnswer(args -> {
            Runnable command = args.getArgument(0);
            new Thread(() -> {
                try {
                    awaitUninterruptibly(beforeExecution);
                    command.run();
                } catch (Exception e) {
                    LOGGER.error("Executor command failed with exception", e);
                    failedExecutor.set(true);
                } finally {
                    afterExecution.countDown();
                }
            }).start();
            return null;
        })
            .when(evictionExecutor)
            .execute(any());

        doReturn(evictionJudge).when(archiveCacheEvictionController).computeEvictionJudge();
    }

    @After
    public void afterTests() {
        assertThat(failedExecutor.get()).withFailMessage("Executor command failed with exception").isFalse();

        // Ensure no more interactions with mocks
        verifyNoMoreInteractions(alertService, evictionExecutor);
    }

    @Test
    public void testInitialization_givenEmptyCacheFolderWhenCacheInitializedThenOK()
        throws IllegalPathException, IOException {
        // Given (empty dir)

        // When
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L
        );

        // Then
        assertThat(instance.getMaxStorageSpace()).isEqualTo(1_000_000L);
        assertThat(instance.getEvictionStorageSpaceThreshold()).isEqualTo(800_000L);
        assertThat(instance.getSafeStorageSpaceThreshold()).isEqualTo(700_000L);
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(0L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", false, false, false);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testInitialization_givenNonEmptyCacheFolderWhenCacheInitializedThenOK() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId3", 200_000);

        // When
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // Then
        assertThat(instance.getMaxStorageSpace()).isEqualTo(1_000_000L);
        assertThat(instance.getEvictionStorageSpaceThreshold()).isEqualTo(800_000L);
        assertThat(instance.getSafeStorageSpaceThreshold()).isEqualTo(700_000L);
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(400_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId3", true, true, false);

        verifyFilesState(instance, FILE_BUCKET_1, "unknown_file", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId1", false, false, false);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testInitialization_givenRootFileInCacheFolderWhenCacheInitializedThenKO() throws Exception {
        // Given file stored on root folder (fileBuckedId=.)
        createArchiveFileInCache(".", "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId3", 200_000);

        // When / Then
        assertThatThrownBy(
            () ->
                new ArchiveCacheStorage(
                    tempFolder.getRoot().toString(),
                    bucketTopologyHelper,
                    archiveCacheEvictionController,
                    1_000_000L,
                    800_000L,
                    700_000L,
                    evictionExecutor,
                    alertService
                )
        ).isInstanceOf(IllegalStateException.class);
        verifyNoBackgroundEviction();
    }

    @Test
    public void testInitialization_givenUnknownFileBucketIdInCacheFolderWhenCacheInitializedThenKO() throws Exception {
        // Given
        createArchiveFileInCache(UNKNOWN_FILE_BUCKET, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId3", 200_000);

        // When / Then
        assertThatThrownBy(
            () ->
                new ArchiveCacheStorage(
                    tempFolder.getRoot().toString(),
                    bucketTopologyHelper,
                    archiveCacheEvictionController,
                    1_000_000L,
                    800_000L,
                    700_000L,
                    evictionExecutor,
                    alertService
                )
        ).isInstanceOf(IllegalStateException.class);
        verifyNoBackgroundEviction();
    }

    @Test
    public void testInitialization_givenIllegalTarIdInCacheFolderWhenCacheInitializedThenKO() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_1, "illegal#filename", 200_000);

        // When / Then
        assertThatThrownBy(
            () ->
                new ArchiveCacheStorage(
                    tempFolder.getRoot().toString(),
                    bucketTopologyHelper,
                    archiveCacheEvictionController,
                    1_000_000L,
                    800_000L,
                    700_000L,
                    evictionExecutor,
                    alertService
                )
        ).isInstanceOf(IllegalPathException.class);
        verifyNoBackgroundEviction();
    }

    @Test
    public void testInitialization_givenFullCacheFolderWhenCacheInitializedThenBackgroundCacheEvictionDeletesOldestArchives()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 200_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId4", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId5", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId6", 200_000);
        doReturn(true).when(evictionJudge).canEvictEntry(any());

        // When
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // Then
        assertThat(instance.getMaxStorageSpace()).isEqualTo(1_000_000L);
        assertThat(instance.getEvictionStorageSpaceThreshold()).isEqualTo(800_000L);
        assertThat(instance.getSafeStorageSpaceThreshold()).isEqualTo(700_000L);
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(1_200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId5", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId6", true, true, false);

        // When : Eviction process finished
        awaitBackgroundEvictionTermination();

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(600_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId5", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId6", true, true, false);
    }

    @Test
    public void testInitialization_givenFullCacheFolderAndNonExpirableFileBucketWhenCacheInitializedThenBackgroundCacheEvictionDeletesOldestExpirableArchives()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 200_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId4", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId5", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId6", 200_000);

        doReturn(true).when(evictionJudge).canEvictEntry(any());
        doReturn(false).when(evictionJudge).canEvictEntry(new ArchiveCacheEntry(FILE_BUCKET_2, "tarId2"));

        // When
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // Then
        assertThat(instance.getMaxStorageSpace()).isEqualTo(1_000_000L);
        assertThat(instance.getEvictionStorageSpaceThreshold()).isEqualTo(800_000L);
        assertThat(instance.getSafeStorageSpaceThreshold()).isEqualTo(700_000L);
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(1_200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId5", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId6", true, true, false);

        // When : Eviction process finished
        awaitBackgroundEvictionTermination();

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(600_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId5", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId6", true, true, false);
    }

    @Test
    public void testArchiveReservation_givenEnoughDiskSpaceWhenReservingArchiveThenArchiveReservedAndNoEviction()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 200_000);

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(400_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, true);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenExistingArchiveReservationWhenReservingDuplicateArchiveThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 200_000);

        // When / Then
        assertThatThrownBy(() -> instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 200_000)).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(400_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, true);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenExistingArchiveInCacheWhenReservingDuplicateArchiveThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId1", 100_000)).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenExistingArchiveOnDiskButNotOnCacheWhenReservingArchiveThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        createArchiveFileInCache(FILE_BUCKET_1, "tarId3", 100_000);
        assertThatThrownBy(() -> instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000)).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", true, false, false);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenLowDiskSpaceWhenReservingArchiveThenArchiveReservedAndBackgroundCacheEvictionDeletesOldestExpirableArchives()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 200_000);
        doReturn(true).when(evictionJudge).canEvictEntry(any());
        doReturn(false).when(evictionJudge).canEvictEntry(new ArchiveCacheEntry(FILE_BUCKET_1, "tarId1"));

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId4", 200_000);

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(800_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", false, false, true);

        // When : Eviction process finished
        awaitBackgroundEvictionTermination();

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(600_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", false, false, true);
    }

    @Test
    public void testArchiveReservation_givenNoDiskSpaceWhenReservingArchiveThenKOAndNoReservationOccursAndSecurityAlert()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 200_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 200_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 200_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When
        assertThatThrownBy(() -> instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId4", 400_000)).isInstanceOf(
            IllegalStateException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(600_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId4", false, false, false);

        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenIllegalFileNameWhenReservingArchiveThenKO() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(
            () -> instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "illegal#filename", 100_000)
        ).isInstanceOf(IllegalPathException.class);

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "illegal#filename", false, false, false);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservation_givenUnknownFileBucketIdWhenReservingArchiveThenKO() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);
        createArchiveFileInCache(FILE_BUCKET_3, "tarId3", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(
            () -> instance.reserveArchiveStorageSpace(UNKNOWN_FILE_BUCKET, "tarId4", 100_000)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyNoBackgroundEviction();
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveReservationWhenWrittenArchiveMovedToCacheThenArchiveAddedToCache()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When
        instance.reserveArchiveStorageSpace(FILE_BUCKET_3, "tarId3", 100_000);
        File tmpTar3 = createTmpArchiveFile(100_000);
        instance.moveArchiveToCache(tmpTar3.toPath(), FILE_BUCKET_3, "tarId3");

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", true, true, false);
    }

    @Test
    public void testArchiveReservationConfirmation_givenUnknownArchiveReservationWhenTryMovingUnreservedArchiveToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        File tmpTar3 = createTmpArchiveFile(100_000);
        assertThatThrownBy(() -> instance.moveArchiveToCache(tmpTar3.toPath(), FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveReservationWhenTryMovingNonExistingArchiveToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);
        Path nonExistingTmpTarFilePath = tempFolder.getRoot().toPath().resolve(FILE_BUCKET_1).resolve("tarId3.tmp");
        assertThatThrownBy(
            () -> instance.moveArchiveToCache(nonExistingTmpTarFilePath, FILE_BUCKET_1, "tarId3")
        ).isInstanceOf(FileNotFoundException.class);

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, true);
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveReservationWhenTryMovingNonRegularFileToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);
        Path nonRegularFile = tempFolder.getRoot().toPath().resolve(FILE_BUCKET_1).resolve("tarId3.tmp");
        FileUtils.forceMkdir(nonRegularFile.toFile());
        assertThatThrownBy(() -> instance.moveArchiveToCache(nonRegularFile, FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IOException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, true);
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveReservationWhenTryMovingArchiveToExistingTargetFileThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);
        File tmpTar3 = createTmpArchiveFile(100_000);
        createArchiveFileInCache(FILE_BUCKET_1, "tarId3", 100_000);
        assertThatThrownBy(() -> instance.moveArchiveToCache(tmpTar3.toPath(), FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IOException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", true, false, true);
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveReservationWhenTryMovingArchiveWithInvalidLengthThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);
        File tmpTar3 = createTmpArchiveFile(200_000);
        assertThatThrownBy(() -> instance.moveArchiveToCache(tmpTar3.toPath(), FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(300_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, true);
    }

    @Test
    public void testArchiveReservationConfirmation_givenArchiveCanceledReservationWhenTryMovingArchiveToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);
        instance.cancelReservedArchive(FILE_BUCKET_1, "tarId3");

        // When / Then
        File tmpTar3 = createTmpArchiveFile(100_000);
        assertThatThrownBy(() -> instance.moveArchiveToCache(tmpTar3.toPath(), FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationConfirmation_givenUnknownArchiveBucketWhenTryMovingArchiveToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        File tmpTarId3 = createTmpArchiveFile(100_000);
        assertThatThrownBy(
            () -> instance.moveArchiveToCache(tmpTarId3.toPath(), UNKNOWN_FILE_BUCKET, "tarId3")
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, UNKNOWN_FILE_BUCKET, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationConfirmation_givenIllegalArchiveIdWhenTryMovingArchiveToCacheThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        File invalidFileName = createTmpArchiveFile(100_000);
        assertThatThrownBy(
            () -> instance.moveArchiveToCache(invalidFileName.toPath(), FILE_BUCKET_1, "illegal#filename")
        ).isInstanceOf(IllegalPathException.class);

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationCanceling_givenReservedArchiveWhenCancelingReservationThenCacheSpaceFreedAndReservationCanceled()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000);

        // When
        instance.cancelReservedArchive(FILE_BUCKET_1, "tarId3");

        // Then
        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationCanceling_givenNonReservedArchiveWhenCancelingReservationThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.cancelReservedArchive(FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReservationCanceling_givenCanceledArchiveReservationWhenDuplicateReservationCancelingThenKO()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 100_000L);
        instance.cancelReservedArchive(FILE_BUCKET_1, "tarId3");

        // When / Then
        assertThatThrownBy(() -> instance.cancelReservedArchive(FILE_BUCKET_1, "tarId3")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThat(instance.getCurrentStorageSpaceUsage()).isEqualTo(200_000L);

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_1, "tarId3", false, false, false);
    }

    @Test
    public void testArchiveReading_givenFileInCacheWhenReadingArchiveThenOK() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 50_000L);
        File tmpTarId3 = createTmpArchiveFile(50_000);
        instance.moveArchiveToCache(tmpTarId3.toPath(), FILE_BUCKET_1, "tarId3");

        // When
        Optional<FileInputStream> tarId3InputStream = instance.tryReadArchive(FILE_BUCKET_1, "tarId3");

        // Then
        assertThat(tarId3InputStream).isPresent();
        assertThat(tarId3InputStream.get()).hasSameContentAs(
            new ExactSizeInputStream(new NullInputStream(50_000L), 50_000L)
        );
    }

    @Test
    public void testArchiveReading_givenReservedFileWhenReadingArchiveThenNothingReturned() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 50_000L);

        // When
        Optional<FileInputStream> tarId3InputStream = instance.tryReadArchive(FILE_BUCKET_1, "tarId3");

        // Then
        assertThat(tarId3InputStream).isEmpty();
    }

    @Test
    public void testArchiveReading_givenUnknownFileWhenReadingArchiveThenNothingReturned() throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 100_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 100_000);

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When
        Optional<FileInputStream> tarId3InputStream = instance.tryReadArchive(FILE_BUCKET_1, "tarId3");

        // Then
        assertThat(tarId3InputStream).isEmpty();
    }

    @Test
    public void testArchiveReading_givenFileInCacheWhenReadingArchiveThenLastUpdateDateRefreshedAndArchiveEvictedLast()
        throws Exception {
        // Given
        createArchiveFileInCache(FILE_BUCKET_1, "tarId1", 300_000);
        createArchiveFileInCache(FILE_BUCKET_2, "tarId2", 200_000);
        doReturn(true).when(evictionJudge).canEvictEntry(any());

        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        instance.reserveArchiveStorageSpace(FILE_BUCKET_1, "tarId3", 50_000L);
        File tmpTarId3 = createTmpArchiveFile(50_000);
        instance.moveArchiveToCache(tmpTarId3.toPath(), FILE_BUCKET_1, "tarId3");

        // When
        Optional<FileInputStream> tarId1InputStream = instance.tryReadArchive(FILE_BUCKET_1, "tarId1");

        // Then
        assertThat(tarId1InputStream).isPresent();
        assertThat(tarId1InputStream.get()).hasSameContentAs(
            new ExactSizeInputStream(new NullInputStream(300_000L), 300_000L)
        );

        // When
        instance.reserveArchiveStorageSpace(FILE_BUCKET_3, "tarId3", 300_000L);

        // Then : tarId2 evicted first
        awaitBackgroundEvictionTermination();

        verifyFilesState(instance, FILE_BUCKET_1, "tarId1", true, true, false);
        verifyFilesState(instance, FILE_BUCKET_2, "tarId2", false, false, false);
        verifyFilesState(instance, FILE_BUCKET_3, "tarId3", false, false, true);
    }

    @Test
    public void testArchiveReading_givenIllegalArchiveNameWhenReadingArchiveThenKO() throws Exception {
        // Given
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.tryReadArchive(FILE_BUCKET_1, "illegal#filename")).isInstanceOf(
            IllegalPathException.class
        );
    }

    @Test
    public void testArchiveReading_givenUnknownArchiveBucketWhenReadingArchiveThenKO() throws Exception {
        // Given
        ArchiveCacheStorage instance = new ArchiveCacheStorage(
            tempFolder.getRoot().toString(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            1_000_000L,
            800_000L,
            700_000L,
            evictionExecutor,
            alertService
        );

        // When / Then
        assertThatThrownBy(() -> instance.tryReadArchive(UNKNOWN_FILE_BUCKET, "tarId")).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    private void createArchiveFileInCache(String fileBucketId, String filename, int size) throws IOException {
        File fileBucketFolder = new File(tempFolder.getRoot(), fileBucketId);
        FileUtils.forceMkdir(fileBucketFolder);

        File file = new File(fileBucketFolder, filename);
        FileUtils.copyInputStreamToFile(new NullInputStream(size), file);

        logicalSleep();
        Files.setAttribute(
            file.toPath(),
            "lastAccessTime",
            FileTime.from(LocalDateUtil.now().toInstant(ZoneOffset.UTC))
        );
    }

    private File createTmpArchiveFile(int size) throws IOException {
        File file = tempFolder.newFile();
        FileUtils.copyInputStreamToFile(new NullInputStream(size), file);
        return file;
    }

    private void verifyFilesState(
        ArchiveCacheStorage instance,
        String fileBucketId,
        String tarId,
        boolean existsOnDisk,
        boolean existsInCached,
        boolean reservedInCache
    ) {
        File bucketFolder = new File(tempFolder.getRoot(), fileBucketId);

        File file = new File(bucketFolder, tarId);
        assertThat(file.exists())
            .withFailMessage(
                "Expecting file " + fileBucketId + "/" + tarId + " to" + (existsOnDisk ? "" : " not") + " exist on disk"
            )
            .isEqualTo(existsOnDisk);

        assertThat(instance.containsArchive(fileBucketId, tarId))
            .withFailMessage(
                "Expecting file " +
                fileBucketId +
                "/" +
                tarId +
                " to" +
                (existsInCached ? "" : " not") +
                " exist in cache"
            )
            .isEqualTo(existsInCached);

        assertThat(instance.isArchiveReserved(fileBucketId, tarId))
            .withFailMessage(
                "Expecting file " +
                fileBucketId +
                "/" +
                tarId +
                " to be" +
                (reservedInCache ? "" : " not") +
                " reserved in cache"
            )
            .isEqualTo(reservedInCache);
    }

    public void awaitBackgroundEvictionTermination() {
        verify(evictionExecutor).execute(any());
        beforeExecution.countDown();
        awaitUninterruptibly(afterExecution);
    }

    private void verifyNoBackgroundEviction() {
        verify(evictionExecutor, never()).execute(any());
        verify(evictionJudge, never()).canEvictEntry(any());
    }

    private void logicalSleep() {
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
    }
}
