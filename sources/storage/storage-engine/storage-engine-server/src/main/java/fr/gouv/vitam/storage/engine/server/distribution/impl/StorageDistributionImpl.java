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

package fr.gouv.vitam.storage.engine.server.distribution.impl;

import static fr.gouv.vitam.common.SedaConstants.STRATEGY_ID;
import static java.util.Collections.singletonList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultiplePipedInputStream;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.model.StorageGetMetadataRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.HotStrategy;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.bulk.BulkStorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.AccessLogParameters;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * StorageDistribution service Implementation process continue if needed)
 */
// TODO P1: see what to do with RuntimeException (catch it and log it to let the
public class StorageDistributionImpl implements StorageDistribution {
    private static final String DEFAULT_SIZE_WHEN_UNKNOWN = "1000000";
    private static final int DELETE_TIMEOUT = 120000;
    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final String CATEGORY_IS_MANDATORY = "Category (object type) is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageDistributionImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    private static final int NB_RETRY = 3;


    /**
     * Global pool thread
     */
    private final ExecutorService executor = new VitamThreadPoolExecutor();

    /**
     * Used to wait for all task submission (executorService)
     */
    private static final String NO_OFFER_IDENTIFIER_SPECIFIED_THIS_IS_MANDATORY =
        "No offer identifier specified, this is mandatory";
    private static final String INTERRUPTED_ON_OFFER_ID = "Interrupted on offer ID ";
    private static final String OBJECT_ID_IS_MANDATORY = "Object id is mandatory";
    private static final String NO_MESSAGE_RETURNED = "No message returned";
    private static final String ERROR_ON_OFFER_ID = "Error on offer ID ";
    private static final String INVALID_NUMBER_OF_COPY = "Invalid number of copy";
    private static final String CANNOT_CREATE_MULTIPLE_INPUT_STREAM = "Cannot create multipleInputStream";
    private static final String ERROR_ENCOUNTERED_IS = "Error encountered is ";
    private static final String INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID = "Interrupted after timeout on offer ID ";
    private static final String NO_NEED_TO_RETRY = ", no need to retry";
    private static final String TIMEOUT_ON_OFFER_ID = "Timeout on offer ID ";
    private static final String OBJECT_NOT_DELETED = "Object not deleted: ";
    private static final String ERROR_WITH_THE_STORAGE_OBJECT_NOT_FOUND_TAKE_NEXT_OFFER_IN_STRATEGY_BY_PRIORITY =
        "Error with the storage: object not found. Take next offer in strategy (by priority)";
    private static final String ERROR_WITH_THE_STORAGE_TAKE_THE_NEXT_OFFER_IN_THE_STRATEGY_BY_PRIORITY =
        "Error with the storage, take the next offer in the strategy (by priority)";
    private static final String OFFER_IDS_IS_MANDATORY = "Offer ids is mandatory";
    private static final String LENGTH_IS_EMPTY = "Length is empty";
    private static final String NO_LENGTH_RETURNED = "no Length returned";
    private static final String CONTAINER_GUID_IS_MANDATORY = "Container guid is mandatory";
    private static final String OBJECT_URI_IN_WORKSPACE_IS_MANDATORY = "Object URI in workspace is mandatory";
    private static final String OBJECT_ADDITIONAL_INFORMATION_GUID_IS_MANDATORY =
        "Object additional information guid is mandatory";
    private static final String OFFER_ID = "offerId";
    private static final String USABLE_SPACE = "usableSpace";
    private static final String NBC = "nbc";
    private static final String STORAGE_SERVICE_CONFIGURATION_IS_MANDATORY =
        "Storage service configuration is mandatory";
    private static final String CAPACITIES = "capacities";
    private static final String ATTEMPT = " attempt ";
    private static final String OFFERS_LIST_IS_MANDATORY = "Offers List is mandatory";
    public static final String DIGEST = "digest";

    private final String urlWorkspace;
    private final TransfertTimeoutHelper transfertTimeoutHelper;

    private final DigestType digestType;

    private StorageLog storageLogService;

    private final ExecutorService batchExecutorService;
    private final int batchDigestComputationTimeout;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final BulkStorageDistribution bulkStorageDistribution;

