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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class MultiplexedStreamTest {

    @Test
    public void testEmpty() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When
        instance.appendEndOfFile();
        InputStream resultInputStream = byteArrayOutputStream.toInputStream();

        // Then
        verifyEntries(resultInputStream, 0);
    }

    @Test
    public void testSingleEntry() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When
        appendEntries(instance, 1);
        instance.appendEndOfFile();
        InputStream resultInputStream = byteArrayOutputStream.toInputStream();

        // Then
        verifyEntries(resultInputStream, 1);
    }

    @Test
    public void testMultipleEntries() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When
        appendEntries(instance, 100);
        instance.appendEndOfFile();
        InputStream resultInputStream = byteArrayOutputStream.toInputStream();

        // Then
        verifyEntries(resultInputStream, 100);
    }

    @Test
    public void testCorruptedStreamNoEOF() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When (append without EOF)
        appendEntries(instance, 100);

        // Then
        try (
            MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(
                byteArrayOutputStream.toInputStream()
            )
        ) {
            readEntries(multiplexedStreamReader, 100);

            // Check EOF
            assertThatThrownBy(multiplexedStreamReader::readNextEntry).isInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCorruptedStreamBeforeEOF() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When
        appendEntries(instance, 100);
        instance.appendEndOfFile();
        InputStream resultInputStream = new BoundedInputStream(
            byteArrayOutputStream.toInputStream(),
            byteArrayOutputStream.size() - 1
        );

        // Then
        try (MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(resultInputStream)) {
            readEntries(multiplexedStreamReader, 100);

            // Check EOF
            assertThatThrownBy(multiplexedStreamReader::readNextEntry).isInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCorruptedStreamIncompleteEntry() throws IOException {
        // Given
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter instance = new MultiplexedStreamWriter(byteArrayOutputStream);

        // When
        appendEntries(instance, 100);
        instance.appendEndOfFile();
        InputStream resultInputStream = new BoundedInputStream(
            byteArrayOutputStream.toInputStream(),
            byteArrayOutputStream.size() - 8 - 2
        );

        // Then
        try (MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(resultInputStream)) {
            readEntries(multiplexedStreamReader, 99);

            assertThatThrownBy(() -> IOUtils.toByteArray(multiplexedStreamReader.readNextEntry().get())).isInstanceOf(
                IOException.class
            );
        }
    }

    private void appendEntries(MultiplexedStreamWriter multiplexedStreamWriter, int nbEntries) throws IOException {
        for (int i = 0; i < nbEntries; i++) {
            byte[] data = getTestData(i);
            multiplexedStreamWriter.appendEntry(data.length, new ByteArrayInputStream(data));
        }
    }

    private void verifyEntries(InputStream inputStream, int expectedEntries) throws IOException {
        InputStream spyInputStream = spy(inputStream);
        try (MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(inputStream)) {
            // Check expected entries
            readEntries(multiplexedStreamReader, expectedEntries);

            // Verify no more entries
            assertThat(multiplexedStreamReader.readNextEntry()).isEmpty();

            // Check EOF
            assertThat(inputStream.read()).isEqualTo(IOUtils.EOF);
        }

        // Check close
        verify(spyInputStream, never()).close();
    }

    private void readEntries(MultiplexedStreamReader multiplexedStreamReader, int expectedEntries) throws IOException {
        for (int i = 0; i < expectedEntries; i++) {
            Optional<ExactSizeInputStream> entry = multiplexedStreamReader.readNextEntry();
            assertThat(entry).isNotEmpty();
            assertThat(entry.get()).hasSameContentAs(new ByteArrayInputStream(getTestData(i)));
        }
    }

    private byte[] getTestData(int index) {
        return ("Test" + index).getBytes();
    }
}
