/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.builders.CollectModelBuilder;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.repository.CollectRepository;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.util.Arrays;
import java.util.Optional;

public class CollectService {
    private final CollectRepository collectRepository;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectService.class);

    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";

    public CollectService(CollectRepository collectRepository) {
        this.collectRepository = collectRepository;
    }

    /**
     * create a collect model
     *
     * @param collectModel collection model to create
     * @throws CollectException exception thrown in case of error
     */
    public void createCollect(TransactionDto transactionDto) throws CollectException {
        CollectModel collectModel = new CollectModelBuilder()
            .withId(transactionDto.getId())
            .withArchivalAgreement(transactionDto.getArchivalAgreement())
            .withMessageIdentifier(transactionDto.getMessageIdentifier())
            .withArchivalAgencyIdentifier(transactionDto.getArchivalAgencyIdentifier())
            .withTransferingAgencyIdentifier(transactionDto.getTransferingAgencyIdentifier())
            .withOriginatingAgencyIdentifier(transactionDto.getOriginatingAgencyIdentifier())
            .withSubmissionAgencyIdentifier(transactionDto.getSubmissionAgencyIdentifier())
            .withArchivalProfile(transactionDto.getArchivalProfile())
            .withComment(transactionDto.getComment())
            .withStatus(TransactionStatus.OPEN)
            .build();
        collectRepository.createCollect(collectModel);
    }

    /**
     * return collection according to id
     *
     * @param id model id to find
     * @return Optional<CollectModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<CollectModel> findCollect(String id) throws CollectException {
        return collectRepository.findCollect(id);
    }

    public void closeTransaction(String transactionId) throws CollectException {
        Optional<CollectModel> collectModel = findCollect(transactionId);
        if (collectModel.isEmpty() || !checkStatus(collectModel.get(), TransactionStatus.OPEN)) {
            throw new IllegalArgumentException(TRANSACTION_NOT_FOUND);
        }
        CollectModel currentCollectModel = collectModel.get();
        currentCollectModel.setStatus(TransactionStatus.CLOSE);
        replaceCollect(currentCollectModel);
    }

    public void replaceCollect(CollectModel collectModel) throws CollectException {
        collectRepository.replaceCollect(collectModel);
    }

    public String createRequestId() {
        String id = GUIDFactory.newRequestIdGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId();
        LOGGER.debug("Generated Request Id : {}", id);
        return id;
    }

    public boolean checkStatus(CollectModel collectModel, TransactionStatus... transactionStatus) {
        return Arrays.stream(transactionStatus).anyMatch(tr -> collectModel.getStatus().equals(tr));
    }

}