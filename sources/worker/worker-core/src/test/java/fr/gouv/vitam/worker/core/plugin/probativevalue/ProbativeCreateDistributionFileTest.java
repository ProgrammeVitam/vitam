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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProbativeCreateDistributionFileTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private HandlerIO handlerIO;

    @InjectMocks
    private ProbativeCreateDistributionFile probativeCreateDistribution;

    @Mock
    private MetaDataClient metaDataClient;

    @Before
    public void setUp() {
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        when(handlerIO.getMetaDataClient()).thenReturn(metaDataClient);
    }

    @Test
    public void should_create_distribution_file() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "Unit1");
        selectedUnitGOT.put("#object", "og1");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        probativeCreateDistribution.execute(null, handlerIO);

        // Then
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(1);
        checkReportLine(listJsonModel, "og1", "Unit1");
    }

    @Test
    public void should_not_include_duplicate_object_group_response() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "Unit1");
        selectedUnitGOT.put("#object", "og");

        ObjectNode selectedUnitGOT2 = JsonHandler.createObjectNode();
        selectedUnitGOT2.put("#id", "Unit2");
        selectedUnitGOT2.put("#object", "og");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        requestResponseOK.addResult(selectedUnitGOT2);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        probativeCreateDistribution.execute(null, handlerIO);

        // Then
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(1);
        checkReportLine(listJsonModel, "og", "Unit1", "Unit2");
    }

    @Test
    public void should_return_item_status_OK() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "Unit1");
        selectedUnitGOT.put("#object", "og1");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(1);
        checkReportLine(listJsonModel, "og1", "Unit1");
    }

    @Test
    public void should_skip_units_when_no_object() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT1 = JsonHandler.createObjectNode().put("#id", "Unit1");

        ObjectNode selectedUnitGOT2 = JsonHandler.createObjectNode().put("#id", "Unit2").putNull("#og");

        ObjectNode selectedUnitGOT3 = JsonHandler.createObjectNode().put("#id", "Unit3").put("#object", "og3");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT1);
        requestResponseOK.addResult(selectedUnitGOT2);
        requestResponseOK.addResult(selectedUnitGOT3);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(1);
        checkReportLine(listJsonModel, "og3", "Unit3");
    }

    @Test
    public void should_extend_selection_to_detached_signing_information_child_units_when_requested() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1",
            true
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT1 = JsonHandler.createObjectNode()
            .put("#id", "Unit1")
            .put("#object", "og1")
            .set(
                "SigningInformation",
                JsonHandler.createObjectNode()
                    .set("DetachedSigningRole", JsonHandler.createStringArrayNode("Signature", "Timestamp"))
            );

        ObjectNode selectedUnitGOT2 = JsonHandler.createObjectNode().put("#id", "Unit2").put("#object", "og2");

        ObjectNode selectedUnitGOT3 = JsonHandler.createObjectNode().put("#id", "Unit3").put("#object", "og3");

        RequestResponseOK<JsonNode> initialRequestResponseOK = new RequestResponseOK<>();
        initialRequestResponseOK.addResult(selectedUnitGOT1);
        initialRequestResponseOK.addResult(selectedUnitGOT2);
        initialRequestResponseOK.addResult(selectedUnitGOT3);

        ObjectNode detachedSignatureForUnit1 = JsonHandler.createObjectNode().put("#id", "Unit4").put("#object", "og4");

        ObjectNode detachedTimestampForUnit1AlreadyExistingInInitialResultSet = JsonHandler.createObjectNode()
            .put("#id", "Unit2")
            .put("#object", "og2");

        RequestResponseOK<JsonNode> detachedSigningInformationRequestResponseOK = new RequestResponseOK<>();
        detachedSigningInformationRequestResponseOK.addResult(detachedSignatureForUnit1);
        detachedSigningInformationRequestResponseOK.addResult(
            detachedTimestampForUnit1AlreadyExistingInInitialResultSet
        );

        given(metaDataClient.selectUnits(any())).willReturn(
            JsonHandler.toJsonNode(initialRequestResponseOK),
            JsonHandler.toJsonNode(detachedSigningInformationRequestResponseOK)
        );

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(4);
        checkReportLine(listJsonModel, "og1", "Unit1");
        checkReportLine(listJsonModel, "og2", "Unit2");
        checkReportLine(listJsonModel, "og3", "Unit3");
        checkReportLine(listJsonModel, "og4", "Unit4");

        verify(metaDataClient, times(2)).selectUnits(any());
    }

    @Test
    public void should_not_extend_selection_to_detached_signing_information_child_units_when_not_requested()
        throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1",
            false
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT1 = JsonHandler.createObjectNode()
            .put("#id", "Unit1")
            .put("#object", "og1")
            .set(
                "SigningInformation",
                JsonHandler.createObjectNode()
                    .set("DetachedSigningRole", JsonHandler.createStringArrayNode("Signature", "Timestamp"))
            );

        ObjectNode selectedUnitGOT2 = JsonHandler.createObjectNode().put("#id", "Unit2").put("#object", "og2");

        ObjectNode selectedUnitGOT3 = JsonHandler.createObjectNode().put("#id", "Unit3").put("#object", "og3");

        RequestResponseOK<JsonNode> initialRequestResponseOK = new RequestResponseOK<>();
        initialRequestResponseOK.addResult(selectedUnitGOT1);
        initialRequestResponseOK.addResult(selectedUnitGOT2);
        initialRequestResponseOK.addResult(selectedUnitGOT3);

        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(initialRequestResponseOK));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<JsonLineModel> listJsonModel = parseReportFile(newLocalFile);
        assertThat(listJsonModel).hasSize(3);
        checkReportLine(listJsonModel, "og1", "Unit1");
        checkReportLine(listJsonModel, "og2", "Unit2");
        checkReportLine(listJsonModel, "og3", "Unit3");

        verify(metaDataClient).selectUnits(any());
    }

    @Test
    public void should_return_item_status_KO_when_any_error() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            new SelectMultiQuery().getFinalSelect(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        given(metaDataClient.selectUnits(any())).willThrow(new IllegalStateException());

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    private static List<JsonLineModel> parseReportFile(File file) throws IOException {
        try (
            FileInputStream inputStream = new FileInputStream(file);
            JsonLineIterator<JsonLineModel> iterator = new JsonLineIterator<>(inputStream, new TypeReference<>() {})
        ) {
            return iterator.stream().collect(Collectors.toList());
        }
    }

    private static void checkReportLine(List<JsonLineModel> reportLines, String objectGroupId, String... unitIds) {
        Optional<JsonLineModel> selectedLine = reportLines
            .stream()
            .filter(reportLine -> reportLine.getId().equals(objectGroupId))
            .findFirst();
        assertThat(selectedLine).withFailMessage("Expected " + objectGroupId + " object group").isPresent();
        JsonAssert.assertJsonEquals(
            JsonHandler.createObjectNode()
                .put("usageVersion", "BinaryMaster_1")
                .set("unitIds", JsonHandler.createStringArrayNode(unitIds)),
            selectedLine.get().getParams(),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }
}
