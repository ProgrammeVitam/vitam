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

package fr.gouv.vitam.storage.engine.server.offerdiff;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

import java.util.concurrent.atomic.AtomicReference;

public class OfferDiffService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferDiffService.class);

    private final StorageDistribution distribution;

    private final AtomicReference<OfferDiffProcess> lastOfferDiffProcess = new AtomicReference<>(null);

    /**
     * Constructor.
     */
    public OfferDiffService(StorageDistribution distribution) {
        this.distribution = distribution;
    }

    public boolean startOfferDiff(String offer1, String offer2, DataCategory dataCategory) {
        OfferDiffProcess offerDiffProcess = createOfferDiffProcess(offer1, offer2, dataCategory);

        OfferDiffProcess currentOfferDiffProcess = lastOfferDiffProcess.updateAndGet(previousOfferDiffService -> {
            if (previousOfferDiffService != null && previousOfferDiffService.isRunning()) {
                return previousOfferDiffService;
            }
            return offerDiffProcess;
        });

        // Ensure no concurrent process running
        if (offerDiffProcess != currentOfferDiffProcess) {
            LOGGER.error("Another offer diff process is already running " + currentOfferDiffProcess.toString());
            return false;
        }

        runDiffAsync(offerDiffProcess);

        return true;
    }

    void runDiffAsync(OfferDiffProcess offerDiffProcess) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    VitamThreadUtils.getVitamSession().setRequestId(requestId);

                    offerDiffProcess.run();
                } catch (Exception e) {
                    LOGGER.error("An error occurred during offer diff process execution", e);
                }
            });
    }

    OfferDiffProcess createOfferDiffProcess(String offer1, String offer2, DataCategory dataCategory) {
        return new OfferDiffProcess(distribution, offer1, offer2, dataCategory);
    }

    public boolean isRunning() {
        OfferDiffProcess offerDiffProcess = lastOfferDiffProcess.get();
        return offerDiffProcess != null && offerDiffProcess.isRunning();
    }

    public OfferDiffStatus getLastOfferDiffStatus() {
        OfferDiffProcess offerDiffProcess = lastOfferDiffProcess.get();
        if (offerDiffProcess == null) {
            return null;
        } else {
            return offerDiffProcess.getOfferDiffStatus();
        }
    }
}
