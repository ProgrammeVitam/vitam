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
package fr.gouv.vitam.common.database.api;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.exception.DatabaseException;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This repository is a specification of vitam data management
 */
public interface VitamRepository {
    /**
     * Save vitam document
     *
     * @param document the document to be saved
     * @throws DatabaseException in case error with database occurs
     */
    void save(Document document) throws DatabaseException;

    /**
     * Save or updatevitam document
     *
     * @param document the document to be saved
     * @return status CREATED or UPDATED
     * @throws DatabaseException in case error with database occurs
     */
    VitamRepositoryStatus saveOrUpdate(Document document) throws DatabaseException;

    /**
     * Save a list of vitam documents
     *
     * @param documents the list of documents to be saved
     * @throws DatabaseException in case error with database occurs
     */
    void save(List<Document> documents) throws DatabaseException;

    /**
     * Save or update a list of vitam documents
     *
     * @param documents the list of document to be saved orupdated
     * @throws DatabaseException in case error with database occurs
     */
    void saveOrUpdate(List<Document> documents) throws DatabaseException;

    /**
     * Used to execute a bulk update
     * If document exists then update
     * If document do not exists then create document
     * throw Duplicate key exception if document exists by _id but not exists by filter in the update one model
     *
     * @param updates
     * @throws DatabaseException
     */
    void update(List<WriteModel<Document>> updates) throws DatabaseException;

    /**
     * Remove document by id
     *
     * @param id the id of the document to be removed
     * @param tenant the tenant of the document to be removed
     * @throws DatabaseException in case error with database occurs
     */
    void remove(String id, Integer tenant) throws DatabaseException;

    /**
     * Be careful when using this method
     * Remove by query
     *
     * @param query
     * @throws DatabaseException
     */
    long remove(Bson query) throws DatabaseException;

    /**
     * Be careful when using this method
     * Remove collection by name and tenant
     *
     * @param name the name of the collection to be removed
     * @param tenant the tenant of the collection to be removed
     * @throws DatabaseException in case error with database occurs
     */
    void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException;

    /**
     * Be careful when using this method
     * Remove by tenant for collection multi-tenant
     *
     * @param tenant the tenant
     * @return the number of deleted documents
     * @throws DatabaseException in case error with database occurs
     */
    long purge(Integer tenant) throws DatabaseException;

    /**
     * Be careful when using this method
     * Remove by tenant for collection cross-tenant
     *
     * @return number of purged documents
     * @throws DatabaseException in case error with database occurs
     */
    long purge() throws DatabaseException;

    /**
     * Get vitam document by id
     *
     * @param id the document id
     * @param tenant the tenant of the document
     * @return the document if found
     * @throws DatabaseException in case error with database occurs
     */
    Optional<Document> getByID(String id, Integer tenant) throws DatabaseException;

    /**
     * find by identifier for all tenant
     *
     * @param identifier the identifier of the document
     * @param tenant the tenant of the document
     * @return the document if found
     * @throws DatabaseException in case error with database occurs
     */
    Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant) throws DatabaseException;

    /**
     * Find by identifier for collections cross tenant
     *
     * @param identifier the identifier of the document
     * @return the document if found
     * @throws DatabaseException in case error with database occurs
     */
    Optional<Document> findByIdentifier(String identifier) throws DatabaseException;

    /**
     * Find collection of document by there id and return only projection fields
     *
     * @param ids list of documents id
     * @param projection the fields wanted in the result
     * @return An iterable of documents
     */
    FindIterable<Document> findDocuments(Collection<String> ids, Bson projection);

    /**
     * Return iterable over document for the given collection for a specific tenant
     *
     * @param mongoBatchSize mongoBatchSize
     * @param tenant tenant id
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findDocuments(int mongoBatchSize, Integer tenant);

    /**
     * Return iterable over document for the given collection for a specific tenant and fields
     *
     * @param fields list of fields for filter
     * @param mongoBatchSize mongoBatchSize
     * @param tenant tenant id
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findByFieldsDocuments(Map<String, String> fields, int mongoBatchSize, Integer tenant);

    /**
     * Return iterable over document for the given collection
     *
     * @param mongoBatchSize mongoBatchSize
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findDocuments(int mongoBatchSize);

    /**
     * Count occurence of documents
     *
     * @return the number of documents
     */
    long count();

    /**
     * Count occurence of documents
     *
     * @param filter filtre
     * @return the number of documents
     */
    long count(Bson filter);

    /**
     * Return iterable over document for the given collection
     *
     * @param query the mongo query to be executed
     * @param mongoBatchSize mongoBatchSize
     * @return iterable over document for the given collection
     */
    FindIterable<Document> findDocuments(Bson query, int mongoBatchSize);

    /**
     * Deleted all documents by ids
     *
     * @param ids
     * @param tenant
     */
    void delete(List<String> ids, int tenant) throws DatabaseException;
}
