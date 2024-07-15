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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import org.apache.commons.lang3.NotImplementedException;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LogbookLifeCyclesClient Mock implementation
 */
class LogbookLifeCyclesClientMock extends AbstractMockClient implements LogbookLifeCyclesClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesClientMock.class);
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();

    private static final String UPDATE = "UPDATE";
    private static final String CREATE = "CREATE";
    private static final String COMMIT = "COMMIT";
    private static final String ROLLBACK = "ROLLBACK";
    private static final ConcurrentMap<String, List<String>> lifeCyclesByOperation = new ConcurrentHashMap<>();

    @Override
    public void create(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        logInformation(CREATE, parameters);
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );

        logInformation(UPDATE, parameters);
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );

        logInformation(UPDATE, parameters);
    }

    @Deprecated
    @Override
    public void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        logInformation(COMMIT, parameters);
    }

    @Override
    public void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        logInformation(ROLLBACK, parameters);
    }

    private void logInformation(String operation, LogbookParameters parameters) {
        String result;
        try {
            result = JsonHandler.writeAsString(parameters);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Cannot serialize parameters", e);
            result = "{}";
        }
        LOGGER.info(operation + ":" + result);
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl) throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectUnitLifeCycle(JsonNode queryDsl) throws InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookLifecycle();
    }

    @Override
    public JsonNode getRawUnitLifeCycleById(String id) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public List<JsonNode> getRawUnitLifeCycleByIds(List<String> ids) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public JsonNode getRawObjectGroupLifeCycleById(String id) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public List<JsonNode> getRawObjectGroupLifeCycleByIds(List<String> ids) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public InputStream exportRawUnitLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public InputStream exportRawObjectGroupLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl) throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public CloseableIterator<JsonNode> objectGroupLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) {
        throw new NotImplementedException("Unsupported mock");
    }

    @Override
    public CloseableIterator<JsonNode> unitLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) {
        throw new NotImplementedException("Unsupported mock");
    }

    private void bulkCreate(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        if (queue != null) {
            final Iterator<LogbookLifeCycleParameters> iterator = queue.iterator();
            if (iterator.hasNext()) {
                logInformation(CREATE, iterator.next());
                while (iterator.hasNext()) {
                    logInformation(UPDATE, iterator.next());
                }
            }
        } else {
            LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            throw new LogbookClientBadRequestException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    private void bulkUpdate(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        if (queue != null) {
            for (LogbookLifeCycleParameters logbookLifeCycleParameters : queue) {
                logInformation(UPDATE, logbookLifeCycleParameters);
            }
        } else {
            LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            throw new LogbookClientBadRequestException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    @Override
    public void bulkCreateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void bulkUpdateUnit(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        bulkUpdate(eventIdProc, queue);
    }

    @Override
    public void bulkCreateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void bulkUpdateObjectGroup(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        bulkUpdate(eventIdProc, queue);
    }

    @Override
    public void commitUnit(String operationId, String unitId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        commitObject(operationId, unitId);
    }

    @Override
    public void commitObjectGroup(String operationId, String objectGroupId) {
        commitObject(operationId, objectGroupId);
    }

    private void commitObject(String operationId, String unitId) {
        if (!lifeCyclesByOperation.containsKey(operationId)) {
            List<String> objectList = new ArrayList<>();
            objectList.add(unitId);
            lifeCyclesByOperation.put(operationId, objectList);
        } else if (!lifeCyclesByOperation.get(operationId).contains(unitId)) {
            lifeCyclesByOperation.get(operationId).add(unitId);
        }
    }

    @Override
    public void rollBackUnitsByOperation(String operationId) throws LogbookClientNotFoundException {
        rollBackObjectByOperation(operationId);
    }

    @Override
    public void rollBackObjectGroupsByOperation(String operationId) throws LogbookClientNotFoundException {
        rollBackObjectByOperation(operationId);
    }

    private void rollBackObjectByOperation(String operationId) throws LogbookClientNotFoundException {
        if (!lifeCyclesByOperation.containsKey(operationId)) {
            throw new LogbookClientNotFoundException("Operation not found");
        } else {
            lifeCyclesByOperation.remove(operationId);
        }
    }

    @Override
    public LifeCycleStatusCode getUnitLifeCycleStatus(String unitId) {
        return getObjectLifeCycleStatus(unitId);
    }

    @Override
    public LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId) {
        return getObjectLifeCycleStatus(objectGroupId);
    }

    private LifeCycleStatusCode getObjectLifeCycleStatus(String objectId) {
        for (List<String> objectList : lifeCyclesByOperation.values()) {
            if (objectList.contains(objectId)) {
                return LifeCycleStatusCode.LIFE_CYCLE_COMMITTED;
            }
        }
        return null;
    }

    @Override
    public JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl) throws InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookLifecycle();
    }

    @Override
    public JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookLifecycle();
    }

    @Override
    public void bulkObjectGroup(String eventIdProc, List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels) {
        // do nothing
    }

    @Override
    public void bulkUnit(String eventIdProc, List<LogbookLifeCycleUnitModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        // do nothing
    }

    @Override
    public void createRawbulkObjectgrouplifecycles(List<JsonNode> logbookLifeCycleRaws) {}

    @Override
    public void createRawbulkUnitlifecycles(List<JsonNode> logbookLifeCycleRaws) {}

    @Override
    public void deleteLifecycleUnitsBulk(Collection<String> listIds) {
        throw new IllegalArgumentException("Stop mocking in production ");
    }

    @Override
    public void deleteLifecycleObjectGroupBulk(Collection<String> listIds) {
        throw new IllegalArgumentException("Stop mocking in production ");
    }

    @Override
    public void bulkLifeCycleTemporary(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) {
        // do nothing
    }

    @Override
    public void bulkLifeCycle(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) {
        // do nothing
    }
}
