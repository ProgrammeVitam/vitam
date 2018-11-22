package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.Griffin;
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
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GriffinServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock private MongoDbAccessReferential mongoDbAccess;

    private GriffinService griffinService;

    @Mock private FunctionalBackupService functionalBackupService;

    @Mock private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock private LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setUp() {

        griffinService = new GriffinService(mongoDbAccess, functionalBackupService, logbookOperationsClientFactory);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
    }

    @Test
    public void givenGriffinsInDataBaseShouldCollectInsertUpdateAndDeleteList() throws Exception {

        //Given
        List<GriffinModel> allGriffinInDatabase = new ArrayList<>();
        String modelString1 = "{\"#id\":\"Id1\",\"Name\":\"1\",\"Identifier\":\"IDENTIFIER1\"}";
        String modelString2 = "{\"#id\":\"Id2\",\"Name\":\"2\",\"Identifier\":\"IDENTIFIER2\"}";
        String modelString3 = "{\"#id\":\"Id3\",\"Name\":\"3\",\"Identifier\":\"IDENTIFIER3\"}";

        allGriffinInDatabase.add(getFromString(modelString2, GriffinModel.class));
        allGriffinInDatabase.add(getFromString(modelString3, GriffinModel.class));

        List<GriffinModel> listToImport = new ArrayList<>();

        listToImport.add(getFromString(modelString1, GriffinModel.class));
        listToImport.add(getFromString(modelString2, GriffinModel.class));

        List<GriffinModel> listToInsert = new ArrayList<>();
        List<GriffinModel> listToUpdate = new ArrayList<>();
        List<String> listToDelete = new ArrayList<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allGriffinInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        griffinService.classifyDataInInsertUpdateOrDeleteLists(listToImport, listToInsert, listToUpdate, listToDelete);

        //Then
        assertThat(listToDelete.size()).isEqualTo(1);
        assertThat(listToInsert.size()).isEqualTo(1);
        assertThat(listToUpdate.size()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportGriffin() throws Exception {
        GUID guid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(1);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        //Given
        List<GriffinModel> allGriffinInDatabase = new ArrayList<>();
        String modelString1 =
            "{\"#id\":\"Id1\",\"Name\":\"1\",\"Identifier\":\"IDENTIFIER1\",\"CreationDate\":\"25/10/2010\",\"LastUpdateDate\":\"25/10/2010\"}";
        String modelString2 =
            "{\"#id\":\"Id2\",\"Name\":\"2\",\"Identifier\":\"IDENTIFIER2\",\"CreationDate\":\"25/10/2010\",\"LastUpdateDate\":\"25/10/2010\"}";
        String modelString3 =
            "{\"#id\":\"Id3\",\"Name\":\"3\",\"Identifier\":\"IDENTIFIER3\",\"CreationDate\":\"25/10/2010\",\"LastUpdateDate\":\"25/10/2010\"}";

        List<GriffinModel> listToImport = new ArrayList<>();

        listToImport.add(getFromString(modelString1, GriffinModel.class));
        listToImport.add(getFromString(modelString2, GriffinModel.class));
        listToImport.add(getFromString(modelString3, GriffinModel.class));

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allGriffinInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        RequestResponse<GriffinModel> requestResponse = griffinService.importGriffin(listToImport);
        ArgumentCaptor<LogbookOperationParameters> event1Captor = forClass(LogbookOperationParameters.class);
        ArgumentCaptor<LogbookOperationParameters> event2Captor = forClass(LogbookOperationParameters.class);

        //Then
        JsonNode result = JsonHandler.toJsonNode(requestResponse);
        int total = result.get("$hits").get("total").asInt();

        assertThat(total).isEqualTo(3);

        verify(logbookOperationsClient, times(1)).create(event1Captor.capture());
        verify(logbookOperationsClient, times(1)).update(event2Captor.capture());

        assertThat(event1Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_GRIFFIN.STARTED");
        assertThat(event2Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_GRIFFIN.OK");

        verify(functionalBackupService).saveCollectionAndSequence(guid, "STP_BACKUP_GRIFFIN", GRIFFIN, guid.getId());
    }

    @Test
    public void shouldGetGriffinById() throws Exception {
        //Given
        DbRequestResult dbRequestResult = new DbRequestResult();
        when(mongoDbAccess.findDocuments(any(), eq(GRIFFIN))).thenReturn(dbRequestResult);
        //When
        RequestResponse<GriffinModel> griffin = griffinService.findGriffin(getFromString("{}"));
        //Then
        assertThat(griffin).isNotNull();
    }
}
