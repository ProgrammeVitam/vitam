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
package fr.gouv.vitam.common.client;

import java.util.concurrent.Future;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;

/**
 * Basic client api for vitam client either in Mock or Rest mode
 */
public interface MockOrRestClient extends AutoCloseable {

    /**
     * Get the resource path of the server.
     *
     * @return the resource path as string
     */
    String getResourcePath();

    /**
     * Get the service URL
     *
     * @return the service URL
     */
    String getServiceUrl();

    /**
     * Check the status from the service
     *
     * @throws VitamApplicationServerException if the Server is unavailable
     */
    void checkStatus() throws VitamApplicationServerException;

    /**
     * Perform a HTTP request to the server for synchronous call using default chunked mode configured in this client
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException 
     */
    Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
        MediaType accept) throws VitamClientInternalException;

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param accept asked type of response
     * @param chunkedMode True use default client, else False use non Chunked mode client
     * @return the response from the server
     * @throws VitamClientInternalException 
     */
    Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
        MediaType accept, boolean chunkedMode) throws VitamClientInternalException;

    /**
     * Perform a HTTP request to the server for synchronous call
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException 
     */
    Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept) throws VitamClientInternalException;

    /**
     * Perform an Async HTTP request to the server with callback
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @param callback
     * @param <T> the type of the Future result (generally Response)
     * @return the response from the server
     * @throws VitamClientInternalException 
     */
    <T> Future<T> performAsyncRequest(String httpMethod, String path,
        MultivaluedHashMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept,
        InvocationCallback<T> callback) throws VitamClientInternalException;

    /**
     * Perform an Async HTTP request to the server with full control of action on caller
     *
     * @param httpMethod HTTP method to use for request
     * @param path URL to request
     * @param headers headers HTTP to add to request, may be null
     * @param body body content of type contentType, may be null
     * @param contentType the media type of the body to send, null if body is null
     * @param accept asked type of response
     * @return the response from the server
     * @throws VitamClientInternalException 
     */
    Future<Response> performAsyncRequest(String httpMethod, String path,
        MultivaluedHashMap<String, Object> headers,
        Object body, MediaType contentType, MediaType accept) throws VitamClientInternalException;

    /**
     * Helper when an error occurs on client usage side to consume response however
     *
     * @param response
     */
    void consumeAnyEntityAndClose(Response response);

    /**
     * Close the underneath http client
     */
    @Override
    void close();
}
