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
package fr.gouv.vitam.worker.core.plugin.reassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OriginatingAgencyReassignmentServiceTest {

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private MetaDataClient metaDataClient;

    private OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(handlerIO.getMetaDataClient()).thenReturn(metaDataClient);
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Test
    public void testGetParentIdsById_emptyList() {
        Map<String, List<String>> result = originatingAgencyReassignmentService.getParentIdsById(
            Collections.emptyList()
        );
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetAllUnitParents() throws Exception {
        ObjectNode unitNode = JsonHandler.createObjectNode();
        ArrayNode parents = JsonHandler.createArrayNode();
        parents.add("parent1");
        parents.add("parent2");
        unitNode.set(VitamFieldsHelper.allunitups(), parents);
        unitNode.put(VitamFieldsHelper.id(), "someId");

        List<String> result = originatingAgencyReassignmentService.getAllUnitParents(unitNode);
        assertThat(result).containsExactly("parent1", "parent2");
    }

    @Test
    public void testGetUnitsIdsToInvalidateComputedInheritedRules() throws Exception {
        ObjectNode unit1 = JsonHandler.createObjectNode();
        unit1.put(VitamFieldsHelper.id(), "unit1");
        unit1.put(VitamFieldsHelper.validComputedInheritedRules(), true);

        ObjectNode unit2 = JsonHandler.createObjectNode();
        unit2.put(VitamFieldsHelper.id(), "unit2");
        unit2.put(VitamFieldsHelper.validComputedInheritedRules(), false);

        List<JsonNode> units = Arrays.asList(unit1, unit2);
        Set<String> result = originatingAgencyReassignmentService.getUnitsIdsToInvalidateComputedInheritedRules(units);

        assertThat(result).containsExactly("unit1");
    }

    @Test
    public void testComputeNodesIdsWithAtLeastOneParentHavingOriginatingAgencyEqualTo() throws Exception {
        // Setup units with parents
        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );

        JsonNode unitResponse1 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units_list.json")
        );

        List<JsonNode> unitsNodes = new ArrayList<>();
        ArrayNode results = (ArrayNode) unitResponse.get("$results");
        for (JsonNode unitNode : results) {
            unitsNodes.add(unitNode);
        }

        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse1);

        Set<String> result =
            originatingAgencyReassignmentService.computeNodesIdsWithAtLeastOneParentHavingOriginatingAgencyEqualTo(
                handlerIO,
                unitsNodes,
                "oldOriginatingAgency"
            );

        assertThat(result).contains("id_unit_3", "id_unit_5");
    }

    @Test
    public void testBuildUnitsOriginatingAgencyReassignmentUpdateQueries() throws Exception {
        int tenantId = 0;
        String operationId = GUIDFactory.newRequestIdGUID(tenantId).toString();

        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );
        List<JsonNode> unitsNodes = new ArrayList<>();
        ArrayNode results = (ArrayNode) unitResponse.get("$results");
        List<String> unitIds = new ArrayList<>();
        for (JsonNode unitNode : results) {
            unitsNodes.add(unitNode);
            unitIds.add(unitNode.get(VitamFieldsHelper.id()).asText());
        }

        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        List<JsonNode> queries =
            originatingAgencyReassignmentService.buildUnitsOriginatingAgencyReassignmentUpdateQueries(
                handlerIO,
                unitsNodes,
                "SRC",
                "TARGET",
                operationId
            );

        assertThat(queries).hasSize(4);
        for (JsonNode query : queries) {
            ArrayNode roots = (ArrayNode) query.get("$roots");
            assertThat(roots).hasSize(1);
            assertThat(unitIds).contains(roots.get(0).asText());
        }
    }
}
