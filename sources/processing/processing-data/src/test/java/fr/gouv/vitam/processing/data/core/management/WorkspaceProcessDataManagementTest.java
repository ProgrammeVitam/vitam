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
package fr.gouv.vitam.processing.data.core.management;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkspaceProcessDataManagementTest {

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private ProcessDataManagement processDataManagement;

    @Before
    public void setUp() {
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        processDataManagement = new WorkspaceProcessDataManagement(workspaceClientFactory);
    }

    @Test
    public void createContainerTestOK() throws Exception {
        doNothing().when(workspaceClient).createContainer(anyString());
        assertTrue(processDataManagement.createProcessContainer());
    }

    @Test
    public void createContainerTestAlreadyExists() throws Exception {
        doReturn(true).when(workspaceClient).isExistingContainer(anyString());
        assertFalse(processDataManagement.createProcessContainer());
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void createContainerTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .createContainer(anyString());
        processDataManagement.createProcessContainer();
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void isProcessContainerExistTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .isExistingContainer(anyString());
        processDataManagement.isProcessContainerExist();
    }

    @Test
    public void createFolderTestOK() throws Exception {
        doNothing().when(workspaceClient).createFolder(anyString(), anyString());
        assertTrue(processDataManagement.createFolder("folder"));
    }

    @Test
    public void createFolderTestAlreadyExists() throws Exception {
        doReturn(true).when(workspaceClient).isExistingFolder(anyString(), anyString());
        assertFalse(processDataManagement.createFolder("folder"));
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void createFolderTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .createFolder(anyString(), anyString());
        processDataManagement.createFolder("folder");
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void isFolderExistsTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .isExistingFolder(anyString(), anyString());
        processDataManagement.isFolderExist("folder");
    }

    @Test
    public void removeFolderTestOK() throws Exception {
        doReturn(true).when(workspaceClient).isExistingFolder(anyString(), anyString());
        doNothing().when(workspaceClient).deleteFolder(anyString(), anyString());
        assertTrue(processDataManagement.removeFolder("folder"));
    }

    @Test
    public void removeFolderTestAlreadyExists() throws Exception {
        doReturn(false).when(workspaceClient).isExistingFolder(anyString(), anyString());
        assertFalse(processDataManagement.removeFolder("folder"));
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void removeFolderTestException() throws Exception {
        doReturn(true).when(workspaceClient).isExistingFolder(anyString(), anyString());
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .deleteFolder(anyString(), anyString());
        processDataManagement.removeFolder("folder");
    }

    @Test
    public void persistProcessWorkflowTestOK() throws Exception {
        doNothing().when(workspaceClient).putObject(anyString(), anyString(), any());
        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processDataManagement.persistProcessWorkflow("folder", processWorkflow);
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void persistProcessWorkflowTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .putObject(anyString(), anyString(), any(Object.class));

        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processDataManagement.persistProcessWorkflow("folder", processWorkflow);
    }

    @Test
    public void getProcessWorkflowTestOK() throws Exception {
        doReturn(getResponseOKWithJsonStream()).when(workspaceClient).getObject(anyString(), anyString());
        ProcessWorkflow processWorkflow = processDataManagement.getProcessWorkflow("folder", "asyncId");
        assertNotNull(processWorkflow);
        assertEquals(Integer.valueOf(0), processWorkflow.getTenantId());
        assertEquals("operationId", processWorkflow.getOperationId());
    }

    private Response getResponseOKWithJsonStream() throws Exception {
        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setTenantId(0);
        processWorkflow.setOperationId("operationId");
        return Response.status(Response.Status.OK)
            .entity(new ByteArrayInputStream(JsonHandler.writeAsString(processWorkflow).getBytes()))
            .build();
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void getProcessWokflowTestKO() throws Exception {
        doReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build())
            .when(workspaceClient)
            .getObject(anyString(), anyString());
        processDataManagement.getProcessWorkflow("folder", "asyncId");
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void getProcessWorkflowTestException() throws Exception {
        doThrow(new ContentAddressableStorageServerException("fail"))
            .when(workspaceClient)
            .getObject(anyString(), anyString());
        processDataManagement.getProcessWorkflow("folder", "asyncId");
    }

    @Test
    public void removeProcessWorkflowTestOK() throws Exception {
        doNothing().when(workspaceClient).deleteObject(anyString(), anyString());
        doReturn(true).when(workspaceClient).isExistingObject(anyString(), anyString());
        processDataManagement.removeProcessWorkflow("folder", "asyncId");
        verify(workspaceClient).deleteObject(anyString(), anyString());
    }

    @Test(expected = ProcessingStorageWorkspaceException.class)
    public void removeProcessWorkflowTestKO() throws Exception {
        doNothing().when(workspaceClient).deleteObject(anyString(), anyString());
        doThrow(new ContentAddressableStorageServerException("Exception"))
            .when(workspaceClient)
            .isExistingObject(anyString(), anyString());
        processDataManagement.removeProcessWorkflow("folder", "asyncId");
    }

    @Test
    public void removeOperationContainerOK()
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        String operationId = "opId";

        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setOperationId(operationId);

        when(workspaceClient.isExistingContainer(operationId)).thenReturn(true);
        when(
            workspaceClient.isExistingObject(
                ProcessDataManagement.PROCESS_CONTAINER,
                ProcessDataManagement.DISTRIBUTOR_INDEX + "/" + operationId + ".json"
            )
        ).thenReturn(true);

        boolean result = processDataManagement.removeOperationContainer(processWorkflow, workspaceClientFactory);

        assertTrue(result);
        verify(workspaceClient).deleteContainer(operationId, true);
        verify(workspaceClient).deleteObject(
            ProcessDataManagement.PROCESS_CONTAINER,
            ProcessDataManagement.DISTRIBUTOR_INDEX + "/" + operationId + ".json"
        );
    }

    @Test
    public void removeOperationContainerKO_because_container_not_exist()
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        String operationId = "opId";

        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setOperationId(operationId);

        ContentAddressableStorageServerException exception = new ContentAddressableStorageServerException("Exception");

        when(workspaceClient.isExistingContainer(operationId)).thenThrow(exception);

        boolean result = processDataManagement.removeOperationContainer(processWorkflow, workspaceClientFactory);

        assertFalse(result);
        verify(workspaceClient, never()).deleteContainer(operationId, true);
    }

    @Test
    public void removeOperationContainerKO_because_object_not_exist()
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        String operationId = "opId";

        ProcessWorkflow processWorkflow = new ProcessWorkflow();
        processWorkflow.setOperationId(operationId);

        ContentAddressableStorageServerException exception = new ContentAddressableStorageServerException("Exception");

        when(
            workspaceClient.isExistingObject(
                ProcessDataManagement.PROCESS_CONTAINER,
                ProcessDataManagement.DISTRIBUTOR_INDEX + "/" + operationId + ".json"
            )
        ).thenThrow(exception);

        boolean result = processDataManagement.removeOperationContainer(processWorkflow, workspaceClientFactory);

        assertFalse(result);
        verify(workspaceClient, never()).deleteObject(
            ProcessDataManagement.PROCESS_CONTAINER,
            ProcessDataManagement.DISTRIBUTOR_INDEX + "/" + operationId + ".json"
        );
    }
}
