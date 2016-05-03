package fr.gouv.vitam.common;

import fr.gouv.vitam.common.exception.InvalidUuidOperationException;

/**
 * UUID Factory
 */
@SuppressWarnings("rawtypes")
public class UUIDFactory {
    final int domainId;
    final int platformId;

    /**
     * Default constructor using Mac address as platformId and domainId = 0
     */
    public UUIDFactory() {
        this(0, UUIDAbstract.MACI);
    }

    /**
     * Default constructor using Mac address as platformId
     * 
     * @param domainId
     */
    public UUIDFactory(final int domainId) {
        this(domainId, UUIDAbstract.MACI);
    }

    /**
     * Full constructor
     * 
     * @param domainId
     * @param platformId
     */
    public UUIDFactory(final int domainId, final int platformId) {
        this.domainId = domainId;
        this.platformId = platformId;
    }

    /**
     * Usable for internal UUID with default domainId or platformId and objectId
     * = 0
     * 
     * @return a new UUID
     */
    public UUID newUuid() {
        return new UUID22(0, domainId, platformId, false);
    }

    /**
     * Usable for UUID with default domainId or platformId
     * 
     * @param objectId
     * @return a new UUID
     */
    public UUID newUuid(final int objectId) {
        return new UUID22(objectId, domainId, platformId, false);
    }

    /**
     * Usable for UUID with default platformId
     * 
     * @param objectId
     * @param domainId
     * @return a new UUID
     */
    public UUID newUuid(final int objectId, final int domainId) {
        return new UUID22(objectId, domainId, platformId, false);
    }

    /**
     * Usable for general UUID
     * 
     * @param objectId
     * @param domainId
     * @param platformId
     * @return a new UUID
     */
    public UUID newUuid(final int objectId, final int domainId, final int platformId) {
        return new UUID22(objectId, domainId, platformId, false);
    }

    /**
     * Usable for internal UUID with default domainId or platformId and objectId
     * = 0
     * 
     * @param worm
     *            True if Worm UUID
     * @return a new UUID
     */
    public UUID newUuid(final boolean worm) {
        return new UUID22(0, domainId, platformId, worm);
    }

    /**
     * Usable for UUID with default domainId or platformId
     * 
     * @param objectId
     * @param worm
     *            True if Worm UUID
     * @return a new UUID
     */
    public UUID newUuid(final int objectId, final boolean worm) {
        return new UUID22(objectId, domainId, platformId, worm);
    }

    /**
     * Usable for UUID with default platformId
     * 
     * @param objectId
     * @param domainId
     * @param worm
     *            True if Worm UUID
     * @return a new UUID
     */
    public UUID newUuid(final int objectId, final int domainId, final boolean worm) {
        return new UUID22(objectId, domainId, platformId, worm);
    }

    /**
     * Usable for general UUID
     * 
     * @param objectId
     * @param domainId
     * @param platformId
     * @param worm
     *            True if Worm UUID
     * @return a new UUID
     */
    public UUID newUuid(final int objectId, final int domainId, final int platformId,
            final boolean worm) {
        return new UUID22(objectId, domainId, platformId, worm);
    }

    /**
     * Usable when a strict children UUID is to be created, therefore inherits
     * information from parent UUID
     * 
     * @param existingUuid
     *            used to get the objectId parent ({@link UuidObjectType}),
     *            domainId, platformId and Worm
     * @return a new UUID
     */
    public UUID newChildrenUuid(final UUID existingUuid) {
        return new UUID22(UuidObjectType.getChildrenType(existingUuid.getObjectId()),
                existingUuid.getDomainId(), existingUuid.getPlatformId(),
                existingUuid.isWorm());
    }

