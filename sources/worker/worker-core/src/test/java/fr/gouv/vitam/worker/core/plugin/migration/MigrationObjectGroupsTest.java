package fr.gouv.vitam.worker.core.plugin.migration;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MigrationObjectGroupsTest {
    private static final int TENAN_ID = 0;
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Mock private MetaDataClientFactory metaDataClientFactory;
    @Mock private MetaDataClient metaDataClient;

    @Mock private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    @Mock private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock private StorageClientFactory storageClientFactory;
    @Mock private StorageClient storageClient;

    @Mock private WorkspaceClientFactory workspaceClientFactory;
    @Mock private WorkspaceClient workspaceClient;

    @Mock private HandlerIO handlerIO;
    @Mock private WorkerParameters defaultWorkerParameters;
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    public MigrationObjectGroupsTest() {
        defaultWorkerParameters = mock(WorkerParameters.class);
    }

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        doReturn(workspaceClientFactory).when(handlerIO).getWorkspaceClientFactory();
    }

    @Test
    @RunWithCustomExecutor
    public void should_migrate_and_save_objects_groups() throws Exception {

        //GIVEN
        VitamThreadUtils.getVitamSession().setTenantId(TENAN_ID);
        String containerName = GUIDFactory.newRequestIdGUID(TENAN_ID).getId();
        VitamThreadUtils.getVitamSession().setRequestId(containerName);
        doReturn(containerName).when(handlerIO).getContainerName();

        GUID guid = GUIDFactory.newGUID();
        MigrationObjectGroups migrationObjectGroup =
            new MigrationObjectGroups(metaDataClientFactory, logbookLifeCyclesClientFactory, storageClientFactory);
        BDDMockito.given(defaultWorkerParameters.getContainerName()).willReturn(containerName);
        BDDMockito.given(defaultWorkerParameters.getObjectName()).willReturn(guid.getId());

        RequestResponseOK oGResponse = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/migration/resultObjectGroup.json"),
                RequestResponseOK.class);
        when(metaDataClient.getObjectGroupByIdRaw(guid.getId())).thenReturn(oGResponse);
        JsonNode lfcResponse = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/migration/LFCObjectGroupResponse.json"),
                JsonNode.class);

        when(logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(guid.getId()))
            .thenReturn(lfcResponse);

        //WHEN
        ItemStatus execute = migrationObjectGroup.execute(defaultWorkerParameters, handlerIO);

        //THEN

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);


        verify(metaDataClient).updateObjectGroupById(any(JsonNode.class), eq(guid.getId()));

        verify(storageClient).storeFileFromWorkspace(eq("default"), eq(DataCategory.OBJECTGROUP),
            eq(guid.getId() + ".json"),
            any(ObjectDescription.class));
        verify(workspaceClient).deleteObject(eq(containerName),
            eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + guid.getId() + ".json"));
    }
}
