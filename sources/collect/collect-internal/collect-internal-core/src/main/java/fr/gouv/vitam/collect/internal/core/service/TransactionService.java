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
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
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
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections.CollectionUtils;

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

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNodeList;

public class TransactionService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String TRANSACTION_NOT_UPDATED = "Wrong status to update! ";
    private static final String PROJECT_ID = "ProjectId";
    private static final String STATUS = "Status";
    private static final String PROCESS_SIP_UNITARY = "PROCESS_SIP_UNITARY";

    private static final Map<String, TransactionStatus> OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP = Map.of(
        StatusCode.OK.name(), TransactionStatus.ACK_OK,
        StatusCode.KO.name(), TransactionStatus.ACK_KO,
        StatusCode.WARNING.name(), TransactionStatus.ACK_WARNING);

    private final TransactionRepository transactionRepository;
    private final MetadataRepository metadataRepository;
    private final ProjectService projectService;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final AccessInternalClientFactory accessInternalClientFactory;

    private final IngestInternalClientFactory ingestInternalClientFactory;

    public TransactionService(TransactionRepository transactionRepository, ProjectService projectService,
        MetadataRepository metadataRepository, WorkspaceClientFactory workspaceCollectClientFactory,
        AccessInternalClientFactory accessInternalClientFactory,
        IngestInternalClientFactory ingestInternalClientFactory) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
        this.metadataRepository = metadataRepository;
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.accessInternalClientFactory = accessInternalClientFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
    }

    /**
     * create a transaction model
     *
     * @throws CollectInternalException exception thrown in case of error
     */
    public void createTransaction(TransactionDto transactionDto, String projectId) throws CollectInternalException {
        Optional<ProjectDto> projectOpt = projectService.findProject(projectId);
        final String creationDate = LocalDateUtil.now().toString();

        if (projectOpt.isEmpty()) {
            throw new CollectInternalException("project with id " + projectId + "not found");
        }
        TransactionModel transactionModel = new TransactionModel(transactionDto.getId(), transactionDto.getName(),
            CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectOpt.get()), TransactionStatus.OPEN,
            projectId,
            creationDate, creationDate, transactionDto.getTenant());

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

    public void deleteTransactionContent(String id) throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(id)) {
                workspaceClient.deleteContainer(id, true);
            }
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Error when trying to delete stream from workspace: {} ", e);
            throw new CollectInternalException("Error when trying to delete stream from workspace: " + e);
        }

        try {
            final SelectMultiQuery request = new SelectMultiQuery();
            QueryProjection queryProjection = new QueryProjection();
            queryProjection.setFields(Map.of(VitamFieldsHelper.id(), 1, VitamFieldsHelper.object(), 1));
            request.setProjection(JsonHandler.toJsonNode(queryProjection));
            final ScrollSpliterator<JsonNode> scrollRequest = metadataRepository.selectUnits(request, id);
            Iterator<List<JsonNode>> iterator =
                Iterators.partition(new SpliteratorIterator<>(scrollRequest), VitamConfiguration.getBatchSize());

            while (iterator.hasNext()) {
                List<JsonNode> units = iterator.next();
                final List<String> idObjectGroups =
                    units.stream().map(e -> e.get(VitamFieldsHelper.object())).filter(Objects::nonNull)
                        .map(JsonNode::asText).collect(Collectors.toList());
                metadataRepository.deleteObjectGroups(idObjectGroups);
                final List<String> idUnits =
                    units.stream().map(e -> e.get(VitamFieldsHelper.id())).map(JsonNode::asText)
                        .collect(Collectors.toList());
                metadataRepository.deleteUnits(idUnits);
            }
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException(e);
        }
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
     */
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
        return listTransactions.stream().map(CollectHelper::convertTransactionModelToTransactionDto)
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

    public void checkAbortTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.OPEN,
            TransactionStatus.READY, TransactionStatus.ACK_KO, TransactionStatus.KO)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_UPDATED);
        }
    }

    public void checkReopenTransaction(TransactionModel transactionModel) throws CollectInternalException {
        if (!checkStatus(transactionModel, TransactionStatus.READY, TransactionStatus.ACK_KO,
            TransactionStatus.KO)) {
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


    public void attachVitamOperationId(String transactionId, String operationId)
        throws CollectInternalException {
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

    private List<TransactionModel> prepareTransactionsToUpdate(Map<String, String> statusOperation,
        List<TransactionModel> transactions) {

        List<TransactionModel> transactionsToUpdate = new ArrayList<>();
        for (TransactionModel transaction : transactions) {

            String operationStatus = statusOperation.get(transaction.getVitamOperationId());

            if (OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP.containsKey(operationStatus)) {
                transaction.setStatus(OPERATION_STATUS_TO_TRANSACTION_STATUS_MAP.get(
                    statusOperation.get(transaction.getVitamOperationId())));
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

        Set<String> operationIds = transactions.stream()
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

                return requestResponseOK.getResults()

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
            for (List<String> batchTransactionIds :
                Lists.partition(transactionIds, VitamConfiguration.getBatchSize())) {

                // FIXME : Add projection
                JsonNode select = getDslForSelectOperation(batchTransactionIds);
                RequestResponse<JsonNode> requestResponse = client.selectOperation(select, true, true);

                if (!requestResponse.isOk()) {
                    throw new CollectInternalException("Error from access client: " + requestResponse);
                }
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
                List<LogbookOperation> logbookOperations =
                    getFromJsonNodeList((requestResponseOK).getResults(),
                        new TypeReference<>() {
                        });
                logbookOperations.forEach(logbookOperation -> results.put(logbookOperation.getId(),
                    getOperationStatus(logbookOperation)));
            }

            List<String> notFoundTransactionIds = transactionIds.stream()
                .filter(transactionId -> !results.containsKey(transactionId))
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(notFoundTransactionIds)) {
                LOGGER.error("Invalid state. Transactions ids have not been found " + notFoundTransactionIds);
                throw new CollectInternalException(
                    "Invalid state. At least one transaction have not been found in Vitam");
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
        if (CollectionUtils.isNotEmpty(logbookOperation.getEvents())
            && PROCESS_SIP_UNITARY.equals(
            logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getEvType())) {
            return logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutcome();
        }

        LOGGER.warn(
            "Cannot retrieve ingest operation status from logbook operations for id " + logbookOperation.getId());
        return StatusCode.UNKNOWN.name();
    }

    public void manageTransactionsStatus() throws CollectInternalException {
        List<TransactionModel> transactions =
            this.transactionRepository.findTransactionsByQuery(eq(STATUS, TransactionStatus.SENT.name()));

        if (CollectionUtils.isEmpty(transactions)) {
            return;
        }

        Map<String, String> statusOperationFromProcessing = getIngestOperationStatusesFromProcessing(transactions);

        List<String> operationsWithoutStatusFromProcessing = transactions.stream()
            .map(TransactionModel::getVitamOperationId)
            .filter(id -> !statusOperationFromProcessing.containsKey(id))
            .collect(Collectors.toList());

        Map<String, String> operationStatusesFromLogbook = getIngestOperationStatusesFromLogbook(
            operationsWithoutStatusFromProcessing);

        Map<String, String> operationStatuses =
            Stream.concat(
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
    public void replaceTransaction(TransactionDto transactionDto) throws CollectInternalException {
        Optional<ProjectDto> projectOpt = projectService.findProject(transactionDto.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new CollectInternalException("project with id " + transactionDto.getProjectId() + "not found");
        }
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setId(transactionDto.getId());
        transactionModel.setName(transactionDto.getName());
        transactionModel
            .setManifestContext(CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectOpt.get()));
        transactionModel.setTenant(transactionDto.getTenant());
        transactionModel.setProjectId(transactionDto.getProjectId());
        transactionModel.setStatus(TransactionStatus.valueOf(transactionDto.getStatus()));
        // Update Dates
        transactionModel.setCreationDate(transactionDto.getCreationDate());
        transactionModel.setLastUpdate(transactionDto.getLastUpdate());

        transactionRepository.replaceTransaction(transactionModel);
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
}
