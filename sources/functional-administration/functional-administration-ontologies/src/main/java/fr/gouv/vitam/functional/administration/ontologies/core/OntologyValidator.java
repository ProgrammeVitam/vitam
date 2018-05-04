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

import fr.gouv.vitam.common.model.ModelConstants;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.functional.administration.common.OntologyErrorCode;

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
        private static final String ERR_USED_ONTOLOGY = "The ontology %s is used in a document type";


        private OntologyModel ontologyModel;
        private String reason;
        private String fieldName;
        private OntologyErrorCode errorCode;

        /**
         * Constructor
         *
         * @param errorCode
         * @param error
         */
        public RejectionCause(String fieldName, OntologyModel ontologyModel, OntologyErrorCode errorCode, String error) {
            setErrorCode(errorCode);
            setReason(error);
            setFieldName(fieldName);
            setOntologyModel(ontologyModel);
        }



        /**
         * Reject if an id exist and the action is creation. If id exists, it should be an update instead of create
         *
         * @param ontologyModel
         * @return RejectionCause
         */
        public static RejectionCause rejectIdNotAllowedInCreate(OntologyModel ontologyModel) {
            return new RejectionCause(ModelConstants.TAG_ID, ontologyModel, OntologyErrorCode.STP_IMPORT_ONTOLOGIES_ID_NOT_ALLOWED_IN_CREATE,
                String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, ontologyModel.getId()));
        }

        /**
         * Verify that no other ontology with the same identifier already exists
         *
         * @param ontologyModel
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicatedInDatabase(OntologyModel ontologyModel) {
            return new RejectionCause(OntologyModel.TAG_IDENTIFIER, ontologyModel, OntologyErrorCode.STP_IMPORT_ONTOLOGIES_IDENTIFIER_ALREADY_IN_ONTOLOGY,
                String.format(ERR_DUPLICATE_ONTOLOGY, ontologyModel.getIdentifier()));
        }


        /**
         * Verify that the ontology is not used in a document type
         *
         * @param ontologyModel
         * @param documentTypes the list of documentTypes using the specified ontology
         * @return RejectionCause
         */
        public static RejectionCause rejectUsedByDocumentTypeInDatabase(OntologyModel ontologyModel, String documentTypes) {
            return new RejectionCause(null, ontologyModel, OntologyErrorCode.STP_IMPORT_ONTOLOGIES_DELETE_USED_ONTOLOGY,
                documentTypes);
        }



        /**
         * Reject if the ontology identifier is not valid
         *
         * @param ontologyModel
         * @return RejectionCause
         */
        public static RejectionCause rejectInvalidIdentifier(OntologyModel ontologyModel) {
            return new RejectionCause(OntologyModel.TAG_IDENTIFIER, ontologyModel, OntologyErrorCode.STP_IMPORT_ONTOLOGIES_INVALID_IDENTIFIER,
                String.format(ERR_INVALID_IDENTIFIER, ontologyModel.getIdentifier()));
        }


        /**
         * Reject if one of multiple mandatory parameter are null
         *
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectMandatoryMissing(OntologyModel ontologyModel, String fieldName) {
            return new RejectionCause(fieldName, ontologyModel, OntologyErrorCode.STP_IMPORT_ONTOLOGIES_MISSING_INFORMATION, String.format(ERR_MANDATORY_FIELD, fieldName));
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

        public OntologyErrorCode getErrorCode() {
            return errorCode;
        }

        private void setErrorCode(OntologyErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public OntologyModel getOntologyModel() {
            return ontologyModel;
        }

        public void setOntologyModel(OntologyModel ontologyModel) {
            this.ontologyModel = ontologyModel;
        }
    }
}
