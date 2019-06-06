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
package fr.gouv.vitam.metadata.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.DbRequest;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.database.collections.ResultDefault;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.metadata.core.model.UpdatedDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_METADATA_UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class MetaDataImplTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private MetaDataImpl metaDataImpl;
    private DbRequest request;
    private DbRequestFactory dbRequestFactory;
    private AdminManagementClientFactory adminManagementClientFactory;
    private IndexationHelper indexationHelper;
    private MongoDbAccessMetadataImpl mongoDbAccessFactory;
    private static final String DATA_INSERT = "{ \"data\": \"test\" }";

    private static final String SAMPLE_OBJECTGROUP_FILENAME = "sample_objectGroup_document.json";
    private static final String SAMPLE_OBJECTGROUP_FILTERED_FILENAME = "sample_objectGroup_document_filtered.json";
    private static JsonNode sampleObjectGroup;
    private static JsonNode sampleObjectGroupFiltered;

    private static final String QUERY =
        "{ \"$queries\": [{ \"$path\": \"aaaaa\" }],\"$filter\": { },\"$projection\": {},\"$facets\": []}";
    private AdminManagementClient adminManagementClient;

    private static JsonNode buildQueryJsonWithOptions(String query, String data)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(new StringBuilder()
            .append("{ $roots : [ '' ], ").append("$query : [ ").append(query).append(" ], ").append("$data : ")
            .append(data).append(" }")
            .toString());
    }

    private static String createLongString(int size) {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }

        return sb.toString();
    }

    @BeforeClass
    public static void loadStaticResources() throws Exception {
        sampleObjectGroup = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECTGROUP_FILENAME));
        sampleObjectGroupFiltered =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECTGROUP_FILTERED_FILENAME));
    }

    @Before
    public void setUp() throws Exception {
        request = mock(DbRequest.class);
        indexationHelper = mock(IndexationHelper.class);
        mongoDbAccessFactory = mock(MongoDbAccessMetadataImpl.class);
        dbRequestFactory = mock(DbRequestFactoryImpl.class);
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        adminManagementClient = mock(AdminManagementClient.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        when(dbRequestFactory.create(anyString())).thenReturn(request);
        when(dbRequestFactory.create()).thenReturn(request);
        metaDataImpl =
            new MetaDataImpl(mongoDbAccessFactory, adminManagementClientFactory, indexationHelper, dbRequestFactory,
                100, 300);

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        doThrow(new InvalidParseOperationException("")).when(request).execInsertUnitRequests(any());
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWhenDuplicateEntryThenThrowMetaDataAlreadyExistException() throws Exception {
        doThrow(new InvalidParseOperationException("")).when(request).execInsertObjectGroupRequests(any());
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void testMetaDataAlreadyExistExceptionExpected() throws Exception {
        final MetaDataAlreadyExistException error = new MetaDataAlreadyExistException("");
        doThrow(error).when(request).execInsertUnitRequests(any());
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertUnitWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        try {
            GlobalDatasParser.limitRequest = 1000;
            final String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
            metaDataImpl.insertUnit(buildQueryJsonWithOptions("", bigData));
        } finally {
            GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWhenStringTooLongThenThrowMetaDataDocumentSizeException() throws Exception {
        try {
            GlobalDatasParser.limitRequest = 1000;
            final String bigData = "{ \"data\": \"" + createLongString(1001) + "\" }";
            metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", bigData));
        } finally {
            GlobalDatasParser.limitRequest = GlobalDatasParser.DEFAULT_LIMIT_REQUEST;
        }
    }

    @Test(expected = MetaDataNotFoundException.class)
    public void givenInsertUnitWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        doThrow(MetaDataNotFoundException.class).when(request).execInsertUnitRequests(any());
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_SelectUnitWhenStringTooLong_Then_Throw_InvalidParseOperationException() throws Exception {
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(createLongString(1001)));
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_selectUnitquery_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {

        when(request.execRequest(any())).thenThrow(new InvalidParseOperationException(""));
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }

    @Test
    public void given_filled_query_When_SelectUnitById_() throws Exception {
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @Test
    public void given_filled_query_When_UpdateUnitById_() throws Exception {
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("title")
        );
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        when(request.execUpdateRequest(any(), eq("unitId"), eq(ontologyModels), eq(MetadataCollections.UNIT)))
            .thenReturn(new UpdatedDocument("unitId",
                JsonHandler.createObjectNode().put("x", "v1"),
                JsonHandler.createObjectNode().put("x", "v2")));
        UpdateUnit result = metaDataImpl.updateUnitById(JsonHandler.getFromString(QUERY), "unitId");
        assertThat(result.getUnitId()).isEqualTo("unitId");
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getDiff()).isEqualTo("-  \"x\" : \"v1\"\n+  \"x\" : \"v2\"");
        assertThat(result.getKey()).isEqualTo(UNIT_METADATA_UPDATE);
        assertThat(result.getMessage()).isEqualTo("Update unit OK.");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        when(request.execRequest(any())).thenThrow(new MetaDataExecutionException(""));
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_IllegalAccessException_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(""));
    }

    @Test(expected = MetaDataExecutionException.class)
    public void givenInsertObjectGroupWhenParentNotFoundThenThrowMetaDataNotFoundException() throws Exception {
        doThrow(MetaDataExecutionException.class).when(request).execInsertObjectGroupRequests(any());
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_selectUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        List<OntologyModel> ontologyModels = emptyList();
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        metaDataImpl.selectUnitsById(JsonHandler.getFromString(""), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_updateUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        List<OntologyModel> ontologyModels = emptyList();
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));
        metaDataImpl.updateUnitById(JsonHandler.getFromString(""), "unitId");
    }

    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        List<OntologyModel> ontologyModels = emptyList();
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        final Result selectResult = new ResultDefault(FILTERARGS.UNITS);
        selectResult.addId("unitId", (float) 1);
        final Unit unit = new Unit();
        unit.put("_id", "unitId");
        unit.put("title", "title");
        unit.put("description", "description");
        selectResult.addFinal(unit);

        when(request.execRequest(isA(RequestParserMultiple.class)))
            .thenReturn(selectResult);

        when(request.execUpdateRequest(any(), eq("unitId"), eq(ontologyModels), eq(MetadataCollections.UNIT)))
            .thenThrow(new MetaDataExecutionException(""));
        metaDataImpl.updateUnitById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_UpdateUnitWhenStringTooLong_Then_Throw_InvalidParseOperationException() throws Exception {
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            metaDataImpl.updateUnitById(JsonHandler.getFromString(createLongString(1001)), "unitId");
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitbyId_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        List<OntologyModel> ontologyModels = emptyList();
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        final Result selectResult = new ResultDefault(FILTERARGS.UNITS);
        selectResult.addId("unitId", (float) 1);
        final Unit unit = new Unit();
        unit.put("_id", "unitId");
        unit.put("title", "title");
        unit.put("description", "description");
        selectResult.addFinal(unit);

        when(request.execRequest(isA(RequestParserMultiple.class)))
            .thenReturn(selectResult);

        when(request.execUpdateRequest(any(), eq("unitId"), eq(ontologyModels), eq(MetadataCollections.UNIT)))
            .thenThrow(new InvalidParseOperationException(""));
        metaDataImpl.updateUnitById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @Test
    public void testSelectObjectGroupById() throws Exception {
        final Result result = new ResultDefault(FILTERARGS.OBJECTGROUPS);
        result.addId("ogId", (float) 1);
        result.addFinal(new ObjectGroup(sampleObjectGroup));
        when(request.execRequest(any())).thenReturn(result);
        RequestResponse<JsonNode> requestResponse =
            metaDataImpl.selectObjectGroupById(JsonHandler.getFromString(QUERY), "ogId");
        assertTrue(requestResponse.isOk());
        RequestResponseOK<JsonNode> jsonNode = (RequestResponseOK<JsonNode>) requestResponse;
        final JsonNode objectGroupDocument = jsonNode.getResults().iterator().next();
        boolean different = false;
        Iterator<Entry<String, JsonNode>> fields = objectGroupDocument.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            if (!sampleObjectGroupFiltered.has(entry.getKey()) ||
                !sampleObjectGroupFiltered.get(entry.getKey()).asText().equals(entry.getValue().asText())) {
                System.err
                    .println("D: " + entry.getKey() + " : " + entry.getValue() + " " + entry.getValue().getNodeType());
                different = true;
            }
        }
        fields = sampleObjectGroupFiltered.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            if (!objectGroupDocument.has(entry.getKey()) ||
                !objectGroupDocument.get(entry.getKey()).asText().equals(entry.getValue().asText())) {
                System.err
                    .println("S: " + entry.getKey() + " : " + entry.getValue() + " " + entry.getValue().getNodeType());
                different = true;
            }
        }
        assertFalse(different);
    }

    @Test
    public void testDiffResultOnUpdate() throws Exception {
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("title")
        );
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        final JsonNode wantedDiff = JsonHandler.getFromFile(PropertiesUtils.findFile("wantedDiff.json"));

        final Result updateResult = new ResultDefault(FILTERARGS.UNITS);
        updateResult.addId("unitId", (float) 1);

        Unit unit = new Unit();
        unit.put("_id", "unitId");
        unit.put("title", "title");
        unit.put("description", "description");

        Unit secondUnit = new Unit();
        secondUnit.put("_id", "unitId");
        secondUnit.put("title", "MODIFIED title");
        secondUnit.put("description", "MODIFIED \"description");

        final JsonNode updateRequest = JsonHandler.getFromFile(PropertiesUtils.findFile("updateQuery.json"));

        when(request.execUpdateRequest(any(), eq("unitId"), eq(ontologyModels), eq(MetadataCollections.UNIT)))
            .thenReturn(
                new UpdatedDocument("unitId", JsonHandler.toJsonNode(unit), JsonHandler.toJsonNode(secondUnit)));

        UpdateUnit result = metaDataImpl.updateUnitById(updateRequest, "unitId");

        assertEquals(wantedDiff.get("#diff").asText(), result.getDiff());
    }

    @Test
    public void testBulkUnitUpdate_OK() throws Exception {
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("title")
        );
        when(adminManagementClient.findOntologies(any()))
            .thenReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels));

        final Result updateResult = new ResultDefault(FILTERARGS.UNITS);
        updateResult.addId("unitId1", (float) 1);
        updateResult.addId("unitId2", (float) 1);

        final Unit unit1Before = createSelectUnitResult("unitId1", "value v1");
        final Unit unit1After = createSelectUnitResult("unitId1", "value v2");

        final Unit unit2Before = createSelectUnitResult("unitId2", "value v1");
        final Unit unit2After = createSelectUnitResult("unitId2", "value v2");

        when(request.execRequest(isA(SelectParserMultiple.class))).thenReturn(
            new ResultDefault(FILTERARGS.UNITS).addFinal(unit1Before),
            new ResultDefault(FILTERARGS.UNITS).addFinal(unit2Before));

        when(request.execUpdateRequest(any(), any(), eq(ontologyModels), eq(MetadataCollections.UNIT)))
            .thenReturn(
                new UpdatedDocument("unit1", JsonHandler.toJsonNode(unit1Before), JsonHandler.toJsonNode(unit1After)),
                new UpdatedDocument("unit2", JsonHandler.toJsonNode(unit2Before), JsonHandler.toJsonNode(unit2After)));

        // When
        final JsonNode updateRequest = JsonHandler.getFromFile(PropertiesUtils.findFile("updateUnits.json"));
        RequestResponseOK<UpdateUnit> requestResponse =
            (RequestResponseOK<UpdateUnit>) metaDataImpl.updateUnits(updateRequest);

        // Then
        assertThat(requestResponse.getResults().stream())
            .extracting(UpdateUnit::getUnitId, UpdateUnit::getKey, UpdateUnit::getMessage, UpdateUnit::getStatus,
                UpdateUnit::getKey, UpdateUnit::getDiff)
            .containsExactlyInAnyOrder(
                tuple("unitId1", UNIT_METADATA_UPDATE, "Update unit OK.", OK, UNIT_METADATA_UPDATE,
                    "-  \"field\" : \"value v1\"\n+  \"field\" : \"value v2\""),
                tuple("unitId2", UNIT_METADATA_UPDATE, "Update unit OK.", OK, UNIT_METADATA_UPDATE,
                    "-  \"field\" : \"value v1\"\n+  \"field\" : \"value v2\"")
            );
    }

    private Unit createSelectUnitResult(String unitId, String value) {
        final Unit unit = new Unit();
        unit.put("_id", unitId);
        unit.put("field", value);
        return unit;
    }

    @Test
    public void reindexCollectionUnknownTest() {
        IndexParameters parameters = new IndexParameters();
        parameters.setCollectionName("fakeName");
        List<Integer> tenants = new ArrayList<>();
        tenants.add(0);
        parameters.setTenants(tenants);

        metaDataImpl =
            new MetaDataImpl(mongoDbAccessFactory, adminManagementClientFactory, IndexationHelper.getInstance(),
                dbRequestFactory, 100, 300);
        IndexationResult result = metaDataImpl.reindex(parameters);
        assertNull(result.getIndexOK());
        assertNotNull(result.getIndexKO());
        assertEquals(1, result.getIndexKO().size());
        assertEquals("fakeName_0_*", result.getIndexKO().get(0).getIndexName());
        assertEquals("Try to reindex a non metadata collection 'fakeName' with metadata module", result.getIndexKO()
            .get(0).getMessage());
        assertEquals((Integer) 0, result.getIndexKO().get(0).getTenant());
    }

    @Test
    public void reindexIOExceptionTest() throws Exception {
        when(indexationHelper.reindex(any(), any(), any(), any(), any()))
            .thenThrow(new IOException());
        when(indexationHelper.getFullKOResult(any(), any()))
            .thenCallRealMethod();
        IndexParameters parameters = new IndexParameters();
        parameters.setCollectionName("Unit");
        List<Integer> tenants = new ArrayList<>();
        tenants.add(0);
        parameters.setTenants(tenants);
        IndexationResult result = metaDataImpl.reindex(parameters);
        assertNull(result.getIndexOK());
        assertNotNull(result.getIndexKO());
        assertEquals(1, result.getIndexKO().size());
        assertEquals("unit_0_*", result.getIndexKO().get(0).getIndexName().toLowerCase());
        assertEquals((Integer) 0, result.getIndexKO().get(0).getTenant());
    }

    @Test(expected = DatabaseException.class)
    public void switchIndexKOTest() throws Exception {
        doThrow(new DatabaseException("erreur")).when(indexationHelper).switchIndex(anyString(), anyString(), any());
        metaDataImpl.switchIndex("alias", "index_name");
    }

    @Test
    public void switchIndexOKTest() throws Exception {
        doNothing().when(indexationHelper).switchIndex(anyString(), anyString(), any());
        metaDataImpl.switchIndex("alias", "index_name");
    }
}
