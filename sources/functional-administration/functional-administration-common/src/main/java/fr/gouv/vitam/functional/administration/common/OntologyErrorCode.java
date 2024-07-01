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
package fr.gouv.vitam.functional.administration.common;

/**
 * Ontology Import ErrorCode enumeration
 */
public enum OntologyErrorCode {
    /**
     * File not in json format
     */
    STP_IMPORT_ONTOLOGIES_NOT_JSON_FORMAT,
    /**
     * Onlogy used by document type while trying to delete
     */
    STP_IMPORT_ONTOLOGIES_DELETE_IDENTIFIER_USED_BY_DT,
    /**
     * Identifier already used
     */
    STP_IMPORT_ONTOLOGIES_IDENTIFIER_ALREADY_IN_ONTOLOGY,
    /**
     * /**
     * Used ontology could not be deleted
     */
    STP_IMPORT_ONTOLOGIES_DELETE_USED_ONTOLOGY,
    /**
     * Missing information
     */
    STP_IMPORT_ONTOLOGIES_MISSING_INFORMATION,

    /**
     * Invalid parameter
     */
    STP_IMPORT_ONTOLOGIES_INVALID_PARAMETER,

    /**
     * Invalid type for update
     */
    STP_IMPORT_ONTOLOGIES_UPDATE_INVALID_TYPE,

    /**
     * Invalid id in create
     */
    STP_IMPORT_ONTOLOGIES_ID_NOT_ALLOWED_IN_CREATE,

    /**
     * Internal origin not allowed for non admin tenants
     */
    STP_IMPORT_ONTOLOGIES_NOT_AUTHORIZED_FOR_TENANT,

    /**
     * Delete not authorized
     */
    STP_IMPORT_ONTOLOGIES_DELETE_NOT_AUTHORIZED,

    /**
     * General import error
     */
    STP_IMPORT_ONTOLOGIES_EXCEPTION,

    /**
     * Conflict merging internal and external at initialization
     */
    STP_IMPORT_ONTOLOGIES_INTERNAL_EXTERNAL_CONFLICT_EXCEPTION,
}
