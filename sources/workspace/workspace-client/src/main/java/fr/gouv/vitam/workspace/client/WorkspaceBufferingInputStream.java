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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

public class WorkspaceBufferingInputStream extends InputStream {

    private final WorkspaceClientFactory workspaceClientFactory;
    private final String containerName;
    private final String objectName;
    private final int maxOnDiskBufferSize;
    private final int maxInMemoryBufferSize;
    private final File tmpDirectory;
    private InputStream inputStream;
    private long totalReadBytes;
    private boolean isLastChunk = false;
    private boolean closed = false;

    public WorkspaceBufferingInputStream(
        WorkspaceClientFactory workspaceClientFactory,
        String containerName,
        String objectName,
        int maxOnDiskBufferSize,
        int maxInMemoryBufferSize,
        File tmpDirectory
    ) throws IOException, ContentAddressableStorageNotFoundException {
        this.workspaceClientFactory = workspaceClientFactory;
        this.containerName = containerName;
        this.objectName = objectName;
        this.maxOnDiskBufferSize = maxOnDiskBufferSize;
        this.maxInMemoryBufferSize = maxInMemoryBufferSize;
        this.tmpDirectory = tmpDirectory;
        this.totalReadBytes = 0L;

        loadNextBuffer();
    }

    private void loadNextBuffer() throws ContentAddressableStorageNotFoundException, IOException {
        // Cleanup previously open input stream
        cleanup();

        // Load next chunk and store it :
        // - In memory if chunk size <= maxInMemoryBufferSize
        // - In a tmp file otherwise (<= maxOnDiskBufferSize)
        try (
            WorkspaceClient workspaceClient = this.workspaceClientFactory.getClient();
            Response response = workspaceClient.getObject(
                this.containerName,
                this.objectName,
                this.totalReadBytes,
                (long) this.maxOnDiskBufferSize
            );
            InputStream objInputStream = response.readEntity(InputStream.class)
        ) {
            int chunkSize = Integer.parseInt(response.getHeaderString(GlobalDataRest.X_CHUNK_LENGTH));

            this.inputStream = new DeferredFileBufferingInputStream(
                objInputStream,
                chunkSize,
                maxInMemoryBufferSize,
                tmpDirectory
            );

            this.isLastChunk = chunkSize < this.maxOnDiskBufferSize;
        } catch (ContentAddressableStorageServerException e) {
            throw new IOException(e);
        }
    }

    private void cleanup() throws IOException {
        if (this.inputStream != null) {
            this.inputStream.close();
        }
    }

    @Override
    public int read() throws IOException {
        ensureNotClosed();

        int result = this.inputStream.read();
        if (result == EOF && !isLastChunk) {
            try {
                loadNextBuffer();
            } catch (ContentAddressableStorageNotFoundException ex) {
                throw new IOException("Could not find file anymore " + containerName + "/" + objectName, ex);
            }

            result = this.inputStream.read();
        }

        if (result != EOF) {
            this.totalReadBytes++;
        }

        return result;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        ensureNotClosed();

        int result = this.inputStream.read(buffer, off, len);
        if (result == EOF && !isLastChunk) {
            try {
                loadNextBuffer();
            } catch (ContentAddressableStorageNotFoundException ex) {
                throw new IOException("Could not find file anymore " + containerName + "/" + objectName, ex);
            }

            result = this.inputStream.read(buffer, off, len);
        }

        if (result != EOF) {
            this.totalReadBytes += result;
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            cleanup();
            this.closed = true;
        }
        super.close();
    }

    private void ensureNotClosed() throws IOException {
        if (this.closed) {
            throw new IOException("Already closed input stream");
        }
    }
}
