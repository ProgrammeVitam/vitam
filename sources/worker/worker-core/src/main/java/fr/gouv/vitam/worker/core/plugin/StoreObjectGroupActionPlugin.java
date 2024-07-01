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
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StoreObjectGroupAction Plugin.<br>
 */
public class StoreObjectGroupActionPlugin extends StoreObjectActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreObjectGroupActionPlugin.class);

    public static final String STORING_OBJECT_TASK_ID = "OBJECT_STORAGE_SUB_TASK";

    private static final String SIP = "SIP/";
    private static final int OG_OUT_RANK = 0;

    public StoreObjectGroupActionPlugin() {
        this(StorageClientFactory.getInstance());
    }

    public StoreObjectGroupActionPlugin(StorageClientFactory storageClientFactory) {
        super(storageClientFactory);
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);

        final List<ItemStatus> itemStatusList = new ArrayList<>();
        final List<Map<String, ItemStatus>> itemStatusByObjectList = new ArrayList<>();

        try {
            checkMandatoryIOParameter(handlerIO);

            Set<String> strategies = new LinkedHashSet<>();
            Map<String, String> strategiesByObjectId = new HashMap<>();
            ListValuedMap<String, String> workspaceObjectURIsByStrategies = new ArrayListValuedHashMap<>();
            ListValuedMap<String, String> objectNamesByStrategies = new ArrayListValuedHashMap<>();
            List<MapOfObjects> mapOfObjectsList = new ArrayList<>();

            // get list of object group's objects
            for (String objectName : params.getObjectNameList()) {
                MapOfObjects mapOfObjects = getMapOfObjectsIdsAndUris(params.getContainerName(), objectName, handlerIO);
                Map<String, ItemStatus> itemStatusByObject = new HashMap<>();
                for (final Map.Entry<String, String> objectGuid : mapOfObjects.getBinaryObjectsToStore().entrySet()) {
                    itemStatusByObject.put(objectGuid.getKey(), new ItemStatus(STORING_OBJECT_TASK_ID));
                    String strategyId = mapOfObjects
                        .getObjectStorageInfos()
                        .get(objectGuid.getKey())
                        .get("strategyId")
                        .asText();
                    strategies.add(strategyId);
                    workspaceObjectURIsByStrategies.put(strategyId, SIP + objectGuid.getValue());
                    objectNamesByStrategies.put(strategyId, objectGuid.getKey());
                    strategiesByObjectId.put(objectGuid.getKey(), strategyId);
                }

                itemStatusByObjectList.add(itemStatusByObject);
                itemStatusList.add(new ItemStatus(STORING_OBJECT_TASK_ID));
                mapOfObjectsList.add(mapOfObjects);

                // get list of object uris
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Pre OG: {}", JsonHandler.prettyPrint(mapOfObjects.getJsonOG()));
                }
            }

            if (objectNamesByStrategies.values().isEmpty()) {
                return Arrays.asList(
                    new ItemStatus(STORING_OBJECT_TASK_ID).setItemsStatus(
                        STORING_OBJECT_TASK_ID,
                        new ItemStatus().increment(StatusCode.OK)
                    )
                );
            }

            Map<String, BulkObjectStoreResponse> resultByStrategy = new LinkedHashMap<>();

            for (String strategy : strategies) {
                List<String> workspaceObjectURIs = workspaceObjectURIsByStrategies.get(strategy);
                List<String> objectNames = objectNamesByStrategies.get(strategy);

                // store objects
                BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
                    params.getContainerName(),
                    workspaceObjectURIs,
                    DataCategory.OBJECT,
                    objectNames
                );
                BulkObjectStoreResponse result = storeObjects(strategy, bulkObjectStoreRequest);
                resultByStrategy.put(strategy, result);
            }

            // update sub task itemStatus
            updateSubTasksAndTasksFromStorageInfos(resultByStrategy, itemStatusByObjectList, itemStatusList);

            // separate by strategy
            storeStorageInfos(mapOfObjectsList, resultByStrategy, strategiesByObjectId);

            for (int i = 0; i < mapOfObjectsList.size(); i++) {
                handlerIO.transferJsonToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER,
                    params.getObjectNameList().get(i),
                    mapOfObjectsList.get(i).getJsonOG(),
                    false,
                    false
                );
            }
        } catch (final ProcessingException e) {
            LOGGER.error(params.getObjectName(), e);
            return Arrays.asList(
                new ItemStatus(STORING_OBJECT_TASK_ID).setItemsStatus(
                    STORING_OBJECT_TASK_ID,
                    new ItemStatus().increment(StatusCode.FATAL)
                )
            );
        } catch (StorageClientException e) {
            LOGGER.error(e);
            return Arrays.asList(
                new ItemStatus(STORING_OBJECT_TASK_ID).setItemsStatus(
                    STORING_OBJECT_TASK_ID,
                    new ItemStatus().increment(StatusCode.FATAL)
                )
            );
        }

        return getFinalResult(itemStatusList);
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    /**
     * Get the list of objects linked to the current object group
     *
     * @param containerId
     * @param objectName
     * @return the list of object guid and corresponding Json
     * @throws ProcessingException throws when error occurs while retrieving the object group file from workspace
     */
    private MapOfObjects getMapOfObjectsIdsAndUris(String containerId, String objectName, HandlerIO handlerIO)
        throws ProcessingException {
        final MapOfObjects mapOfObjects = new MapOfObjects();
        mapOfObjects.setBinaryObjectsToStore(new HashMap<>());
        mapOfObjects.setObjectJsonMap(new HashMap<>());
        mapOfObjects.setObjectStorageInfos(new HashMap<>());
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        // Get objectGroup objects ids
        mapOfObjects.setJsonOG(
            handlerIO.getJsonFromWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectName)
        );
        handlerIO.setCurrentObjectId(objectName);
        handlerIO.addOutputResult(OG_OUT_RANK, mapOfObjects.getJsonOG(), true, false);

        // Filter on objectGroup objects ids to retrieve only binary objects
        // informations linked to the ObjectGroup
        final JsonNode original = mapOfObjects.getJsonOG().get(SedaConstants.PREFIX_QUALIFIERS);
        final JsonNode work = mapOfObjects.getJsonOG().get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            return mapOfObjects;
        }

        final List<JsonNode> originalVersions = original.findValues(SedaConstants.TAG_VERSIONS);
        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            return mapOfObjects;
        }
        for (final JsonNode version : versions) {
            for (final JsonNode binaryObject : version) {
                if (binaryObject.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                    String id = binaryObject.get(SedaConstants.PREFIX_ID).asText();
                    mapOfObjects.getBinaryObjectsToStore().put(id, binaryObject.get(SedaConstants.TAG_URI).asText());
                    for (final JsonNode version2 : originalVersions) {
                        for (final JsonNode binaryObject2 : version2) {
                            if (
                                binaryObject2.get(SedaConstants.TAG_PHYSICAL_ID) == null &&
                                binaryObject2.get(SedaConstants.PREFIX_ID).asText().equals(id)
                            ) {
                                mapOfObjects.getObjectJsonMap().put(id, binaryObject2);
                                mapOfObjects.getObjectStorageInfos().put(id, binaryObject.get(SedaConstants.STORAGE));
                            }
                        }
                    }
                }
            }
        }
        return mapOfObjects;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add objectGroup.json add input and check it
    }

    private List<ItemStatus> getFinalResult(List<ItemStatus> itemStatusList) {
        List<ItemStatus> finalItemStatusList = new ArrayList<>();
        for (ItemStatus itemStatus : itemStatusList) {
            finalItemStatusList.add(
                new ItemStatus(STORING_OBJECT_TASK_ID).setItemsStatus(STORING_OBJECT_TASK_ID, itemStatus)
            );
        }
        return finalItemStatusList;
    }
}
