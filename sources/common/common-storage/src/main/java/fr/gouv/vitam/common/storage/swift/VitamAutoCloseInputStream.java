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
package fr.gouv.vitam.common.storage.swift;

import fr.gouv.vitam.common.storage.exception.StreamAlreadyConsumedException;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * InputStream used to prevent consume an already consumed stream
 */
public class VitamAutoCloseInputStream extends ProxyInputStream {

    /**
     * Creates an automatically closing proxy for the given input stream.
     *
     * @param in underlying input stream
     */
    public VitamAutoCloseInputStream(final InputStream in) {
        super(in);
    }

    /**
     * Closes the underlying input stream and replaces the reference to it
     * with a {@link ClosedInputStream} instance.
     * <p>
     * This method is automatically called by the read methods when the end
     * of input has been reached.
     * <p>
     * Note that it is safe to call this method any number of times. The original
     * underlying input stream is closed and discarded only once when this
     * method is first called.
     *
     * @throws IOException if the underlying input stream can not be closed
     */
    @Override
    public void close() throws IOException {
        in.close();
        in = new AlreadyConsumedInputStream();
    }

    /**
     * Automatically closes the stream if the end of stream was reached.
     *
     * @param n number of bytes read, or -1 if no more bytes are available
     * @throws IOException if the stream could not be closed
     * @since 2.0
     */
    @Override
    protected void afterRead(final int n) throws IOException {
        if (n == EOF) {
            close();
        }
    }

    /**
     * Ensures that the stream is closed before it gets garbage-collected.
     * As mentioned in {@link #close()}, this is a no-op if the stream has
     * already been closed.
     *
     * @throws Throwable if an error occurs
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public class AlreadyConsumedInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new StreamAlreadyConsumedException("Already closed !");
        }

        @Override
        public int available() throws IOException {
            throw new StreamAlreadyConsumedException("Already closed !");
        }

        @Override
        public long skip(final long n) throws IOException {
            throw new StreamAlreadyConsumedException("Already closed !");
        }

        @Override
        public void reset() throws IOException {
            throw new StreamAlreadyConsumedException("Already closed !");
        }

        @Override
        public void close() {
            // NOP
        }
    }
}
