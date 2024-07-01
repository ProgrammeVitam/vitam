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
package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.api.model.TimeToLive;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface WorkspaceContentAddressableStorage {
    /**
     * Creates a container
     *
     * @param containerName name of container to create
     * @throws ContentAddressableStorageAlreadyExistException Thrown when creating a container while it (containerName)
     * already exists
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException;

    /**
     * Deletes the contents of a container at its root path without deleting the container
     * <p>
     * Note: this function will delete everything inside a container recursively.
     * </p>
     *
     * @param containerName name of container to purge
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void purgeContainer(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Deletes everything inside a container recursively.
     *
     * @param containerName name of the container to delete
     * @param recursive false : deletes a container if it is empty, true : deletes everything recursively
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     */
    void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Determines if a container exists
     *
     * @param containerName name of container
     * @return boolean type
     * @throws ContentAddressableStorageServerException
     */
    boolean isExistingContainer(String containerName);

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
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException;

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
     */
    boolean isExistingFolder(String containerName, String folderName);

    /**
     * Retrieves recursively the uri list of object inside a folder rootFolder/subfolder/
     *
     * @param containerName not null allowed container where this exists.
     * @param folderName not null allowed fully qualified folder name relative to the container.
     * @return a list of URI
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */
    List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageException;

    /**
     * create container: will be identified by GUID and extract objects and push it on the container
     *
     * @param containerName : the container name (will be Guid created in ingest module)
     * @param folderName : the folder name
     * @param archiveMimeType : the archive type (zip, tar, tar.gz, tar.bz2)
     * @param inputStreamObject : SIP input stream
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located
     * @throws ContentAddressableStorageAlreadyExistException Thrown when folder exists
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageCompressedFileException Thrown when the file is not a zip or an empty zip
     */
    void uncompressObject(
        String containerName,
        String folderName,
        String archiveMimeType,
        InputStream inputStreamObject
    ) throws ContentAddressableStorageException;

    /**
     * Adds an object representing the data at location containerName/objectName
     *
     * @param containerName container to place the object.
     * @param objectName fully qualified object name relative to the container.
     * @param stream the data
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageException;

    void putAtomicObject(String containerName, String objectName, InputStream stream, long size)
        throws ContentAddressableStorageException;

    /**
     * Retrieves an object representing the data at location
     * containerName/objectName
     * <p>
     * <b>WARNING</b> : use this method only if the response has to be consumed
     * right away.
     * </p>
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @return the object you intended to receive
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     * @throws ContentAddressableStorageAlreadyExistException Thrown when object creating exists
     */
    Response getObject(String containerName, String objectName, Long chunkOffset, Long maxChunkSize)
        throws ContentAddressableStorageException;

    /**
     * Deletes a object representing the data at location
     * containerName/objectName
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located or the blob
     * cannot be located in the container.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */

    void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException;

    /**
     * Determines if an object exists
     *
     * @param containerName container where the object resides
     * @param objectName fully qualified name relative to the container.
     * @return boolean type
     */

    boolean isExistingObject(String containerName, String objectName);

    /**
     * compute Object Digest using a defined algorithm
     *
     * @param containerName container where this exists.
     * @param objectName fully qualified name relative to the container.
     * @param algo Digest algo
     * @return the digest object as String
     * @throws ContentAddressableStorageNotFoundException Thrown when the container or the object cannot be located
     * @throws ContentAddressableStorageServerException Thrown when internal server error happens
     * @throws ContentAddressableStorageException Thrown when put action failed due some other failure
     */
    String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException;

    /**
     * Get container information like capacity
     *
     * @param containerName the container name
     * @return container information like usableSpace
     * @throws ContentAddressableStorageNotFoundException thrown when storage is not available or container does not
     * exist
     * @throws ContentAddressableStorageServerException
     */
    ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException;

    /**
     * Retrieves information about an object at location
     * containerName/objectName
     *
     * @param containerName container where the object is.
     * @param objectName fully qualified name relative to the container.
     * @return the object informations as a JsonNode object
     * @throws ContentAddressableStorageNotFoundException Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException Thrown when get action failed due some other failure
     */
    JsonNode getObjectInformation(String containerName, String objectName) throws ContentAddressableStorageException;

    void purgeOldFilesInContainer(String containerName, TimeToLive timeToLive)
        throws ContentAddressableStorageException;

    Map<String, FileParams> getFilesWithParamsFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageException;
}
