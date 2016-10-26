package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Mock client implementation for Access External
 */
public class AccessExternalClientMock extends AbstractMockClient implements AccessExternalClient {

    private static final String RESULT =
        "{\"query\":{}," +
            "\"hits\":{\"total\":100,\"offset\":0,\"limit\":25}," +
            "\"result\":";

    private static final String OPERATION =
        "\"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
            "    \"evType\": \"Process_SIP_unitary\"," +
            "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
            "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
            "    \"evTypeProc\": \"INGEST\"," +
            "    \"outcome\": \"STARTED\"," +
            "    \"outDetail\": null," +
            "    \"outMessg\": \"SIP entry : SIP.zip\"," +
            "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
            "    \"agIdApp\": null," +
            "    \"agIdAppSession\": null," +
            "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
            "    \"agIdSubm\": null," +
            "    \"agIdOrig\": null," +
            "    \"obId\": null," +
            "    \"obIdReq\": null," +
            "    \"obIdIn\": null," +
            "    \"events\": []}";
    private static final String MOCK_SELECT_RESULT_1 = "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "    \"evType\": \"Process_SIP_unitary\"," +
        "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evTypeProc\": \"INGEST\"," +
        "    \"outcome\": \"STARTED\"," +
        "    \"outDetail\": null," +
        "    \"outMessg\": \"SIP entry : SIP.zip\"," +
        "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
        "    \"agIdApp\": null," +
        "    \"agIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdSubm\": null," +
        "    \"agIdOrig\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

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
        return createResult();
    }

    @Override
    public JsonNode selectOperationbyId(String processId) throws InvalidParseOperationException{
        return JsonHandler.getFromString(MOCK_SELECT_RESULT_1);
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String idUnit)
        throws LogbookClientException, InvalidParseOperationException {
        return JsonHandler.getFromString(MOCK_SELECT_RESULT_1);
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String idObject)
        throws LogbookClientException, InvalidParseOperationException {
        return JsonHandler.getFromString(MOCK_SELECT_RESULT_1);  
    }


    private JsonNode createResult() throws InvalidParseOperationException {
        String result = RESULT + "[";
        for (int i = 0; i < 100; i++) {
            String s_i = "{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaa" + i + "\",";
            s_i += OPERATION;
            result += s_i;
            if (i < 99) {
                result += ",";
            }
        }
        result += "]}";
        return JsonHandler.getFromString(result);
    }
}
