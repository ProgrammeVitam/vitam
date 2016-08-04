/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.offers.workspace.core;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Default offer service interface define offer methods
 */
public interface DefaultOfferService {

    /**
     * Get offer storage digest of object
     *
     * @param containerName the container with the object
     * @param objectId the object name / id
     * @param digestAlgorithm the digest algorithm
     * @return the offer computed digest
     * @throws ContentAddressableStorageException thrown on storage error
     */
    String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException;

    /**
     * Get object on offer
     *
     * @param id the object id
     * @return the object
     */
    InputStream getObject(String id);

    /**
     * Create container on offer
     *
     * @param containerName the container name to create
     * @param objectInit informations about object to create
     * @param objectGUID the object GUID to create
     * @return objectInit with the offer object id (needed for the create object operation)
     * @throws ContentAddressableStorageServerException thrown when a server error occurs
     * @throws ContentAddressableStorageAlreadyExistException thrown if the container to create already exists
     */
    ObjectInit createContainer(String containerName, ObjectInit objectInit, String objectGUID)
        throws ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException;

    /**
     * Create a folder on a container on the offer
     *
     * @param containerName the container to create folder
     * @param folderName the folder name to create
     * @throws ContentAddressableStorageServerException thrown when a server error occurs
     * @throws ContentAddressableStorageNotFoundException thorwn when container does not exists
     * @throws ContentAddressableStorageAlreadyExistException thrown if the folder in the container to create already
     * exists
     */
    void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException;

    /**
     * Create object on container with objectId
     * Receive object part of object. Actually these parts <b>HAVE TO</b> be send in the great order.
     * TODO: multithreading
     * TODO: add chunk number to be able to retry and check error
     * TODO: better chunk management
     *
     * @param containerName the container name
     * @param objectId the offer objectId to create
     * @param objectPart the part of the object to create (chunk style)
     * @param ending true if objectPart is the last part
     * @return the digest of the complete file or the digest of the chunk
     * 
     * @throws IOException if an IOException is encountered with files
     * @throws ContentAddressableStorageException if the container does not exist
     */
    String createObject(String containerName, String objectId, InputStream objectPart, boolean ending) throws
        IOException, ContentAddressableStorageException;

    /**
     * Check if object exists
     *
     * @param containerName the container suppose to contain the object
     * @param objectId the objectId to check
     * @return true if object exists, false otherwise
     */
    boolean isObjectExist(String containerName, String objectId);

    /**
     * Get container capacity
     *
     * @param containerName the container name
     * @return Json with usableSpace information
     * @throws ContentAddressableStorageNotFoundException thrown if the container does not exist
     */
    JsonNode getCapacity(String containerName) throws ContentAddressableStorageNotFoundException;
}
