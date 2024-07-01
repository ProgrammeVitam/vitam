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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

    private MetaDataClient metaDataClient = mock(MetaDataClient.class);

    @Before
    public void setUp() {
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
    }

    @Test
    public void should_create_distribution_file() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            JsonHandler.createObjectNode(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "BATMAN_ID");
        selectedUnitGOT.put("#object", "ROBIN_ID");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        probativeCreateDistribution.execute(null, handlerIO);

        // Then
        List<JsonLineModel> listJsonModel = JsonHandler.getFromFileAsTypeReference(
            newLocalFile,
            new TypeReference<>() {}
        );
        assertThat(listJsonModel).hasSize(1);
        assertThat(listJsonModel).first().extracting(JsonLineModel::getId).isEqualTo("ROBIN_ID");
        assertThat(listJsonModel)
            .first()
            .extracting(JsonLineModel::getParams)
            .extracting(Object::toString)
            .isEqualTo("{\"unitIds\":[\"BATMAN_ID\"],\"usageVersion\":\"BinaryMaster_1\"}");
    }

    @Test
    public void should_not_include_duplicate_object_group_response() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            JsonHandler.createObjectNode(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "BATMAN_ID");
        selectedUnitGOT.put("#object", "ROBIN_ID");

        ObjectNode selectedUnitGOT2 = JsonHandler.createObjectNode();
        selectedUnitGOT2.put("#id", "JOKER_ID");
        selectedUnitGOT2.put("#object", "ROBIN_ID");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        requestResponseOK.addResult(selectedUnitGOT2);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        probativeCreateDistribution.execute(null, handlerIO);

        // Then
        List<JsonLineModel> listJsonModel = JsonHandler.getFromFileAsTypeReference(
            newLocalFile,
            new TypeReference<>() {}
        );
        assertThat(listJsonModel).hasSize(1);
        assertThat(listJsonModel).first().extracting(JsonLineModel::getId).isEqualTo("ROBIN_ID");
        assertThat(listJsonModel)
            .first()
            .extracting(JsonLineModel::getParams)
            .extracting(Object::toString)
            .isEqualTo("{\"unitIds\":[\"JOKER_ID\",\"BATMAN_ID\"],\"usageVersion\":\"BinaryMaster_1\"}");
    }

    @Test
    public void should_return_item_status_OK() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            JsonHandler.createObjectNode(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "BATMAN_ID");
        selectedUnitGOT.put("#object", "ROBIN_ID");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<JsonLineModel> listJsonModel = JsonHandler.getFromFileAsTypeReference(
            newLocalFile,
            new TypeReference<>() {}
        );
        assertThat(listJsonModel).hasSize(1);
        assertThat(listJsonModel).first().extracting(JsonLineModel::getId).isEqualTo("ROBIN_ID");
        assertThat(listJsonModel)
            .first()
            .extracting(JsonLineModel::getParams)
            .extracting(Object::toString)
            .isEqualTo("{\"unitIds\":[\"BATMAN_ID\"],\"usageVersion\":\"BinaryMaster_1\"}");
    }

    @Test
    public void should_return_item_status_OK_when_no_object() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            JsonHandler.createObjectNode(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        ObjectNode selectedUnitGOT = JsonHandler.createObjectNode();
        selectedUnitGOT.put("#id", "BATMAN_ID");

        RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addResult(selectedUnitGOT);
        given(metaDataClient.selectUnits(any())).willReturn(JsonHandler.toJsonNode(requestResponseOK));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(newLocalFile.length()).isEqualTo(0);
    }

    @Test
    public void should_return_item_status_KO_when_any_error() throws Exception {
        // Given
        File newLocalFile = tempFolder.newFile();
        when(handlerIO.getNewLocalFile(eq("OBJECT_GROUP_TO_CHECK.jsonl"))).thenReturn(newLocalFile);

        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            JsonHandler.createObjectNode(),
            "BinaryMaster",
            "1"
        );
        when(handlerIO.getInputStreamFromWorkspace(eq("request"))).thenReturn(
            new ByteArrayInputStream(JsonHandler.fromPojoToBytes(probativeValueRequest))
        );

        given(metaDataClient.selectUnits(any())).willThrow(new IllegalStateException("Hell yeah"));

        // When
        ItemStatus itemStatus = probativeCreateDistribution.execute(null, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }
}
