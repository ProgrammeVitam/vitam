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
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
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

import java.io.IOException;
import java.nio.file.Files;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FileBucketTarCreatorBootstrapRecoveryTest {

    private static final String FILE_BUCKET_ID = "test-metadata";
    private static final String UNIT_CONTAINER = "0_unit";
    private static final String OBJECT_GROUP_CONTAINER = "0_objectGroup";
    private static final String FILE_1 = "file1";
    private static final String DIGEST_TYPE = DigestType.SHA512.getName();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    ObjectReferentialRepository objectReferentialRepository;

    @Mock
    FileBucketTarCreator fileBucketTarCreator;

    @Mock
    BucketTopologyHelper bucketTopologyHelper;

    private BasicFileStorage basicFileStorage;

    private FileBucketTarCreatorBootstrapRecovery fileBucketTarCreatorBootstrapRecovery;

    @Before
    public void initialize() throws IOException {
        String inputTarStorageFolder = temporaryFolder.newFolder("inputFiles").getAbsolutePath();
        Files.createDirectories(LocalFileUtils.fileBuckedInputFilePath(inputTarStorageFolder, FILE_BUCKET_ID));

        basicFileStorage = spy(new BasicFileStorage(inputTarStorageFolder));

        fileBucketTarCreatorBootstrapRecovery = new FileBucketTarCreatorBootstrapRecovery(
            basicFileStorage,
            objectReferentialRepository
        );

        doReturn(ImmutableSet.of(UNIT_CONTAINER, OBJECT_GROUP_CONTAINER))
            .when(bucketTopologyHelper)
            .listContainerNames(FILE_BUCKET_ID);
    }

    @Test
    public void initializeOnBootstrapWithEmptyWorkdirs() throws Exception {
        // Given (empty folder)

        // When
        fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
            FILE_BUCKET_ID,
            fileBucketTarCreator,
            bucketTopologyHelper
        );

        // Then
        verifyNoMoreInteractions(objectReferentialRepository);

        verifyNoMoreInteractions(fileBucketTarCreator);

        verify(basicFileStorage).listStorageIdsByContainerName(UNIT_CONTAINER);
        verify(basicFileStorage).listStorageIdsByContainerName(OBJECT_GROUP_CONTAINER);
        verifyNoMoreInteractions(basicFileStorage);
    }

    @Test
    public void initializeOnBootstrapWithNonExistingFile() throws Exception {
        // Given
        String storageId1 = this.basicFileStorage.writeFile(UNIT_CONTAINER, FILE_1, new NullInputStream(10), 10);
        doReturn(emptyList()).when(objectReferentialRepository).bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        // When
        fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
            FILE_BUCKET_ID,
            fileBucketTarCreator,
            bucketTopologyHelper
        );

        // Then
        verify(objectReferentialRepository).bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        verifyNoMoreInteractions(objectReferentialRepository);

        verifyNoMoreInteractions(fileBucketTarCreator);

        verify(basicFileStorage).writeFile(any(), any(), any(), anyLong());
        verify(basicFileStorage).listStorageIdsByContainerName(UNIT_CONTAINER);
        verify(basicFileStorage).listStorageIdsByContainerName(OBJECT_GROUP_CONTAINER);
        verify(basicFileStorage).deleteFile(UNIT_CONTAINER, storageId1);
        verifyNoMoreInteractions(basicFileStorage);
    }

    @Test
    public void initializeOnBootstrapWithInvalidFileStorageId() throws Exception {
        // Given
        String storageId1 = this.basicFileStorage.writeFile(UNIT_CONTAINER, FILE_1, new NullInputStream(10), 10);
        doReturn(
            singletonList(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(UNIT_CONTAINER, FILE_1),
                    10,
                    DIGEST_TYPE,
                    "digest",
                    "ANOTHER-STORAGE-ID",
                    null,
                    null,
                    null
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        // When
        fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
            FILE_BUCKET_ID,
            fileBucketTarCreator,
            bucketTopologyHelper
        );

        // Then
        verify(objectReferentialRepository).bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        verifyNoMoreInteractions(objectReferentialRepository);

        verifyNoMoreInteractions(fileBucketTarCreator);

        verify(basicFileStorage).writeFile(any(), any(), any(), anyLong());
        verify(basicFileStorage).listStorageIdsByContainerName(UNIT_CONTAINER);
        verify(basicFileStorage).listStorageIdsByContainerName(OBJECT_GROUP_CONTAINER);
        verify(basicFileStorage).deleteFile(UNIT_CONTAINER, storageId1);
        verifyNoMoreInteractions(basicFileStorage);
    }

    @Test
    public void initializeOnBootstrapWithAlreadyProceededFile() throws Exception {
        // Given
        String storageId1 = this.basicFileStorage.writeFile(UNIT_CONTAINER, FILE_1, new NullInputStream(10), 10);
        doReturn(
            singletonList(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(UNIT_CONTAINER, FILE_1),
                    10,
                    DIGEST_TYPE,
                    "digest",
                    storageId1,
                    new TapeLibraryTarObjectStorageLocation(null),
                    null,
                    null
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        // When
        fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
            FILE_BUCKET_ID,
            fileBucketTarCreator,
            bucketTopologyHelper
        );

        // Then
        verify(objectReferentialRepository).bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        verifyNoMoreInteractions(objectReferentialRepository);

        verifyNoMoreInteractions(fileBucketTarCreator);

        verify(basicFileStorage).writeFile(any(), any(), any(), anyLong());
        verify(basicFileStorage).listStorageIdsByContainerName(UNIT_CONTAINER);
        verify(basicFileStorage).listStorageIdsByContainerName(OBJECT_GROUP_CONTAINER);
        verify(basicFileStorage).deleteFile(UNIT_CONTAINER, storageId1);
        verifyNoMoreInteractions(basicFileStorage);
    }

    @Test
    public void initializeOnBootstrapRecoverFile() throws Exception {
        // Given
        String storageId1 = this.basicFileStorage.writeFile(UNIT_CONTAINER, FILE_1, new NullInputStream(10), 10);
        doReturn(
            singletonList(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(UNIT_CONTAINER, FILE_1),
                    10,
                    DIGEST_TYPE,
                    "digest",
                    storageId1,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    null,
                    null
                )
            )
        )
            .when(objectReferentialRepository)
            .bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));

        // When
        fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
            FILE_BUCKET_ID,
            fileBucketTarCreator,
            bucketTopologyHelper
        );

        // Then
        verify(objectReferentialRepository).bulkFind(UNIT_CONTAINER, ImmutableSet.of(FILE_1));
        verifyNoMoreInteractions(objectReferentialRepository);

        ArgumentCaptor<TarCreatorMessage> tarCreatorMessageArgCaptor = ArgumentCaptor.forClass(TarCreatorMessage.class);
        verify(fileBucketTarCreator).addToQueue(tarCreatorMessageArgCaptor.capture());
        InputFileToProcessMessage inputFileToProcessMessage =
            (InputFileToProcessMessage) tarCreatorMessageArgCaptor.getValue();
        assertThat(inputFileToProcessMessage.getContainerName()).isEqualTo(UNIT_CONTAINER);
        assertThat(inputFileToProcessMessage.getStorageId()).isEqualTo(storageId1);
        assertThat(inputFileToProcessMessage.getObjectName()).isEqualTo(FILE_1);
        assertThat(inputFileToProcessMessage.getDigestAlgorithm()).isEqualTo(DIGEST_TYPE);
        assertThat(inputFileToProcessMessage.getDigestValue()).isEqualTo("digest");
        assertThat(inputFileToProcessMessage.getSize()).isEqualTo(10L);

        verifyNoMoreInteractions(fileBucketTarCreator);

        verify(basicFileStorage).writeFile(any(), any(), any(), anyLong());
        verify(basicFileStorage).listStorageIdsByContainerName(UNIT_CONTAINER);
        verify(basicFileStorage).listStorageIdsByContainerName(OBJECT_GROUP_CONTAINER);
        verifyNoMoreInteractions(basicFileStorage);
    }
}
