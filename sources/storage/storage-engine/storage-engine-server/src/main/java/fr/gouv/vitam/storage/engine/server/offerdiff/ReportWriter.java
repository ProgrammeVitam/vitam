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

package fr.gouv.vitam.storage.engine.server.offerdiff;

import fr.gouv.vitam.common.json.JsonHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ReportWriter implements AutoCloseable {

    private final OutputStream outputStream;
    private final Writer writer;

    private long totalObjectCount;
    private long errorCount;

    private boolean isEmpty = true;
    private boolean isClosed = false;

    public ReportWriter(File tempFile) throws IOException {
        this.outputStream = new FileOutputStream(tempFile);
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    public void reportMatchingObject(String objectId) {
        this.totalObjectCount++;
    }

    public void reportObjectMismatch(String objectId, Long sizeOffer1, Long sizeOffer2) throws IOException {
        this.totalObjectCount++;
        this.errorCount++;
        if (!isEmpty) {
            writer.append("\n");
        }
        isEmpty = false;

        writer.append(
            JsonHandler.unprettyPrint(
                new ReportEntry().setObjectId(objectId).setSizeInOffer1(sizeOffer1).setSizeInOffer2(sizeOffer2)
            )
        );
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        writer.flush();
        writer.close();
        outputStream.close();
        isClosed = true;
    }

    public long getTotalObjectCount() {
        return totalObjectCount;
    }

    public long getErrorCount() {
        return errorCount;
    }
}
