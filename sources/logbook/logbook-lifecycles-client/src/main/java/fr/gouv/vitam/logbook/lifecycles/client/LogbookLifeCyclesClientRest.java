/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
package fr.gouv.vitam.logbook.lifecycles.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.PreconditionFailedClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.JsonLineIterator;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.model.RawLifecycleByLastPersistedDateRequest;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static fr.gouv.vitam.common.GlobalDataRest.X_EVENT_STATUS;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.logbook.common.client.ErrorMessage.LOGBOOK_NOT_FOUND;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

class LogbookLifeCyclesClientRest extends DefaultClient implements LogbookLifeCyclesClient {

    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";

    private static final String OPERATIONS_URL = "/operations";
    private static final String UNIT_LIFECYCLES_URL = "/unitlifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_URL = "/objectgrouplifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BULK_URL = "/raw/objectgrouplifecycles/bulk";
    private static final String UNIT_LIFECYCLES_RAW_BULK_URL = "/raw/unitlifecycles/bulk";
    private static final String UNIT_LIFECYCLES_RAW_BY_ID_URL = "/raw/unitlifecycles/byid/";
    private static final String UNIT_LIFECYCLES_RAW_BY_IDS_URL = "/raw/unitlifecycles/byids";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL = "/raw/objectgrouplifecycles/byid/";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_IDS_URL = "/raw/objectgrouplifecycles/byids";
    private static final String UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL =
        "/raw/unitlifecycles/bylastpersisteddate";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL =
        "/raw/objectgrouplifecycles/bylastpersisteddate";

    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();

    LogbookLifeCyclesClientRest(LogbookLifeCyclesClientFactory factory) {
        super(factory);
    }

