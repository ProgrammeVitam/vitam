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
