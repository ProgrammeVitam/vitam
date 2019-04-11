package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeTarReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
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
import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.createTarId;
import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private TarReferentialRepository tarReferentialRepository;

    @Mock
    private BucketTopologyHelper bucketTopologyHelper;

    @Mock
    private WriteOrderCreator writeOrderCreator;

    private Path inputTarStorageFolder;
    private WriteOrderCreatorBootstrapRecovery writeOrderCreatorBootstrapRecovery;

    @Before
    public void initialize() throws Exception {

        inputTarStorageFolder = temporaryFolder.newFolder("inputTar").toPath();

        writeOrderCreatorBootstrapRecovery = new WriteOrderCreatorBootstrapRecovery(
            inputTarStorageFolder.toString(), tarReferentialRepository,
            bucketTopologyHelper, writeOrderCreator, tarFileRapairer);

        doReturn(ImmutableSet.of(FILE_BUCKET_1, FILE_BUCKET_2))
            .when(bucketTopologyHelper).listFileBuckets();

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
        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
    }

    @Test
    public void initializeNonExistingFileBucketDirectories() throws Exception {

        // Given

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
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
        assertThat(writeOrderArgCaptor.getAllValues()).extracting(
            WriteOrder::getTarId, WriteOrder::getBucket, WriteOrder::getDigest, WriteOrder::getSize,
            WriteOrder::getFilePath)
            .containsExactly(
                tuple(tarId1, BUCKED_ID, "digest1", 10L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_1, tarId1)),
                tuple(tarId2, BUCKED_ID, "digest2", 12L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_2, tarId2))
            );

        Path targetFile1 = fileBucketFolder1.resolve(tarId1);
        Path targetFile2 = fileBucketFolder2.resolve(tarId2);

        assertThat(tmpFile1).doesNotExist();
        assertThat(tmpFile2).doesNotExist();
        assertThat(targetFile1).exists();
        assertThat(targetFile2).exists();

        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
    }

    @Test
    public void initializeRecoverTarAlreadyOnTape() throws Exception {

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
        Files.createFile(tarFile);

        doReturn(Optional.of(new TapeTarReferentialEntity(tarId,
            new TapeLibraryOnTapeTarStorageLocation("tape code", 13), 10L, "digest1", null))
        ).when(tarReferentialRepository).find(tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then : delete file
        verify(bucketTopologyHelper).listFileBuckets();
        verify(tarReferentialRepository).find(tarId);
        assertThat(tarFile).doesNotExist();

        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
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
        Files.createFile(tarFile);

        doReturn(new TarFileRapairer.DigestWithSize(10L, "digest1"))
            .when(tarFileRapairer).verifyTarArchive(any());

        doReturn(Optional.of(new TapeTarReferentialEntity(tarId,
            new TapeLibraryBuildingOnDiskTarStorageLocation(), null, null, null))
        ).when(tarReferentialRepository).find(tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();
        verify(tarReferentialRepository).find(tarId);
        verify(tarFileRapairer).verifyTarArchive(any());

        verify(bucketTopologyHelper).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues()).extracting(
            WriteOrder::getTarId, WriteOrder::getBucket, WriteOrder::getDigest, WriteOrder::getSize,
            WriteOrder::getFilePath)
            .containsExactly(
                tuple(tarId, BUCKED_ID, "digest1", 10L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_1, tarId))
            );
        assertThat(tarFile).exists();

        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
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
        Files.createFile(tarFile);

        doReturn(Optional.of(new TapeTarReferentialEntity(tarId,
            new TapeLibraryReadyOnDiskTarStorageLocation(), 10L, "digest1", null))
        ).when(tarReferentialRepository).find(tarId);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();

        verify(tarReferentialRepository).find(tarId);

        verify(bucketTopologyHelper).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues()).extracting(
            WriteOrder::getTarId, WriteOrder::getBucket, WriteOrder::getDigest, WriteOrder::getSize,
            WriteOrder::getFilePath)
            .containsExactly(
                tuple(tarId, BUCKED_ID, "digest1", 10L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_2, tarId))
            );

        assertThat(tarFile).exists();

        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
    }


    @Test
    public void initializeRecoverUnknownTarFile() throws Exception {

        /*
         * fileBucket1 :
         *  - tar1.tar      : Unkown complete tar file
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
        Files.createFile(tarFile);

        doReturn(Optional.empty())
            .when(tarReferentialRepository).find(tarId);

        // When / Then
        assertThatThrownBy(() -> writeOrderCreatorBootstrapRecovery.initializeOnBootstrap())
            .isInstanceOf(Exception.class);
    }


    @Test
    public void initializeRecoverMultipleFiles() throws Exception {

        /*
         * fileBucket1 :
         *  - tar1.tar      : Complete tar file already marked as on tape (to be deleted)
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

        Files.createFile(tarFile1);
        Files.createFile(tarFile2);
        Files.createFile(tmpTarFile3);
        Files.createFile(tarFile4);
        Files.createFile(tmpTarFile5);

        doReturn(new TarFileRapairer.DigestWithSize(11L, "digest2")).when(tarFileRapairer).verifyTarArchive(any());

        TarFileRapairer.DigestWithSize digestWithSize3 = new TarFileRapairer.DigestWithSize(12L, "digest3");
        TarFileRapairer.DigestWithSize digestWithSize5 = new TarFileRapairer.DigestWithSize(14L, "digest5");
        doReturn(digestWithSize3).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId3));
        doReturn(digestWithSize5).when(tarFileRapairer).repairAndVerifyTarArchive(any(), any(), eq(tarId5));


        // Tar 1 has already been proceeded
        doReturn(Optional.of(new TapeTarReferentialEntity(tarId1,
            new TapeLibraryOnTapeTarStorageLocation("tape code", 13), 10L, "digest1", null))
        ).when(tarReferentialRepository).find(tarId1);

        // Tar 2 needs to be verified
        doReturn(Optional.of(new TapeTarReferentialEntity(tarId2,
            new TapeLibraryBuildingOnDiskTarStorageLocation(), null, null, null))
        ).when(tarReferentialRepository).find(tarId2);

        // Tar 4 is already OK
        doReturn(Optional.of(new TapeTarReferentialEntity(tarId4,
            new TapeLibraryReadyOnDiskTarStorageLocation(), 13L, "digest4", null))
        ).when(tarReferentialRepository).find(tarId4);

        // When
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Then
        verify(bucketTopologyHelper).listFileBuckets();

        verify(tarReferentialRepository, times(3)).find(any());

        // File 3 will be verified
        verify(tarFileRapairer, times(1)).verifyTarArchive(any());

        verify(tarFileRapairer, times(2)).repairAndVerifyTarArchive(any(), any(), any());

        // Only 4 messages published (tar1 is already on tape)
        verify(bucketTopologyHelper, times(4)).getBucketFromFileBucket(any());
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator, times(4)).sendMessageToQueue(writeOrderArgCaptor.capture());
        assertThat(writeOrderArgCaptor.getAllValues()).extracting(
            WriteOrder::getTarId, WriteOrder::getBucket, WriteOrder::getDigest, WriteOrder::getSize,
            WriteOrder::getFilePath)
            .containsExactly(
                tuple(tarId2, BUCKED_ID, "digest2", 11L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_1, tarId2)),
                tuple(tarId3, BUCKED_ID, "digest3", 12L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_1, tarId3)),
                tuple(tarId4, BUCKED_ID, "digest4", 13L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_2, tarId4)),
                tuple(tarId5, BUCKED_ID, "digest5", 14L,
                    tarFileNameRelativeToInputTarStorageFolder(FILE_BUCKET_2, tarId5))
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

        verifyNoMoreInteractions(bucketTopologyHelper, tarReferentialRepository, writeOrderCreator, tarFileRapairer);
    }
}
