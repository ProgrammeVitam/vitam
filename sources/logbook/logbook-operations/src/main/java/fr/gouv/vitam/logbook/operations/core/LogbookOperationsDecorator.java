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
package fr.gouv.vitam.logbook.operations.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Decorator for LogbookOperations
 */
public abstract class LogbookOperationsDecorator implements LogbookOperations {

    protected LogbookOperations logbookOperations;

    /**
     * Constructor
     *
     * @param logbookOperations
     */
    public LogbookOperationsDecorator(LogbookOperations logbookOperations) {
        this.logbookOperations = logbookOperations;
    }

    @Override
    public void create(LogbookOperationParameters parameters)
        throws LogbookAlreadyExistsException, LogbookDatabaseException {
        logbookOperations.create(parameters);

    }

    @Override
    public void update(LogbookOperationParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException {
        logbookOperations.update(parameters);

    }

    @Override
    public List<LogbookOperation> select(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        return logbookOperations.select(select);
    }

    @Override
    public List<LogbookOperation> select(JsonNode select, boolean sliced)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        List<LogbookOperation> operations = new ArrayList<>();
            operations = logbookOperations.select(select, sliced);
        return operations;
    }

    @Override
    public LogbookOperation getById(String IdProcess) throws LogbookDatabaseException, LogbookNotFoundException {
        return logbookOperations.getById(IdProcess);
    }

    @Override
    public void createBulkLogbookOperation(LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        logbookOperations.createBulkLogbookOperation(operationArray);

    }

    @Override
    public void updateBulkLogbookOperation(LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookNotFoundException {
        logbookOperations.updateBulkLogbookOperation(operationArray);

    }

    @Override
    public MongoCursor<LogbookOperation> selectOperationsByLastPersistenceDateInterval(LocalDateTime startDate,
        LocalDateTime endDate)
        throws LogbookDatabaseException,
        LogbookNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
        return logbookOperations.selectOperationsByLastPersistenceDateInterval(startDate, endDate);
    }

    @Override
    public LogbookOperation findFirstTraceabilityOperationOKAfterDate(LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException {
        return logbookOperations.findFirstTraceabilityOperationOKAfterDate(date);
    }

    @Override
    public LogbookOperation findLastTraceabilityOperationOK() throws InvalidCreateOperationException,
        LogbookNotFoundException, LogbookDatabaseException, InvalidParseOperationException {
        return logbookOperations.findLastTraceabilityOperationOK();
    }

    @Override
    public ReindexationResult reindex(IndexParameters indexParameters) {
        return logbookOperations.reindex(indexParameters);
    }

    @Override
    public SwitchIndexResult switchIndex(String alias, String newIndexName) throws DatabaseException {
        return logbookOperations.switchIndex(alias, newIndexName);
    }
}
