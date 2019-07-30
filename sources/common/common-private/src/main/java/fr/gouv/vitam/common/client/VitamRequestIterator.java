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
package fr.gouv.vitam.common.client;

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

/**
 * Utility to help with Http based Cursor that implements real Database Cursor on server side
 */
public class VitamRequestIterator<T> implements VitamAutoCloseable, Iterator<T> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIterator.class);

    private final MockOrRestClient client;
    private final JsonNode request;
    private final String method;
    private final String path;
    private final MultivaluedHashMap<String, Object> headers;
    private final Class<T> responseType;
    private String xCursorId = null;
    private boolean first = true;
    private boolean closed = false;
    private RequestResponseOK<T> objectResponse = null;
    private Iterator<T> iterator = null;

    /**
     * Constructor</br>
     * </br>
     * Note: if of type AbstractMockClient or derived, request will be the returned unique result.
     *
     * @param client       the client to use
     * @param method       the method to use
     * @param path         the path to use
     * @param responseType the type of the response to be returned
     * @param headers      the headers to use, could be null
     * @param request      the request to use, could be null
     * @throws IllegalArgumentException if one of mandatory arguments is null or empty
     */
    // TODO P1 Add later on capability to handle maxNbPart in order to control the rate
    public VitamRequestIterator(MockOrRestClient client, String method, String path,
        Class<T> responseType, MultivaluedHashMap<String, Object> headers,
        JsonNode request) {
        ParametersChecker.checkParameter("Arguments method and path could not be null", method, path);
        ParametersChecker.checkParameter("Argument client could not be null", client);
        this.client = client;
        this.method = method;
        this.path = path;
        if (headers != null) {
            this.headers = headers;
        } else {
            this.headers = new MultivaluedHashMap<>();
        }
        this.request = request;
        this.responseType = responseType;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // Callback to close the cursor
        closed = true;
        if (xCursorId == null) {
            return;
        }
        if (client instanceof AbstractMockClient) {
            return;
        }
        Response response = null;
        try {
            headers.putSingle(GlobalDataRest.X_CURSOR, false);
            headers.add(GlobalDataRest.X_CURSOR_ID, xCursorId);
            response =
                ((AbstractCommonClient) client).performRequest(method, path, headers, MediaType.APPLICATION_JSON_TYPE);
        } catch (final VitamClientInternalException e) {
            throw new BadRequestException(e);
        } finally {
            client.consumeAnyEntityAndClose(response);
        }
    }

    private boolean handleFirst(Response response) {
        // TODO P1 Ignore for the moment X-Cursor-Timeout
        xCursorId = (String) response.getHeaders().getFirst(GlobalDataRest.X_CURSOR_ID);
        if (xCursorId == null && !closed) {
            throw new BadRequestException("No Cursor returned");
        } else {
            headers.add(GlobalDataRest.X_CURSOR_ID, xCursorId);
        }

        try {
            objectResponse =
                JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class, responseType);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Invalid response, json parsing fail", e);
            return false;
        }

        iterator = objectResponse.getResults().iterator();
        if (!iterator.hasNext()) {
            objectResponse = null;
            iterator = null;
            return false;
        }
        return true;
    }

    private boolean handleNext(Response response) {
        // TODO P1 Ignore for the moment X-Cursor-Timeout
        try {
            objectResponse =
                JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class, responseType);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Invalid response, json parsing fail", e);
            return false;
        }
        iterator = objectResponse.getResults().iterator();
        if (!iterator.hasNext()) {
            objectResponse = null;
            iterator = null;
            return false;
        }
        return true;
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
        if (client instanceof AbstractMockClient) {
            return true;
        }

        // First call must initialize the cursor-id
        if (first) {
            first = false;
            Response response = null;
            try {
                headers.putSingle(GlobalDataRest.X_CURSOR, true);
                response = ((AbstractCommonClient) client).performRequest(method, path, headers, request,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
                LOGGER.info(response.toString());
                switch (Response.Status.fromStatusCode(response.getStatus())) {
                    case NOT_FOUND:
                        closed = true;
                        return false;
                    case OK:
                        // Unique no Cursor
                        closed = true;
                        return handleFirst(response);
                    case PARTIAL_CONTENT:
                        // Multiple with Cursor
                        return handleFirst(response);
                    default:
                        closed = true;
                        throw new BadRequestException(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                }
            } catch (final VitamClientInternalException e) {
                throw new BadRequestException(e);
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        } else {
            Response response = null;
            try {
                response = ((AbstractCommonClient) client)
                    .performRequest(method, path, headers, JsonHandler.createObjectNode(),
                        MediaType.APPLICATION_JSON_TYPE,
                        MediaType.APPLICATION_JSON_TYPE);
                LOGGER.info(response.toString());
                switch (Response.Status.fromStatusCode(response.getStatus())) {
                    case NOT_FOUND:
                        closed = true;
                        return false;
                    case OK:
                        // End of cursor
                        closed = true;
                        return handleNext(response);
                    case PARTIAL_CONTENT:
                        return handleNext(response);
                    default:
                        closed = true;
                        LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                        throw new BadRequestException(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                }
            } catch (final VitamClientInternalException e) {
                throw new BadRequestException(e);
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public T next() {
        if (client instanceof AbstractMockClient) {
            closed = true;

            if (request == null) {
                getClass().getTypeParameters().getClass();
                return (T) new Object();
            }
        }

        T result = null;
        if (objectResponse != null) {
            result = iterator.next();
            if (!iterator.hasNext()) {
                objectResponse = null;
                iterator = null;
            }
        }
        return result;
    }

    private static boolean checkHeadersConformity(HttpHeaders headers) {
        if (headers != null) {
            boolean xcursor;
            final MultivaluedMap<String, String> map = headers.getRequestHeaders();
            if (!map.containsKey(GlobalDataRest.X_CURSOR)) {
                throw new IllegalStateException(GlobalDataRest.X_CURSOR + " should be always defined");
            }
            if (map.get(GlobalDataRest.X_CURSOR).isEmpty()) {
                throw new IllegalStateException(GlobalDataRest.X_CURSOR + " should be defined");
            }
            xcursor = Boolean.parseBoolean(map.getFirst(GlobalDataRest.X_CURSOR));
            if (!xcursor && isNullOrEmpty(getCursorId(headers))) {
                throw new IllegalStateException(GlobalDataRest.X_CURSOR + " should be true when " +
                    GlobalDataRest.X_CURSOR_ID + " is not set");
            }
            return xcursor;
        }
        throw new IllegalStateException("Headers is null");
    }

    /**
     * Helper for server side to check if this is a end of cursor
     *
     * @param headers
     * @return True if the cursor is to be ended on Server side
     * @throws IllegalStateException if the headers are not consistent
     */
    public static boolean isEndOfCursor(HttpHeaders headers) {
        return !checkHeadersConformity(headers);
    }

    /**
     * Helper for server side to check if this is a ending of cursor
     *
     * @param xcursor
     * @param xcursorId
     * @return True if the cursor is to be ended on Server side
     */
    public static boolean isEndOfCursor(boolean xcursor, String xcursorId) {
        return !xcursor && !isNullOrEmpty(xcursorId);
    }

    /**
     * Helper for server side to check if this is a creation of cursor
     *
     * @param headers
     * @return True if the cursor is to be created on Server side
     * @throws IllegalStateException if the headers are not consistent
     */
    public static boolean isNewCursor(HttpHeaders headers) {
        final boolean xcursor = checkHeadersConformity(headers);
        if (!xcursor) {
            return false;
        }
        final List<String> cidlist = headers.getRequestHeader(GlobalDataRest.X_CURSOR_ID);
        if (cidlist == null || cidlist.isEmpty()) {
            return xcursor;
        }
        return isNewCursor(xcursor, cidlist.get(0));
    }

    /**
     * Helper for server side to check if this is a creation of cursor
     *
     * @param xcursor
     * @param xcursorId
     * @return True if the cursor is to be created on Server side
     */
    public static boolean isNewCursor(boolean xcursor, String xcursorId) {
        return xcursor && isNullOrEmpty(xcursorId);
    }

    /**
     * Helper for server side to get the cursor Id
     *
     * @param headers
     * @return the X-Cursor-ID content
     */
    public static String getCursorId(HttpHeaders headers) {
        final List<String> cidlist = headers.getRequestHeader(GlobalDataRest.X_CURSOR_ID);
        if (cidlist == null) {
            return "";
        }
        return cidlist.get(0);
    }

    /**
     * Helper for server and client to set the needed headers
     *
     * @param builder   the current ResponseBuilder
     * @param active    True for create or continue, False for inactive (X-Cursor)
     * @param xcursorId may be null, else contains the current X-Cursor-Id
     * @return the ResponseBuilder with the new required header
     */
    public static ResponseBuilder setHeaders(ResponseBuilder builder, boolean active, String xcursorId) {
        builder.header(GlobalDataRest.X_CURSOR, active);
        if (xcursorId != null) {
            builder.header(GlobalDataRest.X_CURSOR_ID, xcursorId);
        }
        return builder;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
