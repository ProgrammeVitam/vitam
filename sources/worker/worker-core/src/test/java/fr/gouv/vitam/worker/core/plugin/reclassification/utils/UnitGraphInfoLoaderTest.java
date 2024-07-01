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
package fr.gouv.vitam.worker.core.plugin.reclassification.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.UnitGraphInfo;
import org.apache.commons.collections4.SetUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UnitGraphInfoLoaderTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MetaDataClient metaDataClient;

    private UnitGraphInfoLoader unitGraphInfoLoader = new UnitGraphInfoLoader();

    private static final String originatingAgency = "MySP";
    private static final String includedRootId = "IncludedRootId";
    private static final String excludedRootId = "ExcludedRootId";

    @Test
    public void testSelectUnitsByQueryDslAndAccessContract() throws Exception {
        // Given
        AccessContractModel accessContractModel = createAccessContractWithRestrictions();

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.addQueries(QueryHelper.in(VitamFieldsHelper.id(), "MyId1", "MyId2", "MyId3"));

        doReturn(buildResponseJsonNode("MyId1", "MyId3")).when(metaDataClient).selectUnits((any()));

        // When
        Set<String> ids = unitGraphInfoLoader.selectUnitsByQueryDslAndAccessContract(
            metaDataClient,
            selectMultiQuery,
            accessContractModel
        );

        // Then
        assertThat(ids).containsExactlyInAnyOrder("MyId1", "MyId3");

        // Insure access contract restrictions applied
        ArgumentCaptor<JsonNode> queryArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient).selectUnits((queryArgumentCaptor.capture()));
        String query = JsonHandler.unprettyPrint(queryArgumentCaptor.getValue());
        assertThat(query).contains(originatingAgency);
        assertThat(query).contains(includedRootId);
        assertThat(query).contains(excludedRootId);
        assertThat(query).contains("MyId1");
    }

    @Test
    public void testSelectUnitsByIdsAndAccessContract_emptySet() throws Exception {
        // Given
        AccessContractModel accessContractModel = createAccessContractWithRestrictions();

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.addQueries(QueryHelper.in(VitamFieldsHelper.id(), "MyId1", "MyId2", "MyId3"));

        doReturn(buildResponseJsonNode()).when(metaDataClient).selectUnits((any()));

        // When
        Set<String> foundIds = unitGraphInfoLoader.selectUnitsByIdsAndAccessContract(
            metaDataClient,
            SetUtils.emptySet(),
            accessContractModel
        );

        // Then
        assertThat(foundIds).isEmpty();
        verifyZeroInteractions(metaDataClient);
    }

    @Test
    public void testSelectUnitsByIdsAndAccessContract_LotsOfUnitIds() throws Exception {
        // Given
        AccessContractModel accessContractModel = createAccessContractWithRestrictions();

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.addQueries(QueryHelper.in(VitamFieldsHelper.id(), "MyId1", "MyId2", "MyId3"));

        Set<String> ids = IntStream.rangeClosed(1, UnitGraphInfoLoader.MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE + 1)
            .mapToObj(i -> "MyId" + i)
            .collect(Collectors.toSet());

        when(metaDataClient.selectUnits(any())).thenReturn(
            buildResponseJsonNode("MyId1", "MyId2"),
            buildResponseJsonNode("MyId1001")
        );

        // When
        Set<String> foundIds = unitGraphInfoLoader.selectUnitsByIdsAndAccessContract(
            metaDataClient,
            ids,
            accessContractModel
        );

        // Then
        assertThat(foundIds).containsExactlyInAnyOrder("MyId1", "MyId2", "MyId1001");
        ArgumentCaptor<JsonNode> queryArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(2)).selectUnits((queryArgumentCaptor.capture()));

        // Insure access contract restrictions applied
        String query = JsonHandler.unprettyPrint(queryArgumentCaptor.getValue());
        assertThat(query).contains(originatingAgency);
        assertThat(query).contains(includedRootId);
        assertThat(query).contains(excludedRootId);
    }

    private AccessContractModel createAccessContractWithRestrictions() {
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryOriginatingAgency(false);
        accessContractModel.setOriginatingAgencies(new HashSet<>(Arrays.asList(originatingAgency)));
        accessContractModel.setRootUnits(new HashSet<>(Arrays.asList(includedRootId)));
        accessContractModel.setExcludedRootUnits(new HashSet<>(Arrays.asList(excludedRootId)));
        return accessContractModel;
    }

    private JsonNode buildResponseJsonNode(String... ids) {
        ArrayNode results = JsonHandler.createArrayNode();
        for (String id : ids) {
            results.add(JsonHandler.createObjectNode().put("#id", id));
        }
        return JsonHandler.createObjectNode().set("$results", results);
    }

    @Test
    public void selectAllUnitGraphByIds() throws Exception {
        // Given : basic graph
        /*
         *    1   4   5
         *    ↑ ↗   ↗ ↑
         *    2 ➝ 6   9
         *    ↑   ↑   ↑
         *    3   7   10
         *    ↑       ↑
         *    8       11
         */

        Map<String, List<String>> unitsWithParents = new HashMap<>();
        unitsWithParents.put("1", Arrays.asList());
        unitsWithParents.put("2", Arrays.asList("1", "4", "6"));
        unitsWithParents.put("3", Arrays.asList("2"));
        unitsWithParents.put("4", Arrays.asList());
        unitsWithParents.put("5", Arrays.asList());
        unitsWithParents.put("6", Arrays.asList("5"));
        unitsWithParents.put("7", Arrays.asList("6"));
        unitsWithParents.put("8", Arrays.asList("3"));
        unitsWithParents.put("9", Arrays.asList("5"));
        unitsWithParents.put("10", Arrays.asList("9"));
        unitsWithParents.put("11", Arrays.asList("10"));

        when(metaDataClient.selectUnits(any())).then(args -> {
            // Parse { $in: "#id": [] } query
            JsonNode dsl = args.getArgument(0);
            SelectParserMultiple parserMultiple = new SelectParserMultiple();
            parserMultiple.parse(dsl);
            ArrayNode jsonIds = (ArrayNode) parserMultiple
                .getRequest()
                .getQueries()
                .get(0)
                .getNode("$in")
                .get(VitamFieldsHelper.id());
            Set<String> ids = new HashSet<>();
            jsonIds.elements().forEachRemaining(e -> ids.add(e.asText()));

            // Return expected units by ids
            ArrayNode results = JsonHandler.createArrayNode();
            for (String id : ids) {
                results.add(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), id)
                        .put(VitamFieldsHelper.unitType(), UnitType.INGEST.name())
                        .set(VitamFieldsHelper.unitups(), JsonHandler.toJsonNode(unitsWithParents.get(id)))
                );
            }
            return JsonHandler.createObjectNode().set("$results", results);
        });

        // When
        Map<String, UnitGraphInfo> unitGraphInfoMap = unitGraphInfoLoader.selectAllUnitGraphByIds(
            metaDataClient,
            new HashSet<>(Arrays.asList("8", "10"))
        );

        // Then
        assertThat(unitGraphInfoMap).hasSize(9);
        assertThat(unitGraphInfoMap.keySet()).containsExactlyInAnyOrder("1", "2", "3", "4", "5", "6", "8", "9", "10");
        // Expected full parent graph loading in 4 invocations
        // > 8, 10
        // > 3, 9
        // > 2, 5
        // > 1, 4, 6
        verify(metaDataClient, times(4)).selectUnits(any());
    }

    @Test
    public void testLoadInheritedHoldRules() throws Exception {
        // Given
        doReturn(buildUnitWithInheritedRuleResponseJsonNode("Unit1", "Unit2"))
            .when(metaDataClient)
            .selectUnitsWithInheritedRules((any()));

        // When
        Map<String, InheritedRuleCategoryResponseModel> inheritedHoldRules = unitGraphInfoLoader.loadInheritedHoldRules(
            metaDataClient,
            Set.of("Unit1", "Unit2")
        );

        // Then
        verify(metaDataClient, times(1)).selectUnitsWithInheritedRules((any()));
        assertThat(inheritedHoldRules).hasSize(2);
        assertThat(inheritedHoldRules).containsOnlyKeys("Unit1", "Unit2");
        assertThat(inheritedHoldRules.get("Unit1").getRules()).hasSize(3);
    }

    @Test
    public void testLoadInheritedHoldRulesBatchProcessing() throws Exception {
        // Given
        int batchSize = UnitGraphInfoLoader.MAX_ELASTIC_SEARCH_IN_REQUEST_SIZE;

        Set<String> ids = IntStream.rangeClosed(1, batchSize + 1).mapToObj(i -> "Unit" + i).collect(Collectors.toSet());

        doReturn(
            buildUnitWithInheritedRuleResponseJsonNode(
                IntStream.rangeClosed(1, batchSize + 1).mapToObj(i -> "Unit" + i).toArray(String[]::new)
            ),
            buildUnitWithInheritedRuleResponseJsonNode("Unit" + (batchSize + 1))
        )
            .when(metaDataClient)
            .selectUnitsWithInheritedRules((any()));

        // When
        Map<String, InheritedRuleCategoryResponseModel> inheritedHoldRules = unitGraphInfoLoader.loadInheritedHoldRules(
            metaDataClient,
            ids
        );

        // Then
        verify(metaDataClient, times(2)).selectUnitsWithInheritedRules((any()));
        assertThat(inheritedHoldRules).hasSize(batchSize + 1);
        assertThat(inheritedHoldRules).containsOnlyKeys(ids.toArray(String[]::new));
        assertThat(inheritedHoldRules.get("Unit1").getRules()).hasSize(3);
    }

    private JsonNode buildUnitWithInheritedRuleResponseJsonNode(String... ids)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonNode testInheritedRules = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("InheritedRules/InheritedHoldRules.json")
        );
        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        for (String id : ids) {
            requestResponseOK.addResult(
                JsonHandler.createObjectNode().put("#id", id).set("InheritedRules", testInheritedRules)
            );
        }
        return requestResponseOK.toJsonNode();
    }
}
