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
package fr.gouv.vitam.storage.offers.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Default offer service interface define offer methods
 */
public interface DefaultOfferService {
    String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    /**
     * Get offer storage digest of object
     *
     * @param containerName the container with the object
     * @param objectId the object name / id
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
     * @param objectId the object id
     * @return the object included in a response
     * @throws ContentAddressableStorageNotFoundException thrown when object does not exists
     * @throws ContentAddressableStorageException thrown when a server error occurs
     */
    ObjectContent getObject(String containerName, String objectId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Create access request (asynchronous read from tape to local FS) for the given @containerName and objects list.
     * Return access request id
     *
     * @param containerName the container containing the object
     * @param objectIds the objects ids
     * @return acess request id
     * @throws ContentAddressableStorageNotFoundException thrown when object does not exists
     * @throws ContentAddressableStorageException thrown when a server error occurs
     */
    String createAccessRequest(String containerName, List<String> objectIds) throws ContentAddressableStorageException;

    Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestId,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException;

    void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException;

    boolean checkObjectAvailability(String containerName, List<String> objectsIds)
        throws ContentAddressableStorageException;

    /**
     * Create object on container with objectId Receive object part of object. Actually these parts <b>HAVE TO</b> be
     * send in the great order.
     *
     * @param containerName the container name
     * @param objectId the offer objectId to create
     * @param objectPart the part of the object to create (chunk style)
     * @param type the object type to create
     * @param size inputstream size
     * @param digestType digest of object
     * @return the digest of the complete file or the digest of the chunk
     * @throws ContentAddressableStorageException if the container does not exist
     */
    String createObject(
        String containerName,
        String objectId,
        InputStream objectPart,
        DataCategory type,
        long size,
        DigestType digestType
    ) throws ContentAddressableStorageException;

    StorageBulkPutResult bulkPutObjects(
        String containerName,
        List<String> objectIds,
        MultiplexedStreamReader multiplexedStreamReader,
        DataCategory type,
        DigestType digestType
    ) throws ContentAddressableStorageException, IOException;

    /**
     * Check if object exists
     *
     * @param containerName the container suppose to contain the object
     * @param objectId the objectId to check
     * @return true if object exists, false otherwise
     * @throws ContentAddressableStorageServerException
     */
    boolean isObjectExist(String containerName, String objectId) throws ContentAddressableStorageException;

    /**
     * Get container capacity
     *
     * @param containerName the container name
     * @return Json with usableSpace information
     * @throws ContentAddressableStorageNotFoundException thrown if the container does not exist
     * @throws ContentAddressableStorageServerException
     */
    ContainerInformation getCapacity(String containerName) throws ContentAddressableStorageException;

    /**
     * Deletes a object representing the data at location containerName/objectName
     *
     * @param containerName container where this exists.
     * @param objectId the objectId to delete
     * @param type the object type to delete
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located or the blob cannot
     * be located in the container.
     * @throws ContentAddressableStorageException Thrown when delete action failed due some other failure
     */
    void deleteObject(String containerName, String objectId, DataCategory type)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Get Metadata
     *
     * @param containerName
     * @param objectId
     * @param noCache
     * @return StorageMetadataResult
     * @throws ContentAddressableStorageException
     * @throws IOException
     */
    StorageMetadataResult getMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException;

    /**
     * Get the offer log of objects created in offer container
     *
     * @param containerName container the container name
     * @param offset the offset of the object before the wanted list
     * @param limit number of objects wanted
     * @param order order of search
     * @return list of object informations
     * @throws ContentAddressableStorageDatabaseException Database error
     * @throws ContentAddressableStorageServerException Parsing error
     */
    List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageException;

    /**
     * List container objects
     *
     * @param containerName the container name
     * @param objectListingListener a listener to which are reported found object entries
     * @throws IOException
     */
    void listObjects(String containerName, ObjectListingListener objectListingListener)
        throws IOException, ContentAddressableStorageException;

    void compactOfferLogs() throws Exception;

    StorageBulkMetadataResult getBulkMetadata(String containerName, List<String> objectIds, Boolean noCache)
        throws ContentAddressableStorageException;
}
