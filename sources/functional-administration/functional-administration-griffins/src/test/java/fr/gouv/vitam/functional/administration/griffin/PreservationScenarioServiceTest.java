package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreservationScenarioServiceTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock MongoDbAccessReferential mongoDbAccess;
    @Mock FunctionalBackupService functionalBackupService;
    @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock LogbookOperationsClient logbookOperationsClient;

    PreservationScenarioService preservationScenarioService;

    @Before
    public void setUp() throws Exception {
        preservationScenarioService =
            new PreservationScenarioService(mongoDbAccess, functionalBackupService, logbookOperationsClientFactory);

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
    }

    @Test
    public void givenPreservationScenariosInDataBaseShouldCollectInsertUpdateAndDeleteList() throws Exception {

        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = new ArrayList<>();
        String modelString1 = "{\"#id\":\"Id1\",\"Name\":\"1\",\"Identifier\":\"IDENTIFIER1\"}";
        String modelString2 = "{\"#id\":\"Id2\",\"Name\":\"2\",\"Identifier\":\"IDENTIFIER2\"}";
        String modelString3 = "{\"#id\":\"Id3\",\"Name\":\"3\",\"Identifier\":\"IDENTIFIER3\"}";

        allPreservationScenarioInDatabase.add(JsonHandler.getFromString(modelString2, PreservationScenarioModel.class));
        allPreservationScenarioInDatabase.add(JsonHandler.getFromString(modelString3, PreservationScenarioModel.class));

        List<PreservationScenarioModel> listToImport = new ArrayList<>();

        listToImport.add(JsonHandler.getFromString(modelString1, PreservationScenarioModel.class));
        listToImport.add(JsonHandler.getFromString(modelString2, PreservationScenarioModel.class));

        List<PreservationScenarioModel> listToInsert = new ArrayList<>();
        List<PreservationScenarioModel> listToUpdate = new ArrayList<>();
        List<String> listToDelete = new ArrayList<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class))
            .thenReturn(allPreservationScenarioInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        preservationScenarioService
            .classifyDataInInsertUpdateOrDeleteLists(listToImport, listToInsert, listToUpdate, listToDelete);

        //Then
        assertThat(listToDelete.size()).isEqualTo(1);
        assertThat(listToInsert.size()).isEqualTo(1);
        assertThat(listToUpdate.size()).isEqualTo(1);

    }
}
