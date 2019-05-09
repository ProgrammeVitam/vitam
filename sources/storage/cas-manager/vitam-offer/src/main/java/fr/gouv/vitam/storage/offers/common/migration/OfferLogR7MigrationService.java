/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.common.migration;

import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferService;

import java.util.concurrent.atomic.AtomicReference;

public class OfferLogR7MigrationService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferLogR7MigrationService.class);

    private final DefaultOfferService defaultOfferService;

    private final AtomicReference<OfferLogR7MigrationProcess> lastMigrationProcess = new AtomicReference<>(null);
    private final MongoDatabase mongoDatabase;


    public OfferLogR7MigrationService(DefaultOfferService defaultOfferService,
        MongoDatabase mongoDatabase) {
        this.defaultOfferService = defaultOfferService;
        this.mongoDatabase = mongoDatabase;
    }

    public boolean startMigration(Long startOffset) {

        OfferLogR7MigrationProcess migrationProcess = createMigrationProcess();

        OfferLogR7MigrationProcess currentMigrationProcess =
            lastMigrationProcess.updateAndGet((previousOfferSyncService) -> {
                if (previousOfferSyncService != null && previousOfferSyncService.isRunning()) {
                    return previousOfferSyncService;
                }
                return migrationProcess;
            });

        // Ensure no concurrent synchronization service running
        if (migrationProcess != currentMigrationProcess) {
            LOGGER.error("Another synchronization workflow is already running " + currentMigrationProcess.toString());
            return false;
        }

        LOGGER.info("Start the migration process...");
        runMigrationAsync(migrationProcess, startOffset);

        return true;
    }

    private void runMigrationAsync(OfferLogR7MigrationProcess migrationProcess, Long startOffset) {

        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        VitamThreadPoolExecutor.getDefaultExecutor().execute(
            () -> {
                try {
                    VitamThreadUtils.getVitamSession().setRequestId(requestId);
                    migrationProcess.run(startOffset);
                } catch (Exception e) {
                    LOGGER.error("An error occurred during offer migration process execution", e);
                }
            }
        );
    }

    private OfferLogR7MigrationProcess createMigrationProcess() {
        return new OfferLogR7MigrationProcess(this.defaultOfferService, this.mongoDatabase);
    }

    public boolean isMigrationRunning() {
        OfferLogR7MigrationProcess migrationProcess = lastMigrationProcess.get();
        return migrationProcess != null && migrationProcess.isRunning();
    }

    public OfferMigrationStatus getMigrationStatus() {
        OfferLogR7MigrationProcess migrationProcess = lastMigrationProcess.get();
        if (migrationProcess == null) {
            return null;
        } else {
            return migrationProcess.getMigrationStatus();
        }
    }
}
