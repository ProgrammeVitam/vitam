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
package fr.gouv.vitam.storage.engine.server.distribution.impl;

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
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.retryable.DelegateRetry;
import fr.gouv.vitam.common.retryable.Retryable;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableOnResult;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultiplePipedInputStream;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServerErrorException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServiceUnavailableException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.driver.model.StorageAccessRequestCreationRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageCheckObjectAvailabilityRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetBulkMetadataRequest;
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
import fr.gouv.vitam.storage.engine.common.exception.StorageIllegalOperationException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.exception.StorageUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.engine.common.metrics.DownloadCountingSizeMetricsResponse;
import fr.gouv.vitam.storage.engine.common.metrics.UploadCountingInputStreamMetrics;
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
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * StorageDistribution service Implementation process continue if needed)
 */
// TODO P1: see what to do with RuntimeException (catch it and log it to let the
public class StorageDistributionImpl implements StorageDistribution {

    private static final String DEFAULT_SIZE_WHEN_UNKNOWN = "1000000";
    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final String CATEGORY_IS_MANDATORY = "Category (object type) is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageDistributionImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    public static final String NORMAL_ORIGIN = "normal";
    public static final String COPY_OBJECT_ORIGIN = "copy_object";

    private final RetryableParameters retryableParameters = new RetryableParameters(3, 1, 5, 3, SECONDS);

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
    private static final String CANNOT_CREATE_MULTIPLE_INPUT_STREAM = "Cannot create multipleInputStream";
    private static final String ERROR_ENCOUNTERED_IS = "Error encountered is ";
    private static final String INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID = "Interrupted after timeout on offer ID ";
    private static final String NO_NEED_TO_RETRY = ", no need to retry";
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

    private final StorageLog storageLogService;

    private final WorkspaceClientFactory workspaceClientFactory;
    private final BulkStorageDistribution bulkStorageDistribution;

    /**
     * Constructs the service with a given configuration
     *
     * @param configuration the configuration of the storage
     * @param storageLogService service that allow write and access log
     */
    StorageDistributionImpl(StorageConfiguration configuration, StorageLog storageLogService)
        throws StorageTechnicalException {
        ParametersChecker.checkParameter(STORAGE_SERVICE_CONFIGURATION_IS_MANDATORY, configuration);
        urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        this.storageLogService = storageLogService;
        digestType = VitamConfiguration.getDefaultDigestType();
        this.transfertTimeoutHelper = new TransfertTimeoutHelper(
            configuration.getTimeoutMsPerKB(),
            configuration.getMinWriteTimeoutMs(),
            configuration.getMinBulkWriteTimeoutMsPerObject()
        );
        this.bulkStorageDistribution = new BulkStorageDistribution(
            3,
            this.workspaceClientFactory,
            this.storageLogService,
            this.transfertTimeoutHelper
        );
        validateStrategyOffers();
    }

    @VisibleForTesting
    StorageDistributionImpl(
        WorkspaceClientFactory workspaceClientFactory,
        DigestType digestType,
        StorageLog storageLogService,
        BulkStorageDistribution bulkStorageDistribution
    ) {
        urlWorkspace = null;
        this.transfertTimeoutHelper = new TransfertTimeoutHelper(100L, 60_000, 10_000);
        this.workspaceClientFactory = workspaceClientFactory;
        this.digestType = digestType;
        this.storageLogService = storageLogService;
        this.bulkStorageDistribution = bulkStorageDistribution;
    }

