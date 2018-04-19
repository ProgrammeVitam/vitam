/**
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
 */
package fr.gouv.vitam.functional.administration.ontologies.core;

import java.util.Optional;

import fr.gouv.vitam.common.model.administration.OntologyModel;

/**
 * Used to validate ontology and to apply acceptance rules.
 *
 * Bellow the example of usage :
 *
 * <pre>
 * {@code
 * private static OntologyValidator checkDuplicateInDatabaseValidator() {
 *    return (ontology, identifier) -> {
 *        GenericRejectionCause rejection = null;
 *        boolean exist = ... exists in database?;
 *        if (exist) {
 *           rejection = GenericRejectionCause.rejectDuplicatedInDatabase(identifier);
 *        }
 *        return (rejection == null) ? Optional.empty() : Optional.of(rejection);
 *    };
 * }
 * }
 * </pre>
 *
 * Call the method like this to validate the ontology ontm:
 *
 * GenericRejectionCause rejection = checkDuplicateInDatabaseValidator().validate(aup, aup.getName());
 *
 * Check if rejection is present then do the resolution
 */
@FunctionalInterface
public interface OntologyValidator {



    /**
     * Validate an ontology object
     *
     * @param ontology to validate
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<RejectionCause> validate(OntologyModel ontology);


    /**
     * Rejection Cause
     */
    public class RejectionCause {

        private static final String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating ontology (%s)";
        private static final String ERR_DUPLICATE_ONTOLOGY = "The ontology %s already exists in database";
        private static final String ERR_INVALID_IDENTIFIER = "The ontology identifier %s is not valid";
        private static final String ERR_MANDATORY_FIELD = "The field %s is mandatory";


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
         * Reject if id exist and the action is creation. If id exists, it should be an update instead of create
         *
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectIdNotAllowedInCreate(String identifier) {
            return new RejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, identifier));
        }

        /**
         * Verify that no other ontology with the same identifier already exists
         *
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicatedInDatabase(String identifier) {
            return new RejectionCause(String.format(ERR_DUPLICATE_ONTOLOGY, identifier));
        }



        /**
         * Reject if the ontology identifier is not valid
         *
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectInvalidIdentifier(String identifier) {
            return new RejectionCause(String.format(ERR_INVALID_IDENTIFIER, identifier));
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
