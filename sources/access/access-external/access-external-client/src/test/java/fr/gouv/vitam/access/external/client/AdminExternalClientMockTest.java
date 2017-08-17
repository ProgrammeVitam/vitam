package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;

public class AdminExternalClientMockTest {

    AdminExternalClient client;
    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    private static final String AUDIT_OPTION = "{serviceProducteur: \"Service Producteur 1\"}";
    private static final String DOCUMENT_ID = "1";
    final int TENANT_ID = 0;
    final String CONTRACT = "contract";

    @Before
    public void givenMockConfExistWhenAccessExternalCreateMockedClientThenReturnOK() {
        AdminExternalClientFactory.changeMode(null);
        client = AdminExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }

    @Test
    public void testMockClient()
        throws Exception {
        assertEquals(
            client.checkDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), TENANT_ID),
            Status.OK);

        assertEquals(
            client.createDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), "test.xml",
                TENANT_ID),
            Status.CREATED);

        assertEquals(
            client.createProfiles(new ByteArrayInputStream("test".getBytes()), TENANT_ID).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.importProfileFile("fakeId", new ByteArrayInputStream("test".getBytes()), TENANT_ID).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.downloadProfileFile("fakeId", TENANT_ID).getStatus(),
            Status.OK.getStatusCode());


        assertEquals(
            client.findDocuments(AdminCollections.FORMATS, JsonHandler.createObjectNode(), TENANT_ID).toString(),
            ClientMockResultHelper.getFormatList().toString());

        assertEquals(
            client.findDocuments(AdminCollections.RULES, JsonHandler.createObjectNode(), TENANT_ID).toString(),
            ClientMockResultHelper.getRuleList().toString());

        assertEquals(
            client.findDocumentById(AdminCollections.FORMATS, DOCUMENT_ID, TENANT_ID).toString(),
            ClientMockResultHelper.getFormat().toString());

        assertEquals(
            client.findDocumentById(AdminCollections.RULES, DOCUMENT_ID, TENANT_ID).toString(),
            ClientMockResultHelper.getRule().toString());

        assertEquals(
            client.importContexts(new ByteArrayInputStream("test".getBytes()), TENANT_ID).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.checkTraceabilityOperation(JsonHandler.getFromString(queryDsql), TENANT_ID, CONTRACT).getHttpCode(),
            Status.OK.getStatusCode());
        
        assertEquals(
            client.launchAudit(JsonHandler.getFromString(AUDIT_OPTION), TENANT_ID, CONTRACT),
            Status.OK);

    }

}
