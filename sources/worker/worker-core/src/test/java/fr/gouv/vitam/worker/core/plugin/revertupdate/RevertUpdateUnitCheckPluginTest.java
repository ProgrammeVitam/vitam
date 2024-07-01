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

package fr.gouv.vitam.worker.core.plugin.revertupdate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.revertupdate.RevertUpdateOptions;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.createArrayNode;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.revertupdate.RevertUpdateUnitCheckPlugin.REVERT_UPDATE_UNITS_JSONL_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RevertUpdateUnitCheckPluginTest {

    private static final String OPERATION_ID = "OP_UPDATE";
    private static final String UNIT_ID = "UNIT_ID";
    private static final String ANY_PATH = "anyPath";

    String DIFF_V0 =
        "{\n" +
        "  \"diff\" : \"" +
        "-  \\\"Title\\\" : \\\"Old_Title\\\"\\n" +
        "+  \\\"Title\\\" : \\\"New_Title\\\"\\n" +
        "-  \\\"Description\\\" : \\\"Old_Description\\\"\\n" +
        "+  \\\"DescriptionLevel\\\" : \\\"Item\\\"\\n" +
        "-  \\\"_ops\\\" : [ \\\"OP_INGEST\\\" ]\\n" +
        "+  \\\"_ops\\\" : [ \\\"OP_INGEST\\\", \\\"OP_UPDATE\\\" ]\\n" +
        "-  \\\"_v\\\" : 0\\n" +
        "-  \\\"_av\\\" : 0\\n" +
        "+  \\\"_v\\\" : 1\\n" +
        "+  \\\"_av\\\" : 1\"" +
        "\n}";

    String DIFF_V1 =
        "{\n" +
        "  \"diff\" : \"" +
        "-  \\\"Title_.fr\\\" : \\\"Old_Title\\\"\\n" +
        "+  \\\"Title_.fr\\\" : \\\"New_Title\\\"\\n" +
        "-  \\\"Description_.fr\\\" : \\\"Old_Description\\\"\\n" +
        "-  \\\"_ops\\\" : [ \\\"OP_INGEST\\\" ]\\n" +
        "+  \\\"_ops\\\" : [ \\\"OP_INGEST\\\", \\\"OP_UPDATE\\\" ]\\n" +
        "-  \\\"_v\\\" : 0\\n" +
        "-  \\\"_av\\\" : 0\\n" +
        "+  \\\"_v\\\" : 1\\n" +
        "+  \\\"_av\\\" : 1\",\n" +
        "  \"version\" : 1" +
        "\n}";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @InjectMocks
    private RevertUpdateUnitCheckPlugin revertUpdateUnitCheckPlugin;

    private MetaDataClient metadataClient;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Before
    public void setUp() throws Exception {
        LogbookLifeCyclesClientFactory.changeMode(null);

        metadataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metadataClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        given(logbookLifeCyclesClientFactory.getClient()).willReturn(logbookLifeCyclesClient);
    }

    @Test
    public void should_distribute_when_diff_version0() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.emptyList()
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(VitamFieldsHelper.operations(), createArrayNode().add(OPERATION_ID))
                        )
                )
        );

        LogbookOperation logbookOperation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvIdProc(OPERATION_ID);
        logbookEventOperation.setObId(UNIT_ID);
        logbookEventOperation.setEvDetData(DIFF_V0);
        logbookOperation.setEvents(List.of(logbookEventOperation));
        List<JsonNode> jsonNodes = List.of(JsonHandler.toJsonNode(logbookOperation));
        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(eq(List.of(UNIT_ID)))).thenReturn(jsonNodes);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator)
            .extracting(JsonLineModel::getParams)
            .extracting(JsonNode::toString)
            .isEqualTo(
                List.of(
                    "{\"queryIndex\":0,\"originQuery\":{\"$roots\":[],\"$query\":[{\"$eq\":{\"#id\":\"UNIT_ID\"}}],\"$filter\":{},\"$action\":[{\"$set\":{\"Description\":\"Old_Description\",\"Title\":\"Old_Title\"}},{\"$unset\":[\"DescriptionLevel\"]}]}}"
                )
            );
    }

    @Test
    public void should_distribute_when_diff_version1() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.emptyList()
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(VitamFieldsHelper.operations(), createArrayNode().add(OPERATION_ID))
                        )
                )
        );

        LogbookOperation logbookOperation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvIdProc(OPERATION_ID);
        logbookEventOperation.setObId(UNIT_ID);
        logbookEventOperation.setEvDetData(DIFF_V1);
        logbookOperation.setEvents(List.of(logbookEventOperation));
        List<JsonNode> jsonNodes = List.of(JsonHandler.toJsonNode(logbookOperation));
        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(eq(List.of(UNIT_ID)))).thenReturn(jsonNodes);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator)
            .extracting(JsonLineModel::getParams)
            .extracting(JsonNode::toString)
            .isEqualTo(
                List.of(
                    "{\"queryIndex\":0,\"originQuery\":{\"$roots\":[],\"$query\":[{\"$eq\":{\"#id\":\"UNIT_ID\"}}],\"$filter\":{},\"$action\":[{\"$set\":{\"Title_.fr\":\"Old_Title\",\"Description_.fr\":\"Old_Description\"}}]}}"
                )
            );
    }

    @Test
    public void should_not_distribute_when_no_field_match() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.singletonList("Title")
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(VitamFieldsHelper.operations(), createArrayNode().add(OPERATION_ID))
                        )
                )
        );

        LogbookOperation logbookOperation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvIdProc(OPERATION_ID);
        logbookEventOperation.setObId(UNIT_ID);
        logbookEventOperation.setEvDetData(DIFF_V1);
        logbookOperation.setEvents(List.of(logbookEventOperation));
        List<JsonNode> jsonNodes = List.of(JsonHandler.toJsonNode(logbookOperation));
        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(eq(List.of(UNIT_ID)))).thenReturn(jsonNodes);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(KO, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator).extracting(JsonLineModel::getId).isEqualTo(Collections.emptyList());
    }

    @Test
    public void should_not_distribute_when_operation_is_not_last_and_force_is_false() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.emptyList()
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(
                                    VitamFieldsHelper.operations(),
                                    createArrayNode().add(OPERATION_ID).add("OPERATION_2")
                                )
                        )
                )
        );

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(KO, itemStatus.getGlobalStatus());
    }

    @Test
    public void should_distribute_when_operation_is_not_last_and_force_is_true() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            true,
            createObjectNode(),
            OPERATION_ID,
            Collections.singletonList("Title_.fr")
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(
                                    VitamFieldsHelper.operations(),
                                    createArrayNode().add(OPERATION_ID).add("OPERATION_2")
                                )
                        )
                )
        );

        LogbookOperation logbookOperation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvIdProc(OPERATION_ID);
        logbookEventOperation.setObId(UNIT_ID);
        logbookEventOperation.setEvDetData(DIFF_V1);
        logbookOperation.setEvents(List.of(logbookEventOperation));
        List<JsonNode> jsonNodes = List.of(JsonHandler.toJsonNode(logbookOperation));
        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(eq(List.of(UNIT_ID)))).thenReturn(jsonNodes);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator)
            .extracting(JsonLineModel::getParams)
            .extracting(JsonNode::toString)
            .isEqualTo(
                List.of(
                    "{\"queryIndex\":0,\"originQuery\":{\"$roots\":[],\"$query\":[{\"$eq\":{\"#id\":\"UNIT_ID\"}}],\"$filter\":{},\"$action\":[{\"$set\":{\"Title_.fr\":\"Old_Title\"}}]}}"
                )
            );
    }

    @Test
    public void should_not_distribute_when_no_unit_found() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.singletonList("Title")
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode().set(TAG_RESULTS, createArrayNode())
        );

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(KO, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator).extracting(JsonLineModel::getId).isEqualTo(Collections.emptyList());
    }

    @Test
    public void should_not_include_unit_with_no_diff() throws Exception {
        WorkerParameters params = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        RevertUpdateOptions options = new RevertUpdateOptions(
            false,
            createObjectNode(),
            OPERATION_ID,
            Collections.emptyList()
        );
        File optionsFile = tempFolder.newFile();
        JsonHandler.writeAsFile(options, optionsFile);
        when(handlerIO.getInput(0, File.class)).thenReturn(optionsFile);

        when(metadataClient.selectUnits(any(JsonNode.class))).thenReturn(
            createObjectNode()
                .set(
                    TAG_RESULTS,
                    createArrayNode()
                        .add(
                            createObjectNode()
                                .put(VitamFieldsHelper.id(), UNIT_ID)
                                .set(VitamFieldsHelper.operations(), createArrayNode().add(OPERATION_ID))
                        )
                )
        );

        LogbookOperation logbookOperation = new LogbookOperation();
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvIdProc(OPERATION_ID);
        logbookEventOperation.setObId(UNIT_ID);
        logbookEventOperation.setEvDetData(DIFF_V1);
        logbookOperation.setEvents(List.of(logbookEventOperation));
        List<JsonNode> jsonNodes = List.of(JsonHandler.toJsonNode(logbookOperation));
        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(eq(List.of(UNIT_ID)))).thenReturn(jsonNodes);

        File jsonlFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq(REVERT_UPDATE_UNITS_JSONL_FILE))).thenReturn(jsonlFile);

        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath(ANY_PATH));
        when(handlerIO.getNewLocalFile(eq(ANY_PATH))).thenReturn(tempFolder.newFile());

        ItemStatus itemStatus = revertUpdateUnitCheckPlugin.execute(params, handlerIO);

        assertEquals(OK, itemStatus.getGlobalStatus());

        JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
            new FileInputStream(jsonlFile),
            new TypeReference<>() {}
        );

        assertThat(jsonLineIterator)
            .extracting(JsonLineModel::getParams)
            .extracting(JsonNode::toString)
            .isEqualTo(
                List.of(
                    "{\"queryIndex\":0,\"originQuery\":{\"$roots\":[],\"$query\":[{\"$eq\":{\"#id\":\"UNIT_ID\"}}],\"$filter\":{},\"$action\":[{\"$set\":{\"Title_.fr\":\"Old_Title\",\"Description_.fr\":\"Old_Description\"}}]}}"
                )
            );
    }
}
