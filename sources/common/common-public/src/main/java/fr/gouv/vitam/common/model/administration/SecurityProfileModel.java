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

import java.util.Set;

/**
 * Data Transfer Object Model of security profile (DTO).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SecurityProfileModel {

    private static final String TAG_IDENTIFIER = "Identifier";
    private static final String TAG_NAME = "Name";
    private static final String TAG_FULL_ACCESS = "FullAccess";
    private static final String TAG_PERMISSIONS = "Permissions";

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

    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    /**
     * Security profile name
     */
    @JsonProperty(TAG_NAME)
    private String name;

    /**
     * Flag defining full permission set mode (super admin)
     */
    @JsonProperty(TAG_FULL_ACCESS)
    private Boolean fullAccess;

    /**
     * Permission set
     */
    @JsonProperty(TAG_PERMISSIONS)
    private Set<String> permissions;

    /**
     * Default constructor for jackson
     */
    public SecurityProfileModel() {
        super();
    }

    /**
     * Constructor
     *
     * @param id unique identifier
     * @param identifier the identifier of the security profile. This value must be unique.
     * @param name security profile name
     * @param fullAccess defines whether security profile has full access to all permissions.
     * @param permissions set of permissions of the security profile (should not be defined when fullAccess is true)
     */
    public SecurityProfileModel(
        String id,
        String identifier,
        String name,
        boolean fullAccess,
        Set<String> permissions
    ) {
        this.id = id;
        this.identifier = identifier;
        this.name = name;
        this.fullAccess = fullAccess;
        this.permissions = permissions;
    }

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
     * Get the identifier of the security profile
     *
     * @return String
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier of the security profile. This value must be unique.
     *
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get name of the security profile
     *
     * @return name as String
     */
    public String getName() {
        return name;
    }

    /**
     * Set or change the security profile name
     *
     * @param name as String to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return true if security profile has full access to all permissions. false otherwise.
     * When set to true, all permissions are granted and "Permissions" set is ignored and should not be set.
     */
    public Boolean getFullAccess() {
        return fullAccess == null ? Boolean.FALSE : fullAccess;
    }

    /**
     * Sets / unsets full access to all permissions for security profile.
     * When set to true, all permissions are granted and "Permissions" set is ignored and should not be set.
     *
     * @param fullAccess
     */
    public void setFullAccess(Boolean fullAccess) {
        this.fullAccess = fullAccess;
    }

    /**
     * Gets the set of permissions of the security profile.
     *
     * @return the
     */
    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * Sets the permission set of the security profile.
     * Should not be defined when fullAccess is true
     *
     * @param permissions
     */
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
