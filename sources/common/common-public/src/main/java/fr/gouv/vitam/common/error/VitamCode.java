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

package fr.gouv.vitam.common.error;

import javax.ws.rs.core.Response.Status;

/**
 * List of Vitam errors.
 *
 * The error code is composed by the service code, the domain code and the item (alphanumeric, uppercase). For one
 * error, there is one HTTP status and one configurable message (String.format style).
 *
 * Use the CodeTest unit test when you add an entry to validate it (avoid duplicate error code or wrong error code)
 *
 * Do not remove the TEST error.
 */
@SuppressWarnings("javadoc")
public enum VitamCode {

    /**
     * ONLY FOR TEST PURPOSE (do not remove)
     */
    TEST(ServiceName.VITAM, DomainName.TEST, "00", Status.INTERNAL_SERVER_ERROR, "message or message key and " +
        "parameter %s"),

    GLOBAL_INTERNAL_SERVER_ERROR(ServiceName.VITAM, DomainName.NETWORK, "00", Status.INTERNAL_SERVER_ERROR,
        "Internal Server Error"),

    STORAGE_MISSING_HEADER(ServiceName.STORAGE, DomainName.ILLEGAL, "00", Status.PRECONDITION_FAILED, "Header are " +
        "missing"),
    STORAGE_BAD_REQUEST(ServiceName.STORAGE, DomainName.ILLEGAL, "01", Status.PRECONDITION_FAILED,
        "Storage Engine received a bad request "),
    STORAGE_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "00", Status.NOT_FOUND, "Storage not found"),
    STORAGE_TECHNICAL_INTERNAL_ERROR(ServiceName.STORAGE, DomainName.STORAGE, "01", Status.INTERNAL_SERVER_ERROR,
        "Storage technical" +
            " error"),
    STORAGE_STRATEGY_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "02", Status.NOT_FOUND, "No suitable strategy" +
        " found to be able to store data"),
    STORAGE_OFFER_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "03", Status.NOT_FOUND, "No suitable offer found" +
        " to be able to store data"),
    STORAGE_OBJECT_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "04", Status.NOT_FOUND, "Object with id %s not " +
        "found in all strategy"),
    STORAGE_CANT_STORE_OBJECT(ServiceName.STORAGE, DomainName.STORAGE, "05", Status.INTERNAL_SERVER_ERROR, "Could not" +
        " store object with id '%s' on offers '%s'"),
    STORAGE_LOGBOOK_CANNOT_LOG(ServiceName.STORAGE, DomainName.STORAGE, "06", Status.INTERNAL_SERVER_ERROR,
        "Operation couldnt " +
            "be logged in the storage logbook"),
    STORAGE_CLIENT_UNKNOWN(ServiceName.STORAGE, DomainName.ILLEGAL, "07", Status.INTERNAL_SERVER_ERROR,
        "Storage client type " +
            "unknown"),
    STORAGE_CLIENT_STORAGE_TYPE(ServiceName.STORAGE, DomainName.ILLEGAL, "08", Status.INTERNAL_SERVER_ERROR, "Type of" +
        " storage object cannot be %s"),
    STORAGE_CLIENT_ALREADY_EXISTS(ServiceName.STORAGE, DomainName.ILLEGAL, "09", Status.CONFLICT, "%s"),
    STORAGE_DRIVER_MAPPER_FILE_CONTENT(ServiceName.STORAGE, DomainName.IO, "10", Status.INTERNAL_SERVER_ERROR,
        "Cannot retrieve file content for driver %s, that's an error !"),
    STORAGE_DRIVER_MAPPING_SAVE(ServiceName.STORAGE, DomainName.IO, "11", Status.INTERNAL_SERVER_ERROR, "Cannot save " +
        "driver %s mapping !"),
    STORAGE_DRIVER_MAPPING_INITIALIZE(ServiceName.STORAGE, DomainName.IO, "12", Status.INTERNAL_SERVER_ERROR, "Cannot" +
        " initialize FileDriverMapper, error on configuration file, please check it"),
    STORAGE_DRIVER_OBJECT_ALREADY_EXISTS(ServiceName.STORAGE, DomainName.ILLEGAL, "13", Status.METHOD_NOT_ALLOWED,
        "Cannot override an existing object (%s)"),

    STORAGE_CONTAINER_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "13", Status.NOT_FOUND,
        "Container with name %s not " +
            "found in all strategy"),

    WORKER_FORMAT_IDENTIFIER_NOT_FOUND(ServiceName.WORKER, DomainName.IO, "00", Status.NOT_FOUND, "Format identifier " +
        "%s not found"),
    WORKER_FORMAT_IDENTIFIER_IMPLEMENTATION_NOT_FOUND(ServiceName.WORKER, DomainName.IO, "01", Status.NOT_FOUND,
        "Format " +
            "identifier %s implementation not found"),
    WORKER_FORMAT_IDENTIFIER_TECHNICAL_INTERNAL_ERROR(ServiceName.WORKER, DomainName.IO, "02",
        Status.INTERNAL_SERVER_ERROR,
        "Format identifier internal error"),

    CONTRACT_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "08",
        Status.BAD_REQUEST,
        "Request validation error"),

    PROFILE_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "09",
        Status.BAD_REQUEST,
        "Request profile validation error"),

    PROFILE_FILE_IMPORT_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "10",
        Status.BAD_REQUEST,
        "Request profile file import error");

    private final ServiceName service;
    private final DomainName domain;
    private final String item;
    private final Status status;
    private final String message;

    VitamCode(ServiceName service, DomainName domain, String item, Status status, String message) {
        this.service = service;
        this.domain = domain;
        this.item = item;
        this.status = status;
        this.message = message;
    }

    public ServiceName getService() {
        return service;
    }

    public DomainName getDomain() {
        return domain;
    }

    public String getItem() {
        return item;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
