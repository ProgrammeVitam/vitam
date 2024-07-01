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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Generate multiples InputStreams from one to many using Pipe
 */
public class MultiplePipedInputStream implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultiplePipedInputStream.class);

    /**
     * Global Thread pool for Reader
     */
    private static final ExecutorService EXECUTOR_THREAD_READER = new VitamThreadPoolExecutor();

    private final int nbCopy;
    private final BoundedByteBuffer boundedByteBuffer;
    private final InputStream source;
    private volatile IOException lastException = null;

    /**
     * Create one MultipleInputStreamHandler from one InputStream and make nbCopy linked InputStreams
     *
     * @param source
     * @param nbCopy
     * @throws IllegalArgumentException if source is null or nbCopy &lt;= 0 or global service is down
     */
    public MultiplePipedInputStream(InputStream source, int nbCopy) {
        ParametersChecker.checkParameter("InputStream cannot be null", source);
        ParametersChecker.checkValue("nbCopy", nbCopy, 1);

        this.nbCopy = nbCopy;
        this.source = source;

        int bufferSize = VitamConfiguration.getChunkSize() * VitamConfiguration.getBufferNumber();
        this.boundedByteBuffer = new BoundedByteBuffer(bufferSize, nbCopy);

        EXECUTOR_THREAD_READER.execute(() -> {
            try {
                copy();
            } catch (IOException e) {
                LOGGER.error(e);
                lastException = e;
            }
        });
    }

    /**
     * Get the rank-th linked InputStream
     *
     * @param rank between 0 and nbCopy-1
     * @return the rank-th linked InputStream
     * @throws IllegalArgumentException if rank &lt; 0 or rank &gt;= nbCopy
     */
    public InputStream getInputStream(int rank) {
        if (rank < 0 || rank >= nbCopy) {
            throw new IllegalArgumentException("Rank is invalid");
        }
        return this.boundedByteBuffer.getReader(rank);
    }

    /**
     * @throws IOException if any exception is found during multiple streams
     */
    public void throwLastException() throws IOException {
        if (lastException != null) {
            throw lastException;
        }
    }

    protected final void copy() throws IOException {
        try (BoundedByteBuffer.Writer writer = boundedByteBuffer.getWriter()) {
            // Buffer size should not be greater than buffer size.
            byte[] buffer = new byte[VitamConfiguration.getChunkSize()];

            int n;
            while (-1 != (n = source.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            writer.writeEOF();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted thread", e);
        }
    }

    @Override
    public void close() {
        boundedByteBuffer.close();
        StreamUtils.closeSilently(source);
    }
}
