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
package fr.gouv.vitam.common.storage.constants;

/**
 * Display logged messages from workspace
 */
public enum StorageMessage {
    /**
     * Beginning of getting List of Digital Object
     */
    BEGINNING_GET_LIST_OF_DIGITAL_OBJECT("Beginning of getting List of Digital Object"),
    /**
     * Beginning of getting Uri List of Digital Object
     */
    BEGINNING_GET_URI_LIST_OF_DIGITAL_OBJECT("Beginning of getting Uri List of Digital Object"),
    /**
     * Beginning of getting Uri List of folders
     */
    BEGINNING_GET_URI_LIST_OF_FOLDER("Beginning of getting Uri List of folders"),
    /**
     * Ending of getting List of Digital Object
     */
    ENDING_GET_LIST_OF_DIGITAL_OBJECT("Ending of getting List of Digital Object"),
    /**
     * Ending of getting Uri List of Digital Object
     */
    ENDING_GET_URI_LIST_OF_DIGITAL_OBJECT("Ending of getting Uri List of Digital Object"),
    /**
     * Ending of getting Uri List of folders
     */
    ENDING_GET_URI_LIST_OF_FOLDER("Ending of getting Uri List of folders"),
    /**
     * Uri list of digital objects empty
     */
    URI_LIST_OF_DIGITAL_OBJECT_EMPTY("Uri list of digital objects empty");

    private final String message;

    private StorageMessage(String message) {
        this.message = message;
    }

    /**
     * getter for attribute message
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }
}
