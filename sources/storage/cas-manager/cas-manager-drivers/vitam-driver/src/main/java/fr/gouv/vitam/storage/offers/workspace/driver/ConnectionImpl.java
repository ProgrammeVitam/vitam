/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.storage.offers.workspace.driver;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.model.storage.ObjectEntryReader;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverServiceUnavailableException;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageGetMetadataRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Collections;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

/**
 * Workspace Connection Implementation
 */
public class ConnectionImpl extends AbstractConnection {

    private static final String X_USABLE_SPACE = "X-Usable-Space";
    private static final long DEFAULT_MAX_AVAILABILITY = 100000000000L;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ConnectionImpl.class);

    private static final String OBJECTS_PATH = "/objects";
    private static final String READ_ORDER_PATH = "/readorder";
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
    private static final String EXPORT_ID_IS_A_MANDATORY_PARAMETER = "Export id is a mandatory parameter";
    private static final String BAD_REQUEST_ERROR_MESSAGE = "Bad request";

    /**
     * Constructor
     *
     * @param driverName
     * @param factory
     */
    ConnectionImpl(String driverName, VitamClientFactoryInterface<? extends AbstractConnection> factory) {
        super(driverName, factory);
    }

    private void checkStorageException(Response response) throws StorageDriverException {
        final Status status = fromStatusCode(response.getStatus());


        switch (status) {
            case CREATED:
            case OK:
                return;
            case INTERNAL_SERVER_ERROR:
                LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR));
                throw new StorageDriverException(getDriverName(), status.getReasonPhrase(), true);
            case NOT_FOUND:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverNotFoundException(getDriverName(), status.getReasonPhrase());
            case PRECONDITION_FAILED:
                LOGGER.error("Precondition failed");
                throw new StorageDriverPreconditionFailedException(getDriverName(), "Precondition failed");
            case BAD_REQUEST:
                LOGGER.error(BAD_REQUEST_ERROR_MESSAGE);
                throw new StorageDriverPreconditionFailedException(getDriverName(), BAD_REQUEST_ERROR_MESSAGE);
            case SERVICE_UNAVAILABLE:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverServiceUnavailableException(getDriverName(), status.getReasonPhrase());
            case CONFLICT:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverConflictException(getDriverName(), status.getReasonPhrase());
            default:
                LOGGER.error(status.getReasonPhrase());
                throw new StorageDriverException(getDriverName(), status.getReasonPhrase(), true);
        }
    }

    @Override
    public StorageCapacityResult getStorageCapacity(Integer tenantId)
        throws StorageDriverPreconditionFailedException, StorageDriverNotFoundException, StorageDriverException {
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenantId);

        VitamRequestBuilder request = head()
            .withPath(OBJECTS_PATH + "/" + DataCategory.OBJECT)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withJsonAccept();

        try (Response response = make(request)) {
            checkStorageException(response);
            return new StorageCapacityResult(tenantId, getAvailableSpace(response));
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR.getMessage(),
                true,
                e);
        }
    }

    private long getAvailableSpace(Response response) {
        if (response.getHeaderString(X_USABLE_SPACE) != null) {
            try {
                return Long.parseLong(response.getHeaderString(X_USABLE_SPACE));
            } catch (NumberFormatException e) {
                LOGGER.info("Not a number", e);
            }
        }
        return DEFAULT_MAX_AVAILABILITY;
    }

    @Override
    public StorageGetResult getObject(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));

        VitamRequestBuilder requestbuilder = get()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid())
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withOctetAccept();

        Response response = null;
        try {
            response = make(requestbuilder);
            checkStorageException(response);
            return new StorageGetResult(request.getTenantId(), request.getType(), request.getGuid(), response);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public RequestResponse<TapeReadRequestReferentialEntity> createReadOrderRequest(StorageObjectRequest request)
        throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));

        VitamRequestBuilder requestBuilder = post()
            .withPath(READ_ORDER_PATH + "/" + DataCategory.getByFolder(request.getType()))
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withBody(Collections.singletonList(request.getGuid()))
            .withJson();

        try (Response response = make(requestBuilder)) {
            checkStorageException(response);
            return RequestResponse.parseFromResponse(response, TapeReadRequestReferentialEntity.class);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }


    @Override
    public RequestResponse<TapeReadRequestReferentialEntity> getReadOrderRequest(String readOrderRequestId, int tenant)
        throws StorageDriverException {
        ParametersChecker.checkParameter(EXPORT_ID_IS_A_MANDATORY_PARAMETER, readOrderRequestId);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenant);

        VitamRequestBuilder request = get()
            .withPath(READ_ORDER_PATH + "/" + readOrderRequestId)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenant)
            .withJsonAccept();
        try (Response response = make(request)) {
            checkStorageException(response);
            return RequestResponse.parseFromResponse(response, TapeReadRequestReferentialEntity.class);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public void removeReadOrderRequest(String readOrderRequestId, int tenant) throws StorageDriverException {
        ParametersChecker.checkParameter(EXPORT_ID_IS_A_MANDATORY_PARAMETER, readOrderRequestId);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, tenant);

        VitamRequestBuilder request = delete()
            .withPath(READ_ORDER_PATH + "/" + readOrderRequestId)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenant)
            .withJsonAccept();

        try (Response response = make(request)) {
            checkStorageException(response);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public StoragePutResult putObject(StoragePutRequest request) throws StorageDriverException {
        try {
            ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
            ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
            ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestAlgorithm());
            ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
            ParametersChecker.checkParameter(TYPE_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
            ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, request.getDataStream());
        } catch (final IllegalArgumentException exc) {
            throw new StorageDriverPreconditionFailedException(getDriverName(), exc);
        }

        VitamRequestBuilder requestBuilder = put()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid())
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withHeader(GlobalDataRest.X_DIGEST_ALGORITHM, request.getDigestAlgorithm())
            .withHeader(GlobalDataRest.VITAM_CONTENT_LENGTH, request.getSize())
            .withBody(request.getDataStream())
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(requestBuilder)) {
            checkStorageException(response);
            final JsonNode json = response.readEntity(JsonNode.class);

            if (Status.CREATED.getStatusCode() != response.getStatus()) {
                throw new StorageDriverException(getDriverName(),
                    "Error while performing put object operation for object " + request.getGuid() + " (" +
                        request.getType() + ")", true);
            }

            return new StoragePutResult(request.getTenantId(), request.getType(), request.getGuid(), request.getGuid(),
                json.get("digest").textValue(), json.get("size").longValue());
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public StorageBulkPutResult bulkPutObjects(StorageBulkPutRequest request) throws StorageDriverException {
        try {
            ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getObjectIds());
            ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getObjectIds().toArray());
            ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
            ParametersChecker.checkParameter(ALGORITHM_IS_A_MANDATORY_PARAMETER, request.getDigestType());
            ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());
            ParametersChecker.checkParameter(TYPE_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));
            ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, request.getDataStream());
        } catch (final IllegalArgumentException e) {
            // bad idea but i'm not here to change that now...
            throw new StorageDriverPreconditionFailedException(getDriverName(), e);
        }

        VitamRequestBuilder requestBuilder = put()
            .withPath("/bulk/objects/" + DataCategory.getByFolder(request.getType()))
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withHeader(GlobalDataRest.X_DIGEST_ALGORITHM, request.getDigestType().getName())
            .withHeader(GlobalDataRest.VITAM_CONTENT_LENGTH, request.getSize())
            .withBody(request.getDataStream())
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(requestBuilder)) {
            checkStorageException(response);

            if (Status.CREATED.getStatusCode() != response.getStatus()) {
                throw new StorageDriverException(getDriverName(),
                    "Error while performing bulk put object operation for objects " + request.getObjectIds() + " (" +
                        request.getType() + ")", true);
            }

            return response.readEntity(StorageBulkPutResult.class);
        } catch (final VitamClientInternalException e) {
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public StorageRemoveResult removeObject(StorageRemoveRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(FOLDER_IS_NOT_VALID, DataCategory.getByFolder(request.getType()));

        VitamRequestBuilder requestBuilder = delete()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid())
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withJsonAccept();
        try (Response response = make(requestBuilder)) {
            checkStorageException(response);

            final JsonNode json = response.readEntity(JsonNode.class);
            return new StorageRemoveResult(request.getTenantId(), request.getType(),
                request.getGuid(), Status.OK.toString().equals(json.get("status").asText()));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());

        VitamRequestBuilder requestBuilder = head()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid())
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withJsonAccept();

        try (Response response = make(requestBuilder)) {
            final Status status = fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                case NO_CONTENT:
                    return true;
                case NOT_FOUND:
                    return false;
                case BAD_REQUEST:
                    LOGGER.error(BAD_REQUEST_ERROR_MESSAGE);
                    throw new StorageDriverPreconditionFailedException(getDriverName(), BAD_REQUEST_ERROR_MESSAGE);
                default:
                    LOGGER.error(status.getReasonPhrase());
                    throw new StorageDriverException(getDriverName(), status.getReasonPhrase(), true);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(), true, e);
        }
    }

    @Override
    public StorageMetadataResult getMetadatas(StorageGetMetadataRequest request) throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(FOLDER_IS_A_MANDATORY_PARAMETER, request.getType());
        ParametersChecker.checkParameter(GUID_IS_A_MANDATORY_PARAMETER, request.getGuid());

        VitamRequestBuilder requestBuilder = get()
            .withPath(
                OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()) + "/" + request.getGuid() + METADATAS)
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withHeader(GlobalDataRest.X_OFFER_NO_CACHE, request.isNoCache())
            .withJsonAccept();

        try (Response response = make(requestBuilder)) {
            checkStorageException(response);
            return response.readEntity(StorageMetadataResult.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), e);
            throw new StorageDriverException(getDriverName(),
                VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), true, e);
        }
    }

    @Override
    public CloseableIterator<ObjectEntry> listObjects(StorageListRequest request) throws StorageDriverException, StorageDriverNotFoundException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, request);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, request.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, request.getType());

        VitamRequestBuilder requestBuilder = get()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(request.getType()))
            .withHeader(GlobalDataRest.X_TENANT_ID, request.getTenantId())
            .withJsonAccept();

        try {
            Response response = make(requestBuilder);
            try {
                checkStorageException(response);
            } catch (Exception e) {
                StreamUtils.consumeAnyEntityAndClose(response);
                throw e;
            }
            InputStream rawResponseInputStream = response.readEntity(InputStream.class);
            return new ObjectEntryReader(rawResponseInputStream);
        } catch (VitamClientInternalException exc) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR), exc);
            throw new StorageDriverException(getDriverName(), true, exc);
        }
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(StorageOfferLogRequest storageGetOfferLogRequest)
        throws StorageDriverException {
        ParametersChecker.checkParameter(REQUEST_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest);
        ParametersChecker.checkParameter(TENANT_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getTenantId());
        ParametersChecker.checkParameter(TYPE_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getType());
        ParametersChecker.checkParameter(ORDER_IS_A_MANDATORY_PARAMETER, storageGetOfferLogRequest.getOrder());

        OfferLogRequest offerLogRequest = new OfferLogRequest();
        offerLogRequest.setOffset(storageGetOfferLogRequest.getOffset());
        offerLogRequest.setLimit(storageGetOfferLogRequest.getLimit());
        offerLogRequest.setOrder(storageGetOfferLogRequest.getOrder());

        VitamRequestBuilder requestBuilder = get()
            .withPath(OBJECTS_PATH + "/" + DataCategory.getByFolder(storageGetOfferLogRequest.getType()) + LOGS_PATH)
            .withHeader(GlobalDataRest.X_TENANT_ID, storageGetOfferLogRequest.getTenantId())
            .withBody(offerLogRequest)
            .withJson();

        try (Response response = make(requestBuilder)) {
            return RequestResponse.parseFromResponse(response, OfferLog.class);
        } catch (Exception exc) {
            throw new StorageDriverException(getDriverName(), true, exc);
        }
    }
}
