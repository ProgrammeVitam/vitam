package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamContext;
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

        Response checkDocumentsResponse =
            client.checkDocuments(new VitamContext(TENANT_ID), AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()));
        assertEquals(Status.OK.getStatusCode(), checkDocumentsResponse.getStatus());
        assertEquals(
            client.createDocuments(new VitamContext(TENANT_ID), AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes()), "test.xml"
            ),
            Status.CREATED);

        assertEquals(
            client.createProfiles(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes())).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.importProfileFile(new VitamContext(TENANT_ID), "fakeId", new ByteArrayInputStream("test".getBytes())).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.downloadProfileFile(new VitamContext(TENANT_ID), "fakeId").getStatus(),
            Status.OK.getStatusCode());


        assertEquals(
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode()).toString(),
            ClientMockResultHelper.getFormatList().toString());

        assertEquals(
            client.findRules(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode()).toString(),
            ClientMockResultHelper.getRuleList().toString());

        assertEquals(
            client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getFormat().toString());

        assertEquals(
            client.findRuleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getRule().toString());

        assertEquals(
            client.importContexts(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes())).getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.checkTraceabilityOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(queryDsql)).getHttpCode(),
            Status.OK.getStatusCode());

        assertEquals(
            client.launchAudit(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), JsonHandler.getFromString(AUDIT_OPTION)).getHttpCode(),
            Status.ACCEPTED.getStatusCode());

    }

}
