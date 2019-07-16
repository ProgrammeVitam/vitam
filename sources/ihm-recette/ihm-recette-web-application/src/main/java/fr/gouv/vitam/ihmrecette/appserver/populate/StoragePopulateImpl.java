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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.Collection;
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

import javax.ws.rs.core.Response.Status;

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
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DeleteThread;
import fr.gouv.vitam.storage.engine.server.distribution.impl.ExportThread;
import fr.gouv.vitam.storage.engine.server.distribution.impl.OffersToCopyIn;
import fr.gouv.vitam.storage.engine.server.distribution.impl.ThreadResponseData;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TransferThread;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

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
    private static final int NB_RETRY = 3;

    /**
     * Global pool thread
     */
    static final ExecutorService executor = new VitamThreadPoolExecutor();

    private static final String WRONG_NUMBER_ON_WAIT_ON_OFFER_ID = "Wrong number on wait on offer ID ";
    private static final String INTERRUPTED_ON_OFFER_ID = "Interrupted on offer ID ";
    private static final String ERROR_ON_OFFER_ID = "Error on offer ID ";
    private static final String CANNOT_CREATE_MULTIPLE_INPUT_STREAM = "Cannot create multipleInputStream";
    private static final String NO_MESSAGE_RETURNED = "No message returned";
    private static final String INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID = "Interrupted after timeout on offer ID ";
    private static final String OBJECT_NOT_DELETED = "Object not deleted: ";
    private static final String TIMEOUT_ON_OFFER_ID = "Timeout on offer ID ";
    private static final String OBJECT_ID_IS_MANDATORY = "Object id is mandatory";
    private static final String CATEGORY_IS_MANDATORY = "Category is mandatory";
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
     * @param objectId   objectId
     * @param file       file
     * @param category   category
     * @param tenantId   tenantId
     * @return StoredInfoResult
     * @throws StorageException      StorageException
     * @throws FileNotFoundException FileNotFoundException
     */
    public void storeData(String strategyId, String objectId, File file,
        DataCategory category, int tenantId)
        throws StorageException, FileNotFoundException {
        checkStoreDataParams(strategyId, objectId, category);

        final List<OfferReference> offerReferences = getOffersReferences(strategyId);
        if (offerReferences.isEmpty()) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }

        List<StorageOffer> storageOffers = offerReferences.stream()
            .map(StoragePopulateImpl::apply)
            .collect(Collectors.toList());
        OffersToCopyIn offers = new OffersToCopyIn(storageOffers);

        tryAndRetry(objectId, category, file, tenantId, offers, 1, StorageAction.PUT);
    }

    public void exportData(String strategyId, String objectId, DataCategory category, int tenantId)
            throws StorageException, FileNotFoundException {
        checkStoreDataParams(strategyId, objectId, category);

        final List<OfferReference> offerReferences = getOffersReferences(strategyId);
        if (offerReferences.isEmpty()) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }

        List<StorageOffer> storageOffers = offerReferences.stream()
                .map(StoragePopulateImpl::apply)
                .collect(Collectors.toList());
        OffersToCopyIn offers = new OffersToCopyIn(storageOffers);

        tryAndRetry(objectId, category, null, tenantId, offers, 1, StorageAction.GET);
    }

    private void tryAndRetry(String objectId, DataCategory category, File file,
        Integer tenantId, OffersToCopyIn data, int attempt, StorageAction action)
        throws StorageTechnicalException, FileNotFoundException {
        Digest globalDigest = new Digest(digestType);
        InputStream digestInputStream = file != null ? globalDigest.getDigestInputStream(new FileInputStream(file)) : null;
        Digest digest = new Digest(digestType);
        long finalTimeout = file != null ? getTransferTimeout(file.getTotalSpace()) : DEFAULT_MINIMUM_TIMEOUT;
        MultiplePipedInputStream streams;
        try {
            streams = action == StorageAction.PUT ? 
                        getMultipleInputStreamFromWorkspace(digestInputStream, data.getKoOffers().size(), digest) : 
                        null;
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();
            int rank = 0;
            String offerId2 = null;
            try {
                for (final String offerId : data.getKoOffers()) {
                    offerId2 = offerId;
                    OfferReference offerReference = new OfferReference(offerId);
                    final Driver driver = retrieveDriverInternal(offerReference.getId());
                    switch (action) {
                        case PUT:
                            InputStream inputStream = new BufferedInputStream(streams.getInputStream(rank));
                            StoragePutRequest request =
                                    new StoragePutRequest(tenantId, category.getFolder(), objectId, digestType.getName(),
                                            inputStream);
                            futureMap.put(offerReference.getId(),
                                    executor
                                            .submit(new TransferThread(driver, offerReference, request, globalDigest, file.length())));
                            rank++;
                            break;
                        case GET:
                            futureMap.put(offerReference.getId(),
                                    executor
                                            .submit(new ExportThread(driver, offerReference,
                                                    new StorageObjectRequest(tenantId, category.getFolder(), objectId))));
                            break;
                        default:
                            throw new IllegalArgumentException("Action not implemented");
                    }

                }
            } catch (NumberFormatException e) {
                LOGGER.error(WRONG_NUMBER_ON_WAIT_ON_OFFER_ID + offerId2, e);
            } catch (StorageException e) {
                LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerId2, e);
            }

            // wait for all threads execution
            // TODO: manage interruption and error execution (US #2008 && 2009)
            for (Entry<String, Future<ThreadResponseData>> entry : futureMap.entrySet()) {
                final Future<ThreadResponseData> future = entry.getValue();
                // Check if any has one IO Exception
                if (streams != null) {
                    streams.throwLastException();
                }
                String offerId = entry.getKey();
                try {
                    ThreadResponseData threadResponseData = future
                        .get(finalTimeout, TimeUnit.MILLISECONDS);
                    if (threadResponseData == null) {
                        LOGGER.error(ERROR_ON_OFFER_ID + offerId);
                        throw new StorageTechnicalException(NO_MESSAGE_RETURNED);
                    }
                    data.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error(INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID + offerId);
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerId, e);
                } catch (ExecutionException e) {
                    LOGGER.error(StoragePopulateImpl.ERROR_ON_OFFER_ID + offerId, e);
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        data.changeStatus(offerId, Status.CONFLICT);
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
                    LOGGER.error(WRONG_NUMBER_ON_WAIT_ON_OFFER_ID + offerId, e);
                }
            }
            // Check if any has one IO Exception
            if (streams != null) {
                streams.throwLastException();
            }
        } catch (IOException e1) {
            LOGGER.error(CANNOT_CREATE_MULTIPLE_INPUT_STREAM, e1);
            throw new StorageTechnicalException(CANNOT_CREATE_MULTIPLE_INPUT_STREAM, e1);
        }
        // ACK to prevent retry
        if (attempt < NB_RETRY && !data.getKoOffers().isEmpty()) {
            attempt++;
            tryAndRetry(objectId, category, file, tenantId, data, attempt, action);
        }

        // TODO : error management (US #2009)
        if (!data.getKoOffers().isEmpty()) {
            deleteObjects(data.getOkOffers(), tenantId, category, objectId);
        }
    }

    private long getTransferTimeout(long sizeToTransfer) {
        long timeout = (sizeToTransfer / 1024) * millisecondsPerKB;
        if (timeout < DEFAULT_MINIMUM_TIMEOUT) {
            return DEFAULT_MINIMUM_TIMEOUT;
        }
        return timeout;
    }

    private MultiplePipedInputStream getMultipleInputStreamFromWorkspace(InputStream stream, int nbCopy,
        Digest digest)
        throws  IOException {
        DigestInputStream digestOriginalStream = (DigestInputStream) digest.getDigestInputStream(stream);
        return new MultiplePipedInputStream(digestOriginalStream, nbCopy);
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
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, dataId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
    }

    public StorageOffer getOffer(String offerId) throws StorageException {
        return STRATEGY_PROVIDER.getStorageOffer(offerId);
    }
    
    public Collection<StorageStrategy> getStrategies() throws StorageException {
        return STRATEGY_PROVIDER.getStorageStrategies().values();
    }

    private void deleteObjects(List<String> offerIdList, Integer tenantId, DataCategory category, String objectId)
        throws StorageTechnicalException {
        // Map here to keep offerId linked to Future
        Map<String, Future<Boolean>> futureMap = new HashMap<>();
        for (String offerId : offerIdList) {
            final Driver driver = retrieveDriverInternal(offerId);
            // TODO: review if digest value is really good ?
            StorageRemoveRequest request =
                new StorageRemoveRequest(tenantId, category.getFolder(), objectId);
            futureMap.put(offerId, executor.submit(new DeleteThread(driver, request, offerId)));
        }

        // wait all tasks submission
        for (Entry<String, Future<Boolean>> entry : futureMap.entrySet()) {
            final Future<Boolean> future = entry.getValue();
            String offerId = entry.getKey();
            try {
                Boolean bool = future.get(DEFAULT_MINIMUM_TIMEOUT * 10, TimeUnit.MILLISECONDS);
                if (!bool) {
                    LOGGER.error("Object not deleted: {}", objectId);
                    throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId);
                }
            } catch (TimeoutException e) {
                LOGGER.error(TIMEOUT_ON_OFFER_ID + offerId, e);
                future.cancel(true);
                // TODO: manage thread to take into account this interruption
                LOGGER.error(INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID + offerId, e);
                throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId);
            } catch (InterruptedException e) {
                LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerId, e);
                throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId, e);
            } catch (ExecutionException e) {
                LOGGER.error(ERROR_ON_OFFER_ID + offerId, e);
                // TODO: review this exception to manage errors correctly
                // Take into account Exception class
                // For example, for particular exception do not retry (because
                // it's useless)
                // US : #2009
                throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId, e);
            } catch (NumberFormatException e) {
                future.cancel(true);
                LOGGER.error(WRONG_NUMBER_ON_WAIT_ON_OFFER_ID + offerId, e);
                throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId, e);
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
            return STRATEGY_PROVIDER.getStorageStrategy(VitamConfiguration.getDefaultStrategy()).getCopy();
        } catch (StorageTechnicalException e) {
            LOGGER.error(e);
            return 0;
        }
    }

    public static List<String> getOfferIds() {
        try {
            List<OfferReference> offerReferences =
                STRATEGY_PROVIDER.getStorageStrategy(VitamConfiguration.getDefaultStrategy()).getOffers();
            return offerReferences.stream().map(offer -> offer.getId()).collect(Collectors.toList());
        } catch (StorageTechnicalException e) {
            LOGGER.error(e);
        }

        return new ArrayList<>();
    }

    enum StorageAction {
        PUT,
        GET
    }

    public List<OfferReference> getOffersReferences(String strategyId) throws StorageNotFoundException, StorageTechnicalException {
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        if (storageStrategy == null) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        final List<OfferReference> offerReferences = new ArrayList<>();
        if (storageStrategy != null && !storageStrategy.getOffers().isEmpty()) {
            // TODO P1 : this code will be changed in the future to handle
            // priority (not in current US scope) and copy
            offerReferences.addAll(storageStrategy.getOffers());
        }
        return offerReferences;
    }
}
