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

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

/**
 * Mock client implementation for Access External
 */
class AccessExternalClientMock extends AbstractMockClient implements AccessExternalClient {

    @Override
    public RequestResponse selectUnits(JsonNode selectQuery, Integer tenantId, String contractName)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse selectUnitbyId(JsonNode selectQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse updateUnitbyId(JsonNode updateQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public Response getObject(JsonNode selectQuery, String objectId, String usage, int version, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse selectObjectById(JsonNode selectQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException {
        return ClientMockResultHelper.getArchiveUnitResult();
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperation(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationsRequestResponse();
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperationbyId(String processId, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(String idUnit, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(String idObject, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public Response getUnitByIdWithXMLFormat(JsonNode queryDsl, String idUnit, Integer tenantId, String contractName)
        throws AccessExternalClientServerException {
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/unit.xml")) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(resourceAsStream, byteArrayOutputStream);
            return Response.ok().entity(byteArrayOutputStream.toByteArray()).build();
        } catch (IOException e) {
            throw new AccessExternalClientServerException(e);
        }
    }

    @Override
    public Response getUnitObject(JsonNode selectQuery, String unitId, String usage, int version, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

}
