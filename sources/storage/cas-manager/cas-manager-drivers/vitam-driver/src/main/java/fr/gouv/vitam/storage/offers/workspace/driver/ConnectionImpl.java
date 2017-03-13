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
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckRequest;
import fr.gouv.vitam.storage.driver.model.StorageCheckResult;
import fr.gouv.vitam.storage.driver.model.StorageCountResult;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.driver.model.StorageRequest;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl.InternalDriverFactory;

/**
 * Workspace Connection Implementation
 */
public class ConnectionImpl extends DefaultClient implements Connection {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionImpl.class);

    private static final String OBJECTS_PATH = "/objects";
    private static final String COUNT_PATH = "/count";
    private static final String METADATAS = "/metadatas";

    private static final String REQUEST_IS_A_MANDATORY_PARAMETER = "Request is a mandatory parameter";
    private static final String GUID_IS_A_MANDATORY_PARAMETER = "GUID is a mandatory parameter";
    private static final String TENANT_IS_A_MANDATORY_PARAMETER = "Tenant is a mandatory parameter";
    private static final String ALGORITHM_IS_A_MANDATORY_PARAMETER = "Algorithm is a mandatory parameter";
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "Stream is a mandatory parameter";
    private static final String TYPE_IS_A_MANDATORY_PARAMETER = "Type is a mandatory parameter";
    private static final String TYPE_IS_NOT_VALID = "Type is not valid";
    private static final String FOLDER_IS_A_MANDATORY_PARAMETER = "Folder is a mandatory parameter";
    private static final String FOLDER_IS_NOT_VALID = "Folder is not valid";
    private static final String DIGEST_IS_A_MANDATORY_PARAMETER = "Digest is a mandatory parameter";

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

    @Override
    public StorageCapacityResult getStorageCapacity(Integer tenantId) throws StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, OBJECTS_PATH + "/" + DataCategory.OBJECT,
                getDefaultHeaders(tenantId, null, null, null),
                MediaType.APPLICATION_JSON_TYPE, false);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                StorageCapacityResult result = new StorageCapacityResult(tenantId, Long.valueOf(response.getHeaderString
                    ("X-Usable-Space")), Long.valueOf(response.getHeaderString("X-Used-Space")));
                return result;
            }
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                response.getStatusInfo().getReasonPhrase());
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public StorageCountResult countObjects(StorageRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, OBJECTS_PATH + "/" + request.getType() + COUNT_PATH,
                getDefaultHeaders(request.getTenantId(), null, null, null),
                MediaType.APPLICATION_JSON_TYPE, false);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                JsonNode result = handleResponseStatus(response, JsonNode.class);
                return new StorageCountResult(request.getTenantId(), request.getType(),
                    result.get("numberObjects").longValue());
            }
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                response.getStatusInfo().getReasonPhrase());
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
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
            response =
                performRequest(HttpMethod.GET,
                    OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                    getDefaultHeaders(request.getTenantId(), null, null, null),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    final StorageGetResult result =
                        new StorageGetResult(request.getTenantId(), request.getType(), request.getGuid(), response);
                    return result;
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                        "not found");
                case PRECONDITION_FAILED:
                    LOGGER.error("Precondition failed");
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Precondition failed");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e1) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e1);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e1.getMessage());
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
            final ObjectInit objectInit = new ObjectInit();
            objectInit.setDigestAlgorithm(DigestType.valueOf(request.getDigestAlgorithm()));
            objectInit.setType(DataCategory.getByFolder(request.getType()));
            response =
                performRequest(HttpMethod.POST, OBJECTS_PATH + "/" + objectInit.getType() + "/" + request.getGuid(),
                    getDefaultHeaders(request.getTenantId(), StorageConstants.COMMAND_INIT, null, null),
                    objectInit, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            return performPutRequests(stream, handleResponseStatus(response, ObjectInit.class),
                request.getTenantId());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED, exc
                .getMessage());
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
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
            response =
                performRequest(HttpMethod.DELETE,
                    OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                    getDefaultHeaders(request.getTenantId(), null,
                        request.getDigestHashBase16(), request.getDigestAlgorithm().getName()),
                    MediaType.APPLICATION_JSON_TYPE, false);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    final JsonNode json = handleResponseStatus(response, JsonNode.class);
                    final StorageRemoveResult result =
                        new StorageRemoveResult(request.getTenantId(), request.getType(), request.getGuid(),
                            request.getDigestAlgorithm(), request.getDigestHashBase16(),
                            Response.Status.OK.toString().equals(json.get("status").asText()));
                    return result;
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                        "not found");
                case BAD_REQUEST:
                    LOGGER.error("Bad request");
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD,
                    OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid(),
                    getDefaultHeaders(request.getTenantId(), null, null, null),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                case NO_CONTENT:
                    return true;
                case NOT_FOUND:
                    return false;
                case BAD_REQUEST:
                    LOGGER.error("Bad request");
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Bad request");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
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
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    status.getReasonPhrase());
            case NOT_FOUND:
                // FIXME P1 : clean useless case
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, status
                    .getReasonPhrase());
            case SERVICE_UNAVAILABLE:
                LOGGER.error(status.getReasonPhrase());
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
     * @param digest the digest of the object to be added
     * @param digestType the type of the digest to be added
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(Integer tenantId, String command, String digest,
        String digestType) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        if (tenantId != null) {
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        }
        if (command != null) {
            headers.add(GlobalDataRest.X_COMMAND, command);
        }
        if (digest != null) {
            headers.add(GlobalDataRest.X_DIGEST, digest);
        }
        if (digestType != null) {
            headers.add(GlobalDataRest.X_DIGEST_ALGORITHM, digestType);
        }
        return headers;
    }


    /**
     * Method performing a PutRequests
     *
     * @param stream the stream to be chunked if necessary
     * @param result the result received from the server after the init
     * @param tenantId the tenant id
     * @return a PutObjectResult the final result received from the server
     * @throws StorageDriverException in case the server encounters an exception
     */
    private StoragePutResult performPutRequests(InputStream stream, ObjectInit result,
        Integer tenantId)
        throws StorageDriverException {
        StoragePutResult finalResult = null;
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, OBJECTS_PATH + "/" + result.getType() + "/" + result.getId(),
                getDefaultHeaders(tenantId, StorageConstants.COMMAND_END, null, null),
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final JsonNode json = handleResponseStatus(response, JsonNode.class);
            finalResult = new StoragePutResult(tenantId, result.getType().getFolder(), result.getId(), result.getId(),
                json.get("digest").textValue(), Long.valueOf(json.get("size").textValue()));
            if (Response.Status.CREATED.getStatusCode() != response.getStatus()) {
                LOGGER.error("Error to perfom put object");
                throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error to perfom put object");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
        return finalResult;
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
                        getDefaultHeaders(request.getTenantId(), null,
                                request.getDigestHashBase16(), request.getDigestAlgorithm().getName()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK: case CONFLICT:
                    final StorageCheckResult result =
                        new StorageCheckResult(request.getTenantId(), request.getType(), request.getGuid(),
                            request.getDigestAlgorithm(), request.getDigestHashBase16(), status.equals(Status.OK));
                    return result;
                case NOT_FOUND:
                    LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                        "not found");
                case PRECONDITION_FAILED:
                    LOGGER.error("Precondition failed");
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                        "Precondition failed");
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e1) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e1);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                e1.getMessage());
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
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
          response = performRequest(HttpMethod.GET, OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid() 
                  + METADATAS, getDefaultHeaders(request.getTenantId(), null, null, null),
                  MediaType.APPLICATION_JSON_TYPE, false);
          final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
          switch (status) {
              case OK:
                  return handleResponseStatus(response, StorageMetadatasResult.class);
              case NOT_FOUND:
                  LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND), request.getGuid());
                  throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.NOT_FOUND, "Object " +
                      "not found");
              default:
                  LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                  throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                      INTERNAL_SERVER_ERROR);                                  
          }
      } catch (VitamClientInternalException e) {
          LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
          throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
              e.getMessage());
      } finally {
          consumeAnyEntityAndClose(response);
      }      
    }


    @Override
    public Response listObjects(StorageListRequest request) throws
        StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter("X-Cursor is mandatory", request.isxCursor());
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, request.getTenantId());
            headers.add(GlobalDataRest.X_CURSOR, request.isxCursor());
            if (request.getCursorId() != null) {
                headers.add(GlobalDataRest.X_CURSOR_ID, request.getCursorId());
            }
            return performRequest(HttpMethod.GET, OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()),
                headers, MediaType.APPLICATION_JSON_TYPE);
        } catch (Exception exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageDriverException(driverName, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR, exc);
        }
    }
}
