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
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.antivirus.rest.AntivirusMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.collect.CollectTestHelper.createTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.logbook.LogbookOperation.EVENTS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class CollectSipIngestIT extends AbstractCollectIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectSipIngestIT.class);

    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";

    private static final String INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP = "integration-processing/4_UNITS_2_GOTS.zip";
    private static final String INTEGRATION_PROCESSING_1_UNIT_1_GOTS_1MB_FOLDER_ZIP =
        "integration-processing/1_UNIT_1_GOTS_1MB_FOLDER.zip";

    private static String prefix;

    @Before
    public void setUp() throws Exception {
        prefix = MetadataCollections.UNIT.getPrefix();
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startWorkspaceServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    @After
    public void tearDown() throws Exception {
        MetadataCollections.UNIT.setPrefix(prefix);
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopWorkspaceServer();
        runner.stopMetadataServer(true);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        switchToVitamMetadataHack(runner);
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
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
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            AccessExternalMain.class,
            IngestExternalMain.class,
            AntivirusMain.class,
            CollectInternalMain.class,
            CollectExternalMain.class,
            BatchReportMain.class
        )
    );

    @Test
    @RunWithCustomExecutor
    public void should_perform_collect_ingest_sip_operation() throws Exception {
        InputStream sipInputStream = performCollectIngestSipOperation(INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP);

        String processId;
        switchToVitamMetadataHack(runner);
        prepareVitamSession();
        processId = ingestToVitam(sipInputStream);
        List<JsonNode> results = selectVitamMetadataUnitsByOpi(processId);
        assertEquals(4, results.size());
        switchToCollectMetadataHack(runner);

        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                processId,
                new SelectMultiQuery().getFinalSelect()
            );

            assertEquals(0, unitsByTransaction.getResults().size());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_perform_collect_ingest_sip_withFolder_operation() throws Exception {
        InputStream sipInputStream = performCollectIngestSipOperation(
            INTEGRATION_PROCESSING_1_UNIT_1_GOTS_1MB_FOLDER_ZIP
        );

        String processId;
        switchToVitamMetadataHack(runner);
        prepareVitamSession();
        processId = ingestToVitam(sipInputStream);
        List<JsonNode> results = selectVitamMetadataUnitsByOpi(processId);
        assertEquals(1, results.size());
        switchToCollectMetadataHack(runner);

        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                processId,
                new SelectMultiQuery().getFinalSelect()
            );

            assertEquals(0, unitsByTransaction.getResults().size());
        }
    }

    private InputStream performCollectIngestSipOperation(String sipFilePath) throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(
                vitamContext,
                projectDto.getId()
            ).orElseThrow();
            String transactionId = transactionDtoCreated.getId();

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(sipFilePath)) {
                RequestResponse<Void> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
            }

            waitOperation(transactionId);

            RequestResponse<JsonNode> updatedTransactionResponse = collectClient.getTransactionById(
                new VitamContext(TENANT_ID),
                transactionId
            );
            TransactionDto updatedTransaction = JsonHandler.getFromJsonNode(
                (((RequestResponseOK<JsonNode>) updatedTransactionResponse).getFirstResult()),
                TransactionDto.class
            );
            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbookResult = logbookClient.selectOperationById(transactionDtoCreated.getId());
            assertThat(logbookResult.get(TAG_RESULTS)).isNotNull();
            assertThat(logbookResult.get(TAG_RESULTS).size()).isGreaterThan(0);

            JsonNode firstResult = logbookResult.get(TAG_RESULTS).get(0);
            JsonNode events = firstResult.get(EVENTS);
            assertThat(events).isNotNull();

            assertThat(events.get(events.size() - 1).get("outcome").asText()).isEqualTo("OK");

            final RequestResponseOK<JsonNode> updatedUnitsResp = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDtoCreated.getId(),
                collectClient
            );
            assertThat(updatedUnitsResp.getResults()).isNotNull();
            assertThat(updatedUnitsResp.getResults().size()).isGreaterThan(0);

            // Find a unit with an object
            Optional<JsonNode> unitWithObjectOpt = updatedUnitsResp
                .getResults()
                .stream()
                .filter(unit -> unit.has("#object") && !unit.get("#object").isNull())
                .findFirst();

            // Get unit ID - either from unit with object or from first result
            String unitId = unitWithObjectOpt
                .map(unit -> unit.get("#id").asText())
                .orElseGet(() -> {
                    List<JsonNode> results = updatedUnitsResp.getResults();
                    if (!results.isEmpty()) {
                        String id = results.get(0).get("#id").asText();
                        assertThat(id).isNotNull().isNotEmpty();
                        return id;
                    }
                    return null;
                });

            // Get object ID from unit with object
            String objectId = unitWithObjectOpt.map(unit -> unit.get("#object").asText()).orElse(null);

            assertThat(objectId).isNotNull().isNotEmpty();
            assertThat(unitId).isNotNull().isNotEmpty();

            collectClient.closeTransaction(
                new VitamContext(TENANT_ID)
                    .setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT),
                transactionId
            );

            InputStream sipInputStream = generateSip(transactionId);
            assertThat(sipInputStream).isNotNull();

            JsonNode objectGroup =
                ((RequestResponseOK<JsonNode>) collectClient.getObjectById(vitamContext, objectId)).getFirstResult();
            assertThat(objectGroup).isNotNull();

            try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
                assertThatThrownBy(
                    () -> client.selectUnitLifeCycleById(unitId, new Select().getFinalSelectById())
                ).isInstanceOf(LogbookClientNotFoundException.class);
                assertThatThrownBy(
                    () -> client.selectObjectGroupLifeCycleById(objectId, new Select().getFinalSelectById())
                ).isInstanceOf(LogbookClientNotFoundException.class);
            }
            return sipInputStream;
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_mark_transaction_ko_when_ingest_sip_with_errors() throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(
                vitamContext,
                projectDto.getId()
            ).orElseThrow();
            String transactionId = transactionDtoCreated.getId();

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/SIP_KO_InvalidManifest.zip")) {
                RequestResponse<Void> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
            }
            waitOperation(transactionId);
            verifyOperation(transactionId, StatusCode.KO);

            RequestResponse<JsonNode> updatedTransactionResponse = collectClient.getTransactionById(
                new VitamContext(TENANT_ID),
                transactionId
            );

            TransactionDto updatedTransaction = JsonHandler.getFromJsonNode(
                (((RequestResponseOK<JsonNode>) updatedTransactionResponse).getFirstResult()),
                TransactionDto.class
            );

            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.KO.name());
        }
    }
}
