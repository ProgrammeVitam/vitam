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

import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.TMP_EXTENSION;
import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.archiveFileNameRelativeToInputArchiveStorageFolder;
import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.createTarId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class WriteOrderCreatorBootstrapRecoveryTest {

    private static final String FILE_BUCKET_1 = "test-metadata";
    private static final String FILE_BUCKET_2 = "test-objects";
    private static final String BUCKED_ID = "test";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private TarFileRapairer tarFileRapairer;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private BucketTopologyHelper bucketTopologyHelper;

    @Mock
    private WriteOrderCreator writeOrderCreator;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    private Path inputTarStorageFolder;
    private WriteOrderCreatorBootstrapRecovery writeOrderCreatorBootstrapRecovery;

    @Before
    public void initialize() throws Exception {
        inputTarStorageFolder = temporaryFolder.newFolder("inputTar").toPath();

        writeOrderCreatorBootstrapRecovery = new WriteOrderCreatorBootstrapRecovery(
            inputTarStorageFolder.toString(),
            archiveReferentialRepository,
            bucketTopologyHelper,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );

        doReturn(ImmutableSet.of(FILE_BUCKET_1, FILE_BUCKET_2)).when(bucketTopologyHelper).listFileBuckets();

        doReturn(BUCKED_ID).when(bucketTopologyHelper).getBucketFromFileBucket(FILE_BUCKET_1);
        doReturn(BUCKED_ID).when(bucketTopologyHelper).getBucketFromFileBucket(FILE_BUCKET_2);
    }

    @Test
    public void initializeEmptyFileBucketDirectories() throws Exception {
        // Given
        Files.createDirectories(inputTarStorageFolder.resolve(FILE_BUCKET_1));
        Files.createDirectories(inputTarStorageFolder.resolve(FILE_BUCKET_2));

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeNonExistingFileBucketDirectories() {
        // Given

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverTmpTarFiles() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar.tmp  : Tmp file (to be repaired & rescheduled)
         * fileBucket2 :
         *  - tar2.tar.tmp  : Tmp file (to be repaired & rescheduled)
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId1 = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-01-01T01:23:45.678"));
        String tarId2 = createTarId(LocalDateUtil.parseMongoFormattedDate("2020-02-01T01:23:45.678"));

        Path tmpFile1 = fileBucketFolder1.resolve(tarId1 + LocalFileUtils.TMP_EXTENSION);
        Path tmpFile2 = fileBucketFolder2.resolve(tarId2 + LocalFileUtils.TMP_EXTENSION);

        Files.createFile(tmpFile1);
        Files.createFile(tmpFile2);

        TarFileRapairer.DigestWithSize digestWithSize1 = new TarFileRapairer.DigestWithSize(10L, "digest1");
        TarFileRapairer.DigestWithSize digestWithSize2 = new TarFileRapairer.DigestWithSize(12L, "digest2");
        doReturn(digestWithSize1).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId1));
        doReturn(digestWithSize2).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId2));

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verify(tarFileRapairer, times(2)).repairAndVerifyTarArchive(any(), any(), any());
        verify(bucketTopologyHelper, times(2)).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator, times(2)).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues())
            .extracting(
                WriteOrder::getArchiveId,
                WriteOrder::getBucket,
                WriteOrder::getFileBucketId,
                WriteOrder::getDigest,
                WriteOrder::getSize,
                WriteOrder::getFilePath
            )
            .containsExactly(
                tuple(
                    tarId1,
                    BUCKED_ID,
                    FILE_BUCKET_1,
                    "digest1",
                    10L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_1, tarId1)
                ),
                tuple(
                    tarId2,
                    BUCKED_ID,
                    FILE_BUCKET_2,
                    "digest2",
                    12L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_2, tarId2)
                )
            );

        Path targetFile1 = fileBucketFolder1.resolve(tarId1);
        Path targetFile2 = fileBucketFolder2.resolve(tarId2);

        assertThat(tmpFile1).doesNotExist();
        assertThat(tmpFile2).doesNotExist();
        assertThat(targetFile1).exists();
        assertThat(targetFile2).exists();

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverTarAlreadyOnTapeAndNotInCache() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar      : Complete tar file already marked as on tape (to be deleted)
         * fileBucket2 :
         *  -
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-01-01T01:23:45.678"));

        Path tarFile = fileBucketFolder1.resolve(tarId);
        Files.copy(new NullInputStream(10L), tarFile);

        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId,
                    new TapeLibraryOnTapeArchiveStorageLocation("tape code", 13),
                    10L,
                    "digest1",
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId);

        doReturn(false).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, tarId);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(anyString(), anyString(), anyLong());
        doAnswer(args -> {
            Path filePath = args.getArgument(0);
            assertThat(filePath).exists();
            Files.delete(filePath);
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), anyString(), anyString());

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then : delete file
        verify(bucketTopologyHelper).listFileBuckets();
        verify(archiveReferentialRepository).find(tarId);
        assertThat(tarFile).doesNotExist();

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, tarId);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FILE_BUCKET_1, tarId, 10);
        verify(archiveCacheStorage).moveArchiveToCache(eq(tarFile), eq(FILE_BUCKET_1), eq(tarId));

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverTarAlreadyOnTapeAndInCache() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar      : Complete tar file already marked as on tape (to be deleted)
         * fileBucket2 :
         *  -
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-01-01T01:23:45.678"));

        Path tarFile = fileBucketFolder1.resolve(tarId);
        Files.copy(new NullInputStream(10L), tarFile);

        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId,
                    new TapeLibraryOnTapeArchiveStorageLocation("tape code", 13),
                    10L,
                    "digest1",
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId);

        doReturn(true).when(archiveCacheStorage).containsArchive(FILE_BUCKET_1, tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then : delete file
        verify(bucketTopologyHelper).listFileBuckets();
        verify(archiveReferentialRepository).find(tarId);
        assertThat(tarFile).doesNotExist();

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, tarId);

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverTarBuildingOnDisk() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar      : Complete tar marked as building on disk (to be verified & rescheduled)
         * fileBucket2 :
         *  -
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-01-01T01:23:45.678"));

        Path tarFile = fileBucketFolder1.resolve(tarId);
        Files.copy(new NullInputStream(10L), tarFile);

        doReturn(new TarFileRapairer.DigestWithSize(10L, "digest1")).when(tarFileRapairer).verifyTarArchive(any());

        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId,
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    null,
                    null,
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verify(archiveReferentialRepository).find(tarId);
        verify(tarFileRapairer).verifyTarArchive(any());

        verify(bucketTopologyHelper).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues())
            .extracting(
                WriteOrder::getArchiveId,
                WriteOrder::getBucket,
                WriteOrder::getFileBucketId,
                WriteOrder::getDigest,
                WriteOrder::getSize,
                WriteOrder::getFilePath
            )
            .containsExactly(
                tuple(
                    tarId,
                    BUCKED_ID,
                    FILE_BUCKET_1,
                    "digest1",
                    10L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_1, tarId)
                )
            );
        assertThat(tarFile).exists();

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverReadyTar() throws Exception {
        /*
         * fileBucket1 :
         *  -
         * fileBucket2 :
         *  - tar1.tar      : Complete tar file marked as ready (to be rescheduled)
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-03-01T01:23:45.678"));

        Path tarFile = fileBucketFolder2.resolve(tarId);
        Files.copy(new NullInputStream(10L), tarFile);

        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId,
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    10L,
                    "digest1",
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();

        verify(archiveReferentialRepository).find(tarId);

        verify(bucketTopologyHelper).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues())
            .extracting(
                WriteOrder::getArchiveId,
                WriteOrder::getBucket,
                WriteOrder::getFileBucketId,
                WriteOrder::getDigest,
                WriteOrder::getSize,
                WriteOrder::getFilePath
            )
            .containsExactly(
                tuple(
                    tarId,
                    BUCKED_ID,
                    FILE_BUCKET_2,
                    "digest1",
                    10L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_2, tarId)
                )
            );

        assertThat(tarFile).exists();

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }

    @Test
    public void initializeRecoverUnknownTarFile() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar      : Unknown complete tar file
         * fileBucket2 :
         *  -
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-03-01T01:23:45.678"));

        Path tarFile = fileBucketFolder2.resolve(tarId);
        Files.copy(new NullInputStream(10L), tarFile);

        doReturn(Optional.empty()).when(archiveReferentialRepository).find(tarId);

        // When / Then
        assertThatThrownBy(() -> writeOrderCreatorBootstrapRecovery.initializeOnBootstrap()).isInstanceOf(
            Exception.class
        );
    }

    @Test
    public void initializeRecoverMultipleFiles() throws Exception {
        /*
         * fileBucket1 :
         *  - tar1.tar      : Complete tar file already marked as on tape (to be moved to cache)
         *  - tar2.tar      : Complete tar marked as building on disk (to be verified & rescheduled)
         *  - tar3.tar.tmp  : Tmp file (to be repaired & rescheduled)
         * fileBucket2 :
         *  - tar4.tar      : Complete tar file marked as ready (to be rescheduled)
         *  - tar5.tar.tmp  : Tmp file (to be repaired & rescheduled)
         */

        // Given
        Path fileBucketFolder1 = inputTarStorageFolder.resolve(FILE_BUCKET_1);
        Path fileBucketFolder2 = inputTarStorageFolder.resolve(FILE_BUCKET_2);
        Files.createDirectories(fileBucketFolder1);
        Files.createDirectories(fileBucketFolder2);

        String tarId1 = createTarId(LocalDateUtil.parseMongoFormattedDate("2019-01-01T01:23:45.678"));
        String tarId2 = createTarId(LocalDateUtil.parseMongoFormattedDate("2020-02-01T01:23:45.678"));
        String tarId3 = createTarId(LocalDateUtil.parseMongoFormattedDate("2021-03-01T01:23:45.678"));
        String tarId4 = createTarId(LocalDateUtil.parseMongoFormattedDate("2022-04-01T01:23:45.678"));
        String tarId5 = createTarId(LocalDateUtil.parseMongoFormattedDate("2023-05-01T01:23:45.678"));

        Path tarFile1 = fileBucketFolder1.resolve(tarId1);
        Path tarFile2 = fileBucketFolder1.resolve(tarId2);
        Path tmpTarFile3 = fileBucketFolder1.resolve(tarId3 + TMP_EXTENSION);
        Path tarFile4 = fileBucketFolder2.resolve(tarId4);
        Path tmpTarFile5 = fileBucketFolder2.resolve(tarId5 + TMP_EXTENSION);

        Files.copy(new NullInputStream(10L), tarFile1);
        Files.copy(new NullInputStream(11L), tarFile2);
        Files.copy(new NullInputStream(12L), tmpTarFile3);
        Files.copy(new NullInputStream(13L), tarFile4);
        Files.copy(new NullInputStream(14L), tmpTarFile5);

        doReturn(new TarFileRapairer.DigestWithSize(11L, "digest2")).when(tarFileRapairer).verifyTarArchive(any());

        TarFileRapairer.DigestWithSize digestWithSize3 = new TarFileRapairer.DigestWithSize(12L, "digest3");
        TarFileRapairer.DigestWithSize digestWithSize5 = new TarFileRapairer.DigestWithSize(14L, "digest5");
        doReturn(digestWithSize3).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId3));
        doReturn(digestWithSize5).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId5));

        // Tar 1 has already been proceeded
        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId1,
                    new TapeLibraryOnTapeArchiveStorageLocation("tape code", 13),
                    10L,
                    "digest1",
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId1);

        // Tar 2 needs to be verified
        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId2,
                    new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                    null,
                    null,
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId2);

        // Tar 4 is already OK
        doReturn(
            Optional.of(
                new TapeArchiveReferentialEntity(
                    tarId4,
                    new TapeLibraryReadyOnDiskArchiveStorageLocation(),
                    13L,
                    "digest4",
                    null
                )
            )
        )
            .when(archiveReferentialRepository)
            .find(tarId4);

        doReturn(false).when(archiveCacheStorage).containsArchive(anyString(), anyString());
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(anyString(), anyString(), anyLong());
        doAnswer(args -> {
            Path filePath = args.getArgument(0);
            assertThat(filePath).exists();
            Files.delete(filePath);
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), anyString(), anyString());

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();

        verify(archiveReferentialRepository, times(3)).find(any());

        // File 3 will be verified
        verify(tarFileRapairer, times(1)).verifyTarArchive(any());

        verify(tarFileRapairer, times(2)).repairAndVerifyTarArchive(any(), any(), any());

        // Only 4 messages published (tar1 is already on tape)
        verify(bucketTopologyHelper, times(4)).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator, times(4)).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues())
            .extracting(
                WriteOrder::getArchiveId,
                WriteOrder::getBucket,
                WriteOrder::getFileBucketId,
                WriteOrder::getDigest,
                WriteOrder::getSize,
                WriteOrder::getFilePath
            )
            .containsExactly(
                tuple(
                    tarId2,
                    BUCKED_ID,
                    FILE_BUCKET_1,
                    "digest2",
                    11L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_1, tarId2)
                ),
                tuple(
                    tarId3,
                    BUCKED_ID,
                    FILE_BUCKET_1,
                    "digest3",
                    12L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_1, tarId3)
                ),
                tuple(
                    tarId4,
                    BUCKED_ID,
                    FILE_BUCKET_2,
                    "digest4",
                    13L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_2, tarId4)
                ),
                tuple(
                    tarId5,
                    BUCKED_ID,
                    FILE_BUCKET_2,
                    "digest5",
                    14L,
                    archiveFileNameRelativeToInputArchiveStorageFolder(FILE_BUCKET_2, tarId5)
                )
            );

        assertThat(tarFile1).doesNotExist();
        assertThat(tarFile2).exists();
        Path tarFile3 = fileBucketFolder1.resolve(tarId3);
        assertThat(tarFile3).exists();
        assertThat(tmpTarFile3).doesNotExist();
        assertThat(tarFile4).exists();
        Path tarFile5 = fileBucketFolder2.resolve(tarId5);
        assertThat(tarFile5).exists();
        assertThat(tmpTarFile5).doesNotExist();

        verify(archiveCacheStorage).containsArchive(FILE_BUCKET_1, tarId1);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FILE_BUCKET_1, tarId1, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(eq(tarFile1), eq(FILE_BUCKET_1), eq(tarId1));

        verifyNoMoreInteractions(
            bucketTopologyHelper,
            archiveReferentialRepository,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );
    }
}
