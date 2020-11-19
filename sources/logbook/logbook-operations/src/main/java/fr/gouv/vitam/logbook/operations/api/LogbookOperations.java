/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.logbook.operations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logbook operations interface for database operations
 */
public interface LogbookOperations {

    /**
     * Create and insert logbook operation entries
     *
     * @param parameters the entry parameters
     * @throws fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException if an operation with the same
     * eventIdentifierProcess and outcome="Started" already exists
     * @throws fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException if errors occur while connecting
     * or writing to the database
     */
    void create(LogbookOperationParameters parameters) throws LogbookAlreadyExistsException, LogbookDatabaseException;

    /**
     * Update and insert logbook operation entries
     *
     * @param parameters the entry parameters
     * @throws fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException if no operation with the same
     * eventIdentifierProcess exists
     * @throws fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException if errors occur while connecting
     * or writing to the database
     */
    void update(LogbookOperationParameters parameters) throws LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Select logbook operation entries
     *
     * @param select the select request in format of JsonNode
     * @return List of the logbook operation
     * @throws LogbookNotFoundException if no operation selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the operation
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    List<LogbookOperation> select(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException;

    RequestResponse<LogbookOperation> selectOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException;

    RequestResponseOK<LogbookOperation> selectOperations(JsonNode select, boolean sliced)
        throws VitamDBException, LogbookNotFoundException, LogbookDatabaseException;

    /**
     * Select logbook operation entries
     *
     * @param select the select request in format of JsonNode
     * @param sliced the boolean sliced to filter events or not
     * @return List of the logbook operation
     * @throws LogbookNotFoundException if no operation selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the operation
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    List<LogbookOperation> select(JsonNode select, boolean sliced)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException;

    /**
     * Select logbook operation by the operation's ID
     *
     * @param IdProcess the operation identifier
     * @return the logbook operation found by the ID
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no operation selected cannot be found
     */
    LogbookOperation getById(String IdProcess) throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create one Logbook Operation with already multiple sub-events
     *
     * @param operationArray with first and next events to add/update
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookAlreadyExistsException if logbook already exists
     */
    void createBulkLogbookOperation(LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update one Logbook Operation with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook Operaton entry in "events" array.
     *
     * @param operationArray containing all operations Logbook in order
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws LogbookNotFoundException if no operation selected cannot be found
     */
    void updateBulkLogbookOperation(LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Select all logbook operations entries persisted within provided interval
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the Closeable MongoCursor of LogbookOperation
     * @throws LogbookNotFoundException if no operation selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the operation
     * @throws InvalidCreateOperationException if the query could not be created
     */
    MongoCursor<LogbookOperation> selectOperationsByLastPersistenceDateInterval(LocalDateTime startDate,
        LocalDateTime endDate)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException,
        InvalidCreateOperationException;

    /**
     * Find One logbook TraceabilityOperation after a given date
     *
     * @param date the select request in format of JsonNode
     * @return the LogbookOperation
     * @throws LogbookNotFoundException if no operation selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if invalid parse for selecting the operation
     * @throws InvalidCreateOperationException if the query could not be created
     */
    LogbookOperation findFirstTraceabilityOperationOKAfterDate(LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException;


    /**
     * Find last successful traceability operation
     *
     * @return the last valid traceability operation
     * @throws InvalidCreateOperationException if the query could not be created
     * @throws LogbookNotFoundException if no operation selected cannot be found
     * @throws LogbookDatabaseException if errors occur while connecting or writing to the database
     * @throws InvalidParseOperationException if the query could not be created
     */
    LogbookOperation findLastTraceabilityOperationOK()
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException;

    /**
     * Find last OK or WARNING LFC traceability operation (even if no traceability zip has been generated)
     *
     * @param eventType Logbook event type
     * @param traceabilityWithZipOnly if true, skip operation without Zip (empty operations)
     * @return the last valid traceability operation
     * @throws VitamException if errors occur while retrieving data
     */
    LogbookOperation findLastLifecycleTraceabilityOperation(
        String eventType,
        boolean traceabilityWithZipOnly) throws VitamException;

    /**
     * Reindex one or more collections
     *
     * @param indexParameters the parameters specifying what to reindex
     * @return the reindexation result as a IndexationResult Object
     */
    ReindexationResult reindex(IndexParameters indexParameters);

    /**
     * Switch indexes for one or more collections
     *
     * @param alias the alias name
     * @param newIndexName the new index to be pointed on
     * @throws DatabaseException in case error with database occurs
     * @return
     */
    SwitchIndexResult switchIndex(String alias, String newIndexName) throws DatabaseException;

    boolean checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(
        LocalDateTime traceabilityStartDate, LocalDateTime traceabilityEndDate)
        throws LogbookDatabaseException;

    /**
     * FInd last event of last operation by type
     * @param operationType
     * @return
     * @throws InvalidCreateOperationException
     * @throws LogbookNotFoundException
     * @throws LogbookDatabaseException
     * @throws InvalidParseOperationException
     */
    LogbookOperation findLastOperationByType(String operationType) throws InvalidCreateOperationException,
            LogbookNotFoundException, LogbookDatabaseException, InvalidParseOperationException;
}
