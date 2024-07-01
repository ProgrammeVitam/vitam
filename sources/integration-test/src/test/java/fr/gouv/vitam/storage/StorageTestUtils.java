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

package fr.gouv.vitam.storage;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class StorageTestUtils {

    public static Digest writeFileToOffers(String objectId, int size)
        throws ContentAddressableStorageServerException, StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException, ContentAddressableStorageNotFoundException {
        try (
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()
        ) {
            String container = GUIDFactory.newGUID().getId();
            workspaceClient.createContainer(container);

            Digest digest = new Digest(DigestType.SHA512);
            try (FakeInputStream fis = new FakeInputStream(size, true, true)) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
                workspaceClient.putObject(container, objectId, digest.getDigestInputStream(fis));
            }

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(container);
            description.setWorkspaceObjectURI(objectId);
            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECT,
                objectId,
                description
            );

            workspaceClient.deleteContainer(container, true);

            return digest;
        }
    }

    public static void deleteFile(String objectId) throws StorageServerClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            storageClient.delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, objectId);
        }
    }

    public static LogbookOperation getLogbookOperation(String operationId)
        throws LogbookClientException, InvalidParseOperationException {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode result = logbookClient.selectOperationById(operationId);
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(
                result
            );
            return JsonHandler.getFromJsonNode(
                logbookOperationVersionModelResponseOK.getFirstResult(),
                LogbookOperation.class
            );
        }
    }
}
