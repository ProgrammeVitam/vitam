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
package fr.gouv.vitam.collect.external.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalException;
import fr.gouv.vitam.collect.external.external.service.CollectExternalIngestService;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutomaticIngestThread implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AutomaticIngestThread.class);
    private static final int INITIAL_DELAY = 5;

    private final CollectInternalClientFactory collectInternalClientFactory;
    private final int threadPoolSize;
    private final CollectExternalIngestService collectExternalIngestService;

    public AutomaticIngestThread(CollectExternalConfiguration collectExternalConfiguration) {
        this.collectInternalClientFactory = CollectInternalClientFactory.getInstance();
        this.threadPoolSize = collectExternalConfiguration.getIngestionThreadPoolSize();
        this.collectExternalIngestService = new CollectExternalIngestService();

        final long delay = collectExternalConfiguration.getIngestionThreadFrequencySeconds();
        Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance()).scheduleAtFixedRate(
            this,
            Math.min(INITIAL_DELAY, delay),
            delay,
            TimeUnit.SECONDS
        );
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            LOGGER.error("Error when executing threads:", e);
        }
    }

    private void process() throws VitamClientException {
        Thread.currentThread().setName(AutomaticIngestThread.class.getName());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));

        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(this.threadPoolSize);

        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.getTransactionsToAutomaticallyIngest();

            final List<JsonNode> results = ((RequestResponseOK<JsonNode>) requestResponse).getResults();

            if (results.isEmpty()) {
                return;
            }

            Map<String, Integer> transactionsModel = results
                .stream()
                .collect(
                    Collectors.toMap(
                        transactionNode -> transactionNode.get("#id").asText(),
                        transactionNode -> Integer.parseInt(transactionNode.get("#tenant").asText())
                    )
                );

            List<CompletableFuture<Void>> completableFuturesList = new ArrayList<>();
            for (var transaction : transactionsModel.entrySet()) {
                CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(
                    () -> {
                        String transactionId = transaction.getKey();
                        int tenantId = transaction.getValue();
                        Thread.currentThread().setName(AutomaticIngestThread.class.getName() + "-" + transactionId);
                        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                        try {
                            sendSip(transactionId, tenantId);
                        } catch (Exception e) {
                            LOGGER.error("Error when sending automatic transaction " + transactionId, e);
                        }
                    },
                    executorService
                );

                completableFuturesList.add(completableFuture);
            }
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                completableFuturesList.toArray(new CompletableFuture[0])
            );
            combinedFuture.join();
        } finally {
            executorService.shutdown();
        }
    }

    private void sendSip(String transactionId, Integer tenantId) throws CollectExternalException {
        try (
            CollectInternalClient client = CollectInternalClientFactory.getInstance().getClient();
            IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient();
        ) {
            LOGGER.info("Sending SIP for transaction " + transactionId + " for tenant " + tenantId);
            String ingestOperationId = collectExternalIngestService.ingestSip(
                client,
                ingestExternalClient,
                transactionId
            );
            LOGGER.info("SIP sent successfully with id " + ingestOperationId);
        }
    }
}
