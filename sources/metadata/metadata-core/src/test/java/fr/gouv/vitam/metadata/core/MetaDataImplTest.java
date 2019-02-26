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
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.DbRequest;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.database.collections.ResultDefault;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MetaDataImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private MetaData metaDataImpl;
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
        when(adminManagementClientFactory.getClient()).thenReturn(mock(AdminManagementClient.class));

        when(dbRequestFactory.create(anyString())).thenReturn(request);
        when(dbRequestFactory.create()).thenReturn(request);
        metaDataImpl =
            new MetaDataImpl(mongoDbAccessFactory, adminManagementClientFactory, indexationHelper, dbRequestFactory);
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
    public void givenInsertUnitWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        doThrow(error).when(request).execInsertUnitRequests(any());
        metaDataImpl.insertUnit(buildQueryJsonWithOptions("", DATA_INSERT));
    }

    @Test(expected = MetaDataAlreadyExistException.class)
    public void givenInsertObjectGroupWhenMongoWriteErrorThenThrowMetaDataExecutionException() throws Exception {
        final MongoWriteException error =
            new MongoWriteException(new WriteError(1, "", new BsonDocument()), new ServerAddress());
        doThrow(error).when(request).execInsertObjectGroupRequests(any());
        metaDataImpl.insertObjectGroup(buildQueryJsonWithOptions("", DATA_INSERT));
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

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void given_selectUnitquery_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(any())).thenThrow(new InvalidParseOperationException(""));
        metaDataImpl.selectUnitsByQuery(JsonHandler.getFromString(QUERY));
    }

    @RunWithCustomExecutor
    @Test
    public void given_filled_query_When_SelectUnitById_() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test
    public void given_filled_query_When_UpdateUnitById_() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_selectUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
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
        metaDataImpl.selectUnitsById(JsonHandler.getFromString(""), "unitId");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_empty_query_When_updateUnitById_ThenThrow_MetaDataExecutionException() throws Exception {
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(""), "unitId");
    }

    @RunWithCustomExecutor
    @Test(expected = MetaDataExecutionException.class)
    public void given_updateUnits_ThenThrow_MetaDataExecutionException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(any())).thenThrow(new MetaDataExecutionException(""));
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    public void given_empty_query_UpdateUnitbyId_When_IllegalAccessException_ThenThrow_MetaDataExecutionException()
        throws Exception {
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(""), "");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_UpdateUnitWhenStringTooLong_Then_Throw_InvalidParseOperationException() throws Exception {
        final int oldValue = GlobalDatasParser.limitRequest;
        try {
            GlobalDatasParser.limitRequest = 1000;
            metaDataImpl.updateUnitbyId(JsonHandler.getFromString(createLongString(1001)), "unitId");
        } finally {
            GlobalDatasParser.limitRequest = oldValue;
        }
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void given_updateUnitbyId_When_search_units_Then_Throw_InvalidParseOperationException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        when(request.execRequest(any())).thenThrow(new InvalidParseOperationException(""));
        metaDataImpl.updateUnitbyId(JsonHandler.getFromString(QUERY), "unitId");
    }

    @RunWithCustomExecutor
    @Test
    public void testSelectObjectGroupById() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
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

    @RunWithCustomExecutor
    @Test
    public void testDiffResultOnUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final List<JsonNode> wanted = JsonHandler.getFromString("[{\"#id\":\"unitId\",\"#diff\":\"-    title : title" +
            "\\n-    description : description\\n+    title : MODIFIED title" +
            "\\n+    description : MODIFIED description\"}]", List.class, JsonNode.class);

        final String wantedDiff = "\"-    title : title\\n-    description : description\\n+    " +
            "title : MODIFIED title\\n+    description : MODIFIED description\"";

        final Result updateResult = new ResultDefault(FILTERARGS.UNITS);
        updateResult.addId("unitId", (float) 1);

        final Result firstSelectResult = new ResultDefault(FILTERARGS.UNITS);
        firstSelectResult.addId("unitId", (float) 1);
        final Unit unit = new Unit();
        unit.put("_id", "unitId");
        unit.put("title", "title");
        unit.put("description", "description");
        firstSelectResult.addFinal(unit);

        final Result secondSelectResult = new ResultDefault(FILTERARGS.UNITS);
        secondSelectResult.addId("unitId", (float) 1);
        final Unit secondUnit = new Unit();
        secondUnit.put("_id", "unitId");
        secondUnit.put("title", "MODIFIED title");
        secondUnit.put("description", "MODIFIED description");
        secondSelectResult.addFinal(secondUnit);

        final JsonNode updateRequest = JsonHandler.getFromString("{\"$roots\":[\"#id\"],\"$query\":[],\"$filter\":{}," +
            "\"$action\":[{\"$set\":{\"title\":\"MODIFIED TITLE\", \"description\":\"MODIFIED DESCRIPTION\"}}]}");

        when(request.execRequest(isA(UpdateParserMultiple.class))).thenReturn(updateResult);
        when(request.execRequest(isA(SelectParserMultiple.class))).thenReturn(firstSelectResult,
            secondSelectResult);
        RequestResponse<JsonNode> requestResponse = metaDataImpl.updateUnitbyId(updateRequest, "unitId");
        assertTrue(requestResponse.isOk());
        List<JsonNode> ret = ((RequestResponseOK<JsonNode>) requestResponse).getResults();

        assertEquals(wanted, ret);

        assertEquals(wantedDiff, getDiffMessageFor(requestResponse.toJsonNode(), "unitId"));
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has("$diff") ? diff.get("$diff") : diff.get("$results");
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get("#id") != null && unitId.equals(diffNode.get("#id").textValue())) {
                return JsonHandler.writeAsString(diffNode.get("#diff"));
            }
        }
        return "";
    }

    @Test
    public void reindexCollectionUnknownTest() {
        IndexParameters parameters = new IndexParameters();
        parameters.setCollectionName("fakeName");
        List<Integer> tenants = new ArrayList<>();
        tenants.add(0);
        parameters.setTenants(tenants);

        metaDataImpl =
            new MetaDataImpl(mongoDbAccessFactory, adminManagementClientFactory, IndexationHelper.getInstance(), dbRequestFactory);
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
