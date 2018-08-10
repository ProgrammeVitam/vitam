package fr.gouv.vitam.workspace.client;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WorkspaceAutoCleanableStreamingOutput implements StreamingOutput {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceAutoCleanableStreamingOutput.class);

    private final InputStream inputStream;
    private final WorkspaceClient workspaceClient;
    private final String containerName;

    public WorkspaceAutoCleanableStreamingOutput(InputStream is, WorkspaceClient workspaceClient, String containerName) {
        this.inputStream = is;
        this.workspaceClient = workspaceClient;
        this.containerName = containerName;
    }

    @Override public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        try {
            byte[] buff = new byte[1024000];
            int count;

            while ((count = inputStream.read(buff, 0, buff.length)) != -1) {
                outputStream.write(buff, 0, count);
            }

            outputStream.flush();

            if (workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.deleteContainer(containerName, true);
            }

            workspaceClient.close();
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Unable to close or clean workspace");

        }
    }
}
