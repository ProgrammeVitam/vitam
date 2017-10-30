package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

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
    public Response getObject(VitamContext vitamContext, JsonNode selectQuery,
        String objectId,
        String usage, int version)
        throws InvalidParseOperationException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws VitamClientException {
        return ClientMockResultHelper.getGotSimpleResult(selectQuery);
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperation(VitamContext vitamContext,
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
    public Response getObjectStreamByUnitId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId,
        String usage, int version)
        throws VitamClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext,
    		JsonNode selectQuery) throws VitamClientException {
        return ClientMockResultHelper.getDIPSimpleResult(selectQuery);
    }

    @Override
    public Response getDIPById(VitamContext vitamContext, String dipId)
    		throws VitamClientException {
    	return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

}
