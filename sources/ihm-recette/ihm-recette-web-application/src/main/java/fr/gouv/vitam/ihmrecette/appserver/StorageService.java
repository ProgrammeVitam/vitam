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
package fr.gouv.vitam.ihmrecette.appserver;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageAccessRequestCreationRequest;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
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
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StorageService {

    private static final String STRATEGY_ID_IS_MANDATORY = "Strategy id is mandatory";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageService.class);
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final String OBJECT_ID_IS_MANDATORY = "Object id is mandatory";
    private static final String CATEGORY_IS_MANDATORY = "Category is mandatory";
    private static final String ACCESS_REQUEST_ID_IS_MANDATORY = "Access request id is mandatory";

    /**
     * Constructs the service with a given configuration
     *
     * @param configuration configuration of storage server
     */
    public StorageService(StorageConfiguration configuration) {
        ParametersChecker.checkParameter("Storage service configuration is mandatory", configuration);
        String urlWorkspace = configuration.getUrlWorkspace();
        WorkspaceClientFactory.changeMode(urlWorkspace);
    }

    private static StorageOffer getStorageOffer(OfferReference offerReference) {
        StorageOffer storageOffer = null;
        try {
            storageOffer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
        } catch (StorageException e) {
            LOGGER.error(e);
        }
        return storageOffer;
    }

    private VitamError buildError(VitamCode vitamCode, String message) {
        return new VitamError(VitamCodeHelper.getCode(vitamCode))
            .setContext(vitamCode.getService().getName())
            .setHttpCode(vitamCode.getStatus().getStatusCode())
            .setState(vitamCode.getDomain().getName())
            .setMessage(vitamCode.getMessage())
            .setDescription(message);
    }

    public RequestResponse<String> createAccessRequest(
        Integer tenantId,
        String strategyId,
        String offerId,
        String objectId,
        DataCategory category
    ) throws StorageTechnicalException, StorageNotFoundException {
        checkStoreDataParams(strategyId, objectId, category);

        Driver driver = getDriver(strategyId, offerId, true);
        try (Connection connection = driver.connect(offerId)) {
            StorageAccessRequestCreationRequest storageAccessRequestCreationRequest =
                new StorageAccessRequestCreationRequest(
                    tenantId,
                    category.getFolder(),
                    Collections.singletonList(objectId)
                );

            String accessRequestId = connection.createAccessRequest(storageAccessRequestCreationRequest);
            return new RequestResponseOK<String>().setHttpCode(Status.OK.getStatusCode()).addResult(accessRequestId);
        } catch (StorageDriverException e) {
            return buildError(VitamCode.STORAGE_OBJECT_NOT_FOUND, e.getMessage());
        }
    }

    public VitamAsyncInputStreamResponse download(
        Integer tenantId,
        DataCategory dataCategory,
        String strategyId,
        String offerId,
        String objectId
    ) throws StorageTechnicalException, StorageDriverException, StorageNotFoundException {
        ParametersChecker.checkParameter(ACCESS_REQUEST_ID_IS_MANDATORY, objectId);

        final Driver driver = getDriver(strategyId, offerId, false);

        try (Connection connection = driver.connect(offerId)) {
            final StorageObjectRequest request = new StorageObjectRequest(tenantId, dataCategory.getFolder(), objectId);
            return new VitamAsyncInputStreamResponse(
                connection.getObject(request).getObject(),
                Status.OK,
                MediaType.APPLICATION_OCTET_STREAM_TYPE
            );
        }
    }

    public RequestResponse<AccessRequestStatus> checkAccessRequestStatus(
        Integer tenantId,
        String strategyId,
        String offerId,
        String accessRequestId
    ) throws StorageTechnicalException, StorageNotFoundException {
        ParametersChecker.checkParameter(ACCESS_REQUEST_ID_IS_MANDATORY, accessRequestId);

        Driver driver = getDriver(strategyId, offerId, true);

        try (Connection connection = driver.connect(offerId)) {
            Map<String, AccessRequestStatus> accessRequestStatusMap = connection.checkAccessRequestStatuses(
                List.of(accessRequestId),
                tenantId,
                false
            );

            return new RequestResponseOK<AccessRequestStatus>()
                .setHttpCode(Status.OK.getStatusCode())
                .addResult(accessRequestStatusMap.get(accessRequestId));
        } catch (StorageDriverException e) {
            return buildError(VitamCode.STORAGE_OBJECT_NOT_FOUND, e.getMessage());
        }
    }

    public RequestResponse<AccessRequestStatus> removeAccessRequest(
        Integer tenantId,
        String strategyId,
        String offerId,
        String accessRequestId
    ) throws StorageTechnicalException, StorageNotFoundException {
        ParametersChecker.checkParameter(ACCESS_REQUEST_ID_IS_MANDATORY, accessRequestId);

        Driver driver = getDriver(strategyId, offerId, true);

        try (Connection connection = driver.connect(offerId)) {
            connection.removeAccessRequest(accessRequestId, tenantId, false);

            return new RequestResponseOK<AccessRequestStatus>().setHttpCode(Status.OK.getStatusCode());
        } catch (StorageDriverException e) {
            return buildError(VitamCode.STORAGE_OBJECT_NOT_FOUND, e.getMessage());
        }
    }

    private Driver getDriver(String strategyId, String offerId, boolean mustBeAsyncDriver)
        throws StorageTechnicalException, StorageNotFoundException {
        List<OfferReference> offerReferences = getOffersReferences(strategyId);

        if (offerReferences == null || offerReferences.isEmpty()) {
            throw new StorageTechnicalException("No offer found in strategy " + strategyId);
        }

        StorageOffer storageOffers = offerReferences
            .stream()
            .map(StorageService::getStorageOffer)
            .filter(o -> o.getId().equals(offerId))
            .findFirst()
            .orElseThrow(
                () -> new StorageTechnicalException("No such offer " + offerId + " in strategy " + strategyId)
            );

        if (mustBeAsyncDriver && !storageOffers.isAsyncRead()) {
            throw new StorageTechnicalException("Offer " + offerId + " does not support AsyncRead");
        }

        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException exc) {
            throw new StorageTechnicalException(exc);
        }
    }

    private void checkStoreDataParams(String strategyId, String dataId, DataCategory category) {
        ParametersChecker.checkParameter(STRATEGY_ID_IS_MANDATORY, strategyId);
        ParametersChecker.checkParameter(OBJECT_ID_IS_MANDATORY, dataId);
        ParametersChecker.checkParameter(CATEGORY_IS_MANDATORY, category);
    }

    public List<OfferReference> getOffersReferences(String strategyId)
        throws StorageNotFoundException, StorageTechnicalException {
        final StorageStrategy storageStrategy = STRATEGY_PROVIDER.getStorageStrategy(strategyId);
        if (storageStrategy == null) {
            throw new StorageNotFoundException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_STRATEGY_NOT_FOUND));
        }

        final List<OfferReference> offerReferences = new ArrayList<>();
        if (!storageStrategy.getOffers().isEmpty()) {
            // TODO P1 : this code will be changed in the future to handle
            // priority (not in current US scope) and copy
            offerReferences.addAll(storageStrategy.getOffers());
        }
        return offerReferences;
    }
}
