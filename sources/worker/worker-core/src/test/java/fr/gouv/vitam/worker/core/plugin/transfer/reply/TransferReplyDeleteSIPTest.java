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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static fr.gouv.vitam.worker.core.plugin.dip.StoreExports.TRANSFER_CONTAINER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferReplyDeleteSIPTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    private TransferReplyDeleteSIP transferReplyDeleteSIPPlugin;
    private static final int tenant = 0;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        transferReplyDeleteSIPPlugin = new TransferReplyDeleteSIP();
    }

    @Test
    @RunWithCustomExecutor
    public void should_remove_sip_when_atr_is_ok() throws Exception {
        //Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getWorkspaceClientFactory()).thenReturn(workspaceClientFactory);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        File transferReplyContext = PropertiesUtils.getResourceFile("transfer/transfer_reply_context.json");
        when(handlerIO.getInput(0)).thenReturn(transferReplyContext);

        //When
        ItemStatus itemStatus = transferReplyDeleteSIPPlugin.execute(null, handlerIO);

        //Then
        assertThat(itemStatus.getItemId()).isEqualTo("TRANSFER_REPLY_DELETE_SIP");
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<String> containerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient, times(1)).deleteObject(containerNameCaptor.capture(), objectNameCaptor.capture());
        assertThat(containerNameCaptor.getValue()).isEqualTo(TRANSFER_CONTAINER);

        String expectedFilename = tenant + "/ATR_MESSAGE_REQUEST_IDENTIFIER";
        assertThat(objectNameCaptor.getValue()).isEqualTo(expectedFilename);
    }
}
