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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Mock client implementation for Access External
 */
class AccessExternalClientMock extends AbstractMockClient implements AccessExternalClient {

    @Override
    public RequestResponse selectUnits(String selectQuery) throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse selectUnitbyId(String selectQuery, String unitId) throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse updateUnitbyId(String updateQuery, String unitId) throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString("{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public Response getObject(String selectQuery, String objectId, String usage, int version) throws InvalidParseOperationException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse selectObjectById(String selectQuery, String unitId) throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"#id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
                "[{'#id': '1', 'name': 'abcdef', 'creation_date': '2015-07-14T17:07:14Z', 'fmt': 'ftm/123', 'numerical_information': '55.3'}]}"));
    }

    @Override
    public RequestResponse selectOperation(String select) throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbooksRequestResponse();
    }

    @Override
    public RequestResponse selectOperationbyId(String processId) throws InvalidParseOperationException{
        return ClientMockResultHelper.getLogbookRequestResponse();
    }

    @Override
    public RequestResponse selectUnitLifeCycleById(String idUnit)
        throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookRequestResponse();
    }

    @Override
    public RequestResponse selectObjectGroupLifeCycleById(String idObject)
        throws LogbookClientException, InvalidParseOperationException {
        return ClientMockResultHelper.getLogbookRequestResponse();
    }

    @Override
    public RequestResponse getAccessionRegisterSummary(JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterDetail();
    }
}
