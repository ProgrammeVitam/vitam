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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper for the FileSystem offer
 */
public class HashFileSystemHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashFileSystem.class);

    private final String rootPath;
    private final FileSystem fs = FileSystems.getDefault();
    private final String SEPARATOR = fs.getSeparator();
    private static final String CONTAINER_SUBDIRECTORY = "container";

    /**
     * Constructor
     *
     * @param rootPath : the base path of the storage . Will be appended to all requests
     * @throws IllegalArgumentException : if rootPath is equals to / (to prevent error)
     */
    public HashFileSystemHelper(String rootPath) {
        ParametersChecker.checkParameter("rootPath must be defined", rootPath);
        if (rootPath.equals(SEPARATOR)) {
            throw new IllegalArgumentException("The storage Path can't be /");
        }
        this.rootPath = rootPath;
    }

    // High level functions for the directory structure

    /**
     * Get the path of a container
     *
     * @param containerName
     * @return the Path Object representing the container directory
     */
    public Path getPathContainer(String containerName) {
        return fs.getPath(rootPath, CONTAINER_SUBDIRECTORY, containerName);
    }

    /**
     * Split objectId without extension
     *
     * @param objectId
     * @return a list of tokens that will represent the directory structure
     * @throws ContentAddressableStorageServerException
     */
    public List<String> splitObjectId(String objectId) throws ContentAddressableStorageServerException {
        if (objectId.matches(SEPARATOR)) {
            throw new ContentAddressableStorageServerException(
                "objectId " + objectId + " contains " + SEPARATOR + " character which is forbidden "
            );
        }
        Digest d = new Digest(DigestType.SHA256);
        String digest = d.update(objectId.getBytes()).digestHex();
        List<String> r = new LinkedList<>();
        //Extract the 4 first HexaDigit to have a repartition in a 16x16x16x16 directories
        for (int i = 0; i < 4; i++) {
            r.add(digest.substring(i, i + 1));
        }
        return r;
    }

    /**
     * Get the path of an object based on its container and the objectId
     *
     * @param container
     * @param objectId
     * @return Path of the object
     * @throws ContentAddressableStorageNotFoundException : container not found
     * @throws ContentAddressableStorageServerException : The objectId contains Separator character
     */
    public Path getPathObject(String container, String objectId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        if (!isContainer(container)) {
            throw new ContentAddressableStorageNotFoundException("Container " + container + " doesn't exist");
        }
        List<String> subDirectoriesComponentList = new LinkedList<>();
        for (String id : splitObjectId(objectId)) {
            subDirectoriesComponentList.add(id);
        }
        // Add the objectId name at the end
        subDirectoriesComponentList.add(objectId);
        String[] subDirectoriesComponentArray = subDirectoriesComponentList.toArray(new String[0]);

        return fs.getPath(getPathContainer(container).toString(), subDirectoriesComponentArray);
    }

    // Manage Container

    /**
     * Create a directory recursively in the sub tree
     *
     * @param container : relative path that will be appended at the end of the rootPath
     * @throws ContentAddressableStorageServerException : on I/O Errors
     */
    public void createContainer(String container) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter("Container can't be null", container);
        createDirectories(getPathContainer(container));
    }

    /**
     * @param subpath : relative path that will be appended at the end of the rootPath
     * @return true if the subpath is a directory
     */
    public boolean isContainer(String subpath) {
        ParametersChecker.checkParameter("Subpath can't be null", subpath);
        return getPathContainer(subpath).toFile().isDirectory();
    }

    // Low level functions

    /**
     * Create recursively the directories
     *
     * @param path
     * @throws ContentAddressableStorageServerException
     */
    public void createDirectories(Path path) throws ContentAddressableStorageServerException {
        if (path.toFile().isDirectory()) {
            return;
        }
        try {
            Files.createDirectories(path);
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("Path " + path.toString() + " already exists", e);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("Can't create the directory " + path, e);
        }
    }
}
