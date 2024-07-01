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

package fr.gouv.vitam.common.error;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Enum of Vitam domains
 */
public enum DomainName {
    /**
     * ONLY FOR TEST PURPOSE (do not remove)
     */
    TEST("00", "For test purpose ONLY"),

    /**
     * Used for: FileNotFound FileAlreadyExists Json*Exception InvalidParse...
     */
    IO("01", "Input / Output"),

    /**
     * Used for: Server problems Client problems Network anomalies...
     */
    NETWORK("02", "Network"),

    /**
     * Used for: IllegalArgument UnsupportedOperation Schema XML...
     */
    ILLEGAL("03", "Illegal"),

    /**
     * Used for: ReferentialException DatabaseConflict...
     */
    DATABASE("04", "Database"),

    /**
     * Used for: No space left StorageNotFound...
     */
    STORAGE("05", "Storage"),

    /**
     * Used for business anomalies
     */
    BUSINESS("06", "Business"),

    /**
     * Used for: Permissions anomalies Security problems
     */
    SECURITY("07", "Security"),

    VALIDATION("08", "Validation");

    private final String code;
    private final String name;

    DomainName(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * @return code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve DomainName from code
     *
     * @param code the code
     * @return the domain if exists
     * @throws IllegalArgumentException thrown if code is null or empty or if the attached domain to the code does not
     * exist
     */
    public static DomainName getFromCode(String code) {
        ParametersChecker.checkParameter("code is required", code);
        for (final DomainName domain : values()) {
            if (domain.getCode().equals(code)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("Code {" + code + "} does not exist");
    }
}
