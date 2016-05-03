package fr.gouv.vitam.workspace.api;

import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

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
	 * @throws ContentAddressableStorageException
	 *             Thrown when create action failed due some other failure
	 */
	public void createContainer(String containerName) throws ContentAddressableStorageException;

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
	 * @throws ContentAddressableStorageException
	 *             Thrown when purge action failed due some other failure
	 */

	public void purgeContainer(String containerName) throws ContentAddressableStorageException;

	/**
	 * Deletes a container if it is empty.
	 * 
	 * @param containerName
	 *            name of the container to delete
	 * 
	 * @throws ContentAddressableStorageNotFoundException
	 *             Thrown when the container cannot be located.
	 * @throws ContentAddressableStorageException
	 *             Thrown when delete action failed due some other failure
	 */
	public void deleteContainer(String containerName) throws ContentAddressableStorageException;

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
	 * @throws ContentAddressableStorageException
	 *             Thrown when delete action failed due some other failure
	 */
	public void deleteContainer(String containerName, boolean recursive) throws ContentAddressableStorageException;

	/**
	 * Determines if a container exists
	 * 
	 * @param containerName
	 *            name of container
	 *
	 * @throws ContentAddressableStorageException
	 */
	public boolean containerExists(String containerName) throws ContentAddressableStorageException;

	// Object

	/**
	 * Adds an object representing the data at location containerName/objectName
	 * 
	 * @param containerName
	 *            container to place the object.
	 * @param ObjectName
	 *            fully qualified object name relative to the container.
	 * @param bytes
	 *            the data
	 * 
	 * @throws ContentAddressableStorageNotFoundException
	 *             Thrown when the container cannot be located.
	 * @throws ContentAddressableStorageException
	 *             Thrown when put action failed due some other failure
	 */
	public void putObject(String containerName, String objectName, byte[] bytes)
			throws ContentAddressableStorageException;

	/**
	 * Retrieves an object representing the data at location
	 * containerName/ObjectName
	 * 
	 * @param containerName
	 *            container where this exists.
	 * @param ObjectName
	 *            fully qualified name relative to the container.
	 * @return the object you intended to receive or empty array, if it doesn't
	 *         exist.
	 * 
	 * @throws ContentAddressableStorageNotFoundException
	 *             Thrown when the container cannot be located.
	 * @throws ContentAddressableStorageException
	 *             Thrown when get action failed due some other failure
	 */
	public byte[] getObject(String containerName, String objectName) throws ContentAddressableStorageException;

	/**
	 * Deletes a object representing the data at location
	 * containerName/ObjectName
	 * 
	 * @param containerName
	 *            container where this exists.
	 * @param ObjectName
	 *            fully qualified name relative to the container.
	 * 
	 * @throws ContentAddressableStorageNotFoundException
	 *             Thrown when the container cannot be located or the blob
	 *             cannot be located in the container.
	 * @throws ContentAddressableStorageException
	 *             Thrown when delete action failed due some other failure
	 */

	public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException;

	/**
	 * Determines if an object exists
	 * 
	 * @param containerName
	 *            container where the object resides
	 * @param ObjectName
	 *            full path to the object
	 * 
	 * @throws ContentAddressableStorageNotFoundException
	 *             Thrown when the container cannot be located.
	 * @throws ContentAddressableStorageException
	 *             Thrown when delete action failed due some other failure
	 */

	public boolean objectExists(String containerName, String objectName) throws ContentAddressableStorageException;

}
