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
package fr.gouv.vitam.common.database.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for metadata documents fields (units & object groups).
 */
public class MetadataDocumentHelper {

    private enum ComputedGraphUnitFields {
        US("_us"),
        SPS("_sps"),
        GRAPH("_graph"),
        GRAPH_LAST_PERSISTED_DATE("_glpd"),
        US_SP("_us_sp"),
        MIN("_min"),
        MAX("_max"),
        UDS("_uds");

        private final String fieldName;

        ComputedGraphUnitFields(String fieldName) {
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


    private final static List<String> computedGraphUnitFields;
    private final static List<String> computedGraphObjectGroupFields;

    static {
        computedGraphUnitFields = ListUtils.unmodifiableList(
            Arrays.stream(ComputedGraphUnitFields.values()).map(ComputedGraphUnitFields::getFieldName)
                .collect(Collectors.toList()));

        computedGraphObjectGroupFields = ListUtils.unmodifiableList(
            Arrays.stream(ComputedGraphObjectGroupFields.values()).map(ComputedGraphObjectGroupFields::getFieldName)
                .collect(Collectors.toList()));
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
     * Removes computed graph fields from unit json
     *
     * @param unitJson
     */
    public static void removeComputedGraphFieldsFromUnit(JsonNode unitJson) {
        if (!unitJson.isObject()) {
            throw new IllegalArgumentException("Expected unit object json");
        }

        ObjectNode unit = (ObjectNode) unitJson;
        unit.remove(getComputedGraphUnitFields());
    }

    /**
     * Removes computed graph fields from got json
     *
     * @param objectGroupJson
     */
    public static void removeComputedGraphFieldsFromObjectGroup(JsonNode objectGroupJson) {
        if (!objectGroupJson.isObject()) {
            throw new IllegalArgumentException("Expected object group object json");
        }

        ObjectNode objectGroup = (ObjectNode) objectGroupJson;
        objectGroup.remove(getComputedGraphUnitFields());
    }
}
