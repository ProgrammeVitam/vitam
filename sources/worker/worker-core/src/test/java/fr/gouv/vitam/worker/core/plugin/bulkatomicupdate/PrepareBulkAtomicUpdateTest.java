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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PrepareBulkAtomicUpdateTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private BatchReportClientFactory batchReportClientFactory;

    private static final int TENANT_ID = 0;
    private static final int DISTRIBUTION_FILE_RANK = 0;

    private PrepareBulkAtomicUpdate prepareBulkAtomicUpdate;

    @Before
    public void setUp() throws Exception {
        prepareBulkAtomicUpdate = new PrepareBulkAtomicUpdate(metaDataClientFactory, batchReportClientFactory,
            new InternalActionKeysRetriever(), 5);
    }

    @Test
    @RunWithCustomExecutor
    public void givingRequestWithValidQueriesThenGenerateUnitsListInFileOK() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_OK.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_OK.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willReturn(second);

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(16);
        assertThat(lines.get(0)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwyaaaaq\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value1\"");
        assertThat(lines.get(1)).containsOnlyOnce("{\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaaba\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value2\"");
        assertThat(lines.get(9)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaabz\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value10\"");
    }

    @Test
    @RunWithCustomExecutor
    public void givingRequestWithOneEmptyResultInQueriesThenGenerateUnitsListInFileWARNING() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_WARNING_empty.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_WARNING_empty.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willReturn(second);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(15);
        assertThat(lines.get(0)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwyaaaaq\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value1\"");
        assertThat(lines.get(1)).containsOnlyOnce("{\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaabr\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value3\"");
        assertThat(lines.get(8)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaabz\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value10\"");

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(1);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey())
            .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.name());
    }

    @Test
    @RunWithCustomExecutor
    public void givingRequestWithTooManyResultsInQueriesThenGenerateUnitsListInFileWARNING() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_WARNING_too_many.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);


        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_WARNING_too_many.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willReturn(second);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(15);
        assertThat(lines.get(0)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwyaaaaq\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value1\"");
        assertThat(lines.get(1)).containsOnlyOnce("{\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaaba\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value2\"");
        assertThat(lines.get(14)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaahz\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value16\"");

        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(1);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey())
            .isEqualTo(BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.name());
    }


    @Test
    @RunWithCustomExecutor
    public void givingRequestWithInvalidDSLInQueriesThenGenerateUnitsListInFileWARNING() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(
                getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_WARNING_invalid_query.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_WARNING_invalid_query.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 15);

        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willReturn(second);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(15);
        assertThat(lines.get(0)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwyaaaaq\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value1\"");
        assertThat(lines.get(1)).containsOnlyOnce("{\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaaba\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value2\"");
        assertThat(lines.get(8)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaabz\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value10\"");
        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);
        verify(batchReportClient).appendReportEntries(reportArgumentCaptor.capture());
        assertThat(reportArgumentCaptor.getAllValues().size()).isEqualTo(1);
        ReportBody<BulkUpdateUnitMetadataReportEntry> reportBodyArgument = reportArgumentCaptor.getValue();
        assertThat(reportBodyArgument.getEntries().size()).isEqualTo(1);
        assertThat(reportBodyArgument.getEntries().get(0).getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(reportBodyArgument.getEntries().get(0).getResultKey())
            .isEqualTo(BulkUpdateUnitReportKey.INVALID_DSL_QUERY.name());
        assertThat(reportBodyArgument.getEntries().get(0).getMessage()).contains("_opi");
    }



    @Test
    @RunWithCustomExecutor
    public void givingRequestWithValidQueriesAndVitamErrorSelectThenGenerateUnitsListInFileWARNING() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_OK.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_OK.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willThrow(InvalidParseOperationException.class);

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(8);
        assertThat(lines.get(0)).containsOnlyOnce("\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwyaaaaq\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value1\"");
        assertThat(lines.get(1)).containsOnlyOnce("{\"id\":\"aeaqaaaaaaheuluhab5yialwh7e2nwaaaaba\"")
            .containsOnlyOnce("\"ArchivalAgencyArchiveUnitIdentifier\":\"Value2\"");
    }

    @Test
    @RunWithCustomExecutor
    public void givingMetadataExceptionThenFATAL() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_OK.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        given(metaDataClient.selectUnitsBulk(any())).willThrow(MetaDataExecutionException.class);

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }



    @Test
    @RunWithCustomExecutor
    public void givingBatchReportExceptionThenFATAL() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/query_WARNING_empty.json"));
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);
        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        List<RequestResponseOK<JsonNode>> response = JsonHandler.getFromInputStreamAsTypeReference(
            getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/metadataBulkResult_WARNING_empty.json"),
            new TypeReference<List<RequestResponseOK<JsonNode>>>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        given(metaDataClient.selectUnitsBulk(any())).willReturn(first).willReturn(second);

        willThrow(VitamClientInternalException.class).given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(WorkerParametersFactory.newWorkerParameters(),
            handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

}
