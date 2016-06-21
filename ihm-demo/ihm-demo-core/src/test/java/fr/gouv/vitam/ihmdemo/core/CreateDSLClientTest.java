package fr.gouv.vitam.ihmdemo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.junit.Test;

import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.parser.request.parser.SelectParser;

public class CreateDSLClientTest {

    private static final String result = "QUERY: Requests: \n" +
        "{\"$and\":[" +
        "{\"$eq\":{\"date\":\"2006-03-05\"}}," +
        "{\"$eq\":{\"events.obIdIn\":\"name\"}}," +
        "{\"$eq\":{\"evTypeProc\":\"INGEST\"}}," +
        "{\"$eq\":{\"title\":\"Archive2\"}}]}\n" +
        "\tFilter: {\"$limit\":10000,\"$orderby\":{\"date\":1}}\n" +
        "\tRoots: []\n" +
        "\tProjection: {}\n" +
        "\tLastLevel: 1";   
	@Test
	public void testSearchCriteriaQueries() throws InvalidCreateOperationException, InvalidParseOperationException {

		HashMap<String, String> myHashMap = new HashMap<String, String>();
		myHashMap.put("title", "Archive2");
		myHashMap.put("date", "2006-03-05");
		myHashMap.put("orderby", "date");
		myHashMap.put("obIdIn", "name");
		myHashMap.put("INGEST", "date");

		String request = CreateDSLClient.createSelectDSLQuery(myHashMap);
		assertNotNull(request);
		final SelectParser request2 = new SelectParser();
		request2.parse(request);
		assertEquals(result, request2.toString());

	}

}