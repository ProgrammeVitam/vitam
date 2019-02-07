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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient.FakeInboundResponse;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepareTraceabilityCheckProcessActionHandlerTest {
    private PrepareTraceabilityCheckProcessActionHandler prepareTraceabilityCheckProcessActionHandler;
    private static final String SAMPLE_TRACEABILITY_FILENAME = "SAMPLE_TRACEABILITY_FILENAME.txt";
    private static final String SAMPLE_TRACEABILITY_FILENAME_WRONG_TYPE =
        "SAMPLE_TRACEABILITY_FILENAME_WRONG_TYPE.txt";
    private static final String FAKE_URL = "http://localhost:8080";
    private HandlerIOImpl action;
    private GUID guid;
    private WorkerParameters params;
    private static final Integer TENANT_ID = 0;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private LogbookOperationsClient logbookOperationsClient;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;


    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;
    private List<IOParameter> out;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setUp() throws Exception {
        guid = GUIDFactory.newGUID();
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        final BooleanQuery query = and();
        query.add(eq("evIdProc", "aecaaaaaachgxr27absisak3tofyf4yaaaaq"));
        select.setQuery(query);
        select.getFinalSelect();
        Map<String, String> checkExtraParams = new HashMap<>();
        checkExtraParams.put(WorkerParameterName.logbookRequest.toString(), JsonHandler.unprettyPrint(query));
        params.setMap(checkExtraParams);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        storageClient = mock(StorageClient.class);
        workspaceClient = mock(WorkspaceClient.class);
        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);

        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        storageClientFactory = mock(StorageClientFactory.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        action = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, guid.getId(), "workerId",
            Lists.newArrayList());
        final List<IOParameter> in = new ArrayList<>();
        out = new ArrayList<>();
        out.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "TraceabilityOperationDetails/EVENT_DETAIL_DATA.json")));
        action.addInIOParameters(in);
    }

    @After
    public void end() {
        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testPrepareTraceabilityOK()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareTraceabilityCheckProcessActionHandler =
            new PrepareTraceabilityCheckProcessActionHandler(logbookOperationsClientFactory, storageClientFactory);
        action.addOutIOParameters(out);
        Mockito.doReturn(getTraceabilityDetails(SAMPLE_TRACEABILITY_FILENAME)).when(logbookOperationsClient)
            .selectOperation(any());
        Mockito.doReturn(false).when(workspaceClient).isExistingContainer(any());
        Mockito.doNothing().when(workspaceClient).createContainer(any());
        Mockito.doNothing().when(workspaceClient).uncompressObject(any(), any(), any(), any());
        Mockito.doReturn(
            new FakeInboundResponse(Status.OK, IOUtils.toInputStream("Fake Content", Charset.defaultCharset()),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null))
            .when(storageClient)
            .getContainerAsync(any(), any(), any(), any());

        final ItemStatus response = prepareTraceabilityCheckProcessActionHandler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testPrepareTraceabilityWithWrongOperationTypeKO()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareTraceabilityCheckProcessActionHandler =
            new PrepareTraceabilityCheckProcessActionHandler(logbookOperationsClientFactory, storageClientFactory);
        action.addOutIOParameters(out);
        Mockito.doReturn(getTraceabilityDetails(SAMPLE_TRACEABILITY_FILENAME_WRONG_TYPE)).when(logbookOperationsClient)
            .selectOperation(any());
        final ItemStatus response = prepareTraceabilityCheckProcessActionHandler.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testPrepareTraceabilityWithLogbookExceptionFATAL()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareTraceabilityCheckProcessActionHandler =
            new PrepareTraceabilityCheckProcessActionHandler(logbookOperationsClientFactory, storageClientFactory);
        action.addOutIOParameters(out);
        Mockito.doThrow(new LogbookClientException("Error with Logbook")).when(logbookOperationsClient)
            .selectOperation(any());
        final ItemStatus response = prepareTraceabilityCheckProcessActionHandler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testPrepareTraceabilityWithStorageExceptionFATAL()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareTraceabilityCheckProcessActionHandler =
            new PrepareTraceabilityCheckProcessActionHandler(logbookOperationsClientFactory, storageClientFactory);
        action.addOutIOParameters(out);
        Mockito.doReturn(getTraceabilityDetails(SAMPLE_TRACEABILITY_FILENAME)).when(logbookOperationsClient)
            .selectOperation(any());
        Mockito.doThrow(new StorageNotFoundException("Error with Storage")).when(storageClient)
            .getContainerAsync(any(), any(), any(), any());
        final ItemStatus response = prepareTraceabilityCheckProcessActionHandler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testPrepareTraceabilityWithAlreadyExistingContainerFATAL()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        prepareTraceabilityCheckProcessActionHandler =
            new PrepareTraceabilityCheckProcessActionHandler(logbookOperationsClientFactory, storageClientFactory);
        action.addOutIOParameters(out);
        Mockito.doReturn(getTraceabilityDetails(SAMPLE_TRACEABILITY_FILENAME)).when(logbookOperationsClient)
            .selectOperation(any());
        Mockito.doReturn(true).when(workspaceClient).isExistingContainer(any());
        Mockito.doReturn(
            new FakeInboundResponse(Status.OK, IOUtils.toInputStream("Fake Content", Charset.defaultCharset()),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null))
            .when(storageClient)
            .getContainerAsync(any(), any(), any(), any());
        final ItemStatus response = prepareTraceabilityCheckProcessActionHandler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    private JsonNode getTraceabilityDetails(String filename) throws Exception {
        RequestResponse responseOK = new RequestResponseOK()
            .addResult(JsonHandler.getFromFile(PropertiesUtils.findFile(filename)))
            .setHits(1, 0, 1)
            .setHttpCode(Status.OK.getStatusCode());
        return responseOK.toJsonNode();
    }
}
