/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.graph.GraphBuilderException;
import fr.gouv.vitam.metadata.core.graph.StoreGraphException;
import fr.gouv.vitam.metadata.core.graph.StoreGraphService;
import fr.gouv.vitam.metadata.core.graph.api.GraphBuilderService;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.core.reconstruction.ReconstructionService;
import org.junit.Before;
import org.junit.Test;

/**
 * MetadataManagementResource test
 */
public class MetadataManagementResourceTest {

    private ReconstructionService reconstructionService;
    private StoreGraphService storeGraphService;
    private GraphBuilderService graphBuilderService;
    private ReconstructionRequestItem requestItem;

    @Before
    public void setup() {
        reconstructionService = mock(ReconstructionService.class);
        storeGraphService = mock(StoreGraphService.class);
        graphBuilderService = mock(GraphBuilderService.class);
        requestItem = new ReconstructionRequestItem();
        requestItem.setCollection("unit").setTenant(10).setLimit(100);
    }


    @Test
    public void should_return_ok_when_store_graph_handled() throws StoreGraphException {
        // Given
        final Map<MetadataCollections, Integer> map = new HashMap<>();
        map.put(MetadataCollections.UNIT, 10);
        map.put(MetadataCollections.OBJECTGROUP, 3);
        when(storeGraphService.tryStoreGraph()).thenReturn(map);
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.storeGraph();

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        Map<MetadataCollections, Integer> responseEntity = (Map<MetadataCollections, Integer>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.size()).isEqualTo(2);
        assertThat(responseEntity.get(MetadataCollections.UNIT)).isEqualTo(10);
        assertThat(responseEntity.get(MetadataCollections.OBJECTGROUP)).isEqualTo(3);
    }


    @Test
    public void should_return_ko_when_store_graph_handled() throws StoreGraphException {
        // Given
        String errorMessage = "Error store unit graph in the offer";
        when(storeGraphService.tryStoreGraph()).thenThrow(new RuntimeException(errorMessage));
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.storeGraph();

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String responseEntity = (String) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).contains(errorMessage);
    }


    @Test
    public void should_return_ok_when__graph_builder_handled() throws GraphBuilderException {
        // Given
        final Map<MetadataCollections, Integer> map = new HashMap<>();
        map.put(MetadataCollections.UNIT, 10);
        map.put(MetadataCollections.OBJECTGROUP, 3);
        when(graphBuilderService.computeGraph()).thenReturn(map);
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.computeGraph();

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        Map<MetadataCollections, Integer> responseEntity = (Map<MetadataCollections, Integer>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.size()).isEqualTo(2);
        assertThat(responseEntity.get(MetadataCollections.UNIT)).isEqualTo(10);
        assertThat(responseEntity.get(MetadataCollections.OBJECTGROUP)).isEqualTo(3);
    }


    @Test
    public void should_return_ko_when_graph_builder_handled() throws GraphBuilderException {
        // Given
        String errorMessage = "Error in graph builder";
        when(graphBuilderService.computeGraph()).thenThrow(new RuntimeException(errorMessage));
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.computeGraph();

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String responseEntity = (String) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).contains(errorMessage);
    }

    @Test
    public void should_return_ok_when_request_item_full() {
        // Given
        ReconstructionResponseItem responseItem = new ReconstructionResponseItem(requestItem, StatusCode.OK);

        when(reconstructionService.reconstruct(requestItem)).thenReturn(responseItem);
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.reconstructCollection(Collections.singletonList(requestItem));

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        List<ReconstructionResponseItem> responseEntity = (ArrayList<ReconstructionResponseItem>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.size()).isEqualTo(1);
        assertThat(responseEntity.get(0).getCollection()).isEqualTo("unit");
        assertThat(responseEntity.get(0).getTenant()).isEqualTo(10);
        assertThat(responseEntity.get(0).getStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void should_return_empty_response_when_that_request_empty() {
        // Given
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When
        Response response = reconstructionResource.reconstructCollection(new ArrayList<>());
        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        List<ReconstructionResponseItem> responseEntity = (ArrayList<ReconstructionResponseItem>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).isEmpty();
    }

    @Test
    public void should_return_request_offset_when_reconstruction_throws_database_exception() {
        // Given
        when(reconstructionService.reconstruct(requestItem)).thenThrow(new IllegalArgumentException("Database error"));
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);

        // When
        Response response = reconstructionResource.reconstructCollection(Arrays.asList(requestItem));

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        List<ReconstructionResponseItem> responseEntity = (ArrayList<ReconstructionResponseItem>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.size()).isEqualTo(1);
        assertThat(responseEntity.get(0).getCollection()).isEqualTo("unit");
        assertThat(responseEntity.get(0).getTenant()).isEqualTo(10);
    }

    @Test
    public void should_return_ok_when__request_item_no_offset() {
        // Given
        MetadataManagementResource reconstructionResource =
            new MetadataManagementResource(reconstructionService, storeGraphService, graphBuilderService);
        // When / Then
        assertThatCode(() -> reconstructionResource.reconstructCollection(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
