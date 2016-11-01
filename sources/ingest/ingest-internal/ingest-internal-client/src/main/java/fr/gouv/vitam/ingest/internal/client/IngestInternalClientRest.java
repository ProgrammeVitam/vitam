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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;


/**
 * Rest client implementation for Ingest Internal
 */
public class IngestInternalClientRest extends DefaultClient implements IngestInternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalClientRest.class);
    private static final String UPLOAD_URL = "/upload";
    private static final String LOGBOOK_URL = "/logbooks";
    private static final String INGEST_URL = "/ingests";

    IngestInternalClientRest(IngestInternalClientFactory factory) {
        super(factory);
    }

    @Override
    public Response upload(GUID guid, Iterable<LogbookOperationParameters> logbookParametersList,
        InputStream inputStream,
        String archiveMimeType) throws VitamException {
        ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList);
        final FormDataMultiPart multiPart = new FormDataMultiPart();
        multiPart.field("part", logbookParametersList, MediaType.APPLICATION_JSON_TYPE);
        if (inputStream != null) {
            multiPart.bodyPart(
                new StreamDataBodyPart("part", inputStream, "SIP", CommonMediaType.valueOf(archiveMimeType)));
        }

        Response response =
            performRequest(HttpMethod.POST, UPLOAD_URL, getDefaultHeaders(guid.getId()),
                multiPart, MediaType.MULTIPART_FORM_DATA_TYPE, MediaType.APPLICATION_JSON_TYPE);

        if (Status.OK.getStatusCode() == response.getStatus()) {
            LOGGER.info("SIP : " + Response.Status.OK.getReasonPhrase());
        } else {
            LOGGER.error("SIP Upload Error");
            throw new VitamException("SIP Upload");
        }

        return response;
    }

    // FIXME P0 replace the above command by this one
    /**
     * Same as upload but using async service and 2 queries (delegated logbook and upload)
     * 
     * @param guid
     * @param logbookParametersList
     * @param inputStream
     * @param archiveMimeType
     * @return Response containing an InputStream for the ArchiveTransferReply (OK or KO) except in INTERNAL_ERROR (no
     *         body)
     * @throws VitamException
     */
    public Response uploadAsync(GUID guid, Iterable<LogbookOperationParameters> logbookParametersList,
        InputStream inputStream,
        String archiveMimeType) throws VitamException {
        ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList);
        MultivaluedHashMap<String, Object> headers = getDefaultHeaders(guid.getId());
        Response response = performRequest(HttpMethod.POST, LOGBOOK_URL, headers,
            logbookParametersList, MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE, false);
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            throw new VitamClientException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
        response = performRequest(HttpMethod.POST, INGEST_URL, headers,
            inputStream, CommonMediaType.valueOf(archiveMimeType), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        if (Status.OK.getStatusCode() == response.getStatus()) {
            LOGGER.info("SIP : " + Response.Status.OK.getReasonPhrase());
        } else {
            LOGGER.error("SIP Upload Error");
            throw new VitamException("SIP Upload");
        }
        return response;
    }

    // FIXME P0 to be added in Interface and Mock
    /**
     * Finalize the ingest operation by sending back the final Logbook Operation entries from Ingest external
     * 
     * @param guid
     * @param logbookParametersList
     * @throws VitamClientException
     */
    public void uploadFinalLogbook(GUID guid, Iterable<LogbookOperationParameters> logbookParametersList)
        throws VitamClientException {
        ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList);
        MultivaluedHashMap<String, Object> headers = getDefaultHeaders(guid.getId());
        Response response = performRequest(HttpMethod.PUT, LOGBOOK_URL, headers,
            logbookParametersList, MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_JSON_TYPE, false);
        if (response.getStatus() != Status.OK.getStatusCode()) {
            throw new VitamClientException(Status.fromStatusCode(response.getStatus()).getReasonPhrase());
        }
    }

    /**
     * Generate the default header map
     *
     * @param requestId the x-request-id == operation guid
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String requestId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
        return headers;
    }

}
