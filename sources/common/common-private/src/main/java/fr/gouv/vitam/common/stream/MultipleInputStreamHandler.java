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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Multiple InputStream allows to handle from one unique InputStream multiple InputStreams linked to it, such that from
 * one InputStream, we can read efficiently separately (different threads) the very same InputStream once.
 */
public class MultipleInputStreamHandler implements AutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultipleInputStreamHandler.class);
    private static final int BUFFER_SIZE = 65536;
    // Read ahead x4
    private static final int BUFFER_NUMBER = 4;

    private final InputStream source;
    private final int nbCopy;
    private int currentChunk = 0;
    private volatile boolean endOfRead = false;
    private final AtomicInteger active;
    private final StreamBufferInputStream[] inputStreams;
    private final BlockingQueue<StreamBuffer> buffers;
    private final ExecutorService executor;

    /**
     * Create one MultipleInputStreamHandler from one InputStream and make nbCopy linked InputStreams
     *
     * @param source
     * @param nbCopy
     * @throws IllegalArgumentException if source is null or nbCopy <= 0
     */
    public MultipleInputStreamHandler(InputStream source, int nbCopy) {
        ParametersChecker.checkParameter("InputStream cannot be null", source);
        ParametersChecker.checkValue("nbCopy", nbCopy, 1);
        this.source = source;
        this.nbCopy = nbCopy;
        active = new AtomicInteger(nbCopy);
        inputStreams = new StreamBufferInputStream[nbCopy];
        for (int i = 0; i < nbCopy; i++) {
            inputStreams[i] = new StreamBufferInputStream(this, i);
        }
        // Fill first buffers up to BUFFER_NUMBER
        buffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
        for (int j = 0; j < BUFFER_NUMBER; j++) {
            final StreamBuffer buffer = new StreamBuffer();
            buffers.add(buffer);
        }
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new ThreadReader(this));
    }

    /**
     * Reader asynchronous
     */
    private static class ThreadReader implements Runnable {
        private final MultipleInputStreamHandler mish;

        private ThreadReader(MultipleInputStreamHandler mish) {
            this.mish = mish;
        }

        @Override
        public void run() {
            while (!mish.endOfRead) {
                StreamBuffer buffer;
                try {
                    buffer = mish.buffers.take();
                } catch (final InterruptedException e) {
                    break;
                }
                internalRead(buffer);
            }
            // clean at the end
            mish.buffers.clear();
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
                    read = mish.source.read(buffer.buffer);
                }
            } catch (final IOException e) {
                buffer.exception = e;
                read = -2;
            }
            buffer.available = read;
            buffer.toRead = mish.nbCopy;
            if (read < 0) {
                mish.internalClose();
            }
            buffer.rank = mish.currentChunk;
            LOGGER.debug("Status: {} {}", this, buffer);
            for (int i = 0; i < mish.nbCopy; i++) {
                mish.inputStreams[i].addToQueue(buffer);
            }
        }
    }

    /**
     * Get the rank-th linked InputStream
     *
     * @param rank between 0 and nbCopy-1
     * @return the rank-th linked InputStream
     * @throws IllegalArgumentException if rank < 0 or rank >= nbCopy
     */
    public InputStream getInputStream(int rank) {
        if (rank < 0 || rank >= nbCopy) {
            throw new IllegalArgumentException("Rank is invalid");
        }
        return inputStreams[rank];
    }

    private final void internalClose() {
        try {
            source.close();
        } catch (final IOException e) {
            // ignore
        }
        endOfRead = true;
    }

    /**
     * Close and clear. All linked InputStreams will be closed too.
     */
    @Override
    public final void close() {
        internalClose();
        // empty all perCopyBuffers
        for (int i = 0; i < nbCopy; i++) {
            inputStreams[i].close();
        }
        executor.shutdown();
    }

    /**
     *
     * @return True if this is closed
     */
    public final boolean closed() {
        return active.get() <= 0;
    }

    @Override
    public String toString() {
        return new StringBuilder("{nbCopy: ").append(nbCopy)
            .append(", currentChunk: ").append(currentChunk)
            .append(", futureRead: ").append(buffers.size())
            .append(", endOfRead: ").append(endOfRead)
            .append(", active: ").append(active.get())
            .append("}").toString();
    }

    /**
     * Fake InputStream based on Queue of available StreamBuffers
     */
    private static final class StreamBufferInputStream extends InputStream {
        private final MultipleInputStreamHandler mish;
        private final BlockingQueue<StreamBuffer> buffers;
        private final int rank;
        private StreamBuffer current;
        private int position;
        private boolean noMoreToRead = false;
        private boolean recursive = false;
        private boolean closed = false;

        private StreamBufferInputStream(MultipleInputStreamHandler mish, int rank) {
            this.mish = mish;
            buffers = new LinkedBlockingQueue<>(BUFFER_NUMBER);
            this.rank = rank;
        }

        private void addToQueue(StreamBuffer buffer) {
            buffers.add(buffer);
        }

        @Override
        public String toString() {
            return new StringBuilder("{ Multiple: ").append(mish.toString())
                .append(", rank: ").append(rank).append(", noMore: ").append(noMoreToRead)
                .append(", position: ").append(position).append(", buffers: ").append(buffers.size())
                .append(", currentBuffer: ").append(current != null ? current.toString() : "'none'")
                .append(", closed: ").append(closed)
                .append("}").toString();
        }

        @Override
        public int available() throws IOException {
            if (closed || noMoreToRead && buffers.isEmpty() || recursive && buffers.isEmpty()) {
                return -1;
            }
            // First get or check the current buffer
            synchronized (this) {
                if (current == null || current.available > 0 && current.available <= position) {
                    try {
                        current = buffers.take();
                    } catch (final InterruptedException e) {
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
                current.endOfBufferUsed(mish);
                position = 0;
                noMoreToRead = true;
                return -1;
            }
            // Either this is the original current, either this is a very new one
            return current.available - position;
        }

        @Override
        public void close() {
            synchronized (this) {
                LOGGER.debug("Close: {}", this);
                buffers.clear();
                current = null;
                position = 0;
                noMoreToRead = true;
                if (mish.active.decrementAndGet() == 0 && !closed) {
                    closed = true;
                    mish.close();
                }
                closed = true;
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
                if (!buffers.isEmpty() && newLen < len) {
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
        private int toRead;
        @SuppressWarnings("unused")
        private long rank;

        private int copy(int position, byte[] b, int off, int len) {
            final int newLen = Math.min(available - position, len);
            System.arraycopy(buffer, position, b, off, newLen);
            return newLen;
        }

        private synchronized void endOfBufferUsed(MultipleInputStreamHandler mish) {
            // Ensure only one thread is doing this
            toRead--;
            if (toRead == 0 && !mish.endOfRead) {
                mish.buffers.add(this);
            }
        }
    }
}
