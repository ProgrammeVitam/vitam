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

import java.io.InputStream;
import java.security.DigestInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultipleInputStreamHandler;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageObjectAlreadyExistsException;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
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
import fr.gouv.vitam.storage.engine.server.logbook.StorageLogbook;
import fr.gouv.vitam.storage.engine.server.logbook.StorageLogbookFactory;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameters;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * StorageDistribution service Implementation process continue if needed)
 */
// TODO P1: see what to do with RuntimeException (catch it and log it to let the
public class StorageDistributionImpl implements StorageDistribution {

    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final String TENANT_ID_IS_MANDATORY = "Tenant id is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageDistributionImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER = StorageStrategyProviderFactory
        .getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    private static final int NB_RETRY = 3;
    private static final String SIZE_KEY = "size";
    private static final String STREAM_KEY = "stream";

    /**
     * Used to wait for all task submission (executorService)
     */
    private static final long threadSleep = 10;
    private final String urlWorkspace;
    private final Integer millisecondsPerKB;

    // TODO P2 see API
    // TODO P2 : later, the digest type may be retrieve via REST parameters. Fot the moment (as of US 72 dev) there is
    // no
    // specification about that
    private final DigestType digestType;
    private Digest digest;
    // FOR JUNIT TEST ONLY (TODO P1: review WorkspaceClientFactory to offer a mocked WorkspaceClient)
    private final WorkspaceClient mockedWorkspaceClient;

    /**
     * Constructs the service with a given configuration
     *
     * @param configuration the configuration of the storage
     */
    public StorageDistributionImpl(StorageConfiguration configuration) {
        ParametersChecker.checkParameter("Storage service configuration is mandatory", configuration);
        urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
        millisecondsPerKB = configuration.getTimeoutMsPerKB();
        mockedWorkspaceClient = null;
        // TODO P2 : a real design discussion is needed : should we force it ? Should we negociate it with the offer ?
        // TODO P2 Might be negotiated but limited to available digestType from Vitam (MD5, SHA-1, SHA-256, SHA-512,
        // ...)
        // Just to note, I prefer SHA-512 (more CPU but more accurate and already the default for Vitam, notably to
        // allow check of duplicated files)
        digestType = VitamConfiguration.getDefaultDigestType();
        digest = new Digest(digestType);
    }

    /**
     * For JUnit ONLY
     *
     * @param wkClient a custom instance of workspace client
     * @param digestType a custom digest
     */
    StorageDistributionImpl(WorkspaceClient wkClient, DigestType digestType) {
        urlWorkspace = null;
        millisecondsPerKB = 100;
        mockedWorkspaceClient = wkClient;
        this.digestType = digestType;
        digest = new Digest(digestType);
    }

