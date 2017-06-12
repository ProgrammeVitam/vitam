/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/

package fr.gouv.vitam.common.model;

/**
 * Enum of logbook operation keys
 *
 */
public enum LogbookOperationKey {
    /**
     * STP_STORAGE_SECURISATION : key used when a storage logbook securing event occurs
     */
    STP_STORAGE_SECURISATION;

    /**
     *
     * @return ok key associated to the key name
     */
    public String ok() {
        return concat(name(), StatusCode.OK);
    }

    /**
     *
     * @return the ko key associated to the key name
     */
    public String ko() {
        return concat(name(), StatusCode.KO);
    }

    /**
     *
     * @return the warning key associated to the key name
     */
    public String warning() {
        return concat(name(), StatusCode.WARNING);
    }

    /**
     *
     * @return the started key associated to the key name
     */
    public String started() {
        return concat(name(), StatusCode.STARTED);
    }

    /**
     *
     * @return the fatal key associated to the key name
     */
    public String fatal() {
        return concat(name(), StatusCode.FATAL);
    }

    /**
     *
     * @return the unknown key associated to the key name
     */
    public String unknown() {
        return concat(name(), StatusCode.UNKNOWN);
    }

    /**
     *
     * @param name the key name
     * @param statusCode the status code
     * @return the concatenation of the key and its associated status code
     */
    private String concat(String name, StatusCode statusCode) {
        return name + "." + statusCode;
    }

}
