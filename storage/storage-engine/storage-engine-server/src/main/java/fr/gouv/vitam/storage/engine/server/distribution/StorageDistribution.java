/**
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
 */
package fr.gouv.vitam.storage.engine.server.distribution;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

/**
 * Interface Storage Distribution for Storage Operations
 * 
 */
public interface StorageDistribution {


    /**
     * Get Storage Information (availability and capacity) for the requested tenant + strategy
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the storage
     */
    JsonNode getStorageInformation(String tenantId, String strategyId) throws StorageNotFoundException;

    /**
     * Get Storage Container full content as an InputStream
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Storage Container does not exist
     * @return the content of the container as an InputStream
     */
    InputStream getStorageContainer(String tenantId, String strategyId) throws StorageNotFoundException;

    /**
     * Create a container
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageAlreadyExistsException Thrown in case the Container already exists
     * @return a JsonNode containing informations about the created Container
     */
    JsonNode createContainer(String tenantId, String strategyId) throws StorageAlreadyExistsException;

    /**
     * Delete a container
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown in case the Container does not exist
     */
    void deleteContainer(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get Container Objects Information
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about objects contained in the requested container
     */
    JsonNode getContainerObjects(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Object binary data as an input stream
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectId id of the object
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @return an object as an InputStream
     */
    InputStream getContainerObject(String tenantId, String strategyId, String objectId) throws StorageNotFoundException;

    /**
     * Get a specific Object informations
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectId id of the object
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @return JsonNode containing informations about the requested object
     */
    JsonNode getContainerObjectInformations(String tenantId, String strategyId, String objectId)
        throws StorageNotFoundException;


    /**
     * Create an object
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectId id of the object to be created
     * @throws StorageAlreadyExistsException Thrown in case the Container already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created Object
     */
    JsonNode createObject(String tenantId, String strategyId, String objectId)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Delete an object
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectId id of the object to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the object does not exist
     */
    void deleteObject(String tenantId, String strategyId, String objectId) throws StorageNotFoundException;

    /**
     * Retrieve a list of logbook ids associated to a given tenant
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about logbooks of the requested container
     */
    JsonNode getContainerLogbooks(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Logbook as a JsonNode
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param logbookId id of the logbook
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @return a logbook as a JsonNode
     */
    JsonNode getContainerLogbook(String tenantId, String strategyId, String logbookId)
        throws StorageNotFoundException;


    /**
     * Create a logbook for a given tenant
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param logbookId id of the logbook to be created
     * @throws StorageAlreadyExistsException Thrown in case the Logbook already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created Logbook
     */
    JsonNode createLogbook(String tenantId, String strategyId, String logbookId)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Delete a logbook
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param logbookId id of the logbook to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the logbook does not exist
     */
    void deleteLogbook(String tenantId, String strategyId, String logbookId) throws StorageNotFoundException;


    /**
     * Get Container Units Information
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about units of the requested container
     */
    JsonNode getContainerUnits(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Unit as a JsonNode
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param unitId id of the unit
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @return a unit as a JsonNode
     */
    JsonNode getContainerUnit(String tenantId, String strategyId, String unitId) throws StorageNotFoundException;


    /**
     * Create an unit
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param unitId id of the unit to be created
     * @throws StorageAlreadyExistsException Thrown in case the Unit already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created Unit
     */
    JsonNode createUnit(String tenantId, String strategyId, String unitId)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Update an unit
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param unitId id of the unit to be created
     * @param metadatas to be updated. Example { "title": "Generic Object", "description": "A Generic Object", "type":
     *        "object" }
     * @throws StorageAlreadyExistsException Thrown in case the Unit already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created Unit
     */
    JsonNode updateUnit(String tenantId, String strategyId, String unitId, JsonNode metadatas)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Delete an unit
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param unitId id of the Unit to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the Unit does not exist
     */
    void deleteUnit(String tenantId, String strategyId, String unitId)
        throws StorageNotFoundException;


    /**
     * Get Container ObjectGroups Information
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about objectGroups of the requested container
     */
    JsonNode getContainerObjectGroups(String tenantId, String strategyId)
        throws StorageNotFoundException;


    /**
     * Get a specific ObjectGroup as a JsonNode
     *
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectGroupId id of the ObjectGroup
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     * @return an objectGroup as a JsonNode
     */
    JsonNode getContainerObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException;


    /**
     * Create an ObjectGroup
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectGroupId id of the ObjectGroup to be created
     * @throws StorageAlreadyExistsException Thrown in case the ObjectGroup already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created ObjectGroup
     */
    JsonNode createObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Update an ObjectGroup
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectGroupId id of the ObjectGroup to be created
     * @param metadatas to be updated. Example { "title": "Generic Object", "description": "A Generic Object", "type":
     *        "object" }
     * @throws StorageAlreadyExistsException Thrown in case the ObjectGroup already exists
     * @throws StorageNotFoundException Thrown if the Container does not exist
     * @return a JsonNode containing informations about the created Unit
     */
    JsonNode updateObjectGroup(String tenantId, String strategyId, String objectGroupId, JsonNode metadatas)
        throws StorageAlreadyExistsException, StorageNotFoundException;

    /**
     * Delete an ObjectGroup
     * 
     * @param tenantId id of the tenant
     * @param strategyId id of the strategy
     * @param objectGroupId id of the ObjectGroup to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the ObjectGroup does not exist
     */
    void deleteObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException;


    /**
     * Get the status from the service
     * 
     * @throws StorageException if the Server got an internal error
     * @return the status as a JsonNode
     */
    JsonNode status() throws StorageException;

}
