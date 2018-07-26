package fr.gouv.vitam.metadata.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.core.rules.model.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.metadata.core.rules.model.UnitRuleModel;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ComputeInheritedRuleServiceTest {

    @Test
    public void computeInheritedRules_empty() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase0_Empty/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase0_Empty/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SingleUnit_NoRules() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase1_SingleUnit_NoRules/Input.json";
        String expectedDataSetResult =
            "ComputeInheritedRules/TestCase1_SingleUnit_NoRules/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SingleUnit_SimpleRules() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase2_SingleUnit_SimpleRules/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase2_SingleUnit_SimpleRules/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SimpleHierarchy_NoRules() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase3_SimpleHierarchy_NoRules/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase3_SimpleHierarchy_NoRules/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SimpleHierarchy_BasicRules() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase4_SimpleHierarchy_BasicRules/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase4_SimpleHierarchy_BasicRules/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SimpleHierarchy_PreventInheritance() throws Exception {

        // Given
        String inputDataSet =
            "ComputeInheritedRules/TestCase5_SimpleHierarchy_PreventInheritance/Input.json";
        String expectedDataSetResult =
            "ComputeInheritedRules/TestCase5_SimpleHierarchy_PreventInheritance/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_SimpleHierarchy_PreventRulesId() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase6_SimpleHierarchy_PreventRulesId/Input.json";
        String expectedDataSetResult =
            "ComputeInheritedRules/TestCase6_SimpleHierarchy_PreventRulesId/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_MultipleParents_MergePaths() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase7_MultipleParents_MergePaths/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase7_MultipleParents_MergePaths/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    @Test
    public void computeInheritedRules_ComplexHierarchy() throws Exception {

        // Given
        String inputDataSet = "ComputeInheritedRules/TestCase8_ComplexHierarchy/Input.json";
        String expectedDataSetResult = "ComputeInheritedRules/TestCase8_ComplexHierarchy/ExpectedResult.json";

        // When / Then
        runTest(inputDataSet, expectedDataSetResult);
    }

    private void runTest(String inputDataSet, String expectedDataSetResult)
        throws InvalidParseOperationException, IOException {
        Map<String, UnitRuleModel> emptyUnitRuleModelMap =
            loadInputDataSet(inputDataSet);
        ComputeInheritedRuleService instance = new ComputeInheritedRuleService();

        // When
        Map<String, UnitInheritedRulesResponseModel> response =
            instance.computeInheritedRules(emptyUnitRuleModelMap);

        // Then
        assertThatResponseMatchesExpected(response, expectedDataSetResult);
    }

    private Map<String, UnitRuleModel> loadInputDataSet(String filename)
        throws FileNotFoundException, InvalidParseOperationException {
        Map<String, UnitRuleModel> result = new HashMap<>();

        JsonNode dataSet = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(filename));
        for (JsonNode unitJson : dataSet) {
            result.put(unitJson.get("#id").asText(), JsonHandler.getFromJsonNode(unitJson, UnitRuleModel.class));
        }
        return result;
    }

    private void assertThatResponseMatchesExpected(
        Map<String, UnitInheritedRulesResponseModel> response, String filename)
        throws IOException, InvalidParseOperationException {

        String actual = JsonHandler.unprettyPrint(reorderRulesAndProperties(JsonHandler.toJsonNode(response)));
        String expected = JsonHandler.unprettyPrint(reorderRulesAndProperties(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(filename))));

        try {
            JsonAssert.assertJsonEquals(expected, actual,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        } catch (AssertionError e) {
            System.out.println("Actual  : " + actual);
            System.out.println("Expected: " + expected);
            throw e;
        }
    }

    private JsonNode reorderRulesAndProperties(JsonNode jsonNode) {

        List<String> sortKeys = Arrays.asList("PropertyName", "Rule", "UnitId");

        if (jsonNode == null)
            return null;

        for (Iterator<JsonNode> it = jsonNode.elements(); it.hasNext(); ) {
            JsonNode value = it.next();
            reorderRulesAndProperties(value);
        }

        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;

            TreeMap<String, JsonNode> entries = new TreeMap<>();
            objectNode.fields().forEachRemaining(
                i -> entries.put(i.getKey(), i.getValue())
            );

            objectNode.removeAll();
            objectNode.setAll(entries);
        }

        if (jsonNode.isArray() && jsonNode.size() > 1 && jsonNode.get(0).isObject()) {

            ArrayNode arrayNode = (ArrayNode) jsonNode;

            List<ObjectNode> items = new ArrayList<>();
            jsonNode.forEach(i -> items.add((ObjectNode) i));

            items.sort((node1, node2) -> {

                for (String sortKey : sortKeys) {
                    if (node1.get(sortKey) != null && node2.get(sortKey) != null) {
                        int sort = node1.get(sortKey).asText().compareTo(node2.get(sortKey).asText());
                        if (sort != 0)
                            return sort;
                    }
                }

                return 1;
            });

            arrayNode.removeAll();
            arrayNode.addAll(items);
        }

        return jsonNode;
    }
}
