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
package fr.gouv.vitam.worker.core.plugin.elimination;

import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.rules.BaseInheritedResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedPropertyResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisStatusForOriginatingAgency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfo;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistencyDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoFinalActionInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoKeepAccessSp;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.unit.RuleCategoryModel.FINAL_ACTION;

/**
 * Elimination analysis service.
 */
public class EliminationAnalysisService {

    public EliminationAnalysisResult analyzeElimination(
        String operationId, List<InheritedRuleResponseModel> rules, List<InheritedPropertyResponseModel> properties,
        LocalDate expirationDate, String sp) {

        Map<String, EliminationAnalysisStatusForOriginatingAgency> statusByOriginatingAgency =
            analyzeFinalActionByOriginatingAgency(rules, properties, expirationDate);

        Set<String> destroyableOriginatingAgencies =
            filterByTargetStatus(statusByOriginatingAgency, EliminationAnalysisStatusForOriginatingAgency.DESTROY);
        Set<String> nonDestroyableOriginatingAgencies =
            filterByTargetStatus(statusByOriginatingAgency, EliminationAnalysisStatusForOriginatingAgency.KEEP);
        Set<String> conflictOnOriginatingAgencies =
            filterByTargetStatus(statusByOriginatingAgency, EliminationAnalysisStatusForOriginatingAgency.FINAL_ACTION_INCONSISTENCY);

        EliminationGlobalStatus
            globalStatus = getGlobalStatus(destroyableOriginatingAgencies, nonDestroyableOriginatingAgencies, conflictOnOriginatingAgencies);

        List<EliminationExtendedInfo> extendedInfo = new ArrayList<>();

        if (globalStatus != EliminationGlobalStatus.KEEP) {
            // FINAL_ACTION_INCONSISTENCY
            if (!conflictOnOriginatingAgencies.isEmpty()) {
                extendedInfo.add(new EliminationExtendedInfoFinalActionInconsistency(conflictOnOriginatingAgencies));
            }

            // KEEP_ACCESS_SP
            if (shouldKeepAccessToOriginatingAgency(sp, destroyableOriginatingAgencies,
                nonDestroyableOriginatingAgencies)) {
                extendedInfo.add(new EliminationExtendedInfoKeepAccessSp());
            }

            // ACCESS_LINK_INCONSISTENCY
            List<EliminationExtendedInfoAccessLinkInconsistencyDetails> accessLinkInconsistencies =
                analyzeAccessLinkInconsistencies(rules, properties, expirationDate);
            for (EliminationExtendedInfoAccessLinkInconsistencyDetails accessLinkInconsistency : accessLinkInconsistencies) {
                extendedInfo.add(new EliminationExtendedInfoAccessLinkInconsistency(accessLinkInconsistency));
            }
        }

        return new EliminationAnalysisResult(operationId, globalStatus,
            destroyableOriginatingAgencies, nonDestroyableOriginatingAgencies, extendedInfo);
    }

    private EliminationGlobalStatus getGlobalStatus(Set<String> destroyableOriginatingAgencies,
        Set<String> nonDestroyableOriginatingAgencies, Set<String> conflictOnOriginatingAgencies) {

        if (!conflictOnOriginatingAgencies.isEmpty()) {
            return EliminationGlobalStatus.CONFLICT;
        }

        if (destroyableOriginatingAgencies.isEmpty()) {
            return EliminationGlobalStatus.KEEP;
        } else if (nonDestroyableOriginatingAgencies.isEmpty()) {
            return EliminationGlobalStatus.DESTROY;
        } else {
            return EliminationGlobalStatus.CONFLICT;
        }
    }

    private boolean shouldKeepAccessToOriginatingAgency(String sp, Set<String> destroyableOriginatingAgencies,
        Set<String> nonDestroyableOriginatingAgencies) {

        return sp != null &&
            destroyableOriginatingAgencies.contains(sp) &&
            !nonDestroyableOriginatingAgencies.isEmpty();
    }

