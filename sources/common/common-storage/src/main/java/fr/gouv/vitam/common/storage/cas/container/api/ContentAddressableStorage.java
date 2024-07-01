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
package fr.gouv.vitam.common.storage.cas.container.api;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageUnavailableDataFromAsyncOfferException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The ContentAddressableStorage interface.
 */
public interface ContentAddressableStorage extends VitamAutoCloseable {
    /**
     * Creates a container
     *
     * @param containerName name of container to create
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void createContainer(String containerName) throws ContentAddressableStorageServerException;

    /**
     * Determines if a container exists
     *
     * @param containerName name of container
     * @return boolean type
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException;

    /**
     * Adds an object representing the data at location containerName/objectName
     *
     * @param containerName container to place the object.
     * @param objectName fully qualified object name relative to the container.
     * @param inputStream the data
     * @param digestType parameter to compute an hash.
     * @param size size off the input stream
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    void writeObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException;

    /**
     * Checks objet digest & update persist its digest in object metadata
     *
     * @param containerName container to place the object.
     * @param objectName fully qualified object name relative to the container.
     * @param objectDigest object digest value
     * @param digestType object digest type
     * @param size size off the input stream
     * @throws ContentAddressableStorageException
     */
    void checkObjectDigestAndStoreDigest(
        String containerName,
        String objectName,
        String objectDigest,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException;

    @VisibleForTesting
    default String putObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        Digest digest = new Digest(digestType);
        InputStream digestInputStream = digest.getDigestInputStream(inputStream);
        writeObject(containerName, objectName, digestInputStream, digestType, size);
        String objectDigest = digest.digestHex();
        checkObjectDigestAndStoreDigest(containerName, objectName, objectDigest, digestType, size);
        return objectDigest;
    }

    /**
     * Retrieves an object representing the data at location containerName/objectName
     * <p>
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @return the object you intended to receive
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageUnavailableDataFromAsyncOfferException Thrown when object cannot be read due to missing access request on AsyncRead ContentAddressableStorage
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */
    ObjectContent getObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException, ContentAddressableStorageUnavailableDataFromAsyncOfferException;

    /**
     * Create an access request for objects (asynchronous read from tape to local FS).
     * Return access request identifier
     *
     * @param containerName container where this exists.
     * @param objectNames list of objects names for which access is requested
     * @return access request identifier
     */
    default String createAccessRequest(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * Checks status of access requests by id.
     *
     * @param accessRequestIds the identifiers of the access requests to check
     * @param adminCrossTenantAccessRequestAllowed when {@code true}, access to access requests of other tenants is allowed from Admin tenant
     * @return {@code AccessRequestStatus} representing access request status
     */
    default Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * Delete access request.
     * Ignored if no access request found (not exists, expired or already canceled).
     *
     * @param accessRequestId the identifier of the access request to cancel.
     * @param adminCrossTenantAccessRequestAllowed when {@code true}, removing access requests of other tenants is allowed from Admin tenant
     */
    default void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * Check object availability for async offers.
     *
     * @param containerName container where this exists.
     * @param objectNames list of objects names for which availability is to be checked.
     * @return {@code true} if ALL objects are available, otherwise {@code false}.
     */
    default boolean checkObjectAvailability(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * Deletes a object representing the data at location containerName/objectName
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located or the blob cannot
     * be located in the container.
     * @throws ContentAddressableStorageException Thrown when delete action failed due some other failure
     */

    void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Determines if an object exists
     *
     * @param containerName container where the object resides
     * @param objectName fully qualified name relative to the container.
     * @return boolean type
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */

    boolean isExistingObject(String containerName, String objectName) throws ContentAddressableStorageException;

    /**
     * compute Object Digest using a defined algorithm
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @param algo Digest algo
     * @param noCache forces full digest computation
     * @return the digest object as String
     * @throws ContentAddressableStorageNotFoundException Thrown when the container or the object cannot be located
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     */
    String getObjectDigest(String containerName, String objectName, DigestType algo, boolean noCache)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException;

    /**
     * Get container information like capacity
     *
     * @param containerName the container name
     * @return container information like usableSpace
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * get metadata of the object
     *
     * @param containerName the container name
     * @param objectId the objectId to check
     * @return MetadatasObjectResult
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws IOException if an IOException is encountered with files
     * @throws IllegalArgumentException thrown when containerName or objectId is null
     */
    MetadatasObject getObjectMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException;

    /**
     * List container objects
     *
     * @param containerName the container name
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageException, IOException;
}
