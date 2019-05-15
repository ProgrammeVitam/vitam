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
package fr.gouv.vitam.logbook.common.parameters;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;

/**
 * Helper implementation of LogbookOperationsClient
 */
public class LogbookOperationsClientHelper {

    /**
     * event Detetail Data Type
     */
    public static final String EV_DET_DATA_TYPE = "evDetDataType";
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();
    private final Map<String, Queue<LogbookOperationParameters>> delegatedCreations = new ConcurrentHashMap<>();
    private final Map<String, Queue<LogbookOperationParameters>> delegatedUpdates = new ConcurrentHashMap<>();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientHelper.class);
    
    /**
     * Constructor
     */
    public LogbookOperationsClientHelper() {
        // Empty
    }

    /**
     * Check validity of the input and add default date and ServerIdentity
     *
     * @param parameters to check
     * @return the primary key
     */
    public static final String checkLogbookParameters(LogbookOperationParameters parameters) {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        if (!LogbookTypeProcess.EXTERNAL_LOGBOOK.equals(parameters.getTypeProcess())
            || parameters.getParameterValue(LogbookParameterName.eventDateTime) == null ) {
            parameters.putParameterValue(LogbookParameterName.eventDateTime,
                LocalDateUtil.now().toString());
        } else {
            try {
                LocalDateUtil.getDate(parameters.getParameterValue(LogbookParameterName.eventDateTime));
                LOGGER.warn("External date : "+ parameters.getParameterValue(LogbookParameterName.eventDateTime));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Wrong date format : "+ parameters.getParameterValue(LogbookParameterName.eventDateTime));
            }
        }
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        return parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
    }

    /**
     * Create a copy in order to allow reuse on client side
     *
     * @param source to copy and reuse
     * @return the copy of the source
     */
    public static final LogbookOperationParameters copy(LogbookOperationParameters source) {
        final LogbookOperationParameters copy = LogbookParametersFactory.newLogbookOperationParameters();
        copy.getMapParameters().putAll(source.getMapParameters());
        return copy;
    }

    /**
     * Create logbook entry using delegation<br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one created)
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    public void createDelegate(LogbookOperationParameters parameters) throws LogbookClientAlreadyExistsException {
        final String key = checkLogbookParameters(parameters);
        if (delegatedCreations.containsKey(key)) {
            throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
        }
        final Queue<LogbookOperationParameters> queue = new ConcurrentLinkedQueue<>();
        queue.add(copy(parameters));
        delegatedCreations.put(key, queue);
    }

    /**
     * Update logbook entry using delegation<br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one updated)
     * @throws LogbookClientNotFoundException if the element does not yet exists (createDeletage not called before)
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    public void updateDelegate(LogbookOperationParameters parameters) throws LogbookClientNotFoundException {
        final String key = checkLogbookParameters(parameters);
        Queue<LogbookOperationParameters> queue = delegatedCreations.get(key);
        if (queue == null) {
            // Switch to update part
            queue = delegatedUpdates.get(key);
            if (queue == null) {
                // New Update part
                queue = new ConcurrentLinkedQueue<>();
                delegatedUpdates.put(key, queue);
            }
        }
        queue.add(copy(parameters));
    }

    /**
     *
     * @param key of element to remove
     * @return the associated finalize Delegate Queue for creation
     */
    public Queue<LogbookOperationParameters> removeCreateDelegate(String key) {
        return delegatedCreations.remove(key);
    }

    /**
     *
     * @param key of element to remove
     * @return the associated finalize Delegate Queue for update
     */
    public Queue<LogbookOperationParameters> removeUpdateDelegate(String key) {
        return delegatedUpdates.remove(key);
    }

    /**
     * Clear the underlying data structures
     */
    public void clear() {
        delegatedCreations.clear();
        delegatedUpdates.clear();
    }
}
