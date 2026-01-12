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
import fr.gouv.vitam.collect.common.dto.BatchDto;
import fr.gouv.vitam.collect.common.dto.BatchStatusDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.dto.UploadSipResult;
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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.validations.ValidationError;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
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
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceCollectClientFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import static org.junit.Assert.assertNotNull;

public class CollectSipIngestIT extends AbstractCollectIT {

    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";

    private static final String INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP = "integration-processing/4_UNITS_2_GOTS.zip";
    private static final String INTEGRATION_PROCESSING_SIP2_ZIP = "integration-processing/12_UNITS_12_GOTS.zip";
    private static final String INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP_WITH_INVALID_DATE =
        "integration-processing/4_UNITS_2_GOTS_with_invalid_date.zip";
    private static final String INTEGRATION_PROCESSING_1_UNIT_1_GOTS_1MB_FOLDER_ZIP =
        "integration-processing/1_UNIT_1_GOTS_1MB_FOLDER.zip";
    public static final String BATCH_ID = "#batchId";
    public static final String OPI_FIELD = "#opi";

    private static String prefix;

    @Before
    public void setUp() throws Exception {
        prefix = MetadataCollections.UNIT.getPrefix();
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startWorkspaceServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
        prepareVitamSession();
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
    public void should_perform_collect_ingest_sip_operation_with_invalid_date() throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();
            String operationGuid;
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP_WITH_INVALID_DATE
                )
            ) {
                RequestResponse<UploadSipResult> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                UploadSipResult operationIdDto = ((RequestResponseOK<UploadSipResult>) response).getFirstResult();
                operationGuid = operationIdDto.requestId();
            }

            waitOperation(operationGuid);
            // Verify that the SIP folder has been deleted from the workspace
            try (WorkspaceClient workspaceClient = WorkspaceCollectClientFactory.getInstance().getClient()) {
                assertThat(workspaceClient.isExistingFolder(transactionId, "SIP")).isFalse();
            }
            TransactionDto updatedTransaction = getTransaction(collectClient, transactionId);
            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbookResult = logbookClient.selectOperationById(operationGuid);
            assertThat(logbookResult.get(TAG_RESULTS)).isNotNull();
            assertThat(logbookResult.get(TAG_RESULTS).size()).isGreaterThan(0);

            JsonNode firstResult = logbookResult.get(TAG_RESULTS).get(0);
            JsonNode events = firstResult.get(EVENTS);
            assertThat(events).isNotNull();

            assertThat(events.get(events.size() - 1).get("outcome").asText()).isEqualTo("KO");

            final RequestResponseOK<JsonNode> updatedUnitsResp = CollectTestHelper.selectUnitsByTransactionId(
                vitamContext,
                transactionDtoCreated.getId(),
                collectClient
            );
            assertThat(updatedUnitsResp.getResults()).isNotNull();
            assertThat(updatedUnitsResp.getResults().size()).isGreaterThan(0);
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();
            String operationGuid;
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(sipFilePath)) {
                RequestResponse<UploadSipResult> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                UploadSipResult operationIdDto = ((RequestResponseOK<UploadSipResult>) response).getFirstResult();
                operationGuid = operationIdDto.requestId();
            }

            waitOperation(operationGuid);
            // Verify that the SIP folder has been deleted from the workspace
            try (WorkspaceClient workspaceClient = WorkspaceCollectClientFactory.getInstance().getClient()) {
                assertThat(workspaceClient.isExistingContainer(operationGuid)).isFalse();
                assertThat(workspaceClient.isExistingFolder(operationGuid, "SIP")).isFalse();
            }
            TransactionDto updatedTransaction = getTransaction(collectClient, transactionId);
            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbookResult = logbookClient.selectOperationById(operationGuid);
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();
            String operationGuid;

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/SIP_KO_InvalidManifest.zip")) {
                RequestResponse<UploadSipResult> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

                UploadSipResult operationIdDto = ((RequestResponseOK<UploadSipResult>) response).getFirstResult();
                operationGuid = operationIdDto.requestId();
            }
            waitOperation(operationGuid);
            verifyOperation(operationGuid, StatusCode.KO);

            TransactionDto updatedTransaction = getTransaction(collectClient, transactionId);

            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());
            List<BatchDto> batches = updatedTransaction.getBatches();
            assertNotNull(batches);
            assertThat(batches).hasSize(1);
            batches.forEach(batch -> {
                assertEquals(batch.getBatchId(), operationGuid);
                assertEquals(BatchStatusDto.KO, batch.getBatchStatus());
                assertEquals("COLLECT_SIP_INGEST", batch.getEvTypeProc());
            });
        }
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_empty_title() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream("collect/KO_Empty_Title.zip");
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("<null>");

        // Unit without Title
        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("<null>");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.INVALID_UNIT.KO");
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité des valeurs dans les champs"
        );
        assertThat(validationErrors1.getFirst().getEvDetData()).contains(
            "Invalid unit format : Document schema validation failed"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_non_existing_rule() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream("collect/KO_non_existing_rule.zip");
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1", "Unit 2");

        // No such rule "NO_SUCH_RULE" declaration
        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.UNKNOWN.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains("Rule 'NO_SUCH_RULE' does not exist");
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de l'échéance des règles de gestion: Au moins une règle de gestion déclarée est inconnue du système ou l'échéance calculée est postérieure au 01/01/9000 (Date de début + Durée de la règle)"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // No such rule "NO_SUCH_RULE" in RefNonRuleId
        List<ValidationError> validationErrors2 = validationErrorsByUnitTitle.get("Unit 2");
        assertThat(validationErrors2).hasSize(1);
        assertThat(validationErrors2.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors2.getFirst().getOutDetail()).isEqualTo(
            "LFC.UNITS_RULES_COMPUTE.REF_INCONSISTENCY.KO"
        );
        assertThat(validationErrors2.getFirst().getEvDetData()).contains("Rule 'NO_SUCH_RULE' does not exist");
        assertThat(validationErrors2.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion dont l'annulation est demandée par rapport à sa catégorie : la demande d'annulation d'une règle de gestion n'est pas cohérente avec sa catégorie"
        );
        assertThat(validationErrors2.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_wrong_rule_category() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream("collect/KO_Wrong_Rule_Category.zip");
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1", "Unit 2");

        // Wrong category for rule "APP-00001" declaration
        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.CONSISTENCY.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains(
            "The rule 'APP-00001' is referenced in the wrong rule category. Declared StorageRule, actual AppraisalRule"
        );
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion par rapport à sa catégorie : Une règle déclarée est incohérente par rapport à sa catégorie"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // Wrong category for rule "APP-00001" in RefNonRuleId
        List<ValidationError> validationErrors2 = validationErrorsByUnitTitle.get("Unit 2");
        assertThat(validationErrors2).hasSize(1);
        assertThat(validationErrors2.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors2.getFirst().getOutDetail()).isEqualTo(
            "LFC.UNITS_RULES_COMPUTE.REF_INCONSISTENCY.KO"
        );
        assertThat(validationErrors2.getFirst().getEvDetData()).contains(
            "The rule 'APP-00001' is referenced in the wrong rule category. Declared DisseminationRule, actual AppraisalRule"
        );
        assertThat(validationErrors2.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion dont l'annulation est demandée par rapport à sa catégorie : la demande d'annulation d'une règle de gestion n'est pas cohérente avec sa catégorie"
        );
        assertThat(validationErrors2.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_wrong_rule_category_in_management_metadata() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream(
            "collect/KO_Wrong_Rule_Category_In_ManagementMetadata.zip"
        );
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1");

        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.UNKNOWN.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains("Rule 'NO_SUCH_RULE' does not exist");
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de l'échéance des règles de gestion: Au moins une règle de gestion déclarée est inconnue du système ou l'échéance calculée est postérieure au 01/01/9000 (Date de début + Durée de la règle)"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_wrong_rule_start_date() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream("collect/KO_SIP_RG-STARTDATE_AN9000.zip");
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1");

        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.INVALID_UNIT.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains(
            "Invalid unit format : Document schema validation failed"
        );
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité des valeurs dans les champs"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_sip_with_invalid_classification_level() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream("collect/KO_SIP_Classification_level.zip");
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1");

        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_CLASSIFICATION_LEVEL.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).isNull();
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification du niveau de classification : non autorisé par la plateforme"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_invalid_sip_schema_with_end_date_before_start_date() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream(
            "collect/KO_INVALID_SCHEMA_EndDate_Before_StartDate.zip"
        );
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);
        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);
        Map<String, List<ValidationError>> validationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);

        assertThat(validationErrorsByUnitTitle).containsOnlyKeys("Unit 1");

        List<ValidationError> validationErrors1 = validationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(1);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.CONSISTENCY.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains("EndDate is before StartDate");
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "La date contenue dans le champ Date de début doit être antérieure à la date contenue dans le champ Date de fin"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    @Test
    @RunWithCustomExecutor
    public void test_ingest_invalid_sip_complex_validation_errors() throws Exception {
        // Given
        InputStream sipInputStream = PropertiesUtils.getResourceAsStream(
            "collect/SIP_KO_Multiple_Validation_Errors_With_Virus.zip"
        );
        String transactionId = createTransactionId();

        // When
        String ingestOperationId = uploadSip(sipInputStream, transactionId);

        // Then
        verifyOperation(ingestOperationId, StatusCode.KO);

        Map<String, ArchiveUnitModel> units = selectUnits(transactionId);

        assertThat(units).hasSize(10);

        Map<String, List<ValidationError>> unitValidationErrorsByUnitTitle = mapUnitValidationErrorsByUnitTitle(units);
        Map<String, List<ValidationError>> unitOgInfoValidationErrorsByUnitTitle =
            mapUnitOgInfoValidationErrorsByUnitTitle(units);
        Map<String, DbObjectGroupModel> objectGroupsByUnitTitle = mapObjectGroupsByUnitTitle(units);
        Map<String, List<ValidationError>> objectGroupValidationErrorsByUnitTitle =
            mapObjectGroupValidationErrorsByUnitTitle(objectGroupsByUnitTitle);

        assertThat(unitValidationErrorsByUnitTitle).containsOnlyKeys(
            "Unit 1",
            "Unit 2",
            "Unit 3",
            "Unit 4",
            "Unit 5",
            "<null>",
            "Unit 7",
            "Unit 8"
        );

        assertThat(objectGroupValidationErrorsByUnitTitle).containsOnlyKeys("Unit 0", "Unit 2");
        assertThat(unitOgInfoValidationErrorsByUnitTitle).containsOnlyKeys("Unit 0", "Unit 2");

        // Unit 0 - Unit OK, but OG contains bad binary digest & virus detected
        List<ValidationError> ogValidationErrors0 = objectGroupValidationErrorsByUnitTitle.get("Unit 0");
        assertThat(ogValidationErrors0).hasSize(2);

        assertThat(ogValidationErrors0.getFirst().getEvId()).isNotNull();
        assertThat(ogValidationErrors0.getFirst().getObId()).isEqualTo(
            getBinaryId(objectGroupsByUnitTitle.get("Unit 0"), "BinaryMaster_1")
        );
        assertThat(ogValidationErrors0.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(ogValidationErrors0.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_DIGEST.CALC_CHECK.INVALID.KO");
        assertThat(ogValidationErrors0.getFirst().getEvDetData()).contains("MessageDigest");
        assertThat(ogValidationErrors0.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de l'empreinte du fichier"
        );
        assertThat(ogValidationErrors0.getFirst().getEvDateTime()).isNotNull();
        assertThat(ogValidationErrors0.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(ogValidationErrors0.get(1).getEvId()).isNotNull();
        assertThat(ogValidationErrors0.get(1).getObId()).isEqualTo(
            getBinaryId(objectGroupsByUnitTitle.get("Unit 0"), "BinaryMaster_1")
        );
        assertThat(ogValidationErrors0.get(1).getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(ogValidationErrors0.get(1).getOutDetail()).isEqualTo("LFC.OG_OBJECTS_ANTIVIRUS_CHECK.ANTIVIRUS.KO");
        assertThat(ogValidationErrors0.get(1).getEvDetData()).isNull();
        assertThat(ogValidationErrors0.get(1).getOutMessg()).isEqualTo("L'objet contient un virus");
        assertThat(ogValidationErrors0.get(1).getEvDateTime()).isNotNull();
        assertThat(ogValidationErrors0.get(1).getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(unitOgInfoValidationErrorsByUnitTitle.get("Unit 0"))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(objectGroupValidationErrorsByUnitTitle.get("Unit 0"));

        // Unit 1 - Invalid rule start date + unknown rule
        List<ValidationError> validationErrors1 = unitValidationErrorsByUnitTitle.get("Unit 1");
        assertThat(validationErrors1).hasSize(2);
        assertThat(validationErrors1.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.INVALID_UNIT.KO");
        assertThat(validationErrors1.getFirst().getEvDetData()).contains(
            "Invalid unit format : Document schema validation failed"
        );
        assertThat(validationErrors1.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité des valeurs dans les champs"
        );
        assertThat(validationErrors1.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors1.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(validationErrors1.get(1).getEvId()).isNotNull();
        assertThat(validationErrors1.get(1).getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors1.get(1).getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.UNKNOWN.KO");
        assertThat(validationErrors1.get(1).getEvDetData()).contains("Rule 'NO_SUCH_RULE' does not exist");
        assertThat(validationErrors1.get(1).getOutMessg()).isEqualTo(
            "Échec de la vérification de l'échéance des règles de gestion: Au moins une règle de gestion déclarée est inconnue du système ou l'échéance calculée est postérieure au 01/01/9000 (Date de début + Durée de la règle)"
        );
        assertThat(validationErrors1.get(1).getEvDateTime()).isNotNull();
        assertThat(validationErrors1.get(1).getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 2 - Rule declared in another category + 2x binaries with wrong digest
        List<ValidationError> validationErrors2 = unitValidationErrorsByUnitTitle.get("Unit 2");
        assertThat(validationErrors2).hasSize(1);
        assertThat(validationErrors2.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors2.getFirst().getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.CONSISTENCY.KO");
        assertThat(validationErrors2.getFirst().getEvDetData()).contains(
            "The rule 'APP-00001' is referenced in the wrong rule category. Declared AccessRule, actual AppraisalRule"
        );
        assertThat(validationErrors2.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion par rapport à sa catégorie : Une règle déclarée est incohérente par rapport à sa catégorie"
        );
        assertThat(validationErrors2.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors2.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        List<ValidationError> ogValidationErrors2 = objectGroupValidationErrorsByUnitTitle.get("Unit 2");
        assertThat(ogValidationErrors2).hasSize(3);

        assertThat(ogValidationErrors2.getFirst().getEvId()).isNotNull();
        assertThat(ogValidationErrors2.getFirst().getObId()).isEqualTo(
            getBinaryId(objectGroupsByUnitTitle.get("Unit 2"), "BinaryMaster_1")
        );
        assertThat(ogValidationErrors2.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(ogValidationErrors2.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_DIGEST.CALC_CHECK.INVALID.KO");
        assertThat(ogValidationErrors2.getFirst().getEvDetData()).contains("MessageDigest");
        assertThat(ogValidationErrors2.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de l'empreinte du fichier"
        );
        assertThat(ogValidationErrors2.getFirst().getEvDateTime()).isNotNull();
        assertThat(ogValidationErrors2.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(ogValidationErrors2.get(1).getEvId()).isNotNull();
        assertThat(ogValidationErrors2.get(1).getObId()).isEqualTo(
            getBinaryId(objectGroupsByUnitTitle.get("Unit 2"), "BinaryMaster_2")
        );
        assertThat(ogValidationErrors2.get(1).getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(ogValidationErrors2.get(1).getOutDetail()).isEqualTo("LFC.CHECK_DIGEST.CALC_CHECK.INVALID.KO");
        assertThat(ogValidationErrors2.get(1).getEvDetData()).contains("MessageDigest");
        assertThat(ogValidationErrors2.get(1).getOutMessg()).isEqualTo(
            "Échec de la vérification de l'empreinte du fichier"
        );
        assertThat(ogValidationErrors2.get(1).getEvDateTime()).isNotNull();
        assertThat(ogValidationErrors2.get(1).getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(ogValidationErrors2.get(2).getEvId()).isNotNull();
        assertThat(ogValidationErrors2.get(2).getObId()).isNull();
        assertThat(ogValidationErrors2.get(2).getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(ogValidationErrors2.get(2).getOutDetail()).isEqualTo("LFC.CHECK_OBJECT_GROUP_SCHEMA.KO");
        assertThat(ogValidationErrors2.get(2).getEvDetData()).contains(
            "metadata contains fields declared in ontology with a wrong format : Error 'Invalid date format: bad_date' on field 'LastModified'."
        );
        assertThat(ogValidationErrors2.get(2).getOutMessg()).isEqualTo(
            "Échec lors de la vérification globale du groupe d'objet"
        );
        assertThat(ogValidationErrors2.get(2).getEvDateTime()).isNotNull();
        assertThat(ogValidationErrors2.get(2).getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(unitOgInfoValidationErrorsByUnitTitle.get("Unit 2"))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(objectGroupValidationErrorsByUnitTitle.get("Unit 2"));

        // Unit 3 - RefNonRuleId for an unknown rule id
        List<ValidationError> validationErrors3 = unitValidationErrorsByUnitTitle.get("Unit 3");
        assertThat(validationErrors3).hasSize(1);
        assertThat(validationErrors3.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors3.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors3.getFirst().getOutDetail()).isEqualTo(
            "LFC.UNITS_RULES_COMPUTE.REF_INCONSISTENCY.KO"
        );
        assertThat(validationErrors3.getFirst().getEvDetData()).contains("Rule 'NO_SUCH_RULE' does not exist");
        assertThat(validationErrors3.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion dont l'annulation est demandée par rapport à sa catégorie : la demande d'annulation d'une règle de gestion n'est pas cohérente avec sa catégorie"
        );
        assertThat(validationErrors3.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors3.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 4 - RefNonRuleId with an invalid rule category + EndDate before StartDate
        List<ValidationError> validationErrors4 = unitValidationErrorsByUnitTitle.get("Unit 4");
        assertThat(validationErrors4).hasSize(2);
        assertThat(validationErrors4.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors4.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors4.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.CONSISTENCY.KO");
        assertThat(validationErrors4.getFirst().getEvDetData()).contains("EndDate is before StartDate");
        assertThat(validationErrors4.getFirst().getOutMessg()).isEqualTo(
            "La date contenue dans le champ Date de début doit être antérieure à la date contenue dans le champ Date de fin"
        );
        assertThat(validationErrors4.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors4.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        assertThat(validationErrors4.get(1).getEvId()).isNotNull();
        assertThat(validationErrors4.get(1).getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors4.get(1).getOutDetail()).isEqualTo("LFC.UNITS_RULES_COMPUTE.REF_INCONSISTENCY.KO");
        assertThat(validationErrors4.get(1).getEvDetData()).contains(
            "The rule 'APP-00001' is referenced in the wrong rule category. Declared ReuseRule, actual AppraisalRule"
        );
        assertThat(validationErrors4.get(1).getOutMessg()).isEqualTo(
            "Échec de la vérification de la cohérence de la règle de gestion dont l'annulation est demandée par rapport à sa catégorie : la demande d'annulation d'une règle de gestion n'est pas cohérente avec sa catégorie"
        );
        assertThat(validationErrors4.get(1).getEvDateTime()).isNotNull();
        assertThat(validationErrors4.get(1).getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 5 - Invalid classification level
        List<ValidationError> validationErrors5 = unitValidationErrorsByUnitTitle.get("Unit 5");
        assertThat(validationErrors5).hasSize(1);
        assertThat(validationErrors5.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors5.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors5.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_CLASSIFICATION_LEVEL.KO");
        assertThat(validationErrors5.getFirst().getEvDetData()).isNull();
        assertThat(validationErrors5.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification du niveau de classification : non autorisé par la plateforme"
        );
        assertThat(validationErrors5.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors5.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 6 - Missing title
        List<ValidationError> validationErrors6 = unitValidationErrorsByUnitTitle.get("<null>");
        assertThat(validationErrors6).hasSize(1);
        assertThat(validationErrors6.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors6.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors6.getFirst().getOutDetail()).isEqualTo("LFC.CHECK_UNIT_SCHEMA.INVALID_UNIT.KO");
        assertThat(validationErrors6.getFirst().getEvDetData()).contains(
            "Invalid unit format : Document schema validation failed"
        );
        assertThat(validationErrors6.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité des valeurs dans les champs"
        );
        assertThat(validationErrors6.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors6.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 7 - No such AUP
        List<ValidationError> validationErrors7 = unitValidationErrorsByUnitTitle.get("Unit 7");
        assertThat(validationErrors7).hasSize(1);
        assertThat(validationErrors7.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors7.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors7.getFirst().getOutDetail()).isEqualTo(
            "LFC.CHECK_ARCHIVE_UNIT_PROFILE.NOT_FOUND.KO"
        );
        assertThat(validationErrors7.getFirst().getEvDetData()).contains("Archive Unit Profile not found");
        assertThat(validationErrors7.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité aux profils d'unité archivistique : profil d'unité archivistique non trouvé"
        );
        assertThat(validationErrors7.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors7.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);

        // Unit 8 - AUP validation failed
        List<ValidationError> validationErrors8 = unitValidationErrorsByUnitTitle.get("Unit 8");
        assertThat(validationErrors8).hasSize(1);
        assertThat(validationErrors8.getFirst().getEvId()).isNotNull();
        assertThat(validationErrors8.getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(validationErrors8.getFirst().getOutDetail()).isEqualTo(
            "LFC.CHECK_ARCHIVE_UNIT_PROFILE.NOT_AU_JSON_VALID.KO"
        );
        assertThat(validationErrors8.getFirst().getEvDetData()).contains(
            "Archive unit profile validation failed: Document schema validation failed"
        );
        assertThat(validationErrors8.getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification de la conformité aux profils d'unité archivistique : json invalide"
        );
        assertThat(validationErrors8.getFirst().getEvDateTime()).isNotNull();
        assertThat(validationErrors8.getFirst().getEvIdProc()).isEqualTo(ingestOperationId);
    }

    private String createTransactionId() throws VitamClientException, InvalidParseOperationException {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = initProjectData();
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );
            projectDto.setId(projectDtoResult.getId());

            return createTransaction(vitamContext, projectDto.getId()).getId();
        }
    }

    private String uploadSip(InputStream sipInputStream, String transactionId) throws VitamClientException {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            RequestResponseOK<UploadSipResult> response = (RequestResponseOK<
                    UploadSipResult
                >) collectClient.uploadSipToTransaction(
                new VitamContext(TENANT_ID)
                    .setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT),
                transactionId,
                sipInputStream
            );
            assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
            final String operationId = Objects.requireNonNull(response.getFirstResult()).requestId();
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            waitOperation(operationId);
            return operationId;
        }
    }

    private static Map<String, ArchiveUnitModel> selectUnits(String transactionId) throws VitamClientException {
        try (CollectExternalClient collectExternalClient = CollectExternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery query = new SelectMultiQuery();
            List<JsonNode> units =
                ((RequestResponseOK<JsonNode>) collectExternalClient.getUnitsByTransaction(
                        new VitamContext(TENANT_ID),
                        transactionId,
                        query.getFinalSelect()
                    )).getResults();

            return units
                .stream()
                .map(unit -> {
                    try {
                        return JsonHandler.getFromJsonNode(unit, ArchiveUnitModel.class);
                    } catch (InvalidParseOperationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(
                    Collectors.toMap(
                        unit ->
                            unit.getDescriptiveMetadataModel().getTitle() != null
                                ? unit.getDescriptiveMetadataModel().getTitle()
                                : "<null>",
                        unit -> unit
                    )
                );
        }
    }

    private TransactionDto getTransaction(CollectExternalClient collectClient, String transactionId)
        throws VitamClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> updatedTransactionResponse = collectClient.getTransactionById(
            vitamContext,
            transactionId
        );
        return JsonHandler.getFromJsonNode(
            (((RequestResponseOK<JsonNode>) updatedTransactionResponse).getFirstResult()),
            TransactionDto.class
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_warning_when_ingest_sip_with_incorrect_sizes() throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();
            String operationGuid;
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/sip_warning_incorrect_size.zip")
            ) {
                RequestResponse<UploadSipResult> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
                assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

                UploadSipResult operationIdDto = ((RequestResponseOK<UploadSipResult>) response).getFirstResult();
                operationGuid = operationIdDto.requestId();
            }
            waitOperation(operationGuid);
            verifyOperation(operationGuid, StatusCode.WARNING);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbookResult = logbookClient.selectOperationById(operationGuid);
            assertThat(logbookResult.get(TAG_RESULTS)).isNotNull();
            assertThat(logbookResult.get(TAG_RESULTS).size()).isGreaterThan(0);

            JsonNode firstResult = logbookResult.get(TAG_RESULTS).get(0);
            JsonNode events = firstResult.get(EVENTS);
            assertThat(events).isNotNull();
            assertThat(events.isArray()).isTrue();
            assertThat(events.size()).isGreaterThan(0);
            assertThat(
                StreamSupport.stream(events.spliterator(), false)
                    .filter(
                        event ->
                            Arrays.asList("CHECK_OBJECT_SIZE.WARNING", "STP_OG_CHECK_AND_TRANSFORME.WARNING").contains(
                                event.get("outDetail").asText()
                            )
                    )
                    .count()
            ).isEqualTo(2L);
        }
    }

    private DbObjectGroupModel getObjectGroup(String objectGroupId) {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            return JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) collectClient.getObjectById(
                        vitamContext,
                        objectGroupId
                    )).getFirstResult(),
                DbObjectGroupModel.class
            );
        } catch (VitamClientException | InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode getObjectGroupNode(String objectGroupId) {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            return (
                (RequestResponseOK<JsonNode>) collectClient.getObjectById(vitamContext, objectGroupId)
            ).getFirstResult();
        } catch (VitamClientException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, List<ValidationError>> mapUnitValidationErrorsByUnitTitle(Map<String, ArchiveUnitModel> units) {
        return units
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getErrors() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getErrors()));
    }

    private Map<String, List<ValidationError>> mapUnitOgInfoValidationErrorsByUnitTitle(
        Map<String, ArchiveUnitModel> units
    ) {
        return units
            .entrySet()
            .stream()
            .filter(
                entry ->
                    entry.getValue().getObjectGroupInfo() != null &&
                    entry.getValue().getObjectGroupInfo().getErrors() != null
            )
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getObjectGroupInfo().getErrors()));
    }

    private Map<String, DbObjectGroupModel> mapObjectGroupsByUnitTitle(Map<String, ArchiveUnitModel> units) {
        return units
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getOg() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> getObjectGroup(entry.getValue().getOg())));
    }

    private static Map<String, List<ValidationError>> mapObjectGroupValidationErrorsByUnitTitle(
        Map<String, DbObjectGroupModel> objectGroupsByUnitTitle
    ) {
        return objectGroupsByUnitTitle
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getValidationErrors() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValidationErrors()));
    }

    private String getBinaryId(DbObjectGroupModel dbObjectGroupModel, String version) {
        return dbObjectGroupModel
            .getQualifiers()
            .stream()
            .flatMap(q -> q.getVersions().stream())
            .filter(v -> version.equals(v.getDataObjectVersion()))
            .map(DbVersionsModel::getId)
            .findFirst()
            .orElseThrow();
    }

    @Test
    @RunWithCustomExecutor
    public void should_perform_collect_multiple_ingest_sip_ok_operation() throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();

            String operationGuid1 = performCollectIngestOnTransaction(
                transactionId,
                INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP
            );

            String operationGuid2 = performCollectIngestOnTransaction(transactionId, INTEGRATION_PROCESSING_SIP2_ZIP);

            List<JsonNode> collectUnits =
                ((RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(
                        new VitamContext(TENANT_ID),
                        transactionId,
                        new SelectMultiQuery().getFinalSelect()
                    )).getResults();

            assertThat(collectUnits).isNotNull();
            assertThat(collectUnits).hasSize(16);
            Map<String, Integer> unitsByTransactionsCount = new HashMap<>();
            Map<String, Integer> unitsByBatchIdCount = new HashMap<>();
            collectUnits.forEach(unit -> {
                String batchId = unit.get(BATCH_ID).asText();
                String opi = unit.get(OPI_FIELD).asText();
                int transactionCount = unitsByTransactionsCount.getOrDefault(opi, 0);
                unitsByTransactionsCount.put(opi, transactionCount + 1);
                int batchCount = unitsByBatchIdCount.getOrDefault(batchId, 0);
                unitsByBatchIdCount.put(batchId, batchCount + 1);
            });

            assertThat(unitsByTransactionsCount).isNotEmpty();
            assertThat(unitsByTransactionsCount).containsEntry(transactionId, collectUnits.size());

            assertThat(unitsByBatchIdCount).isNotEmpty();
            assertThat(unitsByBatchIdCount).containsEntry(operationGuid1, 4);
            assertThat(unitsByBatchIdCount).containsEntry(operationGuid2, 12);

            //check gots
            List<JsonNode> gots = collectUnits
                .stream()
                .filter(unit -> unit.has("#object"))
                .map(unit -> getObjectGroupNode(unit.get("#object").asText()))
                .collect(Collectors.toList());

            assertThat(gots).isNotEmpty();
            Map<String, Integer> gotsByTransactionsCount = new HashMap<>();
            Map<String, Integer> gotsByBatchIdCount = new HashMap<>();
            gots.forEach(got -> {
                String batchId = got.get("_batchId").asText();
                String opi = got.get("_opi").asText();

                int transactionCount = gotsByTransactionsCount.getOrDefault(opi, 0);
                gotsByTransactionsCount.put(opi, transactionCount + 1);

                int batchCount = gotsByBatchIdCount.getOrDefault(batchId, 0);
                gotsByBatchIdCount.put(batchId, batchCount + 1);
            });

            assertThat(gotsByTransactionsCount).isNotEmpty();
            assertThat(gotsByTransactionsCount).containsEntry(transactionId, 14);

            assertThat(gotsByBatchIdCount).isNotEmpty();
            assertThat(gotsByBatchIdCount).containsEntry(operationGuid1, 2);
            assertThat(gotsByBatchIdCount).containsEntry(operationGuid2, 12);

            TransactionDto updatedTransaction = getTransaction(collectClient, transactionId);
            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());
            collectClient.closeTransaction(
                new VitamContext(TENANT_ID)
                    .setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT),
                transactionId
            );

            InputStream sipInputStream = generateSip(transactionId);
            assertThat(sipInputStream).isNotNull();

            String processId;
            switchToVitamMetadataHack(runner);
            prepareVitamSession();
            processId = ingestToVitam(sipInputStream);
            List<JsonNode> ingestedUnits = selectVitamMetadataUnitsByOpi(processId);
            assertThat(ingestedUnits).isNotNull();
            assertThat(ingestedUnits).hasSize(16);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_perform_collect_multiple_ingests_partial_ko_operation() throws Exception {
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

            final TransactionDto transactionDtoCreated = createTransaction(vitamContext, projectDto.getId());
            String transactionId = transactionDtoCreated.getId();

            String operationGuid1 = performCollectIngestOnTransaction(
                transactionId,
                INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP
            );

            InputStream koSipInputStream = PropertiesUtils.getResourceAsStream(
                "collect/SIP_KO_Multiple_Validation_Errors_With_Virus.zip"
            );

            InputStream koOtherSipInputStream = PropertiesUtils.getResourceAsStream(
                "collect/SIP_KO_Multiple_Validation_Errors_With_Virus.zip"
            );

            // When
            String operationGuid2 = uploadSip(koSipInputStream, transactionId);

            String operationGuid3 = uploadSip(koOtherSipInputStream, transactionId);

            // Then
            verifyOperation(operationGuid2, StatusCode.KO);
            verifyOperation(operationGuid3, StatusCode.KO);

            List<JsonNode> collectUnits =
                ((RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(
                        new VitamContext(TENANT_ID),
                        transactionId,
                        new SelectMultiQuery().getFinalSelect()
                    )).getResults();

            assertThat(collectUnits).isNotNull();
            assertThat(collectUnits).hasSize(24);
            Map<String, Integer> unitsByTransactionsCount = new HashMap<>();
            Map<String, Integer> unitsByBatchIdCount = new HashMap<>();
            collectUnits.forEach(unit -> {
                String batchId = unit.get(BATCH_ID).asText();
                String opi = unit.get(OPI_FIELD).asText();
                int transactionCount = unitsByTransactionsCount.getOrDefault(opi, 0);
                unitsByTransactionsCount.put(opi, transactionCount + 1);

                int batchCount = unitsByBatchIdCount.getOrDefault(batchId, 0);

                unitsByBatchIdCount.put(batchId, batchCount + 1);
            });

            assertThat(unitsByTransactionsCount).isNotEmpty();
            assertThat(unitsByTransactionsCount).containsEntry(transactionId, collectUnits.size());

            assertThat(unitsByBatchIdCount).isNotEmpty();
            assertThat(unitsByBatchIdCount).containsEntry(operationGuid1, 4);
            assertThat(unitsByBatchIdCount).containsEntry(operationGuid2, 10);

            //check gots
            List<JsonNode> gots = collectUnits
                .stream()
                .filter(unit -> unit.has("#object"))
                .map(unit -> getObjectGroupNode(unit.get("#object").asText()))
                .collect(Collectors.toList());

            assertThat(gots).isNotEmpty();
            Map<String, Integer> gotsByTransactionsCount = new HashMap<>();
            Map<String, Integer> gotsByBatchIdCount = new HashMap<>();
            gots.forEach(got -> {
                String batchId = got.get("_batchId").asText();
                String opi = got.get("_opi").asText();

                int transactionCount = gotsByTransactionsCount.getOrDefault(opi, 0);
                gotsByTransactionsCount.put(opi, transactionCount + 1);

                int batchCount = gotsByBatchIdCount.getOrDefault(batchId, 0);
                gotsByBatchIdCount.put(batchId, batchCount + 1);
            });

            assertThat(gotsByTransactionsCount).isNotEmpty();
            assertThat(gotsByTransactionsCount).containsEntry(transactionId, 10);

            assertThat(gotsByBatchIdCount).isNotEmpty();
            assertThat(gotsByBatchIdCount).containsEntry(operationGuid1, 2);
            assertThat(gotsByBatchIdCount).containsEntry(operationGuid2, 4);

            TransactionDto updatedTransaction = getTransaction(collectClient, transactionId);
            assertThat(updatedTransaction.getStatus()).isEqualTo(TransactionStatus.OPEN.name());

            List<BatchDto> batchs = updatedTransaction.getBatches();
            assertThat(batchs).isNotNull();
            assertThat(batchs).hasSize(2);
            List<BatchDto> koBatchs = batchs
                .stream()
                .filter(batchDto -> BatchStatusDto.KO.equals(batchDto.getBatchStatus()))
                .toList();

            assertThat(koBatchs).hasSize(2);
            Set<String> batchIds = batchs.stream().map(BatchDto::getBatchId).collect(Collectors.toSet());
            assertThat(batchIds).containsExactlyInAnyOrderElementsOf(List.of(operationGuid2, operationGuid3));

            // Verify that all batches have the evTypeProc field set to "COLLECT_SIP_INGEST"
            batchs.forEach(batch -> {
                assertEquals("COLLECT_SIP_INGEST", batch.getEvTypeProc());
            });
        }
    }

    private String performCollectIngestOnTransaction(String transactionId, String sipFilePath) throws Exception {
        String operationGuid;
        prepareVitamSession();
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(sipFilePath)) {
                RequestResponse<UploadSipResult> response = collectClient.uploadSipToTransaction(
                    new VitamContext(TENANT_ID)
                        .setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    transactionId,
                    inputStream
                );
                assertThat(HttpStatus.isSuccess(response.getStatus())).isTrue();
                UploadSipResult operationIdDto = ((RequestResponseOK<UploadSipResult>) response).getFirstResult();
                operationGuid = operationIdDto.requestId();
            }

            waitOperation(operationGuid);

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            JsonNode logbookResult = logbookClient.selectOperationById(operationGuid);
            assertThat(logbookResult.get(TAG_RESULTS)).isNotNull();
            assertThat(logbookResult.get(TAG_RESULTS).size()).isGreaterThan(0);

            JsonNode firstResult = logbookResult.get(TAG_RESULTS).get(0);
            JsonNode events = firstResult.get(EVENTS);
            assertThat(events).isNotNull();

            assertThat(events.get(events.size() - 1).get("outcome").asText()).isEqualTo("OK");
        }
        return operationGuid;
    }
}
