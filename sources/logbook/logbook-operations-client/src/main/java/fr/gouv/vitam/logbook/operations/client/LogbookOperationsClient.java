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
package fr.gouv.vitam.logbook.operations.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

/**
 * Logbook client interface
 */
public interface LogbookOperationsClient extends BasicClient {

    /**
     * Create logbook entry <br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     * @throws LogbookClientException if client received an error from server
     */
    void create(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException;

    /**
     * Update logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void update(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * @param select
     * @return logbook operation as JsonNode
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectOperation(JsonNode select) throws LogbookClientException, InvalidParseOperationException;
    JsonNode selectOperationSliced(JsonNode select) throws LogbookClientException, InvalidParseOperationException;

    /**
     * @param id identifier
     * @return logbook operation as String
     * @throws LogbookClientException LogbookClientException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    JsonNode selectOperationById(String id) throws LogbookClientException, InvalidParseOperationException;


    /**
     * Create logbook entry using delegation<br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one created)
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void createDelegate(LogbookOperationParameters parameters)
        throws LogbookClientAlreadyExistsException;

    /**
     * Update logbook entry using delegation<br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters (can be reused and modified after without impacting the one updated)
     * @throws LogbookClientNotFoundException if the element does not yet exists (createDeletage not called before)
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void updateDelegate(LogbookOperationParameters parameters) throws LogbookClientNotFoundException;

    /**
     * Bulk Create<br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param eventIdProc event Process Identifier
     * @param queue queue of LogbookOperationParameters to create
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkCreate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException;

    /**
     * Bulk Update<br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param eventIdProc event Process Identifier
     * @param queue queue of LogbookOperationParameters to update
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkUpdate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Finalize logbook entry using delegation<br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param eventIdProc event Process Identifier
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commitCreateDelegate(String eventIdProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException;

    /**
     * Finalize logbook entry using delegation<br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param eventIdProc event Process Identifier
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commitUpdateDelegate(String eventIdProc)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;


    /**
     * Call traceability logbook operation <br>
     * <br>
     *
     * @return logbook operation as String
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    RequestResponseOK traceability() throws LogbookClientServerException, InvalidParseOperationException;

    /**
     * Starts Unit logbook lifecycle traceability
     *
     * @return logbook lifecycles as String
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    RequestResponseOK traceabilityLfcUnit() throws LogbookClientServerException, InvalidParseOperationException;

    /**
     * Starts Object Group logbook lifecycle traceability
     *
     * @return logbook lifecycles as String
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    RequestResponseOK traceabilityLfcObjectGroup() throws LogbookClientServerException, InvalidParseOperationException;

    /**
     * Check life cycle traceability status (unit  / got)
     *
     * @param processId the process id
     * @return lifecycle traceability status
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    LifecycleTraceabilityStatus checkLifecycleTraceabilityWorkflowStatus(String processId)
        throws LogbookClientServerException, InvalidParseOperationException;

    /**
     * Reindex a collection with parameters
     *
     * @param indexParam reindexation parameters
     * @return JsonObject containing information about the newly created index
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, LogbookClientServerException;

    /**
     * Switch indexes
     *
     * @param switchIndexParam switch index parameters
     * @return JsonObject containing information about the newly created index
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     */
    JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, LogbookClientServerException;

    void traceabilityAudit(int tenant, AuditLogbookOptions options) throws LogbookClientServerException;

    /**
     * checkLogbookCoherence
     *
     * @return result
     * @throws LogbookClientServerException
     */
    LogbookCheckResult checkLogbookCoherence() throws LogbookClientServerException;
}
