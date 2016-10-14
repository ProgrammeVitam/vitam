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
import java.sql.Driver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.logbook.StorageLogbook;
import fr.gouv.vitam.storage.engine.server.logbook.StorageLogbookFactory;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameters;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;

/**
 * StorageDistribution service Implementation TODO: see what to do with RuntimeException (catch it and log it to let the
 * process continue if needed)
 */
public class StorageDistributionImpl implements StorageDistribution {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageDistributionImpl.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER = StorageStrategyProviderFactory
        .getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String NOT_IMPLEMENTED_MSG = "Not yet implemented";
    private static final int NB_RETRY = 3;
    private final WorkspaceClient workspaceClient;
    // FIXME see API
    // TODO : later, the digest type may be retrieve via REST parameters. Fot the moment (as of US 72 dev) there is no
    // specification about that
    private final DigestType digestType;

    /**
     * Constructs the service with a given configuration
     *
     * @param configuration the configuration of the storage
     */
    public StorageDistributionImpl(StorageConfiguration configuration) {
        ParametersChecker.checkParameter("Storage service configuration is mandatory", configuration);
        workspaceClient = WorkspaceClientFactory.create(configuration.getUrlWorkspace());
        // TODO : a real design discussion is needed : should we force it ? Should we negociate it with the offer ?
        // FIXME Might be negotiated but limited to available digestType from Vitam (MD5, SHA-1, SHA-256, SHA-512, ...)
        // Just to note, I prefer SHA-512 (more CPU but more accurate and already the default for Vitam, notably to
        // allow check of duplicated files)
        digestType = DigestType.SHA256;
    }

    /**
     * For JUnit ONLY
     *
     * @param wkClient a custom instance of workspace client
     * @param digest a custom digest
     */
    StorageDistributionImpl(WorkspaceClient wkClient, DigestType digest) {
        workspaceClient = wkClient;
        digestType = digest;
    }

