package fr.gouv.vitam.common.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditOptions {

    @JsonProperty("auditType")
    private String auditType;
    @JsonProperty("objectId")
    private String objectId;
    @JsonProperty("query")
    private JsonNode query;
    @JsonProperty("auditActions")
    private String auditActions;

    /**
     * Constructor without fields use for jackson
     */
    public AuditOptions() {
    }

    /**
     * Constructor
     * 
     * @param auditType    tenant, originatingagency or dsl
     * @param objectId     id of tenant or originating agency
     * @param query        dsl query
     * @param auditActions AUDIT_FILE_EXISTING or AUDIT_FILE_INTEGRITY
     */
    public AuditOptions(String auditType, String objectId, JsonNode query, String auditActions) {
        this.auditType = auditType;
        this.objectId = objectId;
        this.query = query;
        this.auditActions = auditActions;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }

    public String getAuditActions() {
        return auditActions;
    }

    public void setAuditActions(String auditActions) {
        this.auditActions = auditActions;
    }

    @JsonIgnore
    public void checkValid() {
        String errorMessage = null;
        if (auditType == null) {
            errorMessage = "The field auditType is mandatory";
        } else if (auditActions == null) {
            errorMessage = "The field auditActions is mandatory";
        } else {
            switch (auditType) {
            case "tenant":
            case "originatingagency":
                if (StringUtils.isBlank(objectId)) {
                    errorMessage = "The field objectId is mandatory with auditType " + auditType;
                }
                break;
            case "dsl":
                if (query == null) {
                    errorMessage = "The field query is mandatory with auditType " + auditType;
                }
                break;
            default:
                errorMessage = "The field auditType is invalid";
                break;
            }
        }
        if (errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
