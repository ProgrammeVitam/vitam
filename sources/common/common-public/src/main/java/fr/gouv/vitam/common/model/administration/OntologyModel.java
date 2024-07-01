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

import java.util.List;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.Ontology}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OntologyModel {

    /**
     * Document id
     */
    public static final String TAG_HASH_ID = ModelConstants.HASH + ModelConstants.TAG_ID;

    /**
     * Identifier Tag
     */
    public static final String TAG_IDENTIFIER = "Identifier";

    /**
     * Seda field Tag
     */
    public static final String TAG_SEDAFIELD = "SedaField";

    /**
     * Seda field Tag
     */
    public static final String TAG_APIFIELD = "ApiField";
    /**
     * Description Tag
     */
    public static final String TAG_DESCRIPTION = "Description";
    /**
     * Type Tag
     */
    public static final String TAG_TYPE = "Type";

    /**
     * Origin Tag
     */
    public static final String TAG_ORIGIN = "Origin";
    /**
     * CreationDate Tag
     */
    public static final String CREATION_DATE = "CreationDate";
    /**
     * LastUpdate Tag
     */
    public static final String LAST_UPDATE = "LastUpdate";

    /**
     * ShortName Tag
     */
    public static final String TAG_SHORT_NAME = "ShortName";

    /**
     * Collections Tag
     */
    public static final String TAG_COLLECTIONS = "Collections";

    /**
     * unique id
     */
    @JsonProperty(TAG_HASH_ID)
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

    /**
     * The ontology identifier
     */
    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    /**
     * The ontology seda field
     */
    @JsonProperty(TAG_SEDAFIELD)
    private String sedaField;

    /**
     * The ontology api field
     */
    @JsonProperty(TAG_APIFIELD)
    private String apiField;

    /**
     * The ontology description
     */
    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    /**
     * The ontology type
     */
    @JsonProperty(TAG_TYPE)
    private OntologyType type;

    /**
     * The ontology origin
     */
    @JsonProperty(TAG_ORIGIN)
    private OntologyOrigin origin;

    /**
     * The ontology creationdate
     */
    @JsonProperty(CREATION_DATE)
    private String creationdate;

    /**
     * The ontology lastupdate
     */
    @JsonProperty(LAST_UPDATE)
    private String lastupdate;

    @JsonProperty(TAG_SHORT_NAME)
    private String shortName;

    @JsonProperty(TAG_COLLECTIONS)
    private List<String> collections;

    /**
     * Constructor without fields use for jackson
     */
    public OntologyModel() {
        super();
    }

    public String getId() {
        return id;
    }

    public OntologyModel setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getTenant() {
        return tenant;
    }

    public OntologyModel setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public OntologyModel setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    public OntologyModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public String getSedaField() {
        return sedaField;
    }

    public OntologyModel setSedaField(String sedaField) {
        this.sedaField = sedaField;
        return this;
    }

    public String getApiField() {
        return apiField;
    }

    public OntologyModel setApiField(String apiField) {
        this.apiField = apiField;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public OntologyModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public OntologyOrigin getOrigin() {
        return origin;
    }

    public OntologyModel setOrigin(OntologyOrigin origin) {
        this.origin = origin;
        return this;
    }

    public OntologyType getType() {
        return type;
    }

    public OntologyModel setType(OntologyType type) {
        this.type = type;
        return this;
    }

    public String getCreationdate() {
        return creationdate;
    }

    public OntologyModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
        return this;
    }

    public String getLastupdate() {
        return lastupdate;
    }

    public OntologyModel setLastupdate(String lastupdate) {
        this.lastupdate = lastupdate;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public OntologyModel setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public List<String> getCollections() {
        return collections;
    }

    public OntologyModel setCollections(List<String> collections) {
        this.collections = collections;
        return this;
    }

    @Override
    public String toString() {
        return (
            "OntologyModel{" +
            "identifier='" +
            identifier +
            '\'' +
            ", sedaField='" +
            sedaField +
            '\'' +
            ", apiField='" +
            apiField +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", type=" +
            type +
            ", origin=" +
            origin +
            ", creationdate='" +
            creationdate +
            '\'' +
            ", lastupdate='" +
            lastupdate +
            '\'' +
            ", shortName='" +
            shortName +
            '\'' +
            ", collections=" +
            collections +
            '}'
        );
    }
}
