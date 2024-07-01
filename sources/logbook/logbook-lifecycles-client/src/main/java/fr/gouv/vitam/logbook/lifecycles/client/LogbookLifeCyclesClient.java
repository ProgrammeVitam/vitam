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
package fr.gouv.vitam.logbook.lifecycles.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Logbook client interface
 */
public interface LogbookLifeCyclesClient extends BasicClient {
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
    void create(LogbookLifeCycleParameters parameters)
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
    void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Update logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @param lifeCycleStatusCode the lifeCycle status
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void update(LogbookLifeCycleParameters parameters, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Commit logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Rollback logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * returns the unit life cycle
     *
     * @param id unit life cycle id
     * @param queryDsl dsl query to be executed
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the unit life cycle in progress
     *
     * @param id unit life cycle id
     * @param queryDsl dsl query to be executed
     * @param lifeCycleStatus the lifecycle status
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the unit life cycle
     *
     * @param queryDsl dsl query containing the id
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectUnitLifeCycle(JsonNode queryDsl) throws LogbookClientException, InvalidParseOperationException;

    /**
     * Exports raw unit lifecycles by last persisted date range.
     * Warning: If maxEntries limit reached, lifecycles with exact same last persisted date will also be exported.
     * i.e. Max entries is a "soft limit" that can be exceeded if needed.
     * e.g. Setting a 100_000 max limit, can yield 100_045 entries if last 46 lifecycles have the exact same
     * last persistence date.
     *
     * @param startDate the selection start date
     * @param endDate the selection end date
     * @param maxEntries max entries to export (soft limit)
     * @return json line stream of raw life cycles
     * @throws LogbookClientException
     */
    InputStream exportRawUnitLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException;

    /**
     * Exports raw object group lifecycles by last persisted date range
     * Warning: If maxEntries limit reached, lifecycles with exact same last persisted date will also be exported.
     * i.e. Max entries is a "soft limit" that can be exceeded if needed.
     * e.g. Setting a 100_000 max limit, can yield a 100_045 entries if last 46 lifecycles have the exact same
     * last persistence date.
     *
     * @param startDate the selection start date
     * @param endDate the selection end date
     * @param maxEntries max entries to export (soft limit)
     * @return json line stream of raw life cycles
     * @throws LogbookClientException
     */
    InputStream exportRawObjectGroupLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException;

    /**
     * returns the object group life cycle
     *
     * @param id the object group life cycle id
     * @param queryDsl dsl query to be executed
     * @return the object group life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the object group life cycle
     *
     * @param id the object group life cycle id
     * @param queryDsl dsl query to be executed
     * @param lifeCycleStatus the lifecycle status
     * @return the object group life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the object group life cycles
     *
     * @param queryDsl dsl query to be executed
     * @return the object group life cycles
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException;

    JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException;

    CloseableIterator<JsonNode> objectGroupLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) throws LogbookClientException;

    CloseableIterator<JsonNode> unitLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) throws LogbookClientException;

    /**
     * Bulk Create for Unit<br>
     * <br>
     * To be used ONLY once at top level of process startup (where objectIdentifier is set for the first time).
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to create
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkCreateUnit(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException;

    /**
     * Bulk Update for Unit<br>
     * <br>
     * To be used everywhere except very first time (when objectIdentifier already used once)
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to update
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkUpdateUnit(String objectIdentifier, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Bulk Create for ObjectGroup<br>
     * <br>
     * To be used ONLY once at top level of process startup (where objectIdentifier is set for the first time).
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to create
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkCreateObjectGroup(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException;

    /**
     * Bulk Update for ObjectGroup<br>
     * <br>
     * To be used everywhere except very first time (when objectIdentifier already used once)
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to update
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkUpdateObjectGroup(String objectIdentifier, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Commit unit lifeCycle <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param operationId the operation id
     * @param unitId the unit id
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commitUnit(String operationId, String unitId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Commit objectGroup lifeCycle <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param operationId the operation id
     * @param objectGroupId the object group id
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commitObjectGroup(String operationId, String objectGroupId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Remove created unit lifeCycles during the given operation
     *
     * @param operationId the operation id
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientServerException if the server got an internal error
     */
    void rollBackUnitsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Remove created object group lifeCycles during the given operation
     *
     * @param operationId the operation id
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientServerException if the server got an internal error
     */
    void rollBackObjectGroupsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Gets the unit lifeCycle status (COMMITTED or IN_PROCESS)
     *
     * @param unitId the unit id
     * @return the unit lifeCycle status (COMMITTED or IN_PROCESS)
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the server got an internal error
     */
    LifeCycleStatusCode getUnitLifeCycleStatus(String unitId)
        throws LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Gets the object group lifeCycle status (COMMITTED or IN_PROCESS)
     *
     * @param objectGroupId the object group id
     * @return the object group lifeCycle status (COMMITTED or IN_PROCESS)
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the server got an internal error
     */
    LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId)
        throws LogbookClientNotFoundException, LogbookClientServerException;

    void bulkObjectGroup(String eventIdProc, List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException;

    void bulkUnit(String eventIdProc, List<LogbookLifeCycleUnitModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * returns the raw unit life cycle
     *
     * @param id the id to retrieve
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode getRawUnitLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the raw unit life cycles
     *
     * @param ids the id to retrieve
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    List<JsonNode> getRawUnitLifeCycleByIds(List<String> ids) throws LogbookClientException;

    /**
     * returns the raw object group life cycle
     *
     * @param id the id to retrieve
     * @return the object group life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode getRawObjectGroupLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the raw object group life cycle
     *
     * @param ids the id to retrieve
     * @return the object group life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    List<JsonNode> getRawObjectGroupLifeCycleByIds(List<String> ids) throws LogbookClientException;

    /**
     * Create lifecycle objectgroup
     *
     * @param logbookLifeCycleRaws list of lifecycle objectgroup as jsons
     * @throws LogbookClientBadRequestException LogbookClientBadRequestException
     * @throws LogbookClientServerException LogbookClientServerException
     */
    void createRawbulkObjectgrouplifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Create lifecycle unit
     *
     * @param logbookLifeCycleRaws list of lifecycle unit as jsons
     * @throws LogbookClientBadRequestException LogbookClientBadRequestException
     * @throws LogbookClientServerException LogbookClientServerException
     */
    void createRawbulkUnitlifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * deleteLifecycleUnitsBulk
     *
     * @param listIds unit lfc ids
     * @throws LogbookClientBadRequestException LogbookClientBadRequestException
     * @throws LogbookClientServerException LogbookClientServerException
     */
    void deleteLifecycleUnitsBulk(Collection<String> listIds)
        throws LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * deleteLifecycleObjectGroupBulk
     *
     * @param listIds object group lfc ids
     * @throws LogbookClientBadRequestException LogbookClientBadRequestException
     * @throws LogbookClientServerException LogbookClientServerException
     */
    void deleteLifecycleObjectGroupBulk(Collection<String> listIds)
        throws LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * bulkLifeCycleTemporary
     *
     * @param operationId operationId
     * @param type type
     * @param logbookLifeCycleParametersBulk logbookLifeCycleParametersBulk
     * @throws VitamClientInternalException VitamClientInternalException
     */
    void bulkLifeCycleTemporary(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) throws VitamClientInternalException;

    /**
     * bulkLifeCycle
     *
     * @param operationId operationId
     * @param type type
     * @param logbookLifeCycleParametersBulk logbookLifeCycleParametersBulk
     * @throws VitamClientInternalException VitamClientInternalException
     */
    void bulkLifeCycle(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) throws VitamClientInternalException;
}
