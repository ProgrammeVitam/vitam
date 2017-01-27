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
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.PutObjectResult;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl.InternalDriverFactory;

/**
 * Workspace Connection Implementation
 */
public class ConnectionImpl extends DefaultClient implements Connection {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionImpl.class);

    private static final String OBJECT = "object_";

    private static final String OBJECTS_PATH = "/objects";

    private static final String NOT_YET_IMPLEMENTED = "Not yet implemented";

    private static final String REQUEST_IS_A_MANDATORY_PARAMETER = "Request is a mandatory parameter";
    private static final String GUID_IS_A_MANDATORY_PARAMETER = "GUID is a mandatory parameter";
    private static final String TENANT_IS_A_MANDATORY_PARAMETER = "Tenant is a mandatory parameter";
    private static final String ALGORITHM_IS_A_MANDATORY_PARAMETER = "Algorithm is a mandatory parameter";
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "Stream is a mandatory parameter";
    private static final String TYPE_IS_A_MANDATORY_PARAMETER = "Type is a mandatory parameter";
    private static final String TYPE_IS_NOT_VALID = "Type is not valid";
    private static final String FOLDER_IS_A_MANDATORY_PARAMETER = "Folder is a mandatory parameter";
    private static final String FOLDER_IS_NOT_VALID = "Folder is not valid";
    private final String driverName;

    private final Properties parameters;

    /**
     * Constructor
     *
     * @param factory
     * @param parameters
     */
    public ConnectionImpl(InternalDriverFactory factory, Properties parameters) {
        super(factory);
        driverName = factory.getName();
        this.parameters = parameters;
    }

    /**
     * return account capacity for swift offer
     */
    @Override
    public StorageCapacityResult getStorageCapacity(Integer tenantId) throws StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, OBJECTS_PATH, getDefaultHeadersWithContainerName(tenantId, DataCategory.OBJECT + "_" + tenantId, null),
                MediaType.APPLICATION_JSON_TYPE, false);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return handleResponseStatus(response, StorageCapacityResult.class);
            }
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                response.getStatusInfo().getReasonPhrase());
        } catch (final VitamClientInternalException e) {
            LOGGER.error(e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public GetObjectResult getObject(GetObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getFolder());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, request.getFolder());
        Response response = null;
        try {            
            response =
                performRequest(HttpMethod.GET, OBJECTS_PATH + "/" + request.getGuid(),
                    getDefaultHeadersWithContainerName(request.getTenantId(), DataCategory.getByFolder(request.getFolder()) + "_" + request.getTenantId(), null),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    final GetObjectResult result =
                        new GetObjectResult(request.getTenantId(), response);
                    return result;
                case NOT_FOUND:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                        "not found");
                case PRECONDITION_FAILED:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Precondition failed");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e1) {
            LOGGER.error(e1);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e1.getMessage());
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest request) throws StorageDriverException {
        Response response = null;
        try {
            ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
            ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
            ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
            ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
            ParametersChecker.checkParameter(TYPE_IS_NOT_VALID, request.getType());
            ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, request.getDataStream());

            final InputStream stream = request.getDataStream();
            // init
            final ObjectInit objectInit = new ObjectInit();
            objectInit.setDigestAlgorithm(DigestType.fromValue(request.getDigestAlgorithm()));
            objectInit.setType(DataCategory.valueOf(request.getType()));
            response =
                performRequest(HttpMethod.POST, OBJECTS_PATH + "/" + request.getGuid(),
                    getDefaultHeadersWithContainerName(request.getTenantId(), request.getType() + "_" + request.getTenantId(), StorageConstants.COMMAND_INIT),
                    objectInit, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            return performPutRequests(request.getType() + "_" + request.getTenantId(), stream,
                handleResponseStatus(response, ObjectInit.class), request.getTenantId());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED, exc
                .getMessage());
        } catch (final VitamClientInternalException e) {
            LOGGER.error(e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RemoveObjectResult removeObject(RemoveObjectRequest request) throws StorageDriverException {
        throw new UnsupportedOperationException(NOT_YET_IMPLEMENTED);
    }

    @Override
    public Boolean objectExistsInOffer(GetObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD, OBJECTS_PATH + "/" + request.getGuid(),
                    getDefaultHeadersWithContainerName(request.getTenantId(), request.getFolder() + "_" + request.getTenantId(), null),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                case NO_CONTENT:
                    return true;
                case NOT_FOUND:
                    return false;
                case BAD_REQUEST:
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Common method to handle response status
     *
     * @param response the response to be handled
     * @param responseType the type to map the response into
     * @param <R> the class type to be returned
     * @return the response mapped as a POJO
     * @throws StorageDriverException if any from the server
     */
    protected <R> R handleResponseStatus(Response response, Class<R> responseType)
        throws StorageDriverException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case CREATED:
            case OK:
                return response.readEntity(responseType);
            case INTERNAL_SERVER_ERROR:
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    status.getReasonPhrase());
            case NOT_FOUND:
                // FIXME P1 : clean useless case
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, status
                    .getReasonPhrase());
            case SERVICE_UNAVAILABLE:
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, status
                    .getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
        }
    }



    /**
     * Generate the default header map
     *
     * @param tenantId the tenantId
     * @param command the command to be added
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(Integer tenantId, String command) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        if (tenantId != null) {
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        }
        if (command != null) {
            headers.add(GlobalDataRest.X_COMMAND, command);
        }
        return headers;
    }


    /**
     * Generate the default header map
     *
     * @param tenantId the tenantId
     * @param containerName the containerName
     * @param command the command to be added
     * @return header map
     */
    // TODO - us#1982 - to be changed with this story - tenantId to stay in the header but path (type unit or object) in the uri
    private MultivaluedHashMap<String, Object> getDefaultHeadersWithContainerName(Integer tenantId,
        String containerName, String command) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        if (tenantId != null) {
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        }
        if (containerName != null) {
            headers.add("X_CONTAINER_NAME", containerName);
        }
        if (command != null) {
            headers.add(GlobalDataRest.X_COMMAND, command);
        }
        return headers;
    }

    /**
     * Method performing a PutRequests
     *
     * @param containerName the container Name
     * @param stream the stream to be chunked if necessary
     * @param result the result received from the server after the init
     * @param tenantId the tenant id
     * @return a PutObjectResult the final result received from the server
     * @throws StorageDriverException in case the server encounters an exception
     */
    private PutObjectResult performPutRequests(String containerName, InputStream stream, ObjectInit result,
        Integer tenantId)
        throws StorageDriverException {
        PutObjectResult finalResult = null;
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, OBJECTS_PATH + "/" + result.getId(),
                getDefaultHeadersWithContainerName(tenantId, containerName, StorageConstants.COMMAND_END),
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final JsonNode json = handleResponseStatus(response, JsonNode.class);
            finalResult = new PutObjectResult(result.getId(), json.get("digest").textValue(), tenantId,
                Long.valueOf(json.get("size").textValue()));
            if (Response.Status.CREATED.getStatusCode() != response.getStatus()) {
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error to perfom put object");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
        return finalResult;
    }

}
