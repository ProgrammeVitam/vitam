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
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServiceUnavailableException;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckRequest;
import fr.gouv.vitam.storage.driver.model.StorageCheckResult;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;

/**
 * Workspace Connection Implementation
 */
public class ConnectionImpl extends AbstractConnection {

    private static final String X_USED_SPACE = "X-Used-Space";

    private static final String X_USABLE_SPACE = "X-Usable-Space";
    private static final long DEFAULT_MAX_AVAILABILITY = 100000000000L;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionImpl.class);

    private static final String OBJECTS_PATH = "/objects";
    private static final String LOGS_PATH = "/logs";
    private static final String METADATAS = "/metadatas";

    private static final String REQUEST_IS_A_MANDATORY_PARAMETER = "Request is a mandatory parameter";
    private static final String GUID_IS_A_MANDATORY_PARAMETER = "GUID is a mandatory parameter";
    private static final String TENANT_IS_A_MANDATORY_PARAMETER = "Tenant is a mandatory parameter";
    private static final String ALGORITHM_IS_A_MANDATORY_PARAMETER = "Algorithm is a mandatory parameter";
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "Stream is a mandatory parameter";
    private static final String TYPE_IS_A_MANDATORY_PARAMETER = "Type is a mandatory parameter";
    private static final String ORDER_IS_A_MANDATORY_PARAMETER = "Order is a mandatory parameter";
    private static final String TYPE_IS_NOT_VALID = "Type is not valid";
    private static final String FOLDER_IS_A_MANDATORY_PARAMETER = "Folder is a mandatory parameter";
    private static final String FOLDER_IS_NOT_VALID = "Folder is not valid";
    private static final String DIGEST_IS_A_MANDATORY_PARAMETER = "Digest is a mandatory parameter";

    @SuppressWarnings("unused")
    private final Properties parameters;

    /**
     * Constructor
     *
     * @param driverName
     * @param factory
     * @param parameters
     */
    public ConnectionImpl(String driverName, VitamClientFactoryInterface<? extends AbstractConnection> factory,
        Properties parameters) {
        super(driverName, factory);
        this.parameters = parameters;
    }

    @Override
    public StorageCapacityResult getStorageCapacity(Integer tenantId)
        throws StorageDriverPreconditionFailedException, StorageDriverNotFoundException, StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, OBJECTS_PATH + "/" + DataCategory.OBJECT,
                getDefaultHeaders(tenantId, null, null, null),
                MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            switch (status) {
                case OK:
                    long available = DEFAULT_MAX_AVAILABILITY;
                    if (response.getHeaderString(X_USABLE_SPACE) != null) {
                        try {
                            available = Long.parseLong(response.getHeaderString(X_USABLE_SPACE));
                        } catch (NumberFormatException e) {
                            LOGGER.info("Not a number", e);
                        }
                    }
                    long used = 0;
                    if (response.getHeaderString(X_USED_SPACE) != null) {
                        try {
                            used = Long.parseLong(response.getHeaderString(X_USED_SPACE));
                        } catch (NumberFormatException e) {
                            LOGGER.info("Not a number", e);
                        }
                    }
                    return new StorageCapacityResult(tenantId, available, used);
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
                        tenantId + "_" + DataCategory.OBJECT));
                    throw new StorageDriverNotFoundException(getDriverName(),
                        VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
                            tenantId + "_" + DataCategory.OBJECT));
                case BAD_REQUEST:
                    LOGGER.error("Bad request");
                    throw new StorageDriverPreconditionFailedException(getDriverName(), "Bad request");
                default:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                    throw new StorageDriverException(getDriverName(), response.getStatusInfo().getReasonPhrase(), true);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(), VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR.getMessage(), true,
                    e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public StorageGetResult getObject(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET,
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                getDefaultHeaders(request.getTenantId(), null, null, null),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return new StorageGetResult(request.getTenantId(), request.getType(),
                        request.getGuid(), response);
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverNotFoundException(getDriverName(),
                        "Object " + request.getGuid() + " not found");
                case PRECONDITION_FAILED:
                    LOGGER.error("Precondition failed");
                    throw new StorageDriverPreconditionFailedException(getDriverName(), "Precondition failed");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(), INTERNAL_SERVER_ERROR, true);
            }
        } catch (final VitamClientInternalException e1) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e1);
            throw new StorageDriverException(getDriverName(), true, e1);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public StoragePutResult putObject(StoragePutRequest request) throws StorageDriverException {
        Response response = null;
        try {
            ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
            ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
            ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
            ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
            ParametersChecker.checkParameter(TYPE_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
            ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, request.getDataStream());

            final InputStream stream = request.getDataStream();
            // init
            response = performRequest(HttpMethod.PUT,
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                getDefaultHeaders(request.getTenantId(), null,
                    request.getDigestAlgorithm(), request.getSize()),
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);

            final JsonNode json = handleResponseStatus(response, JsonNode.class);

            if (Response.Status.CREATED.getStatusCode() != response.getStatus()) {
                LOGGER.error("Error while performing put object operation");
                throw new StorageDriverException(getDriverName(), "Error while performing put object operation", true);
            }

            StoragePutResult result =
                new StoragePutResult(request.getTenantId(), request.getType(), request.getGuid(), request.getGuid(),
                    json.get("digest").textValue(), json.get("size").longValue());

            return result;
        } catch (final IllegalArgumentException exc) {
            throw new StorageDriverPreconditionFailedException(getDriverName(), exc);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public StorageRemoveResult removeObject(StorageRemoveRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());

        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
        ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
        ParametersChecker.checkParameter(DIGEST_IS_A_MANDATORY_PARAMETER, request.getDigestHashBase16());
        Response response = null;
        try {
            response = performRequest(HttpMethod.DELETE,
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                getDefaultHeaders(request.getTenantId(), request.getDigestHashBase16(),
                    request.getDigestAlgorithm().getName(), null),
                MediaType.APPLICATION_JSON_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    final JsonNode json = handleResponseStatus(response, JsonNode.class);
                    final StorageRemoveResult result = new StorageRemoveResult(request.getTenantId(), request.getType(),
                        request.getGuid(), request.getDigestAlgorithm(), request.getDigestHashBase16(),
                        Response.Status.OK.toString().equals(json.get("status").asText()));
                    return result;
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverNotFoundException(getDriverName(), "Object " + request.getGuid() +
                        "not found");
                case BAD_REQUEST:
                    LOGGER.error("Bad request");
                    throw new StorageDriverPreconditionFailedException(getDriverName(), "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(), INTERNAL_SERVER_ERROR, true);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(), true, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD,
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                getDefaultHeaders(request.getTenantId(), null, null, null),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                case NO_CONTENT:
                    return true;
                case NOT_FOUND:
                    return false;
                case BAD_REQUEST:
                    LOGGER.error("Bad request");
                    throw new StorageDriverPreconditionFailedException(getDriverName(), "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(), INTERNAL_SERVER_ERROR, true);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(), true, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Common method to handle response status
     *
     * @param response     the response to be handled
     * @param responseType the type to map the response into
     * @param <R>          the class type to be returned
     * @return the response mapped as a POJO
     * @throws StorageDriverException if any from the server
     */
    protected <R> R handleResponseStatus(Response response, Class<R> responseType) throws StorageDriverException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case CREATED:
            case OK:
                return response.readEntity(responseType);
            case INTERNAL_SERVER_ERROR:
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageDriverException(getDriverName(), status.getReasonPhrase(), true);
            case NOT_FOUND:
                // FIXME P1 : clean useless case
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverNotFoundException(getDriverName(), status.getReasonPhrase());
            case SERVICE_UNAVAILABLE:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverServiceUnavailableException(getDriverName(), status.getReasonPhrase());
            case CONFLICT:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverConflictException(getDriverName(), status.getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new StorageDriverException(getDriverName(), INTERNAL_SERVER_ERROR, true);
        }
    }

    /**
     * Generate the default header map
     *
     * @param tenantId   the tenantId
     * @param digest     the digest of the object to be added
     * @param digestType the type of the digest to be added
     * @param size
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(Integer tenantId, String digest,
        String digestType, Long size) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        if (tenantId != null) {
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        }
        if (digest != null) {
            headers.add(GlobalDataRest.X_DIGEST, digest);
        }
        if (digestType != null) {
            headers.add(GlobalDataRest.X_DIGEST_ALGORITHM, digestType);
        }
        if (size != null) {
            headers.add(GlobalDataRest.VITAM_CONTENT_LENGTH, size);
        }
        return headers;
    }

    @Override
    public StorageCheckResult checkObject(StorageCheckRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
        ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
        ParametersChecker.checkParameter(DIGEST_IS_A_MANDATORY_PARAMETER, request.getDigestHashBase16());
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD,
                    OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                    getDefaultHeaders(request.getTenantId(),
                        request.getDigestHashBase16(), request.getDigestAlgorithm().getName(), null),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                case CONFLICT:
                    return new StorageCheckResult(request.getTenantId(), request.getType(),
                        request.getGuid(), request.getDigestAlgorithm(), request.getDigestHashBase16(),
                        status.equals(Status.OK));
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverException(getDriverName(),
                        "Object " + "not found", false);
                case PRECONDITION_FAILED:
                    LOGGER.error("Precondition failed");
                    throw new StorageDriverException(getDriverName(),
                        "Precondition failed", false);
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(),
                        INTERNAL_SERVER_ERROR, true);
            }
        } catch (final VitamClientInternalException e1) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e1);
            throw new StorageDriverException(getDriverName(),
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), true, e1);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public StorageMetadatasResult getMetadatas(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET,
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid() + METADATAS,
                getDefaultHeaders(request.getTenantId(), null, null, null), MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return handleResponseStatus(response, StorageMetadatasResult.class);
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverNotFoundException(getDriverName(),
                        "Object " + "not found");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(),
                            INTERNAL_SERVER_ERROR, true);
            }
        } catch (VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(),
                    VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), true, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> listObjects(StorageListRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter("X-Cursor is mandatory", request.isxCursor());
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, request.getTenantId());
            headers.add(GlobalDataRest.X_CURSOR, request.isxCursor());
            if (request.getCursorId() != null) {
                headers.add(GlobalDataRest.X_CURSOR_ID, request.getCursorId());
            }
            response =
                performRequest(HttpMethod.GET, OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()),
                    headers, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.<JsonNode>parseFromResponse(response);
        } catch (Exception exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageDriverException(getDriverName(), true, exc);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(StorageOfferLogRequest storageGetOfferLogRequest)
        throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getType());
        ParametersChecker.checkParameter(ORDER_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getOrder());
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, storageGetOfferLogRequest.getTenantId());
            OfferLogRequest offerLogRequest = new OfferLogRequest();
            offerLogRequest.setOffset(storageGetOfferLogRequest.getOffset());
            offerLogRequest.setLimit(storageGetOfferLogRequest.getLimit());
            offerLogRequest.setOrder(storageGetOfferLogRequest.getOrder());
            response =
                performRequest(HttpMethod.GET,
                    OBJECTS_PATH + "/" + DataCategory.getByFolder(storageGetOfferLogRequest.getType()) + LOGS_PATH,
                    headers, offerLogRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, OfferLog.class);
        } catch (Exception exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageDriverException(getDriverName(), true, exc);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
