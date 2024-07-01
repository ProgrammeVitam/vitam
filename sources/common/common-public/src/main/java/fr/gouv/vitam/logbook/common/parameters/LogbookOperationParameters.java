/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parameters for the logbook operation
 */
@JsonSerialize(using = LogbookOperationParametersSerializer.class)
@JsonDeserialize(using = LogbookOperationParametersDeserializer.class)
public class LogbookOperationParameters extends AbstractParameters {

    @JsonProperty("events")
    private Set<LogbookParameters> events = new HashSet<>();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationParameters.class);

    /**
     * Constructor use by the factory to initialize the set of mandatories
     *
     * @param mandatory the mandatories fields Set
     */
    @JsonIgnore
    LogbookOperationParameters(final Set<LogbookParameterName> mandatory) {
        super(mandatory);
    }

    /**
     * Builder for REST interface
     *
     * @param map
     * @throws IllegalArgumentException if one key is not allowed
     */
    @JsonCreator
    protected LogbookOperationParameters(Map<String, String> map) {
        super(LogbookParametersFactory.getDefaultOperationMandatory());
        setMap(map);
    }

    @Override
    public String toString() {
        try {
            HashMap<String, Object> finalMap = new HashMap<>();
            for (final Entry<LogbookParameterName, String> item : getMapParameters().entrySet()) {
                finalMap.put(item.getKey().name(), item.getValue());
            }
            finalMap.put(LogbookParameterName.events.name(), getEvents());

            return JsonHandler.writeAsString(finalMap);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Cannot convert to String", e);
            return getMapParameters().toString();
        }
    }

    /**
     * Set event list
     *
     * @param events the list of events
     */
    @JsonProperty("events")
    public void setEvents(Set<LogbookParameters> events) {
        this.events = events;
    }

    /**
     * Get event list
     *
     * @return the event list
     */
    @JsonProperty("events")
    public Set<LogbookParameters> getEvents() {
        return events;
    }
}
