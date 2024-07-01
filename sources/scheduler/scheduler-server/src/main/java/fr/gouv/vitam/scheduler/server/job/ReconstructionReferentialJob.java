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

package fr.gouv.vitam.scheduler.server.job;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisallowConcurrentExecution
public class ReconstructionReferentialJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionReferentialJob.class);

    private final AdminManagementClientFactory adminManagementClientFactory;

    public ReconstructionReferentialJob() {
        this(AdminManagementClientFactory.getInstance());
    }

    ReconstructionReferentialJob(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean allReferentialsSucceeded = true;

        // voir s'il faut lancer sur plusieurs pool
        ExecutorService executorService = Executors.newSingleThreadExecutor(VitamThreadFactory.getInstance());

        try {
            List<CompletableFuture<Void>> completableFutures = getReconstructionCompletableFutures(executorService);

            for (CompletableFuture<Void> completableFuture : completableFutures) {
                try {
                    completableFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JobExecutionException(e.getMessage());
                } catch (ExecutionException e) {
                    LOGGER.error("Reconstruction failed", e);
                    allReferentialsSucceeded = false;
                }
            }
        } finally {
            executorService.shutdown();
        }

        if (!allReferentialsSucceeded) {
            throw new JobExecutionException("one or more referentials are failed");
        }
    }

    private List<CompletableFuture<Void>> getReconstructionCompletableFutures(ExecutorService executorService) {
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (FunctionalAdminCollections referential : FunctionalAdminCollections.values()) {
            switch (referential) {
                case ACCESSION_REGISTER_DETAIL:
                case ACCESSION_REGISTER_SUMMARY:
                case ACCESSION_REGISTER_SYMBOLIC:
                case VITAM_SEQUENCE:
                    continue;
            }
            String referentialValue = referential.name();

            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(
                () -> {
                    try (AdminManagementClient client = this.adminManagementClientFactory.getClient()) {
                        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
                        VitamThreadUtils.getVitamSession()
                            .setRequestId(GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant()));

                        LOGGER.info("Reconstruction " + referentialValue + " in progress...");
                        client.reconstructCollection(referentialValue);
                        LOGGER.info("Reconstruction " + referentialValue + " is finished");
                    } catch (AdminManagementClientServerException e) {
                        throw new IllegalStateException(
                            " Error when reconstruction " +
                            referentialValue +
                            "  :  " +
                            VitamConfiguration.getAdminTenant(),
                            e
                        );
                    }
                },
                executorService
            );
            completableFutures.add(completableFuture);
        }
        return completableFutures;
    }
}
