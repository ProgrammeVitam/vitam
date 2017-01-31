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
package fr.gouv.vitam.logbook.lifecycles.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroupInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnitInProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;

/**
 * Logbook LifeCycles implementation base class
 */
public class LogbookLifeCyclesImpl implements LogbookLifeCycles {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesImpl.class);

    /**
     * This is valid as Static final since this has to be shared among all requests and Concurrent for Thread safety
     */
    private static final Map<String, MongoCursor<?>> mapXCursor = new ConcurrentHashMap<>();
    private static final Map<String, LogbookCollections> mapXCursorByCollection = new ConcurrentHashMap<>();
    private final LogbookDbAccess mongoDbAccess;

    /**
     * Constructor
     *
     * @param mongoDbAccess
     */
    public LogbookLifeCyclesImpl(LogbookDbAccess mongoDbAccess) {
        this.mongoDbAccess = mongoDbAccess;
    }


    @Override
    public void createUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException, IllegalArgumentException {
        checkLifeCyclesUnitArgument(idOperation, idLc, parameters);
        mongoDbAccess.createLogbookLifeCycleUnit(idOperation, parameters);
    }

    @Override
    public void createObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException, IllegalArgumentException {
        checkLifeCyclesObjectGroupArgument(idOperation, idLc, parameters);
        mongoDbAccess.createLogbookLifeCycleObjectGroup(idOperation, parameters);
    }

    @Override
    public void updateUnit(String idOperation, String idLc, LogbookLifeCycleUnitParameters parameters)
        throws LogbookDatabaseException, IllegalArgumentException, LogbookNotFoundException,
        LogbookAlreadyExistsException {
        checkLifeCyclesUnitArgument(idOperation, idLc, parameters);
        mongoDbAccess.updateLogbookLifeCycleUnit(idOperation, parameters);
    }


    @Override
    public void updateObjectGroup(String idOperation, String idLc, LogbookLifeCycleObjectGroupParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException,
        LogbookAlreadyExistsException {
        checkLifeCyclesObjectGroupArgument(idOperation, idLc, parameters);
        mongoDbAccess.updateLogbookLifeCycleObjectGroup(idOperation, parameters);
    }

    @Override
    public LogbookLifeCycleUnit getUnitByOperationIdAndByUnitId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        return mongoDbAccess.getLogbookLifeCycleUnit(idOperation, idLc);
    }

    @Override
    public LogbookLifeCycleObjectGroup getObjectGroupByOperationIdAndByObjectGroupId(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException,
        IllegalArgumentException {
        return mongoDbAccess.getLogbookLifeCycleObjectGroup(idOperation, idLc);
    }

    @Override
    public List<LogbookLifeCycleUnit> selectUnit(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        try (final MongoCursor<LogbookLifeCycleUnit> logbook = mongoDbAccess.getLogbookLifeCycleUnits(select)) {
            final List<LogbookLifeCycleUnit> result = new ArrayList<>();
            if (!logbook.hasNext()) {
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbook.hasNext()) {
                result.add(logbook.next());
            }
            return result;
        }
    }

    @Override
    public String createCursorUnit(String operationId, JsonNode select, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookDatabaseException {
        String newxcursorid;
        // First time call
        newxcursorid = GUIDFactory.newGUID().toString();
        MongoCursor<LogbookLifeCycleUnit> cursor;
        LogbookCollections collection;
        try {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(select);

            if (LifeCycleStatusCode.NOT_COMMITTED.equals(lifeCycleStatusCode)) {
                collection = LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS;
            } else {
                collection = LogbookCollections.LIFECYCLE_UNIT;
            }
            parser.addCondition(QueryHelper.or()
                .add(QueryHelper.eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), operationId))
                .add(QueryHelper.eq(
                    LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                    operationId)));
            final Select selectRequest = parser.getRequest();
            cursor = mongoDbAccess.getLogbookLifeCycleUnitsFull(collection, selectRequest);
            mapXCursor.put(newxcursorid, cursor);
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new LogbookDatabaseException(e);
        }
        return newxcursorid;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public LogbookLifeCycle getCursorUnitNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException {
        try {
            @SuppressWarnings("unchecked")
            final MongoCursor<LogbookLifeCycle> cursor =
                (MongoCursor<LogbookLifeCycle>) mapXCursor.get(cursorId);
            if (cursor != null) {
                if (cursor.hasNext()) {
                    return cursor.next();
                }
                cursor.close();
                mapXCursor.remove(cursorId);
                throw new LogbookNotFoundException("No more entries");
            }
        } catch (final ClassCastException e) {
            throw new LogbookDatabaseException("Cursor not linked to Unit", e);
        }
        throw new LogbookDatabaseException("Cursor already closed");
    }

    @Override
    public String createCursorObjectGroup(String operationId, JsonNode select, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookDatabaseException {
        String newxcursorid;
        // First time call
        newxcursorid = GUIDFactory.newGUID().toString();
        MongoCursor<?> cursor;
        LogbookCollections collection;
        try {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(select);

            if (LifeCycleStatusCode.NOT_COMMITTED.equals(lifeCycleStatus)) {
                collection = LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS;
            } else {
                collection = LogbookCollections.LIFECYCLE_OBJECTGROUP;
            }

            parser.addCondition(QueryHelper.or()
                .add(QueryHelper.eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), operationId))
                .add(QueryHelper.eq(
                    LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                    operationId)));

            final Select selectRequest = parser.getRequest();
            cursor = mongoDbAccess.getLogbookLifeCycleObjectGroupsFull(collection, selectRequest);
            mapXCursor.put(newxcursorid, cursor);
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new LogbookDatabaseException(e);
        }
        return newxcursorid;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public LogbookLifeCycle getCursorObjectGroupNext(String cursorId)
        throws LogbookNotFoundException, LogbookDatabaseException {
        try {
            final MongoCursor<?> cursor =
                (MongoCursor<?>) mapXCursor.get(cursorId);

            if (cursor != null) {
                if (cursor.hasNext()) {
                    return (LogbookLifeCycle) cursor.next();
                }
                cursor.close();
                mapXCursor.remove(cursorId);
                mapXCursorByCollection.remove(cursorId);
                throw new LogbookNotFoundException("No more entries");
            }
        } catch (final ClassCastException e) {
            throw new LogbookDatabaseException("Cursor not linked to ObjectGroup", e);
        }
        throw new LogbookDatabaseException("Cursor already closed");
    }

    @Override
    public void finalizeCursor(String cursorId) {
        final MongoCursor<?> cursor = mapXCursor.get(cursorId);
        if (cursor != null) {
            cursor.close();
            mapXCursor.remove(cursorId);
            mapXCursorByCollection.remove(cursorId);
            return;
        }
    }

    @Override
    public List<LogbookLifeCycleObjectGroup> selectObjectGroup(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        try (final MongoCursor<LogbookLifeCycleObjectGroup> logbook =
            mongoDbAccess.getLogbookLifeCycleObjectGroups(select)) {
            final List<LogbookLifeCycleObjectGroup> result = new ArrayList<>();
            if (!logbook.hasNext()) {
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbook.hasNext()) {
                result.add(logbook.next());
            }
            return result;
        }
    }

    @Override
    public void rollbackUnit(String idOperation, String idLc)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException {
        mongoDbAccess.rollbackLogbookLifeCycleUnit(idOperation, idLc);
    }

    @Override
    public void rollbackObjectGroup(String idOperation, String idLc)
        throws LogbookNotFoundException, LogbookDatabaseException, IllegalArgumentException {
        mongoDbAccess.rollbackLogbookLifeCycleObjectGroup(idOperation, idLc);
    }

    @Override
    public LogbookLifeCycleUnit getUnitById(String idUnit) throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookLifeCycleUnit(idUnit);
    }

    @Override
    public LogbookLifeCycleObjectGroup getObjectGroupById(String idObjectGroup)
        throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookLifeCycleObjectGroup(idObjectGroup);
    }

    private void checkLifeCyclesUnitArgument(String idOperation, String idLcUnit,
        LogbookLifeCycleUnitParameters parameters) throws IllegalArgumentException {
        ParametersChecker.checkParameter("idOperation or idLifeCycle should not be null or empty", idOperation,
            idLcUnit);

        if (!parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }

        if (!parameters.getParameterValue(LogbookParameterName.objectIdentifier).equals(idLcUnit)) {
            LOGGER.error("incoherence entry for idLifeCycles");
            throw new IllegalArgumentException("incoherence entry for idLifeCycles");
        }
    }


    private void checkLifeCyclesObjectGroupArgument(String idOperation, String idLcObjectGroup,
        LogbookLifeCycleObjectGroupParameters parameters) throws IllegalArgumentException {
        ParametersChecker.checkParameter("idOperation or idLifeCycleObjectGroup should not be null or empty",
            idOperation, idLcObjectGroup);

        if (!parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOperation)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }

        if (!parameters.getParameterValue(LogbookParameterName.objectIdentifier).equals(idLcObjectGroup)) {
            LOGGER.error("incoherence entry for idLifeCyclesObjectGroup");
            throw new IllegalArgumentException("incoherence entry for idLifeCyclesObjectGroup");
        }
    }

    @Override
    public void createBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("idOperation should not be null or empty", idOp);
        if (lifecycleArray == null || lifecycleArray.length == 0) {
            throw new IllegalArgumentException("No LifeCycle Logbook");
        }
        if (!lifecycleArray[0].getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOp)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }
        if (lifecycleArray instanceof LogbookLifeCycleUnitParameters[]) {
            mongoDbAccess.createBulkLogbookLifeCycleUnit((LogbookLifeCycleUnitParameters[]) lifecycleArray);
        } else {
            mongoDbAccess
                .createBulkLogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters[]) lifecycleArray);
        }
    }

    @Override
    public void updateBulkLogbookLifecycle(String idOp, LogbookLifeCycleParameters[] lifecycleArray)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        ParametersChecker.checkParameter("idOperation should not be null or empty", idOp);
        if (lifecycleArray == null || lifecycleArray.length == 0) {
            throw new IllegalArgumentException("No LifeCycle Logbook");
        }
        if (!lifecycleArray[0].getParameterValue(LogbookParameterName.eventIdentifierProcess).equals(idOp)) {
            LOGGER.error("incoherence entry for idOperation");
            throw new IllegalArgumentException("incoherence entry for idOperation");
        }
        if (lifecycleArray instanceof LogbookLifeCycleUnitParameters[]) {
            mongoDbAccess.updateBulkLogbookLifeCycleUnit((LogbookLifeCycleUnitParameters[]) lifecycleArray);
        } else {
            mongoDbAccess
                .updateBulkLogbookLifeCycleObjectGroup((LogbookLifeCycleObjectGroupParameters[]) lifecycleArray);
        }
    }


    @Override
    public void commitUnit(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {

        // 1- Find temporary unit lifeCycle
        LogbookLifeCycleUnitInProcess logbookLifeCycleUnitInProcess = mongoDbAccess.getLogbookLifeCycleUnitInProcess(idLc);
        if (logbookLifeCycleUnitInProcess == null) {
            LOGGER.error("The temporary lifeCycle wasn't found");
            throw new LogbookNotFoundException("The temporary lifeCycle wasn't found");
        }

        // 2- Check if it is a creation or an update commit
        boolean isLifeCycleExist = mongoDbAccess.existsLogbookLifeCycleUnit(idLc);

        // 3- Copy to Production Collection
        if (!isLifeCycleExist) {
            // Creation Mode from LogbookLifeCycleUnitInProcess instance
            mongoDbAccess.createLogbookLifeCycleUnit(logbookLifeCycleUnitInProcess);
        } else {
            // Update existing lifeCycle
            mongoDbAccess.updateLogbookLifeCycleUnit(logbookLifeCycleUnitInProcess);
        }
    }


    @Override
    public void commitObjectGroup(String idOperation, String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException, LogbookAlreadyExistsException {
        // 1- Find temporary unit lifeCycle
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroupInProcess =
            mongoDbAccess.getLogbookLifeCycleObjectGroupInProcess(idLc);

        if (logbookLifeCycleObjectGroupInProcess == null) {
            // TODO correct exception and logger messages
            LOGGER.error("The temporary lifeCycle is not found");
            throw new LogbookNotFoundException("The temporary lifeCycle is not found");
        }

        // 2- Check if it is a creation or an update commit
        boolean isLifeCycleExist = mongoDbAccess.existsLogbookLifeCycleObjectGroup(idLc);

        // 3- Copy to Production Collection
        if (!isLifeCycleExist) {
            // Creation Mode from LogbookLifeCycleObjectGroupInProcess instance
            mongoDbAccess.createLogbookLifeCycleObjectGroup(logbookLifeCycleObjectGroupInProcess);
        } else {
            // Update existing lifeCycle
            mongoDbAccess.updateLogbookLifeCycleObjectGroup(logbookLifeCycleObjectGroupInProcess);
        }
    }


    @Override
    public void rollBackUnitsByOperation(String idOperation) throws LogbookNotFoundException, LogbookDatabaseException {
        mongoDbAccess.rollBackUnitLifeCyclesByOperation(idOperation);
    }

    @Override
    public void rollBackObjectGroupsByOperation(String idOperation)
        throws LogbookNotFoundException, LogbookDatabaseException {
        mongoDbAccess.rollBackObjectGroupLifeCyclesByOperation(idOperation);
    }
}


