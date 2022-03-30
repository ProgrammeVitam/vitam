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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;

import java.util.Optional;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class IngestCleanupFinalizationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(IngestCleanupFinalizationPlugin.class);
    static final String INGEST_CLEANUP_FINALIZATION = "INGEST_CLEANUP_FINALIZATION";

    private final StorageClientFactory storageClientFactory;

    public IngestCleanupFinalizationPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public IngestCleanupFinalizationPlugin(
        StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {

        try (StorageClient storageClient = storageClientFactory.getClient()) {

            Optional<CleanupReportManager> cleanupReportManager =
                CleanupReportManager.loadReportDataFromWorkspace(handler);
            if (!cleanupReportManager.isPresent()) {
                LOGGER.warn("No report information available...");
                return buildItemStatus(INGEST_CLEANUP_FINALIZATION, StatusCode.WARNING);
            }

            cleanupReportManager.get().exportReport(handler, storageClient);

            LOGGER.info("Ingest cleanup finalization succeeded");
            return buildItemStatus(INGEST_CLEANUP_FINALIZATION, StatusCode.OK);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Ingest Cleanup finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(INGEST_CLEANUP_FINALIZATION, e.getStatusCode(), e.getEventDetails());
        }
    }
}