    /**
     * Create a Unit UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newUnitUuid(final int domainId) {
        int type = UuidObjectType.UNIT_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a ObjectGroup UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newObjectGroupUuid(final int domainId) {
        int type = UuidObjectType.OBJECTGROUP_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Object UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newObjectUuid(final int domainId) {
        int type = UuidObjectType.OBJECT_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Binary UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newBinaryUuid(final int domainId) {
        int type = UuidObjectType.BINARY_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Unit Logbook UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newUnitLogbookUuid(final int domainId) {
        int type = UuidObjectType.UNIT_LOGBOOK_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a ObjectGroup Logbook UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newObjectGroupLogbookUuid(final int domainId) {
        int type = UuidObjectType.OBJECTGROUP_LOGBOOK_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Operation Logbook UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newOperationLogbookUuid(final int domainId) {
        int type = UuidObjectType.OPERATION_LOGBOOK_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Write Logbook UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newWriteLogbookUuid(final int domainId) {
        int type = UuidObjectType.WRITE_LOGBOOK_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Tenant Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newTenantContainerUuid(final int domainId) {
        int type = UuidObjectType.TENANT_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Unit Logbook Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newUnitLogbookContainerUuid(final int domainId) {
        int type = UuidObjectType.UNIT_LOGBOOK_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a ObjectGroup Logbook Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newObjectGroupLogbookContainerUuid(final int domainId) {
        int type = UuidObjectType.OBJECTGROUP_LOGBOOK_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Operation Logbook Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newOperationLogbookContainerUuid(final int domainId) {
        int type = UuidObjectType.OPERATION_LOGBOOK_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Write Logbook Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newWriteLogbookContainerUuid(final int domainId) {
        int type = UuidObjectType.WRITE_LOGBOOK_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Unit Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newUnitContainerUuid(final int domainId) {
        int type = UuidObjectType.UNIT_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a ObjectGroup Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newObjectGroupContainerUuid(final int domainId) {
        int type = UuidObjectType.OBJECTGROUP_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Binary Container UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newBinaryContainerUuid(final int domainId) {
        int type = UuidObjectType.BINARY_CONTAINER_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Storage Operation UUID
     * 
     * @param domainId
     * @param worm
     * @return a new UUID
     */
    public UUID newStorageOperationUuid(final int domainId, final boolean worm) {
        return new UUID22(UuidObjectType.STORAGE_OPERATION_TYPE, domainId, platformId,
                worm);
    }

    /**
     * Create a Operation Id UUID
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newOperationIdUuid(final int domainId) {
        int type = UuidObjectType.OPERATIONID_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Request Id UUID (X-CID)
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newRequestIdUuid(final int domainId) {
        int type = UuidObjectType.REQUESTID_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Manifest UUID (SEDA)
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newManifestUuid(final int domainId) {
        int type = UuidObjectType.MANIFEST_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Manifest UUID (SEDA)
     * 
     * @param domainId
     * @return a new UUID
     */
    public UUID newFuseUuid(final int domainId) {
        int type = UuidObjectType.FUSE_FILE_TYPE;
        return new UUID22(type, domainId, platformId,
                UuidObjectType.getDefaultWorm(type));
    }

    /**
     * 
     * @param uuid
     * @return True if the given UUID is using a WORM media
     */
    public static boolean isWorm(final UUID uuid) {
        return uuid.isWorm();
    }

    /**
     * A UUID using the string representation given as parameter
     * 
     * @param id
     *            id source in String format
     * @return one UUID
     * @throws InvalidUuidOperationException
     */
    public UUID getUuid(final String id) throws InvalidUuidOperationException {
        return new UUID22(id);
    }

    /**
     * A UUID using the bytes representation given as parameter
     * 
     * @param id
     *            id source in bytes format
     * @return one UUID
     * @throws InvalidUuidOperationException
     */
    public UUID getUuid(final byte[] id) throws InvalidUuidOperationException {
        return new UUID22(id);
    }

    /**
     * @return the size of the key in bytes
     */
    public int getKeysize() {
        return UUID22.KEYSIZE;
    }

    /**
     * @return the size of the key using Base32 format in bytes
     */
    public int getKeysizeBase32() {
        return UUID22.KEYB32SIZE;
    }

    /**
     * @return the domainId
     */
    public final int getDomainId() {
        return domainId;
    }

    /**
     * @return the platformId
     */
    public final int getPlatformId() {
        return platformId;
    }

}
