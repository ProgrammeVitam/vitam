/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.guid;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;

import java.util.Arrays;

/**
 * GUID Read only Abstract implementation *
 */
abstract class GUIDAbstract implements GUID {

    /**
     * ARK header
     */
    public static final String ARK = "ark:/";

    /**
     * real GUID
     */
    @JsonIgnore
    final byte[] guid;

    /**
     * Internal constructor
     *
     * @param size size of the byte representation
     */
    GUIDAbstract(int size) {
        guid = new byte[size];
    }

    /**
     * Internal function
     *
     * @param bytes
     * @param size size of the byte representation
     * @return this
     * @throws InvalidGuidOperationException
     */
    @JsonIgnore
    GUIDAbstract setBytes(final byte[] bytes, int size) throws InvalidGuidOperationException {
        if (bytes == null) {
            throw new InvalidGuidOperationException("Empty argument");
        }
        if (bytes.length != size) {
            throw new InvalidGuidOperationException(
                "Attempted to parse malformed GUID: (" + bytes.length + ") " + Arrays.toString(bytes)
            );
        }
        System.arraycopy(bytes, 0, guid, 0, size);
        return this;
    }

    @Override
    @JsonIgnore
    public String toBase32() {
        return BaseXx.getBase32(guid);
    }

    @Override
    @JsonIgnore
    public String toBase64() {
        return BaseXx.getBase64UrlWithoutPadding(guid);
    }

    @Override
    @JsonIgnore
    public String toHex() {
        return BaseXx.getBase16(guid);
    }

    @Override
    @JsonIgnore
    public String toArk() {
        return new StringBuilder(ARK).append(getTenantId()).append('/').append(toArkName()).toString();
    }

    @Override
    @JsonGetter("id")
    public String getId() {
        return toString();
    }

    @Override
    public String toString() {
        return toBase32();
    }

    @Override
    @JsonIgnore
    public byte[] getBytes() {
        return Arrays.copyOf(guid, guid.length);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Arrays.hashCode(guid);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof GUIDAbstract)) {
            return false;
        }
        return this == o || Arrays.equals(guid, ((GUIDAbstract) o).guid);
    }
}
