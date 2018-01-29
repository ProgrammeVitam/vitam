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
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
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
     * @param parameters the logbook lifecycle parameters
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
     * @param parameters the logbook lifecycle parameters
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
     * @param parameters the logbook lifecycle parameters
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookAlreadyExistsException if the entry already exists
     */
    void updateUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @param parameters the logbook lifecycle parameters
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookAlreadyExistsException if the entry already exists
     */
    void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException,
        LogbookAlreadyExistsException;

    /**
     * Update logbook LifeCycle entries
     *
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @param parameters the logbook lifecycle parameters
     * @param commit if true update is done on committed collection otherwise on inProcess one
     * @throws LogbookNotFoundException if no LifeCycle with the same eventIdentifierProcess exists
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookAlreadyExistsException if the entry already exists
     */
    void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters, boolean commit)
            throws LogbookNotFoundException, LogbookDatabaseException,
            LogbookAlreadyExistsException;

    /**
     * Select logbook LifeCycle entries
     *
     * @param select the select request in format of JsonNode
     * @param collection the collection on which the select operation will be done : Production collection
     *        (LIFECYCLE_UNIT) or Working collection (LIFECYCLE_UNIT_IN_PROCESS)
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycle> selectUnit(JsonNode select, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entries
     *
     * @param select the select request in format of JsonNode
     * @param sliced the boolean sliced filtering events or not
     * @param collection the collection on which the select operation will be done : Production collection
     *        (LIFECYCLE_UNIT) or Working collection (LIFECYCLE_UNIT_IN_PROCESS)
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycle> selectUnit(JsonNode select, boolean sliced, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Selects object group life cycle entries
     *
     * @param select the select request in format of JsonNode
     * @param collection the collection on which the select operation will be done : Production collection
     *        (LIFECYCLE_OBJECT_GROUP) or Working collection (LIFECYCLE_OBJECT_GROUP_IN_PROCESS)
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycle> selectObjectGroup(JsonNode select, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Selects object group life cycle entries
     *
     * @param select the select request in format of JsonNode
     * @param sliced the boolean sliced filtering events or not
     * @param collection the collection on which the select operation will be done : Production collection
     *        (LIFECYCLE_OBJECT_GROUP) or Working collection (LIFECYCLE_OBJECT_GROUP_IN_PROCESS)
     * @return List of the logbook LifeCycle
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    List<LogbookLifeCycle> selectObjectGroup(JsonNode select, boolean sliced, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entry by operation
     *
     * @param idOperation the operation id
     * @param idLc the logbook identifier
     * @return the Unit Logbook Lifecycle
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
     */
    LogbookLifeCycleUnit getUnitByOperationIdAndByUnitId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException;

    /**
     * Select logbook LifeCycle entry by operation
     *
     * @param idOperation the operation identifier
     * @param idLc the logbook identifier
     * @return the ObjectGroup Logbook Lifecycle
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     * @throws InvalidParseOperationException if invalid parse for selecting the LifeCycle
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
     * @param idUnit the unit identifier
     * @return the logbook LifeCycle found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     */
    LogbookLifeCycleUnit getUnitById(String idUnit) throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Selects logbook life cycle by lifecycle ID (using a queryDsl)
     *
     * @param queryDsl the query as a json
     * @param collection the collection on which the select operation will be done : Production collection
     *        (LIFECYCLE_UNIT) or Working collection (LIFECYCLE_UNIT_IN_PROCESS)
     * @return the logbook LifeCycle found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     */
    LogbookLifeCycle getUnitById(JsonNode queryDsl, LogbookCollections collection)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Select logbook life cycle by the lifecycle's ID
     *
     * @param idObject the object group identifier
     * @return the logbook LifeCycle found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no LifeCycle selected cannot be found
     */
    LogbookLifeCycleObjectGroup getObjectGroupById(String idObject)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create a cursor for all Unit Lifecycles from one operation
     *
     * @param operationId the operation id
     * @param select the query in format JsonNode
     * @param logbookCollection the collection on which the cursor creation operation will be done : Production collection
     *        (LIFECYCLE_UNIT) or Working collection (LIFECYCLE_UNIT_IN_PROCESS)
     * @return the X-Cursor-Id
     * @throws LogbookDatabaseException if the cursor is not found
     */
    String createCursorUnit(String operationId, JsonNode select, LogbookCollections logbookCollection)
        throws LogbookDatabaseException;

    /**
     * Get the next available Unit Lifecycle
     *
     * @param cursorId the cursor id
     * @return the next available
     * @throws LogbookNotFoundException if there is no more entry
     * @throws LogbookDatabaseException if the cursor is not found
     */
    LogbookLifeCycle getCursorUnitNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Create a cursor for all ObjectGroup Lifecycles from one operation
     *
     * @param operationId the operation id
     * @param select the query to create cursor
     * @param collection the collection on which the cursor creation operation will be done : Production collection
     *        (LIFECYCLE_OBJECT_GROUP) or Working collection (LIFECYCLE_OBJECT_GROUP_IN_PROCESS)
     * @return the X-Cursor-Id
     * @throws LogbookDatabaseException if the cursor is not found
     */
    String createCursorObjectGroup(String operationId, JsonNode select, LogbookCollections collection)
        throws LogbookDatabaseException;

    /**
     * Get the next available ObjectGroup Lifecycle
     *
     * @param cursorId the cursor id 
     * @return the next available
     * @throws LogbookNotFoundException if there is no more entry
     * @throws LogbookDatabaseException if the cursor is not found
     */
    public LogbookLifeCycle getCursorObjectGroupNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Finalize the cursor
     *
     * @param cursorId the cursor id
     */
    public void finalizeCursor(String cursorId);


    /**
     * Create one Logbook Lifecycle with already multiple sub-events
     *
     * @param idOp Operation Id
     * @param lifecycleArray with first and next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookAlreadyExistsException if LifeCycle already exists
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
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     * @throws LogbookAlreadyExistsException if LifeCycle already exists
     */
    void updateBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException;

    /**
     * Commits Unit lifeCycle
     * 
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     * @throws LogbookAlreadyExistsException if LifeCycle already exists
     */
    void commitUnit(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException;

    /**
     * Commits ObjectGroup lifeCycle
     * 
     * @param idOperation the operation identifier
     * @param idLc the lifecycle identifier
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     * @throws LogbookAlreadyExistsException if LifeCycle already exists
     */
    void commitObjectGroup(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException;

    /**
     * Removes the created unit lifeCycles during a given operation
     * 
     * @param idOperation the operation id
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     */
    void rollBackUnitsByOperation(String idOperation) throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Removes the created object groups lifeCycles during a given operation
     * 
     * @param idOperation the operation id
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     */
    void rollBackObjectGroupsByOperation(String idOperation)
        throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Returns the LifeCycle Status for a given unit Id
     * 
     * @param unitId the unit Id
     * @return the lifeCycleStatusCode
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     */
    LifeCycleStatusCode getUnitLifeCycleStatus(String unitId) throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Returns the LifeCycle Status for a given objectGroup Id
     * 
     * @param objectGroupId the objectGroup Id
     * @return the lifeCycleStatusCode
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if LifeCycle cannot be found
     */
    LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Bulk method
     * 
     * @param collections the logbook collections
     * @param idOp operation identifier
     * @param logbookLifeCycleModels lifecycles to be created
     */
    void bulk(LogbookCollections collections, String idOp, List<? extends LogbookLifeCycleModel> logbookLifeCycleModels);
}
