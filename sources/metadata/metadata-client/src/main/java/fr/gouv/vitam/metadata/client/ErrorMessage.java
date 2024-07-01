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
package fr.gouv.vitam.metadata.client;

/**
 * Error Message for Metadata
 */
public enum ErrorMessage {
    /**
     * message when select units query is null
     */
    SELECT_UNITS_QUERY_NULL("Select units query is null"),
    /**
     * message when select units query bulk is null
     */
    SELECT_UNITS_QUERY_BULK_NULL("Select units query bulk is null"),
    /**
     * message when select objectgroup query is null
     */
    SELECT_OBJECT_GROUP_QUERY_NULL("Select object group query is null"),
    /**
     * message when update units query is null
     */
    UPDATE_UNITS_QUERY_NULL("Update units query is null"),
    /**
     * message when insert units query bulk is null
     */
    UPDATE_UNITS_QUERY_BULK_NULL("Update units query bulk is null"),
    /**
     * message when update object group query is null
     */
    UPDATE_OBJECT_GROUP_QUERY_NULL("Update object group query is null"),

    /**
     * message when insert units query is null
     */
    INSERT_UNITS_QUERY_NULL("Insert units query is null"),
    /**
     * message when unit id parameter is blank
     */
    BLANK_PARAM("Unit id parameter is blank"),
    /**
     * message when internal server error occured
     */
    INTERNAL_SERVER_ERROR("Internal Server Error"),
    /**
     * message when document size of unit is too large
     */
    SIZE_TOO_LARGE("Document Size is Too Large"),
    /**
     * message when parsing an invalid json
     */
    INVALID_PARSE_OPERATION("Invalid Parse Operation"),

    /**
     * when invalid metadata values
     */
    INVALID_METADATA_VALUE("Invalid metadata values"),

    /**
     * when missing select query
     */
    MISSING_SELECT_QUERY("Missing Select Query"),
    /**
     * message when data insert/update exists
     */
    DATA_ALREADY_EXISTS("Data Already Exists"),
    /**
     * message when querying data not found
     */
    NOT_FOUND("Not Found Exception");

    private final String message;

    private ErrorMessage(String message) {
        this.message = message;
    }

    /**
     * @return the associated message
     */
    public String getMessage() {
        return message;
    }
}
