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

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.IOUtils.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BoundedByteBufferTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BoundedByteBufferTest.class);

    private static final int BUFFER_SIZE = 4094;

    @Test
    public void testSingleReader() throws Exception {
        simpleTest(1, 0);
        simpleTest(1, 100);
        simpleTest(1, 1024);
        simpleTest(1, 4094);
        simpleTest(1, 4095);
        simpleTest(1, 10 * 1024 * 1024);
    }

    @Test
    public void testMultipleReaders() throws Exception {
        simpleTest(10, 0);
        simpleTest(10, 100);
        simpleTest(10, 1024);
        simpleTest(10, 4096);
        simpleTest(10, 4097);
        simpleTest(10, 10 * 1024 * 1024);
    }

    private void simpleTest(int readerCount, int size) throws Exception {
        // Given
        ExecutorService executorService = Executors.newFixedThreadPool(
            1 + readerCount,
            VitamThreadFactory.getInstance()
        );
        BoundedByteBuffer instance = new BoundedByteBuffer(BUFFER_SIZE, readerCount);

        // When
        CompletableFuture<Digest> writtenDigestFuture = CompletableFuture.supplyAsync(
            () -> writeRandomData(size, instance),
            executorService
        );
        List<CompletableFuture<Digest>> readDigestFutures = new ArrayList<>();
        for (int i = 0; i < readerCount; i++) {
            InputStream reader = instance.getReader(i);
            readDigestFutures.add(CompletableFuture.supplyAsync(() -> readStream(reader), executorService));
        }

        // Then
        Digest writtenDigest = writtenDigestFuture.get(1, TimeUnit.MINUTES);
        for (CompletableFuture<Digest> readDigestFuture : readDigestFutures) {
            Digest readDigest = readDigestFuture.get(1, TimeUnit.MINUTES);
            assertThat(writtenDigest.digestHex()).isEqualTo(readDigest.digestHex());
        }

        instance.close();

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testBrokenWriter() throws Exception {
        testBrokenWriter(1, 0);
        testBrokenWriter(1, 100);
        testBrokenWriter(1, 1024);
        testBrokenWriter(10, 4096);
        testBrokenWriter(10, 4097);
        testBrokenWriter(10, 10 * 1024 * 1024);
    }

    private void testBrokenWriter(int readerCount, int sizeBeforeBrokenStream) throws InterruptedException {
        // Given
        BoundedByteBuffer instance = new BoundedByteBuffer(BUFFER_SIZE, readerCount);
        ExecutorService executorService = Executors.newFixedThreadPool(
            1 + readerCount,
            VitamThreadFactory.getInstance()
        );

        // When
        CompletableFuture<Digest> writtenDigestFuture = CompletableFuture.supplyAsync(
            () -> writeBrokenData(sizeBeforeBrokenStream, instance),
            executorService
        );
        List<CompletableFuture<Digest>> readDigestFutures = new ArrayList<>();
        for (int i = 0; i < readerCount; i++) {
            InputStream reader = instance.getReader(i);
            readDigestFutures.add(CompletableFuture.supplyAsync(() -> readStream(reader), executorService));
        }

        // Then
        assertThatThrownBy(() -> writtenDigestFuture.get(1, TimeUnit.MINUTES)).hasRootCauseInstanceOf(
            IOException.class
        );

        for (CompletableFuture<Digest> readDigestFuture : readDigestFutures) {
            assertThatThrownBy(() -> readDigestFuture.get(1, TimeUnit.MINUTES)).hasRootCauseInstanceOf(
                IOException.class
            );
        }

        instance.close();

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void givenPartialReaderFailureThenOtherReadersShouldCompleteSuccessfully() throws Exception {
        int size = 1_000_000;
        int readerCount = 3;
        int failingReaderIndex = 1;

        // Given
        ExecutorService executorService = Executors.newFixedThreadPool(
            1 + readerCount,
            VitamThreadFactory.getInstance()
        );
        BoundedByteBuffer instance = new BoundedByteBuffer(BUFFER_SIZE, readerCount);

        instance.getReader(failingReaderIndex).close();

        // When
        CompletableFuture<Digest> writtenDigestFuture = CompletableFuture.supplyAsync(
            () -> writeRandomData(size, instance),
            executorService
        );
        List<CompletableFuture<Digest>> readDigestFutures = new ArrayList<>();
        for (int i = 0; i < readerCount; i++) {
            InputStream reader = instance.getReader(i);
            readDigestFutures.add(CompletableFuture.supplyAsync(() -> readStream(reader), executorService));
        }

        // Then
        Digest writtenDigest = writtenDigestFuture.get(1, TimeUnit.MINUTES);
        for (int i = 0; i < readerCount; i++) {
            if (i == failingReaderIndex) {
                assertThatThrownBy(
                    () -> readDigestFutures.get(failingReaderIndex).get(1, TimeUnit.MINUTES)
                ).hasRootCauseInstanceOf(IOException.class);
            } else {
                Digest readDigest = readDigestFutures.get(i).get(1, TimeUnit.MINUTES);
                assertThat(writtenDigest.digestHex()).isEqualTo(readDigest.digestHex());
            }
        }

        instance.close();

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void givenAllReadersFailThenWriterShouldFail() throws Exception {
        int size = 1_000_000_000;
        int readerCount = 3;

        // Given
        ExecutorService executorService = Executors.newFixedThreadPool(
            1 + readerCount,
            VitamThreadFactory.getInstance()
        );
        BoundedByteBuffer instance = new BoundedByteBuffer(BUFFER_SIZE, readerCount);

        // When
        CompletableFuture<Digest> writtenDigestFuture = CompletableFuture.supplyAsync(
            () -> writeRandomData(size, instance),
            executorService
        );
        for (int i = 0; i < readerCount; i++) {
            InputStream reader = instance.getReader(i);
            CompletableFuture.supplyAsync(() -> failReadStream(reader), executorService);
        }

        // Then
        assertThatThrownBy(() -> writtenDigestFuture.get(1, TimeUnit.MINUTES)).hasRootCauseInstanceOf(
            IOException.class
        );

        instance.close();

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    private Digest failReadStream(InputStream reader) {
        try {
            reader.close();
            return null;
        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
    }

    private Digest readStream(InputStream inputStream) {
        try {
            Digest digest = new Digest(DigestType.SHA512);
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);
            OutputStream os = new NullOutputStream();
            IOUtils.copy(digestInputStream, os);
            digestInputStream.close();
            return digest;
        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
    }

    private Digest writeBrokenData(int sizeBeforeBrokenStream, BoundedByteBuffer boundedByteBuffer) {
        try (BoundedByteBuffer.Writer writer = boundedByteBuffer.getWriter()) {
            FakeInputStream fis = new FakeInputStream(sizeBeforeBrokenStream, false, true);
            BrokenInputStream brokenInputStream = new BrokenInputStream();
            SequenceInputStream sequenceInputStream = new SequenceInputStream(fis, brokenInputStream);
            return writeToStream(writer, sequenceInputStream);
        }
    }

    private Digest writeRandomData(int size, BoundedByteBuffer boundedByteBuffer) {
        try (BoundedByteBuffer.Writer writer = boundedByteBuffer.getWriter()) {
            FakeInputStream fis = new FakeInputStream(size, false, true);
            return writeToStream(writer, fis);
        }
    }

    private Digest writeToStream(BoundedByteBuffer.Writer writer, InputStream inputStream) {
        try {
            byte[] buffer = new byte[1024];
            Digest digest = new Digest(DigestType.SHA512);
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);
            int n;
            while (EOF != (n = digestInputStream.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            writer.writeEOF();
            return digest;
        } catch (InterruptedException | IOException e) {
            LOGGER.error(e);
            throw new RuntimeException("Writer error", e);
        } finally {
            writer.close();
        }
    }
}
