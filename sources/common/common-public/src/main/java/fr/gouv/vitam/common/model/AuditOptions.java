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
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

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
    public AuditOptions() {}

    /**
     * Constructor
     *
     * @param auditType tenant, originatingagency or dsl
     * @param objectId id of tenant or originating agency
     * @param query dsl query
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
