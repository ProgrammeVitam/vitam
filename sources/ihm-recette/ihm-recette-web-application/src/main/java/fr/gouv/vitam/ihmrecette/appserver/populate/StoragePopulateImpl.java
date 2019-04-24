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

package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.stream.MultiplePipedInputStream;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.HotStrategy;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DeleteThread;
import fr.gouv.vitam.storage.engine.server.distribution.impl.ThreadResponseData;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TransferThread;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TryAndRetryData;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response.Status;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


/**
 * StoragePopulateImpl populate binary file
 */
public class StoragePopulateImpl implements VitamAutoCloseable {
    private static final int DEFAULT_MINIMUM_TIMEOUT = 60000;
    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoragePopulateImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    private static final int NB_RETRY = 3;

    /**
     * Global pool thread
     */
    static final ExecutorService executor = new VitamThreadPoolExecutor();

    /**
     * Used to wait for all task submission (executorService)
     */
    private static final long THREAD_SLEEP = 1;
    private final Integer millisecondsPerKB;
    private final DigestType digestType;



    /**
     * Constructs the service with a given configuration
     *
     * @param configuration configuration of storage server
     */
    public StoragePopulateImpl(StorageConfiguration configuration) {
        ParametersChecker.checkParameter("Storage service configuration is mandatory", configuration);
        String urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
        millisecondsPerKB = configuration.getTimeoutMsPerKB();
        digestType = VitamConfiguration.getDefaultDigestType();
    }

    private static StorageOffer apply(OfferReference offerReference) {
        StorageOffer storageOffer = null;
        try {
            storageOffer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        } catch (StorageException e) {
            LOGGER.error(e);
        }
        return storageOffer;
    }

    /**
     * createObjectDescription AND jsonData in the same params but
     * they should not be both resent at the same time. Maybe encapsulate or
     * create 2 methods
     *
     * @param strategyId strategyId
     * @param objectId objectId
     * @param file file
     * @param category category
     * @param tenantId tenantId
     * @return StoredInfoResult
     * @throws StorageException StorageException
     * @throws FileNotFoundException FileNotFoundException
     */
    public StoredInfoResult storeData(String strategyId, String objectId, File file,
        DataCategory category, int tenantId)
        throws StorageException, FileNotFoundException {
        checkStoreDataParams(strategyId, objectId, category);
        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }

            TryAndRetryData datas = new TryAndRetryData();

            List<StorageOffer> storageOffers = offerReferences.stream()
                .map(StoragePopulateImpl::apply)
                .collect(Collectors.toList());

            datas.populateFromOffers(storageOffers);

            tryAndRetry(objectId, category, file, tenantId, datas, 1);

            return buildStoreDataResponse(objectId, category,
                strategyId, datas.getGlobalOfferResult());
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private void tryAndRetry(String objectId, DataCategory category, File file,
        Integer tenantId, TryAndRetryData datas, int attempt)
        throws StorageTechnicalException, StorageNotFoundException,
        FileNotFoundException {
        Digest globalDigest = new Digest(digestType);
        InputStream digestInputStream = globalDigest.getDigestInputStream(new FileInputStream(file));
        Digest digest = new Digest(digestType);
        long finalTimeout = getTransferTimeout(file.getTotalSpace());
        try (MultiplePipedInputStream streams = getMultipleInputStreamFromWorkspace(digestInputStream,
            datas.getKoList().size(),
            digest)) {
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();
            int rank = 0;
            String offerId2 = null;
            try {
                for (final String offerId : datas.getKoList()) {
                    offerId2 = offerId;
                    OfferReference offerReference = new OfferReference();
                    offerReference.setId(offerId);
                    final Driver driver = retrieveDriverInternal(offerReference.getId());
                    InputStream inputStream = new BufferedInputStream(streams.getInputStream(rank));
                    StoragePutRequest request =
                        new StoragePutRequest(tenantId, category.getFolder(), objectId, digestType.getName(),
                            inputStream);
                    futureMap.put(offerReference.getId(),
                        executor
                            .submit(new TransferThread(driver, offerReference, request, globalDigest, file.length())));
                    rank++;
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Wrong number on wait on offer ID " + offerId2, e);
            } catch (StorageException e) {
                LOGGER.error("Interrupted on offer ID " + offerId2, e);
            }

            // wait all tasks submission
            try {
                Thread.sleep(THREAD_SLEEP);
            } catch (InterruptedException exc) {
                LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
                if (!datas.getKoList().isEmpty()) {
                    try {
                        deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
                    } catch (StorageTechnicalException e) {
                        LOGGER.error("Cannot delete object {}", objectId, e);
                        throw e;
                    }
                }
            }

            // wait for all threads execution
            // TODO: manage interruption and error execution (US #2008 && 2009)
            for (Entry<String, Future<ThreadResponseData>> entry : futureMap.entrySet()) {
                final Future<ThreadResponseData> future = entry.getValue();
                // Check if any has one IO Exception
                streams.throwLastException();
                String offerId = entry.getKey();
                try {
                    ThreadResponseData threadResponseData = future
                        .get(finalTimeout, TimeUnit.MILLISECONDS);
                    if (threadResponseData == null) {
                        LOGGER.error("Error on offer ID " + offerId);
                        throw new StorageTechnicalException("No message returned");
                    }
                    datas.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error("Interrupted after timeout on offer ID " + offerId);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted on offer ID " + offerId, e);
                } catch (ExecutionException e) {
                    LOGGER.error("Error on offer ID " + offerId, e);
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        status = Status.CONFLICT;
                        datas.changeStatus(offerId, status);
                    }
                    if (e.getCause() instanceof StorageDriverException ||
                        e.getCause() instanceof StorageDriverPreconditionFailedException) {
                        LOGGER.error("Error encountered is " + e.getCause().getClass() + ", no need to retry");
                        attempt = NB_RETRY;
                    }
                    // TODO: review this exception to manage errors correctly
                    // Take into account Exception class
                    // For example, for particular exception do not retry (because
                    // it's useless)
                    // US : #2009
                } catch (NumberFormatException e) {
                    future.cancel(true);
                    LOGGER.error("Wrong number on wait on offer ID " + offerId, e);
                }
            }
            // Check if any has one IO Exception
            streams.throwLastException();
        } catch (IOException e1) {
            LOGGER.error("Cannot create multipleInputStream", e1);
            throw new StorageTechnicalException("Cannot create multipleInputStream", e1);
        }
        // ACK to prevent retry
        if (attempt < NB_RETRY && !datas.getKoList().isEmpty()) {
            attempt++;
            tryAndRetry(objectId, category, file, tenantId, datas, attempt);
        }

        // TODO : error management (US #2009)
        if (!datas.getKoList().isEmpty()) {
            deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
        }
    }

