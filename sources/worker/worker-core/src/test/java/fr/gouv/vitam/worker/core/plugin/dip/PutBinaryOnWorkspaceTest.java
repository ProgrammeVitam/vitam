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
package fr.gouv.vitam.worker.core.plugin.dip;

import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import static fr.gouv.vitam.worker.core.plugin.dip.PutBinaryOnWorkspace.GUID_TO_INFO_RANK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PutBinaryOnWorkspaceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @InjectMocks
    private PutBinaryOnWorkspace putBinaryOnWorkspace;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private StorageClient storageClient;

    @Before
    public void setUp() throws Exception {
        given(storageClientFactory.getClient()).willReturn(storageClient);

        URL url = this.getClass().getResource("/PutBinaryOnWorkspace/guid_to_path.json");
        given(handlerIO.getInput(GUID_TO_INFO_RANK)).willReturn(new File(url.toURI()));
    }

    @Test
    @RunWithCustomExecutor
    public void should_send_object_from_storage_to_workspace() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId("contextTest");
        VitamThreadUtils.getVitamSession().setContractId("contractTest");
        VitamThreadUtils.getVitamSession().setRequestId("requestId");
        VitamThreadUtils.getVitamSession().setApplicationSessionId("MyApplicationId");

        // Given
        String guid = "aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq";
        ByteArrayInputStream entity = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        given(
            storageClient.getContainerAsync(
                eq("other_strategy"),
                eq(guid),
                eq(DataCategory.OBJECT),
                eq(AccessLogUtils.getNoLogAccessLog())
            )
        ).willReturn(new ServerResponse(entity, 200, new Headers<>()));
        DefaultWorkerParameters param = WorkerParametersFactory.newWorkerParameters();
        param.setObjectName(guid);

        // When
        ItemStatus itemStatus = putBinaryOnWorkspace.execute(param, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(handlerIO).transferInputStreamToWorkspace(
            "Content/aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq",
            entity,
            null,
            false
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_retry_when_transfer_storage_failed_one_times() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId("contextTest");
        VitamThreadUtils.getVitamSession().setContractId("contractTest");
        VitamThreadUtils.getVitamSession().setRequestId("requestId");
        VitamThreadUtils.getVitamSession().setApplicationSessionId("MyApplicationId");

        // Given
        String guid = "aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq";
        ByteArrayInputStream entity = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        given(
            storageClient.getContainerAsync(
                eq("other_strategy"),
                eq(guid),
                eq(DataCategory.OBJECT),
                eq(AccessLogUtils.getNoLogAccessLog())
            )
        )
            .willThrow(new StorageServerClientException("transfer failed"))
            .willReturn(new ServerResponse(entity, 200, new Headers<>()));
        DefaultWorkerParameters param = WorkerParametersFactory.newWorkerParameters();
        param.setObjectName(guid);

        // When
        ItemStatus itemStatus = putBinaryOnWorkspace.execute(param, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(handlerIO).transferInputStreamToWorkspace(
            "Content/aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq",
            entity,
            null,
            false
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_status_is_fatal_when_storage_failed_many_times() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId("contextTest");
        VitamThreadUtils.getVitamSession().setContractId("contractTest");
        VitamThreadUtils.getVitamSession().setRequestId("requestId");
        VitamThreadUtils.getVitamSession().setApplicationSessionId("MyApplicationId");

        // Given
        String guid = "aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq";
        ByteArrayInputStream entity = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        given(
            storageClient.getContainerAsync(
                eq("other_strategy"),
                eq(guid),
                eq(DataCategory.OBJECT),
                eq(AccessLogUtils.getNoLogAccessLog())
            )
        ).willThrow(new StorageServerClientException("transfer failed"));
        DefaultWorkerParameters param = WorkerParametersFactory.newWorkerParameters();
        param.setObjectName(guid);

        // When
        ItemStatus itemStatus = putBinaryOnWorkspace.execute(param, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        verify(handlerIO, never()).transferInputStreamToWorkspace(
            "Content/aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq",
            entity,
            null,
            false
        );
        verify(storageClient, times(3)).getContainerAsync(
            eq("other_strategy"),
            eq(guid),
            eq(DataCategory.OBJECT),
            eq(AccessLogUtils.getNoLogAccessLog())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_retry_when_transfer_to_workspace_failed_one_times() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId("contextTest");
        VitamThreadUtils.getVitamSession().setContractId("contractTest");
        VitamThreadUtils.getVitamSession().setRequestId("requestId");
        VitamThreadUtils.getVitamSession().setApplicationSessionId("MyApplicationId");

        // Given
        String guid = "aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq";
        given(
            storageClient.getContainerAsync(
                eq("other_strategy"),
                eq(guid),
                eq(DataCategory.OBJECT),
                eq(AccessLogUtils.getNoLogAccessLog())
            )
        ).willAnswer(
            args -> new ServerResponse(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }), 200, new Headers<>())
        );

        willThrow(new ProcessingException("transfer failed"))
            .willDoNothing()
            .given(handlerIO)
            .transferInputStreamToWorkspace(
                eq("Content/aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq"),
                any(),
                eq(null),
                eq(false)
            );

        DefaultWorkerParameters param = WorkerParametersFactory.newWorkerParameters();
        param.setObjectName(guid);

        // When
        ItemStatus itemStatus = putBinaryOnWorkspace.execute(param, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(handlerIO, times(2)).transferInputStreamToWorkspace(
            eq("Content/aeaaaaaaaaasqm2gaak5wak7uvv55tqaaaaq"),
            any(),
            eq(null),
            eq(false)
        );
    }
}
