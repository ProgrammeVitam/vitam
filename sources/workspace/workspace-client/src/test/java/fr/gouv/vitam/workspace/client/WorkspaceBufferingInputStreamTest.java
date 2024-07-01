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
package fr.gouv.vitam.workspace.client;

import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;

import static fr.gouv.vitam.common.GlobalDataRest.X_CHUNK_LENGTH;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTENT_LENGTH;
import static org.apache.commons.io.IOUtils.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkspaceBufferingInputStreamTest {

    private static final String CONTAINER = "container";
    private static final String OBJECT = "object";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Before
    public void init() {
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
    }

    @Test
    public void testReadByte() throws Exception {
        verifyReadByte(0, 1_000, 4_500);
        verifyReadByte(1, 1_000, 4_500);
        verifyReadByte(100, 1_000, 4_500);
        verifyReadByte(1_000, 1_000, 4_500);
        verifyReadByte(10_000, 1_000, 4_500);
        verifyReadByte(100_000, 1_000, 4_500);
    }

    @Test
    public void testReadByteArray() throws Exception {
        verifyReadByteArray(0, 1_000, 4_500);
        verifyReadByteArray(1, 1_000, 4_500);
        verifyReadByteArray(100, 1_000, 4_500);
        verifyReadByteArray(1_000, 1_000, 4_500);
        verifyReadByteArray(10_000, 1_000, 4_500);
        verifyReadByteArray(100_000, 1_000, 4_500);
    }

    @Test
    public void testReadByteArrayWithOffset() throws Exception {
        verifyReadByteArrayWithOffset(0, 1_000, 4_500);
        verifyReadByteArrayWithOffset(1, 1_000, 4_500);
        verifyReadByteArrayWithOffset(100, 1_000, 4_500);
        verifyReadByteArrayWithOffset(1_000, 1_000, 4_500);
        verifyReadByteArrayWithOffset(10_000, 1_000, 4_500);
        verifyReadByteArrayWithOffset(100_000, 1_000, 4_500);
    }

    private void verifyReadByte(int size, int maxInMemoryBufferSize, int maxOnDiskBufferSize) throws Exception {
        // Given
        File testFile = createRandomFile(size);

        givenWorkspaceClientReturnsFileContent(testFile);

        // When
        InputStream inputStream;
        try (
            WorkspaceBufferingInputStream instance = new WorkspaceBufferingInputStream(
                workspaceClientFactory,
                "container",
                "object",
                maxOnDiskBufferSize,
                maxInMemoryBufferSize,
                tempFolder.getRoot()
            )
        ) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int read;
            while (EOF != (read = instance.read())) {
                byteArrayOutputStream.write(read);
            }
            inputStream = byteArrayOutputStream.toInputStream();
        }

        // Then
        verifyResult(size, maxOnDiskBufferSize, testFile, inputStream);
    }

    private void verifyReadByteArray(int size, int maxInMemoryBufferSize, int maxOnDiskBufferSize) throws Exception {
        // Given
        File testFile = createRandomFile(size);

        givenWorkspaceClientReturnsFileContent(testFile);

        // When
        InputStream inputStream;
        try (
            WorkspaceBufferingInputStream instance = new WorkspaceBufferingInputStream(
                workspaceClientFactory,
                "container",
                "object",
                maxOnDiskBufferSize,
                maxInMemoryBufferSize,
                tempFolder.getRoot()
            )
        ) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[50];
            int read;
            while (EOF != (read = instance.read(buffer))) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
            inputStream = byteArrayOutputStream.toInputStream();
        }

        // Then
        verifyResult(size, maxOnDiskBufferSize, testFile, inputStream);
    }

    private void verifyReadByteArrayWithOffset(int size, int maxInMemoryBufferSize, int maxOnDiskBufferSize)
        throws Exception {
        // Given
        File testFile = createRandomFile(size);

        givenWorkspaceClientReturnsFileContent(testFile);

        // When
        InputStream inputStream;
        try (
            WorkspaceBufferingInputStream instance = new WorkspaceBufferingInputStream(
                workspaceClientFactory,
                "container",
                "object",
                maxOnDiskBufferSize,
                maxInMemoryBufferSize,
                tempFolder.getRoot()
            )
        ) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[50];
            int read;
            while (EOF != (read = instance.read(buffer, 5, 40))) {
                byteArrayOutputStream.write(buffer, 5, read);
            }
            inputStream = byteArrayOutputStream.toInputStream();
        }

        // Then
        verifyResult(size, maxOnDiskBufferSize, testFile, inputStream);
    }

    private File createRandomFile(int size) throws IOException {
        File file = tempFolder.newFile(GUIDFactory.newGUID().toString() + ".tmp");
        FileUtils.copyToFile(new FakeInputStream(size), file);
        return file;
    }

    private void givenWorkspaceClientReturnsFileContent(File file)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        when(workspaceClient.getObject(CONTAINER, OBJECT)).thenAnswer(
            args -> Response.ok(Files.newInputStream(file.toPath())).status(Response.Status.OK).build()
        );
        when(workspaceClient.getObject(eq(CONTAINER), eq(OBJECT), anyLong(), anyLong())).thenAnswer(args -> {
            long startOffset = args.getArgument(2);
            Long maxSize = args.getArgument(3);
            long actualMaxSize = maxSize == null ? Long.MAX_VALUE : maxSize;

            BoundedInputStream inputStream = new BoundedInputStream(
                Channels.newInputStream(Files.newByteChannel(file.toPath()).position(startOffset)),
                actualMaxSize
            );

            long actualSize = Math.min(file.length() - startOffset, actualMaxSize);

            MultivaluedHashMap headers = new MultivaluedHashMap();
            headers.add(X_CHUNK_LENGTH, actualSize);
            headers.add(X_CONTENT_LENGTH, file.length());
            return new AbstractMockClient.FakeInboundResponse(
                Response.Status.OK,
                new BufferedInputStream(inputStream),
                null,
                headers
            );
        });
    }

    private void verifyResult(int size, int maxOnDiskBufferSize, File testFile, InputStream inputStream)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        assertThat(inputStream).hasSameContentAs(new FileInputStream(testFile));
        int expectedInvocations = size / maxOnDiskBufferSize + 1;
        verify(workspaceClient, times(expectedInvocations)).getObject(eq(CONTAINER), eq(OBJECT), anyLong(), anyLong());
        reset(workspaceClient);
        Files.delete(testFile.toPath());
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }
}
