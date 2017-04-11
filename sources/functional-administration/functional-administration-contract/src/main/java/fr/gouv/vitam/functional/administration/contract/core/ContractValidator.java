package fr.gouv.vitam.functional.administration.contract.core;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.functional.administration.common.IngestContract;

@FunctionalInterface
public interface ContractValidator {



    /**
     * Validate a contract object
     * 
     * @param contract
     * @param contractsToPersist The contracts that have been parsed correctly from the input
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<RejectionCause> validate(IngestContract contract, Map<String, JsonNode> contractsToPersist);


    public class RejectionCause {

        private static final String ERR_INVALID_CONTRACT = "The input contract json is is invalid";
        public static String ERR_DUPLICATE_CONTRACT_ENTRY = "One or many contracts in the imported list have the same name : %s";
        public static String ERR_DUPLICATE_CONTRACT = "The contract %s already exists in database";
        public static String ERR_INVALID_FIELD = "The field %s has an invalid format";
        public static String ERR_MANDATORY_FIELD = "The field %s is mandatory";

        private String reason;
        
        public RejectionCause(String error) {
            setReason(error);
        }

        public static RejectionCause rejectDuplicatedEntry(IngestContract contract){    
            return new RejectionCause(String.format(ERR_DUPLICATE_CONTRACT_ENTRY , contract.toJson()));
        }
        
        public static RejectionCause rejectDuplicatedInDatabase(IngestContract contract){    
            return new RejectionCause(String.format(ERR_DUPLICATE_CONTRACT , contract.toJson()));
        }
        
        public static RejectionCause rejectWrongField(IngestContract contract, String fieldName){
            return new RejectionCause(String.format(ERR_INVALID_FIELD , fieldName));
        }
        
        public static RejectionCause rejectMandatoryMissing(IngestContract contract, String fieldName){
            return new RejectionCause(String.format(ERR_MANDATORY_FIELD , fieldName));
        }
        
        public static RejectionCause rejectInvalidContractFormat(JsonNode contractAsJson){    
            return new RejectionCause(String.format(ERR_INVALID_CONTRACT , contractAsJson.toString()));
        }

        public String getReason() {
            return reason;
        }

        private void setReason(String reason) {
            this.reason = reason;
        }
    }
}
