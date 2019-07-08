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
package fr.gouv.vitam.functional.administration.common.server;

import fr.gouv.vitam.common.database.server.DocumentValidator;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.json.JsonSchemaValidationException;
import fr.gouv.vitam.common.json.JsonSchemaValidator;

public final class ReferentialDocumentValidators {

    private static final DocumentValidator ACCESS_CONTRACT_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/access-contract-schema.json");
    private static final DocumentValidator ACCESSION_REGISTER_DETAIL_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/accession-register-detail.schema.json");
    private static final DocumentValidator ACCESSION_REGISTER_SUMMARY_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/accession-register-summary.schema.json");
    private static final DocumentValidator AGENCIES_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/agencies.schema.json");
    private static final DocumentValidator ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/archive-unit-profile.schema.json");
    private static final DocumentValidator CONTEXT_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/context.schema.json");
    private static final DocumentValidator FILE_FORMAT_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/file-format.schema.json");
    private static final DocumentValidator FILE_RULES_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/file-rules.schema.json");
    private static final DocumentValidator INGEST_CONTRACT_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/ingest-contract.schema.json");
    private static final DocumentValidator PROFILE_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/profile.schema.json");
    private static final DocumentValidator SECURITY_PROFILE_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/security-profile.schema.json");
    private static final DocumentValidator ONTOLOGY_SCHEMA_VALIDATOR =
        forBuiltInSchema("json-schema/ontology.schema.json");
    private static final DocumentValidator GRIFFIN_SCHEMA =
        forBuiltInSchema("json-schema/griffin-shema.schema.json");
    private static final DocumentValidator PRESERVATION_SCENARIO_SCHEMA =
        forBuiltInSchema("json-schema/preservation-scenario-shema.schema.json");
    private static final DocumentValidator NULL_SCHEMA_VALIDATOR = (jsonNode) -> { /* NOP */ };


    private static DocumentValidator forBuiltInSchema(String schemaFilename) {
        JsonSchemaValidator schemaValidator = JsonSchemaValidator.forBuiltInSchema(schemaFilename);

        return jsonNode -> {
            try {
                schemaValidator.validateJson(jsonNode);
            } catch (JsonSchemaValidationException e) {
                throw new SchemaValidationException(e);
            }
        };
    }

    public static DocumentValidator getValidator(FunctionalAdminCollections collection) {

        switch (collection) {

            case RULES:
                return FILE_RULES_SCHEMA_VALIDATOR;
            case INGEST_CONTRACT:
                return INGEST_CONTRACT_SCHEMA_VALIDATOR;
            case ACCESS_CONTRACT:
                return ACCESS_CONTRACT_SCHEMA_VALIDATOR;
            case PROFILE:
                return PROFILE_SCHEMA_VALIDATOR;
            case ARCHIVE_UNIT_PROFILE:
                return ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATOR;
            case AGENCIES:
                return AGENCIES_SCHEMA_VALIDATOR;
            case CONTEXT:
                return CONTEXT_SCHEMA_VALIDATOR;
            case SECURITY_PROFILE:
                return SECURITY_PROFILE_SCHEMA_VALIDATOR;
            case GRIFFIN:
                return GRIFFIN_SCHEMA;
            case PRESERVATION_SCENARIO:
                return PRESERVATION_SCENARIO_SCHEMA;
            case ONTOLOGY:
                return ONTOLOGY_SCHEMA_VALIDATOR;

            case FORMATS:
                // FIXME : File format schema needs to be fixed

                // return FILE_FORMAT_SCHEMA_VALIDATOR;
                return NULL_SCHEMA_VALIDATOR;

            case ACCESSION_REGISTER_SUMMARY:
                // FIXME : Accession register summary schema needs to be fixed

                // return ACCESSION_REGISTER_SUMMARY_SCHEMA_VALIDATOR;
                return NULL_SCHEMA_VALIDATOR;

            case ACCESSION_REGISTER_DETAIL:
                // FIXME : Accession register detail schema needs to be fixed

                // return ACCESSION_REGISTER_DETAIL_SCHEMA_VALIDATOR;
                return NULL_SCHEMA_VALIDATOR;

            case ACCESSION_REGISTER_SYMBOLIC:
                // FIXME : Don't know why there is no schema validator for this one
                return NULL_SCHEMA_VALIDATOR;

            case VITAM_SEQUENCE:
                // Internal collection. No need for schema validator
                return NULL_SCHEMA_VALIDATOR;

            default:
                throw new IllegalStateException("Unexpected value: " + collection);
        }

    }
}
