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
package fr.gouv.vitam.metadata.rest;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.core.ExportsPurge.ExportsPurgeService;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.graph.ReclassificationDistributionService;
import fr.gouv.vitam.metadata.core.graph.api.GraphComputeService;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MetadataManagementResource test
 */
public class MetadataManagementResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private GraphComputeService graphBuilderService;
    private MetadataManagementResource metadataManagementResource;
    private ExportsPurgeService exportsPurgeService;
    private static final String DIP_CONTAINER = "DIP";

    private static final int tenant = VitamConfiguration.getAdminTenant();

    @Before
    public void setup() {
        graphBuilderService = mock(GraphComputeService.class);
        ReclassificationDistributionService reclassificationDistributionService = mock(
            ReclassificationDistributionService.class
        );
        MetaDataConfiguration configuration = new MetaDataConfiguration();
        configuration.setUrlProcessing("http://processing.service.consul:8203/");
        configuration.setContextPath("/metadata");
        exportsPurgeService = mock(ExportsPurgeService.class);
        metadataManagementResource = new MetadataManagementResource(
            graphBuilderService,
            reclassificationDistributionService,
            ProcessingManagementClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(),
            configuration,
            exportsPurgeService
        );
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2));
    }

    @BeforeClass
    public static void beforeClass() {
        VitamConfiguration.setAdminTenant(0);
    }

    @AfterClass
    public static void afterClass() {
        VitamConfiguration.setAdminTenant(tenant);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_ok_when__graph_compute_by_dsl_handled() throws MetaDataException {
        // Given
        when(graphBuilderService.computeGraph(JsonHandler.createObjectNode())).thenReturn(
            new GraphComputeResponse(10, 3)
        );
        // When
        Response response = metadataManagementResource.computeGraphByDSL(0, JsonHandler.createObjectNode());

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        GraphComputeResponse resp = (GraphComputeResponse) response.getEntity();
        assertThat(resp).isNotNull();
        assertThat(resp.getUnitCount()).isEqualTo(10);
        assertThat(resp.getGotCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_ko_when_graph_compute_by_dsl_handled() throws MetaDataException {
        // Given
        String errorMessage = "Error in graph builder";
        when(graphBuilderService.computeGraph(JsonHandler.createObjectNode())).thenThrow(
            new RuntimeException(errorMessage)
        );

        // When
        Response response = metadataManagementResource.computeGraphByDSL(0, JsonHandler.createObjectNode());

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String responseEntity = (String) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).contains(errorMessage);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_ok_when__graph_handled() {
        // Given
        when(
            graphBuilderService.computeGraph(MetadataCollections.UNIT, Sets.newHashSet("fake"), false, true)
        ).thenReturn(new GraphComputeResponse(10, 3));
        // When
        Response response = metadataManagementResource.computeGraph(
            GraphComputeResponse.GraphComputeAction.UNIT,
            Sets.newHashSet("fake")
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        GraphComputeResponse resp = (GraphComputeResponse) response.getEntity();
        assertThat(resp).isNotNull();
        assertThat(resp.getUnitCount()).isEqualTo(10);
        assertThat(resp.getGotCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_ko_when_graph_compute_handled() {
        // Given
        String errorMessage = "Error in graph builder";
        when(
            graphBuilderService.computeGraph(MetadataCollections.UNIT, Sets.newHashSet("fake"), false, true)
        ).thenThrow(new RuntimeException(errorMessage));
        // When
        Response response = metadataManagementResource.computeGraph(
            GraphComputeResponse.GraphComputeAction.UNIT,
            Sets.newHashSet("fake")
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String responseEntity = (String) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).contains(errorMessage);
    }

    @Test
    @RunWithCustomExecutor
    public void purgeExpiredDipFilesShouldReturnOKWhenServiceOK() throws Exception {
        // Given

        // When
        Response response = metadataManagementResource.purgeExpiredDipFiles();

        // Then
        verify(exportsPurgeService).purgeExpiredFiles(DIP_CONTAINER);
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void purgeExpiredDipFilesShouldReturnInternalServerWhenServiceError() throws Exception {
        // Given
        doThrow(new ContentAddressableStorageServerException(""))
            .when(exportsPurgeService)
            .purgeExpiredFiles(DIP_CONTAINER);

        // When
        Response response = metadataManagementResource.purgeExpiredDipFiles();

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void migrationPurgeDipFilesFromOffersTest() throws Exception {
        // When
        Response response = metadataManagementResource.migrationPurgeDipFilesFromOffers();

        // Then
        verify(exportsPurgeService, times(VitamConfiguration.getTenants().size())).migrationPurgeDipFilesFromOffers();
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    }
}
