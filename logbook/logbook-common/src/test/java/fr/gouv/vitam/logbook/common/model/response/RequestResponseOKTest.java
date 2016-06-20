package fr.gouv.vitam.logbook.common.model.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class RequestResponseOKTest {

    @Test
    public void testRequestResponseOK() throws InvalidParseOperationException {
        RequestResponseOK requestOK = new RequestResponseOK();
        DatabaseCursor cursor = new DatabaseCursor(0, 0, 0);
        cursor.setTotal(1).setOffset(0).setLimit(10);
        requestOK.setHits(cursor);
        assertEquals(10, requestOK.getHits().getLimit());
        assertEquals(0, requestOK.getHits().getOffset());
        assertEquals(1, requestOK.getHits().getTotal());
        
        requestOK.setHits(2, 0, 100);
        assertEquals(100, requestOK.getHits().getLimit());
        assertEquals(0, requestOK.getHits().getOffset());
        assertEquals(2, requestOK.getHits().getTotal());
        
        JsonNode test = JsonHandler.getFromString("{}");
        requestOK.setQuery(test).setResult(test);
        assertEquals(test, requestOK.getQuery());
        assertEquals(test, requestOK.getResult());
        
        
    }


}
