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
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RuleCategoryModel {

    private static final String CLASSIFICATION_AUDIENCE = "ClassificationAudience";
    private static final String CLASSIFICATION_LEVEL = "ClassificationLevel";
    private static final String CLASSIFICATION_OWNER = "ClassificationOwner";
    public static final String FINAL_ACTION = "FinalAction";
    private static final String CLASSIFICATION_REASSESSING_DATE = "ClassificationReassessingDate";
    private static final String NEED_REASSESSING_AUTHORIZATION = "NeedReassessingAuthorization";

    @JsonProperty("Rules")
    private List<RuleModel> rules;

    /**
     * Classification Audience
     */
    @JsonProperty(CLASSIFICATION_AUDIENCE)
    private String classificationAudience;

    /**
     * classification level
     */
    @JsonProperty(CLASSIFICATION_LEVEL)
    private String classificationLevel;

    /**
     * classification owner
     */
    @JsonProperty(CLASSIFICATION_OWNER)
    private String classificationOwner;

    /**
     * classificationReassessingDate
     */
    @JsonProperty(CLASSIFICATION_REASSESSING_DATE)
    private String classificationReassessingDate;

    /**
     * needReassessingAuthorization
     */
    @JsonProperty(NEED_REASSESSING_AUTHORIZATION)
    private Boolean needReassessingAuthorization;

    @JsonProperty("Inheritance")
    private InheritanceModel inheritance;

    @JsonProperty(FINAL_ACTION)
    private String finalAction;

    public RuleCategoryModel() {
        rules = new ArrayList<>();
    }

    public List<RuleModel> getRules() {
        return rules;
    }

    public void setRules(List<RuleModel> listRUle) {
        this.rules = listRUle;
    }

    public InheritanceModel getInheritance() {
        return inheritance;
    }

    public String getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String finalAction) {
        this.finalAction = finalAction;
    }

    @JsonIgnore
    public void merge(RuleCategoryModel ruleCategoryModel) {
        if (ruleCategoryModel == null) {
            return;
        }
        rules.addAll(ruleCategoryModel.getRules());
        if (inheritance != null) {
            inheritance.merge(ruleCategoryModel.getInheritance());
        } else {
            inheritance = ruleCategoryModel.getInheritance();
        }
        finalAction = ruleCategoryModel.getFinalAction();
    }

    @JsonIgnore
    public boolean isPreventInheritance() {
        return (
            inheritance != null &&
            inheritance.isPreventInheritance() != null &&
            Boolean.TRUE.equals(inheritance.isPreventInheritance())
        );
    }

    @JsonIgnore
    public void setPreventInheritance(Boolean preventInheritance) {
        if (inheritance == null) {
            inheritance = new InheritanceModel();
        }
        inheritance.setPreventInheritance(preventInheritance);
    }

    @JsonIgnore
    public void addAllPreventRulesId(List<String> preventRulesId) {
        if (inheritance == null) {
            inheritance = new InheritanceModel();
        }
        inheritance.getPreventRulesId().addAll(preventRulesId);
    }

    @JsonIgnore
    public void addToPreventRulesId(String preventRulesId) {
        if (inheritance == null) {
            inheritance = new InheritanceModel();
        }
        inheritance.getPreventRulesId().add(preventRulesId);
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

    /**
     * getter for classificationAudience
     *
     * @return classificationAudience value
     */
    public String getClassificationAudience() {
        return classificationAudience;
    }

    /**
     * set classificationAudience
     */
    public void setClassificationAudience(String classificationAudience) {
        this.classificationAudience = classificationAudience;
    }

    /**
     * getter for classificationReassessingDate
     *
     * @return classificationReassessingDate value
     */
    public String getClassificationReassessingDate() {
        return classificationReassessingDate;
    }

    /**
     * set classificationReassessingDate
     */
    public void setClassificationReassessingDate(String classificationReassessingDate) {
        this.classificationReassessingDate = classificationReassessingDate;
    }

    /**
     * getter for needReassessingAuthorization
     *
     * @return needReassessingAuthorization value
     */
    public Boolean isNeedReassessingAuthorization() {
        return needReassessingAuthorization;
    }

    /**
     * set needReassessingAuthorization
     */
    public void setNeedReassessingAuthorization(Boolean needReassessingAuthorization) {
        this.needReassessingAuthorization = needReassessingAuthorization;
    }

    @JsonIgnore
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CLASSIFICATION_AUDIENCE, this.classificationAudience);
        properties.put(CLASSIFICATION_LEVEL, this.classificationLevel);
        properties.put(CLASSIFICATION_OWNER, this.classificationOwner);
        properties.put(CLASSIFICATION_REASSESSING_DATE, this.classificationReassessingDate);
        properties.put(NEED_REASSESSING_AUTHORIZATION, this.needReassessingAuthorization);
        properties.put(FINAL_ACTION, this.finalAction);
        properties.values().removeIf(Objects::isNull);
        return properties;
    }
}
