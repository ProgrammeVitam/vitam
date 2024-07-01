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
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.model.RequestResponse;
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
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proxy StorageDistribution implementation that prevents write operations on ReadOnly deployments (secondary site)
 */
public class ReadOnlyShieldStorageDistribution implements StorageDistribution {

    private final StorageDistribution innerStorageDistribution;
    private final AlertService alertService;

    ReadOnlyShieldStorageDistribution(StorageDistribution innerStorageDistribution, AlertService alertService) {
        this.innerStorageDistribution = innerStorageDistribution;
        this.alertService = alertService;
    }

    @Override
    public void close() {
        innerStorageDistribution.close();
    }

    @Override
    public StoredInfoResult copyObjectFromOfferToOffer(
        DataContext context,
        String sourceOffer,
        String destinationOffer
    ) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not copy object " +
            context.getCategory() +
            "/" +
            context.getObjectId() +
            " from offer " +
            sourceOffer +
            " to offer " +
            destinationOffer
        );
    }

    @Override
    public StoredInfoResult storeDataInAllOffers(
        String strategyId,
        String objectId,
        ObjectDescription createObjectDescription,
        DataCategory category,
        String requester
    ) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not copy object " +
            category +
            "/" +
            objectId +
            " to all offers for strategy " +
            strategyId
        );
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
    ) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not copy object " +
            category +
            "/" +
            objectId +
            " to offers " +
            offerIds
        );
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
    ) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not copy object " +
            category +
            "/" +
            objectId +
            " to offers " +
            offerIds
        );
    }

    @Override
    public List<String> getOfferIds(String strategyId) throws StorageException {
        return innerStorageDistribution.getOfferIds(strategyId);
    }

    @Override
    public JsonNode getContainerInformation(String strategyId) throws StorageException {
        return innerStorageDistribution.getContainerInformation(strategyId);
    }

    @Override
    public CloseableIterator<ObjectEntry> listContainerObjects(String strategyId, DataCategory category)
        throws StorageException {
        return innerStorageDistribution.listContainerObjects(strategyId, category);
    }

    @Override
    public CloseableIterator<ObjectEntry> listContainerObjectsForOffer(
        DataCategory category,
        String offerId,
        boolean includeDisabled
    ) throws StorageException {
        return innerStorageDistribution.listContainerObjectsForOffer(category, offerId, includeDisabled);
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(
        String strategyId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
        return innerStorageDistribution.getOfferLogs(strategyId, category, offset, limit, order);
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogsByOfferId(
        String strategyId,
        String offerId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
        return innerStorageDistribution.getOfferLogsByOfferId(strategyId, offerId, category, offset, limit, order);
    }

    @Override
    public Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        AccessLogInfoModel logInformation
    ) throws StorageException {
        return innerStorageDistribution.getContainerByCategory(strategyId, origin, objectId, category, logInformation);
    }

    @Override
    public Response getContainerByCategory(
        String strategyId,
        String origin,
        String objectId,
        DataCategory category,
        String offerId
    ) throws StorageException {
        return innerStorageDistribution.getContainerByCategory(strategyId, origin, objectId, category, offerId);
    }

    @Override
    public JsonNode getContainerInformation(
        String strategyId,
        DataCategory type,
        String objectId,
        List<String> offerIds,
        boolean noCache
    ) throws StorageException {
        return innerStorageDistribution.getContainerInformation(strategyId, type, objectId, offerIds, noCache);
    }

    @Override
    public Map<String, Boolean> checkObjectExisting(
        String strategyId,
        String objectId,
        DataCategory category,
        List<String> offerIds
    ) throws StorageException {
        return innerStorageDistribution.checkObjectExisting(strategyId, objectId, category, offerIds);
    }

    @Override
    public void deleteObjectInAllOffers(String strategyId, DataContext context) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not delete object " +
            context.getCategory() +
            "/" +
            context.getObjectId() +
            " from all offers of strategy " +
            strategyId
        );
    }

    @Override
    public void deleteObjectInOffers(String strategyId, DataContext context, List<String> offers) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not delete object " +
            context.getCategory() +
            "/" +
            context.getObjectId() +
            " from offers " +
            offers
        );
    }

    @Override
    public List<BatchObjectInformationResponse> getBatchObjectInformation(
        String strategyId,
        DataCategory type,
        List<String> objectIds,
        List<String> offerIds
    ) throws StorageException {
        return innerStorageDistribution.getBatchObjectInformation(strategyId, type, objectIds, offerIds);
    }

    @Override
    public BulkObjectStoreResponse bulkCreateFromWorkspace(
        String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest,
        String requester
    ) {
        throw reportIllegalAccess(
            "Cannot write in ReadOnly mode. Could not proceed bulk objects write for container " +
            bulkObjectStoreRequest.getType() +
            " into all offers of strategy " +
            strategyId
        );
    }

    @Override
    public Map<String, StorageStrategy> getStrategies() throws StorageException {
        return innerStorageDistribution.getStrategies();
    }

    @Override
    public Optional<String> createAccessRequestIfRequired(
        String strategyId,
        String offerId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException {
        return innerStorageDistribution.createAccessRequestIfRequired(strategyId, offerId, dataCategory, objectsNames);
    }

    @Override
    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        String strategyId,
        String offerId,
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException {
        return innerStorageDistribution.checkAccessRequestStatuses(
            strategyId,
            offerId,
            accessRequestIds,
            adminCrossTenantAccessRequestAllowed
        );
    }

    @Override
    public void removeAccessRequest(
        String strategyId,
        String offerId,
        String accessRequestId,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws StorageException {
        innerStorageDistribution.removeAccessRequest(
            strategyId,
            offerId,
            accessRequestId,
            adminCrossTenantAccessRequestAllowed
        );
    }

    @Override
    public boolean checkObjectAvailability(
        String strategyId,
        String offerId,
        DataCategory dataCategory,
        List<String> objectsNames
    ) throws StorageException {
        return innerStorageDistribution.checkObjectAvailability(strategyId, offerId, dataCategory, objectsNames);
    }

    @Override
    public String getReferentOffer(String strategyId) throws StorageTechnicalException, StorageNotFoundException {
        return innerStorageDistribution.getReferentOffer(strategyId);
    }

    @Override
    public Response launchOfferLogCompaction(String offerReferenceId, Integer tenantId) throws StorageException {
        return innerStorageDistribution.launchOfferLogCompaction(offerReferenceId, tenantId);
    }

    private IllegalStateException reportIllegalAccess(String errorMessage) {
        IllegalStateException illegalStateException = new IllegalStateException(errorMessage);
        alertService.createAlert(VitamLogLevel.ERROR, errorMessage, illegalStateException);
        return illegalStateException;
    }
}
