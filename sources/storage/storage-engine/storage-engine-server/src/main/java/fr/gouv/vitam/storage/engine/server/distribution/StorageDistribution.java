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
package fr.gouv.vitam.storage.engine.server.distribution;

import java.io.InputStream;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * Interface Storage Distribution for Storage Operations
 */
public interface StorageDistribution extends VitamAutoCloseable {

    /**
     * Store data of any type for given tenant on storage offers associated to
     * given strategy
     *
     * @param strategyId
     *            id of the strategy
     * @param objectId
     *            the workspace URI of the data to be retrieve (and stored in
     *            offer)
     * @param createObjectDescription
     *            object additional informations
     * @param category
     *            the category of the data to store (unit, object...)
     * @param requester
     *            the requester information
     * @return a StoredInfoResult containing informations about the created Data
     * @throws StorageNotFoundException
     *             Thrown if the Container does not exist
     * @throws StorageTechnicalException
     *             Thrown in case of any technical problem
     * @throws StorageObjectAlreadyExistsException
     */
    // TODO P1 : maybe the logbook object should be an inputstream as well.
    // This would be an other US responsibility (not #72)
    StoredInfoResult storeData(String strategyId, String objectId, ObjectDescription createObjectDescription,
            DataCategory category, String requester) throws StorageAlreadyExistsException, StorageException;

    /**
     * Get Storage Information (availability and capacity) for the requested
     * tenant + strategy
     *
     * @param strategyId
     *            id of the strategy
     * @return a JsonNode containing informations about the storage
     * @throws StorageNotFoundException
     *             Thrown if the Container does not exist
     * @throws StorageTechnicalException
     *             Thrown in case of any technical problem
     */
    JsonNode getContainerInformation(String strategyId) throws StorageException;

    /**
     * Get Storage Container full content as an InputStream
     * <p>
     *
     * @param strategyId
     *            id of the strategy
     * @return the content of the container as an InputStream
     * @throws StorageNotFoundException
     *             Thrown if the Storage Container does not exist
     * @throws StorageTechnicalException
     *             Thrown if a technical exception is encountered
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to
    // review this code then
    InputStream getStorageContainer(String strategyId) throws StorageNotFoundException, StorageTechnicalException;

    /**
     * Create a container Architects are aware of this.
     *
     * @param strategyId
     *            id of the strategy
     * @return a JsonNode containing informations about the created Container
     * @throws StorageException
     *             Thrown in case the Container already exists
     */
    // TODO P1 : container creation possibility needs to be re-think then
    // deleted or implemented. Vitam
    JsonNode createContainer(String strategyId) throws StorageException;

    /**
     * Delete a container
     * <p>
     * aware of this.
     *
     * @param strategyId
     *            id of the strategy
     * @throws StorageTechnicalException
     *             Thrown in case of any technical problem
     * @throws StorageNotFoundException
     *             Thrown in case the Container does not exist
     */
    // TODO P1 : container deletion possibility needs to be re-think then
    // deleted or implemented. Vitam Architects are
    void deleteContainer(String strategyId) throws StorageTechnicalException, StorageNotFoundException;

    /**
     * List container objects
     *
     * @param strategyId
     *            the strategy id to get offers
     * @param category
     *            the object type to list
     * @param cursorId
     *            the cursorId if exists
     * @return a response with object listing
     * @throws StorageException
     *             thrown in case of any technical problem
     */
    RequestResponse<JsonNode> listContainerObjects(String strategyId, DataCategory category, String cursorId) throws StorageException;

    /**
     * Get a specific Object binary data as an input stream
     * <p>
     *
     * @param strategyId
     *            id of the strategy
     * @param objectId
     *            id of the object
     * @param category
     * @param asyncResponse
     *            asyncResponse
     * @return an object as a Response with an InputStream
     * @throws StorageNotFoundException
     *             Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException
     *             thrown if a technical error happened
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to
    // review this code then
    Response getContainerByCategory(String strategyId, String objectId, DataCategory category, AsyncResponse asyncResponse)
            throws StorageException;

    /**
     * Get a specific Object informations
     *
     * @param strategyId
     *            id of the strategy
     * @param objectId
     *            id of the object
     * @return JsonNode containing informations about the requested object
     * @throws StorageNotFoundException
     *             Thrown if the Container or the object does not exist
     */
    JsonNode getContainerObjectInformations(String strategyId, String objectId) throws StorageNotFoundException;

