package fr.gouv.vitam.metadata.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.metadata.core.MetaDataImpl;

import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataRuleServiceTest {

    @Test
    public void selectUnitsWithInheritedRules_invalidProjectionV1() throws Exception {

        ComputeInheritedRuleService computeInheritedRuleService = mock(ComputeInheritedRuleService.class);
        MetaDataImpl metadata = mock(MetaDataImpl.class);

        MetadataRuleService instance = new MetadataRuleService(computeInheritedRuleService, metadata);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(exists("Title"));
        select.addUsedProjection("Title", BuilderToken.GLOBAL.RULES.exactToken());

        assertThatThrownBy(() -> instance.selectUnitsWithInheritedRules(select.getFinalSelect()))
            .isInstanceOf(InvalidParseOperationException.class)
            .hasMessage("Invalid $rules projection");
    }

    @Test
    public void selectUnitsWithInheritedRules_loadUnitsAndComputeRules() throws Exception {

        // Given
        ComputeInheritedRuleService computeInheritedRuleService = mock(ComputeInheritedRuleService.class);
        MetaDataImpl metadata = mock(MetaDataImpl.class);

        when(metadata.selectUnitsByQuery((any()))).thenReturn(
            responseFromResource("MetadataRuleService/responseSelectUnits.json"),
            responseFromResource("MetadataRuleService/responseSelectUnits_2_4.json"),
            responseFromResource("MetadataRuleService/responseSelectUnits_1_3.json")
        );

        doReturn(computedRulesFromResource(
            "MetadataRuleService/computedUnitRules.json"))
            .when(computeInheritedRuleService)
            .computeInheritedRules(anyMap());

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(exists("Title"));
        select.addUsedProjection("Title");
        JsonNode selectDsl = select.getFinalSelect();

        // When
        MetadataRuleService instance = new MetadataRuleService(computeInheritedRuleService, metadata);
        RequestResponseOK<JsonNode> response =
            (RequestResponseOK<JsonNode>) instance.selectUnitsWithInheritedRules(selectDsl);

        // Then
        JsonAssert.assertJsonEquals(
            JsonHandler.unprettyPrint(response.getResultsAsJsonNodes()),
            IOUtils.toString(PropertiesUtils.getResourceAsStream("MetadataRuleService/expectedResponse.json"),
                StandardCharsets.UTF_8)
            , JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private RequestResponseOK<JsonNode> responseFromResource(String filename)
        throws IOException, InvalidParseOperationException {
        return new RequestResponseOK<>().addAllResults(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(filename), List.class, JsonNode.class));
    }

    private Map computedRulesFromResource(String resourcesFile)
        throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler
            .getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile),
                Map.class, String.class, UnitInheritedRulesResponseModel.class);
    }
}
