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
package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalNotFoundException;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.TransactionRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceCollectClientFactory;
import org.apache.commons.collections4.SetUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.collect.common.enums.TransactionStatus.ABORTED;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.ACK_KO;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.ACK_OK;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.ACK_WAITING;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.ACK_WARNING;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.KO;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.OPEN;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.READY;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.SENDING;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.SENT;
import static fr.gouv.vitam.collect.common.enums.TransactionStatus.VALIDATED;
import static fr.gouv.vitam.common.model.VitamConstants.DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionServiceTest {

    private static final String UNITS_WITH_GRAPH_PATH = "streamZip/units_with_graph.json";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private WorkspaceCollectClientFactory workspaceCollectClientFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceCollectClient;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private AccessInternalClient accessInternalClient;

    @Mock
    private AccessInternalClientFactory accessInternalClientFactory;

    @Mock
    private IngestInternalClientFactory ingestInternalClientFactory;

    @Mock
    private IngestInternalClient ingestInternalClient;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private FluxService fluxService;

    @Mock
    private ProcessingManagementClientFactory processingManagementClientFactory;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Before
    public void setup() {
        given(workspaceCollectClientFactory.getClient()).willReturn(workspaceCollectClient);
        given(workspaceClientFactory.getClient()).willReturn(workspaceCollectClient);

        transactionService = new TransactionService(
            transactionRepository,
            projectService,
            metadataRepository,
            fluxService,
            workspaceCollectClientFactory,
            workspaceClientFactory,
            accessInternalClientFactory,
            ingestInternalClientFactory,
            processingManagementClientFactory,
            logbookOperationsClientFactory,
            new CollectInternalConfiguration().setMaxWaitDelayForTransactionValidationInSeconds(5)
        );
    }

    @Test
    public void createCollectTest() throws CollectInternalException {
        // Given
        TransactionDto transactionDto = new TransactionDto(
            "XXXX00000111111",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            OPEN.toString(),
            false
        );
        ProjectDto project = new ProjectDto();

        // When
        transactionService.createTransaction(transactionDto, project);

        // Then
        ArgumentCaptor<TransactionModel> collectModelCaptor = ArgumentCaptor.forClass(TransactionModel.class);
        then(transactionRepository).should().createTransaction(collectModelCaptor.capture());
        TransactionModel transactionModelAdded = collectModelCaptor.getValue();
        Assertions.assertThat(transactionModelAdded.getId()).isEqualTo(transactionDto.getId());
    }

    @Test
    public void testFindCollect() throws CollectInternalException {
        // Given
        final String idCollect = "XXXX000002222222";
        TransactionDto transactionDto = new TransactionDto(
            "XXXX00000111111",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            OPEN.toString(),
            false
        );
        doReturn(Optional.of(transactionDto)).when(transactionRepository).findTransaction(any());

        // When
        transactionService.findTransaction(idCollect);

        // Then
        then(transactionRepository).should().findTransaction(idCollect);
    }

    @Test
    public void testTransactionStatusTransitions() throws Exception {
        Set<TransactionStatus> allStatuses = Sets.difference(Set.of(TransactionStatus.values()), Set.of(ACK_WAITING));
        for (TransactionStatus targetStatus : allStatuses) {
            Set<TransactionStatus> validInitialStatuses =
                switch (targetStatus) {
                    case OPEN -> Set.of(READY, VALIDATED, KO, ACK_KO);
                    case READY -> Set.of(OPEN);
                    case VALIDATED -> Set.of(READY);
                    case SENDING -> Set.of(VALIDATED);
                    case SENT -> Set.of(SENDING);
                    case KO -> Set.of(OPEN, READY, KO);
                    case ACK_OK, ACK_WARNING, ACK_KO -> Set.of(SENT);
                    case ABORTED -> Set.of(OPEN, READY, VALIDATED, KO, ACK_KO);
                    case ACK_WAITING -> throw new IllegalStateException("Unused status. Should never occur.");
                };

            // All expected statuses should succeed
            for (TransactionStatus validInitialStatus : validInitialStatuses) {
                Mockito.reset(transactionRepository);

                TransactionModel transaction = new TransactionModel().setStatus(validInitialStatus);

                assertThatCode(
                    () -> transactionService.changeTransactionStatus(targetStatus, transaction)
                ).doesNotThrowAnyException();
                assertThat(transaction.getStatus()).isEqualTo(targetStatus);
                verify(transactionRepository).replaceTransaction(transaction);
            }

            // All unexpected statuses should fail
            Set<TransactionStatus> invalidInitialStatuses = Sets.difference(allStatuses, validInitialStatuses);
            for (TransactionStatus invalidInitialStatus : invalidInitialStatuses) {
                Mockito.reset(transactionRepository);

                TransactionModel transaction = new TransactionModel().setStatus(invalidInitialStatus).setId("id");
                assertThatThrownBy(
                    () -> transactionService.changeTransactionStatus(targetStatus, transaction)
                ).isInstanceOf(CollectInternalInvalidRequestException.class);
                assertThat(transaction.getStatus()).isEqualTo(invalidInitialStatus);
                verify(transactionRepository, never()).replaceTransaction(transaction);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void manageTransactionsStatus() throws Exception {
        // Given
        when(accessInternalClientFactory.getClient()).thenReturn(accessInternalClient);
        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setId("5");
        LogbookEventOperation logbookEventOperation = new LogbookEventOperation();
        logbookEventOperation.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperation.setEvType("PROCESS_SIP_UNITARY");
        logbookOperation.setOutcome(StatusCode.OK.name());
        logbookEventOperation.setOutMessg("My awesome message" + DETAILS + "OK:" + 3 + " WARNING:" + 0 + " KO:" + 0);
        logbookEventOperation.setOutcome(StatusCode.OK.name());
        LogbookEventOperation logbookEventOperation2 = new LogbookEventOperation();

        logbookEventOperation2.setEvDetData(
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("data", "data"))
        );
        logbookEventOperation2.setEvType("PROCESS_SIP_UNITARY");
        logbookEventOperation2.setOutcome(StatusCode.OK.name());
        logbookEventOperation2.setOutMessg("My awesome message" + DETAILS + "OK:" + 3 + " WARNING:" + 0 + " KO:" + 0);
        List<LogbookEventOperation> logbookEventOperations = new ArrayList<>();
        logbookEventOperations.add(logbookEventOperation);
        logbookEventOperations.add(logbookEventOperation2);
        logbookOperation.setEvents(logbookEventOperations);
        JsonNode logbookOperationsNode = JsonHandler.toJsonNode(logbookOperation);
        when(accessInternalClient.selectOperation(any(), eq(true), eq(true))).thenReturn(
            new RequestResponseOK<JsonNode>().addAllResults(Collections.singletonList(logbookOperationsNode))
        );
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
        ProcessDetail processDetail1 = new ProcessDetail();
        processDetail1.setOperationId("4321");
        processDetail1.setGlobalState("RUNNING");
        processDetail1.setStepStatus("OK");
        ProcessDetail processDetail2 = new ProcessDetail();
        processDetail2.setOperationId("1234");
        processDetail2.setGlobalState("RUNNING");
        processDetail2.setStepStatus("OK");
        when(ingestInternalClient.listOperationsDetails(any())).thenReturn(
            new RequestResponseOK<ProcessDetail>().addAllResults(Arrays.asList(processDetail1, processDetail2))
        );
        TransactionModel transactionModel1 = new TransactionModel();
        transactionModel1.setVitamOperationId("1234");
        transactionModel1.setId("1234");
        TransactionModel transactionModel2 = new TransactionModel();
        transactionModel2.setVitamOperationId("5");
        transactionModel2.setId("5");
        List<TransactionModel> transactionModels = new ArrayList<>();
        transactionModels.add(transactionModel1);
        transactionModels.add(transactionModel2);
        when(transactionRepository.findTransactionsByQuery(any())).thenReturn(transactionModels);
        VitamThreadUtils.getVitamSession().setTenantId(1);
        // When - Then
        assertThatCode(() -> transactionService.manageTransactionsStatus()).doesNotThrowAnyException();
    }

    @Test
    public void deleteTransaction() throws Exception {
        // Given
        when(workspaceCollectClientFactory.getClient()).thenReturn(workspaceCollectClient);
        when(workspaceCollectClient.isExistingContainer(any())).thenReturn(true);
        final List<JsonNode> unitsJson = JsonHandler.getFromFileAsTypeReference(
            PropertiesUtils.getResourceFile(UNITS_WITH_GRAPH_PATH),
            new TypeReference<>() {}
        );
        when(metadataRepository.selectUnits(any(SelectMultiQuery.class), any())).thenReturn(
            new ScrollSpliterator<>(
                mock(SelectMultiQuery.class),
                query -> new RequestResponseOK<JsonNode>().addAllResults(new ArrayList<>(unitsJson)),
                0,
                0
            )
        );
        doNothing().when(workspaceCollectClient).deleteContainer(any(), eq(true));
        // When
        transactionService.deleteTransaction("1");
        // Then
        verify(metadataRepository, times(1)).deleteUnits(
            List.of(
                "aeaqaaaaaacpbveraqxzuamdvda5j5yaaaaq",
                "aeaqaaaaaacpbveraqxzuamdvda5l4qaaaaq",
                "aeaqaaaaaacpbveraqxzuamdvda5l4qaaaba",
                "aeaqaaaaaacpbveraqxzuamdvda5lcyaaaaq",
                "aeaqaaaaaacpbveraqxzuamdvda5l5qaaaaq",
                "aeaqaaaaaacpbveraqxzuamdvda5lcqaaaaq",
                "aeaqaaaaaacpbveraqxzuamdvda5l3qaaaaq"
            )
        );
    }

    @Test
    public void isTransactionContentNotEmptyTest() throws Exception {
        final String idTransaction = "XXXX000002222222";
        // Given
        when(workspaceCollectClient.isExistingContainer(any())).thenReturn(true);

        // When
        boolean transactionContentEmpty = transactionService.isTransactionContentEmpty(idTransaction);

        // Then
        assertThat(transactionContentEmpty).isFalse();
    }

    @Test
    public void checkTransactionContentEmptyTest() throws Exception {
        final String idTransaction = "XXXX000002222222";
        // Given
        when(workspaceCollectClient.isExistingContainer(any())).thenReturn(false);

        // When
        boolean transactionContentEmpty = transactionService.isTransactionContentEmpty(idTransaction);

        // Then
        assertThat(transactionContentEmpty).isTrue();
    }

    @Test
    public void checkSipDownloadable() throws CollectInternalInvalidRequestException {
        Set<TransactionStatus> validStatuses = Set.of(VALIDATED, SENDING, SENT, ACK_KO);
        Set<TransactionStatus> invalidStatuses = Set.of(READY, OPEN, KO, ACK_OK, ACK_WARNING, ABORTED);

        // Valid statuses for download
        for (TransactionStatus validStatus : validStatuses) {
            TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(validStatus);
            assertThatCode(() -> transactionService.checkSipDownloadable(transactionModel)).doesNotThrowAnyException();
        }

        // Invalid statuses for download
        for (TransactionStatus invalidStatus : invalidStatuses) {
            TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(invalidStatus);
            assertThatThrownBy(() -> transactionService.checkSipDownloadable(transactionModel)).isInstanceOf(
                CollectInternalInvalidRequestException.class
            );
        }

        // Check no other statuses are missed (except unused/deprecated ACK_WAITING)
        assertThat(
            SetUtils.difference(Set.of(TransactionStatus.values()), SetUtils.union(validStatuses, invalidStatuses))
        ).containsOnly(ACK_WAITING);
    }

    @Test
    public void testReopenTransaction_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(KO);
        doReturn(Optional.of(transactionModel)).when(transactionRepository).findTransaction("id");

        // When
        transactionService.reopenTransaction("id");

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(OPEN);
        verify(transactionRepository).replaceTransaction(transactionModel);
    }

    @Test
    public void testReopenTransactionWithInvalidState_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(SENDING);
        doReturn(Optional.of(transactionModel)).when(transactionRepository).findTransaction("id");

        // When / Then
        assertThatThrownBy(() -> transactionService.reopenTransaction("id")).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(SENDING);
        verify(transactionRepository, never()).replaceTransaction(transactionModel);
    }

    @Test
    public void testReopenTransactionNotFound() throws Exception {
        // Given
        doReturn(Optional.empty()).when(transactionRepository).findTransaction("id");

        // When / Then
        assertThatThrownBy(() -> transactionService.reopenTransaction("id")).isInstanceOf(
            CollectInternalNotFoundException.class
        );
    }

    @Test
    public void testAbortTransaction_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(OPEN);
        doReturn(Optional.of(transactionModel)).when(transactionRepository).findTransaction("id");

        // When
        transactionService.abortTransaction("id");

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(ABORTED);
        verify(transactionRepository).replaceTransaction(transactionModel);
    }

    @Test
    public void testAbortTransactionWithInvalidState_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(SENDING);
        doReturn(Optional.of(transactionModel)).when(transactionRepository).findTransaction("id");

        // When / Then
        assertThatThrownBy(() -> transactionService.closeTransaction(transactionModel)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(SENDING);
        verify(transactionRepository, never()).replaceTransaction(transactionModel);
    }

    @Test
    public void testAbortTransactionNotFound() throws Exception {
        // Given
        doReturn(Optional.empty()).when(transactionRepository).findTransaction("id");

        // When / Then
        assertThatThrownBy(() -> transactionService.abortTransaction("id")).isInstanceOf(
            CollectInternalNotFoundException.class
        );
    }

    @Test
    public void testCloseTransaction_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(OPEN);

        // When
        transactionService.closeTransaction(transactionModel);

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(READY);
        verify(transactionRepository).replaceTransaction(transactionModel);
    }

    @Test
    public void testCloseTransactionWithInvalidState_OK() throws Exception {
        // Given
        TransactionModel transactionModel = new TransactionModel().setId("id").setStatus(KO);

        // When / Then
        assertThatThrownBy(() -> transactionService.closeTransaction(transactionModel)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );

        // Then
        assertThat(transactionModel.getStatus()).isEqualTo(KO);
        verify(transactionRepository, never()).replaceTransaction(transactionModel);
    }

    @Test
    public void testAwaitTransactionValidationWhenAlreadyValidated_OK() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(Optional.of(new TransactionModel().setId(transactionId).setStatus(VALIDATED)))
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When
        transactionService.awaitTransactionValidationForIngest(transactionId);

        // Then
        verify(transactionRepository, times(1)).findTransaction(transactionId);
    }

    @Test
    public void testAwaitTransactionValidationWhenReopenedMeanwhile_KO() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(Optional.of(new TransactionModel().setId(transactionId).setStatus(OPEN)))
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When / Then
        assertThatThrownBy(() -> transactionService.awaitTransactionValidationForIngest(transactionId)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );
        verify(transactionRepository, times(1)).findTransaction(transactionId);
    }

    @Test
    public void testAwaitTransactionValidationWhenReadyThenValidated_OK() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(VALIDATED))
        )
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When
        transactionService.awaitTransactionValidationForIngest(transactionId);

        // Then
        verify(transactionRepository, times(3)).findTransaction(transactionId);
    }

    @Test
    public void testAwaitTransactionValidationWhenReadyThenKO_KO() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(KO))
        )
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When / Then
        assertThatThrownBy(() -> transactionService.awaitTransactionValidationForIngest(transactionId)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );
        verify(transactionRepository, times(3)).findTransaction(transactionId);
    }

    @Test
    public void testAwaitTransactionValidationWhenReadyThenOpen_KO() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(OPEN))
        )
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When / Then
        assertThatThrownBy(() -> transactionService.awaitTransactionValidationForIngest(transactionId)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );
        verify(transactionRepository, times(3)).findTransaction(transactionId);
    }

    @Test
    public void testAwaitTransactionValidationWhenSentConcurrently_KO() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(
            Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)),
            Optional.of(new TransactionModel().setId(transactionId).setStatus(SENT))
        )
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When / Then
        assertThatThrownBy(() -> transactionService.awaitTransactionValidationForIngest(transactionId)).isInstanceOf(
            CollectInternalInvalidRequestException.class
        );
        verify(transactionRepository, times(2)).findTransaction(transactionId);
    }

    /**
     * Ensure test timeouts out
     */
    @Test(timeout = 30_000)
    public void testAwaitTransactionValidationTimeout_OK() throws Exception {
        // Given
        String transactionId = "tx-id";
        doReturn(Optional.of(new TransactionModel().setId(transactionId).setStatus(READY)))
            .when(transactionRepository)
            .findTransaction(transactionId);

        // When / then
        assertThatThrownBy(() -> transactionService.awaitTransactionValidationForIngest(transactionId))
            .isInstanceOf(CollectInternalInvalidRequestException.class)
            .hasMessage("Timeout while waiting for transaction to become VALIDATED");
    }
}
