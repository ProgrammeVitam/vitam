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

package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FinalizeUnitLifecycleTraceabilityActionPluginTest {

    private GUID guid = GUIDFactory.newGUID();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private HandlerIOImpl handlerIO;
    private static final Integer TENANT_ID = 0;

    private static final String LAST_OPERATION = "FinalizeUnitLifecycleTraceabilityActionPlugin/lastOperation.json";
    private static final String LAST_OPERATION_EMPTY =
        "FinalizeUnitLifecycleTraceabilityActionPlugin/lastOperationEmpty.json";
    private static final String TRACEABILITY_INFO =
        "FinalizeUnitLifecycleTraceabilityActionPlugin/traceabilityInformation.json";
    private static final String TRACEABILITY_DATA =
        "FinalizeUnitLifecycleTraceabilityActionPlugin/traceabilityData.jsonl";
    private static final String TRACEABILITY_DATA_EMPTY =
        "FinalizeUnitLifecycleTraceabilityActionPlugin/traceabilityData_Empty.jsonl";
    private static final String TRACEABILITY_STATS =
        "FinalizeUnitLifecycleTraceabilityActionPlugin/traceabilityStats.json";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private LogbookOperationsClient logbookOperationsClient;

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083").setProcessId(guid.getId())
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY);

    private List<IOParameter> in;

    public FinalizeUnitLifecycleTraceabilityActionPluginTest() {
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

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        String objectId = "objectId";
        handlerIO =
            new HandlerIOImpl(workspaceClient, "FinalizeUnitLifecycleTraceabilityActionPluginTest", "workerId",
                Lists.newArrayList(objectId));
        handlerIO.setCurrentObjectId(objectId);

        in = new ArrayList<>();
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "lastOperation.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "traceabilityInformation.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "traceabilityData.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "traceabilityStats.json")));
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void givenLogbookNotFoundWhenExecuteThenReturnResponseFATAL() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(2, PropertiesUtils.getResourceFile(TRACEABILITY_DATA), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATS), false);
        handlerIO.addInIOParameters(in);
        when(logbookOperationsClient.selectOperation(any()))
            .thenThrow(new LogbookClientException("LogbookClientException"));

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        FinalizeUnitLifecycleTraceabilityActionPlugin plugin = new FinalizeUnitLifecycleTraceabilityActionPlugin(
            logbookOperationsClientFactory, workspaceClientFactory);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoLifecycleWhenExecuteThenReturnResponseOKWithNoZipCreated() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION_EMPTY), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(2, PropertiesUtils.getResourceFile(TRACEABILITY_DATA_EMPTY), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATS), false);
        handlerIO.addInIOParameters(in);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        Mockito.doNothing().when(workspaceClient).createContainer(any());

        Mockito.doReturn(JsonHandler.createObjectNode()).when(logbookOperationsClient).selectOperation(any());

        saveWorkspacePutObject("LogbookUnitLifecycles", ".zip");

        FinalizeUnitLifecycleTraceabilityActionPlugin plugin = new FinalizeUnitLifecycleTraceabilityActionPlugin(
            logbookOperationsClientFactory, workspaceClientFactory);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        try {
            getSavedWorkspaceObject("LogbookUnitLifecycles", ".zip");
            fail("Should throw an exception");
        } catch (FileNotFoundException e) {
            // do nothing
        }
    }

    @Test
    @RunWithCustomExecutor
    public void givenNothingSpecialWhenExecuteThenReturnResponseOK() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(2, PropertiesUtils.getResourceFile(TRACEABILITY_DATA), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATS), false);
        handlerIO.addInIOParameters(in);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        Mockito.doNothing().when(workspaceClient).createContainer(any());

        Mockito.doReturn(getLogbookOperation()).when(logbookOperationsClient).selectOperation(any());

        saveWorkspacePutObject("LogbookUnitLifecycles", ".zip");

        FinalizeUnitLifecycleTraceabilityActionPlugin plugin = new FinalizeUnitLifecycleTraceabilityActionPlugin(
            logbookOperationsClientFactory, workspaceClientFactory);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        InputStream stream = getSavedWorkspaceObject("LogbookUnitLifecycles", ".zip");
        assertNotNull(stream);

        JsonNode evDetData = JsonHandler.getFromString(response.getEvDetailData());
        JsonAssert.assertJsonEquals(evDetData.get("Statistics"),
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TRACEABILITY_STATS)));
        assertNotNull(evDetData);
    }

    private static JsonNode getLogbookOperation()
        throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop =
            new LogbookOperation(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LAST_OPERATION)));
        response.addResult(JsonHandler.getFromString(JSON.serialize(lop)));
        return JsonHandler.toJsonNode(response);
    }

    private void saveWorkspacePutObject(String filenameContains, String extension)
        throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgument(2);
            java.nio.file.Path file =
                java.nio.file.Paths
                    .get(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                        handlerIO.getWorkerId() + "/" + filenameContains.replaceAll("/", "_") + extension);
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(anyString(),
            and(ArgumentMatchers.endsWith(extension), ArgumentMatchers.contains(filenameContains)),
            ArgumentMatchers.any(InputStream.class));
    }

    private InputStream getSavedWorkspaceObject(String filename, String extension)
        throws FileNotFoundException {
        File objectNameFile =
            new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_") + extension);
        return new FileInputStream(objectNameFile);
    }

}
