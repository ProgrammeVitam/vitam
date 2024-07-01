/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmdemo.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.v2.AccessExternalClientV2;
import fr.gouv.vitam.access.external.client.v2.AccessExternalClientV2Factory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests UserInterfaceTransactionManager class
 */
public class UserInterfaceTransactionManagerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static String SELECT_ID_DSL_QUERY = "{ $roots : [ '1' ] }";
    private static String SEARCH_UNIT_DSL_QUERY =
        "{ \"$queries\": [{$eq : { #id : 1 }}], \"$filter\": {$orderby : { TransactedDate : 1 } }, " +
        "\"$projection\": {$fields : {#id : 1, Title : 1, TransactedDate:1 }}}";
    private static String ID_UNIT = "1";
    private static String UNIT_DETAILS =
        "{$hits: {'total':'1'}, $results:[{#id: '1', Title: 'Archive 1', DescriptionLevel: 'Archive Mock'}]}";
    private static String SEARCH_RESULT =
        "{$hits: {'total':'1'}, $results:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}";
    private static String UPDATE_FIELD_IMPACTED_RESULT =
        "{$hits: {'total':'1'}, $results:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}";
    private static String UPDATE_UNIT_DSL_QUERY =
        "{ \"$queries\": [{$eq : { '#id' : 1 }}], \"$filter\": {$orderby : { TransactedDate : 1 } }, " +
        "\"$actions\": {#id : 1, Title : 1, TransactedDate:1 }}";
    private static String OBJECT_GROUP_QUERY =
        "{\"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {}}";

    final int TENANT_ID = 0;
    final String CONTRACT_NAME = "contract";
    final String APP_SESSION_ID = "app-session-id";
    private static String ID_OBJECT_GROUP = "idOG1";
    private static RequestResponse unitDetails;
    private static RequestResponse searchResult;
    private static RequestResponse updateResult;

    private static AsyncResponseJunitTest asynResponse = new AsyncResponseJunitTest();

    private AccessExternalClientFactory accessExternalClientFactory;
    private AdminExternalClientFactory adminExternalClientFactory;
    private AccessExternalClientV2Factory accessExternalClientV2Factory;

    private AccessExternalClient accessExternalClient;
    private AdminExternalClient adminExternalClient;
    private AccessExternalClientV2 accessExternalClientV2;

    private VitamContext context = getVitamContext();
    private UserInterfaceTransactionManager userInterfaceTransactionManager;

    @BeforeClass
    public static void setup() throws Exception {
        unitDetails = JsonHandler.getFromString(UNIT_DETAILS, RequestResponseOK.class, JsonNode.class);
        searchResult = JsonHandler.getFromString(SEARCH_RESULT, RequestResponseOK.class, JsonNode.class);
        updateResult = JsonHandler.getFromString(UPDATE_FIELD_IMPACTED_RESULT, RequestResponseOK.class, JsonNode.class);
    }

    @Before
    public void setupTests() {
        accessExternalClientFactory = mock(AccessExternalClientFactory.class);
        accessExternalClient = org.mockito.Mockito.spy(AccessExternalClient.class);
        when(accessExternalClientFactory.getClient()).thenReturn(accessExternalClient);

        accessExternalClientV2Factory = mock(AccessExternalClientV2Factory.class);
        accessExternalClientV2 = org.mockito.Mockito.spy(AccessExternalClientV2.class);
        when(accessExternalClientV2Factory.getClient()).thenReturn(accessExternalClientV2);

        adminExternalClientFactory = mock(AdminExternalClientFactory.class);
        adminExternalClient = org.mockito.Mockito.spy(AdminExternalClient.class);
        when(adminExternalClientFactory.getClient()).thenReturn(adminExternalClient);

        userInterfaceTransactionManager = new UserInterfaceTransactionManager(
            accessExternalClientFactory,
            adminExternalClientFactory,
            accessExternalClientV2Factory,
            DslQueryHelper.getInstance()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessSearchUnits() throws Exception {
        when(accessExternalClient.selectUnits(any(), any())).thenReturn(searchResult);
        // Test method
        final RequestResponseOK result = (RequestResponseOK) userInterfaceTransactionManager.searchUnits(
            JsonHandler.getFromString(SEARCH_UNIT_DSL_QUERY),
            context
        );
        assertTrue(result.getHits().getTotal() == 1);
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessGetArchiveUnitDetails() throws Exception {
        when(
            accessExternalClient.selectUnitbyId(any(), eq(JsonHandler.getFromString(SELECT_ID_DSL_QUERY)), eq(ID_UNIT))
        ).thenReturn(unitDetails);
        // Test method
        final RequestResponseOK<JsonNode> archiveDetails =
            (RequestResponseOK) userInterfaceTransactionManager.getArchiveUnitDetails(
                JsonHandler.getFromString(SELECT_ID_DSL_QUERY),
                ID_UNIT,
                context
            );
        assertTrue(archiveDetails.getResults().get(0).get("Title").textValue().equals("Archive 1"));
    }

    @Test
    @RunWithCustomExecutor
    public void testgetLifecycleUnit() throws Exception {
        when(accessExternalClient.selectUnitLifeCycleById(any(), any(), any())).thenReturn(searchResult);

        // Test method
        final RequestResponseOK results = (RequestResponseOK) userInterfaceTransactionManager.selectUnitLifeCycleById(
            "1",
            context
        );

        ArgumentCaptor<JsonNode> selectArgument = ArgumentCaptor.forClass(JsonNode.class);
        verify(accessExternalClient).selectUnitLifeCycleById(any(), any(), selectArgument.capture());
        assertFalse(selectArgument.getValue().has("$filter"));
        assertFalse(selectArgument.getValue().has("$query"));
    }

    @Test
    @RunWithCustomExecutor
    public void testgetLifecycleObjectGroup() throws Exception {
        when(accessExternalClient.selectObjectGroupLifeCycleById(any(), any(), any())).thenReturn(searchResult);

        // Test method
        final RequestResponseOK results =
            (RequestResponseOK) userInterfaceTransactionManager.selectObjectGroupLifeCycleById("1", context);

        ArgumentCaptor<JsonNode> selectArgument = ArgumentCaptor.forClass(JsonNode.class);
        verify(accessExternalClient).selectObjectGroupLifeCycleById(any(), any(), selectArgument.capture());
        assertFalse(selectArgument.getValue().has("$filter"));
        assertFalse(selectArgument.getValue().has("$query"));
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessUpdateUnits() throws Exception {
        when(accessExternalClient.updateUnitbyId(any(), any(), any())).thenReturn(updateResult);
        // Test method
        final RequestResponseOK results = (RequestResponseOK) userInterfaceTransactionManager.updateUnits(
            JsonHandler.getFromString(UPDATE_UNIT_DSL_QUERY),
            "1",
            context
        );
        assertTrue(results.getHits().getTotal() == 1);
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessSelectObjectbyId() throws Exception {
        final RequestResponse result = JsonHandler.getFromString(
            "{$hits: {'total':'1'}, $results:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}],$context :" +
            SEARCH_UNIT_DSL_QUERY +
            "}",
            RequestResponseOK.class,
            JsonNode.class
        );
        when(
            accessExternalClient.selectObjectMetadatasByUnitId(
                any(),
                eq(JsonHandler.getFromString(OBJECT_GROUP_QUERY)),
                eq(ID_OBJECT_GROUP)
            )
        ).thenReturn(result);
        // Test method
        final RequestResponseOK<JsonNode> objectGroup =
            (RequestResponseOK) userInterfaceTransactionManager.selectObjectbyId(
                JsonHandler.getFromString(OBJECT_GROUP_QUERY),
                ID_OBJECT_GROUP,
                context
            );
        assertTrue(objectGroup.getResults().get(0).get("#id").textValue().equals("1"));
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessGetObjectAsInputStream() throws Exception {
        when(accessExternalClient.getObjectStreamByUnitId(any(), eq(ID_OBJECT_GROUP), eq("usage"), eq(1))).thenReturn(
            new AbstractMockClient.FakeInboundResponse(
                Status.OK,
                StreamUtils.toInputStream("Vitam Test"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                null
            )
        );
        assertTrue(
            userInterfaceTransactionManager.getObjectAsInputStream(
                asynResponse,
                ID_OBJECT_GROUP,
                "usage",
                1,
                "vitam_test",
                context,
                null
            )
        );
    }

    @Test
    public void testExtractInformationFromTimestamp() throws Exception {
        final InputStream tokenFile = PropertiesUtils.getResourceAsStream("token.tsp");
        String encodedTimeStampToken = IOUtils.toString(tokenFile, "UTF-8");

        final JsonNode timeStampInfo = userInterfaceTransactionManager.extractInformationFromTimestamp(
            encodedTimeStampToken
        );
        assertTrue(!timeStampInfo.isNull());
        assertTrue("2017-05-26T17:25:26".equals(timeStampInfo.get("genTime").asText()));
        assertTrue(
            "C=FR,ST=idf,L=paris,O=Vitam.,CN=CA_timestamping".equals(timeStampInfo.get("signerCertIssuer").asText())
        );

        try {
            userInterfaceTransactionManager.extractInformationFromTimestamp("FakeTimeStamp");
            fail("Should raized an exception");
        } catch (BadRequestException e) {
            // do nothing
        }
    }

    private VitamContext getVitamContext() {
        return new VitamContext(TENANT_ID).setAccessContract(CONTRACT_NAME).setApplicationSessionId(APP_SESSION_ID);
    }
}
