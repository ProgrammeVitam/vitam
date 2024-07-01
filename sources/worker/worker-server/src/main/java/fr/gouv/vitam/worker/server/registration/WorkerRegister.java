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
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerConfiguration;

/**
 * Worker register task : register the current worker server to the processing server.
 */
public class WorkerRegister implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerRegister.class);

    /**
     * Worker configuration used to retrieve the register configuration
     */
    private final WorkerConfiguration configuration;

    public static final String DEFAULT_FAMILY = "DefaultWorker";

    private final ProcessingManagementClientFactory processingManagementClientFactory;

    @VisibleForTesting
    public WorkerRegister(
        WorkerConfiguration configuration,
        ProcessingManagementClientFactory processingManagementClientFactory
    ) {
        this.configuration = configuration;
        this.processingManagementClientFactory = processingManagementClientFactory;
    }

    @Override
    public synchronized void run() {
        LOGGER.debug("WorkerRegister run : begin");

        final WorkerRemoteConfiguration remoteConfiguration = new WorkerRemoteConfiguration(
            configuration.getRegisterServerHost(),
            configuration.getRegisterServerPort()
        );

        final WorkerBean workerBean = new WorkerBean(
            ServerIdentity.getInstance().getName(),
            configuration.getWorkerFamily(),
            configuration.getCapacity(),
            "active",
            remoteConfiguration
        );

        try (ProcessingManagementClient processingClient = processingManagementClientFactory.getClient()) {
            processingClient.registerWorker(
                configuration.getWorkerFamily(),
                String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()),
                workerBean
            );
        } catch (final Exception e) {
            LOGGER.error(
                "WorkerRegister failed (" +
                configuration.getProcessingUrl() +
                ").Retry in " +
                configuration.getRegisterDelay() +
                " seconds",
                e
            );
        }

        LOGGER.debug("WorkerRegister run : end");
    }
}
