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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class PreservationStorageMetadataAndLfc extends StoreMetadataObjectActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationStorageMetadataAndLfc.class);

    private static final String JSON = ".json";
    private static final String PRESERVATION_STORAGE_METADATA_LFC = "PRESERVATION_STORAGE_METADATA_LFC";
    private static final int WORKFLOWBATCHRESULTS_IN_MEMORY = 0;

    private static final boolean ASYNC_IO = false;

    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    public PreservationStorageMetadataAndLfc() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance());
    }

    @VisibleForTesting
    PreservationStorageMetadataAndLfc(MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        final ItemStatus itemStatus = new ItemStatus(PRESERVATION_STORAGE_METADATA_LFC);
        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(WORKFLOWBATCHRESULTS_IN_MEMORY);
        List<ItemStatus> itemStatuses = new ArrayList<>();
        List<WorkflowBatchResult> workflowBatchResults = results.getWorkflowBatchResults();

        for (WorkflowBatchResult result : workflowBatchResults) {
            List<WorkflowBatchResult.OutputExtra> outputExtras = result.getOutputExtras().stream()
                .filter(WorkflowBatchResult.OutputExtra::isOkAndGenerated)
                .collect(Collectors.toList());

            if (outputExtras.isEmpty()) {
                itemStatuses.add(new ItemStatus(PRESERVATION_STORAGE_METADATA_LFC));
                continue;
            }
            itemStatuses.add(saveDocumentWithLfcInStorage(result.getGotId(), handler, param.getContainerName(), itemStatus));
        }
        return itemStatuses;
    }

    private ItemStatus saveDocumentWithLfcInStorage(String guid, HandlerIO handlerIO, String containerName,
        ItemStatus itemStatus) {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookClient = logbookLifeCyclesClientFactory.getClient()) {
            JsonNode got = selectMetadataDocumentRawById(guid, DataCategory.OBJECTGROUP, metaDataClient);
            MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(got);
            JsonNode lfc = getRawLogbookLifeCycleById(guid, DataCategory.OBJECTGROUP, logbookClient);
            JsonNode docWithLfc = MetadataStorageHelper.getGotWithLFC(got, lfc);
            final String fileName = guid + JSON;
            transferToWorkSpace(guid, handlerIO, docWithLfc, fileName);
            final ObjectDescription description =
                new ObjectDescription(DataCategory.OBJECTGROUP, containerName,
                    fileName, IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + fileName);
            storeObject(description, itemStatus);
            return buildItemStatus(PRESERVATION_STORAGE_METADATA_LFC, OK,
                EventDetails.of(String.format("Storage %s", guid)));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(PRESERVATION_STORAGE_METADATA_LFC, FATAL, e);
        }
    }

    private void transferToWorkSpace(String guid, HandlerIO handlerIO, JsonNode docWithLfc, String fileName)
        throws WorkspaceClientServerException {
        try {
            InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
            handlerIO
                .transferInputStreamToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + fileName, is, null,
                    ASYNC_IO);
        } catch (ProcessingException e) {
            LOGGER.error("Error when save file to workspace" + guid, e);
            throw new WorkspaceClientServerException(e);
        }
    }
}
