package fr.gouv.vitam.workspace.api;

import java.io.InputStream;

import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;

/**
 * The ContentAddressableStorage interface.
 *
 */
public interface ContentAddressableStorage {

    // Container
    /**
     * Creates a container
     * 
     * @param containerName
     *            name of container to create
     * 
     * @throws ContentAddressableStorageAlreadyExistException
     *             Thrown when creating a container while it (containerName)
     *             already exists
     */
    public void createContainer(String containerName) throws ContentAddressableStorageAlreadyExistException;

    /**
     * Deletes the contents of a container at its root path without deleting the
     * container
     * <p>
     * Note: this function will delete everything inside a container
     * recursively.
     * </p>
     * 
     * @param containerName
     *            name of container to purge
     * 
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     */

    public void purgeContainer(String containerName) throws ContentAddressableStorageNotFoundException;

    /**
     * Deletes a container if it is empty.
     * 
     * @param containerName
     *            name of the container to delete
     * 
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     */
    public void deleteContainer(String containerName) throws ContentAddressableStorageNotFoundException;

    /**
     * Deletes everything inside a container recursively.
     * 
     * @param containerName
     *            name of the container to delete
     * @param recursive
     *            false : deletes a container if it is empty, true : deletes
     *            everything recursively
     *
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     */
    public void deleteContainer(String containerName, boolean recursive) throws ContentAddressableStorageNotFoundException;
    /**
     * Determines if a container exists
     * 
     * @param containerName
     *            name of container
     */
    public boolean containerExists(String containerName);

    // FIXME
    /**
     * Lists all objects available to the full path.
     */
    // Set<? extends String> list(String fullPath)

    // folder (or directory)

    /**
     * Creates a folder (or a directory) marker depending on the service
     * 
     * @param containerName
     *            container to create the directory in
     * @param folderName
     *            full path to the folder (or directory)
     * @throws ContentAddressableStorageAlreadyExistException
     *             Thrown when creating a directory while it already exists
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     */
    void createFolder(String containerName, String folderName) throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException;

    /**
     * Deletes a folder (or a directory) marker depending on the service
     * 
     * @param containerName
     *            container to delete the folder from
     * @param folderName
     *            full path to the folder to delete
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the directory cannot be located.
     */
    void deleteFolder(String containerName, String folderName) throws ContentAddressableStorageNotFoundException;

    /**
     * Determines if a folder (or a directory) exists
     * 
     * @param containerName
     *            container where the folder resides
     * @param folderName
     *            full path to the folder
     */
    boolean folderExists(String containerName, String folderName);

    // Object

    /**
     * Adds an object representing the data at location containerName/objectName
     * 
     * @param containerName
     *            container to place the object.
     * @param objectName
     *            fully qualified object name relative to the container.
     * @param stream
     *            the data
     * 
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException
     *             Thrown when put action failed due some other failure
     */
    public void putObject(String containerName, String objectName, InputStream stream) throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException;

    /**
     * Retrieves an object representing the data at location
     * containerName/objectName
     * 
     * @param containerName
     *            container where this exists.
     * @param objectName
     *            fully qualified name relative to the container.
     * @return the object you intended to receive or empty array, if it doesn't
     *         exist.
     * 
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located.
     * @throws ContentAddressableStorageException
     *             Thrown when get action failed due some other failure
     */
    public InputStream getObject(String containerName, String objectName) throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Deletes a object representing the data at location
     * containerName/objectName
     * 
     * @param containerName
     *            container where this exists.
     * @param objectName
     *            fully qualified name relative to the container.
     * 
     * @throws ContentAddressableStorageNotFoundException
     *             Thrown when the container cannot be located or the blob
     *             cannot be located in the container.
     * @throws ContentAddressableStorageException
     *             Thrown when delete action failed due some other failure
     */

    public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException;

    /**
     * Determines if an object exists
     * 
     * @param containerName
     *            container where the object resides
     * @param objectName
     *            fully qualified name relative to the container.
     */

    public boolean objectExists(String containerName, String objectName);

}