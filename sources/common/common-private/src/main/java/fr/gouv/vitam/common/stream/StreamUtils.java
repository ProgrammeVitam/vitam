/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 * This class supports Helpers on streams.
 */
public class StreamUtils {
    private static final int BUFFER_SIZE = 65536;

    private StreamUtils() {
        // Empty
    }

    /**
     * Copy InputStream to OutputStream efficiently
     * 
     * @param inputStream
     * @param outputStream
     * @return the copied length
     * @throws IOException
     */
    public static final long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
            final WritableByteChannel outputChannel = Channels.newChannel(outputStream)) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(VitamConfiguration.getChunkSize());
            long length = 0;
            int len;
            while ((len = inputChannel.read(buffer)) != -1) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                outputChannel.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
                if (len > 0) {
                    length += len;
                }
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }
            return length;
        }
    }

    /**
     * Close silently the InputStream, first consuming any bytes available, ignoring IOException or null object.
     *
     * @param inputStream
     * @return the length in bytes that were read before closing
     */
    public static final long closeSilently(InputStream inputStream) {
        return consumeInputStream(inputStream);
    }

    private static class RemainingReadOnCloseInputStream extends InputStream {
        private final InputStream source;

        private RemainingReadOnCloseInputStream(InputStream inputStream) {
            source = inputStream;
        }

        @Override
        public int available() throws IOException {
            return source.available();
        }

        @Override
        public void close() {
            consumeInputStream(source);
        }

        @Override
        public synchronized void mark(int readlimit) {
            source.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return source.markSupported();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return source.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return source.read(b);
        }

        @Override
        public synchronized void reset() throws IOException {
            source.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return source.skip(n);
        }

        @Override
        public int read() throws IOException {
            return source.read();
        }

    }

    /**
     * Build an InputStream over the source one that will consume any left data when closing it.
     *
     * @param inputStream
     * @return the new InputStream to use
     */
    public static final InputStream getRemainingReadOnCloseInputStream(final InputStream inputStream) {
        return new RemainingReadOnCloseInputStream(inputStream);
    }

    /**
     * Read and close the inputStream using buffer read (read(buffer))
     *
     * @param inputStream
     * @return the size of the inputStream read
     */
    private static final long consumeInputStream(InputStream inputStream) {
        long read = 0;
        if (inputStream == null) {
            return read;
        }
        final byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int len;
            while ((len = inputStream.read(buffer)) >= 0) {
                read += len;
            }
        } catch (final IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
        return read;
    }
}
