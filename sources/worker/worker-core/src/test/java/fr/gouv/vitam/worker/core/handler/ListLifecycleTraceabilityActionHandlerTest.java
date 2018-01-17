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

package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({LogbookLifeCyclesClientFactory.class})
public class ListLifecycleTraceabilityActionHandlerTest {

    ListLifecycleTraceabilityActionHandler handler = new ListLifecycleTraceabilityActionHandler();

    private HandlerIOImpl handlerIO;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;
    private static final Integer TENANT_ID = 0;

    private static final String HANDLER_ID = "PREPARE_LC_TRACEABILITY";

    private static final String LFC_OBJECTS_JSON = "ListLifecycleTraceabilityActionHandler/lfc_objects.json";
    private static final String LFC_UNITS_JSON = "ListLifecycleTraceabilityActionHandler/lfc_units.json";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY)
            .putParameterValue(WorkerParameterName.lifecycleTraceabilityOverlapDelayInSeconds, "300");

    public ListLifecycleTraceabilityActionHandlerTest() throws FileNotFoundException {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        LogbookLifeCyclesClientFactory.changeMode(null);
        PowerMockito.mockStatic(LogbookLifeCyclesClientFactory.class);
        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance()).thenReturn(logbookLifeCyclesClientFactory);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance().getClient())
            .thenReturn(logbookLifeCyclesClient);
        handlerIO = new HandlerIOImpl(workspaceClient, "ListLifecycleTraceabilityActionHandlerTest", "workerId");
        // mock later ?
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            "Operations/lastOperation.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            "Operations/traceabilityInformation.json")));
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(handler.getId());
        assertEquals(handler.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode unitsLFC =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_UNITS_JSON));
        final JsonNode objectsLFC =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSON));
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.selectUnitLifeCycle(anyObject())).thenReturn(unitsLFC);
        when(logbookLifeCyclesClient.selectObjectGroupLifeCycle(anyObject())).thenReturn(objectsLFC);

        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa0.json");
        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1.json");
        saveWorkspacePutObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa0.json");
        saveWorkspacePutObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1.json");
        saveWorkspacePutObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa2.json");

        saveWorkspacePutObject("Operations/lastOperation.json");
        saveWorkspacePutObject("Operations/traceabilityInformation.json");

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa0.json"));
        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1.json"));

        assertNotNull(getSavedWorkspaceObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa0.json"));
        assertNotNull(getSavedWorkspaceObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1.json"));
        assertNotNull(getSavedWorkspaceObject(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/aedqaaaaacaam7mxaaaamakvhiv4rsiaaa2.json"));

        assertNotNull(getSavedWorkspaceObject("Operations/lastOperation.json"));
        JsonNode traceabilityInformation = getSavedWorkspaceObject("Operations/traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertEquals(2, traceabilityInformation.get("numberUnitLifecycles").asLong());
        assertEquals(3, traceabilityInformation.get("numberObjectLifecycles").asLong());
        assertNotNull(traceabilityInformation.get("startDate").asText());
        assertNotNull(traceabilityInformation.get("endDate").asText());

        assertEquals(3, getNumberOfSavedObjectInWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER));
        assertEquals(2, getNumberOfSavedObjectInWorkspace(UpdateWorkflowConstants.UNITS_FOLDER));
        assertEquals(2, getNumberOfSavedObjectInWorkspace("Operations"));
    }


    @Test
    @RunWithCustomExecutor
    public void givenSelectLFCErrorWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);

        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.selectUnitLifeCycle(anyObject()))
            .thenThrow(new InvalidParseOperationException("InvalidParseOperationException"));

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceInErrorWhenExecuteThenReturnResponseKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(handler.getId());
        assertEquals(handler.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode unitsLFC =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_UNITS_JSON));
        final JsonNode objectsLFC =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSON));
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.selectUnitLifeCycle(anyObject())).thenReturn(unitsLFC);
        when(logbookLifeCyclesClient.selectObjectGroupLifeCycle(anyObject())).thenReturn(objectsLFC);

        doThrow(new ContentAddressableStorageServerException("ContentAddressableStorageServerException"))
            .when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLogbookExceptionWhenExecuteThenReturnResponseFATAL() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);
        reset(logbookLifeCyclesClient);        
        when(logbookLifeCyclesClient.selectUnitLifeCycle(anyObject()))
            .thenThrow(new LogbookClientException("LogbookClientException"));

        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }


    private void saveWorkspacePutObject(String filename) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgumentAt(2, InputStream.class);
            java.nio.file.Path file =
                java.nio.file.Paths
                    .get(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                        handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_"));
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(org.mockito.Matchers.anyString(),
            org.mockito.Matchers.eq(filename), org.mockito.Matchers.any(InputStream.class));
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        File objectNameFile =
            new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_"));
        return JsonHandler.getFromFile(objectNameFile);
    }

    private int getNumberOfSavedObjectInWorkspace(String filter) {
        return new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
            handlerIO.getWorkerId()).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().contains(filter.toLowerCase()) && name.toLowerCase().endsWith(".json");
                }
            }).length;
    }

}
