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
package fr.gouv.vitam.logbook.lifecycles.client;


import java.util.Iterator;

import javax.ws.rs.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.client2.ClientMockResultHelper;
import fr.gouv.vitam.common.client2.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;

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

    @Override
    public void create(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(CREATE, parameters);
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(UPDATE, parameters);
    }

    @Override
    public void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(COMMIT, parameters);
    }

    @Override
    public void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
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
    public JsonNode selectUnitLifeCycleById(String id) throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id) throws InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public VitamRequestIterator objectGroupLifeCyclesByOperationIterator(String operationId)
        throws InvalidParseOperationException {
        return new VitamRequestIterator(this, HttpMethod.GET,
            "/", null, ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public VitamRequestIterator unitLifeCyclesByOperationIterator(String operationId)
        throws InvalidParseOperationException {
        return new VitamRequestIterator(this, HttpMethod.GET,
            "/", null, ClientMockResultHelper.getLogbookOperation());
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
            throw new LogbookClientBadRequestException(
                ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    private void bulkUpdate(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException {
        if (queue != null) {
            final Iterator<LogbookLifeCycleParameters> iterator = queue.iterator();
            while (iterator.hasNext()) {
                logInformation(UPDATE, iterator.next());
            }
        } else {
            LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            throw new LogbookClientBadRequestException(
                ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    @Override
    public void bulkCreateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void bulkUpdateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue);
    }

    @Override
    public void bulkCreateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void bulkUpdateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue);
    }

}
