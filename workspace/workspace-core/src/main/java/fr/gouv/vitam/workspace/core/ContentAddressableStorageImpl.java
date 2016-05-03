package fr.gouv.vitam.workspace.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;

/**
 * ContentAddressableStorageImpl implements a Content Addressable Storage
 */
public abstract class ContentAddressableStorageImpl implements ContentAddressableStorage {

	private static final Logger LOGGER = Logger.getLogger(ContentAddressableStorageImpl.class);

	private static final String CONTAINER_ALREADY_EXIST = "Container already exist ";
	private static final String CONTAINER_NOT_FOUND = "Container not found ";
	private static final String OBJECT_ALREADY_EXIST = "Object already exist ";
	private static final String OBJECT_NOT_FOUND = "Object not found ";
	private static final String CONTAINER_NAME_IS_A_MANDATORY_PARAMETER = "Container name is a mandatory parameter";
	private static final String OBJECT_NAME_IS_A_MANDATORY_PARAMETER = "Object name is a mandatory parameter";

	private BlobStoreContext context;

	public ContentAddressableStorageImpl(StorageConfiguration configuration) {
		super();
		context = getContext(configuration);
	}

	public abstract BlobStoreContext getContext(StorageConfiguration configuration);

	@Override
	public void createContainer(String containerName) throws ContentAddressableStorageException {

		checkContainerNameParam(containerName);
		try {
			if (!context.getBlobStore().createContainerInLocation(null, containerName)) {
				LOGGER.error(CONTAINER_ALREADY_EXIST + containerName);
				throw new ContentAddressableStorageAlreadyExistException(CONTAINER_ALREADY_EXIST + containerName);
			}

		} catch (ContentAddressableStorageAlreadyExistException e) {
			LOGGER.error(e.getMessage());
			throw e;
		} finally {
			context.close();
		}

	}

	@Override
	public void purgeContainer(String containerName) throws ContentAddressableStorageException {
		checkContainerNameParam(containerName);
		try {
			BlobStore blobStore = context.getBlobStore();

			if (!containerExists(containerName)) {
				LOGGER.error(CONTAINER_NOT_FOUND + containerName);
				throw new ContentAddressableStorageNotFoundException(CONTAINER_NOT_FOUND + containerName);
			} else {
				blobStore.clearContainer(containerName);
			}

		} catch (ContentAddressableStorageNotFoundException e) {
			LOGGER.error(e.getMessage());
			throw e;
		} finally {
			context.close();
		}

	}

	@Override
	public void deleteContainer(String containerName) throws ContentAddressableStorageException {
		checkContainerNameParam(containerName);
		deleteContainer(containerName, false);

	}

	@Override
	public void deleteContainer(String containerName, boolean recursive) throws ContentAddressableStorageException {
		try {
			BlobStore blobStore = context.getBlobStore();

			if (!containerExists(containerName)) {
				LOGGER.error(CONTAINER_NOT_FOUND + containerName);
				throw new ContentAddressableStorageNotFoundException(CONTAINER_NOT_FOUND + containerName);
			}

			if (recursive) {
				blobStore.deleteContainer(containerName);
			} else {
				blobStore.deleteContainerIfEmpty(containerName);
			}

		} catch (ContentAddressableStorageNotFoundException e) {
			LOGGER.error(e.getMessage());
			throw e;
		} finally {
			context.close();
		}

	}

	@Override
	public boolean containerExists(String containerName) {
		try {
			return context.getBlobStore().containerExists(containerName);
		} finally {
			context.close();
		}
	}

	@Override
	public void putObject(String containerName, String objectName, byte[] bytes)
			throws ContentAddressableStorageException {
		checkObjectNameParam(containerName, objectName);
		try (InputStream stream = new ByteArrayInputStream(bytes)) {
			BlobStore blobStore = context.getBlobStore();

			if (objectExists(containerName, objectName)) {
				LOGGER.info(OBJECT_ALREADY_EXIST + objectName);
			}

			Blob blob = blobStore.blobBuilder(objectName).payload(stream).build();
			blobStore.putBlob(containerName, blob);
		} catch (ContainerNotFoundException e) {
			LOGGER.error(CONTAINER_NOT_FOUND + containerName);
			throw new ContentAddressableStorageNotFoundException(e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new ContentAddressableStorageException(e);
		} finally {
			context.close();
		}
	}

	@Override
	public byte[] getObject(String containerName, String objectName) throws ContentAddressableStorageException {
		checkObjectNameParam(containerName, objectName);
		try {
			BlobStore blobStore = context.getBlobStore();
			Blob blob = blobStore.getBlob(containerName, objectName);
			if (null != blob) {
				try (InputStream stream = blob.getPayload().openStream()) {
					return ByteStreams.toByteArray(stream);
				}
			}
		} catch (ContainerNotFoundException e) {
			LOGGER.error(CONTAINER_NOT_FOUND + containerName);
			throw new ContentAddressableStorageNotFoundException(e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new ContentAddressableStorageException(e);
		} finally {
			context.close();
		}
		return new byte[0];
	}

	@Override
	public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException {
		checkObjectNameParam(containerName, objectName);
		try {
			BlobStore blobStore = context.getBlobStore();

			if (!objectExists(containerName, objectName)) {
				LOGGER.error(OBJECT_NOT_FOUND + objectName);
				throw new ContentAddressableStorageNotFoundException(OBJECT_NOT_FOUND + objectName);
			}

			blobStore.removeBlob(containerName, objectName);
		} catch (ContentAddressableStorageNotFoundException e) {
			LOGGER.error(e.getMessage());
			throw e;
		} finally {
			context.close();
		}

	}

	@Override
	public boolean objectExists(String containerName, String objectName) {
		try {
			BlobStore blobStore = context.getBlobStore();
			return blobStore.containerExists(containerName) && blobStore.blobExists(containerName, objectName);
		} finally {
			context.close();
		}
	}

	public void checkContainerNameParam(String containerName) {
		if (Strings.isNullOrEmpty(containerName)) {
			LOGGER.error(CONTAINER_NAME_IS_A_MANDATORY_PARAMETER);
			throw new IllegalArgumentException(CONTAINER_NAME_IS_A_MANDATORY_PARAMETER);
		}
	}

	public void checkObjectNameParam(String containerName, String objectName) {
		checkContainerNameParam(containerName);
		if (Strings.isNullOrEmpty(objectName)) {
			LOGGER.error(OBJECT_NAME_IS_A_MANDATORY_PARAMETER);
			throw new IllegalArgumentException(OBJECT_NAME_IS_A_MANDATORY_PARAMETER);
		}
	}

}
