package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class MultiplexedStreamObjectInfoListenerThreadTest {

    @Test
    public void testExtractObjectInfo() throws Exception {

        // Given
        List<String> objectIds = Arrays.asList("ob1", "ob2", "ob3");

        byte[][] entries = new byte[][] {
            "some data".getBytes(),
            "".getBytes(),
            "another data".getBytes()
        };

        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        JsonHandler.writeAsOutputStream(objectIds, headerOutputStream);
        byte[] header = headerOutputStream.toByteArray();

        ByteArrayOutputStream multiplexedByteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(multiplexedByteArrayOutputStream);
        multiplexedStreamWriter.appendEntry(header.length, new ByteArrayInputStream(header));
        for (byte[] entry : entries) {
            multiplexedStreamWriter.appendEntry(entry.length, new ByteArrayInputStream(entry));
        }
        multiplexedStreamWriter.appendEndOfFile();
        InputStream multiplexedInputStream = spy(multiplexedByteArrayOutputStream.toInputStream());

        // When
        MultiplexedStreamObjectInfoListenerThread multiplexedStreamObjectInfoListenerThread =
            new MultiplexedStreamObjectInfoListenerThread(multiplexedInputStream, DigestType.SHA512, objectIds);
        List<ObjectInfo> result = multiplexedStreamObjectInfoListenerThread.call();

        // Then
        assertThat(result).hasSize(objectIds.size());
        for (int i = 0; i < objectIds.size(); i++) {
            String objectId = objectIds.get(i);
            ObjectInfo objectInfo = result.get(i);
            assertThat(objectInfo.getObjectId()).isEqualTo(objectId);
            assertThat(objectInfo.getSize()).isEqualTo(entries[i].length);

            Digest digest = new Digest(DigestType.SHA512);
            digest.update(entries[i]);
            assertThat(objectInfo.getDigest()).isEqualTo(digest.digestHex());
        }

        verify(multiplexedInputStream, atLeastOnce()).close();
    }
}