    // TODO P1 : review design : for the moment we handle createObjectDescription AND jsonData in the same params but
    // they should not be both resent at the same time. Maybe encapsulate or create 2 methods
    // TODO P1 : refactor me !
    @Override
    public StoredInfoResult storeData(String strategyId, String objectId,
        CreateObjectDescription createObjectDescription, DataCategory category, String requester)
        throws StorageException, StorageObjectAlreadyExistsException {
        // Check input params
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        checkStoreDataParams(createObjectDescription, tenantId, strategyId, objectId, category);
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
            datas.populateFromOfferReferences(offerReferences);

            StorageLogbookParameters parameters =
                tryAndRetry(objectId, createObjectDescription, category, requester, tenantId, datas, 1);

            logStorage(parameters);
            // TODO P1 Handle Status result if different for offers
            return buildStoreDataResponse(objectId, category, datas.getGlobalOfferResult());
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private StorageLogbookParameters tryAndRetry(String objectId, CreateObjectDescription createObjectDescription,
        DataCategory category, String requester, Integer tenantId, TryAndRetryData datas, int attempt)
        throws StorageTechnicalException, StorageNotFoundException, StorageObjectAlreadyExistsException {
        StorageLogbookParameters parameters = null;

        Map<String, Object> streamAndInfos = getInputStreamFromWorkspace(createObjectDescription);

        MultipleInputStreamHandler streams = getMultipleInputStreamFromWorkspace((InputStream) streamAndInfos.get
                (STREAM_KEY), datas.getKoList().size());

        VitamThreadPoolExecutor executor = new VitamThreadPoolExecutor();

        // init thread and make future map
        // Map here to keep offerId linked to Future
        Map<String, Future<ThreadResponseData>> futureMap = new HashMap<>();
        int rank = 0;
        for (final String offerId : datas.getKoList()) {
            OfferReference offerReference = new OfferReference();
            offerReference.setId(offerId);
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            StoragePutRequest request = new StoragePutRequest(tenantId, category.getFolder(), objectId,
                digestType.name(), streams.getInputStream(rank));
            futureMap.put(offerReference.getId(), executor.submit(new TransferThread(driver, offerReference,
                request)));
            rank++;
        }

        // wait all tasks submission
        try {
            Thread.sleep(threadSleep, TimeUnit.MILLISECONDS.ordinal());
        } catch (InterruptedException exc) {
            LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
        }

        executor.shutdown();

        // wait for all threads execution
        // TODO: manage interruption and error execution (US #2008 && 2009)
        try {
            executor.awaitTermination(getTransferTimeout(Long.valueOf((String) streamAndInfos.get(SIZE_KEY))),
                TimeUnit.MILLISECONDS);
        } catch (InterruptedException exc) {
            LOGGER.warn("ExecutorService interrupted !", exc);
        }

        executor.shutdownNow();

        for (String offerId : futureMap.keySet()) {
            try {
                ThreadResponseData res = futureMap.get(offerId).get();
                parameters = setLogbookStorageParameters(parameters, offerId, res, requester, attempt);
                datas.koListToOkList(offerId);
            } catch (InterruptedException exc) {
                // Interrupted = shutdownNow
                LOGGER.error("Interrupted on offer ID " + offerId, exc);
                parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt);
            } catch (ExecutionException exc) {
                // Execution = thread error (exception)
                LOGGER.error("Error on offer ID " + offerId, exc);
                parameters = setLogbookStorageParameters(parameters, offerId, null, requester, attempt);
                logStorage(parameters);
                // TODO: review this exception to manage errors correctly
                // Take into account Exception class
                // For example, for particular exception do not retry (because it's useless)
                // US : #2009
            }
        }
        if (attempt < NB_RETRY && !datas.getKoList().isEmpty()) {
            attempt++;
            tryAndRetry(objectId, createObjectDescription, category, requester, tenantId, datas, attempt);
        }

        // TODO : error management (US #2008)
        if (!datas.getKoList().isEmpty()) {
            deleteObjects(datas.getOkList(), tenantId, category, objectId);
        }

        return parameters;
    }

    private long getTransferTimeout(long sizeToTransfer) {
        return (sizeToTransfer / 1024) * millisecondsPerKB;
    }

    private void isStrategyValid(HotStrategy hotStrategy) throws StorageTechnicalException {
        if (!hotStrategy.isCopyValid()) {
            throw new StorageTechnicalException("Invalid number of copy");
        }
    }

    private Map<String, Object> getInputStreamFromWorkspace(CreateObjectDescription createObjectDescription)
        throws StorageTechnicalException, StorageNotFoundException {
        try (WorkspaceClient workspaceClient =
            mockedWorkspaceClient == null ? WorkspaceClientFactory.getInstance().getClient() : mockedWorkspaceClient) {
            return retrieveDataFromWorkspace(createObjectDescription
                .getWorkspaceContainerGUID(),
                createObjectDescription.getWorkspaceObjectURI(), workspaceClient);
        }

    }

    private MultipleInputStreamHandler getMultipleInputStreamFromWorkspace(InputStream stream, int nbCopy)
        throws StorageTechnicalException, StorageNotFoundException {
        DigestInputStream digestOriginalStream = (DigestInputStream) digest.getDigestInputStream(stream);
        return new MultipleInputStreamHandler(digestOriginalStream, nbCopy);
    }


    private StorageLogbookParameters setLogbookStorageParameters(StorageLogbookParameters parameters, String offerId, ThreadResponseData res,
        String requester, int attempt) {
        if (parameters == null) {
            parameters = getParameters(res != null ? res.getObjectGuid() : null, res != null ? res.getResponse() : null,
                null, offerId, res != null ? res.getStatus() : Status.INTERNAL_SERVER_ERROR, requester, attempt);
        } else {
            updateStorageLogbookParameters(parameters, offerId, res != null ? res.getStatus() : Status
                .INTERNAL_SERVER_ERROR, attempt);
        }
        return parameters;
    }

