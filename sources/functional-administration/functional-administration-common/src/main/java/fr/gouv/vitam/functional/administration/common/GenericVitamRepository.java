package fr.gouv.vitam.functional.administration.common;

import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;

/**
 * This repository is used to persist, update or remove data from vitam databases.
 * At first, it is used to manage reconstruction.
 */
public class GenericVitamRepository {

    
    /**
     * Persist with bulk mode in vitam databases (mongo, elasticsearch, ...)
     *
     * @param collections the concerning collection
     */
    public void persist(FunctionalAdminCollections collections) {

    }

    /**
     * Persist with bulk mode in vitam databases (mongo, elasticsearch, ...)
     *
     * @param collections the concerning collection
     * @param tenant      the concerning tenant
     */
    public void persist(FunctionalAdminCollections collections, int tenant) {

    }

    /**
     * Delete all documents in all vitam databases for the given tenant
     *
     * @param collections the concerning collection
     * @param tenant      the concerning tenant
     */
    public void remove(FunctionalAdminCollections collections, int tenant) {

    }
}
