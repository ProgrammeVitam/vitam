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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ListRunningIngestsActionHandlerTest {

    private static final ProcessingManagementClient processManagementClient = mock(ProcessingManagementClient.class);
    private static final ProcessingManagementClientFactory processManagementClientFactory = mock(
        ProcessingManagementClientFactory.class
    );
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private List<ProcessDetail> list;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;
    private ProcessDetail pw = new ProcessDetail();

    private ListRunningIngestsActionHandler plugin = new ListRunningIngestsActionHandler(
        processManagementClientFactory
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

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        reset(workspaceClient);
        reset(processManagementClient);
        when(processManagementClientFactory.getClient()).thenReturn(processManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        action = new HandlerIOImpl(
            workspaceClientFactory,
            mock(LogbookLifeCyclesClientFactory.class),
            guid.getId(),
            "workerId",
            Lists.newArrayList()
        );
        out = new ArrayList<>();
        out.add(
            new IOParameter()
                .setUri(
                    new ProcessingUri(
                        UriPrefix.WORKSPACE,
                        UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.RUNNING_INGESTS_JSON
                    )
                )
        );

        pw.setOperationId(GUIDFactory.newOperationLogbookGUID(0).toString());
        pw.setGlobalState(ProcessState.RUNNING.toString());
        pw.setStepStatus(StatusCode.STARTED.toString());
        list = new ArrayList<ProcessDetail>();
        list.add(pw);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenRunningProcessWhenExecuteThenReturnResponseOK() throws Exception {
        action.addOutIOParameters(out);
        when(processManagementClient.listOperationsDetails(any())).thenReturn(
            new RequestResponseOK<ProcessDetail>().addAllResults(list)
        );
        saveWorkspacePutObject(
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.RUNNING_INGESTS_JSON
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode listRunning = getSavedWorkspaceObject(
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.RUNNING_INGESTS_JSON
        );
        ProcessDetail process = null;
        int numberOfProcesses = 0;
        for (final JsonNode objNode : listRunning) {
            numberOfProcesses++;
            process = JsonHandler.getFromJsonNode(objNode, ProcessDetail.class);
        }
        assertEquals(1, numberOfProcesses);
        assertEquals(pw.getOperationId(), process.getOperationId());
    }

    @Test
    public void givenRunningProcessesWhenExecuteThenReturnResponseOK() throws Exception {
        action.addOutIOParameters(out);

        ProcessDetail pw2 = new ProcessDetail();
        pw2.setOperationId(GUIDFactory.newOperationLogbookGUID(0).toString());
        pw2.setGlobalState(ProcessState.RUNNING.toString());
        pw2.setStepStatus(StatusCode.STARTED.toString());
        list = new ArrayList<>();
        list.add(pw);
        list.add(pw2);

        when(processManagementClient.listOperationsDetails(any())).thenReturn(
            new RequestResponseOK<ProcessDetail>().addAllResults(list)
        );
        saveWorkspacePutObject(
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.RUNNING_INGESTS_JSON
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode listRunning = getSavedWorkspaceObject(
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.RUNNING_INGESTS_JSON
        );
        int numberOfProcesses = 0;
        for (final JsonNode objNode : listRunning) {
            numberOfProcesses++;
        }
        assertEquals(2, numberOfProcesses);
    }

    private void saveWorkspacePutObject(String filename) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgument(2);
            java.nio.file.Path file = java.nio.file.Paths.get(
                System.getProperty("vitam.tmp.folder") +
                "/" +
                action.getContainerName() +
                "_" +
                action.getWorkerId() +
                "/" +
                filename.replaceAll("/", "_")
            );
            java.nio.file.Files.copy(inputStream, file);
            return null;
        })
            .when(workspaceClient)
            .putObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(filename),
                org.mockito.ArgumentMatchers.any(InputStream.class)
            );
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        File objectNameFile = new File(
            System.getProperty("vitam.tmp.folder") +
            "/" +
            action.getContainerName() +
            "_" +
            action.getWorkerId() +
            "/" +
            filename.replaceAll("/", "_")
        );
        return JsonHandler.getFromFile(objectNameFile);
    }

    @Test
    public void givenProcessErrorWhenExecuteThenReturnResponseFATAL() throws Exception {
        action.addOutIOParameters(out);
        when(processManagementClient.listOperationsDetails(any())).thenThrow(
            new VitamClientException("Process Management error")
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
}
