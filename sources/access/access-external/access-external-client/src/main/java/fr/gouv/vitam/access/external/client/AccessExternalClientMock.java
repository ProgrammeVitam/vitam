package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.dip.TransferRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;

/**
 * Mock client implementation for Access External
 */
class AccessExternalClientMock extends AbstractMockClient implements AccessExternalClient {

    @Override
    public RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        return ClientMockResultHelper.getArchiveUnitSimpleResult(selectQuery);
    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException {
        return ClientMockResultHelper.getArchiveUnitSimpleResult(selectQuery);
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery,
        String unitId)
        throws VitamClientException {
        return ClientMockResultHelper.getArchiveUnitSimpleResult(updateQuery);
    }

    @Override
    public RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws VitamClientException {
        return ClientMockResultHelper.getGotSimpleResult(selectQuery);
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperations(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationsRequestResponse();
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String processId, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(
        VitamContext vitamContext, String idUnit, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String idObject, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext,
        String unitId,
        String usage, int version)
        throws VitamClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext,
        JsonNode dslRequest) throws VitamClientException {
        return ClientMockResultHelper.getDIPSimpleResult(dslRequest);
    }

    @Override
    public RequestResponse<JsonNode> transfer(VitamContext vitamContext, TransferRequest transferRequest)
        throws VitamClientException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Response getDIPById(VitamContext vitamContext, String dipId)
        throws VitamClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> reclassification(VitamContext vitamContext, JsonNode reclassificationRequest) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnits(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException {
        return ClientMockResultHelper.getArchiveUnitSimpleResult(updateQuery);
    }

    @Override
    public RequestResponse<JsonNode> massUpdateUnitsRules(VitamContext vitamContext, JsonNode queryJson)
        throws VitamClientException {
        return ClientMockResultHelper.getArchiveUnitSimpleResult(queryJson);
    }

    @Override
    public RequestResponse<JsonNode> selectObjects(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        return ClientMockResultHelper.getGotSimpleResult(selectQuery);
    }

    @Override
    public RequestResponse<JsonNode> selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery) {
        throw new UnsupportedOperationException("Will not be implemented");
    }

    @Override
    public Response getAccessLog(VitamContext vitamContext, JsonNode params) {
        return new AbstractMockClient.FakeInboundResponse(Status.OK,
            new ByteArrayInputStream("accessLogTest".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> computedInheritedRules(VitamContext vitamContext, JsonNode updateRulesQuery)
        throws VitamClientException {
        throw new UnsupportedOperationException("Will not be implemented");
    }

    @Override
    public RequestResponse<JsonNode> deleteComputedInheritedRules(VitamContext vitamContext,
        JsonNode deleteComputedInheritedRulesQuery) throws VitamClientException {
        throw new UnsupportedOperationException("Will not be implemented");
    }

    @Override
    public RequestResponse<JsonNode> launchPreservation(VitamContext vitamContext,
        PreservationRequest preservationRequest) {
        throw new UnsupportedOperationException("Will not be implemented");
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAnalysis(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        throw new UnsupportedOperationException("Will not be implemented");
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAction(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody) throws VitamClientException {
        throw new UnsupportedOperationException("Will not be implemented");
    }
}
