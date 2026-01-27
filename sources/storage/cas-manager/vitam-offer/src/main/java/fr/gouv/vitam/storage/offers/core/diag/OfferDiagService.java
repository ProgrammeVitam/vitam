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

package fr.gouv.vitam.storage.offers.core.diag;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OfferDiagService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferDiagService.class);

    private final ContentAddressableStorage contentAddressableStorage;
    private final DefaultOfferService offerService;
    private final AtomicReference<OfferDiagProcess> lastOfferDiagProcess = new AtomicReference<>(null);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Constructor.
     */
    public OfferDiagService(ContentAddressableStorage contentAddressableStorage, DefaultOfferService offerService) {
        this.contentAddressableStorage = contentAddressableStorage;
        this.offerService = offerService;
    }

    /**
     * Start a diagnostic process for the given container and tenant.
     *
     * @param container the container name
     * @return true if the process was started, false if another process is already running
     */
    public boolean startOfferDiag(String container) {
        if (!isRunning.compareAndSet(false, true)) {
            LOGGER.warn("Another offer diagnostic process is already running");
            return false;
        }

        DataCategory dataCategory = DataCategory.getByCollectionName(container);
        String containerName = ContainerUtils.buildContainerName(
            dataCategory,
            VitamThreadUtils.getVitamSession().getTenantId() + ""
        );

        OfferDiagProcess offerDiagProcess = createOfferDiagProcess(containerName);
        lastOfferDiagProcess.set(offerDiagProcess);

        // Run the diagnostic process in a separate thread
        runDiagAsync(offerDiagProcess);

        LOGGER.info("Offer diagnostic started");
        return true;
    }

    /**
     * Run the diagnostic process asynchronously.
     *
     * @param offerDiagProcess the process to run
     */
    private void runDiagAsync(OfferDiagProcess offerDiagProcess) {
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    VitamThreadUtils.getVitamSession().setRequestId(requestId);
                    Thread.currentThread()
                        .setName("offer-diag-main-process-" + offerDiagProcess.getOfferDiagStatus().getContainer());

                    offerDiagProcess.run();
                    // Only set isRunning to false after the process has completed successfully

                } catch (Exception e) {
                    LOGGER.error("An error occurred during offer diff process execution", e);
                } finally {
                    isRunning.set(false);
                }
            });
    }

    /**
     * Create a new diagnostic process.
     *
     * @param containerName the container name
     * @return the new process
     */
    private OfferDiagProcess createOfferDiagProcess(String containerName) {
        return new OfferDiagProcess(contentAddressableStorage, offerService, containerName);
    }

    /**
     * Check if a diagnostic process is currently running.
     *
     * @return true if a process is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Get the status of the last diagnostic process.
     *
     * @return the status, or null if no process has been run
     */
    public OfferDiagStatus getLastOfferDiagStatus() {
        OfferDiagProcess process = lastOfferDiagProcess.get();
        return process != null ? process.getOfferDiagStatus() : null;
    }
}
