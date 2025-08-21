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

package fr.gouv.vitam.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.DeletionRequestBody;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.collect.CollectTestHelper.createTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CollectDeletionIT extends VitamRuleRunner {

    /**
     * Test that verifies the transaction must be in OPEN status for deletion workflow
     */

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectDeletionIT.class);

    private static final String XML = ".xml";
    private static final String ZIP_EXAMPLE_FILE = "collect/collect-example.zip";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String WORKSPACE_COLLECT_PATH = "/workspace-collect/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BATCH_REPORT_PATH = "/batchreport/v1";

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    private static String prefix;

    @BeforeClass
    public static void setUp() throws Exception {
        prefix = MetadataCollections.UNIT.getPrefix();
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startWorkspaceServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        MetadataCollections.UNIT.setPrefix(prefix);
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopWorkspaceServer();
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE_COLLECT;
        RestAssured.basePath = WORKSPACE_COLLECT_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_BATCH_REPORT;
        RestAssured.basePath = BATCH_REPORT_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        FluxIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            AdminManagementMain.class,
            LogbookMain.class,
            AccessInternalMain.class,
            AccessExternalMain.class,
            WorkspaceMain.class,
            CollectInternalMain.class,
            CollectExternalMain.class,
            MetadataMain.class,
            WorkerMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    private static final Integer TENANT_ID = 0;

    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @Test
    @RunWithCustomExecutor
    public void should_perform_deletion_operation() throws Exception {
        // GIVEN
        prepareVitamSession();
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = initProjectData();
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );
            projectDto.setId(projectDtoResult.getId());

            final TransactionDto transactionDto = createTransaction(vitamContext, projectDto.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_EXAMPLE_FILE)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            Assertions.assertThat(unitsByTransaction.getStatus()).isEqualTo(200);
            assertThat(unitsByTransaction.getResults()).hasSize(7);

            // deletion action
            final String collectDeletionOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectDeletionOperationGuid);

            SelectMultiQuery deleteQueryQuery = new SelectMultiQuery();
            ObjectNode finalQuery = deleteQueryQuery.getFinalSelect();
            finalQuery.remove(List.of("$facets", "$projection", "$filter", "$threshold"));
            DeletionRequestBody deletionRequestBody = new DeletionRequestBody(finalQuery);

            final RequestResponse<JsonNode> actionResult = collectClient.performDeletionActionOnTransaction(
                vitamContext,
                transactionDto.getId(),
                deletionRequestBody
            );

            assertThat(actionResult.isOk()).isTrue();
            String deletionOperationGuid =
                ((ObjectNode) ((RequestResponseOK) actionResult).getResults().get(0)).get("itemId").asText();

            awaitForWorkflowTerminationWithStatus(deletionOperationGuid, StatusCode.OK);
            TimeUnit.SECONDS.sleep(1); // wait until deletion is finished

            final RequestResponseOK<JsonNode> remainingUnits = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDto.getId(),
                collectClient
            );
            assertThat(remainingUnits.isOk()).isTrue();
            assertThat(remainingUnits.getResults()).hasSize(0);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_deletion_when_transaction_not_open() throws Exception {
        // GIVEN
        prepareVitamSession();
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            // Create a project
            final ProjectDto projectDto = initProjectData();
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );
            projectDto.setId(projectDtoResult.getId());

            // Create a transaction
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDto.getId();

            // Upload a ZIP file to the transaction
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_EXAMPLE_FILE)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionId,
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            // Close the transaction
            collectClient.closeTransaction(vitamContext, transactionId);

            // Try to perform a deletion action on the closed transaction
            final String collectDeletionOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectDeletionOperationGuid);

            SelectMultiQuery deleteQueryQuery = new SelectMultiQuery();
            ObjectNode finalQuery = deleteQueryQuery.getFinalSelect();
            finalQuery.remove(List.of("$facets", "$projection", "$filter", "$threshold"));
            DeletionRequestBody deletionRequestBody = new DeletionRequestBody(finalQuery);

            // When - Try to perform deletion action on closed transaction
            // This should fail because the transaction is not open
            try {
                collectClient.performDeletionActionOnTransaction(vitamContext, transactionId, deletionRequestBody);
                Assertions.fail("Should have thrown an exception");
            } catch (Exception e) {
                // Then - Verify that the operation failed with the expected error
                assertThat(e.getLocalizedMessage()).contains("must be OPEN but was READY");
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_perform_partial_deletion_operation() throws Exception {
        // GIVEN
        prepareVitamSession();
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = initProjectData();
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );
            projectDto.setId(projectDtoResult.getId());

            final TransactionDto transactionDto = createTransaction(vitamContext, projectDto.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_EXAMPLE_FILE)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            Assertions.assertThat(unitsByTransaction.getStatus()).isEqualTo(200);
            assertThat(unitsByTransaction.getResults()).hasSize(7);

            // deletion action
            final String collectDeletionOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectDeletionOperationGuid);

            SelectMultiQuery deleteQueryQuery = new SelectMultiQuery();
            deleteQueryQuery.addQueries(QueryHelper.and().add(QueryHelper.in("Title", "file3.txt", "file1.txt")));
            ObjectNode finalQuery = deleteQueryQuery.getFinalSelect();
            finalQuery.remove(List.of("$facets", "$projection", "$filter", "$threshold"));
            DeletionRequestBody deletionRequestBody = new DeletionRequestBody(finalQuery);

            final RequestResponse<JsonNode> actionResult = collectClient.performDeletionActionOnTransaction(
                vitamContext,
                transactionDto.getId(),
                deletionRequestBody
            );

            assertThat(actionResult.isOk()).isTrue();
            String deletionOperationGuid =
                ((ObjectNode) ((RequestResponseOK) actionResult).getResults().get(0)).get("itemId").asText();

            awaitForWorkflowTerminationWithStatus(deletionOperationGuid, StatusCode.OK);
            TimeUnit.SECONDS.sleep(1); // wait until deletion is finished

            final RequestResponseOK<JsonNode> remainingUnits = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDto.getId(),
                collectClient
            );
            assertThat(remainingUnits.isOk()).isTrue();
            assertThat(remainingUnits.getResults()).hasSize(1);
        }
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {
        waitOperation(operationGuid);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationGuid, TENANT_ID);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {} catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    filename,
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );

                assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                return new VitamAsyncInputStream(reportResponse);
            } catch (
                RuntimeException
                | StorageServerClientException
                | StorageNotFoundException
                | StorageUnavailableDataFromAsyncOfferClientException e
            ) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }
}
