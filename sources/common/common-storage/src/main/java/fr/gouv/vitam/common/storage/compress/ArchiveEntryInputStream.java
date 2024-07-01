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
package fr.gouv.vitam.common.storage.compress;

import java.io.IOException;
import java.io.InputStream;

/**
 * ArchiveEntryInputStream class
 * Archive input streams <b>MUST</b> override the {@link #read(byte[], int, int)} - or {@link #read()} - method so
 * that reading from the stream generates EOF for the end of data in each entry as well as at the end of the file
 * proper.
 */
public class ArchiveEntryInputStream extends InputStream {

    InputStream inputStream;
    boolean closed = false;

    /**
     * @param archiveInputStream
     * @throws IOException
     */
    public ArchiveEntryInputStream(InputStream archiveInputStream) throws IOException {
        inputStream = archiveInputStream;
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            return -1;
        }
        return inputStream.available();
    }

    @Override
    public long skip(long n) throws IOException {
        if (closed) {
            return -1;
        }
        return inputStream.skip(n);
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (closed) {
            return -1;
        }
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            return -1;
        }
        return inputStream.read(b, off, len);
    }

    @Override
    public void close() {
        closed = true;
    }

    /**
     * Allow to "fakely" reopen this InputStream
     *
     * @param isclosed
     */
    public void setClosed(boolean isclosed) {
        closed = isclosed;
    }
}
