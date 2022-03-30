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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Stores MetaData object group plugin.
 */
public class StoreMetaDataObjectGroupActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StoreMetaDataObjectGroupActionPlugin.class);

    private static final String JSON = ".json";
    private static final String OG_METADATA_STORAGE = "OG_METADATA_STORAGE";

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final StorageClientFactory storageClientFactory;

    public StoreMetaDataObjectGroupActionPlugin() {
        this(MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public StoreMetaDataObjectGroupActionPlugin(MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        StorageClientFactory storageClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters params, HandlerIO handlerIO) {

        List<String> objectGroupIds = params.getObjectNameList().stream()
            .map(metadataFilename -> StringUtils.substringBeforeLast(metadataFilename, "."))
            .collect(Collectors.toList());

        try {

            storeDocumentsWithLfc(params, handlerIO, objectGroupIds);

            return this.getItemStatuses(objectGroupIds, StatusCode.OK);

        } catch (VitamException e) {
            LOGGER.error("An error occurred during object group storage", e);
            return this.getItemStatuses(objectGroupIds, StatusCode.FATAL);
        }
    }

    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
    }

    public void storeDocumentsWithLfc(WorkerParameters params, HandlerIO handlerIO, List<String> objectGroupIds)
        throws VitamException {
        MultiValuedMap<String, String> objectGroupIdsByStrategie =
            saveDocumentWithLfcInWorkspace(handlerIO, objectGroupIds);

        saveDocumentWithLfcInStorage(params.getContainerName(), objectGroupIdsByStrategie);
    }

    private MultiValuedMap<String, String> saveDocumentWithLfcInWorkspace(HandlerIO handlerIO,
        List<String> objectGroupIds)
        throws VitamException {

        // Get metadata
        Stopwatch loadGOT = Stopwatch.createStarted();
        MultiValuedMap<String, String> objectGroupIdsByStrategie = new HashSetValuedHashMap<>();
        Map<String, JsonNode> gots = getObjectGroupsByIdsRaw(objectGroupIds);
        for (JsonNode got : gots.values()) {
            MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(got);
            String strategyId = MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(got);
            objectGroupIdsByStrategie.put(strategyId, got.get("_id").asText());
        }
        PerformanceLogger.getInstance()
            .log("STP_OG_STORING", "OG_METADATA_STORAGE", "loadGOT", loadGOT.elapsed(TimeUnit.MILLISECONDS));

        // Get lfc
        Stopwatch loadLFC = Stopwatch.createStarted();
        Map<String, JsonNode> lfc = getRawLogbookLifeCycleByIds(objectGroupIds);
        PerformanceLogger.getInstance()
            .log("STP_OG_STORING", "OG_METADATA_STORAGE", "loadLFC", loadLFC.elapsed(TimeUnit.MILLISECONDS));

        for (String guid : objectGroupIds) {

            //// create file for storage (in workspace or temp or memory)
            JsonNode docWithLfc = MetadataStorageHelper.getGotWithLFC(gots.get(guid), lfc.get(guid));
            // transfer json to workspace
            final String fileName = guid + JSON;

            try {
                InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
                Stopwatch storeWorkspace = Stopwatch.createStarted();

                handlerIO.transferInputStreamToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + fileName,
                    is, null, false);

                PerformanceLogger.getInstance().log("STP_OG_STORING", "OG_METADATA_STORAGE", "storeWorkspace",
                    storeWorkspace.elapsed(TimeUnit.MILLISECONDS));
            } catch (ProcessingException e) {
                throw new WorkspaceClientServerException("Could not backup file for " + guid, e);
            }
        }
        return objectGroupIdsByStrategie;
    }

    private Map<String, JsonNode> getObjectGroupsByIdsRaw(List<String> documentIds)
        throws VitamException {


        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupsByIdsRaw(documentIds);
            if (!requestResponse.isOk()) {
                throw new ProcessingException("Documents not found " + documentIds);
            }
            List<JsonNode> results = ((RequestResponseOK<JsonNode>) requestResponse).getResults();
            if (results.size() != documentIds.size()) {
                throw new ProcessingException("Documents not found " + documentIds);
            }

            return mapById(results);
        }
    }

    private Map<String, JsonNode> getRawLogbookLifeCycleByIds(List<String> documentIds)
        throws VitamException {

        try (LogbookLifeCyclesClient logbookClient = logbookLifeCyclesClientFactory.getClient()) {

            List<JsonNode> results = logbookClient.getRawObjectGroupLifeCycleByIds(documentIds);
            return mapById(results);
        }
    }

    private void saveDocumentWithLfcInStorage(String containerName,
        MultiValuedMap<String, String> objectGroupIdsByStrategies)
        throws VitamException {

        Stopwatch storeStorage = Stopwatch.createStarted();

        for (String strategy : objectGroupIdsByStrategies.keySet()) {

            List<String> workspaceURIs = new ArrayList<>();
            List<String> objectNames = new ArrayList<>();

            Collection<String> objectGroupIds = objectGroupIdsByStrategies.get(strategy);

            for (String objectGroupId : objectGroupIds) {

                String filename = objectGroupId + JSON;
                String workspaceURI = IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + filename;

                workspaceURIs.add(workspaceURI);
                objectNames.add(filename);
            }

            BulkObjectStoreRequest request = new BulkObjectStoreRequest(
                containerName, workspaceURIs, DataCategory.OBJECTGROUP, objectNames);

            try (StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.bulkStoreFilesFromWorkspace(strategy, request);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException e) {
                LOGGER.debug("Bulk storage failed for strategy {}", strategy);
                throw new ProcessingException("Bulk storage failed", e);
            }
        }

        PerformanceLogger.getInstance().log("STP_OG_STORING", "OG_METADATA_STORAGE", "storeStorage",
            storeStorage.elapsed(TimeUnit.MILLISECONDS));
    }

    private List<ItemStatus> getItemStatuses(List<String> objectGroupIds, StatusCode statusCode) {
        List<ItemStatus> itemStatuses = new ArrayList<>();
        for (int i = 0; i < objectGroupIds.size(); i++) {
            itemStatuses.add(buildItemStatus(OG_METADATA_STORAGE, statusCode, null));
        }
        return itemStatuses;
    }

    private Map<String, JsonNode> mapById(List<JsonNode> results) {
        return results.stream()
            .collect(Collectors.toMap(
                entry -> entry.get(MetadataDocument.ID).textValue(),
                entry -> entry
            ));
    }
}
