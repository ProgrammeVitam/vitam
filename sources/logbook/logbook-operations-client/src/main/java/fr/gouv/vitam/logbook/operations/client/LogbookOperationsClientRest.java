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
package fr.gouv.vitam.logbook.operations.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;
import static fr.gouv.vitam.logbook.common.LogbookDataRest.X_CROSS_TENANT;
import static fr.gouv.vitam.logbook.common.LogbookDataRest.X_SLICED_OPERATIONS;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

class LogbookOperationsClientRest extends DefaultClient implements LogbookOperationsClient {
    private static final String OPERATIONS_URL = "/operations";
    private final String RECONSTRUCTION_OPERATIONS_URI = "/reconstruction/operations";

    LogbookOperationsClientRest(LogbookOperationsClientFactory factory) {
        super(factory);
    }

    @Override
    public void create(LogbookOperationParameters... parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        if (parameters.length == 0) {
            return;
        }
        final String eip = LogbookOperationsClientHelper.checkLogbookParameters(parameters[0]);
        try (Response response = make(post().withPath(OPERATIONS_URL + "/" + eip).withBody(parameters).withJson())) {
            check(response);
        } catch (VitamClientInternalException | LogbookClientNotFoundException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void update(LogbookOperationParameters... parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        if (parameters.length == 0) {
            return;
        }
        final String eip = LogbookOperationsClientHelper.checkLogbookParameters(parameters[0]);
        try (Response response = make(put().withPath(OPERATIONS_URL + "/" + eip).withBody(parameters).withJson())) {
            check(response);
        } catch (LogbookClientAlreadyExistsException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }


    @Override
    public JsonNode selectOperation(JsonNode select) throws LogbookClientException, InvalidParseOperationException {
        return selectOperation(select, false, false);
    }

    @Override
    public JsonNode selectOperation(JsonNode select, boolean isSliced, boolean isCrossTenant)
        throws LogbookClientException, InvalidParseOperationException {
        VitamRequestBuilder request = get().withPath(OPERATIONS_URL).withBody(select).withJson();
        if (isSliced) {
            request.withHeader(X_SLICED_OPERATIONS, true);
        }
        if (isCrossTenant) {
            request.withHeader(X_CROSS_TENANT, true);
        }
        try (Response response = make(request)) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode selectOperationById(String processId, JsonNode query, boolean isSliced, boolean isCrossTenant)
        throws LogbookClientException, InvalidParseOperationException {

        VitamRequestBuilder request = get().withPath(OPERATIONS_URL + "/" + processId).withBody(query)
            .withContentType(MediaType.APPLICATION_JSON_TYPE).withJsonAccept();

        if (isSliced) {
            request.withHeader(X_SLICED_OPERATIONS, true);
        }
        if (isCrossTenant) {
            request.withHeader(X_CROSS_TENANT, true);
        }

        try (Response response = make(
            request)) {
            check(response);
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public JsonNode selectOperationById(String processId)
        throws LogbookClientException, InvalidParseOperationException {
        return selectOperationById(processId, new Select().getFinalSelect(), false, false);
    }

    @Override
    public void create(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        LogbookOperationParameters[] parameters =
            StreamSupport.stream(queue.spliterator(), false).toArray(LogbookOperationParameters[]::new);
        try (Response response = make(
            post().withPath(OPERATIONS_URL + "/" + eventIdProc).withBody(parameters).withJson())) {
            check(response);
        } catch (VitamClientInternalException | LogbookClientNotFoundException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void update(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        LogbookOperationParameters[] parameters =
            StreamSupport.stream(queue.spliterator(), false).toArray(LogbookOperationParameters[]::new);
        try (Response response = make(
            put().withPath(OPERATIONS_URL + "/" + eventIdProc).withBody(parameters).withJson())) {
            check(response);
        } catch (LogbookClientAlreadyExistsException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public RequestResponseOK<TenantLogbookOperationTraceabilityResult> traceability(List<Integer> tenants)
        throws LogbookClientServerException, InvalidParseOperationException {
        try (Response response = make(post().withPath("/operations/traceability")
            .withHeader(X_TENANT_ID, getTenantParameter())
            .withBody(JsonHandler.toJsonNode(tenants))
            .withJson())) {
            check(response);
            RequestResponse<TenantLogbookOperationTraceabilityResult> result =
                RequestResponse.parseFromResponse(response, TenantLogbookOperationTraceabilityResult.class);
            return (RequestResponseOK<TenantLogbookOperationTraceabilityResult>) result;
        } catch (LogbookClientNotFoundException | VitamClientInternalException | LogbookClientBadRequestException |
                 LogbookClientAlreadyExistsException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public RequestResponseOK<String> traceabilityLfcUnit()
        throws LogbookClientServerException, InvalidParseOperationException {
        return traceabilityLFC("/lifecycles/units/traceability");
    }

    @Override
    public RequestResponseOK<String> traceabilityLfcObjectGroup()
        throws LogbookClientServerException, InvalidParseOperationException {
        return traceabilityLFC("/lifecycles/objectgroups/traceability");
    }

    private RequestResponseOK<String> traceabilityLFC(String traceabilityUri)
        throws LogbookClientServerException, InvalidParseOperationException {
        try (Response response = make(
            post().withPath(traceabilityUri).withHeader(X_TENANT_ID, getTenantParameter()).withJsonAccept())) {
            check(response);
            return RequestResponse.parseRequestResponseOk(response, String.class);
        } catch (LogbookClientNotFoundException | VitamClientInternalException | LogbookClientBadRequestException |
                 LogbookClientAlreadyExistsException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public LifecycleTraceabilityStatus checkLifecycleTraceabilityWorkflowStatus(String operationId)
        throws LogbookClientServerException, InvalidParseOperationException {
        try (Response response = make(get().withPath("/lifecycles/traceability/check/" + operationId)
            .withHeader(X_TENANT_ID, getTenantParameter()).withJsonAccept())) {
            check(response);
            JsonNode jsonNode = response.readEntity(JsonNode.class);
            RequestResponseOK<LifecycleTraceabilityStatus> requestResponse =
                RequestResponseOK.getFromJsonNode(jsonNode, LifecycleTraceabilityStatus.class);
            return requestResponse.getFirstResult();
        } catch (VitamClientInternalException | LogbookClientNotFoundException | LogbookClientAlreadyExistsException |
                 LogbookClientBadRequestException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public ReindexationResult reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, LogbookClientServerException {
        try (Response response = make(
            post().withPath("/reindex").withBody(indexParam, "The options are mandatory").withJson())) {
            check(response);
            return response.readEntity(ReindexationResult.class);
        } catch (LogbookClientNotFoundException | VitamClientInternalException | LogbookClientBadRequestException |
                 LogbookClientAlreadyExistsException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public SwitchIndexResult switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, LogbookClientServerException {
        try (Response response = make(
            post().withPath("/alias").withBody(switchIndexParam, "The options are mandatory").withJson())) {
            check(response);
            return response.readEntity(SwitchIndexResult.class);
        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException |
                 LogbookClientBadRequestException | VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public void traceabilityAudit(int tenant, AuditLogbookOptions options) throws LogbookClientServerException {
        try (Response response = make(
            post().withPath("/auditTraceability").withHeader(X_TENANT_ID, tenant).withBody(options).withJson())) {
            check(response);
        } catch (LogbookClientNotFoundException | VitamClientInternalException | LogbookClientBadRequestException |
                 LogbookClientAlreadyExistsException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    @Override
    public LogbookCheckResult checkLogbookCoherence() throws LogbookClientServerException {
        try (Response response = make(post().withPath("/checklogbook").withJson())) {
            check(response);
            JsonNode jsonNode = response.readEntity(JsonNode.class);
            return JsonHandler.getFromJsonNode(jsonNode, LogbookCheckResult.class);
        } catch (VitamClientInternalException | InvalidParseOperationException | LogbookClientNotFoundException |
                 LogbookClientAlreadyExistsException | LogbookClientBadRequestException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }

    private void check(Response response)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case CONFLICT:
                throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
            case BAD_REQUEST:
                throw new LogbookClientBadRequestException(
                    ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            case NOT_FOUND:
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            default:
                throw new LogbookClientServerException(status.toString());
        }
    }

    @Override
    public RequestResponse<JsonNode> getLastOperationByType(String operationType)
        throws LogbookClientServerException {
        try (Response response = make(get().withPath("/lastOperationByType").withBody(operationType).withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                 LogbookClientNotFoundException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }


    @Override
    public void reconstructCollection(List<ReconstructionRequestItem> reconstructionItems)
        throws LogbookClientServerException {
        try (Response response = make(
            post().withPath(RECONSTRUCTION_OPERATIONS_URI).withBody(reconstructionItems).withJson())) {
            check(response);
        } catch (VitamClientInternalException | LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                 LogbookClientNotFoundException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        }
    }
}
