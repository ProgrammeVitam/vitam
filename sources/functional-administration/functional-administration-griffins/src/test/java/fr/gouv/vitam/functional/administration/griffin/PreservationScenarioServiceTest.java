package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationScenarioServiceTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private MongoDbAccessReferential mongoDbAccess;
    @Mock private FunctionalBackupService functionalBackupService;
    @Mock private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock private LogbookOperationsClient logbookOperationsClient;

    private PreservationScenarioService preservationScenarioService;

    @Before
    public void setUp() {
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

    @Test
    @RunWithCustomExecutor
    public void shouldImportScenario() throws Exception {
        GUID guid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(1);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = new ArrayList<>();
        String modelString1 =
            "{\"#id\":\"Id1\",\"Name\":\"1\",\"Identifier\":\"IDENTIFIER1\",\"CreationDate\":\"25/10/2010\",\"LastUpdate\":\"25/10/2010\"}";
        String modelString2 =
            "{\"#id\":\"Id2\",\"Name\":\"2\",\"Identifier\":\"IDENTIFIER2\",\"CreationDate\":\"25/10/2010\",\"LastUpdate\":\"25/10/2010\"}";
        String modelString3 =
            "{\"#id\":\"Id3\",\"Name\":\"3\",\"Identifier\":\"IDENTIFIER3\",\"CreationDate\":\"25/10/2010\",\"LastUpdate\":\"25/10/2010\"}";


        allPreservationScenarioInDatabase.add(JsonHandler.getFromString(modelString2, PreservationScenarioModel.class));
        allPreservationScenarioInDatabase.add(JsonHandler.getFromString(modelString3, PreservationScenarioModel.class));

        List<PreservationScenarioModel> listToImport = new ArrayList<>();
        listToImport.add(getFromString(modelString1, PreservationScenarioModel.class));
        listToImport.add(getFromString(modelString2, PreservationScenarioModel.class));
        listToImport.add(getFromString(modelString3, PreservationScenarioModel.class));

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class))
            .thenReturn(allPreservationScenarioInDatabase);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        RequestResponse<PreservationScenarioModel> requestResponse =
            preservationScenarioService.importScenarios(listToImport);

        ArgumentCaptor<LogbookOperationParameters> event1Captor = forClass(LogbookOperationParameters.class);
        ArgumentCaptor<LogbookOperationParameters> event2Captor = forClass(LogbookOperationParameters.class);

        //Then
        JsonNode result = JsonHandler.toJsonNode(requestResponse);
        int total = result.get("$hits").get("total").asInt();

        assertThat(total).isEqualTo(3);

        verify(logbookOperationsClient, times(1)).create(event1Captor.capture());
        verify(logbookOperationsClient, times(1)).update(event2Captor.capture());

        assertThat(event1Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_PRESERVATION_SCENARIO.STARTED");
        assertThat(event2Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_PRESERVATION_SCENARIO.OK");

        verify(functionalBackupService).saveCollectionAndSequence(guid, "STP_BACKUP_SCENARIO", PRESERVATION_SCENARIO, guid.getId());
    }


    @Test
    public void shouldGetScenarioById() throws Exception {
        //Given
        DbRequestResult dbRequestResult = new DbRequestResult();
        when(mongoDbAccess.findDocuments(any(), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);
        //When
        RequestResponse<PreservationScenarioModel> preservationScenario =
            preservationScenarioService.findPreservationScenario(getFromString("{}"));
        //Then
        assertThat(preservationScenario).isNotNull();
    }

}
