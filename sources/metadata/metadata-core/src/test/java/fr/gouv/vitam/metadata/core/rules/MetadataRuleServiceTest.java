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
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.model.MetadataResult;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

        doReturn(computedRulesFromResource("MetadataRuleService/computedUnitRules.json"))
            .when(computeInheritedRuleService)
            .computeInheritedRules(anyMap());

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(exists("Title"));
        select.addUsedProjection("Title");
        JsonNode selectDsl = select.getFinalSelect();

        // When
        MetadataRuleService instance = new MetadataRuleService(computeInheritedRuleService, metadata);
        final MetadataResult metadataResult = instance.selectUnitsWithInheritedRules(selectDsl);

        // Then
        JsonAssert.assertJsonEquals(
            JsonHandler.unprettyPrint(metadataResult.getResults()),
            IOUtils.toString(
                PropertiesUtils.getResourceAsStream("MetadataRuleService/expectedResponse.json"),
                StandardCharsets.UTF_8
            ),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    private MetadataResult responseFromResource(String filename) throws IOException, InvalidParseOperationException {
        final List<JsonNode> list = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(filename),
            List.class,
            JsonNode.class
        );
        return new MetadataResult(
            JsonHandler.createObjectNode(),
            list,
            Collections.emptyList(),
            list.size(),
            null,
            new DatabaseCursor(list.size(), 0, 0)
        );
    }

    private Map<String, UnitInheritedRulesResponseModel> computedRulesFromResource(String resourcesFile)
        throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(resourcesFile),
            Map.class,
            String.class,
            UnitInheritedRulesResponseModel.class
        );
    }
}
