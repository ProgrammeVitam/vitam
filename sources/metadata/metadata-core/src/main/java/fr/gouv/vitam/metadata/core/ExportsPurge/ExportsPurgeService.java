/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.ExportsPurge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.core.config.TimeToLiveConfiguration;
import fr.gouv.vitam.storage.engine.client.OfferLogHelper;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import static fr.gouv.vitam.common.model.WorkspaceConstants.FREESPACE;

public class ExportsPurgeService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExportsPurgeService.class);

    public static final String DIP_CONTAINER ="DIP";
    public static final String TRANSFERS_CONTAINER ="TRANSFER";

    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final TimeToLiveConfiguration timeToLiveConfiguration;

    public ExportsPurgeService(TimeToLiveConfiguration timeToLiveConfiguration) {
        this(WorkspaceClientFactory.getInstance(), StorageClientFactory.getInstance(), timeToLiveConfiguration);
    }

    @VisibleForTesting
    public ExportsPurgeService(WorkspaceClientFactory workspaceClientFactory, StorageClientFactory storageClientFactory,
        TimeToLiveConfiguration timeToLiveConfiguration) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.timeToLiveConfiguration = timeToLiveConfiguration;
    }

    public void purgeExpiredFiles(String container) throws ContentAddressableStorageServerException {
        try (WorkspaceClient workspaceClient = this.workspaceClientFactory.getClient()) {
            int timeToLiveInMinutes = getTimeToLiveInMinutes(container, workspaceClient);
            workspaceClient.purgeOldFilesInContainer(container,
                new TimeToLive(timeToLiveInMinutes, ChronoUnit.MINUTES));
        } catch (VitamClientException e) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    private int getTimeToLiveInMinutes(String container, WorkspaceClient workspaceClient) throws VitamClientException {
        JsonNode percent = workspaceClient.getFreespacePercent();
        int freespace = percent.get(FREESPACE).asInt();
        if(container.equals(DIP_CONTAINER)) {
            if (freespace <= VitamConfiguration.getWorkspaceFreespaceThreshold()) {
                return timeToLiveConfiguration.getCriticalDipTimeToLiveInMinutes();
            } else {
                return timeToLiveConfiguration.getDipTimeToLiveInMinutes();
            }
        } else {
            return timeToLiveConfiguration.getTransfersSIPTimeToLiveInMinutes();
        }
    }

    public void migrationPurgeDipFilesFromOffers() throws StorageServerClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {

            Iterator<OfferLog> offerLogIterator = OfferLogHelper.getListing(
                storageClientFactory, VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, null,
                Order.ASC, VitamConfiguration.getChunkSize(), null);

            while(offerLogIterator.hasNext()) {
                OfferLog offerLog = offerLogIterator.next();
                switch (offerLog.getAction()) {
                    case WRITE:
                        LOGGER.info("Deleting DIP file " + offerLog.getFileName());
                        storageClient.delete(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, offerLog.getFileName());
                        break;
                    case DELETE:
                        // NOP
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + offerLog.getAction());
                }
            }

        }
    }
}
