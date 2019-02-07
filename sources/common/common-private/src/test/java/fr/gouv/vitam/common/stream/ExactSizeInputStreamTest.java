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
        assertThatThrownBy(
            () -> new ExactSizeInputStream(new NullInputStream(1L), 0L)
        ).isInstanceOf(IOException.class);
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
        assertThatThrownBy(() ->
            readBytes(exactSizeInputStream, 10)
        ).isInstanceOf(IOException.class);
    }

    @Test
    public void testTooLargeStream_readByteArray() throws Exception {

        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 9L);

        // When / Then
        assertThatThrownBy(() ->
            readByteArray(exactSizeInputStream, 3, 10)
        ).isInstanceOf(IOException.class);
    }

    @Test
    public void testInvalidTooShortStream_readBytes() throws Exception {

        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 9L);

        // When / Then
        assertThatThrownBy(() ->
            readBytes(exactSizeInputStream, 10)
        ).isInstanceOf(IOException.class);
    }

    @Test
    public void testInvalidTooShortStream_readByteArray() throws Exception {

        // Given
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(new NullInputStream(10L), 11L);

        // When / Then
        assertThatThrownBy(() ->
            readByteArray(exactSizeInputStream, 3, 10)
        ).isInstanceOf(IOException.class);
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
