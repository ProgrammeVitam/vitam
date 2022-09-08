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

import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.builders.ManifestContextBuilder;
import fr.gouv.vitam.collect.internal.helpers.builders.TransactionModelBuilder;
import fr.gouv.vitam.collect.internal.model.ManifestContext;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.repository.TransactionRepository;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class TransactionService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private final TransactionRepository transactionRepository;
    private final ProjectService projectService;

    public TransactionService(TransactionRepository transactionRepository, ProjectService projectService) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
    }

    /**
     * create a transaction model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void createTransaction(TransactionDto transactionDto, String projectId) throws CollectException {
        Optional<ProjectModel> projectOpt = projectService.findProject(projectId);
        if (projectOpt.isEmpty()) {
            throw new CollectException("project with id " + projectId + "not found");
        }
        TransactionModel transactionModel = new TransactionModelBuilder()
            .withId(transactionDto.getId())
            .withManifestContext(CollectHelper.mapTransactionDtoToManifestContext(transactionDto))
            .withStatus(TransactionStatus.OPEN)
            .withTenant(transactionDto.getTenant())
            .withProjectId(projectId)
            .build();
        transactionRepository.createTransaction(transactionModel);
    }

    /**
     * create a transaction model from project model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void createTransactionFromProjectDto(ProjectDto projectDto, String transactionId) throws CollectException {
        ManifestContext manifestContext = new ManifestContextBuilder()
            .withArchivalAgreement(projectDto.getArchivalAgreement())
            .withMessageIdentifier(projectDto.getMessageIdentifier())
            .withArchivalAgencyIdentifier(projectDto.getArchivalAgencyIdentifier())
            .withTransferingAgencyIdentifier(projectDto.getTransferingAgencyIdentifier())
            .withOriginatingAgencyIdentifier(projectDto.getOriginatingAgencyIdentifier())
            .withSubmissionAgencyIdentifier(projectDto.getSubmissionAgencyIdentifier())
            .withArchivalProfile(projectDto.getArchivalProfile())
            .withComment(projectDto.getComment())
            .withUnitUp(projectDto.getUnitUp())
            .withAcquisitionInformation(projectDto.getAcquisitionInformation())
            .withLegalStatus(projectDto.getLegalStatus())
            .withCreationDate(projectDto.getCreationDate())
            .withlastUpdate(projectDto.getLastUpdate())
            .build();
        TransactionModel transactionModel = new TransactionModelBuilder()
            .withId(transactionId)
            .withManifestContext(manifestContext)
            .withProjectId(projectDto.getId())
            .withStatus(TransactionStatus.OPEN)
            .withTenant(projectDto.getTenant())
            .build();
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
     * return transaction according to id
     *
     * @param id model id to find
     * @return Optional<TransactionModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<TransactionModel> findTransactionByProjectId(String id) throws CollectException {
        return transactionRepository.findTransactionByProjectId(id);
    }

    /**
     * delete transaction according to id
     *
     * @param id transaction to delete
     */
    public void deleteTransactionById(String id) {
        transactionRepository.deleteTransaction(id);
    }


    public void closeTransaction(String transactionId) throws CollectException {
        Optional<TransactionModel> transactionModel = findTransaction(transactionId);
        if (transactionModel.isEmpty() || !checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
        changeStatusTransaction(TransactionStatus.READY, transactionModel.get());
    }

    public void replaceTransaction(TransactionModel transactionModel) throws CollectException {
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

}
