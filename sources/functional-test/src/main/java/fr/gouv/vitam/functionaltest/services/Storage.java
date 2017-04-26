/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.services;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functionaltest.cucumber.step.StorageStep;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.HotStrategy;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.spi.DriverManager;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**

 */
public class Storage {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageStep.class);
    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();

    /**
     * get containers
     *
     * @param strategyId
     * @param objectId
     * @param category
     * @return
     * @throws StorageException
     */
    public Map<String, StorageGetResult> getContainerObject(String strategyId, String objectId, DataCategory category)
        throws StorageException {
        // Check input params
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter("Strategiie obligatoire", strategyId);
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
            return getGetObjectResult(tenantId, objectId, category, offerReferences);
        }
        return null;
    }

    private Map<String, StorageGetResult> getGetObjectResult(Integer tenantId, String objectId, DataCategory type,
        List<OfferReference> offerReferences) throws StorageException {
        Map<String, StorageGetResult> results = new HashMap<>();
        for (final OfferReference offerReference : offerReferences) {
            StorageGetResult result;
            final Driver driver = retrieveDriverInternal(offerReference.getId());
            final StorageOffer offer = OFFER_PROVIDER.getStorageOffer(offerReference.getId());
            final Properties parameters = new Properties();
            parameters.putAll(offer.getParameters());
            try (Connection connection = driver.connect(offer.getId())) {
                final StorageObjectRequest request = new StorageObjectRequest(tenantId, type.getFolder(), objectId);
                result = connection.getObject(request);
                if (result.getObject() != null) {
                    results.put(offerReference.getId(), result);
                }
            } catch (final StorageDriverException exc) {
                LOGGER.warn("Error with the storage, take the next offer in the strategy (by priority)", exc);
            }
        }
        return results;
    }

    private void isStrategyValid(HotStrategy hotStrategy) throws StorageTechnicalException {
        if (!hotStrategy.isCopyValid()) {
            throw new StorageTechnicalException("Invalid number of copy");
        }
    }

    private Driver retrieveDriverInternal(String offerId) throws StorageTechnicalException {
        try {
            return DriverManager.getDriverFor(offerId);
        } catch (final StorageDriverNotFoundException exc) {
            throw new StorageTechnicalException(exc);
        }
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

    private void store_from_workSpace(String uri, ObjectDescription description, String strategy)
        throws StorageNotFoundClientException, StorageServerClientException, StorageAlreadyExistsClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            storageClient.storeFileFromWorkspace(strategy,
                StorageCollectionType.OBJECTS, uri, description);
        }
    }

    /**
     * Store a file
     *
     * @param sip
     * @param uri
     * @param guid
     * @return
     * @throws StorageServerClientException
     * @throws StorageNotFoundClientException
     * @throws StorageAlreadyExistsClientException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageAlreadyExistException
     * @throws ContentAddressableStorageNotFoundException
     * @throws IOException
     */
    public boolean store(Path sip, String uri, String strategy, String guid)
        throws StorageServerClientException, StorageNotFoundClientException, StorageAlreadyExistsClientException,
        ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException, IOException {
        try (
            InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)
        ) {
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(guid);
            workspaceClient.putObject(guid, uri, inputStream);
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(guid);
            description.setWorkspaceObjectURI(uri);
            store_from_workSpace(guid, description, strategy);
            workspaceClient.deleteObject(guid, uri);

            return true;
        }
    }
}
