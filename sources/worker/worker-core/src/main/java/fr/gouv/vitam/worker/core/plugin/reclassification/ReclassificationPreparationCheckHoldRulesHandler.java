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
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
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
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.UnitGraphInfoLoader;
import fr.gouv.vitam.worker.core.utils.HoldRuleUtils;
import org.apache.commons.collections4.SetUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Reclassification hold rule check handler.
 */
public class ReclassificationPreparationCheckHoldRulesHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ReclassificationPreparationCheckHoldRulesHandler.class
    );

    private static final String RECLASSIFICATION_PREPARATION_CHECK_HOLD_RULES =
        "RECLASSIFICATION_PREPARATION_CHECK_HOLD_RULES";
    static final int RECLASSIFICATION_ORDERS_PARAMETER_RANK = 0;

    static final String COULD_NOT_FIND_UNITS_INHERITED_RULES = "Could not find units inherited rules";
    static final String RECLASSIFICATION_BLOCKED_BY_HOLD_RULES =
        "Cannot apply reclassification request. Hold rules with PreventRearrangement found";

    private final int maxGuidListSizeInLogbookOperation;
    private final MetaDataClientFactory metaDataClientFactory;
    private final UnitGraphInfoLoader unitGraphInfoLoader;

    /**
     * Default constructor
     */
    public ReclassificationPreparationCheckHoldRulesHandler() {
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
    ReclassificationPreparationCheckHoldRulesHandler(
        MetaDataClientFactory metaDataClientFactory,
        UnitGraphInfoLoader unitGraphInfoLoader,
        int maxGuidListSizeInLogbookOperation
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.unitGraphInfoLoader = unitGraphInfoLoader;
        this.maxGuidListSizeInLogbookOperation = maxGuidListSizeInLogbookOperation;
    }

    private static boolean isPreventRearrangementEnabled(InheritedRuleResponseModel rule) {
        Object preventRearrangementAttribute = rule.getExtendedRuleAttributes().get(RuleModel.PREVENT_REARRANGEMENT);
        if (preventRearrangementAttribute == null) {
            // Default to false
            return false;
        }
        if (!(preventRearrangementAttribute instanceof Boolean)) {
            throw new IllegalStateException("Expected PreventRearrangement to be a Boolean type");
        }
        return (boolean) preventRearrangementAttribute;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            // Load / parse & validate request
            ReclassificationOrders reclassificationOrders = loadReclassificationOrders(handler);

            // Check hold rules
            checkHoldRules(reclassificationOrders);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Reclassification hold rule check failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(
                RECLASSIFICATION_PREPARATION_CHECK_HOLD_RULES,
                e.getStatusCode(),
                e.getEventDetails()
            );
        }

        LOGGER.info("Reclassification hold rule check succeeded");

        return buildItemStatus(RECLASSIFICATION_PREPARATION_CHECK_HOLD_RULES, StatusCode.OK, null);
    }

    private ReclassificationOrders loadReclassificationOrders(HandlerIO handler) {
        return (ReclassificationOrders) handler.getInput(RECLASSIFICATION_ORDERS_PARAMETER_RANK);
    }

    private void checkHoldRules(ReclassificationOrders reclassificationOrders) throws ProcessingStatusException {
        Set<String> unitsIdToRearrange = SetUtils.union(
            reclassificationOrders.getChildToParentAttachments().keySet(),
            reclassificationOrders.getChildToParentDetachments().keySet()
        );

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            Map<String, InheritedRuleCategoryResponseModel> unitInheritedHoldRules =
                unitGraphInfoLoader.loadInheritedHoldRules(metaDataClient, unitsIdToRearrange);

            // Ensure all unit rules loaded
            checkNotFoundUnits(unitsIdToRearrange, unitInheritedHoldRules.keySet());

            // Check active hold rules with Prevent Ih
            checkActiveHoldRuleRearrangement(unitInheritedHoldRules);
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

    private void checkNotFoundUnits(Set<String> unitIds, Set<String> foundUnitIds) throws ProcessingStatusException {
        Set<String> notFoundUnits = SetUtils.difference(unitIds, foundUnitIds);

        if (!notFoundUnits.isEmpty()) {
            Set<String> firstNotFoundUnits = notFoundUnits
                .stream()
                .limit(maxGuidListSizeInLogbookOperation)
                .collect(Collectors.toSet());

            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                .setError(COULD_NOT_FIND_UNITS_INHERITED_RULES)
                .setNotFoundUnits(firstNotFoundUnits);
            throw new ProcessingStatusException(StatusCode.FATAL, eventDetails, COULD_NOT_FIND_UNITS_INHERITED_RULES);
        }
    }

    private void checkActiveHoldRuleRearrangement(
        Map<String, InheritedRuleCategoryResponseModel> unitInheritedHoldRules
    ) throws ProcessingStatusException {
        Set<String> unitIdsBlockedByHoldRules = unitInheritedHoldRules
            .entrySet()
            .stream()
            .filter(entry -> hasActiveHoldRulesWithPreventRearrangement(entry.getKey(), entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        if (unitIdsBlockedByHoldRules.isEmpty()) {
            LOGGER.info("No HoldRules with PreventRearrangement found");
            return;
        }

        ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
            .setError(RECLASSIFICATION_BLOCKED_BY_HOLD_RULES)
            .setUnitsBlockedByHoldRules(unitIdsBlockedByHoldRules);

        throw new ProcessingStatusException(StatusCode.KO, eventDetails, RECLASSIFICATION_BLOCKED_BY_HOLD_RULES);
    }

    private boolean hasActiveHoldRulesWithPreventRearrangement(
        String unitId,
        InheritedRuleCategoryResponseModel inheritedHoldRules
    ) {
        final LocalDate today = LocalDate.now();
        Set<InheritedRuleResponseModel> activeHoldRules = HoldRuleUtils.listActiveHoldRules(
            unitId,
            inheritedHoldRules.getRules(),
            today
        );

        if (activeHoldRules.isEmpty()) {
            LOGGER.debug("No active hold rules found for unit " + unitId);
            return false;
        }

        Set<String> activeHoldRulesWithPreventRearrangement = activeHoldRules
            .stream()
            .filter(ReclassificationPreparationCheckHoldRulesHandler::isPreventRearrangementEnabled)
            .map(InheritedRuleResponseModel::getRuleId)
            .collect(Collectors.toSet());

        if (activeHoldRulesWithPreventRearrangement.isEmpty()) {
            LOGGER.debug("No active hold rules with PreventRearrangement found for unit " + unitId);
            return false;
        }

        LOGGER.warn(
            "Active hold rules with PreventRearrangement found for unit " +
            unitId +
            ": " +
            activeHoldRulesWithPreventRearrangement
        );
        return true;
    }

    public static String getId() {
        return RECLASSIFICATION_PREPARATION_CHECK_HOLD_RULES;
    }
}
