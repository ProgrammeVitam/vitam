package fr.gouv.vitam.worker.core.plugin.preservation;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeRefence;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.PreservationVersion.LAST;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationPreparationPlugin.OBJECT_GROUPS_TO_PRESERVE_JSONL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PreservationPreparationPluginTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock private AdminManagementClientFactory adminManagementClientFactory;

    @Mock private AdminManagementClient adminManagementClient;

    @Mock private MetaDataClientFactory metaDataClientFactory;

    @Mock private MetaDataClient metaDataClient;

    private PreservationPreparationPlugin preservationPreparationPlugin;

    @Before
    public void setUp() throws Exception {

        preservationPreparationPlugin =
            new PreservationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        List<GriffinModel> list = getFromStringAsTypeRefence(griffinIds,
            new TypeReference<List<GriffinModel>>() {
            });
        RequestResponseOK<GriffinModel> griffinRequestResponseOK = new RequestResponseOK<>();
        griffinRequestResponseOK.addAllResults(list);

        PreservationScenarioModel fromString = getFromString(scenarioText, PreservationScenarioModel.class);
        RequestResponseOK<PreservationScenarioModel> objectRequestResponseOK = new RequestResponseOK<>();

        objectRequestResponseOK.addResult(fromString);

        when(adminManagementClient.findPreservationByID("id")).thenReturn(objectRequestResponseOK);
        when(adminManagementClient.findGriffin(any())).thenReturn(griffinRequestResponseOK);
        when(metaDataClient.selectUnits(any()))
            .thenReturn(getFromInputStream(getClass().getResourceAsStream("/preservation/resultRequest.json")));

        when(metaDataClient.selectObjectGroups(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream("/preservation/objectGroupResult.json")));
    }

    @Test
    public void shouldCreateJsonLFile() throws Exception {

        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        PreservationRequest preservationRequest =
            new PreservationRequest(new Select().getFinalSelect(), "id", "BinaryMaster", LAST, "BinaryMaster");

        when(handler.getJsonFromWorkspace("preservationRequest")).thenReturn(toJsonNode(preservationRequest));

        Map<String, File> files = new HashMap<>();
        doAnswer((args) -> {
            File file = temporaryFolder.newFile();
            files.put(args.getArgument(0), file);
            return file;
        }).when(handler).getNewLocalFile(anyString());

        // When
        ItemStatus itemStatus = preservationPreparationPlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);

        List<String> lines = IOUtils.readLines(new FileInputStream(files.get(OBJECT_GROUPS_TO_PRESERVE_JSONL)), "UTF-8");
        assertThat(lines.size()).isEqualTo(5);
    }

    @Test
    public void should_write_query_dsl_in_logbook() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);

        ObjectNode finalSelect = new Select().getFinalSelect();
        PreservationRequest preservationRequest =
            new PreservationRequest(finalSelect, "id", "BinaryMaster", LAST, "BinaryMaster");

        when(handler.getJsonFromWorkspace("preservationRequest")).thenReturn(toJsonNode(preservationRequest));

        Map<String, File> files = new HashMap<>();
        doAnswer((args) -> {
            File file = temporaryFolder.newFile();
            files.put(args.getArgument(0), file);
            return file;
        }).when(handler).getNewLocalFile(anyString());

        // When
        ItemStatus itemStatus = preservationPreparationPlugin.execute(workerParameters, handler);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getEvDetailData())
            .isEqualTo(JsonHandler.unprettyPrint(createObjectNode().put("query", JsonHandler.unprettyPrint(finalSelect))));
        List<String> lines = IOUtils.readLines(new FileInputStream(files.get(OBJECT_GROUPS_TO_PRESERVE_JSONL)), "UTF-8");
        assertThat(lines.size()).isEqualTo(5);
    }

    @Test
    public void should_make_explicit_precondition_failed_when_griffin_id_is_not_find() throws Exception {
        // Given
        when(adminManagementClient.findGriffin(any())).thenThrow(new AdminManagementClientServerException("Internal Server Error"));
        HandlerIO handler = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        PreservationRequest preservationRequest =
            new PreservationRequest(new Select().getFinalSelect(), "id", "BinaryMaster", LAST, "BinaryMaster");
        when(handler.getJsonFromWorkspace("preservationRequest")).thenReturn(toJsonNode(preservationRequest));
        // When
        // Then
        ItemStatus itemStatus = preservationPreparationPlugin.execute(workerParameters, handler);
        String expectedEventDetailData = "{\"error\":\"Preconditions Failed :  Internal Server Error\"}";
        assertThat(itemStatus.getData("eventDetailData").toString()).isEqualTo(expectedEventDetailData);


    }

    private static final String scenarioText =
        "{\"#id\":\"aeaaaaaaaabba3ylabltwalhpdqlsqiaaaaq\",\"#tenant\":0,\"#version\":0,\"Name\":\"Tranformation en pdf\",\"Identifier\":\"id\",\"Description\":\"Ce sc\\u00E9nario permet de transformer un grand nombre de formats (bureautique et image) en PDF.\",\"CreationDate\":\"2018-11-16T15:55:30.721\",\"LastUpdate\":\"2018-12-04T11:00:52.661\",\"ActionList\":[\"GENERATE\"],\"GriffinByFormat\":[{\"FormatList\":[\"fmt/45\",\"x-fmt/400\",\"fmt/127\",\"fmt/128\",\"fmt/129\",\"x-fmt/203\",\"x-fmt/401\",\"fmt/126\",\"fmt/808\",\"fmt/809\",\"x-fmt/360\",\"fmt/969\",\"x-fmt/17\",\"x-fmt/18\",\"fmt/163\",\"x-fmt/94\",\"fmt/280\",\"fmt/52\",\"fmt/281\",\"x-fmt/359\",\"fmt/50\",\"x-fmt/10\",\"fmt/53\",\"fmt/233\",\"x-fmt/9\",\"fmt/355\",\"fmt/59\",\"fmt/598\",\"fmt/631\",\"x-fmt/8\",\"fmt/61\",\"fmt/949\",\"fmt/138\",\"fmt/215\",\"fmt/139\",\"fmt/810\",\"fmt/811\",\"fmt/812\",\"fmt/136\",\"fmt/412\",\"x-fmt/111\",\"fmt/137\",\"fmt/214\",\"x-fmt/271\",\"x-fmt/394\",\"fmt/39\",\"x-fmt/393\",\"x-fmt/272\",\"fmt/813\",\"fmt/814\",\"fmt/815\",\"fmt/290\",\"fmt/295\",\"fmt/130\",\"fmt/296\",\"fmt/297\",\"x-fmt/84\",\"fmt/291\",\"fmt/40\",\"x-fmt/41\",\"fmt/292\",\"x-fmt/44\",\"fmt/293\",\"fmt/294\",\"x-fmt/87\"],\"GriffinIdentifier\":\"GRI-000003\",\"Timeout\":0,\"MaxSize\":10000000,\"Debug\":true,\"ActionDetail\":[{\"ValuesPreservation\":{\"Extension\":\"pdf\",\"Args\":[\"-f\",\"pdf\",\"-e\",\"SelectedPdfVersion=1\"]},\"Type\":\"GENERATE\",\"Values\":{\"Extension\":\"pdf\",\"Args\":[\"-f\",\"pdf\",\"-e\",\"SelectedPdfVersion=1\"]}},{\"ValuesPreservation\":{\"Args\":[\"-strict\"]},\"Type\":\"ANALYSE\",\"Values\":{\"Args\":[\"-strict\"]}}]},{\"FormatList\":[\"fmt/567\",\"fmt/645\",\"fmt/44\",\"fmt/568\",\"fmt/43\",\"fmt/42\",\"fmt/387\",\"fmt/388\",\"fmt/367\",\"fmt/566\",\"fmt/408\",\"fmt/12\",\"fmt/11\",\"fmt/112\",\"fmt/156\",\"fmt/399\",\"x-fmt/398\",\"x-fmt/178\",\"fmt/13\",\"x-fmt/392\",\"x-fmt/391\",\"fmt/935\",\"x-fmt/390\",\"fmt/152\",\"fmt/153\",\"fmt/154\",\"fmt/155\",\"fmt/353\",\"fmt/41\"],\"GriffinIdentifier\":\"GRI-000001\",\"Timeout\":0,\"MaxSize\":10000000,\"Debug\":true,\"ActionDetail\":[{}]}]}";

    private static final String griffinIds = "[\n" +
        "  {\n" +
        "    \"Identifier\": \"GRI-000003\",\n" +
        "    \"Name\": \"Griffon ImageMagick\",\n" +
        "    \"Description\": \"Ce griffon peut faire la validation, la conversion et l'extraction de metadonnées des formats images par ImageMagick.\\nL'action ANALYSE n'utilise pas d'argument.\\nL'action GENERATE utilise en argument tous les arguments d'ImageMagick.\\nLes actions EXTRACT_MD_AU et EXTRACT_MD_GOT remonte les métadonnées sous forme arborescente (par exemple \\\"/image/properties/exif:ResolutionUnit\\\")\",\n" +
        "    \"CreationDate\": \"2018-11-16T15:55:30.721\",\n" +
        "    \"LastUpdate\": \"2018-11-20T15:34:21.542\",\n" +
        "    \"ExecutableName\": \"griffin-imagemagick\",\n" +
        "    \"ExecutableVersion\": \"V1.0.0\"\n" +
        "  },\n" +
        "  {\n" +
        "    \"Identifier\": \"GRI-000002\",\n" +
        "    \"Name\": \"Griffon Jhove\",\n" +
        "    \"Description\": \"Ce griffon peut analyser des formats par Jhove.\\nL'action ANALYSE n'utilise pas d'arguments.\",\n" +
        "    \"CreationDate\": \"2018-11-16T15:55:30.721\",\n" +
        "    \"LastUpdate\": \"2018-11-20T15:34:21.542\",\n" +
        "    \"ExecutableName\": \"griffin-jhove\",\n" +
        "    \"ExecutableVersion\": \"V1.0.0\"\n" +
        "  }]";

}
