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
package fr.gouv.vitam.common.database.api;

import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import fr.gouv.vitam.common.exception.DatabaseException;

import fr.gouv.vitam.common.exception.DatabaseException;

/**
 * This repository is a specification of vitam data management
 */
public interface VitamRepository {
    /**
     * Save vitam document
     *
     * @param document
     * @throws DatabaseException
     */
    void save(Document document) throws DatabaseException;

    /**
     * Save a list of vitam documents
     *
     * @param documents
     * @throws DatabaseException
     */
    void save(List<Document> documents) throws DatabaseException;

    /**
     * Save or update a list of vitam documents
     *
     * @param documents
     * @throws DatabaseException
     */
    void saveOrUpdate(List<Document> documents) throws DatabaseException;


    /**
     * Remove document by id
     *
     * @param id
     * @param tenant
     * @throws DatabaseException
     */
    void remove(String id, Integer tenant) throws DatabaseException;


    /**
     * Remove collection by name and tenant
     *
     * @param name
     * @param tenant
     * @throws DatabaseException
     */
    void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException;

    /**
     * Remove by tenant for collection multi-tenant
     *
     * Remove by tenant
     * @param tenant
     * @return
     * @throws DatabaseException
     */
    long purge(Integer tenant) throws DatabaseException;

    /**
     * Remove by tenant for collection cross-tenant
     * 
     * @return
     * @throws DatabaseException
     */
    long purge() throws DatabaseException;


    /**
     * Get vitam document by id
     *
     * @param id
     * @param tenant
     * @return Optional
     * @throws DatabaseException
     */
    Optional<Document> getByID(String id, Integer tenant) throws DatabaseException;


    /**
     * find by identifier for all tenant
     * 
     * @param identifier
     * @param tenant
     * @return
     * @throws DatabaseException
     */
    Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant) throws DatabaseException;

    /**
     * Find by identifier for collections cross tenant
     * 
     * @param identifier
     * @return
     * @throws DatabaseException
     */
    Optional<Document> findByIdentifier(String identifier) throws DatabaseException;
    
    /**
     * Return iterable over document for the given collection for a specific tenant
     *
     * @param mongoBatchSize mongoBatchSize
     * @param tenant         tenant id
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findDocuments(int mongoBatchSize, Integer tenant);

    /**
     * Return iterable over document for the given collection
     *
     * @param mongoBatchSize mongoBatchSize
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findDocuments(int mongoBatchSize);
}
