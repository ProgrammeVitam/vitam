/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class VitamDescriptionType {

    private final String path;
    private final String pathRegex;
    private final VitamType type;
    private final VitamCardinality cardinality;
    private final boolean indexed;

    @JsonCreator
    public VitamDescriptionType(
        @JsonProperty("path") String path,
        @JsonProperty("pathRegex") String pathRegex,
        @JsonProperty("type") VitamType type,
        @JsonProperty("cardinality") VitamCardinality cardinality,
        @JsonProperty("indexed") boolean indexed
    ) {
        this.path = path;
        this.pathRegex = pathRegex;
        this.type = type;
        this.cardinality = cardinality;
        this.indexed = indexed;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("pathRegex")
    public String getPathRegex() {
        return pathRegex;
    }

    @JsonProperty("type")
    public VitamType getType() {
        return type;
    }

    @JsonProperty("cardinality")
    public VitamCardinality getCardinality() {
        return cardinality;
    }

    @JsonProperty("indexed")
    public boolean isIndexed() {
        return indexed;
    }

    @JsonIgnore
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VitamDescriptionType that = (VitamDescriptionType) o;
        return (
            indexed == that.indexed && path.equals(that.path) && type == that.type && cardinality == that.cardinality
        );
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return Objects.hash(path, pathRegex, type, cardinality, indexed);
    }

    @JsonIgnore
    @Override
    public String toString() {
        return (
            "VitamDescriptionType{" +
            "path='" +
            path +
            '\'' +
            ", pathRegex='" +
            pathRegex +
            '\'' +
            ", type=" +
            type +
            ", cardinality=" +
            cardinality +
            ", indexed=" +
            indexed +
            '}'
        );
    }

    public enum VitamType {
        datetime,
        object,
        nested_object,
        keyword,
        text,
        signed_long,
        signed_double,
        bool,
    }

    public enum VitamCardinality {
        one,
        many,
    }
}
