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
package fr.gouv.vitam.worker.core.model;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;

public class ManagementModel {

    private List<RuleModel> storageRule;

    private List<RuleModel> appraisalRule;

    private List<RuleModel> accessRule;

    private List<RuleModel> disseminationRule;

    private List<RuleModel> reuseRule;

    private List<RuleModel> classificationRule;

    private Boolean needAuthorization;

    private UpdateOperationType updateOperationType;

    public ManagementModel() {
        storageRule = new ArrayList<>();
        appraisalRule = new ArrayList<>();
        accessRule = new ArrayList<>();
        disseminationRule = new ArrayList<>();
        reuseRule = new ArrayList<>();
        classificationRule = new ArrayList<>();
    }

    public void addStorageRule(RuleModel ruleModel) {
        storageRule.add(ruleModel);
    }

    public void addAppraisalRule(RuleModel ruleModel) {
        appraisalRule.add(ruleModel);
    }

    public void addAccessRule(RuleModel ruleModel) {
        accessRule.add(ruleModel);
    }

    public void addDisseminationRule(RuleModel ruleModel) {
        disseminationRule.add(ruleModel);
    }

    public void addReuseRule(RuleModel ruleModel) {
        reuseRule.add(ruleModel);
    }

    public void addClassificationRule(RuleModel ruleModel) {
        classificationRule.add(ruleModel);
    }

    public List<RuleModel> getStorageRule() {
        return storageRule;
    }

    public List<RuleModel> getAppraisalRule() {
        return appraisalRule;
    }

    public List<RuleModel> getAccessRule() {
        return accessRule;
    }

    public List<RuleModel> getDisseminationRule() {
        return disseminationRule;
    }

    public List<RuleModel> getReuseRule() {
        return reuseRule;
    }

    public List<RuleModel> getClassificationRule() {
        return classificationRule;
    }

    public Boolean isNeedAuthorization() {
        return needAuthorization;
    }

    public UpdateOperationType getUpdateOperationType() {
        return updateOperationType;
    }

    public void setUpdateOperationType(UpdateOperationType updateOperationType) {
        this.updateOperationType = updateOperationType;
    }
}
