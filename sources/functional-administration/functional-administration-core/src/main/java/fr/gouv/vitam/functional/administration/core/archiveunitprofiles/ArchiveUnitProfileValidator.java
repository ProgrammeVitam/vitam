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
package fr.gouv.vitam.functional.administration.core.archiveunitprofiles;

import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;

import java.util.Optional;

/**
 * Used to validate archive unit profiles and to apply acceptance rules.
 *
 * Bellow the example of usage :
 *
 * <pre>
 * {@code
 * private static ArchiveUnitProfileValidator checkDuplicateInDatabaseValidator() {
 *    return (profile, profileIdentifier) -> {
 *        GenericRejectionCause rejection = null;
 *        boolean exist = ... exists in database?;
 *        if (exist) {
 *           rejection = GenericRejectionCause.rejectDuplicatedInDatabase(profileIdentifier);
 *        }
 *        return (rejection == null) ? Optional.empty() : Optional.of(rejection);
 *    };
 * }
 * }
 * </pre>
 *
 * The call the method like this to validate the archive unit profile aup:
 *
 * GenericRejectionCause rejection = checkDuplicateInDatabaseValidator().validate(aup, aup.getName());
 *
 * Check if rejection is present then do the resolution
 */
@FunctionalInterface
public interface ArchiveUnitProfileValidator {
    /**
     * Validate an archive unit profile object
     *
     * @param profile to validate
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<RejectionCause> validate(ArchiveUnitProfileModel profile);

    /**
     * Rejection Cause
     */
    class RejectionCause {

        private static final String ERR_ID_NOT_ALLOWED_IN_CREATE =
            "Id must be null when creating archive unit profile (%s)";
        private static final String ERR_DUPLICATE_IDENTIFIER_ARCHIVE_PROFILE =
            "The archive unit profile identifier %s already exists in database";
        private static final String ERR_DUPLICATE_NAME_ARCHIVE_PROFILE =
            "The archive unit profile name %s already exists in database";
        private static final String ERR_MANDATORY_FIELD = "The field %s is mandatory";
        private static final String ERR_JSON_SHEMA = "The field %s is not a json schema";
        private static final String ERR_JSON_SCHEMA_IN_USE = "The field %s is used by an archiveUnit";
        private static final String ERR_MISSING_ONTOLOGY_FIELD =
            "The field %s specified in the schema is not declared in ontology";
        private static final String ERR_INCORRECT_ONTOLOGY_FIELD =
            "The field %s specified in the schema is not compatible with the one declared in ontology";

        private String reason;

        /**
         * Constructor
         *
         * @param error
         */
        public RejectionCause(String error) {
            setReason(error);
        }

        /**
         * Reject if id exisit and the action is creation. If id exists, it should be an update instead of create
         *
         * @param archiveUnitProfileIdentifier
         * @return RejectionCause
         */
        public static RejectionCause rejectIdNotAllowedInCreate(String archiveUnitProfileIdentifier) {
            return new RejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, archiveUnitProfileIdentifier));
        }

        /**
         * Verify for each archive unit profile if already exists one in database that have the same identifier and/or name. The
         * database my manage this kind of constraint (by creating an unique index on the field or column)
         *
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicateIdentifierInDatabase(String identifier) {
            return new RejectionCause(String.format(ERR_DUPLICATE_IDENTIFIER_ARCHIVE_PROFILE, identifier));
        }

        /**
         * Verify for each archive unit profile if already exists one in database that have the same identifier and/or name. The
         * database my manage this kind of constraint (by creating an unique index on the field or column)
         *
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicateNameInDatabase(String identifier) {
            return new RejectionCause(String.format(ERR_DUPLICATE_NAME_ARCHIVE_PROFILE, identifier));
        }

        /**
         * Reject if the field is not a json shema
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectJsonSchemaModificationIfInUse(String fieldName) {
            return new RejectionCause(String.format(ERR_JSON_SCHEMA_IN_USE, fieldName));
        }

        /**
         * Reject if the field is not a json shema
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectJsonShema(String fieldName) {
            return new RejectionCause(String.format(ERR_JSON_SHEMA, fieldName));
        }

        /**
         * Reject if one of multiple mandatory parameter are null
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectMandatoryMissing(String fieldName) {
            return new RejectionCause(String.format(ERR_MANDATORY_FIELD, fieldName));
        }

        /**
         * Reject if one field is not declared in ontology
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectMissingFieldInOntology(String fieldName) {
            return new RejectionCause(String.format(ERR_MISSING_ONTOLOGY_FIELD, fieldName));
        }

        /**
         * Reject if one field is not compatible with the one declared in ontology
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectIncorrectFieldInOntology(String fieldName) {
            return new RejectionCause(String.format(ERR_INCORRECT_ONTOLOGY_FIELD, fieldName));
        }

        /**
         * Get reason
         *
         * @return reason
         */
        public String getReason() {
            return reason;
        }

        private void setReason(String reason) {
            this.reason = reason;
        }
    }
}
