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
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, LogbookLifeCyclesClientFactory.class,
    LogbookOperationsClientFactory.class})
public class FinalizeLifecycleTraceabilityActionHandlerTest {

    FinalizeLifecycleTraceabilityActionHandler plugin = new FinalizeLifecycleTraceabilityActionHandler();

    private GUID guid = GUIDFactory.newGUID();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private HandlerIOImpl handlerIO;
    private List<IOParameter> out;
    private static final Integer TENANT_ID = 0;

    private final List<URI> uriListWorkspaceOKUnit = new ArrayList<>();
    private final List<URI> uriListWorkspaceOKObj = new ArrayList<>();

    private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";
    private static final String LAST_OPERATION = "FinalizeLifecycleTraceabilityActionHandler/lastOperation.json";
    private static final String TRACEABILITY_INFO =
        "FinalizeLifecycleTraceabilityActionHandler/traceabilityInformation.json";
    private static final String OBJECT_FILE =
        "FinalizeLifecycleTraceabilityActionHandler/object.txt";
    private static final String UNIT_FILE =
        "FinalizeLifecycleTraceabilityActionHandler/unit.txt";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private LogbookOperationsClient logbookOperationsClient;

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083").setProcessId(guid.getId())
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY);

    private List<IOParameter> in;

    public FinalizeLifecycleTraceabilityActionHandlerTest() throws FileNotFoundException {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient())
            .thenReturn(workspaceClient);

        PowerMockito.mockStatic(LogbookLifeCyclesClientFactory.class);
        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance()).thenReturn(logbookLifeCyclesClientFactory);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance().getClient())
            .thenReturn(logbookLifeCyclesClient);

        PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
        logbookOperationsClient = mock(LogbookOperationsClient.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance().getClient()).thenReturn(logbookOperationsClient);

        SystemPropertyUtil.refresh();
        handlerIO = new HandlerIOImpl(workspaceClient, "FinalizeLifecycleTraceabilityActionHandlerTest", "workerId");
        in = new ArrayList<>();
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/lastOperation.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/traceabilityInformation.json")));
        uriListWorkspaceOKUnit
            .add(new URI(URLEncoder.encode("aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq.json", CharsetUtils.UTF_8)));
        uriListWorkspaceOKObj
            .add(new URI(URLEncoder.encode("aebaaaaaaahgausqab7boak55jchzyqaaaaq.json", CharsetUtils.UTF_8)));
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void givenFilesNotInSubFoldersWhenExecuteThenReturnResponseFATAL() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addInIOParameters(in);
        Mockito.reset(workspaceClient);
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_OBJECTS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKObj));
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_UNITS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKUnit));
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException("ContentAddressableStorageNotFoundException"));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLogbookNotFoundWhenExecuteThenReturnResponseFATAL() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addInIOParameters(in);
        Mockito.reset(workspaceClient);
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_OBJECTS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKObj));
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_UNITS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKUnit));
        InputStream objectFile = new FileInputStream(PropertiesUtils.findFile(OBJECT_FILE));
        InputStream unitFile = new FileInputStream(PropertiesUtils.findFile(UNIT_FILE));
        when(workspaceClient.getObject(anyObject(),
            eq(SedaConstants.LFC_OBJECTS_FOLDER + "/aebaaaaaaahgausqab7boak55jchzyqaaaaq.json")))
                .thenReturn(Response.status(Status.OK).entity(objectFile).build());
        when(workspaceClient.getObject(anyObject(),
            eq(SedaConstants.LFC_UNITS_FOLDER + "/aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq.json")))
                .thenReturn(Response.status(Status.OK).entity(unitFile).build());
        when(logbookOperationsClient.selectOperation(anyObject()))
            .thenThrow(new LogbookClientException("LogbookClientException"));

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenNothingSpecialWhenExecuteThenReturnResponseOK() throws Exception {
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addInIOParameters(in);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream objectFile = new FileInputStream(PropertiesUtils.findFile(OBJECT_FILE));
        InputStream unitFile = new FileInputStream(PropertiesUtils.findFile(UNIT_FILE));
        Mockito.reset(workspaceClient);
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_OBJECTS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKObj));
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_UNITS_FOLDER)))
            .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKUnit));

        when(workspaceClient.getObject(anyObject(),
            eq(SedaConstants.LFC_OBJECTS_FOLDER + "/aebaaaaaaahgausqab7boak55jchzyqaaaaq.json")))
                .thenReturn(Response.status(Status.OK).entity(objectFile).build());
        when(workspaceClient.getObject(anyObject(),
            eq(SedaConstants.LFC_UNITS_FOLDER + "/aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq.json")))
                .thenReturn(Response.status(Status.OK).entity(unitFile).build());

        Mockito.doNothing().when(workspaceClient).createContainer(anyObject());

        Mockito.doReturn(getLogbookOperation()).when(logbookOperationsClient).selectOperation(anyObject());

        saveWorkspacePutObject("LogbookLifecycles", ".zip");

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        InputStream stream = getSavedWorkspaceObject("LogbookLifecycles", ".zip");
        assertNotNull(stream);
    }

    private static JsonNode getLogbookOperation()
        throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop =
            new LogbookOperation(IOUtils.toString(PropertiesUtils.getResourceAsStream(LAST_OPERATION)));
        response.addResult(JsonHandler.getFromString(lop.toJson()));
        return JsonHandler.toJsonNode(response);
    }

    private void saveWorkspacePutObject(String filenameContains, String extension)
        throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgumentAt(2, InputStream.class);
            java.nio.file.Path file =
                java.nio.file.Paths
                    .get(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                        handlerIO.getWorkerId() + "/" + filenameContains.replaceAll("/", "_") + extension);
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(anyString(),
            and(Matchers.endsWith(extension), Matchers.contains(filenameContains)),
            org.mockito.Matchers.any(InputStream.class));
    }

    private InputStream getSavedWorkspaceObject(String filename, String extension)
        throws InvalidParseOperationException, FileNotFoundException {
        File objectNameFile =
            new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_") + extension);
        return new FileInputStream(objectNameFile);
    }

}
