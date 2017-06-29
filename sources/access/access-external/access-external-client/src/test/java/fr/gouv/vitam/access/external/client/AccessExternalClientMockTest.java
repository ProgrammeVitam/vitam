package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.json.JsonHandler;

public class AccessExternalClientMockTest {

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String BODY_WITH_ID = "{$query: {$eq: {\"#id\": \"identifier1\" }}, $projection: {}, $filter: {}}";
    final String ID = "identifier1";
    final String USAGE = "usage";
    final String CONTRACT = "contract";
    final int VERSION = 1;
    final int TENANT_ID = 0;
    AccessExternalClient client;

    @Before
    public void givenMockConfExistWhenAccessExternalCreateMockedClientThenReturnOK() {
        AccessExternalClientFactory.changeMode(null);
        client = AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }


    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitsThenReturnResult()
        throws Exception {
        assertNotNull(client.selectUnits(JsonHandler.getFromString(queryDsql), TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.selectUnitbyId(JsonHandler.getFromString(queryDsql), ID, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalUpdateUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.updateUnitbyId(JsonHandler.getFromString(queryDsql), ID, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectOfUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.getObject(JsonHandler.getFromString(queryDsql), ID, USAGE, VERSION, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.selectObjectById(JsonHandler.getFromString(queryDsql), ID, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectOperation(JsonHandler.getFromString(queryDsql), TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationbyIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectOperationbyId(ID, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectUnitLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectUnitLifeCycleById(ID, TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectUnitLifeCycleLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectUnitLifeCycle(JsonHandler.getFromString(BODY_WITH_ID), TENANT_ID, CONTRACT));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectObjectGroupLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectObjectGroupLifeCycleById(ID, TENANT_ID, CONTRACT));
    }
}
