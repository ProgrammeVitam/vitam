/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.stream;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * This class supports Helpers on streams.
 */
public class StreamUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StreamUtils.class);
    private static final int BUFFER_SIZE = 8192;

    private StreamUtils() {
        // Empty
    }

    /**
     * Copy InputStream to OutputStream efficiently<br/>
     * <br/>
     * InputStream will be closed, but not the OutputStream in order to be compatible with StreamingOutput
     *
     * @param inputStream
     * @param outputStream
     * @return the copied length
     * @throws IOException
     */
    public static final long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (
            final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
            final WritableByteChannel outputChannel = Channels.newChannel(outputStream)
        ) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(VitamConfiguration.getChunkSize());
            try {
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
            } finally {
                // Ensure everything is written and Input are closed (not Output)
                try {
                    outputStream.flush();
                } catch (final IOException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                try {
                    inputChannel.close();
                } catch (final IOException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
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
        try {
            // Try read first byte before allocating buffers...
            if (inputStream.read() == -1) {
                return read;
            }
            read++;

            final byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) >= 0) {
                read += len;
            }
        } catch (final IOException e) {
            LOGGER.debug(e);
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
                LOGGER.debug(e);
            }
        }
        return read;
    }

    /**
     * @param inputStream
     * @return the corresponding String from InputStream
     * @throws IOException
     */
    public static final String toString(InputStream inputStream) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(inputStream, Charsets.UTF_8)) {
            return CharStreams.toString(isr);
        } catch (IOException e) {
            throw e;
        } finally {
            closeSilently(inputStream);
        }
    }

    /**
     * @param source
     * @return the corresponding InputStream
     */
    public static InputStream toInputStream(String source) {
        return new ByteArrayInputStream(source.getBytes(CharsetUtils.UTF8));
    }

    /**
     * @param is1
     * @param is2
     * @return True if equals
     */
    public static boolean contentEquals(InputStream is1, InputStream is2) {
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] buffer2 = new byte[BUFFER_SIZE];
        Arrays.fill(buffer, (byte) 0);
        Arrays.fill(buffer2, (byte) 0);
        boolean equals = true;
        try {
            int n1 = 0;
            int n2 = 0;
            do {
                n1 = is1.read(buffer);
                n2 = is2.read(buffer2);
                if (n1 != n2) {
                    return false;
                }
                if (n1 == -1) {
                    return true;
                }
                if (!Arrays.equals(buffer, buffer2)) {
                    return false;
                }
            } while (n1 != -1);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            consumeInputStream(is1);
            consumeInputStream(is2);
        }
    }

    /**
     * This method consume everything (in particular InpuStream) and close the response.
     *
     * @param response
     */
    public static void consumeAnyEntityAndClose(Response response) {
        try {
            if (response != null && response.hasEntity()) {
                final Object object = response.getEntity();
                if (object instanceof InputStream) {
                    StreamUtils.closeSilently((InputStream) object);
                }
            }
        } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (final Exception e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
    }

    public static void closeSilently(Channel channel) {
        try {
            channel.close();
        } catch (Exception ex2) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(ex2);
        }
    }
}