    /**
     * Group rules & properties by originating agency & return destroyable status
     */
    private Map<String, EliminationAnalysisStatusForOriginatingAgency> analyzeFinalActionByOriginatingAgency(
        Collection<InheritedRuleResponseModel> rules, Collection<InheritedPropertyResponseModel> properties,
        LocalDate expirationDate) {

        Set<String> originatingAgencies =
            Stream.concat(rules.stream(), properties.stream())
                .map(BaseInheritedResponseModel::getOriginatingAgency)
                .collect(Collectors.toSet());

        Map<String, EliminationAnalysisStatusForOriginatingAgency> statusForOriginatingAgency = new HashMap<>();

        for (String originatingAgency : originatingAgencies) {

            List<InheritedRuleResponseModel> originatingAgencyRules =
                filterByOriginatingAgency(rules, originatingAgency);
            List<InheritedPropertyResponseModel> originatingAgencyProperties =
                filterByOriginatingAgency(properties, originatingAgency);

            boolean destroyableForOriginatingAgency =
                isDestroyable(originatingAgencyRules, originatingAgencyProperties, expirationDate);

            boolean conflictForOriginatingAgency =
                isFinalActionConflict(originatingAgencyRules, originatingAgencyProperties, expirationDate, destroyableForOriginatingAgency);

            EliminationAnalysisStatusForOriginatingAgency status =
                EliminationAnalysisStatusForOriginatingAgency.getValue(destroyableForOriginatingAgency, conflictForOriginatingAgency);
            statusForOriginatingAgency.put(originatingAgency, status);
        }
        return statusForOriginatingAgency;
    }

    /**
     * If the rules/properties inherited via a direct parent unit have Destroyable & NonDestroyable originating
     * agencies, then report inconsistency.
     */
    private List<EliminationExtendedInfoAccessLinkInconsistencyDetails> analyzeAccessLinkInconsistencies(
        List<InheritedRuleResponseModel> rules,
        List<InheritedPropertyResponseModel> properties, LocalDate expirationDate) {

        List<EliminationExtendedInfoAccessLinkInconsistencyDetails> accessLinkInconsistencies = new ArrayList<>();

        MultiValuedMap<String, InheritedRuleResponseModel> rulesByDirectParentId =
            groupByDirectParentId(rules);
        MultiValuedMap<String, InheritedPropertyResponseModel> propertiesByDirectParentId =
            groupByDirectParentId(properties);

        for (String parentId : SetUtils.union(rulesByDirectParentId.keySet(), propertiesByDirectParentId.keySet())) {

            Map<String, EliminationAnalysisStatusForOriginatingAgency> destroyableForParentId = analyzeFinalActionByOriginatingAgency(
                rulesByDirectParentId.get(parentId), propertiesByDirectParentId.get(parentId), expirationDate);

            Set<String> destroyableOriginatingAgencies =
                filterByTargetStatus(destroyableForParentId, EliminationAnalysisStatusForOriginatingAgency.DESTROY);
            Set<String> nonDestroyableOriginatingAgencies =
                filterByTargetStatus(destroyableForParentId, EliminationAnalysisStatusForOriginatingAgency.KEEP);

            if (!destroyableOriginatingAgencies.isEmpty() &&
                !nonDestroyableOriginatingAgencies.isEmpty()) {

                accessLinkInconsistencies.add(new EliminationExtendedInfoAccessLinkInconsistencyDetails(parentId,
                    destroyableOriginatingAgencies, nonDestroyableOriginatingAgencies));
            }
        }
        return accessLinkInconsistencies;
    }

    private <T extends BaseInheritedResponseModel> List<T> filterByOriginatingAgency(Collection<T> inheritedEntries,
        String originatingAgency) {
        return inheritedEntries.stream()
            .filter(entry -> originatingAgency.equals(entry.getOriginatingAgency()))
            .collect(Collectors.toList());
    }

