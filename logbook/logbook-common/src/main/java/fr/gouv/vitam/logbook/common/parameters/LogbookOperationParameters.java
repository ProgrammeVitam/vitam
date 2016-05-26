/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parameters for the logbook operation
 */
public class LogbookOperationParameters implements LogbookParameters {

    private static Set<String> mandatoryParameters = new HashSet<>();

    /**
     * Initialize mandatories fields list
     */
    static {
        mandatoryParameters.add(LogbookParameterName.eventTypeProcess.name());
        mandatoryParameters.add(LogbookParameterName.outcome.name());
        mandatoryParameters.add(LogbookParameterName.outcomeDetail.name());
        mandatoryParameters.add(LogbookParameterName.outcomeDetailMessage.name());
        mandatoryParameters.add(LogbookParameterName.agentIdentifierApplicationSession.name());
        mandatoryParameters.add(LogbookParameterName.eventIdentifier.name());
        mandatoryParameters.add(LogbookParameterName.eventIdentifierProcess.name());
        mandatoryParameters.add(LogbookParameterName.eventType.name());
        mandatoryParameters.add(LogbookParameterName.eventIdentifierRequest.name());
    }

    private Map<String, String> mapParameters = new HashMap<>();

    /**
     * Set parameterValue on mapParamaters with parameterName key
     * <br /><br />If parameterKey already exists, the override it (no check)
     *
     * @param parameterName  the key of the parameter to put on the paramater map
     * @param parameterValue the value to put on the parameter map
     * @return actual instance of LogbookOperationParameters (fluent like)
     */
    public LogbookOperationParameters setParameterValue(String parameterName, String parameterValue) {
        this.mapParameters.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public Set<String> getMandatoriesParameters() {
        return mandatoryParameters;
    }

    @Override
    public Map<String, String> getMapParameters() {
        return this.mapParameters;
    }
}
