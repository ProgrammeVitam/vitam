/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
package fr.gouv.vitam.common.guid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;

import java.util.Arrays;

/**
 * GUID Reader (Global Unique Identifier Reader) <br>
 */
class GUIDImpl extends GUIDAbstract {

    private static final String ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID = "Attempted to parse malformed ARK GUID: ";
    /**
     * Native size of the GUID
     */
    static final int KEYSIZE = 22;
    static final int KEYB64SIZE = 30;
    static final int KEYB32SIZE = 36;
    static final int KEYB16SIZE = KEYSIZE * 2;
    static final int HEADER_POS = 0;
    static final int HEADER_SIZE = 2;
    static final int TENANT_POS = 2;
    static final int TENANT_SIZE = 4;
    static final int PLATFORM_POS = 6;
    static final int PLATFORM_SIZE = 4;
    static final int PID_POS = 10;
    static final int PID_SIZE = 3;
    static final int TIME_POS = 13;
    static final int TIME_SIZE = 6;
    static final int COUNTER_POS = 19;
    static final int COUNTER_SIZE = 3;
    /**
     * Version to store (to check correctness if future algorithm) between 0 and 255
     */
    static final int VERSION = 1 & 0xFF;

    static final int BYTE_SIZE = 8;

    /**
     * Empty constructor for Json support
     */
    GUIDImpl() {
        super(KEYSIZE);
    }

    /**
     * Constructor that takes a byte array as this GUID's content
     *
     * @param bytes GUID content
     * @throws InvalidGuidOperationException
     */
    GUIDImpl(final byte[] bytes) throws InvalidGuidOperationException {
        super(KEYSIZE);
        setBytes(bytes, KEYSIZE);
        if (getVersion() != VERSION) {
            throw new InvalidGuidOperationException("Version is incorrect: " + getVersion());
        }
    }

    /**
     * Build from String key
     *
     * @param idsource
     * @throws InvalidGuidOperationException
     */
    GUIDImpl(final String idsource) throws InvalidGuidOperationException {
        super(KEYSIZE);
        setString(idsource);
        if (getVersion() != VERSION) {
            throw new InvalidGuidOperationException("Version is incorrect: " + getVersion());
        }
    }

    /**
     * @return the KeySize
     */
    public static int getKeySize() {
        return KEYSIZE;
    }

