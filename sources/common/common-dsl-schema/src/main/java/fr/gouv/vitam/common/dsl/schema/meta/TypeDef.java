/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.dsl.schema.meta;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Definition of a type of the schema.
 */
public class TypeDef implements TDObject, TDArray, TDUnion, TDKeyChoice, TDTypeChoice, TDAnyKey, TDEnum {
    /**
     * Type of type
     */
    public enum Kind {
        OBJECT, ARRAY, UNION, KEY_CHOICE, TYPE_CHOICE, ANY_KEY, REFERENCE, ENUM
    }

    private String reference;
    private Map<String, Property> object;
    private TypeDef array;
    private int min = 0; // for array, object and anykey only
    private int max = Integer.MAX_VALUE; // for array, object and anykey only
    private List<TypeDef> union;
    private Map<String, Property> keychoice;
    private Map<TypeName, TypeDef> typechoice;
    private TypeDef anykey;
    private List<JsonNode> enums;

    /**
     * Constructor used by jackson for complex types (object, array...)
     */
    public TypeDef() {
        // nothing here
    }

    /**
     * Constructor used by jackson for reference type (described by a simple string)
     */
    public TypeDef(String reference) {
        this.reference = reference;
    }

    private Kind kind;

    /**
     * @return The type of the TypeDef, of null is the object is not yet initialized
     */
    public Kind getKind() {
        if (kind != null)
            return kind;
        else {
            // lazy init
            if (reference != null)
                kind = Kind.REFERENCE;
            else if (object != null)
                kind = Kind.OBJECT;
            else if (array != null)
                kind = Kind.ARRAY;
            else if (union != null)
                kind = Kind.UNION;
            else if (keychoice != null)
                kind = Kind.KEY_CHOICE;
            else if (typechoice != null)
                kind = Kind.TYPE_CHOICE;
            else if (anykey != null)
                kind = Kind.ANY_KEY;
            else if (enums != null)
                kind = Kind.ENUM;
            return kind;
        }
    }

    private void checkKind() {
        Kind kind = getKind();
        if (kind != null) {
            throw new IllegalArgumentException("Type already set: " + kind);
        }
    }

    public String getReference() {
        return reference;
    }

    @Override
    public Map<String, Property> getObject() {
        return object;
    }

    public void setObject(Map<String, Property> object) {
        checkKind();
        this.object = object;

        // Send diagnostic info into the Property
        for (Map.Entry<String, Property> entry : object.entrySet()) {
            entry.getValue().setName(entry.getKey());
        }
    }

    @Override
    public TypeDef getArray() {
        return array;
    }

    public void setArray(TypeDef array) {
        checkKind();
        this.array = array;
    }

    public int getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

     public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public List<TypeDef> getUnion() {
        return union;
    }

    public void setUnion(List<TypeDef> union) {
        checkKind();
        this.union = union;
    }

    @Override
    public Map<String, Property> getKeychoice() {
        return keychoice;
    }

    public void setKeychoice(Map<String, Property> keychoice) {
        checkKind();
        this.keychoice = keychoice;

        if (min == 0) {
            min = 1; // TODO Document the fact that min is 1 by default for keychoice
        }

        // Send diagnostic info into the Property
        for (Map.Entry<String, Property> entry : keychoice.entrySet()) {
            entry.getValue().setName(entry.getKey());
        }
    }

    @Override
    public Map<TypeName, TypeDef> getTypechoice() {
        return typechoice;
    }

    public void setTypechoice(Map<TypeName, TypeDef> typechoice) {
        checkKind();
        this.typechoice = typechoice;
    }

    @Override
    public TypeDef getAnykey() {
        return anykey;
    }

    public void setAnykey(TypeDef anykey) {
        checkKind();
        this.anykey = anykey;
    }

    @Override
    public List<JsonNode> getEnum() {
        return enums;
    }

