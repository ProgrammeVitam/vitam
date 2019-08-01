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

    GLOBAL_EMPTY_QUERY(ServiceName.VITAM, DomainName.ILLEGAL, "00", Status.FORBIDDEN,
        "No search query specified, this is mandatory"),

    GLOBAL_INVALID_DSL(ServiceName.VITAM, DomainName.BUSINESS, "01", Status.BAD_REQUEST, "Dsl query is not valid."),

    INTERNAL_SECURITY_UNAUTHORIZED(ServiceName.VITAM, DomainName.SECURITY, "00", Status.UNAUTHORIZED,
        "Internal Security Filter Unauthorized"),

    STORAGE_MISSING_HEADER(ServiceName.STORAGE, DomainName.ILLEGAL, "00", Status.PRECONDITION_FAILED, "Header are " +
        "missing"),
    STORAGE_BAD_REQUEST(ServiceName.STORAGE, DomainName.ILLEGAL, "01", Status.PRECONDITION_FAILED,
        "Storage Engine received a bad request "),
    STORAGE_STRATEGY_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "02", Status.NOT_FOUND, "No suitable strategy" +
        " found to be able to store data"),
    STORAGE_OFFER_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "03", Status.NOT_FOUND, "No suitable offer found" +
        " to be able to read or store data"),
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
    STORAGE_CONTAINER_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "14", Status.NOT_FOUND,
        "Container with name %s not " +
            "found in all strategy"),
    STORAGE_GET_OFFER_LOG_ERROR(ServiceName.STORAGE, DomainName.STORAGE, "15", Status.INTERNAL_SERVER_ERROR,
        "Cannot retrieve objects stored on container"),
    STORAGE_NOT_FOUND(ServiceName.STORAGE, DomainName.STORAGE, "16", Status.NOT_FOUND, "Storage not found"),
    STORAGE_TECHNICAL_INTERNAL_ERROR(ServiceName.STORAGE, DomainName.STORAGE, "17", Status.INTERNAL_SERVER_ERROR,
        "Storage technical error"),
    STORAG_EMPTY_PARAMTER(ServiceName.STORAGE, DomainName.STORAGE, "18", Status.PRECONDITION_FAILED,
        "No offer identifier specified, this is mandatory"),
    STORAGE_INCONSISTENT_STATE(ServiceName.STORAGE, DomainName.STORAGE, "19", Status.CONFLICT,
        "Inconsistent state"),

    STORAGE_GET_READ_ORDER_ERROR(ServiceName.STORAGE, DomainName.STORAGE, "20", Status.INTERNAL_SERVER_ERROR,
        "Cannot retrieve read order request"),
    STORAGE_CREATE_READ_ORDER_ERROR(ServiceName.STORAGE, DomainName.STORAGE, "21", Status.INTERNAL_SERVER_ERROR,
        "Cannot create read order request"),

    WORKSPACE_NOT_ACCEPTABLE_FILES(ServiceName.WORKSPACE, DomainName.STORAGE, "14", Status.NOT_ACCEPTABLE,
        "File or folder name not authorized"),
    WORKSPACE_BAD_REQUEST(ServiceName.WORKSPACE, DomainName.STORAGE, "15", Status.BAD_REQUEST,
        "Bad request"),

    WORKER_FORMAT_IDENTIFIER_NOT_FOUND(ServiceName.WORKER, DomainName.IO, "00", Status.NOT_FOUND, "Format identifier " +
        "%s not found"),
    WORKER_FORMAT_IDENTIFIER_IMPLEMENTATION_NOT_FOUND(ServiceName.WORKER, DomainName.IO, "01", Status.NOT_FOUND,
        "Format " +
            "identifier %s implementation not found"),
    WORKER_FORMAT_IDENTIFIER_TECHNICAL_INTERNAL_ERROR(ServiceName.WORKER, DomainName.IO, "02",
        Status.INTERNAL_SERVER_ERROR,
        "Format identifier internal error"),

    WORKFLOW_DEFINITION_ERROR(ServiceName.PROCESSING, DomainName.VALIDATION, "00", Status.INTERNAL_SERVER_ERROR,
        "Find workflow definitions in error"),
    WORKFLOW_PROCESSES_ERROR(ServiceName.PROCESSING, DomainName.VALIDATION, "01", Status.INTERNAL_SERVER_ERROR,
        "Find workflow processes in error"),

    CONTRACT_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "08",
        Status.BAD_REQUEST,
        "Request validation error"),

    PROFILE_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "09",
        Status.BAD_REQUEST,
        "Request profile validation error"),

    PROFILE_FILE_IMPORT_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "10",
        Status.BAD_REQUEST,
        "Request profile file import error"),
    CONTRACT_NOT_FOUND_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "11",
        Status.BAD_REQUEST,
        "Contract not found for update"),

    CONTEXT_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "12",
        Status.BAD_REQUEST,
        "Request context validation error"),

    AGENCIES_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "13",
        Status.BAD_REQUEST,
        "Request agency validation error"),

    PRESERVATION_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "50",
        Status.BAD_REQUEST,
        "Request griffin validation error"),

    PRESERVATION_INTERNAL_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "51",
        Status.INTERNAL_SERVER_ERROR,
        "Request griffin validation error"),

    SECURITY_PROFILE_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "14",
        Status.BAD_REQUEST,
        "Security profile request validation error"),

    ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "15",
        Status.BAD_REQUEST,
        "Request archive unit profile validation error"),

    ARCHIVE_UNIT_PROFILE_FILE_IMPORT_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "16",
        Status.BAD_REQUEST,
        "Request archive unit profile file import error"),

    ONTOLOGY_IMPORT_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "17",
        Status.BAD_REQUEST,
        "Ontology file import error"),

    ONTOLOGY_VALIDATION_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "18",
        Status.BAD_REQUEST,
        "Request ontology validation error"),


    CTR_SHEMA_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.VALIDATION, "19",
        Status.BAD_REQUEST,
        "Json Schema validation error"),

    REFERENTIAL_REPOSITORY_DATABASE_ERROR(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.DATABASE, "15",
        Status.INTERNAL_SERVER_ERROR,
        "DatabaseException while accessing database through repository service"),

    REFERENTIAL_NOT_FOUND(ServiceName.FUNCTIONAL_ADMINISTRATION, DomainName.DATABASE, "03", Status.NOT_FOUND,
        "Referential not found"),

    ACCESS_EXTERNAL_SELECT_UNITS_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "00",
        Status.BAD_REQUEST,
        "Access external client error in selectUnits method."),

    ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "01",
        Status.BAD_REQUEST,
        "Access external client error in selectUnitbyId method."),

    ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "02",
        Status.BAD_REQUEST,
        "Access external client error in updateUnitbyId method."),

    ACCESS_EXTERNAL_SELECT_OBJECTS_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "49",
        Status.BAD_REQUEST,
        "Access external client error in selectObjects method."),

    ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "03",
        Status.BAD_REQUEST,
        "Access external client error in selectObjectById method."),

    ACCESS_EXTERNAL_SELECT_OPERATION_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "04",
        Status.BAD_REQUEST,
        "Access external client error in selectOperation method."),

    ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "05",
        Status.BAD_REQUEST,
        "Access external client error in selectOperationbyId method."),

    ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "06",
        Status.BAD_REQUEST,
        "Access external client error in selectUnitLifeCycleById method."),

    ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "07",
        Status.BAD_REQUEST,
        "Access external client error in selectUnitLifeCycle method."),

    ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "08",
        Status.BAD_REQUEST,
        "Access external client error in selectObjectGroupLifeCycleById method."),

    ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_PERMISSION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "16",
        Status.UNAUTHORIZED,
        "Unauthorized to access objects group lifecycle"),

    ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_PERMISSION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "17",
        Status.UNAUTHORIZED,
        "Unauthorized to access archive units lifecycle"),

    ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "09",
        Status.BAD_REQUEST,
        "Access external client error in getAccessionRegisterSummary method."),

    ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "10",
        Status.BAD_REQUEST,
        "Access external client error in getAccessionRegisterSummary method."),

    ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "11",
        Status.BAD_REQUEST,
        "Access external client error in checkTraceabilityOperation method."),

    ACCESS_EXTERNAL_SERVER_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "12",
        Status.INTERNAL_SERVER_ERROR,
        "Access external server error."),

    ACCESS_EXTERNAL_CLIENT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "13",
        Status.INTERNAL_SERVER_ERROR,
        "Access external client not found."),

    ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SYMBOLIC_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "66",
        Status.BAD_REQUEST,
        "Access external client error in getAccessionRegisterSymbolic method."),

    ACCESS_EXTERNAL_GET_GET_GRIFFIN_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "67",
        Status.BAD_REQUEST,
        "Access external client in getting griffin."),

    // MASS UPDATE units
    ACCESS_EXTERNAL_MASS_UPDATE_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "40",
        Status.BAD_REQUEST,
        "Access external client error in the mass update units method."),

    ACCESS_EXTERNAL_MASS_UPDATE_UNITS_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "41",
        Status.NOT_FOUND,
        "Access external client error mass update units not found."),

    ACCESS_EXTERNAL_GET_ACCESS_LOG(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "42",
        Status.BAD_REQUEST,
        "Access external client error in getAccessLog method."),


    ACCESS_INTERNAL_MASS_UPDATE_UNITS_CHECK_RULES(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "42",
        Status.BAD_REQUEST,
        "Access internal error while check mass update units on rules"),

    INTERNAL_SECURITY_MASS_UPDATE_AUTHORIZATION_REJECTED(ServiceName.VITAM, DomainName.SECURITY, "43",
        Status.UNAUTHORIZED,
        "Internal Security Filter Mass Update of units Unauthorized"),

    INTERNAL_SECURITY_MASS_UPDATE_MANAGEMENT_UNAUTHORIZED(ServiceName.VITAM, DomainName.SECURITY, "44",
        Status.UNAUTHORIZED,
        "Update management data is Unauthorized"),

    INTERNAL_SECURITY_MASS_UPDATE_GRAPH_UNAUTHORIZED(ServiceName.VITAM, DomainName.SECURITY, "45", Status.UNAUTHORIZED,
        "Update graph is Unauthorized"),

    INTERNAL_SECURITY_MASS_UPDATE_INTERNAL_DATA_UNAUTHORIZED(ServiceName.VITAM, DomainName.SECURITY, "46",
        Status.UNAUTHORIZED,
        "Update internal data is Unauthorized"),

    ADMIN_EXTERNAL_FIND_DOCUMENT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "14",
        Status.BAD_REQUEST,
        "Admin external client error in findDocuments method."),

    ADMIN_EXTERNAL_FIND_DOCUMENT_BY_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "15",
        Status.BAD_REQUEST,
        "Admin external client error in findDocumentById method."),

    ADMIN_EXTERNAL_CHECK_DOCUMENT_BAD_REQUEST(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "16",
        Status.BAD_REQUEST,
        "Admin external bad request error in checkDocument method."),

    ADMIN_EXTERNAL_CHECK_DOCUMENT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "17",
        Status.INTERNAL_SERVER_ERROR,
        "Admin external internal server error in checkDocument method."),

    ADMIN_EXTERNAL_CHECK_DOCUMENT_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "18",
        Status.NOT_FOUND,
        "Admin external not found error in checkDocument method."),

    ACCESS_EXTERNAL_SELECT_DATA_OBJECT_BY_UNIT_ID_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "19",
        Status.BAD_REQUEST,
        "Access external client error in getDataObjectByUnitId method."),

    ADMIN_EXTERNAL_BAD_REQUEST(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "20",
        Status.BAD_REQUEST,
        "Admin external bad request error"),

    ADMIN_EXTERNAL_PRECONDITION_FAILED(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "21",
        Status.PRECONDITION_FAILED,
        "Admin external precondition failed error"),

    ADMIN_EXTERNAL_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "22",
        Status.NOT_FOUND,
        "Admin external not found error"),

    ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "23",
        Status.INTERNAL_SERVER_ERROR,
        "Admin external internal server error"),

    ADMIN_EXTERNAL_UPDATE_PROFILE_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "24",
        Status.BAD_REQUEST,
        "Admin external client error in updateProfile method."),

    ADMIN_EXTERNAL_UPDATE_CONTEXT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "25",
        Status.BAD_REQUEST,
        "Admin external client error in updateContext method."),

    ADMIN_EXTERNAL_UPDATE_SECURITY_PROFILE_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "26",
        Status.BAD_REQUEST,
        "Admin external client error in updateSecurityProfile method."),

    ADMIN_EXTERNAL_UPDATE_ACCESS_CONTRACT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "27",
        Status.BAD_REQUEST,
        "Admin external client error in updateAccessContract method."),

    ADMIN_EXTERNAL_UPDATE_INGEST_CONTRACT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "28",
        Status.BAD_REQUEST,
        "Admin external client error in updateIngestContract method."),

    ACCESS_EXTERNAL_UNIT_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "29",
        Status.NOT_FOUND,
        "Access external client error unit not found."),

    ACCESS_EXTERNAL_CONTEXT_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "30",
        Status.NOT_FOUND, "Access external client error, context not found"),

    ACCESS_EXTERNAL_PROFILE_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "31",
        Status.NOT_FOUND, "Access external client error, profile not found"),

    ACCESS_EXTERNAL_SECURITY_PROFILE_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "32",
        Status.NOT_FOUND, "Access external client error, security profile not found"),

    ACCESS_EXTERNAL_UNIT_TRACREABILITY_AUDIT(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "33",
        Status.BAD_REQUEST, "Access external client error. Unit traceability audit service "),

    ACCESS_EXTERNAL_OBJECT_GROUP_TRACREABILITY_AUDIT(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "34",
        Status.BAD_REQUEST, "Access external client error. Object group traceability audit service "),

    ACCESS_EXTERNAL_INVALID_JSON(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "35",
        Status.BAD_REQUEST, "Access external client error. JSON is invalid"),

    ACCESS_EXTERNAL_ONTOLOGY_NOT_FOUND(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "36",
        Status.BAD_REQUEST, "Access external client error. Ontology not found "),

    ADMIN_EXTERNAL_UPDATE_ONTOLOGY_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "37",
        Status.BAD_REQUEST,
        "Admin external client error in updateOntology method."),

    ACCESS_EXTERNAL_SELECT_UNITS_WITH_INHERITED_RULES_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "38",
        Status.BAD_REQUEST,
        "Access external client error in selectUnitsWithInheritedRules method."),

    ADMIN_EXTERNAL_UPDATE_MANAGEMENT_CONTRACT_ERROR(ServiceName.EXTERNAL_ACCESS, DomainName.IO, "39",
        Status.BAD_REQUEST,
        "Admin external client error in updateManagementContract method."),

    ACCESS_INTERNAL_UPDATE_UNIT_CHECK_RULES(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "01",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules"),
    ACCESS_INTERNAL_UPDATE_UNIT_DELETE_CATEGORY_INHERITANCE(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "02",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't delete rule category that prevent inheritance"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_END_DATE(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "03",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't update rule with a given EndDate"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_BAD_FORMAT(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "39",
        Status.BAD_REQUEST,
        "Bad format ! You can either do {\"#management.CategoryRule.Rules\":[...]} or {\"#management.CategoryRule\":{\"Rules\":[...]}}"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_FINAL_ACTION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "04",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't update rule with a wrong FinalAction"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_EXIST(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "05",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't update rule with an unknow RuleID"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_CATEGORY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "06",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't update rule with a wrong category"),
    ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_END_DATE(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "07",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't create rule with a given EndDate"),
    ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_FINAL_ACTION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "08",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't create rule with a wrong FinalAction"),
    ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_EXIST(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "09",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't create rule with an unknow RuleID"),
    ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_CATEGORY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "10",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't create rule with a wrong category"),
    ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_START_DATE(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "11",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't create rule with a startDate > 9000"),
    ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_START_DATE(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "12",
        Status.BAD_REQUEST,
        "Access internal error while check update on rules: Can't update rule with a startDate > 9000"),
    ACCESS_INTERNAL_DIP_ERROR(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "13",
        Status.BAD_REQUEST,
        "Access internal client error in DIP service"),

    UPDATE_UNIT_PERMISSION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "14", Status.UNAUTHORIZED,
        "Unauthorized to update archive units"),
    UPDATE_UNIT_DESC_PERMISSION(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "15", Status.UNAUTHORIZED,
        "Unauthorized to update other than descriptive metadata in archive units"),
    UPDATE_UNIT_RULES_CONSISTENCY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "18", Status.UNAUTHORIZED,
        "Unauthorized to update rules: Bad ID or wrong category"),
    UPDATE_UNIT_RULES_PROPERTY_CONSISTENCY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "19", Status.UNAUTHORIZED,
        "Unauthorized to update rules: Incorrect value for a property"),
    UPDATE_UNIT_MANAGEMENT_METADATA_CONSISTENCY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "20",
        Status.UNAUTHORIZED,
        "Unauthorized to update management metadata: Incorrect property value"),
    UPDATE_UNIT_RULES_QUERY_CONSISTENCY(ServiceName.INTERNAL_ACCESS, DomainName.BUSINESS, "21", Status.BAD_REQUEST,
        "Request incorrect: Illegal use of properties"),

    INGEST_EXTERNAL_ILLEGAL_ARGUMENT(ServiceName.EXTERNAL_INGEST, DomainName.IO, "00", Status.PRECONDITION_FAILED,
        "Ingest external illegal argument"),
    INGEST_EXTERNAL_PRECONDITION_FAILED(ServiceName.EXTERNAL_INGEST, DomainName.IO, "01", Status.PRECONDITION_FAILED,
        "Ingest external precondition failed"),
    INGEST_EXTERNAL_NOT_FOUND(ServiceName.EXTERNAL_INGEST, DomainName.IO, "02", Status.NOT_FOUND,
        "Ingest external not found"),
    INGEST_EXTERNAL_UNAUTHORIZED(ServiceName.EXTERNAL_INGEST, DomainName.IO, "03", Status.UNAUTHORIZED,
        "Ingest external unauthorized"),
    INGEST_EXTERNAL_BAD_REQUEST(ServiceName.EXTERNAL_INGEST, DomainName.IO, "04", Status.BAD_REQUEST,
        "Ingest external bad request"),
    INGEST_EXTERNAL_INTERNAL_SERVER_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "05",
        Status.INTERNAL_SERVER_ERROR,
        "Ingest external internal server error"),
    INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "06",
        Status.INTERNAL_SERVER_ERROR, "Ingest external internal client error"),
    INGEST_EXTERNAL_UPLOAD_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "07", Status.BAD_REQUEST,
        "Ingest external client error in upload method."),
    INGEST_EXTERNAL_EXECUTE_OPERATION_PROCESS_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "08",
        Status.BAD_REQUEST,
        "Ingest external client error in executeOperationProcess method."),
    INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "09",
        Status.BAD_REQUEST,
        "Ingest external client error in getOperationProcessExecutionDetails method."),
    INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_HANDLING_ERROR(ServiceName.EXTERNAL_INGEST, DomainName.IO, "10",
        Status.BAD_REQUEST,
        "File error during local ingest attempt."),
    INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_SECURITY_ALERT(ServiceName.EXTERNAL_INGEST, DomainName.IO, "11",
        Status.BAD_REQUEST,
        "Security alert during local ingest attempt."),
    METADATA_INDEXATION_ERROR(ServiceName.METADATA, DomainName.DATABASE, "00", Status.INTERNAL_SERVER_ERROR,
        "Indexation error"),
    METADATA_SWITCH_INDEX_ERROR(ServiceName.METADATA, DomainName.DATABASE, "01", Status.INTERNAL_SERVER_ERROR,
        "Switch index error"),
    METADATA_REPOSITORY_DATABASE_ERROR(ServiceName.METADATA, DomainName.DATABASE, "02", Status.INTERNAL_SERVER_ERROR,
        "DatabaseException while accessing database through repository service"),

    METADATA_NOT_FOUND(ServiceName.METADATA, DomainName.DATABASE, "03", Status.NOT_FOUND, "Metadata not found"),


    LOGBOOK_EXTERNAL_INTERNAL_SERVER_ERROR(ServiceName.LOGBOOK, DomainName.NETWORK, "O0", Status.INTERNAL_SERVER_ERROR,
        "Logbook client error"),
    LOGBOOK_EXTERNAL_BAD_REQUEST(ServiceName.LOGBOOK, DomainName.BUSINESS, "O1", Status.BAD_REQUEST,
        "Logbook operation is not correct"),
    LOGBOOK_EXTERNAL_CONFLICT(ServiceName.LOGBOOK, DomainName.ILLEGAL, "O2", Status.CONFLICT,
        "Logbook operation already exists");

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
