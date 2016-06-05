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
package fr.gouv.vitam.logbook.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDbAccess interface
 */
public interface MongoDbAccess {

    /**
     * Close database access
     */
    void close();

    /**
     *
     * @return the current number of Logbook Operation
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    long getLogbookOperationSize() throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     *
     * @return the current number of Logbook LifeCyle
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    long getLogbookLifeCyleSize() throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Check if one eventIdentifier for Operation exists already
     *
     * @param operationItem
     * @return True if one LogbookOperation exists with this id
     *
     * @throws LogbookDatabaseException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    boolean existsLogbookOperation(final LogbookOperationParameters operationItem) throws LogbookDatabaseException;

    /**
     * Check if one eventIdentifier for Lifecycle exists already
     *
     * @param lifecycleItem
     * @return True if one LogbookLibeCycle exists with this id
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    boolean existsLogbookLifeCycle(final LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Operation
     *
     * @param eventIdentifierProcess
     * @return the corresponding LogbookOperation if it exists
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookOperation getLogbookOperation(final String eventIdentifierProcess)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Lifecycle
     *
     * @param objectIdentifier
     * @return the corresponding LogbookLibeCycle if it exists
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookLifeCycle getLogbookLifeCycle(final String objectIdentifier)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create one Logbook Operation
     *
     * @param operationItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void createLogbookOperation(final LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle
     *
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void createLogbookLifeCycle(final LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update one Logbook Operation <br>
     * <br>
     * It adds this new entry within the very same Logbook Operaton entry in "events" array.
     *
     * @param operationItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Update one Logbook LifeCycle <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void updateLogbookLifeCycle(LogbookParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create one Logbook Operation with already multiple sub-events
     *
     * @param operationItem to be created event
     * @param operationItems next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookOperation(final LogbookOperationParameters operationItem,
        LogbookOperationParameters... operationItems) throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle with already multiple sub-events
     *
     * @param lifecycleItem to be created event
     * @param lifecycleItems next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookLifeCycle(LogbookParameters lifecycleItem,
        LogbookParameters... lifecycleItems) throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update one Logbook Operation with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook Operaton entry in "events" array.
     *
     * @param operationItems
     *
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookOperation(LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Update one Logbook LifeCycle with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param lifecycleItems
     *
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookLifeCycle(LogbookParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook Operation through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookOperation
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookOperation> getLogbookOperations(String select)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook Operation through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookOperation
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookOperation> getLogbookOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookLifeCycle> getLogbookLifeCycles(String select)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookLifeCycle> getLogbookLifeCycles(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException;

}
