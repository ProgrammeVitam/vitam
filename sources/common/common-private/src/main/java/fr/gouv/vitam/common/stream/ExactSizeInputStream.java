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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * Input stream that checks input stream size (avoids premature EOF or invalid file size)
 *
 * This input stream guaranties that an exception occur BEFORE the last data byte is returned.
 * For empty streams  (size=0), EOF check is done at constructor initialization time.
 */
public class ExactSizeInputStream extends FilterInputStream {

    private final long size;

    private long pos = 0;

    private boolean isEOF = false;

    public ExactSizeInputStream(final InputStream in, final long size) throws IOException {
        super(in);
        if (size < 0L) {
            throw new IllegalArgumentException("Invalid size " + size);
        }
        this.size = size;
        failFastIfEmptyStream();
    }

    private void failFastIfEmptyStream() throws IOException {
        // If empty, force check EOF at initialization time.
        // Otherwise, we might return an input stream that might never be read.
        // Ex. When returning an empty stream as http response, with Content-Length=0, the client might never know
        // there is a size mismatch, only the server would catch an exception, but it's too late to notify the client
        // (http request have response headers, but no checksum or footer).
        if (this.size == 0L) {
            if (in.read() != EOF) {
                throw new IOException("Broken stream. Expected EOF at position " + 0);
            }
            isEOF = true;
        }
    }

    @Override
    public int read() throws IOException {
        // EOF already reached
        if (isEOF) {
            return EOF;
        }

        final int result = in.read();
        int readBytes = result == EOF ? EOF : 1;
        checkEOF(readBytes);
        return result;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IllegalArgumentException();
        }

        // EOF already reached
        if (isEOF) {
            return EOF;
        }

        int maxLength = (int) Math.min(size - pos, len);
        int result = in.read(b, off, maxLength);

        checkEOF(result);

        return result;
    }

    private void checkEOF(int readBytes) throws IOException {
        if (readBytes == EOF) {
            throw new IOException("Broken stream. Premature EOF at position " + pos + ". Expected size = " + size);
        }

        pos += readBytes;
        if (pos == size) {
            if (in.read() != EOF) {
                throw new IOException("Broken stream. Expected EOF at position " + pos);
            }
            isEOF = true;
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        final long toSkip = Math.min(n, size - pos);
        final long skippedBytes = in.skip(toSkip);
        pos += skippedBytes;
        return skippedBytes;
    }

    @Override
    public synchronized void mark(int readlimit) {}

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public long getSize() {
        return size;
    }
}
