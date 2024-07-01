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
package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Rule;
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

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    @RunWithCustomExecutor
    public void testExtractObjectInfo() throws Exception {
        // Given
        String requestId = GUIDFactory.newGUID().getId();
        List<String> objectIds = Arrays.asList("ob1", "ob2", "ob3");

        byte[][] entries = new byte[][] { "some data".getBytes(), "".getBytes(), "another data".getBytes() };

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
            new MultiplexedStreamObjectInfoListenerThread(
                2,
                requestId,
                multiplexedInputStream,
                DigestType.SHA512,
                objectIds
            );
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
