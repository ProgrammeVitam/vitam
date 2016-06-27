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
package fr.gouv.vitam.workspace.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.ListContainerOptions;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.common.ErrorMessage;
import fr.gouv.vitam.workspace.common.UriUtils;
import fr.gouv.vitam.workspace.common.WorkspaceMessage;

/**
 * Abstract Content Addressable Storage
 */
public abstract class ContentAddressableStorageAbstract implements ContentAddressableStorage {


    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(ContentAddressableStorageAbstract.class);

    private final BlobStoreContext context;


    private static final String TMP_FOLDER = "/tmp/";
    private static final String EMPTY_STRING = "";
    private static final String DOT = ".";



    /**
     * creates a new ContentAddressableStorageImpl with a storage configuration param
     *
     * @param configuration
     */
    public ContentAddressableStorageAbstract(StorageConfiguration configuration) {
        context = getContext(configuration);
    }

    /**
     * enables the connection to a storage service with the param provided
     *
     * @param configuration
     * @return BlobStoreContext
     */
    public abstract BlobStoreContext getContext(StorageConfiguration configuration);

    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageAlreadyExistException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        try {
            if (!context.getBlobStore().createContainerInLocation(null, containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage() + containerName);
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage() + containerName);
            }

        } catch (final ContentAddressableStorageAlreadyExistException e) {
            // FIXME REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public void purgeContainer(String containerName) throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        try {
            final BlobStore blobStore = context.getBlobStore();
            if (!isExistingContainer(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            } else {
                blobStore.clearContainer(containerName);
            }

        } catch (final ContentAddressableStorageNotFoundException e) {
            // FIXME REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public void deleteContainer(String containerName) throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        deleteContainer(containerName, false);

    }

    @Override
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        try {
            final BlobStore blobStore = context.getBlobStore();

            if (!isExistingContainer(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            }
            if (recursive) {
                blobStore.deleteContainer(containerName);
            } else {
                blobStore.deleteContainerIfEmpty(containerName);
            }

        } catch (final ContentAddressableStorageNotFoundException e) {
            // FIXME REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }

    @Override
    public boolean isExistingContainer(String containerName) {
        try {
            return context.getBlobStore().containerExists(containerName);
        } finally {
            context.close();
        }
    }

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        try {

            final BlobStore blobStore = context.getBlobStore();
            if (!isExistingContainer(containerName)) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            }

            if (isExistingFolder(containerName, folderName)) {
                LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST + folderName);
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + folderName);
            }
            blobStore.createDirectory(containerName, folderName);

        } finally {
            context.close();
        }

    }

    @Override
    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        try {
            final BlobStore blobStore = context.getBlobStore();

            if (!isExistingFolder(containerName, folderName)) {
                LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            }
            // FIXME REVIEW should it be a check of emptyness?
            blobStore.deleteDirectory(containerName, folderName);

        } catch (final ContentAddressableStorageNotFoundException e) {
            throw e;
        } finally {
            context.close();
        }
    }

    @Override
    public boolean isExistingFolder(String containerName, String folderName) {
        try {
            final BlobStore blobStore = context.getBlobStore();
            return blobStore.containerExists(containerName) && blobStore.directoryExists(containerName, folderName);
        } finally {
            context.close();
        }
    }

    @Override
    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        try {
            final BlobStore blobStore = context.getBlobStore();

            if (isExistingObject(containerName, objectName)) {
                LOGGER.info(ErrorMessage.OBJECT_ALREADY_EXIST.getMessage() + objectName);
            }

            final Blob blob = blobStore.blobBuilder(objectName).payload(stream).build();
            blobStore.putBlob(containerName, blob);
        } catch (final ContainerNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(e);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } finally {
            context.close();
        }
    }

    @Override
    public InputStream getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        try {
            final BlobStore blobStore = context.getBlobStore();

            if (!isExistingObject(containerName, objectName)) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
            }

            final Blob blob = blobStore.getBlob(containerName, objectName);
            if (null != blob) {
                return blob.getPayload().openStream();
            }
        } catch (final ContainerNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(e);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } finally {
            context.close();
        }
        // FIXME REVIEW use from Commons SingletonUtils
        // FIXME REVIEW : The method return an empty InputStream if the object name doesn't exists or if the file is
        // empty. If the file doesn't exist, it must have a different return value (notfound)
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        try {
            final BlobStore blobStore = context.getBlobStore();

            if (!isExistingObject(containerName, objectName)) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
            }

            blobStore.removeBlob(containerName, objectName);
        } catch (final ContentAddressableStorageNotFoundException e) {
            // FIXME REVIEW You already log the same message before throwing the exception
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            context.close();
        }

    }
    
    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(),
            algo);
        try{
            InputStream stream = getObject(containerName,objectName);
            Digest digest= new Digest(algo);
            digest.update(stream);
            return digest.toString();
        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) {
        try {
            final BlobStore blobStore = context.getBlobStore();
            return blobStore.containerExists(containerName) && blobStore.blobExists(containerName, objectName);
        } finally {
            context.close();
        }
    }

    @Override
    public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        ParametersChecker.checkParameter(ErrorMessage.FOLDER_NOT_FOUND.getMessage(), folderName);
        List<URI> uriFolderListFromContainer;
        try {
            final BlobStore blobStore = context.getBlobStore();

            // It's like a filter
            final ListContainerOptions listContainerOptions = new ListContainerOptions();
            // List of all resources in a container recursively
            final PageSet<? extends StorageMetadata> blobStoreList =
                blobStore.list(containerName, listContainerOptions.inDirectory(folderName).recursive());

            uriFolderListFromContainer = new ArrayList<>();
            LOGGER.info(WorkspaceMessage.BEGINNING_GET_URI_LIST_OF_DIGITAL_OBJECT.getMessage());

            // TODO
            // Get the uri
            // Today the uri null so

            // The temporary solution is to concat the file path to the generated GUID

            // if (storageMetada.getUri()!=null) {
            // uriListFromContainer.add(storageMetada.getUri());
            // }


            for (final StorageMetadata storageMetada : blobStoreList) {
                // select BLOB only, not folder nor relative path
                if (storageMetada.getType().equals(StorageType.BLOB) && storageMetada.getName() != null &&
                    !storageMetada.getName().isEmpty()) {
                    // FIXME REVIEW the UriUtils does not what is specified: it only removes the first folder, not the
                    // extension and only for uri with "." in it
                    uriFolderListFromContainer.add(new URI(UriUtils.splitUri(storageMetada.getName())));
                }
            }
            LOGGER.info(WorkspaceMessage.ENDING_GET_URI_LIST_OF_DIGITAL_OBJECT.getMessage());
        } catch (final ContainerNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(e);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        } finally {
            context.close();
        }
        return uriFolderListFromContainer;
    }


    @Override
    public void unzipObject(String containerName, String folderName, InputStream inputStreamObject)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        LOGGER.info("init unzip method  ...");

        if (!isExistingContainer(containerName)) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }
        
        if (inputStreamObject == null) {
            throw new ContentAddressableStorageException(ErrorMessage.STREAM_IS_NULL.getMessage());
        }

        if (isExistingFolder(containerName, folderName)) {
            LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + ":folderName" + folderName);
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
        }
        LOGGER.info("create folder name " + folderName);
        
       createFolder(containerName, folderName);
       extractZippedInputStreamOnContainer(containerName, folderName, inputStreamObject);
    }

    /**
     * extract compressed SIP and push the objects on the SIP folder
     *
     * @param containerName: GUID
     * @param folderName: folder Name
     * @param inputStreamObject :compressed SIP stream
     * @throws ContentAddressableStorageException
     */
    private void extractZippedInputStreamOnContainer(String containerName, String folderName, InputStream inputStreamObject)
        throws ContentAddressableStorageException {

        try {
            ZipEntry zipEntry;
            final ZipInputStream zInputStream =
                new ZipInputStream(inputStreamObject);
            
            while ((zipEntry = zInputStream.getNextEntry()) != null) {

                LOGGER.info("containerName : " + containerName + "    / ZipEntryName : " + zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    continue;
                }

                final File tempFile = File.createTempFile(TMP_FOLDER, DOT + getExtensionFile(zipEntry.getName()));
                // tempFile will be deleted on exit
                tempFile.deleteOnExit();
                final FileOutputStream fileOutputStream = new FileOutputStream(tempFile);

                IOUtils.copy(zInputStream, fileOutputStream);
                // put object on container Guid
                
                final String[] fullPath = zipEntry.getName().split(File.separator, 2);
                putObject(containerName, folderName+File.separator+fullPath[1], new FileInputStream(tempFile));
                
                // close outpuStram
                fileOutputStream.close();
                // exit && delete tempFile
                tempFile.exists();
            }
            zInputStream.close();

        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageException(e);
        }

    }


    private String getExtensionFile(String name) {

        if (!Strings.isNullOrEmpty(name)) {
            return FilenameUtils.getExtension(name);
        }
        return EMPTY_STRING;
    }

}

