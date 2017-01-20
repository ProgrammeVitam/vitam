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
package fr.gouv.vitam.workspace.core.filesystem;

import java.io.File;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.providers.ProviderMetadata;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageAbstract;
import fr.gouv.vitam.workspace.core.StorageConfiguration;
import fr.gouv.vitam.workspace.core.WorkspaceConfiguration;


/**
 * FileSystemMock implements a Content Addressable Storage that stores objects on the file system.
 */
public class FileSystem extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileSystem.class);

    /**
     * @param configuration to associate with the FileSystem
     */
    public FileSystem(WorkspaceConfiguration configuration) {
        super(configuration);
    }

    /**
     * @param configuration to associate with the FileSystem
     */
    public FileSystem(StorageConfiguration configuration) {
        super(configuration);
    }

    // TODO will be deleted in #US1757
    @Override
    public BlobStoreContext getContext(WorkspaceConfiguration configuration) {
        final Properties props = new Properties();
        WorkspaceConfiguration fileSyestemConfiguration = configuration;
        props.setProperty(FilesystemConstants.PROPERTY_BASEDIR, fileSyestemConfiguration.getStoragePath());
        LOGGER.debug("Get File System Context");
        return ContextBuilder.newBuilder("filesystem").overrides(props).buildView(BlobStoreContext.class);
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException {
        final File baseDirFile = getBaseDir(containerName);
        final long usableSpace = baseDirFile.getUsableSpace();
        final long usedSpace = getFolderUsedSize(baseDirFile);
        final ContainerInformation containerInformation = new ContainerInformation();
        containerInformation.setUsableSpace(usableSpace);
        containerInformation.setUsedSpace(usedSpace);
        return containerInformation;
    }

    private long getFolderUsedSize(File directory) {
        long usedSpace = 0;
        for (final File file : directory.listFiles()) {
            if (file.isFile()) {
                usedSpace += file.length();
            } else {
                usedSpace += getFolderUsedSize(file);
            }
        }
        return usedSpace;
    }

    private File getBaseDir(String containerName) throws ContentAddressableStorageNotFoundException {
        final ProviderMetadata providerMetadata = context.unwrap().getProviderMetadata();
        final Properties properties = providerMetadata.getDefaultProperties();
        final String baseDir = properties.getProperty(FilesystemConstants.PROPERTY_BASEDIR);
        File baseDirFile;
        if (containerName != null) {
            baseDirFile = new File(baseDir, containerName);
        } else {
            baseDirFile = new File(baseDir);
        }
        if (!baseDirFile.exists()) {
            throw new ContentAddressableStorageNotFoundException("Storage not found");
        }
        return baseDirFile;
    }

    @Override
    public BlobStoreContext getContext(StorageConfiguration configuration) {
        final Properties props = new Properties();
        props.setProperty(FilesystemConstants.PROPERTY_BASEDIR, configuration.getStoragePath());
        LOGGER.debug("Get File System Context");
        return ContextBuilder.newBuilder("filesystem").overrides(props).buildView(BlobStoreContext.class);
    }
}
