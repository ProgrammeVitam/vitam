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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
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
class StorageClientRest extends DefaultClient implements StorageClient {
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

    StorageClientRest(StorageClientFactory factory) {
        super(factory);
    }

    @Override
    public JsonNode getStorageInformation(String tenantId, String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, "/",
                    getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_JSON_TYPE);
            return handleCommonResponseStatus(response, JsonNode.class);
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
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
            response = performRequest(HttpMethod.POST, "/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId), description, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return handlePostResponseStatus(response, StoredInfoResult.class);
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean existsContainer(String tenantId, String strategyId) throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, "/",
                getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_JSON_TYPE);
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
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
            response = performRequest(HttpMethod.HEAD, "/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_JSON_TYPE);
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public boolean deleteContainer(String tenantId, String strategyId) throws StorageServerClientException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.DELETE, "/",
                getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_JSON_TYPE);
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
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
            response = performRequest(HttpMethod.DELETE, "/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_JSON_TYPE);
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            consumeAnyEntityAndClose(response);
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
                final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
        return result;
    }

    /**
     * Generate the default header map
     *
     * @param tenantId the tenant id
     * @param strategyId the storage strategy id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String tenantId, String strategyId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
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
                final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
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
                final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
    }


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
                final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
    }


    @Override
    public Response getContainerAsync(String tenantId, String strategyId, String guid, StorageCollectionType type)
        throws StorageServerClientException, StorageNotFoundException {
        ParametersChecker.checkParameter(TENANT_ID_MUST_HAVE_A_VALID_VALUE, tenantId);
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        Response response = null;
        boolean ok = false;
        try {
            response = performRequest(HttpMethod.GET, "/" + type.getCollectionName() + "/" + guid,
                getDefaultHeaders(tenantId, strategyId), MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    ok = true;
                    return response;
                case NOT_FOUND:
                    throw new StorageNotFoundException(VitamCodeHelper.getCode(VitamCode.STORAGE_NOT_FOUND) + " : " +
                        status.getReasonPhrase());
                case PRECONDITION_FAILED:
                    throw new StorageServerClientException(
                        VitamCodeHelper.getCode(VitamCode.STORAGE_MISSING_HEADER) + ": " + status.getReasonPhrase());
                default:
                    final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                        status.getReasonPhrase();
                    LOGGER.error(log);
                    throw new StorageServerClientException(log);
            }
        } catch (VitamClientInternalException e) {
            String errorMessage = VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } finally {
            // Only if KO
            if (!ok) {
                DefaultClient.staticConsumeAnyEntityAndClose(response);
            }
        }
    }

}
