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
package fr.gouv.vitam.functional.administration.core.contract;

import fr.gouv.vitam.common.model.administration.AbstractContractModel;

import java.util.Optional;

/**
 * Used to validate contracts (any class that extends AbstractContractModel) and
 * to apply acceptance rules.
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
 * GenericRejectionCause rejection =
 * checkDuplicateInDatabaseValidator().validate(c, c.getName());
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
    class GenericRejectionCause {

        private static final String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating contracts (%s)";
        private static final String ERR_DUPLICATE_CONTRACT = "The contract %s already exists in database";
        private static final String ERR_ARCHIVEPROFILE_NOT_FOUND_CONTRACT =
            "One or multiple archive profiles or the contract %s not found in db";
        private static final String ERR_CONTRACT_EXCEPTION_OCCURRED =
            "Exception while validating contract (%s), %s : %s";
        private static final String ERR_CONTRACT_ROOT_UNITS_NOT_FOUND =
            "Error while validating contract (%s), RootUnits (%s) not found in database";
        private static final String ERR_CONTRACT_EXCLUDED_ROOT_UNITS_NOT_FOUND =
            "Error while validating contract (%s), ExcludedRootUnits (%s) not found in database";
        private static final String ERR_CONTRACT_EXCLUDED_AND_ROOT_UNITS_NOT_FOUND =
            "Error while validating contract (%s), ExcludedRootUnits and RootUnits (%s) not found in database";
        private static final String ERR_MANDATORY_FIELD = "The field %s is mandatory";
        private static final String ERR_IDS_NOT_FOUND = "At least one AU id %s not found";
        private static final String ERR_MC_IDS_NOT_FOUND = "At least one Management Contract with Id %s not found";
        private static final String ERR_FORMATFILETYPE_NOT_FOUND_CONTRACT =
            "One or multiple file format %s not found in db";
        private static final String ERR_INCONSISTENT_CONTRACT_DEFINITION = "Error while validating contract (%s) : %s";

        private static final String ERR_STORAGE_STRATEGY_NOT_FOUND = "Storage Strategy (%s) not found for the field %s";
        private static final String ERR_STORAGE_STRATEGY_DOES_NOT_CONTAINS_ONE_REFERENT_OFFER =
            "Storage Strategy (%s) does not contains one and only one 'referent' offer for the field %s";

        private static final String ERR_VERSION_RETENTION_POLICY_INVALID =
            "The version retention policy's %s parameter in %s usage is invalid in the contract %s.";
        private static final String ERR_VERSION_RETENTION_POLICY_INVALID_USAGE =
            "The usage type %s is invalid in the contract %s.";

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
         * Reject if id exisit and the action is creation. If id exists, it should be an
         * update instead of create
         *
         * @param contractName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectIdNotAllowedInCreate(String contractName) {
            return new GenericRejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, contractName));
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
         * Reject if the id of the MC is not in database
         *
         * @param managementContractID
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectMCNotFoundInDatabase(String managementContractID) {
            return new GenericRejectionCause(String.format(ERR_MC_IDS_NOT_FOUND, managementContractID));
        }

        /**
         * Verify for each contract if already exists one in database that have the same
         * name. The database my manage this kind of constraint (by creating an unique
         * index on the field or column)
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
                String.format(ERR_CONTRACT_EXCEPTION_OCCURRED, contractName, msg, e.getMessage())
            );
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
                String.format(ERR_CONTRACT_ROOT_UNITS_NOT_FOUND, contractName, guidArrayAsString)
            );
        }

        /**
         * Generate RejectionCause for not found unit for given GUID
         *
         * @param contractName the contract name or identifier
         * @param guidArrayAsString root units as string (guid array as string)
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectExcludedRootUnitsNotFound(
            String contractName,
            String guidArrayAsString
        ) {
            return new GenericRejectionCause(
                String.format(ERR_CONTRACT_EXCLUDED_ROOT_UNITS_NOT_FOUND, contractName, guidArrayAsString)
            );
        }

        /**
         * Generate RejectionCause for not found unit for given GUID
         *
         * @param contractName the contract name or identifier
         * @param guidArrayAsString root units as string (guid array as string)
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectExcludedAndRootUnitsNotFound(
            String contractName,
            String guidArrayAsString
        ) {
            return new GenericRejectionCause(
                String.format(ERR_CONTRACT_EXCLUDED_AND_ROOT_UNITS_NOT_FOUND, contractName, guidArrayAsString)
            );
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

        public static GenericRejectionCause rejectInconsistentContract(String contractName, String reason) {
            return new GenericRejectionCause(
                (String.format(ERR_INCONSISTENT_CONTRACT_DEFINITION, contractName, reason))
            );
        }

        /**
         * Reject if storage strategy was not found
         *
         * @param storageStrategy
         * @param fieldName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectStorageStrategyMissing(String storageStrategy, String fieldName) {
            return new GenericRejectionCause(String.format(ERR_STORAGE_STRATEGY_NOT_FOUND, storageStrategy, fieldName));
        }

        /**
         * Reject if storage strategy does not contains referent
         *
         * @param storageStrategy
         * @param fieldName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectStorageStrategyDoesNotContainsOneReferent(
            String storageStrategy,
            String fieldName
        ) {
            return new GenericRejectionCause(
                String.format(ERR_STORAGE_STRATEGY_DOES_NOT_CONTAINS_ONE_REFERENT_OFFER, storageStrategy, fieldName)
            );
        }

        /**
         * Reject if versionRentionPolicy parameter is invalid
         *
         * @param initialVersion
         * @param fieldName
         * @return GenericRejectionCause
         */
        public static GenericRejectionCause rejectInvalidVersionRetentionPolicyParamOfUsage(
            String fieldName,
            String UsageName,
            String contractName
        ) {
            return new GenericRejectionCause(
                String.format(ERR_VERSION_RETENTION_POLICY_INVALID, fieldName, UsageName, contractName)
            );
        }

        public static GenericRejectionCause rejectInvalidVersionRetentionPolicyUsage(
            String fieldName,
            String contractName
        ) {
            return new GenericRejectionCause(
                String.format(ERR_VERSION_RETENTION_POLICY_INVALID_USAGE, fieldName, contractName)
            );
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
