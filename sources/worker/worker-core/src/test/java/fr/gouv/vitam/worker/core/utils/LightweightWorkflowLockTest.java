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

package fr.gouv.vitam.worker.core.utils;

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class LightweightWorkflowLockTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ProcessingManagementClientFactory processingManagementClientFactory;

    @Mock
    private ProcessingManagementClient processingManagementClient;

    private LightweightWorkflowLock lightweightWorkflowLock;

    @Before
    public void init() {
        doReturn(processingManagementClient).when(processingManagementClientFactory).getClient();
        lightweightWorkflowLock = new LightweightWorkflowLock(processingManagementClientFactory);
    }

    @Test
    public void listConcurrentWorkflows_ResponseOK() throws Exception {
        // Given
        String workflowId = "workflow";
        String currentProcessId = "1234";

        ProcessDetail processDetail1 = new ProcessDetail();
        processDetail1.setOperationId("SomeOtherProcessId");
        processDetail1.setGlobalState("PAUSE");
        processDetail1.setStepStatus("FATAL");

        ProcessDetail processDetail2 = new ProcessDetail();
        processDetail2.setOperationId(currentProcessId);
        processDetail2.setGlobalState("RUNNING");
        processDetail2.setStepStatus("OK");

        doReturn(new RequestResponseOK<>().addAllResults(Arrays.asList(processDetail1, processDetail2)))
            .when(processingManagementClient)
            .listOperationsDetails(any());

        // When
        List<ProcessDetail> processDetails = lightweightWorkflowLock.listConcurrentWorkflows(
            Collections.singletonList(workflowId),
            currentProcessId
        );

        // Then
        ArgumentCaptor<ProcessQuery> processQueryArgumentCaptor = ArgumentCaptor.forClass(ProcessQuery.class);
        verify(processingManagementClient).listOperationsDetails(processQueryArgumentCaptor.capture());
        assertThat(processDetails).hasSize(1);
        assertThat(processDetails.get(0)).isEqualTo(processDetail1);
    }

    @Test
    public void listConcurrentWorkflows_VitamError() throws Exception {
        // Given
        doReturn(new VitamError("KO")).when(processingManagementClient).listOperationsDetails(any());

        // When / Then
        assertThatThrownBy(
            () -> lightweightWorkflowLock.listConcurrentWorkflows(Collections.singletonList("any"), "any")
        ).isInstanceOf(VitamClientException.class);
    }
}
