/*
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
package fr.gouv.vitam.logbook.operations.core;

import java.text.MessageFormat;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

/**
 * LogbookOperationsDecorator implementation.
 * This implementation create a LogbookOperation and if necessary create an alert
 */
public class AlertLogbookOperationsDecorator extends LogbookOperationsDecorator {

    private static final String SECURITY_ALERT = "Evénement de securité déclenché pour l''eventType {0} et l''outcome {1} : {2} {3}";

    /**
     * the configured alertEvents
     */
    private List<LogbookEvent> alertEvents;

    private AlertService alertService;

    public AlertLogbookOperationsDecorator(LogbookOperations logbookOperations, List<LogbookEvent> alertEvents) {
        super(logbookOperations);
        this.alertEvents = alertEvents;
        alertService = new AlertServiceImpl();
    }


    @VisibleForTesting
    AlertLogbookOperationsDecorator(LogbookOperations logbookOperations, List<LogbookEvent> alertEvents, AlertService alertService) {
        super(logbookOperations);
        this.alertEvents = alertEvents;
        this.alertService = alertService;
    }

    @Override
    public void create(LogbookOperationParameters parameters)
            throws LogbookAlreadyExistsException, LogbookDatabaseException {
        logbookOperations.create(parameters);
        createAlertIfNecessary(parameters);
    }

    @Override
    public void update(LogbookOperationParameters parameters)
            throws LogbookNotFoundException, LogbookDatabaseException {
        logbookOperations.update(parameters);
        createAlertIfNecessary(parameters);
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperations(JsonNode select) throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        return logbookOperations.selectOperations(select);
    }

    @Override
    public void createBulkLogbookOperation(LogbookOperationParameters[] operationArray)
            throws LogbookDatabaseException, LogbookAlreadyExistsException {
        logbookOperations.createBulkLogbookOperation(operationArray);
        createAlertIfNecessary(operationArray);
    }

    @Override
    public void updateBulkLogbookOperation(LogbookOperationParameters[] operationArray)
            throws LogbookDatabaseException, LogbookNotFoundException {
        logbookOperations.updateBulkLogbookOperation(operationArray);
        createAlertIfNecessary(operationArray);
    }


    /**
     * Create an alert for the configured LogbookOperationParameters eventType and outcome if the specified eventType should raise an alert
     *
     * @param parameters
     */
    private void createAlertIfNecessary(LogbookOperationParameters parameters) {
        if (isAlertEvent(parameters)) {
            String message = MessageFormat.format(SECURITY_ALERT, parameters.getParameterValue(LogbookParameterName.eventType), parameters.getParameterValue(LogbookParameterName.outcome), parameters.getParameterValue(LogbookParameterName.outcomeDetail), parameters.getParameterValue(LogbookParameterName.outcomeDetailMessage));
            alertService.createAlert(VitamLogLevel.INFO, message);
        }
    }

    /**
     * Create an alert for all the specified LogbookOperationParameters
     *
     * @param operationArray
     */
    private void createAlertIfNecessary(LogbookOperationParameters[] operationArray) {
        for (LogbookOperationParameters parameters : operationArray) {
            createAlertIfNecessary(parameters);
        }

    }


    /**
     * Check if the LogbookOperationParameters should raise an alert
     *
     * @param parameters
     * @return
     */
    @VisibleForTesting
    boolean isAlertEvent(LogbookOperationParameters parameters) {

        for (LogbookEvent logbookEvent : alertEvents) {
            if (logbookEvent.getOutDetail() != null) {
                String outDetail = parameters.getParameterValue(LogbookParameterName.outcomeDetail);

                if (Strings.isNullOrEmpty(outDetail)) {
                    return false;
                }

                if (outDetail.equals(logbookEvent.getOutDetail())) {
                    return true;
                }
            } else {
                String eventType = parameters.getParameterValue(LogbookParameterName.eventType);
                if (Strings.isNullOrEmpty(eventType)) {
                    return false;
                }

                String outcome = parameters.getParameterValue(LogbookParameterName.outcome);
                if (Strings.isNullOrEmpty(outcome)) {
                    return false;
                }

                if (eventType.equals(logbookEvent.getEvType()) && outcome.equals(logbookEvent.getOutcome())) {
                    return true;
                }
            }
        }
        return false;
    }
}
