package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Rest client implementation for Access External
 */
class AccessExternalClientRest extends DefaultClient implements AccessExternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientRest.class);

    private static final String UNITS = "/units/";
    private static final String UNITS_RULES = "/units/rules";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_USAGE = "usage should be filled";
    private static final String BLANK_VERSION = "usage version should be filled";
    private static final String BLANK_DIP_ID = "DIP identifier should be filled";
    private static final String BLANK_TRANSFER_ID = "Transfer identifier should be filled";
    private static final String MISSING_VITAM_CONTEXT = "Missing vitam context";
    private static final String MISSING_RECLASSIFICATION_REQUEST = "Missing reclassification request";
    private static final String MISSING_ELIMINATION_REQUEST = "Missing elimination request";

    private static final String LOGBOOK_OPERATIONS_URL = "/logbookoperations";
    private static final String LOGBOOK_UNIT_LIFECYCLE_URL = "/logbookunitlifecycles";
    private static final String LOGBOOK_OBJECT_LIFECYCLE_URL = "/logbookobjectslifecycles";

    private static final String COULD_NOT_PARSE_SERVER_RESPONSE = "Could not parse server response";
    private static final String VITAM_CLIENT_INTERNAL_EXCEPTION = "VitamClientInternalException: ";

    AccessExternalClientRest(AccessExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, "/units", vitamContext.getHeaders(),
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException {
        Response response = null;

        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        try {
            response = performRequest(HttpMethod.GET, UNITS + unitId, vitamContext.getHeaders(),
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery, String unitId)
        throws VitamClientException {
        Response response = null;
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        try {
            response = performRequest(HttpMethod.PUT, UNITS + unitId, vitamContext.getHeaders(), updateQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext,
        JsonNode selectObjectQuery,
        String unitId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, UNITS + unitId + AccessExtAPI.OBJECTS, vitamContext.getHeaders(),
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext,
        String unitId,
        String usage,
        int version)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        Response response;
        MultivaluedMap<String, Object> headers = vitamContext.getHeaders();
        headers.add(GlobalDataRest.X_QUALIFIER, usage);
        headers.add(GlobalDataRest.X_VERSION, version);

        try {
            response = performRequest(HttpMethod.GET, UNITS + unitId + AccessExtAPI.OBJECTS, headers,
                null, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

        } catch (final VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        }
        return response;
    }

    /* Logbook external */

    @Override
    public RequestResponse<LogbookOperation> selectOperations(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL, vitamContext.getHeaders(), select,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String processId, JsonNode select)
        throws VitamClientException {

        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL + "/" + processId, vitamContext.getHeaders(),
                    select, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(VitamContext vitamContext, String idUnit,
        JsonNode select)
        throws VitamClientException {
        Response response = null;
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);
        try {
            response =
                performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit, vitamContext.getHeaders(),
                    select, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String idObject, JsonNode select)
        throws VitamClientException {
        Response response = null;
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, idObject);
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject,
                vitamContext.getHeaders(),
                select, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext, JsonNode dslRequest)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, AccessExtAPI.DIP_API, vitamContext.getHeaders(),
                dslRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> transfer(VitamContext vitamContext, TransferRequest transferRequest)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, AccessExtAPI.TRANSFER_API, vitamContext.getHeaders(),
                transferRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response getTransferById(VitamContext vitamContext, String transferId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_TRANSFER_ID, transferId);

        Response response;

        try {
            response =
                performRequest(HttpMethod.GET, AccessExtAPI.TRANSFER_API + "/" + transferId + "/sip", vitamContext.getHeaders(),
                    null, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

        } catch (final VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        }
        return response;
    }

    @Override
    public Response getDIPById(VitamContext vitamContext, String dipId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_DIP_ID, dipId);

        Response response;

        try {
            response =
                performRequest(HttpMethod.GET, AccessExtAPI.DIP_API + "/" + dipId + "/dip", vitamContext.getHeaders(),
                    null, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

        } catch (final VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        }
        return response;
    }

    /**
     * Performs a reclassification workflow.
     *
     * @param vitamContext the vitam context
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     */
    public RequestResponse<JsonNode> reclassification(VitamContext vitamContext, JsonNode reclassificationRequest)
        throws VitamClientException {

        ParametersChecker.checkParameter(MISSING_VITAM_CONTEXT, vitamContext);
        ParametersChecker.checkParameter(MISSING_RECLASSIFICATION_REQUEST, reclassificationRequest);

        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/reclassification", vitamContext.getHeaders(),
                reclassificationRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnits(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, UNITS, vitamContext.getHeaders(), updateQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnitsRules(VitamContext vitamContext, JsonNode updateRulesQuery)
        throws VitamClientException {
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, UNITS_RULES, headers, updateRulesQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjects(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, AccessExtAPI.OBJECTS, vitamContext.getHeaders(),
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse<JsonNode> selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, "/unitsWithInheritedRules", vitamContext.getHeaders(),
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response getAccessLog(VitamContext vitamContext, JsonNode params)
        throws VitamClientException {
        Response response;

        try {
            response = performRequest(HttpMethod.GET, "/storageaccesslog", vitamContext.getHeaders(),
                params, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);
            return response;
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> launchPreservation(VitamContext vitamContext,
        PreservationRequest preservationRequest) throws VitamClientException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/preservation", vitamContext.getHeaders(),
                preservationRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAnalysis(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        ParametersChecker.checkParameter(MISSING_VITAM_CONTEXT, vitamContext);
        ParametersChecker.checkParameter(MISSING_ELIMINATION_REQUEST, eliminationRequestBody);

        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/elimination/analysis", vitamContext.getHeaders(),
                eliminationRequestBody, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAction(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        ParametersChecker.checkParameter(MISSING_VITAM_CONTEXT, vitamContext);
        ParametersChecker.checkParameter(MISSING_ELIMINATION_REQUEST, eliminationRequestBody);

        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/elimination/action", vitamContext.getHeaders(),
                eliminationRequestBody, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> computedInheritedRules(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException {
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, UNITS + AccessExtAPI.COMPUTEDINHERITEDRULES, vitamContext.getHeaders(),
                    updateQuery,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> deleteComputedInheritedRules(VitamContext vitamContext,
        JsonNode deleteComputedInheritedRulesQuery) throws VitamClientException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.DELETE, UNITS + AccessExtAPI.COMPUTEDINHERITEDRULES,
                vitamContext.getHeaders(), deleteComputedInheritedRulesQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse transferReply(VitamContext vitamContext, InputStream transferReply) throws VitamClientException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/transfers/reply", vitamContext.getHeaders(), transferReply, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
