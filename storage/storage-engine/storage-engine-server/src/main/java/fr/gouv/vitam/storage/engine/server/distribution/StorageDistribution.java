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
package fr.gouv.vitam.storage.engine.server.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

import java.io.InputStream;

/**
 * Interface Storage Distribution for Storage Operations
 */
public interface StorageDistribution {

    /**
     * Store data of any type for given tenant on storage offers associated to given strategy
     * TODO: maybe the logbook object should be an inputstream as well. This would be an other US responsibility (not
     * #72)
     * TODO : method with 6 parameters, compact it
     *
     * @param tenantId                id of the tenant
     * @param strategyId              id of the strategy
     * @param objectId        the workspace URI of the data to be retrieve (and stored in offer)
     * @param createObjectDescription object additional informations
     * @param category                the category of the data to store (unit, object...)
     * @param jsonData                the data to store. <em>MUST</em> be null for data of category Object since the binary content is
     *                                retrieved on workspace based on the objectId parameter.
     * @return a StoredInfoResult containing informations about the created Data
     * @throws StorageNotFoundException  Thrown if the Container does not exist
     * @throws StorageTechnicalException Thrown in case of any technical problem
     */
    StoredInfoResult storeData(String tenantId, String strategyId, String objectId,
        CreateObjectDescription createObjectDescription, DataCategory category,
        JsonNode jsonData) throws StorageTechnicalException, StorageNotFoundException;

    /**
     * Get Storage Information (availability and capacity) for the requested tenant + strategy
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about the storage
     * @throws StorageNotFoundException Thrown if the Container does not exist
     */
    JsonNode getStorageInformation(String tenantId, String strategyId) throws StorageNotFoundException;

    /**
     * Get Storage Container full content as an InputStream
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return the content of the container as an InputStream
     * @throws StorageNotFoundException Thrown if the Storage Container does not exist
     */
    InputStream getStorageContainer(String tenantId, String strategyId) throws StorageNotFoundException,
        StorageTechnicalException;

    /**
     * Create a container
     * TODO : container creation possibility needs to be re-think then deleted or implemented. Vitam Architects
     * are aware of this.
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about the created Container
     * @throws StorageException Thrown in case the Container already exists
     */
    JsonNode createContainer(String tenantId, String strategyId) throws StorageException;

    /**
     * Delete a container
     * <p>
     * TODO : container deletion possibility needs to be re-think then deleted or implemented. Vitam Architects
     * are aware of this.
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @throws StorageTechnicalException Thrown in case of any technical problem
     * @throws StorageNotFoundException  Thrown in case the Container does not exist
     */
    void deleteContainer(String tenantId, String strategyId) throws StorageTechnicalException, StorageNotFoundException;


    /**
     * Get Container Objects Information
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about objects contained in the requested container
     * @throws StorageNotFoundException Thrown if the Container does not exist
     */
    JsonNode getContainerObjects(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Object binary data as an input stream
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param objectId   id of the object
     * @return an object as an InputStream
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     */
    InputStream getContainerObject(String tenantId, String strategyId, String objectId) throws StorageNotFoundException;

    /**
     * Get a specific Object informations
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param objectId   id of the object
     * @return JsonNode containing informations about the requested object
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     */
    JsonNode getContainerObjectInformations(String tenantId, String strategyId, String objectId)
        throws StorageNotFoundException;

    /**
     * Delete an object
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param objectId   id of the object to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the object does not exist
     */
    void deleteObject(String tenantId, String strategyId, String objectId) throws StorageNotFoundException;

    /**
     * Retrieve a list of logbook ids associated to a given tenant
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about logbooks of the requested container
     * @throws StorageNotFoundException Thrown if the Container does not exist
     */
    JsonNode getContainerLogbooks(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Logbook as a JsonNode
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param logbookId  id of the logbook
     * @return a logbook as a JsonNode
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     */
    JsonNode getContainerLogbook(String tenantId, String strategyId, String logbookId)
        throws StorageNotFoundException;


    /**
     * Delete a logbook
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param logbookId  id of the logbook to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the logbook does not exist
     */
    void deleteLogbook(String tenantId, String strategyId, String logbookId) throws StorageNotFoundException;


    /**
     * Get Container Units Information
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about units of the requested container
     * @throws StorageNotFoundException Thrown if the Container does not exist
     */
    JsonNode getContainerUnits(String tenantId, String strategyId) throws StorageNotFoundException;


    /**
     * Get a specific Unit as a JsonNode
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param unitId     id of the unit
     * @return a unit as a JsonNode
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     */
    JsonNode getContainerUnit(String tenantId, String strategyId, String unitId) throws StorageNotFoundException;

    /**
     * Delete an unit
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @param unitId     id of the Unit to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the Unit does not exist
     */
    void deleteUnit(String tenantId, String strategyId, String unitId)
        throws StorageNotFoundException;


    /**
     * Get Container ObjectGroups Information
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId   id of the tenant
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about objectGroups of the requested container
     * @throws StorageNotFoundException Thrown if the Container does not exist
     */
    JsonNode getContainerObjectGroups(String tenantId, String strategyId)
        throws StorageNotFoundException;


    /**
     * Get a specific ObjectGroup as a JsonNode
     * <p>
     * TODO : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
     *
     * @param tenantId      id of the tenant
     * @param strategyId    id of the strategy
     * @param objectGroupId id of the ObjectGroup
     * @return an objectGroup as a JsonNode
     * @throws StorageNotFoundException Thrown if the Container or the object does not exist
     */
    JsonNode getContainerObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException;

    /**
     * Delete an ObjectGroup
     *
     * @param tenantId      id of the tenant
     * @param strategyId    id of the strategy
     * @param objectGroupId id of the ObjectGroup to be deleted
     * @throws StorageNotFoundException Thrown in case the Container or the ObjectGroup does not exist
     */
    void deleteObjectGroup(String tenantId, String strategyId, String objectGroupId)
        throws StorageNotFoundException;


    /**
     * Get the status from the service
     *
     * @return the status as a JsonNode
     * @throws StorageException if the Server got an internal error
     */
    JsonNode status() throws StorageException;

}
