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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * GUID Interface
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface GUID extends Comparable<GUID> {
    /**
     * @return True if is Worm
     */
    @JsonIgnore
    boolean isWorm();

    /**
     * @return the Base32 representation (default of toString)
     */
    @JsonIgnore
    String toBase32();

    /**
     * @return the Base64 representation (default of toString)
     */
    @JsonIgnore
    String toBase64();

    /**
     * @return the Hexadecimal representation
     */
    @JsonIgnore
    String toHex();

    /**
     * @return the Ark representation of this GUID
     */
    @JsonIgnore
    String toArk();

    /**
     * @return the Ark Name part of Ark representation
     */
    @JsonIgnore
    default String toArkName() {
        return toString();
    }

    @Override
    String toString();

    /**
     * copy the uuid of this GUID, so that it can't be changed, and return it
     *
     * @return raw byte array of GUID
     */
    @JsonIgnore
    byte[] getBytes();

    /**
     * extract version field as a hex char from raw GUID bytes
     *
     * @return version char
     */
    @JsonIgnore
    int getVersion();

    /**
     * @return the id of the Object Type (default being 0)
     */
    @JsonIgnore
    default int getObjectId() {
        return 0;
    }

    /**
     * @return the Tenant Id of GUID from which it belongs to (default being 0)
     */
    @JsonIgnore
    default int getTenantId() {
        return 0;
    }

    /**
     * Extract process id and return as int
     *
     * @return id of process that generated the GUID, or -1 for unrecognized format
     */
    @JsonIgnore
    int getProcessId();

    /**
     * @return the associated counter against collision value
     */
    @JsonIgnore
    int getCounter();

    /**
     * Extract timestamp and return as long
     *
     * @return millisecond UTC timestamp from generation of the GUID, or -1 for unrecognized format
     */
    @JsonIgnore
    long getTimestamp();

    /**
     * Extract Platform id as bytes. Could be using partial MAC address.
     *
     * @return byte array of GUID fragment, or null for unrecognized format
     */
    @JsonIgnore
    byte[] getMacFragment();

    /**
     * Extract Platform id as int. Could be using partial MAC address.
     *
     * @return the Platform id as int, or -1 for unrecognized format
     */
    @JsonIgnore
    int getPlatformId();

    /**
     * @return the String representation of this GUID
     */
    @JsonGetter("id")
    String getId();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    int compareTo(GUID arg0);
}
