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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object Model of Context
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ContextModel {

    public static final String TAG_SECURITY_PROFILE = "SecurityProfile";

    private static final String TAG_DEACTIVATION_DATE = "DeactivationDate";

    private static final String TAG_ACTIVATION_DATE = "ActivationDate";

    public static final String TAG_LAST_UPDATE = "LastUpdate";

    public static final String TAG_CREATION_DATE = "CreationDate";

    public static final String TAG_NAME = "Name";

    public static final String TAG_STATUS = "Status";

    public static final String TAG_ENABLE_CONTROL = "EnableControl";

    public static final String TAG_IDENTIFIER = "Identifier";

    public static final String TAG_PERMISSIONS = "Permissions";

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @JsonProperty(TAG_NAME)
    private String name;

    @JsonProperty(TAG_STATUS)
    private ContextStatus status;

    @JsonProperty(TAG_ENABLE_CONTROL)
    private Boolean enablecontrol;

    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    @JsonProperty(TAG_SECURITY_PROFILE)
    private String securityProfileIdentifier;

    @JsonProperty(TAG_PERMISSIONS)
    private List<PermissionModel> permissions = new ArrayList<>();

    @JsonProperty(TAG_CREATION_DATE)
    private String creationdate;

    @JsonProperty(TAG_LAST_UPDATE)
    private String lastupdate;

    @JsonProperty(TAG_ACTIVATION_DATE)
    private String activationdate;

    @JsonProperty(TAG_DEACTIVATION_DATE)
    private String deactivationdate;

    /**
     * empty constructor
     */
    public ContextModel() {}

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id value to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return status
     */
    public ContextStatus getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(ContextStatus status) {
        this.status = status;
    }

    /**
     * @return enableControle true we must check contract given contract exists in the current context, false else
     */
    public Boolean isEnablecontrol() {
        return enablecontrol == null ? Boolean.FALSE : enablecontrol;
    }

    /**
     * @param enablecontrol
     */
    public void setEnablecontrol(Boolean enablecontrol) {
        this.enablecontrol = enablecontrol;
    }

    /**
     * @return list of PermissionModel
     */
    public List<PermissionModel> getPermissions() {
        return permissions;
    }

    /**
     * @param permissions
     */
    public void setPermissions(List<PermissionModel> permissions) {
        this.permissions = permissions;
    }

    /**
     * @return identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier
     * @return ContextModel
     */
    public ContextModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * @return the creation date of context
     */
    public String getCreationdate() {
        return this.creationdate;
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public ContextModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
        return this;
    }

    /**
     * @return last update of context
     */
    public String getLastupdate() {
        return this.lastupdate;
    }

    /**
     * @param lastupdate to set
     * @return this
     */
    public ContextModel setLastupdate(String lastupdate) {
        this.lastupdate = lastupdate;
        return this;
    }

    /**
     * @return the activation date of context
     */
    public String getActivationdate() {
        return this.activationdate;
    }

    /**
     * @param activationdate to set
     * @return this
     */
    public ContextModel setActivationdate(String activationdate) {
        this.activationdate = activationdate;
        return this;
    }

    /**
     * @return the desactivation date of context
     */
    public String getDeactivationdate() {
        return this.deactivationdate;
    }

    /**
     * @param deactivationdate to set
     * @return this
     */
    public ContextModel setDeactivationdate(String deactivationdate) {
        this.deactivationdate = deactivationdate;
        return this;
    }

    /**
     * @return the security profile identifier
     */
    public String getSecurityProfileIdentifier() {
        return securityProfileIdentifier;
    }

    /**
     * @param securityProfileIdentifier tbe security profile identifier to set.
     * @return this
     */
    public ContextModel setSecurityProfileIdentifier(String securityProfileIdentifier) {
        this.securityProfileIdentifier = securityProfileIdentifier;
        return this;
    }
}
