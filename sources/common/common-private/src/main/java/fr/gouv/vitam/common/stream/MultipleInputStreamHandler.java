/**
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
 */
package fr.gouv.vitam.common.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

/**
 * Multiple InputStream allows to handle from one unique InputStream multiple InputStreams linked to it, such that from
 * one InputStream, we can read efficiently separately (different threads) the very same InputStream once.
 */
public class MultipleInputStreamHandler implements VitamAutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultipleInputStreamHandler.class);
    private static final int BUFFER_SIZE = 65536;
    // Read ahead x4
    private static final int BUFFER_NUMBER = 4;
    // 262 MB max
    // TODO make it configurable
    private static final int MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER = 1000;
    private static final BlockingQueue<StreamBuffer> POOL_CHUNK = new LinkedBlockingQueue<>(
            BUFFER_NUMBER * MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER);
    private static final AtomicInteger NB_CONCURRENT_READER = new AtomicInteger(0);
    private static final StreamBuffer ERROR_NO_BUFFER;
    private static final StreamBuffer ERROR_NOT_READABLE;
    static {
        ERROR_NO_BUFFER = new StreamBuffer();
        ERROR_NO_BUFFER.exception = new IOException("Error while trying to read from source: no buffer available");
        ERROR_NOT_READABLE = new StreamBuffer();
        ERROR_NOT_READABLE.exception = new IOException("Error while trying to read from source: stream not readable");
        for (int i = 0; i < BUFFER_NUMBER * MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
            final StreamBuffer buffer = new StreamBuffer();
            POOL_CHUNK.add(buffer);
        }
    }

    private final InputStream source;
    private final int nbCopy;
    private int currentChunk = 0;
    private volatile boolean endOfRead = false;
    private final AtomicInteger active;
    private final StreamBufferInputStream[] inputStreams;
    private final BlockingQueue<StreamBuffer> buffers;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private ThreadReader threadReader;

    /**
     * Create one MultipleInputStreamHandler from one InputStream and make nbCopy linked InputStreams
     *
     * @param source
     * @param nbCopy
     * @throws IllegalArgumentException
     *             if source is null or nbCopy <= 0
     */
    public MultipleInputStreamHandler(InputStream source, int nbCopy) {
        ParametersChecker.checkParameter("InputStream cannot be null", source);
        ParametersChecker.checkValue("nbCopy", nbCopy, 1);
        this.source = source;
        this.nbCopy = nbCopy;
        active = new AtomicInteger(nbCopy);
        inputStreams = new StreamBufferInputStream[nbCopy];
        // Fill first buffers up to BUFFER_NUMBER
        buffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
        allocateBuffers();
    }

    /**
     * 
     * @return the available pool size
     */
    public static int getPoolAvailability() {
        return POOL_CHUNK.size();
    }

    private synchronized void allocateBuffers() {
        LOGGER.debug("Available Buffers Before {}", POOL_CHUNK.size());
        for (int j = 0; j < BUFFER_NUMBER; j++) {
            try {
                StreamBuffer streamBuffer = POOL_CHUNK.poll(VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM, TimeUnit.MILLISECONDS);
                if (streamBuffer != null) {
                    buffers.add(streamBuffer);
                } else {
                    returnBuffersToPool();
                    throw new IllegalArgumentException("Not enough availability in Pool Chunk");
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Not enough poll in available POOL_CHUNK", e);
                returnBuffersToPool();
                throw new IllegalArgumentException("Not enough poll in available POOL_CHUNK", e);
            }
        }
        LOGGER.debug("Available Buffers After {} allocated {}", POOL_CHUNK.size(), this);
    }

    private synchronized void startThreadReader() {
        if (started.compareAndSet(false, true)) {
            for (int i = 0; i < nbCopy; i++) {
                inputStreams[i] = new StreamBufferInputStream(this, i);
            }
            threadReader = new ThreadReader(this);
            threadReader.setName("ThreadReader_" + NB_CONCURRENT_READER.incrementAndGet());
            threadReader.start();
        }
    }

    /**
     * Get the rank-th linked InputStream
     *
     * @param rank
     *            between 0 and nbCopy-1
     * @return the rank-th linked InputStream
     * @throws IllegalArgumentException
     *             if rank < 0 or rank >= nbCopy
     */
    public InputStream getInputStream(int rank) {
        if (rank < 0 || rank >= nbCopy) {
            throw new IllegalArgumentException("Rank is invalid");
        }
        startThreadReader();
        return inputStreams[rank];
    }

    private final void internalClose() {
        StreamUtils.closeSilently(source);
        endOfRead = true;
    }

    private synchronized void returnBuffersToPool() {
        // clean at the end
        for (StreamBuffer streamBuffer : buffers) {
            if (! POOL_CHUNK.contains(streamBuffer)) {
                POOL_CHUNK.add(streamBuffer);
            }
        }
        LOGGER.debug("Available Buffers After Clean {} from allocated {}", POOL_CHUNK.size(), this);
        buffers.clear();
    }

    private synchronized void returnToBuffersReader(StreamBuffer buffer) {
        if (buffer != null && !buffers.contains(buffer)) {
            buffer.toRead = 0;
            buffers.add(buffer);
        }
    }
    /**
     * Close and clear. All linked InputStreams will be closed too.
     */
    @Override
    public final void close() {
        internalClose();
        // empty all perCopyBuffers
        for (int i = 0; i < nbCopy; i++) {
            if (inputStreams[i] != null) {
                inputStreams[i].close();
            }
        }
        // clean at the end
        if (threadReader != null) {
            threadReader.interrupt();
        }
        returnBuffersToPool();
    }

    /**
     *
     * @return True if this is closed
     */
    private final boolean closed() {
        if (active.get() <= 0) {
            active.set(0);
        }
        return active.get() <= 0;
    }
    private final boolean endingAllStreamBufferInputStreams() {
        active.decrementAndGet();
        return closed();
    }

    @Override
    public String toString() {
        return new StringBuilder("{nbCopy: ").append(nbCopy).append(", currentChunk: ").append(currentChunk)
                .append(", futureRead: ").append(buffers.size()).append(", endOfRead: ").append(endOfRead).append(", active: ")
                .append(active.get()).append(", globalPool: ").append(POOL_CHUNK.size()).append("}").toString();
    }

    /**
     * Reader asynchronous
     */
    private static class ThreadReader extends Thread {
        private final MultipleInputStreamHandler mish;

        private ThreadReader(MultipleInputStreamHandler mish) {
            this.mish = mish;
        }

        @Override
        public void run() {
            boolean status = true;
            StreamBuffer buffer = null;
            try {
                while (!mish.endOfRead) {
                    buffer = mish.buffers.poll(VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM, TimeUnit.MILLISECONDS);
                    if (buffer == null) {
                        // Timeout occurs
                        LOGGER.error("No available allocated Buffers {}", mish);
                        status = false;
                        break;
                    }
                    internalRead(buffer);
                }
            } catch (final InterruptedException e) {
                LOGGER.error("Interruption detected", e);
                status = false;
                mish.returnToBuffersReader(buffer);
            } finally {
                // clean at the end
                if (status == false) {
                    LOGGER.error(ERROR_NO_BUFFER.exception.getMessage(), new Exception("Stack"));
                    for (int i = 0; i < mish.nbCopy; i++) {
                        mish.inputStreams[i].close();
                        mish.inputStreams[i].current = ERROR_NO_BUFFER;
                    }
                }
                mish.returnBuffersToPool();
                NB_CONCURRENT_READER.decrementAndGet();
            }
        }

        private void internalRead(StreamBuffer buffer) {
            if (mish.endOfRead) {
                return;
            }
            int read = -1;
            mish.currentChunk++;
            try {
                read = mish.source.read(buffer.buffer);
                while (read == 0) {
                    Thread.sleep(5);
                    read = mish.source.read(buffer.buffer);
                }
            } catch (final IOException | InterruptedException e) {
                LOGGER.error(ERROR_NOT_READABLE.exception.getMessage(), e);
                mish.returnToBuffersReader(buffer);
                buffer = ERROR_NOT_READABLE;
                read = -2;
            }
            buffer.available = read;
            buffer.toRead = mish.nbCopy;
            if (read < 0) {
                if (buffer != ERROR_NOT_READABLE) {
                    mish.returnToBuffersReader(buffer);
                }
                mish.internalClose();
            }
            buffer.rank = mish.currentChunk;
            LOGGER.debug("Status: {} {}", this, buffer);
            for (int i = 0; i < mish.nbCopy; i++) {
                mish.inputStreams[i].addToQueue(buffer);
            }
            if (mish.closed()) {
                mish.close();
            }
        }
    }

    /**
     * Fake InputStream based on Queue of available StreamBuffers
     */
    private static final class StreamBufferInputStream extends InputStream {
        private final MultipleInputStreamHandler mish;
        private final BlockingQueue<StreamBuffer> streamBuffers;
        private final int rank;
        private StreamBuffer current;
        private int position;
        private boolean noMoreToRead = false;
        private boolean recursive = false;
        private boolean closed = false;

        private StreamBufferInputStream(MultipleInputStreamHandler mish, int rank) {
            this.mish = mish;
            streamBuffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
            this.rank = rank;
        }

        private void addToQueue(StreamBuffer buffer) {
            streamBuffers.add(buffer);
        }

        @Override
        public String toString() {
            return new StringBuilder("{ Multiple: ").append(mish.toString()).append(", rank: ").append(rank).append(", noMore: ")
                    .append(noMoreToRead).append(", position: ").append(position).append(", buffers: ")
                    .append(streamBuffers.size()).append(", currentBuffer: ")
                    .append(current != null ? current.toString() : "'none'").append(", closed: ").append(closed).append("}")
                    .toString();
        }

        @Override
        public int available() throws IOException {
            synchronized (mish) {
                if (closed || (noMoreToRead && streamBuffers.isEmpty())) {
                    return -1;
                }
                if (recursive && streamBuffers.isEmpty()) {
                    return 0;
                }
                // First check if an error during initialization occurs
                if (current == ERROR_NO_BUFFER) {
                    close();
                    throw ERROR_NO_BUFFER.exception;
                }
                if (current == ERROR_NOT_READABLE) {
                    close();
                    throw ERROR_NOT_READABLE.exception;
                }
                // First get or check the current buffer
                if (current == null || (current.available > 0 && current.available <= position)) {
                    if (current != null) {
                        current.endOfBufferUsed(mish);
                    }
                    try {
                        current = streamBuffers.poll(VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM, TimeUnit.MILLISECONDS);
                        if (current == null) {
                            close();
                            throw new IOException("No Buffer for SubStream available");
                        }
                    } catch (final InterruptedException e) {
                        if (current != null) {
                            current.endOfBufferUsed(mish);
                        }
                        close();
                        throw new IOException("InputStream interrupted", e);
                    }
                    position = 0;
                }
            }
            // Second check if this one is the last one
            if (current.exception != null) {
                final IOException e = current.exception;
                current.endOfBufferUsed(mish);
                close();
                throw e;
            }
            if (current.available < 0) {
                close();
                return -1;
            }
            // Either this is the original current, either this is a very new one
            return current.available - position;
        }

        @Override
        public void close() {
            synchronized (mish) {
                LOGGER.info("Close: {}", this);
                position = 0;
                noMoreToRead = true;
                closed = true;
                if (current != null) {
                    current.endOfBufferUsed(mish);
                }
                current = null;
                if (mish.endingAllStreamBufferInputStreams()) {
                    for (StreamBuffer streamBuffer : streamBuffers) {
                        streamBuffer.endOfBufferUsed(mish);
                    }
                    mish.returnBuffersToPool();
                }
                streamBuffers.clear();
            }
        }

        private void checkEnd() {
            if (current.available <= position) {
                current.endOfBufferUsed(mish);
                current = null;
            }
        }

        @Override
        public int read() throws IOException {
            final int available = available();
            if (available > 0) {
                final int value = current.buffer[position++];
                checkEnd();
                return value;
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int available = available();
            LOGGER.debug("Status: {}\n\tAvailable: {}", this, available);
            if (available > 0) {
                int newLen = current.copy(position, b, off, len);
                position += newLen;
                if (!streamBuffers.isEmpty() && newLen < len) {
                    // Can read more
                    checkEnd();
                    recursive = true;
                    final int addLen = read(b, off + newLen, len - newLen);
                    recursive = false;
                    if (addLen > 0) {
                        newLen += addLen;
                    }
                } else {
                    checkEnd();
                }
                return newLen;
            } else if (available == 0) {
                return 0;
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }
    }

    /**
     * Internal StreamBuffer pooled in the this.availableBuffers
     */
    private static final class StreamBuffer {
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private int available;
        private IOException exception;
        private volatile int toRead;
        private long rank;

        private int copy(int position, byte[] b, int off, int len) {
            final int newLen = Math.min(available - position, len);
            System.arraycopy(buffer, position, b, off, newLen);
            return newLen;
        }

        private void endOfBufferUsed(MultipleInputStreamHandler mish) {
            // Ensure only one thread is doing this
            synchronized (mish) {
                if (this != ERROR_NO_BUFFER && this != ERROR_NOT_READABLE) {
                    toRead--;
                    if (toRead <= 0) {
                        mish.returnToBuffersReader(this);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "Buffer: { available: " + available + ", toRead: " + toRead + " rank: " + rank + " exception: "
                    + (exception != null ? exception.getMessage() : "none");
        }
    }
}
