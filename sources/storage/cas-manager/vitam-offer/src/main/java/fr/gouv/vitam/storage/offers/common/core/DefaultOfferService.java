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

package fr.gouv.vitam.storage.offers.common.core;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Default offer service interface define offer methods
 */
public interface DefaultOfferService {

    /**
     * Get offer storage digest of object
     *
     * @param containerName   the container with the object
     * @param objectId        the object name / id
     * @param digestAlgorithm the digest algorithm
     * @return the offer computed digest
     * @throws ContentAddressableStorageException thrown on storage error
     */
    @VisibleForTesting
    String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException;

    /**
     * Get object on offer as an inputStream
     *
     * @param containerName the container containing the object
     * @param objectId      the object id
     * @return the object included in a response
     * @throws ContentAddressableStorageNotFoundException thrown when object does not exists
     * @throws ContentAddressableStorageException         thrown when a server error occurs
     */
    Response getObject(String containerName, String objectId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Create object on container with objectId Receive object part of object. Actually these parts <b>HAVE TO</b> be
     * send in the great order.
     *
     * @param containerName the container name
     * @param objectId      the offer objectId to create
     * @param objectPart    the part of the object to create (chunk style)
     * @param type          the object type to create
     * @param size          inputstream size
     * @return the digest of the complete file or the digest of the chunk
     * @throws ContentAddressableStorageException if the container does not exist
     */
    String createObject(String containerName, String objectId, InputStream objectPart, DataCategory
        type, Long size, DigestType digestType) throws ContentAddressableStorageException;

    /**
     * Check if object exists
     *
     * @param containerName the container suppose to contain the object
     * @param objectId      the objectId to check
     * @return true if object exists, false otherwise
     * @throws ContentAddressableStorageServerException
     */
    boolean isObjectExist(String containerName, String objectId) throws ContentAddressableStorageServerException;

    /**
     * Get container capacity
     *
     * @param containerName the container name
     * @return Json with usableSpace information
     * @throws ContentAddressableStorageNotFoundException thrown if the container does not exist
     * @throws ContentAddressableStorageServerException
     */
    JsonNode getCapacity(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Check object
     *
     * @param containerName   the container name
     * @param objectId        the objectId to check
     * @param digest          the digest to be compared with
     * @param digestAlgorithm the digest Algorithm
     * @return true if the digest is correct
     * @throws ContentAddressableStorageException
     */
    boolean checkObject(String containerName, String objectId, String digest, DigestType digestAlgorithm)
        throws ContentAddressableStorageException;

    /**
     * Deletes a object representing the data at location containerName/objectName
     *
     * @param containerName   container where this exists.
     * @param objectId        the objectId to delete
     * @param digest          the digest to be compared with
     * @param digestAlgorithm the digest Algorithm
     * @param type            the object type to delete
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located or the blob cannot
     *                                                    be located in the container.
     * @throws ContentAddressableStorageException         Thrown when delete action failed due some other failure
     */
    void deleteObject(String containerName, String objectId, String digest, DigestType digestAlgorithm,
        DataCategory type)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Check digest (UNIMPLEMENTED)
     *
     * @param containerName the container name
     * @param idObject      the objectId to check
     * @param digest        the digest to be compared with
     * @return true if the digest is correct
     * @throws UnsupportedOperationException (UNIMPLEMENTED)
     */
    boolean checkDigest(String containerName, String idObject, String digest);

    /**
     * Check digest algorithm (UNIMPLEMENTED)
     *
     * @param containerName   the container name
     * @param idObject        the objectId to check
     * @param digestAlgorithm the digest Algorithm
     * @return true if the digest algorithm is correct
     * @throws UnsupportedOperationException (UNIMPLEMENTED)
     */
    boolean checkDigestAlgorithm(String containerName, String idObject, DigestType digestAlgorithm);

    /**
     * Get Metadata
     *
     * @param containerName
     * @param objectId
     * @return StorageMetadatasResult
     * @throws ContentAddressableStorageException
     * @throws IOException
     */
    StorageMetadatasResult getMetadatas(String containerName, String objectId)
        throws ContentAddressableStorageException, IOException;

    /**
     * Create a new cursor for listing container operation
     *
     * @param containerName the container name
     * @return the cursor ID value
     * @throws ContentAddressableStorageNotFoundException thrown when the container cannot be located
     * @throws ContentAddressableStorageServerException   thrown when delete action failed due some other failure
     */
    String createCursor(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Check if iterator have a next value
     *
     * @param containerName the container name
     * @param cursorId      the cursor ID
     * @return true if there is yet one or more value
     */
    boolean hasNext(String containerName, String cursorId);

    /**
     * Get next values
     *
     * @param containerName the container name
     * @param cursorId      the cursor ID
     * @return a list of next values
     * @throws ContentAddressableStorageNotFoundException thrown when the container cannot be located
     * @throws ContentAddressableStorageServerException
     */
    List<JsonNode> next(String containerName, String cursorId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Close the cursor
     *
     * @param containerName the container name
     * @param cursorId      the cursor ID
     */
    void finalizeCursor(String containerName, String cursorId);

    /**
     * Get the offer log of objects created in offer container
     *
     * @param containerName container the container name
     * @param offset        the offset of the object before the wanted list
     * @param limit         number of objects wanted
     * @param order         order of search
     * @return list of object informations
     * @throws ContentAddressableStorageDatabaseException Database error
     * @throws ContentAddressableStorageServerException   Parsing error
     */
    List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageDatabaseException, ContentAddressableStorageServerException;


}
