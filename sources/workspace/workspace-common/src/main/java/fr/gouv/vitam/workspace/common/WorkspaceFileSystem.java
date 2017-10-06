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

package fr.gouv.vitam.workspace.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

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
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Workspace Filesystem implementation
 */
public class WorkspaceFileSystem implements WorkspaceContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceFileSystem.class);

    private Path root;

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
        root = Paths.get(configuration.getStoragePath());
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
                Path containerPath = getContainerPath(containerName);
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
            Path containerPath = getContainerPath(containerName);
            Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS).filter(path -> !containerPath.equals(path))
                .sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(ex);
        }
    }

    @Override
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Path containerPath = getContainerPath(containerName);
        if (!Files.exists(containerPath)) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName);
        }
        if (recursive) {
            try {
                Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
            } catch (IOException ex) {
                throw new ContentAddressableStorageServerException(ex);
            }
        } else {
            try {
                Files.delete(containerPath);
            } catch (DirectoryNotEmptyException ex) {
                // TODO: keep this old none recursive delete workspace container style ?
                LOGGER.warn("Directory {} not empty, do nothing", containerPath.toString());
            } catch (IOException ex) {
                throw new ContentAddressableStorageServerException(ex);
            }
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        Path containerPath = getContainerPath(containerName);
        // if not a directory -> error ?
        return Files.exists(containerPath) && Files.isDirectory(containerPath);
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
        Path folderPath = getFolderPath(containerName, folderName);
        try {
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
        Path folderPath = getFolderPath(containerName, folderName);
        try {
            Files.delete(folderPath);
        } catch (DirectoryNotEmptyException ex) {
            LOGGER.warn("Directory {} not empty, then we delete files and folder", folderPath.toString());
            try {
                Files.walk(folderPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder())
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
        Path folderPath = getFolderPath(containerName, folderName);
        // if not a directory -> error ?
        return Files.exists(folderPath) && Files.isDirectory(folderPath);
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
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            // throw new ContentAddressableStorageNotFoundException(
            // ErrorMessage.FOLDER_NOT_FOUND.getMessage() + folderName);
            // TODO: ugly retro-compatibility !
            return new ArrayList<>();
        }
        Path folderPath = getFolderPath(containerName, folderName);
        try {
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
        createFolder(containerName, folderName);
        extractArchiveInputStreamOnContainer(containerName, folderName, CommonMediaType.valueOf(archiveMimeType),
            inputStreamObject);
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
        Path filePath = getObjectPath(containerName, objectName);
        try {
            if (!existingObject) {
                // create parent folders if needed
                Path parentPath = filePath.getParent();
                if (!Files.exists(parentPath)) {
                    Files.createDirectories(parentPath);
                }
                filePath = Files.createFile(filePath);
            }
            Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOGGER.error("Try to rollback because of ", ex);
            // try to rollback -> keep it ?
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException exc) {
                    LOGGER.error("Cannot rollback because of ", exc);
                }
            }
        }
    }

    @Override
    public Response getObject(String containerName, String objectName) throws ContentAddressableStorageException {
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
            Path objectPath = getObjectPath(containerName, objectName);
            InputStream inputStream = Files.newInputStream(objectPath, StandardOpenOption.READ);
            long size = Files.size(objectPath);
            return new AbstractMockClient.FakeInboundResponse(Response.Status.OK, inputStream,
                MediaType.APPLICATION_OCTET_STREAM_TYPE, getXContentLengthHeader(size));
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
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
            Files.delete(getObjectPath(containerName, objectName));
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) {
        Path objectPath = getObjectPath(containerName, objectName);
        return Files.exists(objectPath) && Files.isRegularFile(objectPath);
    }

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.ALGO_IS_A_MANDATORY_PARAMETER.getMessage(), algo);
        try (final InputStream stream = (InputStream) getObject(containerName, objectName).getEntity()) {
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
        Path containerPath = getContainerPath(containerName);
        try {
            final long usedSpace = Files.size(containerPath);
            final long usableSpace = Files.getFileStore(containerPath).getUsableSpace();
            final ContainerInformation containerInformation = new ContainerInformation();
            containerInformation.setUsableSpace(usableSpace);
            containerInformation.setUsedSpace(usedSpace);
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
        Path objectPath = getObjectPath(containerName, objectName);
        try {
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
        Path containerPath = getContainerPath(containerName);
        try {
            return Files.walk(containerPath, FileVisitOption.FOLLOW_LINKS).filter(path -> !containerPath.equals(path))
                .count();
        } catch (IOException ex) {
            throw new ContentAddressableStorageException(ex);
        }
    }

    private Path getContainerPath(String containerName) {
        return Paths.get(root.toString(), containerName);
    }

    private Path getFolderPath(String containerName, String folder) {
        return Paths.get(root.toString(), containerName, folder);
    }

    // Same as getFolderPath but...
    private Path getObjectPath(String containerName, String objectName) {
        return Paths.get(root.toString(), containerName, objectName);
    }

    /**
     * Extract compressed SIP and push the objects on the SIP folder
     *
     * @param containerName     GUID
     * @param folderName        folder Name
     * @param archiverType      archive type zip, tar tar.gz
     * @param inputStreamObject :compressed SIP stream
     * @throws ContentAddressableStorageCompressedFileException if the file is not a zip or an empty zip
     * @throws ContentAddressableStorageException               if an IOException occurs when extracting the file
     */
    private void extractArchiveInputStreamOnContainer(final String containerName, final String folderName,
        final MediaType archiverType, final InputStream inputStreamObject)
        throws ContentAddressableStorageException {

        try (final InputStream inputStreamClosable = StreamUtils.getRemainingReadOnCloseInputStream(inputStreamObject);
            final ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                .createArchiveInputStream(archiverType, inputStreamClosable)) {

            ArchiveEntry entry;
            boolean isEmpty = true;
            // create entryInputStream to resolve the stream closed problem
            final ArchiveEntryInputStream entryInputStream = new ArchiveEntryInputStream(archiveInputStream);


            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    isEmpty = false;

                    final String entryName = entry.getName();
                    final Path target = Paths.get(root.toString(), containerName, folderName, entryName);
                    final Path parent = target.getParent();

                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    if (!entry.isDirectory()) {
                        Files.copy(entryInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                entryInputStream.setClosed(false);
            }
            archiveInputStream.close();
            if (isEmpty) {
                throw new ContentAddressableStorageCompressedFileException("File is empty");
            }
        } catch (final IOException | ArchiveException e) {
            LOGGER.error(e);
            throw new ContentAddressableStorageException(e);
        }
    }

    private MultivaluedHashMap<String, Object> getXContentLengthHeader(long size) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        List<Object> headersList = new ArrayList<>();
        headersList.add(size);
        headers.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(), headersList);
        return headers;
    }

    /**
     *
     * @param containerName name of the container
     * @param folderNames list of file or directory to archive
     * @param zipName name of the archive file
     * @throws IOException
     * @throws CompressorException
     * @throws ArchiveException
     */
    public void compress(String containerName, List<String> folderNames, String zipName)
        throws IOException, CompressorException, ArchiveException {

        Path zip = Paths.get(root.toString(), containerName, zipName);

        try (ArchiveOutputStream archive = new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, new FileOutputStream(zip.toString()))) {

            for (String folderName : folderNames) {
                final Path target = Paths.get(root.toString(), containerName, folderName);

                Files.walkFileTree(Paths.get(target.toString()), new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!attrs.isDirectory()) {
                            Path relativize = Paths.get(target.getParent().toString()).relativize(file);
                            ArchiveEntry entry = new ZipArchiveEntry(relativize.toString());
                            archive.putArchiveEntry(entry);

                            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file.toFile()));

                            IOUtils.copy(input, archive);
                            input.close();
                            archive.closeArchiveEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    /**
     * Archive input streams <b>MUST</b> override the {@link #read(byte[], int, int)} - or {@link #read()} - method so
     * that reading from the stream generates EOF for the end of data in each entry as well as at the end of the file
     * proper.
     */
    static class ArchiveEntryInputStream extends InputStream {

        InputStream inputStream;
        boolean closed = false;

        /**
         * @param archiveInputStream
         * @throws IOException
         */
        public ArchiveEntryInputStream(InputStream archiveInputStream) throws IOException {
            inputStream = archiveInputStream;
        }

        @Override
        public int available() throws IOException {
            if (closed) {
                return -1;
            }
            return inputStream.available();
        }

        @Override
        public long skip(long n) throws IOException {
            if (closed) {
                return -1;
            }
            return inputStream.skip(n);
        }

        @Override
        public int read() throws IOException {
            if (closed) {
                return -1;
            }
            return inputStream.read();

        }

        @Override
        public int read(byte[] b) throws IOException {
            if (closed) {
                return -1;
            }
            return inputStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                return -1;
            }
            return inputStream.read(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }

        /**
         * Allow to "fakely" reopen this InputStream
         *
         * @param isclosed
         */
        public void setClosed(boolean isclosed) {
            closed = isclosed;
        }

    }
}
