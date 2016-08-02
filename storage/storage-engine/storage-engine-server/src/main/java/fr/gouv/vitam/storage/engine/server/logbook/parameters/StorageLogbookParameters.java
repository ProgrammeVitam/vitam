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
package fr.gouv.vitam.storage.engine.server.logbook.parameters;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Storage Logbook Parameters Class
 */
@JsonSerialize(using = StorageLogbookParametersSerializer.class)
public class StorageLogbookParameters {

    private static final String MANDATORY_PARAMETER_CAN_NOT_BE_NULL_OR_EMPTY =
        "Mandatory parameters can not be null or empty";

    private static final Set<StorageLogbookParameterName> mandatoryParameters = new HashSet<>();

    /**
     * Mandatories parameters initialisation
     */
    static {
        mandatoryParameters.add(StorageLogbookParameterName.eventDateTime);
        mandatoryParameters.add(StorageLogbookParameterName.objectIdentifier);
        mandatoryParameters.add(StorageLogbookParameterName.objectGroupIdentifier);
        mandatoryParameters.add(StorageLogbookParameterName.digest);
        mandatoryParameters.add(StorageLogbookParameterName.digestAlgorithm);
        mandatoryParameters.add(StorageLogbookParameterName.size);
        mandatoryParameters.add(StorageLogbookParameterName.agentIdentifiers);
        mandatoryParameters.add(StorageLogbookParameterName.agentIdentifierRequester);
        mandatoryParameters.add(StorageLogbookParameterName.outcome);
    }

    @JsonIgnore
    private final Map<StorageLogbookParameterName, String> mapParameters = new TreeMap<>();


    /**
     * Put parameter value on parameters map
     *
     * @param parameterName the parameter name to put
     * @param parameterValue the parameter value for the parameter name
     * @return this (fluent style)
     */
    @JsonIgnore
    public StorageLogbookParameters putParameterValue(StorageLogbookParameterName parameterName,
        String parameterValue) {
        mapParameters.put(parameterName, parameterValue);
        return this;
    }

    /**
     * Get the event date time as a local date time
     * 
     * @return the local date time as a LocalDateTime
     */
    @JsonIgnore
    public LocalDateTime getEventDateTime() {
        final String date = mapParameters.get(StorageLogbookParameterName.eventDateTime);
        if (!StringUtils.isBlank(date)) {
            return LocalDateTime.parse(date);
        }
        return null;
    }

    /**
     * set The status of the operation
     * 
     * @param outcome the outcome
     * @return the StorageLogbookParameters after the parameter has been added
     */
    @JsonIgnore
    public StorageLogbookParameters setStatus(StorageLogbookOutcome outcome) {
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
        if (status != null) {
            return StorageLogbookOutcome.valueOf(status);
        }
        return null;
    }



    /**
     * Check if mandatories parameters are not empty or null
     *
     * @return true if mandatories parameters are ok
     * @throws IllegalArgumentException thrown when one parameter is empty or null
     */
    public boolean checkMandatoryParameters() throws IllegalArgumentException {
        for (StorageLogbookParameterName s : mandatoryParameters) {
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

}
