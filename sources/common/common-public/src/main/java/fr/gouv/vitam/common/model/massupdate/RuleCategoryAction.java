/**
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
package fr.gouv.vitam.common.model.massupdate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RuleCategoryAction {

    @JsonProperty("Rules")
    private List<RuleAction> rules;

    // Appraisal / Storage property
    @JsonProperty("FinalAction")
    private String finalAction;

    // Classification properties
    @JsonProperty("ClassificationLevel")
    private String classificationLevel;
    @JsonProperty("ClassificationOwner")
    private String classificationOwner;
    @JsonProperty("ClassificationAudience")
    private String classificationAudience;
    @JsonProperty("ClassificationReassessingDate")
    private String classificationReassessingDate;
    @JsonProperty("NeedReassessingAuthorization")
    private Boolean needReassessingAuthorization;

    // Inheritance properties
    @JsonProperty("PreventInheritance")
    private Boolean preventInheritance;
    @JsonProperty("PreventRulesId")
    private Set<String> preventRulesId;

    public List<RuleAction> getRules() {
        return rules == null ? new ArrayList<>() : rules;
    }

    public void setRules(List<RuleAction> rules) {
        this.rules = rules;
    }

    public String getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String finalAction) {
        this.finalAction = finalAction;
    }

    public Boolean getPreventInheritance() {
        return preventInheritance;
    }

    public void setPreventInheritance(Boolean preventInheritance) {
        this.preventInheritance = preventInheritance;
    }

    public Set<String> getPreventRulesId() {
        return preventRulesId;
    }

    public void setPreventRulesId(Set<String> preventRulesId) {
        this.preventRulesId = preventRulesId;
    }

    public String getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(String classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public String getClassificationOwner() {
        return classificationOwner;
    }

    public void setClassificationOwner(String classificationOwner) {
        this.classificationOwner = classificationOwner;
    }

    public String getClassificationAudience() {
        return classificationAudience;
    }

    public void setClassificationAudience(String classificationAudience) {
        this.classificationAudience = classificationAudience;
    }

    public String getClassificationReassessingDate() {
        return classificationReassessingDate;
    }

    public void setClassificationReassessingDate(String classificationReassessingDate) {
        this.classificationReassessingDate = classificationReassessingDate;
    }

    public Boolean getNeedReassessingAuthorization() {
        return needReassessingAuthorization;
    }

    public void setNeedReassessingAuthorization(Boolean needReassessingAuthorization) {
        this.needReassessingAuthorization = needReassessingAuthorization;
    }
}
