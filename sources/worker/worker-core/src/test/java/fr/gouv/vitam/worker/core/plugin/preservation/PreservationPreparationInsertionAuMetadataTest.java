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
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PreservationPreparationInsertionAuMetadataTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private BatchReportClientFactory batchReportFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private BatchReportClient batchReportClient;

    @Mock
    private WorkspaceClient workspaceClient;

    private PreservationPreparationInsertionAuMetadata plugin;

    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();

    private HandlerIO handler = new TestHandlerIO();

    @Before
    public void setUp() throws Exception {
        given(batchReportFactory.getClient()).willReturn(batchReportClient);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);

        plugin = new PreservationPreparationInsertionAuMetadata(batchReportFactory, workspaceClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_Distribution_File_For_Au() throws Exception {
        // Given
        given(workspaceClient.isExistingObject(anyString(), anyString())).willReturn(false);

        // When
        ItemStatus itemStatuses = plugin.execute(parameter, handler);

        // Then
        verify(batchReportClient, times(1)).createExtractedMetadataDistributionFileForAu(anyString());
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).isEqualTo(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_recreate_distribution_when_already_exists() throws Exception {
        // Given
        given(workspaceClient.isExistingObject(anyString(), anyString())).willReturn(true);

        // When
        ItemStatus itemStatuses = plugin.execute(parameter, handler);

        // Then
        verify(batchReportClient, never()).createExtractedMetadataDistributionFileForAu(anyString());
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).isEqualTo(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_create_Distribution_File_For_Au_batch_report() throws Exception {
        // Given
        doThrow(new VitamClientInternalException("Exception when creating diStribution file for AU"))
            .when(batchReportClient)
            .createExtractedMetadataDistributionFileForAu(any());

        // When
        ThrowableAssert.ThrowingCallable shouldThrow = () -> plugin.execute(parameter, handler);

        // Then
        assertThatThrownBy(shouldThrow).isInstanceOf(ProcessingException.class);
    }
}
