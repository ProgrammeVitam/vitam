package fr.gouv.vitam.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * nUUID interface
 * 
 * @param <E>
 *            Real Class
 * 
 * 
 */
public interface UUID<E> extends Comparable<E> {
    /**
     * @return True if is Worm
     */
    @JsonIgnore
    public boolean isWorm();

    /**
     * @return the Base32 representation (default of toString)
     */
    public abstract String toBase32();

    /**
     * @return the Base64 representation (default of toString)
     */
    public abstract String toBase64();

    /**
     * @return the Hexadecimal representation
     */
    public abstract String toHex();

    /**
     * @return the Ark representation of this UUID
     */
    public abstract String toArk();

    /**
     * @return the Ark Name part of Ark representation
     */
    @JsonIgnore
    public default String toArkName() {
        return toString();
    }

    @Override
    public abstract String toString();

    /**
     * copy the uuid of this UUID, so that it can't be changed, and return it
     *
     * @return raw byte array of UUID
     */
    public byte[] getBytes();

    /**
     * extract version field as a hex char from raw UUID bytes
     *
     * @return version char
     */
    public int getVersion();

    /**
     * @return the id of the Object Type (default being 0)
     */
    @JsonIgnore
    public default int getObjectId() {
        return 0;
    }

    /**
     * @return the Domain Id (tenant) of UUID from which it belongs to (default
     *         being 0)
     */
    @JsonIgnore
    public default int getDomainId() {
        return 0;
    }

    /**
     * Extract process id and return as int
     *
     * @return id of process that generated the UUID, or -1 for unrecognized
     *         format
     */
    public int getProcessId();

    /**
     * @return the associated counter against collision value
     */
    public int getCounter();

    /**
     * Extract timestamp and return as long
     *
     * @return millisecond UTC timestamp from generation of the UUID, or -1 for
     *         unrecognized format
     */
    public long getTimestamp();

    /**
     * Extract Platform id as bytes. Could be using partial MAC address.
     *
     * @return byte array of UUID fragment, or null for unrecognized format
     */
    public byte[] getMacFragment();

    /**
     * Extract Platform id as int. Could be using partial MAC address.
     *
     * @return the Platform id as int, or -1 for unrecognized format
     */
    public int getPlatformId();

    @Override
    public boolean equals(Object o);

    @Override
    public int hashCode();

    @Override
    public int compareTo(E arg0);

}
