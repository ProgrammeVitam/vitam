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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageIllegalOperationClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectAvailabilityRequest;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage Client interface
 */
public interface StorageClient extends BasicClient {
    /**
     * Check if the storage of objects could be done, knowing a required size
     *
     * @param strategyId the storage strategy id
     * @return the capacity of the storage
     * @throws StorageNotFoundClientException if the Server got a NotFound result
     * @throws StorageServerClientException if the Server got an internal error
     */
    JsonNode getStorageInformation(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException;

    /**
     * get List of offers for a strategy
     *
     * @param strategyId strategyId
     * @return list id  of offers
     * @throws StorageNotFoundClientException
     * @throws StorageServerClientException
     */
    List<String> getOffers(String strategyId) throws StorageNotFoundClientException, StorageServerClientException;

    /**
     * Store an object available in workspace by its vitam guid
     *
     * @param strategyId the storage strategy id
     * @param type the type of object collection
     * @param guid vitam guid
     * @param description object description
     * @return the result status of object creation
     * @throws StorageAlreadyExistsClientException if the Server got a CONFLICT status result
     * @throws StorageNotFoundClientException if the Server got a NotFound result
     * @throws StorageServerClientException if the Server got an internal error
     */
    StoredInfoResult storeFileFromWorkspace(
        String strategyId,
        DataCategory type,
        String guid,
        ObjectDescription description
    ) throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException;

    /**
     * Store objects available in workspace into offers
     *
     * @param strategyId the storage strategy id
     * @param bulkObjectStoreRequest request
     * @return the result status of object creation
     * @throws StorageAlreadyExistsClientException if the Server got a CONFLICT status result
     * @throws StorageNotFoundClientException if the Server got a NotFound result
     * @throws StorageServerClientException if the Server got an internal error
     */
    BulkObjectStoreResponse bulkStoreFilesFromWorkspace(
        String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest
    ) throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException;

    /**
     * Check the existence of an object in storage by its id and type {@link DataCategory}.
     *
     * @param strategyId the storage strategy id
     * @param type the type of object collection
     * @param guid vitam guid
     * @return true/false for each offer
     * @throws StorageServerClientException if the Server got an internal error
     */
    Map<String, Boolean> exists(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException;

    /**
     * Delete an object of given type in the storage offer strategy
     *
     * @param strategyId the storage strategy id
     * @param type the type of object collection
     * @param guid vitam guid
     * @return true if deleted
     * @throws StorageServerClientException if the Server got an internal error
     */
    boolean delete(String strategyId, DataCategory type, String guid) throws StorageServerClientException;

    /**
     * Delete an object of given type in the storage offer strategy
     *
     * @param strategyId the storage strategy id
     * @param type the type of object collection
     * @param guid vitam guid
     * @param offerIds offers ids to delete
     * @return true if deleted
     */

    boolean delete(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException;

    /**
     * Retrieves a binary object knowing its guid as an inputStream for a specific tenant/strategy
     *
     * @param strategyId the storage strategy id
     * @param guid vitam guid of the object to be returned
     * @param type the object type to list
     * @param logInfo additional information for accessLog
     * @return the object requested
     * @throws StorageServerClientException if the Server got an internal error
     * @throws StorageNotFoundException if the Server got a NotFound result, if the container or the object does not exist
     * @throws StorageUnavailableDataFromAsyncOfferClientException if object is not available for immediate access from async offer
     */
    Response getContainerAsync(String strategyId, String guid, DataCategory type, AccessLogInfoModel logInfo)
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException;

    /**
     * Retrieves a binary object knowing its guid as an inputStream for a specific tenant/strategy/offerId
     *
     * @param strategyId
     * @param offerId
     * @param objectName
     * @param type
     * @param logInfo
     * @return
     * @throws StorageServerClientException
     * @throws StorageNotFoundException
     */
    Response getContainerAsync(
        String strategyId,
        String offerId,
        String objectName,
        DataCategory type,
        AccessLogInfoModel logInfo
    )
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException;

    /**
     * List object type in container
     *
     * @param strategyId the strategy ID
     * @param offerId the Offer ID
     * @param type the object type to list
     * @return an iterator with object list
     * @throws StorageServerClientException thrown if the server got an internal error
     */
    CloseableIterator<ObjectEntry> listContainer(String strategyId, String offerId, DataCategory type)
        throws StorageServerClientException, StorageNotFoundClientException;

    /**
     * Call storage accesslog backup operation.
     *
     * @param tenants tenants list to backup
     * @return Storage logbook backup response
     * @throws StorageServerClientException
     * @throws InvalidParseOperationException
     */
    RequestResponseOK<StorageLogBackupResult> storageAccessLogBackup(List<Integer> tenants)
        throws StorageServerClientException, InvalidParseOperationException;

    /**
     * Call storage log backup operation.
     *
     * @param tenants tenants list to backup
     * @return Storage logbook backup response
     * @throws StorageServerClientException StorageServerClientException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    RequestResponseOK<StorageLogBackupResult> storageLogBackup(List<Integer> tenants)
        throws StorageServerClientException, InvalidParseOperationException;

    /**
     * Call storage log traceability operation.
     *
     * @param tenants
     * @return storage log traceability response
     * @throws StorageServerClientException StorageServerClientException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    RequestResponseOK<StorageLogTraceabilityResult> storageLogTraceability(List<Integer> tenants)
        throws StorageServerClientException, InvalidParseOperationException;

    /**
     * Get object information from objects in storage
     *
     * @param strategyId the storage strategy id
     * @param type the object type to list
     * @param guid vitam guid
     * @param offerIds offers ids to delete
     * @param noCache noCache forces digest computation.
     * @return informations
     * @throws StorageServerClientException StorageServerClientException
     * @throws StorageNotFoundClientException StorageNotFoundClientException
     */
    JsonNode getInformation(String strategyId, DataCategory type, String guid, List<String> offerIds, boolean noCache)
        throws StorageServerClientException, StorageNotFoundClientException;

    /**
     * Get object information from objects in storage
     *
     * @param strategyId the storage strategy id
     * @param type the object type to list
     * @param offerIds offers ids
     * @param objectIds list of object ids
     * @return informations
     * @throws StorageServerClientException StorageServerClientException
     */
    RequestResponse<BatchObjectInformationResponse> getBatchObjectInformation(
        String strategyId,
        DataCategory type,
        Collection<String> offerIds,
        Collection<String> objectIds
    ) throws StorageServerClientException;

    /**
     * @param objectId objectId
     * @param category category
     * @param source source
     * @param destination destination
     * @param strategyId strategyId
     * @return RequestResponseOK
     * @throws StorageServerClientException StorageServerClientException
     * @throws InvalidParseOperationException StorageServerClientException
     */
    RequestResponseOK copyObjectFromOfferToOffer(
        String objectId,
        DataCategory category,
        String source,
        String destination,
        String strategyId
    )
        throws StorageServerClientException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException;

    /**
     * @param strategyId strategyId
     * @param objectId objectId
     * @param category category
     * @param inputStream inputStream
     * @param inputStreamSize inputStreamSize
     * @param offerIds offers ids
     * @return RequestResponseOK
     * @throws StorageServerClientException StorageServerClientException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    RequestResponseOK create(
        String strategyId,
        String objectId,
        DataCategory category,
        InputStream inputStream,
        Long inputStreamSize,
        List<String> offerIds
    ) throws StorageServerClientException, InvalidParseOperationException;

    /**
     * Get offer log .
     *
     * @param strategyId the strategy to get offers
     * @param offerId the offer Id to read object from
     * @param type the object type to list
     * @param offset offset of the last object before
     * @param limit the number of result wanted
     * @param order the order order
     * @return list of offer log
     * @throws StorageServerClientException
     */
    RequestResponse<OfferLog> getOfferLogs(
        String strategyId,
        String offerId,
        DataCategory type,
        Long offset,
        int limit,
        Order order
    ) throws StorageServerClientException;

    /**
     * Get strategies.
     *
     * @return strategies available for storage
     * @throws StorageServerClientException
     */
    RequestResponse<StorageStrategy> getStorageStrategies() throws StorageServerClientException;

    /**
     * Create access request if target offer does not support synchronous read (tape library storage). If target offer
     * supports synchronous read requests, then no access request is created.
     *
     * @param strategyId the target storage strategy identifier
     * @param offerId the target offer identifier (Optional). If provided, the offerId must be a valid active offer of the specified strategy. Otherwise, the referent offer of strategy is used.
     * @param dataCategory the data category of objects to which access is requested
     * @param objectNames the object names/ids/guids to which access is requested
     * @return an AccessRequestId if access request is required (async offer), otherwiser {@code Optional.empty()}
     * @throws StorageServerClientException if any problem occurs during request
     */
    Optional<String> createAccessRequestIfRequired(
        String strategyId,
        String offerId,
        DataCategory dataCategory,
        List<String> objectNames
    ) throws StorageServerClientException;

    /**
     * Check access request statuses of asynchronous offer.
     *
     * @param strategyId the target storage strategy identifier
     * @param offerId the target offer identifier (Optional). If provided, the offerId must be a valid active offer of the specified strategy. Otherwise, the referent offer of strategy is used.
     * @param accessRequestIds the accessRequestIds whose status is to be checked
     * @param adminCrossTenantAccessRequestAllowed when {@code true}, access to access requests of other tenants is allowed from Admin tenant
     * @return the statuses of provided access request ids
     * @throws StorageServerClientException if any problem occurs during request
     */
    Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        String strategyId,
        String offerId,
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageServerClientException, StorageIllegalOperationClientException;

    /**
     * Removes (cancel / delete) and access request for an asynchronous offer.
     * Does nothing is access request id does not exist (idempotency)
     *
     * @param strategyId the target storage strategy identifier
     * @param offerId the target offer identifier (Optional). If provided, the offerId must be a valid active offer of the specified strategy. Otherwise, the referent offer of strategy is used.
     * @param accessRequestId the accessRequestId to remove
     * @param adminCrossTenantAccessRequestAllowed when {@code true}, removing access requests of other tenants is allowed from Admin tenant
     * @throws StorageServerClientException if any problem occurs during request
     */
    void removeAccessRequest(
        String strategyId,
        String offerId,
        String accessRequestId,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageServerClientException, StorageIllegalOperationClientException;

    /**
     * Checks immediate object availability in storage offer.
     * Synchronous offers have guaranteed immediate availability of objects.
     * Async offers (tape storage) may not be able to serve object immediately.
     *
     * @param strategyId the target storage strategy identifier
     * @param offerId the target offer identifier (Optional). If provided, the offerId must be a valid active offer of the specified strategy. Otherwise, the referent offer of strategy is used.
     * @param bulkObjectAvailabilityRequest object availability check request
     * @return {@code true} if ALL objects are available, otherwise {@code false}.
     * @throws StorageServerClientException
     */
    BulkObjectAvailabilityResponse checkBulkObjectAvailability(
        String strategyId,
        String offerId,
        BulkObjectAvailabilityRequest bulkObjectAvailabilityRequest
    ) throws StorageServerClientException;

    /**
     * Get referent offer of strategy
     *
     * @param strategy
     * @return
     */
    String getReferentOffer(String strategy) throws StorageNotFoundClientException, StorageServerClientException;

    Response launchOfferLogCompaction(VitamContext vitamContext, String offerId) throws StorageServerClientException;
}
