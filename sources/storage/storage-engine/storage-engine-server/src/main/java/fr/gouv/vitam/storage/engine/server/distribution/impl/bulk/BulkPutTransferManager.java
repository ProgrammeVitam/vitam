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
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultiplePipedInputStream;
import fr.gouv.vitam.common.stream.PrependedMultiplexedInputStream;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.metrics.UploadCountingInputStreamMetrics;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StreamAndInfo;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TimeoutStopwatch;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TransfertTimeoutHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toMap;

/**
 * Handles single bulk transfer from workspace to offers. No retries are handled.
 */
class BulkPutTransferManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BulkPutTransferManager.class);
    public static final String BULK_ORIGIN = "bulk";

    private final WorkspaceClientFactory workspaceClientFactory;
    private final DigestType digestType;
    private final AlertService alertService;
    private final ExecutorService executor;
    private final TransfertTimeoutHelper transfertTimeoutHelper;

    BulkPutTransferManager(
        WorkspaceClientFactory workspaceClientFactory,
        TransfertTimeoutHelper transfertTimeoutHelper
    ) {
        this(
            workspaceClientFactory,
            VitamConfiguration.getDefaultDigestType(),
            new AlertServiceImpl(),
            VitamThreadPoolExecutor.getDefaultExecutor(),
            transfertTimeoutHelper
        );
    }

    @VisibleForTesting
    BulkPutTransferManager(
        WorkspaceClientFactory workspaceClientFactory,
        DigestType digestType,
        AlertService alertService,
        ExecutorService executor,
        TransfertTimeoutHelper transfertTimeoutHelper
    ) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.digestType = digestType;
        this.alertService = alertService;
        this.executor = executor;
        this.transfertTimeoutHelper = transfertTimeoutHelper;
    }

    BulkPutResult bulkSendDataToOffers(
        String workspaceContainerGUID,
        String strategyId,
        int attempts,
        int tenantId,
        DataCategory dataCategory,
        List<String> offerIds,
        Map<String, Driver> storageDrivers,
        Map<String, StorageOffer> storageOffers,
        List<String> workspaceObjectURIs,
        List<String> objectIds
    ) {
        ResultOrError<StreamAndInfo, BulkPutResult> streamWithInfoFromWorkspace = null;
        ResultOrError<StreamAndInfo, BulkPutResult> prependedStreamWithInfo = null;
        MultiplePipedInputStream streams = null;
        List<Future<StorageBulkPutResult>> transferThreadFutures = null;
        Future<List<ObjectInfo>> digestListenerFuture = null;

        try {
            // Get multiplexed stream from workspace
            streamWithInfoFromWorkspace = retrieveBulkDataFromWorkspace(
                workspaceContainerGUID,
                workspaceObjectURIs,
                offerIds
            );
            if (streamWithInfoFromWorkspace.hasError()) {
                return streamWithInfoFromWorkspace.getError();
            }

            // Prepend with header entry containing object Ids
            prependedStreamWithInfo = prependWithObjectIdsHeaderEntry(
                objectIds,
                streamWithInfoFromWorkspace.getResult(),
                offerIds
            );
            if (prependedStreamWithInfo.hasError()) {
                return prependedStreamWithInfo.getError();
            }

            // We need 1 thread per offer + 1 thread for digest computing
            streams = new MultiplePipedInputStream(
                prependedStreamWithInfo.getResult().getStream(),
                offerIds.size() + 1
            );

            String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            transferThreadFutures = startTransferThreads(
                strategyId,
                attempts,
                tenantId,
                requestId,
                dataCategory,
                objectIds,
                offerIds,
                storageDrivers,
                storageOffers,
                streams,
                prependedStreamWithInfo.getResult().getSize()
            );

            digestListenerFuture = startDigestComputeThread(offerIds, streams, objectIds);

            // Await termination with timeout
            long finalTimeout = transfertTimeoutHelper.getBulkTransferTimeout(
                prependedStreamWithInfo.getResult().getSize(),
                objectIds.size()
            );
            TimeoutStopwatch timeoutStopwatch = new TimeoutStopwatch(finalTimeout);

            ResultOrError<List<ObjectInfo>, BulkPutResult> objectInfo = awaitDigestListenerThread(
                digestListenerFuture,
                timeoutStopwatch,
                offerIds
            );
            if (objectInfo.hasError()) {
                return objectInfo.getError();
            }

            HashMap<String, OfferBulkPutStatus> statusByOfferIds = new HashMap<>();
            for (int rank = 0; rank < offerIds.size(); rank++) {
                String offerId = offerIds.get(rank);
                Future<StorageBulkPutResult> transferThreadFuture = transferThreadFutures.get(rank);

                OfferBulkPutStatus status = awaitTransferThread(
                    timeoutStopwatch,
                    objectInfo.getResult(),
                    offerId,
                    transferThreadFuture
                );

                statusByOfferIds.put(offerId, status);
            }
            return new BulkPutResult(objectInfo.getResult(), statusByOfferIds);
        } finally {
            if (transferThreadFutures != null) {
                for (Future<StorageBulkPutResult> transferThreadFuture : transferThreadFutures) {
                    transferThreadFuture.cancel(true);
                }
            }

            if (digestListenerFuture != null) {
                digestListenerFuture.cancel(true);
            }

            if (streams != null) {
                streams.close();
            }
            if (prependedStreamWithInfo != null && prependedStreamWithInfo.hasResult()) {
                prependedStreamWithInfo.getResult().close();
            }
            if (streamWithInfoFromWorkspace != null && streamWithInfoFromWorkspace.hasResult()) {
                streamWithInfoFromWorkspace.getResult().close();
            }
        }
    }

    private ResultOrError<StreamAndInfo, BulkPutResult> retrieveBulkDataFromWorkspace(
        String containerGUID,
        List<String> workspaceObjectURIs,
        List<String> offerIds
    ) {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            Response response = workspaceClient.bulkGetObjects(containerGUID, workspaceObjectURIs);
            Long size = Long.valueOf(response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
            return ResultOrError.result(new StreamAndInfo(new VitamAsyncInputStream(response), size));
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID), e);
            return reportGlobalBlockerFailure(offerIds);
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            return reportGlobalNonBlockerFailure(offerIds);
        }
    }

    private ResultOrError<StreamAndInfo, BulkPutResult> prependWithObjectIdsHeaderEntry(
        List<String> objectIds,
        StreamAndInfo streamAndInfo,
        List<String> offerIds
    ) {
        try {
            // Write object ids as first entry
            ByteArrayOutputStream headerEntry = new ByteArrayOutputStream();
            JsonHandler.writeAsOutputStream(objectIds, headerEntry);

            PrependedMultiplexedInputStream multiplexedInputStreamWithHeader = new PrependedMultiplexedInputStream(
                headerEntry.toInputStream(),
                headerEntry.size(),
                streamAndInfo.getStream(),
                streamAndInfo.getSize()
            );

            return ResultOrError.result(
                new StreamAndInfo(multiplexedInputStreamWithHeader, multiplexedInputStreamWithHeader.size())
            );
        } catch (IOException | InvalidParseOperationException e) {
            LOGGER.error("Could not prepend header entry", e);
            return reportGlobalNonBlockerFailure(offerIds);
        }
    }

    private Future<List<ObjectInfo>> startDigestComputeThread(
        List<String> offerIds,
        MultiplePipedInputStream streams,
        List<String> objectIds
    ) {
        return executor.submit(
            new MultiplexedStreamObjectInfoListenerThread(
                VitamThreadUtils.getVitamSession().getTenantId(),
                VitamThreadUtils.getVitamSession().getRequestId(),
                streams.getInputStream(offerIds.size()),
                digestType,
                objectIds
            )
        );
    }

    private List<Future<StorageBulkPutResult>> startTransferThreads(
        String strategyId,
        int attempts,
        int tenantId,
        String requestId,
        DataCategory dataCategory,
        List<String> objectIds,
        List<String> offerIds,
        Map<String, Driver> storageDrivers,
        Map<String, StorageOffer> storageOffers,
        MultiplePipedInputStream streams,
        long size
    ) {
        List<Future<StorageBulkPutResult>> transferThreadFutures = new ArrayList<>();
        for (int rank = 0; rank < offerIds.size(); rank++) {
            String offerId = offerIds.get(rank);
            Driver driver = storageDrivers.get(offerId);
            StorageOffer storageOffer = storageOffers.get(offerId);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(streams.getInputStream(rank));
            InputStream offerInputStream = new UploadCountingInputStreamMetrics(
                tenantId,
                strategyId,
                offerId,
                BULK_ORIGIN,
                dataCategory,
                attempts,
                bufferedInputStream
            );

            transferThreadFutures.add(
                executor.submit(
                    new MultiplexedStreamTransferThread(
                        tenantId,
                        requestId,
                        dataCategory,
                        objectIds,
                        offerInputStream,
                        size,
                        driver,
                        storageOffer,
                        this.digestType
                    )
                )
            );
        }
        return transferThreadFutures;
    }

    private ResultOrError<List<ObjectInfo>, BulkPutResult> awaitDigestListenerThread(
        Future<List<ObjectInfo>> digestListenerFuture,
        TimeoutStopwatch timeoutStopwatch,
        List<String> offerIds
    ) {
        try {
            return ResultOrError.result(
                digestListenerFuture.get(timeoutStopwatch.getRemainingDelayInMilliseconds(), TimeUnit.MILLISECONDS)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted thread", e);
            return reportGlobalBlockerFailure(offerIds);
        } catch (Exception e) {
            LOGGER.error("Could not compute object information for all object ids", e);
            return reportGlobalNonBlockerFailure(offerIds);
        }
    }

    private OfferBulkPutStatus awaitTransferThread(
        TimeoutStopwatch timeoutStopwatch,
        List<ObjectInfo> objectInfos,
        String offerId,
        Future<StorageBulkPutResult> transferThreadFuture
    ) {
        try {
            StorageBulkPutResult storageBulkPutResult = transferThreadFuture.get(
                timeoutStopwatch.getRemainingDelayInMilliseconds(),
                TimeUnit.MILLISECONDS
            );

            validationResponseDigestConsistency(objectInfos, offerId, storageBulkPutResult);

            return OfferBulkPutStatus.OK;
        } catch (StorageInconsistentStateException e) {
            LOGGER.error(e);
            alertService.createAlert(VitamLogLevel.ERROR, e.getMessage());
            return OfferBulkPutStatus.BLOCKER;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted thread", e);
            return OfferBulkPutStatus.BLOCKER;
        } catch (ExecutionException e) {
            LOGGER.error("Transfer thread failed for offer " + offerId, e);
            if (
                e.getCause() instanceof StorageDriverException &&
                !((StorageDriverException) e.getCause()).isShouldRetry()
            ) {
                return OfferBulkPutStatus.BLOCKER;
            }
            return OfferBulkPutStatus.KO;
        } catch (TimeoutException | RuntimeException e) {
            LOGGER.error("Transfer thread failed for offer " + offerId, e);
            return OfferBulkPutStatus.KO;
        }
    }

    private void validationResponseDigestConsistency(
        List<ObjectInfo> objectInfos,
        String offerId,
        StorageBulkPutResult storageBulkPutResult
    ) throws StorageInconsistentStateException {
        if (storageBulkPutResult.getEntries().size() != objectInfos.size()) {
            throw new StorageInconsistentStateException(
                "Bulk put operation failed for offer " +
                offerId +
                ". " +
                "Invalid result size. Expected " +
                objectInfos.size() +
                "." +
                " Received from offer " +
                storageBulkPutResult.getEntries().size()
            );
        }
        for (int i = 0; i < objectInfos.size(); i++) {
            StorageBulkPutResultEntry bulkPutResultEntry = storageBulkPutResult.getEntries().get(i);
            if (!objectInfos.get(i).getObjectId().equals(bulkPutResultEntry.getObjectId())) {
                throw new StorageInconsistentStateException(
                    "Bulk put operation failed for offer " +
                    offerId +
                    ". " +
                    "Invalid object id. Expected '" +
                    objectInfos.get(i).getObjectId() +
                    "'. Received from offer '" +
                    bulkPutResultEntry.getObjectId() +
                    "'"
                );
            }
            if (!objectInfos.get(i).getDigest().equals(bulkPutResultEntry.getDigest())) {
                throw new StorageInconsistentStateException(
                    "Bulk put operation failed for offer " +
                    offerId +
                    ". " +
                    "Invalid digest for object '" +
                    objectInfos.get(i).getObjectId() +
                    "'. " +
                    "Expected " +
                    objectInfos.get(i).getDigest() +
                    ". " +
                    "Received from offer " +
                    bulkPutResultEntry.getDigest()
                );
            }
        }
    }

    private <T> ResultOrError<T, BulkPutResult> reportGlobalBlockerFailure(List<String> offerIds) {
        return ResultOrError.error(
            new BulkPutResult(
                null,
                offerIds.stream().collect(toMap(offerId -> offerId, offerId -> OfferBulkPutStatus.BLOCKER))
            )
        );
    }

    private <T> ResultOrError<T, BulkPutResult> reportGlobalNonBlockerFailure(List<String> offerIds) {
        return ResultOrError.error(
            new BulkPutResult(
                null,
                offerIds.stream().collect(toMap(offerId -> offerId, offerId -> OfferBulkPutStatus.KO))
            )
        );
    }
}