    /**
     * Constructs the service with a given configuration
     *
     * @param configuration the configuration of the storage
     * @param storageLogService service that allow write and access log
     */
    public StorageDistributionImpl(StorageConfiguration configuration, StorageLog storageLogService) {
        ParametersChecker.checkParameter(STORAGE_SERVICE_CONFIGURATION_IS_MANDATORY, configuration);
        urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        this.storageLogService = storageLogService;
        digestType = VitamConfiguration.getDefaultDigestType();
        batchExecutorService =
            new ThreadPoolExecutor(configuration.getMinBatchThreadPoolSize(), configuration.getMaxBatchThreadPoolSize(),
                1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), VitamThreadFactory.getInstance());
        batchDigestComputationTimeout = configuration.getBatchDigestComputationTimeout();
        this.transfertTimeoutHelper = new TransfertTimeoutHelper(configuration.getTimeoutMsPerKB());
        this.bulkStorageDistribution = new BulkStorageDistribution(NB_RETRY, this.workspaceClientFactory,
            this.storageLogService, this.transfertTimeoutHelper);
    }

    @VisibleForTesting
    StorageDistributionImpl(WorkspaceClientFactory workspaceClientFactory, DigestType digestType, StorageLog storageLogService,
        ExecutorService batchExecutorService, int batchDigestComputationTimeout, BulkStorageDistribution bulkStorageDistribution) {
        urlWorkspace = null;
        this.transfertTimeoutHelper = new TransfertTimeoutHelper(100L);
        this.workspaceClientFactory = workspaceClientFactory;
        this.digestType = digestType;
        this.storageLogService = storageLogService;
        this.batchExecutorService = batchExecutorService;
        this.batchDigestComputationTimeout = batchDigestComputationTimeout;
        this.bulkStorageDistribution = bulkStorageDistribution;
    }

    @Override
    public StoredInfoResult copyObjectFromOfferToOffer(DataContext context, String sourceOffer, String destinationOffer)
        throws StorageException {

        //TODO log in log storage bug #4836

        JsonNode containerInformation =
            getContainerInformation(STRATEGY_ID, context.getCategory(), context.getObjectId(),
                Lists.newArrayList(sourceOffer, destinationOffer), false);
        //verify source offer
        boolean existsSourceOffer = containerInformation.get(sourceOffer) != null;
        existsSourceOffer = existsSourceOffer && (containerInformation.get(sourceOffer).get(DIGEST) != null);


        if (!existsSourceOffer) {
            throw new StorageNotFoundException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, context.getObjectId()));
        }
        Response resp = null;
        // load the object/file from the given offer
        resp = getContainerByCategory(STRATEGY_ID, context.getObjectId(), context.getCategory(),
            sourceOffer);

        if (resp == null) {
            throw new StorageTechnicalException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
        }

        boolean existsDestinationOffer = containerInformation.get(destinationOffer) != null;

        existsDestinationOffer =
            existsDestinationOffer && (containerInformation.get(destinationOffer).get(DIGEST) != null);

        if (existsDestinationOffer) {
            deleteObjectInOffers(STRATEGY_ID, context, singletonList(destinationOffer));
        }
        if (resp.getStatus() == Response.Status.OK.getStatusCode()) {

            return storeDataInOffers(STRATEGY_ID, context.getObjectId(), context.getCategory(),
                context.getRequester(), singletonList(destinationOffer), resp);
        }

        DefaultClient.staticConsumeAnyEntityAndClose(resp);

        throw new StorageTechnicalException(
            VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
    }

    private StorageLogbookParameters sendDataToOffersWithRetries(DataContext dataContext, OffersToCopyIn offersParams,
        ObjectDescription description)
        throws StorageTechnicalException, StorageNotFoundException {
        StorageLogbookParameters parameters = null;
        int attempt = 0;
        // ACK to prevent retry

        AtomicBoolean needToRetry = new AtomicBoolean(true);

        while (needToRetry.get() && attempt < NB_RETRY && !offersParams.getKoOffers().isEmpty()) {

            attempt++;

            StreamAndInfo streamAndInfo = getInputStreamFromWorkspace(description);

            parameters = sendDataToOffers(streamAndInfo, dataContext, offersParams, attempt, needToRetry);
        }
        return parameters;
    }

    @Override
    public StoredInfoResult storeDataInOffers(String strategyId, String objectId, DataCategory category,
        String requester,
        List<String> offerIds, Response response)
        throws StorageException {


        Long size = Long.valueOf(response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));

        StreamAndInfo streamAndInfo = new StreamAndInfo(new VitamAsyncInputStream(response), size);
        return this.storeDataInOffers(strategyId, streamAndInfo, objectId, category, requester, offerIds);
    }

    @Override
    public StoredInfoResult storeDataInOffers(String strategyId, StreamAndInfo streamAndInfo, String objectId,
        DataCategory category, String requester,
        List<String> offerIds)
        throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();

        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);

        // Retrieve strategy offersToCopyIn
        checkStrategy(strategyId);

        List<StorageOffer> offers = new ArrayList<>();

        for (String o : offerIds) {
            offers.add(OFFER_PROVIDER.getStorageOffer(o));
        }

        OffersToCopyIn offersToCopyIn = new OffersToCopyIn(offers);

        final DataContext dataContext = new DataContext(objectId, category, requester, tenantId);

        //try only once
        StorageLogbookParameters parameters =
            sendDataToOffers(streamAndInfo, dataContext, offersToCopyIn, 1, new AtomicBoolean(false));

        try {
            logStorage(tenantId, parameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // TODO P1 Handle Status result if different for offers
        return buildStoreDataResponse(objectId, category,
            parameters.getMapParameters().get(StorageLogbookParameterName.digest),
            strategyId, offersToCopyIn.getGlobalOfferResult());

    }

    private HotStrategy checkStrategy(String strategyId) throws StorageTechnicalException, StorageNotFoundException {

        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);

        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();

        if (hotStrategy == null) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        if (!hotStrategy.isCopyValid()) {
            throw new StorageTechnicalException(INVALID_NUMBER_OF_COPY);
        }

        return hotStrategy;
    }

    private StorageLogbookParameters startCopyToOffers(DataContext dataContext, OffersToCopyIn data,
        int attempt, Long size, Digest globalDigest, MultiplePipedInputStream streams,
        Map<String, Future<ThreadResponseData>> futureMap) {
        String offerIdString = null;
        StorageLogbookParameters parameters = null;
        int rank = 0;
        try {
            for (int i = 0; i < data.getKoOffers().size(); i++) {
                offerIdString = data.getKoOffers().get(i);
                OfferReference offerReference = new OfferReference(offerIdString);
                final Driver driver = retrieveDriverInternal(offerIdString);
                InputStream inputStream = new BufferedInputStream(streams.getInputStream(rank));
                StoragePutRequest request =
                    new StoragePutRequest(dataContext.getTenantId(), dataContext.getCategory().getFolder(),
                        dataContext.getObjectId(), digestType.getName(),
                        inputStream);
                futureMap.put(offerIdString,
                    executor.submit(new TransferThread(driver, offerReference, request, globalDigest,
                        size)));
                rank++;
            }
        } catch (StorageException e) {
            LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerIdString, e);
            parameters =
                setLogbookStorageParameters(null, offerIdString, null, dataContext.getRequester(), attempt,
                    Status.INTERNAL_SERVER_ERROR, dataContext.getCategory().getFolder());
        }
        return parameters;
    }

    // TODO P1 : review design : for the moment we handle
    // createObjectDescription AND jsonData in the same params but
    // they should not be both resent at the same time. Maybe encapsulate or
    // create 2 methods
    // TODO P1 : refactor me !
    // FIXME SHOULD not be synchronized but instability needs it
    @Override
    public StoredInfoResult storeDataInAllOffers(String strategyId, String objectId,
        ObjectDescription createObjectDescription, DataCategory category, String requester)
        throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        checkStoreDataParams(createObjectDescription, strategyId, objectId, category);
        List<StorageOffer> storageOffers = getStorageOffers(strategyId);

        OffersToCopyIn offersToCopyIn = new OffersToCopyIn(storageOffers);
        final DataContext dataContext = new DataContext(objectId, category, requester, tenantId);

        StorageLogbookParameters parameters =
            sendDataToOffersWithRetries(dataContext, offersToCopyIn, createObjectDescription);
        ///logging
        try {
            logStorage(tenantId, parameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // TODO P1 Handle Status result if different for offers
        return buildStoreDataResponse(objectId, category,
            parameters.getMapParameters().get(StorageLogbookParameterName.digest),
            strategyId, offersToCopyIn.getGlobalOfferResult());

    }

    private List<StorageOffer> getStorageOffers(String strategyId)
        throws StorageTechnicalException, StorageNotFoundException {
        // Retrieve strategy offersToCopyIn
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();

        if (hotStrategy == null) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));

        }

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);


        return offerReferences.stream()
            .map(StorageDistributionImpl::apply)
            .collect(Collectors.toList());
    }

    @Override
    public BulkObjectStoreResponse bulkCreateFromWorkspace(String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest, String requester)
        throws StorageException {

        Integer tenantId = ParameterHelper.getTenantParameter();
        List<String> offerIds =
            getStorageOffers(strategyId).stream().map(StorageOffer::getId).collect(Collectors.toList());

        Map<String, Driver> storageDrivers = new HashMap<>();
        for (String offerId : offerIds) {
            storageDrivers.put(offerId, retrieveDriverInternal(offerId));
        }

        Map<String, StorageOffer> storageOffers = new HashMap<>();
        for (String offerId : offerIds) {
            storageOffers.put(offerId, OFFER_PROVIDER.getStorageOffer(offerId));
        }

        Map<String, String> objectDigests =
            bulkStorageDistribution.bulkCreateFromWorkspaceWithRetries(tenantId, offerIds, storageDrivers, storageOffers,
                bulkObjectStoreRequest.getType(), bulkObjectStoreRequest.getWorkspaceContainerGUID(),
                bulkObjectStoreRequest.getWorkspaceObjectURIs(), bulkObjectStoreRequest.getObjectNames(), requester);

        return new BulkObjectStoreResponse(offerIds, digestType.getName(), objectDigests);
    }

    @Override
    public List<String> getOfferIds(String strategyId) throws StorageException {

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        return hotStrategy.getOffers().stream().map(OfferReference::getId).collect(Collectors.toList());
    }


    private StorageLogbookParameters sendDataToOffers(StreamAndInfo streamAndInfo,
        DataContext dataContext, OffersToCopyIn offersParams, final int attempt,
        AtomicBoolean needToRetry)
        throws StorageTechnicalException {
        StorageLogbookParameters parameters;
        Digest globalDigest = new Digest(digestType);

        try (
            InputStream digestInputStream = globalDigest.getDigestInputStream(streamAndInfo.getStream());
            MultiplePipedInputStream streams = new MultiplePipedInputStream(digestInputStream,
            offersParams.getKoOffers().size())) {
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();

            long finalTimeout = transfertTimeoutHelper.getTransferTimeout(streamAndInfo.getSize());
            TimeoutStopwatch timeoutStopwatch = new TimeoutStopwatch(finalTimeout);

            parameters =
                startCopyToOffers(dataContext, offersParams, attempt,
                    streamAndInfo.getSize(), globalDigest, streams, futureMap);

            // wait for all threads execution
            // TODO: manage interruption and error execution (US #2008 && 2009)
            for (Entry<String, Future<ThreadResponseData>> entry : futureMap.entrySet()) {
                final Future<ThreadResponseData> future = entry.getValue();
                // Check if any has one IO Exception
                streams.throwLastException();
                String offerId = entry.getKey();
                try {
                    ThreadResponseData threadResponseData = future.get(
                        timeoutStopwatch.getRemainingDelayInMilliseconds(), TimeUnit.MILLISECONDS);

                    if (threadResponseData == null) {
                        LOGGER.error(ERROR_ON_OFFER_ID + offerId);
                        parameters =
                            setLogbookStorageParameters(parameters, offerId, null, dataContext.getRequester(),
                                attempt,
                                Status.INTERNAL_SERVER_ERROR, dataContext.getCategory().getFolder());
                        throw new StorageTechnicalException(NO_MESSAGE_RETURNED);
                    }
                    parameters =
                        setLogbookStorageParameters(parameters, offerId, threadResponseData,
                            dataContext.getRequester(),
                            attempt,
                            threadResponseData.getStatus(), dataContext.getCategory().getFolder());
                    offersParams.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error(INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID + offerId);
                    parameters =
                        setLogbookStorageParameters(parameters, offerId, null, dataContext.getRequester(), attempt,
                            null, dataContext.getCategory().getFolder());
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerId, e);
                    parameters =
                        setLogbookStorageParameters(parameters, offerId, null, dataContext.getRequester(), attempt,
                            null, dataContext.getCategory().getFolder());
                } catch (ExecutionException e) {
                    LOGGER.error(StorageDistributionImpl.ERROR_ON_OFFER_ID + offerId, e);
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        status = Status.CONFLICT;
                        offersParams.changeStatus(offerId, status);
                    }
                    parameters =
                        setLogbookStorageParameters(parameters, offerId, null, dataContext.getRequester(), attempt,
                            status, dataContext.getCategory().getFolder());

                    if (e.getCause() instanceof StorageInconsistentStateException) {
                        LOGGER.error(
                            StorageDistributionImpl.ERROR_ENCOUNTERED_IS + e.getCause().getClass() +
                                NO_NEED_TO_RETRY,
                            e);
                        needToRetry.set(false);
                    } else if (e.getCause() instanceof StorageDriverException) {
                        StorageDriverException ex = (StorageDriverException) e.getCause();
                        if (!ex.isShouldRetry()) {
                            LOGGER.error(
                                StorageDistributionImpl.ERROR_ENCOUNTERED_IS + e.getCause().getClass() +
                                    NO_NEED_TO_RETRY, ex);
                            needToRetry.set(false);
                        }
                    }
                }
            }
            // Check if any has one IO Exception
            streams.throwLastException();

        } catch (IOException e) {
            LOGGER.error(CANNOT_CREATE_MULTIPLE_INPUT_STREAM, e);
            throw new StorageTechnicalException(CANNOT_CREATE_MULTIPLE_INPUT_STREAM, e);
        }
        // TODO : error management (US #2009)
        if (!offersParams.getKoOffers().isEmpty()) {
            deleteObjects(offersParams.getOkOffers(), dataContext.getTenantId(), dataContext.getCategory(),
                dataContext.getObjectId());
        }
        return parameters;
    }

    private StreamAndInfo getInputStreamFromWorkspace(ObjectDescription createObjectDescription)
        throws StorageTechnicalException, StorageNotFoundException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            return retrieveDataFromWorkspace(createObjectDescription.getWorkspaceContainerGUID(),
                createObjectDescription.getWorkspaceObjectURI(), workspaceClient);
        }
    }

    private StorageLogbookParameters setLogbookStorageParameters(StorageLogbookParameters parameters, String
        offerId,
        ThreadResponseData res,
        String requester, int attempt, Status status, String dataCategoty) {
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        if (parameters == null) {
            parameters = getParameters(res != null ? res.getObjectGuid() : null, dataCategoty,
                res != null ? res.getResponse() : null,
                null, offerId, res != null ? res.getStatus() : status, requester, attempt);
        } else {
            updateStorageLogbookParameters(parameters, offerId,
                res != null ? res.getStatus() : status, attempt);
        }
        return parameters;
    }

    private void logStorage(Integer tenant, StorageLogbookParameters parameters)
        throws IOException {
        storageLogService.appendWriteLog(tenant, parameters);
    }

    private StoredInfoResult buildStoreDataResponse(String objectId, DataCategory category, String digest,
        String strategy, Map<String, Status> offerResults)
        throws StorageTechnicalException, StorageAlreadyExistsException {

        final String offerIds = String.join(", ", offerResults.keySet());
        // Aggregate result of all store actions. If all went well, allSuccess is true, false if one action failed
        final boolean allWithoutInternalServerError = offerResults.entrySet().stream()
            .map(Map.Entry::getValue)
            .noneMatch(Status.INTERNAL_SERVER_ERROR::equals);
        final boolean allWithoutAlreadyExists = offerResults.entrySet().stream()
            .map(Map.Entry::getValue)
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
            case STORAGEACCESSLOG:
                description.append("StorageAccessLog ");
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
                break;
            case CHECKLOGBOOKREPORTS:
                description.append("CHECKLOGBOOKREPORTS ");
                break;
            case UNIT_GRAPH:
                description.append("UNIT_GRAPH ");
                break;
            case OBJECTGROUP_GRAPH:
                description.append("OBJECTGROUP_GRAPH ");
                break;
            case ACCESSION_REGISTER_DETAIL:
                description.append("ACCESSION_REGISTER_DETAIL ");
                break;
            case ACCESSION_REGISTER_SYMBOLIC:
                description.append("ACCESSION_REGISTER_SYMBOLIC ");
                break;
            case DISTRIBUTIONREPORTS:
                description.append("DISTRIBUTION_REPORTS ");
                break;
            default:
                throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
        }
        // StorageDistributionImpl.
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

    /**
     * Storage logbook entry for ONE offer
     *
     * @param objectGuid the object Guid
     * @param dataCategoty
     * @param putObjectResult the response
     * @param messageDigest the computed digest
     * @param offerId the offerId
     * @param objectStored the operation status
     * @return storage logbook parameters
     */
    private StorageLogbookParameters getParameters(String objectGuid, String dataCategoty,
        StoragePutResult putObjectResult,
        Digest messageDigest, String offerId, Status objectStored, String requester, int attempt) {
        final String objectIdentifier = objectGuid != null ? objectGuid : "objectRequest NA";
        final String messageDig = messageDigest != null ? messageDigest.digestHex()
            : (putObjectResult != null ? putObjectResult.getDigestHashBase16() : "messageDigest NA");
        final String size = putObjectResult != null ? String.valueOf(putObjectResult.getObjectSize()) : "Size NA";
        boolean error = objectStored == Status.INTERNAL_SERVER_ERROR;
        final StorageLogbookOutcome outcome = error ? StorageLogbookOutcome.KO : StorageLogbookOutcome.OK;

        return StorageLogbookParameters.createLogParameters(objectIdentifier, dataCategoty, messageDig, digestType.getName(), size,
            getAttemptLog(offerId, attempt, error), requester, outcome);
    }

    private String getAttemptLog(String offerId, int attempt, boolean error) {
        return offerId + ATTEMPT + attempt + " : " + (error ? "KO" : "OK");
    }

    private void updateStorageLogbookParameters(StorageLogbookParameters parameters, String offerId, Status status,
        int attempt) {
        String offers = parameters.getMapParameters().get(StorageLogbookParameterName.agentIdentifiers);
        if (Status.INTERNAL_SERVER_ERROR.equals(status) || Status.CONFLICT.equals(status)) {
            parameters.getMapParameters().put(StorageLogbookParameterName.outcome, StorageLogbookOutcome.KO.name());
            offers += ", " + offerId + ATTEMPT + attempt + " : KO - " + status.name();
        } else {
            offers += ", " + offerId + ATTEMPT + attempt + " : OK";
            parameters.setStatus(StorageLogbookOutcome.OK);
        }
        parameters.getMapParameters().put(StorageLogbookParameterName.agentIdentifiers, offers);
    }

    private Driver retrieveDriverInternal(String offerId) throws StorageTechnicalException {
        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException e) {
            throw new StorageTechnicalException(e);
        }
    }

    private void checkStoreDataParams(ObjectDescription createObjectDescription, String strategyId, String dataId,
        DataCategory category) {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, dataId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        ParametersChecker.checkParameter(OBJECT_ADDITIONAL_INFORMATION_GUID_IS_MANDATORY, createObjectDescription);
        ParametersChecker.checkParameter(CONTAINER_GUID_IS_MANDATORY,
            createObjectDescription.getWorkspaceContainerGUID());
        ParametersChecker.checkParameter(OBJECT_URI_IN_WORKSPACE_IS_MANDATORY,
            createObjectDescription.getWorkspaceObjectURI());
    }

    private StreamAndInfo retrieveDataFromWorkspace(String containerGUID, String objectURI,
        WorkspaceClient workspaceClient)
        throws StorageNotFoundException, StorageTechnicalException {
        Response response;
        try {
            response = workspaceClient.getObject(containerGUID, objectURI);

            String length = response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName());
            Object entity = response.getEntity();
            if (entity == null) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID));
                throw new StorageNotFoundException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID));
            }
            try {
                ParametersChecker.checkParameter(LENGTH_IS_EMPTY, length);
                Long.valueOf(length);
            } catch (IllegalArgumentException e) {
                // Default value (hack)
                LOGGER.warn(NO_LENGTH_RETURNED, e);
                length = DEFAULT_SIZE_WHEN_UNKNOWN;
            }

            return new StreamAndInfo(new VitamAsyncInputStream(response), Long.valueOf(length));
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID), e);
            throw new StorageNotFoundException(e);
        } catch (final ContentAddressableStorageServerException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageTechnicalException(e);
        }
    }

    @Override
    public JsonNode getContainerInformation(String strategyId) throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        // Retrieve strategy data
        HotStrategy hotStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

        ArrayNode resultArray = JsonHandler.createArrayNode();

        for (OfferReference offerReference : offerReferences) {
            resultArray.add(getOfferInformation(offerReference, tenantId, hotStrategy.getCopy()));
        }

        return JsonHandler.createObjectNode().set(CAPACITIES, resultArray);
    }

    private JsonNode getOfferInformation(OfferReference offerReference, Integer tenantId, int nbCopy)
        throws StorageException {
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            final ObjectNode ret = JsonHandler.createObjectNode();
            ret.put(OFFER_ID, offerReference.getId());
            ret.put(USABLE_SPACE, connection.getStorageCapacity(tenantId).getUsableSpace());
            ret.put(NBC, nbCopy);
            return ret;
        } catch (StorageDriverException | RuntimeException e) {
            if (e instanceof StorageDriverPreconditionFailedException) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_BAD_REQUEST), e);
                throw new IllegalArgumentException(e);
            }
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageTechnicalException(e);
        }
    }

    private List<OfferReference> getOfferListFromHotStrategy(HotStrategy hotStrategy) throws
        StorageTechnicalException {
        final List<OfferReference> offerReferences = new ArrayList<>();
        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {

            offerReferences.addAll(hotStrategy.getOffers());
        }

        if (offerReferences.isEmpty()) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }
        return offerReferences;
    }

    private OfferReference chooseReferentOffer(HotStrategy hotStrategy) {

        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {
            List<OfferReference> offerReferences =
                hotStrategy.getOffers().stream().filter(OfferReference::isReferent).collect(Collectors.toList());
            return Iterables.getOnlyElement(offerReferences);
        }
        throw new IllegalArgumentException("Exactly one offer should be declared as 'referent' in hot strategy");
    }

    @Override
    public JsonNode createContainer(String strategyId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }


    @Override
    public RequestResponse<JsonNode> listContainerObjects(String strategyId, DataCategory category, String cursorId)
        throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

            // TODO: make priority -> Use the first one here but don't take into
            // account errors !
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReferences.get(0).getId());
            final Driver driver = retrieveDriverInternal(offerReferences.get(0).getId());
            try (Connection connection = driver.connect(offer.getId())) {
                StorageListRequest request = new StorageListRequest(tenantId, category.getFolder(), cursorId, true);
                return connection.listObjects(request);
            } catch (final StorageDriverException exc) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
                throw new StorageTechnicalException(exc);
            }
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(String strategyId, DataCategory category, Long offset, int limit,
        Order order)
        throws StorageException {

        return getOfferLogRequestResponse(strategyId, null, category, offset, limit, order);
    }

    /**
     * Get offer log from the given offer
     *
     * @param strategyId the strategy id to get offers
     * @param offerId offerId
     * @param category the object type to list
     * @param offset offset of the excluded object
     * @param limit the number of result wanted
     * @param order order
     * @return list of offer log
     * @throws StorageException thrown in case of any technical problem
     */
    @Override
    public RequestResponse<OfferLog> getOfferLogsByOfferId(String strategyId, String offerId,
        DataCategory category,
        Long offset, int limit, Order order) throws StorageException {
        ParametersChecker.checkParameter(NO_OFFER_IDENTIFIER_SPECIFIED_THIS_IS_MANDATORY, offerId);

        return getOfferLogRequestResponse(strategyId, offerId, category, offset, limit, order);
    }

    /**
     * Get offer logs from the offer.
     *
     * @param strategyId strategyId
     * @param offerId offerId
     * @param category category
     * @param offset offset
     * @param limit limit
     * @param order order
     * @return RequestResponse
     * @throws StorageException StorageException
     */
    private RequestResponse<OfferLog> getOfferLogRequestResponse(String strategyId, String offerId,
        DataCategory category, Long offset, int limit, Order order) throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();

        if (hotStrategy == null) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        // In case we do not specify an offer as parameter, the referent offer is chosen
        if (offerId == null) {
            // find offer referent
            final OfferReference offerReference = chooseReferentOffer(hotStrategy);
            if (offerReference != null) {
                offerId = offerReference.getId();
            } else {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
                throw new StorageTechnicalException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
        }

        // get the storage offer from the given identifier
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);

        final Driver driver = retrieveDriverInternal(offer.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            StorageOfferLogRequest request =
                new StorageOfferLogRequest(tenantId, category.getFolder(), offset, limit, order);
            return connection.getOfferLogs(request);
        } catch (final StorageDriverException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageTechnicalException(e);
        }
    }

    @Override
    public Response getContainerByCategory(String strategyId, String objectId, DataCategory category,
        AccessLogInfoModel logInformation)
        throws StorageException {
        return getContainerByCategoryResponse(strategyId, objectId, category, null, logInformation);
    }

    @Override
    public Response getContainerByCategory(String strategyId, String objectId, DataCategory category, String
        offerId)
        throws StorageException {
        return getContainerByCategoryResponse(strategyId, objectId, category, offerId,
            AccessLogUtils.getNoLogAccessLog());
    }

    /**
     * getContainerByCategoryResponse.
     *
     * @param strategyId strategyId
     * @param objectId objectId
     * @param category category
     * @return Response
     * @throws StorageException the exception
     */
    private Response getContainerByCategoryResponse(String strategyId, String objectId, DataCategory category,
        String offerId, AccessLogInfoModel logInformation)
        throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);

        HotStrategy hotStrategy = checkStrategy(strategyId);

        List<StorageOffer> storageOffers = new ArrayList<>();

        if (StringUtils.isBlank(offerId)) {

            final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

            List<StorageOffer> collect = offerReferences.stream()
                .map(StorageDistributionImpl::apply)
                .collect(Collectors.toList());

            storageOffers.addAll(collect);
        } else {
            // get the storage offer from the given identifier
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
            storageOffers = singletonList(offer);
        }

        if (DataCategory.OBJECT.equals(category)) {

            try {
                if (AccessLogUtils.mustLog(logInformation)) {
                    AccessLogParameters params = createParamsForAccessLog(logInformation, objectId);
                    storageLogService.appendAccessLog(tenantId, params);
                }
            } catch (IOException e) {
                // Doit on faire qqch si on arrive pas a logger ?
                LOGGER.error(e);
            }
        }

        final StorageGetResult result =
            getObjectResult(tenantId, objectId, category, storageOffers);
        return result.getObject();
    }

    /**
     * apply.
     *
     * @param offerReference offerReference
     * @return StorageOffer
     */
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
     * getObjectResult.
     *
     * @param tenantId tenantId
     * @param objectId objectID
     * @param type type
     * @param storageOffers storageOffer
     * @return StorageGetResult
     * @throws StorageException the exception
     */
    private StorageGetResult getObjectResult(Integer tenantId, String objectId, DataCategory type,
        List<StorageOffer> storageOffers)

        throws StorageException {

        StorageGetResult result;
        boolean offerOkNoBinary = false;
        for (final StorageOffer storageOffer : storageOffers) {
            final Driver driver = retrieveDriverInternal(storageOffer.getId());
            try (Connection connection = driver.connect(storageOffer.getId())) {
                final StorageObjectRequest request = new StorageObjectRequest(tenantId, type.getFolder(), objectId);
                result = connection.getObject(request);
                if (result.getObject() != null) {
                    return result;
                }
            } catch (final fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException exc) {
                LOGGER.warn(ERROR_WITH_THE_STORAGE_OBJECT_NOT_FOUND_TAKE_NEXT_OFFER_IN_STRATEGY_BY_PRIORITY, exc);
                offerOkNoBinary = true;
            } catch (final StorageDriverException exc) {
                LOGGER.warn(ERROR_WITH_THE_STORAGE_TAKE_THE_NEXT_OFFER_IN_THE_STRATEGY_BY_PRIORITY, exc);
            }
        }
        if (offerOkNoBinary) {
            throw new StorageNotFoundException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId));
        } else {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }
    }

    @Override
    public JsonNode getContainerInformation(String strategyId, DataCategory type, String objectId,
        List<String> offerIds, boolean noCache)
        throws StorageException {

        ObjectNode offerIdToMetadata = JsonHandler.createObjectNode();

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);

        HotStrategy hotStrategy = checkStrategy(strategyId);
        getOfferListFromHotStrategy(hotStrategy);

        for (final String offerId : offerIds) {
            final Driver driver = retrieveDriverInternal(offerId);
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
            try (Connection connection = driver.connect(offer.getId())) {

                final StorageGetMetadataRequest request = new StorageGetMetadataRequest(tenantId,
                    type.getFolder(), objectId, noCache);

                StorageMetadataResult metaData = connection.getMetadatas(request);
                offerIdToMetadata.set(offerId, JsonHandler.toJsonNode(metaData));

            } catch (StorageDriverException exc) {
                LOGGER.warn(ERROR_WITH_THE_STORAGE_TAKE_THE_NEXT_OFFER_IN_THE_STRATEGY_BY_PRIORITY, exc);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn(e);
            }
        }
        return offerIdToMetadata;
    }

    @Override
    public Map<String, Boolean> checkObjectExisting(String strategyId, String objectId, DataCategory category,
            List<String> offerIds) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);
        ParametersChecker.checkParameter(OFFER_IDS_IS_MANDATORY, offerIds);

        Map<String, Boolean> resultByOffer = new HashMap<String, Boolean>();

        HotStrategy hotStrategy = checkStrategy(strategyId);
        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

        List<String> offerReferencesIds = offerReferences.stream().map(OfferReference::getId)
                .collect(Collectors.toList());
        resultByOffer.putAll(offerIds.stream().filter(offer -> !offerReferencesIds.contains(offer))
                .collect(Collectors.toMap(offerId -> offerId, offerId -> Boolean.FALSE)));

        for (final String offerId : offerIds) {
            if (!resultByOffer.containsKey(offerId)) {
                final Driver driver = retrieveDriverInternal(offerId);
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
                try (Connection connection = driver.connect(offer.getId())) {
                    final StorageObjectRequest request = new StorageObjectRequest(tenantId, category.getFolder(),
                            objectId);
                    if (!connection.objectExistsInOffer(request)) {
                        resultByOffer.put(offerId, Boolean.FALSE);
                    } else {
                        resultByOffer.put(offerId, Boolean.TRUE);
                    }
                } catch (final fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException e) {
                    LOGGER.warn(ERROR_WITH_THE_STORAGE_OBJECT_NOT_FOUND_TAKE_NEXT_OFFER_IN_STRATEGY_BY_PRIORITY, e);
                    resultByOffer.put(offerId, Boolean.FALSE);
                } catch (final StorageDriverException e) {
                    LOGGER.warn(ERROR_WITH_THE_STORAGE_TAKE_THE_NEXT_OFFER_IN_THE_STRATEGY_BY_PRIORITY, e);
                    resultByOffer.put(offerId, Boolean.FALSE);
                }
            }
            
        }
        return resultByOffer;
    }


    @Deprecated
    private void deleteObjects(List<String> offerIdList, Integer tenantId, DataCategory category, String
        objectId)
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

        for (Entry<String, Future<Boolean>> entry : futureMap.entrySet()) {
            final Future<Boolean> future = entry.getValue();
            String offerId = entry.getKey();
            try {
                Boolean bool = future.get(DELETE_TIMEOUT, TimeUnit.MILLISECONDS);
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

                throw new StorageTechnicalException(OBJECT_NOT_DELETED + objectId, e);
            }
        }
    }

    @Override
    public void deleteObjectInAllOffers(String strategyId, DataContext context)
        throws StorageException {

        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, context.getObjectId());

        HotStrategy hotStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

        deleteObject(context, offerReferences);
    }

    @Override
    public void deleteObjectInOffers(String strategyId, DataContext context, List<String> offers)
        throws StorageException {

        // Check input params
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);

        ParametersChecker.checkParameter(OFFERS_LIST_IS_MANDATORY, offers);
        if (offers.isEmpty()) {
            LOGGER.error(OFFERS_LIST_IS_MANDATORY);
            throw new StorageTechnicalException(OFFERS_LIST_IS_MANDATORY);
        }
        HotStrategy hotStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

        final List<OfferReference> offerReferencesToDelete = new ArrayList<>();

        for (String offerId : offers) {

            Optional<OfferReference> found =
                offerReferences.stream().filter(offerReference -> offerReference.getId().equals(offerId))
                    .findFirst();

            found.ifPresent(offerReferencesToDelete::add);

            if (!found.isPresent()) {
                throw new StorageTechnicalException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
        }

        deleteObject(context, offerReferencesToDelete);
    }


    private void deleteObject(DataContext context, List<OfferReference> offerReferences)
        throws StorageException {

        Map<String, StorageLogbookOutcome> deleteOutcomeByOfferId = new HashMap<>();

        for (final OfferReference offerReference : offerReferences) {
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            try {
                deleteObject(context.getObjectId(), context.getTenantId(), driver, offer, context.getCategory());
                deleteOutcomeByOfferId.put(driver.getName(), StorageLogbookOutcome.OK);
            } catch (Exception e) {
                LOGGER.error(String.format("An error occurred during object delete %s/%s from offer %s",
                    context.getCategory(), context.getObjectId(), driver.getName()), e);
                deleteOutcomeByOfferId.put(driver.getName(), StorageLogbookOutcome.KO);
            }
        }

        String offerDetails = deleteOutcomeByOfferId.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));

        StorageLogbookOutcome globalOutcome = deleteOutcomeByOfferId.values().stream()
            .max(Enum::compareTo)
            .orElse(StorageLogbookOutcome.KO);

        StorageLogbookParameters deleteLogParameters = buildDeleteLogParameters(
            context.getObjectId(), context.getCategory().getFolder(), offerDetails, context.getRequester(),
            globalOutcome);

        // Logging
        try {
            logStorage(context.getTenantId(), deleteLogParameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        if (globalOutcome != StorageLogbookOutcome.OK) {
            throw new StorageException(String.format(
                "Could not delete object %s/%s. %s", context.getCategory(), context.getObjectId(), offerDetails));
        }
    }

    private void deleteObject(String objectId, Integer tenantId, Driver driver, StorageOffer offer,
        DataCategory category)
        throws StorageTechnicalException {
        try (Connection connection = driver.connect(offer.getId())) {
            StorageRemoveRequest request =
                new StorageRemoveRequest(tenantId, category.getFolder(), objectId);
            StorageRemoveResult result = connection.removeObject(request);
            if (!result.isObjectDeleted()) {
                LOGGER.warn("Not found " + objectId + ". Already deleted?");
            }
        } catch (fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException e) {
            LOGGER.warn("Not found " + objectId + ". Already deleted?", e);
        } catch (StorageDriverException | RuntimeException e) {
            if (e instanceof StorageDriverPreconditionFailedException) {
                throw new IllegalArgumentException(e);
            }
            throw new StorageTechnicalException(e);
        }
    }

    private StorageLogbookParameters buildDeleteLogParameters(String objectIdentifier, String dataCategory,
        String agentIdentifiers, String agentIdentifierRequester, StorageLogbookOutcome outcome) {

        final Map<StorageLogbookParameterName, String> mandatoryParameters = new TreeMap<>();
        mandatoryParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.now().toString());
        mandatoryParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        mandatoryParameters.put(StorageLogbookParameterName.objectIdentifier, objectIdentifier);
        mandatoryParameters.put(StorageLogbookParameterName.dataCategory, dataCategory);
        mandatoryParameters.put(StorageLogbookParameterName.eventType, "DELETE");
        mandatoryParameters.put(StorageLogbookParameterName.xRequestId,
            VitamThreadUtils.getVitamSession().getRequestId());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifiers, agentIdentifiers);
        mandatoryParameters
            .put(StorageLogbookParameterName.tenantId, ParameterHelper.getTenantParameter().toString());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifierRequester, agentIdentifierRequester);


        return StorageLogbookParameters.buildDeleteLogParameters(mandatoryParameters);
    }

    @Override
    public List<BatchObjectInformationResponse> getBatchObjectInformation(String strategyId, DataCategory type,
        List<String> objectIds, List<String> offerIds)
        throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);

        HotStrategy hotStrategy = checkStrategy(strategyId);
        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(hotStrategy);

        List<String> offerReferencesIds =
            offerReferences.stream().map(OfferReference::getId).collect(Collectors.toList());
        if (!offerReferencesIds.containsAll(offerIds)) {
            List<String> missingOfferIds = offerIds.stream()
                .filter(id -> !offerReferencesIds.contains(id))
                .collect(Collectors.toList());
            throw new StorageException("Invalid offer ids " +
                missingOfferIds
                + "for strategy " + strategyId);
        }

        Map<String, Driver> driverByOfferId = new HashMap<>();
        Map<String, StorageOffer> storageOfferByOfferId = new HashMap<>();

        for (String offerId : offerIds) {
            driverByOfferId.put(offerId, retrieveDriverInternal(offerId));
            storageOfferByOfferId.put(offerId, OFFER_PROVIDER.getStorageOffer(offerId));
        }

        List<CompletableFuture<BatchObjectInformationResponse>> completableFutures = new ArrayList<>();
        for (String objectId : objectIds) {

            CompletableFuture<BatchObjectInformationResponse> objectInformationCompletableFuture =
                CompletableFuture.supplyAsync(() -> getObjectInformation(type, offerIds, tenantId, objectId,
                    driverByOfferId, storageOfferByOfferId),
                    batchExecutorService);
            completableFutures.add(objectInformationCompletableFuture);
        }

        CompletableFuture<List<BatchObjectInformationResponse>> batchObjectInformationFuture
            = sequence(completableFutures);

        try {
            return batchObjectInformationFuture.get(batchDigestComputationTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Abort pending tasks
            for (CompletableFuture<BatchObjectInformationResponse> completableFuture : completableFutures) {
                completableFuture.cancel(false);
            }
            throw new StorageException("Batch object information timed out", e);
        }
    }

    private CompletableFuture<List<BatchObjectInformationResponse>> sequence(
        List<CompletableFuture<BatchObjectInformationResponse>> completableFutures) {
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
            completableFutures.toArray(new CompletableFuture[0]));

        return allDoneFuture
            .thenApply(v -> completableFutures.stream().map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    private BatchObjectInformationResponse getObjectInformation(DataCategory type, List<String> offerIds,
        Integer tenantId, String objectId,
        Map<String, Driver> driverByOfferId,
        Map<String, StorageOffer> storageOfferByOfferId) {

        Map<String, String> offerDigests = new HashMap<>();
        for (final String offerId : offerIds) {
            final Driver driver = driverByOfferId.get(offerId);
            final StorageOffer offer = storageOfferByOfferId.get(offerId);
            String digest;
            try (Connection connection = driver.connect(offer.getId())) {

                // Get object metadata (cache enabled)
                final StorageGetMetadataRequest request = new StorageGetMetadataRequest(tenantId,
                    type.getFolder(), objectId, false);

                StorageMetadataResult metaData = connection.getMetadatas(request);
                digest = metaData.getDigest();
            } catch (StorageDriverException exc) {
                LOGGER.warn(String.format("Could not retrieve object digest for object '%s' of type %s in offer %s",
                    objectId, type, offerId), exc);
                digest = null;
            }
            offerDigests.put(offerId, digest);
        }
        return new BatchObjectInformationResponse(type, objectId, offerDigests);
    }

    private AccessLogParameters createParamsForAccessLog(AccessLogInfoModel logInfo, String objectId) {
        Map<StorageLogbookParameterName, String> mapParameters = new HashMap<>();
        mapParameters.put(StorageLogbookParameterName.objectIdentifier, objectId);
        if (logInfo.getContextId() != null) {
            mapParameters.put(StorageLogbookParameterName.contextId, logInfo.getContextId());
        }

        if (logInfo.getContractId() != null) {
            mapParameters.put(StorageLogbookParameterName.contractId, logInfo.getContractId());
        }

        if (logInfo.getRequestId() != null) {
            mapParameters.put(StorageLogbookParameterName.xRequestId, logInfo.getRequestId());
        }

        if (logInfo.getApplicationId() != null) {
            mapParameters.put(StorageLogbookParameterName.applicationId, logInfo.getApplicationId());
        }

        if (logInfo.getArchiveId() != null) {
            mapParameters.put(StorageLogbookParameterName.archivesId, logInfo.getArchiveId());
        }

        if (logInfo.getQualifier() != null) {
            mapParameters.put(StorageLogbookParameterName.qualifier, logInfo.getQualifier());
        }

        if (logInfo.getVersion() != null) {
            mapParameters.put(StorageLogbookParameterName.version, logInfo.getVersion().toString());
        }

        if (logInfo.getSize() != null) {
            mapParameters.put(StorageLogbookParameterName.size, logInfo.getSize().toString());
        }

        mapParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.now().toString());

        return new AccessLogParameters(mapParameters);
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
}
