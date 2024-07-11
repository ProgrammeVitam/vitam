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
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.TransactionRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.QueryProjection;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNodeList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class TransactionService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String TRANSACTION_NOT_UPDATED = "Wrong status to update! ";
    private static final String PROJECT_ID = "ProjectId";
    private static final String STATUS = "Status";
    private static final String AUTOMATIC_INGEST = "AutomaticIngest";
    private static final String PROCESS_SIP_UNITARY = "PROCESS_SIP_UNITARY";

    private static final Map<String, TransactionStatus> OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP = Map.of(
        StatusCode.OK.name(),
        TransactionStatus.ACK_OK,
        StatusCode.KO.name(),
        TransactionStatus.ACK_KO,
        StatusCode.WARNING.name(),
        TransactionStatus.ACK_WARNING
    );
    private static final int MAX_RETRY = 3;

    private final TransactionRepository transactionRepository;
    private final MetadataRepository metadataRepository;
    private final ProjectService projectService;
    private final FluxService fluxService;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final AccessInternalClientFactory accessInternalClientFactory;

    private final IngestInternalClientFactory ingestInternalClientFactory;

    public TransactionService(
        TransactionRepository transactionRepository,
        ProjectService projectService,
        MetadataRepository metadataRepository,
        FluxService fluxService,
        WorkspaceClientFactory workspaceCollectClientFactory,
        AccessInternalClientFactory accessInternalClientFactory,
        IngestInternalClientFactory ingestInternalClientFactory
    ) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
        this.metadataRepository = metadataRepository;
        this.fluxService = fluxService;
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.accessInternalClientFactory = accessInternalClientFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
    }

    /**
     * create a transaction model
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public void createTransaction(TransactionDto transactionDto, ProjectDto projectDto)
        throws CollectInternalException {
        final String creationDate = LocalDateUtil.now().toString();

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

    public void checkReadyTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.OPEN)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
    }

    public void checkSendingTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.READY)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
    }

    public void checkSendTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.SENDING)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
    }

    public void replaceTransaction(TransactionModel transactionModel) throws CollectInternalException {
        final String updateDate = LocalDateUtil.now().toString();
        transactionModel.setLastUpdate(updateDate);
        transactionRepository.replaceTransaction(transactionModel);
    }

    public boolean findOneAndReplace(TransactionStatus transactionStatus, TransactionModel transactionModel)
        throws InvalidParseOperationException {
        return transactionRepository.findOneAndReplace(transactionStatus, transactionModel);
    }

    public void checkAbortTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (
            !checkStatus(
                transactionModel,
                TransactionStatus.OPEN,
                TransactionStatus.READY,
                TransactionStatus.ACK_KO,
                TransactionStatus.KO
            )
        ) {
            throw new IllegalArgumentException(TRANSACTION_NOT_UPDATED);
        }
    }

    public void checkReopenTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.READY, TransactionStatus.ACK_KO, TransactionStatus.KO)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_UPDATED);
        }
    }

    public boolean checkStatus(TransactionModel transactionModel, TransactionStatus... transactionStatus) {
        return Arrays.stream(transactionStatus).anyMatch(tr -> transactionModel.getStatus().equals(tr));
    }

    public void changeTransactionStatus(TransactionStatus transactionStatus, String transactionId)
        throws CollectInternalException {
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
        TransactionModel transactionModel = transactionModelOptional.get();

        switch (transactionStatus) {
            case OPEN:
                checkReopenTransaction(transactionModel);
                break;
            case READY:
                checkReadyTransaction(transactionModel);
                break;
            case SENT:
                checkSendTransaction(transactionModel);
                break;
            case SENDING:
                checkSendingTransaction(transactionModel);
                break;
            case ABORTED:
                checkAbortTransaction(transactionModel);
                break;
            default:
                break;
        }
        transactionModel.setStatus(transactionStatus);
        transactionModel.setLastUpdate(LocalDateUtil.now().toString());
        replaceTransaction(transactionModel);
    }

    public void attachVitamOperationId(String transactionId, String operationId) throws CollectInternalException {
        Optional<TransactionModel> transactionModelOptional = findTransaction(transactionId);
        if (transactionModelOptional.isEmpty()) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
        TransactionModel transactionModel = transactionModelOptional.get();

        transactionModel.setVitamOperationId(operationId);
        transactionModel.setLastUpdate(LocalDateUtil.now().toString());
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
            InQuery in = QueryHelper.in(id(), vitamOperationsIds.toArray(new String[0]));
            select.setQuery(in);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Error when generate DSL for get Operations: {}", e);
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
            LOGGER.error("Error when select operation: {}", e);
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

    public List<TransactionModel> findReadyAutoIngestTransactions() throws CollectInternalException {
        return this.transactionRepository.findTransactionsByQueryWithoutTenant(
                and(eq(STATUS, TransactionStatus.READY.name()), eq(AUTOMATIC_INGEST, true))
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
        this.transactionRepository.replaceTransactions(transactionsToUpdate);
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
        transactionModel.setLastUpdate(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        transactionRepository.replaceTransaction(transactionModel);
        return transactionModel;
    }

    /**
     * check if the transaction content is empty
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public void isTransactionContentEmpty(String id) throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(id)) {
                throw new CollectInternalException("Cannot send an empty transaction");
            }
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error(e.getLocalizedMessage());
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
            if (updateTransaction(transactionModel)) {
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
            if (updateTransaction(retrievedTransaction)) {
                return Optional.of(retrievedTransaction);
            }
            retryCount++;
        }
        LOGGER.info(errorMsg);
        return Optional.empty();
    }

    private boolean updateTransaction(TransactionModel transactionModel) {
        try {
            return transactionRepository.findOneAndReplace(transactionModel);
        } catch (InvalidParseOperationException e) {
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
            purgeWorkspace(objectsGroup, transactionId);
            deleteGots(objectsGroup);
        }
    }

    private void purgeWorkspace(List<JsonNode> objects, String transactionId) throws CollectInternalException {
        for (JsonNode object : objects) {
            for (JsonNode qualifier : object.get(VitamFieldsHelper.qualifiers())) {
                if (!DataObjectVersionType.PHYSICAL_MASTER.getName().equals(qualifier.get("qualifier").textValue())) {
                    for (JsonNode version : qualifier.get("versions")) {
                        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
                            String uri = version.get("Uri").textValue();
                            if (
                                workspaceClient.isExistingContainer(transactionId) &&
                                workspaceClient.isExistingObject(transactionId, uri)
                            ) {
                                workspaceClient.deleteObject(transactionId, uri);
                            }
                        } catch (ContentAddressableStorageException e) {
                            throw new CollectInternalException(
                                "Error when trying to delete stream from workspace: ",
                                e
                            );
                        }
                    }
                }
            }
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
            throw new CollectInternalException("Invalid operation during unit creation : ", e);
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
        @Nullable String encoding
    ) throws CollectInternalException {
        try {
            fluxService.processStream(
                inputStreamObject,
                transactionModel.getProjectId(),
                transactionModel.getId(),
                encoding
            );
            return Response.ok().build();
        } catch (CollectInternalInvalidRequestException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (CollectInternalException e) {
            purgeFailedUploadSilently(transactionModel);
            throw e;
        }
    }

    public boolean changeTransactionToSendingIfBatchesNotKo(TransactionModel transaction)
        throws InvalidParseOperationException, CollectInternalException {
        boolean hasBatchKo =
            transaction.getBatches() != null &&
            transaction.getBatches().stream().anyMatch(batch -> BatchStatus.KO.equals(batch.getBatchStatus()));
        if (hasBatchKo) {
            throw new CollectInternalException(
                "impossible to generate the sip: this transaction has at least one KO batch"
            );
        }

        transaction.setStatus(TransactionStatus.SENDING);

        transaction.setLastUpdate(LocalDateUtil.now().toString());
        return findOneAndReplace(TransactionStatus.READY, transaction);
    }
}
