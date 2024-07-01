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

import java.util.Objects;

/**
 * Data Transfer Object Model of Agency
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgenciesModel {

    public static final String TAG_NAME = "Name";
    public static final String TAG_IDENTIFIER = "Identifier";
    public static final String TAG_DESCRIPTION = "Description";

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.UNDERSCORE + ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @JsonProperty(TAG_NAME)
    private String name;

    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    /**
     * Constructor of AgencyModel
     *
     * @param identifier
     * @param name
     * @param description
     */
    public AgenciesModel(
        @JsonProperty(TAG_IDENTIFIER) String identifier,
        @JsonProperty(TAG_NAME) String name,
        @JsonProperty(TAG_DESCRIPTION) String description,
        @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT) int tenant
    ) {
        this.tenant = tenant;
        this.identifier = identifier;
        this.name = name;
        this.description = description;
    }

    /**
     * empty constructor
     */
    public AgenciesModel() {}

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     * @return AgencyModel
     */
    public AgenciesModel setId(String id) {
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
    public AgenciesModel setTenant(Integer tenant) {
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
     * @return identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier
     * @return AgencyModel
     */
    public AgenciesModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * @return last update of Agency
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description to set
     * @return this
     */
    public AgenciesModel setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgenciesModel that = (AgenciesModel) o;
        return tenant.equals(that.tenant) && identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, identifier, name, description);
    }
}
