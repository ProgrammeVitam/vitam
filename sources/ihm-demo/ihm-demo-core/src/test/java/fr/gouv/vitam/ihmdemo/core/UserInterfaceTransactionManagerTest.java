/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.ihmdemo.core;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;

/**
 * Tests UserInterfaceTransactionManager class
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({AccessExternalClientFactory.class})
public class UserInterfaceTransactionManagerTest {
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
    private static final String ALL_PARENTS =
        "[{#id:'ID029',Title:'ID029',#unitups:['ID028', 'ID030'],_tenant:0}, " +
            "{#id:'ID028',Title:'ID028',#unitups:['ID027'],_tenant:0}," +
            "{#id:'ID030',Title:'ID030',#unitups:['ID027'],_tenant:0}," +
            "{#id:'ID027',Title:'ID027',#unitups:['ID026', 'ID025'],_tenant:0}," +
            "{#id:'ID026',Title:'ID026',#unitups:[],_tenant:0}," +
            "{#id:'ID025',Title:'ID025',#unitups:[],_tenant:0}]";

    private static String ID_OBJECT_GROUP = "idOG1";
    private static RequestResponse unitDetails;
    private static RequestResponse searchResult;
    private static RequestResponse updateResult;
    private static JsonNode allParents;

    private static AsyncResponseJunitTest asynResponse = new AsyncResponseJunitTest();

    private static AccessExternalClientFactory accessClientFactory;
    private static AccessExternalClient accessClient;

    @BeforeClass
    public static void setup() throws Exception {
        unitDetails = JsonHandler.getFromString(UNIT_DETAILS, RequestResponseOK.class, JsonNode.class);
        searchResult = JsonHandler.getFromString(SEARCH_RESULT, RequestResponseOK.class, JsonNode.class);
        updateResult = JsonHandler.getFromString(UPDATE_FIELD_IMPACTED_RESULT, RequestResponseOK.class, JsonNode.class);
        allParents = JsonHandler.getFromString(ALL_PARENTS);
    }

    @Before
    public void setupTests() throws Exception {
        PowerMockito.mockStatic(AccessExternalClientFactory.class);
        accessClientFactory = PowerMockito.mock(AccessExternalClientFactory.class);
        accessClient = org.mockito.Mockito.mock(AccessExternalClient.class);
        PowerMockito.when(AccessExternalClientFactory.getInstance()).thenReturn(accessClientFactory);
        PowerMockito.when(AccessExternalClientFactory.getInstance().getClient()).thenReturn(accessClient);
    }

    @Test
    public void testSuccessSearchUnits()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        when(accessClient.selectUnits(anyObject())).thenReturn(searchResult);
        // Test method
        final RequestResponseOK result = (RequestResponseOK) UserInterfaceTransactionManager
            .searchUnits(JsonHandler.getFromString(SEARCH_UNIT_DSL_QUERY));
        assertTrue(result.getHits().getTotal() == 1);
    }

    @Test
    public void testSuccessGetArchiveUnitDetails()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        when(accessClient.selectUnitbyId(JsonHandler.getFromString(SELECT_ID_DSL_QUERY), ID_UNIT))
            .thenReturn(unitDetails);
        // Test method
        final RequestResponseOK<JsonNode> archiveDetails =
            (RequestResponseOK) UserInterfaceTransactionManager
                .getArchiveUnitDetails(JsonHandler.getFromString(SELECT_ID_DSL_QUERY), ID_UNIT);

        assertTrue(archiveDetails.getResults().get(0).get("Title").textValue().equals("Archive 1"));
    }

    @Test
    public void testSuccessUpdateUnits()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        when(accessClient.updateUnitbyId(anyObject(), anyObject())).thenReturn(updateResult);
        // Test method
        final RequestResponseOK results = (RequestResponseOK) UserInterfaceTransactionManager.updateUnits(JsonHandler
            .getFromString(UPDATE_UNIT_DSL_QUERY), "1");
        assertTrue(results.getHits().getTotal() == 1);
    }

    @Test
    public void testSuccessSelectObjectbyId()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        final RequestResponse result =
            JsonHandler.getFromString(
                "{$hits: {'total':'1'}, $results:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}],$context :" +
                    SEARCH_UNIT_DSL_QUERY + "}", RequestResponseOK.class, JsonNode.class);
        when(accessClient.selectObjectById(JsonHandler.getFromString(OBJECT_GROUP_QUERY), ID_OBJECT_GROUP))
            .thenReturn(result);
        // Test method
        final RequestResponseOK<JsonNode> objectGroup =
            (RequestResponseOK) UserInterfaceTransactionManager
                .selectObjectbyId(JsonHandler.getFromString(OBJECT_GROUP_QUERY), ID_OBJECT_GROUP);
        assertTrue(
            objectGroup.getResults().get(0).get("#id").textValue().equals("1"));
    }

    @Test
    public void testSuccessGetObjectAsInputStream()
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException, IOException {
        when(accessClient.getObject(JsonHandler.getFromString(OBJECT_GROUP_QUERY), ID_OBJECT_GROUP, "usage", 1))
            .thenReturn(new AbstractMockClient.FakeInboundResponse(Status.OK, IOUtils.toInputStream("Vitam Test"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        assertTrue(UserInterfaceTransactionManager.getObjectAsInputStream(asynResponse,
            JsonHandler.getFromString(OBJECT_GROUP_QUERY), ID_OBJECT_GROUP, "usage", 1, "vitam_test"));
    }

    @Test
    public void testBuildUnitTree() throws VitamException {
        final JsonNode unitTree = UserInterfaceTransactionManager.buildUnitTree("ID029", allParents);
        assertTrue(unitTree.isArray());
        assertTrue(unitTree.size() == 4);
        assertTrue(unitTree.get(0).isArray());

        final ArrayNode onePath = (ArrayNode) unitTree.get(0);
        assertTrue(onePath.size() == 3);

        final JsonNode oneImmediateParent = onePath.get(0);
        assertTrue(oneImmediateParent.get(UiConstants.ID.getResultCriteria()).asText().equals("ID028") ||
            oneImmediateParent.get(UiConstants.ID.getResultCriteria()).asText().equals("ID030"));

        final JsonNode nextParent = onePath.get(1);
        assertTrue(nextParent.get(UiConstants.ID.getResultCriteria()).asText().equals("ID027"));

        final JsonNode oneRoot = onePath.get(2);
        assertTrue(oneRoot.get(UiConstants.ID.getResultCriteria()).asText().equals("ID025") ||
            oneRoot.get(UiConstants.ID.getResultCriteria()).asText().equals("ID026"));
    }
}
