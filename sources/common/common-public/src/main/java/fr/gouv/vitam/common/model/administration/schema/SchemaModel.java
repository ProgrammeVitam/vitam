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
package fr.gouv.vitam.common.model.administration.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.Schema}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SchemaModel {

    /**
     * Document id
     */
    public static final String TAG_HASH_ID = ModelConstants.HASH + ModelConstants.TAG_ID;

    /**
     * Collections Tag
     */
    public static final String TAG_COLLECTION = "Collection";

    /**
     * Description Tag
     */
    public static final String TAG_DESCRIPTION = "Description";

    /**
     * Origin Tag
     */
    public static final String TAG_ORIGIN = "Origin";

    /**
     * ShortName Tag
     */
    public static final String TAG_SHORT_NAME = "ShortName";

    public static final String TAG_PATH = "Path";

    /**
     * pathCardinality tag
     */
    public static final String TAG_PATH_CARDINALITY = "Cardinality";

    /**
     * isObject Tag
     */
    public static final String TAG_IS_OBJECT_NAME = "IsObject";

    /**
     * the creation date of the schema
     */
    public static final String TAG_CREATION_DATE = "CreationDate";
    /**
     * the last update of schema
     */
    public static final String TAG_LAST_UPDATE = "LastUpdate";

    public SchemaModel() {
        super();
    }

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

    @JsonProperty(TAG_COLLECTION)
    private String collection;

    /**
     * The description
     */
    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    @JsonProperty(TAG_PATH_CARDINALITY)
    private SchemaCardinality cardinality;

    /**
     * is object
     */
    @JsonProperty(TAG_IS_OBJECT_NAME)
    private Boolean isObject = false;

    /**
     * The origin of the schema
     */
    @JsonProperty(TAG_ORIGIN)
    private SchemaOrigin origin;

    /**
     * Short name of the element
     */
    @JsonProperty(TAG_SHORT_NAME)
    private String shortName;

    /**
     * The full path
     */
    @JsonProperty(TAG_PATH)
    private String path;

    /**
     * The schema element creation date
     */
    @JsonProperty(TAG_CREATION_DATE)
    private String creationDate;

    /**
     * The schema element last update
     */
    @JsonProperty(TAG_LAST_UPDATE)
    private String lastUpdate;

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SchemaCardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(SchemaCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public Boolean getObject() {
        return isObject;
    }

    public void setObject(Boolean object) {
        isObject = object;
    }

    public SchemaOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(SchemaOrigin origin) {
        this.origin = origin;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public String toString() {
        return (
            "SchemaModel{" +
            "tenant=" +
            tenant +
            ", collection=" +
            collection +
            ", description='" +
            description +
            '\'' +
            ", cardinality=" +
            cardinality +
            ", isObject=" +
            isObject +
            ", origin=" +
            origin +
            ", shortName='" +
            shortName +
            '\'' +
            ", creationdate='" +
            creationDate +
            '\'' +
            ", lastupdate='" +
            lastUpdate +
            '\'' +
            ", path='" +
            path +
            '\'' +
            '}'
        );
    }
}
