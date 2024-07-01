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

import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
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
        assertThatThrownBy(() -> basicFileStorage.readFile(CONTAINER, storageId)).isInstanceOf(IOException.class);
    }

    @Test
    public void testFileListingEmpty() throws Exception {
        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());

        // When
        Stream<String> storageIds = basicFileStorage.listStorageIdsByContainerName(CONTAINER);

        // Then
        assertThat(IteratorUtils.toList(storageIds.iterator())).isEmpty();
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
        assertThat(IteratorUtils.toList(storageIds.iterator())).containsExactlyInAnyOrder(
            storageId1,
            storageId2,
            storageId3
        );
    }

    @Test
    public void testStorageIdToObjectName() throws Exception {
        // Given
        BasicFileStorage basicFileStorage = new BasicFileStorage(temporaryFolder.getRoot().getAbsolutePath());
        byte[] data = "data".getBytes();

        // When
        String storageId1 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId2 = basicFileStorage.writeFile(CONTAINER, OBJ_1, new ByteArrayInputStream(data), data.length);
        String storageId3 = basicFileStorage.writeFile(CONTAINER, OBJ_2, new ByteArrayInputStream(data), data.length);

        // Then
        assertThat(LocalFileUtils.storageIdToObjectName(storageId1)).isEqualTo(OBJ_1);
        assertThat(LocalFileUtils.storageIdToObjectName(storageId2)).isEqualTo(OBJ_1);
        assertThat(LocalFileUtils.storageIdToObjectName(storageId3)).isEqualTo(OBJ_2);
    }
}
