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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.IllegalUnitTypeAttachment;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.UnitGraphInfo;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.GraphCycleDetector;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.UnitGraphInfoLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Reclassification graph check handler.
 */
public class ReclassificationPreparationCheckGraphHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ReclassificationPreparationCheckGraphHandler.class
    );

    private static final String RECLASSIFICATION_PREPARATION_CHECK_GRAPH = "RECLASSIFICATION_PREPARATION_CHECK_GRAPH";
    private static final int RECLASSIFICATION_ORDERS_PARAMETER_RANK = 0;

    static final String COULD_NOT_LOAD_UNITS = "Could not load units";
    static final String CANNOT_APPLY_RECLASSIFICATION_REQUEST_CYCLE_DETECTED =
        "Cannot apply reclassification request. Cycle detected";
    static final String INVALID_UNIT_TYPE_ATTACHMENTS = "Invalid unit type attachment(s)";

    private final int maxGuildListSizeInLogbookOperation;
    private final MetaDataClientFactory metaDataClientFactory;
    private final UnitGraphInfoLoader unitGraphInfoLoader;

    /**
     * Default constructor
     */
    public ReclassificationPreparationCheckGraphHandler() {
        this(
            MetaDataClientFactory.getInstance(),
            new UnitGraphInfoLoader(),
            VitamConfiguration.getReclassificationMaxGuildListSizeInLogbookOperation()
        );
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    ReclassificationPreparationCheckGraphHandler(
        MetaDataClientFactory metaDataClientFactory,
        UnitGraphInfoLoader unitGraphInfoLoader,
        int maxGuildListSizeInLogbookOperation
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.unitGraphInfoLoader = unitGraphInfoLoader;
        this.maxGuildListSizeInLogbookOperation = maxGuildListSizeInLogbookOperation;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            // Load / parse & validate request
            ReclassificationOrders reclassificationOrders = loadReclassificationOrders(handler);

            // Check graph (check unit types & graph cycles)
            checkGraphCoherence(reclassificationOrders);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Reclassification graph check failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(RECLASSIFICATION_PREPARATION_CHECK_GRAPH, e.getStatusCode(), e.getEventDetails());
        }

        LOGGER.info("Reclassification graph check succeeded");

        return buildItemStatus(RECLASSIFICATION_PREPARATION_CHECK_GRAPH, StatusCode.OK, null);
    }

    private ReclassificationOrders loadReclassificationOrders(HandlerIO handler) {
        return (ReclassificationOrders) handler.getInput(RECLASSIFICATION_ORDERS_PARAMETER_RANK);
    }

    private void checkGraphCoherence(ReclassificationOrders reclassificationUpdates) throws ProcessingStatusException {
        // Load all units & their parents recursively
        Map<String, UnitGraphInfo> unitGraphByIds = loadAllUnitGraphByIds(reclassificationUpdates);

        // Check unit type coherence
        checkAttachmentUnitTypeCoherence(reclassificationUpdates, unitGraphByIds);

        // Check cycles
        checkCycles(reclassificationUpdates, unitGraphByIds);
    }

    private Map<String, UnitGraphInfo> loadAllUnitGraphByIds(ReclassificationOrders reclassificationOrders)
        throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            Set<String> allUnitIds = getAllUnitIds(reclassificationOrders);

            Map<String, UnitGraphInfo> result = unitGraphInfoLoader.selectAllUnitGraphByIds(metaDataClient, allUnitIds);

            Set<String> notFoundUnits = result
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            if (!notFoundUnits.isEmpty()) {
                Set<String> firstNotFoundUnits = notFoundUnits
                    .stream()
                    .limit(maxGuildListSizeInLogbookOperation)
                    .collect(Collectors.toSet());

                ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                    .setError(COULD_NOT_LOAD_UNITS)
                    .setNotFoundUnits(firstNotFoundUnits);
                throw new ProcessingStatusException(StatusCode.FATAL, eventDetails, COULD_NOT_LOAD_UNITS);
            }

            return result;
        } catch (
            InvalidCreateOperationException
            | InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load unit graph information", e);
        }
    }

    private Set<String> getAllUnitIds(ReclassificationOrders reclassificationOrders) {
        Set<String> result = new HashSet<>();
        result.addAll(reclassificationOrders.getChildToParentAttachments().keySet());
        result.addAll(reclassificationOrders.getChildToParentAttachments().values());
        result.addAll(reclassificationOrders.getChildToParentDetachments().keySet());
        result.addAll(reclassificationOrders.getChildToParentDetachments().values());
        return result;
    }

    private void checkAttachmentUnitTypeCoherence(
        ReclassificationOrders reclassificationOrders,
        Map<String, UnitGraphInfo> unitGraphByIds
    ) throws ProcessingStatusException {
        List<IllegalUnitTypeAttachment> illegalUnitTypeAttachments = new ArrayList<>();

        for (Map.Entry<String, String> entry : reclassificationOrders.getChildToParentAttachments().entries()) {
            String childUnitId = entry.getKey();
            String parentUnitId = entry.getValue();
            UnitType childUnitType = unitGraphByIds.get(childUnitId).getUnitType();
            UnitType parentUnitType = unitGraphByIds.get(parentUnitId).getUnitType();

            if (childUnitType.ordinal() > parentUnitType.ordinal()) {
                illegalUnitTypeAttachments.add(
                    new IllegalUnitTypeAttachment(childUnitId, childUnitType, parentUnitId, parentUnitType)
                );
            }
        }

        if (!illegalUnitTypeAttachments.isEmpty()) {
            String error = INVALID_UNIT_TYPE_ATTACHMENTS;

            List<IllegalUnitTypeAttachment> firstIllegalUnitTypeAttachments = illegalUnitTypeAttachments
                .stream()
                .limit(maxGuildListSizeInLogbookOperation)
                .collect(Collectors.toList());

            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                .setError(error)
                .setIllegalUnitTypeAttachments(firstIllegalUnitTypeAttachments);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, error);
        }
    }

    private void checkCycles(ReclassificationOrders reclassificationOrders, Map<String, UnitGraphInfo> unitGraphByIds)
        throws ProcessingStatusException {
        GraphCycleDetector graphCycleDetector = new GraphCycleDetector();

        for (UnitGraphInfo unitGraph : unitGraphByIds.values()) {
            String unitId = unitGraph.getId();

            // Add current graph parents
            graphCycleDetector.addRelations(unitId, unitGraph.getUp());

            // Add attachments / detachments
            graphCycleDetector.addRelations(unitId, reclassificationOrders.getChildToParentAttachments().get(unitId));
            graphCycleDetector.removeRelations(
                unitId,
                reclassificationOrders.getChildToParentDetachments().get(unitId)
            );
        }

        Set<String> graphCycles = graphCycleDetector.checkCycles();

        if (!graphCycles.isEmpty()) {
            Set<String> firstGraphCycles = graphCycles
                .stream()
                .limit(maxGuildListSizeInLogbookOperation)
                .collect(Collectors.toSet());

            String error = CANNOT_APPLY_RECLASSIFICATION_REQUEST_CYCLE_DETECTED;
            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                .setError(error)
                .setUnitsWithCycles(firstGraphCycles);

            throw new ProcessingStatusException(StatusCode.KO, eventDetails, error);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return RECLASSIFICATION_PREPARATION_CHECK_GRAPH;
    }
}
