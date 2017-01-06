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

package fr.gouv.vitam.ingest.internal.client;

import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;


/**
 * Rest client implementation for Ingest Internal
 */
class IngestInternalClientRest extends DefaultClient implements IngestInternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalClientRest.class);
    private static final String LOGBOOK_URL = "/logbooks";
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";
    
    private static final String REPORT = "/report";

    IngestInternalClientRest(IngestInternalClientFactory factory) {
        super(factory);
    }

    @Override
    public Response uploadInitialLogbook(Iterable<LogbookOperationParameters> logbookParametersList)
        throws VitamException {
        ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList);

        final Response response = performRequest(HttpMethod.POST, LOGBOOK_URL, null,
            logbookParametersList, MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE, false);
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            throw new VitamClientException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
        return response;
    }

    @Override
    public Response upload(InputStream inputStream, MediaType archiveMimeType) throws VitamException {
        ParametersChecker.checkParameter("Params cannot be null", inputStream, archiveMimeType);
        final Response response = performRequest(HttpMethod.POST, INGEST_URL, null,
            inputStream, archiveMimeType, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        if (Status.OK.getStatusCode() == response.getStatus()) {
            LOGGER.info("SIP : " + Response.Status.OK.getReasonPhrase());
        } else if (Status.ACCEPTED.getStatusCode() == response.getStatus()) {
            LOGGER.warn("SIP Warning : " + Response.Status.ACCEPTED.getReasonPhrase());
        } else {
            LOGGER.error("SIP Upload Error: " + Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
        return response;
    }

    @Override
    public void uploadFinalLogbook(Iterable<LogbookOperationParameters> logbookParametersList)
        throws VitamClientException {
        ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList);
        final Response response = performRequest(HttpMethod.PUT, LOGBOOK_URL, null,
            logbookParametersList, MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE, false);
        if (response.getStatus() != Status.OK.getStatusCode()) {
            throw new VitamClientException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type) throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);
        ParametersChecker.checkParameter(BLANK_TYPE, type);

        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, INGEST_URL + "/" + objectId + "/" + type.getCollectionName(), 
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return response;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public Response storeATR(GUID guid, InputStream input) throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, INGEST_URL + "/" + guid + REPORT,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return response;
            
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }
    
    
}
