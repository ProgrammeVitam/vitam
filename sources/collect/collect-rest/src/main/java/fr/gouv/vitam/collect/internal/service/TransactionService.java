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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.builders.ManifestContextBuilder;
import fr.gouv.vitam.collect.internal.model.ManifestContext;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.repository.TransactionRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamContext;
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
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
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
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;

public class TransactionService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String PROJECT_ID = "ProjectId";
    private static final String STATUS = "Status";
    private static final String PROCESS_SIP_UNITARY = "PROCESS_SIP_UNITARY";

    private static final Map<String, String> statusTrasactionsMap = getStatusTransactionsMap();


    private final TransactionRepository transactionRepository;
    private final ProjectService projectService;
    private final MetadataService metadataService;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final AccessExternalClientFactory accessExternalClientFactory;
    private final AdminExternalClientFactory adminExternalClientFactory;

    public TransactionService(TransactionRepository transactionRepository, ProjectService projectService,
        MetadataService metadataService, WorkspaceClientFactory workspaceCollectClientFactory,
        AccessExternalClientFactory accessExternalClientFactory,
        AdminExternalClientFactory adminExternalClientFactory) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
        this.metadataService = metadataService;
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.accessExternalClientFactory = accessExternalClientFactory;
        this.adminExternalClientFactory = adminExternalClientFactory;
    }


    private static Map<String, String> getStatusTransactionsMap() {
        Map<String, String> statusTrasactionsMap = new HashMap<>();
        statusTrasactionsMap.put(StatusCode.OK.name(), TransactionStatus.ACK_OK.name());
        statusTrasactionsMap.put(StatusCode.KO.name(), TransactionStatus.ACK_KO.name());
        statusTrasactionsMap.put(StatusCode.FATAL.name(), TransactionStatus.ACK_KO.name());
        statusTrasactionsMap.put(StatusCode.WARNING.name(), TransactionStatus.ACK_WARNING.name());
        return statusTrasactionsMap;
    }

    /**
     * create a transaction model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void createTransaction(TransactionDto transactionDto, String projectId) throws CollectException {
        Optional<ProjectDto> projectOpt = projectService.findProject(projectId);
        final String creationDate = LocalDateUtil.now().toString();

        if (projectOpt.isEmpty()) {
            throw new CollectException("project with id " + projectId + "not found");
        }
        TransactionModel transactionModel = new TransactionModel(transactionDto.getId(), transactionDto.getName(),
            CollectHelper.mapTransactionDtoToManifestContext(transactionDto), TransactionStatus.OPEN, projectId,
            creationDate, creationDate, transactionDto.getTenant());

        transactionRepository.createTransaction(transactionModel);
    }

    /**
     * delete transaction according to id
     *
     * @param id transaction to delete
     * @throws CollectException exception thrown in case of error
     */
    public void deleteTransaction(String id) throws CollectException {
        deleteTransactionContent(id);
        transactionRepository.deleteTransaction(id);
    }

    public void deleteTransactionContent(String id) throws CollectException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(id)) {
                workspaceClient.deleteContainer(id, true);
            }
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Error when trying to delete stream from workspace: {} ", e);
            throw new CollectException("Error when trying to delete stream from workspace: " + e);
        }

        try {
            final SelectMultiQuery request = new SelectMultiQuery();
            QueryProjection queryProjection = new QueryProjection();
            queryProjection.setFields(Map.of(VitamFieldsHelper.id(), 1, VitamFieldsHelper.object(), 1));
            request.setProjection(JsonHandler.toJsonNode(queryProjection));
            final ScrollSpliterator<JsonNode> scrollRequest = metadataService.selectUnits(request, id);
            Iterator<List<JsonNode>> iterator =
                Iterators.partition(new SpliteratorIterator<>(scrollRequest), VitamConfiguration.getBatchSize());

            while (iterator.hasNext()) {
                List<JsonNode> units = iterator.next();
                final List<String> idObjectGroups =
                    units.stream().map(e -> e.get(VitamFieldsHelper.object())).filter(Objects::nonNull)
                        .map(JsonNode::asText).collect(Collectors.toList());
                metadataService.deleteObjectGroups(idObjectGroups);
                final List<String> idUnits =
                    units.stream().map(e -> e.get(VitamFieldsHelper.id())).map(JsonNode::asText)
                        .collect(Collectors.toList());
                metadataService.deleteUnits(idUnits);
            }
        } catch (InvalidParseOperationException e) {
            throw new CollectException(e);
        }
    }

    /**
     * create a transaction model from project model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void createTransactionFromProjectDto(ProjectDto projectDto, String transactionId) throws CollectException {
        ManifestContext manifestContext =
            new ManifestContextBuilder().withArchivalAgreement(projectDto.getArchivalAgreement())
                .withMessageIdentifier(projectDto.getMessageIdentifier())
                .withArchivalAgencyIdentifier(projectDto.getArchivalAgencyIdentifier())
                .withTransferringAgencyIdentifier(projectDto.getTransferringAgencyIdentifier())
                .withOriginatingAgencyIdentifier(projectDto.getOriginatingAgencyIdentifier())
                .withSubmissionAgencyIdentifier(projectDto.getSubmissionAgencyIdentifier())
                .withArchivalProfile(projectDto.getArchivalProfile()).withComment(projectDto.getComment())
                .withAcquisitionInformation(projectDto.getAcquisitionInformation()).withUnitUp(projectDto.getUnitUp())
                .withLegalStatus(projectDto.getLegalStatus()).build();
        TransactionModel transactionModel =
            new TransactionModel(transactionId, projectDto.getName(), manifestContext, TransactionStatus.OPEN,
                projectDto.getId(), projectDto.getCreationDate(), projectDto.getLastUpdate(), projectDto.getTenant());
        transactionRepository.createTransaction(transactionModel);
    }

    /**
     * return transaction according to id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<TransactionModel> findTransaction(String id) throws CollectException {
        return transactionRepository.findTransaction(id);
    }

    /**
     * return transaction according to project id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<TransactionModel> findLastTransactionByProjectId(String id) throws CollectException {
        LOGGER.debug("Project id to find : {}", id);
        return transactionRepository.findTransactionByQuery(eq(PROJECT_ID, id));
    }

    /**
     * return transaction according to id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectException exception thrown in case of error
     */
    public List<TransactionDto> findTransactionsByProjectId(String id) throws CollectException {
        LOGGER.debug("Transaction id to find : {}", id);
        List<TransactionModel> listTransactions = transactionRepository.findTransactionsByQuery(eq(PROJECT_ID, id));
        return listTransactions.stream().map(CollectHelper::convertTransactionModelToTransactionDto).collect(Collectors.toList());
    }


    public void closeTransaction(String transactionId) throws CollectException {
        Optional<TransactionModel> transactionModel = findTransaction(transactionId);
        if (transactionModel.isEmpty() || !checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
        changeStatusTransaction(TransactionStatus.READY, transactionModel.get());
    }

    public void replaceTransaction(TransactionModel transactionModel) throws CollectException {
        final String updateDate = LocalDateUtil.now().toString();
        transactionModel.setLastUpdate(updateDate);
        transactionRepository.replaceTransaction(transactionModel);
    }

    public boolean checkStatus(TransactionModel transactionModel, TransactionStatus... transactionStatus) {
        return Arrays.stream(transactionStatus).anyMatch(tr -> transactionModel.getStatus().equals(tr));
    }

    public void changeStatusTransaction(TransactionStatus transactionStatus, TransactionModel transactionModel)
        throws CollectException {
        transactionModel.setStatus(transactionStatus);
        replaceTransaction(transactionModel);
    }

    public List<TransactionModel> getListTransactionToDeleteByTenant(Integer tenantId) throws CollectException {
        return transactionRepository.getListTransactionToDeleteByTenant(tenantId);
    }

    private Map<String, String> mapStatusOperations(List<LogbookOperation> results) {
        return results.stream()
            .map(logbookOperation -> logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1))
            .filter(eventLogBook -> PROCESS_SIP_UNITARY.equals(eventLogBook.getEvType())).collect(
                Collectors.toMap(LogbookEvent::getEvIdProc,
                    eventLogBook -> statusTrasactionsMap.containsKey(eventLogBook.getOutcome()) ?
                        statusTrasactionsMap.get(eventLogBook.getOutcome()) :
                        TransactionStatus.SENT.name()));


    }

    private Map<String, String> mapStatusOperationsFromProcess(List<ProcessDetail> results) {
        return results.stream().collect(Collectors.toMap(ProcessDetail::getOperationId, processDetail ->
            statusTrasactionsMap.containsKey(processDetail.getStepStatus()) ?
                statusTrasactionsMap.get(processDetail.getStepStatus()) :
                TransactionStatus.SENT.name()));
    }


    private List<TransactionModel> getTransactionsToUpdate(Map<String, String> statusOperation,
        List<TransactionModel> transactions) {

        List<TransactionModel> transactionsToUpdate = new ArrayList<>();
        for (TransactionModel transaction : transactions) {
            if (statusOperation.containsKey(transaction.getVitamOperationId())) {
                if (!TransactionStatus.SENT.name().equals(statusOperation.get(transaction.getId()))) {
                    transaction.setStatus(
                        TransactionStatus.valueOf(statusOperation.get(transaction.getVitamOperationId())));
                    transactionsToUpdate.add(transaction);
                }
            }
        }
        return transactionsToUpdate;
    }


    private JsonNode getDslForSelectOperation(List<String> vitamOperationsIds) throws CollectException {
        Select select = new Select();
        try {
            InQuery in = QueryHelper.in(id(), vitamOperationsIds.toArray(new String[0]));
            select.setQuery(in);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Error when generate DSL for get Operations: {}", e);
            throw new CollectException(e);
        }
        return select.getFinalSelect();
    }



    private Map<String, String> getOperationsStatusFromProcess(List<TransactionModel> transactions)
        throws CollectException {

        ProcessQuery processQuery = new ProcessQuery();
        processQuery.setListProcessTypes(List.of(LogbookTypeProcess.INGEST.toString()));
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            VitamContext vitamcontext = new VitamContext(tenantId);
            RequestResponse<ProcessDetail> requestResponse = client.listOperationsDetails(vitamcontext, processQuery);
            if (!requestResponse.isOk()) {
                LOGGER.error("Error from access client: {}", requestResponse.toString());
                throw new CollectException("Error from access client: " + requestResponse);
            } else {
                RequestResponseOK<ProcessDetail> requestResponseOK = (RequestResponseOK<ProcessDetail>) requestResponse;
                List<ProcessDetail> results = requestResponseOK.getResults();
                return mapStatusOperationsFromProcess(results);
            }
        } catch (VitamClientException e) {
            LOGGER.error("Error when select operation: {}", e);
            throw new CollectException(e);
        }
    }


    private Map<String, String> getOperationsStatusFromLogbook(List<TransactionModel> transactions)
        throws CollectException {

        JsonNode select = getDslForSelectOperation(
            transactions.stream().map(TransactionModel::getVitamOperationId).collect(Collectors.toList()));

        try (AccessExternalClient client = accessExternalClientFactory.getClient()) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            VitamContext vitamcontext = new VitamContext(tenantId);
            RequestResponse<LogbookOperation> requestResponse = client.selectOperations(vitamcontext, select);
            if (!requestResponse.isOk()) {
                LOGGER.error("Error from access client: {}", requestResponse.toString());
                throw new CollectException("Error from access client: " + requestResponse);
            } else {
                RequestResponseOK<LogbookOperation> requestResponseOK =
                    (RequestResponseOK<LogbookOperation>) requestResponse;
                List<LogbookOperation> results = requestResponseOK.getResults();
                return mapStatusOperations(results);
            }
        } catch (VitamClientException e) {
            LOGGER.error("Error when select operation: {}", e);
            throw new CollectException(e);
        }
    }

    public void manageTransactionsStatus() throws CollectException {
        List<TransactionModel> transactions =
            this.transactionRepository.findTransactionsByQuery(eq(STATUS, TransactionStatus.SENT.name()));
        Map<String, String> statusOperation;
        List<TransactionModel> transactionsToUpdate;
        if (CollectionUtils.isNotEmpty(transactions)) {
            statusOperation = getOperationsStatusFromProcess(transactions);
            transactionsToUpdate = getTransactionsToUpdate(statusOperation, transactions);
            this.transactionRepository.replaceTransactions(transactionsToUpdate);
            transactions.removeAll(transactionsToUpdate);

            if (CollectionUtils.isNotEmpty(transactions)) {
                statusOperation = getOperationsStatusFromLogbook(transactions);
                transactionsToUpdate.addAll(getTransactionsToUpdate(statusOperation, transactions));
                this.transactionRepository.replaceTransactions(transactionsToUpdate);
            }

        }
    }
    /**
     * update a transaction model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void replaceTransaction(TransactionDto transactionDto) throws CollectException {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setId(transactionDto.getId());
        transactionModel.setName(transactionDto.getName());
        transactionModel.setManifestContext(CollectHelper.mapTransactionDtoToManifestContext(transactionDto));
        transactionModel.setTenant(transactionDto.getTenant());
        transactionModel.setProjectId(transactionDto.getProjectId());
        transactionModel.setStatus(TransactionStatus.valueOf(transactionDto.getStatus()));
        transactionRepository.replaceTransaction(transactionModel);
    }

}
