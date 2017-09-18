package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamContext;
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
    public RequestResponse selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery,
        String unitId)
        throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
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
    public RequestResponse selectObjectById(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws InvalidParseOperationException {
        return ClientMockResultHelper.getArchiveUnitResult();
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperation(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationsRequestResponse();
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String processId)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookOperationRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(
        VitamContext vitamContext, String idUnit)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String idObject)
        throws VitamClientException {
        return ClientMockResultHelper.getLogbookLifecycleRequestResponse();
    }

    @Override
    public Response getUnitByIdWithXMLFormat(VitamContext vitamContext, JsonNode queryDsl,
        String idUnit)
        throws AccessExternalClientServerException {
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/unit.xml")) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(resourceAsStream, byteArrayOutputStream);
            return Response.ok().entity(byteArrayOutputStream.toByteArray()).build();
        } catch (IOException e) {
            throw new AccessExternalClientServerException(e);
        }
    }

    @Override public Response getObjectGroupByIdWithXMLFormat(VitamContext vitamContext,
        JsonNode queryDsl, String idUnit) throws AccessExternalClientServerException {
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/object_group.xml")){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(resourceAsStream, byteArrayOutputStream);
            return Response.ok().entity(byteArrayOutputStream.toByteArray()).build();
        } catch (IOException e) {
            throw new AccessExternalClientServerException(e);
        }
    }

    @Override
    public Response getUnitObject(VitamContext vitamContext, JsonNode selectQuery,
        String unitId,
        String usage, int version)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

}
