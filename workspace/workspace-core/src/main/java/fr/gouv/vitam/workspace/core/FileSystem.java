package fr.gouv.vitam.workspace.core;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.filesystem.reference.FilesystemConstants;

import fr.gouv.vitam.workspace.api.config.StorageConfiguration;


// TODO REVIEW Licence file
/**
 * FileSystemMock implements a Content Addressable Storage that stores objects
 * on the file system.
 */
public class FileSystem extends ContentAddressableStorageImpl {

    private static final Logger LOGGER = Logger.getLogger(FileSystem.class);

    // TODO REVIEW comment
    public FileSystem(StorageConfiguration configuration) {
        super(configuration);
    }

    @Override
    public BlobStoreContext getContext(StorageConfiguration configuration) {
        Properties props = new Properties();
        props.setProperty(FilesystemConstants.PROPERTY_BASEDIR, configuration.getStoragePath());
        LOGGER.info("Get File System Context");
        return ContextBuilder.newBuilder("filesystem").overrides(props).buildView(BlobStoreContext.class);

    }

}
