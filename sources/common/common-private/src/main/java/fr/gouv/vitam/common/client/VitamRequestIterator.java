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
package fr.gouv.vitam.common.client;

import java.util.Iterator;
import java.util.Objects;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

import static fr.gouv.vitam.common.GlobalDataRest.X_CURSOR;
import static fr.gouv.vitam.common.GlobalDataRest.X_CURSOR_ID;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

public class VitamRequestIterator<T> implements VitamAutoCloseable, Iterator<T> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIterator.class);

    private final AbstractCommonClient client;
    private final Class<T> responseType;
    private final VitamRequestBuilder requestBuilder;

    private boolean first = true;
    private boolean closed = false;
    private RequestResponseOK<T> objectResponse = null;
    private Iterator<T> iterator = null;

    public VitamRequestIterator(AbstractCommonClient client,  VitamRequestBuilder requestBuilder, Class<T> responseType) {
        this.client = Objects.requireNonNull(client);
        this.requestBuilder = Objects.requireNonNull(requestBuilder);
        this.responseType = Objects.requireNonNull(responseType);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // Callback to close the cursor
        closed = true;
        requestBuilder.withNoContentType()
            .withNoBaseUrl()
            .withJsonAccept()
            .withHeaderReplaceExisting(X_CURSOR, false);
        try (Response response = client.make(requestBuilder)) {
            check(response);
        } catch (VitamClientInternalException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private boolean handleFirst(Response response) {
        Object cursorId = response.getHeaders().getFirst(X_CURSOR_ID);
        // TODO P1 Ignore for the moment X-Cursor-Timeout
        if (cursorId == null && !closed) {
            throw new VitamRuntimeException("No Cursor returned");
        }

        requestBuilder.withHeaderReplaceExisting(X_CURSOR_ID, cursorId);

        return handleNext(response);
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
            try (Response response = client.make(requestBuilder.withHeaderReplaceExisting(X_CURSOR, true).withJson())) {
                check(response);
                if (PARTIAL_CONTENT.equals(response.getStatusInfo().toEnum())) {
                    return handleFirst(response);
                }
                closed = true;
                return handleFirst(response);
            } catch (VitamClientInternalException e) {
                closed = true;
                throw new VitamRuntimeException(e);
            } catch (NotFoundException e) {
                closed = true;
                return false;
            }
        }

        try (Response response = client.make(requestBuilder.withNoBaseUrl().withBody(JsonHandler.createObjectNode()).withJson())) {
            check(response);
            if (PARTIAL_CONTENT.equals(response.getStatusInfo().toEnum())) {
                return handleNext(response);
            }
            closed = true;
            return handleNext(response);
        } catch (VitamClientInternalException e) {
            closed = true;
            throw new VitamRuntimeException(e);
        } catch (NotFoundException e) {
            closed = true;
            return false;
        }
    }

    @Override
    public T next() {
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
     * @param xcursor
     * @param xcursorId
     * @return True if the cursor is to be created on Server side
     */
    public static boolean isNewCursor(boolean xcursor, String xcursorId) {
        return xcursor && isNullOrEmpty(xcursorId);
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
        builder.header(X_CURSOR, active);
        if (xcursorId != null) {
            builder.header(X_CURSOR_ID, xcursorId);
        }
        return builder;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    private void check(Response response) throws VitamClientInternalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }
        if (NOT_FOUND.equals(status)) {
            throw new NotFoundException();
        }
        throw new VitamClientInternalException(String.format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(), fromStatusCode(response.getStatus()).getReasonPhrase()));
    }
}
