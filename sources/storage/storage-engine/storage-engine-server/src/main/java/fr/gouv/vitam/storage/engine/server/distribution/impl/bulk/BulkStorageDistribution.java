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
package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TransfertTimeoutHelper;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BulkStorageDistribution {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BulkStorageDistribution.class);
    private static final String ATTEMPT = " attempt ";
    private static final int MIN_SLEEP_DELAY_BEFORE_RETRY = 10_000;
    private static final int MAX_SLEEP_DELAY_BEFORE_RETRY = 30_000;

    private final int nbReties;
    private final DigestType digestType;
    private final StorageLog storageLogService;
    private final BulkPutTransferManager bulkPutTransferManager;
    private final int minSleepDelayBeforeRetry;
    private final int maxSleepDelayBeforeRetry;

    public BulkStorageDistribution(
        int nbReties,
        WorkspaceClientFactory workspaceClientFactory,
        StorageLog storageLogService,
        TransfertTimeoutHelper transfertTimeoutHelper
    ) {
        this(
            nbReties,
            storageLogService,
            VitamConfiguration.getDefaultDigestType(),
            new BulkPutTransferManager(workspaceClientFactory, transfertTimeoutHelper),
            MIN_SLEEP_DELAY_BEFORE_RETRY,
            MAX_SLEEP_DELAY_BEFORE_RETRY
        );
    }

    @VisibleForTesting
    BulkStorageDistribution(
        int nbReties,
        StorageLog storageLogService,
        DigestType digestType,
        BulkPutTransferManager bulkPutTransferManager,
        int minSleepDelayBeforeRetry,
        int maxSleepDelayBeforeRetry
    ) {
        this.nbReties = nbReties;
        this.storageLogService = storageLogService;
        this.digestType = digestType;
        this.bulkPutTransferManager = bulkPutTransferManager;
        this.minSleepDelayBeforeRetry = minSleepDelayBeforeRetry;
        this.maxSleepDelayBeforeRetry = maxSleepDelayBeforeRetry;
    }

    public Map<String, String> bulkCreateFromWorkspaceWithRetries(
        String strategyId,
        int tenantId,
        List<String> allOfferIds,
        Map<String, Driver> storageDrivers,
        Map<String, StorageOffer> storageOffers,
        DataCategory dataCategory,
        String workspaceContainerGUID,
        List<String> workspaceObjectURIs,
        List<String> objectIds,
        String requester
    ) throws StorageException {
        List<String> remainingOfferIds = new ArrayList<>(allOfferIds);
        Map<String, ObjectInfo> objectInfos = null;

        List<String> events = new ArrayList<>();

        try {
            for (int attempt = 1; attempt <= nbReties; attempt++) {
                BulkPutResult bulkOutResult = bulkPutTransferManager.bulkSendDataToOffers(
                    workspaceContainerGUID,
                    strategyId,
                    attempt,
                    tenantId,
                    dataCategory,
                    remainingOfferIds,
                    storageDrivers,
                    storageOffers,
                    workspaceObjectURIs,
                    objectIds
                );

                if (bulkOutResult.getObjectInfos() != null) {
                    objectInfos = bulkOutResult
                        .getObjectInfos()
                        .stream()
                        .collect(Collectors.toMap(ObjectInfo::getObjectId, objectInfo -> objectInfo));
                }

                for (Map.Entry<String, OfferBulkPutStatus> entry : bulkOutResult.getStatusByOfferIds().entrySet()) {
                    String offerId = entry.getKey();
                    OfferBulkPutStatus status = entry.getValue();

                    events.add(offerId + ATTEMPT + attempt + " : " + status);

                    if (status == OfferBulkPutStatus.OK) {
                        remainingOfferIds.remove(offerId);
                    }
                }

                boolean hasBlockerError = bulkOutResult
                    .getStatusByOfferIds()
                    .values()
                    .contains(OfferBulkPutStatus.BLOCKER);
                if (hasBlockerError) {
                    throw new StorageInconsistentStateException("A fatal error occurred during bulk object storage");
                }

                if (remainingOfferIds.isEmpty()) {
                    // Build response
                    return objectInfos
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDigest()));
                }

                sleepBeforeRetry(remainingOfferIds);
            }

            throw new StorageException("Could not proceed bulk put operation after " + nbReties + " attempts");
        } finally {
            // Log events to storage log
            logStorageEvents(tenantId, dataCategory, objectIds, requester, remainingOfferIds, objectInfos, events);
        }
    }

    private void sleepBeforeRetry(List<String> remainingOfferIds) throws StorageException {
        int sleepDelay = ThreadLocalRandom.current().nextInt(minSleepDelayBeforeRetry, maxSleepDelayBeforeRetry);
        LOGGER.warn("Will retry writing to " + remainingOfferIds + " in " + sleepDelay + "ms");
        try {
            Thread.sleep(sleepDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Bulk write to offers interrupted", e);
        }
    }

    private void logStorageEvents(
        Integer tenantId,
        DataCategory dataCategory,
        List<String> objectIds,
        String requester,
        List<String> remainingOfferIds,
        Map<String, ObjectInfo> objectInfos,
        List<String> events
    ) {
        for (String objectId : objectIds) {
            StorageLogbookParameters parameters = StorageLogbookParameters.createLogParameters(
                objectId,
                dataCategory.getFolder(),
                objectInfos != null ? objectInfos.get(objectId).getDigest() : null,
                digestType.getName(),
                objectInfos != null ? Long.toString(objectInfos.get(objectId).getSize()) : null,
                String.join(", ", events),
                requester,
                remainingOfferIds.isEmpty() ? StorageLogbookOutcome.OK : StorageLogbookOutcome.KO
            );

            try {
                storageLogService.appendWriteLog(tenantId, parameters);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }
}
