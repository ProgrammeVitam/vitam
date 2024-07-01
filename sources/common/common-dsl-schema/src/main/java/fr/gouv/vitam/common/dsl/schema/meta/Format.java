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
package fr.gouv.vitam.common.dsl.schema.meta;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Definition of a type of the schema.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    defaultImpl = ShortReferenceFormat.class,
    property = "format"
)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = AnyKeyFormat.class, name = "anykey"),
        @JsonSubTypes.Type(value = ArrayFormat.class, name = "array"),
        @JsonSubTypes.Type(value = EnumFormat.class, name = "enum"),
        @JsonSubTypes.Type(value = KeyChoiceFormat.class, name = "keychoice"),
        @JsonSubTypes.Type(value = ObjectFormat.class, name = "object"),
        @JsonSubTypes.Type(value = TypeChoiceFormat.class, name = "typechoice"),
        @JsonSubTypes.Type(value = TypeChoiceArrayFormat.class, name = "typechoicearray"),
        @JsonSubTypes.Type(value = UnionFormat.class, name = "union"),
        @JsonSubTypes.Type(value = ReferenceFormat.class, name = "ref"),
    }
)
public abstract class Format {

    /**
     * Second phase of initialization, when the Format is associated to the Schema
     *
     * @param schema the schema it belongs to.
     */
    protected abstract void resolve(Schema schema);

    protected void consumeAllFields(JsonNode node, Consumer<String> fieldReport) {
        if (fieldReport != null) {
            for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
                final String fieldname = it.next();
                fieldReport.accept(fieldname);
            }
        }
    }

    public abstract void validate(JsonNode node, Consumer<String> fieldReport, ValidatorEngine validator);

    /**
     * Execute an action on each node of the TypeDef tree. E.g. to resolve type name of KindReference
     *
     * @param consumer the action to do on the node
     */
    public abstract void walk(Consumer<Format> consumer);

    private String name; // only for property formats
    private boolean optional = false;
    private String hint;

    private int min = 0;
    private int max = Integer.MAX_VALUE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
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

    /**
     * Return a short partial description of the type, useful of DSL users
     *
     * @return a description of the type (e.g. {"$foo":..., "$bar":... })
     */
    public abstract String debugInfo();

    @Override
    public String toString() {
        if (name != null) {
            return name + (optional ? "?: " : ": ") + debugInfo();
        }
        return debugInfo();
    }

    private Format reportingType;

    protected void setReportingType(Format format) {
        if (name == null) name = format.name;
        if (hint == null) hint = format.hint;
        reportingType = format;
    }

    public Format getReportingType() {
        return reportingType != null ? reportingType : this;
    }
}
