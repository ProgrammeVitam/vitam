/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.helper.LogbookParametersHelper;

/**
 * Parameters for the logbook operation
 */
@JsonSerialize(using = LogbookOperationSerializer.class)
public class LogbookOperationParameters implements LogbookParameters {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationParameters.class);

    // Note: enum has declaration order comparable property
    @JsonIgnore
    private final Map<LogbookParameterName, String> mapParameters = new TreeMap<>();

    @JsonIgnore
    private final Set<LogbookParameterName> mandatoryParameters;

    /**
     * To keep compatibility
     *
     * @deprecated set initialize by factory
     */
    @JsonIgnore
    @Deprecated
    private static final Set<LogbookParameterName> deprecatedMandatoryParameters = new HashSet<>();

    /**
     * Initialize mandatories fields list
     */
    static {
        deprecatedMandatoryParameters.add(LogbookParameterName.eventIdentifier);
        deprecatedMandatoryParameters.add(LogbookParameterName.eventType);
        deprecatedMandatoryParameters.add(LogbookParameterName.eventIdentifierProcess);
        deprecatedMandatoryParameters.add(LogbookParameterName.eventTypeProcess);
        deprecatedMandatoryParameters.add(LogbookParameterName.outcome);
        deprecatedMandatoryParameters.add(LogbookParameterName.outcomeDetailMessage);
        deprecatedMandatoryParameters.add(LogbookParameterName.eventIdentifierRequest);
    }

    /**
     * Constructor to keep compatibility
     *
     * @deprecated use the LogbookParametersFactory to get the {@link LogbookOperationParameters}
     */
    @JsonIgnore
    @Deprecated
    public LogbookOperationParameters() {
        mandatoryParameters = deprecatedMandatoryParameters;
    }

    /**
     * Constructor use by the factory to initialize the set of mandatories
     *
     * @param mandatory the mandatories fields Set
     */
    @JsonIgnore
    public LogbookOperationParameters(final Set<LogbookParameterName> mandatory) {
        mandatoryParameters = mandatory;
    }

    /**
     * Builder for REST interface
     * 
     * @param map
     * @throws IllegalArgumentException if one key is not allowed
     */
    @JsonCreator
    protected LogbookOperationParameters(Map<String, String> map) {
        mandatoryParameters = LogbookParametersFactory.getDefaultMandatory();
        setMap(map);
    }


    @Override
    public LogbookParameters setMap(Map<String, String> map) {
        for (final Entry<String, String> item : map.entrySet()) {
            final LogbookParameterName lpname = LogbookParameterName.valueOf(item.getKey());
            mapParameters.put(lpname, item.getValue());
        }
        return this;
    }

    @Override
    public LogbookParameters setFromParameters(LogbookParameters parameters) {
        for (final LogbookParameterName item : LogbookParameterName.values()) {
            mapParameters.put(item, parameters.getParameterValue(item));
        }
        return this;
    }

    @JsonIgnore
    @Override
    public LogbookOperationParameters putParameterValue(LogbookParameterName parameterName, String parameterValue) {
        LogbookParametersHelper.checkNullOrEmptyParameter(parameterName, parameterValue, getMandatoriesParameters());
        mapParameters.put(parameterName, parameterValue);
        return this;
    }

    @JsonIgnore
    @Override
    public String getParameterValue(LogbookParameterName parameterName) {
        ParametersChecker.checkParameter("Parameter cannot be null or empty", parameterName);
        return mapParameters.get(parameterName);
    }

    @JsonIgnore
    @Override
    public Set<LogbookParameterName> getMandatoriesParameters() {
        return mandatoryParameters;
    }

    @JsonIgnore
    @Override
    public Map<LogbookParameterName, String> getMapParameters() {
        return mapParameters;
    }

    @JsonIgnore
    @Override
    public LocalDateTime getEventDateTime() {
        final String date = mapParameters.get(LogbookParameterName.eventDateTime);
        if (date != null) {
            return LocalDateTime.parse(date);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public LogbookOperationParameters setStatus(LogbookOutcome outcome) {
        mapParameters.put(LogbookParameterName.outcome, outcome.name());
        return this;
    }

    @JsonIgnore
    @Override
    public LogbookOutcome getStatus() {
        final String status = mapParameters.get(LogbookParameterName.outcome);
        if (status != null) {
            return LogbookOutcome.valueOf(status);
        }
        return null;
    }

    @JsonIgnore
    protected static final Set<LogbookParameterName> getDeprecatedmandatoryparameters() {
        return deprecatedMandatoryParameters;
    }

    @JsonIgnore
    @Override
    public LogbookOperationParameters setTypeProcess(LogbookTypeProcess process) {
        mapParameters.put(LogbookParameterName.eventTypeProcess, process.name());
        return this;
    }

    @JsonIgnore
    @Override
    public LogbookTypeProcess getTypeProcess() {
        final String process = mapParameters.get(LogbookParameterName.eventTypeProcess);
        if (process != null) {
            return LogbookTypeProcess.valueOf(process);
        }
        return null;
    }

    @Override
    public String toString() {
        try {
            return JsonHandler.writeAsString(mapParameters);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Cannot convert to String", e);
            return mapParameters.toString();
        }
    }
}
