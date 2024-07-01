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
package fr.gouv.vitam.common.server.application;

import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Helper for Streaming to output one InputStream or File in non async mode
 */
public class VitamStreamingOutput implements StreamingOutput, VitamAutoCloseable {

    private InputStream inputStream;
    private final File file;
    private final boolean toDelete;

    /**
     * @param inputStream the inputstream to forward without Async mode
     */
    public VitamStreamingOutput(InputStream inputStream) {
        this.inputStream = inputStream;
        file = null;
        toDelete = false;
    }

    /**
     * @param file the file to output
     * @param toDelete If True, will delete the file once sent
     */
    public VitamStreamingOutput(File file, boolean toDelete) {
        this.file = file;
        this.toDelete = toDelete;
        inputStream = null;
    }

    @Override
    public void write(OutputStream output) throws IOException {
        if (inputStream == null && file != null) {
            inputStream = new FileInputStream(file);
        }
        if (inputStream != null) {
            try (final InputStream entityStream = inputStream) {
                StreamUtils.copy(entityStream, output);
            }
        }
        if (file != null && toDelete) {
            Files.delete(file.toPath());
        }
    }

    @Override
    public void close() {
        if (inputStream != null) {
            StreamUtils.closeSilently(inputStream);
        }
    }
}
