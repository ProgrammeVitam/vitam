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

import com.mongodb.MongoCursorNotFoundException;
import com.mongodb.ServerAddress;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.collection.EmptyCloseableIterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageUnavailableDataFromAsyncOfferException;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private AccessRequestManager accessRequestManager;

    @Mock
    private FileBucketTarCreatorManager fileBucketTarCreatorManager;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    @Mock
    private ArchiveCacheEvictionController archiveCacheEvictionController;

    @Mock
    private BucketTopologyHelper bucketTopologyHelper;

    private BasicFileStorage basicFileStorage;

    private TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage;

    @Before
    public void initialize() throws Exception {
        basicFileStorage = spy(new BasicFileStorage(temporaryFolder.newFolder("inputFiles").getAbsolutePath()));

        tapeLibraryContentAddressableStorage = new TapeLibraryContentAddressableStorage(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestManager,
            fileBucketTarCreatorManager,
            archiveCacheStorage,
            archiveCacheEvictionController,
            bucketTopologyHelper
        );
    }

    @After
    public void cleanup() {
        verifyNoMoreInteractions(bucketTopologyHelper);
    }

    @Test
    public void createContainer() {
        // Given

        // When
        tapeLibraryContentAddressableStorage.createContainer("container");

        // Then
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            bucketTopologyHelper,
            archiveReferentialRepository,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController
        );
    }

    @Test
    public void isExistingContainer() {
        // Given

        // When
        boolean existingContainer = tapeLibraryContentAddressableStorage.isExistingContainer("container");

        // Then
        assertThat(existingContainer).isTrue();
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            bucketTopologyHelper,
            archiveReferentialRepository,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController
        );
    }

    @Test
    public void putObject() throws Exception {
        // Given
        AtomicReference<String> storageId = new AtomicReference<>();
        doAnswer(args -> {
            String str = (String) args.callRealMethod();
            storageId.set(str);
            return str;
        })
            .when(basicFileStorage)
            .writeFile(any(), any(), any(), anyLong());

        byte[] data = "test data".getBytes();

        // When
        tapeLibraryContentAddressableStorage.writeObject(
            "containerName",
            "objectName",
            new ByteArrayInputStream(data),
            DigestType.SHA512,
            data.length
        );

        // Then
        verify(basicFileStorage).writeFile(eq("containerName"), eq("objectName"), any(), eq((long) data.length));

        String dataDigest = new Digest(DigestType.SHA512).update(data).digestHex();

        ArgumentCaptor<TapeObjectReferentialEntity> objectReferentialEntityArgumentCaptor = ArgumentCaptor.forClass(
            TapeObjectReferentialEntity.class
        );

        verify(objectReferentialRepository).insertOrUpdate(objectReferentialEntityArgumentCaptor.capture());
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getId().getContainerName()).isEqualTo(
            "containerName"
        );
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getId().getObjectName()).isEqualTo("objectName");
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getSize()).isEqualTo(data.length);
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getDigestType()).isEqualTo("SHA-512");
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getDigest()).isEqualTo(dataDigest);
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getLastUpdateDate()).isNotNull();
        assertThat(objectReferentialEntityArgumentCaptor.getValue().getLastObjectModifiedDate()).isNotNull();

        ArgumentCaptor<InputFileToProcessMessage> inputFileToProcessMessageArgumentCaptor = ArgumentCaptor.forClass(
            InputFileToProcessMessage.class
        );
        verify(fileBucketTarCreatorManager).addToQueue(inputFileToProcessMessageArgumentCaptor.capture());

        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getSize()).isEqualTo(data.length);
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getDigestValue()).isEqualTo(dataDigest);
        assertThat(inputFileToProcessMessageArgumentCaptor.getValue().getDigestAlgorithm()).isEqualTo("SHA-512");

        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );

        assertThat(basicFileStorage.readFile("containerName", storageId.get())).hasSameContentAs(
            new ByteArrayInputStream(data)
        );
    }

    @Test
    public void deleteExistingObject() throws Exception {
        // Given
        doReturn(true).when(objectReferentialRepository).delete(any());

        // When
        tapeLibraryContentAddressableStorage.deleteObject("containerName", "objectName");

        // Then
        ArgumentCaptor<TapeLibraryObjectReferentialId> idArgumentCaptor = ArgumentCaptor.forClass(
            TapeLibraryObjectReferentialId.class
        );
        verify(objectReferentialRepository).delete(idArgumentCaptor.capture());
        assertThat(idArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(idArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");

        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void deleteNonExistingObject() throws Exception {
        // Given
        doReturn(false).when(objectReferentialRepository).delete(any());

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.deleteObject("containerName", "objectName")
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        ArgumentCaptor<TapeLibraryObjectReferentialId> idArgumentCaptor = ArgumentCaptor.forClass(
            TapeLibraryObjectReferentialId.class
        );
        verify(objectReferentialRepository).delete(idArgumentCaptor.capture());
        assertThat(idArgumentCaptor.getValue().getContainerName()).isEqualTo("containerName");
        assertThat(idArgumentCaptor.getValue().getObjectName()).isEqualTo("objectName");

        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void isExistingObjectNonExistingObject() throws Exception {
        // Given
        doReturn(Optional.empty()).when(objectReferentialRepository).find(any(), anyString());

        // When
        boolean existingObject = tapeLibraryContentAddressableStorage.isExistingObject("containerName", "objectName");

        // Then
        assertThat(existingObject).isFalse();

        verify(objectReferentialRepository).find("containerName", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void isExistingObjectExistingObject() throws Exception {
        // Given
        doReturn(Optional.of(mock(TapeObjectReferentialEntity.class)))
            .when(objectReferentialRepository)
            .find(any(), anyString());

        // When
        boolean existingObject = tapeLibraryContentAddressableStorage.isExistingObject("containerName", "objectName");

        // Then
        assertThat(existingObject).isTrue();

        verify(objectReferentialRepository).find("containerName", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void getObjectDigestNonExistingObject() throws Exception {
        // Given
        doReturn(Optional.empty()).when(objectReferentialRepository).find(any(), anyString());

        // When / Then
        assertThatThrownBy(
            () ->
                tapeLibraryContentAddressableStorage.getObjectDigest(
                    "containerName",
                    "objectName",
                    DigestType.SHA512,
                    true
                )
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        verify(objectReferentialRepository).find("containerName", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void getObjectDigestExistingObject() throws Exception {
        // Given
        doReturn(
            Optional.of(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("containerName", "objectName"),
                    0L,
                    "SHA-512",
                    "digest",
                    "storageId",
                    null,
                    null,
                    null
                )
            )
        )
            .when(objectReferentialRepository)
            .find(any(), anyString());

        // When
        String digest = tapeLibraryContentAddressableStorage.getObjectDigest(
            "containerName",
            "objectName",
            DigestType.SHA512,
            true
        );

        // Then
        assertThat(digest).isEqualTo("digest");
        verify(objectReferentialRepository).find("containerName", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void getContainerInformation() {
        // Given

        // When
        ContainerInformation containerInformation = tapeLibraryContentAddressableStorage.getContainerInformation(
            "container"
        );

        // Then
        assertThat(containerInformation.getUsableSpace()).isEqualTo(-1);
    }

    @Test
    public void getObjectMetadataNonExistingObject() throws Exception {
        // Given
        doReturn(Optional.empty()).when(objectReferentialRepository).find(any(), anyString());

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.getObjectMetadata("0_unit", "objectName", true)
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        verify(objectReferentialRepository).find("0_unit", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void getObjectMetadataExistingObject() throws Exception {
        // Given
        doReturn(
            Optional.of(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId("containerName", "objectName"),
                    20L,
                    "SHA-512",
                    "digest",
                    "storageId",
                    null,
                    "date1",
                    "date2"
                )
            )
        )
            .when(objectReferentialRepository)
            .find(any(), anyString());

        // When
        MetadatasObject objectMetadata = tapeLibraryContentAddressableStorage.getObjectMetadata(
            "0_unit",
            "objectName",
            true
        );

        // Then

        assertThat(objectMetadata.getType()).isEqualTo("unit");
        assertThat(objectMetadata.getDigest()).isEqualTo("digest");
        assertThat(objectMetadata.getFileSize()).isEqualTo(20L);
        assertThat(objectMetadata.getObjectName()).isEqualTo("objectName");
        assertThat(objectMetadata.getLastModifiedDate()).isEqualTo("date1");

        verify(objectReferentialRepository).find("0_unit", "objectName");
        verifyNoMoreInteractions(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            fileBucketTarCreatorManager
        );
    }

    @Test
    public void listContainerWithEmptyContainer()
        throws ObjectReferentialException, ContentAddressableStorageServerException, IOException {
        // Given
        CloseableIterator<ObjectEntry> closeableIterator = spy(new EmptyCloseableIterator<>());
        doReturn(closeableIterator).when(objectReferentialRepository).listContainerObjectEntries("container");

        ArrayList<ObjectEntry> entries = new ArrayList<>();
        ObjectListingListener objectListingListener = entries::add;

        // When
        tapeLibraryContentAddressableStorage.listContainer("container", objectListingListener);

        // Then
        assertThat(entries).isEmpty();
        verify(closeableIterator).close();
    }

    @Test
    public void listContainerWithNonEmptyContainer()
        throws ObjectReferentialException, ContentAddressableStorageServerException, IOException {
        // Given
        List<ObjectEntry> objectEntries = List.of(new ObjectEntry("obj1", 100L), new ObjectEntry("obj2", 200L));
        CloseableIterator<ObjectEntry> closeableIterator = spy(
            CloseableIteratorUtils.toCloseableIterator(objectEntries)
        );
        doReturn(closeableIterator).when(objectReferentialRepository).listContainerObjectEntries("container");

        ArrayList<ObjectEntry> entries = new ArrayList<>();
        ObjectListingListener objectListingListener = entries::add;

        // When
        tapeLibraryContentAddressableStorage.listContainer("container", objectListingListener);

        // Then
        assertThat(entries).isEqualTo(objectEntries);
        verify(closeableIterator).close();
    }

    @Test
    public void listContainerWithMongoExceptionThenIteratorClosedAndExceptionThrown()
        throws ObjectReferentialException {
        // Given
        CloseableIterator<ObjectEntry> closeableIterator = mock(CloseableIterator.class);
        when(closeableIterator.hasNext()).thenReturn(true).thenReturn(true).thenThrow(IllegalStateException.class);
        ObjectEntry objectEntry1 = new ObjectEntry("obj1", 100L);
        when(closeableIterator.next())
            .thenReturn(objectEntry1)
            .thenThrow(new MongoCursorNotFoundException(124L, new ServerAddress()));

        doReturn(closeableIterator).when(objectReferentialRepository).listContainerObjectEntries("container");

        ArrayList<ObjectEntry> entries = new ArrayList<>();
        ObjectListingListener objectListingListener = entries::add;

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.listContainer("container", objectListingListener))
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasCauseInstanceOf(MongoCursorNotFoundException.class);

        assertThat(entries).containsExactly(objectEntry1);
        verify(closeableIterator).close();
    }

    @Test
    public void listContainerWithObjectReferentialExceptionThenExceptionThrown() throws ObjectReferentialException {
        // Given
        doThrow(new ObjectReferentialException("error"))
            .when(objectReferentialRepository)
            .listContainerObjectEntries("container");

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        // When / Then
        assertThatThrownBy(() -> tapeLibraryContentAddressableStorage.listContainer("container", objectListingListener))
            .isInstanceOf(ContentAddressableStorageServerException.class)
            .hasCauseInstanceOf(ObjectReferentialException.class);
    }

    @Test
    public void listContainerWithIOExceptionWhileWritingEntriesThenIteratorClosedAndExceptionThrown()
        throws ObjectReferentialException, IOException {
        // Given
        ObjectEntry objectEntry1 = new ObjectEntry("obj1", 100L);
        ObjectEntry objectEntry2 = new ObjectEntry("obj2", 200L);
        List<ObjectEntry> objectEntries = List.of(objectEntry1, objectEntry2);
        CloseableIterator<ObjectEntry> closeableIterator = spy(
            CloseableIteratorUtils.toCloseableIterator(objectEntries)
        );
        doReturn(closeableIterator).when(objectReferentialRepository).listContainerObjectEntries("container");

        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);
        doNothing().when(objectListingListener).handleObjectEntry(objectEntry1);
        doThrow(new IOException("error")).when(objectListingListener).handleObjectEntry(objectEntry2);

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.listContainer("container", objectListingListener)
        ).isInstanceOf(IOException.class);

        verify(objectListingListener).handleObjectEntry(objectEntry1);
        verify(objectListingListener).handleObjectEntry(objectEntry2);
        verify(closeableIterator).close();
    }

    @Test
    public void createAccessRequest() throws Exception {
        // Given
        doReturn("myAccessRequestId")
            .when(accessRequestManager)
            .createAccessRequest("0_object", List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"));

        // When
        String accessRequestId = tapeLibraryContentAddressableStorage.createAccessRequest(
            "0_object",
            List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")
        );

        // Then
        verify(accessRequestManager).createAccessRequest("0_object", List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"));
        assertThat(accessRequestId).isEqualTo("myAccessRequestId");
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void checkAccessRequestStatuses() throws Exception {
        // Given
        Map<String, AccessRequestStatus> accessRequestStatuses = Map.of(
            "accessRequestId1",
            AccessRequestStatus.NOT_READY,
            "accessRequestId2",
            AccessRequestStatus.READY
        );
        doReturn(accessRequestStatuses)
            .when(accessRequestManager)
            .checkAccessRequestStatuses(List.of("accessRequestId1", "accessRequestId2"), true);

        // When
        Map<String, AccessRequestStatus> result = tapeLibraryContentAddressableStorage.checkAccessRequestStatuses(
            List.of("accessRequestId1", "accessRequestId2"),
            true
        );

        // Then
        assertThat(result).isEqualTo(accessRequestStatuses);
        verify(accessRequestManager).checkAccessRequestStatuses(List.of("accessRequestId1", "accessRequestId2"), true);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void cancelAccessRequest() throws Exception {
        // Given

        // When
        tapeLibraryContentAddressableStorage.removeAccessRequest("accessRequestId", true);

        // Then
        verify(accessRequestManager).removeAccessRequest("accessRequestId", true);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void checkObjectAvailability() throws Exception {
        // Given
        doReturn(true)
            .when(accessRequestManager)
            .checkObjectAvailability("0_object", List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"));

        // When
        boolean result = tapeLibraryContentAddressableStorage.checkObjectAvailability(
            "0_object",
            List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")
        );

        // Then
        verify(accessRequestManager).checkObjectAvailability(
            "0_object",
            List.of("aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")
        );
        assertThat(result).isTrue();
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void getObjectWith1SegmentInCachedTarOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190625115513038-406fceff-2c4f-475c-898f-493331756eda.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq-0",
                    1024,
                    fileSize,
                    "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager, never()).containsTar("test-objects", tarId);
        verify(archiveCacheStorage, never()).containsArchive("test-objects", tarId);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith1SegmentWhileTarIsBeingMovedToCache() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190625115513038-406fceff-2c4f-475c-898f-493331756eda.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq-0",
                    1024,
                    fileSize,
                    "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        // TAR is reported as "on_tape" in database, but still is available in fileBucketTarCreatorManager (not moved to cache)
        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId).toFile())))
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager, never()).containsTar("test-objects", tarId);
        verify(archiveCacheStorage, never()).containsArchive("test-objects", tarId);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        verify(archiveCacheStorage, never()).tryReadArchive("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith2SegmentsInSameCachedTarOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190702131434269-84970e20-402d-4a88-b1df-ae05281ec7e6.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    2048,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    3072,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        verifyNoMoreInteractions(lockHandle);

        assertThat(response.getInputStream()).hasSameContentAs(new ByteArrayInputStream("test 2".getBytes()));

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId);
        verify(archiveCacheStorage, times(2)).tryReadArchive("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith2SegmentsInSameCachedTarWhileTarIsBeingMovedToCache() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190702131434269-84970e20-402d-4a88-b1df-ae05281ec7e6.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    2048,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    3072,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        // On first invocation (second segment), tar has still not be moved to cache, on second invocation (second segment) tar is in the cache
        when(fileBucketTarCreatorManager.containsTar("test-objects", tarId)).thenReturn(false);
        when(archiveCacheStorage.containsArchive("test-objects", tarId)).thenReturn(true);

        when(fileBucketTarCreatorManager.tryReadTar("test-objects", tarId))
            .thenAnswer(
                args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId).toFile()))
            )
            .thenReturn(Optional.empty());

        when(archiveCacheStorage.tryReadArchive("test-objects", tarId)).thenAnswer(
            args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId).toFile()))
        );

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        verifyNoMoreInteractions(lockHandle);

        assertThat(response.getInputStream()).hasSameContentAs(new ByteArrayInputStream("test 2".getBytes()));

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId);
        verify(archiveCacheStorage, times(1)).tryReadArchive("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith2SegmentsInMultipleCachedTarsOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId1 = "20211020221332998-37386fe7-2b05-492d-81f5-8c6b6ab6aa81.tar";
        String tarId2 = "20211020221632650-d1f1a746-b1e5-4d65-bc35-8a92ae22c3f2.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId1,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    0,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId2,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    0,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tar1ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId1,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 249),
            2048L,
            "c658ba37a44f5e42b4d13cde06c51911f2e8b8afd0768ba18c6be1a831a4c008c04d01a50809683735fe09190df729d7f0f996cd090a26b3d418693a982103e9",
            null
        );

        TapeArchiveReferentialEntity tar2ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId2,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 250),
            2048L,
            "0dccee7a7d82c0c3ace856e9feaf1656b86d555830e3825c656d2cf63b893865444767d1ce1fbec81c5795bd7d5bf29b6ce191562ebad251705898c0a803a818",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId1).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId1);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId1);
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId2).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId2);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId2);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId1, tarId2))).thenReturn(
            List.of(tar1ReferentialEntity, tar2ReferentialEntity)
        );

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(
                Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
            );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(
            Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
        );
        verifyNoMoreInteractions(archiveCacheEvictionController);

        verifyNoMoreInteractions(lockHandle);

        assertThat(response.getInputStream()).hasSameContentAs(new ByteArrayInputStream("test 2".getBytes()));

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId2);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId2);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithInvalidTarEntryDigestThenKO() throws Exception {
        // Given
        int fileSize = 6;
        String tarId1 = "20211020221332998-37386fe7-2b05-492d-81f5-8c6b6ab6aa81.tar";
        String tarId2 = "20211020221632650-d1f1a746-b1e5-4d65-bc35-8a92ae22c3f2.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId1,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    0,
                    3,
                    "2783c02c85e059159cee8a5f1118c2aabe578fd897a1035c293e9fb99c2997ff61b55270bc341c988208b866999985feb51b05fee172fc4623c32fcf07d9cc4f"
                ),
                new TarEntryDescription(
                    tarId2,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    0,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tar1ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId1,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 249),
            2048L,
            "c658ba37a44f5e42b4d13cde06c51911f2e8b8afd0768ba18c6be1a831a4c008c04d01a50809683735fe09190df729d7f0f996cd090a26b3d418693a982103e9",
            null
        );

        TapeArchiveReferentialEntity tar2ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId2,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 250),
            2048L,
            "0dccee7a7d82c0c3ace856e9feaf1656b86d555830e3825c656d2cf63b893865444767d1ce1fbec81c5795bd7d5bf29b6ce191562ebad251705898c0a803a818",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId1).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId1);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId1);
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId2).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId2);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId2);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId1, tarId2))).thenReturn(
            List.of(tar1ReferentialEntity, tar2ReferentialEntity)
        );

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(
                Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
            );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(
            Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
        );
        verifyNoMoreInteractions(archiveCacheEvictionController);
        verifyNoMoreInteractions(lockHandle);

        assertThatThrownBy(() -> IOUtils.toByteArray(response.getInputStream()))
            .isInstanceOf(IOException.class)
            .hasMessageContaining(
                "2783c02c85e059159cee8a5f1118c2aabe578fd897a1035c293e9fb99c2997ff61b55270bc341c988208b866999985feb51b05fee172fc4623c32fcf07d9cc4f"
            )
            .hasMessageContaining(
                "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
            );
        response.getInputStream().close();

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId2);
        verify(fileBucketTarCreatorManager, never()).tryReadTar("test-objects", tarId2);
        verify(archiveCacheStorage, never()).tryReadArchive("test-objects", tarId2);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithInvalidDigestThenKO() throws Exception {
        // Given
        int fileSize = 6;
        String tarId1 = "20211020221332998-37386fe7-2b05-492d-81f5-8c6b6ab6aa81.tar";
        String tarId2 = "20211020221632650-d1f1a746-b1e5-4d65-bc35-8a92ae22c3f2.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId1,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    0,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId2,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    0,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "ed072f56dc7eeb86968a2a03c70e77a13818b9de64160400ebca799ee4a4a459e520ad27f3f404d01161ea80db14cd6dd7f5e5af17027122cfa34cce552f04e3",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tar1ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId1,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 249),
            2048L,
            "c658ba37a44f5e42b4d13cde06c51911f2e8b8afd0768ba18c6be1a831a4c008c04d01a50809683735fe09190df729d7f0f996cd090a26b3d418693a982103e9",
            null
        );

        TapeArchiveReferentialEntity tar2ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId2,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 250),
            2048L,
            "0dccee7a7d82c0c3ace856e9feaf1656b86d555830e3825c656d2cf63b893865444767d1ce1fbec81c5795bd7d5bf29b6ce191562ebad251705898c0a803a818",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId1).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId1);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId1);
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId2).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId2);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId2);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId1, tarId2))).thenReturn(
            List.of(tar1ReferentialEntity, tar2ReferentialEntity)
        );

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(
                Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
            );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(
            Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
        );
        verifyNoMoreInteractions(archiveCacheEvictionController);
        verifyNoMoreInteractions(lockHandle);

        assertThatThrownBy(() -> IOUtils.toByteArray(response.getInputStream()))
            .isInstanceOf(IOException.class)
            .hasMessageContaining(
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b"
            )
            .hasMessageContaining(
                "ed072f56dc7eeb86968a2a03c70e77a13818b9de64160400ebca799ee4a4a459e520ad27f3f404d01161ea80db14cd6dd7f5e5af17027122cfa34cce552f04e3"
            );
        response.getInputStream().close();

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId2);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId2);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithInvalidSizeThenKO() throws Exception {
        // Given
        int fileSize = 7;
        String tarId1 = "20211020221332998-37386fe7-2b05-492d-81f5-8c6b6ab6aa81.tar";
        String tarId2 = "20211020221632650-d1f1a746-b1e5-4d65-bc35-8a92ae22c3f2.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId1,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    0,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId2,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    0,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tar1ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId1,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 249),
            2048L,
            "c658ba37a44f5e42b4d13cde06c51911f2e8b8afd0768ba18c6be1a831a4c008c04d01a50809683735fe09190df729d7f0f996cd090a26b3d418693a982103e9",
            null
        );

        TapeArchiveReferentialEntity tar2ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId2,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 250),
            2048L,
            "0dccee7a7d82c0c3ace856e9feaf1656b86d555830e3825c656d2cf63b893865444767d1ce1fbec81c5795bd7d5bf29b6ce191562ebad251705898c0a803a818",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId1).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId1);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId1);
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourcePath("tar/" + tarId2).toFile())))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId2);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId2);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId1, tarId2))).thenReturn(
            List.of(tar1ReferentialEntity, tar2ReferentialEntity)
        );

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(
                Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
            );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(
            Set.of(new ArchiveCacheEntry("test-objects", tarId1), new ArchiveCacheEntry("test-objects", tarId2))
        );
        verifyNoMoreInteractions(archiveCacheEvictionController);
        verifyNoMoreInteractions(lockHandle);

        assertThatThrownBy(() -> IOUtils.toByteArray(response.getInputStream()))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Broken stream. Premature EOF at position 6. Expected size = 7");
        response.getInputStream().close();

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId2);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId2);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getUnknownObjectThen404() throws Exception {
        // Given
        when(objectReferentialRepository.find("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")).thenReturn(
            Optional.empty()
        );

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.getObject("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")
        ).isInstanceOf(ContentAddressableStorageNotFoundException.class);

        verifyNoMoreInteractions(archiveCacheStorage, archiveCacheEvictionController, accessRequestManager);
    }

    @Test
    public void getObjectInInputFilesThenOK() throws Exception {
        // Given
        int fileSize = 6;
        byte[] data = "test 1".getBytes();
        String storageId = basicFileStorage.writeFile(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq",
            new ByteArrayInputStream(data),
            6L
        );

        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                storageId,
                new TapeLibraryInputFileObjectStorageLocation(),
                null,
                null
            )
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        verify(basicFileStorage).writeFile(any(), anyString(), any(), anyLong());
        verify(basicFileStorage).readFile("0_object", storageId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectInSingleTarBuildingOnDiskThenOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20211020160332409-2fefcb77-755c-4844-b623-b20dd9dd5438.tar.tmp";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        TarEntryDescription tarEntryDescription = new TarEntryDescription(
            tarId,
            "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
            1536L,
            6L,
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(tarEntryDescription)
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            5120L,
            "7d07f356d70aba2cefb7305fc78a7971b12ab5792ffc634e5e0ae84caf61793d5f821e5db68814e0e0937bfd911f3a77b2699303417904c0e624a43bb6d5a4a0",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        doAnswer(
            args ->
                Optional.of(
                    new FileInputStream(
                        PropertiesUtils.getResourceFile(
                            "tar/20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar"
                        )
                    )
                )
        )
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId);

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager, never()).containsTar("test-objects", tarId);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectInSingleTarReadyOnDiskThenOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        TarEntryDescription tarEntryDescription = new TarEntryDescription(
            tarId,
            "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
            1536L,
            6L,
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(tarEntryDescription)
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            5120L,
            "7d07f356d70aba2cefb7305fc78a7971b12ab5792ffc634e5e0ae84caf61793d5f821e5db68814e0e0937bfd911f3a77b2699303417904c0e624a43bb6d5a4a0",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        doAnswer(
            args ->
                Optional.of(
                    new FileInputStream(
                        PropertiesUtils.getResourceFile(
                            "tar/20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar"
                        )
                    )
                )
        )
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId);

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager, never()).containsTar("test-objects", tarId);
        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
    }

    @Test
    public void getObjectWithMultipleSegmentsInBuildingOnDiskAndReadyOnDiskThenOK() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190702131434269-84970e20-402d-4a88-b1df-ae05281ec7e6.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    2048,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    3072,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            5120L,
            "7d07f356d70aba2cefb7305fc78a7971b12ab5792ffc634e5e0ae84caf61793d5f821e5db68814e0e0937bfd911f3a77b2699303417904c0e624a43bb6d5a4a0",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourceFile("tar/" + tarId))))
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId);
        doReturn(true).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId);

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);

        verify(archiveCacheEvictionController).createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));
        verifyNoMoreInteractions(archiveCacheEvictionController);

        verifyNoMoreInteractions(lockHandle);

        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 2");
        response.getInputStream().close();

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithComplexMultipleSegmentsThenOK() throws Exception {
        // Given
        String objectName = "aeaaaaaaaaasicexadwraal4y27gi5qaaaaq";
        String storageId = "aeaaaaaaaaasicexadwraal4y27gi5qaaaaq-aeaaaaaaaaasicexadwraal4y27gjaqaaaaq";

        // 2 segments stored on tarId1 "on_tape" and existing in cache
        String tarId1 = "20211028115218834-84a8914a-c5bb-4c39-9e85-0971bf60a947.tar";
        TarEntryDescription tarEntryDescription1 = new TarEntryDescription(
            tarId1,
            "0_object/" + storageId + "-0",
            2560L,
            8L,
            "205f2ca6df4dc39a4c2471a9da68e93544c92f483d5f51dd876c2aa0f12200f1a5131b6f18e8e7884a663d243f15876783aec1cd4eafc21226e5f704b42ec123"
        );
        TarEntryDescription tarEntryDescription2 = new TarEntryDescription(
            tarId1,
            "0_object/" + storageId + "-1",
            3584L,
            8L,
            "4756deb2a9ea480f5f93b541a9720fcb60224d748e40867beb6b5237374ced43713aa7be4e33766410da64025dfa6250859962de88fe37b067eaf7d01c1ceeb8"
        );
        TapeArchiveReferentialEntity tar1ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId1,
            new TapeLibraryOnTapeArchiveStorageLocation("tape007", 123),
            5632L,
            "ed40c36ed8b37fd01723828f6bd327f0d5cf6ba93026f4e3a670534d1b421807e17bf77644aa9a86d560aaf23bd2941bb6b1c48e6b7df9ed0a2cbd9f36cfc199",
            null
        );
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId1);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourceFile("tar/" + tarId1))))
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId1);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        doReturn(true).when(archiveCacheStorage).containsArchive("test-objects", tarId1);

        // 2 segments stored on tarId2 "ready_on_disk"
        String tarId2 = "20211028115218906-1e87409b-4c79-42f3-8bbb-001038bd39ae.tar";
        TarEntryDescription tarEntryDescription3 = new TarEntryDescription(
            tarId2,
            "0_object/" + storageId + "-2",
            0L,
            8L,
            "d77dfbf749645f18d6e2ea2538765fc9285f55b38ade1b4e8736308c2c66855fa6c0883b4e1421031082c773ab66fe826bfb5c7c6627a85f4b600289933c610a"
        );
        TarEntryDescription tarEntryDescription4 = new TarEntryDescription(
            tarId2,
            "0_object/" + storageId + "-3",
            1024L,
            8L,
            "b3b74169f6b31fa28ee1464cf6eae41d96a41c9bd12a0c2cece5acd293d2204ed194a5ccc4ba34ee4e6080f4a67807235d0b6f2921669b3e4885a39019823966"
        );
        TapeArchiveReferentialEntity tar2ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId2,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            3072L,
            "0b50f2aa4483856fb2d56dffa631a6bbe82bf13d1c355f29a244fafbd475fa2b48db650fb0b3d510cd15625a247e7e70892274a5b4ee17797d7106f7b0e935a7",
            null
        );
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId2);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourceFile("tar/" + tarId2))))
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId2);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        doReturn(true).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);

        // 2 segments stored on tarId3 "building_on_disk"
        String tarId3 = "20211028115218919-ab9153d1-58f2-4820-8025-b7b125662853.tar";
        TarEntryDescription tarEntryDescription5 = new TarEntryDescription(
            tarId3,
            "0_object/" + storageId + "-4",
            0L,
            8L,
            "82c7914177594f163c886824f67080d2218444cbfa21dea4d3a5778983d14dca0d66ac7380b6a2ac2627ea3f56661525ea52f3929659a674471b07d4995df398"
        );
        TarEntryDescription tarEntryDescription6 = new TarEntryDescription(
            tarId3,
            "0_object/" + storageId + "-5",
            1024L,
            8L,
            "b35a5763fc76acec49aa5c2b04b901f50294d4fdcf3f13494fe7ae684097a17b3486fcb2456261fc5bf58edfcffb9208d24c55328d309a7dfcfe0bb1fc24e7da"
        );
        TapeArchiveReferentialEntity tar3ReferentialEntity = new TapeArchiveReferentialEntity(
            tarId3,
            new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
            null,
            null,
            null
        );
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId3);
        doAnswer(args -> Optional.of(new FileInputStream(PropertiesUtils.getResourceFile("tar/" + tarId3 + ".tmp"))))
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId3);
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId3);
        doReturn(true).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId3);

        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId("0_object", objectName);
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(
                tarEntryDescription1,
                tarEntryDescription2,
                tarEntryDescription3,
                tarEntryDescription4,
                tarEntryDescription5,
                tarEntryDescription6
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                48,
                "SHA-512",
                "911538fa6b7359ef540f4cab543f592727c8049287f884b0189eb96b6764541c3f0f01b940edd050e4c302fcaf41bafbc4229a4bfaecc2b7920f8f75d5a07089",
                storageId,
                tarObjectStorageLocation,
                null,
                null
            )
        );

        when(objectReferentialRepository.find("0_object", objectName)).thenReturn(objectReferentialEntity);
        when(archiveReferentialRepository.bulkFind(Set.of(tarId1, tarId2, tarId3))).thenReturn(
            List.of(tar1ReferentialEntity, tar2ReferentialEntity, tar3ReferentialEntity)
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(
                Set.of(
                    new ArchiveCacheEntry("test-objects", tarId1),
                    new ArchiveCacheEntry("test-objects", tarId2),
                    new ArchiveCacheEntry("test-objects", tarId3)
                )
            );

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject("0_object", objectName);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(48);

        verify(archiveCacheEvictionController).createLock(
            Set.of(
                new ArchiveCacheEntry("test-objects", tarId1),
                new ArchiveCacheEntry("test-objects", tarId2),
                new ArchiveCacheEntry("test-objects", tarId3)
            )
        );
        verifyNoMoreInteractions(archiveCacheEvictionController);
        verifyNoMoreInteractions(lockHandle);

        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo(
            "segment1segment2segment3segment4segment5segment6"
        );
        response.getInputStream().close();

        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId1);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId1);
        verify(archiveCacheStorage, times(2)).tryReadArchive("test-objects", tarId1);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId2);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId2);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId2);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId3);
        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId3);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId3);
        verify(fileBucketTarCreatorManager, times(2)).tryReadTar("test-objects", tarId3);

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithObjectBeingConcurrentlyMovedFromInputFileToBuildingOnDiskThenEventuallyOK()
        throws Exception {
        // Given
        int fileSize = 6;
        String storageId = "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq";

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        // V1 : Object in "input_file" state
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntityV1InputFile = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                storageId,
                new TapeLibraryInputFileObjectStorageLocation(),
                null,
                null
            )
        );

        // V2 : Object in "tar" state
        String tarId = "20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar";
        TarEntryDescription tarEntryDescription = new TarEntryDescription(
            tarId,
            "0_object/" + storageId + "-0",
            1536L,
            6L,
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(tarEntryDescription)
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntityV2InTar = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                storageId,
                tarObjectStorageLocation,
                null,
                null
            )
        );

        doReturn(objectReferentialEntityV1InputFile, objectReferentialEntityV2InTar)
            .when(objectReferentialRepository)
            .find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");

        // TAR Building on disk
        TapeArchiveReferentialEntity tarReferentialEntityV1BuildingOnDisk = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
            null,
            null,
            null
        );
        TapeArchiveReferentialEntity tarReferentialEntityV2ReadyOnDisk = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            null,
            null,
            null
        );

        doReturn(List.of(tarReferentialEntityV1BuildingOnDisk), List.of(tarReferentialEntityV2ReadyOnDisk))
            .when(archiveReferentialRepository)
            .bulkFind(Set.of(tarId));

        doAnswer(
            args ->
                Optional.of(
                    new FileInputStream(
                        PropertiesUtils.getResourceFile(
                            "tar/20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar"
                        )
                    )
                )
        )
            .when(fileBucketTarCreatorManager)
            .tryReadTar("test-objects", tarId);

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        InOrder inOrder = Mockito.inOrder(
            objectReferentialRepository,
            archiveReferentialRepository,
            basicFileStorage,
            fileBucketTarCreatorManager
        );
        inOrder.verify(objectReferentialRepository).find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        inOrder.verify(basicFileStorage).readFile("0_object", storageId);
        inOrder.verify(objectReferentialRepository).find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        inOrder.verify(archiveReferentialRepository).bulkFind(Set.of(tarId));
        inOrder.verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        inOrder.verifyNoMoreInteractions();

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWithObjectBeingConcurrentlyMovedFromBuildingOnDiskTarToReadyOnDiskTarThenEventuallyOK()
        throws Exception {
        // Given
        int fileSize = 6;
        String storageId = "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq";

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");

        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        String tarId = "20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar";
        TarEntryDescription tarEntryDescription = new TarEntryDescription(
            tarId,
            "0_object/" + storageId + "-0",
            1536L,
            6L,
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(tarEntryDescription)
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                storageId,
                tarObjectStorageLocation,
                null,
                null
            )
        );

        doReturn(objectReferentialEntity)
            .when(objectReferentialRepository)
            .find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");

        // V1 : TAR Ready on disk
        TapeArchiveReferentialEntity tarReferentialEntityV1ReadyOnDisk = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryReadyOnDiskArchiveStorageLocation(),
            5120L,
            "7d07f356d70aba2cefb7305fc78a7971b12ab5792ffc634e5e0ae84caf61793d5f821e5db68814e0e0937bfd911f3a77b2699303417904c0e624a43bb6d5a4a0",
            null
        );
        // V2 : TAR OnTape (+InCache)
        TapeArchiveReferentialEntity tarReferentialEntityV2OnTape = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("tape007", 1234),
            5120L,
            "7d07f356d70aba2cefb7305fc78a7971b12ab5792ffc634e5e0ae84caf61793d5f821e5db68814e0e0937bfd911f3a77b2699303417904c0e624a43bb6d5a4a0",
            null
        );

        doReturn(List.of(tarReferentialEntityV1ReadyOnDisk), List.of(tarReferentialEntityV2OnTape))
            .when(archiveReferentialRepository)
            .bulkFind(Set.of(tarId));

        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);

        doAnswer(
            args ->
                Optional.of(
                    new FileInputStream(
                        PropertiesUtils.getResourceFile(
                            "tar/20211020155647059-45fc6f9c-8af7-4d8a-945f-bb7aee6c9252.tar"
                        )
                    )
                )
        )
            .when(archiveCacheStorage)
            .tryReadArchive("test-objects", tarId);

        // When
        ObjectContent response = tapeLibraryContentAddressableStorage.getObject(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSize()).isEqualTo(fileSize);
        assertThat(IOUtils.toString(response.getInputStream(), StandardCharsets.UTF_8.name())).isEqualTo("test 1");

        InOrder inOrder = Mockito.inOrder(
            objectReferentialRepository,
            archiveReferentialRepository,
            archiveCacheStorage,
            fileBucketTarCreatorManager
        );
        inOrder.verify(objectReferentialRepository).find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq");
        inOrder.verify(archiveReferentialRepository).bulkFind(Set.of(tarId));
        inOrder.verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        inOrder.verify(archiveCacheStorage).tryReadArchive("test-objects", tarId);
        inOrder.verifyNoMoreInteractions();

        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith1SegmentInTarMissingFromCacheThenRejected() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190625115513038-406fceff-2c4f-475c-898f-493331756eda.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            List.of(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq-0",
                    1024,
                    fileSize,
                    "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1",
                "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq-aeaaaaaaaaecntv2ab5meallrz6w2eaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(Optional.empty()).when(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        doReturn(Optional.empty()).when(archiveCacheStorage).tryReadArchive("test-objects", tarId);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.getObject("0_object", "aeaaaaaaaaecntv2ab5tmallrz6wdwqaaaaq")
        ).isInstanceOf(ContentAddressableStorageUnavailableDataFromAsyncOfferException.class);

        verify(fileBucketTarCreatorManager).tryReadTar("test-objects", tarId);
        verify(archiveCacheStorage).tryReadArchive("test-objects", tarId);
        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }

    @Test
    public void getObjectWith2SegmentsInTarMissingFromCacheThenRejected() throws Exception {
        // Given
        int fileSize = 6;
        String tarId = "20190702131434269-84970e20-402d-4a88-b1df-ae05281ec7e6.tar";
        TapeLibraryObjectReferentialId objectReferentialId = new TapeLibraryObjectReferentialId(
            "0_object",
            "aeaaaaaaaafklihzablkmallwljiqoiaaaaq"
        );
        TapeLibraryTarObjectStorageLocation tarObjectStorageLocation = new TapeLibraryTarObjectStorageLocation(
            Arrays.asList(
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-0",
                    2048,
                    3,
                    "b551ea951724d66921f7e4991ee3b86e883921abf6a14552c73a4032cc87fa4900b2faa27d1cca5139d71a12937797cd29b589561fcc7fbb60dca460141afa65"
                ),
                new TarEntryDescription(
                    tarId,
                    "0_object/aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq-1",
                    3072,
                    3,
                    "2da4d0d9a4a1b2c0a27d10d6d7e92dd3e6db3b1b187e2419a044c21d5b20256cc8d87d438873837063d18ec7b6fe05a3050532611b21071ed3b736f09db905c4"
                )
            )
        );
        Optional<TapeObjectReferentialEntity> objectReferentialEntity = Optional.of(
            new TapeObjectReferentialEntity(
                objectReferentialId,
                fileSize,
                "SHA-512",
                "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
                "aeaaaaaaaafklihzablkmallwljiqoiaaaaq-aeaaaaaaaafklihzabqb2allwljjpiaaaaaq",
                tarObjectStorageLocation,
                null,
                null
            )
        );

        TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
            tarId,
            new TapeLibraryOnTapeArchiveStorageLocation("VIT002L6", 248),
            5120L,
            "60566c5d1821190fe9d1df5a7c112ff7b9ff3aec0fbcc6b9934cbebc3f9b33ef1c0aef1c1acd2291c8adb23e6cdcd36b34a2cf9fa564e9f686ea3baf5447e222",
            null
        );

        doReturn("test-objects").when(bucketTopologyHelper).getFileBucketFromContainerName("0_object");
        doReturn(false).when(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        doReturn(false).when(archiveCacheStorage).containsArchive("test-objects", tarId);

        when(objectReferentialRepository.find("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")).thenReturn(
            objectReferentialEntity
        );
        when(archiveReferentialRepository.bulkFind(Set.of(tarId))).thenReturn(List.of(tarReferentialEntity));

        LockHandle lockHandle = mock(LockHandle.class);
        doReturn(lockHandle)
            .when(archiveCacheEvictionController)
            .createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));

        // When / Then
        assertThatThrownBy(
            () -> tapeLibraryContentAddressableStorage.getObject("0_object", "aeaaaaaaaafklihzablkmallwljiqoiaaaaq")
        ).isInstanceOf(ContentAddressableStorageUnavailableDataFromAsyncOfferException.class);

        verify(archiveCacheEvictionController).createLock(Set.of(new ArchiveCacheEntry("test-objects", tarId)));
        verifyNoMoreInteractions(archiveCacheEvictionController);
        verify(lockHandle).release();
        verifyNoMoreInteractions(lockHandle);

        verify(fileBucketTarCreatorManager).containsTar("test-objects", tarId);
        verify(archiveCacheStorage).containsArchive("test-objects", tarId);
        verify(bucketTopologyHelper, atLeastOnce()).getFileBucketFromContainerName("0_object");

        verifyNoMoreInteractions(
            basicFileStorage,
            bucketTopologyHelper,
            fileBucketTarCreatorManager,
            archiveCacheEvictionController,
            archiveCacheStorage
        );
    }
}
