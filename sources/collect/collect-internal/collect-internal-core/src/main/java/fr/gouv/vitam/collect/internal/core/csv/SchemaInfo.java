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

package fr.gouv.vitam.collect.internal.core.csv;

import java.util.Objects;

public final class SchemaInfo {

    private final String sedaPath;
    private final String apiPath;
    private final String apiField;
    private final boolean isObject;
    private final boolean isArray;
    private final boolean isExternal;

    public SchemaInfo(
        String sedaPath,
        String apiPath,
        String apiField,
        boolean isObject,
        boolean isArray,
        boolean isExternal
    ) {
        this.sedaPath = sedaPath;
        this.apiPath = apiPath;
        this.apiField = apiField;
        this.isObject = isObject;
        this.isArray = isArray;
        this.isExternal = isExternal;
    }

    public String sedaPath() {
        return sedaPath;
    }

    public String apiPath() {
        return apiPath;
    }

    public String apiField() {
        return apiField;
    }

    public boolean isObject() {
        return isObject;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isExternal() {
        return isExternal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SchemaInfo) obj;
        return (
            Objects.equals(this.sedaPath, that.sedaPath) &&
            Objects.equals(this.apiPath, that.apiPath) &&
            Objects.equals(this.apiField, that.apiField) &&
            this.isObject == that.isObject &&
            this.isArray == that.isArray &&
            this.isExternal == that.isExternal
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(sedaPath, apiPath, apiField, isObject, isArray, isExternal);
    }

    @Override
    public String toString() {
        return (
            "SchemaInfo[" +
            "sedaPath=" +
            sedaPath +
            ", " +
            "apiPath=" +
            apiPath +
            ", " +
            "apiField=" +
            apiField +
            ", " +
            "isObject=" +
            isObject +
            ", " +
            "isArray=" +
            isArray +
            ", " +
            "isExternal=" +
            isExternal +
            ']'
        );
    }
}
