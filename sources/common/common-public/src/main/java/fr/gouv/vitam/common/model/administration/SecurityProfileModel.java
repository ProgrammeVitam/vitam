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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Data Transfer Object Model of security profile (DTO).
 */
public class SecurityProfileModel {

    private static final String ID = "_id";
    private static final String IDENTIFIER = "Identifier";
    private static final String NAME = "Name";
    private static final String PERMISSIONS = "Permissions";

    /**
     * unique identifier
     */
    @JsonProperty(ID)
    private String id;

    @JsonProperty(IDENTIFIER)
    private String identifier;

    /**
     * Security profile name
     */
    @JsonProperty(NAME)
    private String name;

    /**
     * Permission set
     */
    @JsonProperty(PERMISSIONS)
    private Set<String> permissions;

    /**
     * Default constructor for jackson
     */
    public SecurityProfileModel() {
        super();
    }

    /**
     * Constructor
     * @param id unique identifier
     * @param identifier the identifier of the security profile. This value must be unique.
     * @param name security profile name
     * @param permissions set of permissions of the security profile
     */
    public SecurityProfileModel(String id, String identifier, String name, Set<String> permissions) {
        this.id = id;
        this.identifier = identifier;
        this.name = name;
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
     * @return this
     */
    public void setId(String id) {
        this.id = id;
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
     * @return this
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the set of permissions of the security profile
     * @return the
     */
    public Set<String> getPermissions() { return permissions; }

    /**
     * Sets the set
     * @param permissions
     */
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}
