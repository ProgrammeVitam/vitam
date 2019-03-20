package fr.gouv.vitam.storage.offers.tape.cas;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BasicFileStorageTest {

    private static final String OBJ_1 = "obj1";
    private static final String OBJ_2 = "obj2";
    private static final String CONTAINER = "container";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testWriteFile() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();

        // When
        String storageId = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);

        // Then
        assertThat(new File(temporaryFolder.getRoot(), "/container/" + storageId)).hasBinaryContent(data);
    }

    @Test
    public void testMultipleWritesWithSameObjectName() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data1 = "data1".getBytes();
        byte[] data2 = "data2".getBytes();
        byte[] data3 = "data3".getBytes();

        // When
        String storageId1 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data1), data1.length);
        String storageId2 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data2), data2.length);
        String storageId3 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data3), data3.length);

        // Then
        assertThat(new File(temporaryFolder.getRoot(), "/container/" + storageId1)).hasBinaryContent(data1);
        assertThat(new File(temporaryFolder.getRoot(), "/container/" + storageId2)).hasBinaryContent(data2);
        assertThat(new File(temporaryFolder.getRoot(), "/container/" + storageId3)).hasBinaryContent(data3);
    }

    @Test
    public void testReadFile() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();
        String storageId = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);

        // When
        try (InputStream inputStream = basicFileStorage.readFile(CONTAINER, storageId)) {

            // Then
            assertThat(inputStream).hasSameContentAs(new ByteArrayInputStream(data));
        }
    }

    @Test
    public void testDeleteFile() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();
        String storageId = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);

        // When
        basicFileStorage.deleteFile(CONTAINER, storageId);

        // Then
        assertThatThrownBy(
            () -> basicFileStorage.readFile(CONTAINER, storageId)
        ).isInstanceOf(IOException.class);
    }


    @Test
    public void testFileListingEmpty() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());

        // When
        Stream<String> storageIds = basicFileStorage.listStorageIdsByContainerName(CONTAINER);

        // Then
        assertThat(IteratorUtils.toList(storageIds.iterator()))
            .isEmpty();
    }

    @Test
    public void testFileListing() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();
        String storageId1 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId2 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId3 = basicFileStorage.writeFile(CONTAINER, OBJ_2, new ByteArrayInputStream(data), data.length);

        // When
        Stream<String> storageIds = basicFileStorage.listStorageIdsByContainerName(CONTAINER);

        // Then
        assertThat(IteratorUtils.toList(storageIds.iterator()))
            .containsExactlyInAnyOrder(storageId1, storageId2, storageId3);

        basicFileStorage.deleteFile(CONTAINER, storageId1);
        basicFileStorage.deleteFile(CONTAINER, storageId2);
        basicFileStorage.deleteFile(CONTAINER, storageId3);
    }

    @Test
    public void testStorageIdToObjectName() throws Exception {

        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();
        String storageId1 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId2 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId3 = basicFileStorage.writeFile(CONTAINER, OBJ_2, new ByteArrayInputStream(data), data.length);

        // When / Then
        assertThat(BasicFileStorage.storageIdToObjectName(storageId1)).isEqualTo(OBJ_1);
        assertThat(BasicFileStorage.storageIdToObjectName(storageId2)).isEqualTo(OBJ_1);
        assertThat(BasicFileStorage.storageIdToObjectName(storageId3)).isEqualTo(OBJ_2);

        basicFileStorage.deleteFile(CONTAINER, storageId1);
        basicFileStorage.deleteFile(CONTAINER, storageId2);
        basicFileStorage.deleteFile(CONTAINER, storageId3);
    }
}
