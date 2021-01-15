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
package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
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
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
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
import org.mockito.ArgumentCaptor;
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
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkAtomicUpdateProcessTest {
    private static final int TENANT_ID = 0;

    private static final String CONTAINER_NAME = "aebaaaaaaaag3r7cabf4aak2izdlnwiaaaop";
    private static final String UNIT1_GUID = "aeaqaaaaaahxpfgvab4ygalehsmdu5iaaaaq";
    private static final String UNIT2_GUID = "aeaqaaaaaahxpfgvab4ygalehsmdvcyaaaaq";
    private static final String ORIGINAL_QUERY_FILE1 = "BulkAtomicUpdateProcess/originalQuery1.json";
    private static final String ORIGINAL_QUERY_FILE2 = "BulkAtomicUpdateProcess/originalQuery2.json";
    private static final String UNIT = "BulkAtomicUpdateProcess/unitMd.json";
    private static final String METADATA_UNIT_RESPONSE_JSON = "BulkAtomicUpdateProcess/unit.json";
    private static final String LFC_UNIT_RESPONSE_JSON = "BulkAtomicUpdateProcess/lfc.json";

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
    private BulkAtomicUpdateProcess bulkAtomicUpdateProcess;

    private MetaDataClient metadataClient;
    private LogbookLifeCyclesClient lfcClient;
    private StorageClient storageClient;
    private WorkspaceClient workspaceClient;
    private AdminManagementClient adminManagementClient;
    private BatchReportClient batchReportClient;


    private InputStream unit;
    private RequestResponse<JsonNode> unitResponse;
    private JsonNode lfcResponse;

    @Before
    public void setUp() throws Exception {
        LogbookLifeCyclesClientFactory.changeMode(null);

        batchReportClient = mock(BatchReportClient.class);
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
        File mdFile = PropertiesUtils.getResourceFile(METADATA_UNIT_RESPONSE_JSON);
        unitResponse = JsonHandler.getFromFile(mdFile, RequestResponseOK.class);
        File lfcFile = PropertiesUtils.getResourceFile(LFC_UNIT_RESPONSE_JSON);
        lfcResponse = JsonHandler.getFromFile(lfcFile);
    }

    @Test
    @RunWithCustomExecutor
    public void givingUnitListWhenMassUpdateThenReturnOK() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        RequestResponseOK<JsonNode> globalResponse = createMetadataResponseOK(UNIT1_GUID, UNIT2_GUID);

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        List<ItemStatus> itemStatuses = bulkAtomicUpdateProcess.executeList(params, handlerIO);
        assertThat(itemStatuses).isNotNull();
        assertThat(itemStatuses.size()).isEqualTo(2);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatuses.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(2);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey()).isEqualTo("UNIT_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(0).getUnitId()).isEqualTo(UNIT1_GUID);
        assertThat(reportBodyArgument.getEntries().get(0).getDetailId()).isEqualTo("0");
        assertThat(reportBodyArgument.getEntries().get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reportBodyArgument.getEntries().get(1).getResultKey()).isEqualTo("UNIT_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(1).getUnitId()).isEqualTo(UNIT2_GUID);
        assertThat(reportBodyArgument.getEntries().get(1).getDetailId()).isEqualTo("1");
    }

    @Test
    @RunWithCustomExecutor
    public void should_an_update_ok_contains_error_produce_a_ko_item_status() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        RequestResponseOK<JsonNode> globalResponse = createMetadataResponseOKWithErrors(UNIT1_GUID, UNIT2_GUID);

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        List<ItemStatus> itemStatuses = bulkAtomicUpdateProcess.executeList(params, handlerIO);
        assertThat(itemStatuses).isNotNull();
        assertThat(itemStatuses.size()).isEqualTo(2);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatuses.get(1).getGlobalStatus()).isEqualTo(StatusCode.KO);

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(2);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey()).isEqualTo("ERROR_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(0).getUnitId()).isEqualTo(UNIT1_GUID);
        assertThat(reportBodyArgument.getEntries().get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(reportBodyArgument.getEntries().get(1).getResultKey()).isEqualTo("ERROR_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(1).getUnitId()).isEqualTo(UNIT2_GUID);
    }

    @Test
    @RunWithCustomExecutor
    public void should_an_update_contain_a_warning_produce_an_exception() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        RequestResponseOK<JsonNode> globalResponse = createMetadataResponseOKWithWarning(UNIT1_GUID, UNIT2_GUID);

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        assertThatThrownBy(() -> bulkAtomicUpdateProcess.executeList(params, handlerIO))
            .isInstanceOf(VitamRuntimeException.class);

    }

    @Test
    @RunWithCustomExecutor
    public void should_an_update_error_produce_a_fatal_item_status() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.atomicUpdateBulk(any())).willReturn(new VitamError("ERROR"));
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        assertThatThrownBy(() -> bulkAtomicUpdateProcess.executeList(params, handlerIO))
            .isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_an_update_exception_produce_a_fatal_item_status() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        given(handlerIO.getWorkerId()).willReturn(workerId);
        given(metadataClient.atomicUpdateBulk(any())).willThrow(new MetaDataDocumentSizeException("too big"));
        when(metadataClient.getUnitByIdRaw(any())).thenReturn(unitResponse);
        when(lfcClient.getRawUnitLifeCycleById(any())).thenReturn(lfcResponse);
        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.UNIT.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Response.Status.OK).entity(unit).build());
        when(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .thenReturn(getStoredInfoResult());
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(ClientMockResultHelper.getOntologies(Response.Status.OK.getStatusCode()));

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File reportFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(any())).willReturn(reportFile);

        assertThatThrownBy(() -> bulkAtomicUpdateProcess.executeList(params, handlerIO))
            .isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_a_storage_error_produce_a_fatal_item_status() throws Exception {
        // Given
        String operationId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();
        JsonNode originalQuery1 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        JsonNode originalQuery2 =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE2));
        final WorkerParameters params =
            newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID().getId()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(UNIT1_GUID, UNIT2_GUID))
                .setObjectMetadataList(Lists.newArrayList(originalQuery1, originalQuery2))
                .setProcessId(operationId)
                .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        JsonNode query =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/MassUpdateUnitsProcess/query.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(query);
        String workerId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        RequestResponseOK<JsonNode> globalResponse = createMetadataResponseOK(UNIT1_GUID, UNIT2_GUID);

        given(handlerIO.getWorkerId()).willReturn(workerId);
        doThrow(new ProcessingException("exception")).when(handlerIO)
            .transferInputStreamToWorkspace(anyString(), any(), any(), anyBoolean());
        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);
        given(metadataClient.getUnitByIdRaw(any())).willReturn(unitResponse);
        given(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .willReturn(getStoredInfoResult());
        given(lfcClient.getRawUnitLifeCycleById(any())).willReturn(lfcResponse);

        // When
        List<ItemStatus> itemStatuses = bulkAtomicUpdateProcess.executeList(params, handlerIO);

        // Then
        assertThat(itemStatuses).isNotNull();
        assertThat(itemStatuses).hasSize(2);
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus)
            .containsOnly(StatusCode.FATAL, StatusCode.FATAL);

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(2);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey()).isEqualTo("UNIT_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(0).getUnitId()).isEqualTo(UNIT1_GUID);
        assertThat(reportBodyArgument.getEntries().get(0).getDetailId()).isEqualTo("0");
        assertThat(reportBodyArgument.getEntries().get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reportBodyArgument.getEntries().get(1).getResultKey()).isEqualTo("UNIT_METADATA_UPDATE");
        assertThat(reportBodyArgument.getEntries().get(1).getUnitId()).isEqualTo(UNIT2_GUID);
        assertThat(reportBodyArgument.getEntries().get(1).getDetailId()).isEqualTo("1");
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_save_in_mongo_lfc_already_saved() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String processId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        // New ArrayList, otherwise the originQuery parameter is skipped by Lists.newArrayList()
        JsonNode originalQuery =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        WorkerParameters params = newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(processId)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList(UNIT1_GUID))
            .setObjectMetadataList(Collections.singletonList(originalQuery))
            .setProcessId(processId)
            .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);


        RequestResponseOK<JsonNode> globalResponse = new RequestResponseOK<>();
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();

        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                UNIT1_GUID,
                StatusCode.OK,
                UpdateUnitKey.UNIT_METADATA_NO_CHANGES,
                "Unit updated with UNKNOWN changes.",
                "UNKNOWN diff, there are some changes but they cannot be trace."
            )
        );
        responseOK.addResult(JsonHandler.toJsonNode(updatedUnit));
        responseOK.setHttpCode(OK.getStatusCode());
        globalResponse.addResult(JsonHandler.toJsonNode(responseOK));
        globalResponse.setHttpCode(OK.getStatusCode());

        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);

        given(handlerIO.getWorkerId()).willReturn(processId);
        given(handlerIO.getContainerName()).willReturn(processId);
        given(storageClient.storeFileFromWorkspace(eq("other_strategy"), any(), any(), any()))
            .willReturn(getStoredInfoResult());

        LogbookLifecycle lfc = new LogbookLifecycle();
        LogbookEvent event = new LogbookEvent();
        event.setEvIdProc(processId);
        lfc.setEvents(Collections.singletonList(event));
        given(lfcClient.getRawUnitLifeCycleById(UNIT1_GUID)).willReturn(JsonHandler.toJsonNode(lfc));

        given(metadataClient.getUnitByIdRaw(UNIT1_GUID)).willReturn(unitResponse);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        // When
        List<ItemStatus> itemStatuses = bulkAtomicUpdateProcess.executeList(params, handlerIO);

        // Then
        verify(lfcClient, never()).update(any(), any());
        assertThat(itemStatuses.size()).isEqualTo(1);
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(StatusCode.OK);

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(1);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey()).isEqualTo("UNIT_METADATA_NO_CHANGES");
        assertThat(reportBodyArgument.getEntries().get(0).getUnitId()).isEqualTo(UNIT1_GUID);
        assertThat(reportBodyArgument.getEntries().get(0).getDetailId()).isEqualTo("0");
    }
    

    @Test
    @RunWithCustomExecutor
    public void should_not_save_in_mongo_and_store_lfc_when_no_change() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        String processId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

        // New ArrayList, otherwise the originQuery parameter is skipped by Lists.newArrayList()
        JsonNode originalQuery =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ORIGINAL_QUERY_FILE1));
        WorkerParameters params = newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(processId)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList(UNIT1_GUID))
            .setObjectMetadataList(Collections.singletonList(originalQuery))
            .setProcessId(processId)
            .setLogbookTypeProcess(LogbookTypeProcess.BULK_UPDATE);


        RequestResponseOK<JsonNode> globalResponse = new RequestResponseOK<>();
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();

        JsonNode updatedUnit = JsonHandler.toJsonNode(
            new UpdateUnit(
                UNIT1_GUID,
                StatusCode.OK,
                UpdateUnitKey.UNIT_METADATA_NO_NEW_DATA,
                "Unit not updated.",
                "No diff, there are no new changes."
            )
        );
        responseOK.addResult(JsonHandler.toJsonNode(updatedUnit));
        responseOK.setHttpCode(OK.getStatusCode());
        globalResponse.addResult(JsonHandler.toJsonNode(responseOK));
        globalResponse.setHttpCode(OK.getStatusCode());

        given(metadataClient.atomicUpdateBulk(any())).willReturn(globalResponse);

        given(handlerIO.getWorkerId()).willReturn(processId);
        given(handlerIO.getContainerName()).willReturn(processId);

        given(metadataClient.getUnitByIdRaw(UNIT1_GUID)).willReturn(unitResponse);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        // When
        List<ItemStatus> itemStatuses = bulkAtomicUpdateProcess.executeList(params, handlerIO);

        // Then
        verify(lfcClient, never()).update(any(), any());
        verify(lfcClient, never()).getRawUnitLifeCycleById(any());
        verify(storageClient, never()).storeFileFromWorkspace(eq("other_strategy"), any(), any(), any());
        assertThat(itemStatuses.size()).isEqualTo(1);
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(StatusCode.WARNING);

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(1);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey()).isEqualTo("UNIT_METADATA_NO_NEW_DATA");
        assertThat(reportBodyArgument.getEntries().get(0).getUnitId()).isEqualTo(UNIT1_GUID);
        assertThat(reportBodyArgument.getEntries().get(0).getDetailId()).isEqualTo("0");
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

    private RequestResponseOK<JsonNode> createMetadataResponseOK(String... unitIds)
        throws InvalidParseOperationException {
        RequestResponseOK<JsonNode> globalResponse = new RequestResponseOK<>();
        for (String unitId : unitIds) {
            RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
            responseOK.addResult(JsonHandler.toJsonNode(
                new UpdateUnit(unitId, StatusCode.OK, UpdateUnitKey.UNIT_METADATA_UPDATE, "update ok",
                    "-    Title : monSIP 5\n+    Title : nouveauSIP 6\n-    #version : 3\n+    #version : 4")));
            responseOK.setHttpCode(OK.getStatusCode());

            globalResponse.addResult(JsonHandler.toJsonNode(responseOK));
        }
        globalResponse.setHttpCode(OK.getStatusCode());
        return globalResponse;
    }

    private RequestResponseOK<JsonNode> createMetadataResponseOKWithWarning(String... unitIds)
        throws InvalidParseOperationException {
        RequestResponseOK<JsonNode> globalResponse = new RequestResponseOK<>();
        for (String unitId : unitIds) {
            RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
            responseOK.addResult(JsonHandler.toJsonNode(
                new UpdateUnit(unitId, StatusCode.WARNING, UpdateUnitKey.UNIT_METADATA_UPDATE, "update warning",
                    "-    Title : monSIP 5\n+    Title : nouveauSIP 6\n-    #version : 3\n+    #version : 4")));
            responseOK.setHttpCode(OK.getStatusCode());

            globalResponse.addResult(JsonHandler.toJsonNode(responseOK));
        }
        globalResponse.setHttpCode(OK.getStatusCode());
        return globalResponse;
    }

    private RequestResponseOK<JsonNode> createMetadataResponseOKWithErrors(String... unitIds)
        throws InvalidParseOperationException {
        RequestResponseOK<JsonNode> globalResponse = new RequestResponseOK<>();
        for (String unitId : unitIds) {
            VitamError vitamError = new VitamError("BAD_REQUEST")
                .setHttpCode(400)
                .setContext("ACCESS")
                .setDescription("Bag idea description")
                .setMessage("Bad idea " + unitId)
                .setState("code_vitam");
            globalResponse.addResult(JsonHandler.toJsonNode(vitamError));
        }
        globalResponse.setHttpCode(OK.getStatusCode());
        return globalResponse;
    }

}
