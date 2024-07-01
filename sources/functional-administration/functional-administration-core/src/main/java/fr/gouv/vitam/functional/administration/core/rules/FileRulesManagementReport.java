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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Data Transfer Object Model of Reporting File Rule Management
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileRulesManagementReport {

    @JsonProperty("insertedRules")
    private List<String> fileRulesToImport = new ArrayList<>();

    @JsonProperty("updatedRules")
    private List<String> fileRulesToUpdate = new ArrayList<>();

    @JsonProperty("deletedRules")
    private List<String> fileRulesToDelete = new ArrayList<>();

    @JsonProperty("usedRulesWithDurationModeUpdate")
    private List<String> usedRulesWithDurationModeUpdate = new ArrayList<>();

    @JsonProperty("usedFileRulesToDelete")
    private List<String> usedFileRulesToDelete = new ArrayList<>();

    @JsonProperty("usedFileRulesToUpdate")
    private List<String> usedFileRulesToUpdate = new ArrayList<>();

    @JsonProperty("Operation")
    private HashMap<String, String> jdo = new HashMap<>();

    @JsonProperty("error")
    private HashMap<String, Object> errors;

    /**
     * @return
     */
    public List<String> getFileRulesToImport() {
        return fileRulesToImport;
    }

    /**
     * @param fileRulesToImport
     */
    public void setFileRulesToImport(List<String> fileRulesToImport) {
        this.fileRulesToImport = fileRulesToImport;
    }

    /**
     * @return
     */
    public List<String> getFileRulesToUpdate() {
        return fileRulesToUpdate;
    }

    /**
     * @param fileRulesToUpdate
     */
    public void setFileRulesToUpdate(List<String> fileRulesToUpdate) {
        this.fileRulesToUpdate = fileRulesToUpdate;
    }

    /**
     * @return
     */
    public List<String> getFileRulesToDelete() {
        return fileRulesToDelete;
    }

    /**
     * @param fileRulesToDelete
     */
    public void setFileRulesToDelete(List<String> fileRulesToDelete) {
        this.fileRulesToDelete = fileRulesToDelete;
    }

    public List<String> getUsedFileRulesToDelete() {
        return usedFileRulesToDelete;
    }

    public void setUsedFileRulesToDelete(List<String> usedFileRulesToDelete) {
        this.usedFileRulesToDelete = usedFileRulesToDelete;
    }

    public List<String> getUsedFileRulesToUpdate() {
        return usedFileRulesToUpdate;
    }

    public void setUsedFileRulesToUpdate(List<String> usedFileRulesToUpdate) {
        this.usedFileRulesToUpdate = usedFileRulesToUpdate;
    }

    public List<String> getUsedRulesWithDurationModeUpdate() {
        return usedRulesWithDurationModeUpdate;
    }

    public void setUsedRulesWithDurationModeUpdate(List<String> usedRulesWithDurationModeUpdate) {
        this.usedRulesWithDurationModeUpdate = usedRulesWithDurationModeUpdate;
    }

    /**
     * @return
     */
    public HashMap<String, String> getJdo() {
        return jdo;
    }

    /**
     * @param jdo
     */
    public void setJdo(HashMap<String, String> jdo) {
        this.jdo = jdo;
    }

    /**
     * @return
     */
    public HashMap<String, Object> getErrors() {
        return errors;
    }

    /**
     * @param errors
     */
    public void setError(HashMap<String, Object> errors) {
        this.errors = errors;
    }
}
