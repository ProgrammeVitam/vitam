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
package fr.gouv.vitam.logbook.operations.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;

import javax.ws.rs.core.Response;

/**
 * Mock client implementation for logbook operation
 */
public class LogbookOperationsClientMock extends AbstractMockClient implements LogbookOperationsClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientMock.class);

    private static final String UPDATE = "UPDATE";
    private static final String CREATE = "CREATE";
    private static final String GUID_EXAMPLE = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq";
    private final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

    @Override
    public void create(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        LogbookOperationsClientHelper.checkLogbookParameters(parameters);
        logInformation(CREATE, parameters);
    }

    @Override
    public void update(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        LogbookOperationsClientHelper.checkLogbookParameters(parameters);
        logInformation(UPDATE, parameters);
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
    public JsonNode selectOperation(JsonNode select) throws LogbookClientException, InvalidParseOperationException {
        LOGGER.debug("Select request:" + select);
        return ClientMockResultHelper.getLogbookResults();
    }

    @Override
    public JsonNode selectOperationSliced(JsonNode select) throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookResults();
    }

    @Override
    public JsonNode selectOperationById(String id)
        throws LogbookClientException, InvalidParseOperationException {
        LOGGER.debug("Select request with id:" + id);
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public RequestResponseOK traceability() throws InvalidParseOperationException {
        LOGGER.debug("calling traceability ");
        final List<String> resultAsJson = new ArrayList<>();

        resultAsJson.add(GUID_EXAMPLE);

        return new RequestResponseOK().addAllResults(resultAsJson);
    }

    @Override
    public void createDelegate(LogbookOperationParameters parameters) throws LogbookClientAlreadyExistsException {
        helper.createDelegate(parameters);
    }

    @Override
    public void updateDelegate(LogbookOperationParameters parameters) throws LogbookClientNotFoundException {
        helper.updateDelegate(parameters);
    }

    @Override
    public void bulkCreate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientBadRequestException {
        if (queue != null) {
            final Iterator<LogbookOperationParameters> iterator = queue.iterator();
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

    @Override
    public void bulkUpdate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientBadRequestException {
        if (queue != null) {
            final Iterator<LogbookOperationParameters> iterator = queue.iterator();
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
    public void commitCreateDelegate(String eventIdProc) throws LogbookClientBadRequestException {
        final Queue<LogbookOperationParameters> queue = helper.removeCreateDelegate(eventIdProc);
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void commitUpdateDelegate(String eventIdProc) throws LogbookClientBadRequestException {
        final Queue<LogbookOperationParameters> queue = helper.removeUpdateDelegate(eventIdProc);
        bulkUpdate(eventIdProc, queue);
    }

    @Override
    public void close() {
        super.close();
        helper.clear();
    }

    @Override
    public RequestResponseOK traceabilityLfcUnit() {
        LOGGER.debug("calling traceability LFC unit");
        final List<String> resultAsJson = new ArrayList<>();
        resultAsJson.add(GUID_EXAMPLE);
        return new RequestResponseOK().addAllResults(resultAsJson);
    }

    @Override
    public RequestResponseOK traceabilityLfcObjectGroup() {
        LOGGER.debug("calling traceability LFC ObjectGroup");
        final List<String> resultAsJson = new ArrayList<>();
        resultAsJson.add(GUID_EXAMPLE);
        return new RequestResponseOK().addAllResults(resultAsJson);
    }

    @Override
    public JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, LogbookClientServerException {
        return ClientMockResultHelper.getReindexationInfo().toJsonNode();
    }

    @Override
    public JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, LogbookClientServerException {
        return ClientMockResultHelper.getSwitchIndexesInfo().toJsonNode();
    }

    @Override
    public void traceabilityAudit(int tenant, AuditLogbookOptions options) {
        LOGGER.info("audit traceability");
    }

    @Override public Response checkLogbookCoherence() throws VitamException {
        return Response.ok().build();
    }

    @Override
    public LifecycleTraceabilityStatus checkLifecycleTraceabilityWorkflowStatus(String operationId) {
        throw new IllegalStateException("Stop using mocks in production");
    }
}
