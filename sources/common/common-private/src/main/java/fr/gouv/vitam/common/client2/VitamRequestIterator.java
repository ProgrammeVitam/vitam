/**
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
package fr.gouv.vitam.common.client2;

import java.util.Iterator;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;

/**
 * Utility to help with Http based Cursor that implements real Database Cursor on server side
 */
public class VitamRequestIterator implements AutoCloseable, Iterator<String> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIterator.class);

    private final MockOrRestClient client;
    private final JsonNode request;
    private final String method;
    private final String path;
    private final MultivaluedHashMap<String, Object> headers;
    private boolean first = true;
    private boolean closed = false;
    private RequestResponseOK objectResponse = null;
    private Iterator<String> iterator = null;

    /**
     * Constructor
     * 
     * @param client the client to use
     * @param method the method to use
     * @param path the path to use
     * @param headers the headers to use
     * @param request the request to use
     */
    // FIXME Add later on capability to handle maxNbPart in order to control the rate
    public VitamRequestIterator(MockOrRestClient client, String method, String path,
        MultivaluedHashMap<String, Object> headers,
        JsonNode request) {
        this.client = client;
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.request = request;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // Callback to close the cursor
        closed = true;
        Response response = null;
        try {
            headers.add(GlobalDataRest.X_CURSOR, false);
            response = client.performRequest(method, path, headers, MediaType.APPLICATION_JSON_TYPE);
        } catch (VitamClientInternalException e) {
            throw new BadRequestException(e);
        } finally {
            client.consumeAnyEntityAndClose(response);
        }
    }

    /**
     * @return true if there is a next element
     * @throws BadRequestException (RuntimeException) if the request is in error
     */
    @Override
    public boolean hasNext() {
        // next not called after a previous hasNext
        if (objectResponse != null) {
            return true;
        }
        if (closed) {
            return false;
        }
        // First call must initialize the cursor-id
        if (first) {
            first = false;
            Response response = null;
            try {
                headers.add(GlobalDataRest.X_CURSOR, true);
                response = client.performRequest(method, path, headers, request, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
                switch (Response.Status.fromStatusCode(response.getStatus())) {
                    case NOT_FOUND:
                        closed = true;
                        return false;
                    case PARTIAL_CONTENT:
                        // Multiple with Cursor
                        String xCurrsorId = (String) response.getHeaders().getFirst(GlobalDataRest.X_REQUEST_ID);
                        if (xCurrsorId == null) {
                            throw new BadRequestException("No Cursor returned");
                        }
                        headers.add(GlobalDataRest.X_CURSOR_ID, xCurrsorId);
                        // TODO Ignore for the moment X-Cursor-Timeout
                    case OK:
                        // Unique no Cursor
                        objectResponse = response.readEntity(RequestResponseOK.class);
                        iterator = objectResponse.getResults().iterator();
                        closed = true;
                        return true;
                    default:
                        closed = true;
                        LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                        throw new BadRequestException(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                }
            } catch (VitamClientInternalException e) {
                throw new BadRequestException(e);
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        } else {
            Response response = null;
            try {
                response = client.performRequest(method, path, headers, MediaType.APPLICATION_JSON_TYPE);
                switch (Response.Status.fromStatusCode(response.getStatus())) {
                    case NOT_FOUND:
                        closed = true;
                        return false;
                    case PARTIAL_CONTENT:
                        // TODO Ignore for the moment X-Cursor-Timeout
                        objectResponse = response.readEntity(RequestResponseOK.class);
                        iterator = objectResponse.getResults().iterator();
                        return true;
                    default:
                        closed = true;
                        LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                        throw new BadRequestException(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                }
            } catch (VitamClientInternalException e) {
                throw new BadRequestException(e);
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public String next() {
        String result = null;
        if (objectResponse != null) {
            result = iterator.next();
            if (!iterator.hasNext()) {
                objectResponse = null;
                iterator = null;
            }
        }
        return result;
    }

}
