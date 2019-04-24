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

import java.io.BufferedInputStream;
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
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultiplePipedInputStream;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.model.StorageCheckRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
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
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogProvider;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * StorageDistribution service Implementation process continue if needed)
 */
// TODO P1: see what to do with RuntimeException (catch it and log it to let the
public class StorageDistributionImpl implements StorageDistribution {
    private static final String DEFAULT_SIZE_WHEN_UNKNOWN = "1000000";
    private static final int DEFAULT_MINIMUM_TIMEOUT = 60000;
    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final String CATEGORY_IS_MANDATORY = "Category (object type) is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageDistributionImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
            StorageStrategyProviderFactory.getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    private static final int NB_RETRY = 3;
    private static final String SIZE_KEY = "size";
    private static final String STREAM_KEY = "stream";
    private static final String RESPONSE_KEY = "response";

    /**
     * Global pool thread
     */
    static final ExecutorService executor = new VitamThreadPoolExecutor();

    /**
     * Used to wait for all task submission (executorService)
     */
    private static final long THREAD_SLEEP = 1;
    private static final String NO_OFFER_IDENTIFIER_SPECIFIED_THIS_IS_MANDATORY =
            "No offer identifier specified, this is mandatory";
    private final String urlWorkspace;
    private final Integer millisecondsPerKB;
    // TODO P2 see API
    // TODO P2 : later, the digest type may be retrieve via REST parameters. Fot
    // the moment (as of US 72 dev) there is
    // no
    // specification about that
    private final DigestType digestType;
    // FOR JUNIT TEST ONLY (TODO P1: review WorkspaceClientFactory to offer a
    // mocked WorkspaceClient)
    private final WorkspaceClient mockedWorkspaceClient;
    private StorageLogProvider storageLogService;


    /**
     * Constructs the service with a given configuration
     *
     * @param configuration     the configuration of the storage
     * @param storageLogService
     */
    public StorageDistributionImpl(StorageConfiguration configuration, StorageLogProvider storageLogService) {
        ParametersChecker.checkParameter("Storage service configuration is mandatory", configuration);
        urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
        millisecondsPerKB = configuration.getTimeoutMsPerKB();
        mockedWorkspaceClient = null;
        this.storageLogService = storageLogService;
        // TODO P2 : a real design discussion is needed : should we force it ?
        // Should we negociate it with the offer ?
        // TODO P2 Might be negotiated but limited to available digestType from
        // Vitam (MD5, SHA-1, SHA-256, SHA-512,
        // ...)
        // Just to note, I prefer SHA-512 (more CPU but more accurate and
        // already the default for Vitam, notably to
        // allow check of duplicated files)
        digestType = VitamConfiguration.getDefaultDigestType();
    }

    /**
     * For JUnit ONLY
     *
     * @param wkClient   a custom instance of workspace client
     * @param digestType a custom digest
     */
    StorageDistributionImpl(WorkspaceClient wkClient, DigestType digestType, StorageLogProvider storageLogService) {
        urlWorkspace = null;
        millisecondsPerKB = 100;
        mockedWorkspaceClient = wkClient;
        this.digestType = digestType;
        this.storageLogService = storageLogService;
    }

    @Override
    public StoredInfoResult storeData(String strategyId, String objectId, DataCategory category, String requester,
                                      String offerId, Response response)
            throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();

