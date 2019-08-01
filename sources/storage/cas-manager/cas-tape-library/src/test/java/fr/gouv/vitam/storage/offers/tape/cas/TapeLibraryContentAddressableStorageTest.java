package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TapeLibraryContentAddressableStorageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ObjectReferentialRepository objectReferentialRepository;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private ReadRequestReferentialRepository readRequestReferentialRepository;

    @Mock
    private FileBucketTarCreatorManager fileBucketTarCreatorManager;

    @Mock
    private QueueRepository readWriteQueueRepository;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy;

    private BasicFileStorage basicFileStorage;

    private TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage;

    private String outputTarsPath;

    @Before
    public void initialize() throws Exception {

        basicFileStorage = spy(new BasicFileStorage(temporaryFolder.newFolder("inputFiles").getAbsolutePath()));

        outputTarsPath = temporaryFolder.newFolder("outputTars").getAbsolutePath();
        tapeLibraryContentAddressableStorage =
            new TapeLibraryContentAddressableStorage(basicFileStorage, objectReferentialRepository,
                archiveReferentialRepository, readRequestReferentialRepository, fileBucketTarCreatorManager,
                readWriteQueueRepository, tapeCatalogService, outputTarsPath, archiveOutputRetentionPolicy);

    }

    @Test
    public void createContainer() {

        // Given

        // When
        tapeLibraryContentAddressableStorage.createContainer("container");

        // Then
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void isExistingContainer() {
        // Given

        // When
        boolean existingContainer = tapeLibraryContentAddressableStorage.isExistingContainer("container");

        // Then
        assertThat(existingContainer).isTrue();
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void putObject() throws Exception {

        // Given
        AtomicReference<String> storageId = new AtomicReference<>();
        doAnswer(
            (args) -> {
                String str = (String) args.callRealMethod();
                storageId.set(str);
                return str;
            }
        ).when(basicFileStorage).writeFile(any(), any(), any(), anyLong());

        byte[] data = "test data".getBytes();

        // When
        String digest = tapeLibraryContentAddressableStorage.putObject("containerName", "objectName",
            new ByteArrayInputStream(data), DigestType.SHA512, (long) data.length);

        // Then
        assertThat(digest).isEqualTo(new Digest(DigestType.SHA512).update(data).digestHex());

        verify(basicFileStorage).writeFile(eq("containerName"), eq("objectName"), any(), eq((long) data.length));

        ArgumentCaptor<TapeObjectReferentialEntity> objectReferentialEntityArgumentCaptor =
            ArgumentCaptor.forClass(TapeObjectReferentialEntity.class);

        verify(objectReferentialRepository).insertOrUpdate(objectReferentialEntityArgumentCaptor.capture());
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getId().getContainerName())
            .isEqualTo("containerName");
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getId().getObjectName()).isEqualTo("objectName");
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getSize()).isEqualTo(data.length);
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getDigestType()).isEqualTo("SHA-512");
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getDigest()).isEqualTo(digest);
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getLastUpdateDate()).isNotNull();
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getLastObjectModifiedDate()).isNotNull();

        ArgumentCaptor<InputFileToProcessMessage> inputFileToProcessMessageArgumentCaptor =
            ArgumentCaptor.forClass(InputFileToProcessMessage.class);
        verify(fileBucketTarCreatorManager).addToQueue(inputFileToProcessMessageArgumentCaptor.capture());

        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getSize()).isEqualTo(data.length);
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getDigestValue()).isEqualTo(digest);
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getDigestAlgorithm()).isEqualTo("SHA-512");

        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);

        assertThat(basicFileStorage.readFile("containerName", storageId.get())).hasSameContentAs(
            new ByteArrayInputStream(data));
    }

    @Test
    public void deleteExistingObject() throws Exception {

        // Given
        doReturn(true)
            .when(objectReferentialRepository).delete(any());

        // When
        tapeLibraryContentAddressableStorage.deleteObject("containerName", "objectName");

        // Then
        ArgumentCaptor<TapeLibraryObjectReferentialId> idArgumentCaptor =
            ArgumentCaptor.forClass(TapeLibraryObjectReferentialId.class);
        verify(objectReferentialRepository).delete(idArgumentCaptor.capture());
        assertThat(idArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(idArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");

        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void deleteNonExistingObject() throws Exception {

        // Given
        doReturn(false)
            .when(objectReferentialRepository).delete(any());

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.deleteObject("containerName", "objectName"))
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        ArgumentCaptor<TapeLibraryObjectReferentialId> idArgumentCaptor =
            ArgumentCaptor.forClass(TapeLibraryObjectReferentialId.class);
        verify(objectReferentialRepository).delete(idArgumentCaptor.capture());
        assertThat(idArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(idArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");

        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void isExistingObjectNonExistingObject() throws Exception {

        // Given
        doReturn(Optional.empty())
            .when(objectReferentialRepository).find(any(), any());

        // When
        boolean existingObject = tapeLibraryContentAddressableStorage.isExistingObject("containerName", "objectName");

        // Then
        assertThat(existingObject).isFalse();

        verify(objectReferentialRepository).find("containerName", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void isExistingObjectExistingObject() throws Exception {

        // Given
        doReturn(Optional.of(mock(TapeObjectReferentialEntity.class)))
            .when(objectReferentialRepository).find(any(), any());

        // When
        boolean existingObject = tapeLibraryContentAddressableStorage.isExistingObject("containerName", "objectName");

        // Then
        assertThat(existingObject).isTrue();

        verify(objectReferentialRepository).find("containerName", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void getObjectDigestNonExistingObject() throws Exception {

        // Given
        doReturn(Optional.empty())
            .when(objectReferentialRepository).find(any(), any());

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage
            .getObjectDigest("containerName", "objectName", DigestType.SHA512, true))
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        verify(objectReferentialRepository).find("containerName", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void getObjectDigestExistingObject() throws Exception {

        // Given
        doReturn(Optional.of(
            new TapeObjectReferentialEntity(new TapeLibraryObjectReferentialId("containerName", "objectName"), 0L,
                "SHA-512", "digest", "storageId", null, null, null))
        ).when(objectReferentialRepository).find(any(), any());

        // When
        String digest = tapeLibraryContentAddressableStorage
            .getObjectDigest("containerName", "objectName", DigestType.SHA512, true);

        // Then
        assertThat(digest).isEqualTo("digest");
        verify(objectReferentialRepository).find("containerName", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void getContainerInformation() {

        // Given

        // When
        ContainerInformation containerInformation =
            tapeLibraryContentAddressableStorage.getContainerInformation("container");

        // Then
        assertThat(containerInformation.getUsableSpace())
            .isEqualTo(-1);
    }

    @Test
    public void getObjectMetadataNonExistingObject() throws Exception {

        // Given
        doReturn(Optional.empty())
            .when(objectReferentialRepository).find(any(), any());

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage
            .getObjectMetadata("0_unit", "objectName", true))
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        verify(objectReferentialRepository).find("0_unit", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void getObjectMetadataExistingObject() throws Exception {

        // Given
        doReturn(Optional.of(
            new TapeObjectReferentialEntity(new TapeLibraryObjectReferentialId("containerName", "objectName"), 20L,
                "SHA-512", "digest", "storageId", null, "date1", "date2"))
        ).when(objectReferentialRepository).find(any(), any());

        // When
        MetadatasObject objectMetadata =
            tapeLibraryContentAddressableStorage.getObjectMetadata("0_unit", "objectName", true);

        // Then

        assertThat(objectMetadata.getType()).isEqualTo("unit");
        assertThat(objectMetadata.getDigest()).isEqualTo("digest");
        assertThat(objectMetadata.getFileSize()).isEqualTo(20L);
        assertThat(objectMetadata.getObjectName()).isEqualTo("objectName");
        assertThat(objectMetadata.getLastModifiedDate()).isEqualTo("date1");

        verify(objectReferentialRepository).find("0_unit", "objectName");
        Mockito.verifyNoMoreInteractions(basicFileStorage, objectReferentialRepository,
            archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);
    }

    @Test
    public void listContainer() {

        // Given

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.listContainer("container"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void listContainerNext() {

        // Given

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.listContainerNext("container", "maker"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void createReadOrderOK()
        throws ObjectReferentialException, IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, URISyntaxException, TapeCatalogException,
        ReadRequestReferentialException, ArchiveReferentialException {
        // Given
        String tapeCode = "VIT002L6";
        int fileSize = 6;
        String tarId = "20190625115513038-406fceff-2c4f-475c-898f-493331756eda.tar";
        TapeLibraryObjectReferentialId objectReferentialId =
            new TapeLibraryObjectReferentialId("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq-0",
                    1024, fileSize,
                    "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
                )));
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(new TapeObjectReferentialEntity(
            objectReferentialId, fileSize, "SHA-512",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq", tarObjectStorageLocation, null,
            null));

        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = Optional.of(
            new TapeArchiveReferentialEntity(tarId,
                new TapeLibraryOnTapeArchiveStorageLocation(tapeCode, 248), 5120L,
                "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
                null)
        );

        Files.copy(PropertiesUtils.getResourcePath("tar/" + tarId), Paths.get(outputTarsPath + "/" + tarId));

        // When / Then
        TapeCatalog tape = new TapeCatalog();
        tape.setCode(tapeCode);
        tape.setBucket("bucket");
        ArgumentCaptor<List<QueryCriteria>> captor = ArgumentCaptor.forClass(List.class);

        when(tapeCatalogService.find(captor.capture())).thenReturn(Arrays.asList(tape));
        when(objectReferentialRepository.find(anyString(), anyString())).thenReturn(objectReferentialEntity);
        when(archiveReferentialRepository.find(anyString())).thenReturn(tarReferentialEntity);

        String readOrderId = tapeLibraryContentAddressableStorage
            .createReadOrderRequest("0_object", Arrays.asList("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"));
        assertThat(readOrderId).isNotNull();

        TapeReadRequestReferentialEntity tapeReadRequestReferentialEntity =
            mock(TapeReadRequestReferentialEntity.class);
        when(tapeReadRequestReferentialEntity.isCompleted()).thenReturn(true);

        when(readRequestReferentialRepository.find(eq(readOrderId)))
            .thenReturn(Optional.of(tapeReadRequestReferentialEntity));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }

        assertThat(readRequestReferentialRepository.find(readOrderId).get().isCompleted()).isTrue();
    }

    @Test
    public void getObjectWith1SegmentsOK() throws ObjectReferentialException, IOException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ArchiveReferentialException {
        // Given
        int fileSize = 6;
        String tarId = "20190625115513038-406fceff-2c4f-475c-898f-493331756eda.tar";
        TapeLibraryObjectReferentialId objectReferentialId =
            new TapeLibraryObjectReferentialId("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq-0",
                    1024, fileSize,
                    "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
                )));
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(new TapeObjectReferentialEntity(
            objectReferentialId, fileSize, "SHA-512",
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq", tarObjectStorageLocation, null,
            null));

        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = Optional.of(
            new TapeArchiveReferentialEntity(tarId,
                new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248), 5120L,
                "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
                null)
        );

        Files.copy(PropertiesUtils.getResourcePath("tar/" + tarId), Paths.get(outputTarsPath + "/" + tarId));

        // When / Then
        when(objectReferentialRepository.find(anyString(), anyString())).thenReturn(objectReferentialEntity);
        when(archiveReferentialRepository.find(anyString())).thenReturn(tarReferentialEntity);

        ObjectContent response =
            tapeLibraryContentAddressableStorage.getObject("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq");
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");
    }

    @Test
    public void getObjectWith2SegmentsOK()
        throws ObjectReferentialException, IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, ArchiveReferentialException {
        // Given
        int fileSize = 6;
        String tarId = "20190702131434269-84970e20-402d-4a88-b1df-ae05281ec7e6.tar";
        TapeLibraryObjectReferentialId objectReferentialId =
            new TapeLibraryObjectReferentialId("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    2048, 3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    3072, 3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )));
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(new TapeObjectReferentialEntity(
            objectReferentialId, fileSize, "SHA-512",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq", tarObjectStorageLocation, null,
            null));

        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = Optional.of(
            new TapeArchiveReferentialEntity(tarId,
                new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248), 5120L,
                "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
                null)
        );

        Files.copy(PropertiesUtils.getResourcePath("tar/" + tarId), Paths.get(outputTarsPath + "/" + tarId));

        // When / Then
        when(objectReferentialRepository.find(anyString(), anyString())).thenReturn(objectReferentialEntity);
        when(archiveReferentialRepository.find(anyString())).thenReturn(tarReferentialEntity);

        ObjectContent response =
            tapeLibraryContentAddressableStorage.getObject("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 2");
    }
}
