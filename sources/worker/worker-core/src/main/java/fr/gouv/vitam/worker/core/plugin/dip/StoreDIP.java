/**
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
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;

import fr.gouv.vitam.common.VitamConfiguration;

/**
 * ZIP the dip and move it from workspace to storage
 */
public class StoreDIP extends ActionHandler {

    private static final String STORE_DIP = "STORE_DIP";
    public static final String ARCHIVE_ZIP = "archive.zip";
    public static final String CONTENT = "Content";

    /**
     * factory of a storage client
     */
    private final StorageClientFactory storageClientFactory;

    /**
     * default constructor
     */
    public StoreDIP() {
        storageClientFactory = StorageClientFactory.getInstance();
    }

    /**
     *
     * @param params
     * @param handler
     * @return
     * @throws ProcessingException
     * @throws ContentAddressableStorageServerException
     */
    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(STORE_DIP);

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            String output = ARCHIVE_ZIP;
            handler.zipWorkspace(output, SEDA_FILE, CONTENT);
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(output);

            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.DIP,
                params.getContainerName(), description);

            itemStatus.increment(StatusCode.OK);
        } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
            StorageServerClientException | ContentAddressableStorageException e) {
            throw new ProcessingException(e);
        }
        return new ItemStatus(STORE_DIP).setItemsStatus(STORE_DIP, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {

    }

}
