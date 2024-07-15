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

package fr.gouv.vitam.metadata.core.reconstruction.service;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.model.PersistentIdentifierReconstructionRequest;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.reconstruction.domain.OffsetManager;
import fr.gouv.vitam.metadata.core.reconstruction.domain.PersistentIdentifierReconstructionManager;
import fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse.ReconstructionStatus.FAILURE;
import static fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse.ReconstructionStatus.INIT;

public class PersistentIdentifierReconstructionService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        PersistentIdentifierReconstructionService.class
    );

    private final OffsetManager offsetManager;
    private final PersistentIdentifierReconstructionManager persistentIdentifierReconstructionManager;
    private final int persistentIdentifierReconstructionThreadPoolSize;
    private final long persistentIdentifierReconstructionDelayInMinutes;

    public PersistentIdentifierReconstructionService(
        OffsetManager offsetManager,
        PersistentIdentifierReconstructionManager persistentIdentifierReconstructionManager,
        MetaDataConfiguration metaDataConfiguration
    ) {
        this.offsetManager = offsetManager;
        this.persistentIdentifierReconstructionManager = persistentIdentifierReconstructionManager;
        this.persistentIdentifierReconstructionThreadPoolSize =
            metaDataConfiguration.getPersistentIdentifierReconstructionThreadPoolSize();
        this.persistentIdentifierReconstructionDelayInMinutes =
            metaDataConfiguration.getPersistentIdentifierReconstructionDelayInMinutes();
    }

    public ReconstructionResponse reconstruct(PersistentIdentifierReconstructionRequest request) {
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        final List<Integer> tenantList = ParametersChecker.checkNullOrEmptyParameters(
            "List of tenants cannot be null and must contain at least one element",
            request.getTenants()
        );

        ExecutorService executorService = createExecutorService(
            Math.min(this.persistentIdentifierReconstructionThreadPoolSize, tenantList.size())
        );
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        try {
            ReconstructionResponse globalResponse = new ReconstructionResponse.Builder().status(INIT).build();
            for (Integer tenant : tenantList) {
                CompletableFuture<Void> reconstructionFuture = CompletableFuture.runAsync(
                    () -> {
                        try {
                            configureThreadAndSession(tenant, requestId);
                            LocalDateTime startDate = offsetManager.retrieveLastReconstructionDateFromOffset(tenant);
                            LocalDateTime endDate = offsetManager.retrieveEndDateWithDelay(
                                persistentIdentifierReconstructionDelayInMinutes
                            );
                            LOGGER.info(
                                "Start of reconstruction between dates {} and {} for tenant {}",
                                startDate,
                                endDate,
                                tenant
                            );
                            ReconstructionResponse response = persistentIdentifierReconstructionManager.reconstruct(
                                startDate,
                                endDate
                            );
                            offsetManager.saveNextReconstructionDateInOffset(
                                tenant,
                                response.getLastSuccessfulOperationDate()
                            );
                            LOGGER.info(
                                "End of reconstruction between dates {} and {} for tenant {}",
                                startDate,
                                endDate,
                                tenant
                            );
                            LOGGER.info(
                                "Date of the last operation processed in the persistent identifier reconstruction : {} for tenant {}",
                                response.getLastSuccessfulOperationDate(),
                                tenant
                            );

                            globalResponse.accumulate(response);
                        } catch (Exception e) {
                            LOGGER.error(
                                "An error occurred during persistent identifier reconstruction for tenant " + tenant,
                                e
                            );
                            globalResponse.accumulate(new ReconstructionResponse.Builder().status(FAILURE).build());
                        }
                    },
                    executorService
                );
                completableFutures.add(reconstructionFuture);
            }
            CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
            return globalResponse;
        } catch (Exception e) {
            LOGGER.error("An error occurred during persistent identifier reconstruction", e);
            return new ReconstructionResponse.Builder().status(FAILURE).build();
        } finally {
            executorService.shutdown();
        }
    }

    private ExecutorService createExecutorService(int threadPoolSize) {
        return ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);
    }

    private void configureThreadAndSession(int tenant, String requestId) {
        Thread.currentThread().setName("PersistentIdentifierReconstruction-" + tenant);
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);
    }
}
