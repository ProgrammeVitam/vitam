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
package fr.gouv.vitam.driver.mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.driver.mock.MockDriverImpl.MockClientFactory;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServiceUnavailableException;
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
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferServiceImpl;

/**
 * Workspace Connection Implementation
 */
public class ConnectionMockImpl extends AbstractMockClient implements Connection {

    private static final long DEFAULT_MAX_AVAILABILITY = 100000000000L;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionMockImpl.class);

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

    @SuppressWarnings("unused")
    private final Properties parameters;
    private final String driverName;
    private final String offerName;
    private static final Map<String, OfferMaps> GLOBAL_MAPS = new ConcurrentHashMap<>();
    
    private static class OfferMaps {
        final Map<String, Long> FILES_MAP = new ConcurrentHashMap<>();
        final Map<String, String> FOLDERS_MAP = new ConcurrentHashMap<>();
        final Map<String, String> CONTAINER_MAP = new ConcurrentHashMap<>();
    }

    /**
     * Constructor
     *
     * @param driverName
     * @param factory
     * @param parameters
     */
    public ConnectionMockImpl(String driverName, MockClientFactory factory,
        Properties parameters) {
        if (null == driverName) {
            throw new IllegalArgumentException("The parameter driverName is requied");
        }
        this.driverName = driverName;
        this.parameters = parameters;
        offerName = (String) factory.parameters.get("id");
        if (! GLOBAL_MAPS.containsKey(offerName)) {
            OfferMaps offerMaps = new OfferMaps();
            GLOBAL_MAPS.put(offerName, offerMaps);
        }
    }

    /**
     * @return the driverName associated
     */
    public String getDriverName() {
        return driverName;
    }

    private String getName(String containerName, String folderName, String objectName) {
        String key = containerName;
        if (folderName != null) {
            key += "/" + folderName;
        }
        if (objectName != null) {
            key += "/" + objectName;
        }
        return key;
    }

    @Override
    public StorageCapacityResult getStorageCapacity(Integer tenantId)
        throws StorageDriverPreconditionFailedException, StorageDriverNotFoundException, StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        if (offerMaps.CONTAINER_MAP.containsKey(tenantId.toString())) {
            long available = DEFAULT_MAX_AVAILABILITY;
            long usedSize = 0;
            for (Entry<String, Long> entry : offerMaps.FILES_MAP.entrySet()) {
                if (entry.getKey().startsWith(tenantId.toString())) {
                    usedSize += entry.getValue();
                }
            }
            return new StorageCapacityResult(tenantId, available, usedSize);
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
            tenantId + "_" + DataCategory.OBJECT));
        throw new StorageDriverNotFoundException(getDriverName(),
            VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
                tenantId + "_" + DataCategory.OBJECT));
    }

    @Override
    public StorageCountResult countObjects(StorageRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);

        if (offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString())) {
            long count = 0;
            String key = getName(request.getTenantId().toString(),
                DataCategory.getByFolder(request.getType()).getFolder(), null);
            for (String item : offerMaps.FILES_MAP.keySet()) {
                if (item.startsWith(key)) {
                    count++;
                }
            }
            return new StorageCountResult(request.getTenantId(), request.getType(),
                count);
        }
        LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
            request.getTenantId() + "_" + request.getType()));
        throw new StorageDriverNotFoundException(getDriverName(),
            VitamCodeHelper.getLogMessage(VitamCode.STORAGE_CONTAINER_NOT_FOUND,
                request.getTenantId() + "_" + request.getType()));
    }

    @Override
    public StorageGetResult getObject(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(),
            request.getGuid());
        if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString())) {
            throw new StorageDriverPreconditionFailedException(getDriverName(), TENANT_IS_A_MANDATORY_PARAMETER);
        } else if (!offerMaps.FILES_MAP.containsKey(key)) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
            throw new StorageDriverNotFoundException(getDriverName(),
                "Object " + request.getGuid() + " not found");
        }
        long length = offerMaps.FILES_MAP.get(key);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(VitamHttpHeader.X_CONTENT_LENGTH.getName(), length);
        return new StorageGetResult(request.getTenantId(), request.getType(),
            request.getGuid(), new FakeInboundResponse(Status.OK,
                new FakeInputStream(length),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, headers));
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
            OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
            final InputStream stream = request.getDataStream();
            String xTenantId = request.getTenantId().toString();

            if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString())) {
                offerMaps.CONTAINER_MAP.put(xTenantId, xTenantId);
            }
            final String containerName = buildContainerName(DataCategory.getByFolder(request.getType()),
                xTenantId);
            if (!offerMaps.FOLDERS_MAP.containsKey(containerName)) {
                offerMaps.FOLDERS_MAP.put(containerName, containerName);
            }
            final ObjectInit objectInit = new ObjectInit();
            objectInit.setDigestAlgorithm(DigestType.valueOf(request.getDigestAlgorithm()));
            objectInit.setType(DataCategory.getByFolder(request.getType()));
            objectInit.setId(request.getGuid());

            response = new FakeInboundResponse(Status.OK,
                objectInit,
                MediaType.APPLICATION_JSON_TYPE, null);
            return performPutRequests(stream, handleResponseStatus(response, ObjectInit.class), request.getTenantId());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            throw new StorageDriverPreconditionFailedException(getDriverName(), exc.getMessage());
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
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(),
            request.getGuid());
        if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString()) ||
            !offerMaps.FILES_MAP.containsKey(key)) {
            throw new StorageDriverNotFoundException(getDriverName(), "Object " + request.getGuid() +
                "not found");
        }
        offerMaps.FILES_MAP.remove(key);
        Response response;
        try {
            response = new FakeInboundResponse(Status.OK,
                JsonHandler.getFromString(
                    "{\"id\":\"" + request.getGuid() + "\",\"status\":\"" + Response.Status.OK.toString() + "\"}"),
                MediaType.APPLICATION_JSON_TYPE, null);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Bad request", e);
            throw new StorageDriverPreconditionFailedException(getDriverName(), "Bad request", e);
        }
        final JsonNode json = handleResponseStatus(response, JsonNode.class);
        return new StorageRemoveResult(request.getTenantId(), request.getType(),
            request.getGuid(), request.getDigestAlgorithm(), request.getDigestHashBase16(),
            Response.Status.OK.toString().equals(json.get("status").asText()));
    }

    @Override
    public boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(),
            request.getGuid());
        if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString()) ||
            !offerMaps.FILES_MAP.containsKey(key)) {
            return false;
        }
        return true;
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
    protected <R> R handleResponseStatus(Response response, Class<R> responseType) throws StorageDriverException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case CREATED:
            case OK:
                return response.readEntity(responseType);
            case INTERNAL_SERVER_ERROR:
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageDriverException(getDriverName(), status.getReasonPhrase());
            case NOT_FOUND:
                // FIXME P1 : clean useless case
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverNotFoundException(getDriverName(), status.getReasonPhrase());
            case SERVICE_UNAVAILABLE:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverServiceUnavailableException(getDriverName(), status.getReasonPhrase());
            case CONFLICT:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverException(getDriverName(),
                    status.getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new StorageDriverException(getDriverName(),
                    INTERNAL_SERVER_ERROR);
        }
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
    private StoragePutResult performPutRequests(InputStream stream, ObjectInit result, Integer tenantId)
        throws StorageDriverException {
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(tenantId.toString(), result.getType().getFolder(), result.getId());
        long length = StreamUtils.closeSilently(stream);
        offerMaps.FILES_MAP.put(key, length);
        StoragePutResult finalResult = null;
        Digest digest = new Digest(DigestType.SHA512);
        digest.update("test");
        finalResult = new StoragePutResult(tenantId, result.getType().getFolder(), result.getId(), result.getId(),
            digest.digestHex(), length);
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
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(),
            request.getGuid());
        if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString()) ||
            !offerMaps.FILES_MAP.containsKey(key)) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
            throw new StorageDriverException(getDriverName(),
                "Object " + "not found");
        }
        return new StorageCheckResult(request.getTenantId(), request.getType(),
            request.getGuid(), request.getDigestAlgorithm(), request.getDigestHashBase16(),
            true);
    }

    @Override
    public StorageMetadatasResult getMetadatas(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        String key = getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(),
            request.getGuid());
        if (!offerMaps.CONTAINER_MAP.containsKey(request.getTenantId().toString()) ||
            !offerMaps.FILES_MAP.containsKey(key)) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_OBJECT_NOT_FOUND, request.getGuid()));
            throw new StorageDriverException(getDriverName(),
                "Object " + "not found");
        }
        MetadatasStorageObject result = new MetadatasStorageObject();
        result.setObjectName(request.getGuid());
        Digest digest = new Digest(DigestType.SHA512);
        digest.update("test");
        result.setDigest(digest.digestHex());
        result.setFileSize(offerMaps.FILES_MAP.get(key));
        result.setType(DataCategory.getByFolder(request.getType()).getFolder());
        result.setFileOwner("Vitam_" + request.getTenantId().toString());
        result.setLastAccessDate(new Date().toString());
        result.setLastModifiedDate(new Date().toString());
        return new StorageMetadatasResult(result);
    }

    @Override
    public RequestResponse<JsonNode> listObjects(StorageListRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter("X-Cursor is mandatory", request.isxCursor());
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);

        Response response = null;
        boolean xcursor = request.isxCursor();
        String xcursorId = request.getCursorId();
        String prefix =
            getName(request.getTenantId().toString(), DataCategory.getByFolder(request.getType()).getFolder(), null);
        if (VitamRequestIterator.isEndOfCursor(xcursor, xcursorId)) {
            DefaultOfferServiceImpl.getInstance().finalizeCursor(prefix, xcursorId);
            final Response.ResponseBuilder builder = Response.status(Status.NO_CONTENT);
            response = VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            return RequestResponse.<JsonNode>parseFromResponse(response);
        }
        if (VitamRequestIterator.isNewCursor(xcursor, xcursorId)) {
            xcursorId = "cursorId";
        }
        final RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<JsonNode>();
        List<JsonNode> list = new ArrayList<>();
        for (String filename : offerMaps.FILES_MAP.keySet()) {
            if (filename.startsWith(prefix)) {
                list.add(JsonHandler.createObjectNode().put("objectId", filename.replace(prefix, "").substring(1)));
            }
        }
        return responseOK.addAllResults(list);
    }

    public void deleteAll() {
        OfferMaps offerMaps = GLOBAL_MAPS.get(offerName);
        offerMaps.FILES_MAP.clear();
        offerMaps.FOLDERS_MAP.clear();
        offerMaps.CONTAINER_MAP.clear();
    }


    private String buildContainerName(DataCategory type, String tenantId) {
        if (type == null || Strings.isNullOrEmpty(type.getFolder()) || Strings.isNullOrEmpty(tenantId)) {
            return null;
        }
        return tenantId + "_" + type.getFolder();
    }

}
