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
package fr.gouv.vitam.common.storage.filesystem.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.storage.constants.ExtendedAttributes;
import fr.gouv.vitam.common.storage.filesystem.v2.metadata.container.HashContainerMetadata;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileSystem implements a Content Addressable Storage that stores objects on the file system with a hierarchical vision
 */
public class HashFileSystem extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashFileSystem.class);
    private final String storagePath;
    private HashFileSystemHelper fsHelper;
    // It is not needed to have a concurrent structure (eg: ConcurrentHashMap) as long as the put is only in the
    // unmarshall (which is static synchronized)
    private static Map<String, HashContainerMetadata> containerMetadata = new HashMap<>();


    /**
     * @param configuration
     */
    public HashFileSystem(StorageConfiguration configuration) {
        ParametersChecker.checkParameter("Storage configuration can't be null", configuration);
        ParametersChecker.checkParameter("StoragePath can't be null", configuration.getStoragePath());
        storagePath = configuration.getStoragePath();
        fsHelper = new HashFileSystemHelper(storagePath);
        File f = new File(storagePath);
        if (!f.exists()) {
            throw new IllegalArgumentException("The storage path doesn't exist");
        } else if (!f.isDirectory()) {
            throw new IllegalArgumentException("The storage path is not a directory");
        }
        HashFileSystem.unmarshall(fsHelper);
    }

    // It must be synchronized to prevent simultaneous put on the HashMap
    private static synchronized void unmarshall(HashFileSystemHelper fsHelper) {
        if (containerMetadata.size() == 0) { // Prevent to redo the serialization more than once
            for (String containerName : fsHelper.getListContainers()) {
                containerMetadata.put(containerName, new HashContainerMetadata(containerName, fsHelper, true));
            }
        }
    }

    @Override
    public void createContainer(String containerName)
            throws ContentAddressableStorageServerException {
        synchronized (HashFileSystem.class) {
            ParametersChecker
                .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
            if (isExistingContainer(containerName)) {
                LOGGER.warn("Container " + containerName + " already exists");
                return;
            }
            fsHelper.createContainer(containerName);
            containerMetadata.put(containerName, new HashContainerMetadata(containerName, fsHelper));
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        return fsHelper.isContainer(containerName);
    }

    // FIXME : This method doesn't implement the contract of ContentAdressableStorage interface
    // On update, it rewrites the file and doesn't throw an ContentAddressableStorageAlreadyExistException
    // This was choosen to be coherent with existing Jclouds implementation of ContentAdressableStorage
    // This must be changed by verifying that where the call is done, it implements the contract
    @Override
    public void putObject(String containerName, String objectName, InputStream stream, DigestType digestType,
        Long size)
        throws ContentAddressableStorageException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path filePath = fsHelper.getPathObject(containerName, objectName);
        Path parentPath = filePath.getParent();
        // Create the chain of directories
        fsHelper.createDirectories(parentPath);
        long beforeSize = 0L;
        long beforeObj = 0L;
        // Update case
        if (filePath.toFile().exists()) {
            try {
                beforeObj = 1L;
                beforeSize = Files.size(filePath);
            } catch (IOException e) {
                LOGGER.warn("Can't calculate the real size of the file " + filePath, e);
            }
        }
        try {
            Digest digest = new Digest(digestType);
            InputStream digestInputStream = digest.getDigestInputStream(stream);

            // Create the file from the inputstream
            Files.copy(digestInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            String streamDigest = digest.digestHex();

            String computedDigest = super.computeObjectDigest(containerName, objectName, digestType);

            if (!streamDigest.equals(computedDigest)) {
                throw new ContentAddressableStorageException("Illegal state for container " + containerName +
                        " and  object " + objectName + ". Stream digest " + streamDigest
                        + " is not equal to computed digest " + computedDigest);
            }

            storeDigest(containerName, objectName, digestType, computedDigest);
            containerMetadata.get(containerName).updateAndMarshall(1L - beforeObj, Files.size(filePath) - beforeSize);
        } catch (FileAlreadyExistsException e) {
            throw new ContentAddressableStorageAlreadyExistException("File " + filePath.toString() + " already exists",
                e);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("I/O Error on writing file " + filePath.toString(), e);
        }
    }

    @Override
    public Response getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path filePath = fsHelper.getPathObject(containerName, objectName);
        if (!Files.isRegularFile(filePath)) {
            throw new ContentAddressableStorageNotFoundException(
                objectName + " in container " + containerName + " not found");
        }
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            return new AbstractMockClient.FakeInboundResponse(Status.OK, inputStream,
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                getXContentLengthHeader(filePath));
        } catch (IOException e) {
            throw new ContentAddressableStorageException(
                "I/O error on retrieving object " + objectName + " in the container " + containerName, e);
        }
    }

    // TODO : To be modified when there will be a real method in ContentAddressableStorageJcloudsAbstract
    // TODO P1 : asyncResponse not used !
    @Override
    public Response getObjectAsync(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        return getObject(containerName, objectName);
    }

    @Override
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path filePath = fsHelper.getPathObject(containerName, objectName);
        // Delete file
        try {
            long size = Files.size(filePath);
            Files.delete(filePath);
            containerMetadata.get(containerName).updateAndMarshall(-1L, -size);
        } catch (NoSuchFileException e) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND + objectName, e);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("I/O error on removing " + filePath, e);
        }

        // Delete parent directory if parents directories are empty
        try {
            filePath = filePath.getParent();
            while (!filePath.equals(fsHelper.getPathContainer(containerName))) {
                Files.delete(filePath);
                filePath = filePath.getParent();
            }
        } catch (DirectoryNotEmptyException e) {// NOSONAR : Do nothing it is the normal way to exit the loop

        } catch (IOException e) {
            LOGGER.warn("Impossible to remove directory" + filePath.toString(), e);
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path filePath = null;
        try {
            filePath = fsHelper.getPathObject(containerName, objectName);
        } catch (ContentAddressableStorageNotFoundException e) {// NOSONAR : not found => false
            return false;
        }
        return filePath.toFile().isFile();
    }

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(), algo);

        if (!isExistingObject(containerName, objectName)) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND + objectName);
        }

        // Calculate the digest via the common method
        String digest = super.computeObjectDigest(containerName, objectName, algo);

        // Update digest in XATTR if needed
        String digestFromMD = getObjectDigestFromMD(containerName, objectName, algo);
        if (digest != null && !digest.equals(digestFromMD)) {
            storeDigest(containerName, objectName, algo, digest);
        }

        // return the calculated digest
        return digest;
    }

    /**
     * @param containerName the container name
     * @param objectName the object name
     * @param algo the algo type
     * @return the digest
     * @throws ContentAddressableStorageException if workspace could not be reached
     */
    @VisibleForTesting
    public String getObjectDigestFromMD(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        Path filePath = fsHelper.getPathObject(containerName, objectName);

        // Retrieve Digest XATTR attribute
        String digestMetadata = null;
        try {
            digestMetadata = readExtendedMetadata(filePath, ExtendedAttributes.DIGEST.getKey());
        } catch (IOException e) {
            LOGGER.warn("Unable to retrieve DIGEST extended attribute for the object " + objectName +
                " in the container " + containerName, e);
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
                LOGGER.warn("DigestAlgorithm in the extended attribute of file " + containerName + "/" + objectName +
                    " is unknown : " + digestTokens[0], e);
            }
        }

        return digestFromMD;
    }

    private void storeDigest(String containerName, String objectName, DigestType algo, String digest)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        StringBuilder sb = new StringBuilder(algo.getName()).append(":").append(digest);
        try {
            writeExtendedMetadata(fsHelper.getPathObject(containerName, objectName),
                ExtendedAttributes.DIGEST.getKey(), sb.toString());
        } catch (IOException e) {
            LOGGER.warn("Unable to write DIGEST extended attribute for the object " + objectName +
                " in the container " + containerName, e);
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
        containerInformation.setUsedSpace(containerMetadata.get(containerName).getUsedBytes());
        return containerInformation;
    }

    // FIXME : Copié/collé du FS v1
    @Override
    public JsonNode getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        ObjectNode jsonNodeObjectInformation;
        Long size;
        File file = fsHelper.getPathObject(containerName, objectName).toFile();
        if (!file.exists()) {
            throw new ContentAddressableStorageNotFoundException(
                "object + " + objectName + " of container " + containerName + " not found");
        }
        try {
            size = Files.size(Paths.get(file.getPath()));
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException(
                "I/O Error on determining size of object " + objectName + " of container " + containerName, e);
        }
        jsonNodeObjectInformation = JsonHandler.createObjectNode();
        jsonNodeObjectInformation.put("size", size);
        jsonNodeObjectInformation.put("object_name", objectName);
        jsonNodeObjectInformation.put("container_name", containerName);
        return jsonNodeObjectInformation;
    }

    // FIXME : <Copié/collé du FS v1
    @Override
    public MetadatasObject getObjectMetadatas(String containerName, String objectId)
        throws ContentAddressableStorageException, IOException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName,
                objectId);
        MetadatasStorageObject result = new MetadatasStorageObject();
        try {
            File file = fsHelper.getPathObject(containerName, objectId).toFile();
            BasicFileAttributes basicAttribs = getFileAttributes(file);
            long size = Files.size(Paths.get(file.getPath()));
            if (objectId != null) {
                result.setObjectName(objectId);
                // TODO To be reviewed with the X-DIGEST-ALGORITHM parameter
                result.setDigest(
                    computeObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType()));
                result.setFileSize(size);
            } else {
                result.setObjectName(containerName);
                result.setDigest(null);
            }
            // TODO see how to retrieve metadatas
            result.setType(containerName.split("_")[1]);
            result.setFileOwner("Vitam_" + containerName.split("_")[0]);
            result.setLastAccessDate(basicAttribs.lastAccessTime().toString());
            result.setLastModifiedDate(basicAttribs.lastModifiedTime().toString());
        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
        return result;
    }

    @Override
    public VitamPageSet<? extends VitamStorageMetadata> listContainer(String containerName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path p = fsHelper.getPathContainer(containerName);
        HashFileListVisitor hfv = new HashFileListVisitor();
        try {
            fsHelper.walkFileTreeOrdered(p, hfv);
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return hfv.getPageSet();
    }

    @Override
    public VitamPageSet<? extends VitamStorageMetadata> listContainerNext(String containerName,
                                                                          String nextMarker)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Path p = fsHelper.getPathContainer(containerName);
        try {
            HashFileListVisitor hfv = new HashFileListVisitor(fsHelper.splitObjectId(nextMarker), nextMarker);
            fsHelper.walkFileTreeOrdered(p, hfv);
            return hfv.getPageSet();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new ContentAddressableStorageServerException(e);
        }

    }

    @Override
    public void close() {
        // Nothing to do
    }

    private MultivaluedHashMap<String, Object> getXContentLengthHeader(Path path) throws IOException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        List<Object> headersList = new ArrayList<>();
        headersList.add(String.valueOf(Files.size(path)));
        headers.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(), headersList);
        return headers;
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
            LOGGER.error("Extended attribute not supported. You should consider to use XFS filesystem.", e);
            throw new IOException("Extended attribute not supported. You should consider to use XFS filesystem.", e);
        }
    }

    @VisibleForTesting
    public String readExtendedMetadata(Path p, String name) throws IOException {
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
            LOGGER.error("Extended attribute not supported. You should consider to use XFS filesystem.", e);
            throw new IOException("Extended attribute not supported. You should consider to use XFS filesystem.", e);
        }
    }
}
