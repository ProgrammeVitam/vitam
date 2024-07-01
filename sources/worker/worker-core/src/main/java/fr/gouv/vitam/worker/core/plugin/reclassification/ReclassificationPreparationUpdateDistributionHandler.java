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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import org.apache.commons.collections4.SetUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Reclassification update distribution handler.
 */
public class ReclassificationPreparationUpdateDistributionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ReclassificationPreparationUpdateDistributionHandler.class
    );

    private static final String RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION =
        "RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION";
    private static final int RECLASSIFICATION_ORDERS_PARAMETER_RANK = 0;
    private static final String UNITS_TO_DETACH_DIR = "UnitsToDetach";
    private static final String UNITS_TO_ATTACH_DIR = "UnitsToAttach";
    private static final String UNITS_TO_UPDATE_JSONL_FILE = "UnitsToUpdate.jsonl";
    private static final String OG_TO_UPDATE_JSONL_FILE = "ObjectGroupsToUpdate.jsonl";

    private static final String COULD_NOT_EXPORT_THE_LIST_OF_UNITS_AND_OBJECT_GROUPS_TO_UPDATE =
        "Could not export the list of units and object groups to update";

    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * Default constructor
     */
    public ReclassificationPreparationUpdateDistributionHandler() {
        this(MetaDataClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    ReclassificationPreparationUpdateDistributionHandler(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            // Load / parse & validate request
            ReclassificationOrders reclassificationOrders = loadReclassificationOrders(handler);

            // Prepare distributions
            prepareUpdates(reclassificationOrders, handler);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Reclassification update distribution failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(
                RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION,
                e.getStatusCode(),
                e.getEventDetails()
            );
        }

        LOGGER.info("Reclassification update distribution succeeded");

        return buildItemStatus(RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION, StatusCode.OK, null);
    }

    private ReclassificationOrders loadReclassificationOrders(HandlerIO handler) {
        return (ReclassificationOrders) handler.getInput(RECLASSIFICATION_ORDERS_PARAMETER_RANK);
    }

    private void prepareUpdates(ReclassificationOrders reclassificationUpdates, HandlerIO handler)
        throws ProcessingStatusException {
        prepareDetachments(reclassificationUpdates, handler);

        prepareAttachments(reclassificationUpdates, handler);

        prepareUnitAndObjectGroupGraphUpdates(reclassificationUpdates);
    }

    private void prepareDetachments(ReclassificationOrders reclassificationOrders, HandlerIO handler)
        throws ProcessingStatusException {
        for (String childUnitId : reclassificationOrders.getChildToParentDetachments().keySet()) {
            storeToWorkspace(
                handler,
                reclassificationOrders.getChildToParentDetachments().get(childUnitId),
                UNITS_TO_DETACH_DIR + "/" + childUnitId
            );
        }
    }

    private void prepareAttachments(ReclassificationOrders reclassificationOrders, HandlerIO handler)
        throws ProcessingStatusException {
        for (String childUnitId : reclassificationOrders.getChildToParentAttachments().keySet()) {
            storeToWorkspace(
                handler,
                reclassificationOrders.getChildToParentAttachments().get(childUnitId),
                UNITS_TO_ATTACH_DIR + "/" + childUnitId
            );
        }
    }

    private void prepareUnitAndObjectGroupGraphUpdates(ReclassificationOrders reclassificationOrders)
        throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            Set<String> unitIds = SetUtils.union(
                reclassificationOrders.getChildToParentAttachments().keySet(),
                reclassificationOrders.getChildToParentDetachments().keySet()
            );

            metaDataClient.exportReclassificationChildNodes(
                unitIds,
                UNITS_TO_UPDATE_JSONL_FILE,
                OG_TO_UPDATE_JSONL_FILE
            );
        } catch (VitamClientException | MetaDataExecutionException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                COULD_NOT_EXPORT_THE_LIST_OF_UNITS_AND_OBJECT_GROUPS_TO_UPDATE,
                e
            );
        }
    }

    private void storeToWorkspace(HandlerIO handler, Object data, String filePath) throws ProcessingStatusException {
        try (InputStream inputStream = JsonHandler.writeToInpustream(data)) {
            handler.transferInputStreamToWorkspace(filePath, inputStream, null, false);
        } catch (InvalidParseOperationException | IOException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store to workspace: " + filePath, e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION;
    }
}
