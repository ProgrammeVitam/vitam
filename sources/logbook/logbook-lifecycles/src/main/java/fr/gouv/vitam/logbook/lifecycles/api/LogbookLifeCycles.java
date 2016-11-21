/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.logbook.lifecycles.api;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * Core API for LifeCycles
 */
public interface LogbookLifeCycles {

    /**
     * Create and insert logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle unit identifier
     * @param parameters
     * @throws LogbookAlreadyExistsException if an LifeCycle with the same eventIdentifierProcess and outcome="Started"
     *         already exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void createUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException;

    /**
     * Create and insert logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @param parameters
     * @throws LogbookAlreadyExistsException if an LifeCycle with the same eventIdentifierProcess and outcome="Started"
     *         already exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void createObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException;


    /**
     * Update logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @param parameters
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void updateUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Update logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @param parameters
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Select logbook LifeCycle entries
     *
     * @param select the select request in format of JsonNode
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycleUnit> selectUnit(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entries
     *
     * @param select the select request in format of JsonNode
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycleObjectGroup> selectObjectGroup(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entry by operation
     *
     * @param idOperation
     * @param idLc
     * @return the Unit Logbook Lifecycle
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws InvalidParseOperationException
     */
    LogbookLifeCycleUnit getUnitByOperationIdAndByUnitId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entry by operation
     *
     * @param idOperation
     * @param idLc
     * @return the ObjectGroup Logbook Lifecycle
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws InvalidParseOperationException
     */
    LogbookLifeCycleObjectGroup getObjectGroupByOperationIdAndByObjectGroupId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;


    /**
     * Rollback logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void rollbackUnit(String idOperation, String idLc) throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Rollback logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     */
    void rollbackObjectGroup(String idOperation, String idLc) throws LogbookNotFoundException, LogbookDatabaseException;


    /**
     * Select logbook life cycle by the lifecycle's ID
     * 
     * @param idUnit
     * @return the logbook LifeCycle found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     */
    LogbookLifeCycleUnit getUnitById(String idUnit) throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Select logbook life cycle by the lifecycle's ID
     *
     * @param idObject
     * @return the logbook LifeCycle found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     */
    LogbookLifeCycleObjectGroup getObjectGroupById(String idObject)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create a cursor for all Unit Lifecycles from one operation
     * 
     * @param operationId
     * @param select
     * @return the X-Cursor-Id
     * @throws LogbookDatabaseException
     */
    public String createCursorUnit(String operationId, JsonNode select)
        throws LogbookDatabaseException;

    /**
     * Get the next available Unit Lifecycle
     * 
     * @param cursorId
     * @return the next available
     * @throws LogbookNotFoundException if there is no more entry
     * @throws LogbookDatabaseException if the cursor is not found
     */
    public LogbookLifeCycleUnit getCursorUnitNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Create a cursor for all ObjectGroup Lifecycles from one operation
     * 
     * @param operationId
     * @param select
     * @return the X-Cursor-Id
     * @throws LogbookDatabaseException
     */
    public String createCursorObjectGroup(String operationId, JsonNode select)
        throws LogbookDatabaseException;

    /**
     * Get the next available ObjectGroup Lifecycle
     * 
     * @param cursorId
     * @return the next available
     * @throws LogbookNotFoundException if there is no more entry
     * @throws LogbookDatabaseException if the cursor is not found
     */
    public LogbookLifeCycleObjectGroup getCursorObjectGroupNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Finalize the cursor
     * 
     * @param cursorId
     */
    public void finalizeCursor(String cursorId);
    

    /**
     * Create one Logbook Lifecycle with already multiple sub-events
     * 
     * @param idOp Operation Id
     * @param lifecycleArray with first and next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update one Logbook Lifecycle with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook Lifecycle entry in "events" array.
     * 
     * @param idOp Operation Id
     * @param lifecycleArray containing all Lifecycle Logbook in order
     * 
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookNotFoundException;

}
