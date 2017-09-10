package fr.gouv.vitam.worker.core.plugin.dip;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;

public class StoreManifest extends ActionHandler {

    private static final String STORE_MANIFEST = "STORE_MANIFEST";
    private static final String DEFAULT_STRATEGY = "default";

    private final StorageClientFactory storageClientFactory;

    public StoreManifest() {
        storageClientFactory = StorageClientFactory.getInstance();
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(STORE_MANIFEST);

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            String output = "archive.zip";
            handler.zipWorkspace(output, SEDA_FILE);
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(output);

            storageClient.storeFileFromWorkspace(
                DEFAULT_STRATEGY,
                StorageCollectionType.DIP,
                params.getContainerName(), description);

            itemStatus.increment(StatusCode.OK);
        } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
            StorageServerClientException | ContentAddressableStorageException e) {
            throw new ProcessingException(e);
        }
        return new ItemStatus(STORE_MANIFEST).setItemsStatus(STORE_MANIFEST, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {

    }

}
