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
package fr.gouv.vitam.functional.administration.core.griffin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.Griffin;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FileFormatModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.guid.GUIDReader.getGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.FORMATS;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GriffinServiceTest {

    private static final TypeReference<List<PreservationScenarioModel>> scenarioTypeRef = new TypeReference<>() {};
    private static final TypeReference<List<GriffinModel>> griffinTypeRef = new TypeReference<>() {};
    private static final TypeReference<List<FileFormatModel>> fileFormatTypeRef = new TypeReference<>() {};
    private static final TypeReference<List<GriffinModel>> valueTypeRef = new TypeReference<>() {};

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private FunctionalBackupService functionalBackupService;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private VitamCollection preservationScenarioCollection;

    @Mock
    private MongoDbAccessReferential mongoDbAccess;

    private GriffinService griffinService;

    @Before
    public void setUp() {
        griffinService = new GriffinService(
            mongoDbAccess,
            functionalBackupService,
            logbookOperationsClientFactory,
            preservationScenarioCollection
        );
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        GUID guid = newGUID();

        getVitamSession().setTenantId(1);
        getVitamSession().setRequestId(guid);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenNameIsNull() {
        //Given
        GriffinModel griffinModel = new GriffinModel(null, "id", "exName", "version");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenNameIsNullOrEmpty() {
        //Given
        GriffinModel griffinModel = new GriffinModel(null, "id", "exName", "version");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");

        //Given
        griffinModel.setName("");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedWhenImportTwoDuplicatedGriffinIdentifiers() {
        //Given
        GriffinModel griffinModel1 = new GriffinModel("name", "id", "exName", "version");
        GriffinModel griffinModel2 = new GriffinModel("name", "id", "exName", "version");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Lists.newArrayList(griffinModel1, griffinModel2)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Duplicate griffin");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenIdentifierIsNullOrEmpty() {
        //Given
        GriffinModel griffinModel = new GriffinModel("name", null, "exName", "version");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");

        //Given
        griffinModel.setIdentifier("");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenDateIsNotCorrect() throws Exception {
        //Given
        List<GriffinModel> listGriffins = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile("KO_griffin_false_date.json"),
            valueTypeRef
        );
        List<GriffinModel> allGriffinInDatabase = new ArrayList<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        givenPreservationScenarioCollectionReturn(Collections.singletonList(new PreservationScenarioModel()));
        given(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).willReturn(allGriffinInDatabase);
        given(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).willReturn(dbRequestResult);
        given(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class)).willReturn(
            new ArrayList<>()
        );
        given(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).willReturn(dbRequestResult);

        // When
        ThrowingCallable importInError = () -> griffinService.importGriffin(listGriffins);

        // Then
        assertThatThrownBy(importInError)
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("GRIFFIN1 Invalid CreationDate : 10 décembre 16");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenExecutableNameIsNullOrEmpty() {
        //Given
        GriffinModel griffinModel = new GriffinModel("name", "id", null, "version");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");

        //Given
        griffinModel.setExecutableName("");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateGriffinWhenExecutableVersionIsNullOrEmpty() {
        //Given
        GriffinModel griffinModel = new GriffinModel("name", "id", "exName", null);

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");

        //Given
        griffinModel.setExecutableVersion("");

        // Then
        assertThatThrownBy(() -> griffinService.importGriffin(Collections.singletonList(griffinModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid griffin");
    }

    @Test
    @RunWithCustomExecutor
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
        Set<String> listToDelete = new HashSet<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allGriffinInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        griffinService.classifyDataInInsertUpdateOrDeleteLists(
            listToImport,
            listToInsert,
            listToUpdate,
            listToDelete,
            allGriffinInDatabase
        );

        //Then
        assertThat(listToDelete.size()).isEqualTo(1);
        assertThat(listToInsert.size()).isEqualTo(1);
        assertThat(listToUpdate.size()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenRemovingUsedGriffinShouldFailedImport() throws Exception {
        List<GriffinModel> allGriffinInDatabase = getGriffinsModels("griffins_referentiel.json");

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        String requestId = getVitamSession().getRequestId();
        File griffinFile = PropertiesUtils.getResourceFile("griffin_logbook_operation.json");
        JsonNode griffinOperation = JsonHandler.getFromFile(griffinFile);

        File preservationScenarioFile = getResourceFile("preservation_scenario_logbook_operation.json");
        JsonNode preservationScenarioOperation = JsonHandler.getFromFile(preservationScenarioFile);

        List<FileFormatModel> listFormat = getFileFormatModels("fileformatModel.json");

        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allGriffinInDatabase);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(griffinOperation);
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(preservationScenarioOperation);
        givenPreservationScenarioCollectionReturn(getPreservationScenarioModels("preservation_scenario.json"));
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);
        when(dbRequestResult.getDocuments(FileFormat.class, FileFormatModel.class)).thenReturn(listFormat);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(FORMATS))).thenReturn(dbRequestResult);

        List<GriffinModel> listGriffinsToImport = getGriffinsModels("griffins/KO_griffin_maj_remove_used_griffin.json");
        assertThatThrownBy(() -> griffinService.importGriffin(listGriffinsToImport))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Can not remove used griffin(s), GRI-000001.");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportGriffin() throws Exception {
        //Given
        List<GriffinModel> listToImport = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile("griffins.json"),
            valueTypeRef
        );

        List<GriffinModel> allGriffinInDatabase = new ArrayList<>();

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(allGriffinInDatabase);

        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        String requestId = getVitamSession().getRequestId();
        File griffinFile = PropertiesUtils.getResourceFile("griffin_logbook_operation.json");
        JsonNode griffinOperation = JsonHandler.getFromFile(griffinFile);
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(griffinOperation);
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class)).thenReturn(
            new ArrayList<>()
        );
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        RequestResponse<GriffinModel> requestResponse = griffinService.importGriffin(listToImport);
        ArgumentCaptor<LogbookOperationParameters> event1Captor = forClass(LogbookOperationParameters.class);
        ArgumentCaptor<LogbookOperationParameters> event2Captor = forClass(LogbookOperationParameters.class);

        //Then
        JsonNode result = JsonHandler.toJsonNode(requestResponse);
        int total = result.get("$hits").get("total").asInt();

        assertThat(total).isEqualTo(3);

        verify(logbookOperationsClient, times(1)).create(event1Captor.capture());
        verify(logbookOperationsClient, times(1)).update(event2Captor.capture());

        assertThat(event1Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "STP_IMPORT_GRIFFIN.STARTED"
        );
        assertThat(event2Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "STP_IMPORT_GRIFFIN.OK"
        );

        verify(functionalBackupService).saveCollectionAndSequence(
            getGUID(requestId),
            "STP_BACKUP_GRIFFIN",
            GRIFFIN,
            requestId
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldGetGriffinById() throws Exception {
        //Given
        DbRequestResult dbRequestResult = new DbRequestResult();
        when(mongoDbAccess.findDocuments(any(), eq(GRIFFIN))).thenReturn(dbRequestResult);
        //When
        RequestResponse<GriffinModel> griffin = griffinService.findGriffin(getFromString("{}"));
        //Then
        assertThat(griffin).isNotNull();
    }

    private void givenPreservationScenarioCollectionReturn(List<PreservationScenarioModel> preservationScenarioModels) {
        MongoCollection mongoCollection = mock(MongoCollection.class);
        given(preservationScenarioCollection.getCollection()).willReturn(mongoCollection);
        FindIterable findIterable = mock(FindIterable.class);
        given(mongoCollection.find()).willReturn(findIterable);
        MongoIterable mongoIterable = mock(MongoIterable.class);
        given(findIterable.map(any())).willReturn(mongoIterable);
        given(mongoIterable.spliterator()).willReturn(preservationScenarioModels.spliterator());
    }

    private List<PreservationScenarioModel> getPreservationScenarioModels(String s)
        throws InvalidParseOperationException, FileNotFoundException {
        return getFromFileAsTypeReference(getResourceFile(s), scenarioTypeRef);
    }

    private List<GriffinModel> getGriffinsModels(String s)
        throws InvalidParseOperationException, FileNotFoundException {
        return getFromFileAsTypeReference(getResourceFile(s), griffinTypeRef);
    }

    private List<FileFormatModel> getFileFormatModels(String s)
        throws InvalidParseOperationException, FileNotFoundException {
        return getFromFileAsTypeReference(getResourceFile(s), fileFormatTypeRef);
    }
}
