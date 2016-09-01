/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.logbook.common.parameters;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;

/**
 * Abstract class for all Parameters in Logbook
 */
abstract class AbstractParameters implements LogbookParameters {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractParameters.class);

    @JsonIgnore
    private final Map<LogbookParameterName, String> mapParameters = new TreeMap<>();

    @JsonIgnore
    protected Set<LogbookParameterName> mandatoryParameters;

    /**
     * Constructor using mandatory definition
     * 
     * @param mandatory
     */
    AbstractParameters(final Set<LogbookParameterName> mandatory) {
        mandatoryParameters = mandatory;
    }

    /**
     * Builder for REST interface
     *
     * @param map
     * @throws IllegalArgumentException if one key is not allowed
     */
    @JsonCreator
    protected AbstractParameters(Map<String, String> map) {
        mandatoryParameters = LogbookParametersFactory.getDefaultOperationMandatory();
        setMap(map);
    }

    @JsonIgnore
    @Override
    public Set<LogbookParameterName> getMandatoriesParameters() {
        return mandatoryParameters;
    }

    @JsonIgnore
    @Override
    public LocalDateTime getEventDateTime() {
        final String date = mapParameters.get(LogbookParameterName.eventDateTime);
        if (!StringUtils.isBlank(date)) {
            return LocalDateTime.parse(date);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public LogbookParameters setStatus(LogbookOutcome outcome) {
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
    @Override
    public LogbookParameters setTypeProcess(LogbookTypeProcess process) {
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
    @JsonIgnore
    public LogbookParameters putParameterValue(LogbookParameterName parameterName, String parameterValue) {
        ParameterHelper.checkNullOrEmptyParameter(parameterName, parameterValue, getMandatoriesParameters());
        mapParameters.put(parameterName, parameterValue);
        return this;
    }

    @JsonIgnore
    @Override
    public String getParameterValue(LogbookParameterName parameterName) {
        ParametersChecker.checkParameter("Parameter cannot be null or empty", parameterName);
        return mapParameters.get(parameterName);
    }

    @Override
    public LogbookParameters setMap(Map<String, String> map) {
        for (final Map.Entry<String, String> item : map.entrySet()) {
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
    public Map<LogbookParameterName, String> getMapParameters() {
        return mapParameters;
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
