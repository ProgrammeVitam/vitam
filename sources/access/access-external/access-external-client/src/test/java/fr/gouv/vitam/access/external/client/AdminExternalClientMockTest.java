package fr.gouv.vitam.access.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.client2.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;

public class AdminExternalClientMockTest {

    AdminExternalClient client;
    private static final String DOCUMENT_ID = "1";
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
            client.checkDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes())),
            Status.OK);

        assertEquals(
            client.createDocuments(AdminCollections.FORMATS, new ByteArrayInputStream("test".getBytes())),
            Status.OK);

        assertEquals(
            client.findDocuments(AdminCollections.FORMATS, JsonHandler.createObjectNode()).toString(),
            ClientMockResultHelper.getFormatList().toString());

        assertEquals(
            client.findDocuments(AdminCollections.RULES, JsonHandler.createObjectNode()).toString(),
            ClientMockResultHelper.getRuleList().toString());

        assertEquals(
            client.findDocumentById(AdminCollections.FORMATS, DOCUMENT_ID).toString(),
            ClientMockResultHelper.getFormat().toString());

        assertEquals(
            client.findDocumentById(AdminCollections.RULES, DOCUMENT_ID).toString(),
            ClientMockResultHelper.getRule().toString());
    }

}
