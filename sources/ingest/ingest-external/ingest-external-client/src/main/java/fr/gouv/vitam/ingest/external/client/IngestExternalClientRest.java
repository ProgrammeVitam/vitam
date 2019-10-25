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
package fr.gouv.vitam.ingest.external.client;

import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

/**
 * Ingest External client
 */
class IngestExternalClientRest extends DefaultClient implements IngestExternalClient {
    private static final String INGEST_EXTERNAL_MODULE = "IngestExternalModule";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";

    IngestExternalClientRest(IngestExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<Void> ingest(VitamContext vitamContext, InputStream stream,
        String contextId,
        String action)
        throws IngestExternalException {

        ParametersChecker.checkParameter("Stream is a mandatory parameter", stream);
        ParametersChecker.checkParameter("Tenant identifier is a mandatory parameter", vitamContext.getTenantId());
        Response response = null;
        final MultivaluedMap<String, Object> headers = vitamContext.getHeaders();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_ACTION, action);
        headers.add(EXPECT, EXPECT_CONTINUE);

        try {
            response = performRequest(HttpMethod.POST, INGEST_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_XML_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case ACCEPTED:
                    LOGGER.debug(Status.ACCEPTED.getReasonPhrase());
                    return new RequestResponseOK<Void>().parseHeadersFromResponse(response)
                        .setHttpCode(response.getStatus());
                case BAD_REQUEST:
                case PARTIAL_CONTENT:
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamError = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage())
                        .setState(StatusCode.KO.name())
                        .setContext(INGEST_EXTERNAL_MODULE);

                    return vitamError.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                case SERVICE_UNAVAILABLE:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamErrorFatal = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(response.readEntity(String.class))
                        .setState(StatusCode.FATAL.name())
                        .setContext("IngestExternalModule");

                    return vitamErrorFatal.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                default:
                    throw new IngestExternalException("Unknown error: " + status.getReasonPhrase());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Ingest External Internal Server Error", e);
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    public Response downloadObjectAsync(VitamContext vitamContext, String objectId,
        IngestCollection type)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);
        ParametersChecker.checkParameter(BLANK_TYPE, type);

        Response response;
        try {
            response = performRequest(HttpMethod.GET, INGEST_URL + "/" + objectId + "/" + type.getCollectionName(),
                vitamContext.getHeaders(), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
        return response;
    }

    @Override
    public RequestResponse<Void> ingestLocal(VitamContext vitamContext, LocalFile localFile, String contextId,
        String action)
        throws IngestExternalException {

        ParametersChecker.checkParameter("localFile is a mandatory parameter", localFile);
        ParametersChecker.checkParameter("Tenant identifier is a mandatory parameter", vitamContext.getTenantId());
        Response response = null;
        final MultivaluedMap<String, Object> headers = vitamContext.getHeaders();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_ACTION, action);
        headers.add(EXPECT, EXPECT_CONTINUE);

        try {
            response = performRequest(HttpMethod.POST, INGEST_URL, headers,
                localFile, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case ACCEPTED:
                    LOGGER.debug(Status.ACCEPTED.getReasonPhrase());
                    return new RequestResponseOK<Void>().parseHeadersFromResponse(response)
                        .setHttpCode(response.getStatus());
                case BAD_REQUEST:
                case PARTIAL_CONTENT:
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamError = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage())
                        .setState(StatusCode.KO.name())
                        .setContext(INGEST_EXTERNAL_MODULE);

                    return vitamError.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                case SERVICE_UNAVAILABLE:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamErrorFatal = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(response.readEntity(String.class))
                        .setState(StatusCode.FATAL.name())
                        .setContext("IngestExternalModule");

                    return vitamErrorFatal.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                default:
                    throw new IngestExternalException("Unknown error: " + status.getReasonPhrase());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Ingest External Internal Server Error", e);
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


}
