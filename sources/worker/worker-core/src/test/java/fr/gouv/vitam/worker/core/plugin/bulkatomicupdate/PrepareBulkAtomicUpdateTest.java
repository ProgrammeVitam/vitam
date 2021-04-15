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
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PrepareBulkAtomicUpdateTest {

    private static final int TENANT_ID = 0;
    private static final int DISTRIBUTION_FILE_RANK = 0;
    private static final int BATCH_SIZE = 8;
    private static final int THREAD_POOL_SIZE = 8;
    private static final int THREAD_POOL_QUEUE_SIZE = 16;

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

    private PrepareBulkAtomicUpdate prepareBulkAtomicUpdate;

    @Before
    public void setUp() throws Exception {
        prepareBulkAtomicUpdate = new PrepareBulkAtomicUpdate(
            metaDataClientFactory, batchReportClientFactory,
            new InternalActionKeysRetriever(), BATCH_SIZE, THREAD_POOL_SIZE, THREAD_POOL_QUEUE_SIZE);
    }

    @After
    public void cleanup() {
        // Restore default batch size
        VitamConfiguration.setBatchSize(1000);
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
            new TypeReference<>() {
            });
        List<RequestResponseOK<JsonNode>> firstBulkResponse = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> secondBulkResponse = response.subList(8, 16);
        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            if (selectQueryBulk.get(0).toString().contains("\"Value1\"")) {
                return firstBulkResponse;
            } else if (selectQueryBulk.get(0).toString().contains("\"Value9\"")) {
                return secondBulkResponse;
            } else {
                throw new IllegalStateException("Unexpected queries " + selectQueryBulk);
            }
        }).when(metaDataClient).selectUnitsBulk(any());

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


        verifyDistributionFile(handlerIO, distributionFile, "/PrepareBulkAtomicUpdate/distributionResult_OK.jsonl");
        verify(batchReportClient, never()).appendReportEntries(any());
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
            new TypeReference<>() {
            });
        List<RequestResponseOK<JsonNode>> firstBulkResponse = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> secondBulkResponse = response.subList(8, 16);
        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            if (selectQueryBulk.get(0).toString().contains("\"Value1\"")) {
                return firstBulkResponse;
            } else if (selectQueryBulk.get(0).toString().contains("\"Value9\"")) {
                return secondBulkResponse;
            } else {
                throw new IllegalStateException("Unexpected queries " + selectQueryBulk);
            }
        }).when(metaDataClient).selectUnitsBulk(any());

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

        verifyDistributionFile(handlerIO, distributionFile,
            "/PrepareBulkAtomicUpdate/distributionResult_WARNING_empty.jsonl");

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
            new TypeReference<>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            if (selectQueryBulk.get(0).toString().contains("\"Value1\"")) {
                return first;
            } else if (selectQueryBulk.get(0).toString().contains("\"Value9\"")) {
                return second;
            } else {
                throw new IllegalStateException("Unexpected queries " + selectQueryBulk);
            }
        }).when(metaDataClient).selectUnitsBulk(any());

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

        verifyDistributionFile(handlerIO, distributionFile,
            "/PrepareBulkAtomicUpdate/distributionResult_WARNING_too_many.jsonl");

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

    private void verifyDistributionFile(HandlerIO handlerIO, File distributionFile,
        String expectedDistributionFile) throws IOException, ProcessingException {

        verify(handlerIO).transferFileToWorkspace(anyString(), eq(distributionFile), eq(true), eq(false));

        List<String> actualLines = Files.readAllLines(Paths.get(distributionFile.toURI()), StandardCharsets.UTF_8);
        List<String> expectedLines =
            IOUtils.readLines(getClass().getResourceAsStream(expectedDistributionFile), StandardCharsets.UTF_8);

        List<JsonLineModel> parsedActualLines = parseAndSort(actualLines);
        List<JsonLineModel> parsedExpectedLines = parseAndSort(expectedLines);

        JsonAssert.assertJsonEquals(parsedExpectedLines, parsedActualLines);
    }

    private List<JsonLineModel> parseAndSort(List<String> actualLines) {
        return actualLines.stream()
            .map(line -> {
                try {
                    return JsonHandler.getFromString(line, JsonLineModel.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .sorted(Comparator.comparingInt(jsonLineModel -> jsonLineModel.getParams().get("queryIndex").asInt()))
            .collect(Collectors.toList());
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
            new TypeReference<>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            if (selectQueryBulk.get(0).toString().contains("\"Value1\"")) {
                countDownLatch.countDown();
                return first;
            } else if (selectQueryBulk.get(0).toString().contains("\"Value9\"")) {
                // Ensure first query bulk already started processing (otherwise, may be cancelled)
                countDownLatch.await();
                throw new InvalidParseOperationException("");
            } else {
                throw new IllegalStateException("Unexpected queries " + selectQueryBulk);
            }
        }).when(metaDataClient).selectUnitsBulk(any());
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
            new TypeReference<>() {
            });
        List<RequestResponseOK<JsonNode>> first = response.subList(0, 8);
        List<RequestResponseOK<JsonNode>> second = response.subList(8, 16);
        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            if (selectQueryBulk.get(0).toString().contains("\"Value1\"")) {
                return first;
            } else if (selectQueryBulk.get(0).toString().contains("\"Value9\"")) {
                return second;
            } else {
                throw new IllegalStateException("Unexpected queries " + selectQueryBulk);
            }
        }).when(metaDataClient).selectUnitsBulk(any());

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

    @Test
    @RunWithCustomExecutor
    public void givingComplexRequestWithMultipleOKAndWarningQueriesThenWarning() throws Exception {
        // given
        int nbQueries = 5000;
        int batchReportBatchSize = 43;
        VitamConfiguration.setBatchSize(batchReportBatchSize);

        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        String processId = GUIDFactory.newGUID().getId();
        params.setProcessId(processId);

        HandlerIO handlerIO = mock(HandlerIO.class);

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        BatchReportClient batchReportClient = mock(BatchReportClient.class);
        given(batchReportClientFactory.getClient()).willReturn(batchReportClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArrayNode queries = JsonHandler.createArrayNode();
        for (int i = 0; i < nbQueries; i++) {
            UpdateMultiQuery update = new UpdateMultiQuery();
            update.addQueries(
                QueryHelper.eq("ArchivalAgencyArchiveUnitIdentifier", "Value" + i)
            );
            update.addActions(
                new SetAction("Title", "nouvelle valeur Title " + i)
                    .add("Description", "nouvelle valeur Description " + i));
            queries.add(update.getFinalUpdate());
        }

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(
            JsonHandler.createObjectNode()
                .set("queries", queries)
        );

        JsonNode accessContract = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/PrepareBulkAtomicUpdate/accessContract.json"));
        given(handlerIO.getJsonFromWorkspace("accessContract.json")).willReturn(accessContract);

        doAnswer(args -> {
            List<JsonNode> selectQueryBulk = args.getArgument(0);
            List<RequestResponseOK<JsonNode>> results = new ArrayList<>();
            for (JsonNode query : selectQueryBulk) {
                int queryN = Integer.parseInt(StringUtils.substringBetween(query.toString(), "\"Value", "\""));
                switch (queryN % 10) {
                    case 8:
                        // Empty response
                        results.add(new RequestResponseOK<>());
                        break;
                    case 9:
                        // Multiple results
                        results.add(new RequestResponseOK<JsonNode>()
                            .addResult(JsonHandler.createObjectNode()
                                .put(VitamFieldsHelper.id(), GUIDFactory.newGUID().toString()))
                            .addResult(JsonHandler.createObjectNode()
                                .put(VitamFieldsHelper.id(), GUIDFactory.newGUID().toString())));
                        break;
                    default:
                        results.add(new RequestResponseOK<JsonNode>()
                            .addResult(JsonHandler.createObjectNode()
                                .put(VitamFieldsHelper.id(), GUIDFactory.newGUID().toString())));
                        break;
                }
            }
            return results;
        }).when(metaDataClient).selectUnitsBulk(any());

        willDoNothing().given(batchReportClient).appendReportEntries(any());

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath()));
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareBulkAtomicUpdate.execute(params, handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        Set<Integer> expectedOkLines = IntStream.range(0, nbQueries)
            // Filter empty responses
            .filter(i -> i % 10 != 8)
            // Filter multiple result responses
            .filter(i -> i % 10 != 9)
            .boxed()
            .collect(Collectors.toSet());
        Set<Integer> expectedLinesWithEmptyResultSet = IntStream.range(0, nbQueries)
            .filter(i -> i % 10 == 8)
            .boxed()
            .collect(Collectors.toSet());
        Set<Integer> expectedLinesWithMultipleResultSet = IntStream.range(0, nbQueries)
            .filter(i -> i % 10 == 9)
            .boxed()
            .collect(Collectors.toSet());

        assertThat(itemStatus.getStatusMeter().get(StatusCode.OK.getStatusLevel())).
            isEqualTo(expectedOkLines.size());
        assertThat(itemStatus.getStatusMeter().get(StatusCode.WARNING.getStatusLevel())).
            isEqualTo(expectedLinesWithEmptyResultSet.size() + expectedLinesWithMultipleResultSet.size());

        int nbBatchesToMetadata = (int) Math.ceil(nbQueries * 1.0 / BATCH_SIZE);
        verify(metaDataClient, times(nbBatchesToMetadata)).selectUnitsBulk(any());

        // Check distribution file
        verify(handlerIO).transferFileToWorkspace(anyString(), eq(distributionFile), eq(true), eq(false));
        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        Map<Integer, JsonLineModel> reportLineByQueryIndex = lines.stream()
            .map(line -> {
                try {
                    return JsonHandler.getFromString(line, JsonLineModel.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toMap(
                jsonLineModel -> jsonLineModel.getParams().get("queryIndex").asInt(),
                jsonLineModel -> jsonLineModel
            ));
        assertThat(reportLineByQueryIndex.keySet()).containsExactlyElementsOf(expectedOkLines);
        for (Integer lineIndex : reportLineByQueryIndex.keySet()) {
            assertThat(reportLineByQueryIndex.get(lineIndex).getId()).isNotNull();
            assertThat(reportLineByQueryIndex.get(lineIndex).getDistribGroup()).isNull();
            assertThat(reportLineByQueryIndex.get(lineIndex).getParams().get("originQuery").toString())
                .contains("Value" + lineIndex);
        }

        // Check report entries
        ArgumentCaptor<ReportBody<BulkUpdateUnitMetadataReportEntry>> reportArgumentCaptor =
            ArgumentCaptor.forClass(ReportBody.class);

        int expectedReportEntries = expectedLinesWithEmptyResultSet.size() + expectedLinesWithMultipleResultSet.size();
        int expectedReportBatches = (int) Math.ceil(expectedReportEntries * 1.0 / batchReportBatchSize);

        verify(batchReportClient, times(expectedReportBatches))
            .appendReportEntries(reportArgumentCaptor.capture());

        Map<Integer, BulkUpdateUnitMetadataReportEntry> reportEntriesByQueryIndex =
            reportArgumentCaptor.getAllValues().stream()
                .flatMap(value -> value.getEntries().stream())
                .collect(Collectors.toMap(
                    entry -> Integer
                        .parseInt(StringUtils.substringBetween(entry.getQuery(), "\"nouvelle valeur Title ", "\"")),
                    entry -> entry));

        assertThat(reportEntriesByQueryIndex.keySet()).containsExactlyInAnyOrderElementsOf(
            SetUtils.union(expectedLinesWithEmptyResultSet, expectedLinesWithMultipleResultSet));

        for (Integer queryIndex : expectedLinesWithEmptyResultSet) {
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getStatus()).isEqualTo(StatusCode.WARNING);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getTenantId()).isEqualTo(TENANT_ID);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getProcessId()).isEqualTo(processId);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getUnitId()).isNull();
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getResultKey())
                .isEqualTo(BulkUpdateUnitReportKey.UNIT_NOT_FOUND.name());
        }

        for (Integer queryIndex : expectedLinesWithMultipleResultSet) {
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getStatus()).isEqualTo(StatusCode.WARNING);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getTenantId()).isEqualTo(TENANT_ID);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getProcessId()).isEqualTo(processId);
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getUnitId()).isNull();
            assertThat(reportEntriesByQueryIndex.get(queryIndex).getResultKey())
                .isEqualTo(BulkUpdateUnitReportKey.TOO_MANY_UNITS_FOUND.name());
        }
    }
}
