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
package fr.gouv.vitam.functional.administration.agencies.api;

import java.util.Optional;

import fr.gouv.vitam.common.model.administration.AgenciesModel;

/**
 * AgenciesValidator interface
 */
public interface AgenciesValidator {
    /**
     * Validate a agency object
     *
     * @param agency to validate
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<AgenciesRejectionCause> validate(AgenciesModel agency);

    /**
     * AgenciesRejectionCause class
     * 
     */
    public class AgenciesRejectionCause {
        /**
         * Error label for id not null
         */
        public static String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating agency (%s)";
        
        /**
         * Error label for duplication of agency in the database
         */
        public static String ERR_DUPLICATE_AGENCY_ENTRY =
            "One or many Agencies in the imported list have the same name : %s";
        /**
         * Error label for a mandatory field missing
         */
        public static String ERR_MANDATORY_FIELD = "The field %s is mandatory";
        /**
         * Error label for duplication of agency in the database
         */
        public static String ERR_DUPLICATE_AGENCY = "The agency %s already exists in database";


        private String reason;

        /**
         * Constructor
         * 
         * @param error the rejection cause
         */
        public AgenciesRejectionCause(String error) {
            setReason(error);
        }

        /**
         * Reject if id exisit and the action is creation. If id exists, it should be an update instead of create
         *
         * @param agencyIdentifier the id of the agency
         * @return agenciesRejectionCause the cause of rejection
         */
        public static AgenciesRejectionCause rejectIdNotAllowedInCreate(String agencyIdentifier) {
            return new AgenciesRejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, agencyIdentifier));
        }

        /**
         * Reject if multiple agency have the same name in the same request before persist into database. The agency
         * identifier must be unique
         *
         * @param agencyIdentifier the id of the agency
         * @return agenciesRejectionCause the cause of rejection
         */
        public static AgenciesRejectionCause rejectDuplicatedEntry(String agencyIdentifier) {
            return new AgenciesRejectionCause(String.format(ERR_DUPLICATE_AGENCY_ENTRY, agencyIdentifier));
        }

        /**
         * Reject if one of multiple mandatory parameter are null
         * 
         * @param fieldName the field name
         * @return agenciesRejectionCause the cause of rejection
         */
        public static AgenciesRejectionCause rejectMandatoryMissing(String fieldName) {
            return new AgenciesRejectionCause(String.format(ERR_MANDATORY_FIELD, fieldName));
        }

        /**
         * Verify for each agency if already exists one in database that have the same name. The database my manage this
         * kind of constraint (by creating an unique index on the field or column)
         * 
         * @param agencyName the name of the agency
         * @return agenciesRejectionCause the cause of rejection
         */
        public static AgenciesRejectionCause rejectDuplicatedInDatabase(String agencyName) {
            return new AgenciesRejectionCause(String.format(ERR_DUPLICATE_AGENCY, agencyName));
        }


        /**
         * Get the reason of rejection
         * 
         * @return the reason of rejection
         */
        public String getReason() {
            return reason;
        }

        private void setReason(String reason) {
            this.reason = reason;
        }

    }

}
