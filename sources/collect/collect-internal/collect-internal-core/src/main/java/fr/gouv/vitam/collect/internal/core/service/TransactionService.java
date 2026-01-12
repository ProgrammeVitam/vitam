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
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.collect.common.dto.BatchDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalNotFoundException;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.core.common.Batch;
import fr.gouv.vitam.collect.internal.core.common.BatchStatus;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.TransactionRepository;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.QueryProjection;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.DeletionRequestBody;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.common.utils.TransactionRestrictionHelper;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceCollectClientFactory;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNodeList;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class TransactionService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);
    private static final String TRANSACTION_NOT_FOUND = "Transaction not found";
    private static final String PROJECT_ID = "ProjectId";
    private static final String STATUS = "Status";
    private static final String AUTOMATIC_INGEST = "AutomaticIngest";
    private static final String PROCESS_SIP_UNITARY = "PROCESS_SIP_UNITARY";
    private static final String FOLDER_SIP = "SIP";

    private static final Map<String, TransactionStatus> OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP = Map.of(
        StatusCode.OK.name(),
        TransactionStatus.ACK_OK,
        StatusCode.KO.name(),
        TransactionStatus.ACK_KO,
        StatusCode.WARNING.name(),
        TransactionStatus.ACK_WARNING
    );
    private static final int MAX_RETRY = 3;
    public static final String TRANSACTION_ID = "transactionId";

    private final TransactionRepository transactionRepository;
    private final MetadataRepository metadataRepository;
    private final ProjectService projectService;
    private final FluxService fluxService;
    private final WorkspaceCollectClientFactory workspaceCollectClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final AccessInternalClientFactory accessInternalClientFactory;
    private final IngestInternalClientFactory ingestInternalClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final int maxWaitDelayForTransactionValidationInSeconds;

    public TransactionService(
        TransactionRepository transactionRepository,
        ProjectService projectService,
        MetadataRepository metadataRepository,
        FluxService fluxService,
        WorkspaceCollectClientFactory workspaceCollectClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        AccessInternalClientFactory accessInternalClientFactory,
        IngestInternalClientFactory ingestInternalClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        CollectInternalConfiguration configuration
    ) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
        this.metadataRepository = metadataRepository;
        this.fluxService = fluxService;
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.accessInternalClientFactory = accessInternalClientFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.maxWaitDelayForTransactionValidationInSeconds =
            configuration.getMaxWaitDelayForTransactionValidationInSeconds();
    }

    /**
     * create a transaction model
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public void createTransaction(TransactionDto transactionDto, ProjectDto projectDto)
        throws CollectInternalException {
        final String creationDate = LocalDateUtil.nowFormatted();

        TransactionModel transactionModel = new TransactionModel(
            transactionDto.getId(),
            transactionDto.getName(),
            CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectDto),
            TransactionStatus.OPEN,
            projectDto.getId(),
            creationDate,
            creationDate,
            transactionDto.getTenant(),
            projectDto.getAutomaticIngest()
        );

        transactionRepository.createTransaction(transactionModel);
    }

    /**
     * delete transaction according to id
     *
     * @param id transaction to delete
     * @throws CollectInternalException exception thrown in case of error
     */
    public void deleteTransaction(String id) throws CollectInternalException {
        deleteTransactionContent(id);
        transactionRepository.deleteTransaction(id);
    }

    /**
     * return transaction according to id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectInternalException exception thrown in case of error
     */
    public Optional<TransactionModel> findTransaction(String id) throws CollectInternalException {
        return transactionRepository.findTransaction(id);
    }

    /**
     * return transaction according to project id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectInternalException exception thrown in case of error
     * FIXME : Delete usages since no there is no more 1 transaction limit for project
     */
    @Deprecated
    public Optional<TransactionModel> findLastTransactionByProjectId(String id) throws CollectInternalException {
        LOGGER.debug("Project id to find : {}", id);
        return transactionRepository.findTransactionByQuery(eq(PROJECT_ID, id));
    }

    /**
     * return transaction according to id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectInternalException exception thrown in case of error
     */
    public List<TransactionDto> findTransactionsByProjectId(String id) throws CollectInternalException {
        LOGGER.debug("Transaction id to find : {}", id);
        List<TransactionModel> listTransactions = transactionRepository.findTransactionsByQuery(eq(PROJECT_ID, id));
        return listTransactions
            .stream()
            .map(CollectHelper::convertTransactionModelToTransactionDto)
            .collect(Collectors.toList());
    }

    public void ensureTransactionIsOpen(TransactionModel transactionModel) throws CollectInternalException {
        if (transactionModel.getStatus() != TransactionStatus.OPEN) {
            throw new CollectInternalInvalidRequestException(
                "Transaction " + transactionModel.getId() + " must be OPEN but was " + transactionModel.getStatus()
            );
        }
    }

    public void replaceTransaction(TransactionModel transactionModel) throws CollectInternalException {
        final String updateDate = LocalDateUtil.nowFormatted();
        transactionModel.setLastUpdate(updateDate);
        transactionRepository.replaceTransaction(transactionModel);
    }

    public void changeTransactionStatus(TransactionStatus transactionStatus, TransactionModel transactionModel)
        throws CollectInternalException {
        switch (transactionStatus) {
            case OPEN -> checkTransitionToOpenStatus(transactionModel);
            case READY -> checkTransactionToReadyStatus(transactionModel);
            case VALIDATED -> checkTransitionToValidatedStatus(transactionModel);
            case SENDING -> checkTransitionToSendingStatus(transactionModel);
            case SENT -> checkTransitionToSentStatus(transactionModel);
            case ACK_OK -> checkTransactionToAckOKStatus(transactionModel);
            case ACK_WARNING -> checkTransactionToAckWarningStatus(transactionModel);
            case ACK_KO -> checkTransactionToAckKOStatus(transactionModel);
            case ABORTED -> checkTransitionToAbortedStatus(transactionModel);
            case KO -> checkTransitionToKoStatus(transactionModel);
            case ACK_WAITING -> throw new IllegalStateException("Unused status. Should never occur.");
            default -> throw new IllegalStateException("Unexpected value: " + transactionStatus);
        }
        LOGGER.info(
            "Updating transaction status from " +
            transactionModel.getStatus() +
            " to " +
            transactionStatus +
            " for transaction " +
            transactionModel.getId()
        );
        transactionModel.setStatus(transactionStatus);
        transactionModel.setLastUpdate(LocalDateUtil.nowFormatted());
        replaceTransaction(transactionModel);
    }

    public void checkTransitionToOpenStatus(TransactionModel transactionModel) throws CollectInternalException {
        checkStatus(
            TransactionStatus.OPEN,
            transactionModel,
            // Reopening a closed transaction before async transaction validation
            TransactionStatus.READY,
            // Reopening a validated transaction for editing
            TransactionStatus.VALIDATED,
            // Reopening a rejected transaction for editing
            TransactionStatus.ACK_KO,
            // Reopening an invalid transaction for editing
            TransactionStatus.KO
        );
    }

    public void checkTransactionToReadyStatus(TransactionModel transactionModel) throws CollectInternalException {
        // READY can only occur after OPEN
        checkStatus(TransactionStatus.READY, transactionModel, TransactionStatus.OPEN);
    }

    private void checkTransitionToValidatedStatus(TransactionModel transactionModel) throws CollectInternalException {
        // VALIDATED can only occur after READY
        checkStatus(TransactionStatus.VALIDATED, transactionModel, TransactionStatus.READY);
    }

    public void checkTransitionToSendingStatus(TransactionModel transactionModel) throws CollectInternalException {
        // SENDING can only occur after VALIDATED
        checkStatus(TransactionStatus.SENDING, transactionModel, TransactionStatus.VALIDATED);
    }

    public void checkTransitionToSentStatus(TransactionModel transactionModel) throws CollectInternalException {
        // SEND can only occur after SENDING
        checkStatus(TransactionStatus.SENT, transactionModel, TransactionStatus.SENDING);
    }

    private void checkTransactionToAckOKStatus(TransactionModel transactionModel) throws CollectInternalException {
        // ACK_OK can only occur after SENT
        checkStatus(TransactionStatus.ACK_OK, transactionModel, TransactionStatus.SENT);
    }

    private void checkTransactionToAckWarningStatus(TransactionModel transactionModel) throws CollectInternalException {
        // ACK_WARNING can only occur after SENT
        checkStatus(TransactionStatus.ACK_WARNING, transactionModel, TransactionStatus.SENT);
    }

    private void checkTransactionToAckKOStatus(TransactionModel transactionModel) throws CollectInternalException {
        // ACK_KO can only occur after SENT
        checkStatus(TransactionStatus.ACK_KO, transactionModel, TransactionStatus.SENT);
    }

    public void checkTransitionToAbortedStatus(TransactionModel transactionModel) throws CollectInternalException {
        checkStatus(
            TransactionStatus.ABORTED,
            transactionModel,
            // Aborting an OPEN transaction
            TransactionStatus.OPEN,
            // Aborting a READY transaction (before async validation ends)
            TransactionStatus.READY,
            // Aborting a closed transaction
            TransactionStatus.VALIDATED,
            // Aborted a rejected transaction
            TransactionStatus.ACK_KO,
            // Aborted a transaction with errors
            TransactionStatus.KO
        );
    }

    private void checkTransitionToKoStatus(TransactionModel transactionModel) throws CollectInternalException {
        checkStatus(
            TransactionStatus.KO,
            transactionModel,
            /* Ingest SIP with errors */
            TransactionStatus.OPEN,
            /* Validating / generating manifest failed */
            TransactionStatus.READY,
            /* Idempotency (multiple errors) */
            TransactionStatus.KO
        );
    }

    public void checkStatus(
        TransactionStatus targetStatus,
        TransactionModel transactionModel,
        TransactionStatus... acceptableTransactionStatuses
    ) throws CollectInternalException {
        if (Arrays.stream(acceptableTransactionStatuses).noneMatch(tr -> transactionModel.getStatus().equals(tr))) {
            throw new CollectInternalInvalidRequestException(
                "Cannot change transaction state from " + transactionModel.getStatus() + " to " + targetStatus
            );
        }
    }

    public void attachVitamOperationId(String transactionId, String operationId) throws CollectInternalException {
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new CollectInternalNotFoundException(TRANSACTION_NOT_FOUND);
        }

        if (transactionModelOptional.get().getStatus() != TransactionStatus.SENDING) {
            throw new CollectInternalInvalidRequestException(TRANSACTION_NOT_FOUND);
        }

        TransactionModel transactionModel = transactionModelOptional.get();

        transactionModel.setVitamOperationId(operationId);
        transactionModel.setLastUpdate(LocalDateUtil.nowFormatted());
        replaceTransaction(transactionModel);
    }

    public List<TransactionModel> getListTransactionToDeleteByTenant(Integer tenantId) throws CollectInternalException {
        return transactionRepository.getListTransactionToDeleteByTenant(tenantId);
    }

    private List<TransactionModel> prepareTransactionsToUpdate(
        Map<String, String> statusOperation,
        List<TransactionModel> transactions
    ) {
        List<TransactionModel> transactionsToUpdate = new ArrayList<>();
        for (TransactionModel transaction : transactions) {
            String operationStatus = statusOperation.get(transaction.getVitamOperationId());

            if (OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP.containsKey(operationStatus)) {
                transaction.setStatus(
                    OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP.get(
                        statusOperation.get(transaction.getVitamOperationId())
                    )
                );
                transactionsToUpdate.add(transaction);
            }
        }
        return transactionsToUpdate;
    }

    private JsonNode getDslForSelectOperation(List<String> vitamOperationsIds) throws CollectInternalException {
        Select select = new Select();
        try {
            InQuery in = QueryHelper.in(VitamFieldsHelper.id(), vitamOperationsIds.toArray(new String[0]));
            select.setQuery(in);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Error when generate DSL for get Operations:", e);
            throw new CollectInternalException(e);
        }
        return select.getFinalSelect();
    }

    private Map<String, String> getIngestOperationStatusesFromProcessing(List<TransactionModel> transactions)
        throws CollectInternalException {
        Set<String> operationIds = transactions
            .stream()
            .map(TransactionModel::getVitamOperationId)
            .collect(Collectors.toSet());

        ProcessQuery processQuery = new ProcessQuery();
        processQuery.setListProcessTypes(List.of(LogbookTypeProcess.INGEST.toString()));
        try (IngestInternalClient client = ingestInternalClientFactory.getClient()) {
            RequestResponse<ProcessDetail> requestResponse = client.listOperationsDetails(processQuery);
            if (!requestResponse.isOk()) {
                LOGGER.error("Error from access client: {}", requestResponse.toString());
                throw new CollectInternalException("Error from access client: " + requestResponse);
            } else {
                RequestResponseOK<ProcessDetail> requestResponseOK = (RequestResponseOK<ProcessDetail>) requestResponse;

                return requestResponseOK
                    .getResults()
                    .stream()
                    .filter(processDetail -> operationIds.contains(processDetail.getOperationId()))
                    .collect(Collectors.toMap(ProcessDetail::getOperationId, ProcessDetail::getStepStatus));
            }
        } catch (VitamClientException e) {
            throw new CollectInternalException("Error when select operation", e);
        }
    }

    private Map<String, String> getIngestOperationStatusesFromLogbook(List<String> transactionIds)
        throws CollectInternalException {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            Map<String, String> results = new HashMap<>();
            for (List<String> batchTransactionIds : Lists.partition(
                transactionIds,
                VitamConfiguration.getBatchSize()
            )) {
                // FIXME : Add projection
                JsonNode select = getDslForSelectOperation(batchTransactionIds);
                RequestResponse<JsonNode> requestResponse = client.selectOperation(select, true, true);

                if (!requestResponse.isOk()) {
                    throw new CollectInternalException("Error from access client: " + requestResponse);
                }
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
                List<LogbookOperation> logbookOperations = getFromJsonNodeList(
                    (requestResponseOK).getResults(),
                    new TypeReference<>() {}
                );
                logbookOperations.forEach(
                    logbookOperation -> results.put(logbookOperation.getId(), getOperationStatus(logbookOperation))
                );
            }

            List<String> notFoundTransactionIds = transactionIds
                .stream()
                .filter(transactionId -> !results.containsKey(transactionId))
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(notFoundTransactionIds)) {
                LOGGER.error("Invalid state. Transactions ids have not been found " + notFoundTransactionIds);
                throw new CollectInternalException(
                    "Invalid state. At least one transaction have not been found in Vitam"
                );
            }

            return results;
        } catch (VitamClientException e) {
            LOGGER.error("Error when select operation:", e);
            throw new CollectInternalException(e);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }

    private String getOperationStatus(LogbookOperation logbookOperation) {
        if (
            CollectionUtils.isNotEmpty(logbookOperation.getEvents()) &&
            PROCESS_SIP_UNITARY.equals(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getEvType()
            )
        ) {
            return logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutcome();
        }

        LOGGER.warn(
            "Cannot retrieve ingest operation status from logbook operations for id " + logbookOperation.getId()
        );
        return StatusCode.UNKNOWN.name();
    }

    public List<TransactionModel> findValidatedAutoIngestTransactions() throws CollectInternalException {
        return this.transactionRepository.findTransactionsByQueryWithoutTenant(
                and(eq(STATUS, TransactionStatus.VALIDATED.name()), eq(AUTOMATIC_INGEST, true))
            );
    }

    public void manageTransactionsStatus() throws CollectInternalException {
        List<TransactionModel> transactions =
            this.transactionRepository.findTransactionsByQuery(eq(STATUS, TransactionStatus.SENT.name()));

        if (CollectionUtils.isEmpty(transactions)) {
            return;
        }

        Map<String, String> statusOperationFromProcessing = getIngestOperationStatusesFromProcessing(transactions);

        List<String> operationsWithoutStatusFromProcessing = transactions
            .stream()
            .map(TransactionModel::getVitamOperationId)
            .filter(id -> !statusOperationFromProcessing.containsKey(id))
            .collect(Collectors.toList());

        Map<String, String> operationStatusesFromLogbook = getIngestOperationStatusesFromLogbook(
            operationsWithoutStatusFromProcessing
        );

        Map<String, String> operationStatuses = Stream.concat(
            statusOperationFromProcessing.entrySet().stream(),
            operationStatusesFromLogbook.entrySet().stream()
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<TransactionModel> transactionsToUpdate = prepareTransactionsToUpdate(operationStatuses, transactions);
        if (CollectionUtils.isNotEmpty(transactionsToUpdate)) {
            this.transactionRepository.replaceTransactions(transactionsToUpdate);
        }
    }

    /**
     * update a transaction model
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public TransactionModel replaceTransaction(TransactionDto transactionDto) throws CollectInternalException {
        final String projectId = transactionDto.getProjectId();
        Optional<ProjectDto> projectOpt = projectService.findProject(projectId);
        if (projectOpt.isEmpty()) {
            throw new CollectInternalException("project with id " + projectId + " not found");
        }
        final String id = transactionDto.getId();
        TransactionModel transactionModel = findTransaction(id).orElseThrow(
            () -> new CollectInternalNotFoundException("transaction with id " + id + " not found")
        );
        transactionModel.setName(transactionDto.getName());
        transactionModel.setManifestContext(
            CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectOpt.get())
        );
        transactionModel.setLastUpdate(LocalDateUtil.nowFormatted());
        transactionRepository.replaceTransaction(transactionModel);
        return transactionModel;
    }

    /**
     * check if the transaction content is empty
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public boolean isTransactionContentEmpty(String transactionId) throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            boolean containerEmpty = !workspaceClient.isExistingContainer(transactionId);
            if (containerEmpty) return true;

            final SelectMultiQuery request = new SelectMultiQuery();
            request.addUsedProjection(VitamFieldsHelper.id());
            request.setLimitFilter(0, 1);
            final RequestResponseOK<JsonNode> unitsResponse = metadataRepository.selectUnits(
                request.getFinalSelect(),
                transactionId
            );
            return unitsResponse.getHits().getSize() == 0;
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException("Invalid operation during metadata query : ", e);
        } catch (ContentAddressableStorageServerException e) {
            throw new CollectInternalException(e);
        }
    }

    public void deleteTransactionContent(String transactionId) throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(transactionId)) {
                workspaceClient.deleteContainer(transactionId, true);
            }
        } catch (ContentAddressableStorageException e) {
            throw new CollectInternalException("Error when trying to delete stream from workspace: ", e);
        }

        try {
            final SelectMultiQuery request = new SelectMultiQuery();
            QueryProjection queryProjection = new QueryProjection();
            queryProjection.setFields(Map.of(VitamFieldsHelper.id(), 1, VitamFieldsHelper.object(), 1));
            request.setProjection(JsonHandler.toJsonNode(queryProjection));
            final ScrollSpliterator<JsonNode> scrollRequest = metadataRepository.selectUnits(request, transactionId);
            Iterator<List<JsonNode>> iterator = Iterators.partition(
                new SpliteratorIterator<>(scrollRequest),
                VitamConfiguration.getBatchSize()
            );

            while (iterator.hasNext()) {
                List<JsonNode> units = iterator.next();
                final List<String> idObjectGroups = units
                    .stream()
                    .map(e -> e.get(VitamFieldsHelper.object()))
                    .filter(Objects::nonNull)
                    .map(JsonNode::asText)
                    .collect(Collectors.toList());
                metadataRepository.deleteObjectGroups(idObjectGroups);
                final List<String> idUnits = units
                    .stream()
                    .map(e -> e.get(VitamFieldsHelper.id()))
                    .map(JsonNode::asText)
                    .collect(Collectors.toList());
                metadataRepository.deleteUnits(idUnits);
            }
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
    }

    private void purgeFailedUploadSilently(TransactionModel transactionModel) {
        String batchId = VitamThreadUtils.getVitamSession().getRequestId();
        Batch batch = new Batch();
        batch.setBatchId(batchId);
        batch.setBatchStatus(BatchStatus.KO);
        Optional<TransactionModel> optionalTransactionModel;
        List<Batch> batches;
        try {
            batches = Optional.ofNullable(transactionModel.getBatches()).orElse(new ArrayList<>());
            batches.add(batch);
            optionalTransactionModel = traceTransaction(batches, transactionModel);
            if (optionalTransactionModel.isEmpty()) {
                return;
            }
        } catch (CollectInternalException e) {
            LOGGER.info("unable to Update Transaction :", e);
            return;
        }

        TransactionModel newTransactionModel = optionalTransactionModel.get();
        try {
            purgeByBatchId(batchId, newTransactionModel);
        } catch (CollectInternalException e) {
            LOGGER.error("unable to purge uploaded content :", e);
            return;
        }

        try {
            batch.setBatchStatus(BatchStatus.PURGED);
            traceTransaction(batches, newTransactionModel);
        } catch (CollectInternalException e) {
            LOGGER.info("unable to Update Transaction :", e);
        }
    }

    private Optional<TransactionModel> traceTransaction(List<Batch> batches, TransactionModel transactionModel)
        throws CollectInternalException {
        String errorMsg = "concurrency problem: The transaction was deleted by someone else";

        transactionModel.setBatches(batches);

        int retryCount = 0;
        while (retryCount < MAX_RETRY) {
            if (tryUpdateTransaction(transactionModel)) {
                return Optional.of(transactionModel);
            }
            LOGGER.info("Failed to update transaction with the provided model. Retrying with findTransaction...");
            Optional<TransactionModel> optionalTransaction = transactionRepository.findTransaction(
                transactionModel.getId()
            );

            if (optionalTransaction.isEmpty()) {
                LOGGER.error(errorMsg);
                return Optional.empty();
            }

            TransactionModel retrievedTransaction = optionalTransaction.get();
            retrievedTransaction.setBatches(transactionModel.getBatches());
            if (tryUpdateTransaction(retrievedTransaction)) {
                return Optional.of(retrievedTransaction);
            }
            retryCount++;
        }
        LOGGER.info(errorMsg);
        return Optional.empty();
    }

    private boolean tryUpdateTransaction(TransactionModel transactionModel) {
        try {
            return transactionRepository.findOneAndReplace(transactionModel);
        } catch (CollectInternalException e) {
            LOGGER.error("Unable to update transaction: ", e);
            return false;
        }
    }

    public void purgeByBatchId(String batchId, TransactionModel transactionModel) throws CollectInternalException {
        String transactionId = transactionModel.getId();
        purgeObjectAndWorkspace(batchId, transactionId);
        purgeUnits(batchId, transactionId);
    }

    private void purgeObjectAndWorkspace(String batchId, String transactionId) throws CollectInternalException {
        Iterator<List<JsonNode>> selectObjectIterator;
        // Simply delete the batch container
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(batchId)) {
                workspaceClient.deleteContainer(batchId, true);
            }
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("unable to delete batch container :", e);
            return;
        }

        try {
            final Select select = buildSelectWithBatchId(batchId);

            final SelectMultiQuery selectObjectRequest = new SelectMultiQuery();
            selectObjectRequest.setQuery(select.getQuery());

            // Scroll through object groups
            final ScrollSpliterator<JsonNode> selectObjectScrollRequest = metadataRepository.selectObjectGroups(
                selectObjectRequest,
                transactionId
            );
            selectObjectIterator = Iterators.partition(
                new SpliteratorIterator<>(selectObjectScrollRequest),
                VitamConfiguration.getBatchSize()
            );
        } catch (InvalidCreateOperationException e) {
            throw new CollectInternalException("Invalid operation during object creation : ", e);
        }
        while (selectObjectIterator.hasNext()) {
            List<JsonNode> objectsGroup = selectObjectIterator.next();
            deleteGots(objectsGroup);
        }
    }

    private void deleteGots(List<JsonNode> objects) throws CollectInternalException {
        final List<String> idObjectGroups = objects
            .stream()
            .map(object -> object.get(VitamFieldsHelper.id()).textValue())
            .collect(Collectors.toList());
        metadataRepository.deleteObjectGroups(idObjectGroups);
    }

    private void purgeUnits(String batchId, String transactionId) throws CollectInternalException {
        try {
            final Select select = buildSelectWithBatchId(batchId);
            final SelectMultiQuery request = new SelectMultiQuery();
            request.setQuery(select.getQuery());
            request.addUsedProjection(VitamFieldsHelper.id());

            // Scroll through units
            final ScrollSpliterator<JsonNode> scrollRequest = metadataRepository.selectUnits(request, transactionId);
            Iterator<List<JsonNode>> iterator = Iterators.partition(
                new SpliteratorIterator<>(scrollRequest),
                VitamConfiguration.getBatchSize()
            );

            while (iterator.hasNext()) {
                List<JsonNode> units = iterator.next();
                // Collect unit ids
                final List<String> idUnits = units
                    .stream()
                    .map(e -> e.get(VitamFieldsHelper.id()).asText())
                    .collect(Collectors.toList());
                metadataRepository.deleteUnits(idUnits);
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new CollectInternalException("Invalid operation during unit purge : ", e);
        }
    }

    private Select buildSelectWithBatchId(String batchId) throws InvalidCreateOperationException {
        final Select select = new Select();
        select.setQuery(QueryHelper.eq((VitamFieldsHelper.batchId()), batchId));
        return select;
    }

    public Response uploadTransactionZip(
        InputStream inputStreamObject,
        TransactionModel transactionModel,
        @Nullable String encoding,
        @Nullable String attachementId
    ) throws CollectInternalException {
        String batchId = VitamThreadUtils.getVitamSession().getRequestId();
        try {
            fluxService.processStream(
                inputStreamObject,
                transactionModel.getProjectId(),
                transactionModel.getId(),
                encoding,
                attachementId
            );

            fluxService.moveObjectsFromBatchToTransaction(batchId, transactionModel.getId());

            return Response.ok().build();
        } catch (CollectInternalInvalidRequestException e) {
            LOGGER.error("An error occurs when try to upload the ZIP:", e);
            purgeFailedUploadSilently(transactionModel);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (CollectInternalException e) {
            purgeFailedUploadSilently(transactionModel);
            throw e;
        }
    }

    public Response startEliminationActionWorkflow(
        String transactionId,
        EliminationRequestBody eliminationRequestBody,
        Contexts eliminationWorkflowContext
    )
        throws CollectInternalException, InternalServerException, BadRequestException, OperationContextException, InvalidParseOperationException, ContentAddressableStorageServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException, InvalidGuidOperationException, VitamClientException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("Missing elimination request", eliminationRequestBody);

        //Enhancement to do : to apply AccessContract Restriction
        final SelectParserMultiple parser = new SelectParserMultiple();
        SelectMultiQuery selectMultiQuery = parser.getRequest();
        parser.parse(eliminationRequestBody.getDslRequest());
        TransactionRestrictionHelper.applyTransactionToQuery(transactionId, selectMultiQuery);
        eliminationRequestBody.setDslRequest(selectMultiQuery.getFinalSelect());

        // Start workflow
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                eliminationWorkflowContext.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.COLLECT_ELIMINATION_ACTION,
                STARTED,
                VitamLogbookMessages.getLabelOp("COLLECT_ELIMINATION_ACTION.STARTED") +
                " : " +
                GUIDReader.getGUID(operationId),
                GUIDReader.getGUID(operationId)
            );

            logbookOperationsClient.create(initParameters);
            workspaceClient.createContainer(operationId);
            workspaceClient.putObject(operationId, "request.json", writeToInpustream(eliminationRequestBody));

            // store original query in workspace
            workspaceClient.putObject(
                operationId,
                OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(eliminationRequestBody))
            );

            // compress file to backup
            OperationContextMonitor.compressInWorkspace(
                workspaceClientFactory,
                operationId,
                LogbookTypeProcess.COLLECT_ELIMINATION_ACTION,
                OperationContextMonitor.OperationContextFileName
            );

            processingClient.initVitamProcess(
                new ProcessingEntry(operationId, Contexts.COLLECT_ELIMINATION_ACTION.name())
            );

            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationId,
                Contexts.COLLECT_ELIMINATION_ACTION.name(),
                RESUME.getValue()
            );
            return jsonNodeRequestResponse.toResponse();
        }
    }

    public Response startDeletionWorkflow(
        String transactionId,
        DeletionRequestBody deletionRequestBody,
        Contexts deletionWorkflowContext
    )
        throws InvalidGuidOperationException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException, ContentAddressableStorageServerException, InvalidParseOperationException, OperationContextException, InternalServerException, BadRequestException, VitamClientException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("Missing deletion request", deletionRequestBody);

        final SelectParserMultiple parser = new SelectParserMultiple();
        SelectMultiQuery selectMultiQuery = parser.getRequest();
        parser.parse(deletionRequestBody.getDslRequest());
        TransactionRestrictionHelper.applyTransactionToQuery(transactionId, selectMultiQuery);
        deletionRequestBody.setDslRequest(selectMultiQuery.getFinalSelect());
        // Start workflow
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()
        ) {
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                GUIDReader.getGUID(operationId),
                deletionWorkflowContext.getEventType(),
                GUIDReader.getGUID(operationId),
                LogbookTypeProcess.COLLECT_DELETION_ACTION,
                STARTED,
                VitamLogbookMessages.getLabelOp("COLLECT_DELETION_ACTION.STARTED") +
                " : " +
                GUIDReader.getGUID(operationId),
                GUIDReader.getGUID(operationId)
            );

            logbookOperationsClient.create(initParameters);
            workspaceClient.createContainer(operationId);

            workspaceClient.putObject(operationId, "request.json", writeToInpustream(deletionRequestBody));

            // store original query in workspace
            workspaceClient.putObject(
                operationId,
                OperationContextMonitor.OperationContextFileName,
                writeToInpustream(OperationContextModel.get(deletionRequestBody))
            );

            // compress file to backup
            OperationContextMonitor.compressInWorkspace(
                workspaceClientFactory,
                operationId,
                LogbookTypeProcess.COLLECT_DELETION_ACTION,
                OperationContextMonitor.OperationContextFileName
            );

            processingClient.initVitamProcess(
                new ProcessingEntry(operationId, Contexts.COLLECT_DELETION_ACTION.name())
            );
            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationId,
                Contexts.COLLECT_DELETION_ACTION.name(),
                RESUME.getValue()
            );
            return jsonNodeRequestResponse.toResponse();
        }
    }

    public String uploadSipOnTransaction(
        String transactionId,
        GUID operationId,
        String contentType,
        InputStream uploadedInputStream
    )
        throws LogbookClientAlreadyExistsException, VitamClientException, InternalServerException, BadRequestException, InvalidParseOperationException, LogbookClientServerException, ContentAddressableStorageException, LogbookClientBadRequestException, InvalidGuidOperationException, CollectInternalException {
        try {
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            checkTransactionStatus(transactionId);
            pushSipStreamToWorkspaceCollect(operationId.toString(), contentType, uploadedInputStream);
            return launchCollectSipWorkflow(operationId, transactionId);
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
    }

    private void checkTransactionStatus(String transactionId) throws CollectInternalException {
        ParametersChecker.checkParameter("Missing transactionId", transactionId);
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new CollectInternalNotFoundException(TRANSACTION_NOT_FOUND);
        }
        ensureTransactionIsOpen(transactionModelOptional.get());
    }

    private String launchCollectSipWorkflow(GUID operationId, String transactionId)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException, ContentAddressableStorageServerException, InternalServerException, BadRequestException, VitamClientException, CollectInternalException, InvalidParseOperationException {
        ParametersChecker.checkParameter("Missing transaction id", transactionId);
        ParametersChecker.checkParameter("Missing request id", operationId);
        // Start workflow

        String operationGuid = operationId.toString();
        try (
            ProcessingManagementClient processingClient = processingManagementClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient();
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
            WorkspaceClient workspaceCollectClient = workspaceCollectClientFactory.getClient()
        ) {
            if (!workspaceCollectClient.isExistingContainer(transactionId)) {
                workspaceCollectClient.createContainer(transactionId);
            }

            Optional<TransactionModel> optionalTransaction = transactionRepository.findTransaction(transactionId);

            if (optionalTransaction.isEmpty()) {
                LOGGER.error("No transaction found with id " + transactionId);
                throw new CollectInternalException("No transaction found with id " + transactionId);
            }
            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                operationId,
                Contexts.COLLECT_SIP_INGEST.getEventType(),
                operationId,
                LogbookTypeProcess.COLLECT_SIP_INGEST,
                STARTED,
                VitamLogbookMessages.getLabelOp("COLLECT_SIP_INGEST.STARTED") + " : " + operationId,
                operationId
            );

            final ProcessingEntry processingEntry = new ProcessingEntry(
                operationGuid,
                Contexts.COLLECT_SIP_INGEST.name()
            );
            processingEntry.getExtraParams().put(WorkerParameterName.collectTransactionId.name(), transactionId);

            logbookOperationsClient.create(initParameters);
            workspaceClient.createContainer(operationGuid);

            processingClient.initVitamProcess(processingEntry);
            RequestResponse<ItemStatus> jsonNodeRequestResponse = processingClient.executeOperationProcess(
                operationGuid,
                Contexts.COLLECT_SIP_INGEST.name(),
                RESUME.getValue()
            );
            jsonNodeRequestResponse.toResponse();

            return operationGuid;
        }
    }

    /**
     * Pushes the inputStream to WorkspaceCollect
     *
     * @param containerName the containerName
     * @param uploadedInputStream the inputStream to store in workspace
     * @param contentType inputStream contentType
     * @throws ContentAddressableStorageException
     */
    private void pushSipStreamToWorkspaceCollect(
        final String containerName,
        final String contentType,
        final InputStream uploadedInputStream
    ) throws ContentAddressableStorageException {
        LOGGER.debug("Try to push stream to workspace...");

        try (WorkspaceClient workspaceCollectClient = workspaceCollectClientFactory.getClient()) {
            if (workspaceCollectClient.isExistingContainer(containerName)) {
                throw new ContentAddressableStorageException(containerName + " container already exist");
            }

            MediaType mediaType = CommonMediaType.valueOf(contentType);
            String archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);

            workspaceCollectClient.createContainer(containerName);
            workspaceCollectClient.uncompressObject(containerName, FOLDER_SIP, archiveMimeType, uploadedInputStream);

            String manifestPath = FOLDER_SIP + "/manifest.xml";
            Response manifestResponse = workspaceCollectClient.getObject(containerName, manifestPath);

            // FIXME: Document limitation (requires a single manifest file named exactly "manifest.xml"). We'll need to handle missing manifest.xml, multiple manifest.xml files (ignoring case)

            InputStream manifestInputStream = (InputStream) manifestResponse.getEntity();

            try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
                workspaceClient.createContainer(containerName);
                workspaceClient.putObject(containerName, FOLDER_SIP + "/manifest.xml", manifestInputStream);
            } finally {
                StreamUtils.closeSilently(manifestInputStream);
            }
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }

        LOGGER.debug("Push stream to workspace collect finished");
    }

    public void abortTransaction(String transactionId) throws CollectInternalException {
        LOGGER.info("Aborting transaction " + transactionId);
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new CollectInternalNotFoundException(TRANSACTION_NOT_FOUND);
        }
        changeTransactionStatus(TransactionStatus.ABORTED, transactionModelOptional.get());
    }

    public void reopenTransaction(String transactionId) throws CollectInternalException {
        LOGGER.info("Reopening transaction " + transactionId);
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new CollectInternalNotFoundException(TRANSACTION_NOT_FOUND);
        }
        changeTransactionStatus(TransactionStatus.OPEN, transactionModelOptional.get());
    }

    public void checkSipDownloadable(TransactionModel transaction) throws CollectInternalInvalidRequestException {
        switch (transaction.getStatus()) {
            case VALIDATED, SENDING, SENT, ACK_KO -> {
                // OK
            }
            case READY -> throw new CollectInternalInvalidRequestException(
                "Transaction SIP is being generated (" + transaction.getId() + ")"
            );
            case OPEN, KO, ACK_OK, ACK_WARNING, ABORTED -> throw new CollectInternalInvalidRequestException(
                "Cannot download SIP. Invalid transaction status %s (%s)".formatted(
                        transaction.getStatus(),
                        transaction.getId()
                    )
            );
            case ACK_WAITING -> throw new IllegalStateException("Unused status. Should never occur.");
            default -> throw new IllegalStateException("Unexpected value: " + transaction.getStatus());
        }
    }

    public void closeTransaction(TransactionModel transaction) throws CollectInternalException {
        // Mark transaction as Ready
        changeTransactionStatus(TransactionStatus.READY, transaction);
    }

    public void awaitTransactionValidationForIngest(String transactionId) throws CollectInternalException {
        Optional<TransactionModel> initialTransaction = findTransaction(transactionId);
        if (initialTransaction.isEmpty()) {
            throw new CollectInternalNotFoundException("No such transaction '" + transactionId + "'");
        }

        boolean noNeedToWait =
            switch (initialTransaction.get().getStatus()) {
                case OPEN, ABORTED -> throw new CollectInternalInvalidRequestException(
                    "Concurrent update for transaction '" +
                    transactionId +
                    "'. Expected " +
                    TransactionStatus.READY +
                    "/" +
                    TransactionStatus.VALIDATED +
                    ", got " +
                    initialTransaction.get().getStatus()
                );
                case SENDING, SENT, ACK_OK, ACK_WARNING, ACK_KO -> throw new CollectInternalInvalidRequestException(
                    "Another process started ingest for transaction '" +
                    transactionId +
                    "'. Expected " +
                    TransactionStatus.READY +
                    "/" +
                    TransactionStatus.VALIDATED +
                    ", got " +
                    initialTransaction.get().getStatus()
                );
                case READY -> {
                    // Log and start waiting...
                    LOGGER.info(
                        "Transaction " + transactionId + " is being processed. Waiting for transaction validation"
                    );
                    yield false;
                }
                case VALIDATED -> {
                    LOGGER.info("Transaction " + transactionId + " has been validated. No need for waiting");
                    yield true;
                }
                case KO -> throw new CollectInternalInvalidRequestException(
                    "Cannot ingest transaction " + transactionId + " because it has errors"
                );
                case ACK_WAITING -> throw new IllegalStateException("Unused status. Should never occur.");
            };

        if (noNeedToWait) {
            return;
        }

        int sleepDelayInMs = 250;
        int maxSleepDelayInMs = 60_000;
        StopWatch stopWatch = StopWatch.createStarted();

        while (true) {
            if (stopWatch.getTime(TimeUnit.SECONDS) > maxWaitDelayForTransactionValidationInSeconds) {
                throw new CollectInternalInvalidRequestException(
                    "Timeout while waiting for transaction to become " + TransactionStatus.VALIDATED
                );
            }

            try {
                Thread.sleep(Duration.ofMillis(sleepDelayInMs));
            } catch (InterruptedException e) {
                throw new CollectInternalException("Thread interrupted", e);
            }

            Optional<TransactionModel> currentTransaction = findTransaction(transactionId);
            if (currentTransaction.isEmpty()) {
                throw new CollectInternalInvalidRequestException(
                    "Concurrent update for transaction '" + transactionId + "'. Transaction deleted meanwhile?"
                );
            }

            boolean doneWaiting =
                switch (currentTransaction.get().getStatus()) {
                    case OPEN,
                        ABORTED,
                        SENDING,
                        SENT,
                        ACK_OK,
                        ACK_WARNING,
                        ACK_KO -> throw new CollectInternalInvalidRequestException(
                        "Concurrent update for transaction '" +
                        transactionId +
                        "'. Expected " +
                        TransactionStatus.READY +
                        "/" +
                        TransactionStatus.VALIDATED +
                        ", got " +
                        initialTransaction.get().getStatus()
                    );
                    case READY -> {
                        // Still waiting
                        LOGGER.info(
                            "Transaction " +
                            transactionId +
                            " is being processed. Waiting for transaction validation..."
                        );
                        yield false;
                    }
                    case VALIDATED -> {
                        LOGGER.info(
                            "Transaction " +
                            transactionId +
                            " has been " +
                            TransactionStatus.VALIDATED +
                            ". Done waiting."
                        );
                        yield true;
                    }
                    case KO -> throw new CollectInternalInvalidRequestException(
                        "Cannot ingest transaction " + transactionId + " because it has errors"
                    );
                    case ACK_WAITING -> throw new IllegalStateException("Unused status. Should never occur.");
                };

            if (doneWaiting) {
                return;
            }

            sleepDelayInMs = Math.min(sleepDelayInMs * 2, maxSleepDelayInMs);
        }
    }

    public void addTransactionBatch(String transactionId, BatchDto batchDto) throws CollectInternalException {
        Optional<TransactionModel> initialTransaction = findTransaction(transactionId);
        if (initialTransaction.isEmpty()) {
            throw new CollectInternalNotFoundException("No such transaction '" + transactionId + "'");
        }
        TransactionModel transaction = initialTransaction.get();
        //If the batch is executed before, we replace the old occurrence by the new including status change
        List<Batch> batches = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(transaction.getBatches())) {
            batches.addAll(
                transaction
                    .getBatches()
                    .stream()
                    .filter(batch -> !batch.getBatchId().equals(batchDto.getBatchId()))
                    .toList()
            );
        }
        Batch batch = new Batch();
        batch.setBatchId(batchDto.getBatchId());
        batch.setBatchStatus(BatchStatus.valueOf(batchDto.getBatchStatus().name()));
        batch.setEvTypeProc(batchDto.getEvTypeProc());
        batches.add(batch);
        transaction.setBatches(batches);
        transactionRepository.replaceTransaction(transaction);
    }
}
