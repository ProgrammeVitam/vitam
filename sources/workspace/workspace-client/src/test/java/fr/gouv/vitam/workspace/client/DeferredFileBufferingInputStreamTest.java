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

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import static org.apache.commons.io.IOUtils.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class DeferredFileBufferingInputStreamTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testReadByte() throws IOException {
        verifyReadByte(0, 1_000);
        verifyReadByte(1, 1_000);
        verifyReadByte(100, 1_000);
        verifyReadByte(1_000, 1_000);
        verifyReadByte(10_000, 1_000);
    }

    @Test
    public void testReadByteArray() throws IOException {
        verifyReadByteArray(0, 1_000);
        verifyReadByteArray(1, 1_000);
        verifyReadByteArray(100, 1_000);
        verifyReadByteArray(1_000, 1_000);
        verifyReadByteArray(10_000, 1_000);
    }

    @Test
    public void testReadByteArrayWithOffset() throws IOException {
        verifyReadByteArrayWithOffset(0, 1_000);
        verifyReadByteArrayWithOffset(1, 1_000);
        verifyReadByteArrayWithOffset(100, 1_000);
        verifyReadByteArrayWithOffset(1_000, 1_000);
        verifyReadByteArrayWithOffset(10_000, 1_000);
    }

    @Test
    public void testNoTempFileWhenEmpty() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(0);

        // When
        try (
            DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
                sourceInputStream,
                0,
                1000,
                tempFolder.getRoot()
            )
        ) {
            // Then
            assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
        }
    }

    @Test
    public void testNoTempFileWhenSmallSize() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(100);

        // When
        try (
            DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
                sourceInputStream,
                100,
                1000,
                tempFolder.getRoot()
            )
        ) {
            // Then
            assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
        }
    }

    @Test
    public void testNoTempFileWhenSizeEqualsMaxInMemorySize() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(1000);

        // When
        try (
            DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
                sourceInputStream,
                1000,
                1000,
                tempFolder.getRoot()
            )
        ) {
            // Then
            assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
        }
    }

    @Test
    public void testTempFileWhenBigSize() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(10000);

        // When
        try (
            DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
                sourceInputStream,
                10000,
                1000,
                tempFolder.getRoot()
            )
        ) {
            // Then
            assertThat(tempFolder.getRoot().list()).hasSize(1);
        }

        // Ensure file deleted when stream closed
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }

    @Test
    public void testIOExceptionWhenImMemoryBufferingWithSizeMismatches() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(100);

        // When
        assertThatThrownBy(
            () -> new DeferredFileBufferingInputStream(sourceInputStream, 123, 1000, tempFolder.getRoot())
        ).isInstanceOf(IOException.class);

        // Ensure any tmp file deleted
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }

    @Test
    public void testIOExceptionWhenInMemoryBufferingWithBrokenSource() throws IOException {
        // Given
        InputStream sourceInputStream = new SequenceInputStream(new NullInputStream(50), new BrokenInputStream());

        // When
        assertThatThrownBy(
            () -> new DeferredFileBufferingInputStream(sourceInputStream, 100, 1000, tempFolder.getRoot())
        ).isInstanceOf(IOException.class);
        // Ensure tmp file deleted
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }

    @Test
    public void testIOExceptionWhenOnDiskBufferingWithSizeMismatches() throws IOException {
        // Given
        InputStream sourceInputStream = new NullInputStream(10000);

        // When
        assertThatThrownBy(
            () -> new DeferredFileBufferingInputStream(sourceInputStream, 12345, 1000, tempFolder.getRoot())
        ).isInstanceOf(IOException.class);
        // Ensure tmp file deleted
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }

    @Test
    public void testIOExceptionWhenOnDiskBufferingWithBrokenSource() throws IOException {
        // Given
        InputStream brokenInputStream = mock(InputStream.class);
        doThrow(IOException.class).when(brokenInputStream).read();
        doThrow(IOException.class).when(brokenInputStream).read(any());
        doThrow(IOException.class).when(brokenInputStream).read(any(), anyInt(), anyInt());
        doThrow(IOException.class).when(brokenInputStream).close();

        InputStream sourceInputStream = new SequenceInputStream(new NullInputStream(100), brokenInputStream);

        // When
        assertThatThrownBy(
            () -> new DeferredFileBufferingInputStream(sourceInputStream, 10000, 1000, tempFolder.getRoot())
        ).isInstanceOf(IOException.class);
        // Ensure tmp file deleted
        assertThat(tempFolder.getRoot().list()).isNullOrEmpty();
    }

    private void verifyReadByte(int size, int maxInMemoryBufferSize) throws IOException {
        // Given
        InputStream sourceInputStream = new FakeInputStream(size);
        Digest digest = new Digest(DigestType.SHA512);
        InputStream digestInputStream = digest.getDigestInputStream(sourceInputStream);

        // When
        DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
            digestInputStream,
            size,
            maxInMemoryBufferSize,
            tempFolder.getRoot()
        );

        // Then
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read;
        while (EOF != (read = deferredFileBufferingInputStream.read())) {
            byteArrayOutputStream.write(read);
        }
        Digest digest2 = new Digest(DigestType.SHA512).update(byteArrayOutputStream.toInputStream());
        assertThat(digest2.digestHex()).isEqualTo(digest.digestHex());
    }

    private void verifyReadByteArray(int size, int maxInMemoryBufferSize) throws IOException {
        // Given
        InputStream sourceInputStream = new FakeInputStream(size);
        Digest digest = new Digest(DigestType.SHA512);
        InputStream digestInputStream = digest.getDigestInputStream(sourceInputStream);

        // When
        DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
            digestInputStream,
            size,
            maxInMemoryBufferSize,
            tempFolder.getRoot()
        );

        // Then
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[50];
        int read;
        while (EOF != (read = deferredFileBufferingInputStream.read(buffer))) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        Digest digest2 = new Digest(DigestType.SHA512).update(byteArrayOutputStream.toInputStream());
        assertThat(digest2.digestHex()).isEqualTo(digest.digestHex());
    }

    private void verifyReadByteArrayWithOffset(int size, int maxInMemoryBufferSize) throws IOException {
        // Given
        InputStream sourceInputStream = new FakeInputStream(size);
        Digest digest = new Digest(DigestType.SHA512);
        InputStream digestInputStream = digest.getDigestInputStream(sourceInputStream);

        // When
        DeferredFileBufferingInputStream deferredFileBufferingInputStream = new DeferredFileBufferingInputStream(
            digestInputStream,
            size,
            maxInMemoryBufferSize,
            tempFolder.getRoot()
        );

        // Then
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[50];
        int read;
        while (EOF != (read = deferredFileBufferingInputStream.read(buffer, 5, 40))) {
            byteArrayOutputStream.write(buffer, 5, read);
        }
        Digest digest2 = new Digest(DigestType.SHA512).update(byteArrayOutputStream.toInputStream());
        assertThat(digest2.digestHex()).isEqualTo(digest.digestHex());
    }
}