    /**
     * Delete an object
     *
     * @param strategyId
     *            id of the strategy
     * @param objectId
     *            id of the object to be deleted
     * @param digest
     *            the digest to be compared with
     * @param digestAlgorithm
     *            the digest Algorithm
     * @throws StorageNotFoundException
     *             Thrown if the Container or the object does not exist
     * @throws StorageTechnicalException
     *             thrown if a technical error happened
     */
    void deleteObject(String strategyId, String objectId, String digest, DigestType digestAlgorithm) throws StorageException;

    // TODO P2 see list/count/size API
    /**
     * Retrieve a list of logbook ids associated to a given tenant
     * <p>
     * @param strategyId
     *            id of the strategy
     * @return a JsonNode containing informations about logbooks of the
     *         requested container
     * @throws StorageNotFoundException
     *             Thrown if the Container does not exist
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to
    // review this code then
    JsonNode getContainerLogbooks(String strategyId) throws StorageNotFoundException;

    /**
     * Get a specific Logbook as a JsonNode
     * <p>
     *
     * @param strategyId
     *            id of the strategy
     * @param logbookId
     *            id of the logbook
     * @return a logbook as a JsonNode
     * @throws StorageNotFoundException
     *             Thrown if the Container or the object does not exist
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to
    // review this code then
    JsonNode getContainerLogbook(String strategyId, String logbookId) throws StorageNotFoundException;

    // FIXME P1 missing digest which is mandatory for a delete
    /**
     * Delete a logbook
     *
     * @param strategyId id of the strategy
     * @param logbookId id of the logbook to be deleted
     * @throws UnsupportedOperationException method not implemented yet
     */
    void deleteLogbook(String strategyId, String logbookId) throws UnsupportedOperationException;

    // TODO P2 see list/count/size API
    /**
     * Get Container Units Information
     * <p>
     *
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about units of the requested container
     * @throws UnsupportedOperationException method not implemented yet
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
    JsonNode getContainerUnits(String strategyId) throws UnsupportedOperationException;

    /**
     * Get a specific Unit as a JsonNode
     * <p>
     *
     * @param strategyId
     *            id of the strategy
     * @param unitId
     *            id of the unit
     * @return a unit as a JsonNode
     * @throws UnsupportedOperationException method not implemented yet
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
    JsonNode getContainerUnit(String strategyId, String unitId) throws UnsupportedOperationException;

    // FIXME P1 missing digest which is mandatory for a delete
    /**
     * Delete an unit
     *
     * @param strategyId id of the strategy
     * @param unitId id of the Unit to be deleted
     * @throws UnsupportedOperationException method not implemented yet
     */
    void deleteUnit(String strategyId, String unitId)
        throws UnsupportedOperationException;

    // TODO P2 see list/count/size API
    /**
     * Get Container ObjectGroups Information
     * <p>
     *
     * @param strategyId id of the strategy
     * @return a JsonNode containing informations about objectGroups of the requested container
     * @throws UnsupportedOperationException method not implemented yet
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
    JsonNode getContainerObjectGroups(String strategyId)
        throws UnsupportedOperationException;

    /**
     * Get a specific ObjectGroup as a JsonNode
     * <p>
     *
     * @param strategyId
     *            id of the strategy
     * @param objectGroupId
     *            id of the ObjectGroup
     * @return an objectGroup as a JsonNode
     * @throws UnsupportedOperationException method not implemented yet
     */
    // TODO P1 : "bonus" code, this is NOT to be handled in item #72. No need to review this code then
    JsonNode getContainerObjectGroup(String strategyId, String objectGroupId)
        throws UnsupportedOperationException;

    /**
     * Delete an ObjectGroup
     *
     * @param strategyId id of the strategy
     * @param objectGroupId id of the ObjectGroup to be deleted
     * @throws UnsupportedOperationException method not implemented yet
     */
    void deleteObjectGroup(String strategyId, String objectGroupId)
        throws UnsupportedOperationException;


    /**
     * Get the status from the service
     *
     * @return the status as a JsonNode
     * @throws UnsupportedOperationException method not implemented yet
     */
    JsonNode status() throws UnsupportedOperationException;

}
