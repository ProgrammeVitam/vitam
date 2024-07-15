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
package fr.gouv.vitam.logbook.common.parameters;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Helper implementation of LogbookLifeCyclesClient
 */
@Deprecated
public class LogbookLifeCyclesClientHelper {

    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();
    private final Map<String, Queue<LogbookLifeCycleParameters>> delegatedCreations = new ConcurrentHashMap<>();
    private final Map<String, Queue<LogbookLifeCycleParameters>> delegatedUpdates = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    public LogbookLifeCyclesClientHelper() {
        // Empty
    }

    /**
     * Check validity of the input and add default date and ServerIdentity
     *
     * @param parameters to check
     * @return the primary key
     */
    public static String checkLogbookParameters(LogbookLifeCycleParameters parameters) {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        return parameters.getParameterValue(LogbookParameterName.objectIdentifier);
    }

    /**
     * Create a copy in order to allow reuse on client side
     *
     * @param source to copy and reuse
     * @return the copy of the source
     */
    public static LogbookLifeCycleParameters copy(LogbookLifeCycleParameters source) {
        LogbookLifeCycleParameters copy;
        if (source instanceof LogbookLifeCycleObjectGroupParameters) {
            copy = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        } else {
            copy = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        }
        copy.getMapParameters().putAll(source.getMapParameters());
        return copy;
    }

    /**
     * Create logbook entry using delegation<br>
     * <br>
     * To be used ONLY once at top level of process startup (where objectIdentifier is set for the first time).
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one created)
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    public void createDelegate(LogbookLifeCycleParameters parameters) throws LogbookClientAlreadyExistsException {
        final String key = checkLogbookParameters(parameters);
        if (delegatedCreations.containsKey(key)) {
            throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
        }
        final Queue<LogbookLifeCycleParameters> queue = new ConcurrentLinkedQueue<>();
        queue.add(copy(parameters));
        delegatedCreations.put(key, queue);
    }

    /**
     * Update logbook entry using delegation<br>
     * <br>
     * To be used everywhere except very first time if creation (when objectIdentifier already used once)
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one updated)
     * @throws LogbookClientNotFoundException if the element does not yet exists (createDeletage not called before)
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    public void updateDelegate(LogbookLifeCycleParameters parameters) throws LogbookClientNotFoundException {
        final String key = checkLogbookParameters(parameters);
        Queue<LogbookLifeCycleParameters> queue = delegatedCreations.get(key);
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
     * @param key of element to remove
     * @return the associated finalize Delegate Queue for creation
     */
    public Queue<? extends LogbookLifeCycleParameters> removeCreateDelegate(String key) {
        return delegatedCreations.remove(key);
    }

    /**
     * @param key of element to remove
     * @return the associated finalize Delegate Queue for update
     */
    public Queue<? extends LogbookLifeCycleParameters> removeUpdateDelegate(String key) {
        return delegatedUpdates.remove(key);
    }

    /**
     * @param key of element to remove
     * @return the associated finalize Delegate Queue for update
     */
    public boolean containsUpdate(String key) {
        return delegatedUpdates.containsKey(key) && delegatedUpdates.get(key).size() > 0;
    }

    public boolean containsCreate(String key) {
        return delegatedCreations.containsKey(key) && delegatedCreations.get(key).size() > 0;
    }

    /**
     * @return the Set of LifeCycles entries in creation mode
     */
    public Set<Entry<String, Queue<LogbookLifeCycleParameters>>> getAllCreations() {
        return delegatedCreations.entrySet();
    }

    /**
     * @return the Set of LifeCycles entries in update mode
     */
    public Set<Entry<String, Queue<LogbookLifeCycleParameters>>> getAllUpdates() {
        return delegatedUpdates.entrySet();
    }

    /**
     * Clear the underlying data structures
     */
    public void clear() {
        delegatedCreations.clear();
        delegatedUpdates.clear();
    }

    public void updateDelegateWithKey(String key, LogbookLifeCycleParameters parameters) {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );

        Queue<LogbookLifeCycleParameters> queue = delegatedCreations.get(key);
        if (queue == null) {
            queue = delegatedUpdates.get(key);
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
                delegatedUpdates.put(key, queue);
            }
        }
        queue.add(copy(parameters));
    }
}
