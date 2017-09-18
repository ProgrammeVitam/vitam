package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertNotNull;

import fr.gouv.vitam.common.client.VitamContext;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.json.JsonHandler;

public class AccessExternalClientMockTest {

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
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
        assertNotNull(client.selectUnits(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql)));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.selectUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql), ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalUpdateUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.updateUnitbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql), ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectOfUnitbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.getObject(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql), ID, USAGE, VERSION));
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectbyIDThenReturnResult()
        throws Exception {
        assertNotNull(client.selectObjectById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql), ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql)));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationbyIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectOperationbyId(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectUnitLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectUnitLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID));
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectObjectGroupLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(client.selectObjectGroupLifeCycleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), ID));
    }
}
