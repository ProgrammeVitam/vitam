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
package fr.gouv.vitam.worker.core.distribution;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.ORDERED;

public class JsonLineGenericIterator<T> implements CloseableIterator<T> {

    private final InputStream inputStream;
    private final TypeReference<T> typeReference;
    private byte[] buffer;
    private int pos;
    private int available;
    private boolean eof;
    private boolean closed;

    public JsonLineGenericIterator(InputStream inputStream, TypeReference<T> typeReference) {
        ParametersChecker.checkParameter("inputStream", inputStream);
        ParametersChecker.checkParameter("typeReference", typeReference);
        this.inputStream = inputStream;
        this.typeReference = typeReference;
        this.buffer = new byte[8192];
        this.pos = 0;
        this.available = 0;
        this.eof = false;
        this.closed = false;
    }

    @Override
    public boolean hasNext() {
        try {
            ensureLoaded();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return !eof;
    }

    private void ensureLoaded() throws IOException {
        if (closed) {
            throw new IOException("Attempt to read from closed stream");
        }
        if (this.eof) {
            return;
        }
        if (pos < available) {
            return;
        }
        int read = inputStream.read(this.buffer);
        if (read == -1) {
            // EOF
            this.eof = true;
            return;
        }
        available = read;
        pos = 0;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }

        InputStream lineInputStream = new InputStream() {
            boolean endOfLineStream = false;

            @Override
            public int read() throws IOException {
                if (endOfLineStream) {
                    return -1;
                }

                ensureLoaded();
                if (eof) {
                    endOfLineStream = true;
                    return -1;
                }

                byte nextChar = buffer[pos++];
                if (nextChar == '\n') {
                    endOfLineStream = true;
                    return -1;
                }
                return nextChar;
            }

            @Override
            public int read(byte[] buf) throws IOException {
                return read(buf, 0, buf.length);
            }

            @Override
            public int read(byte[] buf, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                if (endOfLineStream) {
                    return -1;
                }

                ensureLoaded();
                if (eof) {
                    endOfLineStream = true;
                    return -1;
                }

                int totalReadBytes = 0;
                int writeOffset = off;

                while (totalReadBytes < len && pos < available) {
                    byte nextChar = JsonLineGenericIterator.this.buffer[pos++];
                    if (nextChar == '\n') {
                        endOfLineStream = true;
                        break;
                    }
                    totalReadBytes++;
                    buf[writeOffset++] = nextChar;
                }
                return totalReadBytes;
            }

            @Override
            public void close() {
                // Do not close inner stream
            }
        };

        try {
            // Wrap line input stream to ensure all line is read
            // Jackson may not consume the whole line stream if it ends with spacing or \n
            InputStream remainingReadOnCloseInputStream = StreamUtils.getRemainingReadOnCloseInputStream(
                lineInputStream
            );
            return JsonHandler.getFromInputStreamAsTypeReference(remainingReadOnCloseInputStream, typeReference);
        } catch (InvalidParseOperationException | IOException e) {
            throw new RuntimeException("Could not parse json line entry", e);
        }
    }

    public void skip() {
        this.next();
    }

    @Override
    public void close() {
        try {
            this.buffer = null;
            this.closed = true;
            this.inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close reader", e);
        }
    }

    private Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this, ORDERED);
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
