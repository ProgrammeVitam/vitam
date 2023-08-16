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
package fr.gouv.vitam.collect.internal.thread;

import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ManageStatusThread implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ManageStatusThread.class);
    private final TransactionService transactionService;
    private final int threadPoolSize;

    public ManageStatusThread(CollectInternalConfiguration collectInternalConfiguration,
        TransactionService transactionService) {
        this.transactionService = transactionService;

        Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance())
            .scheduleAtFixedRate(this, Math.min(5, collectInternalConfiguration.getStatusTransactionThreadFrequency()),
                collectInternalConfiguration.getStatusTransactionThreadFrequency(), TimeUnit.MINUTES);

        threadPoolSize = Math.min(collectInternalConfiguration.getTransactionStatusThreadPoolSize(),
            VitamConfiguration.getTenants().size());
    }

    @Override
    public void run() {
        try {
            process();
        } catch (CollectInternalException e) {
            LOGGER.error("Error when executing threads: {}", e);
        }
    }

    private void process() throws CollectInternalException {
        Thread.currentThread().setName(ManageStatusThread.class.getName());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));
        List<Integer> tenants = VitamConfiguration.getTenants();

        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);
        List<CompletableFuture<Void>> completableFuturesList = new ArrayList<>();
        try{
            for (var tenant : tenants) {
                CompletableFuture<Void> traceabilityCompletableFuture = CompletableFuture.runAsync(() -> {
                    Thread.currentThread().setName(ManageStatusThread.class.getName() + "-" + tenant);
                    VitamThreadUtils.getVitamSession().setTenantId(tenant);
                    try {
                        this.transactionService.manageTransactionsStatus();
                    } catch (Exception e) {
                        LOGGER.error("Error when managing status transaction: {}", e);
                    }
                }, executorService);

                completableFuturesList.add(traceabilityCompletableFuture);
            }
            CompletableFuture<Void> combinedFuture =
                CompletableFuture.allOf(completableFuturesList.toArray(new CompletableFuture[0]));
            combinedFuture.join();
        }finally {
            executorService.shutdown();
        }

    }
}
