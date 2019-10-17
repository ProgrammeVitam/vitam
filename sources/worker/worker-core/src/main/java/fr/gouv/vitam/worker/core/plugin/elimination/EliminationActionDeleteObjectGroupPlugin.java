/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.elimination;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * Elimination action delete object group plugin.
 */
public class EliminationActionDeleteObjectGroupPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionDeleteObjectGroupPlugin.class);

    private static final String ELIMINATION_ACTION_DELETE_OBJECT_GROUP = "ELIMINATION_ACTION_DELETE_OBJECT_GROUP";

    private final EliminationActionDeleteService eliminationActionDeleteService;

    /**
     * Default constructor
     */
    public EliminationActionDeleteObjectGroupPlugin() {
        this(new EliminationActionDeleteService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionDeleteObjectGroupPlugin(
        EliminationActionDeleteService eliminationActionDeleteService) {
        this.eliminationActionDeleteService = eliminationActionDeleteService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters param, HandlerIO handler) {

        try {

            
            Map<String, String> objectGroupIdsWithStrategies = new HashMap<String, String>();
            IntStream.range(0, param.getObjectNameList().size())
            .forEach(i -> objectGroupIdsWithStrategies.put(param.getObjectNameList().get(i),
                    param.getObjectMetadataList().get(i).get("strategyId").asText()));
            
            Map<String, String> objectIdsWithStrategies = loadObjectsToDelete(param);

            processObjectGroups(objectGroupIdsWithStrategies);

            processObjects(objectIdsWithStrategies);

            return buildBulkItemStatus(param, ELIMINATION_ACTION_DELETE_OBJECT_GROUP, StatusCode.OK);

        } catch (ProcessingStatusException e) {
            LOGGER.error("Elimination action delete object groups failed with status " + e.getStatusCode(), e);
            return singletonList(
                buildItemStatus(ELIMINATION_ACTION_DELETE_OBJECT_GROUP, e.getStatusCode(), e.getEventDetails()));
        }

    }

    private void processObjectGroups(Map<String, String> objectGroupIdsWithStrategies)
        throws ProcessingStatusException {

        LOGGER.info("Deleting object groups [" + String.join(", ", objectGroupIdsWithStrategies.keySet()) + "]");

        try {

            eliminationActionDeleteService.deleteObjectGroups(objectGroupIdsWithStrategies);

        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataClientServerException |
            LogbookClientBadRequestException | StorageServerClientException | LogbookClientServerException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not delete object groups [" + String.join(", ", objectGroupIdsWithStrategies.keySet()) + "]", e);
        }
    }

    private void processObjects(Map<String, String> objectIdsWithStrategies) throws ProcessingStatusException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting object binaries [" + String.join(", ", objectIdsWithStrategies.keySet()) + "]");
        }

        try {

            eliminationActionDeleteService.deleteObjects(objectIdsWithStrategies);

        } catch (StorageServerClientException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not delete object groups [" + String.join(", ", objectIdsWithStrategies.keySet()) + "]", e);
        }

    }

    private Map<String, String> loadObjectsToDelete(WorkerParameters param) throws ProcessingStatusException {

        Map<String, String> objectsWithStrategiesToDelete = new HashMap<String, String>();
        for (JsonNode jsonNode : param.getObjectMetadataList()) {
            if (jsonNode.has("objects") && !jsonNode.get("objects").isArray()) {
                throw new ProcessingStatusException(StatusCode.FATAL, "Could not retrieve object ids to delete");
            }
            ArrayNode objectDetails = (ArrayNode) jsonNode.get("objects");
            for (JsonNode objectDetail : objectDetails) {
                objectsWithStrategiesToDelete.put(objectDetail.get("id").asText(),
                        objectDetail.get("strategyId").asText());
            }
        }
        return objectsWithStrategiesToDelete;
        
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_DELETE_OBJECT_GROUP;
    }
}
