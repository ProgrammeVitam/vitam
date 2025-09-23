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

import java.util.List;

/**
 * POJO java for schema response
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SchemaResponse {

    /**
     * Collections Tag
     */
    public static final String TAG_COLLECTION = "Collection";

    /**
     * Identifier Tag
     */
    public static final String TAG_FIELD_NAME = "FieldName";

    /**
     * Seda field Tag
     */
    public static final String TAG_SEDAFIELD = "SedaField";

    /**
     * Seda field Tag
     */
    public static final String TAG_APIFIELD = "ApiField";
    public static final String TAG_CATEGORY = "Category";
    public static final String TAG_API_PATH = "ApiPath";
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
     * ShortName Tag
     */
    public static final String TAG_SHORT_NAME = "ShortName";

    /**
     * SedaVersion tag
     */
    public static final String TAG_SEDA_VERSIONS = "SedaVersions";
    public static final String TAG_PATH = "Path";

    /**
     * StringSize tag for string fields: short, medium, and large
     */
    public static final String TAG_STRING_TYPE_SIZE = "StringSize";

    /**
     * TypeDetail tag
     */
    public static final String TAG_TYPE_DETAIL = "TypeDetail";

    /**
     * pathCardinality tag
     */
    public static final String TAG_PATH_CARDINALITY = "Cardinality";

    public static final String TAG_CUSTOM_SEARCH_TYPES = "CustomSearchTypes";

    /**
     * Creation date tag
     */
    public static final String TAG_CREATION_DATE = "CreationDate";

    /**
     * Last update date tag
     */
    public static final String TAG_LAST_UPDATE_DATE = "LastUpdate";

    /**
     * The fieldName
     */
    @JsonProperty(TAG_FIELD_NAME)
    private String fieldName;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * The seda field
     */
    @JsonProperty(TAG_SEDAFIELD)
    private String sedaField;

    /**
     * The api field
     */

    @JsonProperty(TAG_COLLECTION)
    private String collection;

    @JsonProperty(TAG_API_PATH)
    private String apiPath;

    @JsonProperty(TAG_APIFIELD)
    private String apiField;

    @JsonProperty(TAG_CATEGORY)
    private SchemaCategory category;

    /**
     * The description
     */
    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    @JsonProperty(TAG_PATH_CARDINALITY)
    private SchemaCardinality cardinality;

    /**
     * The type
     */
    @JsonProperty(TAG_TYPE)
    private SchemaType type;

    /**
     * The origin
     */
    @JsonProperty(TAG_ORIGIN)
    private SchemaOrigin origin;

    @JsonProperty(TAG_SHORT_NAME)
    private String shortName;

    @JsonProperty(TAG_PATH)
    private String path;

    @JsonProperty(TAG_SEDA_VERSIONS)
    private List<String> sedaVersions;

    @JsonProperty(TAG_STRING_TYPE_SIZE)
    private SchemaStringSizeType stringSize;

    @JsonProperty(TAG_TYPE_DETAIL)
    private SchemaTypeDetail typeDetail;

    @JsonProperty(TAG_CUSTOM_SEARCH_TYPES)
    private List<String> customSearchTypes;

    @JsonProperty(TAG_CREATION_DATE)
    private String creationDate;

    @JsonProperty(TAG_LAST_UPDATE_DATE)
    private String lastUpdate;

    /**
     * Constructor without fields use for jackson
     */
    public SchemaResponse() {
        super();
    }

    public String getFieldName() {
        return fieldName;
    }

    public SchemaResponse setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getSedaField() {
        return sedaField;
    }

    public SchemaResponse setSedaField(String sedaField) {
        this.sedaField = sedaField;
        return this;
    }

    public String getApiField() {
        return apiField;
    }

    public SchemaResponse setApiField(String apiField) {
        this.apiField = apiField;
        return this;
    }

    public SchemaCategory getCategory() {
        return category;
    }

    public SchemaResponse setCategory(SchemaCategory category) {
        this.category = category;
        return this;
    }

    public String getApiPath() {
        return apiPath;
    }

    public SchemaResponse setApiPath(String apiPath) {
        this.apiPath = apiPath;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SchemaResponse setDescription(String description) {
        this.description = description;
        return this;
    }

    public SchemaOrigin getOrigin() {
        return origin;
    }

    public SchemaResponse setOrigin(SchemaOrigin origin) {
        this.origin = origin;
        return this;
    }

    public SchemaType getType() {
        return type;
    }

    public SchemaResponse setType(SchemaType type) {
        this.type = type;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public SchemaResponse setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getCollection() {
        return collection;
    }

    public SchemaResponse setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public SchemaCardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(SchemaCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public SchemaStringSizeType getStringSize() {
        return stringSize;
    }

    public void setStringSize(SchemaStringSizeType stringSize) {
        this.stringSize = stringSize;
    }

    public SchemaTypeDetail getTypeDetail() {
        return typeDetail;
    }

    public void setTypeDetail(SchemaTypeDetail typeDetail) {
        this.typeDetail = typeDetail;
    }

    public Integer getTenant() {
        return tenant;
    }

    public SchemaResponse setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getSedaVersions() {
        return sedaVersions;
    }

    public void setSedaVersions(List<String> sedaVersions) {
        this.sedaVersions = sedaVersions;
    }

    public List<String> getCustomSearchTypes() {
        return customSearchTypes;
    }

    public SchemaResponse setCustomSearchTypes(List<String> customSearchTypes) {
        this.customSearchTypes = customSearchTypes;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public SchemaResponse setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public SchemaResponse setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }
}
