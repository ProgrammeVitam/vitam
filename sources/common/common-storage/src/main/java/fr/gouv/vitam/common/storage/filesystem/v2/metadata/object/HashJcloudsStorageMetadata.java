/*
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
package fr.gouv.vitam.common.storage.filesystem.v2.metadata.object;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.cas.container.api.Location;
import fr.gouv.vitam.common.storage.cas.container.api.StorageType;
import fr.gouv.vitam.common.storage.cas.container.api.VitamResourceMetadata;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

/**
 * POJO object that store the metadata of the object
 *
 * @author vitam
 */
public class HashJcloudsStorageMetadata implements VitamStorageMetadata {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashJcloudsStorageMetadata.class);

    private Date creationDate;
    private String etag;
    private Date lastModifiedDate;
    private String name;
    private String providerId;
    private Long size;
    private StorageType type;
    private Path filePath;

    /**
     * Constructor . Will only load the name of the object for performance reasons. If you need additionals metadata, use setStatValues
     *
     * @param path
     */
    public HashJcloudsStorageMetadata(Path path) {
        filePath = path;
        name = filePath.getFileName().toString();
    }

    /**
     * set statistics values
     */
    public void setStatValues() {
        try {
            File f = filePath.toFile();
            size = Files.size(filePath);
            lastModifiedDate = new Date(Files.getLastModifiedTime(filePath).toMillis());
            if (f.isFile()) {
                type = StorageType.BLOB;
            } else if (f.isDirectory()) {
                type = StorageType.FOLDER;
            }
        } catch (IOException e) {
            LOGGER.error("I/O error when having stat on file " + filePath.toString(), e);
        }
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public int compareTo(VitamResourceMetadata<StorageType> arg0) {
        return 0;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public String getETag() {
        return etag;
    }

    @Override
    public Date getLastModified() {
        return lastModifiedDate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public StorageType getType() {
        return type;
    }

    @Override
    public URI getUri() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map<String, String> getUserMetadata() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
