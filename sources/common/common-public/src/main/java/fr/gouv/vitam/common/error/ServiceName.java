/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
 * Enum of Vitam services
 */
public enum ServiceName {

    /**
     * Used if the service does not exist. Also used in test.
     */
    VITAM("00", "Vitam"),

    /**
     * Used for internal access error
     */
    INTERNAL_ACCESS("01", "Internal Access"),

    /**
     * Used for external access error
     */
    EXTERNAL_ACCESS("02", "External Access"),

    /**
     * Used for internal ingest error
     */
    INTERNAL_INGEST("03", "Internal Ingest"),

    /**
     * used for external ingest error
     */
    EXTERNAL_INGEST("04", "External Ingest"),

    /**
     * Use for logbook error
     */
    LOGBOOK("05", "Logbook"),

    /**
     * Used for metadata error
     */
    METADATA("06", "Metadata"),

    /**
     * Used for processing error
     */
    PROCESSING("07", "Processing"),

    /**
     * Used for distribution error
     */
    DISTRIBUTOR("08", "Distributor"),

    /**
     * Used for worker error
     */
    WORKER("09", "Worker"),

    /**
     * Used for storage error
     */
    STORAGE("10", "Storage"),

    /**
     * Used for workspace error
     */
    WORKSPACE("11", "Workspace"),

    /**
     * Used for Functional Administration error
     */
    FUNCTIONAL_ADMINISTRATION("12", "Functional_Administration");


    private final String code;
    private final String name;

    ServiceName(String code, String name) {
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
     * Retrieve ServiceName from code
     *
     * @param code the code
     * @return the service if exists
     * @throws IllegalArgumentException thrown if code is null or empty or if the attached service to the code does not
     *         exist
     */
    public static ServiceName getFromCode(String code) {
        ParametersChecker.checkParameter("code is required", code);
        for (final ServiceName service : values()) {
            if (service.getCode().equals(code)) {
                return service;
            }
        }
        throw new IllegalArgumentException("Code {" + code + "} does not exist");
    }
}
