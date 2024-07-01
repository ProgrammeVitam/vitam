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
package fr.gouv.vitam.common.database.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for metadata documents fields (units &amp; object groups).
 */
public class MetadataDocumentHelper {

    public static final String STORAGE_KEY = "_storage";
    public static final String STRATEGY_KEY = "strategyId";

    private enum ComputedGraphUnitFields {
        US("_us"),
        SPS("_sps"),
        GRAPH("_graph"),
        GRAPH_LAST_PERSISTED_DATE("_glpd"),
        MIN("_min"),
        MAX("_max"),
        UDS("_uds"),
        COMPUTEDINHERITEDRULES("_computedInheritedRules"),
        VALIDCOMPUTEDINHERITEDRULES("_validComputedInheritedRules");

        private final String fieldName;

        ComputedGraphUnitFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private enum TemporaryUnitFields {
        ELIMINATION("_elimination"),
        COMPUTEDINHERITEDRULES("_computedInheritedRules"),
        VALIDCOMPUTEDINHERITEDRULES("_validComputedInheritedRules"),
        ATOMIC_VERSION("_av"),
        APPROXIMATE_CREATION_DATE("_acd"),
        APPROXIMATE_UPDATE_DATE("_aud");

        private final String fieldName;

        TemporaryUnitFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private enum ComputedGraphObjectGroupFields {
        SPS("_sps"),
        US("_us"),
        GRAPH_LAST_PERSISTED_DATE("_glpd");

        private final String fieldName;

        ComputedGraphObjectGroupFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private enum TemporaryObjectGroupFields {
        ATOMIC_VERSION("_av"),
        APPROXIMATE_CREATION_DATE("_acd"),
        APPROXIMATE_UPDATE_DATE("_aud");

        private final String fieldName;

        TemporaryObjectGroupFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private static final List<String> computedGraphUnitFields;
    private static final List<String> computedGraphObjectGroupFields;
    private static final Set<String> temporaryUnitFields;
    private static final Set<String> computedUnitFields;
    private static final Set<String> temporaryObjectGroupFields;
    private static final Set<String> computedObjectGroupFields;

    static {
        computedGraphUnitFields = ListUtils.unmodifiableList(
            Arrays.stream(ComputedGraphUnitFields.values())
                .map(ComputedGraphUnitFields::getFieldName)
                .collect(Collectors.toList())
        );

        computedGraphObjectGroupFields = ListUtils.unmodifiableList(
            Arrays.stream(ComputedGraphObjectGroupFields.values())
                .map(ComputedGraphObjectGroupFields::getFieldName)
                .collect(Collectors.toList())
        );

        temporaryUnitFields = SetUtils.unmodifiableSet(
            Arrays.stream(TemporaryUnitFields.values())
                .map(TemporaryUnitFields::getFieldName)
                .collect(Collectors.toSet())
        );

        temporaryObjectGroupFields = SetUtils.unmodifiableSet(
            Arrays.stream(TemporaryObjectGroupFields.values())
                .map(TemporaryObjectGroupFields::getFieldName)
                .collect(Collectors.toSet())
        );

        computedUnitFields = SetUtils.unmodifiableSet(
            new HashSet<>(CollectionUtils.union(computedGraphUnitFields, temporaryUnitFields))
        );

        computedObjectGroupFields = SetUtils.unmodifiableSet(
            new HashSet<>(CollectionUtils.union(computedGraphObjectGroupFields, temporaryObjectGroupFields))
        );
    }

    /**
     * @return the list of computed graph unit fields
     */
    public static List<String> getComputedGraphUnitFields() {
        return computedGraphUnitFields;
    }

    /**
     * @return the list of computed graph object group fields
     */
    public static List<String> getComputedGraphObjectGroupFields() {
        return computedGraphObjectGroupFields;
    }

    /**
     * @return the list of temporary unit fields
     */
    public static Set<String> getTemporaryUnitFields() {
        return temporaryUnitFields;
    }

    /**
     * @return the list of all unit computed fields (computed graph fields + temporary fields)
     */
    public static Set<String> getComputedUnitFields() {
        return computedUnitFields;
    }

    /**
     * @return the list of all object group computed fields (computed graph fields + temporary fields)
     */
    public static Set<String> getComputedObjectGroupFields() {
        return computedObjectGroupFields;
    }

    /**
     * Removes computed fields (graph, elimination indexation... ) from unit json
     *
     * @param unitJson
     */
    public static void removeComputedFieldsFromUnit(JsonNode unitJson) {
        if (!unitJson.isObject()) {
            throw new IllegalArgumentException("Expected unit object json");
        }

        ObjectNode unit = (ObjectNode) unitJson;
        unit.remove(computedUnitFields);
    }

    /**
     * Removes computed graph fields from got json
     *
     * @param objectGroupJson
     */
    public static void removeComputedFieldsFromObjectGroup(JsonNode objectGroupJson) {
        if (!objectGroupJson.isObject()) {
            throw new IllegalArgumentException("Expected object group object json");
        }

        ObjectNode objectGroup = (ObjectNode) objectGroupJson;
        objectGroup.remove(computedObjectGroupFields);
    }

    /**
     * Retrieve the strategyId from a raw unit or got json
     *
     * @param documentJson unit or got json
     * @return strategyId
     */
    public static String getStrategyIdFromRawUnitOrGot(JsonNode documentJson) {
        if (documentJson == null || !documentJson.isObject()) {
            throw new IllegalArgumentException("Expected object json");
        }
        ObjectNode document = (ObjectNode) documentJson;
        if (
            !document.has(STORAGE_KEY) ||
            !document.get(STORAGE_KEY).has(STRATEGY_KEY) ||
            !document.get(STORAGE_KEY).get(STRATEGY_KEY).isTextual()
        ) {
            throw new IllegalArgumentException("Expected storage/strategy information in MD document");
        }
        return document.get(STORAGE_KEY).get(STRATEGY_KEY).asText();
    }

    /**
     * Retrieve the strategyId from a unit json
     *
     * @param unitJson unit json
     * @return strategyId
     */
    public static String getStrategyIdFromUnit(JsonNode unitJson) {
        if (unitJson == null || !unitJson.isObject()) {
            throw new IllegalArgumentException("Expected unit object json");
        }
        ObjectNode unit = (ObjectNode) unitJson;
        if (
            !unit.has(VitamFieldsHelper.storage()) ||
            !unit.get(VitamFieldsHelper.storage()).has(STRATEGY_KEY) ||
            !unit.get(VitamFieldsHelper.storage()).get(STRATEGY_KEY).isTextual()
        ) {
            throw new IllegalArgumentException("Expected storage/strategy information in unit");
        }
        return unit.get(VitamFieldsHelper.storage()).get(STRATEGY_KEY).asText();
    }
}
