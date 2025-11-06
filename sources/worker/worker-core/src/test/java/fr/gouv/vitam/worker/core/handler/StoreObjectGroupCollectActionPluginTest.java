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
package fr.gouv.vitam.worker.core.handler;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreObjectGroupCollectActionPluginTest {

    private static final String FAKE_URL = "http://localhost:8083";
    private GUID guid;
    private static String transactionId = "transactionId";
    private static final String EXISTING_OBJECT_GROUP_GUID_1 = "StoreObjectGroupCollectActionPlugin/guid1.json";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private WorkspaceClient workspaceClient;

    private WorkspaceClientFactory workspaceClientFactory;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    @Before
    public void setUp() throws IOException {
        File tempFolder = temporaryFolder.newFolder();
        guid = GUIDFactory.newGUID();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());
        storageClientFactory = mock(StorageClientFactory.class);
        storageClient = mock(StorageClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(1);

        final InputStream guid1 = PropertiesUtils.getResourceAsStream(EXISTING_OBJECT_GROUP_GUID_1);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getWorkspaceCollectClient()).thenReturn(workspaceClient);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromInputStream(guid1));

        when(workspaceClient.isExistingObject(anyString(), anyString())).thenReturn(true);
        doNothing().when(workspaceClient).bulkMove(anyString(), anyString(), any());

        try (StoreObjectGroupCollectActionPlugin instance = new StoreObjectGroupCollectActionPlugin()) {
            ItemStatus response = instance.execute(getWorkerParametersInstance(), handlerIO);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
        }
    }

    private WorkerParameters getWorkerParametersInstance() {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters(WorkFlowExecutionContext.VITAM)
            .setUrlWorkspace(FAKE_URL)
            .setUrlMetadata(FAKE_URL)
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("OBJ_STORAGE_COLLECT")
            .setContainerName(guid.getId());

        params.putParameterValue(WorkerParameterName.collectTransactionId, transactionId);
        return params;
    }
}
