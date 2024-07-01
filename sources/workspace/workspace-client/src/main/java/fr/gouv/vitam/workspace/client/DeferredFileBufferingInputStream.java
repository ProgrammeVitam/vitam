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
package fr.gouv.vitam.workspace.client;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * An input stream that buffers data in memory for small sizes, and to a temporary file otherwise.
 * Any temporary file will be deleted when stream is closed, or if initialization fails in constructor.
 */
public class DeferredFileBufferingInputStream extends ProxyInputStream {

    private File tmpFile;
    private boolean closed = false;

    public DeferredFileBufferingInputStream(
        InputStream sourceInputStream,
        long sourceSize,
        int maxInMemoryBufferSize,
        File tmpDirectory
    ) throws IOException {
        super(null);
        try {
            ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(sourceInputStream, sourceSize);

            if (sourceSize <= maxInMemoryBufferSize) {
                this.tmpFile = null;
                this.in = ByteArrayOutputStream.toBufferedInputStream(exactSizeInputStream, (int) sourceSize);
            } else {
                this.tmpFile = File.createTempFile(GUIDFactory.newGUID().toString(), ".tmp", tmpDirectory);
                FileUtils.copyToFile(exactSizeInputStream, this.tmpFile);
                this.in = new BufferedInputStream(Files.newInputStream(tmpFile.toPath(), StandardOpenOption.READ));
            }
        } catch (IOException e) {
            IOUtils.closeQuietly(this.in);
            FileUtils.deleteQuietly(this.tmpFile);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            // Close any previously open stream buffer
            this.in.close();

            // Delete tmp file
            if (this.tmpFile != null) {
                Files.delete(this.tmpFile.toPath());
            }
            closed = true;
        }
    }
}
