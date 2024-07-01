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
package fr.gouv.vitam.common.stream;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import java.io.IOException;

import static org.apache.commons.io.IOUtils.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExactSizeInputStreamTest {

    @Test
    public void testValidEmptyStream_readByte() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(0L), 0L);

        // When / Then
        readBytes(exactSizeInputStream, 0);
    }

    @Test
    public void testValidEmptyStream_readByteArray() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(0L), 0L);

        // When / Then
        readByteArray(exactSizeInputStream, 10, 0);
    }

    @Test
    public void testInvalidEmptyStream_failFast() {
        assertThatThrownBy(() -> new ExactSizeInputStream(new NullInputStream(1L), 0L)).isInstanceOf(IOException.class);
    }

    @Test
    public void testValidNonEmptyStream_readByte() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 10L);

        // When / Then
        readBytes(exactSizeInputStream, 10);
    }

    @Test
    public void testValidNonEmptyStream_readByteArray() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 10L);

        // When / Then
        readByteArray(exactSizeInputStream, 3, 10);
    }

    @Test
    public void testTooLargeStream_readBytes() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 11L);

        // When / Then
        assertThatThrownBy(() -> readBytes(exactSizeInputStream, 10)).isInstanceOf(IOException.class);
    }

    @Test
    public void testTooLargeStream_readByteArray() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 9L);

        // When / Then
        assertThatThrownBy(() -> readByteArray(exactSizeInputStream, 3, 10)).isInstanceOf(IOException.class);
    }

    @Test
    public void testInvalidTooShortStream_readBytes() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 9L);

        // When / Then
        assertThatThrownBy(() -> readBytes(exactSizeInputStream, 10)).isInstanceOf(IOException.class);
    }

    @Test
    public void testInvalidTooShortStream_readByteArray() throws Exception {
        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 11L);

        // When / Then
        assertThatThrownBy(() -> readByteArray(exactSizeInputStream, 3, 10)).isInstanceOf(IOException.class);
    }

    private void readBytes(ExactSizeInputStream exactSizeInputStream, int expectedSize) throws IOException {
        for (int i = 0; i < expectedSize; i++) {
            assertThat(exactSizeInputStream.read()).isEqualTo(0);
        }
        assertThat(exactSizeInputStream.read()).isEqualTo(EOF);
    }

    private void readByteArray(ExactSizeInputStream exactSizeInputStream, int bufferSize, int expectedSize)
        throws IOException {
        byte[] bytes = new byte[bufferSize];
        int totalSize = 0;
        int readBytes;
        while (EOF != (readBytes = exactSizeInputStream.read(bytes))) {
            totalSize += readBytes;
        }
        assertThat(totalSize).isEqualTo(expectedSize);
    }
}
