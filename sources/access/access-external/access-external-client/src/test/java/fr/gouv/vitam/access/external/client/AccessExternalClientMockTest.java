package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

public class AccessExternalClientMockTest {

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String ID = "identifier1";
    final String USAGE = "usage";
    final int VERSION = 1;
    AccessExternalClient client;

    @Before
    public void givenMockConfExistWhenAccessExternalCreateMockedClientThenReturnOK() {
        AccessExternalClientFactory.changeMode(null);
        client = AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }


    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitsThenReturnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        assertNotNull(client.selectUnits(queryDsql));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitbyIDThenReturnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        assertNotNull(client.selectUnitbyId(queryDsql, ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalUpdateUnitbyIDThenReturnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        assertNotNull(client.updateUnitbyId(queryDsql, ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectOfUnitbyIDThenReturnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException, IOException {
        assertNotNull(client.getObject(queryDsql, ID, USAGE, VERSION));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectbyIDThenReturnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        assertNotNull(client.selectObjectById(queryDsql, ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationLogbook_ThenRetururnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        LogbookClientException {
        assertNotNull(client.selectOperation(queryDsql));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationbyIdLogbook_ThenRetururnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        LogbookClientException {
        assertNotNull(client.selectOperationbyId(ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectUnitLifeCycleByIdLogbook_ThenRetururnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        LogbookClientException {
        assertNotNull(client.selectUnitLifeCycleById(ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectObjectGroupLifeCycleByIdLogbook_ThenRetururnResult()
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException,
        LogbookClientException {
        assertNotNull(client.selectObjectGroupLifeCycleById(ID));
    }
}
