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
package fr.gouv.vitam.metadata.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.rules.UnitRuleModel;
import fr.gouv.vitam.common.utils.JsonSorter;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String expectedDataSetResult = "ComputeInheritedRules/TestCase1_SingleUnit_NoRules/ExpectedResult.json";

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
        String inputDataSet = "ComputeInheritedRules/TestCase5_SimpleHierarchy_PreventInheritance/Input.json";
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
        Map<String, UnitRuleModel> inputUnitRuleModelMap = loadInputDataSet(inputDataSet);
        ComputeInheritedRuleService instance = new ComputeInheritedRuleService();

        // When
        Map<String, UnitInheritedRulesResponseModel> response = instance.computeInheritedRules(inputUnitRuleModelMap);

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
        Map<String, UnitInheritedRulesResponseModel> response,
        String filename
    ) throws IOException, InvalidParseOperationException {
        JsonNode actualJson = JsonHandler.toJsonNode(response);
        JsonNode expectedJson = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(filename));

        List<String> orderedKeys = Arrays.asList("PropertyName", "Rule", "UnitId");
        JsonSorter.sortJsonEntriesByKeys(actualJson, orderedKeys);
        JsonSorter.sortJsonEntriesByKeys(expectedJson, orderedKeys);

        String actual = JsonHandler.unprettyPrint(actualJson);
        String expected = JsonHandler.unprettyPrint(expectedJson);

        try {
            JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        } catch (AssertionError e) {
            System.out.println("Actual  : " + actual);
            System.out.println("Expected: " + expected);
            throw e;
        }
    }
}
