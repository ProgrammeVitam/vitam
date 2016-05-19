package fr.gouv.vitam.workspace.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;

import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.common.ErrorMessage;
import fr.gouv.vitam.workspace.common.ParametersChecker;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;


// TODO REVIEW Add licence file
// FIXME REVIEW In the interface, you have the throws exception in the prototype . Put it also in the Implementation Class
// FIXME REVIEW The name is not clean . It has Impl suffix but is the abstract class . The real implementation is FileSystem (which don't have Impl suffix). This has to be XxxxAbstract and provide a Factory to create the right implmentation one.
/**
 * ContentAddressableStorageImpl implements a Content Addressable Storage
 */
public abstract class ContentAddressableStorageImpl implements ContentAddressableStorage {

    private static final Logger LOGGER = Logger.getLogger(ContentAddressableStorageImpl.class);

    private BlobStoreContext context;

    // TODO REVIEW comment
    public ContentAddressableStorageImpl(StorageConfiguration configuration) {
        // TODO REVIEW super() Useless as it is implements an interface
        super();
        context = getContext(configuration);
    }

    public abstract BlobStoreContext getContext(StorageConfiguration configuration);

    @Override
    public void createContainer(String containerName) {

        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        try {
            if (!context.getBlobStore().createContainerInLocation(null, containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage() + containerName);
                throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage()  + containerName);
            }

        } catch (ContentAddressableStorageAlreadyExistException e) {
        // TODO REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public void purgeContainer(String containerName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        try {
            BlobStore blobStore = context.getBlobStore();
            if (!containerExists(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            } else {
                blobStore.clearContainer(containerName);
            }

        } catch (ContentAddressableStorageNotFoundException e) {
        // TODO REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public void deleteContainer(String containerName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        deleteContainer(containerName, false);

    }

    @Override
    public void deleteContainer(String containerName, boolean recursive) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        try {
            BlobStore blobStore = context.getBlobStore();

            if (!containerExists(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            }
            if (recursive) {
                blobStore.deleteContainer(containerName);
            } else {
                blobStore.deleteContainerIfEmpty(containerName);
            }

        } catch (ContentAddressableStorageNotFoundException e) {
        // TODO REVIEW You already log the same message before throwing the exception
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
    public void createFolder(String containerName, String folderName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, folderName);

        try {

            BlobStore blobStore = context.getBlobStore();
            if (!containerExists(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            }

            if (folderExists(containerName, folderName)) {
                LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST + folderName);
                throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + folderName);
            }

            blobStore.createDirectory(containerName, folderName);

        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageAlreadyExistException e) {
        // TODO REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public void deleteFolder(String containerName, String folderName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, folderName);
        try {
            BlobStore blobStore = context.getBlobStore();

            if (!folderExists(containerName, folderName)) {
                LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            }
            // FIXME REVIEW should it be a check of emptyness?
            blobStore.deleteDirectory(containerName, folderName);

        } catch (ContentAddressableStorageNotFoundException e) {
        // TODO REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }
    }

    @Override
    public boolean folderExists(String containerName, String folderName) {
        try {
            BlobStore blobStore = context.getBlobStore();
            return blobStore.containerExists(containerName) && blobStore.directoryExists(containerName, folderName);
        } finally {
            context.close();
        }
    }

    @Override
    public void putObject(String containerName, String objectName, InputStream stream) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        try {
            BlobStore blobStore = context.getBlobStore();

            if (objectExists(containerName, objectName)) {
                LOGGER.info(ErrorMessage.OBJECT_ALREADY_EXIST.getMessage() + objectName);
            }

            Blob blob = blobStore.blobBuilder(objectName).payload(stream).build();
            blobStore.putBlob(containerName, blob);
        } catch (ContainerNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } finally {
            context.close();
        }
    }

    @Override
    public InputStream getObject(String containerName, String objectName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        try {
            BlobStore blobStore = context.getBlobStore();
            Blob blob = blobStore.getBlob(containerName, objectName);
            if (null != blob) {
                return blob.getPayload().openStream();
            }
        } catch (ContainerNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } finally {
            context.close();
        }
        // FIXME REVIEW in Commons create an utility methods (among others) that returns a static final true NullInputStream (static singleton), not a like you did
        // FIXME REVIEW : The method return an empty InputStream if the object name doesn't exists or if the file is empty. If the file doesn't exist, it must have a different return value (notfound)
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void deleteObject(String containerName, String objectName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        try {
            BlobStore blobStore = context.getBlobStore();

            if (!objectExists(containerName, objectName)) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
            }

            blobStore.removeBlob(containerName, objectName);
        } catch (ContentAddressableStorageNotFoundException e) {
        // TODO REVIEW You already log the same message before throwing the exception
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
}
