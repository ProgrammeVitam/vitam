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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

public class RuleActions {

    @JsonProperty("add")
    List<Map<String, RuleCategoryAction>> add = new ArrayList<>();

    @JsonProperty("update")
    List<Map<String, RuleCategoryAction>> update = new ArrayList<>();

    @JsonProperty("delete")
    @JsonInclude(ALWAYS)
    List<Map<String, RuleCategoryActionDeletion>> delete = new ArrayList<>();

    @JsonProperty("addOrUpdateMetadata")
    ManagementMetadataAction addOrUpdateMetadata;

    @JsonProperty("deleteMetadata")
    ManagementMetadataAction deleteMetadata;

    public List<Map<String, RuleCategoryAction>> getAdd() {
        return add;
    }

    public void setAdd(List<Map<String, RuleCategoryAction>> add) {
        this.add = add;
    }

    public List<Map<String, RuleCategoryAction>> getUpdate() {
        return update;
    }

    public void setUpdate(List<Map<String, RuleCategoryAction>> update) {
        this.update = update;
    }

    public List<Map<String, RuleCategoryActionDeletion>> getDelete() {
        return delete;
    }

    public void setDelete(List<Map<String, RuleCategoryActionDeletion>> delete) {
        this.delete = delete;
    }

    public ManagementMetadataAction getAddOrUpdateMetadata() {
        return addOrUpdateMetadata;
    }

    public void setAddOrUpdateMetadata(ManagementMetadataAction addOrUpdateMetadata) {
        this.addOrUpdateMetadata = addOrUpdateMetadata;
    }

    public ManagementMetadataAction getDeleteMetadata() {
        return deleteMetadata;
    }

    public void setDeleteMetadata(ManagementMetadataAction deleteMetadata) {
        this.deleteMetadata = deleteMetadata;
    }

    @JsonIgnore
    public boolean isRuleActionsEmpty() {
        return (
            add.isEmpty() &&
            update.isEmpty() &&
            delete.isEmpty() &&
            (addOrUpdateMetadata == null || StringUtils.isBlank(addOrUpdateMetadata.getArchiveUnitProfile())) &&
            (deleteMetadata == null || StringUtils.isBlank(deleteMetadata.getArchiveUnitProfile()))
        );
    }
}