    /**
     * Internal function
     *
     * @param idsource
     * @return this
     * @throws InvalidGuidOperationException
     */
    @JsonSetter("id")
    GUIDImpl setString(final String idsource) throws InvalidGuidOperationException {
        if (idsource == null) {
            throw new InvalidGuidOperationException("Empty argument");
        }
        final String id = idsource.trim();
        if (idsource.startsWith(ARK)) {
            String ids = idsource;
            ids = ids.substring(ARK.length());
            final int separator = ids.indexOf('/');
            if (separator <= 0) {
                throw new InvalidGuidOperationException(ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
            }
            int tenantId;
            try {
                tenantId = Integer.parseInt(ids.substring(0, separator));
            } catch (final NumberFormatException e) {
                throw new InvalidGuidOperationException(ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
            }
            // BASE32
            ids = ids.substring(separator + 1);
            final byte[] base32 = BaseXx.getFromBase32(ids);
            if (base32.length != KEYSIZE - TENANT_SIZE) {
                throw new InvalidGuidOperationException(ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
            }
            System.arraycopy(base32, 0, guid, HEADER_POS, HEADER_SIZE);
            // GUID Domain default to 0 (from 0 to 2^30-1)
            guid[TENANT_POS + 3] = (byte) (tenantId & 0xFF);
            tenantId >>>= BYTE_SIZE;
            guid[TENANT_POS + 2] = (byte) (tenantId & 0xFF);
            tenantId >>>= BYTE_SIZE;
            guid[TENANT_POS + 1] = (byte) (tenantId & 0xFF);
            tenantId >>>= BYTE_SIZE;
            guid[TENANT_POS] = (byte) (tenantId & 0x3F);
            // BASE32
            System.arraycopy(
                base32,
                HEADER_SIZE,
                guid,
                PLATFORM_POS,
                PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE
            );
            return this;
        }
        final int len = id.length();
        try {
            if (len == KEYB16SIZE) {
                // HEXA BASE16
                System.arraycopy(BaseXx.getFromBase16(idsource), 0, guid, 0, KEYSIZE);
            } else if (len == KEYB32SIZE) {
                // BASE32
                System.arraycopy(BaseXx.getFromBase32(idsource), 0, guid, 0, KEYSIZE);
            } else if (len == KEYB64SIZE) {
                // BASE64
                System.arraycopy(BaseXx.getFromBase64UrlWithoutPadding(idsource), 0, guid, 0, KEYSIZE);
            } else {
                throw new InvalidGuidOperationException("Attempted to parse malformed GUID: (" + len + ") " + id);
            }
        } catch (final IllegalArgumentException e) {
            throw new InvalidGuidOperationException("Attempted to parse malformed GUID: " + id, e);
        }
        return this;
    }

    @Override
    @JsonIgnore
    public final int getVersion() {
        return guid[HEADER_POS] & 0xFF;
    }

    @Override
    @JsonIgnore
    public final int getObjectId() {
        return guid[HEADER_POS + 1] & 0xFF;
    }

    @Override
    @JsonIgnore
    public final int getTenantId() {
        return (
            ((guid[TENANT_POS] & 0x3F) << (BYTE_SIZE * 3)) |
            ((guid[TENANT_POS + 1] & 0xFF) << (BYTE_SIZE * 2)) |
            ((guid[TENANT_POS + 2] & 0xFF) << BYTE_SIZE) |
            (guid[TENANT_POS + 3] & 0xFF)
        );
    }

    @Override
    @JsonIgnore
    public final boolean isWorm() {
        return (guid[PLATFORM_POS] & 0x80) != 0;
    }

    @Override
    @JsonIgnore
    public final int getPlatformId() {
        return (
            ((guid[PLATFORM_POS] & 0x7F) << (BYTE_SIZE * 3)) |
            ((guid[PLATFORM_POS + 1] & 0xFF) << (BYTE_SIZE * 2)) |
            ((guid[PLATFORM_POS + 2] & 0xFF) << BYTE_SIZE) |
            (guid[PLATFORM_POS + 3] & 0xFF)
        );
    }

    @Override
    @JsonIgnore
    public final byte[] getMacFragment() {
        if (getVersion() != VERSION) {
            return new byte[0];
        }
        final byte[] x = new byte[6];
        x[0] = 0;
        x[1] = 0;
        x[2] = (byte) (guid[PLATFORM_POS] & 0x7F);
        x[3] = guid[PLATFORM_POS + 1];
        x[4] = guid[PLATFORM_POS + 2];
        x[5] = guid[PLATFORM_POS + 3];
        return x;
    }

    @Override
    @JsonIgnore
    public final int getProcessId() {
        if (getVersion() != VERSION) {
            return -1;
        }
        return (
            ((guid[PID_POS] & 0xFF) << (BYTE_SIZE * 2)) |
            ((guid[PID_POS + 1] & 0xFF) << BYTE_SIZE) |
            (guid[PID_POS + 2] & 0xFF)
        );
    }

    @Override
    @JsonIgnore
    public final long getTimestamp() {
        if (getVersion() != VERSION) {
            return -1;
        }
        long time = 0;
        for (int i = 0; i < TIME_SIZE; i++) {
            time <<= BYTE_SIZE;
            time |= guid[TIME_POS + i] & 0xFF;
        }
        return time;
    }

    @Override
    @JsonIgnore
    public final int getCounter() {
        return (
            ((guid[COUNTER_POS] & 0xFF) << (BYTE_SIZE * 2)) |
            ((guid[COUNTER_POS + 1] & 0xFF) << BYTE_SIZE) |
            (guid[COUNTER_POS + 2] & 0xFF)
        );
    }

    @Override
    @JsonIgnore
    public final String toArkName() {
        final byte[] temp = new byte[KEYSIZE - TENANT_SIZE];
        System.arraycopy(guid, HEADER_POS, temp, 0, HEADER_SIZE);
        System.arraycopy(guid, PLATFORM_POS, temp, HEADER_SIZE, PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE);
        return BaseXx.getBase32(temp);
    }

    @Override
    public int compareTo(final GUID arg1) {
        int id = getTenantId();
        int id2 = arg1.getTenantId();
        if (id != id2) {
            return id < id2 ? -1 : 1;
        }
        id = getObjectId();
        id2 = arg1.getObjectId();
        if (id != id2) {
            return id < id2 ? -1 : 1;
        }
        final long ts = getTimestamp();
        final long ts2 = arg1.getTimestamp();
        if (ts == ts2) {
            final int ct = getCounter();
            final int ct2 = arg1.getCounter();
            if (ct == ct2) {
                // then all must be equals, else whatever
                return Arrays.equals(guid, arg1.getBytes()) ? 0 : -1;
            }
            // Cannot be equal
            return ct < ct2 ? -1 : 1;
        }
        // others as ProcessId or Platform are unimportant in comparison
        return ts < ts2 ? -1 : 1;
    }
}