    public void setEnum(List<JsonNode> enums) {
        checkKind();
        this.enums = enums;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    private void toString(StringBuilder builder) {
        Kind kind = getKind();
        if (kind == null) {
            builder.append("#not_initialized");
        } else {
            switch (kind) {
                case REFERENCE: {
                    builder.append(getReference());
                    break;
                }
                case ARRAY: {
                    builder.append(getArray().toString());
                    builder.append("[]");
                    break;
                }
                case OBJECT: {
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<String, Property> entry : getObject().entrySet()) {
                        if (notFirst)
                            builder.append(", ");
                        builder.append(entry.getKey());
                        builder.append(": ");
                        entry.getValue().getType().toString(builder);
                        notFirst = true;
                    }
                    builder.append("}");
                    break;
                }
                case KEY_CHOICE: {
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<String, Property> entry : getKeychoice().entrySet()) {
                        if (notFirst)
                            builder.append("}|{");
                        builder.append(entry.getKey());
                        builder.append(": ");
                        entry.getValue().getType().toString(builder);
                        notFirst = true;
                    }
                    builder.append("}");
                    break;
                }
                case UNION: {
                    boolean notFirst = false;
                    for (TypeDef item : getUnion()) {
                        if (notFirst)
                            builder.append(" & ");
                        item.toString(builder);
                        notFirst = true;
                    }
                    break;
                }
                case TYPE_CHOICE: {
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<TypeName, TypeDef> entry : getTypechoice().entrySet()) {
                        if (notFirst)
                            builder.append("}|{");
                        builder.append(entry.getKey());
                        builder.append("-> ");
                        entry.getValue().toString(builder);
                        notFirst = true;
                    }
                    builder.append("}");
                    break;
                }
                case ANY_KEY: {
                    builder.append("{[key]: ");
                    builder.append(getAnykey().toString());
                    builder.append("}");
                    break;
                }
                case ENUM: {
                    boolean notFirst = false;
                    for (JsonNode item : getEnum()) {
                        if (notFirst)
                            builder.append(" | ");
                        builder.append(item);
                        notFirst = true;
                    }
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Short version of {@link #toString()}, useful for DSL query debugging
     */
    public String debugInfo() {
        Kind kind = getKind();
        if (kind == null) {
            return "#not_initialized";
        } else {
            switch (kind) {
                case REFERENCE: {
                    return getReference();
                }
                case ARRAY: {
                    return getArray().debugInfo() + "[]";
                }
                case OBJECT: {
                    StringBuilder builder = new StringBuilder();
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<String, Property> entry : getObject().entrySet()) {
                        if (notFirst)
                            builder.append(", ");
                        builder.append(entry.getKey());
                        builder.append(": ...");
                        notFirst = true;
                    }
                    builder.append("}");
                    return builder.toString();
                }
                case KEY_CHOICE: {
                    StringBuilder builder = new StringBuilder();
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<String, Property> entry : getKeychoice().entrySet()) {
                        if (notFirst)
                            builder.append("}|{");
                        builder.append(entry.getKey());
                        builder.append(": ...");
                        notFirst = true;
                    }
                    builder.append("}");
                    return builder.toString();
                }
                case UNION: {
                    StringBuilder builder = new StringBuilder();
                    boolean notFirst = false;
                    for (TypeDef item : getUnion()) {
                        if (notFirst)
                            builder.append(" & ");
                        builder.append(item.debugInfo());
                        notFirst = true;
                    }
                    return builder.toString();
                }
                case TYPE_CHOICE: {
                    StringBuilder builder = new StringBuilder();
                    builder.append("{");
                    boolean notFirst = false;
                    for (Map.Entry<TypeName, TypeDef> entry : getTypechoice().entrySet()) {
                        if (notFirst)
                            builder.append("}|{");
                        builder.append(entry.getKey());
                        builder.append("-> ");
                        builder.append(entry.getValue().debugInfo());
                        notFirst = true;
                    }
                    builder.append("}");
                    return builder.toString();
                }
                case ANY_KEY: {
                    return "{[key]: " + getAnykey().debugInfo() + "}";
                }
                case ENUM: {
                    StringBuilder builder = new StringBuilder();
                    boolean notFirst = false;
                    for (JsonNode item : getEnum()) {
                        if (notFirst)
                            builder.append(" | ");
                        builder.append(item);
                        notFirst = true;
                    }
                    return builder.toString();
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

}
