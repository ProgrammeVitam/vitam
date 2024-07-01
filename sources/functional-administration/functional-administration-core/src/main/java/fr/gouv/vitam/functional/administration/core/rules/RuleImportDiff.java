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
package fr.gouv.vitam.functional.administration.core.rules;

import fr.gouv.vitam.common.model.administration.FileRulesModel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RuleImportDiff {

    private final List<FileRulesModel> rulesToInsert;
    private final List<FileRulesModel> rulesToUpdateUnsafely;
    private final List<FileRulesModel> rulesToUpdate;
    private final List<FileRulesModel> rulesToDelete;
    private final List<FileRulesModel> rulesWithDurationModeUpdate;

    private RuleImportDiff() {
        this.rulesToInsert = new ArrayList<>();
        this.rulesToUpdateUnsafely = new ArrayList<>();
        this.rulesToDelete = new ArrayList<>();
        this.rulesToUpdate = new ArrayList<>();
        this.rulesWithDurationModeUpdate = new ArrayList<>();
    }

    RuleImportDiff(List<FileRulesModel> rulesFromFileIds, List<FileRulesModel> rulesInDatabaseIds) {
        this();
        getDiff(
            Optional.ofNullable(rulesFromFileIds).orElse(Collections.emptyList()),
            Optional.ofNullable(rulesInDatabaseIds).orElse(Collections.emptyList())
        );
    }

    RuleImportDiff(Map<String, FileRulesModel> rulesFromFile, Map<String, FileRulesModel> rulesInDatabase) {
        this();
        getDiff(
            Optional.ofNullable(rulesFromFile).orElse(Collections.emptyMap()),
            Optional.ofNullable(rulesInDatabase).orElse(Collections.emptyMap())
        );
    }

    private void getDiff(List<FileRulesModel> rulesFromFileIds, List<FileRulesModel> rulesInDatabaseIds) {
        Map<String, FileRulesModel> rulesInDatabase = rulesInDatabaseIds
            .stream()
            .collect(Collectors.toMap(FileRulesModel::getRuleId, Function.identity()));
        Map<String, FileRulesModel> rulesFromFile = rulesFromFileIds
            .stream()
            .collect(Collectors.toMap(FileRulesModel::getRuleId, Function.identity()));
        getDiff(rulesFromFile, rulesInDatabase);
    }

    private void getDiff(Map<String, FileRulesModel> rulesFromFile, Map<String, FileRulesModel> rulesInDatabase) {
        for (FileRulesModel rule : rulesFromFile.values()) {
            FileRulesModel ruleInDatabase = rulesInDatabase.get(rule.getRuleId());
            if (ruleInDatabase != null) {
                if (!ruleInDatabase.equals(rule)) {
                    addRuleToUpdate(rule);

                    if (hasRuleDurationChanged(ruleInDatabase, rule)) {
                        addRuleToUpdateUnsafely(rule);
                    }

                    if (
                        isUpdateOfOldRuleWithoutDurationToNewRuleWithDuration(ruleInDatabase, rule) ||
                        isUpdateOfOldRuleWithDurationToNewRuleWithoutDuration(ruleInDatabase, rule)
                    ) {
                        addRuleToUpdateWithDurationModeSwitch(rule);
                    }
                }
            } else {
                addRuleToInsert(rule);
            }
        }

        for (FileRulesModel databaseRule : rulesInDatabase.values()) {
            if (!rulesFromFile.containsKey(databaseRule.getRuleId())) {
                addRuleToDelete(databaseRule);
            }
        }
    }

    private boolean hasRuleDurationChanged(FileRulesModel oldRule, FileRulesModel newRule) {
        boolean sameRuleDuration = StringUtils.equals(oldRule.getRuleDuration(), newRule.getRuleDuration());
        boolean sameRuleMeasurement = oldRule.getRuleMeasurement() == newRule.getRuleMeasurement();
        return !sameRuleDuration || !sameRuleMeasurement;
    }

    private boolean isUpdateOfOldRuleWithoutDurationToNewRuleWithDuration(
        FileRulesModel oldRule,
        FileRulesModel newRule
    ) {
        return !hasRuleDuration(oldRule) && hasRuleDuration(newRule);
    }

    private boolean isUpdateOfOldRuleWithDurationToNewRuleWithoutDuration(
        FileRulesModel oldRule,
        FileRulesModel newRule
    ) {
        return hasRuleDuration(oldRule) && !hasRuleDuration(newRule);
    }

    private boolean hasRuleDuration(FileRulesModel rule) {
        return rule.getRuleDuration() != null;
    }

    private void addRuleToInsert(FileRulesModel rule) {
        rulesToInsert.add(rule);
    }

    private void addRuleToUpdateUnsafely(FileRulesModel rule) {
        rulesToUpdateUnsafely.add(rule);
    }

    private void addRuleToUpdateWithDurationModeSwitch(FileRulesModel rule) {
        rulesWithDurationModeUpdate.add(rule);
    }

    private void addRuleToUpdate(FileRulesModel rule) {
        rulesToUpdate.add(rule);
    }

    private void addRuleToDelete(FileRulesModel rule) {
        rulesToDelete.add(rule);
    }

    public List<FileRulesModel> getRulesToInsert() {
        return rulesToInsert;
    }

    public List<FileRulesModel> getRulesToUpdateUnsafely() {
        return rulesToUpdateUnsafely;
    }

    public List<FileRulesModel> getRulesWithDurationModeUpdate() {
        return rulesWithDurationModeUpdate;
    }

    public List<FileRulesModel> getRulesToDelete() {
        return rulesToDelete;
    }

    public List<FileRulesModel> getRulesToUpdate() {
        return rulesToUpdate;
    }
}
