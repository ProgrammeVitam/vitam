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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;
import fr.gouv.vitam.common.model.administration.preservation.DefaultGriffin;
import fr.gouv.vitam.common.model.administration.preservation.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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
import java.util.List;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.FORMATS;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationScenarioServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MongoDbAccessReferential mongoDbAccess;

    @Mock
    private FunctionalBackupService functionalBackupService;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    private PreservationScenarioService preservationScenarioService;
    private PreservationScenarioModel defaultScenarioModel;

    @Mock
    private DbRequestResult dbRequestResult;

    @Before
    public void setUp() {
        preservationScenarioService = new PreservationScenarioService(
            mongoDbAccess,
            functionalBackupService,
            logbookOperationsClientFactory
        );

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        DefaultGriffin defaultGriffin = new DefaultGriffin("id", ImmutableList.of(new ActionPreservation()));

        defaultScenarioModel = new PreservationScenarioModel(
            "name",
            "id",
            singletonList(GENERATE),
            singletonList(new GriffinByFormat()),
            defaultGriffin
        );

        defaultScenarioModel.setVersion(1);
        GUID guid = newGUID();

        getVitamSession().setTenantId(1);
        getVitamSession().setRequestId(guid);
    }

    @Test
    @RunWithCustomExecutor
    public void givenPreservationScenariosInDataBaseShouldCollectInsertUpdateAndDeleteList() throws Exception {
        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = getPreservationScenarioModels(
            "scenarii.json"
        );

        List<PreservationScenarioModel> listToImport = getPreservationScenarioModels("scenarii_import.json");

        String requestId = getVitamSession().getRequestId();
        File preservationScenarioFile = getResourceFile("preservation_scenario_logbook_operation.json");
        JsonNode preservationScenarioOperation = JsonHandler.getFromFile(preservationScenarioFile);

        List<GriffinModel> listGriffons = singletonList(new GriffinModel().setIdentifier("GRI-000001"));
        List<FileFormatModel> listFormat = getFileFormatModels("fileformatModel.json");

        //When
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(preservationScenarioOperation);

        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class)).thenReturn(
            allPreservationScenarioInDatabase
        );
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        when(dbRequestResult.getDocuments(FileFormat.class, FileFormatModel.class)).thenReturn(listFormat);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(FORMATS))).thenReturn(dbRequestResult);

        preservationScenarioService.importScenarios(listToImport);

        verify(mongoDbAccess, times(1)).insertDocuments(any(), eq(PRESERVATION_SCENARIO));

        verify(mongoDbAccess, times(1)).replaceDocument(
            any(),
            eq("IDENTIFIER2"),
            eq("Identifier"),
            eq(PRESERVATION_SCENARIO)
        );

        verify(mongoDbAccess, times(1)).deleteDocument(any(), eq(PRESERVATION_SCENARIO));
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportScenario() throws Exception {
        GUID guid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(1);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = getPreservationScenarioModels(
            "scenarii.json"
        );

        List<PreservationScenarioModel> listToImport = getPreservationScenarioModels("scenarii_all.json");

        String requestId = getVitamSession().getRequestId();
        File preservationScenarioFile = getResourceFile("preservation_scenario_logbook_operation.json");
        JsonNode preservationScenarioOperation = JsonHandler.getFromFile(preservationScenarioFile);

        List<FileFormatModel> listFormat = getFileFormatModels("fileformatModel.json");
        List<GriffinModel> listGriffons = singletonList(new GriffinModel().setIdentifier("GRI-000001"));

        //When

        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(preservationScenarioOperation);
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        //When
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class)).thenReturn(
            allPreservationScenarioInDatabase
        );
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);
        when(dbRequestResult.getDocuments(FileFormat.class, FileFormatModel.class)).thenReturn(listFormat);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(FORMATS))).thenReturn(dbRequestResult);

        RequestResponse<PreservationScenarioModel> requestResponse = preservationScenarioService.importScenarios(
            listToImport
        );

        ArgumentCaptor<LogbookOperationParameters> event1Captor = forClass(LogbookOperationParameters.class);
        ArgumentCaptor<LogbookOperationParameters> event2Captor = forClass(LogbookOperationParameters.class);

        //Then
        JsonNode result = JsonHandler.toJsonNode(requestResponse);
        int total = result.get("$hits").get("total").asInt();

        assertThat(total).isEqualTo(3);

        verify(logbookOperationsClient, times(1)).create(event1Captor.capture());
        verify(logbookOperationsClient, times(1)).update(event2Captor.capture());

        verify(mongoDbAccess, times(1)).insertDocuments(any(), eq(PRESERVATION_SCENARIO));
        verify(mongoDbAccess, times(1)).replaceDocument(
            any(),
            eq("IDENTIFIER2"),
            eq("Identifier"),
            eq(PRESERVATION_SCENARIO)
        );

        assertThat(event1Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "STP_IMPORT_PRESERVATION_SCENARIO.STARTED"
        );
        assertThat(event2Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "STP_IMPORT_PRESERVATION_SCENARIO.OK"
        );

        verify(functionalBackupService).saveCollectionAndSequence(
            guid,
            "STP_BACKUP_SCENARIO",
            PRESERVATION_SCENARIO,
            guid.getId()
        );

        // test false type creation date
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_false_type_creationDate.json"
        );
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("field 'CreationDate' format is invalid");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldGetScenarioById() throws Exception {
        //Given
        when(mongoDbAccess.findDocuments(any(), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);
        when(dbRequestResult.getRequestResponseOK(any(), any(), any())).thenReturn(new RequestResponseOK<>());

        //When
        RequestResponse<PreservationScenarioModel> preservationScenario =
            preservationScenarioService.findPreservationScenario(getFromString("{}"));
        //Then
        assertThat(preservationScenario).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenNameIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setName(null);

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setName("");

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedWhenImportTwoDuplicatedScenarioIdentifiers() throws Exception {
        //Given
        GriffinByFormat griffinByFormat = new GriffinByFormat(
            of("fmt"),
            "id",
            ImmutableList.of(new ActionPreservation())
        );
        griffinByFormat.setDebug(false);
        griffinByFormat.setActionDetail(singletonList(new ActionPreservation(GENERATE, null)));
        griffinByFormat.setFormatList(Sets.newHashSet("ts"));
        griffinByFormat.setMaxSize(2L);
        griffinByFormat.setTimeOut(2000);

        DefaultGriffin defaultGriffin = new DefaultGriffin(
            "id",
            ImmutableList.of(new ActionPreservation(GENERATE, null))
        );

        defaultGriffin.setDebug(false);
        defaultGriffin.setActionDetail(singletonList(new ActionPreservation(GENERATE, null)));
        defaultGriffin.setMaxSize(2);
        defaultGriffin.setTimeOut(2000);

        PreservationScenarioModel secondScenarioModel = new PreservationScenarioModel(
            "name",
            "id",
            singletonList(GENERATE),
            singletonList(griffinByFormat),
            defaultGriffin
        );

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(newArrayList(secondScenarioModel, secondScenarioModel))
        )
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Duplicate scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenIdentifierIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setIdentifier(null);

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setIdentifier("");

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_actionList() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_actionList.json"
        );
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_actionDetail() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_actionDetail.json.json"
        );
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_debug() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_debug.json.json"
        );

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_griffinIdentifier() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_griffinIdentifier.json.json"
        );
        //Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_maxSize() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_maxSize.json.json"
        );
        //Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_timeout() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_timeout.json.json"
        );
        //Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_defaultGriffin_type() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_defaultGriffin_type.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_actionDetail() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_actionDetail.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_debug() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_debug.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_formatList() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_formatList.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_griffinIdentifier() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_griffinIdentifier.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_maxSize() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_maxSize.json.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_timeout() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_timeout.json.json"
        );
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_griffinByFormat_type() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_griffinByFormat_type.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_identifier() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_identifier.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_field_name() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_field_name.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_actionList() throws Exception {
        //Given / When / Then

        assertThatThrownBy(
            () -> getPreservationScenarioModels("preservationScenarios/KO_scenario_absent_value_actionList.json")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_actionDetail() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_defaultFormat_actionDetail.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_debug() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_defaultFormat_debug.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_griffinIdentifier() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_defaultFormat_griffinIdentifier.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_maxSize() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_defaultFormat_maxSize.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_timeout() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_defaultFormat_timeout.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_defaultFormat_type() throws Exception {
        // Given / When / Then

        assertThatThrownBy(
            () ->
                getPreservationScenarioModels("preservationScenarios/KO_scenario_absent_value_defaultFormat_type.json")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_formatList() throws Exception {
        // Given
        List<GriffinModel> listGriffons = singletonList(new GriffinModel().setIdentifier("GRI-000001"));

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_formatList.json"
        );

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_actionDetail() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_griffinByFormat_actionDetail.json"
        );
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_debug() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_griffinByFormat_debug.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_griffinIdentifier() throws Exception {
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_griffinByFormat_griffinIdentifier.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_maxSize() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_griffinByFormat_maxSize.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_timeout() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_griffinByFormat_timeout.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_griffinByFormat_type() throws Exception {
        //given /When / Then

        assertThatThrownBy(
            () ->
                getPreservationScenarioModels(
                    "preservationScenarios/KO_scenario_absent_value_griffinByFormat_type.json"
                )
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_identifier() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_identifier.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_absent_value_name() throws Exception {
        //Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_absent_value_name.json"
        );

        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_false_type_debug() throws Exception {
        //Given/ When / Then

        assertThatThrownBy(
            () -> getPreservationScenarioModels("preservationScenarios/KO_scenario_false_type_debug.json")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_false_type_maxSize() throws Exception {
        //Given
        String resource = "preservationScenarios/KO_scenario_false_type_maxSize.json";

        //When / Then
        assertThatThrownBy(() -> getPreservationScenarioModels(resource)).isInstanceOf(
            InvalidParseOperationException.class
        );
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_false_type_timeout() throws Exception {
        //Given         //When / Then
        assertThatThrownBy(
            () -> getPreservationScenarioModels("preservationScenarios/KO_scenario_false_type_timeout.json")
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    //        preservationScenarios/KO_scenario_format.pdf
    @Test
    @RunWithCustomExecutor
    public void KO_scenario_same_identifier() throws Exception {
        //Given

        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_same_identifier.json"
        );
        //When / Then

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Duplicate scenario : 'PSC-000001'");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_unknown_value_defaultGriffin_griffinIdentifier() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_unknown_value_defaultGriffin_griffinIdentifier.json"
        );

        List<GriffinModel> listGriffons = singletonList(new GriffinModel().setIdentifier("GRI-000001"));

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Griffin 'TOTO' is not in database");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_unknown_value_defaultGriffin_type() throws Exception {
        //When / Then

        assertThatThrownBy(
            () ->
                getPreservationScenarioModels(
                    "preservationScenarios/KO_scenario_unknown_value_defaultGriffin_type.json"
                )
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_unknown_value_formatList() throws Exception {
        // Given
        List<GriffinModel> listGriffons = singletonList(new GriffinModel().setIdentifier("GRI-000001"));

        List<FileFormatModel> listFormat = new ArrayList<>();

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        when(dbRequestResult.getDocuments(FileFormat.class, FileFormatModel.class)).thenReturn(listFormat);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(FORMATS))).thenReturn(dbRequestResult);

        //Then
        assertThatThrownBy(
            () ->
                preservationScenarioService.importScenarios(
                    getPreservationScenarioModels("preservationScenarios/KO_scenario_unknown_value_formatList.json")
                )
        )
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("List: [TOTO] does not exist in the database.");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_unknown_value_GriffinByFormat_griffinIdentifier() throws Exception {
        // Given
        List<PreservationScenarioModel> scenarios = getPreservationScenarioModels(
            "preservationScenarios/KO_scenario_unknown_value_GriffinByFormat_griffinIdentifier.json"
        );

        List<GriffinModel> listGriffons = new ArrayList<>();

        //When
        when(dbRequestResult.getDocuments(Griffin.class, GriffinModel.class)).thenReturn(listGriffons);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(GRIFFIN))).thenReturn(dbRequestResult);

        assertThatThrownBy(() -> preservationScenarioService.importScenarios(scenarios))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Griffin 'TOTO' is not in database");
    }

    @Test
    @RunWithCustomExecutor
    public void KO_scenario_unknown_value_GriffinByFormat_type() throws Exception {
        assertThatThrownBy(
            () ->
                getPreservationScenarioModels(
                    "preservationScenarios/KO_scenario_unknown_value_GriffinByFormat_type.json"
                )
        ).isInstanceOf(InvalidParseOperationException.class);
    }

    private List<PreservationScenarioModel> getPreservationScenarioModels(String s)
        throws InvalidParseOperationException, FileNotFoundException {
        return getFromFileAsTypeReference(getResourceFile(s), new TypeReference<List<PreservationScenarioModel>>() {});
    }

    private List<FileFormatModel> getFileFormatModels(String s)
        throws InvalidParseOperationException, FileNotFoundException {
        return getFromFileAsTypeReference(getResourceFile(s), new TypeReference<List<FileFormatModel>>() {});
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenActionListIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setActionList(null);

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setActionList(new ArrayList<ActionTypePreservation>());

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenGriffinByFormatIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setGriffinByFormat(null);

        //When
        when(mongoDbAccess.findDocuments(any(), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class)).thenReturn(
            new ArrayList<>()
        );

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setGriffinByFormat(new ArrayList<>());

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateDefaultGriffinHasNoActionDetail() throws Exception {
        //Given
        DefaultGriffin defaultGriffin = new DefaultGriffin("id", ImmutableList.of(new ActionPreservation()));
        defaultScenarioModel.setDefaultGriffin(defaultGriffin);

        // Then
        assertThatThrownBy(() -> preservationScenarioService.importScenarios(singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class)
            .hasMessageContaining("Invalid scenario");
    }
}