    @Override
    public void create(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);

        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());

        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        try (
            Response response = make(
                post().withPath(getServiceUrl(parameters, eip, oid)).withJson().withBody(parameters)
            )
        ) {
            check(response);
        } catch (VitamClientInternalException | LogbookClientNotFoundException | PreconditionFailedClientException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        update(parameters, null);
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        if (parameters.getParameterValue(LogbookParameterName.eventDateTime) == null) {
            parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        }
        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        String lid = parameters.getParameterValue(LogbookParameterName.lifeCycleIdentifier);

        VitamRequestBuilder request = put()
            .withPath(getServiceUrl(parameters, eip, (lid != null) ? lid : oid))
            .withBody(parameters)
            .withJson()
            .withHeaderIgnoreNull(X_EVENT_STATUS, lifeCycleStatusCode);

        try (Response response = make(request)) {
            check(response);
        } catch (
            VitamClientInternalException | LogbookClientAlreadyExistsException | PreconditionFailedClientException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());

        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        try (
            Response response = make(put().withPath(getServiceUrl(parameters, eip, oid) + "/commit").withJsonAccept())
        ) {
            check(response);
        } catch (
            LogbookClientAlreadyExistsException | VitamClientInternalException | PreconditionFailedClientException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier, SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());

        ParametersChecker.checkNullOrEmptyParameters(
            parameters.getMapParameters(),
            parameters.getMandatoriesParameters()
        );
        String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);

        try (Response response = make(delete().withPath(getServiceUrl(parameters, eip, oid)).withJsonAccept())) {
            check(response);
        } catch (
            LogbookClientAlreadyExistsException | VitamClientInternalException | PreconditionFailedClientException e
        ) {
            throw new LogbookClientNotFoundException(LOGBOOK_NOT_FOUND.getMessage(), e);
        }
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return selectUnitLifeCycleById(id, queryDsl, null);
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException {
        VitamRequestBuilder request = get()
            .withPath(UNIT_LIFECYCLES_URL + "/" + id)
            .withJson()
            .withBody(queryDsl)
            .withHeaderIgnoreNull(X_EVENT_STATUS, lifeCycleStatus);
        try (Response response = make(request)) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(REQUEST_PRECONDITION_FAILED, e);
        }
    }

    @Override
    public JsonNode selectUnitLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        try (
            Response response = make(
                get().withPath(UNIT_LIFECYCLES_URL).withJson().withBody(queryDsl, "QueryDSL cannot be null.")
            )
        ) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(REQUEST_PRECONDITION_FAILED, e);
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public InputStream exportRawUnitLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException {
        return exportRawLifecyclesByLastPersistedDate(
            UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL,
            startDate,
            endDate,
            maxEntries
        );
    }

    @Override
    public InputStream exportRawObjectGroupLifecyclesByLastPersistedDate(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException {
        return exportRawLifecyclesByLastPersistedDate(
            OBJECT_GROUP_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL,
            startDate,
            endDate,
            maxEntries
        );
    }

    private InputStream exportRawLifecyclesByLastPersistedDate(
        String uri,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int limit
    ) throws LogbookClientException, InvalidParseOperationException, IOException {
        RawLifecycleByLastPersistedDateRequest request = new RawLifecycleByLastPersistedDateRequest(
            LocalDateUtil.getFormattedDateTimeForMongo(startDate),
            LocalDateUtil.getFormattedDateTimeForMongo(endDate),
            limit
        );
        Response response = null;
        try {
            response = make(
                post()
                    .withPath(uri)
                    .withOctetAccept()
                    .withJsonContentType()
                    .withBody(JsonHandler.toJsonNode(request), "Request cannot be null.")
            );
            check(response);
            String contentLengthHeader = response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName());
            if (contentLengthHeader == null) {
                throw new LogbookClientException("Missing " + VitamHttpHeader.X_CONTENT_LENGTH.getName() + " header");
            }
            long contentLength = Long.parseLong(contentLengthHeader);
            return new ExactSizeInputStream(response.readEntity(InputStream.class), contentLength);
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(e);
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return selectObjectGroupLifeCycleById(id, queryDsl, null);
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException {
        VitamRequestBuilder request = get()
            .withJson()
            .withPath(OBJECT_GROUP_LIFECYCLES_URL + "/" + id)
            .withBody(queryDsl)
            .withHeaderIgnoreNull(X_EVENT_STATUS, lifeCycleStatus);
        try (Response response = make(request)) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(REQUEST_PRECONDITION_FAILED, e);
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        try (Response response = make(get().withPath(OBJECT_GROUP_LIFECYCLES_URL).withBody(queryDsl).withJson())) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(REQUEST_PRECONDITION_FAILED, e);
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException {
        try (
            Response response = make(
                get()
                    .withPath(OBJECT_GROUP_LIFECYCLES_URL)
                    .withBody(queryDsl)
                    .withHeaderIgnoreNull(X_EVENT_STATUS, lifeCycleStatus)
                    .withJson()
            )
        ) {
            check(response);
            return JsonHandler.getFromInputStream(response.readEntity(InputStream.class));
        } catch (PreconditionFailedClientException e) {
            throw new LogbookClientException(REQUEST_PRECONDITION_FAILED, e);
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public CloseableIterator<JsonNode> objectGroupLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) throws LogbookClientException {
        return getLifeCyclesByOperationIterator(operationId, lifeCycleStatus, query, OBJECT_GROUP_LIFECYCLES_URL);
    }

    @Override
    public CloseableIterator<JsonNode> unitLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query
    ) throws LogbookClientException {
        return getLifeCyclesByOperationIterator(operationId, lifeCycleStatus, query, UNIT_LIFECYCLES_URL);
    }

    private CloseableIterator<JsonNode> getLifeCyclesByOperationIterator(
        String operationId,
        LifeCycleStatusCode lifeCycleStatus,
        JsonNode query,
        String operationTypeUrlSuffix
    )
        throws LogbookClientAlreadyExistsException, LogbookClientServerException, LogbookClientBadRequestException, LogbookClientNotFoundException {
        VitamRequestBuilder request = get()
            .withPath(OPERATIONS_URL + "/" + operationId + operationTypeUrlSuffix)
            .withJsonContentType()
            .withOctetAccept()
            .withBody(query)
            .withHeader(X_EVENT_STATUS, lifeCycleStatus);
        try (Response response = make(request)) {
            check(response);
            return JsonLineIterator.parseFromResponse(response, JsonNode.class);
        } catch (PreconditionFailedClientException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void bulkCreateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        bulkCreate(eventIdProc, queue, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void bulkCreateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        bulkCreate(eventIdProc, queue, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void bulkCreate(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue, String uri)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        if (queue == null) {
            throw new LogbookClientBadRequestException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
        try (
            Response response = make(
                post()
                    .withPath(OPERATIONS_URL + "/" + eventIdProc + uri)
                    .withBody(queue, "Queue must be not null.")
                    .withJson()
            )
        ) {
            check(response);
        } catch (LogbookClientNotFoundException | PreconditionFailedClientException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void bulkUpdateUnit(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void bulkUpdateObjectGroup(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void bulkUpdate(String eventIdProc, Iterable<? extends LogbookLifeCycleParameters> queue, String uri)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        if (queue == null) {
            throw new LogbookClientBadRequestException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
        try (
            Response response = make(
                put().withPath(OPERATIONS_URL + "/" + eventIdProc + uri).withBody(queue).withJson()
            )
        ) {
            check(response);
        } catch (
            PreconditionFailedClientException | VitamClientInternalException | LogbookClientAlreadyExistsException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void commitUnit(String operationId, String unitId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        commitLifeCycle(operationId, unitId, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void commitObjectGroup(String operationId, String objectGroupId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        commitLifeCycle(operationId, objectGroupId, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void commitLifeCycle(String operationId, String idLc, String uri)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        String commitPath = OPERATIONS_URL + "/" + operationId + uri + "/" + idLc;
        VitamRequestBuilder request = put()
            .withPath(commitPath)
            .withJson()
            .withBody(JsonHandler.createObjectNode())
            .withHeader(X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED.toString());
        // BIG HACK because we use the same method to update and commit the collection
        // BIG HACK: I use an empty JSON to by pass the rest easy check
        // The best way is probably to have two different resource but in the past, we have an another resource
        // .../commit
        // and we migrate with only one resource because I don't know : discuss with an architect
        try (Response response = make(request)) {
            check(response);
        } catch (
            PreconditionFailedClientException | LogbookClientAlreadyExistsException | VitamClientInternalException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void rollBackUnitsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        rollBackOperationObjects(operationId, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void rollBackObjectGroupsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        rollBackOperationObjects(operationId, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void rollBackOperationObjects(String operationId, String uri)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        String rollBackPath = OPERATIONS_URL + "/" + operationId + uri;
        try (Response response = make(delete().withPath(rollBackPath).withJsonAccept())) {
            check(response);
        } catch (
            PreconditionFailedClientException | VitamClientInternalException | LogbookClientAlreadyExistsException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public LifeCycleStatusCode getUnitLifeCycleStatus(String unitId)
        throws LogbookClientNotFoundException, LogbookClientServerException {
        try (Response response = make(head().withPath(UNIT_LIFECYCLES_URL + "/" + unitId).withJsonAccept())) {
            check(response);
            if (response.getHeaderString(X_EVENT_STATUS) != null) {
                return LifeCycleStatusCode.valueOf(response.getHeaderString(X_EVENT_STATUS));
            }
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        } catch (
            PreconditionFailedClientException
            | VitamClientInternalException
            | LogbookClientBadRequestException
            | LogbookClientAlreadyExistsException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId)
        throws LogbookClientNotFoundException, LogbookClientServerException {
        try (
            Response response = make(
                head().withPath(OBJECT_GROUP_LIFECYCLES_URL + "/" + objectGroupId).withJsonAccept()
            )
        ) {
            check(response);
            if (response.getHeaderString(X_EVENT_STATUS) != null) {
                return LifeCycleStatusCode.valueOf(response.getHeaderString(X_EVENT_STATUS));
            }
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        } catch (
            PreconditionFailedClientException
            | VitamClientInternalException
            | LogbookClientBadRequestException
            | LogbookClientAlreadyExistsException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void bulkObjectGroup(String eventIdProc, List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        try (
            Response response = make(
                put()
                    .withPath(OPERATIONS_URL + "/" + eventIdProc + "/lifecycles/objectgroup/bulk")
                    .withBody(logbookLifeCycleModels)
                    .withJson()
            )
        ) {
            check(response);
        } catch (LogbookClientNotFoundException | VitamClientInternalException | PreconditionFailedClientException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void bulkUnit(String eventIdProc, List<LogbookLifeCycleUnitModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        try (
            Response response = make(
                put()
                    .withPath(OPERATIONS_URL + "/" + eventIdProc + "/lifecycles/unit/bulk")
                    .withBody(logbookLifeCycleModels, "Cannot be null")
                    .withJson()
            )
        ) {
            check(response);
        } catch (LogbookClientNotFoundException | VitamClientInternalException | PreconditionFailedClientException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode getRawUnitLifeCycleById(String id) throws LogbookClientException {
        try (Response response = make(get().withPath(UNIT_LIFECYCLES_RAW_BY_ID_URL + id).withJsonAccept())) {
            check(response);
            return ((RequestResponseOK<JsonNode>) RequestResponse.parseFromResponse(response)).getFirstResult();
        } catch (PreconditionFailedClientException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public List<JsonNode> getRawUnitLifeCycleByIds(List<String> ids) throws LogbookClientException {
        try (
            Response response = make(
                get().withPath(UNIT_LIFECYCLES_RAW_BY_IDS_URL).withBody(ids, "Ids cannot be null.").withJson()
            )
        ) {
            check(response);
            return ((RequestResponseOK<JsonNode>) RequestResponse.parseFromResponse(response)).getResults();
        } catch (PreconditionFailedClientException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode getRawObjectGroupLifeCycleById(String id) throws LogbookClientException {
        try (Response response = make(get().withPath(OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL + id).withJsonAccept())) {
            check(response);
            return ((RequestResponseOK<JsonNode>) RequestResponse.parseFromResponse(response)).getFirstResult();
        } catch (PreconditionFailedClientException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public List<JsonNode> getRawObjectGroupLifeCycleByIds(List<String> ids) throws LogbookClientException {
        try (
            Response response = make(
                get().withPath(OBJECT_GROUP_LIFECYCLES_RAW_BY_IDS_URL).withBody(ids, "Ids cannot be null.").withJson()
            )
        ) {
            check(response);
            return ((RequestResponseOK<JsonNode>) RequestResponse.parseFromResponse(response)).getResults();
        } catch (VitamClientInternalException | PreconditionFailedClientException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void createRawbulkObjectgrouplifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        createRawbulk(logbookLifeCycleRaws, OBJECT_GROUP_LIFECYCLES_RAW_BULK_URL);
    }

    @Override
    public void createRawbulkUnitlifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        createRawbulk(logbookLifeCycleRaws, UNIT_LIFECYCLES_RAW_BULK_URL);
    }

    @Override
    public void bulkLifeCycleTemporary(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) throws VitamClientInternalException {
        bulkLFC(
            type,
            logbookLifeCycleParametersBulk,
            OPERATIONS_URL + "/" + operationId + "/bulklifecycles/%s/temporary"
        );
    }

    @Override
    public void bulkLifeCycle(
        String operationId,
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk
    ) throws VitamClientInternalException {
        bulkLFC(type, logbookLifeCycleParametersBulk, OPERATIONS_URL + "/" + operationId + "/bulklifecycles/%s");
    }

    private void bulkLFC(
        DistributionType type,
        List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk,
        String uriPattern
    ) throws VitamClientInternalException {
        VitamRequestBuilder request = post()
            .withBody(logbookLifeCycleParametersBulk, "LogbookLifeCycleParametersBulk cannot be null.")
            .withJson();
        if (type == DistributionType.Units) {
            request.withPath(String.format(uriPattern, "unit"));
        } else if (type == DistributionType.ObjectGroup) {
            request.withPath(String.format(uriPattern, "got"));
        } else {
            throw new VitamClientInternalException(String.format("DistributionType with '%s' not managed.", type));
        }
        try (Response response = make(request)) {
            check(response);
        } catch (
            LogbookClientBadRequestException
            | PreconditionFailedClientException
            | LogbookClientServerException
            | LogbookClientAlreadyExistsException
            | LogbookClientNotFoundException e
        ) {
            throw new VitamRuntimeException("Unable to store lifecycle.", e);
        }
    }

    @Override
    public void deleteLifecycleUnitsBulk(Collection<String> unitsIds)
        throws LogbookClientServerException, LogbookClientBadRequestException {
        try (
            Response response = make(
                delete()
                    .withPath("/lifeCycleUnits/bulkDelete")
                    .withBody(unitsIds, "unitsIds has to be provided")
                    .withJson()
            )
        ) {
            check(response);
        } catch (
            PreconditionFailedClientException
            | VitamClientInternalException
            | LogbookClientAlreadyExistsException
            | LogbookClientNotFoundException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void deleteLifecycleObjectGroupBulk(Collection<String> objectGroupIds)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        try (
            Response response = make(
                delete()
                    .withPath("/objectgrouplifecycles/bulkDelete")
                    .withBody(objectGroupIds, "objectGroupIds has to be provided")
                    .withJson()
            )
        ) {
            check(response);
        } catch (
            PreconditionFailedClientException
            | VitamClientInternalException
            | LogbookClientAlreadyExistsException
            | LogbookClientNotFoundException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    private void createRawbulk(List<JsonNode> logbookLifeCycleRaws, String url)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        try (
            Response response = make(
                post()
                    .withPath(url)
                    .withBody(logbookLifeCycleRaws, "logbookLifeCycleRaws has to be provided")
                    .withJson()
            )
        ) {
            check(response);
        } catch (
            PreconditionFailedClientException
            | VitamClientInternalException
            | LogbookClientAlreadyExistsException
            | LogbookClientNotFoundException e
        ) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    private String getServiceUrl(LogbookLifeCycleParameters parameters, String eip, String oid) {
        String logBookLifeCycleUrl;
        if (parameters instanceof LogbookLifeCycleObjectGroupParameters) {
            logBookLifeCycleUrl = OBJECT_GROUP_LIFECYCLES_URL;
        } else if (parameters instanceof LogbookLifeCycleUnitParameters) {
            logBookLifeCycleUrl = UNIT_LIFECYCLES_URL;
        } else {
            throw new IllegalArgumentException("Parameters to be checked");
        }
        return "/operations/" + eip + logBookLifeCycleUrl + "/" + oid;
    }

    private void check(Response response)
        throws LogbookClientAlreadyExistsException, LogbookClientServerException, LogbookClientBadRequestException, LogbookClientNotFoundException, PreconditionFailedClientException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case INTERNAL_SERVER_ERROR:
                throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            case CONFLICT:
                throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
            case PRECONDITION_FAILED:
                throw new PreconditionFailedClientException(REQUEST_PRECONDITION_FAILED);
            case NOT_FOUND:
                throw new LogbookClientNotFoundException(LOGBOOK_NOT_FOUND.getMessage());
            case BAD_REQUEST:
                throw new LogbookClientBadRequestException(
                    ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage()
                );
            default:
                throw new LogbookClientServerException(status.toString());
        }
    }
}
