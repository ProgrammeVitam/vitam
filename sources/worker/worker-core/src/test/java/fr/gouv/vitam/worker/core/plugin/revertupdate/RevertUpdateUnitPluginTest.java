/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.worker.core.plugin.revertupdate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.metadata.core.model.UpdateUnitKey;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory.newWorkerParameters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RevertUpdateUnitPluginTest {

    private static final int TENANT_ID = 0;

    private static final String CONTAINER_NAME = "aebaaaaaaaag3r7cabf4aak2izdlnwiaaaop";
    private static final String UNIT = "MassUpdateUnitsProcess/unitMd.json";
    private static final String METDATA_UNIT_RESPONSE_JSON = "MassUpdateUnitsProcess/unit.json";
    private static final String LFC_UNIT_RESPONSE_JSON = "MassUpdateUnitsProcess/lfc.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory lfcClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    @InjectMocks
    private RevertUpdateUnitPlugin revertUpdateUnitPlugin;

    private MetaDataClient metadataClient;
    private LogbookLifeCyclesClient lfcClient;
    private StorageClient storageClient;
    private WorkspaceClient workspaceClient;
    private AdminManagementClient adminManagementClient;


    private InputStream unit;
    private RequestResponseOK<JsonNode> unitResponse;
    private JsonNode lfcResponse;

    @Before
    public void setUp() throws Exception {
        LogbookLifeCyclesClientFactory.changeMode(null);

        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        workspaceClient = mock(WorkspaceClient.class);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        metadataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metadataClient);
        storageClient = mock(StorageClient.class);
        given(storageClientFactory.getClient()).willReturn(storageClient);
        lfcClient = mock(LogbookLifeCyclesClient.class);
        given(lfcClientFactory.getClient()).willReturn(lfcClient);
        adminManagementClient = mock(AdminManagementClient.class);
        given(adminManagementClientFactory.getClient()).willReturn(adminManagementClient);
        unit = PropertiesUtils.getResourceAsStream(UNIT);
        File mdFile = PropertiesUtils.getResourceFile(METDATA_UNIT_RESPONSE_JSON);
        unitResponse = JsonHandler.getFromFileAsTypeReference(mdFile, new TypeReference<>() {
        });
        File lfcFile = PropertiesUtils.getResourceFile(LFC_UNIT_RESPONSE_JSON);
        lfcResponse = JsonHandler.getFromFile(lfcFile);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUnitListWhenMassUpdateThenReturnOK() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode query =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/MassUpdateUnitsProcess/query.json"));

        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectName(query.toString())
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.MASS_UPDATE);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        responseOK.addResult(JsonHandler.toJsonNode(new UpdateUnit("aeaqaaaaaahxpfgvab4ygalehsmdu5iaaaaq", StatusCode.OK, UpdateUnitKey.UNIT_METADATA_UPDATE, "update ok", "-    Title : monSIP 5\n+    Title : monSIP 6\n-    #version : 3\n+    #version : 4")));
        responseOK.addResult(JsonHandler.toJsonNode(new UpdateUnit("aeaqaaaaaahxpfgvab4ygalehsmdvcyaaaaq", StatusCode.OK, UpdateUnitKey.UNIT_METADATA_UPDATE, "update ok", "-    Title : monSIP 5\n+    Title : monSIP 6\n-    #version : 3\n+    #version : 4")));

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.updateUnitBulk(any())).willReturn(responseOK);
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any())).thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        ItemStatus itemStatus = revertUpdateUnitPlugin.execute(params, handlerIO);
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_a_storage_error_produce_a_fatal_item_status() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode query =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/MassUpdateUnitsProcess/query.json"));

        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectName(query.toString())
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.MASS_UPDATE);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        responseOK.addResult(JsonHandler.toJsonNode(new UpdateUnit("aeaqaaaaaahxpfgvab4ygalehsmdu5iaaaaq", StatusCode.OK, UpdateUnitKey.UNIT_METADATA_UPDATE, "update ok", "-    Title : monSIP 5\n+    Title : monSIP 6\n-    #version : 3\n+    #version : 4")));
        responseOK.addResult(JsonHandler.toJsonNode(new UpdateUnit("aeaqaaaaaahxpfgvab4ygalehsmdvcyaaaaq", StatusCode.OK, UpdateUnitKey.UNIT_METADATA_UPDATE, "update ok", "-    Title : monSIP 5\n+    Title : monSIP 6\n-    #version : 3\n+    #version : 4")));

        given(handlerIO.getWorkerId()).willReturn(workerId);
        doThrow(new ProcessingException("exception")).when(handlerIO).transferInputStreamToWorkspace(anyString(), any(), any(), anyBoolean());
        given(metadataClient.updateUnitBulk(any())).willReturn(responseOK);
        given(metadataClient.getUnitByIdRaw(any())).willReturn(unitResponse);
        given(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .willReturn(getStoredInfoResult());
        given(lfcClient.getRawUnitLifeCycleById(any())).willReturn(lfcResponse);

        // When
        ItemStatus itemStatus = revertUpdateUnitPlugin.execute(params, handlerIO);

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_save_in_mongo_lfc_already_saved() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String processId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode query =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/MassUpdateUnitsProcess/query.json"));

        WorkerParameters params = newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(processId)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectName(query.toString())
            .setProcessId(processId)
            .setLogbookTypeProcess(LogbookTypeProcess.MASS_UPDATE);

        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                "MY_ID_YEAH",
                StatusCode.OK,
                UpdateUnitKey.UNIT_METADATA_NO_CHANGES,
                "Unit updated with UNKNOWN changes.",
                "UNKNOWN diff, there are some changes but they cannot be trace."
            )
        );
        responseOK.addResult(updatedUnit);
        given(metadataClient.updateUnitBulk(any())).willReturn(responseOK);

        given(handlerIO.getWorkerId()).willReturn(processId);
        given(handlerIO.getContainerName()).willReturn(processId);
        given(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any())).willReturn(getStoredInfoResult());

        LogbookLifecycle lfc = new LogbookLifecycle();
        LogbookEvent event = new LogbookEvent();
        event.setEvIdProc(processId);
        lfc.setEvents(Collections.singletonList(event));
        given(lfcClient.getRawUnitLifeCycleById("MY_ID_YEAH")).willReturn(JsonHandler.toJsonNode(lfc));

        given(metadataClient.getUnitByIdRaw("MY_ID_YEAH")).willReturn(unitResponse);

        // When
        ItemStatus itemStatus = revertUpdateUnitPlugin.execute(params, handlerIO);

        // Then
        verify(lfcClient, never()).update(any(), any());
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    private StoredInfoResult getStoredInfoResult() {
        return new StoredInfoResult()
            .setNbCopy(1)
            .setCreationTime(LocalDateUtil.now().toString())
            .setId("id")
            .setLastAccessTime(LocalDateUtil.now().toString())
            .setLastModifiedTime(LocalDateUtil.now().toString())
            .setObjectGroupId("id")
            .setOfferIds(Collections.singletonList("id1"))
            .setStrategy(VitamConfiguration.getDefaultStrategy());
    }
}
