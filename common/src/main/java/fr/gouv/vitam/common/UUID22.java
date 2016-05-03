/**
 * This file is part of Vitam Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import fr.gouv.vitam.common.exception.InvalidUuidOperationException;

/**
 * UUID Generator (also Global UUID Generator) <br>
 * <br>
 * Inspired from com.groupon locality-uuid which used combination of internal
 * counter value - process id - fragment of MAC address and Timestamp. see
 * https://github.com/groupon/locality-uuid.java <br>
 * <br>
 * But force sequence and take care of errors and improves some performance
 * issues. <br>
 * Moreover it adds a baseId, to separate virtually multiple generators which
 * should have no intersection (such as done in ARK UUID), using a BaseId set
 * for one "instance". <br>
 * This version uses a 22 bytes length version, more precise but incompatible
 * with standard UUID.
 * 
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public final class UUID22 extends UUIDAbstract<UUID22> {
    /**
     * Native size of the UUID
     */
    public static final int KEYSIZE = 22;
    private static final int KEYB64SIZE = 30;
    static final int KEYB32SIZE = 36;
    private static final int KEYB16SIZE = KEYSIZE * 2;
    private static final int HEADER_POS = 0;
    private static final int HEADER_SIZE = 2;
    private static final int DOMAIN_POS = 2;
    private static final int DOMAIN_SIZE = 4;
    private static final int PLATFORM_POS = 6;
    private static final int PLATFORM_SIZE = 4;
    private static final int PID_POS = 10;
    private static final int PID_SIZE = 3;
    private static final int TIME_POS = 13;
    private static final int TIME_SIZE = 6;
    private static final int COUNTER_POS = 19;
    private static final int COUNTER_SIZE = 3;
    /**
     * Version to store (to check correctness if future algorithm) between 0 and
     * 255
     */
    private static final int VERSION = (1 & 0xFF);

    /**
     * @return the KeySize
     */
    public static int getKeySize() {
        return KEYSIZE;
    }

    /**
     * Counter part
     */
    private static volatile int counter = 0;
    /**
     * Counter reset
     */
    private static volatile long lastTimeStamp = 0;

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp with no object type and no domain
     */
    public UUID22() {
        this(0, 0, MACI, false);
    }

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp with no domain
     * 
     * @param objectId
     *            object type id between 0 and 255
     */
    public UUID22(final int objectId) {
        this(objectId, 0, MACI, false);
    }

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp
     * 
     * @param objectId
     *            object type id between 0 and 255
     * @param domainId
     *            domain id between 0 and 2^30-1
     */
    public UUID22(final int objectId, final int domainId) {
        this(objectId, domainId, MACI, false);
    }

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp with no object type and no domain
     * 
     * @param worm
     *            True if Worm UUID
     */
    public UUID22(final boolean worm) {
        this(0, 0, MACI, worm);
    }

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp with no domain
     * 
     * @param objectId
     *            object type id between 0 and 255
     * @param worm
     *            True if Worm UUID
     */
    public UUID22(final int objectId, final boolean worm) {
        this(objectId, 0, MACI, worm);
    }

    /**
     * Constructor that generates a new UUID using the current process id, MAC
     * address and timestamp
     * 
     * @param objectId
     *            object type id between 0 and 255
     * @param domainId
     *            domain id between 0 and 2^30-1
     * @param worm
     *            True if Worm UUID
     */
    public UUID22(final int objectId, final int domainId, final boolean worm) {
        this(objectId, domainId, MACI, worm);
    }

    /**
     * Constructor that generates a new UUID using the current process id and
     * timestamp
     * 
     * @param objectId
     *            object type id between 0 and 255
     * @param domainId
     *            domain id between 0 and 2^30-1
     * @param platformId
     *            platform Id between 0 and 2^31-1
     */
    public UUID22(final int objectId, final int domainId, final int platformId) {
        this(objectId, domainId, platformId, false);
    }

    /**
     * Constructor that generates a new UUID using the current process id and
     * timestamp
     * 
     * @param objectId
     *            object type id between 0 and 255
     * @param domainId
     *            domain id between 0 and 2^30-1
     * @param platformId
     *            platform Id between 0 and 2^31-1
     * @param worm
     *            True if Worm UUID
     */
    public UUID22(final int objectId, final int domainId, final int platformId,
            final boolean worm) {
        super(KEYSIZE);
        if (objectId < 0 || objectId > 0xFF) {
            throw new IllegalArgumentException(
                    "Object Type ID must be between 0 and 255: " + objectId);
        }
        if (domainId < 0 || domainId > 0x3FFFFFFF) {
            throw new IllegalArgumentException(
                    "DomainId must be between 0 and 2^30-1: " + domainId);
        }
        if (platformId < 0 || platformId > 0x7FFFFFFF) {
            throw new IllegalArgumentException(
                    "PlatformId must be between 0 and 2^31-1: " + platformId);
        }

        // atomically
        final long time;
        final int count;
        synchronized (MACHINE_ID_PATTERN) {
            time = System.currentTimeMillis();
            if (lastTimeStamp != time) {
                counter = 0;
                lastTimeStamp = time;
            }
            count = ++counter;
        }
        // 2 bytes = Version (8) + Object Id (8)
        uuid[HEADER_POS] = (byte) VERSION;
        uuid[HEADER_POS + 1] = (byte) (objectId & 0xFF);

        // 4 bytes - 2 bits = Domain (30)
        int value = domainId;
        uuid[DOMAIN_POS + 3] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[DOMAIN_POS + 2] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[DOMAIN_POS + 1] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[DOMAIN_POS] = (byte) (value & 0x3F);

        // 4 bytes = Worm status + Platform (31)
        value = platformId;
        uuid[PLATFORM_POS + 3] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[PLATFORM_POS + 2] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[PLATFORM_POS + 1] = (byte) (value & 0xFF);
        value >>>= 8;
        if (worm) {
            uuid[PLATFORM_POS] = (byte) ((0x80) | (value & 0x7F));
        } else {
            uuid[PLATFORM_POS] = (byte) (value & 0x7F);
        }

        // 3 bytes = -2 bits JVMPID (22)
        value = JVMPID;
        uuid[PID_POS + 2] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[PID_POS + 1] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[PID_POS] = (byte) (value & 0xFF);

        // 7 bytes = timestamp (so up to 8 925 years after Time 0 so year 10
        // 895)
        long lvalue = time;
        uuid[TIME_POS + 5] = (byte) (lvalue & 0xFF);
        lvalue >>>= 8;
        uuid[TIME_POS + 4] = (byte) (lvalue & 0xFF);
        lvalue >>>= 8;
        uuid[TIME_POS + 3] = (byte) (lvalue & 0xFF);
        lvalue >>>= 8;
        uuid[TIME_POS + 2] = (byte) (lvalue & 0xFF);
        lvalue >>>= 8;
        uuid[TIME_POS + 1] = (byte) (lvalue & 0xFF);
        lvalue >>>= 8;
        uuid[TIME_POS] = (byte) (lvalue & 0xFF);

        // 3 bytes = counter against collision
        value = count;
        uuid[COUNTER_POS + 2] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[COUNTER_POS + 1] = (byte) (value & 0xFF);
        value >>>= 8;
        uuid[COUNTER_POS] = (byte) (value & 0xFF);

    }

    /**
     * Constructor that takes a byte array as this UUID's content
     *
     * @param bytes
     *            UUID content
     * @throws InvalidUuidOperationException
     */
    public UUID22(final byte[] bytes) throws InvalidUuidOperationException {
        super(KEYSIZE);
        setBytes(bytes, KEYSIZE);
    }

    /**
     * Build from String key
     *
     * @param idsource
     * @throws InvalidUuidOperationException
     */
    public UUID22(final String idsource) throws InvalidUuidOperationException {
        super(KEYSIZE);
        setString(idsource);
    }

    /**
     * Internal function
     * 
     * @param idsource
     * @return this
     * @throws InvalidUuidOperationException
     */
    @JsonSetter("id")
    protected UUID22 setString(final String idsource)
            throws InvalidUuidOperationException {
        final String id = idsource.trim();
        if (idsource.startsWith(ARK)) {
            String ids = idsource;
            ids = ids.substring(ARK.length());
            int separator = ids.indexOf('/');
            if (separator <= 0) {
                throw new InvalidUuidOperationException(
                        "Attempted to parse malformed ARK UUID: " + id);
            }
            int domainId = Integer.parseInt(ids.substring(0, separator));
            // BASE32
            ids = ids.substring(separator + 1);
            byte[] base32 = BaseXx.getFromBase32(ids);
            if (base32.length != KEYSIZE - DOMAIN_SIZE) {
                throw new InvalidUuidOperationException(
                        "Attempted to parse malformed ARK UUID: " + id);
            }
            System.arraycopy(base32, 0, uuid, HEADER_POS, HEADER_SIZE);
            // UUID Domain default to 0 (from 0 to 2^30-1)
            uuid[DOMAIN_POS + 3] = (byte) (domainId & 0xFF);
            domainId >>>= 8;
            uuid[DOMAIN_POS + 2] = (byte) (domainId & 0xFF);
            domainId >>>= 8;
            uuid[DOMAIN_POS + 1] = (byte) (domainId & 0xFF);
            domainId >>>= 8;
            uuid[DOMAIN_POS] = (byte) (domainId & 0x3F);
            // BASE32
            System.arraycopy(base32, HEADER_SIZE, uuid, PLATFORM_POS,
                    PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE);
            return this;
        }
        final int len = id.length();
        if (len == KEYB16SIZE) {
            // HEXA BASE16
            System.arraycopy(BaseXx.getFromBase16(idsource), 0, uuid, 0, KEYSIZE);
        } else if (len == KEYB32SIZE) {
            // BASE32
            System.arraycopy(BaseXx.getFromBase32(idsource), 0, uuid, 0, KEYSIZE);
        } else if (len == KEYB64SIZE) {
            // BASE64
            System.arraycopy(BaseXx.getFromBase64(idsource), 0, uuid, 0, KEYSIZE);
        } else {
            throw new InvalidUuidOperationException(
                    "Attempted to parse malformed UUID: (" + len + ") " + id);
        }
        return this;
    }

    @Override
    @JsonIgnore
    public final int getVersion() {
        return (uuid[HEADER_POS] & 0xFF);
    }

    @Override
    @JsonIgnore
    public final int getObjectId() {
        return (uuid[HEADER_POS + 1] & 0xFF);
    }

    @Override
    @JsonIgnore
    public final int getDomainId() {
        return ((uuid[DOMAIN_POS] & 0x3F) << 24) | ((uuid[DOMAIN_POS + 1] & 0xFF) << 16) |
                ((uuid[DOMAIN_POS + 2] & 0xFF) << 8) | (uuid[DOMAIN_POS + 3] & 0xFF);
    }

    @Override
    @JsonIgnore
    public final boolean isWorm() {
        return (uuid[PLATFORM_POS] & 0x80) != 0;
    }

    @Override
    @JsonIgnore
    public final int getPlatformId() {
        return ((uuid[PLATFORM_POS] & 0x7F) << 24)
                | ((uuid[PLATFORM_POS + 1] & 0xFF) << 16) |
                ((uuid[PLATFORM_POS + 2] & 0xFF) << 8) | (uuid[PLATFORM_POS + 3] & 0xFF);
    }

    @Override
    @JsonIgnore
    public final byte[] getMacFragment() {
        if (getVersion() != VERSION) {
            return null;
        }
        final byte[] x = new byte[6];
        x[0] = 0;
        x[1] = 0;
        x[2] = (byte) (uuid[PLATFORM_POS] & 0x7F);
        x[3] = uuid[PLATFORM_POS + 1];
        x[4] = uuid[PLATFORM_POS + 2];
        x[5] = uuid[PLATFORM_POS + 3];
        return x;
    }

    @Override
    @JsonIgnore
    public final int getProcessId() {
        if (getVersion() != VERSION) {
            return -1;
        }
        return ((uuid[PID_POS] & 0xFF) << 16) | ((uuid[PID_POS + 1] & 0xFF) << 8) |
                (uuid[PID_POS + 2] & 0xFF);
    }

    @Override
    @JsonIgnore
    public final long getTimestamp() {
        if (getVersion() != VERSION) {
            return -1;
        }
        long time = 0;
        for (int i = 0; i < TIME_SIZE; i++) {
            time <<= 8;
            time |= uuid[TIME_POS + i] & 0xFF;
        }
        return time;
    }

    @Override
    @JsonIgnore
    public final int getCounter() {
        return (((uuid[COUNTER_POS] & 0xFF) << 16) |
                ((uuid[COUNTER_POS + 1] & 0xFF) << 8) |
                (uuid[COUNTER_POS + 2] & 0xFF));
    }

    @Override
    @JsonIgnore
    public final String toArkName() {
        byte[] temp = new byte[KEYSIZE - DOMAIN_SIZE];
        System.arraycopy(uuid, HEADER_POS, temp, 0, HEADER_SIZE);
        System.arraycopy(uuid, PLATFORM_POS, temp, HEADER_SIZE,
                PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE);
        return BaseXx.getBase32(temp);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof UUID22)) {
            return false;
        }
        return (this == o) || Arrays.equals(uuid, ((UUID22) o).uuid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(uuid);
    }

    @Override
    public int compareTo(final UUID22 arg1) {
        int id = getDomainId();
        int id2 = arg1.getDomainId();
        if (id != id2) {
            return id < id2 ? -1 : 1;
        }
        id = getObjectId();
        id2 = arg1.getObjectId();
        if (id != id2) {
            return id < id2 ? -1 : 1;
        }
        long ts = getTimestamp();
        long ts2 = arg1.getTimestamp();
        if (ts == ts2) {
            int ct = getCounter();
            int ct2 = arg1.getCounter();
            if (ct == ct2) {
                // then all must be equals, else whatever
                return Arrays.equals(uuid, arg1.uuid) ? 0 : -1;
            }
            // Cannot be equal
            return ct < ct2 ? -1 : 1;
        }
        // others as ProcessId or Platform are unimportant in comparison
        return ts < ts2 ? -1 : 1;
    }

}
