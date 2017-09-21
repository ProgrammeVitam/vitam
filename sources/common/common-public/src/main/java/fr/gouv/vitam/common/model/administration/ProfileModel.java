/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object Model of Profile (DTO).
 */

public class ProfileModel {

    public static final String TAG_ID = "id";
    public static final String TAG_TENANT = "tenant";
    public static final String HASH = "#";
    public static final String UNDERSCORE = "_";

    /**
     * unique identifier
     */
    private String id;

    /**
     * tenant id
     */
    private Integer tenant;

    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Status")
    private ProfileStatus status;


    @JsonProperty("Format")
    private ProfileFormat format;

    @JsonProperty("Path")
    private String path;


    @JsonProperty("CreationDate")
    private String creationdate;

    @JsonProperty("LastUpdate")
    private String lastupdate;

    @JsonProperty("ActivationDate")
    private String activationdate;

    @JsonProperty("DeactivationDate")
    private String deactivationdate;



    /**
     * Constructor without fields use for jackson
     */
    public ProfileModel() {
        super();
    }

    /**
     * @return id
     */
    @JsonProperty(HASH + TAG_ID)
    public String getId() {
        return id;
    }

    /**
     * @param id value to set field
     * @return this
     */
    @JsonProperty(UNDERSCORE + TAG_ID)
    public ProfileModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @param id value to set field
     * @return this
     */
    @JsonProperty(HASH + TAG_ID)
    public ProfileModel setIdExt(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    @JsonProperty(HASH + TAG_TENANT)
    public Integer getTenant() {
        return tenant;
    }

    /**
     * @param tenant value to set working tenant
     * @return this
     */
    @JsonProperty(UNDERSCORE + TAG_TENANT)
    public ProfileModel setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * @param tenant value to set working tenant
     * @return this
     */
    @JsonProperty(HASH + TAG_TENANT)
    public ProfileModel setTenantExt(Integer tenant) {
        this.tenant = tenant;
        return this;
    }


    /**
     * Get the identifier of the profile
     *
     * @return identifier as String
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier of the profile This value must be unique by tenant
     *
     * @param identifier as String
     * @return this
     */
    public ProfileModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Get name of the profile
     *
     * @return name as String
     */
    public String getName() {
        return name;
    }

    /**
     * Set or change the profile name
     *
     * @param name as String to set
     * @return this
     */
    public ProfileModel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the profile description
     *
     * @return description of profile
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set or change the profile description
     *
     * @param description to set
     * @return this
     */
    public ProfileModel setDescription(String description) {
        this.description = description;
        return this;
    }


    /**
     * Get the profile status
     *
     * @return status of profile
     */
    public ProfileStatus getStatus() {
        return status;
    }


    /**
     * Set or change the profile status
     *
     * @param status toi set
     * @return this
     */
    public ProfileModel setStatus(ProfileStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Get the format of the profile file (xsd, rng, ...)
     *
     * @return the file format as string
     */
    public ProfileFormat getFormat() {
        return format;
    }


    /**
     * Set the profile file format (xsd, rng, ...)
     *
     * @param format
     * @return this
     */
    public ProfileModel setFormat(ProfileFormat format) {
        this.format = format;
        return this;
    }

    /**
     * @return path as String
     */
    public String getPath() {
        return path;
    }

    /**
     * Profile path in storage
     *
     * @param path
     * @return this
     */
    public ProfileModel setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * @return the creation date of profile
     */
    public String getCreationdate() {
        return creationdate;
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public ProfileModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
        return this;
    }

    /**
     * @return last update of profile
     */
    public String getLastupdate() {
        return lastupdate;
    }

    /**
     * @param lastupdate to set
     * @return this
     */
    public ProfileModel setLastupdate(String lastupdate) {
        this.lastupdate = lastupdate;
        return this;
    }

    /**
     * @return the activation date of profile
     */
    public String getActivationdate() {
        return activationdate;
    }

    /**
     * @param activationdate to set
     * @return this
     */
    public ProfileModel setActivationdate(String activationdate) {
        this.activationdate = activationdate;
        return this;
    }

    /**
     * @return the desactivation date of profile
     */
    public String getDeactivationdate() {
        return deactivationdate;
    }

    /**
     * @param deactivationdate to set
     * @return this
     */
    public ProfileModel setDeactivationdate(String deactivationdate) {
        this.deactivationdate = activationdate;
        return this;
    }


}
