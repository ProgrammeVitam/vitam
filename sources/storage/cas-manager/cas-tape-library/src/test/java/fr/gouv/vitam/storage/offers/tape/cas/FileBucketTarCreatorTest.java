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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessingException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.storage.offers.tape.cas.TarTestHelper.checkEntryAtPos;
import static fr.gouv.vitam.storage.offers.tape.cas.TarTestHelper.readEntryAtPos;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FileBucketTarCreatorTest {

    private static final String INPUT_FILES = "inputFiles";
    private static final String INPUT_TARS = "inputTars";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    ObjectReferentialRepository objectReferentialRepository;

    @Mock
    ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    WriteOrderCreator writeOrderCreator;

    private Path inputFilesStoragePath;
    private Path inputTarStoragePath;
    private BasicFileStorage basicFileStorage;
    private DigestType digestType = DigestType.SHA512;

    @BeforeClass
    public static void initializeClass() {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2, 3));
        VitamConfiguration.setEnvironmentName(null);
    }

    @Before
    public void initialize() throws Exception {
        inputFilesStoragePath = temporaryFolder.getRoot().toPath().resolve(INPUT_FILES);
        inputTarStoragePath = temporaryFolder.getRoot().toPath().resolve(INPUT_TARS);

        Files.createDirectories(inputFilesStoragePath);
        Files.createDirectories(inputTarStoragePath);

        basicFileStorage = spy(new BasicFileStorage(inputFilesStoragePath.toString()));
    }

    @Test
    public void processMessageWithoutSegmentationNorRotation() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite(
                "file2",
                "0_unit",
                new NullInputStream(250_000),
                250_000,
                singletonList(new SegmentInfo(250_000L, 0)),
                0
            ),
            new ObjectToWrite(
                "file3",
                "0_objectGroup",
                new ByteArrayInputStream("test data 3".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite("file4", "0_unit", new NullInputStream(0), 0, singletonList(new SegmentInfo(0L, 0)), 0),
            new ObjectToWrite(
                "file5",
                "0_objectGroup",
                new ByteArrayInputStream("test data 5".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            )
        );
        int expectedSealedTarCount = 0;
        int expectedTmpTarCount = 1;
        int tarBufferingTimeoutInSeconds = 1000;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            500_000L,
            1_000_000L
        );
    }

    @Test
    public void processMessageMultipleVersions() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite(
                "file1",
                "0_unit",
                new NullInputStream(250_000),
                250_000,
                singletonList(new SegmentInfo(250_000L, 0)),
                0
            ),
            new ObjectToWrite("file1", "0_unit", new NullInputStream(0), 0, singletonList(new SegmentInfo(0L, 0)), 0),
            new ObjectToWrite("file1", "0_unit", new NullInputStream(10), 10, singletonList(new SegmentInfo(10L, 0)), 0)
        );

        int expectedSealedTarCount = 0;
        int expectedTmpTarCount = 1;
        int tarBufferingTimeoutInSeconds = 1000;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            500_000L,
            1_000_000L
        );
    }

    @Test
    public void processMessageWithMultipleSegmentsWithoutRotation() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite(
                "file2",
                "0_unit",
                new NullInputStream(250_000),
                250_000,
                asList(new SegmentInfo(100_000L, 0), new SegmentInfo(100_000L, 0), new SegmentInfo(50_000L, 0)),
                0
            ),
            new ObjectToWrite(
                "file3",
                "0_objectGroup",
                new ByteArrayInputStream("test data 3".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite("file4", "0_unit", new NullInputStream(0), 0, singletonList(new SegmentInfo(0L, 0)), 0),
            new ObjectToWrite(
                "file5",
                "0_objectGroup",
                new ByteArrayInputStream("test data 5".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            )
        );
        int expectedSealedTarCount = 0;
        int expectedTmpTarCount = 1;
        int tarBufferingTimeoutInSeconds = 1000;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            100_000L,
            1_000_000L
        );
    }

    @Test
    public void processMessageWithRotation() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite(
                "file2",
                "0_unit",
                new NullInputStream(250_000),
                250_000,
                asList(new SegmentInfo(100_000L, 0), new SegmentInfo(100_000L, 1), new SegmentInfo(50_000L, 1)),
                0
            ),
            new ObjectToWrite(
                "file3",
                "0_objectGroup",
                new ByteArrayInputStream("test data 3".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 1)),
                0
            ),
            new ObjectToWrite("file4", "0_unit", new NullInputStream(0), 0, singletonList(new SegmentInfo(0L, 1)), 0),
            new ObjectToWrite(
                "file5",
                "0_objectGroup",
                new ByteArrayInputStream("test data 5".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 1)),
                0
            )
        );
        int expectedSealedTarCount = 1;
        int expectedTmpTarCount = 1;
        int tarBufferingTimeoutInSeconds = 1000;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            100_000L,
            200_000L
        );
    }

    @Test
    public void processMessageRotationAfterTimeout() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                10
            ),
            new ObjectToWrite(
                "file2",
                "0_unit",
                new ByteArrayInputStream("test data 2".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 1)),
                10
            )
        );
        int expectedSealedTarCount = 2;
        int expectedTmpTarCount = 0;
        int tarBufferingTimeoutInSeconds = 5;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            500_000L,
            1_000_000L
        );
    }

    @Test
    public void initializeOnBootstrapThenProcessMessages() throws Exception {
        List<ObjectToWrite> objectsToWrite = asList(
            new ObjectToWrite(
                "file1",
                "0_unit",
                new ByteArrayInputStream("test data 1".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite(
                "file2",
                "0_unit",
                new NullInputStream(250_000),
                250_000,
                singletonList(new SegmentInfo(250_000L, 0)),
                0
            ),
            new ObjectToWrite(
                "file3",
                "0_objectGroup",
                new ByteArrayInputStream("test data 3".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            ),
            new ObjectToWrite("file4", "0_unit", new NullInputStream(0), 0, singletonList(new SegmentInfo(0L, 0)), 0),
            new ObjectToWrite(
                "file5",
                "0_objectGroup",
                new ByteArrayInputStream("test data 5".getBytes()),
                11,
                singletonList(new SegmentInfo(11L, 0)),
                0
            )
        );
        int expectedSealedTarCount = 0;
        int expectedTmpTarCount = 1;
        int tarBufferingTimeoutInSeconds = 1000;

        runProcessMessageTest(
            objectsToWrite,
            expectedSealedTarCount,
            expectedTmpTarCount,
            tarBufferingTimeoutInSeconds,
            500_000L,
            1_000_000L
        );
    }

    private void runProcessMessageTest(
        List<ObjectToWrite> objectsToWrite,
        int expectedSealedTarCount,
        int expectedTmpTarCount,
        int tarBufferingTimeoutInSeconds,
        long maxTarEntrySize,
        long maxTarFileSize
    )
        throws IOException, QueueProcessingException, InterruptedException, ArchiveReferentialException, ObjectReferentialException {
        String bucketId = "test";
        String fileBucketId = "test-metadata";

        Path fileBucketStoragePath = LocalFileUtils.fileBuckedInputFilePath(
            inputTarStoragePath.toString(),
            fileBucketId
        );
        Files.createDirectories(fileBucketStoragePath);

        FileBucketTarCreator fileBucketTarCreator = new FileBucketTarCreator(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            writeOrderCreator,
            bucketId,
            fileBucketId,
            tarBufferingTimeoutInSeconds,
            TimeUnit.SECONDS,
            inputTarStoragePath.toString(),
            maxTarEntrySize,
            maxTarFileSize
        );

        doAnswer(args -> null).when(basicFileStorage).deleteFile(any(), any());

        fileBucketTarCreator.startListener();

        List<String> objectDigests = new ArrayList<>();
        List<String> storageIds = new ArrayList<>();

        for (ObjectToWrite objectToWrite : objectsToWrite) {
            Digest digest = new Digest(digestType);
            InputStream digestInputStream = digest.getDigestInputStream(objectToWrite.inputStream);

            String storageId = basicFileStorage.writeFile(
                objectToWrite.containerName,
                objectToWrite.objectName,
                digestInputStream,
                objectToWrite.size
            );

            String digestValue = digest.digestHex();

            storageIds.add(storageId);
            objectDigests.add(digestValue);

            fileBucketTarCreator.processMessage(
                new InputFileToProcessMessage(
                    objectToWrite.containerName,
                    objectToWrite.objectName,
                    storageId,
                    objectToWrite.size,
                    digestValue,
                    digestType.getName()
                )
            );

            Thread.sleep(1000L * objectToWrite.sleepDelayInSeconds);
        }

        // Then

        // Verify tar file creation & tar referential insertion
        assertThat(inputTarStoragePath.resolve(fileBucketId).toFile().list()).hasSize(
            expectedSealedTarCount + expectedTmpTarCount
        );

        ArgumentCaptor<TapeArchiveReferentialEntity> tarReferentialEntityArgumentCaptor = ArgumentCaptor.forClass(
            TapeArchiveReferentialEntity.class
        );
        verify(archiveReferentialRepository, times(expectedSealedTarCount + expectedTmpTarCount)).insert(
            tarReferentialEntityArgumentCaptor.capture()
        );
        verifyNoMoreInteractions(archiveReferentialRepository);
        List<TapeArchiveReferentialEntity> tarReferentialEntities = tarReferentialEntityArgumentCaptor.getAllValues();

        Map<String, Path> tarIdToTarFile = new HashMap<>();
        for (int i = 0; i < expectedSealedTarCount + expectedTmpTarCount; i++) {
            TapeArchiveReferentialEntity tarReferentialEntity = tarReferentialEntities.get(i);
            boolean shouldBeSealed = i < expectedSealedTarCount;

            Path tarFilePath = inputTarStoragePath
                .resolve(fileBucketId)
                .resolve(tarReferentialEntity.getArchiveId() + (shouldBeSealed ? "" : LocalFileUtils.TMP_EXTENSION));

            tarIdToTarFile.put(tarReferentialEntity.getArchiveId(), tarFilePath);

            assertThat(tarFilePath).exists();
            assertThat(tarReferentialEntity.getLocation()).isInstanceOf(
                TapeLibraryBuildingOnDiskArchiveStorageLocation.class
            );
            assertThat(tarReferentialEntity.getSize()).isNull();
            assertThat(tarReferentialEntity.getDigestValue()).isNull();
            assertThat(tarReferentialEntity.getLastUpdateDate()).isNotNull();
        }

        // Verify object referential update
        List<List<TarEntryDescription>> tarEntries = new ArrayList<>();
        for (int i = 0; i < objectsToWrite.size(); i++) {
            ObjectToWrite objectToWrite = objectsToWrite.get(i);

            ArgumentCaptor<TapeLibraryTarObjectStorageLocation> objectStorageLocationArgumentCaptor =
                ArgumentCaptor.forClass(TapeLibraryTarObjectStorageLocation.class);

            verify(objectReferentialRepository).updateStorageLocation(
                eq(objectToWrite.containerName),
                eq(objectToWrite.objectName),
                eq(storageIds.get(i)),
                objectStorageLocationArgumentCaptor.capture()
            );

            TapeLibraryTarObjectStorageLocation objectStorageLocation = objectStorageLocationArgumentCaptor.getValue();
            assertThat(objectStorageLocation.getTarEntries()).hasSize(objectsToWrite.get(i).expectedSegments.size());

            List<TarEntryDescription> objectTarEntries = new ArrayList<>();
            for (int entryIndex = 0; entryIndex < objectToWrite.expectedSegments.size(); entryIndex++) {
                TarEntryDescription tarEntry = objectStorageLocation.getTarEntries().get(entryIndex);

                assertThat(tarEntry.getSize()).isEqualTo(objectToWrite.expectedSegments.get(entryIndex).size);
                assertThat(tarEntry.getTarFileId()).isEqualTo(
                    tarReferentialEntities.get(objectToWrite.expectedSegments.get(entryIndex).tarIndex).getArchiveId()
                );
                assertThat(tarEntry.getEntryName()).isEqualTo(
                    objectToWrite.containerName + "/" + storageIds.get(i) + "-" + entryIndex
                );

                objectTarEntries.add(tarEntry);
            }
            tarEntries.add(objectTarEntries);
        }
        verifyNoMoreInteractions(objectReferentialRepository);

        // Verify segments digests
        for (int i = 0; i < tarEntries.size(); i++) {
            List<TarEntryDescription> fileTarEntries = tarEntries.get(i);
            Digest digest = new Digest(digestType);
            OutputStream digestOutputStream = digest.getDigestOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
            for (TarEntryDescription fileTarEntry : fileTarEntries) {
                Path tarFilePath = tarIdToTarFile.get(fileTarEntry.getTarFileId());

                // Verify segment digest
                checkEntryAtPos(tarFilePath, fileTarEntry);

                // Verify combined segments digest
                readEntryAtPos(tarFilePath, fileTarEntry, digestOutputStream);
            }
            assertThat(digest.digestHex()).isEqualTo(objectDigests.get(i));
        }

        // Verify that input files have been purged
        for (int i = 0; i < objectsToWrite.size(); i++) {
            verify(basicFileStorage).deleteFile(objectsToWrite.get(i).containerName, storageIds.get(i));
        }

        // Verify tar write order for sealed tar files
        ArgumentCaptor<WriteOrder> writeOrderArgCaptor = ArgumentCaptor.forClass(WriteOrder.class);
        verify(writeOrderCreator, times(expectedSealedTarCount)).addToQueue(writeOrderArgCaptor.capture());

        List<WriteOrder> allValues = writeOrderArgCaptor.getAllValues();
        for (int i = 0; i < allValues.size(); i++) {
            WriteOrder writeOrder = allValues.get(i);
            TapeArchiveReferentialEntity tarReferentialEntity = tarReferentialEntities.get(i);

            Path tarFilePath = tarIdToTarFile.get(tarReferentialEntity.getArchiveId());
            assertThat(writeOrder.getBucket()).isEqualTo(bucketId);
            assertThat(writeOrder.getFileBucketId()).isEqualTo(fileBucketId);
            assertThat(writeOrder.getDigest()).isEqualTo(
                new Digest(digestType).update(tarFilePath.toFile()).digestHex()
            );
            assertThat(writeOrder.getSize()).isEqualTo(tarFilePath.toFile().length());
            assertThat(writeOrder.getArchiveId()).isEqualTo(tarReferentialEntity.getArchiveId());
            assertThat(inputTarStoragePath.resolve(writeOrder.getFilePath()).toAbsolutePath()).isEqualTo(
                tarFilePath.toAbsolutePath()
            );
        }
        verifyNoMoreInteractions(writeOrderCreator);
    }

    @Test
    public void testReadTarAndCheckExistence() throws Exception {
        // Given

        // Populate
        String bucketId = "test";
        String fileBucketId = "test-metadata";

        Path fileBucketStoragePath = LocalFileUtils.fileBuckedInputFilePath(
            inputTarStoragePath.toString(),
            fileBucketId
        );
        Files.createDirectories(fileBucketStoragePath);

        FileBucketTarCreator fileBucketTarCreator = new FileBucketTarCreator(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            writeOrderCreator,
            bucketId,
            fileBucketId,
            60,
            TimeUnit.SECONDS,
            inputTarStoragePath.toString(),
            10_000,
            45_000
        );

        Digest digest = new Digest(DigestType.SHA512);
        String unitGuidStorageId1 = basicFileStorage.writeFile(
            "0_unit",
            "unitGuid1",
            digest.getDigestInputStream(new NullInputStream(60_000L)),
            60_000L
        );
        fileBucketTarCreator.processMessage(
            new InputFileToProcessMessage("0_unit", "obj1", unitGuidStorageId1, 60_000, digest.digestHex(), "SHA-512")
        );

        ArgumentCaptor<TapeArchiveReferentialEntity> tarReferentialEntityArgumentCaptor = ArgumentCaptor.forClass(
            TapeArchiveReferentialEntity.class
        );
        verify(archiveReferentialRepository, times(2)).insert(tarReferentialEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(archiveReferentialRepository);
        List<TapeArchiveReferentialEntity> tapeArchiveReferentialEntities =
            tarReferentialEntityArgumentCaptor.getAllValues();

        // Ensure 1 sealed tar (ready_on_disk) & 1 temp file (building_on_disk)
        String sealedTar1 = tapeArchiveReferentialEntities.get(0).getArchiveId();
        String tmpTarId2 = tapeArchiveReferentialEntities.get(1).getArchiveId();

        assertThat(inputTarStoragePath.resolve(fileBucketId).resolve(sealedTar1)).exists();
        assertThat(inputTarStoragePath.resolve(fileBucketId).resolve(sealedTar1 + ".tmp")).doesNotExist();

        assertThat(inputTarStoragePath.resolve(fileBucketId).resolve(tmpTarId2)).doesNotExist();
        assertThat(inputTarStoragePath.resolve(fileBucketId).resolve(tmpTarId2 + ".tmp")).exists();

        // When
        boolean tar1Exists = fileBucketTarCreator.containsTar(sealedTar1);
        boolean tar2Exists = fileBucketTarCreator.containsTar(tmpTarId2);
        boolean unknownTarExists = fileBucketTarCreator.containsTar("unknown");

        Optional<FileInputStream> tar1InputStream = fileBucketTarCreator.tryReadTar(sealedTar1);
        Optional<FileInputStream> tar2InputStream = fileBucketTarCreator.tryReadTar(tmpTarId2);
        Optional<FileInputStream> unknownTarInputStream = fileBucketTarCreator.tryReadTar("unknown");

        // Then
        assertThat(tar1Exists).isTrue();
        assertThat(tar2Exists).isTrue();
        assertThat(unknownTarExists).isFalse();

        assertThat(tar1InputStream).isPresent();
        assertThat(tar1InputStream.get()).hasSameContentAs(
            new FileInputStream(inputTarStoragePath.resolve(fileBucketId).resolve(sealedTar1).toFile())
        );

        assertThat(tar2InputStream).isPresent();
        assertThat(tar2InputStream.get()).hasSameContentAs(
            new FileInputStream(inputTarStoragePath.resolve(fileBucketId).resolve(tmpTarId2 + ".tmp").toFile())
        );

        assertThat(unknownTarInputStream).isEmpty();
    }

    private static class ObjectToWrite {

        final String objectName;
        final String containerName;
        final InputStream inputStream;
        final long size;
        final List<SegmentInfo> expectedSegments;
        final int sleepDelayInSeconds;

        ObjectToWrite(
            String objectName,
            String containerName,
            InputStream inputStream,
            long size,
            List<SegmentInfo> expectedSegmentSizes,
            int sleepDelayInSeconds
        ) {
            this.objectName = objectName;
            this.containerName = containerName;
            this.inputStream = inputStream;
            this.size = size;
            this.expectedSegments = expectedSegmentSizes;
            this.sleepDelayInSeconds = sleepDelayInSeconds;
        }
    }

    private static class SegmentInfo {

        long size;
        int tarIndex;

        SegmentInfo(long size, int tarIndex) {
            this.size = size;
            this.tarIndex = tarIndex;
        }
    }
}
