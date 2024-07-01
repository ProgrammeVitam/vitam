/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.massupdate;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.Boolean.FALSE;

@JsonInclude(NON_NULL)
public class RuleCategoryActionDeletion {

    protected static final String UNUSED_VALUE = "UNUSED_VALUE";

    @JsonProperty("Rules")
    private Optional<List<RuleAction>> rules;

    @JsonProperty("FinalAction")
    private Optional<String> finalAction;

    @JsonProperty("ClassificationAudience")
    private Optional<String> classificationAudience;

    @JsonProperty("ClassificationReassessingDate")
    private Optional<String> classificationReassessingDate;

    @JsonProperty("NeedReassessingAuthorization")
    private Optional<Boolean> needReassessingAuthorization;

    @JsonProperty("PreventInheritance")
    private Optional<Boolean> preventInheritance;

    @JsonProperty("PreventRulesId")
    private Set<String> preventRulesId;

    @JsonProperty("PreventRulesIdToRemove")
    private Set<String> preventRulesIdToRemove;

    @JsonIgnore
    public Optional<List<RuleAction>> getRules() {
        return rules;
    }

    public void setRules(List<RuleAction> rules) {
        this.rules = Optional.ofNullable(rules);
    }

    @JsonGetter
    @JsonProperty("Rules")
    public List<RuleAction> getRulesForDeserializer() {
        return Objects.isNull(rules) ? null : rules.orElse(null);
    }

    @JsonIgnore
    public Optional<String> getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String unused) {
        this.finalAction = Optional.empty();
    }

    @JsonGetter
    @JsonProperty("FinalAction")
    public String getFinalActionForDeserializer() {
        return Objects.isNull(finalAction) ? null : UNUSED_VALUE;
    }

    @JsonIgnore
    public Optional<String> getClassificationAudience() {
        return classificationAudience;
    }

    public void setClassificationAudience(String unused) {
        this.classificationAudience = Optional.empty();
    }

    @JsonGetter
    @JsonProperty("ClassificationAudience")
    public String getClassificationAudienceForDeserializer() {
        return Objects.isNull(classificationAudience) ? null : UNUSED_VALUE;
    }

    @JsonIgnore
    public Optional<String> getClassificationReassessingDate() {
        return classificationReassessingDate;
    }

    public void setClassificationReassessingDate(String unused) {
        this.classificationReassessingDate = Optional.empty();
    }

    @JsonGetter
    @JsonProperty("ClassificationReassessingDate")
    public String getClassificationReassessingDateForDeserializer() {
        return Objects.isNull(classificationReassessingDate) ? null : UNUSED_VALUE;
    }

    @JsonIgnore
    public Optional<Boolean> getNeedReassessingAuthorization() {
        return needReassessingAuthorization;
    }

    public void setNeedReassessingAuthorization(Boolean needReassessingAuthorization) {
        this.needReassessingAuthorization = Optional.ofNullable(needReassessingAuthorization);
    }

    @JsonGetter
    @JsonProperty("NeedReassessingAuthorization")
    public Boolean getNeedReassessingAuthorizationForDeserializer() {
        return Objects.isNull(needReassessingAuthorization) ? null : FALSE;
    }

    @JsonIgnore
    public Optional<Boolean> getPreventInheritance() {
        return preventInheritance;
    }

    public void setPreventInheritance(Boolean unused) {
        this.preventInheritance = Optional.empty();
    }

    @JsonGetter
    @JsonProperty("PreventInheritance")
    public Boolean getPreventInheritanceForDeserializer() {
        return Objects.isNull(preventInheritance) ? null : FALSE;
    }

    @JsonIgnore
    public Set<String> getPreventRulesId() {
        return preventRulesId;
    }

    public void setPreventRulesId(Set<String> preventRulesId) {
        this.preventRulesId = preventRulesId;
    }

    @JsonGetter
    @JsonProperty("PreventRulesId")
    public Set<String> getPreventRulesIdForDeserializer() {
        return Objects.isNull(preventRulesId) ? null : Collections.singleton(UNUSED_VALUE);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (
            Objects.isNull(rules) &&
            Objects.isNull(finalAction) &&
            Objects.isNull(classificationAudience) &&
            Objects.isNull(classificationReassessingDate) &&
            Objects.isNull(needReassessingAuthorization) &&
            Objects.isNull(preventInheritance) &&
            Objects.isNull(preventRulesIdToRemove) &&
            Objects.isNull(preventRulesId)
        );
    }

    @JsonIgnore
    public Set<String> getPreventRulesIdToRemove() {
        return preventRulesIdToRemove;
    }

    public void setPreventRulesIdToRemove(Set<String> preventRulesIdToRemove) {
        this.preventRulesIdToRemove = preventRulesIdToRemove;
    }
}
