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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.model.storage.ObjectEntryReader;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.apache.commons.lang.BooleanUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

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
    private static final String STRATEGY_ID_MUST_HAVE_A_VALID_VALUE = "Strategy id must have a valid value";
    private static final String TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE =
        "Type of storage object must have a valid value";
    private static final String ORDER_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE =
        "Order of storage object must have a valid value";
    private static final String STORAGE_ACCESSLOG_BACKUP_URI = "/storage/backup/accesslog";
    private static final String STORAGE_LOG_BACKUP_URI = "/storage/backup";
    private static final String STORAGE_LOG_TRACEABILITY_URI = "/storage/traceability";
    private static final String STRATEGIES_URI = "/strategies";
    private static final String COPY = "/copy/";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientRest.class);
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error:";

    StorageClientRest(StorageClientFactory factory) {
        super(factory);
    }

    @Override
    public JsonNode getStorageInformation(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();

        VitamRequestBuilder request = get()
            .withPath("/")
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withJsonAccept();
        try (Response response = make(request)) {
            return handleCommonResponseStatus(response).readEntity(JsonNode.class);
        } catch (final VitamClientInternalException | StorageAlreadyExistsClientException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public List<String> getOffers(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();

        VitamRequestBuilder request = get()
            .withPath("/offers")
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return handleCommonResponseStatus(response).readEntity(ArrayList.class);
        } catch (final VitamClientInternalException | StorageAlreadyExistsClientException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public StoredInfoResult storeFileFromWorkspace(String strategyId, DataCategory type, String guid,
        ObjectDescription description)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        ParametersChecker.checkParameter(OBJECT_DESCRIPTION_MUST_HAVE_A_VALID_VALUE, description);
        if (description != null) {
            ParametersChecker.checkParameter(OBJECT_DESCRIPTION_GUID_MUST_HAVE_A_VALID_VALUE,
                description.getWorkspaceContainerGUID());
            ParametersChecker.checkParameter(OBJECT_DESCRIPTION_URI_MUST_HAVE_A_VALID_VALUE,
                description.getWorkspaceObjectURI());
        }
        VitamRequestBuilder request = post()
            .withPath("/" + type.getCollectionName() + "/" + guid)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withBody(description)
            .withJson();
        try (Response response = make(request)) {
            return handleCommonResponseStatus(response).readEntity(StoredInfoResult.class);
        } catch (final VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public BulkObjectStoreResponse bulkStoreFilesFromWorkspace(String strategyId,
        BulkObjectStoreRequest bulkObjectStoreRequest)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {

        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter("Expected valid request", bulkObjectStoreRequest);
        ParametersChecker.checkParameter("Invalid digest type", bulkObjectStoreRequest.getType());
        ParametersChecker.checkParameter("Invalid workspace URIs", bulkObjectStoreRequest.getWorkspaceObjectURIs());
        ParametersChecker.checkParameter("Invalid workspace URIs",
            bulkObjectStoreRequest.getWorkspaceObjectURIs().toArray());
        ParametersChecker.checkParameter("Invalid object ids", bulkObjectStoreRequest.getObjectNames());
        ParametersChecker.checkParameter("Invalid object ids", bulkObjectStoreRequest.getObjectNames().toArray());
        if (bulkObjectStoreRequest.getObjectNames().size() != bulkObjectStoreRequest.getWorkspaceObjectURIs().size()) {
            throw new IllegalArgumentException("Object uris count do not match object ids count");
        }

        VitamRequestBuilder request = post()
            .withPath("/bulk/" + bulkObjectStoreRequest.getType().getCollectionName())
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withBody(bulkObjectStoreRequest)
            .withJson();
        try (Response response = make(request)) {
            return handleCommonResponseStatus(response).readEntity(BulkObjectStoreResponse.class);
        } catch (final VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public boolean existsContainer(String strategyId) throws StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();

        VitamRequestBuilder request = head()
            .withPath("/")
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withJsonAccept();

        try (Response response = make(request)) {
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (final VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public Map<String, Boolean> exists(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(STRATEGY_ID_MUST_HAVE_A_VALID_VALUE, strategyId);
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);

        VitamRequestBuilder request = head()
            .withPath("/" + type.name() + "/" + guid)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withJsonAccept();
        for (String offerId : offerIds) {
            request.withHeader(GlobalDataRest.X_OFFER_IDS, offerId);
        }

        try (Response response = make(request)) {
            notContentResponseToBoolean(handleNoContentResponseStatus(response));
            Map<String, Boolean> result = new HashMap<>();
            for (String offerId : offerIds) {
                result.put(offerId,
                    response.getHeaders().containsKey(offerId) && response.getHeaders().get(offerId) != null
                        ? BooleanUtils.toBoolean(response.getHeaderString(offerId))
                        : Boolean.FALSE);
            }
            return result;
        } catch (final VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public JsonNode getInformation(String strategyId, DataCategory type, String guid, List<String> offerIds,
        boolean noCache)
        throws StorageServerClientException, StorageNotFoundClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);

        VitamRequestBuilder request = get()
            .withPath("/info/" + type.getCollectionName() + "/" + guid)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withHeader(GlobalDataRest.X_OFFER_NO_CACHE, Boolean.toString(noCache))
            .withJsonAccept();

        for (String offerId : offerIds) {
            request.withHeader(GlobalDataRest.X_OFFER_IDS, offerId);
        }

        try (Response response = make(request)) {
            check(response);
            return handleCommonResponseStatus(response).readEntity(JsonNode.class);
        } catch (VitamClientInternalException | StorageAlreadyExistsClientException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public RequestResponse<BatchObjectInformationResponse> getBatchObjectInformation(String strategyId,
        DataCategory type, Collection<String> offerIds,
        Collection<String> objectIds)
        throws StorageServerClientException {

        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, objectIds);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, objectIds.toArray());

        VitamRequestBuilder request = get()
            .withPath("/batch_info/" + type.getCollectionName())
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withBody(objectIds)
            .withJson();

        for (String offerId : offerIds) {
            request.withHeader(GlobalDataRest.X_OFFER_IDS, offerId);
        }

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, BatchObjectInformationResponse.class);
        } catch (VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public boolean delete(String strategyId, DataCategory type, String guid)
        throws StorageServerClientException {
        return delete(strategyId, type, guid, new ArrayList<>());
    }


    @Override
    public boolean delete(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);


        VitamRequestBuilder request = VitamRequestBuilder.delete()
            .withPath("/delete/" + guid)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withHeader(GlobalDataRest.X_DATA_CATEGORY, type.name())
            .withJsonAccept();
        if (offerIds != null && !offerIds.isEmpty()) {
            offerIds.forEach(id -> request.withHeader(GlobalDataRest.X_OFFER_IDS, id));
        }
        try (Response response = make(request)) {
            return notContentResponseToBoolean(handleNoContentResponseStatus(response));
        } catch (final VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
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

    private Response.Status handleNoContentResponseStatus(Response response) throws StorageServerClientException {
        ParametersChecker.checkParameter("Response", response);
        final Response.Status status = fromStatusCode(response.getStatus());
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

    private Response handleCommonResponseStatus(Response response)
        throws StorageNotFoundClientException, StorageServerClientException, StorageAlreadyExistsClientException {
        final Response.Status status = fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
            case CREATED:
                return response;
            case CONFLICT:
                throw new StorageAlreadyExistsClientException(
                    VitamCodeHelper.getCode(VitamCode.STORAGE_CLIENT_ALREADY_EXISTS) + " : " +
                        status.getReasonPhrase());
            case NOT_FOUND:
                throw new StorageNotFoundClientException(VitamCodeHelper.getCode(VitamCode.STORAGE_NOT_FOUND) + " : " +
                    status.getReasonPhrase());
            case PRECONDITION_FAILED:
                throw new StorageServerClientException(VitamCodeHelper.getCode(VitamCode.STORAGE_BAD_REQUEST) + " : " +
                    status.getReasonPhrase());
            default:
                final String log = VitamCodeHelper.getCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR) + " : " +
                    status.getReasonPhrase();
                LOGGER.error(log);
                throw new StorageServerClientException(log);
        }
    }

    @Override
    public Response getContainerAsync(String strategyId, String guid, DataCategory type, AccessLogInfoModel logInfo)
        throws StorageServerClientException, StorageNotFoundException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(GUID_MUST_HAVE_A_VALID_VALUE, guid);
        VitamRequestBuilder request = get()
            .withPath(type.getCollectionName() + "/" + guid)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withBody(logInfo)
            .withContentType(MediaType.APPLICATION_JSON_TYPE)
            .withAccept(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        try {
            Response response = make(request);
            return handleCommonResponseStatus(response);
        } catch (final VitamClientInternalException | StorageAlreadyExistsClientException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        } catch (StorageNotFoundClientException e) {
            throw new StorageNotFoundException(e);
        }
    }

    @Override
    public CloseableIterator<ObjectEntry> listContainer(String strategyId, DataCategory type)
        throws StorageServerClientException {
        ParametersChecker.checkParameter("Type cannot be null", type);
        VitamRequestBuilder request = VitamRequestBuilder.get()
            .withPath("/" + type.name())
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter());

        try {
            Response response = make(request);

            try {
                check(response);

                return new ObjectEntryReader(response.readEntity(InputStream.class));

            } catch (Exception e) {
                StreamUtils.consumeAnyEntityAndClose(response);
                throw e;
            }

        } catch (final VitamClientInternalException e) {
            throw new StorageServerClientException(INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public RequestResponseOK storageAccessLogBackup()
        throws StorageServerClientException, InvalidParseOperationException {
        VitamRequestBuilder request = post()
            .withPath(STORAGE_ACCESSLOG_BACKUP_URI)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new StorageServerClientException(INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public RequestResponseOK storageLogBackup()
        throws StorageServerClientException, InvalidParseOperationException {
        VitamRequestBuilder request = post()
            .withPath(STORAGE_LOG_BACKUP_URI)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new StorageServerClientException(INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public RequestResponseOK storageLogTraceability()
        throws StorageServerClientException, InvalidParseOperationException {

        VitamRequestBuilder request = post()
            .withPath(STORAGE_LOG_TRACEABILITY_URI)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withAccept(MediaType.APPLICATION_JSON_TYPE);
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new StorageServerClientException(INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public RequestResponseOK copyObjectToOneOfferAnother(String objectId, DataCategory category, String source,
        String destination, String strategyId)
        throws StorageServerClientException, InvalidParseOperationException {

        VitamRequestBuilder request = post()
            .withPath(COPY + objectId)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withHeader(GlobalDataRest.X_CONTENT_DESTINATION, destination)
            .withHeader(GlobalDataRest.X_CONTENT_SOURCE, source)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withHeader(GlobalDataRest.X_STRATEGY_ID, VitamConfiguration.getDefaultStrategy())
            .withHeader(GlobalDataRest.X_DATA_CATEGORY, category.name())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new StorageServerClientException(INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public RequestResponseOK create(String strategyId, String objectId, DataCategory category, InputStream inputStream,
        Long inputStreamSize,
        List<String> offerIds)
        throws StorageServerClientException, InvalidParseOperationException {

        VitamRequestBuilder request = post()
            .withPath("/create/" + objectId)
            .withHeader(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter())
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withHeader(GlobalDataRest.X_CONTENT_LENGTH, inputStreamSize)
            .withHeader(GlobalDataRest.X_DATA_CATEGORY, category.name())
            .withBody(inputStream)
            .withJsonAccept()
            .withOctetContentType();
        for (String offerId : offerIds) {
            request.withHeader(GlobalDataRest.X_OFFER_IDS, offerId);
        }
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new StorageServerClientException("Internal Server Error", e);
        }
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(String strategyId, DataCategory type, Long offset, int limit,
        Order order)
        throws StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        ParametersChecker.checkParameter(TYPE_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, type);
        ParametersChecker.checkParameter(ORDER_OF_STORAGE_OBJECT_MUST_HAVE_A_VALID_VALUE, order);

        VitamRequestBuilder request = get()
            .withPath("/" + type.name() + "/logs")
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withHeader(GlobalDataRest.X_STRATEGY_ID, strategyId)
            .withBody(new OfferLogRequest(offset, limit, order))
            .withJson();
        try (Response response = make(request)) {
            return RequestResponse.parseFromResponse(response, OfferLog.class);
        } catch (VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    @Override
    public RequestResponse<StorageStrategy> getStorageStrategies() throws StorageServerClientException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamRequestBuilder request = get()
            .withPath(STRATEGIES_URI)
            .withHeader(GlobalDataRest.X_TENANT_ID, tenantId)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, StorageStrategy.class);
        } catch (VitamClientInternalException e) {
            final String errorMessage =
                VitamCodeHelper.getMessageFromVitamCode(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            LOGGER.error(errorMessage, e);
            throw new StorageServerClientException(errorMessage, e);
        }
    }

    private void check(Response response) throws VitamClientInternalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientInternalException(
            String.format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }
}
