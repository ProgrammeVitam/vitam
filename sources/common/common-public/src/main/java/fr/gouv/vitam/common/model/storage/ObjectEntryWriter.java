/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.model.storage;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ObjectEntryWriter implements AutoCloseable {

    private static final String EMPTY_ENTRY = "{}";
    private final OutputStream ouputStream;
    private final BufferedWriter writer;
    private boolean isEmpty = true;
    private boolean isEofWritten;
    private boolean isClosed;

    public ObjectEntryWriter(OutputStream ouputStream) {
        this.ouputStream = ouputStream;
        this.writer = new BufferedWriter(new OutputStreamWriter(ouputStream));
    }

    public void write(ObjectEntry objectEntry) throws IOException {
        ParametersChecker.checkParameter("");
        ensureNotClosed();
        ensureNotEof();
        if (!isEmpty) {
            writer.append("\n");
        }
        isEmpty = false;
        writer.write(JsonHandler.unprettyPrint(objectEntry));
    }

    public void writeEof() throws IOException {
        ensureNotClosed();
        ensureNotEof();
        if (!isEmpty) {
            writer.append("\n");
        }
        isEmpty = false;
        writer.write(EMPTY_ENTRY);
        isEofWritten = true;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        isClosed = true;
        if(isEmpty) {
            return;
        }
        this.writer.flush();
        this.writer.close();
        this.ouputStream.close();
    }

    private void ensureNotClosed() throws IOException {
        if (isClosed) {
            throw new IOException("Closed stream");
        }
    }

    private void ensureNotEof() throws IOException {
        if (isEofWritten) {
            throw new IOException("Cannot append after EOF");
        }
    }
}
