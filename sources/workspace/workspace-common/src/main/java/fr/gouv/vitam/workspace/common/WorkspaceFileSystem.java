/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.compress.ArchiveEntryInputStream;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.stream.StreamUtils.closeSilently;

/**
 * Workspace Filesystem implementation
 */
public class WorkspaceFileSystem implements WorkspaceContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceFileSystem.class);

    private Path root;
    /**
     * To prevent multiple Path traversal attack check in inter-methods calls
     */
    private boolean isPathTraversalAlreadyChecked;

    /**
     * Default constructor Define the root of workspace with the storagePath property from configuration
     *
     * @param configuration the configuration, just StoragePath property is required
     * @throws IOException when error occurs to create root directory
     */
    public WorkspaceFileSystem(StorageConfiguration configuration) throws IOException {
        ParametersChecker.checkParameter("Workspace configuration cannot be null", configuration);
        ParametersChecker.checkParameter("Storage path configuration have to be define",
            configuration.getStoragePath());
        root = Paths.get(new File(configuration.getStoragePath()).getCanonicalPath());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
    }

    @Override
    public void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        if (!isExistingContainer(containerName)) {
            try {
                Path containerPath = getContainerPath(containerName, false);
                Files.createDirectories(containerPath);
            } catch (IOException ex) {
                throw new ContentAddressableStorageServerException(ex);
            }
        } else {
            // Retro-compatibility
            throw new ContentAddressableStorageAlreadyExistException("Container " + containerName + "already exists");
        }
    }

    @Override
    public void purgeContainer(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        if (!isExistingContainer(containerName)) {
            LOGGER.info(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        try {
            Path containerPath = getContainerPath(containerName, false);
            try (Stream<Path> streams = Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS)) {
                streams.filter(path -> !containerPath.equals(path))
                    .sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public void purgeOldFilesInContainer(String containerName, TimeToLive timeToLive)
        throws ContentAddressableStorageException {

        if (!isExistingContainer(containerName)) {
            LOGGER.info(ErrorMessage.CONTAINER_NOT_FOUND + containerName);
            return;
        }

        try {
            Path folderPath = getContainerPath(containerName, false);
            Instant expirationInstant = Instant.now().minus(timeToLive.getValue(), timeToLive.getUnit());

            try (Stream<Path> streams = Files.walk(folderPath, FileVisitOption.FOLLOW_LINKS)) {
                streams.filter(
                    path -> {
                        try {
                            if (Files.isDirectory(path)) {
                                return false;
                            }
                            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                            return expirationInstant.isAfter(lastModifiedTime.toInstant());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                ).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        LOGGER.warn("Could not delete file " + path, e);
                    }
                });
            }
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Path containerPath = null;
        try {
            containerPath = getContainerPath(containerName, true);
            if (!containerPath.toFile().exists()) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
                throw new ContentAddressableStorageNotFoundException(
                    ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            }
            if (recursive) {
                try (Stream<Path> streams = Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS)) {
                    streams.sorted(Comparator.reverseOrder())
                        .map(Path::toFile).forEach(File::delete);
                }
            } else {
                Files.delete(containerPath);
            }
        } catch (DirectoryNotEmptyException ex) {
            // TODO: keep this old none recursive delete workspace container style ?
            LOGGER.warn("Directory {} not empty, do nothing ", containerPath != null ? containerPath.toString() : "");
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        Path containerPath = null;
        try {
            containerPath = getContainerPath(containerName, true);
            return containerPath.toFile().exists() && containerPath.toFile().isDirectory();
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        if (isExistingFolder(containerName, folderName)) {
            LOGGER.info(ErrorMessage.FOLDER_ALREADY_EXIST + folderName);
            throw new ContentAddressableStorageAlreadyExistException(
                ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + folderName);
        }
        try {
            Path folderPath = getFolderPath(containerName, folderName, false);
            Files.createDirectories(folderPath);
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        if (!isExistingFolder(containerName, folderName)) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
        }
        Path folderPath = null;
        try {
            folderPath = getFolderPath(containerName, folderName, false);
            Files.delete(folderPath);
        } catch (DirectoryNotEmptyException ex) {
            LOGGER.warn("Directory {} not empty, then we delete files and folder",
                folderPath != null ? folderPath.toString() : "");
            try (Stream<Path> streams = Files.walk(folderPath, FileVisitOption.FOLLOW_LINKS)) {
                streams.sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);

            } catch (IOException exc) {
                throw new ContentAddressableStorageServerException(exc);
            }
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public boolean isExistingFolder(String containerName, String folderName) {
        try {
            Path folderPath = getObjectPath(containerName, folderName, true);
            return folderPath.toFile().exists() && folderPath.toFile().isDirectory();
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        ParametersChecker.checkParameter(ErrorMessage.FOLDER_NOT_FOUND.getMessage(), folderName);
        if (!isExistingContainer(containerName)) {
            // retro-compatibility
            throw new ContentAddressableStorageNotFoundException("Container " + containerName + " not found");
        }
        if (!isExistingFolder(containerName, folderName)) {
            LOGGER.debug(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            // throw new ContentAddressableStorageNotFoundException(
            // ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            // TODO: ugly retro-compatibility !
            return new ArrayList<>();
        }

        try {
            Path folderPath = getFolderPath(containerName, folderName, false);
            /*
             * return Files .walk(folderPath, FileVisitOption.FOLLOW_LINKS) .filter(path -> !folderPath.equals(path))
             * .map(Path::getFileName) .map(path -> URI.create(path.toString())) .collect(Collectors.toList());
             */

            final List<URI> list = new ArrayList<>();
            Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }
                    String pathFile = file.toString().replace(folderPath.toString() + "/", "");
                    list.add(URI.create(URLEncoder.encode(pathFile, CharsetUtils.UTF_8)));

                    return FileVisitResult.CONTINUE;
                }
            });
            return list;
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public void uncompressObject(String containerName, String folderName, String archiveMimeType,
        InputStream inputStreamObject) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        if (!isExistingContainer(containerName)) {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }
        if (inputStreamObject == null) {
            throw new ContentAddressableStorageException(ErrorMessage.STREAM_IS_NULL.getMessage());
        }
        if (isExistingFolder(containerName, folderName)) {
            LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + " : folderName " + folderName);
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
        }

        isPathTraversalAlreadyChecked = true;
        try {
            createFolder(containerName, folderName);
            extractArchiveInputStreamOnContainer(containerName, folderName, CommonMediaType.valueOf(archiveMimeType),
                inputStreamObject);
        } finally {
            isPathTraversalAlreadyChecked = false;
        }
    }

    @Override
    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }

        boolean existingObject = false;
        if (isExistingObject(containerName, objectName)) {
            LOGGER.info(ErrorMessage.OBJECT_ALREADY_EXIST.getMessage() + objectName);
            existingObject = true;
        }
        Path filePath = null;
        try {

            filePath = getObjectPath(containerName, objectName, false);

            if (!existingObject) {
                // create parent folders if needed
                Path parentPath = filePath.getParent();
                if (!parentPath.toFile().exists()) {
                    Files.createDirectories(parentPath);
                }
                filePath = Files.createFile(filePath);
            }
            Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOGGER.error("Try to rollback because of ", ex);
            // try to rollback -> keep it ?
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException exc) {
                LOGGER.error("Cannot rollback because of ", exc);
            }
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public Response getObject(String containerName, String objectName, Long chunkOffset,
        Long maxChunkSize)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);

        if (chunkOffset == null && maxChunkSize != null) {
            throw new IllegalArgumentException("Max chunk size without chunk offset");
        }

        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        if (!isExistingObject(containerName, objectName)) {
            LOGGER.error(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName + " in container '" + containerName + "'");
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
        }

        try {
            Path objectPath = getObjectPath(containerName, objectName, false);

            InputStream inputStream = openFile(objectPath, chunkOffset, maxChunkSize);
            long totalSize = Files.size(objectPath);

            long chunkSize;
            if (chunkOffset == null) {
                chunkSize = totalSize;
            } else if (maxChunkSize == null) {
                chunkSize = totalSize - chunkOffset;
            } else {
                chunkSize = Math.min(maxChunkSize, totalSize - chunkOffset);
            }
            return new AbstractMockClient.FakeInboundResponse(Response.Status.OK,
                new ExactSizeInputStream(inputStream, chunkSize),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, getXContentLengthHeader(totalSize, chunkSize));
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    private InputStream openFile(Path objectPath, Long chunkOffset, Long maxChunkSize) throws IOException {

        if (chunkOffset != null) {

            SeekableByteChannel seekableByteChannel = null;
            try {
                seekableByteChannel = Files.newByteChannel(objectPath, StandardOpenOption.READ);
                seekableByteChannel.position(chunkOffset);

                BufferedInputStream inputStream = new BufferedInputStream(
                    Channels.newInputStream(seekableByteChannel));

                if (maxChunkSize == null) {
                    return inputStream;
                } else {
                    return new BoundedInputStream(inputStream, maxChunkSize);
                }

            } catch (Exception ex) {
                closeSilently(seekableByteChannel);
                throw ex;
            }

        } else {
            return new BufferedInputStream(
                Files.newInputStream(objectPath, StandardOpenOption.READ));
        }
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        if (!isExistingObject(containerName, objectName)) {
            LOGGER.error(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName + " in container '" + containerName + "'");
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
        }
        try {
            Files.delete(getObjectPath(containerName, objectName, false));
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) {
        Path objectPath = null;
        try {
            objectPath = getObjectPath(containerName, objectName, true);
            return objectPath.toFile().exists() && Files.isRegularFile(objectPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(), algo);
        try (final InputStream stream = (InputStream) getObject(containerName, objectName, null, null).getEntity()) {
            final Digest digest = new Digest(algo);
            digest.update(stream);
            return digest.toString();
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        try {
            Path containerPath = getContainerPath(containerName, false);
            final long usableSpace = Files.getFileStore(containerPath).getUsableSpace();
            final ContainerInformation containerInformation = new ContainerInformation();
            containerInformation.setUsableSpace(usableSpace);
            return containerInformation;
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public JsonNode getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        if (!isExistingObject(containerName, objectName)) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
        }
        try {
            Path objectPath = getObjectPath(containerName, objectName, true);
            ObjectNode objectInformation = JsonHandler.createObjectNode();
            objectInformation.put("size", Files.size(objectPath));
            objectInformation.put("object_name", objectName);
            objectInformation.put("container_name", containerName);
            return objectInformation;
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public long countObjects(String containerName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        if (!isExistingContainer(containerName)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        try {
            Path containerPath = getContainerPath(containerName, false);
            try (Stream<Path> streams = Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS)) {
                return streams.filter(path -> !containerPath.equals(path))
                    .count();
            }
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    private Path getContainerPath(String containerName, boolean wouldCheckTraversalAttack) throws IOException {
        if (wouldCheckTraversalAttack && !isPathTraversalAlreadyChecked) {
            SafeFileChecker.checkSafeFilePath(root.toString(), containerName);
        }
        return Paths.get(root.toString(), containerName);
    }

    private Path getFolderPath(String containerName, String folder, boolean wouldCheckTraversalAttack)
        throws IOException {
        return getObjectPath(containerName, folder, wouldCheckTraversalAttack);
    }

    // Same as getFolderPath but...
    private Path getObjectPath(String containerName, String objectName, boolean wouldCheckTraversalAttack)
        throws IOException {
        if (wouldCheckTraversalAttack && !isPathTraversalAlreadyChecked) {
            SafeFileChecker.checkSafeFilePath(root.toString(), containerName, objectName);
        }
        return Paths.get(root.toString(), containerName, objectName);
    }

    /**
     * Extract compressed SIP and push the objects on the SIP folder
     *
     * @param containerName GUID
     * @param folderName folder Name
     * @param archiverType archive type zip, tar tar.gz
     * @param inputStreamObject :compressed SIP stream
     * @throws ContentAddressableStorageCompressedFileException if the file is not a zip or an empty zip
     * @throws ContentAddressableStorageException if an IOException occurs when extracting the file
     */
    private void extractArchiveInputStreamOnContainer(final String containerName, final String folderName,
        final MediaType archiverType, final InputStream inputStreamObject)
        throws ContentAddressableStorageException {

        try (final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(archiverType, inputStreamClosable)) {

            ArchiveEntry entry;
            boolean isEmpty = true;
            boolean manifestFileFounded = false;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);

            Path folderPath = getFolderPath(containerName, folderName, false);

            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    isEmpty = false;

                    final String entryName = entry.getName();

                    if (!SanityChecker.isValidFileName(entryName)) {
                        throw new ZipFilesNameNotAllowedException(
                            String.format("%s file or folder not allowed name", entryName));
                    }

                    final Path target = Paths.get(folderPath.toString(), entryName);
                    final Path parent = target.getParent();

                    if (parent != null && !parent.toFile().exists()) {
                        Files.createDirectories(parent);
                    }
                    if (!entry.isDirectory()) {
                        Files.copy(entryInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                        if (!manifestFileFounded && isManifestFileName(entry.getName())) {
                            Files.move(target, target.resolveSibling(IngestWorkflowConstants.SEDA_FILE));
                            manifestFileFounded = true;
                        }
                    }
                }
                entryInputStream.setClosed(false);
            }
            if (isEmpty) {
                throw new ContentAddressableStorageCompressedFileException("File is empty");
            }
        } catch (final IOException | ArchiveException e) {
            LOGGER.error(e);
            throw new ContentAddressableStorageException(e);
        }
    }

    private MultivaluedHashMap<String, Object> getXContentLengthHeader(long totalSize, long chunkSize) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(), Collections.singletonList(totalSize));
        headers.put(VitamHttpHeader.X_CHUNK_LENGTH.getName(), Collections.singletonList(chunkSize));
        return headers;
    }

    /**
     * @param containerName name of the container
     * @param folderNames list of file or directory to archive
     * @param zipName name of the archive file
     * @param outputContainer
     * @throws IOException
     * @throws ArchiveException
     */
    public void compress(String containerName, List<String> folderNames, String zipName,
        String outputContainer)
        throws IOException, ArchiveException {

        Path zip = getObjectPath(outputContainer, zipName, true);

        try (ArchiveOutputStream archive = new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, new FileOutputStream(zip.toString()))) {

            for (String folderName : folderNames) {
                final Path target = getFolderPath(containerName, folderName, true);

                Files.walkFileTree(target, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!attrs.isDirectory()) {

                            ZipArchiveOutputStream zipArchiveOutputStream = (ZipArchiveOutputStream) archive;
                            zipArchiveOutputStream.setUseZip64(Zip64Mode.Always);

                            Path relativize = Paths.get(target.getParent().toString()).relativize(file);
                            ZipArchiveEntry entry = new ZipArchiveEntry(relativize.toString());

                            zipArchiveOutputStream.putArchiveEntry(entry);

                            try (BufferedInputStream input = new BufferedInputStream(
                                new FileInputStream(file.toFile()))) {
                                IOUtils.copy(input, zipArchiveOutputStream);
                            }
                            zipArchiveOutputStream.closeArchiveEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    private boolean isManifestFileName(String fileName) {
        return fileName.matches(VitamConstants.MANIFEST_FILE_NAME_REGEX);
    }
}