    private void logStorage(StorageLogbookParameters parameters) throws StorageTechnicalException {
        try {
            final StorageLogbook storageLogbook = StorageLogbookFactory.getInstance().getStorageLogbook();
            storageLogbook.add(parameters);
        } catch (final StorageException exc) {
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_LOGBOOK_CANNOT_LOG),
                exc);
        }
    }

    private StoredInfoResult buildStoreDataResponse(String objectId, DataCategory category,
        Map<String, Status> offerResults) throws StorageTechnicalException {

        final String offerIds = String.join(", ", offerResults.keySet());
        // Aggregate result of all store actions. If all went well, allSuccess is true, false if one action failed
        final boolean allSuccess = offerResults.entrySet().stream()
            .map(Map.Entry::getValue)
            .noneMatch(Status.INTERNAL_SERVER_ERROR::equals);

        if (!allSuccess) {
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CANT_STORE_OBJECT,
                objectId, offerIds));
        }

        // TODO P1 Witch status code return if an offer is updated (Status.OK) and another is created (Status.CREATED) ?
        final StoredInfoResult result = new StoredInfoResult();
        final LocalDateTime now = LocalDateTime.now();
        final StringBuilder description = new StringBuilder();
        switch (category) {
            case UNIT:
                description.append("Unit ");
                break;
            case OBJECT_GROUP:
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
        return result;
    }

    /**
     * Storage logbook entry for ONE offer
     *
     * @param objectGuid the object Guid
     * @param putObjectResult the response
     * @param messageDigest the computed digest
     * @param offerId the offerId
     * @param objectStored the operation status
     * @return storage logbook parameters
     */
    private StorageLogbookParameters getParameters(String objectGuid, StoragePutResult putObjectResult,
        Digest messageDigest, String offerId, Status objectStored, String requester, int attempt) {
        final String objectIdentifier = objectGuid != null ? objectGuid : "objectRequest NA";
        final String messageDig = messageDigest != null ? messageDigest.digestHex() : "messageDigest NA";
        final String size = putObjectResult != null ? String.valueOf(putObjectResult.getObjectSize()) : "Size NA";
        boolean error = objectStored == Status.INTERNAL_SERVER_ERROR;
        final StorageLogbookOutcome outcome = error ? StorageLogbookOutcome.KO : StorageLogbookOutcome.OK;

        return getStorageLogbookParameters(
            objectIdentifier, null, messageDig, digestType.getName(), size, getAttemptLog(offerId, attempt, error),
            requester, null, null, outcome);
    }

    private String getAttemptLog(String offerId, int attempt, boolean error) {
        StringBuilder sb = new StringBuilder();
        sb.append(offerId).append(" attempt ").append(attempt).append(" : ").append(error ? "KO" : "OK");
        return sb.toString();
    }

    private void updateStorageLogbookParameters(StorageLogbookParameters parameters, String offerId,
        Status status, int attempt) {
        String offers = parameters.getMapParameters().get(StorageLogbookParameterName.agentIdentifiers);
        if (Status.INTERNAL_SERVER_ERROR.equals(status)) {
            parameters.getMapParameters().put(StorageLogbookParameterName.outcome, StorageLogbookOutcome.KO.name());
            offers += ", " + offerId + " attempt " + attempt + " : KO";
        } else {
            offers += ", " + offerId + " attempt " + attempt +  " : OK";
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

    private void checkStoreDataParams(CreateObjectDescription createObjectDescription, Integer tenantId,
        String strategyId, String dataId, DataCategory category) {
        ParametersChecker.checkParameter(TENANT_ID_IS_MANDATORY, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", dataId);
        ParametersChecker.checkParameter("Category is mandatory", category);
        ParametersChecker.checkParameter("Object additional information guid is mandatory",
            createObjectDescription);
        ParametersChecker.checkParameter("Container guid is mandatory", createObjectDescription
            .getWorkspaceContainerGUID());
        ParametersChecker.checkParameter("Object URI in workspaceis mandatory", createObjectDescription
            .getWorkspaceObjectURI());
    }

    private Map<String, Object> retrieveDataFromWorkspace(String containerGUID, String objectURI,
        WorkspaceClient workspaceClient)
        throws StorageNotFoundException, StorageTechnicalException {
        try {
            Response response = workspaceClient.getObject(containerGUID, objectURI);
            Map<String, Object> result = new HashMap<>();
            result.put(SIZE_KEY, response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
            result.put(STREAM_KEY, response.getEntity());
            return result;
        } catch (final ContentAddressableStorageNotFoundException exc) {
            throw new StorageNotFoundException(exc);
        } catch (final ContentAddressableStorageServerException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public JsonNode getContainerInformation(String strategyId)
        throws StorageException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        ParametersChecker.checkParameter(TENANT_ID_IS_MANDATORY, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
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
            ArrayNode resultArray = JsonHandler.createArrayNode();
            for (OfferReference offerReference : offerReferences) {
                resultArray.add(getOfferInformation(offerReference, tenantId));
            }
            JsonNode result = JsonHandler.createObjectNode().set("capacities", resultArray);
            return result;
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private JsonNode getOfferInformation(OfferReference offerReference, Integer tenantId) throws StorageException {
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        final Properties parameters = new Properties();
        parameters.putAll(offer.getParameters());
        try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
            final ObjectNode ret = JsonHandler.createObjectNode();
            ret.put("offerId", offer.getId());
            ret.put("usableSpace", connection.getStorageCapacity(tenantId).getUsableSpace());
            return ret;
        } catch (StorageDriverException | RuntimeException exc) {
            // TODO IT_13 (celeg): response error ? (like offerId + usableSpace to 0 ?)
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public InputStream getStorageContainer(String strategyId)
        throws StorageNotFoundException, StorageTechnicalException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    private List<OfferReference> choosePriorityOffers(HotStrategy hotStrategy) {
        final List<OfferReference> offerReferences = new ArrayList<>();
        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {
            // TODO P1 : this code will be changed in the future to handle priority (not in current US scope) and copy
            offerReferences.addAll(hotStrategy.getOffers());
        }
        return offerReferences;
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
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifiers, agentIdentifiers);
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
    public JsonNode createContainer(String strategyId) throws StorageException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteContainer(String strategyId) throws StorageTechnicalException,
        StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjects(String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public Response getContainerByCategory(String strategyId, String objectId,
        DataCategory category, AsyncResponse asyncResponse) throws StorageException {
        // Check input params
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        ParametersChecker.checkParameter(TENANT_ID_IS_MANDATORY, tenantId);
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
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            final StorageGetResult result =
                getGetObjectResult(tenantId, objectId, category, offerReferences, asyncResponse);
            return result.getObject();
        }
        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private StorageGetResult getGetObjectResult(Integer tenantId, String objectId, DataCategory type,
        List<OfferReference> offerReferences, AsyncResponse asyncResponse) throws StorageException {
        StorageGetResult result;
        for (final OfferReference offerReference : offerReferences) {
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            final Properties parameters = new Properties();
            parameters.putAll(offer.getParameters());
            try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
                final StorageObjectRequest request = new StorageObjectRequest(tenantId, type.getFolder(), objectId);
                result = connection.getObject(request);
                if (result.getObject() != null) {
                    final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, result.getObject());
                    final ResponseBuilder responseBuilder =
                        Response.status(Status.OK).type(MediaType.APPLICATION_OCTET_STREAM);
                    helper.writeResponse(responseBuilder);
                    return result;
                }
            } catch (final StorageDriverException exc) {
                LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
            }
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId));
    }

    @Override
    public JsonNode getContainerObjectInformations(String strategyId, String objectId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    private void deleteObjects(List<String> offerIdList, Integer tenantId, DataCategory category, String objectId)
        throws StorageTechnicalException {
        VitamThreadPoolExecutor executor = new VitamThreadPoolExecutor();
        for (String offerId : offerIdList) {
            final Driver driver = retrieveDriverInternal(offerId);
            // TODO: review if digest value is really good ?
            StorageRemoveRequest request = new StorageRemoveRequest(tenantId, category.getFolder(), objectId,
            digestType, digest.digestHex());
            executor.submit(new DeleteThread(driver, request, offerId));
        }

        // wait all tasks submission
        try {
            Thread.sleep(threadSleep, TimeUnit.MILLISECONDS.ordinal());
        } catch (InterruptedException exc) {
            LOGGER.warn("Thread sleep to wait all task submission interrupted !", exc);
        }

        executor.shutdown();

        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException exc) {
            LOGGER.warn("ExecutorService interrupted !", exc);
        }

        executor.shutdownNow();
    }

    @Override
    public void deleteObject(String strategyId, String objectId, String digest, DigestType digestAlgorithm)
        throws StorageException {

        // Check input params
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        ParametersChecker.checkParameter(TENANT_ID_IS_MANDATORY, tenantId);
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
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            // TODO : Improve this code, use same thread system as used for the storeData method see @TrasferThread
            for (final OfferReference offerReference : offerReferences) {
                final Driver driver = retrieveDriverInternal(offerReference.getId());
                final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
                final Properties parameters = new Properties();
                parameters.putAll(offer.getParameters());
                try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
                    StorageRemoveRequest request = new StorageRemoveRequest(tenantId, DataCategory.OBJECT.getFolder(),
                        objectId, digestType, digest);
                    StorageRemoveResult result = connection.removeObject(request);
                    if (!result.isObjectDeleted()) {
                        throw new StorageNotFoundException(
                            VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND));
                    }
                } catch (StorageDriverException | RuntimeException exc) {
                    throw new StorageTechnicalException(exc);
                }

            }
        }
    }

    @Override
    public JsonNode getContainerLogbooks(String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerLogbook(String strategyId, String logbookId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }


    @Override
    public void deleteLogbook(String strategyId, String logbookId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnits(String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnit(String strategyId, String unitId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteUnit(String strategyId, String unitId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroups(String strategyId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroup(String strategyId, String objectGroupId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteObjectGroup(String strategyId, String objectGroupId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode status() throws StorageException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }
}
