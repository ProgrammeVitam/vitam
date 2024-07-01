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
package fr.gouv.vitam.worker.core.plugin.massprocessing.management;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.massupdate.ManagementMetadataAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.massprocessing.MassUpdateErrorInfo;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.VitamConstants.TAG_RULE_CLASSIFICATION;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class MassUpdateRulesCheck extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);
    private static final String PLUGIN_NAME = "MASS_UPDATE_RULES_CHECK";

    private final AlertService alertService;
    private final List<String> classificationLevels;

    public MassUpdateRulesCheck() {
        this(new AlertServiceImpl(), VitamConfiguration.getClassificationLevel().getAllowList());
    }

    @VisibleForTesting
    public MassUpdateRulesCheck(AlertService alertService, List<String> classificationLevels) {
        this.alertService = alertService;
        this.classificationLevels = classificationLevels;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        try {
            RuleActions ruleActions = JsonHandler.getFromJsonNode(
                handler.getJsonFromWorkspace("actions.json"),
                RuleActions.class
            );
            if (ruleActions.isRuleActionsEmpty()) {
                MassUpdateErrorInfo errorInfo = new MassUpdateErrorInfo(
                    "RULE_ACTION_EMPTY",
                    "Rule actions used to update units is empty."
                );
                return buildItemStatus(PLUGIN_NAME, KO, errorInfo);
            }

            List<String> duplicateKeys = getDuplicateKeys(ruleActions);
            if (!duplicateKeys.isEmpty()) {
                String message = String.format(
                    "Invalid rule actions query: duplicate rules '%s'.",
                    String.join(", ", duplicateKeys)
                );
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            boolean deleteOrAddSameAUP = isTryingToAddAndDeleteAUP(
                ruleActions.getAddOrUpdateMetadata(),
                ruleActions.getDeleteMetadata()
            );
            if (deleteOrAddSameAUP) {
                String message = String.format(
                    "Invalid AUP query: duplicate in add or delete '%s'.",
                    ruleActions.getAddOrUpdateMetadata().getArchiveUnitProfile()
                );
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            List<String> unknownAddClassificationLevels = ruleActions
                .getAdd()
                .stream()
                .filter(rule -> Objects.nonNull(rule.get(TAG_RULE_CLASSIFICATION)))
                .map(rule -> rule.get(TAG_RULE_CLASSIFICATION).getClassificationLevel())
                .filter(Objects::nonNull)
                .filter(classificationLevel -> !classificationLevels.contains(classificationLevel))
                .collect(Collectors.toList());

            if (!unknownAddClassificationLevels.isEmpty()) {
                String message = String.format(
                    "Unknown classification level '%s' in added rule action.",
                    String.join(", ", unknownAddClassificationLevels)
                );
                alertService.createAlert(message);
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            List<String> unknownUpdateClassificationLevels = ruleActions
                .getUpdate()
                .stream()
                .filter(rule -> Objects.nonNull(rule.get(TAG_RULE_CLASSIFICATION)))
                .map(rule -> rule.get(TAG_RULE_CLASSIFICATION).getClassificationLevel())
                .filter(Objects::nonNull)
                .filter(classificationLevel -> !classificationLevels.contains(classificationLevel))
                .collect(Collectors.toList());

            if (!unknownUpdateClassificationLevels.isEmpty()) {
                String message = String.format(
                    "Unknown classification level '%s' in updated rule action.",
                    String.join(", ", unknownUpdateClassificationLevels)
                );
                alertService.createAlert(message);
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            if (numberOfInCorrectDeleteRule(ruleActions) > 0) {
                final String message = "You can not add preventRulesId and preventRulesIdToRemove at the same time";
                alertService.createAlert(message);
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            if (numberOfInCorrectAddRule(ruleActions) > 0) {
                final String message = "You can not add preventRulesId and preventRulesIdToAdd at the same time";
                alertService.createAlert(message);
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("Step OK."));
        } catch (VitamException e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, KO, EventDetails.of("Unexpected error."));
        }
    }

    private boolean isTryingToAddAndDeleteAUP(
        ManagementMetadataAction addOrUpdateMetadata,
        ManagementMetadataAction deleteMetadata
    ) {
        if (
            addOrUpdateMetadata == null ||
            addOrUpdateMetadata.getArchiveUnitProfile() == null ||
            deleteMetadata == null ||
            deleteMetadata.getArchiveUnitProfile() == null
        ) {
            return false;
        }
        return addOrUpdateMetadata.getArchiveUnitProfile().equalsIgnoreCase(deleteMetadata.getArchiveUnitProfile());
    }

    private List<String> getDuplicateKeys(RuleActions rules) {
        Set<String> foundKeys = new HashSet<>();
        return Stream.concat(
            Stream.concat(rules.getAdd().stream(), rules.getUpdate().stream()),
            rules.getDelete().stream()
        )
            .flatMap(map -> map.keySet().stream())
            .filter(key -> includeFoundedKey(foundKeys, key))
            .collect(Collectors.toList());
    }

    private boolean includeFoundedKey(Set<String> foundKeys, String key) {
        if (foundKeys.contains(key)) {
            return true;
        }
        foundKeys.add(key);
        return false;
    }

    private int numberOfInCorrectDeleteRule(RuleActions ruleActions) {
        AtomicInteger numberOfInCorrectDeleteRule = new AtomicInteger();
        ruleActions
            .getDelete()
            .forEach(ruleCategoryActionMap ->
                ruleCategoryActionMap
                    .keySet()
                    .forEach(ruleCategoryName -> {
                        if (
                            ruleCategoryActionMap.get(ruleCategoryName) != null &&
                            !CollectionUtils.isEmpty(
                                ruleCategoryActionMap.get(ruleCategoryName).getPreventRulesIdToRemove()
                            ) &&
                            !CollectionUtils.isEmpty(ruleCategoryActionMap.get(ruleCategoryName).getPreventRulesId())
                        ) {
                            numberOfInCorrectDeleteRule.getAndIncrement();
                        }
                    }));
        return numberOfInCorrectDeleteRule.get();
    }

    private int numberOfInCorrectAddRule(RuleActions ruleActions) {
        AtomicInteger numberOfInCorrectAddRule = new AtomicInteger();
        ruleActions
            .getAdd()
            .forEach(ruleCategoryActionMap ->
                ruleCategoryActionMap
                    .keySet()
                    .forEach(ruleCategoryName -> {
                        if (
                            ruleCategoryActionMap.get(ruleCategoryName) != null &&
                            !CollectionUtils.isEmpty(
                                ruleCategoryActionMap.get(ruleCategoryName).getPreventRulesIdToAdd()
                            ) &&
                            !CollectionUtils.isEmpty(ruleCategoryActionMap.get(ruleCategoryName).getPreventRulesId())
                        ) {
                            numberOfInCorrectAddRule.getAndIncrement();
                        }
                    }));
        return numberOfInCorrectAddRule.get();
    }
}
