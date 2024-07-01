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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Buffer with buffering allowing One Writer and Multiple Readers.
 *
 * - Storage is done in a fixed size circular buffer (https://en.wikipedia.org/wiki/Circular_buffer)
 * - Reader &amp; writers are synchronized using multiple Producer-Consumer locks : (https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem)
 * - Writer can write till circular buffer is full. Then it blocks until free space is available (ALL readers have read some data)
 * - Reader cannot read till the Writer writes data to the circular buffer.
 */
public class BoundedByteBuffer implements AutoCloseable {

    private final int bufferSize;
    private final byte[] circularBuffer;

    private final ProducerConsumerLock[] locks;
    private final int readerCount;

    private AtomicBoolean endOfStream = new AtomicBoolean(false);

    private final Writer writer;
    private final Reader[] readers;

    public BoundedByteBuffer(int bufferSize, int readerCount) {
        this.bufferSize = bufferSize;
        this.readerCount = readerCount;

        this.circularBuffer = new byte[bufferSize];

        this.locks = new ProducerConsumerLock[readerCount];
        for (int i = 0; i < readerCount; i++) {
            this.locks[i] = new ProducerConsumerLock(bufferSize);
        }

        writer = new Writer();
        readers = new Reader[readerCount];
        for (int i = 0; i < readerCount; i++) {
            readers[i] = new Reader(i);
        }
    }

    public Writer getWriter() {
        return writer;
    }

    public InputStream getReader(int index) {
        if (index < 0 || index >= readerCount) {
            throw new IllegalArgumentException("Invalid index");
        }
        return readers[index];
    }

    @Override
    public void close() {
        writer.close();
        for (Reader reader : readers) {
            reader.close();
        }
    }

    /**
     * Writes data to the {@link BoundedByteBuffer}
     * At the end of data, should write and End Of File (EOF) using the writeEOF() method
     * Closing the Writer without EOF would throw a IOException (Broken stream)
     *
     * Non thread safe. Writer should be used by a single thread.
     */
    public class Writer implements AutoCloseable {

        private int writePos;
        private boolean closed;

        private Writer() {
            writePos = 0;
            closed = false;
        }

        /**
         * Writes data to buffer.
         * Cannot write more than buffer size
         */
        public void write(byte[] src, int offset, int length) throws InterruptedException, IOException {
            if (offset < 0 || length < 0 || offset + length > src.length || length > bufferSize) {
                throw new IllegalArgumentException("Invalid offset / length");
            }

            if (closed) {
                throw new IOException("Cannot write to closed buffer");
            }

            // Await for free space
            awaitFreeBufferSpace(length);

            // Write to end (from offset to end)
            int bytesToWriteAtEnd = Math.min(bufferSize - writePos, length);
            if (bytesToWriteAtEnd > 0) {
                System.arraycopy(src, offset, circularBuffer, writePos, bytesToWriteAtEnd);
            }

            // Write from beginning (from 0)
            int bytesToWriteAtBeginning = length - bytesToWriteAtEnd;
            if (bytesToWriteAtBeginning > 0) {
                System.arraycopy(src, offset + bytesToWriteAtEnd, circularBuffer, 0, bytesToWriteAtBeginning);
            }

            writePos = (writePos + length) % bufferSize;

            // notify
            notifyConsumers(length);
        }

        private void awaitFreeBufferSpace(int length) throws InterruptedException, IOException {
            boolean atLeastOneReaderAlive = false;
            for (ProducerConsumerLock lock : locks) {
                boolean acquired = lock.tryBeginProduce(length);
                if (acquired) {
                    atLeastOneReaderAlive = true;
                }
            }
            if (!atLeastOneReaderAlive) {
                throw new IOException("Broken stream. No more active readers");
            }
        }

        private void notifyConsumers(int length) {
            for (ProducerConsumerLock lock : locks) {
                lock.endProduce(length);
            }
        }

        /**
         * Signals that stream ended successfully.
         */
        public void writeEOF() {
            endOfStream.set(true);
        }

        /**
         * Closes the writer &amp; all associated resources.
         * If close() is invoked without writeEOF() the reader side will get an IOException (broken stream).
         */
        public void close() {
            this.closed = true;
            for (ProducerConsumerLock lock : locks) {
                lock.close();
            }
        }
    }

    /**
     * Reader InputStream.
     * Every reader has a read index from the circular buffer.
     *
     * Non thread safe. A Reader should be used by a single thread.
     */
    private class Reader extends InputStream implements AutoCloseable {

        private final ProducerConsumerLock lock;
        private int readPos;
        private boolean closed;

        private Reader(int index) {
            if (index < 0 || index >= readerCount) {
                throw new IllegalArgumentException("Invalid index");
            }
            this.readPos = 0;
            this.lock = locks[index];
            this.closed = false;
        }

        /**
         * Reads next byte
         *
         * @return 0-255 if byte read successfully. -1 if EOF (writer stream is closed AFTER writeEOF method invoked).
         * @throws IOException is reader stream is closed, or writer stream is closed WITHOUT writeEOF method invocation.
         */
        @Override
        public int read() throws IOException {
            byte[] buffer = new byte[1];
            int res = read(buffer, 0, 1);
            if (res == IOUtils.EOF) {
                return IOUtils.EOF;
            }
            return buffer[0] & 0xFF;
        }

        /**
         * Reads from stream and fills buffer
         *
         * @return Read data length, if any. -1 if EOF (writer stream is closed AFTER writeEOF method invoked).
         * @throws IOException is reader stream is closed, or writer stream is closed WITHOUT writeEOF method invocation.
         */
        @Override
        public int read(byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        /**
         * Reads from stream and fills buffer
         *
         * @param buffer the buffer into which the data is written.
         * @param offset the start offset at which the data is written.
         * @param length the maximum number of bytes to read.
         * @return the total number of bytes read into the buffer, if any. OR -1 if EndOfFile (writer stream is closed AFTER writeEOF method invoked).
         * @throws IOException is reader stream is closed, or writer stream is closed WITHOUT writeEOF method invocation.
         */
        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (offset < 0 || length < 0 || offset + length > buffer.length) {
                throw new IllegalArgumentException("Invalid length");
            }

            if (closed) {
                throw new IOException("Cannot read from closed buffer");
            }

            if (length == 0) {
                return 0;
            }

            // Await available data
            int availableLength = awaitDataAvailableToRead(length);

            // Check end of file
            if (availableLength == 0) {
                if (endOfStream.get()) {
                    return IOUtils.EOF;
                }

                throw new IOException("Broken stream. Buffer closed without EOF");
            }

            // Copy buffer
            // Read from pos to end
            int bytesToReadFromPos = Math.min(bufferSize - readPos, availableLength);
            System.arraycopy(circularBuffer, readPos, buffer, offset, bytesToReadFromPos);

            // Copy from beginning
            int bytesToReadFromBeginning = availableLength - bytesToReadFromPos;
            if (bytesToReadFromBeginning > 0) {
                System.arraycopy(circularBuffer, 0, buffer, offset + bytesToReadFromPos, bytesToReadFromBeginning);
            }
            readPos = (readPos + availableLength) % bufferSize;

            // Release writer lock
            notifyWriter(availableLength);

            return availableLength;
        }

        private int awaitDataAvailableToRead(int length) throws IOException {
            int availableLength;
            try {
                availableLength = lock.tryBeginConsume(length);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted thread", e);
            }
            return availableLength;
        }

        private void notifyWriter(int availableLength) {
            lock.endConsume(availableLength);
        }

        /**
         * Closes the reader & all associated resources.
         * If all readers are closed, the writer might get an IOException (broken stream) if it tries to write to buffer.
         */
        @Override
        public void close() {
            this.closed = true;
            this.lock.close();
        }
    }
}
