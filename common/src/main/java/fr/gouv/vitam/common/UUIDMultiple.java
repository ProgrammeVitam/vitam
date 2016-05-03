package fr.gouv.vitam.common;

import java.util.Set;

import fr.gouv.vitam.common.exception.InvalidUuidOperationException;

/**
 * Multiple UUID handling
 */
@SuppressWarnings("rawtypes")
public class UUIDMultiple {
    UUIDFactory uuidFactory;
    
    /**
     * 
     * @param uuidFactory
     */
    public UUIDMultiple(UUIDFactory uuidFactory) {
        this.uuidFactory = uuidFactory;
    }
    /**
     *
     * @param uuids
     * @return the assembly UUID of all given UUIDs
     */
    public String assembleUuids(final UUID... uuids) {
        final StringBuilder builder = new StringBuilder();
        for (final UUID uuid : uuids) {
            builder.append(uuid.toString());
        }
        return builder.toString();
    }

    /**
     *
     * @param idsource
     * @return the array of UUID according to the source (concatenation of
     *         UUIDs)
     * @throws InvalidUuidOperationException
     */
    public UUID[] getUuids(final String idsource) throws InvalidUuidOperationException {
        final String id = idsource.trim();
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final int nb = id.length() / keyb32size;
        final UUID[] uuids = new UUID[nb];
        int beginIndex = 0;
        int endIndex = keyb32size;
        for (int i = 0; i < nb; i++) {
            uuids[i] = uuidFactory.getUuid(id.substring(beginIndex, endIndex));
            beginIndex = endIndex;
            endIndex += keyb32size;
        }
        return uuids;
    }

    /**
     *
     * @param idsource
     * @return the number of UUID in this idsource
     */
    public int getUuidNb(final String idsource) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        return idsource.trim().length() / keyb32size;
    }

    /**
     *
     * @param idsource
     * @return true if this idsource represents more than one UUID (path of
     *         UUIDs)
     */
    public boolean isMultipleUUID(final String idsource) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        return idsource.trim().length() > keyb32size;
    }

    /**
     *
     * @param idsource
     * @return the last UUID from this idsource
     * @throws InvalidUuidOperationException
     */
    public UUID getLast(final String idsource) throws InvalidUuidOperationException {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim();
        final int nb = id.length() / keyb32size - 1;
        final int pos = keyb32size * nb;
        return uuidFactory.getUuid(id.substring(pos, pos + keyb32size));
    }

    /**
     *
     * @param idsource
     * @return the first UUID from this idsource
     * @throws InvalidUuidOperationException
     */
    public UUID getFirst(final String idsource) throws InvalidUuidOperationException {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim().substring(0, keyb32size);
        return uuidFactory.getUuid(id);
    }

    /**
     *
     * @param idsource
     * @return the last UUID from this idsource
     */
    public String getLastAsString(final String idsource) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim();
        final int nb = id.length() / keyb32size - 1;
        final int pos = keyb32size * nb;
        return id.substring(pos, pos + keyb32size);
    }

    /**
     *
     * @param idsource
     * @return the first UUID from this idsource
     */
    public String getFirstAsString(final String idsource) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        return idsource.trim().substring(0, keyb32size);
    }

    /**
     * 
     * @param idsource
     * @param idIn
     * @return True if idIn is in idsource
     */
    public boolean isInPath(final String idsource, final String idIn) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim();
        final int nb = id.length() / keyb32size;
        int beginIndex = 0;
        int endIndex = keyb32size;
        for (int i = 0; i < nb; i++) {
            if (idIn.equals(id.substring(beginIndex, endIndex))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param idsource
     * @param idsIn
     * @return True if any of id in idsIn is in idsource
     */
    public boolean isInPath(final String idsource, Set<String> idsIn) {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim();
        final int nb = id.length() / keyb32size;
        final int beginIndex = 0;
        final int endIndex = keyb32size;
        final String searched = id.substring(beginIndex, endIndex);
        for (int i = 0; i < nb; i++) {
            if (idsIn.contains(searched)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param idsource
     * @return the array of UUID according to the source (concatenation of UUIDs
     *         separated by '#')
     * @throws InvalidUuidOperationException
     */
    public UUID[] getUuidsSharp(final String idsource)
            throws InvalidUuidOperationException {
        final int keyb32size = uuidFactory.getKeysizeBase32();
        final String id = idsource.trim();
        final int nb = id.length() / (keyb32size + 1) + 1;
        final UUID[] uuids = new UUID[nb];
        int beginIndex = 0;
        int endIndex = keyb32size;
        for (int i = 0; i < nb; i++) {
            uuids[i] = uuidFactory.getUuid(id.substring(beginIndex, endIndex));
            beginIndex = endIndex + 1;
            endIndex += keyb32size + 1;
        }
        return uuids;
    }

}
