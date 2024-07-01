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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class RunningIngestsUpdateActionPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final MetaDataClient metaDataClient = mock(MetaDataClient.class);
    private static final MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);
    private static final WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);

    private static final StorageClient storageClient = mock(StorageClient.class);
    private static final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);

    private static final ProcessingManagementClient processManagementClient = mock(ProcessingManagementClient.class);
    private static final ProcessingManagementClientFactory processingManagementClientFactory = mock(
        ProcessingManagementClientFactory.class
    );

    private GUID guid = GUIDFactory.newGUID();

    private static final String UPDATED_RULES_JSON = "RunningIngestsUpdateActionPlugin/updatedRules.json";
    private static final String RUNNING_INGESTS = "RunningIngestsUpdateActionPlugin/runningIngests.json";

    private static final String AU_DETAIL = "RunningIngestsUpdateActionPlugin/archiveUnits.json";
    private static final String UPDATED_AU = "RunningIngestsUpdateActionPlugin/updatedAu.json";
    private static final StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin = mock(
        StoreMetaDataUnitActionPlugin.class
    );

    RunningIngestsUpdateActionPlugin plugin = new RunningIngestsUpdateActionPlugin(
        processingManagementClientFactory,
        metaDataClientFactory,
        storeMetaDataUnitActionPlugin
    );

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectName("archiveUnit.json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.UPDATE);

    private HandlerIO handlerIO = mock(HandlerIO.class);

    public RunningIngestsUpdateActionPluginTest() {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        reset(metaDataClient);
        reset(storageClient);
        reset(processManagementClient);
        reset(workspaceClient);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(processingManagementClientFactory.getClient()).thenReturn(processManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
    }

    @RunWithCustomExecutor
    @Test
    public void givenRunningProcessWhenExecuteThenCheckAllPossibleStatusCode() throws Exception {
        reset(storeMetaDataUnitActionPlugin);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final File runningIngests = PropertiesUtils.getResourceFile(RUNNING_INGESTS);

        final JsonNode archiveUnitToBeUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(AU_DETAIL)
        );
        final JsonNode archiveUnitUpdated = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(UPDATED_AU)
        );

        params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());

        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)
            )
        ).then(o -> PropertiesUtils.getResourceAsStream(UPDATED_RULES_JSON));
        when(handlerIO.getInput(0)).thenReturn(runningIngests);
        when(handlerIO.getLifecyclesClient()).thenReturn(mock(LogbookLifeCyclesClient.class));
        when(processManagementClient.getOperationProcessStatus(any())).thenReturn(
            new ItemStatus().setGlobalState(ProcessState.COMPLETED)
        );

        when(metaDataClient.selectUnits(any())).thenReturn(archiveUnitToBeUpdated);
        when(metaDataClient.updateUnitById(any(), any())).thenReturn(archiveUnitUpdated);

        doNothing().when(storeMetaDataUnitActionPlugin).storeDocumentsWithLfc(any(), any(), anyList());
        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        doThrow(VitamException.class)
            .when(storeMetaDataUnitActionPlugin)
            .storeDocumentsWithLfc(any(), any(), anyList());
        response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void givenEmptyFileReturnedWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final InputStream rulesUpdated = IOUtils.toInputStream("[]", "UTF-8");

        try {
            params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());
            when(
                handlerIO.getInputStreamFromWorkspace(
                    eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)
                )
            ).thenReturn(rulesUpdated);
            final ItemStatus response = plugin.execute(params, handlerIO);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
        } finally {
            rulesUpdated.close();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenWrongFileReturnedWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final InputStream rulesUpdated = IOUtils.toInputStream("<root>root</root>", "UTF-8");

        try {
            params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());

            when(
                handlerIO.getInputStreamFromWorkspace(
                    eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)
                )
            ).thenReturn(rulesUpdated);
            final ItemStatus response = plugin.execute(params, handlerIO);
            assertEquals(StatusCode.KO, response.getGlobalStatus());
        } finally {
            rulesUpdated.close();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenFileNotFoundWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)
            )
        ).thenThrow(new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
}
