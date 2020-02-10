/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

class AccessExternalClientRest extends DefaultClient implements AccessExternalClient {
    public static final String BLANK_QUERY = "selectQuery cannot be null.";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientRest.class);
    private static final String UNITS = "/units/";
    private static final String UNITS_RULES = "/units/rules";
    private static final String LOGBOOK_OPERATIONS_URL = "/logbookoperations";
    private static final String LOGBOOK_UNIT_LIFECYCLE_URL = "/logbookunitlifecycles";
    private static final String LOGBOOK_OBJECT_LIFECYCLE_URL = "/logbookobjectslifecycles";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_DIP_ID = "DIP identifier should be filled";
    private static final String BLANK_TRANSFER_ID = "Transfer identifier should be filled";
    private static final String MISSING_RECLASSIFICATION_REQUEST = "Missing reclassification request";
    private static final String MISSING_ELIMINATION_REQUEST = "Missing elimination request";

    AccessExternalClientRest(AccessExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        VitamRequestBuilder request =
            get().withPath("/units").withHeaders(vitamContext.getHeaders()).withBody(selectQuery, BLANK_QUERY)
                .withJson();
        try (Response response = make(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (!SUCCESSFUL.equals(status.getFamily()) && !REDIRECTION.equals(status.getFamily())) {
                LOGGER.error(String
                    .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                        fromStatusCode(response.getStatus()).getReasonPhrase()));
                LOGGER.warn(
                    "We should have throw an exception in this case but we cannot, because it will break this API.");
            }
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);
        try (Response response = make(
            get().withPath(UNITS + unitId).withHeaders(vitamContext.getHeaders()).withBody(selectQuery, BLANK_QUERY)
                .withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery, String unitId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);
        VitamRequestBuilder request = put()
            .withPath(UNITS + unitId)
            .withHeaders(vitamContext.getHeaders())
            .withBody(updateQuery, BLANK_QUERY)
            .withJson();
        try (Response response = make(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (!SUCCESSFUL.equals(status.getFamily()) && !REDIRECTION.equals(status.getFamily())) {
                LOGGER.error(String
                    .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                        fromStatusCode(response.getStatus()).getReasonPhrase()));
                LOGGER.warn(
                    "We should have throw an exception in this case but we cannot, because it will break this API.");
            }
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext,
        JsonNode selectObjectQuery,
        String unitId)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(UNITS + unitId + AccessExtAPI.OBJECTS)
            .withHeaders(vitamContext.getHeaders())
            .withBody(selectObjectQuery, BLANK_OBJECT_ID)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }


    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext,
        String unitId,
        String usage,
        int version)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);
        VitamRequestBuilder request = get()
            .withPath(UNITS + unitId + AccessExtAPI.OBJECTS)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(GlobalDataRest.X_QUALIFIER, usage)
            .withHeader(GlobalDataRest.X_VERSION, version)
            .withJsonContentType()
            .withOctetAccept();
        Response response = make(request);
        check(response);
        return response;
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperations(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        try (Response response = make(
            get().withPath(LOGBOOK_OPERATIONS_URL).withHeaders(vitamContext.getHeaders()).withBody(select)
                .withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);
        }
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String processId, JsonNode select)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(LOGBOOK_OPERATIONS_URL + "/" + processId)
            .withHeaders(vitamContext.getHeaders())
            .withBody(select, BLANK_QUERY)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);
        }
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(VitamContext vitamContext, String idUnit,
        JsonNode select)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit)
            .withHeaders(vitamContext.getHeaders())
            .withBody(select, BLANK_QUERY)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);
        }
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String idObject, JsonNode select)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, idObject);
        try (Response response = make(
            get().withPath(LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject).withHeaders(vitamContext.getHeaders())
                .withBody(select, BLANK_QUERY).withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext, JsonNode dslRequest)
        throws VitamClientException {
        try (Response response = make(post().withPath(AccessExtAPI.DIP_API).withHeaders(vitamContext.getHeaders())
            .withBody(dslRequest, "dslRequest cannot be null.").withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> transfer(VitamContext vitamContext, TransferRequest transferRequest)
        throws VitamClientException {
        try (Response response = make(
            post().withPath(AccessExtAPI.TRANSFER_API).withHeaders(vitamContext.getHeaders()).withBody(transferRequest)
                .withJson())) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public Response getTransferById(VitamContext vitamContext, String transferId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_TRANSFER_ID, transferId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.TRANSFER_API + "/" + transferId + "/sip")
            .withHeaders(vitamContext.getHeaders())
            .withJsonContentType()
            .withOctetAccept();
        return make(request);
    }

    @Override
    public Response getDIPById(VitamContext vitamContext, String dipId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_DIP_ID, dipId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.DIP_API + "/" + dipId + "/dip")
            .withHeaders(vitamContext.getHeaders())
            .withJsonContentType()
            .withOctetAccept();
        return make(request);
    }

    /**
     * Performs a reclassification workflow.
     *
     * @param vitamContext the vitam context
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     */
    public RequestResponse<JsonNode> reclassification(VitamContext vitamContext, JsonNode reclassificationRequest)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath("/reclassification")
            .withHeaders(vitamContext.getHeaders())
            .withBody(reclassificationRequest, MISSING_RECLASSIFICATION_REQUEST)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnits(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(UNITS)
            .withHeaders(vitamContext.getHeaders())
            .withBody(updateQuery)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnitsRules(VitamContext vitamContext, JsonNode updateRulesQuery)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(UNITS_RULES)
            .withHeaders(vitamContext.getHeaders())
            .withBody(updateRulesQuery)
            .withJson();
        try (Response response = make(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (!SUCCESSFUL.equals(status.getFamily()) && !REDIRECTION.equals(status.getFamily())) {
                LOGGER.error(String
                    .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                        fromStatusCode(response.getStatus()).getReasonPhrase()));
                LOGGER.warn(
                    "We should have throw an exception in this case but we cannot, because it will break this API.");
            }
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjects(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.OBJECTS)
            .withHeaders(vitamContext.getHeaders())
            .withBody(selectQuery, BLANK_QUERY)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath("/unitsWithInheritedRules")
            .withHeaders(vitamContext.getHeaders())
            .withBody(selectQuery)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public Response getAccessLog(VitamContext vitamContext, JsonNode params)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath("/storageaccesslog")
            .withBody(params)
            .withHeaders(vitamContext.getHeaders())
            .withJsonOctet();
        return make(request);
    }

    @Override
    public RequestResponse<JsonNode> launchPreservation(VitamContext vitamContext,
        PreservationRequest preservationRequest) throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath("/preservation")
            .withHeaders(vitamContext.getHeaders())
            .withBody(preservationRequest)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAnalysis(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath("/elimination/analysis")
            .withHeaders(vitamContext.getHeaders())
            .withBody(eliminationRequestBody, MISSING_ELIMINATION_REQUEST)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAction(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath("/elimination/action")
            .withHeaders(vitamContext.getHeaders())
            .withBody(eliminationRequestBody, MISSING_ELIMINATION_REQUEST)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> computedInheritedRules(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(UNITS + AccessExtAPI.COMPUTEDINHERITEDRULES)
            .withHeaders(vitamContext.getHeaders())
            .withBody(updateQuery)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> deleteComputedInheritedRules(VitamContext vitamContext,
        JsonNode deleteComputedInheritedRulesQuery) throws VitamClientException {
        VitamRequestBuilder request = delete()
            .withPath(UNITS + AccessExtAPI.COMPUTEDINHERITEDRULES)
            .withHeaders(vitamContext.getHeaders())
            .withBody(deleteComputedInheritedRulesQuery)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse transferReply(VitamContext vitamContext, InputStream transferReply)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath("/transfers/reply")
            .withHeaders(vitamContext.getHeaders())
            .withBody(transferReply)
            .withXMLContentType()
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }


    private void check(Response response) throws VitamClientException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientException(String
            .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }
}
