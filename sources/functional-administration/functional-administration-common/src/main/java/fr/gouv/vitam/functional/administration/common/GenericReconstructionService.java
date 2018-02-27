package fr.gouv.vitam.functional.administration.common;

import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;

/**
 * This service is used to handle reconstruction of vitam referential.
 */
public class GenericReconstructionService {

    private GenericVitamRepository repository;
    private StorageClientFactory storageClientFactory;

    /**
     *
     * @param repository
     * @param storageClientFactory
     */
    public GenericReconstructionService(GenericVitamRepository repository,
        StorageClientFactory storageClientFactory) {
        this.repository = repository;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * Reconstruct
     * 
     * @param collections
     */
    public void reconstruct(FunctionalAdminCollections collections) {

    }

    /**
     * Reconstruct collection
     * @param collections
     * @param tenant
     */
    public void reconstruct(FunctionalAdminCollections collections, int tenant) {

    }

}
