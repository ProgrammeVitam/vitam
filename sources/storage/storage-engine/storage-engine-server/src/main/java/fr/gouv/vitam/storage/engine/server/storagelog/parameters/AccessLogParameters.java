package fr.gouv.vitam.storage.engine.server.storagelog.parameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AccessLogParameters implements StorageLogStructure {
    private static final String MANDATORY_PARAMETER_CAN_NOT_BE_NULL_OR_EMPTY = "Mandatory parameters can not be null or empty";

    private static final Set<StorageLogbookParameterName> mandatoryParameters = new HashSet<>();

    /**
     * Mandatories parameters initialisation
     */
    static {
        // FIXME: Make mandatory parameters and add new AccessLogParameters
    }

    @JsonIgnore
    private final Map<StorageLogbookParameterName, String> mapParameters = new TreeMap<>();

    /**
     * Set directly at least all mandatory parameters in the
     * AccessLogParameters. This constructor checks if all mandatory
     * parameters are set
     *
     * @param mapParameters
     *            The initial parameters (MUST contains mandatory parameters
     * @throws StorageException
     */
    public AccessLogParameters(Map<StorageLogbookParameterName, String> mapParameters) {
        this.mapParameters.putAll(mapParameters);
        checkMandatoryParameters();
    }

    /**
     * Get the event date time as a local date time
     *
     * @return the local date time as a LocalDateTime
     */
    @JsonIgnore
    public LocalDateTime getEventDateTime() {
        final String date = mapParameters.get(StorageLogbookParameterName.eventDateTime);
        return LocalDateTime.parse(date);
    }

    /**
     * set The status of the operation
     *
     * @param outcome
     *            the outcome
     * @return the AccessLogParameters after the parameter has been added
     */
    @JsonIgnore
    public AccessLogParameters setStatus(StorageLogbookOutcome outcome) {
        mapParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        return this;
    }

    /**
     * Get the status as an outcome
     *
     * @return the status as a StorageLogbookOutcome
     */
    @JsonIgnore
    public StorageLogbookOutcome getStatus() {
        final String status = mapParameters.get(StorageLogbookParameterName.outcome);
        return StorageLogbookOutcome.valueOf(status);
    }

    /**
     * Check if mandatories parameters are not empty or null
     *
     * @return true if mandatories parameters are ok
     * @throws IllegalArgumentException
     *             thrown when one parameter is empty or null
     */
    public boolean checkMandatoryParameters() throws IllegalArgumentException {
        for (final StorageLogbookParameterName s : mandatoryParameters) {
            ParametersChecker.checkParameter(MANDATORY_PARAMETER_CAN_NOT_BE_NULL_OR_EMPTY, mapParameters.get(s));
        }
        return true;
    }

    /**
     * Get the parameters map
     *
     * @return the parameters map
     */
    @JsonIgnore
    public Map<StorageLogbookParameterName, String> getMapParameters() {
        return mapParameters;
    }

    /**
     * set The output detail message of the operation
     *
     * @param outcomeDetailMessage
     *            the output message
     * @return the AccessLogParameters after the parameter has been added
     */
    @JsonIgnore
    public AccessLogParameters setOutcomDetailMessage(String outcomeDetailMessage) {
        mapParameters.put(StorageLogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
        return this;
    }

    /**
     * set The External Object Identifier
     *
     * @param objectIdentifierIncome
     *            the External Object Identifier
     * @return the AccessLogParameters after the parameter has been added
     */
    @JsonIgnore
    public AccessLogParameters setObjectIdentifierIncome(String objectIdentifierIncome) {
        mapParameters.put(StorageLogbookParameterName.objectIdentifierIncome, objectIdentifierIncome);
        return this;
    }
}
