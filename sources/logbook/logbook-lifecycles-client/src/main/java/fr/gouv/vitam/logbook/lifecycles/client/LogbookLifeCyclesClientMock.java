/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.logbook.lifecycles.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.client.StatusMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.response.DatabaseCursor;
import fr.gouv.vitam.logbook.common.model.response.RequestResponseOK;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.common.parameter.ParameterHelper;

/**
 * LogbookLifeCyclesClient Mock implementation
 */
class LogbookLifeCyclesClientMock implements LogbookLifeCycleClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesClientMock.class);
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();

    private static final String UPDATE = "UPDATE";
    private static final String CREATE = "CREATE";
    private static final String COMMIT = "COMMIT";
    private static final String ROLLBACK = "ROLLBACK";

    private static final String MOCK_SELECT_RESULT_1 = "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "    \"evType\": \"Process_SIP_unitary\"," +
        "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evTypeProc\": \"INGEST\"," +
        "    \"outcome\": \"STARTED\"," +
        "    \"outDetail\": null," +
        "    \"outMessg\": \"SIP entry : SIP.zip\"," +
        "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
        "    \"agIdApp\": null," +
        "    \"agIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

    private static final String MOCK_SELECT_RESULT_2 = "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaz\"," +
        "    \"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "    \"evType\": \"Process_SIP_unitary\"," +
        "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evTypeProc\": \"INGEST\"," +
        "    \"outcome\": \"STARTED\"," +
        "    \"outDetail\": null," +
        "    \"outMessg\": \"SIP entry : SIP.zip\"," +
        "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
        "    \"agIdApp\": null," +
        "    \"agIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";


    @Override
    public void create(LogbookParameters parameters)
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
    public void update(LogbookParameters parameters)
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
    public void commit(LogbookParameters parameters)
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
    public void rollback(LogbookParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        logInformation(ROLLBACK, parameters);
    }

    @Override
    public StatusMessage status() throws LogbookClientServerException {
        return new StatusMessage(SERVER_IDENTITY);
    }

    @Override
    public void close() {
        // Empty
    }

    /**
     *
     * @return the default first answer
     */
    public static String getMockSelectOperationResult() {
        return MOCK_SELECT_RESULT_1;
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
    public JsonNode selectLifeCycles(String select) throws LogbookClientException, InvalidParseOperationException {
        LOGGER.info("Select request:" + select);
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(2, 0, 10));
        response.setQuery(JsonHandler.getFromString(select));
        response.setResult(JsonHandler.getFromString('[' + MOCK_SELECT_RESULT_1 + ',' + MOCK_SELECT_RESULT_2 + ']'));
        return new ObjectMapper().convertValue(response, JsonNode.class);
    }

    @Override
    public JsonNode selectLifeCyclesById(String id) throws LogbookClientException, InvalidParseOperationException {
        LOGGER.info("Select request with id:" + id);
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 10));
        response.setResult(JsonHandler.getFromString(MOCK_SELECT_RESULT_1));
        return new ObjectMapper().convertValue(response, JsonNode.class);
    }
}
