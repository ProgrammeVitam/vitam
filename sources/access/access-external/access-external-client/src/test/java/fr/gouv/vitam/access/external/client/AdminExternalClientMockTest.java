/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        AdminExternalClientFactory.changeMode((SecureClientConfiguration) null);
        client = AdminExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }

    @Test
    public void testMockClient() throws Exception {
        Response checkDocumentsResponse = client.checkFormats(
            new VitamContext(TENANT_ID),
            new ByteArrayInputStream("test".getBytes())
        );
        assertEquals(Status.OK.getStatusCode(), checkDocumentsResponse.getStatus());
        assertEquals(
            client
                .createFormats(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .createRules(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .createAgencies(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()), "test.xml")
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .createProfiles(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .createProfileFile(new VitamContext(TENANT_ID), "fakeId", new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client.downloadProfileFile(new VitamContext(TENANT_ID), "fakeId").getStatus(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client
                .findFormats(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getFormatList().toString()
        );

        assertEquals(
            client
                .findRules(new VitamContext(TENANT_ID).setAccessContract(null), JsonHandler.createObjectNode())
                .toString(),
            ClientMockResultHelper.getRuleList().toString()
        );

        assertEquals(
            client.findFormatById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getFormat().toString()
        );

        assertEquals(
            client.findRuleById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), DOCUMENT_ID).toString(),
            ClientMockResultHelper.getRule().toString()
        );

        assertEquals(
            client
                .createContexts(new VitamContext(TENANT_ID), new ByteArrayInputStream("test".getBytes()))
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .checkTraceabilityOperation(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql)
                )
                .getHttpCode(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client
                .launchAudit(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(AUDIT_OPTION)
                )
                .getHttpCode(),
            Status.ACCEPTED.getStatusCode()
        );

        assertEquals(
            client
                .createSecurityProfiles(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    new ByteArrayInputStream("test".getBytes()),
                    "test.json"
                )
                .getHttpCode(),
            Status.CREATED.getStatusCode()
        );

        assertEquals(
            client
                .findSecurityProfiles(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.createObjectNode()
                )
                .getHttpCode(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client.findSecurityProfileById(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), "ID").getHttpCode(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client
                .updateSecurityProfile(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    "ID",
                    JsonHandler.createObjectNode()
                )
                .getHttpCode(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client
                .updateProfile(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    "ID",
                    JsonHandler.createObjectNode()
                )
                .getHttpCode(),
            Status.OK.getStatusCode()
        );
        assertEquals(
            client
                .listOperationsDetails(new VitamContext(TENANT_ID).setAccessContract(CONTRACT), new ProcessQuery())
                .getHttpCode(),
            Status.OK.getStatusCode()
        );
        assertEquals(
            client.downloadRulesReport(new VitamContext(TENANT_ID), "ID").getStatus(),
            Status.OK.getStatusCode()
        );
        assertEquals(
            client.downloadAgenciesCsvAsStream(new VitamContext(TENANT_ID), "ID").getStatus(),
            Status.OK.getStatusCode()
        );

        assertEquals(
            client
                .createExternalOperation(
                    new VitamContext(TENANT_ID),
                    LogbookParametersFactory.newLogbookOperationParameters()
                )
                .getStatus(),
            Status.CREATED.getStatusCode()
        );
    }
}
