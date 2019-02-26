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

package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
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
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({MetaDataClientFactory.class, WorkspaceClientFactory.class, ProcessingManagementClientFactory.class})
public class RunningIngestsUpdateActionPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    RunningIngestsUpdateActionPlugin plugin = new RunningIngestsUpdateActionPlugin();
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataClientFactory;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private ProcessingManagementClient processManagementClient;
    private ProcessingManagementClientFactory processManagementClientFactory;

    private GUID guid = GUIDFactory.newGUID();

    private static final String UPDATED_RULES_JSON = "RunningIngestsUpdateActionPlugin/updatedRules.json";
    private static final String RUNNING_INGESTS = "RunningIngestsUpdateActionPlugin/runningIngests.json";

    private static final String AU_DETAIL = "RunningIngestsUpdateActionPlugin/archiveUnits.json";
    private static final String UPDATED_AU = "RunningIngestsUpdateActionPlugin/updatedAu.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE);

    private HandlerIO handlerIO = mock(HandlerIO.class);

    public RunningIngestsUpdateActionPluginTest() {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        PowerMockito.mockStatic(MetaDataClientFactory.class);
        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        PowerMockito.mockStatic(ProcessingManagementClientFactory.class);
        processManagementClient = mock(ProcessingManagementClient.class);
        processManagementClientFactory = mock(ProcessingManagementClientFactory.class);

        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(metadataClientFactory);
        PowerMockito.when(MetaDataClientFactory.getInstance().getClient())
            .thenReturn(metadataClient);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient())
            .thenReturn(workspaceClient);
        PowerMockito.when(ProcessingManagementClientFactory.getInstance()).thenReturn(processManagementClientFactory);
        PowerMockito.when(ProcessingManagementClientFactory.getInstance().getClient())
            .thenReturn(processManagementClient);
    }

    @RunWithCustomExecutor
    @Test
    public void givenRunningProcessWhenExecuteThenCheckAllPossibleStatusCode() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final File runningIngests = PropertiesUtils.getResourceFile(RUNNING_INGESTS);

        final JsonNode archiveUnitToBeUpdated =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_DETAIL));
        final JsonNode archiveUnitUpdated =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UPDATED_AU));

        params.setProcessId(GUIDFactory.newOperationLogbookGUID(0).toString());
        reset(workspaceClient);
        reset(metadataClient);
        reset(processManagementClient);
        when(handlerIO.getInputStreamFromWorkspace(
            eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)))
            .then(o -> PropertiesUtils.getResourceAsStream(UPDATED_RULES_JSON));
        when(handlerIO.getInput(0)).thenReturn(runningIngests);
        when(processManagementClient.getOperationProcessStatus(any()))
            .thenReturn(new ItemStatus().setGlobalState(ProcessState.COMPLETED));

        when(metadataClient.selectUnits(any())).thenReturn(archiveUnitToBeUpdated);
        when(metadataClient.updateUnitbyId(any(), any())).thenReturn(archiveUnitUpdated);

        StoreMetadataObjectActionHandler storeMetadataObjectActionHandler =
            mock(StoreMetadataObjectActionHandler.class);
        plugin.setStoreMetadataObjectActionHandler(storeMetadataObjectActionHandler);

        List<StatusCode> statusCodeList = Lists.newArrayList(StatusCode.OK);
        when(storeMetadataObjectActionHandler.execute(any(), any()))
            .thenAnswer(o -> new ItemStatus().increment(
                statusCodeList.get(0)));
        ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        statusCodeList.set(0, StatusCode.WARNING);
        response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        statusCodeList.set(0, StatusCode.KO);
        response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());

        statusCodeList.set(0, StatusCode.FATAL);
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
            reset(workspaceClient);
            reset(metadataClient);
            reset(processManagementClient);
            when(handlerIO.getInputStreamFromWorkspace(
                eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)))
                .thenReturn(rulesUpdated);
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
            reset(workspaceClient);
            reset(metadataClient);
            reset(processManagementClient);
            when(handlerIO.getInputStreamFromWorkspace(
                eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)))
                .thenReturn(rulesUpdated);
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
        reset(workspaceClient);
        reset(metadataClient);
        reset(processManagementClient);
        when(handlerIO.getInputStreamFromWorkspace(
            eq(UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON)))
            .thenThrow(
                new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());

    }



}