    @Override
    public StoredInfoResult copyObjectFromOfferToOffer(
        DataContext context,
        String sourceOffer,
        String destinationOffer
    ) throws StorageException {
        //TODO log in log storage bug #4836

        JsonNode containerInformation = getContainerInformation(
            context.getStrategyId(),
            context.getCategory(),
            context.getObjectId(),
            Lists.newArrayList(sourceOffer, destinationOffer),
            false
        );
        //verify source offer
        boolean existsSourceOffer = containerInformation.get(sourceOffer) != null;
        existsSourceOffer = existsSourceOffer && (containerInformation.get(sourceOffer).get(DIGEST) != null);

        if (!existsSourceOffer) {
            throw new StorageNotFoundException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, context.getObjectId())
            );
        }
        Response resp = null;
        try {
            // load the object/file from the given offer
            resp = getContainerByCategory(
                context.getStrategyId(),
                COPY_OBJECT_ORIGIN,
                context.getObjectId(),
                context.getCategory(),
                sourceOffer
            );

            if (resp == null) {
                throw new StorageTechnicalException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR)
                );
            }

            boolean existsDestinationOffer = containerInformation.get(destinationOffer) != null;

            existsDestinationOffer = existsDestinationOffer &&
            (containerInformation.get(destinationOffer).get(DIGEST) != null);

            if (existsDestinationOffer) {
                deleteObjectInOffers(context.getStrategyId(), context, singletonList(destinationOffer));
            }
            if (resp.getStatus() == Response.Status.OK.getStatusCode()) {
                return storeDataInOffers(
                    context.getStrategyId(),
                    NORMAL_ORIGIN,
                    context.getObjectId(),
                    context.getCategory(),
                    context.getRequester(),
                    singletonList(destinationOffer),
                    resp
                );
            }
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(resp);
        }

        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
    }

    private StorageLogbookParameters sendDataToOffersWithRetries(
        DataContext dataContext,
        OffersToCopyIn offersParams,
        ObjectDescription description
    ) throws StorageException {
        AtomicInteger attempt = new AtomicInteger();
        AtomicBoolean needToRetry = new AtomicBoolean(true);

        DelegateRetry<StorageLogbookParameters, StorageException> delegate = () -> {
            try (StreamAndInfo streamAndInfo = getInputStreamFromWorkspace(description)) {
                return sendDataToOffers(
                    streamAndInfo,
                    dataContext,
                    offersParams,
                    NORMAL_ORIGIN,
                    attempt.incrementAndGet(),
                    needToRetry
                );
            } catch (StorageNotFoundException e) {
                // File not found in the workspace
                needToRetry.set(false);
                throw e;
            }
        };

        RetryableOnResult<StorageLogbookParameters, StorageException> retryable = new RetryableOnResult<>(
            retryableParameters,
            p -> needToRetry.get() && !offersParams.getKoOffers().isEmpty()
        );
        return retryable.exec(delegate);
    }

    @Override
    public StoredInfoResult storeDataInOffers(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String requester,
        List<String> offerIds,
        Response response
    ) throws StorageException {
        Long size = Long.valueOf(response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));

        try (StreamAndInfo streamAndInfo = new StreamAndInfo(new VitamAsyncInputStream(response), size)) {
            return this.storeDataInOffers(strategyId, origin, streamAndInfo, objectId, category, requester, offerIds);
        }
    }

    @Override
    public StoredInfoResult storeDataInOffers(
        String strategyId,
        String origin,
        StreamAndInfo streamAndInfo,
        String objectId,
        DataCategory category,
        String requester,
        List<String> offerIds
    ) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();

        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);

        // Retrieve strategy offersToCopyIn
        StorageStrategy storageStrategy = checkStrategy(strategyId);

        List<String> strategyOfferIds = storageStrategy
            .getOffers()
            .stream()
            .map(OfferReference::getId)
            .collect(Collectors.toList());
        List<StorageOffer> offers = new ArrayList<>();

        if (offerIds != null) {
            for (String offerId : offerIds) {
                if (strategyOfferIds.contains(offerId)) {
                    offers.add(OFFER_PROVIDER.getStorageOffer(offerId));
                } else {
                    LOGGER.error("Offer {} ignored : not found in strategy {}", offerId, strategyId);
                }
            }
        } else {
            for (String offerId : strategyOfferIds) {
                offers.add(OFFER_PROVIDER.getStorageOffer(offerId));
            }
        }

        OffersToCopyIn offersToCopyIn = new OffersToCopyIn(offers);

        final DataContext dataContext = new DataContext(objectId, category, requester, tenantId, strategyId);

        //try only once
        StorageLogbookParameters parameters = sendDataToOffers(
            streamAndInfo,
            dataContext,
            offersToCopyIn,
            origin,
            1,
            new AtomicBoolean(false)
        );

        try {
            logStorage(tenantId, parameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // TODO P1 Handle Status result if different for offers
        return buildStoreDataResponse(
            objectId,
            category,
            parameters.getMapParameters().get(StorageLogbookParameterName.digest),
            strategyId,
            offersToCopyIn.getGlobalOfferResult()
        );
    }

    private StorageStrategy checkStrategy(String strategyId)
        throws StorageTechnicalException, StorageNotFoundException {
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);

        if (storageStrategy == null) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        return storageStrategy;
    }

    private StorageLogbookParameters startCopyToOffers(
        DataContext dataContext,
        OffersToCopyIn data,
        final String origin,
        int attempt,
        Long size,
        Digest globalDigest,
        MultiplePipedInputStream streams,
        Map<String, Future<ThreadResponseData>> futureMap
    ) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String requestId = VitamThreadUtils.getVitamSession().getRequestId();

        String offerIdString = null;
        StorageLogbookParameters parameters = null;
        int rank = 0;
        try {
            for (int i = 0; i < data.getKoOffers().size(); i++) {
                offerIdString = data.getKoOffers().get(i);
                OfferReference offerReference = new OfferReference(offerIdString);
                final Driver driver = retrieveDriverInternal(offerIdString);
                InputStream inputStream = new BufferedInputStream(streams.getInputStream(rank));
                InputStream offerInputStream = new UploadCountingInputStreamMetrics(
                    dataContext.getTenantId(),
                    dataContext.getStrategyId(),
                    offerIdString,
                    origin,
                    dataContext.getCategory(),
                    attempt,
                    inputStream
                );

                StoragePutRequest request = new StoragePutRequest(
                    dataContext.getTenantId(),
                    dataContext.getCategory().getFolder(),
                    dataContext.getObjectId(),
                    digestType.getName(),
                    offerInputStream
                );
                futureMap.put(
                    offerIdString,
                    executor.submit(
                        new TransferThread(tenantId, requestId, driver, offerReference, request, globalDigest, size)
                    )
                );
                rank++;
            }
        } catch (StorageException e) {
            LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerIdString, e);
            parameters = setLogbookStorageParameters(
                null,
                offerIdString,
                null,
                dataContext.getRequester(),
                attempt,
                Status.INTERNAL_SERVER_ERROR,
                dataContext.getCategory().getFolder()
            );
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
    public StoredInfoResult storeDataInAllOffers(
        String strategyId,
        String objectId,
        ObjectDescription createObjectDescription,
        DataCategory category,
        String requester
    ) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        checkStoreDataParams(createObjectDescription, strategyId, objectId, category);
        List<StorageOffer> storageOffers = getStorageOffers(strategyId);

        OffersToCopyIn offersToCopyIn = new OffersToCopyIn(storageOffers);
        final DataContext dataContext = new DataContext(objectId, category, requester, tenantId, strategyId);

        StorageLogbookParameters parameters = sendDataToOffersWithRetries(
            dataContext,
            offersToCopyIn,
            createObjectDescription
        );
        ///logging
        try {
            logStorage(tenantId, parameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        // TODO P1 Handle Status result if different for offers
        return buildStoreDataResponse(
            objectId,
            category,
            parameters.getMapParameters().get(StorageLogbookParameterName.digest),
            strategyId,
            offersToCopyIn.getGlobalOfferResult()
        );
    }

    private List<StorageOffer> getStorageOffers(String strategyId)
        throws StorageTechnicalException, StorageNotFoundException {
        StorageStrategy storageStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

        return offerReferences.stream().map(StorageDistributionImpl::apply).collect(Collectors.toList());
    }

    @Override
    public BulkObjectStoreResponse bulkCreateFromWorkspace(
        String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest,
        String requester
    ) throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        List<String> offerIds = getStorageOffers(strategyId)
            .stream()
            .map(StorageOffer::getId)
            .collect(Collectors.toList());

        Map<String, Driver> storageDrivers = new HashMap<>();
        for (String offerId : offerIds) {
            storageDrivers.put(offerId, retrieveDriverInternal(offerId));
        }

        Map<String, StorageOffer> storageOffers = new HashMap<>();
        for (String offerId : offerIds) {
            storageOffers.put(offerId, OFFER_PROVIDER.getStorageOffer(offerId));
        }

        Map<String, String> objectDigests = bulkStorageDistribution.bulkCreateFromWorkspaceWithRetries(
            strategyId,
            tenantId,
            offerIds,
            storageDrivers,
            storageOffers,
            bulkObjectStoreRequest.getType(),
            bulkObjectStoreRequest.getWorkspaceContainerGUID(),
            bulkObjectStoreRequest.getWorkspaceObjectURIs(),
            bulkObjectStoreRequest.getObjectNames(),
            requester
        );

        return new BulkObjectStoreResponse(offerIds, digestType.getName(), objectDigests);
    }

    @Override
    public List<String> getOfferIds(String strategyId) throws StorageException {
        // Retrieve strategy data
        StorageStrategy storageStrategy = checkStrategy(strategyId);
        return storageStrategy.getOffers().stream().map(OfferReference::getId).collect(Collectors.toList());
    }

    @Override
    public Map<String, StorageStrategy> getStrategies() throws StorageException {
        return STRATEGY_PROVIDER.getStorageStrategies();
    }

    @Override
    public Optional<String> createAccessRequestIfRequired(
        String strategyId,
        String optionalOfferId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException {
        if (objectsNames.isEmpty()) {
            LOGGER.debug("No access request required. Empty objectsNames");
            return Optional.empty();
        }

        OfferReference offerReference = selectFirstOffer(strategyId, optionalOfferId);

        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId(), false);

        if (!offer.isAsyncRead()) {
            LOGGER.debug("Offer {} is synchronous, no access request required", offer.getId());
            return Optional.empty();
        }

        LOGGER.debug("Offer {} is asynchronous, creating access request", offer.getId());
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            String accessRequestId = connection.createAccessRequest(
                new StorageAccessRequestCreationRequest(
                    VitamThreadUtils.getVitamSession().getTenantId(),
                    dataCategory.getFolder(),
                    objectsNames
                )
            );

            LOGGER.debug("AccessRequestId '{}' created for asynchronous offer {}", accessRequestId, offer.getId());
            return Optional.of(accessRequestId);
        } catch (StorageDriverException | RuntimeException e) {
            throw new StorageTechnicalException("Could not create AccessRequest", e);
        }
    }

    @Override
    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        String strategyId,
        String optionalOfferId,
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException {
        OfferReference offerReference = selectFirstOffer(strategyId, optionalOfferId);

        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId(), false);
        if (!offer.isAsyncRead()) {
            throw new StorageIllegalOperationException(
                "Expected offer " + offerReference.getId() + " to be asynchronous."
            );
        }

        LOGGER.debug("Checking access request ids for offer {}", offerReference.getId());
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            Map<String, AccessRequestStatus> accessRequestStatuses = connection.checkAccessRequestStatuses(
                accessRequestIds,
                VitamThreadUtils.getVitamSession().getTenantId(),
                adminCrossTenantAccessRequestAllowed
            );

            LOGGER.debug("Access request statuses {}", accessRequestStatuses);
            return accessRequestStatuses;
        } catch (StorageDriverException | RuntimeException e) {
            throw new StorageTechnicalException("Could not check access request ids", e);
        }
    }

    @Override
    public void removeAccessRequest(
        String strategyId,
        String optionalOfferId,
        String accessRequestId,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException {
        OfferReference offerReference = selectFirstOffer(strategyId, optionalOfferId);

        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId(), false);
        if (!offer.isAsyncRead()) {
            throw new StorageIllegalOperationException(
                "Expected offer " + offerReference.getId() + " to be asynchronous."
            );
        }

        LOGGER.debug("Deleting access request id {} from offer {}", accessRequestId, offerReference.getId());
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            connection.removeAccessRequest(
                accessRequestId,
                VitamThreadUtils.getVitamSession().getTenantId(),
                adminCrossTenantAccessRequestAllowed
            );

            LOGGER.debug("Access request removed successfully {}", accessRequestId);
        } catch (StorageDriverException | RuntimeException e) {
            throw new StorageTechnicalException("Could not remove access request " + accessRequestId, e);
        }
    }

    @Override
    public boolean checkObjectAvailability(
        String strategyId,
        String optionalOfferId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException {
        if (objectsNames.isEmpty()) {
            LOGGER.debug("No objects to check.");
            return true;
        }

        OfferReference offerReference = selectFirstOffer(strategyId, optionalOfferId);

        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId(), false);

        if (!offer.isAsyncRead()) {
            LOGGER.debug(
                "Offer {} is synchronous, objects {} are availability for immediate access",
                offer.getId(),
                objectsNames
            );
            return true;
        }

        LOGGER.debug("Offer {} is asynchronous, checking object availability", offer.getId());
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            boolean areObjectsAvailable = connection.checkObjectAvailability(
                new StorageCheckObjectAvailabilityRequest(
                    VitamThreadUtils.getVitamSession().getTenantId(),
                    dataCategory.getFolder(),
                    objectsNames
                )
            );

            if (areObjectsAvailable) {
                LOGGER.debug("Objects {} are AVAILABLE", objectsNames);
            } else {
                LOGGER.debug("Objects {} are NOT AVAILABLE", objectsNames);
            }
            return areObjectsAvailable;
        } catch (StorageDriverException | RuntimeException e) {
            throw new StorageTechnicalException("Could not check object availability", e);
        }
    }

    @Override
    public String getReferentOffer(String strategyId) throws StorageTechnicalException, StorageNotFoundException {
        StorageStrategy storageStrategy = checkStrategy(strategyId);
        return chooseReferentOffer(storageStrategy).getId();
    }

    @Override
    public Response launchOfferLogCompaction(String offerReferenceId, Integer tenantId) throws StorageException {
        final Driver driver = retrieveDriverInternal(offerReferenceId);
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReferenceId);
        try (Connection connection = driver.connect(offer.getId())) {
            return connection.launchOfferLogCompaction(new VitamContext(tenantId));
        } catch (StorageDriverException | RuntimeException e) {
            if (e instanceof StorageDriverPreconditionFailedException) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_BAD_REQUEST), e);
                throw new IllegalArgumentException(e);
            }
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageTechnicalException(e);
        }
    }

    private OfferReference selectFirstOffer(String strategyId, String optionalOfferId)
        throws StorageTechnicalException, StorageNotFoundException, StorageDriverNotFoundException {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);

        // Retrieve / check storage strategy
        StorageStrategy storageStrategy = checkStrategy(strategyId);

        if (optionalOfferId == null) {
            return storageStrategy
                .getOffers()
                .stream()
                .findFirst()
                .orElseThrow(
                    () -> new StorageDriverNotFoundException("No active offer found in strategy " + strategyId)
                );
        } else {
            // Select offer by id
            return storageStrategy
                .getOffers()
                .stream()
                .filter(offerRef -> optionalOfferId.equals(offerRef.getId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new StorageDriverNotFoundException(
                            "No active offer found with Id '" + optionalOfferId + "' in strategy " + strategyId
                        )
                );
        }
    }

    private StorageLogbookParameters sendDataToOffers(
        StreamAndInfo streamAndInfo,
        DataContext dataContext,
        OffersToCopyIn offersParams,
        final String origin,
        final int attempt,
        AtomicBoolean needToRetry
    ) throws StorageTechnicalException {
        StorageLogbookParameters parameters;
        Digest globalDigest = new Digest(digestType);

        try (
            InputStream digestInputStream = globalDigest.getDigestInputStream(streamAndInfo.getStream());
            MultiplePipedInputStream streams = new MultiplePipedInputStream(
                digestInputStream,
                offersParams.getKoOffers().size()
            )
        ) {
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();

            long finalTimeout = transfertTimeoutHelper.getTransferTimeout(streamAndInfo.getSize());
            TimeoutStopwatch timeoutStopwatch = new TimeoutStopwatch(finalTimeout);

            parameters = startCopyToOffers(
                dataContext,
                offersParams,
                origin,
                attempt,
                streamAndInfo.getSize(),
                globalDigest,
                streams,
                futureMap
            );

            // wait for all threads execution
            // TODO: manage interruption and error execution (US #2008 && 2009)
            for (Entry<String, Future<ThreadResponseData>> entry : futureMap.entrySet()) {
                final Future<ThreadResponseData> future = entry.getValue();
                // Check if any has one IO Exception
                streams.throwLastException();
                String offerId = entry.getKey();
                try {
                    ThreadResponseData threadResponseData = future.get(
                        timeoutStopwatch.getRemainingDelayInMilliseconds(),
                        TimeUnit.MILLISECONDS
                    );

                    if (threadResponseData == null) {
                        LOGGER.error(ERROR_ON_OFFER_ID + offerId);
                        parameters = setLogbookStorageParameters(
                            parameters,
                            offerId,
                            null,
                            dataContext.getRequester(),
                            attempt,
                            Status.INTERNAL_SERVER_ERROR,
                            dataContext.getCategory().getFolder()
                        );
                        throw new StorageTechnicalException(NO_MESSAGE_RETURNED);
                    }
                    parameters = setLogbookStorageParameters(
                        parameters,
                        offerId,
                        threadResponseData,
                        dataContext.getRequester(),
                        attempt,
                        threadResponseData.getStatus(),
                        dataContext.getCategory().getFolder()
                    );
                    offersParams.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error(INTERRUPTED_AFTER_TIMEOUT_ON_OFFER_ID + offerId);
                    parameters = setLogbookStorageParameters(
                        parameters,
                        offerId,
                        null,
                        dataContext.getRequester(),
                        attempt,
                        null,
                        dataContext.getCategory().getFolder()
                    );
                } catch (InterruptedException e) {
                    LOGGER.error(INTERRUPTED_ON_OFFER_ID + offerId, e);
                    parameters = setLogbookStorageParameters(
                        parameters,
                        offerId,
                        null,
                        dataContext.getRequester(),
                        attempt,
                        null,
                        dataContext.getCategory().getFolder()
                    );
                } catch (ExecutionException e) {
                    LOGGER.error(StorageDistributionImpl.ERROR_ON_OFFER_ID + offerId, e);
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        status = Status.CONFLICT;
                        offersParams.changeStatus(offerId, status);
                    }
                    parameters = setLogbookStorageParameters(
                        parameters,
                        offerId,
                        null,
                        dataContext.getRequester(),
                        attempt,
                        status,
                        dataContext.getCategory().getFolder()
                    );

                    if (e.getCause() instanceof StorageInconsistentStateException) {
                        LOGGER.error(
                            StorageDistributionImpl.ERROR_ENCOUNTERED_IS + e.getCause().getClass() + NO_NEED_TO_RETRY,
                            e
                        );
                        needToRetry.set(false);
                    } else if (e.getCause() instanceof StorageDriverException) {
                        StorageDriverException ex = (StorageDriverException) e.getCause();
                        if (!ex.isShouldRetry()) {
                            LOGGER.error(
                                StorageDistributionImpl.ERROR_ENCOUNTERED_IS +
                                e.getCause().getClass() +
                                NO_NEED_TO_RETRY,
                                ex
                            );
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
        return parameters;
    }

    private StreamAndInfo getInputStreamFromWorkspace(ObjectDescription createObjectDescription)
        throws StorageTechnicalException, StorageNotFoundException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            return retrieveDataFromWorkspace(
                createObjectDescription.getWorkspaceContainerGUID(),
                createObjectDescription.getWorkspaceObjectURI(),
                workspaceClient
            );
        }
    }

    private StorageLogbookParameters setLogbookStorageParameters(
        StorageLogbookParameters parameters,
        String offerId,
        ThreadResponseData res,
        String requester,
        int attempt,
        Status status,
        String dataCategoty
    ) {
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        if (parameters == null) {
            parameters = getParameters(
                res != null ? res.getObjectGuid() : null,
                dataCategoty,
                res != null ? (StoragePutResult) res.getResponse() : null,
                offerId,
                res != null ? res.getStatus() : status,
                requester,
                attempt
            );
        } else {
            updateStorageLogbookParameters(parameters, offerId, res != null ? res.getStatus() : status, attempt);
        }
        return parameters;
    }

    private void logStorage(Integer tenant, StorageLogbookParameters parameters) throws IOException {
        storageLogService.appendWriteLog(tenant, parameters);
    }

    private StoredInfoResult buildStoreDataResponse(
        String objectId,
        DataCategory category,
        String digest,
        String strategy,
        Map<String, Status> offerResults
    ) throws StorageTechnicalException, StorageAlreadyExistsException {
        final String offerIds = String.join(", ", offerResults.keySet());
        // Aggregate result of all store actions. If all went well, allSuccess is true, false if one action failed
        final boolean allWithoutInternalServerError = offerResults
            .values()
            .stream()
            .noneMatch(Status.INTERNAL_SERVER_ERROR::equals);
        final boolean allWithoutAlreadyExists = offerResults.values().stream().noneMatch(Status.CONFLICT::equals);

        if (!allWithoutInternalServerError) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CANT_STORE_OBJECT, objectId, offerIds));
            throw new StorageTechnicalException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CANT_STORE_OBJECT, objectId, offerIds)
            );
        } else if (!allWithoutAlreadyExists) {
            LOGGER.error(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS, objectId, offerIds)
            );
            throw new StorageAlreadyExistsException(
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS, objectId, offerIds)
            );
        }

        // TODO P1 Witch status code return if an offer is updated (Status.OK)
        // and another is created (Status.CREATED) ?
        final StoredInfoResult result = new StoredInfoResult();
        final String now = LocalDateUtil.nowFormatted();
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
            case ARCHIVAL_TRANSFER_REPLY:
                description.append("ARCHIVAL_TRANSFER_REPLY ");
                break;
            case TMP:
                description.append("TMP ");
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
        result.setCreationTime(now);
        result.setLastAccessTime(now);
        result.setLastCheckedTime(now);
        result.setLastModifiedTime(now);
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
     * @param dataCategory category
     * @param putObjectResult the response
     * @param offerId the offerId
     * @param objectStored the operation status
     * @return storage logbook parameters
     */
    private StorageLogbookParameters getParameters(
        String objectGuid,
        String dataCategory,
        StoragePutResult putObjectResult,
        String offerId,
        Status objectStored,
        String requester,
        int attempt
    ) {
        final String objectIdentifier = objectGuid != null ? objectGuid : "objectRequest NA";
        final String messageDig = putObjectResult != null ? putObjectResult.getDigestHashBase16() : "messageDigest NA";
        final String size = putObjectResult != null ? String.valueOf(putObjectResult.getObjectSize()) : "Size NA";
        boolean error = objectStored == Status.INTERNAL_SERVER_ERROR;
        final StorageLogbookOutcome outcome = error ? StorageLogbookOutcome.KO : StorageLogbookOutcome.OK;

        return StorageLogbookParameters.createLogParameters(
            objectIdentifier,
            dataCategory,
            messageDig,
            digestType.getName(),
            size,
            getAttemptLog(offerId, attempt, error),
            requester,
            outcome
        );
    }

    private String getAttemptLog(String offerId, int attempt, boolean error) {
        return offerId + ATTEMPT + attempt + " : " + (error ? "KO" : "OK");
    }

    private void updateStorageLogbookParameters(
        StorageLogbookParameters parameters,
        String offerId,
        Status status,
        int attempt
    ) {
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

    private void checkStoreDataParams(
        ObjectDescription createObjectDescription,
        String strategyId,
        String dataId,
        DataCategory category
    ) {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, dataId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        ParametersChecker.checkParameter(OBJECT_ADDITIONAL_INFORMATION_GUID_IS_MANDATORY, createObjectDescription);
        ParametersChecker.checkParameter(
            CONTAINER_GUID_IS_MANDATORY,
            createObjectDescription.getWorkspaceContainerGUID()
        );
        ParametersChecker.checkParameter(
            OBJECT_URI_IN_WORKSPACE_IS_MANDATORY,
            createObjectDescription.getWorkspaceObjectURI()
        );
    }

    private StreamAndInfo retrieveDataFromWorkspace(
        String containerGUID,
        String objectURI,
        WorkspaceClient workspaceClient
    ) throws StorageNotFoundException, StorageTechnicalException {
        Response response;
        try {
            response = workspaceClient.getObject(containerGUID, objectURI);

            String length = response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName());
            Object entity = response.getEntity();
            if (entity == null) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID));
                throw new StorageNotFoundException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID)
                );
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
        StorageStrategy storageStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

        ArrayNode resultArray = JsonHandler.createArrayNode();

        for (OfferReference offerReference : offerReferences) {
            resultArray.add(getOfferInformation(offerReference, tenantId, offerReferences.size()));
        }

        return JsonHandler.createObjectNode().set(CAPACITIES, resultArray);
    }

    private List<OfferReference> checkCoherentOfferRanksAndSort(List<OfferReference> offerReferences)
        throws StorageTechnicalException {
        if (
            offerReferences.size() !=
            offerReferences
                .stream()
                .filter(elmt -> null != elmt.getRank())
                .map(OfferReference::getRank)
                .distinct()
                .count()
        ) {
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_EXCEPTION_RANK));
        }
        return offerReferences
            .stream()
            .sorted(Comparator.comparing(OfferReference::getRank))
            .collect(Collectors.toList());
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

    private List<OfferReference> getOfferListFromHotStrategy(StorageStrategy storageStrategy)
        throws StorageTechnicalException {
        final List<OfferReference> declaredOffers = new ArrayList<>();
        if (storageStrategy != null && !storageStrategy.getOffers().isEmpty()) {
            declaredOffers.addAll(storageStrategy.getOffers());
        }

        if (declaredOffers.isEmpty()) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }
        return declaredOffers;
    }

    private OfferReference chooseReferentOffer(StorageStrategy storageStrategy) {
        if (storageStrategy != null && !storageStrategy.getOffers().isEmpty()) {
            List<OfferReference> offerReferences = storageStrategy
                .getOffers()
                .stream()
                .filter(OfferReference::isReferent)
                .collect(Collectors.toList());
            return Iterables.getOnlyElement(offerReferences);
        }
        throw new IllegalArgumentException("Exactly one offer should be declared as 'referent' in hot strategy");
    }

    @Override
    public CloseableIterator<ObjectEntry> listContainerObjects(String strategyId, DataCategory category)
        throws StorageException {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        if (storageStrategy != null) {
            final List<OfferReference> orderedOffersByPriority = getOfferListFromHotStrategy(storageStrategy);

            if (orderedOffersByPriority.isEmpty()) {
                LOGGER.error("No offer found");
                throw new StorageTechnicalException("No offer found");
            }

            return listContainerObjectsForOffer(category, orderedOffersByPriority.get(0).getId(), false);
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    @Override
    public CloseableIterator<ObjectEntry> listContainerObjectsForOffer(
        DataCategory category,
        String offerId,
        boolean includeDisabled
    ) throws StorageException {
        // Check offer exists
        OFFER_PROVIDER.getStorageOffer(offerId, includeDisabled);

        final Driver driver = retrieveDriverInternal(offerId);
        try (Connection connection = driver.connect(offerId)) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            StorageListRequest request = new StorageListRequest(tenantId, category.getFolder());
            return connection.listObjects(request);
        } catch (final fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException exc) {
            throw new StorageDriverNotFoundException(exc);
        } catch (final StorageDriverException exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(
        String strategyId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
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
    public RequestResponse<OfferLog> getOfferLogsByOfferId(
        String strategyId,
        String offerId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
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
    private RequestResponse<OfferLog> getOfferLogRequestResponse(
        String strategyId,
        String offerId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);

        if (storageStrategy == null) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        if (offerId == null) {
            final OfferReference priorizedOffer = selectFirstOffer(storageStrategy.getId(), null);
            if (priorizedOffer != null) {
                offerId = priorizedOffer.getId();
            } else {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
        }

        // get the storage offer from the given identifier
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);

        final Driver driver = retrieveDriverInternal(offer.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            StorageOfferLogRequest request = new StorageOfferLogRequest(
                tenantId,
                category.getFolder(),
                offset,
                limit,
                order
            );
            return connection.getOfferLogs(request);
        } catch (final StorageDriverException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageTechnicalException(e);
        }
    }

    @Override
    public Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        AccessLogInfoModel logInformation
    ) throws StorageException {
        return getContainerByCategoryResponse(strategyId, origin, objectId, category, null, logInformation);
    }

    @Override
    public Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String offerId
    ) throws StorageException {
        return getContainerByCategoryResponse(
            strategyId,
            origin,
            objectId,
            category,
            offerId,
            AccessLogUtils.getNoLogAccessLog()
        );
    }

    /**
     * getContainerByCategoryResponse.
     *
     * @param strategyId strategyId
     * @param origin origin
     * @param objectId objectId
     * @param category category
     * @return Response
     * @throws StorageException the exception
     */
    private Response getContainerByCategoryResponse(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String offerId,
        AccessLogInfoModel logInformation
    ) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);

        StorageStrategy storageStrategy = checkStrategy(strategyId);

        List<StorageOffer> storageOffers = new ArrayList<>();

        if (StringUtils.isBlank(offerId)) {
            // FIXME : #9032 : Handle failover over async offers
            final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

            List<StorageOffer> collect = offerReferences
                .stream()
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

        return getObjectResult(tenantId, strategyId, origin, objectId, category, storageOffers);
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
     * @param strategyId strategyId
     * @param origin origin
     * @param objectId objectID
     * @param type type
     * @param storageOffers storageOffer
     * @return StorageGetResult
     * @throws StorageException the exception
     */
    private Response getObjectResult(
        Integer tenantId,
        String strategyId,
        String origin,
        String objectId,
        DataCategory type,
        List<StorageOffer> storageOffers
    ) throws StorageException {
        StorageGetResult result;

        for (final StorageOffer storageOffer : storageOffers) {
            final Driver driver = retrieveDriverInternal(storageOffer.getId());
            try (Connection connection = driver.connect(storageOffer.getId())) {
                final StorageObjectRequest request = new StorageObjectRequest(tenantId, type.getFolder(), objectId);
                RetryableOnException<StorageGetResult, StorageDriverException> retryable = new RetryableOnException<>(
                    retryableParameters,
                    exception ->
                        exception instanceof StorageDriverServiceUnavailableException ||
                        exception instanceof StorageDriverServerErrorException
                );
                result = retryable.exec(() -> connection.getObject(request));

                Response response = result.getObject();
                if (response != null) {
                    return new DownloadCountingSizeMetricsResponse(
                        tenantId,
                        strategyId,
                        storageOffer.getId(),
                        origin,
                        type,
                        response
                    );
                }
            } catch (final fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException exc) {
                throw new StorageNotFoundException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId),
                    exc
                );
            } catch (final StorageDriverUnavailableDataFromAsyncOfferException exc) {
                throw new StorageUnavailableDataFromAsyncOfferException(
                    "Access not acceptable for object '" +
                    type.getFolder() +
                    "/" +
                    objectId +
                    "' from offer " +
                    storageOffer.getId() +
                    " of strategy " +
                    strategyId,
                    exc
                );
            } catch (final StorageDriverException exc) {
                LOGGER.warn(ERROR_WITH_THE_STORAGE_TAKE_THE_NEXT_OFFER_IN_THE_STRATEGY_BY_PRIORITY, exc);
            }
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
    }

    @Override
    public JsonNode getContainerInformation(
        String strategyId,
        DataCategory type,
        String objectId,
        List<String> offerIds,
        boolean noCache
    ) throws StorageException {
        ObjectNode offerIdToMetadata = JsonHandler.createObjectNode();

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);

        StorageStrategy storageStrategy = checkStrategy(strategyId);
        // TODO gafou usage ?
        getOfferListFromHotStrategy(storageStrategy);

        for (final String offerId : offerIds) {
            final Driver driver = retrieveDriverInternal(offerId);
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
            try (Connection connection = driver.connect(offer.getId())) {
                final StorageGetMetadataRequest request = new StorageGetMetadataRequest(
                    tenantId,
                    type.getFolder(),
                    objectId,
                    noCache
                );

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

    /**
     * Verify if object exists
     *
     * @param strategyId id of the strategy
     * @param objectId id of the object
     * @param category category
     * @param offerIds list id of offers  @return
     * @throws StorageException StorageException
     */
    @Override
    public Map<String, Boolean> checkObjectExisting(
        String strategyId,
        String objectId,
        DataCategory category,
        List<String> offerIds
    ) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, objectId);
        ParametersChecker.checkParameter(OFFER_IDS_IS_MANDATORY, offerIds);

        Map<String, Boolean> resultByOffer = new HashMap<>();

        StorageStrategy storageStrategy = checkStrategy(strategyId);
        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);
        List<String> offerReferencesIds = offerReferences
            .stream()
            .map(OfferReference::getId)
            .collect(Collectors.toList());
        resultByOffer.putAll(
            offerIds
                .stream()
                .filter(offer -> !offerReferencesIds.contains(offer))
                .collect(Collectors.toMap(offerId -> offerId, offerId -> Boolean.FALSE))
        );

        for (final String offerId : offerIds) {
            if (!resultByOffer.containsKey(offerId)) {
                final Driver driver = retrieveDriverInternal(offerId);
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
                try (Connection connection = driver.connect(offer.getId())) {
                    final StorageObjectRequest request = new StorageObjectRequest(
                        tenantId,
                        category.getFolder(),
                        objectId
                    );
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

    @Override
    public void deleteObjectInAllOffers(String strategyId, DataContext context) throws StorageException {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, context.getObjectId());

        StorageStrategy storageStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

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
        StorageStrategy storageStrategy = checkStrategy(strategyId);

        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

        final List<OfferReference> offerReferencesToDelete = new ArrayList<>();

        for (String offerId : offers) {
            Optional<OfferReference> found = offerReferences
                .stream()
                .filter(offerReference -> offerReference.getId().equals(offerId))
                .findFirst();

            found.ifPresent(offerReferencesToDelete::add);

            if (found.isEmpty()) {
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
        }

        deleteObject(context, offerReferencesToDelete);
    }

    private void deleteObject(DataContext context, List<OfferReference> offerReferences) throws StorageException {
        Map<String, StorageLogbookOutcome> deleteOutcomeByOfferId = new HashMap<>();

        for (final OfferReference offerReference : offerReferences) {
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            try {
                Retryable<Void, StorageTechnicalException> retryable = new RetryableOnException<>(
                    retryableParameters,
                    e -> e instanceof StorageTechnicalException
                );
                retryable.execute(
                    () ->
                        deleteObject(context.getObjectId(), context.getTenantId(), driver, offer, context.getCategory())
                );

                deleteOutcomeByOfferId.put(driver.getName(), StorageLogbookOutcome.OK);
            } catch (Exception e) {
                LOGGER.error(
                    String.format(
                        "An error occurred during object delete %s/%s from offer %s",
                        context.getCategory(),
                        context.getObjectId(),
                        driver.getName()
                    ),
                    e
                );
                deleteOutcomeByOfferId.put(driver.getName(), StorageLogbookOutcome.KO);
            }
        }

        String offerDetails = deleteOutcomeByOfferId
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));

        StorageLogbookOutcome globalOutcome = deleteOutcomeByOfferId
            .values()
            .stream()
            .max(Enum::compareTo)
            .orElse(StorageLogbookOutcome.KO);

        StorageLogbookParameters deleteLogParameters = buildDeleteLogParameters(
            context.getObjectId(),
            context.getCategory().getFolder(),
            offerDetails,
            context.getRequester(),
            globalOutcome
        );

        // Logging
        try {
            logStorage(context.getTenantId(), deleteLogParameters);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        if (globalOutcome != StorageLogbookOutcome.OK) {
            throw new StorageException(
                String.format(
                    "Could not delete object %s/%s. %s",
                    context.getCategory(),
                    context.getObjectId(),
                    offerDetails
                )
            );
        }
    }

    private void deleteObject(
        String objectId,
        Integer tenantId,
        Driver driver,
        StorageOffer offer,
        DataCategory category
    ) throws StorageTechnicalException {
        try (Connection connection = driver.connect(offer.getId())) {
            StorageRemoveRequest request = new StorageRemoveRequest(tenantId, category.getFolder(), objectId);
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

    private StorageLogbookParameters buildDeleteLogParameters(
        String objectIdentifier,
        String dataCategory,
        String agentIdentifiers,
        String agentIdentifierRequester,
        StorageLogbookOutcome outcome
    ) {
        final Map<StorageLogbookParameterName, String> mandatoryParameters = new TreeMap<>();
        mandatoryParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        mandatoryParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        mandatoryParameters.put(StorageLogbookParameterName.objectIdentifier, objectIdentifier);
        mandatoryParameters.put(StorageLogbookParameterName.dataCategory, dataCategory);
        mandatoryParameters.put(StorageLogbookParameterName.eventType, "DELETE");
        mandatoryParameters.put(
            StorageLogbookParameterName.xRequestId,
            VitamThreadUtils.getVitamSession().getRequestId()
        );
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifiers, agentIdentifiers);
        mandatoryParameters.put(StorageLogbookParameterName.tenantId, ParameterHelper.getTenantParameter().toString());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifierRequester, agentIdentifierRequester);

        return StorageLogbookParameters.buildDeleteLogParameters(mandatoryParameters);
    }

    @Override
    public List<BatchObjectInformationResponse> getBatchObjectInformation(
        String strategyId,
        DataCategory type,
        List<String> objectIds,
        List<String> offerIds
    ) throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);

        StorageStrategy storageStrategy = checkStrategy(strategyId);
        final List<OfferReference> offerReferences = getOfferListFromHotStrategy(storageStrategy);

        List<String> offerReferencesIds = offerReferences
            .stream()
            .map(OfferReference::getId)
            .collect(Collectors.toList());
        if (!offerReferencesIds.containsAll(offerIds)) {
            List<String> missingOfferIds = offerIds
                .stream()
                .filter(id -> !offerReferencesIds.contains(id))
                .collect(Collectors.toList());
            throw new StorageException("Invalid offer ids " + missingOfferIds + "for strategy " + strategyId);
        }

        Map<String, Driver> driverByOfferId = new HashMap<>();
        Map<String, StorageOffer> storageOfferByOfferId = new HashMap<>();

        for (String offerId : offerIds) {
            driverByOfferId.put(offerId, retrieveDriverInternal(offerId));
            storageOfferByOfferId.put(offerId, OFFER_PROVIDER.getStorageOffer(offerId));
        }

        Map<String, CompletableFuture<List<StorageBulkMetadataResultEntry>>> completableFutures = new HashMap<>();
        for (String offerId : offerIds) {
            CompletableFuture<List<StorageBulkMetadataResultEntry>> objectInformationCompletableFuture =
                CompletableFuture.supplyAsync(
                    () ->
                        getBatchObjectInformation(
                            type,
                            tenantId,
                            objectIds,
                            driverByOfferId.get(offerId),
                            storageOfferByOfferId.get(offerId)
                        )
                );
            completableFutures.put(offerId, objectInformationCompletableFuture);
        }

        try {
            Map<String, Map<String, String>> offerDigestsByObjectId = new HashMap<>();

            for (String offerId : completableFutures.keySet()) {
                List<StorageBulkMetadataResultEntry> storageBulkMetadataResultEntries = completableFutures
                    .get(offerId)
                    .get();

                for (StorageBulkMetadataResultEntry storageBulkMetadataResultEntry : storageBulkMetadataResultEntries) {
                    offerDigestsByObjectId
                        .computeIfAbsent(storageBulkMetadataResultEntry.getObjectName(), objectId -> new HashMap<>())
                        .put(offerId, storageBulkMetadataResultEntry.getDigest());
                }
            }

            return offerDigestsByObjectId
                .keySet()
                .stream()
                .map(
                    objectId -> new BatchObjectInformationResponse(type, objectId, offerDigestsByObjectId.get(objectId))
                )
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            // Abort pending tasks
            completableFutures.values().forEach(future -> future.cancel(false));
            throw new StorageException("Batch object information failed", e);
        }
    }

    private List<StorageBulkMetadataResultEntry> getBatchObjectInformation(
        DataCategory type,
        Integer tenantId,
        List<String> objectIds,
        Driver driver,
        StorageOffer offer
    ) {
        try (Connection connection = driver.connect(offer.getId())) {
            // Get object metadata (cache enabled)
            final StorageGetBulkMetadataRequest request = new StorageGetBulkMetadataRequest(
                tenantId,
                type.getFolder(),
                objectIds,
                false
            );

            StorageBulkMetadataResult metaData = connection.getBulkMetadata(request);
            return metaData.getObjectMetadata();
        } catch (StorageDriverException | InvalidParseOperationException e) {
            throw new VitamRuntimeException(
                "Could not retrieve batch object information for offer " + offer.getId(),
                e
            );
        }
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

        mapParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());

        return new AccessLogParameters(mapParameters);
    }

    private void validateStrategyOffers() throws StorageTechnicalException {
        for (Entry<String, StorageStrategy> strategyEntry : getStrategyProvider().getStorageStrategies().entrySet()) {
            strategyEntry.getValue().setOffers(checkCoherentOfferRanksAndSort(strategyEntry.getValue().getOffers()));
        }
    }

    protected StorageStrategyProvider getStrategyProvider() {
        return STRATEGY_PROVIDER;
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
