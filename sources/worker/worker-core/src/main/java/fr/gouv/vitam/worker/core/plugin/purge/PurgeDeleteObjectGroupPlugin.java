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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.Collections.singletonList;

/**
 * Purge delete object group plugin.
 */
public class PurgeDeleteObjectGroupPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PurgeDeleteObjectGroupPlugin.class);

    private final String actionId;
    private final PurgeDeleteService purgeDeleteService;

    /**
     * Default constructor
     *
     * @param actionId
     */
    public PurgeDeleteObjectGroupPlugin(String actionId) {
        this(actionId, new PurgeDeleteService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    protected PurgeDeleteObjectGroupPlugin(String actionId, PurgeDeleteService purgeDeleteService) {
        this.actionId = actionId;
        this.purgeDeleteService = purgeDeleteService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters param, HandlerIO handler) {
        try {
            List<PurgeObjectGroupParams> objectGroupParams = param
                .getObjectMetadataList()
                .stream()
                .map(og -> {
                    try {
                        return JsonHandler.getFromJsonNode(og, PurgeObjectGroupParams.class);
                    } catch (InvalidParseOperationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

            Map<String, String> objectGroupIdsWithStrategies = objectGroupParams
                .stream()
                .collect(Collectors.toMap(PurgeObjectGroupParams::getId, PurgeObjectGroupParams::getStrategyId));

            processObjectGroups(objectGroupIdsWithStrategies);

            Map<String, String> objectIdsWithStrategies = objectGroupParams
                .stream()
                .flatMap(objectGroup -> objectGroup.getObjects().stream())
                .collect(Collectors.toMap(PurgeObjectParams::getId, PurgeObjectParams::getStrategyId));

            processObjects(objectIdsWithStrategies);

            return buildBulkItemStatus(param, actionId, StatusCode.OK);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Purge of object groups failed with status " + e.getStatusCode(), e);
            return singletonList(buildItemStatus(actionId, e.getStatusCode(), e.getEventDetails()));
        }
    }

    private void processObjectGroups(Map<String, String> objectGroupIdsWithStrategies)
        throws ProcessingStatusException {
        LOGGER.info("Deleting object groups [" + String.join(", ", objectGroupIdsWithStrategies.keySet()) + "]");

        try {
            purgeDeleteService.deleteObjectGroups(objectGroupIdsWithStrategies);
        } catch (
            InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataClientServerException
            | LogbookClientBadRequestException
            | StorageServerClientException
            | LogbookClientServerException e
        ) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not delete object groups [" + String.join(", ", objectGroupIdsWithStrategies.keySet()) + "]",
                e
            );
        }
    }

    private void processObjects(Map<String, String> objectIdsWithStrategies) throws ProcessingStatusException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting object binaries [" + String.join(", ", objectIdsWithStrategies.keySet()) + "]");
        }

        try {
            purgeDeleteService.deleteObjects(objectIdsWithStrategies);
        } catch (StorageServerClientException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not delete object groups [" + String.join(", ", objectIdsWithStrategies.keySet()) + "]",
                e
            );
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }
}
