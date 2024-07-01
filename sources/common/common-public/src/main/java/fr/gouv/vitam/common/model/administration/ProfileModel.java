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

/**
 * Data Transfer Object Model of Profile (DTO).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProfileModel {

    public static final String TAG_IDENTIFIER = "Identifier";
    public static final String TAG_NAME = "Name";
    public static final String TAG_DESCRIPTION = "Description";
    public static final String TAG_STATUS = "Status";
    public static final String TAG_FORMAT = "Format";
    public static final String TAG_PATH = "Path";
    public static final String CREATION_DATE = "CreationDate";
    public static final String LAST_UPDATE = "LastUpdate";
    public static final String ACTIVATION_DATE = "ActivationDate";
    public static final String DEACTIVATION_DATE = "DeactivationDate";

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    @JsonProperty(TAG_NAME)
    private String name;

    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    @JsonProperty(TAG_STATUS)
    private ProfileStatus status;

    @JsonProperty(TAG_FORMAT)
    private ProfileFormat format;

    @JsonProperty(TAG_PATH)
    private String path;

    @JsonProperty(CREATION_DATE)
    private String creationdate;

    @JsonProperty(LAST_UPDATE)
    private String lastupdate;

    @JsonProperty(ACTIVATION_DATE)
    private String activationdate;

    @JsonProperty(DEACTIVATION_DATE)
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
    public String getId() {
        return id;
    }

    /**
     * @param id value to set field
     * @return this
     */
    public ProfileModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * @param tenant value to set working tenant
     * @return this
     */
    public ProfileModel setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
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
