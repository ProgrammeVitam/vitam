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
package fr.gouv.vitam.storage.engine.server.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
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
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StreamAndInfo;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface Storage Distribution for Storage Operations
 */
public interface StorageDistribution extends VitamAutoCloseable {
    /**
     * copy object from on offer to an another
     *
     * @param context the context
     * @param destinationOffer destination Offer
     * @param sourceOffer source offer
     * @return StoredInfoResult Object
     * @throws StorageException StorageException
     */
    StoredInfoResult copyObjectFromOfferToOffer(DataContext context, String sourceOffer, String destinationOffer)
        throws StorageException;

    /**
     * Store data of any type for given tenant on storage offers associated to
     * given strategy
     *
     * @param strategyId id of the strategy
     * @param objectId the workspace URI of the data to be retrieve (and stored in
     * offer)
     * @param createObjectDescription object additional informations
     * @param category the category of the data to store (unit, object...)
     * @param requester the requester information
     * @return a StoredInfoResult containing informations about the created Data
     * @throws StorageException StorageException
     */
    StoredInfoResult storeDataInAllOffers(
        String strategyId,
        String objectId,
        ObjectDescription createObjectDescription,
        DataCategory category,
        String requester
    ) throws StorageException;

    /**
     * Store data of any type for given tenant on the given storage offer.
     *
     * @param strategyId id of the strategy
     * @param origin origin of the transaction (normal, bulk, offer_sync)
     * @param objectId the workspace URI of the data to be retrieve (and stored in
     * @param category the category of the data to store (unit, object...)
     * @param requester the requester information
     * @param offerIds offer identifiers
     * @param response the response
     * @return a StoredInfoResult containing information about the created Data
     * @throws StorageException StorageException
     */
    StoredInfoResult storeDataInOffers(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String requester,
        List<String> offerIds,
        Response response
    ) throws StorageException;

    /**
     * @param strategyId id of the strategy
     * @param origin origin of the transaction (normal, bulk, offer_sync)
     * @param streamAndInfo streamAndInfo
     * @param objectId id of the object
     * @param category the object type to list
     * @param requester the requester information
     * @param offerIds offer identfiers
     * @return StoredInfoResult
     * @throws StorageException StorageException
     */
    StoredInfoResult storeDataInOffers(
        String strategyId,
        String origin,
        StreamAndInfo streamAndInfo,
        String objectId,
        DataCategory category,
        String requester,
        List<String> offerIds
    ) throws StorageException;

    /**
     * get  offer ids list
     *
     * @param strategyId strategy  id
     * @return offers ids list
     */
    List<String> getOfferIds(String strategyId) throws StorageException;

    /**
     * Get Storage Information (availability and capacity) for the requested
     * tenant + strategy
     *
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about the storage
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @throws StorageTechnicalException Thrown in case of any technical problem
     */
    JsonNode getContainerInformation(String strategyId) throws StorageException;

    /**
     * List container objects
     *
     * @param strategyId the strategy id to get offers
     * @param category the object type to list
     * @return a response with object listing
     * @throws StorageException thrown in case of any technical problem
     */
    CloseableIterator<ObjectEntry> listContainerObjects(String strategyId, DataCategory category)
        throws StorageException;

    CloseableIterator<ObjectEntry> listContainerObjectsForOffer(
        DataCategory category,
        String offerId,
        boolean includeDisabled
    ) throws StorageException;

    /**
     * Get offer log from referent
     *
     * @param strategyId the strategy id to get offers
     * @param category the object type to list
     * @param offset offset of the excluded object
     * @param limit the number of result wanted
     * @param order order
     * @return list of offer log
     * @throws StorageException thrown in case of any technical problem
     */
    RequestResponse<OfferLog> getOfferLogs(
        String strategyId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException;

    /**
     * Get offer log from the given offer
     *
     * @param strategyId the strategy id to get offers
     * @param offerId
     * @param category the object type to list
     * @param offset offset of the excluded object
     * @param limit the number of result wanted
     * @param order order
     * @return list of offer log
     * @throws StorageException thrown in case of any technical problem
     */
    RequestResponse<OfferLog> getOfferLogsByOfferId(
        String strategyId,
        String offerId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException;

    /**
     * Get a specific Object binary data as an input stream
     * <p>
     *
     * @param strategyId id of the strategy
     * @param origin origin
     * @param objectId id of the object
     * @param category category of the object
     * @param logInformation information for accessLog
     * @return an object as a Response with an InputStream
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException thrown if a technical error happened
     */
    Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        AccessLogInfoModel logInformation
    ) throws StorageException;

    /**
     * Get a specific Object binary data as an input stream
     * <p>
     *
     * @param strategyId id of the strategy
     * @param origin origin
     * @param objectId id of the object
     * @param category category
     * @param offerId offer identfier
     * @return an object as a Response with an InputStream
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException thrown if a technical error happened
     */
    Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String offerId
    ) throws StorageException;

    /**
     * Get a specific Object information
     *
     * @param strategyId id of the strategy
     * @param type data category
     * @param objectId id of the object
     * @param offerIds list id of offers
     * @return JsonNode containing informations about the requested object
     * @throws StorageException
     */
    JsonNode getContainerInformation(
        String strategyId,
        DataCategory type,
        String objectId,
        List<String> offerIds,
        boolean noCache
    ) throws StorageException;

    /**
     * Verify if object exists. If an offer is not in hot offers in strategy, it is
     * considered as not existing.
     *
     * @param strategyId id of the strategy
     * @param objectId id of the object
     * @param category category
     * @param offerIds list id of offers
     * @return list of result existence by offerId
     * @throws StorageException StorageException
     */
    Map<String, Boolean> checkObjectExisting(
        String strategyId,
        String objectId,
        DataCategory category,
        List<String> offerIds
    ) throws StorageException;

    /**
     * Delete an object
     *
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException thrown if a technical error happened
     */
    void deleteObjectInAllOffers(String strategyId, DataContext context) throws StorageException;

    /**
     * Delete an object in  offers
     *
     * @param strategyId id of the strategy
     * @param context context
     * @param offers offers
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException thrown if a technical error happened
     */
    void deleteObjectInOffers(String strategyId, DataContext context, List<String> offers) throws StorageException;

    List<BatchObjectInformationResponse> getBatchObjectInformation(
        String strategyId,
        DataCategory type,
        List<String> objectIds,
        List<String> offerIds
    ) throws StorageException;

    BulkObjectStoreResponse bulkCreateFromWorkspace(
        String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest,
        String requester
    ) throws StorageException;

    /**
     * Retrieve all the available storage strategies
     *
     * @return all storage strategies by id
     * @throws StorageException if any unwanted technical issue happens
     */
    Map<String, StorageStrategy> getStrategies() throws StorageException;

    Optional<String> createAccessRequestIfRequired(
        String strategyId,
        String offerId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException;

    Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        String strategyId,
        String offerId,
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException;

    void removeAccessRequest(
        String strategyId,
        String offerId,
        String accessRequestId,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException;

    boolean checkObjectAvailability(
        String strategyId,
        String offerId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException;

    String getReferentOffer(String strategyId) throws StorageTechnicalException, StorageNotFoundException;

    Response launchOfferLogCompaction(String offerReferenceId, Integer tenantId) throws StorageException;
}