    private <T extends BaseInheritedResponseModel> MultiValuedMap<String, T> groupByDirectParentId(
        List<T> inheritedEntries) {
        MultiValuedMap<String, T> response = new HashSetValuedHashMap<>();
        for (T entry : inheritedEntries) {
            for (List<String> path : entry.getPaths()) {
                // Current unit id is at index 0
                // Direct parent is at index 1
                if (path.size() >= 2) {
                    String directParentId = path.get(1);
                    response.put(directParentId, entry);
                }
            }
        }
        return response;
    }

    private Set<String> filterByTargetStatus(Map<String, EliminationAnalysisStatusForOriginatingAgency> statusForOriginatingAgency,
        EliminationAnalysisStatusForOriginatingAgency targetStatus) {
        return statusForOriginatingAgency.entrySet().stream()
            .filter(entry -> targetStatus.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private boolean isFinalActionConflict(List<InheritedRuleResponseModel> rules, List<InheritedPropertyResponseModel> properties,
        LocalDate expirationDate, Boolean isDestroyableForOriginatingAgency) {

        // If DESTROY: no conflict
        if (isDestroyableForOriginatingAgency) {
            return false;
        }

        // From there, status is a global keep
        List<String> destroyIds = properties.stream()
            .filter(p -> FINAL_ACTION.equals(p.getPropertyName()))
            .filter(p -> VitamConstants.AppraisalRuleFinalAction.DESTROY.equals(
                VitamConstants.AppraisalRuleFinalAction.fromValue(p.getPropertyValue().toString())
            ))
            .map(InheritedPropertyResponseModel::getUnitId)
            .collect(Collectors.toList());

        // If no Destroy => All are KEEP, no conflict
        if (destroyIds.isEmpty()) {
            return false;
        }

        return areEndDatesReached(rules, expirationDate);
    }

    /**
     * A unit with a set of rules & properties is destroyable iif :
     * - The rule set is not empty
     * - The property set is not empty
     * - All rules have expired
     * - All FinalAction properties are DESTROY
     */
    private boolean isDestroyable(List<InheritedRuleResponseModel> rules,
        List<InheritedPropertyResponseModel> properties, LocalDate expirationDate) {

        if (!isFinalActionDestroy(properties))
            return false;

        if (!areEndDatesReached(rules, expirationDate))
            return false;

        return true;
    }

    private boolean isFinalActionDestroy(List<InheritedPropertyResponseModel> properties) {

        List<VitamConstants.AppraisalRuleFinalAction> finalActions = properties.stream()
            .filter(p -> FINAL_ACTION.equals(p.getPropertyName()))
            .map(p -> (String) p.getPropertyValue())
            .map(VitamConstants.AppraisalRuleFinalAction::fromValue)
            .collect(Collectors.toList());

        if (finalActions.isEmpty()) {
            // No FinalAction --> Keep
            return false;
        }

        if (finalActions.stream().anyMatch(VitamConstants.AppraisalRuleFinalAction.KEEP::equals)) {
            // Only KEEP (implicit or explicit)
            return false;
        }

        return true;
    }

    private boolean areEndDatesReached(List<InheritedRuleResponseModel> rules,
        LocalDate expirationDate) {

        List<LocalDate> endDates = rules.stream()
            .map(InheritedRuleResponseModel::getEndDate)
            .map(this::parseDate)
            .collect(Collectors.toList());

        if (endDates.isEmpty()) {
            // No rules --> Keep
            return false;
        }

        if (endDates.stream().anyMatch(Objects::isNull)) {
            // Undefined end date --> Keep
            return false;
        }

        if (endDates.stream().anyMatch(expirationDate::isBefore)) {
            // At least 1 non-expired rule
            return false;
        }

        return true;
    }

    private LocalDate parseDate(String endDateStr) {

        if (endDateStr == null) {
            return null;
        }

        return LocalDate.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
