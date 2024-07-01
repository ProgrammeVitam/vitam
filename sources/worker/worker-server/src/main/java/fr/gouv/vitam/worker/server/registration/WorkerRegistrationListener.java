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
package fr.gouv.vitam.worker.server.registration;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerConfiguration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listener used for registration between the current worker and the processing server
 */
public class WorkerRegistrationListener implements ServletContextListener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerRegistrationListener.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
        1,
        VitamThreadFactory.getInstance()
    );
    private final WorkerConfiguration configuration;

    private final ProcessingManagementClientFactory processingManagementClientFactory;

    private final WorkerRegister workerRegister;

    public WorkerRegistrationListener(WorkerConfiguration configuration) {
        this.configuration = configuration;
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getProcessingUrl());
        this.processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
        this.workerRegister = new WorkerRegister(configuration, processingManagementClientFactory);
    }

    @VisibleForTesting
    public WorkerRegistrationListener(
        WorkerConfiguration configuration,
        ProcessingManagementClientFactory processingManagementClientFactory
    ) {
        this.configuration = configuration;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workerRegister = new WorkerRegister(configuration, processingManagementClientFactory);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.debug("ServletContextListener started");
        executorService.scheduleWithFixedDelay(
            this.workerRegister,
            0,
            configuration.getRegisterDelay(),
            TimeUnit.SECONDS
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.debug("ServletContextListener destroyed");
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn(e);
        } finally {
            try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {
                processingClient.unregisterWorker(
                    configuration.getWorkerFamily(),
                    String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId())
                );
            } catch (final Exception e) {
                LOGGER.error(
                    "WorkerUnRegister run : unregister call failed => Processing (" +
                    configuration.getProcessingUrl() +
                    ") will unregister worker automatically ",
                    e
                );
            }
        }
    }
}
