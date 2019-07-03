package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
    private FileBucketTarCreatorManager fileBucketTarCreatorManager;

    @Mock
    private QueueRepository readWriteQueueRepository;

    @Mock
    private TapeCatalogService tapeCatalogService;

    private BasicFileStorage basicFileStorage;

    private TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage;

    @Before
    public void initialize() throws Exception {

        basicFileStorage = spy(new BasicFileStorage(temporaryFolder.newFolder("inputFiles").getAbsolutePath()));

        tapeLibraryContentAddressableStorage =
            new TapeLibraryContentAddressableStorage(basicFileStorage, objectReferentialRepository,
                archiveReferentialRepository, fileBucketTarCreatorManager, readWriteQueueRepository, tapeCatalogService);

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
    public void getObject() {
        // Given

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.getObject("containerName", "objectName"))
            .isInstanceOf(UnsupportedOperationException.class);
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
    public void asyncGetObject() {
        // FIXME / TODO
    }
}
