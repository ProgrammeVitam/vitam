package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;

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
        AdminExternalClientFactory.changeMode((SecureClientConfiguration)null);
        client = AdminExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }

    @Test
    public void testMockClient()
        throws Exception {

        Response checkDocumentsResponse =
            client.checkFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()));
        assertEquals(Status.OK.getStatusCode(), checkDocumentsResponse.getStatus());
        assertEquals(
            client.createFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.createRules(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.createAgencies(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.createProfiles(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.createProfileFile(new VitamContext(TENANT_ID), "fakeId", new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.downloadProfileFile(new VitamContext(TENANT_ID), "fakeId").getStatus(),
            Status.OK.getStatusCode());


        assertEquals(
            client.findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getFormatList().toString());

        assertEquals(
            client.findRules(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getRuleList().toString());

        assertEquals(
            client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getFormat().toString());

        assertEquals(
            client.findRuleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getRule().toString());

        assertEquals(
            client.createContexts(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.checkTraceabilityOperation(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql)).getHttpCode(),
            Status.OK.getStatusCode());

        assertEquals(
            client.launchAudit(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(AUDIT_OPTION)).getHttpCode(),
            Status.ACCEPTED.getStatusCode());

        assertEquals(
            client.createSecurityProfiles(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                new ByteArrayInputStream("test".getBytes()), "test.json").getHttpCode(),
            Status.CREATED.getStatusCode());

        assertEquals(
            client.findSecurityProfiles(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.createObjectNode()).getHttpCode(),
            Status.OK.getStatusCode());

        assertEquals(
            client.findSecurityProfileById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "ID").getHttpCode(),
            Status.OK.getStatusCode());

        assertEquals(
            client.updateSecurityProfile(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "ID",
                JsonHandler.createObjectNode()).getHttpCode(),
            Status.OK.getStatusCode());

        assertEquals(
            client.updateProfile(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "ID",
                JsonHandler.createObjectNode()).getHttpCode(),
            Status.OK.getStatusCode());
        assertEquals(
            client.listOperationsDetails(new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                new ProcessQuery()).getHttpCode(),
            Status.OK.getStatusCode());
        assertEquals(client
            .downloadRulesReport(new VitamContext(TENANT_ID), "ID").getStatus(),
            Status.OK.getStatusCode());
        assertEquals(client
            .downloadAgenciesCsvAsStream(new VitamContext(TENANT_ID), "ID").getStatus(),
            Status.OK.getStatusCode());

        assertEquals(
            client.createExternalOperation(new VitamContext(TENANT_ID),
                LogbookParametersFactory.newLogbookOperationParameters()).getStatus(),
            Status.CREATED.getStatusCode());

    }
}
