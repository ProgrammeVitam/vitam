/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.storage.filesystem.v2.metadata;

import fr.gouv.vitam.common.storage.cas.container.api.Location;
import fr.gouv.vitam.common.storage.cas.container.api.StorageType;
import fr.gouv.vitam.common.storage.cas.container.api.VitamResourceMetadata;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;

import java.net.URI;
import java.util.Date;
import java.util.Map;

/**
 * This class is Immutable and wrap jcloud implementation
 */
public final class VitamStorageMetadataImpl implements VitamStorageMetadata {
    private final StorageType storageType;
    private final String providerId;
    private final String name;
    private final Location location;
    private final URI uri;
    private final Map<String, String> userMetadata;
    private final String eTag;
    private final Date creationDate;
    private final Date lastModified;
    private final Long size;

    /**
     *
     * @param storageType
     * @param providerId
     * @param name
     * @param location
     * @param uri
     * @param userMetadata
     * @param eTag
     * @param creationDate
     * @param lastModified
     * @param size
     */
    public VitamStorageMetadataImpl(StorageType storageType, String providerId, String name,
        Location location, URI uri, Map<String, String> userMetadata, String eTag, Date creationDate,
        Date lastModified, Long size) {
        this.storageType = storageType;
        this.providerId = providerId;
        this.name = name;
        this.location = location;
        this.uri = uri;
        this.userMetadata = userMetadata;
        this.eTag = eTag;
        this.creationDate = creationDate;
        this.lastModified = lastModified;
        this.size = size;
    }

    @Override
    public StorageType getType() {
        return storageType;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    @Override
    public String getETag() {
        return eTag;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public int compareTo(VitamResourceMetadata<StorageType> o) {
        if (getName() == null)
            return -1;
        return (this == o) ? 0 : getName().compareTo(o.getName());
    }
}
