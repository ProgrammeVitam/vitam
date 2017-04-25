/**
 *
 */
package fr.gouv.vitam.common.storage.filesystem.v2.metadata.container;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.filesystem.v2.HashFileScanVisitor;
import fr.gouv.vitam.common.storage.filesystem.v2.HashFileSystemHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Structure
 */
public class HashContainerMetadata {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashContainerMetadata.class);
    private static final String NB_OBJECTS = "nbObjects";
    private static final String USED_BYTES = "usedBytes";
    private final String containerName;
    private final HashFileSystemHelper fsHelper;
    private final Path containerDirectoryPath;
    private AtomicLong nbObjects = new AtomicLong(0);
    private AtomicLong usedBytes = new AtomicLong(0);


    /**
     * Default constructor . Try to load from system
     *
     * @param containerName
     * @param fsHelper
     */
    public HashContainerMetadata(String containerName, HashFileSystemHelper fsHelper) {
        this(containerName, fsHelper, false);
    }

    /**
     * Constructor
     *
     * @param containerName
     * @param fsHelper
     * @param existingContainer : boolean to indicate if it is a new container or an existing container
     */
    public HashContainerMetadata(String containerName, HashFileSystemHelper fsHelper, boolean existingContainer) {
        this.containerName = containerName;
        this.fsHelper = fsHelper;
        containerDirectoryPath = fsHelper.getPathContainer(containerName);
        if (existingContainer && !unmarshall()) {
            // Rescan the container to calculate the correct value
            rescanContainer(containerName);
        }
    }



    private void rescanContainer(String containerName) {
        HashFileScanVisitor fv = new HashFileScanVisitor();
        try {
            Files.walkFileTree(fsHelper.getPathContainer(containerName), fv);
            nbObjects = new AtomicLong(fv.getNbObjects());
            usedBytes = new AtomicLong(fv.getContainerBytes());
        } catch (IOException e) {
            LOGGER.warn("Unable to correctly calculate the container metadata of " + containerName, e);
        }
    }

    /**
     * Synchronized method to update the object
     *
     * @param diffNbObjects
     * @param diffUsedBytes
     */
    public synchronized void updateAndMarshall(long diffNbObjects, long diffUsedBytes) {
        nbObjects.addAndGet(diffNbObjects);
        usedBytes.addAndGet(diffUsedBytes);
        marshall();
    }

    /**
     * @return the containerName
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * @return the nbObjects
     */
    public long getNbObjects() {
        return nbObjects.get();
    }

    /**
     * @return the usedBytes
     */
    public long getUsedBytes() {
        return usedBytes.get();
    }


    private void marshall() {
        UserDefinedFileAttributeView userXattrView =
            Files.getFileAttributeView(containerDirectoryPath, UserDefinedFileAttributeView.class);
        try {
            writeToXattr(userXattrView, NB_OBJECTS, nbObjects);
            writeToXattr(userXattrView, USED_BYTES, usedBytes);
        } catch (IOException e) {
            LOGGER.error("Unable to write container Metadata to extended attributes", e);
        }
    }

    private boolean unmarshall() {
        UserDefinedFileAttributeView userXattrView =
            Files.getFileAttributeView(containerDirectoryPath, UserDefinedFileAttributeView.class);
        try {
            nbObjects = new AtomicLong(readFromXattr(userXattrView, NB_OBJECTS));
            usedBytes = new AtomicLong(readFromXattr(userXattrView, USED_BYTES));
            return true;
        } catch (IOException e) {
            LOGGER.error("Unable to retrieve the container metadata information for container " + containerName, e);
            return false;
        }

    }

    private void writeToXattr(UserDefinedFileAttributeView view, String name, AtomicLong al) throws IOException {
        view.write(name, ByteBuffer.wrap(al.toString().getBytes()));
    }

    private Long readFromXattr(UserDefinedFileAttributeView view, String name) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(view.size(name));
        view.read(name, bb);
        return bb.getLong();
    }
}
