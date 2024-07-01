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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.VitamRuntimeException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ComputedInheritedRules
 */
public class ComputedInheritedRules {

    public static final String STORAGE_RULE = "StorageRule";
    public static final String APPRAISAL_RULE = "AppraisalRule";
    public static final String DISSEMINATION_RULE = "DisseminationRule";
    public static final String ACCESS_RULE = "AccessRule";
    public static final String REUSE_RULE = "ReuseRule";
    public static final String HOLD_RULE = "HoldRule";
    public static final String CLASSIFICATION_RULE = "ClassificationRule";
    private static final String NEED_AUTHORIZATION = "NeedAuthorization";

    @JsonProperty(STORAGE_RULE)
    private StorageRule storageRule;

    @JsonProperty(APPRAISAL_RULE)
    private AppraisalRule appraisalRule;

    @JsonProperty(DISSEMINATION_RULE)
    private InheritedRule disseminationRule;

    @JsonProperty(ACCESS_RULE)
    private InheritedRule accessRule;

    @JsonProperty(REUSE_RULE)
    private InheritedRule reuseRule;

    @JsonProperty(CLASSIFICATION_RULE)
    private ClassificationRule classificationRule;

    @JsonProperty(HOLD_RULE)
    private InheritedRule holdRule;

    @JsonProperty("inheritedRulesAPIOutput")
    private JsonNode inheritedRulesAPIOutput;

    @JsonProperty("indexationDate")
    private String indexationDate;

    @JsonProperty(NEED_AUTHORIZATION)
    private List<Boolean> needAuthorization;

    public ComputedInheritedRules() {}

    public ComputedInheritedRules(
        Map<String, InheritedRule> inheritedRules,
        JsonNode inheritedRulesAPIOutput,
        Map<String, Object> globalInheritedProperties,
        String indexationDate
    ) {
        this(inheritedRules, indexationDate, globalInheritedProperties);
        this.inheritedRulesAPIOutput = inheritedRulesAPIOutput;
    }

    public ComputedInheritedRules(
        Map<String, InheritedRule> inheritedRules,
        String indexationDate,
        Map<String, Object> globalInheritedProperties
    ) {
        this.storageRule = (StorageRule) inheritedRules.get(STORAGE_RULE);
        this.appraisalRule = (AppraisalRule) inheritedRules.get(APPRAISAL_RULE);
        this.disseminationRule = inheritedRules.get(DISSEMINATION_RULE);
        this.accessRule = inheritedRules.get(ACCESS_RULE);
        this.reuseRule = inheritedRules.get(REUSE_RULE);
        this.classificationRule = (ClassificationRule) inheritedRules.get(CLASSIFICATION_RULE);
        this.holdRule = inheritedRules.get(HOLD_RULE);
        this.indexationDate = indexationDate;
        this.needAuthorization = parseNeedAuthorizationProperty(globalInheritedProperties.get(NEED_AUTHORIZATION));
    }

    private List<Boolean> parseNeedAuthorizationProperty(Object needAuthorizationProperty) {
        if (needAuthorizationProperty == null) {
            return null;
        }
        if (needAuthorizationProperty instanceof Boolean) {
            return this.needAuthorization = Collections.singletonList((Boolean) needAuthorizationProperty);
        } else if (needAuthorizationProperty instanceof Collection<?>) {
            return this.needAuthorization = (List<Boolean>) needAuthorizationProperty;
        }

        throw new VitamRuntimeException("needAuthorization Type invalide : " + needAuthorizationProperty.getClass());
    }

    public InheritedRule getStorageRule() {
        return storageRule;
    }

    public void setStorageRule(StorageRule storageRule) {
        this.storageRule = storageRule;
    }

    public InheritedRule getAppraisalRule() {
        return appraisalRule;
    }

    public void setAppraisalRule(AppraisalRule appraisalRule) {
        this.appraisalRule = appraisalRule;
    }

    public InheritedRule getDisseminationRule() {
        return disseminationRule;
    }

    public void setDisseminationRule(InheritedRule disseminationRule) {
        this.disseminationRule = disseminationRule;
    }

    public InheritedRule getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(InheritedRule accessRule) {
        this.accessRule = accessRule;
    }

    public InheritedRule getReuseRule() {
        return reuseRule;
    }

    public void setReuseRule(InheritedRule reuseRule) {
        this.reuseRule = reuseRule;
    }

    public ClassificationRule getClassificationRule() {
        return classificationRule;
    }

    public void setClassificationRule(ClassificationRule classificationRule) {
        this.classificationRule = classificationRule;
    }

    public InheritedRule getHoldRule() {
        return this.holdRule;
    }

    public void setHoldRule(InheritedRule holdRule) {
        this.holdRule = holdRule;
    }

    public JsonNode getInheritedRulesAPIOutput() {
        return inheritedRulesAPIOutput;
    }

    public void setInheritedRulesAPIOutput(JsonNode inheritedRulesAPIOutput) {
        this.inheritedRulesAPIOutput = inheritedRulesAPIOutput;
    }

    public String getIndexationDate() {
        return indexationDate;
    }

    public void setIndexationDate(String indexationDate) {
        this.indexationDate = indexationDate;
    }

    public List<Boolean> getNeedAuthorization() {
        return needAuthorization;
    }

    public void setNeedAuthorization(List<Boolean> needAuthorization) {
        this.needAuthorization = needAuthorization;
    }
}
