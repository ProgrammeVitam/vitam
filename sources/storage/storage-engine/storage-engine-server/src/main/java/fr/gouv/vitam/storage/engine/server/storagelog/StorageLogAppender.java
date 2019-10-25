/*
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
package fr.gouv.vitam.storage.engine.server.storagelog;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogStructure;

/**
 * Storage log appender.
 *
 * Not thread-safe. Should not be invoked by multiple threads concurrently.
 */
public class StorageLogAppender implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogAppender.class);

    private final Writer writer;
    private final String lineSeparator = "\n";

    public StorageLogAppender(Path filePath) throws IOException {
        OutputStream outputStream = openStream(filePath);
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    private OutputStream openStream(Path path) throws IOException {
        try {
            return Files.newOutputStream(path, CREATE_NEW, APPEND);
        } catch (IOException e) {
            throw new IOException(String.format("Cannot open storage log file %s", path.toFile().getAbsolutePath()), e);
        }
    }

    /**
     * Append to the current log.
     *
     * @param parameters information to append to logFile
     * @throws IOException
     */
    public void append(StorageLogStructure parameters) throws IOException {
        writer.append(JsonHandler.unprettyPrint(parameters.getMapParameters()));
        writer.append(lineSeparator);
        writer.flush();
    }
    
    @Override
    public void close() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not close stream", ex);
        }
    }
}
