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
package fr.gouv.vitam.common.model.administration.preservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * GriffinModel class
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GriffinModel {

    private static final String TAG_DESCRIPTION = "Description";

    private static final String TAG_NAME = "Name";

    public static final String TAG_IDENTIFIER = "Identifier";

    public static final String TAG_CREATION_DATE = "CreationDate";

    public static final String TAG_LAST_UPDATE = "LastUpdate";

    private static final String TAG_EXECUTABLE_VERSION = "ExecutableVersion";

    private static final String TAG_EXECUTABLE_NAME = "ExecutableName";

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @JsonProperty(TAG_NAME)
    private String name;

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    @JsonProperty(TAG_CREATION_DATE)
    private String creationDate;

    @JsonProperty(TAG_LAST_UPDATE)
    private String lastUpdate;

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @JsonProperty(TAG_EXECUTABLE_NAME)
    private String executableName;

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @JsonProperty(TAG_EXECUTABLE_VERSION)
    private String executableVersion;

    public GriffinModel() {
        // empty constructor
    }

    public GriffinModel(
        @NotNull @NotEmpty String name,
        @NotNull @NotEmpty String identifier,
        @NotNull @NotEmpty String executableName,
        @NotNull @NotEmpty String executableVersion
    ) {
        this.name = name;
        this.identifier = identifier;
        this.executableName = executableName;
        this.executableVersion = executableVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public GriffinModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getExecutableName() {
        return executableName;
    }

    public void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    public String getExecutableVersion() {
        return executableVersion;
    }

    public void setExecutableVersion(String executableVersion) {
        this.executableVersion = executableVersion;
    }
}
