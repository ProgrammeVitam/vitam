package fr.gouv.vitam.common.stream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class PrependedMultiplexedInputStreamTest {

    @Test
    public void testPrependEmptyMultiplexedInputStream() throws IOException {

        // Given
        byte[] header = "header".getBytes();

        ByteArrayOutputStream multiplexedByteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(multiplexedByteArrayOutputStream);
        multiplexedStreamWriter.appendEndOfFile();
        InputStream multiplexedInputStream = multiplexedByteArrayOutputStream.toInputStream();
        ByteArrayInputStream headerInputStream = new ByteArrayInputStream(header);

        // When
        PrependedMultiplexedInputStream prependedMultiplexedInputStream = new PrependedMultiplexedInputStream(
            headerInputStream,
            header.length,
            multiplexedInputStream,
            multiplexedByteArrayOutputStream.size());

        // Then
        MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(prependedMultiplexedInputStream);
        assertThat(multiplexedStreamReader.readNextEntry().get()).hasSameContentAs(new ByteArrayInputStream(header));
        assertThat(multiplexedStreamReader.readNextEntry()).isEmpty();
    }

    @Test
    public void testPrepensNonEmptyMultiplexedInputStream() throws IOException {

        // Given
        byte[] entry1 = "entry1".getBytes();
        byte[] entry2 = "entry2".getBytes();
        byte[] header = "header".getBytes();

        ByteArrayOutputStream multiplexedByteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(multiplexedByteArrayOutputStream);
        multiplexedStreamWriter.appendEntry(entry1.length, new ByteArrayInputStream(entry1));
        multiplexedStreamWriter.appendEntry(entry2.length, new ByteArrayInputStream(entry2));
        multiplexedStreamWriter.appendEndOfFile();
        InputStream multiplexedInputStream = multiplexedByteArrayOutputStream.toInputStream();


        ByteArrayInputStream headerInputStream = new ByteArrayInputStream(header);

        // When
        PrependedMultiplexedInputStream prependedMultiplexedInputStream = new PrependedMultiplexedInputStream(
            headerInputStream,
            header.length,
            multiplexedInputStream,
            multiplexedByteArrayOutputStream.size());

        // Then
        MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(prependedMultiplexedInputStream);
        assertThat(multiplexedStreamReader.readNextEntry().get()).hasSameContentAs(new ByteArrayInputStream(header));
        assertThat(multiplexedStreamReader.readNextEntry().get()).hasSameContentAs(new ByteArrayInputStream(entry1));
        assertThat(multiplexedStreamReader.readNextEntry().get()).hasSameContentAs(new ByteArrayInputStream(entry2));
        assertThat(multiplexedStreamReader.readNextEntry()).isEmpty();
    }
}