        if (!StringUtils.isBlank(offerId)) {
            ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
            ParametersChecker.checkParameter("Object id is mandatory", objectId);
            ParametersChecker.checkParameter("Category is mandatory", category);
        }

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);

            // get the storage offer from the given identifier
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
            final List<StorageOffer> storageOffers = Arrays.asList(offer);

            TryAndRetryData datas = new TryAndRetryData();
            datas.populateFromOffers(storageOffers);

            Map<String, Object> streamAndInfos = new HashMap<>();
            streamAndInfos.put(SIZE_KEY, response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
            streamAndInfos.put(STREAM_KEY, response.getEntity());
            streamAndInfos.put(RESPONSE_KEY, response);

            StorageLogbookParameters parameters =
                    tryAndRetry(objectId, category, requester, tenantId,
                            datas, 1, null, streamAndInfos);

            // TODO P1 Handle Status result if different for offers
            return buildStoreDataResponse(objectId, category,
                    parameters.getMapParameters().get(StorageLogbookParameterName.digest),
                    strategyId, datas.getGlobalOfferResult());
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private StorageLogbookParameters tryAndRetry(String objectId,
                                                 DataCategory category, String requester, Integer tenantId, TryAndRetryData datas, int attempt,
                                                 StorageLogbookParameters parameters, Map<String, Object> streamAndInfos)
            throws StorageTechnicalException, StorageNotFoundException, StorageAlreadyExistsException {
        Digest globalDigest = new Digest(digestType);
        InputStream digestInputStream = globalDigest.getDigestInputStream((InputStream) streamAndInfos.get(STREAM_KEY));
        Response response = (Response) streamAndInfos.get(RESPONSE_KEY);
        Digest digest = new Digest(digestType);
        try (MultiplePipedInputStream streams = getMultipleInputStreamFromWorkspace(digestInputStream,
                datas.getKoList().size(),
                digest)) {
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();
            int rank = 0;
            String offerId2 = null;
            long finalTimeout = getTransferTimeout(Long.valueOf((String) streamAndInfos.get(SIZE_KEY)));
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
                            executor.submit(new TransferThread(driver, offerReference, request, globalDigest,
                                    Long.valueOf((String) streamAndInfos.get(SIZE_KEY)))));
                    rank++;
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Wrong number on wait on offer ID " + offerId2, e);
                parameters = setLogbookStorageParameters(parameters, offerId2, null, requester, attempt,
                        Status.INTERNAL_SERVER_ERROR);
            } catch (StorageException e) {
                LOGGER.error("Interrupted on offer ID " + offerId2, e);
                parameters = setLogbookStorageParameters(parameters, offerId2, null, requester, attempt,
                        Status.INTERNAL_SERVER_ERROR);
            }

            // wait all tasks submission
            try {
                Thread.sleep(THREAD_SLEEP);
            } catch (InterruptedException exc) {
                DefaultClient.staticConsumeAnyEntityAndClose(response);
                LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
                for (String offerId : futureMap.keySet()) {
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            Status.INTERNAL_SERVER_ERROR);
                }
                if (!datas.getKoList().isEmpty()) {
                    try {
                        deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
                    } catch (StorageTechnicalException e) {
                        LOGGER.error("Cannot delete object {}", objectId, e);
                        throw e;
                    }
                }
                return parameters;
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
                        parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                                Status.INTERNAL_SERVER_ERROR);
                        DefaultClient.staticConsumeAnyEntityAndClose(response);
                        throw new StorageTechnicalException("No message returned");
                    }
                    parameters =
                            setLogbookStorageParameters(parameters, offerId, threadResponseData, requester, attempt,
                                    threadResponseData.getStatus());
                    datas.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error("Interrupted after timeout on offer ID " + offerId);
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted on offer ID " + offerId, e);
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                } catch (ExecutionException e) {
                    LOGGER.error("Error on offer ID " + offerId, e);
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        status = Status.CONFLICT;
                        datas.changeStatus(offerId, status);
                    }
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt, status);
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
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                }
            }
            // Check if any has one IO Exception
            streams.throwLastException();
        } catch (IOException e1) {
            LOGGER.error("Cannot create multipleInputStream", e1);
            parameters = setLogbookStorageParameters(parameters, "none", null, requester, attempt,
                    Status.INTERNAL_SERVER_ERROR);
            DefaultClient.staticConsumeAnyEntityAndClose(response);
            throw new StorageTechnicalException("Cannot create multipleInputStream", e1);
        }
        DefaultClient.staticConsumeAnyEntityAndClose(response);
        // ACK to prevent retry
        if (attempt < NB_RETRY && !datas.getKoList().isEmpty()) {
            attempt++;
            tryAndRetry(objectId, category, requester, tenantId, datas, attempt, parameters, streamAndInfos);
        }

        // TODO : error management (US #2009)
        if (!datas.getKoList().isEmpty()) {
            deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
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
    public StoredInfoResult storeData(String strategyId, String objectId,
                                      ObjectDescription createObjectDescription, DataCategory category, String requester)
            throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        checkStoreDataParams(createObjectDescription, strategyId, objectId, category);
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
                    .map(StorageDistributionImpl::apply)
                    .collect(Collectors.toList());

            datas.populateFromOffers(storageOffers);

            StorageLogbookParameters parameters =
                    tryAndRetry(objectId, createObjectDescription, category, requester, tenantId,
                            datas, 1, null);

            try {
                logStorage(tenantId, parameters);
            } catch (IOException e) {
                LOGGER.error(e);
            }

            // TODO P1 Handle Status result if different for offers
            return buildStoreDataResponse(objectId, category,
                    parameters.getMapParameters().get(StorageLogbookParameterName.digest),
                    strategyId, datas.getGlobalOfferResult());
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    @Override
    public List<String> getOfferIds(String strategyId) throws StorageException {

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        return hotStrategy.getOffers().stream().map(OfferReference::getId).collect(Collectors.toList());
    }

    private StorageLogbookParameters tryAndRetry(String objectId, ObjectDescription createObjectDescription,
                                                 DataCategory category, String requester, Integer tenantId, TryAndRetryData datas, int attempt,
                                                 StorageLogbookParameters parameters)
            throws StorageTechnicalException, StorageNotFoundException, StorageAlreadyExistsException {

        Map<String, Object> streamAndInfos = getInputStreamFromWorkspace(createObjectDescription);

        Digest globalDigest = new Digest(digestType);
        InputStream digestInputStream = globalDigest.getDigestInputStream((InputStream) streamAndInfos.get(STREAM_KEY));
        Response response = (Response) streamAndInfos.get(RESPONSE_KEY);
        Digest digest = new Digest(digestType);
        try (MultiplePipedInputStream streams = getMultipleInputStreamFromWorkspace(digestInputStream,
                datas.getKoList().size(),
                digest)) {
            // init thread and make future map
            // Map here to keep offerId linked to Future
            Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();
            int rank = 0;
            String offerId2 = null;
            long finalTimeout = getTransferTimeout(Long.valueOf((String) streamAndInfos.get(SIZE_KEY)));
            try {
                for (final String offerId : datas.getKoList()) {
                    offerId2 = offerId;
                    OfferReference offerReference = new OfferReference();
                    offerReference.setId(offerId);
                    final Driver driver = retrieveDriverInternal(offerReference.getId());
                    StoragePutRequest request =
                            new StoragePutRequest(tenantId, category.getFolder(), objectId, digestType.getName(),
                                    streams.getInputStream(rank));
                    futureMap.put(offerReference.getId(),
                            executor.submit(new TransferThread(driver, offerReference, request, globalDigest,
                                    Long.valueOf((String) streamAndInfos.get(SIZE_KEY)))));
                    rank++;
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Wrong number on wait on offer ID " + offerId2, e);
                parameters = setLogbookStorageParameters(parameters, offerId2, null, requester, attempt,
                        Status.INTERNAL_SERVER_ERROR);
            } catch (StorageException e) {
                LOGGER.error("Interrupted on offer ID " + offerId2, e);
                parameters = setLogbookStorageParameters(parameters, offerId2, null, requester, attempt,
                        Status.INTERNAL_SERVER_ERROR);
            }

            // wait all tasks submission
            try {
                Thread.sleep(THREAD_SLEEP);
            } catch (InterruptedException exc) {
                DefaultClient.staticConsumeAnyEntityAndClose(response);
                LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
                for (String offerId : futureMap.keySet()) {
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            Status.INTERNAL_SERVER_ERROR);
                }
                if (!datas.getKoList().isEmpty()) {
                    try {
                        deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
                    } catch (StorageTechnicalException e) {
                        LOGGER.error("Cannot delete object {}", objectId, e);
                        throw e;
                    }
                }
                return parameters;
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
                        parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                                Status.INTERNAL_SERVER_ERROR);
                        DefaultClient.staticConsumeAnyEntityAndClose(response);
                        throw new StorageTechnicalException("No message returned");
                    }
                    parameters =
                            setLogbookStorageParameters(parameters, offerId, threadResponseData, requester, attempt,
                                    threadResponseData.getStatus());
                    datas.koListToOkList(offerId);
                } catch (TimeoutException e) {
                    LOGGER.info("Timeout on offer ID {} TimeOut: {}", offerId, finalTimeout, e);
                    future.cancel(true);
                    // TODO: manage thread to take into account this interruption
                    LOGGER.error("Interrupted after timeout on offer ID " + offerId);
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted on offer ID " + offerId, e);
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                } catch (ExecutionException e) {
                    LOGGER.error("Error on offer ID " + offerId, e);
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    if (e.getCause() instanceof StorageDriverConflictException) {
                        status = Status.CONFLICT;
                        datas.changeStatus(offerId, status);
                    }
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt, status);

                    if (e.getCause() instanceof StorageDriverException) {
                        StorageDriverException ex = (StorageDriverException) e.getCause();
                        if (!ex.isShouldRetry()) {
                            LOGGER.error("Error encountered is " + e.getCause().getClass() + ", no need to retry");
                            attempt = NB_RETRY;
                        }
                    }
                    // TODO: review this exception to manage errors correctly
                    // Take into account Exception class
                    // For example, for particular exception do not retry (because
                    // it's useless)
                    // US : #2009
                } catch (NumberFormatException e) {
                    future.cancel(true);
                    LOGGER.error("Wrong number on wait on offer ID " + offerId, e);
                    parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt,
                            null);
                }
            }

            // Check if any has one IO Exception
            streams.throwLastException();
        } catch (IOException e1) {
            throw new StorageTechnicalException("Cannot create multipleInputStream", e1);
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
        // ACK to prevent retry
        if (attempt < NB_RETRY && !datas.getKoList().isEmpty()) {
            attempt++;
            tryAndRetry(objectId, createObjectDescription, category, requester, tenantId, datas, attempt, parameters);
        }

        // TODO : error management (US #2009)
        if (!datas.getKoList().isEmpty()) {
            deleteObjects(datas.getOkList(), tenantId, category, objectId, digest);
        }

        return parameters;
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

    private Map<String, Object> getInputStreamFromWorkspace(ObjectDescription createObjectDescription)
            throws StorageTechnicalException, StorageNotFoundException {
        try (WorkspaceClient workspaceClient =
                     mockedWorkspaceClient == null ? WorkspaceClientFactory.getInstance().getClient()
                             : mockedWorkspaceClient) {

            return retrieveDataFromWorkspace(createObjectDescription.getWorkspaceContainerGUID(),
                    createObjectDescription.getWorkspaceObjectURI(), workspaceClient);
        }

    }

    private MultiplePipedInputStream getMultipleInputStreamFromWorkspace(InputStream stream, int nbCopy,
                                                                         Digest digest)
            throws StorageTechnicalException, StorageNotFoundException, IOException {
        DigestInputStream digestOriginalStream = (DigestInputStream) digest.getDigestInputStream(stream);
        return new MultiplePipedInputStream(digestOriginalStream, nbCopy);
    }


    private StorageLogbookParameters setLogbookStorageParameters(StorageLogbookParameters parameters, String offerId,
                                                                 ThreadResponseData res,
                                                                 String requester, int attempt, Status status) {
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        if (parameters == null) {
            parameters = getParameters(res != null ? res.getObjectGuid() : null, res != null ? res.getResponse() : null,
                    null, offerId, res != null ? res.getStatus() : status, requester, attempt);
        } else {
            updateStorageLogbookParameters(parameters, offerId,
                    res != null ? res.getStatus() : status, attempt);
        }
        return parameters;
    }

    private void logStorage(Integer tenant, StorageLogbookParameters parameters)
            throws IOException {
        storageLogService.append(tenant, parameters);
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

    /**
     * Storage logbook entry for ONE offer
     *
     * @param objectGuid      the object Guid
     * @param putObjectResult the response
     * @param messageDigest   the computed digest
     * @param offerId         the offerId
     * @param objectStored    the operation status
     * @return storage logbook parameters
     */
    private StorageLogbookParameters getParameters(String objectGuid, StoragePutResult putObjectResult,
                                                   Digest messageDigest, String offerId, Status objectStored, String requester, int attempt) {
        final String objectIdentifier = objectGuid != null ? objectGuid : "objectRequest NA";
        final String messageDig = messageDigest != null ? messageDigest.digestHex()
                : (putObjectResult != null ? putObjectResult.getDigestHashBase16() : "messageDigest NA");
        final String size = putObjectResult != null ? String.valueOf(putObjectResult.getObjectSize()) : "Size NA";
        boolean error = objectStored == Status.INTERNAL_SERVER_ERROR;
        final StorageLogbookOutcome outcome = error ? StorageLogbookOutcome.KO : StorageLogbookOutcome.OK;

        return getStorageLogbookParameters(objectIdentifier, null, messageDig, digestType.getName(), size,
                getAttemptLog(offerId, attempt, error), requester, null, null, outcome);
    }

    private String getAttemptLog(String offerId, int attempt, boolean error) {
        StringBuilder sb = new StringBuilder();
        sb.append(offerId).append(" attempt ").append(attempt).append(" : ").append(error ? "KO" : "OK");
        return sb.toString();
    }

    private void updateStorageLogbookParameters(StorageLogbookParameters parameters, String offerId, Status status,
                                                int attempt) {
        String offers = parameters.getMapParameters().get(StorageLogbookParameterName.agentIdentifiers);
        if (Status.INTERNAL_SERVER_ERROR.equals(status) || Status.CONFLICT.equals(status)) {
            parameters.getMapParameters().put(StorageLogbookParameterName.outcome, StorageLogbookOutcome.KO.name());
            offers += ", " + offerId + " attempt " + attempt + " : KO - " + status.name();
        } else {
            offers += ", " + offerId + " attempt " + attempt + " : OK";
            parameters.setStatus(StorageLogbookOutcome.OK);
        }
        parameters.getMapParameters().put(StorageLogbookParameterName.agentIdentifiers, offers);
    }

    private Driver retrieveDriverInternal(String offerId) throws StorageTechnicalException {
        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    private void checkStoreDataParams(ObjectDescription createObjectDescription, String strategyId, String dataId,
                                      DataCategory category) {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", dataId);
        ParametersChecker.checkParameter("Category is mandatory", category);
        ParametersChecker.checkParameter("Object additional information guid is mandatory", createObjectDescription);
        ParametersChecker.checkParameter("Container guid is mandatory",
                createObjectDescription.getWorkspaceContainerGUID());
        ParametersChecker.checkParameter("Object URI in workspaceis mandatory",
                createObjectDescription.getWorkspaceObjectURI());
    }

    private Map<String, Object> retrieveDataFromWorkspace(String containerGUID, String objectURI,
                                                          WorkspaceClient workspaceClient)
            throws StorageNotFoundException, StorageTechnicalException {
        Response response = null;
        try {
            response = workspaceClient.getObject(containerGUID, objectURI);
            Map<String, Object> result = new HashMap<>();
            String length = response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName());
            Object entity = response.getEntity();
            if (entity == null) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID));
                throw new StorageNotFoundException(
                        VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID));
            }
            try {
                ParametersChecker.checkParameter("Length is empty", length);
                Long.valueOf(length);
            } catch (IllegalArgumentException e) {
                // Default value (hack)
                LOGGER.warn("no Length returned", e);
                length = DEFAULT_SIZE_WHEN_UNKNOWN;
            }
            result.put(SIZE_KEY, length);
            result.put(STREAM_KEY, entity);
            result.put(RESPONSE_KEY, response);
            return result;
        } catch (final ContentAddressableStorageNotFoundException exc) {
            workspaceClient.consumeAnyEntityAndClose(response);
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, containerGUID), exc);
            throw new StorageNotFoundException(exc);
        } catch (final ContentAddressableStorageServerException exc) {
            workspaceClient.consumeAnyEntityAndClose(response);
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public JsonNode getContainerInformations(String strategyId) throws StorageException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
                throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            ArrayNode resultArray = JsonHandler.createArrayNode();
            for (OfferReference offerReference : offerReferences) {
                resultArray.add(getOfferInformation(offerReference, tenantId, hotStrategy.getCopy()));
            }
            return JsonHandler.createObjectNode().set("capacities", resultArray);
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private JsonNode getOfferInformation(OfferReference offerReference, Integer tenantId, int nbCopy)
            throws StorageException {
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        try (Connection connection = driver.connect(offer.getId())) {
            final ObjectNode ret = JsonHandler.createObjectNode();
            ret.put("offerId", offerReference.getId());
            ret.put("usableSpace", connection.getStorageCapacity(tenantId).getUsableSpace());
            ret.put("nbc", nbCopy);
            return ret;
        } catch (StorageDriverException | RuntimeException exc) {
            if (exc instanceof StorageDriverNotFoundException) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND, tenantId), exc);
                throw new StorageNotFoundException(exc);
            } else if (exc instanceof StorageDriverPreconditionFailedException) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_BAD_REQUEST), exc);
                throw new IllegalArgumentException(exc);
            }
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public InputStream getStorageContainer(String strategyId)
            throws StorageNotFoundException, StorageTechnicalException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
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

    private OfferReference chooseReferentOffer(HotStrategy hotStrategy) {
        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {
            List<OfferReference> offerReferences =
                    hotStrategy.getOffers().stream().filter(offer -> offer.isReferent()).collect(Collectors.toList());
            return Iterables.getOnlyElement(offerReferences);
        }
        throw new IllegalArgumentException("Exactly one offer should be declared as 'referent' in hot strategy");
    }

    private StorageLogbookParameters getStorageLogbookParameters(String objectIdentifier, GUID objectGroupIdentifier,
                                                                 String digest, String digestAlgorithm, String size, String agentIdentifiers, String agentIdentifierRequester,
                                                                 String outcomeDetailMessage, String objectIdentifierIncome, StorageLogbookOutcome outcome) {
        final Map<StorageLogbookParameterName, String> mandatoryParameters = new TreeMap<>();
        mandatoryParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.now().toString());
        mandatoryParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        mandatoryParameters.put(StorageLogbookParameterName.objectIdentifier,
                objectIdentifier != null ? objectIdentifier : "objId NA");
        mandatoryParameters.put(StorageLogbookParameterName.objectGroupIdentifier,
                objectGroupIdentifier != null ? objectGroupIdentifier.toString() : "objGId NA");
        mandatoryParameters.put(StorageLogbookParameterName.digest, digest);
        mandatoryParameters.put(StorageLogbookParameterName.digestAlgorithm, digestAlgorithm);
        mandatoryParameters.put(StorageLogbookParameterName.size, size);
        mandatoryParameters.put(StorageLogbookParameterName.eventType, "CREATE");
        mandatoryParameters.put(StorageLogbookParameterName.xRequestId,
                VitamThreadUtils.getVitamSession().getRequestId());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifiers, agentIdentifiers);
        mandatoryParameters.put(StorageLogbookParameterName.tenantId, ParameterHelper.getTenantParameter().toString());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifierRequester, agentIdentifierRequester);
        final StorageLogbookParameters parameters = new StorageLogbookParameters(mandatoryParameters);

        if (outcomeDetailMessage != null) {
            parameters.setOutcomDetailMessage(outcomeDetailMessage);
        }
        if (objectIdentifierIncome != null) {
            parameters.setObjectIdentifierIncome(objectIdentifierIncome);
        }
        return parameters;
    }

    @Override
    public JsonNode createContainer(String strategyId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteContainer(String strategyId) throws UnsupportedOperationException {
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
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
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
     * @param offerId
     * @param category   the object type to list
     * @param offset     offset of the excluded object
     * @param limit      the number of result wanted
     * @param order      order
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
     * @param strategyId
     * @param offerId
     * @param category
     * @param offset
     * @param limit
     * @param order
     * @return
     * @throws StorageException
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
        } catch (final StorageDriverException exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public Response getContainerByCategory(String strategyId, String objectId, DataCategory category)
            throws StorageException {

        return getContainerByCategoryResponse(strategyId, objectId, category, null);
    }

    @Override
    public Response getContainerByCategory(String strategyId, String objectId, DataCategory category, String offerId)
            throws StorageException {

        return getContainerByCategoryResponse(strategyId, objectId, category, offerId);
    }

    /**
     * getContainerByCategoryResponse.
     *
     * @param strategyId
     * @param objectId
     * @param category
     * @return
     * @throws StorageException
     */
    private Response getContainerByCategoryResponse(String strategyId, String objectId, DataCategory category,
                                                    String offerId)
            throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", objectId);

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy == null) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        // TODO: check this on starting application
        isStrategyValid(hotStrategy);

        List<StorageOffer> storageOffers = new ArrayList<>();
        if (StringUtils.isBlank(offerId)) {
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }

            storageOffers.addAll(offerReferences.stream()
                    .map(StorageDistributionImpl::apply)
                    .collect(Collectors.toList()));
        } else {
            // get the storage offer from the given identifier
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
            storageOffers = Arrays.asList(offer);
        }

        final StorageGetResult result =
                getGetObjectResult(tenantId, objectId, category, storageOffers);
        return result.getObject();
    }

    /**
     * apply.
     *
     * @param offerReference
     * @return
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
     * getGetObjectResult.
     *
     * @param tenantId
     * @param objectId
     * @param type
     * @param storageOffers
     * @return
     * @throws StorageException
     */
    private StorageGetResult getGetObjectResult(Integer tenantId, String objectId, DataCategory type,
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
                LOGGER.warn("Error with the storage: object not found. Take next offer in strategy (by priority)", exc);
                offerOkNoBinary = true;
            } catch (final StorageDriverException exc) {
                LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
            }
        }
        if (offerOkNoBinary) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId));
            throw new StorageNotFoundException(
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId));
        } else {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
        }
    }

    @Override
    public JsonNode getContainerInformations(String strategyId, DataCategory type, String objectId,
                                             List<String> offerIds)
            throws StorageException {

        ObjectNode offerIdToMetadata = JsonHandler.createObjectNode();

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", objectId);

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }

            for (final String offerId : offerIds) {
                final Driver driver = retrieveDriverInternal(offerId);
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
                try (Connection connection = driver.connect(offer.getId())) {

                    final StorageObjectRequest request = new StorageObjectRequest(tenantId,
                            type.getFolder(), objectId);

                    StorageMetadatasResult metaData = connection.getMetadatas(request);
                    offerIdToMetadata.set(offerId, JsonHandler.toJsonNode(metaData));

                } catch (StorageDriverException exc) {
                    LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
                } catch (InvalidParseOperationException e) {
                    LOGGER.warn(e);
                }
            }
            return offerIdToMetadata;
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    /**
     * Verify if object exists
     *
     * @param strategyId id of the strategy
     * @param objectId   id of the object
     * @param category
     * @param offerIds   list id of offers  @return
     * @throws StorageException
     */
    @Override
    public boolean checkObjectExisting(String strategyId, String objectId, DataCategory category, List<String> offerIds)
            throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", objectId);
        ParametersChecker.checkParameter("Offer ids is mandatory", offerIds);

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            List<String> offerReferencesIds = offerReferences.stream().map(e -> e.getId()).collect(Collectors.toList());
            if (!offerReferencesIds.containsAll(offerIds)) {
                return false;
            }
            for (final String offerId : offerIds) {
                final Driver driver = retrieveDriverInternal(offerId);
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerId);
                try (Connection connection = driver.connect(offer.getId())) {
                    final StorageCheckRequest request =
                            new StorageCheckRequest(tenantId, category.getFolder(),
                                    objectId, null, null);
                    if (!connection.objectExistsInOffer(request)) {
                        return false;
                    }
                } catch (final fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException exc) {
                    LOGGER.warn("Error with the storage: object not found. Take next offer in strategy (by priority)",
                            exc);
                    return false;
                } catch (final StorageDriverException exc) {
                    LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
                    return false;
                }
            }
            return true;
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
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
    public void deleteObject(String strategyId, String objectId, String digest, DigestType digestAlgorithm)
            throws StorageException {

        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", objectId);
        ParametersChecker.checkParameter("Digest is mandatory", digest);
        ParametersChecker.checkParameter("Digest Algorithm is mandatory", digestAlgorithm);

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            // TODO: check this on starting application
            isStrategyValid(hotStrategy);
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            // TODO : Improve this code, use same thread system as used for the
            // storeData method see @TrasferThread
            for (final OfferReference offerReference : offerReferences) {
                final Driver driver = retrieveDriverInternal(offerReference.getId());
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
                final Properties parameters = new Properties();
                parameters.putAll(offer.getParameters());
                try (Connection connection = driver.connect(offer.getId())) {
                    StorageRemoveRequest request =
                            new StorageRemoveRequest(tenantId, DataCategory.OBJECT.getFolder(), objectId,
                                    digestType, digest);
                    StorageRemoveResult result = connection.removeObject(request);
                    if (!result.isObjectDeleted()) {
                        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND));
                        throw new StorageTechnicalException("Object not deleted: " + objectId);
                    }
                } catch (StorageDriverException | RuntimeException exc) {
                    if (exc instanceof StorageDriverNotFoundException) {
                        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId), exc);
                        throw new StorageNotFoundException(exc);
                    } else if (exc instanceof StorageDriverPreconditionFailedException) {
                        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_BAD_REQUEST), exc);
                        throw new IllegalArgumentException(exc);
                    }
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
                    throw new StorageTechnicalException(exc);
                }

            }
        }
    }

    @Override
    public JsonNode getContainerLogbooks(String strategyId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerLogbook(String strategyId, String logbookId)
            throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteLogbook(String strategyId, String logbookId)
            throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnits(String strategyId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnit(String strategyId, String unitId)
            throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteUnit(String strategyId, String unitId)
            throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroups(String strategyId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroup(String strategyId, String objectGroupId)
            throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteObjectGroup(String strategyId, String objectGroupId) throws UnsupportedOperationException {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode status() {
        LOGGER.error(NOT_IMPLEMENTED_MSG);
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
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
