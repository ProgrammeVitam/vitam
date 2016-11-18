package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.client2.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Mock client implementation for Access External
 */
class AccessExternalClientMock extends AbstractMockClient implements AccessExternalClient {

    @Override
    public JsonNode selectUnits(String selectQuery) throws InvalidParseOperationException {
        return JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'_id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}");
    }

    @Override
    public JsonNode selectUnitbyId(String selectQuery, String unitId) throws InvalidParseOperationException {
        return JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'_id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}");
    }

    @Override
    public JsonNode updateUnitbyId(String updateQuery, String unitId) throws InvalidParseOperationException {
        return JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'_id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}");
    }

    @Override
    public Response getObject(String selectQuery, String objectId, String usage, int version) throws InvalidParseOperationException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public JsonNode selectObjectById(String selectQuery, String unitId) throws InvalidParseOperationException {
        return JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
                "[{'_id': '1', 'name': 'abcdef', 'creation_date': '2015-07-14T17:07:14Z', 'fmt': 'ftm/123', 'numerical_information': '55.3'}]}");
    }

    @Override
    public JsonNode selectOperation(String select) throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.createLogbookResult();
    }

    @Override
    public JsonNode selectOperationbyId(String processId) throws InvalidParseOperationException{
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String idUnit)
        throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String idObject)
        throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookOperation();
    }

    @Override
    public JsonNode getAccessionRegisterSummary(JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public JsonNode getAccessionRegisterDetail(String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterDetail();
    }
}
