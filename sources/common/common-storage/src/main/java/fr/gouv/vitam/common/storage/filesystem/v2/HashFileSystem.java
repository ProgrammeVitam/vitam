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
package fr.gouv.vitam.common.storage.filesystem.v2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.storage.constants.ExtendedAttributes;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.concurrent.TimeUnit;

/**
 * FileSystem implements a Content Addressable Storage that stores objects on the file system with a hierarchical vision
 */
public class HashFileSystem extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashFileSystem.class);
    private static final String ERROR_MSG_NOT_SUPPORTED =
        "Extended attribute not supported. You should consider to use XFS filesystem.";
    private final HashFileSystemHelper fsHelper;

    /**
     * @param configuration
     */
    public HashFileSystem(StorageConfiguration configuration) {
        super(configuration);
        ParametersChecker.checkParameter("Storage configuration can't be null", configuration);
        ParametersChecker.checkParameter("StoragePath can't be null", configuration.getStoragePath());
        final String storagePath = configuration.getStoragePath();
        fsHelper = new HashFileSystemHelper(storagePath);
        File f = new File(storagePath);
        if (!f.exists()) {
            throw new IllegalArgumentException("The storage path doesn't exist");
        } else if (!f.isDirectory()) {
            throw new IllegalArgumentException("The storage path is not a directory");
        }
    }

    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageServerException {
        synchronized (HashFileSystem.class) {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            if (isExistingContainer(containerName)) {
                LOGGER.warn("Container " + containerName + " already exists");
                return;
            }
            fsHelper.createContainer(containerName);
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );

        if (super.isExistingContainerInCache(containerName)) {
            return true;
        }

        boolean exists = fsHelper.isContainer(containerName);
        cacheExistsContainer(containerName, exists);
        return exists;
    }

    // FIXME : This method doesn't implement the contract of ContentAdressableStorage interface
    // On update, it rewrites the file and doesn't throw an ContentAddressableStorageAlreadyExistException
    // This was chosen to be coherent with existing Jclouds implementation of ContentAddressableStorage
    // This must be changed by verifying that where the call is done, it implements the contract
    @Override
    public void writeObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        Path filePath = fsHelper.getPathObject(containerName, objectName);

        Path parentPath = filePath.getParent();
        // Create the chain of directories
        fsHelper.createDirectories(parentPath);
        try {
            // Create the file from the InputStream
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException e) {
            throw new ContentAddressableStorageAlreadyExistException("File " + filePath + " already exists", e);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("I/O Error on writing file " + filePath, e);
        }
    }

    @Override
    public void checkObjectDigestAndStoreDigest(
        String containerName,
        String objectName,
        String objectDigest,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        String computedDigest = computeObjectDigest(containerName, objectName, digestType);
        if (!objectDigest.equals(computedDigest)) {
            throw new ContentAddressableStorageException(
                "Illegal state for container " +
                containerName +
                " and  object " +
                objectName +
                ". Stream digest " +
                objectDigest +
                " is not equal to computed digest " +
                computedDigest
            );
        }

        storeDigest(containerName, objectName, digestType, objectDigest);
    }

    @Override
    public ObjectContent getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        Path filePath = fsHelper.getPathObject(containerName, objectName);
        if (!filePath.toFile().isFile()) {
            throw new ContentAddressableStorageNotFoundException(
                objectName + " in container " + containerName + " not found"
            );
        }
        try {
            long size = Files.size(filePath);
            InputStream inputStream = Files.newInputStream(filePath);
            return new ObjectContent(inputStream, size);
        } catch (IOException e) {
            throw new ContentAddressableStorageException(
                "I/O error on retrieving object " + objectName + " in the container " + containerName,
                e
            );
        }
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        Path filePath = fsHelper.getPathObject(containerName, objectName);
        // Delete file
        try {
            Files.delete(filePath);
        } catch (NoSuchFileException e) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND + objectName, e);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("I/O error on removing " + filePath, e);
        }

        // Delete parent directory if parents directories are empty
        try {
            Path containerPath = fsHelper.getPathContainer(containerName);
            filePath = filePath.getParent();
            while (!filePath.equals(containerPath)) {
                Files.delete(filePath);
                filePath = filePath.getParent();
            }
        } catch (DirectoryNotEmptyException e) {} catch (IOException e) { // NOSONAR : Do nothing it is the normal way to exit the loop
            LOGGER.warn("Impossible to remove directory" + filePath, e);
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        Path filePath;
        try {
            filePath = fsHelper.getPathObject(containerName, objectName);
        } catch (ContentAddressableStorageNotFoundException e) { // NOSONAR : not found => false
            return false;
        }
        return filePath.toFile().isFile();
    }

    @Override
    public String getObjectDigest(String containerName, String objectName, DigestType algo, boolean noCache)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(), algo);

        // Get digest from XATTR
        String digestFromMD = getObjectDigestFromMD(containerName, objectName, algo);

        if (!noCache) {
            if (digestFromMD != null) {
                return digestFromMD;
            }

            LOGGER.warn(
                String.format(
                    "Could not retrieve cached digest for object '%s' in container '%s'. Recomputing digest",
                    objectName,
                    containerName
                )
            );
        }

        String digest = computeObjectDigest(containerName, objectName, algo);

        if (digestFromMD == null || !digestFromMD.equals(digest)) {
            storeDigest(containerName, objectName, algo, digest);
        }

        return digest;
    }

    /**
     * @param containerName the container name
     * @param objectName the object name
     * @param algo the algo type
     * @return the digest
     * @throws ContentAddressableStorageException if workspace could not be reached
     */
    String getObjectDigestFromMD(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Path filePath = fsHelper.getPathObject(containerName, objectName);

        // Retrieve Digest XATTR attribute
        String digestMetadata = null;
        try {
            digestMetadata = readExtendedMetadata(filePath, ExtendedAttributes.DIGEST.getKey());
        } catch (IOException e) {
            LOGGER.warn(
                "Unable to retrieve DIGEST extended attribute for the object " +
                objectName +
                " in the container " +
                containerName,
                e
            );
        }

        // See if the retrieved XATTR attribute is correct. If so, get it.
        String digestFromMD = null;
        if (digestMetadata != null) {
            String[] digestTokens = digestMetadata.split(":");
            try {
                if ((digestTokens.length == 2) && DigestType.fromValue(digestTokens[0]) == algo) {
                    digestFromMD = digestTokens[1];
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warn(
                    "DigestAlgorithm in the extended attribute of file " +
                    containerName +
                    "/" +
                    objectName +
                    " is unknown : " +
                    digestTokens[0],
                    e
                );
            }
        }

        PerformanceLogger.getInstance()
            .log(
                "STP_Offer_" + getConfiguration().getProvider(),
                containerName,
                "READ_DIGEST_FROM_XATTR",
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
        return digestFromMD;
    }

    private void storeDigest(String containerName, String objectName, DigestType algo, String digest)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        StringBuilder sb = new StringBuilder(algo.getName()).append(":").append(digest);
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            writeExtendedMetadata(
                fsHelper.getPathObject(containerName, objectName),
                ExtendedAttributes.DIGEST.getKey(),
                sb.toString()
            );
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + getConfiguration().getProvider(),
                    containerName,
                    "STORE_DIGEST_TO_XATTR",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                );
        } catch (IOException e) {
            LOGGER.warn(
                "Unable to write DIGEST extended attribute for the object " +
                objectName +
                " in the container " +
                containerName,
                e
            );
        }
    }

    // TODO : manage used information per container
    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        // A discuter : je l'ai enlevé car les autres implémentations considèrent que containerName peut être null
        // ParametersChecker.checkParameter(LOG_MESSAGE_CHECK_CONTAINER, containerName);
        if (containerName == null) {
            containerName = "";
        }
        if (!isExistingContainer(containerName)) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND + containerName);
        }
        File containerDir = fsHelper.getPathContainer(containerName).toFile();
        final long usableSpace = containerDir.getUsableSpace();
        final ContainerInformation containerInformation = new ContainerInformation();
        containerInformation.setUsableSpace(usableSpace);
        return containerInformation;
    }

    // FIXME : <Copié/collé du FS v1
    @Override
    public MetadatasObject getObjectMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectId
            );
            MetadatasStorageObject result = new MetadatasStorageObject();
            Path filePath = fsHelper.getPathObject(containerName, objectId);
            File file = filePath.toFile();
            BasicFileAttributes basicAttribs = getFileAttributes(file);
            long size = Files.size(Paths.get(file.getPath()));
            result.setObjectName(objectId);
            // TODO To be reviewed with the X-DIGEST-ALGORITHM parameter
            result.setDigest(
                getObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType(), noCache)
            );
            result.setFileSize(size);
            // TODO see how to retrieve metadatas
            result.setType(containerName.split("_")[1]);
            result.setLastAccessDate(basicAttribs.lastAccessTime().toString());
            result.setLastModifiedDate(basicAttribs.lastModifiedTime().toString());
            return result;
        } catch (FileNotFoundException | NoSuchFileException fnfe) {
            throw new ContentAddressableStorageNotFoundException(
                "Object " + objectId + "for container " + containerName + " is not found",
                fnfe
            );
        } catch (IOException io) {
            throw new ContentAddressableStorageException(
                "Object " + objectId + "for container " + containerName + " is not accessible",
                io
            );
        }
    }

    @Override
    public void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        if (!isExistingContainer(containerName)) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND + containerName);
        }
        Path path = fsHelper.getPathContainer(containerName);
        Files.walkFileTree(
            path,
            new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    objectListingListener.handleObjectEntry(
                        new ObjectEntry(file.getFileName().toString(), file.toFile().length())
                    );
                    return super.visitFile(file, attrs);
                }
            }
        );
    }

    @Override
    public void close() {
        // Nothing to do
    }

    private BasicFileAttributes getFileAttributes(File file) throws IOException {
        Path path = Paths.get(file.getPath());
        BasicFileAttributeView basicView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        return basicView.readAttributes();
    }

    private void writeExtendedMetadata(Path p, String name, String value) throws IOException {
        writeExtendedMetadata(Files.getFileAttributeView(p, UserDefinedFileAttributeView.class), name, value);
    }

    private void writeExtendedMetadata(UserDefinedFileAttributeView view, String name, String value)
        throws IOException {
        try {
            view.write(name, ByteBuffer.wrap(value.getBytes()));
        } catch (SecurityException | FileSystemException e) {
            LOGGER.error(ERROR_MSG_NOT_SUPPORTED, e);
            throw new IOException(ERROR_MSG_NOT_SUPPORTED, e);
        }
    }

    @VisibleForTesting
    String readExtendedMetadata(Path p, String name) throws IOException {
        return readExtendedMetadata(Files.getFileAttributeView(p, UserDefinedFileAttributeView.class), name);
    }

    private String readExtendedMetadata(UserDefinedFileAttributeView view, String name) throws IOException {
        try {
            ByteBuffer bb = ByteBuffer.allocate(view.size(name));
            view.read(name, bb);
            bb.flip();
            CharBuffer buffer = Charset.defaultCharset().decode(bb);
            return buffer.toString();
        } catch (IllegalArgumentException | FileSystemException e) {
            LOGGER.error(ERROR_MSG_NOT_SUPPORTED, e);
            throw new IOException(ERROR_MSG_NOT_SUPPORTED, e);
        }
    }
}
