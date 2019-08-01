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
package fr.gouv.vitam.common.storage.cas.container.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
    void createContainer(String containerName)
        throws ContentAddressableStorageServerException;

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
     * @param stream the data
     * @param digestType parameter to compute an hash.
     * @param size size off the input stream
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    String putObject(String containerName, String objectName, InputStream stream,
        DigestType digestType, Long size)
        throws ContentAddressableStorageException;

    /**
     * Retrieves an object representing the data at location containerName/objectName
     * <p>
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @return the object you intended to receive
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    ObjectContent getObject(String containerName, String objectName)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Create read order (asynchronous read from tape to local FS) for the given objects representing the data at location containerName/objectId.
     * Return read order entity
     * <p>
     *
     * @param containerName container where this exists.
     * @param objectsIds list of the fully qualified name relative to the container.
     * @return read order request id
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    String createReadOrderRequest(String containerName, List<String> objectsIds)
            throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Purge all read request id to cleanup local FS
     * <p>
     *
     * @param readRequestID the read request ID.
     */
    void removeReadOrderRequest(String readRequestID)
            throws ContentAddressableStorageServerException;

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

    boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException;

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
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ContentAddressableStorageException;

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
        throws ContentAddressableStorageException, IOException;

    /**
     * List container (create cursor)
     *
     * @param containerName the container name
     * @return container listing
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    VitamPageSet<? extends VitamStorageMetadata> listContainer(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * List container (next on cursor)
     *
     * @param containerName the container name
     * @param nextMarker the last id of the list to get next
     * @return container listing
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    VitamPageSet<? extends VitamStorageMetadata> listContainerNext(String containerName, String nextMarker)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

}
