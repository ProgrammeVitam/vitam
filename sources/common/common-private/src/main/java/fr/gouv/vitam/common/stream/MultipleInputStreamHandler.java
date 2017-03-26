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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

/**
 * Multiple InputStream allows to handle from one unique InputStream multiple InputStreams linked to it, such that from
 * one InputStream, we can read efficiently separately (different threads) the very same InputStream once.
 */
public class MultipleInputStreamHandler implements VitamAutoCloseable {
    private static final String GLOBAL_SERVICE_OF_MULTIPLE_INPUT_STREAM_HANDLER_IS_DOWN = "Global service of MultipleInputStreamHandler is down";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultipleInputStreamHandler.class);
    // Static part
    /**
     * Global Thread pool for Reader
     */
    private static final ExecutorService EXECUTOR_THREADREADER = new VitamThreadPoolExecutor();
    /**
     * Global status, if the pool is shutdown already
     */
    private static final AtomicBoolean SERVICE_AVAILABLE = new AtomicBoolean(true);
    /**
     * Read ahead x4 Buffers
     */
    private static final int BUFFER_NUMBER = 4;
    /**
     * Global Pool of chunks
     */
    private static final BlockingQueue<StreamBuffer> POOL_CHUNK = new LinkedBlockingQueue<>(
        BUFFER_NUMBER * VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER);
    /**
     * ERROR when no more buffer is available
     */
    private static final StreamBuffer ERROR_NO_BUFFER =
        new StreamBuffer(new IOException("Error while trying to read from source: no buffer available"), -1);
    /**
     * ERROR when the source is not readable (or through substream)
     */
    private static final StreamBuffer ERROR_NOT_READABLE =
        new StreamBuffer(new IOException("Error while trying to read from source: stream not readable"), -1);
    /**
     * End of InputStream
     */
    private static final StreamBuffer END_OF_STREAM =
        new StreamBuffer(null, -1);

    // Initialize the global static context
    static {
        for (int i = 0; i < BUFFER_NUMBER * VitamConfiguration.MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER; i++) {
            final StreamBuffer buffer = new StreamBuffer();
            POOL_CHUNK.add(buffer);
        }
    }

    /**
     * Real InputStream
     */
    private final InputStream source;
    /**
     * Number of substreams
     */
    private final int nbCopy;
    /**
     * Current chunk readed by the Reader
     */
    private volatile int currentChunk = 0;
    /**
     * End of the source
     */
    private AtomicBoolean endOfReadSource = new AtomicBoolean(false);
    /**
     * Count of active substreams
     */
    private final CountDownLatch activeSubStreams;
    /**
     * Substreams
     */
    private final StreamBufferInputStream[] subInputStreams;
    /**
     * Available buffers for Reader
     */
    private final BlockingQueue<StreamBuffer> availableBuffers;
    /**
     * Allocated buffers for Reader, at the beginning same as Available
     */
    private final List<StreamBuffer> allocatedBuffers;
    /**
     * Is the Reader started or not
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * The future associated with the reader
     */
    private volatile Future<StatusCode> threadReaderStatus;
    /**
     * The Reader
     */
    private volatile ThreadReader threadReader;

    /**
     * Create one MultipleInputStreamHandler from one InputStream and make nbCopy linked InputStreams
     *
     * @param source
     * @param nbCopy
     * @throws IllegalArgumentException if source is null or nbCopy <= 0 or global service is down
     */
    public MultipleInputStreamHandler(InputStream source, int nbCopy) {
        ParametersChecker.checkParameter("InputStream cannot be null", source);
        ParametersChecker.checkValue("nbCopy", nbCopy, 1);
        if (!SERVICE_AVAILABLE.get()) {
            throw new IllegalArgumentException(GLOBAL_SERVICE_OF_MULTIPLE_INPUT_STREAM_HANDLER_IS_DOWN);
        }
        this.source = source;
        this.nbCopy = nbCopy;
        activeSubStreams = new CountDownLatch(nbCopy);
        subInputStreams = new StreamBufferInputStream[nbCopy];
        // Fill first buffers up to BUFFER_NUMBER
        availableBuffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
        allocatedBuffers = new ArrayList<>(BUFFER_NUMBER);
        if (!SERVICE_AVAILABLE.get()) {
            throw new IllegalArgumentException(GLOBAL_SERVICE_OF_MULTIPLE_INPUT_STREAM_HANDLER_IS_DOWN);
        }
        allocateBuffers();
    }

    /**
     * Allow to for terminate of the MISH
     */
    public static void forceClosingAllMultipleInputStreamHandler() {
        SERVICE_AVAILABLE.set(false);
        EXECUTOR_THREADREADER.shutdownNow();
    }

    /**
     * Allocate the availableBuffers for the handler and the allocatedBuffers
     */
    private void allocateBuffers() {
        LOGGER.debug("Available Buffers Before {}", POOL_CHUNK.size());
        for (int j = 0; j < BUFFER_NUMBER; j++) {
            try {
                StreamBuffer streamBuffer =
                    POOL_CHUNK.poll(VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM, TimeUnit.MILLISECONDS);
                if (streamBuffer != null) {
                    availableBuffers.add(streamBuffer);
                    allocatedBuffers.add(streamBuffer);
                } else {
                    returnBuffersToPool();
                    LOGGER.error("Not enough poll in available POOL_CHUNK after delay");
                    throw new IllegalArgumentException("Not enough availability in Pool Chunk");
                }
            } catch (InterruptedException e) {
                LOGGER.error("Not enough poll in available POOL_CHUNK", e);
                returnBuffersToPool();
                throw new IllegalArgumentException("Not enough poll in available POOL_CHUNK", e);
            }
        }
        LOGGER.debug("Available Buffers After {} allocated {}", POOL_CHUNK.size(), this);
    }

    /**
     * Return the allocated buffers to POOL
     */
    private void returnBuffersToPool() {
        // clean at the end
        for (StreamBuffer streamBuffer : allocatedBuffers) {
            POOL_CHUNK.add(streamBuffer.clear());
        }
        LOGGER.debug("Available Buffers After Clean {} from allocated {}", POOL_CHUNK.size(), this);
        availableBuffers.clear();
        allocatedBuffers.clear();
    }

    /**
     * Return the used buffer to availableGlobalBuffers
     * 
     * @param buffer
     */
    private synchronized void returnToBuffersReader(StreamBuffer buffer) {
        if (buffer != null && !availableBuffers.contains(buffer)) {
            availableBuffers.add(buffer.clear());
        }
    }

    /**
     * 
     * @return the available pool size
     */
    public static int getPoolAvailability() {
        return POOL_CHUNK.size();
    }

    /**
     * Start if necessary only once the ThreadReader
     * 
     * @throws IllegalArgumentException if the reader cannot be started
     */
    private synchronized void startThreadReader() {
        if (!SERVICE_AVAILABLE.get()) {
            close();
            throw new IllegalArgumentException(GLOBAL_SERVICE_OF_MULTIPLE_INPUT_STREAM_HANDLER_IS_DOWN);
        }
        if (started.compareAndSet(false, true)) {
            for (int i = 0; i < nbCopy; i++) {
                subInputStreams[i] = new StreamBufferInputStream(this, i);
            }
            threadReader = new ThreadReader(this);
            if (!SERVICE_AVAILABLE.get()) {
                close();
                throw new IllegalArgumentException(GLOBAL_SERVICE_OF_MULTIPLE_INPUT_STREAM_HANDLER_IS_DOWN);
            }
            threadReaderStatus = EXECUTOR_THREADREADER.submit(threadReader);
            try {
                if (!threadReader.started.await(VitamConfiguration.DELAY_MULTIPLE_SUBINPUTSTREAM,
                    TimeUnit.MILLISECONDS)) {
                    close();
                    throw new IllegalArgumentException("Cannot start the thread reader");
                }
            } catch (InterruptedException e) {
                close();
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Get the rank-th linked InputStream
     *
     * @param rank between 0 and nbCopy-1
     * @return the rank-th linked InputStream
     * @throws IllegalArgumentException if rank < 0 or rank >= nbCopy or if the reader cannot be started
     */
    public InputStream getInputStream(int rank) {
        if (rank < 0 || rank >= nbCopy) {
            throw new IllegalArgumentException("Rank is invalid");
        }
        startThreadReader();
        if (subInputStreams[rank] != null && !subInputStreams[rank].closed) {
            return subInputStreams[rank];
        }
        throw new IllegalArgumentException("Rank is already closed");
    }

    @Override
    public void close() {
        LOGGER.info("Closing: {} {}", this, threadReader, new Exception("trace"));
        endOfReadSource.set(true);
        StreamUtils.closeSilently(source);
        // empty all perCopyBuffers
        for (int i = 0; i < nbCopy; i++) {
            if (subInputStreams[i] != null && !subInputStreams[i].closed) {
                subInputStreams[i].close();
            }
        }
        /*
         * try { activeSubStreams.await(VitamConfiguration.DELAY_MULTIPLE_INPUTSTREAM, TimeUnit.MILLISECONDS); } catch
         * (InterruptedException e) { // Ignore SysErrLogger.FAKE_LOGGER.ignoreLog(e); }
         */
        // clean at the end
        if (threadReader != null && threadReaderStatus != null) {
            threadReaderStatus.cancel(true);
        }
        // Finally returns all buffers to POOL
        returnBuffersToPool();
    }


    @Override
    public String toString() {
        return new StringBuilder("{nbCopy: ").append(nbCopy).append(", currentChunk: ").append(currentChunk)
            .append(", futureRead: ").append(availableBuffers.size()).append(", endOfRead: ")
            .append(endOfReadSource.get()).append(", active: ")
            .append(activeSubStreams.getCount()).append(", globalPool: ").append(POOL_CHUNK.size()).append("}")
            .toString();
    }


    /**
     * Reader asynchronous
     */
    private static class ThreadReader implements Callable<StatusCode> {
        private final MultipleInputStreamHandler mish;
        private CountDownLatch started = new CountDownLatch(1);
        private volatile StatusCode status = StatusCode.UNKNOWN;

        private ThreadReader(MultipleInputStreamHandler mish) {
            this.mish = mish;
        }


        @Override
        public StatusCode call() throws Exception {
            started.countDown();
            status = StatusCode.STARTED;
            StreamBuffer buffer = null;
            try {
                // While the Source InputStream is not at the end
                while (!mish.endOfReadSource.get()) {
                    // Get a buffer from the available ones
                    LOGGER.debug("Status 1: {}", this);
                    buffer = mish.availableBuffers.poll(VitamConfiguration.DELAY_MULTIPLE_SUBINPUTSTREAM,
                        TimeUnit.MILLISECONDS);
                    if (buffer == null) {
                        // Timeout occurs
                        LOGGER.error("No available allocated Buffers {}", mish);
                        status = StatusCode.FATAL;
                        break;
                    }
                    // Fill the buffer
                    buffer.clear();
                    LOGGER.debug("Status 2: {} {}", this, buffer);
                    internalRead(buffer);
                }
            } catch (final InterruptedException e) {
                LOGGER.error("Interruption detected", e);
                status = StatusCode.FATAL;
                mish.returnToBuffersReader(buffer);
            } finally {
                // clean at the end
                if (status != StatusCode.OK) {
                    LOGGER.error(ERROR_NO_BUFFER.exception.getMessage() + this.toString());
                    for (int i = 0; i < mish.nbCopy; i++) {
                        mish.subInputStreams[i].addToQueue(ERROR_NOT_READABLE);
                    }
                }
            }
            return status;
        }

        /**
         * Check of abnormal ending
         * 
         * @param buffer
         * @throws IOException if ERROR_NOT_READABLE
         */
        private void checkAbnormalEnd(StreamBuffer buffer) throws IOException {
            if (mish.endOfReadSource.get()) {
                LOGGER.warn(ERROR_NOT_READABLE.exception.getMessage());
                mish.returnToBuffersReader(buffer);
                status = StatusCode.FATAL;
                throw ERROR_NOT_READABLE.exception;
            }
        }

        /**
         * Read one buffer from real InputStream
         * 
         * @param buffer
         */
        private void internalRead(StreamBuffer buffer) {
            buffer.toRead = mish.nbCopy;
            int read = -1;
            mish.currentChunk++;
            try {
                checkAbnormalEnd(buffer);
                read = mish.source.read(buffer.buffer);
                while (read == 0) {
                    Thread.sleep(5);
                    checkAbnormalEnd(buffer);
                    read = mish.source.read(buffer.buffer);
                }
            } catch (final IOException e) {
                LOGGER.error(ERROR_NOT_READABLE.exception.getMessage(), e);
                mish.returnToBuffersReader(buffer);
                buffer = ERROR_NOT_READABLE;
                status = StatusCode.FATAL;
                read = -2;
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
                mish.returnToBuffersReader(buffer);
                buffer = ERROR_NOT_READABLE;
                status = StatusCode.FATAL;
                read = -2;
            }
            buffer.available = read;
            if (read < 0) {
                LOGGER.debug("Set end of reading");
                mish.endOfReadSource.set(true);
            }
            if (read < 0 && buffer != ERROR_NOT_READABLE) {
                mish.returnToBuffersReader(buffer);
            }
            LOGGER.debug("Status: {} {} {}", read, this, buffer);
            if (read == -1) {
                status = StatusCode.OK;
                for (int i = 0; i < mish.nbCopy; i++) {
                    mish.subInputStreams[i].addToQueue(END_OF_STREAM);
                }
            } else {
                for (int i = 0; i < mish.nbCopy; i++) {
                    mish.subInputStreams[i].addToQueue(buffer);
                }
            }
        }

        @Override
        public String toString() {
            return " started: " + started.getCount() + " Status: " + status + " mish: " + mish.toString();
        }
    }

    /**
     * Fake InputStream based on Queue of available StreamBuffers
     */
    private static final class StreamBufferInputStream extends InputStream {
        private final MultipleInputStreamHandler mish;
        /**
         * Buffer for this SubStream
         */
        private final BlockingQueue<StreamBuffer> streamBuffers;
        /**
         * Current rank
         */
        private final int rank;
        /**
         * Current buffer
         */
        private volatile StreamBuffer current;
        /**
         * Current position in current buffer
         */
        private volatile int position;
        /**
         * When a buffer is not fully filled, try to continue
         */
        private boolean recursive = false;
        /**
         * Is this subStream closed
         */
        private volatile boolean closed = false;

        private StreamBufferInputStream(MultipleInputStreamHandler mish, int rank) {
            this.mish = mish;
            streamBuffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
            this.rank = rank;
        }

        /**
         * Add the current buffer to the queue for the current subStream
         * 
         * @param buffer
         */
        private void addToQueue(StreamBuffer buffer) {
            streamBuffers.add(buffer);
        }

        /**
         * End of one Substream, implying if necessary closing of Handler
         */
        private void endingOneSubStream() {
            mish.activeSubStreams.countDown();
            if (mish.activeSubStreams.getCount() == 0) {
                mish.close();
            }
        }

        @Override
        public String toString() {
            return new StringBuilder("{ Multiple: ").append(mish.toString()).append(", rank: ").append(rank)
                .append(", position: ").append(position).append(", buffers: ")
                .append(streamBuffers.size()).append(", currentBuffer: ")
                .append(current != null ? current.toString() : "'none'").append(", closed: ").append(closed).append("}")
                .toString();
        }

        @Override
        public int available() throws IOException {
            LOGGER.debug("Status: {}", this);
            // SubStream already closed or finished
            if (closed) {
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
            // First get or check the current buffer if empty
            if (current == null || (current.available > 0 && current.available <= position)) {
                LOGGER.debug("Status: {}", this);
                // Return the buffer to the StreamReader if possible
                if (current != null) {
                    current.closeBuffer(mish);
                }
                // Get a new buffer
                try {
                    if (closed || streamBuffers.isEmpty()) {
                        LOGGER.debug("Status: {}", this);
                        return 0;
                    }
                    current =
                        streamBuffers.poll(VitamConfiguration.DELAY_MULTIPLE_SUBINPUTSTREAM, TimeUnit.MILLISECONDS);
                    if (current == null) {
                        LOGGER.debug("Status: {}", this);
                        if (mish.endOfReadSource.get()) {
                            return -1;
                        } else {
                            close();
                            throw new IOException("No Buffer for SubStream available");
                        }
                    }
                } catch (final InterruptedException e) {
                    LOGGER.error("Status: {}", this);
                    if (current != null) {
                        current.closeBuffer(mish);
                    }
                    mish.close();
                    throw new IOException("InputStream interrupted", e);
                }
                position = 0;
                LOGGER.debug("Status: {}", this);
            }
            LOGGER.debug("Status: {}", this);
            // Second check if this one is the last one
            if (current.exception != null) {
                final IOException e = current.exception;
                LOGGER.error("Exception found", current.exception);
                close();
                throw e;
            }
            LOGGER.debug("Status: {}", this);
            if (current.available < 0) {
                return -1;
            }
            LOGGER.debug("Status: {}", this);
            // Either this is the original current, either this is a very new one
            return current.available - position;
        }

        @Override
        public void close() {
            if (closed == true) {
                return;
            }
            closed = true;
            LOGGER.debug("Close: {}", this, new Exception("Trace"));
            position = 0;
            if (current != null) {
                current.closeBuffer(mish);
            }
            current = null;
            streamBuffers.clear();
            endingOneSubStream();
        }

        /**
         * For each substream, when a buffer is totally read, check of it can be reused by the global Reader
         */
        private void checkEnd() {
            if (current != null && current.available <= position) {
                current.closeBuffer(mish);
                current = null;
            }
        }

        @Override
        public int read() throws IOException {
            int available = available();
            while (available >= 0) {
                if (available > 0) {
                    final int value = current.buffer[position++];
                    checkEnd();
                    return value;
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                        mish.close();
                        throw new IOException(e);
                    }
                    available = available();
                }
            }
            return -1;
        }

        /**
         * Try to continue to fill the buffer as much as possible
         * 
         * @param b
         * @param off
         * @param len
         * @return the length read
         * @throws IOException
         */
        private int readMore(byte[] b, int off, int len) throws IOException {
            final int available = available();
            LOGGER.debug("Status: {}\n\tAvailable: {}", this, available);
            if (available > 0) {
                int newLen = current.copy(position, b, off, len);
                position += newLen;
                if (!streamBuffers.isEmpty() && newLen < len) {
                    // Can read more
                    checkEnd();
                    final int addLen = readMore(b, off + newLen, len - newLen);
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
                    final int addLen = readMore(b, off + newLen, len - newLen);
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
        private final byte[] buffer;
        private int available;
        private IOException exception;
        private volatile int toRead;

        /**
         * Standard builder
         */
        private StreamBuffer() {
            buffer = new byte[VitamConfiguration.getChunkSize()];
            clear();
        }

        private StreamBuffer(IOException exception, int available) {
            buffer = null;
            this.available = available;
            this.toRead = 0;
            this.exception = exception;
        }

        /**
         * Copy into the buffer (not zero copy - sic -) the data from the offset and the length according to the current
         * position for the current subStream
         * 
         * @param position
         * @param b
         * @param off
         * @param len
         * @return the length really copied
         */
        private int copy(int position, byte[] b, int off, int len) {
            final int newLen = Math.min(available - position, len);
            System.arraycopy(buffer, position, b, off, newLen);
            return newLen;
        }

        /**
         * Check if this buffer is ready to return to the pool for the Reader
         * 
         * @param mish
         */
        private void closeBuffer(MultipleInputStreamHandler mish) {
            // Ensure only one thread is doing this
            if (this != ERROR_NO_BUFFER && this != ERROR_NOT_READABLE && this != END_OF_STREAM) {
                toRead--;
                if (toRead <= 0) {
                    mish.returnToBuffersReader(this.clear());
                }
            }
        }

        /**
         * Clear the buffer
         * 
         * @return this
         */
        public StreamBuffer clear() {
            toRead = 0;
            available = 0;
            exception = null;
            return this;
        }

        @Override
        public String toString() {
            return "Buffer: { available: " + available + ", toRead: " + toRead + " exception: " +
                (exception != null ? exception.getMessage() : "none");
        }
    }

}
