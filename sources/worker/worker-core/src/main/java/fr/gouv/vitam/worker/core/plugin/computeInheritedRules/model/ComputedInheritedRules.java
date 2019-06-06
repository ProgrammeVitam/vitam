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
package fr.gouv.vitam.worker.core.plugin.computeInheritedRules.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ComputedInheritedRules
 */
public class ComputedInheritedRules {

    private static final String STORAGE_RULE = "StorageRule";
    private static final String APPRAISAL_RULE = "AppraisalRule";
    private static final String DISSEMINATION_RULE = "DisseminationRule";
    private static final String ACCESS_RULE = "AccessRule";
    private static final String REUSE_RULE = "ReuseRule";
    private static final String CLASSIFICATION_RULE = "ClassificationRule";

    @JsonProperty(STORAGE_RULE)
    private InheritedRule storageRule;
    @JsonProperty(APPRAISAL_RULE)
    private InheritedRule appraisalRule;
    @JsonProperty(DISSEMINATION_RULE)
    private InheritedRule disseminationRule;
    @JsonProperty(ACCESS_RULE)
    private InheritedRule accessRule;
    @JsonProperty(REUSE_RULE)
    private InheritedRule reuseRule;
    @JsonProperty(CLASSIFICATION_RULE)
    private InheritedRule classificationRule;
    @JsonProperty("GlobalProperties")
    private Properties globalProperties;
    @JsonProperty("inheritedRulesAPIOutput")
    private JsonNode inheritedRulesAPIOutput;

    public ComputedInheritedRules() {

    }

    public ComputedInheritedRules(Map<String, InheritedRule> inheritedRules, Properties globalProperties,
        JsonNode inheritedRulesAPIOutput) {
        this.storageRule = inheritedRules.get(STORAGE_RULE);
        this.appraisalRule = inheritedRules.get(APPRAISAL_RULE);
        this.disseminationRule = inheritedRules.get(DISSEMINATION_RULE);
        this.accessRule = inheritedRules.get(ACCESS_RULE);
        this.reuseRule = inheritedRules.get(REUSE_RULE);
        this.classificationRule = inheritedRules.get(CLASSIFICATION_RULE);
        this.globalProperties = globalProperties;
        this.inheritedRulesAPIOutput = inheritedRulesAPIOutput;
    }

    public ComputedInheritedRules(Map<String, InheritedRule> inheritedRules, Properties globalProperties) {
        this.storageRule = inheritedRules.get(STORAGE_RULE);
        this.appraisalRule = inheritedRules.get(APPRAISAL_RULE);
        this.disseminationRule = inheritedRules.get(DISSEMINATION_RULE);
        this.accessRule = inheritedRules.get(ACCESS_RULE);
        this.reuseRule = inheritedRules.get(REUSE_RULE);
        this.classificationRule = inheritedRules.get(CLASSIFICATION_RULE);
        this.globalProperties = globalProperties;
    }



    public InheritedRule getStorageRule() {
        return storageRule;
    }

    public void setStorageRule(InheritedRule storageRule) {
        this.storageRule = storageRule;
    }

    public InheritedRule getAppraisalRule() {
        return appraisalRule;
    }

    public void setAppraisalRule(InheritedRule appraisalRule) {
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

    public InheritedRule getClassificationRule() {
        return classificationRule;
    }

    public void setClassificationRule(InheritedRule classificationRule) {
        this.classificationRule = classificationRule;
    }

    public Properties getGlobalProperties() {
        return globalProperties;
    }

    public void setGlobalProperties(Properties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public JsonNode getInheritedRulesAPIOutput() {
        return inheritedRulesAPIOutput;
    }

    public void setInheritedRulesAPIOutput(JsonNode inheritedRulesAPIOutput) {
        this.inheritedRulesAPIOutput = inheritedRulesAPIOutput;
    }
}
