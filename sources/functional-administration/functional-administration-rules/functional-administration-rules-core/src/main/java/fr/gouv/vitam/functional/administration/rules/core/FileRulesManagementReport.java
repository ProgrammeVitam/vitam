package fr.gouv.vitam.functional.administration.rules.core;

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


    @JsonProperty("FileRulesToImport")
    private List<String> fileRulesToImport = new ArrayList<>();
    @JsonProperty("updatedRules")
    private List<String> fileRulesToUpdate = new ArrayList<>();
    @JsonProperty("deletedRules")
    private List<String> fileRulesToDelete = new ArrayList<>();

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
