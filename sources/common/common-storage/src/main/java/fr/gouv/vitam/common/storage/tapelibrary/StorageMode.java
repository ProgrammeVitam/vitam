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
package fr.gouv.vitam.common.storage.tapelibrary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the operation mode of the Cold Storage service.
 * LOCAL: the service uses local storage and tape access directly.
 * REMOTE: the service uses a remote REST API via a Resteasy client.
 */
public enum StorageMode {
    /** Local implementation (file system, tape, etc.) */
    LOCAL,

    /** Remote implementation (REST client) */
    INA;

    /**
     * Parses a string value (case-insensitive) into a StorageMode.
     *
     * @param value the string representation (e.g. "local" or "remote")
     * @return the corresponding StorageMode
     * @throws IllegalArgumentException if the value is invalid
     */
    @JsonCreator
    public static StorageMode fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Storage mode cannot be null");
        }
        return switch (value.trim().toLowerCase()) {
            case "local" -> LOCAL;
            case "ina" -> INA;
            default -> throw new IllegalArgumentException("Unknown storage mode: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
