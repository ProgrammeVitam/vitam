package fr.gouv.vitam.worker.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class WorkerIOManagementHelperTest {
    private WorkspaceClient workspaceClient;

    @Before
    public void setUp() {
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.create(anyObject())).thenReturn(workspaceClient);
    }
    
    @Test
    public void testGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(PropertiesUtils.getResourcesAsStream("sip.xml"));
        File file = WorkerIOManagementHelper.findFileFromWorkspace(workspaceClient, "containerName", "objectName", "workerId");
        file.delete();
    }
    
    @Test(expected=FileNotFoundException.class)
    public void testGetFileError() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenThrow(new ContentAddressableStorageNotFoundException(""));
        WorkerIOManagementHelper.findFileFromWorkspace(workspaceClient, "containerName", "objectName","workerId");
    }

}
