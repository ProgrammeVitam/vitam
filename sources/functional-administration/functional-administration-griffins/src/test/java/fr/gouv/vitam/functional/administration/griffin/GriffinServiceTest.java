package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.functional.administration.common.Griffin;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GriffinServiceTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock MongoDbAccessReferential mongoDbAccess;

    GriffinService griffinService;

    @Before
    public void setUp() throws Exception {

        griffinService = new GriffinService(mongoDbAccess);
    }

    @Test
    public void givenGriffinsInDataBaseShouldCollectInsertUpdateAndDeleteList() throws Exception {

        //Given
        List<GriffinModel> allgriffinInDatabase = new ArrayList<>();
        String modelString1 = "{\"#id\":\"Id1\",\"Name\":\"1\",\"Identifier\":\"IDENTIFIER1\"}";
        String modelString2 = "{\"#id\":\"Id2\",\"Name\":\"2\",\"Identifier\":\"IDENTIFIER2\"}";
        String modelString3 = "{\"#id\":\"Id3\",\"Name\":\"3\",\"Identifier\":\"IDENTIFIER3\"}";

        allgriffinInDatabase.add(JsonHandler.getFromString(modelString2, GriffinModel.class));
        allgriffinInDatabase.add(JsonHandler.getFromString(modelString3, GriffinModel.class));

        List<GriffinModel> listToImport = new ArrayList<>();

        listToImport.add(JsonHandler.getFromString(modelString1, GriffinModel.class));
        listToImport.add(JsonHandler.getFromString(modelString2, GriffinModel.class));

        List<GriffinModel> listToInsert = new ArrayList<>();
        List<GriffinModel> listToUpdate = new ArrayList<>();
        List<String> listToDelete = new ArrayList<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allgriffinInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        griffinService.classifyDataInInsertUpdateOrDeleteLists(listToImport, listToInsert, listToUpdate, listToDelete);

        //Then
        assertThat(listToDelete.size()).isEqualTo(1);
        assertThat(listToInsert.size()).isEqualTo(1);
        assertThat(listToUpdate.size()).isEqualTo(1);

    }
}
