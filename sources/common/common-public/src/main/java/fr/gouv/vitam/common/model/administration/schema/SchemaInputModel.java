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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for external input schema api
 */
public class SchemaInputModel {

    /**
     * Api path
     */
    public static final String TAG_PATH = "Path";
    /**
     * Description Tag
     */
    public static final String TAG_DESCRIPTION = "Description";
    /**
     * Type Tag
     */
    public static final String TAG_IS_OBJECT = "IsObject";

    /**
     * ShortName Tag
     */
    public static final String TAG_SHORT_NAME = "ShortName";

    /**
     * pathCardinality tag
     */
    public static final String TAG_PATH_CARDINALITY = "Cardinality";

    /**
     * The schema elt description
     */
    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    /**
     * The schema elt cardinality
     */
    @JsonProperty(value = TAG_PATH_CARDINALITY, required = true)
    private SchemaCardinality cardinality;

    /**
     * is object flag to specify that the element is object
     */
    @JsonProperty(TAG_IS_OBJECT)
    private Boolean isObject = false;

    /**
     * short name of the element
     */
    @JsonProperty(value = TAG_SHORT_NAME, required = true)
    private String shortName;

    /**
     * short full path of the element
     */
    @JsonProperty(value = TAG_PATH, required = true)
    private String path;

    public String getDescription() {
        return description;
    }

    public SchemaInputModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public SchemaCardinality getCardinality() {
        return cardinality;
    }

    public SchemaInputModel setCardinality(SchemaCardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public Boolean isObject() {
        return isObject;
    }

    @JsonIgnore
    public Boolean notObject() {
        return !isObject;
    }

    public SchemaInputModel setObject(Boolean isObject) {
        this.isObject = isObject;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public SchemaInputModel setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getPath() {
        return path;
    }

    public SchemaInputModel setPath(String path) {
        this.path = path;
        return this;
    }
}
