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
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
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
import jakarta.annotation.Nonnull;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.collect.CollectTestHelper.createTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CollectReclassificationIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectReclassificationIT.class);

    private static final String XML = ".xml";
    private static final String ZIP_EXAMPLE_FILE = "collect/collect-example.zip";
    private static String prefix;

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

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
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class,
            CollectExternalMain.class,
            CollectInternalMain.class
        )
    );

    private static final Integer TENANT_ID = 0;

    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @Test
    @RunWithCustomExecutor
    public void should_perform_move_reclassification_operation() throws Exception {
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
            JsonNode unitsByTransaction = collectClient
                .getUnitsByTransaction(vitamContext, transactionDto.getId(), new SelectMultiQuery().getFinalSelect())
                .toJsonNode();

            JsonNode selectedUnits = unitsByTransaction.get(TAG_RESULTS);

            // Reclassification action
            final String collectDeletionOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectDeletionOperationGuid);

            UpdateMultiQuery reclassificationRequest = new UpdateMultiQuery();
            reclassificationRequest.setQuery(
                QueryHelper.eq(VitamFieldsHelper.id(), getUnitId(getUnitIdByTitle(selectedUnits, "file1.txt")))
            );

            JsonNode newParentUnit = getUnitIdByTitle(selectedUnits, "folder2");
            JsonNode oldParentUnit = getUnitIdByTitle(selectedUnits, "folder1");

            reclassificationRequest.addActions(
                UpdateActionHelper.add(VitamFieldsHelper.unitups(), getUnitId(newParentUnit)),
                UpdateActionHelper.pull(VitamFieldsHelper.unitups(), getUnitId(oldParentUnit))
            );

            ObjectNode finalUpdate = reclassificationRequest.getFinalUpdate();

            finalUpdate.remove("$filter");
            JsonNode reclassificationQuery = JsonHandler.createArrayNode().add(finalUpdate);

            final RequestResponse<JsonNode> actionResult = collectClient.performReclassificationOnTransaction(
                vitamContext,
                transactionDto.getId(),
                reclassificationQuery
            );

            assertThat(actionResult.isOk()).isTrue();
            String reclassificationOperationGuid =
                ((ObjectNode) ((RequestResponseOK) actionResult).getResults().get(0)).get("itemId").asText();

            awaitForWorkflowTerminationWithStatus(reclassificationOperationGuid, StatusCode.OK);
            TimeUnit.SECONDS.sleep(1); // wait until operation is finished

            final RequestResponseOK<JsonNode> updatedUnitsResp = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDto.getId(),
                collectClient
            );

            assertThat(updatedUnitsResp.isOk()).isTrue();
            JsonNode updatedUnits = updatedUnitsResp.toJsonNode().get(TAG_RESULTS);

            JsonNode unitUpdated = getUnitIdByTitle(updatedUnits, "file1.txt");
            String newParentId = getUnitId(newParentUnit);
            String oldParentId = getUnitId(oldParentUnit);
            List<String> updatedUnitParents = getUnitParents(unitUpdated);
            assertThat(updatedUnitParents).contains(newParentId);
            assertThat(updatedUnitParents).doesNotContain(oldParentId);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_perform_add_reclassification_operation() throws Exception {
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
            JsonNode unitsByTransaction = collectClient
                .getUnitsByTransaction(vitamContext, transactionDto.getId(), new SelectMultiQuery().getFinalSelect())
                .toJsonNode();

            JsonNode selectedUnits = unitsByTransaction.get(TAG_RESULTS);

            // Reclassification action
            final String collectDeletionOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectDeletionOperationGuid);

            UpdateMultiQuery reclassificationRequest = new UpdateMultiQuery();
            reclassificationRequest.setQuery(
                QueryHelper.eq(VitamFieldsHelper.id(), getUnitId(getUnitIdByTitle(selectedUnits, "file1.txt")))
            );

            JsonNode newParentUnit = getUnitIdByTitle(selectedUnits, "folder2");
            JsonNode oldParentUnit = getUnitIdByTitle(selectedUnits, "folder1");

            reclassificationRequest.addActions(
                UpdateActionHelper.add(VitamFieldsHelper.unitups(), getUnitId(newParentUnit))
            );

            ObjectNode finalUpdate = reclassificationRequest.getFinalUpdate();

            finalUpdate.remove("$filter");
            JsonNode reclassificationQuery = JsonHandler.createArrayNode().add(finalUpdate);

            final RequestResponse<JsonNode> actionResult = collectClient.performReclassificationOnTransaction(
                vitamContext,
                transactionDto.getId(),
                reclassificationQuery
            );

            assertThat(actionResult.isOk()).isTrue();
            String reclassificationOperationGuid =
                ((ObjectNode) ((RequestResponseOK) actionResult).getResults().get(0)).get("itemId").asText();

            awaitForWorkflowTerminationWithStatus(reclassificationOperationGuid, StatusCode.OK);
            TimeUnit.SECONDS.sleep(1); // wait until operation is finished

            final RequestResponseOK<JsonNode> updatedUnitsResp = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDto.getId(),
                collectClient
            );

            assertThat(updatedUnitsResp.isOk()).isTrue();
            JsonNode updatedUnits = updatedUnitsResp.toJsonNode().get(TAG_RESULTS);

            JsonNode unitUpdated = getUnitIdByTitle(updatedUnits, "file1.txt");
            String newParentId = getUnitId(newParentUnit);
            String oldParentId = getUnitId(oldParentUnit);
            List<String> updatedUnitParents = getUnitParents(unitUpdated);
            assertThat(updatedUnitParents).contains(newParentId);
            assertThat(updatedUnitParents).contains(oldParentId);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_reclassification_on_closed_transaction() throws Exception {
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
            final String transactionId = transactionDto.getId();

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

            JsonNode unitsByTransaction = collectClient
                .getUnitsByTransaction(vitamContext, transactionId, new SelectMultiQuery().getFinalSelect())
                .toJsonNode();

            JsonNode selectedUnits = unitsByTransaction.get(TAG_RESULTS);

            // Close the transaction
            collectClient.closeTransaction(vitamContext, transactionId);

            // Try to perform a reclassification action on the closed transaction
            final String collectReclassificationOperationGuid = newOperationLogbookGUID(TENANT_ID).toString();
            VitamThreadUtils.getVitamSession().setRequestId(collectReclassificationOperationGuid);

            UpdateMultiQuery reclassificationRequest = new UpdateMultiQuery();
            reclassificationRequest.setQuery(
                QueryHelper.eq(VitamFieldsHelper.id(), getUnitId(getUnitIdByTitle(selectedUnits, "file1.txt")))
            );

            JsonNode newParentUnit = getUnitIdByTitle(selectedUnits, "folder2");

            reclassificationRequest.addActions(
                UpdateActionHelper.add(VitamFieldsHelper.unitups(), getUnitId(newParentUnit))
            );

            ObjectNode finalUpdate = reclassificationRequest.getFinalUpdate();
            finalUpdate.remove("$filter");
            JsonNode reclassificationQuery = JsonHandler.createArrayNode().add(finalUpdate);

            // When - Try to perform reclassification action on closed transaction
            // This should fail because the transaction is not open
            try {
                collectClient.performReclassificationOnTransaction(vitamContext, transactionId, reclassificationQuery);
                Assertions.fail("Should have thrown an exception");
            } catch (Exception e) {
                // Then - Verify that the operation failed with the expected error
                assertThat(e.getLocalizedMessage()).contains("must be OPEN but was READY");
            }
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

    @Nonnull
    private JsonNode getUnitIdByTitle(JsonNode units, String title) {
        Optional<JsonNode> unit = StreamSupport.stream(units.spliterator(), false)
            .filter(u -> u.get("Title").asText().equals(title))
            .findFirst();
        if (unit.isPresent()) {
            return unit.get();
        } else {
            throw new RuntimeException("Unit not found");
        }
    }

    private String getUnitId(JsonNode unit) {
        return unit.get(VitamFieldsHelper.id()).asText();
    }

    private List<String> getUnitParents(JsonNode unit) {
        return StreamSupport.stream(unit.get(VitamFieldsHelper.unitups()).spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toList());
    }
}
