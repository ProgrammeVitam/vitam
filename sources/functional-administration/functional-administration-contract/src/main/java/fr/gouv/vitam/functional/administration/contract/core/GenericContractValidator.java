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
package fr.gouv.vitam.functional.administration.contract.core;

import java.util.Optional;

import fr.gouv.vitam.common.model.administration.AbstractContractModel;

/**
 * Used to validate contracts (any class that extends AbstractContractModel) and to apply acceptance rules.
 *
 * Bellow the example of usage :
 * 
 * <pre>
 * {@code
 * private static GenericContractValidator checkDuplicateInDatabaseValidator() {
 *    return (contract, contractName) -> {
 *        GenericRejectionCause rejection = null;
 *        boolean exist = ... exists in database?;
 *        if (exist) {
 *           rejection = GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
 *        }
 *        return (rejection == null) ? Optional.empty() : Optional.of(rejection);
 *    };
 * }
 * }
 * </pre>
 *
 * The call the method like this to validate the contract c:
 *
 * GenericRejectionCause rejection = checkDuplicateInDatabaseValidator().validate(c, c.getName());
 *
 * Check if rejection is present then do the resolution
 *
 * @param <T>
 */
public interface GenericContractValidator<T extends AbstractContractModel> {



    /**
     * Validate a contract object
     * 
     * @param contract to validate
     * @param contractName
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<GenericRejectionCause> validate(T contract, String contractName);


    /**
     * Generic Rejection Cause inner class
     */
    public class GenericRejectionCause {

        private static final String ERR_DUPLICATE_CONTRACT_ENTRY =
            "One or many contracts in the imported list have the same name : %s";
        private static final String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating contracts (%s)";
        private static final String ERR_DUPLICATE_CONTRACT = "The contract %s already exists in database";
        private static String ERR_ARCHIVEPROFILE_NOT_FOUND_CONTRACT =
            "One or multiple archive profiles or the contract %s not found in db";
        private static final String ERR_CONTRACT_EXCEPTION_OCCURRED = "Exception while validating contract (%s), %s : %s";
        private static final String ERR_CONTRACT_ROOT_UNITS_NOT_FOUND =
            "Error while validating contract (%s), RootUnits (%s) not found in database";
        private static final String ERR_CONTRACT_ROOT_GUID_INCLUDED_AND_EXCLUDED =
                "Error while validating contract (%s), root GUID (%s) can not be included and excluded at the same time";
        private static final String ERR_MANDATORY_FIELD = "The field %s is mandatory";
        private static final String ERR_IDS_NOT_FOUND =
            "At least one AU id %s not found";
        private static final String ERR_FORMATFILETYPE_NOT_FOUND_CONTRACT = "One or multiple file format %s not found in db";

        private String reason;

        /**
         * Constructor
         * 
         * @param error the error
         */
        public GenericRejectionCause(String error) {
            setReason(error);
        }

        /**
         * Reject if id exisit and the action is creation. If id exists, it should be an update instead of create
         *
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectIdNotAllowedInCreate(String contractName) {
            return new GenericRejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, contractName));
        }

        /**
         * Reject if multiple contract have the same name in the same request before persist into database. The contract
         * name must be unique
         *
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectDuplicatedEntry(String contractName) {
            return new GenericRejectionCause(String.format(ERR_DUPLICATE_CONTRACT_ENTRY, contractName));
        }

        /**
         * Reject if the id of the AU is not in filing schema
         *
         * @param linkParentId
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectAuNotFoundInDatabase(String linkParentId) {
            return new GenericRejectionCause(String.format(ERR_IDS_NOT_FOUND, linkParentId));
        }

        /**
         * Verify for each contract if already exists one in database that have the same name. The database my manage
         * this kind of constraint (by creating an unique index on the field or column)
         * 
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectDuplicatedInDatabase(String contractName) {
            return new GenericRejectionCause(String.format(ERR_DUPLICATE_CONTRACT, contractName));
        }

        /**
         * Verify for each contract that all archive profiles exists in database
         * 
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectArchiveProfileNotFoundInDatabase(String contractName) {
            return new GenericRejectionCause(String.format(ERR_ARCHIVEPROFILE_NOT_FOUND_CONTRACT, contractName));
        }

        /**
         * Generate RejectionCause from any throwable
         * 
         * @param contractName the contract name or identifier
         * @param msg custom message
         * @param e throwable
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectExceptionOccurred(String contractName, String msg, Throwable e) {
            return new GenericRejectionCause(
                String.format(ERR_CONTRACT_EXCEPTION_OCCURRED, contractName, msg, e.getMessage()));
        }

        /**
         * Generate RejectionCause for not found unit for given GUID
         * 
         * @param contractName the contract name or identifier
         * @param guidArrayAsString root units as string (guid array as string)
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectRootUnitsNotFound(String contractName, String guidArrayAsString) {
            return new GenericRejectionCause(
                String.format(ERR_CONTRACT_ROOT_UNITS_NOT_FOUND, contractName, guidArrayAsString));
        }

        /**
         * Generate RejectionCause for root GUID used in both RootUnits and ExcludedRootUnits
         *
         * @param guid the contract name or identifier
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectRootGuidIncludedAndExcluded(String contractName, String guid) {
            return new GenericRejectionCause(
                    String.format(ERR_CONTRACT_ROOT_GUID_INCLUDED_AND_EXCLUDED, contractName, guid));
        }

        /**
         * Reject if one of multiple mandatory parameter are null
         * 
         * @param fieldName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectMandatoryMissing(String fieldName) {
            return new GenericRejectionCause(String.format(ERR_MANDATORY_FIELD, fieldName));
        }

        /**
         * Verify for each contract that all archive profiles exists in database
         *
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectFormatFileTypeNotFoundInDatabase(String contractName) {
            return new GenericRejectionCause(String.format(ERR_FORMATFILETYPE_NOT_FOUND_CONTRACT, contractName));
        }

        /**
         * Get Reason
         * 
         * @return the reason
         */
        public String getReason() {
            return reason;
        }

        private void setReason(String reason) {
            this.reason = reason;
        }

    }
}
