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
 * Enum that describes error messages due the workspace
 */
public enum ErrorMessage {
    /**
     * Container already exist
     */
    CONTAINER_ALREADY_EXIST("Container already exist "),
    /**
     * CONTAINER Container not found FOUND
     */
    CONTAINER_NOT_FOUND("Container not found "),

    /**
     * Folder already exist
     */
    FOLDER_ALREADY_EXIST("Folder already exist "),
    /**
     * Folder not found
     */
    FOLDER_NOT_FOUND("Folder not found "),

    /**
     * Object already exist
     */
    OBJECT_ALREADY_EXIST("Object already exist "),

    /**
     * Object not found
     */
    OBJECT_NOT_FOUND("Object not found "),

    /**
     * Input stream is null
     */
    STREAM_IS_NULL("Input stream is null"),

    /**
     * Container name is a mandatory parameter
     */
    CONTAINER_NAME_IS_A_MANDATORY_PARAMETER("Container name is a mandatory parameter"),

    /**
     * Container name and Folder name are a mandatory parameter
     */
    CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER("Container name and Folder name are a mandatory parameter"),
    /**
     * Container name and Object name are a mandatory parameter
     */
    CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER("Container name and Object name are a mandatory parameter"),
    /**
     * Container name, Object name and size are a mandatory parameter
     */
    CONTAINER_OBJECT_NAMES_SIZE_ARE_A_MANDATORY_PARAMETER(
        "Container name, Object name and Size are a mandatory parameter"
    ),
    /**
     * Internal Server Error
     */
    INTERNAL_SERVER_ERROR("Internal Server Error"),

    /**
     * Algo name is a mandatory parameter
     */
    ALGO_IS_A_MANDATORY_PARAMETER("Digest Algo name is a mandatory parameter"),

    /**
     * When zip contains file or folder with not allowed name example : (test.txt#)
     */
    FOLDER_OR_FILE_NAME_NOT_ALLOWED("File or folder not allowed name"),

    /**
     * Bad Request
     */
    BAD_REQUEST("Bad request"),

    /**
     * Not allowed to perform action
     */
    NOT_ACCEPTABLE_FILES("File or folder name not authorized");

    private final String message;

    private ErrorMessage(String message) {
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

    @Override
    public String toString() {
        return message;
    }
}