    private long getTransferTimeout(long sizeToTransfer) {
        long timeout = (sizeToTransfer / 1024) * millisecondsPerKB;
        if (timeout < DEFAULT_MINIMUM_TIMEOUT) {
            return DEFAULT_MINIMUM_TIMEOUT;
        }
        return timeout;
    }

    private void isStrategyValid(HotStrategy hotStrategy) throws StorageTechnicalException {
        if (!hotStrategy.isCopyValid()) {
            throw new StorageTechnicalException("Invalid number of copy");
        }
    }

    private MultiplePipedInputStream getMultipleInputStreamFromWorkspace(InputStream stream, int nbCopy,
        Digest digest)
        throws StorageTechnicalException, StorageNotFoundException, IOException {
        DigestInputStream digestOriginalStream = (DigestInputStream) digest.getDigestInputStream(stream);
        return new MultiplePipedInputStream(digestOriginalStream, nbCopy);
    }

    private StoredInfoResult buildStoreDataResponse(String objectId, DataCategory category, String strategy,
        Map<String, Status> offerResults)
        throws StorageTechnicalException, StorageAlreadyExistsException {

        String digest = DigestType.SHA512.getName();
        final String offerIds = String.join(", ", offerResults.keySet());
        // Aggregate result of all store actions. If all went well, allSuccess is true, false if one action failed
        final boolean allWithoutInternalServerError = offerResults.entrySet().stream()
            .map(Entry::getValue)
            .noneMatch(Status.INTERNAL_SERVER_ERROR::equals);
        final boolean allWithoutAlreadyExists = offerResults.entrySet().stream()
            .map(Entry::getValue)
            .noneMatch(Status.CONFLICT::equals);

        if (!allWithoutInternalServerError) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CANT_STORE_OBJECT,
                objectId, offerIds));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CANT_STORE_OBJECT,
                objectId, offerIds));
        } else if (!allWithoutAlreadyExists) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS,
                objectId, offerIds));
            throw new StorageAlreadyExistsException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS,
                    objectId, offerIds));
        }

        // TODO P1 Witch status code return if an offer is updated (Status.OK)
        // and another is created (Status.CREATED) ?
        final StoredInfoResult result = new StoredInfoResult();
        final LocalDateTime now = LocalDateTime.now();
        final StringBuilder description = new StringBuilder();
        switch (category) {
            case UNIT:
                description.append("Unit ");
                break;
            case OBJECTGROUP:
                description.append("ObjectGroup ");
                break;
            case LOGBOOK:
                description.append("Logbook ");
                break;
            case OBJECT:
                description.append("Object ");
                break;
            case REPORT:
                description.append("Report ");
                break;
            case MANIFEST:
                description.append("Manifest ");
                break;
            case PROFILE:
                description.append("Profile ");
                break;
            case STORAGELOG:
                description.append("Storagelog ");
                break;
            case STORAGETRACEABILITY:
                description.append("STORAGETRACEABILITY");
                break;
            case RULES:
                description.append("Rules ");
                break;
            case DIP:
                description.append("DIP ");
                break;
            case AGENCIES:
                description.append("Agencies ");
                break;
            case BACKUP:
                description.append("Backup ");
                break;
            case BACKUP_OPERATION:
                description.append("Backup Operation ");
            case CHECKLOGBOOKREPORTS:
                description.append("CHECKLOGBOOKREPORTS ");
                break;
            case UNIT_GRAPH:
                description.append("UNIT_GRAPH ");
                break;
            case OBJECTGROUP_GRAPH:
                description.append("OBJECTGROUP_GRAPH ");
                break;
            default:
                throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
        }
        description.append("with id '");
        description.append(objectId);
        description.append("' stored successfully");
        result.setId(objectId);
        result.setInfo(description.toString());
        result.setCreationTime(LocalDateUtil.getString(now));
        result.setLastAccessTime(LocalDateUtil.getString(now));
        result.setLastCheckedTime(LocalDateUtil.getString(now));
        result.setLastModifiedTime(LocalDateUtil.getString(now));
        result.setNbCopy(offerResults.size());
        result.setStrategy(strategy);
        result.setOfferIds(Arrays.asList(offerResults.keySet().toArray(new String[0])));
        result.setDigestType(digestType.getName());
        result.setDigest(digest);
        LOGGER.debug("DEBUG result: {}", result);
        return result;
    }

    private Driver retrieveDriverInternal(String offerId) throws StorageTechnicalException {
        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    private void checkStoreDataParams(String strategyId, String dataId,
        DataCategory category) {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", dataId);
        ParametersChecker.checkParameter("Category is mandatory", category);
    }

    private List<OfferReference> choosePriorityOffers(HotStrategy hotStrategy) {
        final List<OfferReference> offerReferences = new ArrayList<>();
        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {
            // TODO P1 : this code will be changed in the future to handle
            // priority (not in current US scope) and copy
            offerReferences.addAll(hotStrategy.getOffers());
        }
        return offerReferences;
    }

    private void deleteObjects(List<String> offerIdList, Integer tenantId, DataCategory category, String objectId,
        Digest digest)
        throws StorageTechnicalException {
        // Map here to keep offerId linked to Future
        Map<String, Future<Boolean>> futureMap = new HashMap<>();
        for (String offerId : offerIdList) {
            final Driver driver = retrieveDriverInternal(offerId);
            // TODO: review if digest value is really good ?
            StorageRemoveRequest request =
                new StorageRemoveRequest(tenantId, category.getFolder(), objectId, digestType,
                    digest.digestHex());
            futureMap.put(offerId, executor.submit(new DeleteThread(driver, request, offerId)));
        }

        // wait all tasks submission
        try {
            Thread.sleep(THREAD_SLEEP, TimeUnit.MILLISECONDS.ordinal());
        } catch (InterruptedException exc) {
            LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
            throw new StorageTechnicalException("Object potentially not deleted: " + objectId, exc);
        }
        for (Entry<String, Future<Boolean>> entry : futureMap.entrySet()) {
            final Future<Boolean> future = entry.getValue();
            String offerId = entry.getKey();
            try {
                Boolean bool = future.get(DEFAULT_MINIMUM_TIMEOUT * 10, TimeUnit.MILLISECONDS);
                if (!bool) {
                    LOGGER.error("Object not deleted: {}", objectId);
                    throw new StorageTechnicalException("Object not deleted: " + objectId);
                }
            } catch (TimeoutException e) {
                LOGGER.error("Timeout on offer ID " + offerId, e);
                future.cancel(true);
                // TODO: manage thread to take into account this interruption
                LOGGER.error("Interrupted after timeout on offer ID " + offerId, e);
                throw new StorageTechnicalException("Object not deleted: " + objectId);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted on offer ID " + offerId, e);
                throw new StorageTechnicalException("Object not deleted: " + objectId, e);
            } catch (ExecutionException e) {
                LOGGER.error("Error on offer ID " + offerId, e);
                // TODO: review this exception to manage errors correctly
                // Take into account Exception class
                // For example, for particular exception do not retry (because
                // it's useless)
                // US : #2009
                throw new StorageTechnicalException("Object not deleted: " + objectId, e);
            } catch (NumberFormatException e) {
                future.cancel(true);
                LOGGER.error("Wrong number on wait on offer ID " + offerId, e);
                throw new StorageTechnicalException("Object not deleted: " + objectId, e);
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn(e);
        }
        executor.shutdownNow();
    }

    public static int getNbc() {
        try {
            return STRATEGY_PROVIDER.getStorageStrategy("default").getHotStrategy().getCopy();
        } catch (StorageTechnicalException e) {
            LOGGER.error(e);
            return 0;
        }
    }

    public static List<String> getOfferIds() {
        try {
            List<OfferReference> offerReferences =
                STRATEGY_PROVIDER.getStorageStrategy("default").getHotStrategy().getOffers();
            return offerReferences.stream().map(offer -> offer.getId()).collect(Collectors.toList());
        } catch (StorageTechnicalException e) {
            LOGGER.error(e);
        }

        return new ArrayList<>();
    }
}
