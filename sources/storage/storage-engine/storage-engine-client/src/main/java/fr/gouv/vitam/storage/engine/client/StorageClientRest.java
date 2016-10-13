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

package fr.gouv.vitam.storage.engine.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractSSLClient;
import fr.gouv.vitam.common.client.SSLClientConfiguration;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * StorageClient Abstract class use to set generic client configuration (not depending on client type)
 */
class StorageClientRest extends AbstractSSLClient implements StorageClient {
    private static final String OBJECT_DESCRIPTION_MUST_HAVE_A_VALID_VALUE =
        "Object's description must have a valid value";
    private static final String OBJECT_DESCRIPTION_GUID_MUST_HAVE_A_VALID_VALUE =
        "Object's description container's GUID must have a valid value";
    private static final String OBJECT_DESCRIPTION_URI_MUST_HAVE_A_VALID_VALUE =
        "Object's description workspace's URI must have a valid value";
    private static final String GUID_MUST_HAVE_A_VALID_VALUE = "GUID must have a valid value";
    private static final String TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE =
        "Type of storage object must have a valid value";
    private static final String STRATEGY_ID_MUST_HAVE_A_VALID_VALUE = "Strategy id must have a valid value";
    private static final String TENANT_ID_MUST_HAVE_A_VALID_VALUE = "Tenant id must have a valid value";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientRest.class);

    StorageClientRest(SSLClientConfiguration clientConfiguration, String resourcePath,
        boolean suppressHttpCompliance) {
        super(clientConfiguration, resourcePath, suppressHttpCompliance);
    }

    StorageClientRest(SSLClientConfiguration clientConfiguration, String resourcePath, Client client) {
        super(clientConfiguration, resourcePath, client);
    }

    @Override
    public JsonNode getStorageInformation(String tenantId, String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response =
                performGenericRequest("/", null, MediaType.APPLICATION_JSON, getDefaultHeaders(tenantId, strategyId),
                    HttpMethod.GET, MediaType.APPLICATION_JSON);
            return handleCommonResponseStatus(response, JsonNode.class);
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public StoredInfoResult storeFileFromWorkspace(String tenantId, String strategyId, StorageCollectionType type,
        String guid,
        CreateObjectDescription description)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        ParametersChecker.checkParameter(OBJECT_DESCRIPTION_MUST_HAVE_A_VALID_VALUE, description);
        if (description != null) {
            ParametersChecker.checkParameter(OBJECT_DESCRIPTION_GUID_MUST_HAVE_A_VALID_VALUE,
                description.getWorkspaceContainerGUID());
            ParametersChecker.checkParameter(OBJECT_DESCRIPTION_URI_MUST_HAVE_A_VALID_VALUE,
                description.getWorkspaceObjectURI());
        }
        Response response = null;
        try {
            response =
                performGenericRequest("/" + type.getCollectionName() + "/" + guid, description,
                    MediaType.APPLICATION_JSON, getDefaultHeaders(tenantId, strategyId), HttpMethod.POST,
                    MediaType.APPLICATION_JSON);
            return handlePostResponseStatus(response, StoredInfoResult.class);
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public boolean existsContainer(String tenantId, String strategyId) throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response = performHeadRequest("/", getDefaultHeaders(tenantId, strategyId));
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public boolean exists(String tenantId, String strategyId, StorageCollectionType type, String guid)
        throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        if (StorageCollectionType.CONTAINERS.equals(type)) {
            throw new IllegalArgumentException("Type of storage object cannot be " + type.getCollectionName());
        }
        Response response = null;
        try {
            response = performHeadRequest("/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId));
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }


    @Override
    public boolean deleteContainer(String tenantId, String strategyId) throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response = performDeleteRequest("/", getDefaultHeaders(tenantId, strategyId));
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public boolean delete(String tenantId, String strategyId, StorageCollectionType type, String guid)
        throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        if (StorageCollectionType.CONTAINERS.equals(type)) {
            throw new IllegalArgumentException(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CLIENT_STORAGE_TYPE,
                type.getCollectionName()));
        }
        Response response = null;
        try {
            response = performDeleteRequest("/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId));
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    /**
     * Tranform a noContent call response to a boolean (or error)
     * 
     * @param status Http reponse
     * @return true 204, false if 404
     * @throws StorageServerClientException is thrown if an unexpected response is sent by the server
     */
    private boolean notContentResponseToBoolean(Response.Status status) throws StorageServerClientException {
        boolean result;
        switch (status) {
            case NO_CONTENT:
                result = true;
                break;
            case NOT_FOUND:
                result = false;
                break;
            default:
                String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
        return result;
    }

    /**
     * Perform a HEAD request on given resource
     * 
     * @param path the path to the resource to request
     * @param headers headers HTTP to add to request
     * @return the server response
     */
    protected Response performHeadRequest(String path, MultivaluedHashMap<String, Object> headers) {
        Invocation.Builder request = getClient().target(getServiceUrl()).path(path).request().headers(headers);
        return request.head();
    }



    /**
     * Perform a DELETE request on given resource
     * 
     * @param path the path to the resource to request
     * @return the server response
     */
    protected Response performDeleteRequest(String path, MultivaluedHashMap<String, Object> headers) {
        Invocation.Builder request = getClient().target(getServiceUrl()).path(path).request().headers(headers);
        return request.delete();
    }


    /**
     * Generate the default header map
     * 
     * @param tenantId the tenant id
     * @param strategyId the storage strategy id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String tenantId, String strategyId) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_STRATEGY_ID, strategyId);
        return headers;
    }


    /**
     * Common method to handle responses expecting NO_CONTENT (204) status
     * 
     * @param response the server response
     * @return the response status if it is an expected response (204, 404 and 412)
     * @throws StorageServerClientException is thrown if an unexpected response is sent by the server
     */
    protected Response.Status handleNoContentResponseStatus(Response response) throws StorageServerClientException {
        ParametersChecker.checkParameter("Response", response);
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case NO_CONTENT:
            case NOT_FOUND:
            case PRECONDITION_FAILED:
                return status;
            default:
                String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }

    }


    protected <R> R handlePostResponseStatus(Response response, Class<R> responseType)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
            case CREATED:
                return response.readEntity(responseType);
            case CONFLICT:
                throw new StorageAlreadyExistsClientException(
                    VitamCodeHelper.getCode(VitamCode.STORAGE_CLIENT_ALREADY_EXISTS) + " : " +
                        status.getReasonPhrase());
            case NOT_FOUND:
                throw new StorageNotFoundClientException(VitamCodeHelper.getCode(VitamCode.STORAGE_NOT_FOUND) + " : " +
                    status.getReasonPhrase());
            default:
                String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
    }


    @Override
    protected <R> R handleCommonResponseStatus(Response response, Class<R> responseType)
        throws StorageNotFoundClientException, StorageServerClientException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                return response.readEntity(responseType);
            case NOT_FOUND:
                // No space left on storage offer(s)
                throw new StorageNotFoundClientException(VitamCodeHelper.getCode(VitamCode.STORAGE_NOT_FOUND) + " : " +
                    status.getReasonPhrase());
            default:
                String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
    }

    @Override
    public InputStream getContainer(String tenantId, String strategyId, String guid, StorageCollectionType type)
        throws StorageServerClientException, StorageNotFoundException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        Response response = null;
        InputStream stream = null;
        try {
            response =
                performGenericRequest("/" + type.getCollectionName() + "/" + guid, null,
                    MediaType.APPLICATION_OCTET_STREAM, getDefaultHeaders(tenantId, strategyId), HttpMethod.GET,
                    MediaType.APPLICATION_JSON);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    // TODO : this is ugly but necessarily in order to close the response and avoid concurrent issues
                    // to be improved (https://jersey.java.net/documentation/latest/client.html#d0e5170) and
                    // remove the IOUtils.toByteArray after correction of concurrent problem
                    InputStream streamClosedAutomatically = response.readEntity(InputStream.class);
                    try {
                        stream = new ByteArrayInputStream(IOUtils.toByteArray(streamClosedAutomatically));
                    } catch (IOException e) {
                        LOGGER.error(VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " + e);
                        throw new StorageServerClientException(
                            VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
                    }
                    return stream;
                case NOT_FOUND:
                    throw new StorageNotFoundException(VitamCodeHelper.getCode(VitamCode.STORAGE_NOT_FOUND) + " : " +
                        status.getReasonPhrase());
                case PRECONDITION_FAILED:
                    throw new StorageServerClientException(
                        VitamCodeHelper.getCode(VitamCode.STORAGE_MISSING_HEADER) + ": " + status.getReasonPhrase());
                default:
                    String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                        status.getReasonPhrase();
                    LOGGER.error(log);
                    throw new StorageServerClientException(log);
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

}
