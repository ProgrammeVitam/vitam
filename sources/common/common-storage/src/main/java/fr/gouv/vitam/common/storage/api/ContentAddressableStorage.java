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
package fr.gouv.vitam.common.storage.api;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;

/**
 * The ContentAddressableStorage interface.
 *
 */
public interface ContentAddressableStorage {
    // TODO P0 REVIEW should see null checking variable as IllegalArgumentException explicitly

    // Container
    /**
     * Creates a container
     *
     * @param containerName name of container to create
     *
     * @throws ContentAddressableStorageAlreadyExistException Thrown when creating a container while it (containerName)
     *         already exists
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    public void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException;

    /**
     * Deletes the contents of a container at its root path without deleting the container
     * <p>
     * Note: this function will delete everything inside a container recursively.
     * </p>
     *
     * @param containerName name of container to purge
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     */

    public void purgeContainer(String containerName) throws ContentAddressableStorageNotFoundException;


    /**
     * Deletes everything inside a container recursively.
     *
     * @param containerName name of the container to delete
     * @param recursive false : deletes a container if it is empty, true : deletes everything recursively
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Determines if a container exists
     *
     * @param containerName name of container
     * @return boolean type
     * @throws ContentAddressableStorageServerException
     */
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException;

    // folder (or directory)

    /**
     * Creates a folder (or a directory) marker depending on the service
     *
     * @param containerName container to create the directory in
     * @param folderName full path to the folder (or directory)
     * @throws ContentAddressableStorageAlreadyExistException Thrown when creating a directory while it already exists
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException;

    /**
     * Deletes a folder (or a directory) marker depending on the service
     *
     * @param containerName container to delete the folder from
     * @param folderName full path to the folder to delete
     * @throws ContentAddressableStorageNotFoundException Thrown when the directory cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Determines if a folder (or a directory) exists
     *
     * @param containerName container where the folder resides
     * @param folderName full path to the folder
     * @return boolean type
     * @throws ContentAddressableStorageServerException
     */
    boolean isExistingFolder(String containerName, String folderName) throws ContentAddressableStorageServerException;

    // Object

    /**
     * Adds an object representing the data at location containerName/objectName
     *
     * @param containerName container to place the object.
     * @param objectName fully qualified object name relative to the container.
     * @param stream the data
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageException;

    /**
     * Retrieves an object representing the data at location containerName/objectName
     * <p>
     * <b>WARNING</b> : use this method only if the response has to be consumed right away. If the response has to be
     * forwarded, you should use the method {@link #getObjectAsync(String, String, AsyncResponse) getObjectAsync}
     * instead
     * </p>
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @return the object you intended to receive
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    public Response getObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;


    // TODO P1 : getObjectAsync should replace getObject in the future. and getObject uses should be reviewed
    /**
     * Retrieves an object representing the data at location containerName/objectName
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @param asyncResponse the asyncResponse
     * @return the object you intended to receive
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    public Response getObjectAsync(String containerName, String objectName, AsyncResponse asyncResponse)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Deletes a object representing the data at location containerName/objectName
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located or the blob cannot
     *         be located in the container.
     * @throws ContentAddressableStorageException Thrown when delete action failed due some other failure
     */

    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Determines if an object exists
     *
     * @param containerName container where the object resides
     * @param objectName fully qualified name relative to the container.
     * @return boolean type
     * @throws ContentAddressableStorageServerException
     */

    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException;

    /**
     * Retrieves recursively the uri list of object inside a folder rootFolder/subfolder/
     *
     * @param containerName not null allowed container where this exists.
     * @param folderName not null allowed fully qualified folder name relative to the container.
     *
     * @return a list of URI
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */
    public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;


    /**
     * create container: will be identified by GUID and extract objects and push it on the container
     *
     * @param containerName : the container name (will be Guid created in ingest module)
     * @param folderName : the folder name
     * @param archiveType : the archive type (zip, tar, tar.gz, tar.bz2)
     * @param inputStreamObject : SIP input stream
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located
     * @throws ContentAddressableStorageAlreadyExistException Thrown when folder exists
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageCompressedFileException Thrown when the file is not a zip or an empty zip
     */
    public void uncompressObject(String containerName, String folderName, String archiveType,
        InputStream inputStreamObject)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageServerException, ContentAddressableStorageCompressedFileException,
        ContentAddressableStorageException;

    /**
     * compute Object Digest using a defined algorithm
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @param algo Digest algo
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container or the object cannot be located
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     * @return the digest object as String
     */
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ContentAddressableStorageException;

    /**
     * Get container information like capacity
     *
     * @param containerName the container name
     * @return container information like usableSpace and usedSpace
     * @throws ContentAddressableStorageNotFoundException thrown when storage is not available or container does not
     *         exist
     * @throws ContentAddressableStorageServerException
     */
    ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Retrieves information about an object at location containerName/objectName
     *
     * @param containerName container where the object is.
     * @param objectName fully qualified name relative to the container.
     * @return the object informations as a JsonNode object
     *
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */
    public JsonNode getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;


    /**
     * Check object
     * 
     * @param containerName the container name
     * @param objectId the objectId to check
     * @param digest the digest to be compared with
     * @param digestAlgorithm the digest Algorithm
     * @return true if the digest is correct
     * @throws ContentAddressableStorageException
     */
    boolean checkObject(String containerName, String objectId, String digest, DigestType digestAlgorithm)
        throws ContentAddressableStorageException;
    
}
