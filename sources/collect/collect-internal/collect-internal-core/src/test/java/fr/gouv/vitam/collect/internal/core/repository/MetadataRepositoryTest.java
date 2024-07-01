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

package fr.gouv.vitam.collect.internal.core.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.FileInfoDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.internal.core.helpers.handlers.QueryHandler;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbStorageModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetadataRepositoryTest {

    private final String TRANSACTION_ID = "TRANSACTION_ID";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MetadataRepository metadataRepository;

    @Mock
    private MetaDataClientFactory metaDataCollectClientFactory;

    @Mock
    private MetaDataClient metaDataCollectClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        when(metaDataCollectClientFactory.getClient()).thenReturn(metaDataCollectClient);
    }

    @Test
    public void testSelectUnits() throws Exception {
        // Given
        SelectMultiQuery select = new SelectMultiQuery();
        JsonNode query = select.getFinalSelect();

        when(metaDataCollectClient.selectUnits(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>().setQuery(a.getArgument(0)))
        );
        // When
        RequestResponseOK<JsonNode> jsonNode = metadataRepository.selectUnits(query, TRANSACTION_ID);
        // Then
        assertEquals(
            "[{\"$in\":{\"#opi\":[\"" + TRANSACTION_ID + "\"]}}]",
            jsonNode.getQuery().get("$query").toString()
        );
    }

    @Test
    public void testSelectAllUnits() throws Exception {
        // Given
        SelectMultiQuery select = new SelectMultiQuery();
        List<JsonNode> results = List.of(JsonHandler.createObjectNode());
        when(metaDataCollectClient.selectUnits(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>(a.getArgument(0), results, 1))
        );
        // When
        final ScrollSpliterator<JsonNode> scrollSpliterator = metadataRepository.selectUnits(select, TRANSACTION_ID);
        // Then
        assertEquals(results.size(), scrollSpliterator.estimateSize());
    }

    @Test
    public void testSelectUnitById() throws Exception {
        // Given
        final String UNIT_ID = "UNIT_ID";
        when(metaDataCollectClient.selectUnitbyId(any(), eq(UNIT_ID))).thenReturn(
            JsonHandler.toJsonNode(new RequestResponseOK<>().addResult(JsonHandler.createObjectNode()))
        );
        // When
        metadataRepository.selectUnitById(UNIT_ID);
        // Then
        verify(metaDataCollectClient).selectUnitbyId(any(), eq(UNIT_ID));
    }

    @Test
    public void testSelectObjectGroups() throws Exception {
        // Given
        SelectMultiQuery select = new SelectMultiQuery();
        JsonNode query = select.getFinalSelect();

        when(metaDataCollectClient.selectObjectGroups(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>().setQuery(a.getArgument(0)))
        );
        // When
        final JsonNode jsonNode = metadataRepository.selectObjectGroups(query, TRANSACTION_ID);
        // Then
        assertEquals(
            "[{\"$in\":{\"#opi\":[\"" + TRANSACTION_ID + "\"]}}]",
            jsonNode.get("$context").get("$query").toString()
        );
    }

    @Test
    public void testAtomicBulkUpdate() throws Exception {
        // Given
        final List<JsonNode> queries = List.of(JsonHandler.createObjectNode());
        // When
        metadataRepository.atomicBulkUpdate(queries);
        // Then
        verify(metaDataCollectClient).atomicUpdateBulk(eq(queries));
    }

    @Test
    public void selectObjectGroupById() throws Exception {
        // Given
        given(metaDataCollectClient.getObjectGroupByIdRaw("1")).willReturn(
            getResponseWith("version_id", "storage_id", "default", "BinaryMaster_25", "OPI")
        );
        // When
        JsonNode jsonNode = metadataRepository.selectObjectGroupById("1", true);
        // Then
        Assertions.assertThat(jsonNode).isNotNull();
        Assertions.assertThat(jsonNode.get("_qualifiers").get(0).get("versions").get(0).get("_id").asText()).isEqualTo(
            "version_id"
        );
        Assertions.assertThat(
            jsonNode.get("_qualifiers").get(0).get("versions").get(0).get("DataObjectVersion").asText()
        ).isEqualTo("BinaryMaster_25");
    }

    @Test
    public void selectObjectGroupById_without_raw() throws Exception {
        // Given
        when(metaDataCollectClient.selectObjectGrouptbyId(any(), any())).thenReturn(
            JsonHandler.createObjectNode().put("test", true)
        );
        // When
        JsonNode jsonNode = metadataRepository.selectObjectGroupById("1", false);
        // Then
        Assertions.assertThat(jsonNode).isNotNull();
        Assertions.assertThat(jsonNode.get("test").asText()).isEqualTo("true");
    }

    @Test
    public void saveArchiveUnit() throws Exception {
        // Given
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add("aeaqaaaaaagbcaacaa3woak5by7by4aaaaba");
        ObjectNode objectNode = JsonHandler.createObjectNode()
            .put("#id", "1")
            .put("Identifier", "value" + 1)
            .put("Name", "Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        objectNode.set("#unitups", arrayNode);
        when(metaDataCollectClient.insertUnitBulk(any())).thenReturn(JsonHandler.getFromString("{\"test\":\"true\"}"));
        // When
        JsonNode jsonNode = metadataRepository.saveArchiveUnit(objectNode);
        // Then
        Assertions.assertThat(jsonNode).isNotNull();
        Assertions.assertThat(jsonNode.get("test").asText()).isEqualTo("true");
    }

    @Test
    public void saveArchiveUnit_without_unit_up() throws Exception {
        // Given
        ObjectNode objectNode = JsonHandler.createObjectNode()
            .put("#id", "1")
            .put("Identifier", "value" + 1)
            .put("Name", "Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        when(metaDataCollectClient.insertUnitBulk(any())).thenReturn(JsonHandler.getFromString("{\"test\":\"true\"}"));
        // When
        JsonNode jsonNode = metadataRepository.saveArchiveUnit(objectNode);
        // Then
        Assertions.assertThat(jsonNode).isNotNull();
        Assertions.assertThat(jsonNode.get("test").asText()).isEqualTo("true");
    }

    @Test
    public void saveObjectGroup() throws Exception {
        // Given
        ObjectNode objectNode = JsonHandler.createObjectNode()
            .put("#id", "1")
            .put("Identifier", "value" + 1)
            .put("Name", "Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        when(metaDataCollectClient.insertObjectGroup(any())).thenReturn(
            JsonHandler.getFromString("{\"test\":\"true\"}")
        );
        // When
        JsonNode jsonNode = metadataRepository.saveObjectGroup(objectNode);
        // Then
        Assertions.assertThat(jsonNode).isNotNull();
        Assertions.assertThat(jsonNode.get("test").asText()).isEqualTo("true");
    }

    @Test
    public void updateObjectGroupById() throws Exception {
        // Given
        ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        DbObjectGroupModel objectGroupModel = new DbObjectGroupModel();
        objectGroupModel.setQualifiers(new ArrayList<>());
        UpdateMultiQuery query = QueryHandler.getQualifiersAddMultiQuery(
            objectGroupModel,
            DataObjectVersionType.BINARY_MASTER,
            1,
            new ObjectDto("1", new FileInfoDto("filename", "lastModified"))
        );
        // When
        metadataRepository.updateObjectGroupById(query, "1", "1");
        // Then
        verify(metaDataCollectClient).updateObjectGroupById(jsonNodeArgumentCaptor.capture(), eq("1"));
    }

    @Test
    public void deleteUnits() throws Exception {
        // Given - When
        metadataRepository.deleteUnits(Arrays.asList("1", "2"));
        // Then
        verify(metaDataCollectClient).deleteUnitsBulk(eq(Arrays.asList("1", "2")));
    }

    @Test
    public void deleteObjectGroups() throws Exception {
        // Given - When
        metadataRepository.deleteObjectGroups(Arrays.asList("1", "2"));
        // Then
        verify(metaDataCollectClient).deleteObjectGroupBulk(Arrays.asList("1", "2"));
    }

    private RequestResponseOK<JsonNode> getResponseWith(
        String versionId,
        String storageId,
        String strategyId,
        String usageVersion,
        String opi
    ) {
        DbVersionsModel versionsModel = new DbVersionsModel();
        versionsModel.setDataObjectVersion(usageVersion);
        versionsModel.setId(versionId);
        versionsModel.setOpi(opi);
        versionsModel.setMessageDigest("DIGEST");
        DbStorageModel storage = new DbStorageModel();
        storage.setOfferIds(Collections.singletonList(storageId));
        storage.setStrategyId(strategyId);
        versionsModel.setStorage(storage);
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        DbObjectGroupModel groupModel = new DbObjectGroupModel();
        DbQualifiersModel qualifiersModel = new DbQualifiersModel();
        qualifiersModel.setVersions(Collections.singletonList(versionsModel));
        groupModel.setQualifiers(Collections.singletonList(qualifiersModel));
        responseOK.addResult(objectMapper.valueToTree(groupModel));
        return responseOK;
    }
}