    // TODO : review design : for the moment we handle createObjectDescription AND jsonData in the same params but
    // they should not be both resent at the same time. Maybe encapsulate or create 2 methods
    @Override
    public StoredInfoResult storeData(String tenantId, String strategyId, String objectId,
        CreateObjectDescription createObjectDescription, DataCategory category)
        throws StorageTechnicalException, StorageNotFoundException, StorageObjectAlreadyExistsException {
        // Check input params
        checkStoreDataParams(createObjectDescription, tenantId, strategyId, objectId, category);
        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            final Map<String, Status> offerResults = new HashMap<>();

            // For each offer, store object on it
            for (final OfferReference offerReference : offerReferences) {
                // TODO: sequential process for now (we have only 1 offer anyway) but storing object should be
                // processed in parallel for each driver, in order to not be blocked on 1 driver storage process
                // TODO special notice: when parallel, try to get only once the inputstream and then multiplexing it to
                // multiple intputstreams as needed
                // 1 IS => 3 IS (if 3 offers) where this special class handles one IS as input to 3 IS as output
                final Status success =
                    tryAndRetryStoreObjectInOffer(createObjectDescription, tenantId, objectId, category,
                        offerReference);
                offerResults.put(offerReference.getId(), success);
            }
            // TODO Handle Status result if different for offers
            return buildStoreDataResponse(objectId, category, offerResults);
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
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

        // TODO Witch status code return if an offer is updated (Status.OK) and another is created (Status.CREATED) ?
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

    // TODO : globalize try and retry mechanism to avoid implementing it manually on all methods (C++ would have been
    // great here) by creating an interface of Retryable actions and different implementations for each retryable action
    private Status tryAndRetryStoreObjectInOffer(CreateObjectDescription createObjectDescription, String tenantId,
        String objectId, DataCategory category, OfferReference offerReference)
        throws StorageTechnicalException, StorageObjectAlreadyExistsException {
        // TODO: optimize workspace InputStream to not request workspace for each offer but only once.
        final Driver driver = retrieveDriverInternal(offerReference.getId());
        // Retrieve storage offer description and parameters
        final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        final Properties parameters = new Properties();
        parameters.putAll(offer.getParameters());
        PutObjectRequest putObjectRequest = null;
        PutObjectResult putObjectResult;
        Status objectStored = Status.INTERNAL_SERVER_ERROR;
        boolean existInOffer = false;
        Digest messageDigest = null;
        int i = 0;
        while (i < NB_RETRY && objectStored == Status.INTERNAL_SERVER_ERROR) {
            i++;
            LOGGER.info("[Attempt " + i + "] Trying to store object '" + objectId + "' in offer " + offer.getId());
            try {
                messageDigest = new Digest(digestType);
            } catch (final IllegalArgumentException exc) {
                throw new StorageTechnicalException(exc);
            }
            try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
                final GetObjectRequest request = new GetObjectRequest(tenantId, objectId, category.getFolder());
                if (connection.objectExistsInOffer(request)) {
                    switch (category) {
                        case LOGBOOK:
                        case OBJECT:
                            throw new StorageObjectAlreadyExistsException(VitamCodeHelper
                                .getLogMessage(VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS, objectId));
                        case UNIT:
                        case OBJECT_GROUP:
                            existInOffer = true;
                            break;
                        default:
                            throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
                    }
                }

                putObjectRequest =
                    buildPutObjectRequest(createObjectDescription, tenantId, objectId, category, messageDigest);
                // Perform actual object upload
                putObjectResult = connection.putObject(putObjectRequest);

                // Check digest
                if (BaseXx.getBase16(messageDigest.digest()).equals(putObjectResult.getDigestHashBase16())) {
                    if (existInOffer) {
                        objectStored = Status.OK;
                    } else {
                        objectStored = Status.CREATED;
                    }
                } else {
                    throw new StorageTechnicalException("[Driver:" + driver.getName() + "] Content digest invalid in " +
                        "offer id : '" + offer.getId() + "' for object " + objectId);
                }
            } catch (StorageDriverException | StorageNotFoundException | StorageTechnicalException exc) {
                LOGGER.error(exc);
                if (i >= NB_RETRY) {
                    objectStored = Status.INTERNAL_SERVER_ERROR;
                    break;
                }
            } finally {
                if (putObjectRequest != null && putObjectRequest.getDataStream() != null) {
                    IOUtils.closeQuietly(putObjectRequest.getDataStream());
                    LOGGER.debug("Manually closing the data stream for object id '" + objectId + "'");
                }
            }
        }

        try {
            final StorageLogbook storageLogbook = StorageLogbookFactory.getInstance().getStorageLogbook();
            // TODO : replace fakeSize by the real size of the file
            storageLogbook.add(getStorageLogbookParameters(
                putObjectRequest != null ? putObjectRequest.getGuid() : "objectReques NA", null,
                messageDigest != null ? messageDigest.digest().toString() : "messageDigest NA", digestType.getName(),
                "fakeSize", offer.getId(), "X-Application-Id",
                null, null,
                objectStored == Status.INTERNAL_SERVER_ERROR ? StorageStatusCode.KO : StorageStatusCode.OK));
        } catch (final StorageException exc) {
            throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_LOGBOOK_CANNOT_LOG),
                exc);
        }

        return objectStored;
    }

    private Driver retrieveDriverInternal(String offerId) throws StorageTechnicalException {
        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    private void checkStoreDataParams(CreateObjectDescription createObjectDescription, String tenantId,
        String strategyId, String dataId, DataCategory category) {
        ParametersChecker.checkParameter("Tenant id is mandatory", tenantId);
        ParametersChecker.checkParameter("Strategy id is mandatory", strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", dataId);
        ParametersChecker.checkParameter("Category is mandatory", category);
        ParametersChecker.checkParameter("Object additional information guid is mandatory",
            createObjectDescription);
        ParametersChecker.checkParameter("Container guid is mandatory", createObjectDescription
            .getWorkspaceContainerGUID());
        ParametersChecker.checkParameter("Object URI in workspaceis mandatory", createObjectDescription
            .getWorkspaceObjectURI());
    }

    private PutObjectRequest buildPutObjectRequest(CreateObjectDescription createObjectDescription, String tenantId,
        String objectId, DataCategory category, Digest messageDigest)
        throws StorageTechnicalException, StorageNotFoundException {
        final InputStream dataStream = retrieveDataFromWorkspace(createObjectDescription.getWorkspaceContainerGUID(),
            createObjectDescription.getWorkspaceObjectURI());
        return new PutObjectRequest(tenantId, digestType.getName(), objectId,
            messageDigest.getDigestInputStream(dataStream), category.name());
    }

    private InputStream retrieveDataFromWorkspace(String containerGUID, String objectURI)
        throws StorageNotFoundException, StorageTechnicalException {
        try {
            return workspaceClient.getObject(containerGUID, objectURI);
        } catch (final ContentAddressableStorageNotFoundException exc) {
            throw new StorageNotFoundException(exc);
        } catch (final ContentAddressableStorageServerException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    @Override
    public JsonNode getContainerInformation(String tenantId, String strategyId)
        throws StorageNotFoundException, StorageTechnicalException {
        ParametersChecker.checkParameter("Tenant id is mandatory", tenantId);
        ParametersChecker.checkParameter("Strategy id is mandatory", strategyId);
        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }

            // HACK: HARDCODE ! actually we only have one offer
            // TODO: review algo
            final OfferReference offerReference = offerReferences.get(0);
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            final Properties parameters = new Properties();
            parameters.putAll(offer.getParameters());
            try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
                final ObjectNode ret = JsonHandler.createObjectNode();
                ret.put("usableSpace", connection.getStorageCapacity(tenantId).getUsableSpace());
                return ret;
            } catch (StorageDriverException | RuntimeException exc) {
                throw new StorageTechnicalException(exc);
            }
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }



    @Override
    public InputStream getStorageContainer(String tenantId, String strategyId)
        throws StorageNotFoundException, StorageTechnicalException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    private List<OfferReference> choosePriorityOffers(HotStrategy hotStrategy) {
        final List<OfferReference> offerReferences = new ArrayList<>();
        if (hotStrategy != null && !hotStrategy.getOffers().isEmpty()) {
            // TODO : this code will be changed in the future to handle priority (not in current US scope) and copy
            offerReferences.add(hotStrategy.getOffers().get(0));
        }
        return offerReferences;
    }

    // TODO : refactor this method, too many arguments
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
    public JsonNode createContainer(String tenantId, String strategyId) throws StorageException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteContainer(String tenantId, String strategyId) throws StorageTechnicalException,
        StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjects(String tenantId, String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public InputStream getContainerByCategory(String tenantId, String strategyId, String objectId,
        DataCategory category)
        throws StorageNotFoundException, StorageTechnicalException {
        // Check input params
        ParametersChecker.checkParameter("Tenant id is mandatory", tenantId);
        ParametersChecker.checkParameter("Strategy id is mandatory", strategyId);
        ParametersChecker.checkParameter("Object id is mandatory", objectId);

        // Retrieve strategy data
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        final HotStrategy hotStrategy = storageStrategy.getHotStrategy();
        if (hotStrategy != null) {
            final List<OfferReference> offerReferences = choosePriorityOffers(hotStrategy);
            if (offerReferences.isEmpty()) {
                throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OFFER_NOT_FOUND));
            }
            final GetObjectResult result = getGetObjectResult(tenantId, objectId, category, offerReferences);
            return result.getObject();
        }
        throw new StorageTechnicalException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
    }

    private GetObjectResult getGetObjectResult(String tenantId, String objectId, DataCategory type,
        List<OfferReference> offerReferences)
        throws StorageTechnicalException, StorageNotFoundException {
        GetObjectResult result;
        for (final OfferReference offerReference : offerReferences) {
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            final Properties parameters = new Properties();
            parameters.putAll(offer.getParameters());
            try (Connection connection = driver.connect(offer.getBaseUrl(), parameters)) {
                final GetObjectRequest request = new GetObjectRequest(tenantId, objectId, type.getFolder());
                result = connection.getObject(request);
                if (result.getObject() != null) {
                    return result;
                }
            } catch (final StorageDriverException exc) {
                LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
            }
        }
        throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, objectId));
    }

    @Override
    public JsonNode getContainerObjectInformations(String tenantId, String strategyId, String objectId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteObject(String tenantId, String strategyId, String objectId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerLogbooks(String tenantId, String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerLogbook(String tenantId, String strategyId, String logbookId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }


    @Override
    public void deleteLogbook(String tenantId, String strategyId, String logbookId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnits(String tenantId, String strategyId) throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerUnit(String tenantId, String strategyId, String unitId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteUnit(String tenantId, String strategyId, String unitId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroups(String tenantId, String strategyId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode getContainerObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public void deleteObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public JsonNode status() throws StorageException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }
}
