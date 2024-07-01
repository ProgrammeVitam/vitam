/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VitamPoolingClientTest {

    public static final String GUID = "guid";
    public static final int TENANT_ID = 0;

    @Test(expected = VitamException.class)
    public void testWaitThenVitamException() throws Exception {
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any())).thenReturn(new VitamError(""));
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        vitamPoolingClient.wait(TENANT_ID, GUID, 30, 1000L, TimeUnit.MILLISECONDS);
    }

    @Test(expected = VitamClientException.class)
    public void testWaitThenVitamClientException() throws Exception {
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        doThrow(new VitamClientException("")).when(operationStatusClient).getOperationProcessStatus(any(), any());
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        vitamPoolingClient.wait(TENANT_ID, GUID, 30, 1000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testWaitWithFiveParameters() throws Exception {
        RequestResponseOK<ItemStatus> running = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.RUNNING)
        );
        RequestResponseOK<ItemStatus> completed = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.COMPLETED)
        );
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any()))
            .thenReturn(running)
            .thenReturn(running)
            .thenReturn(running)
            .thenReturn(completed);
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        boolean wait = vitamPoolingClient.wait(TENANT_ID, GUID, 1, 1L, TimeUnit.MILLISECONDS);
        assertThat(wait).isFalse();
        wait = vitamPoolingClient.wait(TENANT_ID, GUID, 1, 1L, TimeUnit.MILLISECONDS);
        assertThat(wait).isFalse();
        wait = vitamPoolingClient.wait(TENANT_ID, GUID, 2, 1L, TimeUnit.MILLISECONDS);
        assertThat(wait).isTrue();
    }

    @Test
    public void testWaitWithThreeParametersCompletedThenTrue() throws Exception {
        RequestResponseOK<ItemStatus> completed = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.COMPLETED)
        );
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any())).thenReturn(completed);
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        boolean wait = vitamPoolingClient.wait(TENANT_ID, GUID, null);
        assertThat(wait).isTrue();
    }

    @Test
    public void testWaitWithThreeParametersPauseThenTrue() throws Exception {
        RequestResponseOK<ItemStatus> pause = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.PAUSE).increment(StatusCode.OK)
        );
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any())).thenReturn(pause);
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        boolean wait = vitamPoolingClient.wait(TENANT_ID, GUID, null);
        assertThat(wait).isTrue();
    }

    @Test
    public void testWaitWithTwoParametersCompletedThenTrue() throws Exception {
        RequestResponseOK<ItemStatus> completed = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.COMPLETED)
        );
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any())).thenReturn(completed);
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        boolean wait = vitamPoolingClient.wait(TENANT_ID, GUID);
        assertThat(wait).isTrue();
    }

    @Test
    public void testWaitWithTwoParametersPauseThenTrue() throws Exception {
        RequestResponseOK<ItemStatus> paused = new RequestResponseOK<ItemStatus>().addResult(
            new ItemStatus().setGlobalState(ProcessState.PAUSE).increment(StatusCode.OK)
        );
        OperationStatusClient operationStatusClient = mock(OperationStatusClient.class);
        when(operationStatusClient.getOperationProcessStatus(any(), any())).thenReturn(paused);
        VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(operationStatusClient);
        boolean wait = vitamPoolingClient.wait(TENANT_ID, GUID);
        assertThat(wait).isTrue();
    }
}
